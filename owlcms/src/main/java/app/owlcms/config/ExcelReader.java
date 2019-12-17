package app.owlcms.config;

import java.io.IOException;
import java.util.Iterator;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;

/**
 * Created by rajeevkumarsingh on 18/12/17.
 */

public class ExcelReader {
    public static final String SAMPLE_XLS_FILE_PATH = "./sample-xls-file.xls";
    public static final String SAMPLE_XLSX_FILE_PATH = "./sample-xlsx-file.xlsx";

    public static void main(String[] args) throws IOException, InvalidFormatException {

        // Creating a Workbook from an Excel file (.xls or .xlsx)
//        Workbook workbook = WorkbookFactory.create(new File(SAMPLE_XLSX_FILE_PATH));
        Workbook workbook = WorkbookFactory
                .create(ExcelReader.class.getResourceAsStream("/templates/registration/AgeGroups.xlsx"));

        // Retrieving the number of sheets in the Workbook
        System.out.println("Workbook has " + workbook.getNumberOfSheets() + " Sheets : ");

        /*
         * ============================================================= Iterating over
         * all the sheets in the workbook (Multiple ways)
         * =============================================================
         */

        // 1. You can obtain a sheetIterator and iterate over it
        Iterator<Sheet> sheetIterator = workbook.sheetIterator();
        System.out.println("Retrieving Sheets using Iterator");
        while (sheetIterator.hasNext()) {
            Sheet sheet = sheetIterator.next();
            System.out.println("=> " + sheet.getSheetName());
        }

        // 2. Or you can use a for-each loop
        System.out.println("Retrieving Sheets using for-each loop");
        for (Sheet sheet : workbook) {
            System.out.println("=> " + sheet.getSheetName());
        }

        // 3. Or you can use a Java 8 forEach wih lambda
        System.out.println("Retrieving Sheets using Java 8 forEach with lambda");
        workbook.forEach(sheet -> {
            System.out.println("=> " + sheet.getSheetName());
        });

        /*
         * ================================================================== Iterating
         * over all the rows and columns in a Sheet (Multiple ways)
         * ==================================================================
         */

        // Getting the Sheet at index zero
        Sheet sheet = workbook.getSheetAt(1);

        // Create a DataFormatter to format and get each cell's value as String
        DataFormatter dataFormatter = new DataFormatter();

        // 1. You can obtain a rowIterator and columnIterator and iterate over them
        System.out.println("\n\nIterating over Rows and Columns using Iterator\n");
        Iterator<Row> rowIterator = sheet.rowIterator();
        int iRow = 0;
        while (rowIterator.hasNext()) {
            int iColumn = 0;
            Row row;
            if (iRow == 0) {
                // process header
                row = rowIterator.next();
            }
            row = rowIterator.next();

            // Now let's iterate over the columns of the current row

            AgeGroup ag = new AgeGroup("FU20", true, 18, 20, Gender.F, AgeDivision.DEFAULT);

            Iterator<Cell> cellIterator = row.cellIterator();

            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                switch (iColumn) {
                case 0: {
                    String cellValue = dataFormatter.formatCellValue(cell);
                    ag.setCode(cellValue);
                }
                    break;
                case 1: {
                    String cellValue = dataFormatter.formatCellValue(cell);
                    ag.setAgeDivision(AgeDivision.getAgeDivisionFromCode(cellValue));
                }
                    break;
                case 2: {
                    String cellValue = dataFormatter.formatCellValue(cell);
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        ag.setGender(cellValue.contentEquals("F") ? Gender.F : Gender.M);
                    }
                }
                    break;
                case 3: {
                    long cellValue = Math.round(cell.getNumericCellValue());
                    ag.setMinAge(Math.toIntExact(cellValue));
                }
                    break;
                case 4: {
                    String cellValue = dataFormatter.formatCellValue(cell);
                    ag.setCode(cellValue);
                }
                    break;

                default:
                    break;
                }
                String cellValue = getCellValue(dataFormatter, cell);
                ag.setCode(cellValue);

            }
            System.out.println();
        }

        // Closing the workbook
        workbook.close();
    }

    private static String getCellValue(DataFormatter dataFormatter, Cell cell) {
        String cellValue;
        if (cell.getCellTypeEnum() == CellType.FORMULA) {
            switch (cell.getCachedFormulaResultTypeEnum()) {
            case STRING:
                cellValue = cell.getRichStringCellValue().toString();
                break;
            default:
                cellValue = dataFormatter.formatCellValue(cell);
            }
        } else {
            cellValue = dataFormatter.formatCellValue(cell);
        }
        return cellValue;
    }

    @SuppressWarnings("unused")
    private static void printCellValue(Cell cell) {
        switch (cell.getCellTypeEnum()) {
        case BOOLEAN:
            System.out.print(cell.getBooleanCellValue());
            break;
        case STRING:
            System.out.print(cell.getRichStringCellValue().getString());
            break;
        case NUMERIC:
            if (DateUtil.isCellDateFormatted(cell)) {
                System.out.print(cell.getDateCellValue());
            } else {
                System.out.print(cell.getNumericCellValue());
            }
            break;
        case FORMULA:
            System.out.print(cell.getCellFormula());
            break;
        case BLANK:
            System.out.print("");
            break;
        default:
            System.out.print("");
        }

        System.out.print("\t");
    }
}