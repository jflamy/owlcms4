package app.owlcms.nui.preparation;

import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import app.owlcms.i18n.Translator;

/**
 * Dialog subclass that provides named areas for the download control and the
 * single processing/error paragraph so callers do not need to navigate the
 * component tree.
 */
public class DocumentDownloadDialog extends Dialog {
    private static final long serialVersionUID = 1L;

    private Paragraph processingParagraph;
    private Div downloadDiv;

    public DocumentDownloadDialog() {
        super();
    }

    public void setDownloadDiv(Div div) {
        this.downloadDiv = div;
    }

    /**
     * Report precheck errors to the dialog. This will show a single paragraph
     * (id="documents-processing") with an appropriate translated message and
     * disable the download button area. If errors is empty the paragraph is
     * removed and the download area is enabled.
     */
    public void reportPrecheckErrors(List<Exception> errors) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            // best-effort: try to use dialog's UI
            if (getUI().isPresent()) ui = getUI().get();
        }
        final UI theUi = ui;
        if (theUi == null) {
            return;
        }
        theUi.access(() -> {
            if (errors == null || errors.isEmpty()) {
                clearProcessingParagraph();
                setDownloadEnabled(true);
                return;
            }

            String text = null;
            // pick the first meaningful error to display
            Exception e = errors.get(0);
            if (e instanceof AtLeastOneTemplateRequiredException) {
                // Set-level error: require at least one template selected for the set
                text = Translator.translate("Documents.AtLeastOneTemplateRquired");
            } else if ("NoTemplate".equals(e.getMessage()) || "NoTemplates".equals(e.getMessage())) {
                // Per-element missing-template legacy messages
                text = Translator.translate("Documents.NoTemplate");
            } else if (e.getMessage() != null && e.getMessage().equals("NoSession")) {
                text = Translator.translate("Documents.NoSession");
            } else if (e.getMessage() != null && e.getMessage().equals("TooManyAthletes")) {
                text = Translator.translate("Documents.TooManyAthletes");
            } else if (e.getMessage() != null) {
                text = e.getMessage();
            } else {
                text = Translator.translate("Download.failed");
            }

            if (processingParagraph == null) {
                processingParagraph = new Paragraph(text);
                processingParagraph.setId("documents-processing");
                processingParagraph.getStyle().set("color", "var(--lumo-error-text-color)");
                processingParagraph.getStyle().set("font-weight", "bold");
                processingParagraph.getStyle().set("text-align", "center");
                processingParagraph.getStyle().set("font-size", "large");
                add(processingParagraph);
            } else {
                processingParagraph.setText(text);
                processingParagraph.getStyle().set("color", "var(--lumo-error-text-color)");
                processingParagraph.getStyle().set("font-weight", "bold");
                processingParagraph.getStyle().set("text-align", "center");
                processingParagraph.getStyle().set("font-size", "large");
            }

            setDownloadEnabled(false);
        });
    }

    /**
     * Show a processing or error message in the dialog (replaces any existing one)
     * and disable the download control area.
     */
    public void showProcessing(String text) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            if (getUI().isPresent()) ui = getUI().get();
        }
        final UI theUi = ui;
        if (theUi == null) return;
        theUi.access(() -> {
            if (processingParagraph == null) {
                processingParagraph = new Paragraph(text);
                processingParagraph.setId("documents-processing");
                add(processingParagraph);
            } else {
                processingParagraph.setText(text);
            }
            processingParagraph.getStyle().set("color", "var(--lumo-error-text-color)");
            processingParagraph.getStyle().set("font-weight", "bold");
            processingParagraph.getStyle().set("text-align", "center");
            processingParagraph.getStyle().set("font-size", "large");
            setDownloadEnabled(false);
        });
    }

    /**
     * Clear any processing/error paragraph and re-enable the download control.
     */
    public void clearProcessing() {
        UI ui = UI.getCurrent();
        if (ui == null) {
            if (getUI().isPresent()) ui = getUI().get();
        }
        final UI theUi = ui;
        if (theUi == null) return;
        theUi.access(() -> {
            clearProcessingParagraph();
            setDownloadEnabled(true);
        });
    }

    private void clearProcessingParagraph() {
        if (processingParagraph != null) {
            try {
                processingParagraph.getParent().ifPresent(p -> p.getElement().removeChild(processingParagraph.getElement()));
            } catch (Throwable ignore) {
            }
            processingParagraph = null;
        }
    }

    private void setDownloadEnabled(boolean enabled) {
        if (downloadDiv == null) return;
        try {
            downloadDiv.getChildren().findFirst().ifPresent(c -> {
                if (c instanceof Button) {
                    ((Button) c).setEnabled(enabled);
                } else {
                    try {
                        // attempt to treat the child as a Vaadin button by reflection fallback
                        // (original code sometimes wrapped a Button in a Div)
                        // if not a Button, do nothing
                    } catch (Throwable ignore) {
                    }
                }
            });
        } catch (Throwable ignore) {
        }
    }
}
