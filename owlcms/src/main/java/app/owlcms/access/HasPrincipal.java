package app.owlcms.access;

/**
 * A page, or a component created by a page, that holds the principal captured on the UI thread when the page was
 * created. Access checks and audit attribution read it from here, never from the session: event handlers run on
 * event-bus threads, where the session lookup does not designate the page's user.
 */
public interface HasPrincipal {

	Principal getPrincipal();
}
