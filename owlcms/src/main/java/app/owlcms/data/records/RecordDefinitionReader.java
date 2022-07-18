/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.records;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.records.RecordEvent.MissingAgeGroup;
import app.owlcms.data.records.RecordEvent.MissingGender;
import app.owlcms.data.records.RecordEvent.UnknownIWFBodyWeightCategory;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ZipUtils;
import ch.qos.logback.classic.Logger;

/**
 * Read records from an Excel file.
 *
 * Records for snatch, clean&jerk and total are read. All available tabs are scanned. Reading stops at first empty line.
 * Header line is skipped.
 *
 * @author Jean-François Lamy
 *
 */
public class RecordDefinitionReader {

    private final static Logger logger = (Logger) LoggerFactory.getLogger(RecordDefinitionReader.class);

    public static int createRecords(Workbook workbook, String name, String baseName) {

        return JPAService.runInTransaction(em -> {
            int iRecord = 0;

            for (Sheet sheet : workbook) {
                processSheet: for (Row row : sheet) {
                    int iRow = row.getRowNum();
                    if (iRow == 0) {
                        iRow++;
                        continue;
                    }

                    RecordEvent rec = new RecordEvent();
                    rec.setFileName(baseName);

                    // beware: on a truly empty row we will not enter this loop.
                    for (Cell cell : row) {
                        int iColumn = cell.getAddress().getColumn();

                        // logger.debug("[" + sheet.getSheetName() + "," + cell.getAddress() + "]");
                        switch (iColumn) {
                        case 0: { // A
                            String cellValue = cell.getStringCellValue();
                            String trim = cellValue.trim();
                            if (trim.isEmpty()) {
                                // stop processing sheet on first row with an empty first cell
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
                            try {
                                String cellValue = cell.getStringCellValue();
                                rec.setBwCatString(cellValue);
                                try {
                                    rec.setBwCatUpper(cellValue.startsWith(">") ? 999 : Integer.parseInt(cellValue));
                                } catch (NumberFormatException e) {
                                    if (cellValue != null && !cellValue.isBlank()) {
                                        logger.error("[" + sheet.getSheetName() + "," + cell.getAddress() + "]");
                                    }
                                }
                            } catch (IllegalStateException e) {
                                long cellValue = Math.round(cell.getNumericCellValue());
                                rec.setBwCatString(Long.toString(cellValue));
                                rec.setBwCatUpper(Math.toIntExact(cellValue));
                            }
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
                    }

                    if (rec.getRecordFederation() != null) {
                        // if row was empty, we get no cells but rec was created.
                        try {
                            rec.fillDefaults();
                        } catch (MissingAgeGroup | MissingGender | UnknownIWFBodyWeightCategory e1) {
                            throw new RuntimeException(e1 + " row " + iRow);
                        }

                        try {
                            em.persist(rec);
                            iRecord++;
                        } catch (Exception e) {
                            logger.error("could not persist RecordEvent {}", LoggerUtils./**/stackTrace(e));
                        }
                    }
                }
            }
            Competition comp = Competition.getCurrent();
            Competition comp2 = em.contains(comp) ? comp : em.merge(comp);
            comp2.setAgeGroupsFileName(name);
            logger.info("inserted {} record entries.", iRecord);
            return iRecord;
        });
    }

    public static void readZip(InputStream source) throws IOException {
        // so that each workbook does not close the zip stream
        final ZipUtils.NoCloseInputStream zipStream = new ZipUtils.NoCloseInputStream(source);
        RecordRepository.clearRecords();

        ZipEntry nextEntry;
        while ((nextEntry = zipStream.getNextEntry()) != null) {
            String name = nextEntry.getName();
            if (!name.endsWith("/")) {
                Logger mainLogger = (Logger) LoggerFactory.getLogger(Main.class);
                logger.info("unzipping {}", name);
                mainLogger.info("unzipping {}", name);
                // read the current zip entry
                try (Workbook workbook = WorkbookFactory.create(zipStream)) {
                    logger.info("loading record definition file {} {}", name, FilenameUtils.removeExtension(name));
                    mainLogger.info("loading record definition file {}", name);
                    createRecords(workbook, name, FilenameUtils.removeExtension(name));
                } catch (Exception e) {
                    logger.error("could not process record definition file {}\n{}", name, LoggerUtils./**/stackTrace(e));
                    mainLogger.error("could not process record definition file {}. See log files for details.", name);
                }
            }
        }
        zipStream.doClose(); // a real close
    }

    public static void readFolder(Path recordsPath) throws IOException {
        if (recordsPath == null || !Files.exists(recordsPath)) {
            return;
        }
        Files.walk(recordsPath).filter(f -> f.toString().endsWith(".xls") || f.toString().endsWith(".xlsx"))
                .forEach(f -> {
                    InputStream is;
                    Logger mainLogger = Main.getStartupLogger();
                    try {
                        is = Files.newInputStream(f);
                        try (Workbook workbook = WorkbookFactory.create(is)) {
                            logger.info("loading record definition file {} {}", f.toString(), FilenameUtils.removeExtension(f.getFileName().toString()));
                            mainLogger.info("loading record definition file {}", f.toString());
                            createRecords(workbook, f.toString(), FilenameUtils.removeExtension(f.getFileName().toString()));
                        } catch (Exception e) {
                            logger.error("could not process record definition file {}\n{}", f.toString(), LoggerUtils./**/stackTrace(e));
                            mainLogger.error("could not process record definition file {}. See log files for details.", f.toString());
                        }
                    } catch (IOException e1) {
                        logger.error("could not open record definition file {}\n{}", f.toString(), LoggerUtils./**/stackTrace(e1));
                        mainLogger.error("could not open record definition file {}.  See log files for details.", f.toString());
                    }

                });

    }

}
