/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

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
import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

public class AgeGroupDefinitionReader {

	private static final String AGE_GROUP_SCORING_HEADER = "agegroupscoring";
	private static final String AGE_GROUP_BEST_ATHLETE = "agegroupbestathlete";
	private static Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupDefinitionReader.class);
	static DataFormatter formatter = new DataFormatter();
	private static int[] countDefaults = new int[Gender.values().length];
	private static Map<String, AgeGroup> ageGroupByCodeGender = new HashMap<>();
	private static ThreadLocal<Consumer<String>> errorCollector = new ThreadLocal<>();

	public static void setErrorCollector(Consumer<String> collector) {
		errorCollector.set(collector);
	}

	public static void doInsertRobiAndAgeGroups(InputStream ageGroupStream) {
		Logger mainLogger = Main.getStartupLogger();
		Map<String, Category> templates = loadRobi(mainLogger);
		loadAgeGroupStream(null, "custom upload", mainLogger, templates, ageGroupStream);
	}

	@SuppressWarnings("null")
	static void createAgeGroups(Workbook workbook, Map<String, Category> templates,
	        EnumSet<ChampionshipType> forcedInsertion,
	        String localizedName) {

		for (int i = 0; i < Gender.values().length; i++) {
			countDefaults[i] = 0;
		}
		JPAService.runInTransaction(em -> {
			// backward compatibility
			Sheet sheet = workbook.getSheetAt(workbook.getNumberOfSheets() - 1);
			Iterator<Row> rowIterator = sheet.rowIterator();
			int iRow;
			boolean ageGroupScoring = false;
			boolean ageGroupBestAthlete = false;
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
					Cell bestAthlete = row.getCell(8);
					if (bestAthlete != null) {
						try {
							String lowerCase = bestAthlete.getStringCellValue().toLowerCase();
							ageGroupBestAthlete = lowerCase.equals(AGE_GROUP_BEST_ATHLETE);
						} catch (Exception e) {
						}
					}
					continue;
				}

				AgeGroup ag = null;
				double curMin = 0.0D;
				boolean skip = false;

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
							if (!cellValue.isBlank()) {
								try {
									ag.setAgeDivision(cellValue);
									ag.setChampionshipType(ChampionshipType.valueOf(cellValue));
								} catch (Exception e) {
									reportError(iRow, iColumn, cellValue, new IllegalArgumentException("Unknown Championship Type " + cellValue));
								}
							} else {
								ag.setAgeDivision(cellValue);
								ag.setChampionshipType(ChampionshipType.U);
							}

							if (ag.getConfiguredChampionshipType().isMasters()) {
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
							if (ag.getGender() == null) {
								reportError(iRow, iColumn, cellValue, new IllegalArgumentException("You must indicate a Gender M or F"));
							} else {
								if (ag.getConfiguredChampionshipType() == ChampionshipType.DEFAULT) {
									countDefaults[ag.getGender().ordinal()] = countDefaults[ag.getGender().ordinal()] + 1;
									int nbDefaults = countDefaults[ag.getGender().ordinal()];
									if (nbDefaults > 1) {
										IllegalArgumentException ex = new IllegalArgumentException(
										        "You can only have one DEFAULT for Men and one DEFAULT for Women");
										reportError(iRow, 0, safeGetTextValue(row.getCell(0)), ex);
										em.getTransaction().setRollbackOnly();
										throw ex;
									}
								}

								String codeGenderKey = buildCodeGenderKey(ag);
								if (codeGenderKey != null && ageGroupByCodeGender.get(codeGenderKey) != null) {
									String message = Translator.translate("AgeGroup.DuplicateCodeGender",ag.getCode(),ag.getGender());
									IllegalArgumentException ex = new IllegalArgumentException(message);
									reportError(iRow, iColumn, ag.getCode() + " " + ag.getGender(), ex);
									em.getTransaction().setRollbackOnly();
									throw ex;
								} else if (codeGenderKey != null) {
									ageGroupByCodeGender.put(codeGenderKey, ag);
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
								ChampionshipType aDiv = ag.getConfiguredChampionshipType();
								boolean forcedActive = forcedInsertion != null ? forcedInsertion.contains(aDiv) : false;
								ag.setActive(forcedInsertion != null ? forcedActive : explicitlyActive);
							}
						}
							break;
						default:
							if (skip) {
								break;
							}
							if (ageGroupScoring && iColumn == 7) {
								String cellValue = null;
								cellValue = safeGetTextValue(cell);
								if (cellValue != null && !cellValue.isBlank()) {
									try {
										String lowerCase = cellValue.toLowerCase();
										Ranking rv = Ranking.rankingByReportingName.get(lowerCase);
										if (rv == null) {
											reportError(iRow, iColumn, cellValue, new IllegalArgumentException(lowerCase));
										} else {
											ag.setScoringSystem(rv);
										}
									} catch (Exception e) {
										reportError(iRow, iColumn, cellValue, e);
									}
								}
							} else if (ageGroupBestAthlete && iColumn == 8) {
								String cellValue = null;
								cellValue = safeGetTextValue(cell);
								if (cellValue != null && !cellValue.isBlank()) {
									try {
										String lowerCase = cellValue.toLowerCase();
										Ranking rv = Ranking.rankingByReportingName.get(lowerCase);
										if (rv == null) {
											reportError(iRow, iColumn, cellValue, new IllegalArgumentException(lowerCase));
										} else {
											ag.setBestAthleteScoringSystem(rv);
										}
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

				if (ag != null && !skip) {
					if (ag.getChampionshipName() == null || ag.getChampionshipName().isBlank()) {
						ag.setChampionshipName(ag.getCode());
					}
					if (ag.getStoredChampionshipType() == null) {
						ag.setChampionshipType(ChampionshipType.U);
					}
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

	private static void loadAgeGroupStream(EnumSet<ChampionshipType> forcedInsertion, String localizedName,
	        Logger mainLogger,
	        Map<String, Category> templates, InputStream localizedResourceAsStream1) {
		try (Workbook workbook = WorkbookFactory
		        .create(localizedResourceAsStream1)) {
			logger.info("loading age group configuration file {}", localizedName);
			mainLogger.info("loading age group definitions {}", localizedName);
			ageGroupByCodeGender.clear();
			CategoryRepository.clearCodeMap();
			createAgeGroups(workbook, templates, forcedInsertion, localizedName);
			ChampionshipRepository.reconcileFromAgeGroups();
			Championship.reset();
			CategoryRepository.resetCodeMap();
		} catch (Exception e) {
			logger.error("could not process ageGroup configuration: {}", e.getMessage());
		}
	}

	private static String buildCodeGenderKey(AgeGroup ag) {
		if (ag == null) {
			return null;
		}
		String code = ag.getCode();
		Gender gender = ag.getGender();
		if (code == null || code.isBlank() || gender == null) {
			return null;
		}
		return code.trim().toUpperCase() + "_" + gender.name();
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

	private static void reportError(int iRow, int iColumn, String cellValue, Exception e) {
		String template = Translator.translate("AgeGroup.CannotProcessCell");
		String msg = MessageFormat.format(
		        template,
		        cellName(iColumn, iRow), e.getMessage());
		logger.error(msg);

		Consumer<String> collector = errorCollector.get();
		if (collector != null) {
			collector.accept(msg);
		}

		if (collector == null && UI.getCurrent() != null) {
			NotificationUtils.errorNotification(msg);
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

}
