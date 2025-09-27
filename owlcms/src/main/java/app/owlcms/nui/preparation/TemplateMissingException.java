package app.owlcms.nui.preparation;

/**
 * Exception used to signal a missing template during pre-checks so the UI dialog
 * can display the error consistently like other precheck failures.
 */
public class TemplateMissingException extends Exception {
    private static final long serialVersionUID = 1L;

    public TemplateMissingException(String message) {
        super(message);
    }

    public TemplateMissingException(String message, Throwable cause) {
        super(message, cause);
    }
}
