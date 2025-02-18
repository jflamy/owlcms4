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
                            errors.append("Row " + (row.getRowNum()+1) + ": " + ex.getMessage() + "\n")
                                  .append(ex.toString()).append("\n");
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
        Map<String, Integer> headerMap = new HashMap<>();
        
        for (Cell cell : headerRow) {
            String header = cell.getStringCellValue().trim();
            int colIndex = cell.getColumnIndex();

            headerMap.put(header, colIndex);

            String englishHeader = Translator.translate("TechnicalOfficial."+header, Locale.ENGLISH);
            headerMap.put(englishHeader, colIndex);
            
            String translatedHeader = Translator.translate("TechnicalOfficial."+header);
            headerMap.put(translatedHeader, colIndex);
        }
        
        indices[0] = headerMap.getOrDefault(LAST_NAME, -1);
        indices[1] = headerMap.getOrDefault(FIRST_NAME, -1);
        indices[2] = headerMap.getOrDefault(LEVEL, -1);
        indices[3] = headerMap.getOrDefault(IWF_ID, -1);
        indices[4] = headerMap.getOrDefault(FEDERATION, -1);
        indices[5] = headerMap.getOrDefault(FEDERATION_ID, -1);
        
        return indices;
    }

    private TechnicalOfficial readRow(Row row, int[] colIndices) {
        Cell currentCell = null;
        try {
            currentCell = row.getCell(colIndices[0]);
            if (isEmptyCell(currentCell)) {
                return null;
            }
            String lastName = getCellValueAsString(currentCell);
            
            currentCell = row.getCell(colIndices[1]);
            String firstName = getCellValueAsString(currentCell);
            
            currentCell = row.getCell(colIndices[2]);
            String levelStr = getCellValueAsString(currentCell);
            TOLevel level = (levelStr != null && !levelStr.isBlank() ? TOLevel.valueOf(levelStr) : null);
            
            currentCell = row.getCell(colIndices[3]);
            String iwfId = getCellValueAsString(currentCell);
            
            currentCell = row.getCell(colIndices[4]);
            String federation = getCellValueAsString(currentCell);
            
            currentCell = row.getCell(colIndices[5]);
            String federationId = getCellValueAsString(currentCell);
            
            return new TechnicalOfficial(lastName, firstName, level, iwfId, federation, federationId);
        } catch(Exception e) {
            throw new IllegalArgumentException("Error processing cell " + getCellAddress(currentCell) + ": " + e.getMessage(), e);
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
        return colLetter.toString() + "-" + rowNum;
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
}
