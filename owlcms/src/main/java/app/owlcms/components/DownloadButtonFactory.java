package app.owlcms.components;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.LoggerFactory;

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
import ch.qos.logback.classic.Logger;

public class DownloadButtonFactory {

    private JXLSWorkbookStreamSource xlsWriter;
    private Supplier<JXLSWorkbookStreamSource> streamSourceSupplier;
    private String resourceDirectoryLocation;
    private Function<Competition, String> fileNameGetter;
    private BiConsumer<Competition, String> fileNameSetter;
    private String buttonLabel;
    private String outputFileName;
    private Logger logger = (Logger) LoggerFactory.getLogger(DownloadButtonFactory.class);
    private String dialogTitle;

    /**
     * @param streamSourceSupplier      lambda that creates a JXLSWorkbookStreamSource and sets its filters
     * @param resourceDirectoryLocation where to look for templates
     * @param fileNameGetter            get last file name stored in Competition
     * @param fileNameSetter            set last file name in Competition
     * @param buttonLabel
     * @param buttonLabel               label used in top bar
     * @param outputFileName            first part of the downloaded file name (not dependent on template).
     * @param dialogTitle
     * @return
     */
    public DownloadButtonFactory(
            JXLSWorkbookStreamSource xlsWriter,
            Supplier<JXLSWorkbookStreamSource> streamSourceSupplier,
            String resourceDirectoryLocation,
            Function<Competition, String> fileNameGetter,
            BiConsumer<Competition, String> fileNameSetter,
            String dialogTitle, String outputFileName, String buttonLabel) {
        this.xlsWriter = xlsWriter;
        this.outputFileName = outputFileName;
        this.streamSourceSupplier = streamSourceSupplier;
        this.resourceDirectoryLocation = resourceDirectoryLocation;
        this.fileNameGetter = fileNameGetter;
        this.fileNameSetter = fileNameSetter;
        this.buttonLabel = buttonLabel;
        this.dialogTitle = dialogTitle;
    }

    /**
     * @return
     */
    public Button createTopBarDownloadButton() {
        Button dialogOpen = new Button(dialogTitle, new Icon(VaadinIcon.DOWNLOAD_ALT),
                e -> {
                    EnhancedDialog dialog = createDialog();
                    dialog.open();
                });
        return dialogOpen;
    }

    private EnhancedDialog createDialog() {
        Anchor wrappedButton = new Anchor("", "");
        EnhancedDialog dialog = new EnhancedDialog();
        dialog.setHeader(new H3(dialogTitle));
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
            logger.warn("curTemplateName {}", curTemplateName);
            // searchMatch should always return something unless the directory is empty.
            Resource found = searchMatch(resourceList, curTemplateName);

            templateSelect.addValueChangeListener(e -> {
                try {
                    // Competition.setTemplateFileName(...)
                    String fileName = e.getValue().getFileName();
                    fileNameSetter.accept(Competition.getCurrent(), fileName);
                    xlsWriter = streamSourceSupplier.get();
//
//                    // supplier is a lambda that sets the template and the filter values in the xls source
//                    Resource res = searchMatch(resourceList, curTemplateName);
//                    logger.warn("resource found {}", found != null ? found.getFilePath() : null);
//                    InputStream is;
//
//                    is = res.getStream();
//                    xlsWriter.setInputStream(is);
//                    logger.debug("filter present = {}", xlsWriter.getGroup());
//
//                    CompetitionRepository.save(Competition.getCurrent());
//                    fileName = genHrefName();
//                    logger.debug("setHref change {}", fileName);
//                    wrappedButton.setHref(new StreamResource(fileName, xlsWriter));
                } catch (Throwable e1) {
                    e1.printStackTrace();
                }
                
                templateSelect.setValue(found);
            });

        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }

        wrappedButton.getStyle().set("margin-left", "1em");
        Button innerButton = new Button(buttonLabel, new Icon(VaadinIcon.DOWNLOAD_ALT));
        logger.debug("adding dialog button {}", wrappedButton.getHref());
        wrappedButton.add(innerButton);
        //innerButton.addClickListener(e -> dialog.close());

        dialog.add(templateSelect, wrappedButton);
        return dialog;
    }

    private String genHrefName() {
        String fileName = outputFileName + (xlsWriter.getGroup() != null ? "_" + xlsWriter.getGroup() : "_all")
                + ".xls";
        logger.debug(fileName);
        return fileName;
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
