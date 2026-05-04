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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private static final String[] TIMETABLE_COLUMNS = {
            "JURY",
            "REFEREE",
            "MARSHALL",
            "TIMEKEEPER",
            "ANNOUNCER",
            "WEIGHIN",
            "TECHNICAL_CONTROLLER",
            "DOCTOR",
            "COMPETITION_SECRETARY"
    };

    /**
     * Export timetable entries to XLSX format.
     * If no entries exist, creates an empty timetable with all sessions and roles.
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

            // Get all sessions, ordered by competition time
            List<Group> allGroups = GroupRepository.findAll();
            allGroups.sort((g1, g2) -> {
                if (g1.getCompetitionTime() == null && g2.getCompetitionTime() == null) {
                    return 0;
                }
                if (g1.getCompetitionTime() == null) {
                    return 1;
                }
                if (g2.getCompetitionTime() == null) {
                    return -1;
                }
                return g1.getCompetitionTime().compareTo(g2.getCompetitionTime());
            });

            // Define timetable role categories in required order
            // These correspond to team assignment categories that map to Group session setters:
            // - JURY -> jury1..jury5, reserveJury
            // - REFEREE -> referee1..referee3, reserve
            // - MARSHALL -> marshall, marshall2
            // - TIMEKEEPER -> timeKeeper
            // - ANNOUNCER -> announcer
            // - WEIGHIN -> weighIn1, weighIn2
            // - TECHNICAL_CONTROLLER -> technicalController, technicalController2
            // - DOCTOR -> doctor, doctor2, doctor3
            // - COMPETITION_SECRETARY -> competitionSecretary, competitionSecretary2

            // Build a map of (session, role) -> teamNumber from existing entries
            Map<String, Map<String, Integer>> sessionRoleTeamMap = new HashMap<>();
            if (timetableEntries != null) {
                for (TechnicalOfficialsTimetable entry : timetableEntries) {
                    if (entry.getGroup() != null && entry.getRoleCategory() != null) {
                        String sessionName = entry.getGroup().getName();
                        String roleName = entry.getRoleCategory().name();
                        sessionRoleTeamMap
                            .computeIfAbsent(sessionName, k -> new HashMap<>())
                            .put(roleName, entry.getTeamNumber());
                    }
                }
            }

            // Matrix format (sessions × roles)
            // Create header row with "Session" in first column and role category names
            Row headerRow = sheet.createRow(0);
            Cell sessionHeader = headerRow.createCell(0);
            sessionHeader.setCellValue(Translator.translate("Session"));
            sessionHeader.setCellStyle(headerStyle);

            for (int i = 0; i < TIMETABLE_COLUMNS.length; i++) {
                Cell cell = headerRow.createCell(i + 1);
                cell.setCellValue(TIMETABLE_COLUMNS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create row for each session with team numbers in role columns
            int rowNum = 1;
            for (Group group : allGroups) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(group.getName());
                
                // Fill in team numbers for each role category if they exist
                Map<String, Integer> roleTeamMap = sessionRoleTeamMap.get(group.getName());
                if (roleTeamMap != null) {
                    for (int i = 0; i < TIMETABLE_COLUMNS.length; i++) {
                        Integer teamNumber = roleTeamMap.get(TIMETABLE_COLUMNS[i]);
                        if (teamNumber != null) {
                            row.createCell(i + 1).setCellValue(teamNumber);
                        }
                    }
                }
            }

            // Autosize columns
            sheet.autoSizeColumn(0);
            for (int i = 0; i < TIMETABLE_COLUMNS.length; i++) {
                sheet.autoSizeColumn(i + 1);
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
    //to be fixed: errors are not reported to user, only logged - need to collect and report back in UI
    public static List<TechnicalOfficialsTimetable> importTimetable(InputStream in) throws IOException {
        List<TechnicalOfficialsTimetable> result = new ArrayList<>();

        try {
            Workbook workbook = WorkbookFactory.create(in);
            Sheet sheet = workbook.getSheetAt(0);

            // Read header row to get column indices
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                logger.error("No header row found");
                workbook.close();
                return result;
            }

            // Build map of role category name -> column index
            Map<String, Integer> roleColumnMap = new LinkedHashMap<>();
            List<String> rawHeaders = new ArrayList<>();
            for (int col = 1; col < headerRow.getLastCellNum(); col++) {
                Cell cell = headerRow.getCell(col);
                if (cell != null) {
                    String rawHeaderValue = getCellValueAsString(cell).trim();
                    if (rawHeaderValue.isEmpty()) {
                        continue;
                    }
                    String headerValue = rawHeaderValue.toUpperCase(Locale.ROOT);
                    rawHeaders.add(col + ":" + rawHeaderValue + " -> " + headerValue);
                    roleColumnMap.put(headerValue, col);
                }
            }
            logger.info("Timetable import header row raw/normalized headers: {}", rawHeaders);
            logger.info("Timetable import recognized canonical header columns: {}", roleColumnMap);

            List<String> missingCanonicalHeaders = new ArrayList<>();
            for (String timetableColumn : TIMETABLE_COLUMNS) {
                if (!roleColumnMap.containsKey(timetableColumn)) {
                    missingCanonicalHeaders.add(timetableColumn);
                }
            }
            if (!missingCanonicalHeaders.isEmpty()) {
                logger./**/warn("Timetable import missing canonical headers: {}", missingCanonicalHeaders);
            }

            Map<String, Integer> importedRoleCounts = new LinkedHashMap<>();
            for (String timetableColumn : TIMETABLE_COLUMNS) {
                importedRoleCounts.put(timetableColumn, 0);
            }

            // Process data rows (matrix format: Session | JURY | REFEREE | ... )
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                try {
                    Cell sessionCell = row.getCell(0);
                    if (sessionCell == null) {
                        continue;
                    }

                    String sessionName = getCellValueAsString(sessionCell).trim();
                    if (sessionName.isEmpty()) {
                        continue;
                    }

                    // Lookup session by name
                    Group group = GroupRepository.findByName(sessionName);
                    if (group == null) {
                        logger./**/warn("Session '{}' not found at row {}", sessionName, i + 1);
                        continue;
                    }

                    // Process each role category column
                    for (String roleCategory : TIMETABLE_COLUMNS) {
                        Integer colIndex = roleColumnMap.get(roleCategory);
                        if (colIndex == null) {
                            continue;
                        }

                        Cell teamCell = row.getCell(colIndex);
                        if (teamCell == null) {
                            continue;
                        }

                        String teamNumberStr = getCellValueAsString(teamCell).trim();
                        if (teamNumberStr.isEmpty()) {
                            continue;
                        }

                        // Parse team number
                        Integer teamNumber;
                        try {
                            teamNumber = Integer.parseInt(teamNumberStr);
                        } catch (NumberFormatException e) {
                            logger./**/warn("Invalid team number '{}' at row {} column {}", teamNumberStr, i + 1, roleCategory);
                            continue;
                        }

                        // Parse role category
                        OfficialRole role;
                        try {
                            role = OfficialRole.valueOf(roleCategory);
                        } catch (IllegalArgumentException e) {
                            logger./**/warn("Invalid role category '{}' at row {}", roleCategory, i + 1);
                            continue;
                        }

                        TechnicalOfficialsTimetable entry = new TechnicalOfficialsTimetable(group, role, teamNumber);
                        result.add(entry);
                        importedRoleCounts.merge(roleCategory, 1, Integer::sum);
                    }

                } catch (Exception e) {
                    logger./**/warn("Error parsing row {}: {}", i + 1, e.getMessage());
                }
            }

            logger.info("Timetable import created {} entries by role: {}", result.size(), importedRoleCounts);
            if (importedRoleCounts.getOrDefault("JURY", 0) == 0) {
                logger./**/warn("Timetable import created no JURY entries. JURY header present: {}, column: {}",
                        roleColumnMap.containsKey("JURY"), roleColumnMap.get("JURY"));
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
