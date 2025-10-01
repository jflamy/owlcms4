package app.owlcms.nui.preparation;

/**
 * Exception type representing scope-related precheck failures such as
 * missing session or too many athletes for the selected scope.
 */
public abstract class ScopeException extends DocumentPrecheckException {
    private static final long serialVersionUID = 1L;

    public ScopeException(String message) {
        super(message);
    }

    public ScopeException(String message, Throwable cause) {
        super(message, cause);
    }
}
