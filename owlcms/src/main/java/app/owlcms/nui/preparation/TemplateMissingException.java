package app.owlcms.nui.preparation;

/**
 * Exception used to signal a missing template during pre-checks so the UI dialog
 * can display the error consistently like other precheck failures.
 * 
 * @deprecated Use {@link NoTemplateException} instead. This class is kept for backward compatibility
 * and now extends TemplateException to align with the new exception hierarchy.
 */
@Deprecated
public class TemplateMissingException extends TemplateException {
    private static final long serialVersionUID = 1L;

    public TemplateMissingException(String message) {
        super(message);
    }

    public TemplateMissingException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getTranslationKey() {
        return "Documents.NoTemplate";
    }
}
