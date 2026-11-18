package app.owlcms.endpoints;

import java.io.IOException;

import org.slf4j.LoggerFactory;

import app.owlcms.access.RememberedBrowser.Kind;
import app.owlcms.nui.home.LoginView;
import app.owlcms.utils.ProxyUtils;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * The end of a logout. The pages navigate here after the identity was forgotten: this response deletes every cookie
 * that remembers a login on this browser (account kept for the day, officials PIN, display PIN) and ends the HTTP
 * session, so that nothing brings the user back in, even when the browser restores its tabs after a restart.
 */
@SuppressWarnings("serial")
public class LogoutServlet extends HttpServlet {

	/** Relative to the application, like the routes. */
	public static final String PATH = "logout";
	private static final Logger logger = (Logger) LoggerFactory.getLogger(LogoutServlet.class);

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		for (Kind kind : Kind.values()) {
			Cookie forgotten = new Cookie(kind.getCookieName(), "");
			forgotten.setHttpOnly(true);
			forgotten.setPath(RememberServlet.cookiePath(request));
			forgotten.setMaxAge(0);
			response.addCookie(forgotten);
		}

		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
			logger.info("{} HTTP session ended at logout", ProxyUtils.getClientIp(request));
		}
		response.setHeader("Cache-Control", "no-store");
		response.sendRedirect(request.getContextPath() + "/" + LoginView.LOGIN);
	}
}
