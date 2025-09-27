package app.owlcms.init;

/**
 * Provide an InheritableThreadLocal holder for OwlcmsSession so background threads
 * created from a Vaadin session can access the OwlcmsSession instance.
 */
public final class OwlcmsSessionThreadLocal {

	private static final InheritableThreadLocal<OwlcmsSession> holder = new InheritableThreadLocal<>();

	private OwlcmsSessionThreadLocal() {
		// utility
	}

	public static OwlcmsSession get() {
		return holder.get();
	}

	public static void set(OwlcmsSession session) {
		holder.set(session);
	}

	public static void remove() {
		holder.remove();
	}

}
