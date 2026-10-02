package app.owlcms.access;

import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.nui.home.LoginView;

/** Redirects every UI before Vaadin discards the shared session at the end of the request. */
public final class SessionLogout {

	private SessionLogout() {
	}

	public static void redirectAllAndInvalidate(VaadinSession session) {
		session.checkHasLock();
		for (UI ui : List.copyOf(session.getUIs())) {
			ui.getPage().setLocation(LoginView.LOGIN);
		}
		session.close();
	}
}