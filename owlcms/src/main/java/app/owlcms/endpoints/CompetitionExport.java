/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
//
//  Copyright (C) 2009 BalusC
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Lesser General Public License as published by the Free Software Foundation, either version 3
//  of the License, or (at your option) any later version.
//
//  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public License along with this library.
//  If not, see <http://www.gnu.org/licenses/>.

package app.owlcms.endpoints;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Supplier;

import org.slf4j.LoggerFactory;

import app.owlcms.access.LoginThrottle;
import app.owlcms.access.Role;
import app.owlcms.access.TokenAuthenticator;
import app.owlcms.apputils.AccessUtils;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.audit.AuditActor;
import app.owlcms.audit.ExportAudit;
import app.owlcms.data.export.CompetitionData;
import app.owlcms.data.export.v2.CompetitionDataV2;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.init.OwlcmsSessionThreadLocal;
import app.owlcms.spreadsheet.JXLSSBDEExport;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ProxyUtils;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A file servlet supporting resume of downloads and client-side caching and GZIP of text content. This servlet can also be used for images, client-side caching
 * would become more efficient. This servlet can also be used for text files, GZIP would decrease network bandwidth.
 *
 * @author BalusC
 * @link http://balusc.blogspot.com/2009/02/fileservlet-supporting-resume-and.html
 */
@SuppressWarnings("serial")
/**
 * Modified to fetch files under the ./local directory relative to the startup directory and, failing that, as a resource on the classpath.
 *
 * @author Jean-François Lamy
 *
 */
@WebServlet(urlPatterns = {
        CompetitionExport.LEGACY_JSON_V1_PATH,
        CompetitionExport.JSON_V1_PATH,
        CompetitionExport.JSON_V2_PATH,
        CompetitionExport.SBDE_PATH
})
public class CompetitionExport extends HttpServlet {

	static final String LEGACY_JSON_V1_PATH = "/competition/export";
	static final String JSON_V1_PATH = "/competition/export/json/1";
	static final String JSON_V2_PATH = "/competition/export/json/2";
	static final String SBDE_PATH = "/competition/export/sbde";
	private static final DateTimeFormatter EXPORT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm';'ss");
	private static final String JSON_CONTENT_TYPE = "application/json";
	private static final String XLS_CONTENT_TYPE = "application/vnd.ms-excel";
	private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private static class ExportPayload {
		private final Supplier<InputStream> inputStreamSupplier;
		private final String contentType;
		private final String fileName;
		private final boolean text;

		private ExportPayload(Supplier<InputStream> inputStreamSupplier, String contentType, String fileName, boolean text) {
			this.inputStreamSupplier = inputStreamSupplier;
			this.contentType = contentType;
			this.fileName = fileName;
			this.text = text;
		}

		private InputStream openStream() {
			return this.inputStreamSupplier.get();
		}
	}

	// Helpers (can be refactored to public utility class)
	// ----------------------------------------

	private static Logger logger = (Logger) LoggerFactory.getLogger(CompetitionExport.class);
	// { logger.setLevel(Level.DEBUG); }

	// Inner classes
	// ------------------------------------------------------------------------------

