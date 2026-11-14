package app.owlcms.access;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Per-tab navigation state. The scheduling and refresh operations are supplied by the UI boundary so this class
 * never looks up a UI or session.
 */
final class PrincipalRefresh {

	private Principal principal;
	private boolean pending;

	void authenticated(Principal principal, Consumer<Runnable> defer, Runnable refresh) {
		Principal previous = this.principal;
		this.principal = Objects.requireNonNull(principal);
		if (previous == null || previous.equals(principal) || this.pending) {
			return;
		}
		this.pending = true;
		// Remember the new identity before refreshing: the refresh itself authorizes navigation again.
		defer.accept(() -> {
			this.pending = false;
			refresh.run();
		});
	}
}
