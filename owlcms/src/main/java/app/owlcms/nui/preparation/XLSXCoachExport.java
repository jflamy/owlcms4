package app.owlcms.nui.preparation;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.LoggerFactory;

import app.owlcms.data.coach.Coach;
import app.owlcms.data.coach.CoachRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.XLSXWorkbookStreamSource;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class XLSXCoachExport extends XLSXWorkbookStreamSource {

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

            // Create headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"LastName", "FirstName", "MembershipId", "Team"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                switch (headers[i]) {
                    case "LastName":
                        cell.setCellValue(Translator.translate("LastName"));
                        break;
                    case "FirstName":
                        cell.setCellValue(Translator.translate("FirstName"));
                        break;
                    case "MembershipId":
                        cell.setCellValue(Translator.translate("Registration.FederationCodesShort"));
                        break;
                    case "Team":
                        cell.setCellValue(Translator.translate("Coach.Team"));
                        break;
                    default:
                        cell.setCellValue(headers[i]);
                }
                cell.setCellStyle(headerStyle);
            }

            // Add data rows
            List<Coach> coaches = CoachRepository.findAll();
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
}
