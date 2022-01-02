/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.records;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.EnumSet;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

/**
 * Read lifted weight records from an Excel file.
 *
 * Competition records for snatch, clean&jerk and total are read. All available tabs are scanned. Reading stops at first
 * empty line. Header line is skipped.
 *
 * @author Jean-François Lamy
 *
 */
public class RecordDefinitionReader {

    private final static Logger logger = (Logger) LoggerFactory.getLogger(RecordDefinitionReader.class);

    public static int createRecords(Workbook workbook, EnumSet<AgeDivision> ageDivisionOverride,
            String localizedName) {

        return JPAService.runInTransaction(em -> {
            int iRecord = 0;

            for (Sheet sheet : workbook) {
                int iRow = 0;

                processsheet: for (Row row : sheet) {
                    int iColumn = 0;

                    if (iRow == 0) {
                        iRow++;
                        continue;
                    }

                    RecordEvent rec = new RecordEvent();

                    for (Cell cell : row) {
                        // logger.trace("[" + iSheet + "," + iRow + "," + iColumn + "]");
                        switch (iColumn) {
                        case 0: {
                            String cellValue = cell.getStringCellValue();
                            String trim = cellValue.trim();
                            if (trim.isEmpty()) {
                                break processsheet;
                            }
                            rec.setRecordFederation(trim);
                            break;
                        }

                        case 1: {
                            String cellValue = cell.getStringCellValue();
                            cellValue = cellValue != null ? cellValue.trim() : cellValue;
                            rec.setAgeGrp(cellValue);
                            break;
                        }

                        case 2: {
                            String cellValue = cell.getStringCellValue();
                            cellValue = cellValue != null ? cellValue.trim().toUpperCase() : cellValue;
                            rec.setGender(Gender.valueOf(cellValue));
                            break;
                        }

                        case 3: {
                            long cellValue = Math.round(cell.getNumericCellValue());
                            rec.setBwCatUpper(Math.toIntExact(cellValue));
                            break;
                        }

                        case 4: {
                            String cellValue = cell.getStringCellValue();
                            cellValue = cellValue != null ? cellValue.trim() : cellValue;
                            rec.setRecordKind(cellValue.substring(0, 1));
                            break;
                        }

                        case 5: {
                            long cellValue = Math.round(cell.getNumericCellValue());
                            rec.setRecordValue(Math.toIntExact(cellValue));
                            break;
                        }

                        case 6: {
                            String cellValue = cell.getStringCellValue();
                            cellValue = cellValue != null ? cellValue.trim() : cellValue;
                            rec.setAthleteName(cellValue);
                            break;
                        }

                        case 7: {
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

                        case 8: {
                            String cellValue = cell.getStringCellValue();
                            cellValue = cellValue != null ? cellValue.trim() : cellValue;
                            rec.setNation(cellValue);
                            break;
                        }

                        case 9: {
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
                    }
                    // logger.trace(rec);

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
            comp2.setAgeGroupsFileName(localizedName);

            return iRecord;
        });
    }

    static void doInsertRecords(EnumSet<AgeDivision> es, String localizedName) {
        InputStream localizedResourceAsStream = ResourceWalker.getResourceAsStream(localizedName);
        try (Workbook workbook = WorkbookFactory
                .create(localizedResourceAsStream)) {
            RecordRepository.logger.info("loading configuration file {}", localizedName);
            createRecords(workbook, es, localizedName);
            workbook.close();
        } catch (Exception e) {
            RecordRepository.logger.error("could not process ageGroup configuration\n{}",
                    LoggerUtils./**/stackTrace(e));
        }
    }

}
