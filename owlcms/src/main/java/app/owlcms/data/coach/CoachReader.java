package app.owlcms.data.coach;

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
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class CoachReader {

    @SuppressWarnings("unused")
    private final static Logger logger = (Logger) LoggerFactory.getLogger(CoachReader.class);
    public static final String LAST_NAME = "LastName";
    public static final String FIRST_NAME = "FirstName";
    public static final String MEMBERSHIP_ID = "Membership";
    public static final String TEAM = "Team";

    public List<Coach> importFromXLS(InputStream is, StringBuilder errors) {
        List<Coach> coaches = new ArrayList<>();
        logger.setLevel(Level.TRACE);
        if (is != null) {
            JPAService.runInTransaction((em) -> {
                try {
                    // Delete existing coaches
                    logger.trace("Starting coach import - deleting existing coaches");
                    CoachRepository.deleteAll(em);
                    logger.trace("Existing coaches deleted, reading workbook");
                    Workbook workbook = WorkbookFactory.create(is);
                    Sheet sheet = workbook.getSheetAt(0);
                    logger.trace("Processing sheet with {} rows", sheet.getLastRowNum() + 1);
                    Row headerRow = sheet.getRow(0);
                    int[] colIndices = findColumnIndices(headerRow, errors);
                    logger.trace("Column indices - lastName:{}, firstName:{}, membershipId:{}, team:{}",
                        colIndices[0], colIndices[1], colIndices[2], colIndices[3]);
                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) {
                            break;
                        }
                        try {
                            Coach coach = readRow(row, colIndices);
                            if (coach != null) {
                                logger.trace("Read coach from row {}: lastName='{}', firstName='{}', membershipId='{}', team='{}'",
                                    i + 1, coach.getLastName(), coach.getFirstName(), coach.getMembershipId(), coach.getTeam());
                                Coach merged = em.merge(coach);
                                logger.trace("Merged coach: id={}, lastName='{}', firstName='{}', membershipId='{}', team='{}'",
                                    merged.getId(), merged.getLastName(), merged.getFirstName(), merged.getMembershipId(), merged.getTeam());
                                coaches.add(merged);
                            } else {
                                logger.trace("Row {} returned null coach, stopping import", i + 1);
                                break;
                            }
                        } catch (IllegalArgumentException ex) {
                            errors.append(ex.getMessage()).append("\n");
                        } catch (Exception ex) {
                            errors.append("Error processing row " + (i + 1) + ": " + ex.getMessage() + "\n")
                                    .append(ex.toString()).append("\n");
                        }
                    }
                    workbook.close();
                    logger.trace("Coach import completed, {} coaches imported", coaches.size());
                } catch (IOException e) {
                    logger.trace("File reading error during coach import: {}", e.getMessage());
                    errors.append("File reading error: " + e.getMessage() + "\n").append(e.toString()).append("\n");
                }
                return null;
            });
        }
        logger.trace("Returning {} coaches from import", coaches.size());
        return coaches;
    }

    private int[] findColumnIndices(Row headerRow, StringBuilder errors) {
        int[] indices = new int[]{-1, -1, -1, -1}; // lastName, firstName, membershipId, team - initialize to -1
        Map<String, String> headerMap = new HashMap<>();
        List<String> unmatchedHeaders = new ArrayList<>();
        List<String> matchedHeaders = new ArrayList<>();

        // Use canonical translation keys
        putTranslated(headerMap, "LastName", LAST_NAME);
        putTranslated(headerMap, "FirstName", FIRST_NAME);
        putTranslated(headerMap, "Scoreboard.Team", TEAM);
        putTranslated(headerMap, "Membership", MEMBERSHIP_ID);

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
                    // lastName(0), firstName(1), membershipId(2), team(3)
                    case MEMBERSHIP_ID:
                        indices[2] = colIndex;
                        break;
                    case TEAM:
                        indices[3] = colIndex;
                        break;
                }
            } else {
                unmatchedHeaders.add(header);
            }
        }

        // Report unmatched headers as warnings
        if (!unmatchedHeaders.isEmpty()) {
            String warning = "Warning: Unmatched headers in coach file: " + String.join(", ", unmatchedHeaders);
            logger.warn(warning);
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
            String error = "Missing required columns in coach file: " + String.join(", ", missingColumns);
            logger.error(error);
            if (errors != null) {
                errors.append(error).append("\n");
            }
            throw new IllegalArgumentException(error);
        }

        return indices;
    }

    private Coach readRow(Row row, int[] colIndices) {
        Cell currentCell = colIndices[0] >= 0 ? row.getCell(colIndices[0]) : null;
        try {
            if (isEmptyCell(currentCell)) {
                logger.trace("Empty cell at {}, stopping row processing", getCellAddress(currentCell));
                return null;
            }
            String lastName = colIndices[0] >= 0 ? getCellValueAsString(currentCell) : "";
            String firstName = colIndices[1] >= 0 ? getCellValueAsString(row.getCell(colIndices[1])) : "";
            String membershipId = colIndices[2] >= 0 ? getCellValueAsString(row.getCell(colIndices[2])) : "";
            String team = colIndices[3] >= 0 ? getCellValueAsString(row.getCell(colIndices[3])) : "";

            logger.trace("Creating coach object: lastName='{}', firstName='{}', membershipId='{}', team='{}'",
                lastName, firstName, membershipId, team);
            return new Coach(lastName, firstName, membershipId, team);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error processing cell " + getCellAddress(currentCell) + ": " + e.getMessage());
        }
    }

    // Helper method to convert a cell's position to columnLetter-rowNumber format
    private String getCellAddress(Cell cell) {
        if (cell == null)
            return "unknown";
        int col = cell.getColumnIndex();
        StringBuilder colLetter = new StringBuilder();
        while (col >= 0) {
            colLetter.insert(0, (char) ('A' + (col % 26)));
            col = (col / 26) - 1;
        }
        int rowNum = cell.getRowIndex() + 1; // Excel rows are 1-indexed
        return colLetter.toString() + rowNum;
    }

    private boolean isEmptyCell(Cell cell) {
        if (cell == null)
            return true;
        if (cell.getCellType() == CellType.BLANK)
            return true;
        if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty())
            return true;
        return false;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            default:
                return "";
        }
    }

    // Helper to translate a canonical key (English/current-locale) and register the translation in headerMap.
    private void putTranslated(Map<String, String> headerMap, String canonicalKey, String constant) {
        try {
            String tEng = Translator.translateExplicitLocale(canonicalKey, Locale.ENGLISH);
            if (tEng != null && !tEng.isBlank()) {
                headerMap.put(tEng, constant);
            }
        } catch (Exception ex) {
            // ignore
        }
        try {
            String tCur = Translator.translate(canonicalKey);
            if (tCur != null && !tCur.isBlank()) {
                headerMap.put(tCur, constant);
            }
        } catch (Exception ex) {
            // ignore
        }
    }
}
