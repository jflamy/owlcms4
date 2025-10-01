package app.owlcms.nui.preparation;

/**
 * Exception thrown when a document generation operation requires athletes
 * but none are available in the selected scope.
 */
public class NoAthletesException extends ScopeException {
    private static final long serialVersionUID = 1L;

    public NoAthletesException() {
        super("NoAthletes");
    }

    public NoAthletesException(String message) {
        super(message);
    }

    public NoAthletesException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getTranslationKey() {
        return "Documents.NoSession";
    }
}
