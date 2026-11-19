package app.owlcms.access;

import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.nui.home.LoginView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Ends a logged-in session from the click that asked for it: every UI is sent to the login page, the response to the
 * click deletes every cookie that remembers a login on this browser, and the HTTP session is invalidated, so that
 * nothing brings the user back in, even when the browser restores its tabs after a restart.
 */
public final class SessionLogout {

	private SessionLogout() {
	}

	/**
	 * @param request  the HTTP request of the click
	 * @param response its response, which carries the cookie deletions
	 */
	public static void redirectAllAndInvalidate(VaadinSession session, HttpServletRequest request,
	        HttpServletResponse response) {
		session.checkHasLock();
		for (UI ui : List.copyOf(session.getUIs())) {
			ui.getPage().setLocation(LoginView.LOGIN);
		}
		if (response != null) {
			RememberCookies.forgetAll(request, response);
		}
		session.getSession().invalidate();
	}
}
