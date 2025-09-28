package app.owlcms.nui.preparation;

import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.server.InputStreamFactory;
import java.util.function.Supplier;
import java.util.Optional;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.components.elements.LazyDownloadButton;
import app.owlcms.i18n.Translator;

/**
 * Dialog subclass that provides named areas for the download control and the
 * single processing/error paragraph so callers do not need to navigate the
 * component tree.
 */
public class DocumentDownloadDialog extends Dialog {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_WIDTH = "50em";

    private Paragraph processingParagraph;
    private Div downloadDiv;

    public DocumentDownloadDialog() {
        super();
        // Dialog width is a responsibility of this dialog implementation.
        try {
            setWidth(DEFAULT_WIDTH);
        } catch (Throwable ignore) {
        }
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
            // Neutral processing message: black text
            processingParagraph.getStyle().set("color", "var(--lumo-body-text-color)");
            processingParagraph.getStyle().set("font-weight", "bold");
            processingParagraph.getStyle().set("text-align", "center");
            processingParagraph.getStyle().set("font-size", "large");
            setDownloadEnabled(false);
        });
    }

    /**
     * Show an error message (red) and disable the download control area.
     */
    public void showError(String text) {
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

    /**
     * Create a dynamic download control (single-file or .zip) and add it to the
     * dialog footer. The dialog will keep a reference to the control so it can
     * enable/disable the inner button when reporting processing/errors.
     * Returns the Div that contains the download control.
     */
    public Div createDynamicDownloadArea(Supplier<String> baseFileNameSupplier, String label, InputStreamFactory streamSupplier,
            Supplier<String> extensionSupplier) {
        Div d = DownloadButtonFactory.createDynamicDownloadButton(baseFileNameSupplier, label, streamSupplier, extensionSupplier);
        setDownloadDiv(d);
        try {
            getFooter().add(d);
        } catch (Throwable ignore) {
            // best-effort
        }
        return d;
    }

    public Div createDynamicZipDownloadArea(String id, String label, InputStreamFactory streamSupplier,
            Supplier<Optional<Exception>> uiPreCheck, Icon icon) {
        Div d = DownloadButtonFactory.createDynamicZipDownloadButton(id, label, streamSupplier, uiPreCheck, icon);
        setDownloadDiv(d);
        try {
            getFooter().add(d);
        } catch (Throwable ignore) {
            // best-effort
        }
        return d;
    }

    /**
     * Add the do-it / download control to the dialog. The dialog decides where
     * to place the component (usually in the footer) and manages sizing.
     */
    public void addDoItButton(Component c) {
        try {
            getFooter().add(c);
        } catch (Throwable ignore) {
            // best-effort
        }
    }

    /**
     * When the template selection changes, re-run the UI precheck attached to the
     * download control (if any) and update the dialog messaging accordingly.
     */
    public void runDownloadControlUiPrecheck() {
        if (downloadDiv == null) return;
        try {
            downloadDiv.getChildren().findFirst().ifPresent(c -> {
                if (c instanceof LazyDownloadButton) {
                    LazyDownloadButton ldb = (LazyDownloadButton) c;
                    java.util.function.Supplier<java.util.Optional<java.lang.Exception>> pre = ldb.getUiPreCheck();
                    if (pre != null) {
                        try {
                            java.util.Optional<java.lang.Exception> res = pre.get();
                            if (res != null && res.isPresent()) {
                                java.util.List<Exception> errors = new java.util.ArrayList<>();
                                errors.add(res.get());
                                reportPrecheckErrors(errors);
                            } else {
                                clearProcessing();
                                setDownloadEnabled(true);
                            }
                        } catch (Throwable t) {
                            java.util.List<Exception> errors = new java.util.ArrayList<>();
                            errors.add(new Exception(t));
                            reportPrecheckErrors(errors);
                        }
                    } else {
                        // nothing to run
                        clearProcessing();
                    }
                }
            });
        } catch (Throwable ignore) {
        }
    }
}
