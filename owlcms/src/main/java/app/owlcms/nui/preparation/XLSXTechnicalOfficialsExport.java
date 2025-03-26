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

import app.owlcms.data.technicalofficial.TechnicalOfficial;
import app.owlcms.data.technicalofficial.TechnicalOfficialRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.XLSXWorkbookStreamSource;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class XLSXTechnicalOfficialsExport extends XLSXWorkbookStreamSource {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(XLSXTechnicalOfficialsExport.class);

    @Override
    protected void writeStream(OutputStream stream) {
        Workbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet("Technical Officials");
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Create headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"LastName", "FirstName", "Level", "Federation", "FederationId", "Affiliation", "IWFId",};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                switch (headers[i]) {
                    case "LastName":
                        cell.setCellValue(Translator.translate("TechnicalOfficial.LastName"));
                        break;
                    case "FirstName":
                        cell.setCellValue(Translator.translate("TechnicalOfficial.FirstName"));
                        break;
                    case "Level":
                        cell.setCellValue(Translator.translate("TechnicalOfficial.Level"));
                        break;
                    case "IWFId":
                        cell.setCellValue(Translator.translate("TechnicalOfficial.IWFId"));
                        break;
                    case "Federation":
                        cell.setCellValue(Translator.translate("TechnicalOfficial.Federation"));
                        break;
                    case "FederationId":
                        cell.setCellValue(Translator.translate("TechnicalOfficial.FederationId"));
                        break;
                    case "Affiliation":
                        cell.setCellValue(Translator.translate("TechnicalOfficial.Affiliation"));
                        break;
                    default:
                        cell.setCellValue(headers[i]);
                }
                cell.setCellStyle(headerStyle);
            }

            // Add data rows
            List<TechnicalOfficial> officials = TechnicalOfficialRepository.findAll();
            int rowNum = 1;
            for (TechnicalOfficial official : officials) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(official.getLastName() != null ? official.getLastName() : "");
                row.createCell(1).setCellValue(official.getFirstName() != null ? official.getFirstName() : "");
                row.createCell(2).setCellValue(official.getLevel() != null ? Translator.translate("TOLevel."+official.getLevel().toString()) : "");
                row.createCell(3).setCellValue(official.getFederation() != null ? official.getFederation() : "");
                row.createCell(4).setCellValue(official.getFederationId() != null ? official.getFederationId() : "");
                row.createCell(5).setCellValue(official.getAffiliation() != null ? official.getAffiliation() : "");
                row.createCell(6).setCellValue(official.getIwfId() != null ? official.getIwfId() : "");
            }

            // Autosize columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(stream);
        } catch (IOException e) {
            logger.error("Error writing technical officials: {}", e);
            throw new RuntimeException("Error writing technical officials", e);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                logger.error("Error closing workbook: {}", e);
            }
        }
    }
}
