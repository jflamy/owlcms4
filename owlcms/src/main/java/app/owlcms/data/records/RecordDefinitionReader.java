/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
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
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.ZipUtils;
import ch.qos.logback.classic.Logger;

/**
 * Read records from an Excel file.
 *
 * Records for snatch, clean&jerk and total are read. All available tabs are
 * scanned. Reading stops at first empty line. Header line is skipped.
 *
 * @author Jean-François Lamy
 *
 */
public class RecordDefinitionReader {

	private final static Logger logger = (Logger) LoggerFactory.getLogger(RecordDefinitionReader.class);
	private final static Logger startupLogger = Main.getStartupLogger();

	public static List<String> createRecords(Workbook workbook, String name, String baseName) {
		cleanUp(baseName);
		DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
		DateTimeFormatter yFormatter = DateTimeFormatter.ofPattern("yyyy");

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
								if (rec.getAgeGrpUpper() < rec.getAgeGrpLower()) {
									throw new Exception(cellValue
									        + " upper limit on age category should be >= to "+rec.getAgeGrpLower());
									
								}
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
										if (cellValue.startsWith(">") || cellValue.startsWith("+")) {
											rec.setBwCatUpper(999);
											rec.setBwCatString(">"+rec.getBwCatLower());
										} else {
											rec.setBwCatUpper(Integer.parseInt(cellValue));
										}
										
									} catch (NumberFormatException e) {
										if (cellValue != null && !cellValue.isBlank()) {
											startupLogger
											        .error("[" + sheet.getSheetName() + "," + cell.getAddress() + "]");
											logger.error("[" + sheet.getSheetName() + "," + cell.getAddress() + "]");
										}
									}
									logger.warn("normal {} {} {}", iRecord, rec.getBwCatUpper(), rec.getBwCatLower());
									if (rec.getBwCatUpper() < rec.getBwCatLower()) {
										throw new Exception(cellValue
										        + " upper limit on bodyweight category should be >= to "+rec.getAgeGrpLower());
										
									}
								} catch (IllegalStateException e) {
									long cellValue = Math.round(cell.getNumericCellValue());
									rec.setBwCatString(Long.toString(cellValue));
									rec.setBwCatUpper(Math.toIntExact(cellValue));
									logger.warn("illegalstate {} {} {}", iRecord, rec.getBwCatUpper(), rec.getBwCatLower());
									if (rec.getBwCatUpper() <= rec.getBwCatLower()) {
										throw new Exception(cellValue
										        + " upper limit on bodyweight category should be > to "+rec.getBwCatLower());
									}
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
								if (cell.getCellType() == CellType.NUMERIC) {
									long cellValue = Math.round(cell.getNumericCellValue());
									int intExact = Math.toIntExact(cellValue);
									if (cellValue < 3000) {
										rec.setBirthYear(intExact);
										logger.warn("number {}", intExact);
									} else {
										LocalDate epoch = LocalDate.of(1900, 1, 1);
										LocalDate plusDays = epoch.plusDays(intExact - 2);
										// Excel quirks: 1 is 1900-01-01 and mistakenly assumes 1900-02-29 existed
										rec.setBirthDate(plusDays);
										logger.warn("plusDays {}", rec.getRecordDateAsString());
									}
								} else if (cell.getCellType() == CellType.STRING) {
									String cellValue = cell.getStringCellValue();
									logger.warn("string value = '{}'", cellValue);
									try {
										LocalDate date = LocalDate.parse(cellValue, ymdFormatter);
										rec.setBirthDate(date);
										logger.warn("date {}", date);
									} catch (DateTimeParseException e) {
										try {
											YearMonth date = YearMonth.parse(cellValue, ymFormatter);
											rec.setBirthYear(date.getYear());
											logger.warn("datemonth {}", date.getYear());
										} catch (DateTimeParseException e2) {
											try {
												Year date = Year.parse(cellValue, yFormatter);
												rec.setBirthYear(date.getValue());
												logger.warn("year {}", date.getValue());
											} catch (DateTimeParseException e3) {
												logger.error(cellValue
												        + " not in yyyy-MM-dd or yyyy-MM or yyyy date format");
												throw new Exception(cellValue
												        + " not in yyyy-MM-dd or yyyy-MM or yyyy date format");
											}
										}
									}
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
								if (cell.getCellType() == CellType.NUMERIC) {
									long cellValue = Math.round(cell.getNumericCellValue());
									int intExact = Math.toIntExact(cellValue);
									if (cellValue < 3000) {
										rec.setRecordYear(intExact);
										logger.warn("number {}", intExact);
									} else {
										LocalDate epoch = LocalDate.of(1900, 1, 1);
										LocalDate plusDays = epoch.plusDays(intExact - 2);
										// Excel quirks: 1 is 1900-01-01 and mistakenly assumes 1900-02-29 existed
										rec.setRecordDate(plusDays);
										logger.warn("plusDays {}", rec.getRecordDateAsString());
									}
								} else if (cell.getCellType() == CellType.STRING) {
									String cellValue = cell.getStringCellValue();
									logger.warn("string value = '{}'", cellValue);
									try {
										LocalDate date = LocalDate.parse(cellValue, ymdFormatter);
										rec.setRecordDate(date);
										logger.warn("date {}", date);
									} catch (DateTimeParseException e) {
										try {
											YearMonth date = YearMonth.parse(cellValue, ymFormatter);
											rec.setRecordYear(date.getYear());
											logger.warn("datemonth {}", date.getYear());
										} catch (DateTimeParseException e2) {
											try {
												Year date = Year.parse(cellValue, yFormatter);
												rec.setRecordYear(date.getValue());
												logger.warn("year {}", date.getValue());
											} catch (DateTimeParseException e3) {
												logger.error(cellValue
												        + " not in yyyy-MM-dd or yyyy-MM or yyyy date format");
												throw new Exception(cellValue
												        + " not in yyyy-MM-dd or yyyy-MM or yyyy date format");
											}
										}
									}

								}
								break;
							}

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
			errors.add(Integer.toString(iRecord));
			return errors;
		});
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

	public static void readInputStream(InputStream is, String fileName) {
		try (Workbook workbook = WorkbookFactory.create(is)) {
			logger.info("loading record definition file {} {}", fileName,
			        FilenameUtils.removeExtension(fileName));
			startupLogger.info("loading record definition file {}", fileName);

			createRecords(workbook, fileName,
			        FilenameUtils.removeExtension(fileName.toString()));
		} catch (Exception e) {
			logger.error("could not process record definition file {}\n{}", fileName,
			        LoggerUtils./**/stackTrace(e));
			startupLogger.error(
			        "could not process record definition file {}. See log files for details.",
			        fileName);
		}
	}

	private static void cleanUp(String fileName) {
		logger.info("removing records originally from {}", fileName);
		RecordRepository.clearRecordsOriginallyFromFile(fileName);

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

	private static boolean isEmptyRow(RecordEvent rec) {
		return rec.getRecordFederation() == null || rec.getRecordFederation().isBlank();
	}

}
