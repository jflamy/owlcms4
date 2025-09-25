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
import ch.qos.logback.classic.Logger;

public class CoachReader {

    @SuppressWarnings("unused")
    private final static Logger logger = (Logger) LoggerFactory.getLogger(CoachReader.class);
    private static final String LAST_NAME = "LastName";
    private static final String FIRST_NAME = "FirstName";
    // LEVEL and IWF_ID removed for Coach
    private static final String MEMBERSHIP_ID = "MembershipId";
    private static final String TEAM = "Team";

    public List<Coach> importFromXLS(InputStream is, StringBuilder errors) {
        List<Coach> coaches = new ArrayList<>();
        if (is != null) {
            JPAService.runInTransaction((em) -> {
                try {
                    // Delete existing coaches
                    CoachRepository.deleteAll(em);
                    Workbook workbook = WorkbookFactory.create(is);
                    Sheet sheet = workbook.getSheetAt(0);
                    Row headerRow = sheet.getRow(0);
                    int[] colIndices = findColumnIndices(headerRow);
                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) {
                            break;
                        }
                        try {
                            Coach coach = readRow(row, colIndices);
                            if (coach != null) {
                                Coach merged = em.merge(coach);
                                coaches.add(merged);
                            } else {
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
                } catch (IOException e) {
                    errors.append("File reading error: " + e.getMessage() + "\n").append(e.toString()).append("\n");
                }
                return null;
            });
        }
        return coaches;
    }

    private int[] findColumnIndices(Row headerRow) {
        int[] indices = new int[4]; // lastName, firstName, membershipId, team
        Map<String, String> headerMap = new HashMap<>();

        // Use athlete canonical translation keys
        putTranslated(headerMap, "LastName", LAST_NAME);
        putTranslated(headerMap, "FirstName", FIRST_NAME);
        putTranslated(headerMap, "Scoreboard.Team", TEAM);
        putTranslated(headerMap, "Registration.FederationCodesShort", MEMBERSHIP_ID);

        for (Cell cell : headerRow) {
            String header = cell.getStringCellValue().trim();
            int colIndex = cell.getColumnIndex();

            String constant = headerMap.get(header);
            if (constant != null) {
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
            }
        }

        return indices;
    }

    private Coach readRow(Row row, int[] colIndices) {
        Cell currentCell = colIndices[0] >= 0 ? row.getCell(colIndices[0]) : null;
        try {
            if (isEmptyCell(currentCell)) {
                return null;
            }
            String lastName = colIndices[0] >= 0 ? getCellValueAsString(currentCell) : "";
            String firstName = colIndices[1] >= 0 ? getCellValueAsString(row.getCell(colIndices[1])) : "";
            String membershipId = colIndices[2] >= 0 ? getCellValueAsString(row.getCell(colIndices[2])) : "";
            String team = colIndices[3] >= 0 ? getCellValueAsString(row.getCell(colIndices[3])) : "";

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
            String tEng = Translator.translate(canonicalKey, Locale.ENGLISH);
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
