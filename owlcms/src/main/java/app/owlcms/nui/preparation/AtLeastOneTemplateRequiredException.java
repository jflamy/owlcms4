package app.owlcms.nui.preparation;

/**
 * Thrown when a document set requires at least one template to be present but none are available.
 * This exception extends TemplateException to indicate template-related failures.
 */
public class AtLeastOneTemplateRequiredException extends TemplateException {
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

    @Override
    public String getTranslationKey() {
        return "Documents.AtLeastOneTemplateRquired";
    }
}
