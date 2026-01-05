/*******************************************************************************
 * @author Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0 (NPOSL-3.0)
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;

import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetable;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetableRepository;
import app.owlcms.spreadsheet.TimetableIO;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

/**
 * TimetableUploadDialog - Dialog for uploading timetable XLSX file.
 *
 * Allows users to upload an XLSX file containing timetable entries (session, role, team).
 */
@SuppressWarnings("serial")
public class TimetableUploadDialog extends Dialog {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(TimetableUploadDialog.class);
    private Runnable callback;
    private Upload upload;
    private TextArea errorArea = new TextArea();

    public TimetableUploadDialog() {
        Button uploadButton = new Button(Translator.translate("SelectFile"));

        UploadHandler uploadHandler = UploadHandler.inMemory((metadata, bytes) -> {
            try {
                ByteArrayInputStream is = new ByteArrayInputStream(bytes);
                List<TechnicalOfficialsTimetable> entries = TimetableIO.importTimetable(is);

                if (entries.isEmpty()) {
                    errorArea.setValue(Translator.translate("Timetable.NoEntriesFound"));
                    errorArea.setVisible(true);
                    return;
                }

                // Clear existing timetable and insert new entries
                JPAService.runInTransaction(em -> {
                    TechnicalOfficialsTimetableRepository.deleteAll(em);
                    for (TechnicalOfficialsTimetable entry : entries) {
                        em.persist(entry);
                    }
                    em.flush();
                    return null;
                });

                errorArea.setValue(Translator.translate("Timetable.ImportedSuccessfully") + ": " + entries.size() + " " +
                        Translator.translate("Timetable.entries"));
                errorArea.setVisible(true);

                if (callback != null) {
                    callback.run();
                }
            } catch (Exception ex) {
                errorArea.setVisible(true);
                errorArea.setValue(Translator.translate("Timetable.ImportFailed") + ": " + ex.getMessage());
                logger.error("Upload failed", ex);
            }
        });

        upload = new Upload(uploadHandler);
        upload.setUploadButton(uploadButton);
        upload.setDropLabel(new NativeLabel(Translator.translate("Timetable.SelectXLSXFile")));
        upload.setAcceptedFileTypes("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");

        errorArea.setReadOnly(true);
        errorArea.setVisible(false);
        errorArea.setWidth("100%");

        add(upload);
        add(errorArea);

        Button closeButton = new Button(Translator.translate("Close"), event -> close());
        getFooter().add(closeButton);
    }

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }

}
