/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.records;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.records.RecordEvent.MissingAgeGroup;
import app.owlcms.data.records.RecordEvent.MissingGender;
import app.owlcms.data.records.RecordEvent.UnknownIWFBodyWeightCategory;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.ZipUtils;
import ch.qos.logback.classic.Logger;

/**
 * Read records from an Excel file.
 *
 * Records for snatch, clean&jerk and total are read. All available tabs are scanned. Reading stops at first empty line. Header line is skipped.
 *
 * @author Jean-François Lamy
 *
 */
public class RecordDefinitionReader {

	private final static Logger logger = (Logger) LoggerFactory.getLogger(RecordDefinitionReader.class);
	private final static Logger startupLogger = Main.getStartupLogger();

	public static List<String> createRecords(Workbook workbook, String name, String baseName) {
		cleanUp(baseName);

		return JPAService.runInTransaction(em -> {
			int iRecord = 0;
			List<String> errors = new ArrayList<>();

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
					boolean error = false;
					for (Cell cell : row) {
						try {
							int iColumn = cell.getAddress().getColumn();

							logger.debug("[" + sheet.getSheetName() + "," + cell.getAddress() + "]");
							switch (iColumn) {
                                case 0: // A
                                    RecordEventSetters.setFederation(rec, cell.getStringCellValue());
                                    break;
                                case 1: // B
                                    RecordEventSetters.setRecordName(rec, cell.getStringCellValue());
                                    break;
                                case 2: // C
                                    RecordEventSetters.setAgeGroup(rec, cell.getStringCellValue());
                                    break;
                                case 3: // D
                                    RecordEventSetters.setGender(rec, cell.getStringCellValue());
                                    break;
                                case 4: // E
                                    RecordEventSetters.setAgeLower(rec, cell.getNumericCellValue());
                                    break;
                                case 5: // F
                                    RecordEventSetters.setAgeUpper(rec, cell.getNumericCellValue());
                                    break;
                                case 6: // G
                                    RecordEventSetters.setBwLower(rec, cell.getNumericCellValue());
                                    break;
                                case 7: // H
                                    RecordEventSetters.setBwUpper(rec, cell.getStringCellValue(), cell.getCellType());
                                    break;
                                case 8: // I
                                    RecordEventSetters.setRecordLift(rec, cell.getStringCellValue());
                                    break;
                                case 9: // J
                                    RecordEventSetters.setRecordValue(rec, cell.getNumericCellValue());
                                    break;
                                case 10: // K
                                    RecordEventSetters.setAthleteName(rec, cell.getStringCellValue());
                                    break;
                                case 11: // L
                                    RecordEventSetters.setBirthDate(rec, cell.getStringCellValue(), cell.getCellType());
                                    break;
                                case 12: // M
                                    RecordEventSetters.setNation(rec, cell.getStringCellValue());
                                    break;
                                case 13: // N
                                    RecordEventSetters.setRecordDate(rec, cell.getStringCellValue(), cell.getCellType());
                                    break;
                                case 14: // O
                                    RecordEventSetters.setEventLocation(rec, cell.getStringCellValue());
                                    break;
                                case 15: // P is used for new records
                                    break;
                                case 16: // Q
                                    RecordEventSetters.setEvent(rec, cell.getStringCellValue(), cell.getCellType());
                                    break;
                            }

							iColumn++;
						} catch (Exception e) {
							// do not report errors on empty rows
							if (!isEmptyRow(rec)) {
								startupLogger.error("{}[{}] {} ", sheet.getSheetName(), cell.getAddress(),
								        e.getMessage());
								logger.error("{}[{}] {} ", sheet.getSheetName(), cell.getAddress(), e.getMessage());
								errors.add(MessageFormat.format("{0}[{1}] {2} ", sheet.getSheetName(),
								        cell.getAddress(), e.getMessage()));
								error = true;
							}
						}
					}

					if (!error && !isEmptyRow(rec)) {
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
			startupLogger.info("inserted {} record entries.", iRecord);
			logger.info("inserted {} record entries.", iRecord);
			errors.add(Translator.translate("Records.Inserted", iRecord));
			return errors;
		});
	}

	public static void loadRecords() {
		Path recordsPath;
		try {
			recordsPath = ResourceWalker.getFileOrResourcePath("/records");
			try {
				if (recordsPath != null && Files.exists(recordsPath)) {
					RecordDefinitionReader.readFolder(recordsPath);
				} else {
					logger.info("no record definition files in local/records");
				}
			} catch (IOException e) {
				logger.error("cannot process records {}");
			}
		} catch (FileNotFoundException e1) {
			logger.error("cannot find records {}", LoggerUtils.stackTrace(e1));
		}

	}

	public static void readFolder(Path recordsPath) throws IOException {
		if (recordsPath == null || !Files.exists(recordsPath)) {
			return;
		}
		Files.walk(recordsPath).filter(f -> f.toString().endsWith(".xls") || f.toString().endsWith(".xlsx"))
		        .forEach(f -> {
			        InputStream is;
			        String fileName = f.getFileName().toString();
			        try {
				        is = Files.newInputStream(f);
				        readInputStream(is, fileName);
			        } catch (IOException e1) {
				        logger.error("could not open record definition file {}\n{}", fileName,
				                LoggerUtils./**/stackTrace(e1));
				        startupLogger.error("could not open record definition file {}.  See log files for details.",
				                fileName);
			        }

		        });

	}

	public static List<String> readInputStream(InputStream is, String fileName) {
		List<String> errors = new ArrayList<>();
		try (Workbook workbook = WorkbookFactory.create(is)) {
			logger.info("loading record definition file {} {}", fileName,
			        FilenameUtils.removeExtension(fileName));
			startupLogger.info("loading record definition file {}", fileName);

			errors = createRecords(workbook, fileName,
			        FilenameUtils.removeExtension(fileName.toString()));
			return errors;
		} catch (Exception e) {
			logger.error("could not process record definition file {}\n{}", fileName,
			        LoggerUtils./**/stackTrace(e));
			startupLogger.error(
			        "could not process record definition file {}. See log files for details.",
			        fileName);
			errors.add(Translator.translate("Records.couldNotProcess", fileName));
			return errors;
		}
	}

	public static void readZip(InputStream source) throws IOException {
		// so that each workbook does not close the zip stream
		final ZipUtils.NoCloseInputStream zipStream = new ZipUtils.NoCloseInputStream(source);
		RecordRepository.clearLoadedRecords();

		ZipEntry nextEntry;
		while ((nextEntry = zipStream.getNextEntry()) != null) {
			String name = nextEntry.getName();
			if (!name.endsWith("/")) {
				Logger startupLogger = (Logger) LoggerFactory.getLogger(Main.class);
				logger.info("unzipping {}", name);
				startupLogger.info("unzipping {}", name);
				// read the current zip entry
				try (Workbook workbook = WorkbookFactory.create(zipStream)) {
					String fileName = FilenameUtils.removeExtension(name);
					logger.info("loading record definition file {} {}", name, fileName);
					startupLogger.info("loading record definition file {}", name);
					createRecords(workbook, name, fileName);
				} catch (Exception e) {
					logger.error("could not process record definition file {}\n{}", name,
					        LoggerUtils./**/stackTrace(e));
					startupLogger.error("could not process record definition file {}. See log files for details.",
					        name);
				}
			}
		}
		zipStream.doClose(); // a real close
	}

	public static void resetRecords() {
		Path recordsPath;
		try {
			recordsPath = ResourceWalker.getFileOrResourcePath("/records");
			try {
				RecordRepository.clearLoadedRecords();
				if (recordsPath != null && Files.exists(recordsPath)) {
					RecordDefinitionReader.readFolder(recordsPath);
				} else {
					logger.info("no record definition files in local/records");
				}
			} catch (IOException e) {
				logger.error("cannot process records {}");
			}
		} catch (FileNotFoundException e1) {
			logger.error("cannot find records {}", LoggerUtils.stackTrace(e1));
		}
	}

	private static void cleanUp(String fileName) {
		logger.info("removing records originally from {}", fileName);
		RecordRepository.clearRecordsOriginallyFromFile(fileName);

	}

	private static boolean isEmptyRow(RecordEvent rec) {
		return rec.getRecordFederation() == null || rec.getRecordFederation().isBlank();
	}

}
