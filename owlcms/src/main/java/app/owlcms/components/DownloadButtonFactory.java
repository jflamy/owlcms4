package app.owlcms.components;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.vaadin.componentfactory.EnhancedDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.StreamResource;

import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;
import app.owlcms.ui.results.Resource;
import app.owlcms.utils.ResourceWalker;

public class DownloadButtonFactory {

    private JXLSWorkbookStreamSource xlsWriter;
    private Supplier<JXLSWorkbookStreamSource> streamSourceSupplier;
    private String resourceDirectoryLocation;
    private Function<Competition, String> fileNameGetter;
    private BiConsumer<Competition, String> fileNameSetter;
    private String buttonLabel;

    /**
     * @param streamSourceSupplier      lambda that creates a JXLSWorkbookStreamSource and sets its filters
     * @param resourceDirectoryLocation where to look for templates
     * @param fileNameGetter            get last file name stored in Competition
     * @param fileNameSetter            set last file name in Competition
     * @param buttonLabel
     * @param buttonLabel               label used in top bar
     * @return
     */
    public DownloadButtonFactory(
            JXLSWorkbookStreamSource xlsWriter,
            Supplier<JXLSWorkbookStreamSource> streamSourceSupplier,
            String resourceDirectoryLocation,
            Function<Competition, String> fileNameGetter,
            BiConsumer<Competition, String> fileNameSetter,
            String buttonLabel) {
        this.xlsWriter = xlsWriter;
        this.streamSourceSupplier = streamSourceSupplier;
        this.resourceDirectoryLocation = resourceDirectoryLocation;
        this.fileNameGetter = fileNameGetter;
        this.fileNameSetter = fileNameSetter;
        this.buttonLabel = buttonLabel;
    }

    /**
     * @return
     */
    public Button createResultsDownloadButton() {

        // supplier is a lambda that sets the filter values in the xls source
        xlsWriter = streamSourceSupplier.get();
        Anchor wrappedButton = new Anchor("", "");

        EnhancedDialog dialog = new EnhancedDialog();
        dialog.setHeader(new H3(buttonLabel));

        ComboBox<Resource> templateSelect = new ComboBox<>();
        templateSelect.setPlaceholder(Translator.translate("AvailableTemplates"));
        templateSelect.setHelperText(Translator.translate("SelectTemplate"));
        List<Resource> resourceList = new ResourceWalker().getResourceList(resourceDirectoryLocation,
                ResourceWalker::relativeName, null, OwlcmsSession.getLocale());
        templateSelect.setItems(resourceList);
        templateSelect.setValue(null);
        templateSelect.setWidth("15em");
        templateSelect.getStyle().set("margin-left", "1em");
        try {
            // Competition.getTemplateFileName()
            // the getter should return a default if not set.
            String curTemplateName = fileNameGetter.apply(Competition.getCurrent());
            // searchMatch should always return something unless the directory is empty.
            Resource found = searchMatch(resourceList, curTemplateName);
            wrappedButton.setHref(new StreamResource(found != null ? found.getFileName() : "", xlsWriter));

            templateSelect.addValueChangeListener(e -> {
                // Competition.setTemplateFileName(...)
                String fileName = e.getValue().getFileName();
                fileNameSetter.accept(Competition.getCurrent(), fileName);
                CompetitionRepository.save(Competition.getCurrent());
                fileName = genHrefName();
                wrappedButton.setHref(new StreamResource(fileName, xlsWriter));
            });
            templateSelect.setValue(found);
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }

        wrappedButton.getStyle().set("margin-left", "1em");
        Button innerButton = new Button(buttonLabel, new Icon(VaadinIcon.DOWNLOAD_ALT));
        wrappedButton.add(innerButton);
        wrappedButton.getElement().setAttribute("innerButton",
                genHrefName());

        dialog.add(templateSelect, wrappedButton);

        Button dialogOpen = new Button(buttonLabel, new Icon(VaadinIcon.DOWNLOAD_ALT),
                e -> {
                    dialog.open();
                });
        return dialogOpen;
    }

    private String genHrefName() {
        return "results" + (xlsWriter.getGroup() != null ? "_" + xlsWriter.getGroup() : "_all") + ".xls";
    }

    private Resource searchMatch(List<Resource> resourceList, String curTemplateName) {
        Resource found = null;
        for (Resource curResource : resourceList) {
            String fileName = curResource.getFileName();
            if (fileName.equals(curTemplateName)) {
                found = curResource;
                break;
            }
        }
        return found;
    }

}
