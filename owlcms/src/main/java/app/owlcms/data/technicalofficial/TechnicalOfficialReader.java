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
                    int[] colIndices = findColumnIndices(headerRow);
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

    private int[] findColumnIndices(Row headerRow) {
        int[] indices = new int[6];  // One for each field
        Map<String, String> headerMap = new HashMap<>();
        
        // Map constants to themselves (legacy support)
        headerMap.put(LAST_NAME, LAST_NAME);
        headerMap.put(FIRST_NAME, FIRST_NAME);
        headerMap.put(LEVEL, LEVEL);
        headerMap.put(IWF_ID, IWF_ID);
        headerMap.put(FEDERATION, FEDERATION);
        headerMap.put(FEDERATION_ID, FEDERATION_ID);
        
        // Map English translations to constants (always accept English)
        headerMap.put(Translator.translate("TechnicalOfficial.LastName", Locale.ENGLISH), LAST_NAME);
        headerMap.put(Translator.translate("TechnicalOfficial.FirstName", Locale.ENGLISH), FIRST_NAME);
        headerMap.put(Translator.translate("TechnicalOfficial.Level", Locale.ENGLISH), LEVEL);
        headerMap.put(Translator.translate("TechnicalOfficial.IWFId", Locale.ENGLISH), IWF_ID);
        headerMap.put(Translator.translate("TechnicalOfficial.Federation", Locale.ENGLISH), FEDERATION);
        headerMap.put(Translator.translate("TechnicalOfficial.FederationId", Locale.ENGLISH), FEDERATION_ID);
        
        // Map local translations to constants
        headerMap.put(Translator.translate("TechnicalOfficial.LastName"), LAST_NAME);
        headerMap.put(Translator.translate("TechnicalOfficial.FirstName"), FIRST_NAME);
        headerMap.put(Translator.translate("TechnicalOfficial.Level"), LEVEL);
        headerMap.put(Translator.translate("TechnicalOfficial.IWFId"), IWF_ID);
        headerMap.put(Translator.translate("TechnicalOfficial.Federation"), FEDERATION);
        headerMap.put(Translator.translate("TechnicalOfficial.FederationId"), FEDERATION_ID);
        
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
                }
            }
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

            return new TechnicalOfficial(lastName, firstName, level, iwfId, federation, federationId);
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
                levelStr.equals(Translator.translate("TOLevel." + level.name(), Locale.ENGLISH))) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown level: " + levelStr);
    }
}
