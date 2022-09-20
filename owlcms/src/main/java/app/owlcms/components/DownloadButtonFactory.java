/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import app.owlcms.data.config.Config;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.Resource;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

public class DownloadButtonFactory {

    private String buttonLabel;
    private String dialogTitle;
    private Function<Competition, String> fileNameGetter;
    private BiConsumer<Competition, String> fileNameSetter;
    private Logger logger = (Logger) LoggerFactory.getLogger(DownloadButtonFactory.class);
    private String outputFileName;
    private String resourceDirectoryLocation;
    private Supplier<JXLSWorkbookStreamSource> streamSourceSupplier;
    private JXLSWorkbookStreamSource xlsWriter;

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
            Supplier<JXLSWorkbookStreamSource> streamSourceSupplier,
            String resourceDirectoryLocation,
            Function<Competition, String> fileNameGetter,
            BiConsumer<Competition, String> fileNameSetter,
            String dialogTitle, String outputFileName, String buttonLabel) {
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
        Button innerButton = new Button(buttonLabel, new Icon(VaadinIcon.DOWNLOAD_ALT));
        EnhancedDialog dialog = new EnhancedDialog();
        dialog.setHeader(new H3(dialogTitle));
        ComboBox<Resource> templateSelect = new ComboBox<>();

        templateSelect.setPlaceholder(Translator.translate("AvailableTemplates"));
        templateSelect.setHelperText(Translator.translate("SelectTemplate"));
        List<Resource> resourceList = new ResourceWalker().getResourceList(resourceDirectoryLocation,
                ResourceWalker::relativeName, null, OwlcmsSession.getLocale(),
                Config.getCurrent().isLocalTemplatesOnly());
        templateSelect.setItems(resourceList);
        templateSelect.setValue(null);
        templateSelect.setWidth("15em");
        templateSelect.getStyle().set("margin-left", "1em");
        innerButton.setEnabled(false);
        try {
            // Competition.getTemplateFileName()
            // the getter should return a default if not set.
            String curTemplateName = fileNameGetter.apply(Competition.getCurrent());
            logger.debug("(1) curTemplateName {}", curTemplateName);
            // searchMatch should always return something unless the directory is empty.
            Resource found = searchMatch(resourceList, curTemplateName);
            logger.debug("(1) template found {}", found != null ? found.getFilePath() : null);

            templateSelect.addValueChangeListener(e -> {
                try {
                    // Competition.setTemplateFileName(...)
                    String fileName = e.getValue().getFileName();
                    fileNameSetter.accept(Competition.getCurrent(), fileName);
                    xlsWriter = streamSourceSupplier.get();
                    logger.debug("(2) xlsWriter {} {}", xlsWriter, fileName);

                    // supplier is a lambda that sets the template and the filter values in the xls source
                    Resource res = searchMatch(resourceList, fileName);
                    logger.debug("(2) template found {}", res != null ? res.getFileName() : null);

                    InputStream is = res.getStream();
                    xlsWriter.setInputStream(is);
                    logger.debug("(2) filter present = {}", xlsWriter.getGroup());

                    CompetitionRepository.save(Competition.getCurrent());
                    fileName = getTargetFileName();
                    logger.debug("(2) filename final = {}", fileName);
                    wrappedButton.setHref(new StreamResource(fileName, xlsWriter));
                    innerButton.setEnabled(true);
                } catch (Throwable e1) {
                    logger.error("{}", LoggerUtils.stackTrace(e1));
                }
            });
            templateSelect.setValue(found);

        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }

        wrappedButton.getStyle().set("margin-left", "1em");

        logger.debug("adding dialog button {}", wrappedButton.getHref());
        wrappedButton.add(innerButton);
        innerButton.addClickListener(e -> dialog.close());

        dialog.add(templateSelect, wrappedButton);
        return dialog;
    }

    private String getTargetFileName() {
        StringBuilder suffix = new StringBuilder();
        if (xlsWriter.getCategory() != null) {
            suffix.append("_");
            suffix.append(xlsWriter.getCategory().getCode());
        } else if (xlsWriter.getAgeGroupPrefix() != null) {
            suffix.append("_");
            suffix.append(xlsWriter.getAgeGroupPrefix().toString());
        } else if (xlsWriter.getAgeDivision() != null) {
            suffix.append("_");
            suffix.append(xlsWriter.getAgeDivision().toString());
        }

        if (xlsWriter.getGroup() != null) {
            suffix.append("_");
            suffix.append(xlsWriter.getGroup().toString());
        }
        LocalDateTime now = LocalDateTime.now().withNano(0);
        suffix.append("_");
        suffix.append(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm';'ss")));
        suffix.append(".xls");
        String fileName = outputFileName + suffix;
        fileName = sanitizeFilename(fileName);
        logger.debug(fileName);
        return fileName;
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[:\\\\/*?|<>]", "_");
    }

    private Resource searchMatch(List<Resource> resourceList, String curTemplateName) {
        Resource found = null;
        for (Resource curResource : resourceList) {
            String fileName = curResource.getFileName();
            logger.debug("comparing {} {}", fileName, curTemplateName);
            if (fileName.equals(curTemplateName)) {
                found = curResource;
                break;
            }
        }
        return found;
    }

}
