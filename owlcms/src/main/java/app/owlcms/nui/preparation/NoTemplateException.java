package app.owlcms.nui.preparation;

/**
 * Exception thrown when a single document element requires a template
 * to be selected but none is available or selected.
 */
public class NoTemplateException extends TemplateException {
    private static final long serialVersionUID = 1L;

    public NoTemplateException() {
        super("NoTemplate");
    }

    public NoTemplateException(String message) {
        super(message);
    }

    public NoTemplateException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getTranslationKey() {
        return "Documents.NoTemplate";
    }
}
