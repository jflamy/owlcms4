package app.owlcms.endpoints;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.access.AdminRecovery;
import app.owlcms.access.LoginThrottle;
import app.owlcms.audit.AuthAudit;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Registered in front of every servlet, Vaadin included, only in admin recovery mode: apart from the control panel
 * stop, every request gets the recovery form. Plain HTML, so nothing else needs to be reachable.
 */
public class AdminRecoveryFilter implements Filter {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(AdminRecoveryFilter.class);
	private static final String AUDIT_PATH = "/recovery";

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
	        throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		String path = request.getRequestURI().substring(request.getContextPath().length());
		if (AdminRecovery.passesThrough(path)) {
			chain.doFilter(servletRequest, servletResponse);
			return;
		}
		if (!AdminRecovery.isReady()) {
			// the database is not open yet; translations are not needed for a page that only reloads itself
			writeStarting(response);
			return;
		}

		Locale locale = OwlcmsSession.findLanguageOnlyMatch(Collections.list(request.getLocales()),
		        Translator.getAvailableLocales());
		if (AdminRecovery.isDone()) {
			writeMessage(response, locale, Translator.translate("AdminRecovery.Done", locale));
			return;
		}
		String expected = AdminRecovery.expectedToken();
		if (expected == null) {
			writeMessage(response, locale, Translator.translate("AdminRecovery.NoToken", locale));
			return;
		}

		String error = null;
		if ("POST".equalsIgnoreCase(request.getMethod())) {
			String clientIp = request.getRemoteAddr();
			LoginThrottle throttle = LoginThrottle.shared();
			long blocked = throttle.blockedSeconds(AdminRecovery.THROTTLE_USER, clientIp);
			String password = request.getParameter("password");
			switch (AdminRecovery.check(expected, request.getParameter("token"), password,
			        request.getParameter("confirm"), blocked)) {
				case ACCEPTED -> {
					throttle.success(AdminRecovery.THROTTLE_USER, clientIp);
					if (AdminRecovery.setAdminPassword(password)) {
						logger.info("admin password set in recovery mode from {}", clientIp);
					}
					writeMessage(response, locale, Translator.translate("AdminRecovery.Done", locale));
					return;
				}
				case THROTTLED -> {
					AuthAudit.tokenRefused(clientIp, AUDIT_PATH, "throttled", null, null);
					error = Translator.translate("Access.TooManyAttempts", locale) + " (" + blocked + " s)";
				}
				case INVALID_TOKEN -> {
					throttle.fail(AdminRecovery.THROTTLE_USER, clientIp);
					AuthAudit.tokenRefused(clientIp, AUDIT_PATH, "invalid", null, null);
					logger.info("admin recovery refused from {}: wrong token", clientIp);
					error = Translator.translate("AdminRecovery.WrongToken", locale);
				}
				case UNACCEPTABLE -> error = Translator.translate("Access.Error.PasswordTooShort", locale);
				case MISMATCH -> error = Translator.translate("Access.Error.PasswordMismatch", locale);
				case NO_TOKEN_CONFIGURED -> error = Translator.translate("AdminRecovery.NoToken", locale);
			}
		}
		writeForm(response, locale, error);
	}

	private void writeStarting(HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		response.setHeader("Retry-After", "2");
		PrintWriter pw = start(response, Locale.ENGLISH, "OWLCMS");
		pw.println("<meta http-equiv='refresh' content='2'></head><body><p>&hellip;</p></body></html>");
		pw.flush();
	}

	private void writeMessage(HttpServletResponse response, Locale locale, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		PrintWriter pw = start(response, locale, Translator.translate("AdminRecovery.Title", locale));
		pw.println("</head><body>");
		pw.println("<h2>" + html(Translator.translate("AdminRecovery.Title", locale)) + "</h2>");
		pw.println("<p>" + html(message) + "</p>");
		pw.println("</body></html>");
		pw.flush();
	}

	private void writeForm(HttpServletResponse response, Locale locale, String error) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		String title = Translator.translate("AdminRecovery.Title", locale);
		PrintWriter pw = start(response, locale, title);
		pw.println("</head><body>");
		pw.println("<h2>" + html(title) + "</h2>");
		pw.println("<p>" + html(Translator.translate("AdminRecovery.Explanation", locale)) + "</p>");
		if (error != null) {
			pw.println("<p style='color:#b00020'>" + html(error) + "</p>");
		}
		pw.println("<form method='post' action=''>");
		pw.println(field(Translator.translate("AdminRecovery.Token", locale), "token", true));
		pw.println("<p>" + html(Translator.translate("AdminRecovery.TokenHint", locale)) + "</p>");
		pw.println(field(Translator.translate("Access.Account.NewPassword", locale), "password", false));
		pw.println(field(Translator.translate("Access.Account.ConfirmPassword", locale), "confirm", false));
		pw.println("<button type='submit'>" + html(Translator.translate("AdminRecovery.Submit", locale)) + "</button>");
		pw.println("</form>");
		pw.println("</body></html>");
		pw.flush();
	}

	private PrintWriter start(HttpServletResponse response, Locale locale, String title) throws IOException {
		response.setHeader("Cache-Control", "no-store");
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter pw = new PrintWriter(response.getOutputStream(), true, StandardCharsets.UTF_8);
		String dir = Translator.isRTL(locale) ? " dir='rtl'" : "";
		pw.println("<!doctype html>");
		pw.println("<html lang='" + html(locale.toLanguageTag()) + "'" + dir + "><head><meta charset='utf-8'>");
		pw.println("<title>" + html(title) + "</title>");
		pw.println("<style>body{font-family:Arial,Helvetica,sans-serif}</style>");
		return pw;
	}

	private String field(String label, String name, boolean autofocus) {
		return "<p><label>" + html(label) + "<br><input type='password' name='" + name + "' autocomplete='off'"
		        + (autofocus ? " autofocus" : "") + "></label></p>";
	}

	private static String html(String text) {
		return StringEscapeUtils.escapeHtml4(text == null ? "" : text);
	}
}
