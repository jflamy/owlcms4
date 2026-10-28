package app.owlcms.audit;

/**
 * Who acted and where the action was triggered from, captured where the action starts and passed down as a
 * parameter to the back-end function that makes the change and writes the audit entry.
 *
 * @param actor the person or station acting; never null
 * @param cause plain text naming the page, button or process that triggered the change
 */
public record AuditOrigin(AuditActor actor, String cause) {

	public AuditOrigin {
		if (actor == null) {
			actor = AuditActor.system();
		}
	}

	/** For processes that no person triggered, such as startup. */
	public static AuditOrigin system(String cause) {
		return new AuditOrigin(AuditActor.system(), cause);
	}

	/**
	 * Captures the person on the current page. Call only on the UI thread, where the action starts; the result is
	 * then passed down, including to background work.
	 */
	public static AuditOrigin fromUi(String cause) {
		return new AuditOrigin(AuditActor.fromCurrentUi(), cause);
	}
}
