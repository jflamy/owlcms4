package app.owlcms.nui.preparation;

/**
 * Exception thrown when a document generation operation has too many athletes
 * in the selected scope (e.g., cards can only be generated for one athlete at a time).
 */
public class TooManyAthletesException extends ScopeException {
    private static final long serialVersionUID = 1L;

    public TooManyAthletesException() {
        super("TooManyAthletes");
    }

    public TooManyAthletesException(String message) {
        super(message);
    }

    public TooManyAthletesException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getTranslationKey() {
        return "Documents.TooManyAthletes";
    }
}
