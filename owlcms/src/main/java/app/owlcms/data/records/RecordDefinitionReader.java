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
import java.util.Map;
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

	@FunctionalInterface
	private interface CellSetter {
		void set(RecordEvent rec, Cell cell) throws Exception;
	}

	private static final CellSetter EMPTY_SETTER = (rec, cell) -> {
	};
	
	private final Map<String, CellSetter> SETTER_MAP = Map.ofEntries(
	        Map.entry("federation", (rec, cell) -> RecordEventSetters.setFederation(rec, cell)),
	        Map.entry("recordname", (rec, cell) -> RecordEventSetters.setRecordName(rec, cell)),
	        Map.entry("agegroup", (rec, cell) -> RecordEventSetters.setAgeGroup(rec, cell)),
	        
	        Map.entry("gender", (rec, cell) -> RecordEventSetters.setGender(rec, cell)),
	        Map.entry("m/f", (rec, cell) -> RecordEventSetters.setGender(rec, cell)),

	        Map.entry("agelow", (rec, cell) -> RecordEventSetters.setAgeLower(rec, cell)),
	        Map.entry("agemin", (rec, cell) -> RecordEventSetters.setAgeLower(rec, cell)), // synonym
	        
	        Map.entry("ageupper", (rec, cell) -> RecordEventSetters.setAgeUpper(rec, cell)),
	        Map.entry("agemax", (rec, cell) -> RecordEventSetters.setAgeUpper(rec, cell)), // synonym
	        Map.entry("agecat", (rec, cell) -> RecordEventSetters.setAgeUpper(rec, cell)), // synonym

	        Map.entry("bwlow", (rec, cell) -> RecordEventSetters.setBwLower(rec, cell)),
	        Map.entry("bodyweightmin", (rec, cell) -> RecordEventSetters.setBwLower(rec, cell)),
	        
	        Map.entry("bwupper", (rec, cell) -> RecordEventSetters.setBwUpper(rec, cell)),
	        Map.entry("bwcat", (rec, cell) -> RecordEventSetters.setBwUpper(rec, cell)), // synonym
	        Map.entry("bwhigh", (rec, cell) -> RecordEventSetters.setBwUpper(rec, cell)), // synonym
	        Map.entry("bodyweightmax", (rec, cell) -> RecordEventSetters.setBwUpper(rec, cell)), // synonym

	        Map.entry("recordlift", (rec, cell) -> RecordEventSetters.setRecordLift(rec, cell)),  
	        Map.entry("lift", (rec, cell) -> RecordEventSetters.setRecordLift(rec, cell)), // synonym

	        Map.entry("recordvalue", (rec, cell) -> RecordEventSetters.setRecordValue(rec, cell)),
	        Map.entry("record", (rec, cell) -> RecordEventSetters.setRecordValue(rec, cell)), // synonym
	        
	        Map.entry("athletename", (rec, cell) -> RecordEventSetters.setAthleteName(rec, cell)),
	        Map.entry("name", (rec, cell) -> RecordEventSetters.setAthleteName(rec, cell)), // synonym
	        
	        Map.entry("born", (rec, cell) -> RecordEventSetters.setBirthDate(rec, cell)),
	        Map.entry("birth date", (rec, cell) -> RecordEventSetters.setBirthDate(rec, cell)), // synonym
	        
	        Map.entry("nation", (rec, cell) -> RecordEventSetters.setNation(rec, cell)),
	        
	        Map.entry("date", (rec, cell) -> RecordEventSetters.setRecordDate(rec, cell)),
	        Map.entry("place", (rec, cell) -> RecordEventSetters.setEventLocation(rec, cell)),
	        
	        Map.entry("event", (rec, cell) -> RecordEventSetters.setEvent(rec, cell)),
	        Map.entry("group", (rec, cell) -> RecordEventSetters.setGroup(rec, cell))
	);

	private CellSetter[] createSetterTableFromHeaderRow(Row headerRow, List<String> errors) {
		List<CellSetter> setters = new ArrayList<>();
		for (Cell cell : headerRow) {
			if (cell.getCellType() != CellType.STRING || cell.getStringCellValue().isBlank()) {
				break;
			}
			String headerValue = cell.getStringCellValue().trim().toLowerCase();
			CellSetter setter = SETTER_MAP.get(headerValue);
			if (setter != null) {
				logger.debug("Mapped header '{}' to setter", headerValue);
			} else {
				logger.warn("No setter found for header '{}'", headerValue);
				errors.add(MessageFormat.format("Ignoring unknown column ''{0}'' at sheet {1} [{2}]", 
				    headerValue, cell.getSheet().getSheetName(), cell.getAddress()));
				setter = EMPTY_SETTER;
			}
			setters.add(setter);
		}
		return setters.toArray(new CellSetter[0]);
	}

	public List<String> createRecords(Workbook workbook, String name, String baseName) {
		cleanUp(baseName);

		return JPAService.runInTransaction(em -> {
			int iRecord = 0;
			List<String> errors = new ArrayList<>();
			CellSetter[] setterTable = null;

			for (Sheet sheet : workbook) {
				for (Row row : sheet) {
					int iRow = row.getRowNum();
					if (iRow == 0) {
						// Process header row to create setter table
						setterTable = createSetterTableFromHeaderRow(row, errors);
						continue;
					}

					RecordEvent rec = new RecordEvent();
					rec.setFileName(baseName);

					// beware: on a truly empty row we will not enter this loop.
					// but if the row has blank non empty cells we will.
					boolean error = false;
					for (Cell cell : row) {
						try {
							int iColumn = cell.getAddress().getColumn();

							logger.debug("[" + sheet.getSheetName() + "," + cell.getAddress() + "]");

							if (setterTable != null && iColumn < setterTable.length) {
								setterTable[iColumn].set(rec, cell);
							}

							iColumn++;
						} catch (Exception e) {
							// do not report errors on empty rows
							if (!isEmptyRow(rec)) {
								logger.error("{}[{}] {} ", sheet.getSheetName(), cell.getAddress(), e.getMessage());
								errors.add(MessageFormat.format("{0}[{1}] {2} ", sheet.getSheetName(),
								        cell.getAddress(), e.getMessage()));
								error = true;
							}
						}
					}

					if (!error && !isEmptyRow(rec)) {
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

	public void loadRecords() {
		Path recordsPath;
		try {
			recordsPath = ResourceWalker.getFileOrResourcePath("/records");
			try {
				if (recordsPath != null && Files.exists(recordsPath)) {
					readFolder(recordsPath);
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

	public void readFolder(Path recordsPath) throws IOException {
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

	public List<String> readInputStream(InputStream is, String fileName) {
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

	public void readZip(InputStream source) throws IOException {
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

	public void resetRecords() {
		Path recordsPath;
		try {
			recordsPath = ResourceWalker.getFileOrResourcePath("/records");
			try {
				RecordRepository.clearLoadedRecords();
				if (recordsPath != null && Files.exists(recordsPath)) {
					readFolder(recordsPath);
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
