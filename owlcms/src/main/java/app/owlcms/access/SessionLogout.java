package app.owlcms.access;

import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.endpoints.LogoutServlet;

/**
 * Redirects every UI before Vaadin discards the shared session at the end of the request. The redirect goes through
 * {@link LogoutServlet}, whose response ends the HTTP session and deletes the account cookie kept for the day.
 */
public final class SessionLogout {

	private SessionLogout() {
	}

	public static void redirectAllAndInvalidate(VaadinSession session) {
		session.checkHasLock();
		for (UI ui : List.copyOf(session.getUIs())) {
			ui.getPage().setLocation(LogoutServlet.PATH);
		}
		session.close();
	}
}