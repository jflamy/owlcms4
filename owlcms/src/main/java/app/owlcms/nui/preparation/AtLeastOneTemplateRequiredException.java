package app.owlcms.nui.preparation;

/**
 * Thrown when a document set requires at least one template to be present but none are available.
 * This is an unchecked exception used to signal UI-level precheck failures that have
 * already been rendered inside the dialog, so callers should avoid duplicating notifications.
 */
public class AtLeastOneTemplateRequiredException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AtLeastOneTemplateRequiredException() {
        super("AtLeastOneTemplateRequired");
    }

    public AtLeastOneTemplateRequiredException(String message) {
        super(message);
    }

    public AtLeastOneTemplateRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
