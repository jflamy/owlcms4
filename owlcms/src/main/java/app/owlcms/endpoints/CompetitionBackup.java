/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.endpoints;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.slf4j.LoggerFactory;

import app.owlcms.access.Role;
import app.owlcms.access.ServletTokenGuard;
import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditSnapshot;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ProxyUtils;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Recovery checkpoint for the tracker: the export and the log files modified since {@code since}, as a zip named with
 * the OWLCMS time that the caller passes back as the next {@code since}. Nothing is rolled or sealed.
 */
@SuppressWarnings("serial")
@WebServlet(CompetitionBackup.PATH)
public class CompetitionBackup extends HttpServlet {

	static final String PATH = "/competition/backup";
	static final String SINCE = "since";
	private static final Logger logger = (Logger) LoggerFactory.getLogger(CompetitionBackup.class);

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String host = ProxyUtils.getClientIp(request);
		AuditActor actor = ServletTokenGuard.authorize(request, response, host, Role.COMPETITION_SUPERVISOR,
		        "backup");
		if (actor == null) {
			response.flushBuffer();
			return;
		}
		Instant since;
		try {
			since = AuditSnapshot.parseCheckpointTime(request.getParameter(SINCE));
		} catch (DateTimeParseException e) {
			logger.info("{} backup refused: since={} is not a time", host, request.getParameter(SINCE));
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		Instant selectedAt = Instant.now();
		response.reset();
		response.setContentType("application/zip");
		response.setHeader("Content-Disposition",
		        "attachment; filename=\"" + AuditSnapshot.checkpointName(selectedAt) + "\"");
		try (OutputStream out = response.getOutputStream()) {
			AuditSnapshot.writeCheckpoint(out, actor, since);
		} catch (Throwable t) {
			// once bytes are sent the status cannot change: the zip is left incomplete and the caller rejects it
			LoggerUtils.logError(logger, t);
			if (!response.isCommitted()) {
				response.reset();
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
	}
}
