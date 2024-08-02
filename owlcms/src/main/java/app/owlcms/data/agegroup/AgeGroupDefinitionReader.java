/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.Main;
import app.owlcms.apputils.NotificationUtils;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.category.RobiCategories;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

public class AgeGroupDefinitionReader {

	private static final String AGE_GROUP_SCORING_HEADER = "agegroupscoring";
	private static Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupDefinitionReader.class);

	public static void doInsertRobiAndAgeGroups(InputStream ageGroupStream) {
		Logger mainLogger = Main.getStartupLogger();
		Map<String, Category> templates = loadRobi(mainLogger);
		loadAgeGroupStream(null, "custom upload", mainLogger, templates, ageGroupStream);

	}

	static void createAgeGroups(Workbook workbook, Map<String, Category> templates,
	        EnumSet<ChampionshipType> forcedInsertion,
	        String localizedName) {

		JPAService.runInTransaction(em -> {
			// backward compatibility
			Sheet sheet = workbook.getSheetAt(workbook.getNumberOfSheets() - 1);
			Iterator<Row> rowIterator = sheet.rowIterator();
			int iRow;
			boolean ageGroupScoring = false;
			rows: while (rowIterator.hasNext()) {
				int iColumn;
				Row row;
				row = rowIterator.next();
				iRow = row.getRowNum();
				if (iRow == 0) {
					// process header
					iRow = row.getRowNum();
					Cell scoring = row.getCell(7);
					if (scoring != null) {
						try {
							String lowerCase = scoring.getStringCellValue().toLowerCase();
							ageGroupScoring = lowerCase.equals(AGE_GROUP_SCORING_HEADER);
						} catch (Exception e) {
						}
					}
					continue;
				}

				AgeGroup ag = null;
				double curMin = 0.0D;

				Iterator<Cell> cellIterator = row.cellIterator();
				String championshipName = null;
				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();
					iColumn = cell.getColumnIndex();
					switch (iColumn) {
						case 0: {
							String cellValue = safeGetTextValue(cell);
							String trim = cellValue.trim();
							if (trim.isBlank()) {
								ag = null;
								break rows;
							} else if (trim.startsWith("!")) {
								ag = new AgeGroup();
								ag.setCode(trim.substring(1));
								ag.setAlreadyGendered(true);
							} else {
								ag = new AgeGroup();
								ag.setCode(trim);
								ag.setAlreadyGendered(false);
							}
						}
							break;
						case 1:
							championshipName = safeGetTextValue(cell);
							if (championshipName != null && !championshipName.isBlank()) {
								ag.setChampionshipName(championshipName);
							}
							break;
						case 2: {
							String cellValue = safeGetTextValue(cell);
							ag.setAgeDivision(cellValue);
							if (ag.getChampionshipType() == ChampionshipType.MASTERS) {
								ag.setAlreadyGendered(true);
							}
						}
							break;
						case 3: {
							String cellValue = safeGetTextValue(cell);
							if (cellValue != null && !cellValue.trim().isEmpty() && ag != null) {
								try {
									ag.setGender(Gender.valueOf(cellValue));
								} catch (IllegalArgumentException e) {
									ag.setGender(cellValue.contentEquals("W") ? Gender.F : Gender.M);
								}
							}
						}
							break;
						case 4: {
							long cellValue = Math.round(cell.getNumericCellValue());
							if (ag != null) {
								ag.setMinAge(Math.toIntExact(cellValue));
							}
						}
							break;
						case 5: {
							long cellValue = Math.round(cell.getNumericCellValue());
							if (ag != null) {
								ag.setMaxAge(Math.toIntExact(cellValue));
							}
						}
							break;
						case 6: {
							boolean explicitlyActive = getSafeBooleanValue(cell);
							// age division is active according to spreadsheet, unless we are given an
							// explicit list of championship types as override (e.g. to setup tests or demos)
							if (ag != null) {
								ChampionshipType aDiv = ag.getChampionshipType();
								boolean forcedActive = forcedInsertion != null ? forcedInsertion.contains(aDiv) : false;
								ag.setActive(forcedInsertion != null ? forcedActive : explicitlyActive);
							}
						}
							break;
						default:
							if (ageGroupScoring && iColumn == 7) {
								String cellValue = null;
								cellValue = safeGetTextValue(cell);
								if (cellValue != null && !cellValue.isBlank()) {
									try {
										String upperCase = cellValue.toUpperCase();
										Ranking rv = Ranking.valueOf(upperCase.equals("SMHF") ? "SMM" : upperCase);
										ag.setScoringSystem(rv);
									} catch (Exception e) {
										reportError(iRow, iColumn, cellValue, e);
									}
								}
							} else {
								String cellValue = null;
								try {
									cellValue = safeGetTextValue(cell);
								} catch (IllegalStateException e) {
									Double doubleValue = cell.getNumericCellValue();
									if (doubleValue != null) {
										cellValue = Integer.toString(doubleValue.intValue());
									}
								}
								if (cellValue != null && !cellValue.trim().isEmpty()) {
									String[] parts = cellValue.split("[-_. /]");
									String catCode = parts.length > 0 ? parts[0] : cellValue;
									String qualTotal = parts.length > 1 ? parts[1] : "0";
									Category cat;
									try {
										Gender gender;
										String upper;
										if (catCode.matches("^[A-Za-z]\\d+$")) {
											gender = Gender.valueOf(catCode.substring(0, 1));
											upper = catCode.substring(1);
										} else {
											gender = ag.getGender();
											upper = catCode;
										}
										cat = new Category(curMin, Double.parseDouble(upper),
										        gender, ag.isActive(), 0, 0, 0,
										        ag, Integer.parseInt(qualTotal));
										em.persist(cat);
										// logger.debug(cat.longDump());
										curMin = cat.getMaximumWeight();
									} catch (Exception e) {
										reportError(iRow, iColumn, cellValue, e);
									}

								}
							}
							break;
					}
					iColumn++;
				}
				if (ag != null) {
					em.persist(ag);
				}
				iRow++;
			}
			Competition comp = Competition.getCurrent();
			Competition comp2 = em.contains(comp) ? comp : em.merge(comp);
			comp2.setAgeGroupsFileName(localizedName);

			return null;
		});
	}

	private static void reportError(int iRow, int iColumn, String cellValue, Exception e) {
		String msg = MessageFormat.format(
		        "cannot process cell {0} (content = \"{1}\") {2}",
		        cellName(iColumn, iRow), cellValue, e);
		logger.error(msg);
		if (UI.getCurrent() != null) {
			NotificationUtils.errorNotification(msg);
		}
	}

	static DataFormatter formatter = new DataFormatter();

	private static boolean getSafeBooleanValue(Cell cell) {
		try {
			return cell.getBooleanCellValue();
		} catch (IllegalStateException e) {
			if (cell.getCellType() == CellType.NUMERIC) {
				String strValue = formatter.formatCellValue(cell);
				return strValue.equalsIgnoreCase("true");
			} else {
				logger.error("cannot extract string from cell {}", cell.getAddress());
				throw new IllegalStateException("cannot extract boolean from cell " + cell.getAddress());
			}
		}
	}

	private static String safeGetTextValue(Cell cell) {
		try {
			return cell.getStringCellValue();
		} catch (IllegalStateException e) {
			if (cell.getCellType() == CellType.NUMERIC) {
				String strValue = formatter.formatCellValue(cell);
				return strValue;
			} else {
				logger.error("cannot extract string from cell {}", cell.getAddress());
				throw new IllegalStateException("cannot extract string from cell " + cell.getAddress());
			}
		}
	}

	static void doInsertRobiAndAgeGroups(EnumSet<ChampionshipType> forcedInsertion, String localizedFileName) {
		Logger mainLogger = Main.getStartupLogger();
		Map<String, Category> templates = loadRobi(mainLogger);
		InputStream ageGroupStream = findAgeGroupFile(localizedFileName, mainLogger);
		loadAgeGroupStream(forcedInsertion, localizedFileName, mainLogger, templates, ageGroupStream);
	}

	private static Object cellName(int iColumn, int iRow) {
		return Character.toString('A' + iColumn) + (Integer.toString(iRow + 1));
	}

	private static InputStream findAgeGroupFile(String localizedFileName, Logger mainLogger) {
		InputStream ageGroupStream = null;
		try {
			ageGroupStream = ResourceWalker.getResourceAsStream(localizedFileName);
		} catch (FileNotFoundException e1) {
			logger.error("could not find ageGroup configuration\n{}", LoggerUtils./**/stackTrace(e1));
			mainLogger.error("could not find ageGroup configuration. See logs for details");
		}
		return ageGroupStream;
	}

	private static void loadAgeGroupStream(EnumSet<ChampionshipType> forcedInsertion, String localizedName,
	        Logger mainLogger,
	        Map<String, Category> templates, InputStream localizedResourceAsStream1) {
		try (Workbook workbook = WorkbookFactory
		        .create(localizedResourceAsStream1)) {
			logger.info("loading age group configuration file {}", localizedName);
			mainLogger.info("loading age group definitions {}", localizedName);
			createAgeGroups(workbook, templates, forcedInsertion, localizedName);
			Championship.reset();
			CategoryRepository.resetCodeMap();
		} catch (Exception e) {
			logger.error("could not process ageGroup configuration\n{}", LoggerUtils./**/stackTrace(e));
			mainLogger.error("could not process ageGroup configuration. See logs for details");
		}
	}

	private static Map<String, Category> loadRobi(Logger mainLogger) {
		InputStream localizedResourceAsStream;
		Map<String, Category> templates = new TreeMap<>();
		try {
			localizedResourceAsStream = ResourceWalker.getResourceAsStream(RobiCategories.ROBI_CATEGORIES_XLSX);
			try (Workbook workbook = WorkbookFactory.create(localizedResourceAsStream)) {
				templates = RobiCategories.createCategoryTemplates(workbook);
			}
		} catch (Exception e) {
			logger.error("could not process RobiCategories configuration\n{}", LoggerUtils./**/stackTrace(e));
			mainLogger.error("could not process RobiCategories configuration. See logs for details");
		}
		return templates;
	}

}
