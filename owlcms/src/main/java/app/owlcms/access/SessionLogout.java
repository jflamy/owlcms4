package app.owlcms.access;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.nui.home.LoginView;
import ch.qos.logback.classic.Logger;

/** Redirects every UI in one Vaadin session before ending the shared HTTP session. */
public final class SessionLogout {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(SessionLogout.class);

	private SessionLogout() {
	}

	public static void redirectAllAndInvalidate(VaadinSession session) {
		List<UI> sessionUis = List.copyOf(session.getUIs());
		Thread.ofVirtual().name("session-logout").start(() -> {
			for (UI ui : sessionUis) {
				try {
					ui.access(() -> ui.getPage().setLocation(LoginView.LOGIN)).get();
				} catch (UIDetachedException _) {
					// The tab closed while logout was being delivered.
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				} catch (ExecutionException e) {
					logger.warn("Could not redirect a UI during logout", e.getCause());
				}
			}
			try {
				session.access(() -> session.getSession().invalidate()).get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				logger.warn("Could not invalidate session after logout", e.getCause());
			}
		});
	}
}