package app.owlcms.data.technicalofficial;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.LoggerFactory;

import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Logger;

public class TechnicalOfficialReader {

    private final static Logger logger = (Logger) LoggerFactory.getLogger(TechnicalOfficialReader.class);
    private static final String LAST_NAME = "LastName";
    private static final String FIRST_NAME = "FirstName";
    private static final String LEVEL = "Level";
    private static final String IWF_ID = "IWFId";
    private static final String FEDERATION = "Federation";
    private static final String FEDERATION_ID = "FederationId";

    public static List<TechnicalOfficial> importFromXLS(InputStream is) {
        List<TechnicalOfficial> officials = new ArrayList<>();
        
        if (is != null) {
            JPAService.runInTransaction((em) -> {
                try {
                    // Delete existing officials
                    TechnicalOfficialRepository.deleteAll(em);
                    
                    Workbook workbook = WorkbookFactory.create(is);
                    Sheet sheet = workbook.getSheetAt(0);
                    
                    // Get header and column indices
                    Row headerRow = sheet.getRow(0);
                    int[] colIndices = findColumnIndices(headerRow);
                    
                    // Process data rows
                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) continue;
                        
                        logger.trace("row[{}] {}", i, colIndices);
                        TechnicalOfficial official = readRow(row, colIndices);
                        
                        if (official != null) {
                            // add the official to the database.
                            TechnicalOfficial mergedOff = em.merge(official);
                            officials.add(mergedOff);
                        } else {
                            break;
                        }
                    }
                    workbook.close();
                } catch (Exception e) {
                    logger.error("Error reading technical officials: {}", e);
                    throw new RuntimeException(e);
                }
                return null;
            });
        }
        return officials;
    }

    private static int[] findColumnIndices(Row headerRow) {
        int[] indices = new int[6];  // One for each field
        for (Cell cell : headerRow) {
            String header = cell.getStringCellValue().trim();
            int colIndex = cell.getColumnIndex();
            
            switch (header) {
                case LAST_NAME: indices[0] = colIndex; break;
                case FIRST_NAME: indices[1] = colIndex; break;
                case LEVEL: indices[2] = colIndex; break;
                case IWF_ID: indices[3] = colIndex; break;
                case FEDERATION: indices[4] = colIndex; break;
                case FEDERATION_ID: indices[5] = colIndex; break;
            }
        }
        return indices;
    }

    private static TechnicalOfficial readRow(Row row, int[] colIndices) {
        // Check required fields
        if (isEmptyCell(row.getCell(colIndices[0]))) {
            return null;
        }

        String lastName = getCellValueAsString(row.getCell(colIndices[0]));
        String firstName = getCellValueAsString(row.getCell(colIndices[1]));
        TOLevel level = null;
        try {
            level = TOLevel.valueOf(getCellValueAsString(row.getCell(colIndices[2])));
        } catch (IllegalArgumentException e) {
        }
        String iwfId = getCellValueAsString(row.getCell(colIndices[3]));
        String federation = getCellValueAsString(row.getCell(colIndices[4]));
        String federationId = getCellValueAsString(row.getCell(colIndices[5]));

        return new TechnicalOfficial(lastName, firstName, level, iwfId, federation, federationId);
    }

    private static boolean isEmptyCell(Cell cell) {
        if (cell == null) return true;
        if (cell.getCellType() == CellType.BLANK) return true;
        if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty()) return true;
        return false;
    }

    private static String getCellValueAsString(Cell cell) {
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
