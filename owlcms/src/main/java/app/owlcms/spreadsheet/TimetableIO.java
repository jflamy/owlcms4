/*******************************************************************************
 * @author Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0 (NPOSL-3.0)
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.LoggerFactory;

import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.technicalofficial.OfficialRole;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetable;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

/**
 * TimetableIO - Import/export technical officials timetable assignments.
 *
 * Handles XLSX format for timetable entries mapping sessions, roles, and teams.
 * Format: Session | Role | Team Number
 */
public class TimetableIO {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(TimetableIO.class);

    /**
     * Export timetable entries to XLSX format.
     *
     * @param out Output stream to write XLSX data
     * @param timetableEntries List of timetable entries to export
     * @throws IOException If write fails
     */
    public static void exportTimetable(OutputStream out, List<TechnicalOfficialsTimetable> timetableEntries)
            throws IOException {

        Workbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet("Timetable");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Create headers
            Row headerRow = sheet.createRow(0);
            String[] headers = { "Session", "Role", "TeamNumber" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                switch (headers[i]) {
                    case "Session":
                        cell.setCellValue(Translator.translate("Session"));
                        break;
                    case "Role":
                        cell.setCellValue(Translator.translate("TechnicalOfficial.Accreditation"));
                        break;
                    case "TeamNumber":
                        cell.setCellValue(Translator.translate("Team"));
                        break;
                    default:
                        cell.setCellValue(headers[i]);
                }
                cell.setCellStyle(headerStyle);
            }

            // Add data rows
            int rowNum = 1;
            for (TechnicalOfficialsTimetable entry : timetableEntries) {
                Row row = sheet.createRow(rowNum++);
                String sessionName = entry.getGroup() != null ? entry.getGroup().getName() : "";
                String roleName = entry.getRoleCategory() != null ? entry.getRoleCategory().name() : "";
                Integer teamNumber = entry.getTeamNumber();

                row.createCell(0).setCellValue(sessionName);
                row.createCell(1).setCellValue(roleName);
                if (teamNumber != null) {
                    row.createCell(2).setCellValue(teamNumber);
                }
            }

            // Autosize columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                logger.error("Error closing workbook: {}", e);
            }
        }
    }

    /**
     * Import timetable entries from XLSX stream.
     *
     * @param in Input stream containing XLSX data
     * @return List of imported timetable entries
     * @throws IOException If read fails
     */
    public static List<TechnicalOfficialsTimetable> importTimetable(InputStream in) throws IOException {
        List<TechnicalOfficialsTimetable> result = new ArrayList<>();

        try {
            Workbook workbook = WorkbookFactory.create(in);
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                try {
                    Cell sessionCell = row.getCell(0);
                    Cell roleCell = row.getCell(1);
                    Cell teamCell = row.getCell(2);

                    if (sessionCell == null || roleCell == null || teamCell == null) {
                        logger.warn("Skipping incomplete row {}", i + 1);
                        continue;
                    }

                    String sessionName = getCellValueAsString(sessionCell).trim();
                    String roleName = getCellValueAsString(roleCell).trim();
                    String teamNumberStr = getCellValueAsString(teamCell).trim();

                    if (sessionName.isEmpty() || roleName.isEmpty() || teamNumberStr.isEmpty()) {
                        logger.warn("Skipping empty row {}", i + 1);
                        continue;
                    }

                    // Lookup session by name
                    Group group = GroupRepository.findByName(sessionName);
                    if (group == null) {
                        logger.warn("Session '{}' not found at row {}", sessionName, i + 1);
                        continue;
                    }

                    // Parse role
                    OfficialRole role;
                    try {
                        role = OfficialRole.valueOf(roleName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid role '{}' at row {}", roleName, i + 1);
                        continue;
                    }

                    // Parse team number
                    Integer teamNumber;
                    try {
                        teamNumber = Integer.parseInt(teamNumberStr);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid team number '{}' at row {}", teamNumberStr, i + 1);
                        continue;
                    }

                    TechnicalOfficialsTimetable entry = new TechnicalOfficialsTimetable(group, role, teamNumber);
                    result.add(entry);

                } catch (Exception e) {
                    logger.warn("Error parsing row {}: {}", i + 1, e.getMessage());
                }
            }

            workbook.close();
        } catch (IOException e) {
            logger.error("Error reading timetable file: {}", e.getMessage());
            throw e;
        }

        return result;
    }

    /**
     * Helper method to get cell value as string (handles numeric cells)
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

}
