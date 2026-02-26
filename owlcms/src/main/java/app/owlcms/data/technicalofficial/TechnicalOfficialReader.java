package app.owlcms.data.technicalofficial;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.LoggerFactory;

import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

public class TechnicalOfficialReader {

    @SuppressWarnings("unused")
    private final static Logger logger = (Logger) LoggerFactory.getLogger(TechnicalOfficialReader.class);
    private static final String LAST_NAME = "LastName";
    private static final String FIRST_NAME = "FirstName";
    private static final String LEVEL = "Level";
    private static final String IWF_ID = "IWFId";
    private static final String FEDERATION = "Federation";
    private static final String FEDERATION_ID = "FederationId";
    private static final String AFFILIATION = "Affiliation";
    private static final String ACCREDITATION_ROLE = "AccreditationRole";
    private static final String ACTIVE = "Active";
    private static final String TEAM = "Team";
    private static final String TEAM_ROLE = "TeamRole";

    public List<TechnicalOfficial> importFromXLS(InputStream is, StringBuilder errors) {
        List<TechnicalOfficial> officials = new ArrayList<>();
        if (is != null) {
            JPAService.runInTransaction((em) -> {
                try {
                    // Delete existing officials
                    TechnicalOfficialRepository.deleteAll(em);
                    Workbook workbook = WorkbookFactory.create(is);
                    Sheet sheet = workbook.getSheetAt(0);
                    Row headerRow = sheet.getRow(0);
                    int[] colIndices = findColumnIndices(headerRow, errors);
                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) {
                            break;
                        }
                        try {
                            TechnicalOfficial official = readRow(row, colIndices);
                            if (official != null) {
                                TechnicalOfficial mergedOff = em.merge(official);
                                officials.add(mergedOff);
                            } else {
                                break;
                            }
                        } catch (IllegalArgumentException ex) {
                            // Append exception details
                            errors.append(ex.getMessage()).append("\n");
                        } catch (Exception ex) {
                            errors.append("Error processing row " + (i + 1) + ": " + ex.getMessage() + "\n")
                                  .append(ex.toString()).append("\n");
                        }
                    }
                    workbook.close();
                } catch (IOException e) {
                    errors.append("File reading error: " + e.getMessage() + "\n")
                          .append(e.toString()).append("\n");
                }
                return null;
            });
        }
        return officials;
    }

    private int[] findColumnIndices(Row headerRow, StringBuilder errors) {
        int[] indices = new int[11];  // One for each field (7 original + role + active + team + teamRole)
        // Initialize all indices to -1 to indicate column not found
        for (int i = 0; i < indices.length; i++) {
            indices[i] = -1;
        }
        Map<String, String> headerMap = new HashMap<>();
        
        // Map constants to themselves (legacy support)
        headerMap.put(LAST_NAME, LAST_NAME);
        headerMap.put(FIRST_NAME, FIRST_NAME);
        headerMap.put(LEVEL, LEVEL);
        headerMap.put(IWF_ID, IWF_ID);
        headerMap.put(FEDERATION, FEDERATION);
        headerMap.put(FEDERATION_ID, FEDERATION_ID);
        headerMap.put(ACCREDITATION_ROLE, ACCREDITATION_ROLE);
        headerMap.put("Role", ACCREDITATION_ROLE); // backward compatibility with old exports
        headerMap.put(ACTIVE, ACTIVE);
        headerMap.put(TEAM_ROLE, TEAM_ROLE);
        
        // Map English translations to constants (always accept English)
        headerMap.put(Translator.translateExplicitLocale("TechnicalOfficial.LastName", Locale.ENGLISH), LAST_NAME);
        headerMap.put(Translator.translateExplicitLocale("TechnicalOfficial.FirstName", Locale.ENGLISH), FIRST_NAME);
        headerMap.put(Translator.translateExplicitLocale("TechnicalOfficial.Level", Locale.ENGLISH), LEVEL);
        headerMap.put(Translator.translateExplicitLocale("TechnicalOfficial.IWFId", Locale.ENGLISH), IWF_ID);
        headerMap.put(Translator.translateExplicitLocale("TechnicalOfficial.Federation", Locale.ENGLISH), FEDERATION);
        headerMap.put(Translator.translateExplicitLocale("TechnicalOfficial.FederationId", Locale.ENGLISH), FEDERATION_ID);
        headerMap.put(Translator.translateExplicitLocale("TechnicalOfficial.Affiliation", Locale.ENGLISH), AFFILIATION);
        headerMap.put(Translator.translateExplicitLocale("TechnicalOfficial.Accreditation", Locale.ENGLISH), ACCREDITATION_ROLE);
        headerMap.put(Translator.translateExplicitLocale("TechnicalOfficial.Active", Locale.ENGLISH), ACTIVE);
        headerMap.put(Translator.translateExplicitLocale("Team", Locale.ENGLISH), TEAM);
        headerMap.put(Translator.translateExplicitLocale("TechnicalOfficials.TeamRole", Locale.ENGLISH), TEAM_ROLE);
        
        // Map local translations to constants
        headerMap.put(Translator.translate("TechnicalOfficial.LastName"), LAST_NAME);
        headerMap.put(Translator.translate("TechnicalOfficial.FirstName"), FIRST_NAME);
        headerMap.put(Translator.translate("TechnicalOfficial.Level"), LEVEL);
        headerMap.put(Translator.translate("TechnicalOfficial.IWFId"), IWF_ID);
        headerMap.put(Translator.translate("TechnicalOfficial.Federation"), FEDERATION);
        headerMap.put(Translator.translate("TechnicalOfficial.FederationId"), FEDERATION_ID);
        headerMap.put(Translator.translate("TechnicalOfficial.Affiliation"), AFFILIATION);
        headerMap.put(Translator.translate("TechnicalOfficial.Accreditation"), ACCREDITATION_ROLE);
        headerMap.put(Translator.translate("TechnicalOfficial.Active"), ACTIVE);
        headerMap.put(Translator.translate("TechnicalOfficials.TeamRole"), TEAM_ROLE);
        
        List<String> unmatchedHeaders = new ArrayList<>();
        List<String> matchedHeaders = new ArrayList<>();
        
        for (Cell cell : headerRow) {
            String header = cell.getStringCellValue().trim();
            if (header.isEmpty()) {
                continue; // Skip empty headers
            }
            int colIndex = cell.getColumnIndex();
            
            String constant = headerMap.get(header);
            if (constant != null) {
                matchedHeaders.add(header);
                switch (constant) {
                    case LAST_NAME:
                        indices[0] = colIndex;
                        break;
                    case FIRST_NAME:
                        indices[1] = colIndex;
                        break;
                    case LEVEL:
                        indices[2] = colIndex;
                        break;
                    case IWF_ID:
                        indices[3] = colIndex;
                        break;
                    case FEDERATION:
                        indices[4] = colIndex;
                        break;
                    case FEDERATION_ID:
                        indices[5] = colIndex;
                        break;
                    case AFFILIATION:
                        indices[6] = colIndex;
                        break;
                    case ACCREDITATION_ROLE:
                        indices[7] = colIndex;
                        break;
                    case ACTIVE:
                        indices[8] = colIndex;
                        break;
                    case TEAM:
                        indices[9] = colIndex;
                        break;
                    case TEAM_ROLE:
                        indices[10] = colIndex;
                        break;
                }
            } else {
                unmatchedHeaders.add(header);
            }
        }
        
        // Report unmatched headers as warnings
        if (!unmatchedHeaders.isEmpty()) {
            String warning = "Warning: Unmatched headers in technical official file: " + String.join(", ", unmatchedHeaders);
            logger./**/warn(warning);
            if (errors != null) {
                errors.append(warning).append("\n");
            }
        }
        
        // Log matched headers for debugging
        if (!matchedHeaders.isEmpty()) {
            logger.trace("Matched headers: {}", String.join(", ", matchedHeaders));
        }
        
        // Check for required columns
        List<String> missingColumns = new ArrayList<>();
        if (indices[0] == -1) missingColumns.add("LastName");
        if (indices[1] == -1) missingColumns.add("FirstName");
        
        if (!missingColumns.isEmpty()) {
            String error = "Missing required columns in technical official file: " + String.join(", ", missingColumns);
            logger.error(error);
            if (errors != null) {
                errors.append(error).append("\n");
            }
            throw new IllegalArgumentException(error);
        }
        
        return indices;
    }

    private TechnicalOfficial readRow(Row row, int[] colIndices) {
        Cell currentCell = colIndices[0] >= 0 ? row.getCell(colIndices[0]) : null;
        try {
            if (isEmptyCell(currentCell)) {
                return null;
            }
            String lastName = colIndices[0] >= 0 ? getCellValueAsString(currentCell) : "";
            String firstName = colIndices[1] >= 0 ? getCellValueAsString(row.getCell(colIndices[1])) : "";
            String levelStr = colIndices[2] >= 0 ? getCellValueAsString(row.getCell(colIndices[2])) : "";
            TOLevel level = null;
            if (levelStr != null && !levelStr.isBlank()) {
                level = findEnumValueForTranslatedTOLevel(levelStr);
            }
            String iwfId = colIndices[3] >= 0 ? getCellValueAsString(row.getCell(colIndices[3])) : "";
            String federation = colIndices[4] >= 0 ? getCellValueAsString(row.getCell(colIndices[4])) : "";
            String federationId = colIndices[5] >= 0 ? getCellValueAsString(row.getCell(colIndices[5])) : "";
            String affiliation = colIndices[6] >= 0 ? getCellValueAsString(row.getCell(colIndices[6])) : "";
            Cell teamCell = colIndices.length > 9 && colIndices[9] >= 0 ? row.getCell(colIndices[9]) : null;
            String teamStr = teamCell != null ? getCellValueAsString(teamCell) : "";
            
            String roleStr = colIndices[7] >= 0 ? getCellValueAsString(row.getCell(colIndices[7])) : "";
            TechnicalOfficial.Role role = TechnicalOfficial.Role.TECHNICAL_OFFICIAL; // Default value
            if (roleStr != null && !roleStr.isBlank()) {
                role = findEnumValueForTranslatedRole(roleStr);
            }
            
            // Default to active=true when importing (officials being imported are presumably active)
            // Only set to false if explicitly specified as FALSE/NO/N/0 in the spreadsheet
            String activeStr = colIndices[8] >= 0 ? getCellValueAsString(row.getCell(colIndices[8])) : "";
            boolean active = parseBooleanValueDefaultTrue(activeStr);

            // Parse TeamRole
            TeamRole teamRole = null;
            if (colIndices.length > 10 && colIndices[10] >= 0) {
                Cell teamRoleCell = row.getCell(colIndices[10]);
                String teamRoleStr = teamRoleCell != null ? getCellValueAsString(teamRoleCell) : "";
                if (teamRoleStr != null && !teamRoleStr.isEmpty()) {
                    teamRole = findEnumValueForTranslatedTeamRole(teamRoleStr);
                    if (teamRole == null) {
                        logger.error("Invalid TeamRole value '{}' for {} {} (row {}) - skipping TeamRole assignment", 
                            teamRoleStr, firstName, lastName, row.getRowNum() + 1);
                    }
                }
            }

            Integer team = null;
            if (teamStr != null && !teamStr.isBlank()) {
                try {
                    team = Integer.valueOf(teamStr);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Invalid team value: '" + teamStr + "' at " + getCellAddress(teamCell));
                }
            }

            TechnicalOfficial official = new TechnicalOfficial(lastName, firstName, level, iwfId, federation, federationId, affiliation, team);
            official.setAccreditationRole(role);
            official.setActive(active);
            official.setTeamRole(teamRole);
            return official;
        } catch(Exception e) {
            throw new IllegalArgumentException("Error processing cell "+ getCellAddress(currentCell) + ": " + e.getMessage());
        }
    }

    // Helper method to convert a cell's position to columnLetter-rowNumber format
    private String getCellAddress(Cell cell) {
        if(cell == null) return "unknown";
        int col = cell.getColumnIndex();
        StringBuilder colLetter = new StringBuilder();
        while(col >= 0) {
            colLetter.insert(0, (char) ('A' + (col % 26)));
            col = (col / 26) - 1;
        }
        int rowNum = cell.getRowIndex() + 1; // Excel rows are 1-indexed
        return colLetter.toString() + rowNum;
    }

    private boolean isEmptyCell(Cell cell) {
        if (cell == null) return true;
        if (cell.getCellType() == CellType.BLANK) return true;
        if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty()) return true;
        return false;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((int)cell.getNumericCellValue());
            default:
                return "";
        }
    }

    private TOLevel findEnumValueForTranslatedTOLevel(String levelStr) {
        for (TOLevel level : TOLevel.values()) {
            if (levelStr.equals(level.name()) ||
                levelStr.equals(Translator.translate("TOLevel." + level.name())) ||
                levelStr.equals(Translator.translateExplicitLocale("TOLevel." + level.name(), Locale.ENGLISH))) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown level: " + levelStr);
    }

    private TechnicalOfficial.Role findEnumValueForTranslatedRole(String roleStr) {
        for (TechnicalOfficial.Role role : TechnicalOfficial.Role.values()) {
            if (roleStr.equals(role.name()) ||
                roleStr.equals(Translator.translate("AccreditationRole." + role.name())) ||
                roleStr.equals(Translator.translateExplicitLocale("AccreditationRole." + role.name(), Locale.ENGLISH))) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + roleStr);
    }

    /**
     * Parse boolean value, defaulting to TRUE if empty/missing.
     * Returns false only if explicitly set to FALSE/NO/N/0.
     */
    private boolean parseBooleanValueDefaultTrue(String value) {
        if (value == null || value.isBlank()) {
            return true; // Default to active when importing
        }
        String normalized = value.trim().toLowerCase();
        // Return false only if explicitly false
        if (normalized.equals("false") || 
            normalized.equals("no") || 
            normalized.equals("n") || 
            normalized.equals("0") ||
            normalized.equals(Translator.translate("No").toLowerCase()) ||
            normalized.equals(Translator.translateExplicitLocale("No", Locale.ENGLISH).toLowerCase())) {
            return false;
        }
        return true; // Default to true for any other value
    }

    private TeamRole findEnumValueForTranslatedTeamRole(String roleStr) {
        // Try exact enum name match first
        for (TeamRole teamRole : TeamRole.values()) {
            if (roleStr.equalsIgnoreCase(teamRole.name())) {
                return teamRole;
            }
        }
        // Try translation key match (e.g., "Referee", "Marshall")
        for (TeamRole teamRole : TeamRole.values()) {
            if (roleStr.equalsIgnoreCase(teamRole.getTranslationKey())) {
                return teamRole;
            }
        }
        // Try translated value match (current locale and English)
        for (TeamRole teamRole : TeamRole.values()) {
            String translatedCurrent = Translator.translate(teamRole.getTranslationKey());
            String translatedEnglish = Translator.translateExplicitLocale(teamRole.getTranslationKey(), Locale.ENGLISH);
            if (roleStr.equals(translatedCurrent) || roleStr.equals(translatedEnglish)) {
                return teamRole;
            }
        }
        // Not found
        return null;
    }
}
