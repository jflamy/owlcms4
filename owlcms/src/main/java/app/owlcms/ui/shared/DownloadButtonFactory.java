/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.shared;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.StreamResource;

import app.owlcms.components.elements.LazyDownloadButton;
import app.owlcms.data.export.CompetitionData;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;
import ch.qos.logback.classic.Logger;

/**
 * A factory for creating Download Button objects.
 */
public class DownloadButtonFactory {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(DownloadButtonFactory.class);

    public static Div createDynamicJsonDownloadButton(String prefix, String label) {
        final LazyDownloadButton downloadButton = new LazyDownloadButton(
                label,
                new Icon(VaadinIcon.DOWNLOAD_ALT),
                () -> {
                    LocalDateTime now = LocalDateTime.now().withNano(0);
                    return prefix
                            + "_" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm"))
                            + ".json";
                },
                () -> {
                    return new CompetitionData().exportData();
                });

        return new Div(downloadButton);
    }

    /**
     * Creates a new DownloadButton object for a dynamically created file.
     *
     * @param prefix    the prefix
     * @param label     the label
     * @param xlsSource the xls source
     * @return the div
     */
    public static Div createDynamicXLSDownloadButton(String prefix, String label, JXLSWorkbookStreamSource xlsSource) {
        final LazyDownloadButton downloadButton = new LazyDownloadButton(
                label,
                new Icon(VaadinIcon.DOWNLOAD_ALT),
                () -> {
                    LocalDateTime now = LocalDateTime.now().withNano(0);
                    return prefix
                            + "_" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm';'ss"))
                            + ".xls";
                },
                xlsSource);

        return new Div(downloadButton);
    }

    public static Div createDynamicZipDownloadButton(String prefix, String label, byte[] content) {
        final LazyDownloadButton downloadButton = new LazyDownloadButton(
                label,
                new Icon(VaadinIcon.DOWNLOAD_ALT),
                () -> {
                    LocalDateTime now = LocalDateTime.now().withNano(0);
                    return prefix
                            + "_" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm"))
                            + ".zip";
                },
                () -> {
                    return new ByteArrayInputStream(content);
                });

        return new Div(downloadButton);
    }

    /**
     * Creates a new Download Button object for a static resource
     *
     * @param prefix             the prefix
     * @param label              the label
     * @param templateNamePrefix the template name prefix
     * @return the div
     */
    public static Div createStaticXLSDownloadButton(String prefix, String label, String templateNamePrefix) {
        String templateFileName = templateNamePrefix + OwlcmsSession.getLocale().getLanguage() + ".xls";
        logger.debug("templateFileName = {} href={}", templateFileName);
        StreamResource href = new StreamResource(prefix + ".xls",
                () -> OwlcmsSession.class.getResourceAsStream(templateFileName));
        return buildButton(prefix, label, href);
    }

    private static Div buildButton(String prefix, String label, StreamResource href) {
        Anchor finalResults = new Anchor(href, "");
        Button finalResultsButton = new Button(label, new Icon(VaadinIcon.DOWNLOAD_ALT));
        finalResultsButton.addFocusListener(e -> {
            String dlName = prefix + "_" + LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xls";
            finalResults.getElement().setAttribute("download", dlName);
        });
        finalResultsButton.setWidth("93%"); // don't ask. this is a kludge.
        finalResults.add(finalResultsButton);
        finalResults.setWidth("100%");
        Div finalResultsDiv = new Div(finalResults);
        finalResultsDiv.setWidthFull();
        return finalResultsDiv;
    }

}
