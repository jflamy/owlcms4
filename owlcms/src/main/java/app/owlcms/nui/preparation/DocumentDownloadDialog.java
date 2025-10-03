package app.owlcms.nui.preparation;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.InputStreamFactory;
import java.util.function.Supplier;
import java.util.Optional;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.components.elements.LazyDownloadButton;

import app.owlcms.i18n.Translator;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.function.BiFunction;
import app.owlcms.data.group.Group;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.utils.Resource;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.data.config.Config;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.html.H4;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.utils.LoggerUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;

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
    private List<KitElement> kitElements;
    private Supplier<List<Group>> selectedSessionsSupplier;
    private Supplier<List<Athlete>> computeAthletesSupplier;

    public DocumentDownloadDialog(java.util.List<KitElement> kitElements) {
        this();
        this.kitElements = kitElements;
        // If the dialog was constructed with kit elements, attempt to add
        // an appropriate template selection automatically so callers do not
        // always need to call singleTemplateSelection themselves.
        logger.debug("DocumentDownloadDialog kitElements {}", kitElements);
        if (kitElements != null && !kitElements.isEmpty()) {
            // For multi-element kits (document sets), show info message about template handling
            if (kitElements.size() > 1) {
                Paragraph infoPara = new Paragraph(Translator.translate("Documents.IgnoreNoTemplate"));
                infoPara.getStyle().set("color", "var(--lumo-primary-text-color)");
                infoPara.getStyle().set("font-style", "italic");
                infoPara.getStyle().set("margin-bottom", "1em");
                add(infoPara);
            }
            
            int index = 0;
            for (KitElement ke : kitElements) {
                try {
                    PreCompetitionTemplate p = ke.templateEnum();
                    logger.debug("Adding template selection for {} with folder '{}'", p, p.folder);
                    FormLayout templateSection = singleTemplateSelection(p);
                    // Add vertical spacing before items after the first one
                    if (index > 0) {
                        templateSection.getStyle().set("margin-top", "1em");
                    }
                    add(templateSection);
                } catch (Throwable t) {
                    // Log detailed error about which template failed
                    PreCompetitionTemplate p = ke.templateEnum();
                    String templateInfo = p != null ? (p.name() + " (folder: '" + p.folder + "')") : "unknown template";
                    logger.error("Failed to add template selection for {}: {}", templateInfo, t.getMessage());
                    LoggerUtils.logError(logger, t);
                    // Show error paragraph in dialog instead of silently failing
                    String errorMsg = "Template location not found for " + templateInfo;
                    Paragraph errorPara = new Paragraph(errorMsg);
                    errorPara.getStyle().set("color", "var(--lumo-error-text-color)");
                    errorPara.getStyle().set("font-weight", "bold");
                    if (index > 0) {
                        errorPara.getStyle().set("margin-top", "1em");
                    }
                    add(errorPara);
                }
                index++;
            }
        }
    }

    /**
     * Construct a DocumentDownloadDialog with a kit and a factory that will produce
     * the "do it" / download control. The factory is invoked with the dialog
     * instance so callers can create controls that reference the dialog (for
     * example to call dialog.showError/clearProcessing). The factory should NOT
     * perform heavy preparation work at construction time; defer heavy work to
     * the element supplier used by the control.
     */
    public DocumentDownloadDialog(List<KitElement> kitElements,
        Function<DocumentDownloadDialog, Component> doItFactory) {
        this(kitElements);
        if (doItFactory == null) return;
        try {
            Component c = null;
            try {
                c = doItFactory.apply(this);
            } catch (Throwable t) {
                LoggerUtils.logError(this.logger, t);
            }
            if (c != null) addDoItButton(c);
        } catch (Throwable ignore) {
            // best-effort
        }
    }

    /**
     * Constructor accepting a factory that receives both the dialog and the kit
     * list. This allows callers to build a do-it control that uses the precomputed
     * kit list without re-running preparation.
     */
    public DocumentDownloadDialog(List<KitElement> kitElements,
        BiFunction<DocumentDownloadDialog, List<KitElement>, Component> doItFactory) {
        this(kitElements);
        if (doItFactory == null) return;
        try {
            Component c = null;
            try {
                c = doItFactory.apply(this, kitElements);
            } catch (Throwable t) {
                LoggerUtils.logError(this.logger, t);
            }
            if (c != null) addDoItButton(c);
        } catch (Throwable ignore) {
            // best-effort
        }
    }

    /**
     * Constructor accepting kit elements, suppliers for groups and athletes, and a factory for the download control.
     * This constructor runs prechecks automatically when the dialog is created.
     */
    public DocumentDownloadDialog(List<KitElement> kitElements,
        Supplier<List<Group>> selectedSessionsSupplier,
        Supplier<List<Athlete>> computeAthletesSupplier,
        BiFunction<DocumentDownloadDialog, List<KitElement>, Component> doItFactory) {
        this(kitElements);
        this.selectedSessionsSupplier = selectedSessionsSupplier;
        this.computeAthletesSupplier = computeAthletesSupplier;
        if (doItFactory == null) return;
        try {
            Component c = null;
            try {
                c = doItFactory.apply(this, kitElements);
            } catch (Throwable t) {
                LoggerUtils.logError(this.logger, t);
            }
            if (c != null) addDoItButton(c);
        } catch (Throwable ignore) {
            // best-effort
        }
        // Run prechecks after creating the download control
        runPrechecks();
    }

    public List<KitElement> getKitElements() {
        return kitElements;
    }

    /**
     * Run all prechecks for the current dialog state and report any errors.
     * This is the single entry point for precheck validation:
     * 1. Template selection precheck (at least one template must be selected)
     * 2. Scope precheck (session/athlete validation from kit element lambdas)
     * 
     * Designed to be called:
     * - When the dialog is initially constructed
     * - When template selection changes
     */
    public void runPrechecks() {
        if (kitElements == null || kitElements.isEmpty()) {
            clearProcessing();
            setDownloadEnabled(true);
            return;
        }

        try {
            // Step 1: Check template selection
            DocumentsPrecheckService svc = new DocumentsPrecheckService();
            Optional<Exception> templateResult = svc.runTemplateSetPrecheck(kitElements);
            if (templateResult.isPresent()) {
                reportPrecheckErrors(List.of(templateResult.get()));
                return;
            }

            // Step 2: Check scope (session/athletes) if suppliers are available
            if (selectedSessionsSupplier != null && computeAthletesSupplier != null) {
                List<Group> sessions = selectedSessionsSupplier.get();
                Group g = (sessions != null && !sessions.isEmpty()) ? sessions.get(0) : null;
                List<Athlete> athletes = computeAthletesSupplier.get();

                // Run scope precheck only for elements with templates selected
                for (KitElement ke : kitElements) {
                    // Check if this element has a template selected
                    Supplier<String> selectedTemplateSupplier = ke.selectedTemplateSupplier();
                    boolean hasTemplate = false;
                    if (selectedTemplateSupplier != null) {
                        String selected = selectedTemplateSupplier.get();
                        hasTemplate = (selected != null && !selected.isBlank());
                    }
                    
                    // Skip scope precheck if no template is selected for this element
                    if (!hasTemplate) {
                        continue;
                    }
                    
                    Optional<Exception> scopeResult = ke.scopePrecheck().apply(athletes, g);
                    if (scopeResult != null && scopeResult.isPresent()) {
                        Exception e = scopeResult.get();
                        // Template exceptions are already handled in step 1
                        if (!(e instanceof TemplateException)) {
                            reportPrecheckErrors(List.of(e));
                            return;
                        }
                    }
                }
            }

            // All prechecks passed
            clearProcessing();
            setDownloadEnabled(true);

        } catch (Throwable t) {
            LoggerUtils.logError(logger, t);
            reportPrecheckErrors(List.of(new Exception(t.getMessage(), t)));
        }
    }

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
     * 
     * Always called on the UI thread (from dialog creation or template selection dropdown),
     * so no UI.access() wrapper is needed.
     * 
     * Error Handling:
     * - DocumentPrecheckException: reported simply using getTranslationKey()
     * - Other exceptions: logged with LoggerUtils.error and message displayed as-is
     */
    public void reportPrecheckErrors(List<Exception> errors) {
        if (errors == null || errors.isEmpty()) {
            clearProcessingParagraph();
            setDownloadEnabled(true);
            return;
        }

        String text = null;
        // pick the first meaningful error to display
        Exception e = errors.get(0);
        
        // Document precheck exceptions provide their own translation keys
        if (e instanceof DocumentPrecheckException) {
            text = Translator.translate(((DocumentPrecheckException) e).getTranslationKey());
        }
        // All other exceptions: log them and use the message as-is
        else {
            LoggerUtils.logError(logger, e);
            if (e.getMessage() != null) {
                text = e.getMessage();
            } else {
                text = Translator.translate("Download.failed");
            }
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
    }

    /**
     * Public wrapper: display precheck errors (keeps old semantics).
     */
    public void displayPrecheckErrors(List<Exception> errors) {
        reportPrecheckErrors(errors);
    }

    /**
     * Show a processing or error message in the dialog (replaces any existing one)
     * and disable the download control area.
     * 
     * Called on the UI thread from button click listeners.
     */
    public void showProcessing(String text) {
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
    }

    /**
     * Public wrapper: display processing message when the doit button is pressed.
     */
    public void displayProcessingMessage(String text) {
        showProcessing(text);
    }

    /**
     * Show an error message (red) and disable the download control area.
     * 
     * Must be called on the UI thread (callers use ui.access() when on background threads).
     */
    public void showError(String text) {
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
    }

    /**
     * Public wrapper: display processing errors (when processing fails).
     */
    public void displayProcessingErrors(List<Exception> errors) {
        if (errors == null || errors.isEmpty()) {
            clearProcessing();
            return;
        }
        // pick first error message to display
        Exception e = errors.get(0);
        String text;
        if (e.getMessage() != null) text = e.getMessage(); else text = Translator.translate("Download.failed");
        showError(text);
    }

    /**
     * Clear any processing/error paragraph and re-enable the download control.
     * 
     * Called on the UI thread from template selection or precheck completion.
     */
    public void clearProcessing() {
        clearProcessingParagraph();
        setDownloadEnabled(true);
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
     * Create and wire a do-it / download control from caller-supplied factories.
     * This centralizes the wiring of LazyDownloadButton callbacks and initial
     * precheck behaviour so callers only need to provide the InputStreamFactory
     * and UI precheck supplier.
     *
     * @param baseFileNameSupplier base name for the generated file (or zip)
     * @param kit                  kit elements (used to determine multi/single)
     * @param streamSupplier       supplier that returns an InputStream when invoked
     * @param uiPreCheck           supplier invoked on UI thread to validate template/session
     * @param extensionSupplier    extension supplier for single-file downloads (ignored for zip)
     * @param icon                 icon to use for zip download button (ignored for single-file)
     * @return the Div containing the download control
     */
    public Component createDoItButtonForKits(Supplier<String> baseFileNameSupplier, java.util.List<KitElement> kit,
        InputStreamFactory streamSupplier,
        Supplier<Optional<Exception>> uiPreCheck,
        Supplier<String> zipBaseFileNameSupplier,
        Supplier<String> extensionSupplier,
        Icon icon) {
        if (kit == null || kit.isEmpty()) {
            Button b = new Button(Translator.translate("Download"), VaadinIcon.DOWNLOAD_ALT.create());
            b.setEnabled(false);
            return new Div(b);
        }

        final boolean multi = kit.size() > 1;
        // Determine processing message: use LongProcessing for multi-file downloads, otherwise use the kit element's message
        final String processingKey = multi ? "LongProcessing" : (kit.isEmpty() ? "Processing" : kit.get(0).processingMessageSupplier().get());
        Div d;
    if (multi) {
        // zip download: prefer zipBaseFileNameSupplier when provided
        String zipBase = (zipBaseFileNameSupplier == null) ? baseFileNameSupplier.get() : zipBaseFileNameSupplier.get();
        d = DownloadButtonFactory.createDynamicZipDownloadButton(zipBase, Translator.translate("Download"), streamSupplier,
            uiPreCheck, icon == null ? VaadinIcon.DOWNLOAD_ALT.create() : icon);
    } else {
            // single-file download: use the provided extensionSupplier
            d = DownloadButtonFactory.createDynamicDownloadButton(baseFileNameSupplier, Translator.translate("Download"), streamSupplier,
                    extensionSupplier == null ? () -> ".xlsx" : extensionSupplier);
        }

        setDownloadDiv(d);

        // Wire inner LazyDownloadButton callbacks: close dialog on success, show error on failure
        try {
            d.getChildren().findFirst().ifPresent(c -> {
                if (c instanceof LazyDownloadButton) {
                    LazyDownloadButton ldb = (LazyDownloadButton) c;
                    try {
                        ldb.setUiPreCheck(uiPreCheck);
                    } catch (Throwable ignore) {
                    }
                    // Show processing message when download starts
                    ldb.addClickListener(event -> {
                        this.showProcessing(Translator.translate(processingKey));
                    });
                    ldb.setDoneCallback((tc, transferredBytes) -> {
                        try {
                            // Callback is already on UI lock (TransferProgressListener wraps onComplete with ui.access)
                            this.close();
                        } catch (Throwable cb) {
                            LoggerUtils.logError(this.logger, cb);
                        }
                    });
                    ldb.setErrorCallback((tc, error) -> {
                        try {
                            // Callback is already on UI lock (TransferProgressListener wraps onError with ui.access)
                            String msg = error.getMessage() == null ? Translator.translate("Download.failed") : error.getMessage();
                            this.showError(msg);
                        } catch (Throwable cb) {
                            LoggerUtils.logError(this.logger, cb);
                        }
                    });
                }
            });
        } catch (Throwable ignore) {
        }

        // Skip initial UI precheck during button creation - the dialog's runPrechecks()
        // will handle validation when called from the constructor. The uiPreCheck is
        // still attached to the button for click-time validation.
        // try {
        //     if (uiPreCheck != null) {
        //         Optional<Exception> pre = uiPreCheck.get();
        //         if (pre != null && pre.isPresent()) {
        //             java.util.List<Exception> errors = new java.util.ArrayList<>();
        //             errors.add(pre.get());
        //             this.reportPrecheckErrors(errors);
        //         }
        //     }
        // } catch (Throwable ignore) {
        // }

        return d;
    }

    /**
     * Overload that accepts domain helper functions so callers can pass method
     * references from DocumentsContent without recreating the wiring locally.
     */
    public Component createDoItButtonForKitsWithHelpers(
        Supplier<String> baseFileNameSupplier,
            java.util.List<KitElement> kit,
            Supplier<java.util.List<app.owlcms.data.group.Group>> selectedSessionsSupplier,
            Supplier<java.util.List<app.owlcms.data.athlete.Athlete>> computeAthletesSupplier,
            java.util.function.BiFunction<java.util.List<app.owlcms.data.group.Group>, java.util.List<KitElement>, java.io.InputStream> zipSupplier,
            java.util.function.BiFunction<java.util.List<app.owlcms.data.group.Group>, java.util.List<KitElement>, java.io.InputStream> excelSupplier,
            RunPrecheck runSetPrecheck,
            RunPrecheck filterElementsPrecheck,
            Supplier<String> zipBaseFileNameSupplier,
            Supplier<String> extSupplier,
            Icon icon) {
        if (kit == null || kit.isEmpty()) {
            Button b = new Button(Translator.translate("Download"), VaadinIcon.DOWNLOAD_ALT.create());
            b.setEnabled(false);
            return new Div(b);
        }

        final boolean multi = kit.size() > 1;
        InputStreamFactory streamFactory = () -> {
            java.util.List<app.owlcms.data.group.Group> selected = selectedSessionsSupplier == null ? null : selectedSessionsSupplier.get();
            app.owlcms.data.group.Group g = (selected != null && !selected.isEmpty()) ? selected.get(0) : null;
            java.util.List<app.owlcms.data.athlete.Athlete> athletes = computeAthletesSupplier == null ? null : computeAthletesSupplier.get();
            if (multi) {
                // Run the set-level precheck at stream time and use the filtered kit
                java.util.List<KitElement> effectiveKit = null;
                try {
                    effectiveKit = runSetPrecheck.apply(kit, g, athletes, this);
                } catch (RuntimeException re) {
                    throw re;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                return zipSupplier.apply(selected, effectiveKit);
            } else {
                // For single-element kits, allow per-element filtering
                java.util.List<KitElement> effectiveKit = null;
                try {
                    effectiveKit = filterElementsPrecheck.apply(kit, g, athletes, this);
                } catch (RuntimeException re) {
                    throw re;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                return excelSupplier.apply(selected, effectiveKit);
            }
        };

        // In dialog context, use no-op precheck since runPrechecks() handles all validation.
        // Stream-time prechecks in streamFactory still run as defense-in-depth.
        Supplier<Optional<Exception>> uiPreCheck = () -> Optional.empty();

    return createDoItButtonForKits(baseFileNameSupplier, kit, streamFactory, uiPreCheck, zipBaseFileNameSupplier, extSupplier, icon);
    }

    @FunctionalInterface
    public interface RunPrecheck {
        java.util.List<KitElement> apply(java.util.List<KitElement> elements, app.owlcms.data.group.Group g, java.util.List<app.owlcms.data.athlete.Athlete> athletes, DocumentDownloadDialog dialog) throws Exception;
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
            // The download control may be nested inside wrapper components. Find the
            // LazyDownloadButton recursively so template selection will always be able
            // to re-run the attached UI precheck regardless of wrapping.
            LazyDownloadButton ldb = findLazyDownloadButton(downloadDiv);
            if (ldb != null) {
                Supplier<Optional<Exception>> pre = ldb.getUiPreCheck();
                if (pre != null) {
                    try {
                        Optional<Exception> res = pre.get();
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
        } catch (Throwable ignore) {
        }
    }

    /**
     * Recursively search the component tree rooted at {@code root} for the
     * first LazyDownloadButton instance. Returns null when none found.
     */
    private LazyDownloadButton findLazyDownloadButton(Component root) {
        if (root == null) return null;
        try {
            if (root instanceof LazyDownloadButton) return (LazyDownloadButton) root;
            java.util.Iterator<Component> it = root.getChildren().iterator();
            while (it.hasNext()) {
                Component c = it.next();
                if (c instanceof LazyDownloadButton) return (LazyDownloadButton) c;
                LazyDownloadButton found = findLazyDownloadButton(c);
                if (found != null) return found;
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    // ---- Template selection helpers moved here from TemplateSelectionFormFactory ----
    private static final String DOCUMENTS_IGNORE_NO_TEMPLATE = "Documents.IgnoreNoTemplate";
    private Logger logger = (Logger) LoggerFactory.getLogger(DocumentDownloadDialog.class);

    public FormLayout singleTemplateSelection(PreCompetitionTemplate templateDefinition) {
        FormLayout layout = createLayoutHeader(templateDefinition);
        addTemplateSelection(layout, templateDefinition);
        return layout;
    }

    // Helper to build a set-level template selection form by iterating the
    // provided template elements and adding each selection control. This
    // centralizes the formerly duplicated logic and makes it easy to call
    // singleTemplateSelection-like behavior for each element.
    private FormLayout templateSelectionFormForSet(PreCompetitionTemplate set, PreCompetitionTemplate... elements) {
        FormLayout layout = createSetLayoutHeader(set);
        if (elements != null) {
            for (PreCompetitionTemplate p : elements) {
                addTemplateSelection(layout, p);
            }
        }
        return layout;
    }

    public FormLayout postWeighInTemplateSelectionForm() {
        return templateSelectionFormForSet(PreCompetitionTemplate.POST_WEIGHIN,
                PreCompetitionTemplate.INTRODUCTION,
                PreCompetitionTemplate.EMPTY_PROTOCOL,
                PreCompetitionTemplate.JURY);
    }

    public FormLayout preWeighInTemplateSelectionForm() {
        return templateSelectionFormForSet(PreCompetitionTemplate.PRE_WEIGHIN,
                PreCompetitionTemplate.CARDS,
                PreCompetitionTemplate.WEIGHIN);
    }

    private void addTemplateSelection(FormLayout layout, PreCompetitionTemplate template) {
        List<Resource> prioritizedList = computeResourceList(template.folder, (f) -> matchExtension(template, f));
        ComboBox<Resource> templateSelect = createTemplateSelect(layout, template.name(), prioritizedList, template.templateFileNameSupplier.get());

        templateSelect.addValueChangeListener(e -> {
                Resource value = e.getValue();
                String newTemplateName = value != null ? value.getFileName() : null;
                // logger removed
            try {
                if (newTemplateName != null) {
                    Resource res = searchMatch(prioritizedList, newTemplateName);
                    if (res == null) {
                        throw new FileNotFoundException("template not found " + newTemplateName);
                    }
                }

                // lambda uses getCurrent().
                template.templateFileNameSetter.accept(newTemplateName);
                Competition current = Competition.getCurrent();
                CompetitionRepository.save(current);
                current = Competition.getCurrent();

                // clear previous messages
                clearProcessing();

                // If a template was selected and we have kitElements, try resolving
                // and updating the matching kit elements first. If any resource is
                // missing, report and treat as precheck failure.
                boolean resourceProblem = false;
                if (newTemplateName != null && kitElements != null) {
                    String resourceFolder = template.folder + "/";
                    String newFullName = resourceFolder + newTemplateName;
                    for (int i = 0; i < kitElements.size(); i++) {
                        KitElement ke = kitElements.get(i);
                        if (ke == null) continue;
                        String id = ke.id();
                        if (id == null) continue;
                        String normId = id.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
                        String normEnum = template.name().replaceAll("[^A-Za-z0-9]", "").toLowerCase();
                        if (normId.equals(normEnum)) {
                                try {
                                    java.nio.file.Path ispPath = null;
                                    String ext = "";
                                    try {
                                        ispPath = ResourceWalker.getFileOrResourcePath(newFullName);
                                        if (ispPath != null) {
                                            ext = FilenameUtils.getExtension(ispPath.getFileName().toString());
                                        }
                                        // logger removed
                                    } catch (java.io.FileNotFoundException fnf) {
                                        // Template not found: report and mark problem
                                        java.util.List<Exception> errors = new java.util.ArrayList<>();
                                        errors.add(new NoTemplateException("NoTemplate", fnf));
                                        reportPrecheckErrors(errors);
                                        resourceProblem = true;
                                        break;
                                    }
                                    Supplier<List<Resource>> availableTemplatesSupplier = () -> computeResourceList(template.folder, (f) -> matchExtension(template, f));
                                    Supplier<String> selectedTemplateSupplier = () -> template.templateFileNameSupplier.get();
                                    // Update the existing KitElement in-place
                                    ke.setName(newFullName);
                                    ke.setExtension(ext);
                                    ke.setIsp(ispPath);
                                    ke.setAvailableTemplatesSupplier(availableTemplatesSupplier);
                                    ke.setSelectedTemplateSupplier(selectedTemplateSupplier);
                                } catch (Throwable ignore) {
                                    LoggerUtils.logError(this.logger, ignore);
                                }
                        }
                    }
                }

                if (!resourceProblem) {
                    // Resource is OK, run unified prechecks
                    runPrechecks();
                }
                // If resourceProblem is true, download is already disabled by reportPrecheckErrors above
            } catch (Throwable ex) {
                LoggerUtils.logError(this.logger, ex);
            }
        });
    }

    public boolean matchExtension(PreCompetitionTemplate template, String f) {
        if (template.extension.equals(".xlsx")) {
            return (f.endsWith(".xlsx") || f.endsWith(".xlsm"));
        } else {
            return f.endsWith(template.extension);
        }
    }

    private List<Resource> computeResourceList(String resourceDirectoryLocation, Predicate<String> nameFilter) {
        List<Resource> resourceList = new ResourceWalker().getResourceList(
                resourceDirectoryLocation,
                ResourceWalker::relativeName,
                nameFilter,
                OwlcmsSession.getLocale(),
                Config.getCurrent().isLocalTemplatesOnly());
        List<Resource> prioritizedList = xlsxPriority(resourceList);
        return prioritizedList;
    }

    private FormLayout createLayout() {
        FormLayout layout = new FormLayout();
        layout.setResponsiveSteps(new ResponsiveStep("0", 1, LabelsPosition.TOP),
                new ResponsiveStep("800px", 2, LabelsPosition.TOP));
        return layout;
    }

    private FormLayout createLayoutHeader(PreCompetitionTemplate templateDefinition) {
        FormLayout layout = createLayout();
        Component title = createTitle(templateDefinition.name());
        layout.add(title);
        layout.setColspan(title, 2);
        return layout;
    }

    private FormLayout createSetLayoutHeader(PreCompetitionTemplate templateDefinition) {
        FormLayout layout = createLayout();
        Component title = createTitle(templateDefinition.name());
        layout.add(title);
        layout.setColspan(title, 2);
        Div div = new Div(Translator.translate(DOCUMENTS_IGNORE_NO_TEMPLATE));
        layout.add(div);
        layout.setColspan(div, 2);
        return layout;
    }

    private ComboBox<Resource> createTemplateSelect(FormLayout layout, String labelKey, List<Resource> prioritizedList, String string) {
        ComboBox<Resource> templateSelect = new ComboBox<>();
        templateSelect.setPlaceholder(Translator.translate("AvailableTemplates"));
        templateSelect.setHelperText(Translator.translate("SelectTemplate"));
        templateSelect.setItems(prioritizedList);
        templateSelect.setValue(null);
        templateSelect.setWidth("15em");
        templateSelect.getStyle().set("margin-right", "0.8em");
        templateSelect.setClearButtonVisible(true);
        templateSelect.setWidthFull();
        layout.addFormItem(templateSelect, Translator.translate(labelKey));
        templateSelect.setValue(searchMatch(prioritizedList, string));
        return templateSelect;
    }

    /**
     * Public helper: create a FormLayout with a labeled template ComboBox.
     * Returns the FormLayout so callers can add it to the dialog. The valueListener
     * is invoked when selection changes.
     */
    public FormLayout createLabeledTemplateSelection(String labelKey, List<Resource> prioritizedList, String selectedFileName,
            java.util.function.Consumer<com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent<ComboBox<Resource>, Resource>> valueListener) {
        FormLayout layout = createLayout();
        ComboBox<Resource> templateSelect = createTemplateSelect(layout, labelKey, prioritizedList, selectedFileName);
        if (valueListener != null) {
            templateSelect.addValueChangeListener(e -> valueListener.accept(e));
        }
        Component title = createTitle(labelKey);
        // Use the provided label as the title (like singleTemplateSelection does)
        layout.add(title);
        layout.setColspan(title, 2);
        // Ensure the template selection is added as a form item
        // (createTemplateSelect already added it to the layout)
        return layout;
    }

    private Component createTitle(String string) {
        H4 title = new H4(Translator.translate(string));
        title.getStyle().set("margin-top", "0");
        title.getStyle().set("margin-bottom", "0");
        return title;
    }

    private Resource searchMatch(List<Resource> resourceList, String curTemplateName) {
        Resource found = null;
        for (Resource curResource : resourceList) {
            String fileName = curResource.getFileName();
            this.logger.trace("comparing {} {}", fileName, curTemplateName);
            if (fileName.equals(curTemplateName)) {
                found = curResource;
                break;
            }
        }
        return found;
    }

    /**
     * Create a horizontal layout containing a ComboBox for a given resource list.
     * The returned layout can be added to the dialog by callers. When the user
     * changes the selected Resource the provided valueListener is invoked.
     */
    public com.vaadin.flow.component.orderedlayout.HorizontalLayout createTemplateSelectionArea(
            List<Resource> prioritizedList,
            String selectedFileName,
            java.util.function.Consumer<com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent<ComboBox<Resource>, Resource>> valueListener) {
        com.vaadin.flow.component.orderedlayout.HorizontalLayout templateSelection = new com.vaadin.flow.component.orderedlayout.HorizontalLayout();
        templateSelection.setSpacing(false);

        ComboBox<Resource> templateSelect = new ComboBox<>();
        templateSelect.setPlaceholder(Translator.translate("AvailableTemplates"));
        templateSelect.setHelperText(Translator.translate("SelectTemplate"));
        templateSelect.setItems(prioritizedList);
        templateSelect.setValue(searchMatch(prioritizedList, selectedFileName));
        templateSelect.setWidth("15em");
        templateSelect.getStyle().set("margin-right", "0.8em");

        if (valueListener != null) {
            templateSelect.addValueChangeListener(e -> valueListener.accept(e));
        }

        templateSelection.add(templateSelect);
        return templateSelection;
    }

    private List<Resource> xlsxPriority(List<Resource> resourceList) {
        resourceList.sort(Comparator.comparing(Resource::getFileName).reversed());

        ArrayList<Resource> proritizedList = new ArrayList<>();
        String prevName = "";
        for (Resource r : resourceList) {
            String curName = r.getFileName();
            if (curName.endsWith(".xlsx") || curName.endsWith(".xlsm") || (curName.endsWith(".xls") && !prevName.contentEquals(curName + "x"))) {
                proritizedList.add(r);
            }
            prevName = curName;
        }
        proritizedList.sort(Comparator.comparing(Resource::getFileName));
        return proritizedList;
    }

    // ---- end moved helpers ----
}
