package app.owlcms.nui.preparation;

import app.owlcms.i18n.Translator;

/**
 * Base exception type for all document generation precheck failures.
 * Subclasses should override getTranslationKey() to provide appropriate
 * i18n message keys for user-facing error messages.
 */
public abstract class DocumentPrecheckException extends Exception {
    private static final long serialVersionUID = 1L;

    public DocumentPrecheckException(String message) {
        super(message);
    }

    public DocumentPrecheckException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Return the i18n translation key that should be used to display
     * this exception to the user. Subclasses should override this method
     * to provide specific translation keys based on the exception type.
     * 
     * @return the translation key (e.g., "Documents.NoSession")
     */
    public abstract String getTranslationKey();

    /**
     * Return the user-facing message to display for this precheck failure.
     * Subclasses may override when the message needs composed translated parts.
     * 
     * @return the translated display message
     */
    public String getDisplayMessage() {
        return Translator.translate(getTranslationKey());
    }

    /**
     * Return whether this precheck should block document generation.
     */
    public boolean isBlocking() {
        return true;
    }
}
