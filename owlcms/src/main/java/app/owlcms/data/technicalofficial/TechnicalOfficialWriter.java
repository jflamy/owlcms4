package app.owlcms.data.technicalofficial;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.LoggerFactory;

import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

public class TechnicalOfficialWriter {
    private final static Logger logger = (Logger) LoggerFactory.getLogger(TechnicalOfficialWriter.class);

    public static InputStream write() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Technical Officials");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Create headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"LastName", "FirstName", "Level", "IWFId", "Federation", "FederationId"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                switch (headers[i]) {
                    case "LastName":
                        cell.setCellValue(Translator.translate("LastName"));
                        break;
                    case "FirstName":
                        cell.setCellValue(Translator.translate("FirstName"));
                        break;
                    case "Level":
                        cell.setCellValue(Translator.translate("TechnicalOfficials.Level"));
                        break;
                    case "IWFId":
                        cell.setCellValue(Translator.translate("TechnicalOfficials.IWFId"));
                        break;
                    case "Federation":
                        cell.setCellValue(Translator.translate("TechnicalOfficials.Federation"));
                        break;
                    case "FederationId":
                        cell.setCellValue(Translator.translate("TechnicalOfficials.FederationId"));
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
                row.createCell(3).setCellValue(official.getIwfId() != null ? official.getIwfId() : "");
                row.createCell(4).setCellValue(official.getFederation() != null ? official.getFederation() : "");
                row.createCell(5).setCellValue(official.getFederationId() != null ? official.getFederationId() : "");
            }

            // Autosize columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());

        } catch (IOException e) {
            logger.error("Error writing technical officials: {}", e);
            return null;
        }
    }
}
