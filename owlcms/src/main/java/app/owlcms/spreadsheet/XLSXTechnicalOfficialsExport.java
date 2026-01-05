package app.owlcms.spreadsheet;

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

import app.owlcms.data.technicalofficial.TechnicalOfficial;
import app.owlcms.data.technicalofficial.TechnicalOfficialRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.servlet.StopProcessingException;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class XLSXTechnicalOfficialsExport extends XLSXWorkbookStreamSource {

    public XLSXTechnicalOfficialsExport(UI ui) {
        super(ui);
    }

    public Optional<Exception> preCheck() {
        try {
            List<TechnicalOfficial> officials = TechnicalOfficialRepository.findAll();
            if (officials == null || officials.isEmpty()) {
                return Optional.of(new StopProcessingException(Translator.translate("export.noTechnicalOfficials"), null));
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
            String[] headers = {"Active", "Role", "LastName", "FirstName", "Level", "FederationId", "Federation", "Affiliation", "IWFId", "Team", "OfficialRole"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                switch (headers[i]) { 
                    case "Team":
                        cell.setCellValue(Translator.translate("Team"));
                        break;
                    case "Active":
                        cell.setCellValue(Translator.translate("TechnicalOfficial.Active"));
                        break;
                    case "Role":
                        cell.setCellValue(Translator.translate("TechnicalOfficial.Accreditation"));
                        break;
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
                    case "OfficialRole":
                        cell.setCellValue(Translator.translate("TechnicalOfficials.OfficialRole"));
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
                row.createCell(0).setCellValue(official.isActive() ? "TRUE" : "FALSE");
                row.createCell(1).setCellValue(official.getRole() != null ? Translator.translate("TO.Role."+official.getRole().toString()) : "");
                row.createCell(2).setCellValue(official.getLastName() != null ? official.getLastName() : "");
                row.createCell(3).setCellValue(official.getFirstName() != null ? official.getFirstName() : "");
                row.createCell(4).setCellValue(official.getLevel() != null ? Translator.translate("TOLevel."+official.getLevel().toString()) : "");
                row.createCell(5).setCellValue(official.getFederationId() != null ? official.getFederationId() : "");
                row.createCell(6).setCellValue(official.getFederation() != null ? official.getFederation() : "");
                row.createCell(7).setCellValue(official.getAffiliation() != null ? official.getAffiliation() : "");
                row.createCell(8).setCellValue(official.getIwfId() != null ? official.getIwfId() : "");
                // Team may be null
                row.createCell(9).setCellValue(official.getTechnicalOfficialTeam() != null ? String.valueOf(official.getTechnicalOfficialTeam()) : "");
                // OfficialRole - export generic role (REFEREE, JURY_MEMBER) instead of detailed session assignments
                String exportRole = "";
                if (official.getOfficialRole() != null) {
                    switch (official.getOfficialRole()) {
                        case CENTER_REFEREE:
                        case LEFT_REFEREE:
                        case RIGHT_REFEREE:
                        case REFEREE_RESERVE:
                            exportRole = "REFEREE";
                            break;
                        case JURY_A:
                        case JURY_B:
                        case JURY_C:
                        case JURY_D:
                        case JURY_RESERVE:
                            exportRole = "JURY_MEMBER";
                            break;
                        default:
                            // Keep specific roles as-is: JURY_PRESIDENT, MARSHAL1, MARSHAL2, TECHNICAL_CONTROLLER1, etc.
                            exportRole = official.getOfficialRole().name();
                            break;
                    }
                }
                row.createCell(10).setCellValue(exportRole);
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
