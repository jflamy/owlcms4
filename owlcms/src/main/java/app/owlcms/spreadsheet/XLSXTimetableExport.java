/*******************************************************************************
 * @author Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0 (NPOSL-3.0)
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetable;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetableRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.servlet.StopProcessingException;
import ch.qos.logback.classic.Logger;

/**
 * XLSXTimetableExport - Exports timetable entries to XLSX file.
 *
 * Extends XLSXWorkbookStreamSource for integration with DownloadButtonFactory.
 */
@SuppressWarnings("serial")
public class XLSXTimetableExport extends XLSXWorkbookStreamSource {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(XLSXTimetableExport.class);

    public XLSXTimetableExport(UI ui) {
        super(ui);
    }

    @Override
    public Optional<Exception> prepare() {
        try {
            List<TechnicalOfficialsTimetable> entries = JPAService.runInTransaction(em ->
                    TechnicalOfficialsTimetableRepository.findAll(em));
            if (entries == null || entries.isEmpty()) {
                return Optional.of(new StopProcessingException(Translator.translate("Timetable.NoEntriesFound"), null));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(e);
        }
    }

    @Override
    protected void writeStream(OutputStream stream) {
        try {
            List<TechnicalOfficialsTimetable> entries = JPAService.runInTransaction(em ->
                    TechnicalOfficialsTimetableRepository.findAll(em));
            TimetableIO.exportTimetable(stream, entries);
        } catch (IOException e) {
            logger.error("Error writing timetable: {}", e);
            throw new RuntimeException("Error writing timetable", e);
        }
    }

}
