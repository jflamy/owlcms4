package app.owlcms.nui.preparation;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.coach.Coach;
import app.owlcms.data.coach.CoachRepository;
import app.owlcms.data.coach.CoachReader;
import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.XLSXWorkbookStreamSource;
import app.owlcms.servlet.StopProcessingException;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class XLSXCoachExport extends XLSXWorkbookStreamSource {

    public XLSXCoachExport(UI ui) {
        super(ui);
    }

    final private static Logger logger = (Logger) LoggerFactory.getLogger(XLSXCoachExport.class);

    @Override
    protected void writeStream(OutputStream stream) {
        Workbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet("Coaches");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Create headers using canonical constants from CoachReader
            Row headerRow = sheet.createRow(0);
            String[] headers = { CoachReader.LAST_NAME, CoachReader.FIRST_NAME, CoachReader.MEMBERSHIP_ID, CoachReader.TEAM };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                String h = headers[i];
                cell.setCellValue(Translator.translate(h));
                cell.setCellStyle(headerStyle);
            }

            // Add data rows
            List<Coach> coaches = CoachRepository.findAll();
            if (coaches.isEmpty()) {
                throw new IOException(Translator.translate("export.noCoaches"));
            }
            int rowNum = 1;
            for (Coach coach : coaches) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(coach.getLastName() != null ? coach.getLastName() : "");
                row.createCell(1).setCellValue(coach.getFirstName() != null ? coach.getFirstName() : "");
                row.createCell(2).setCellValue(coach.getMembershipId() != null ? coach.getMembershipId() : "");
                row.createCell(3).setCellValue(coach.getTeam() != null ? coach.getTeam() : "");
            }

            // Autosize columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(stream);
        } catch (IOException e) {
            logger.error("Error writing coaches: {}", e);
            throw new RuntimeException("Error writing coaches", e);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                logger.error("Error closing workbook: {}", e);
            }
        }
    }


    public Optional<Exception> preCheck() {
        try {
            List<Coach> coaches = CoachRepository.findAll();
            if (coaches == null || coaches.isEmpty()) {
                return Optional.of(new StopProcessingException(Translator.translate("export.noCoaches"), null));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(e);
        }
    }

    @Override
    public Optional<Exception> prepare() {
        return preCheck();
    }
}
