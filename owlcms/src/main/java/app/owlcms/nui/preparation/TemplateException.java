package app.owlcms.nui.preparation;

/**
 * Base exception type for template-related failures during document generation.
 * Template exceptions indicate that a required template is missing or unavailable.
 */
public abstract class TemplateException extends DocumentPrecheckException {
    private static final long serialVersionUID = 1L;

    public TemplateException(String message) {
        super(message);
    }

    public TemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}
