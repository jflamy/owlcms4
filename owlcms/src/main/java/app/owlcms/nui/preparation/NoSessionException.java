package app.owlcms.nui.preparation;

/**
 * Exception thrown when a document generation operation requires a session
 * to be selected but none is available.
 */
public class NoSessionException extends ScopeException {
    private static final long serialVersionUID = 1L;

    public NoSessionException() {
        super("NoSession");
    }

    public NoSessionException(String message) {
        super(message);
    }

    public NoSessionException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getTranslationKey() {
        return "Documents.NoSession";
    }
}
