/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.records;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.zip.ZipEntry;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.ZipUtils;
import ch.qos.logback.classic.Logger;

/**
 * Read records from an Excel file.
 *
 * Records for snatch, clean&jerk and total are read. All available tabs are scanned. Reading stops at first
 * empty line. Header line is skipped.
 *
 * @author Jean-François Lamy
 *
 */
public class RecordDefinitionReader {

    private final static Logger logger = (Logger) LoggerFactory.getLogger(RecordDefinitionReader.class);

    public static int createRecords(Workbook workbook, String name) {

        return JPAService.runInTransaction(em -> {
            int iRecord = 0;

            for (Sheet sheet : workbook) {
                

                processSheet: for (Row row : sheet) {
                    int iRow = row.getRowNum();
                    if (iRow == 0) {
//                        if (! row.getCell(0).getStringCellValue().equalsIgnoreCase("Federation")) {
//                            break processSheet;
//                        }
                        iRow++;
                        continue;
                    }

                    RecordEvent rec = new RecordEvent();

                    for (Cell cell : row) {
                        int iColumn = cell.getAddress().getColumn();
                        try {
                        //logger.debug("[" + sheet.getSheetName() + "," + cell.getAddress() + "]");
                        switch (iColumn) {
                        case 0: { // A
                            String cellValue = cell.getStringCellValue();
                            String trim = cellValue.trim();
                            if (trim.isEmpty()) {
                                break processSheet;
                            }
                            rec.setRecordFederation(trim);
                            break;
                        }
                        
                        case 1: { // B
                            String cellValue = cell.getStringCellValue();
                            cellValue = cellValue != null ? cellValue.trim() : cellValue;
                            rec.setRecordName(cellValue);
                            break;
                        }
                        

                        case 2: { // C
                            String cellValue = cell.getStringCellValue();
                            cellValue = cellValue != null ? cellValue.trim() : cellValue;
                            rec.setAgeGrp(cellValue);
                            break;
                        }
                        
                        case 3: { // D
                            String cellValue = cell.getStringCellValue();
                            cellValue = cellValue != null ? cellValue.trim().toUpperCase() : cellValue;
                            rec.setGender(Gender.valueOf(cellValue));
                            break;
                        }
                        
                        case 4: { // E
                            long cellValue = Math.round(cell.getNumericCellValue());
                            rec.setAgeGrpLower(Math.toIntExact(cellValue));
                            break;
                        }
                        
                        case 5: { // F
                            long cellValue = Math.round(cell.getNumericCellValue());
                            rec.setAgeGrpUpper(Math.toIntExact(cellValue));
                            break;
                        }

                        case 6: { // G
                            long cellValue = Math.round(cell.getNumericCellValue());
                            rec.setBwCatLower(Math.toIntExact(cellValue));
                            break;
                        }

                        case 7: { // H
                            long cellValue = Math.round(cell.getNumericCellValue());
                            rec.setBwCatUpper(Math.toIntExact(cellValue));
                            break;
                        }

                        case 8: { // I
                            String cellValue = cell.getStringCellValue();
                            cellValue = cellValue != null ? cellValue.trim() : cellValue;
                            rec.setRecordLift(cellValue);
                            break;
                        }

                        case 9: { // J
                            rec.setRecordValue(cell.getNumericCellValue());
                            break;
                        }

                        case 10: { // K
                            String cellValue = cell.getStringCellValue();
                            cellValue = cellValue != null ? cellValue.trim() : cellValue;
                            rec.setAthleteName(cellValue);
                            break;
                        }

                        case 11: { // L
                            long cellValue = Math.round(cell.getNumericCellValue());
                            int intExact = Math.toIntExact(cellValue);
                            if (cellValue < 3000) {
                                rec.setRecordYear(intExact);
                            } else {
                                LocalDate epoch = LocalDate.of(1900, 1, 1);
                                LocalDate plusDays = epoch.plusDays(intExact - 2);
                                // Excel quirks: 1 is 1900-01-01 and mistakenly assumes 1900-02-29 existed
                                rec.setRecordDate(plusDays);
                            }
                            break;
                        }

                        case 12: { // M
                            String cellValue = cell.getStringCellValue();
                            cellValue = cellValue != null ? cellValue.trim() : cellValue;
                            rec.setNation(cellValue);
                            break;
                        }

                        case 13: { // N
                            long cellValue = Math.round(cell.getNumericCellValue());
                            int intExact = Math.toIntExact(cellValue);
                            if (cellValue < 3000) {
                                rec.setRecordYear(intExact);
                            } else {
                                LocalDate epoch = LocalDate.of(1900, 1, 1);
                                LocalDate plusDays = epoch.plusDays(intExact - 2);
                                // Excel quirks: 1 is 1900-01-01 and mistakenly assumes 1900-02-29 existed
                                rec.setRecordDate(plusDays);
                            }
                            break;
                        }

                        }

                        iColumn++;
                        } catch (Exception e) {
                            logger.error("{} {}: \\n{}", sheet.getSheetName(), cell.getAddress(), LoggerUtils.stackTrace(e));
                            System.exit(-1);
                        }
                    }
                    //logger.debug("{}", rec);

                    try {
                        em.persist(rec);
                    } catch (Exception e) {
                        logger.error("could not persist RecordEvent {}", LoggerUtils./**/stackTrace(e));
                    }

                    iRow++;
                    iRecord++;
                }
            }
            Competition comp = Competition.getCurrent();
            Competition comp2 = em.contains(comp) ? comp : em.merge(comp);
            comp2.setAgeGroupsFileName(name);

            return iRecord;
        });
    }

    static void doInsertRecords(String localizedName) {
        InputStream localizedResourceAsStream = ResourceWalker.getResourceAsStream(localizedName);
        try (Workbook workbook = WorkbookFactory
                .create(localizedResourceAsStream)) {
            RecordRepository.logger.info("loading record definition file {}", localizedName);
            createRecords(workbook, localizedName);
        } catch (Exception e) {
            RecordRepository.logger.error("could not process record definition file {}\n{}",
                    LoggerUtils./**/stackTrace(e));
        }
    }
    
    public static void readZip(InputStream source) throws IOException {
        // so that each workbook does not close the zip stream
        final ZipUtils.NoCloseInputStream zipStream = new ZipUtils.NoCloseInputStream(source);
        
        ZipEntry nextEntry;
        while ((nextEntry = zipStream.getNextEntry()) != null) {
            String name = nextEntry.getName();
            if (!name.endsWith("/")) {
                logger.warn("unzipping {}", name);
                // read the current zip entry
                try (Workbook workbook = WorkbookFactory.create(zipStream)) {
                    RecordRepository.logger.info("loading record definition file {}", name);
                    createRecords(workbook, name);
                } catch (Exception e) {
                    RecordRepository.logger.error("could not process record definition file {}\n{}",
                            LoggerUtils./**/stackTrace(e));
                }
            }
        }
        zipStream.doClose(); // a real close
    }

}