	/**
	 * Process GET request.
	 *
	 * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse).
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		// Process request with content.
		processRequest(request, response, true);
	}

	/**
	 * Process HEAD request. This returns the same headers as GET request, but without content.
	 *
	 * @see HttpServlet#doHead(HttpServletRequest, HttpServletResponse).
	 */
	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		// Process request without content.
		processRequest(request, response, false);
	}

	/**
	 * Process the actual request.
	 *
	 * @param request  The request to be processed.
	 * @param response The response to be created.
	 * @param content  Whether the request body should be written (GET) or not (HEAD).
	 * @throws IOException If something fails at I/O level.
	 */
	private void processRequest(HttpServletRequest request, HttpServletResponse response, boolean content)
	        throws IOException {
		logger.info("processing competition export request {}", request.getServletPath());
		// use proxyutils because this is a plain servlet, not a Vaadin servlet
		String host = ProxyUtils.getClientIp(request);
		AuditActor actor = authorize(request, response, host);
		if (actor == null) {
			response.flushBuffer();
			return;
		}

		// Prepare and initialize response
		// --------------------------------------------------------

		// Initialize response.
		response.reset();

		// Prepare streams.
		InputStream inputStream = null;
		OutputStream output = null;

		try {
			installRequestSession(request);
			ExportPayload payload = createPayload(request.getServletPath(), actor);
			if (payload == null) {
				response.setStatus(404);
				response.flushBuffer();
				return;
			}

			// Open streams.
			response.setContentType(payload.contentType);
			if (payload.text) {
				response.setCharacterEncoding("UTF-8");
			}
			if (payload.fileName != null) {
				response.setHeader("Content-Disposition", "attachment; filename=\"" + payload.fileName + "\"");
			}

			if (content) {
				output = response.getOutputStream();
				inputStream = payload.openStream();
				inputStream.transferTo(output);
				output.flush();
			}

			response.setStatus(200);
			response.flushBuffer();
		} catch (Throwable t) {
			logger.error("{}", LoggerUtils.stackTrace(t));
			response.setStatus(500);
		} finally {
			OwlcmsSessionThreadLocal.remove();
			if (output != null) {
				output.close();
			}
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}

	private ExportPayload createPayload(String path, AuditActor actor) {
		if (LEGACY_JSON_V1_PATH.equals(path) || JSON_V1_PATH.equals(path)) {
			return new ExportPayload(() -> new CompetitionData().exportData(actor, ExportAudit.CHANNEL_HTTP),
			        JSON_CONTENT_TYPE, null, true);
		}
		if (JSON_V2_PATH.equals(path)) {
			return new ExportPayload(() -> new CompetitionDataV2().exportData(actor, ExportAudit.CHANNEL_HTTP),
			        JSON_CONTENT_TYPE, null, true);
		}
		if (SBDE_PATH.equals(path)) {
			JXLSSBDEExport sbdeExport = new JXLSSBDEExport(null);
			String extension = sbdeExport.getFileExtension();
			if (extension == null || extension.isBlank()) {
				extension = ".xlsx";
			}
			String fileName = "SBDE_" + LocalDateTime.now().withNano(0).format(EXPORT_TIMESTAMP_FORMAT) + extension;
			return new ExportPayload(sbdeExport::createInputStream, excelContentType(extension), fileName, false);
		}
		return null;
	}

	private String excelContentType(String extension) {
		return ".xls".equals(extension) ? XLS_CONTENT_TYPE : XLSX_CONTENT_TYPE;
	}

	/**
	 * PIN mode: local network or backdoor list, as before. Accounts mode: a token whose owner holds the results
	 * role, whatever the address. Sets the refusal status itself.
	 *
	 * @return the audit actor for the export, or null when refused
	 */
	private AuditActor authorize(HttpServletRequest request, HttpServletResponse response, String host) {
		if (!Config.getCurrent().isAccountsMode()) {
			if (AccessUtils.isLocalNetwork(host) || AccessUtils.checkBackdoor(host)) {
				logger.info("{} authorized full state access", host);
				return AuditActor.http(host);
			}
			logger.error("{} not local and not in backdoor list, denied full state access", host);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}
		String path = request.getServletPath();
		String token = TokenAuthenticator.presentedToken(request.getParameter("token"),
		        request.getHeader("Authorization"));
		TokenAuthenticator authenticator = new TokenAuthenticator(UserAccountRepository::findByTokenHash,
		        LoginThrottle.shared());
		switch (authenticator.authenticate(token, host)) {
			case TokenAuthenticator.Result.Authenticated authenticated -> {
				String username = authenticated.account().getUsername();
				String tokenId = authenticated.token().getTokenId();
				if (TokenAuthenticator.holds(authenticated.account(), Role.RESULTS)) {
					logger.info("{} export authorized by token id={} of account {}", host, tokenId, username);
					return AuditActor.token(username, tokenId, host);
				}
				logger./**/warn("{} export refused: token id={} of account {} does not hold the results role", host,
				        tokenId, username);
				AuthAudit.tokenRefused(host, path, "forbidden", username, tokenId);
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			}
			case TokenAuthenticator.Result.Missing _ -> {
				logger.info("{} export refused: no token", host);
				response.setHeader("WWW-Authenticate", "Bearer");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			}
			case TokenAuthenticator.Result.Invalid _ -> {
				AuthAudit.tokenRefused(host, path, "invalid", null, null);
				response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			}
			case TokenAuthenticator.Result.Throttled throttled -> {
				AuthAudit.tokenRefused(host, path, "throttled", null, null);
				response.setHeader("Retry-After", Long.toString(throttled.seconds()));
				response.setStatus(429);
			}
		}
		return null;
	}

	private void installRequestSession(HttpServletRequest request) {
		OwlcmsSession requestSession = new OwlcmsSession();
		OwlcmsSessionThreadLocal.set(requestSession);
		Locale locale = request.getLocale();
		requestSession.setLocale(locale != null ? locale : Locale.ENGLISH);
	}

}
