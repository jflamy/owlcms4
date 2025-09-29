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
            });
        } catch (Throwable ignore) {
        }
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
            try {
                Resource value = e.getValue();
                String newTemplateName = value != null ? value.getFileName() : null;
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

                // notify dialog so it can re-run any prechecks attached to the download control and clear messages.
                try {
                    clearProcessing();
                    runDownloadControlUiPrecheck();
                } catch (Throwable ignore) {
                }
            } catch (Throwable e1) {
                LoggerUtils.logError(this.logger, e1);
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
