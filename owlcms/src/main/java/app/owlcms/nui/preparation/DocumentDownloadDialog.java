package app.owlcms.nui.preparation;

import java.util.List;

import com.vaadin.flow.component.UI;
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
    private java.util.List<KitElement> kitElements;

    public DocumentDownloadDialog(java.util.List<KitElement> kitElements) {
        this();
        this.kitElements = kitElements;
        try {
            // If the dialog was constructed with kit elements, attempt to add
            // an appropriate template selection automatically so callers do not
            // always need to call singleTemplateSelection themselves.
            if (kitElements != null && !kitElements.isEmpty()) {
                // For each kit element, attempt to map its id to a
                // PreCompetitionTemplates enum value and add a template
                // selection for it. This centralizes selection UI creation
                // so callers do not need to call dialog.add(...).
                for (KitElement ke : kitElements) {
                    try {
                        String id = ke.id();
                        if (id == null || id.isBlank()) continue;
                        String normId = id.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
                        for (PreCompetitionTemplates p : PreCompetitionTemplates.values()) {
                            String normEnum = p.name().replaceAll("[^A-Za-z0-9]", "").toLowerCase();
                            if (normEnum.equals(normId)) {
                                try {
                                    add(singleTemplateSelection(p));
                                } catch (Throwable ignore) {
                                }
                                break;
                            }
                        }
                    } catch (Throwable ignore) {
                        // best-effort per element
                    }
                }
            }
        } catch (Throwable ignore) {
            // best-effort: do not fail construction
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
    public DocumentDownloadDialog(java.util.List<KitElement> kitElements,
            java.util.function.Function<DocumentDownloadDialog, Component> doItFactory) {
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
    public DocumentDownloadDialog(java.util.List<KitElement> kitElements,
            java.util.function.BiFunction<DocumentDownloadDialog, java.util.List<KitElement>, Component> doItFactory) {
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

    public java.util.List<KitElement> getKitElements() {
        return kitElements;
    }

    /**
     * Run the stored kit-elements precheck. If there are multiple kit elements, iterate
     * over each element's preCheck and report the first error found. Returns Optional.empty()
     * when OK or Optional.of(Exception) when failing.
     */
    public java.util.Optional<Exception> runStoredKitElementsPrecheck(app.owlcms.data.group.Group g, java.util.List<app.owlcms.data.athlete.Athlete> athletes) {
        if (kitElements == null || kitElements.isEmpty()) return java.util.Optional.empty();
        try {
            if (kitElements.size() == 1) {
                var pre = kitElements.get(0).preCheck();
                return pre.apply(athletes, g);
            } else {
                for (KitElement ke : kitElements) {
                    var res = ke.preCheck().apply(athletes, g);
                    if (res != null && res.isPresent()) {
                        return res;
                    }
                }
                return java.util.Optional.empty();
            }
        } catch (Throwable t) {
            return java.util.Optional.of(new Exception(t));
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
     * Public wrapper: display precheck errors (keeps old semantics).
     */
    public void displayPrecheckErrors(List<Exception> errors) {
        reportPrecheckErrors(errors);
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
     * Public wrapper: display processing message when the doit button is pressed.
     */
    public void displayProcessingMessage(String text) {
        showProcessing(text);
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
            Supplier<String> extensionSupplier,
            Icon icon) {
        if (kit == null || kit.isEmpty()) {
            Button b = new Button(Translator.translate("Download"), VaadinIcon.DOWNLOAD_ALT.create());
            b.setEnabled(false);
            return new Div(b);
        }

        final boolean multi = kit.size() > 1;
        Div d;
        if (multi) {
            // zip download
            d = DownloadButtonFactory.createDynamicZipDownloadButton(baseFileNameSupplier.get(), Translator.translate("Download"), streamSupplier,
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
                    ldb.setDoneCallback((tc, transferredBytes) -> {
                        try {
                            // The writer is expected to set flags or otherwise indicate success via its own callbacks.
                            UI ui = tc != null ? tc.getUI() : UI.getCurrent();
                            if (ui != null) {
                                ui.access(() -> this.close());
                            }
                        } catch (Throwable cb) {
                            LoggerUtils.logError(this.logger, cb);
                        }
                    });
                    ldb.setErrorCallback((tc, error) -> {
                        try {
                            UI ui = tc != null ? tc.getUI() : UI.getCurrent();
                            if (ui != null) {
                                String msg = error.getMessage() == null ? Translator.translate("Download.failed") : error.getMessage();
                                ui.access(() -> this.showError(msg));
                            }
                        } catch (Throwable cb) {
                            LoggerUtils.logError(this.logger, cb);
                        }
                    });
                }
            });
        } catch (Throwable ignore) {
        }

        // Run initial UI precheck so dialog shows any missing-template / session messages immediately
        try {
            if (uiPreCheck != null) {
                Optional<Exception> pre = uiPreCheck.get();
                if (pre != null && pre.isPresent()) {
                    java.util.List<Exception> errors = new java.util.ArrayList<>();
                    errors.add(pre.get());
                    this.reportPrecheckErrors(errors);
                }
            }
        } catch (Throwable ignore) {
        }

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
            if (multi) {
                return zipSupplier.apply(selected, kit);
            } else {
                return excelSupplier.apply(selected, kit);
            }
        };

        Supplier<Optional<Exception>> uiPreCheck = () -> {
            try {
                java.util.List<app.owlcms.data.group.Group> ss = selectedSessionsSupplier == null ? null : selectedSessionsSupplier.get();
                app.owlcms.data.group.Group g = (ss != null && ss.size() > 0) ? ss.get(0) : null;
                java.util.List<app.owlcms.data.athlete.Athlete> athletes = computeAthletesSupplier == null ? null : computeAthletesSupplier.get();
                try {
                    if (multi) {
                        runSetPrecheck.apply(kit, g, athletes, this);
                    } else {
                        filterElementsPrecheck.apply(kit, g, athletes, this);
                    }
                    return Optional.empty();
                } catch (Exception preEx) {
                    return Optional.of(preEx);
                }
            } catch (Throwable t) {
                return Optional.of(new Exception(t));
            }
        };

        return createDoItButtonForKits(baseFileNameSupplier, kit, streamFactory, uiPreCheck, extSupplier, icon);
    }

    @FunctionalInterface
    public interface RunPrecheck {
        java.util.List<KitElement> apply(java.util.List<KitElement> elements, app.owlcms.data.group.Group g, java.util.List<app.owlcms.data.athlete.Athlete> athletes, DocumentDownloadDialog dialog);
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

    public FormLayout singleTemplateSelection(PreCompetitionTemplates templateDefinition) {
        FormLayout layout = createLayoutHeader(templateDefinition);
        addTemplateSelection(layout, templateDefinition);
        return layout;
    }

    // FIXME: should be done by iterating singleTemplateSelectionover elements
    public FormLayout postWeighInTemplateSelectionForm() {
        FormLayout layout = createSetLayoutHeader(PreCompetitionTemplates.POST_WEIGHIN);
        addTemplateSelection(layout, PreCompetitionTemplates.INTRODUCTION);
        addTemplateSelection(layout, PreCompetitionTemplates.EMPTY_PROTOCOL);
        addTemplateSelection(layout, PreCompetitionTemplates.JURY);
        return layout;
    }

    // FIXME: should be done by iterating singleTemplateSelection over elements
    public FormLayout preWeighInTemplateSelectionForm() {
        FormLayout layout = createSetLayoutHeader(PreCompetitionTemplates.PRE_WEIGHIN);
        addTemplateSelection(layout, PreCompetitionTemplates.CARDS);
        addTemplateSelection(layout, PreCompetitionTemplates.WEIGHIN);
        return layout;
    }

    private void addTemplateSelection(FormLayout layout, PreCompetitionTemplates template) {
        List<Resource> prioritizedList = computeResourceList(template.folder, (f) -> matchExtension(template, f));
        ComboBox<Resource> templateSelect = createTemplateSelect(layout, template.name(), prioritizedList, template.templateFileNameSupplier.get());

        templateSelect.addValueChangeListener(e -> {
                Resource value = e.getValue();
                String newTemplateName = value != null ? value.getFileName() : null;
                logger.warn("DocumentDownloadDialog.templateSelection: selected template='{}' for enum={}", newTemplateName, template.name());
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

                // find the inner LazyDownloadButton and its ui precheck supplier
                LazyDownloadButton ldb = findLazyDownloadButton(downloadDiv);
                Supplier<Optional<Exception>> pre = ldb == null ? null : ldb.getUiPreCheck();
                boolean ok = false;

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
                                        logger.warn("DocumentDownloadDialog: resolved template '{}' -> path='{}' ext='{}' for kit id='{}'", newFullName, ispPath, ext, ke.id());
                                    } catch (java.io.FileNotFoundException fnf) {
                                        // Template not found: report and mark problem
                                        java.util.List<Exception> errors = new java.util.ArrayList<>();
                                        errors.add(new TemplateMissingException("NoTemplate", fnf));
                                        reportPrecheckErrors(errors);
                                        resourceProblem = true;
                                        break;
                                    }
                                    KitElement newKe = new KitElement(ke.id(), newFullName, ext, ispPath, ke.count(), ke.writerFactory(), ke.preCheck());
                                    kitElements.set(i, newKe);
                                    logger.warn("DocumentDownloadDialog: updated kitElements[{}] -> name='{}' isp='{}'", i, newFullName, ispPath);
                                } catch (Throwable ignore) {
                                    LoggerUtils.logError(this.logger, ignore);
                                }
                        }
                    }
                }

                if (resourceProblem) {
                    // ensure the download control is disabled if resource missing
                    setDownloadEnabled(false);
                } else {
                    // no resource problems; run UI precheck if present
                    if (pre != null) {
                        try {
                            Optional<Exception> res = pre.get();
                            if (res == null || res.isEmpty()) {
                                ok = true;
                            } else {
                                java.util.List<Exception> errors = new java.util.ArrayList<>();
                                errors.add(res.get());
                                reportPrecheckErrors(errors);
                            }
                        } catch (Throwable t) {
                            java.util.List<Exception> errors = new java.util.ArrayList<>();
                            errors.add(new Exception(t));
                            reportPrecheckErrors(errors);
                        }
                    } else {
                        // no ui precheck attached: treat as OK
                        ok = true;
                    }

                    if (ok) {
                        clearProcessing();
                        setDownloadEnabled(true);
                    } else {
                        setDownloadEnabled(false);
                    }
                }
            } catch (Throwable ex) {
                LoggerUtils.logError(this.logger, ex);
            }
        });
    }

    public boolean matchExtension(PreCompetitionTemplates template, String f) {
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

    private FormLayout createLayoutHeader(PreCompetitionTemplates templateDefinition) {
        FormLayout layout = createLayout();
        Component title = createTitle(templateDefinition.name());
        layout.add(title);
        layout.setColspan(title, 2);
        return layout;
    }

    private FormLayout createSetLayoutHeader(PreCompetitionTemplates templateDefinition) {
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
