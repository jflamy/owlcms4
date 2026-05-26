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

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

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
	private static final String AGE_GROUPS_SHEET_NAME = "AgeGroups";
	private static final String CHAMPIONSHIPS_SHEET_NAME = "Championships";
	private static final String CHAMPIONSHIP_HEADER = "championship";
	private static final String CHAMPIONSHIP_TYPE_HEADER = "championshiptype";
	private static final String CODE_HEADER = "code";
	private static final String GENDER_HEADER = "gender";
	private static final String FROM_HEADER = "from";
	private static final String TO_HEADER = "to";
	private static final String ACTIVE_HEADER = "active";
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
			Sheet championshipsSheet = getSheet(workbook, CHAMPIONSHIPS_SHEET_NAME);
			boolean simplifiedAgeGroups = championshipsSheet != null;
			Map<String, ChampionshipType> importedChampionshipTypes = simplifiedAgeGroups
			        ? createChampionships(championshipsSheet, em)
			        : new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			Map<String, Championship> createdLegacyChampionships = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			Sheet sheet = getAgeGroupsSheet(workbook, simplifiedAgeGroups);
			Iterator<Row> rowIterator = sheet.rowIterator();
			int iRow;
			Map<String, Integer> headerColumns = new HashMap<>();
			int codeColumn = 0;
			int championshipColumn = simplifiedAgeGroups ? -1 : 1;
			int championshipTypeColumn = simplifiedAgeGroups ? -1 : 2;
			int genderColumn = simplifiedAgeGroups ? 2 : 3;
			int fromColumn = simplifiedAgeGroups ? 3 : 4;
			int toColumn = simplifiedAgeGroups ? 4 : 5;
			int activeColumn = simplifiedAgeGroups ? 5 : 6;
			int scoringColumn = -1;
			int bestAthleteColumn = -1;
			rows: while (rowIterator.hasNext()) {
				Row row;
				row = rowIterator.next();
				iRow = row.getRowNum();
				if (iRow == 0) {
					// process header
					headerColumns = readHeaderColumns(row);
					codeColumn = columnIndex(headerColumns, CODE_HEADER, codeColumn);
					championshipColumn = columnIndex(headerColumns, CHAMPIONSHIP_HEADER, championshipColumn);
					championshipTypeColumn = columnIndex(headerColumns, CHAMPIONSHIP_TYPE_HEADER, championshipTypeColumn);
					genderColumn = columnIndex(headerColumns, GENDER_HEADER, genderColumn);
					fromColumn = columnIndex(headerColumns, FROM_HEADER, fromColumn);
					toColumn = columnIndex(headerColumns, TO_HEADER, toColumn);
					activeColumn = columnIndex(headerColumns, ACTIVE_HEADER, activeColumn);
					scoringColumn = columnIndex(headerColumns, AGE_GROUP_SCORING_HEADER, scoringColumn);
					bestAthleteColumn = columnIndex(headerColumns, AGE_GROUP_BEST_ATHLETE, bestAthleteColumn);
					continue;
				}

				AgeGroup ag = createAgeGroupFromRow(row, iRow, simplifiedAgeGroups, importedChampionshipTypes,
				        codeColumn, championshipColumn, championshipTypeColumn, genderColumn, fromColumn, toColumn,
				        activeColumn, forcedInsertion, em);
				if (ag == null) {
					break rows;
				}
				double curMin = 0.0D;
				boolean skip = false;

				setAgeGroupRanking(row, iRow, scoringColumn, ag, true);
				setAgeGroupRanking(row, iRow, bestAthleteColumn, ag, false);
				if (!simplifiedAgeGroups) {
					ensureLegacyChampionship(em, ag, createdLegacyChampionships);
				}

				Iterator<Cell> cellIterator = row.cellIterator();
				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();
					int iColumn = cell.getColumnIndex();
					if (skip) {
						break;
					}
					if (!isKnownAgeGroupColumn(iColumn, codeColumn, championshipColumn, championshipTypeColumn,
					        genderColumn, fromColumn, toColumn, activeColumn, scoringColumn, bestAthleteColumn)) {
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
				}

				if (ag != null && !skip) {
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

	private static AgeGroup createAgeGroupFromRow(Row row, int iRow, boolean simplifiedAgeGroups,
	        Map<String, ChampionshipType> importedChampionshipTypes, int codeColumn, int championshipColumn,
	        int championshipTypeColumn, int genderColumn, int fromColumn, int toColumn, int activeColumn,
	        EnumSet<ChampionshipType> forcedInsertion, EntityManager em) {
		String codeValue = getCellText(row, codeColumn);
		String trim = codeValue.trim();
		if (trim.isBlank()) {
			return null;
		}

		AgeGroup ag = new AgeGroup();
		if (trim.startsWith("!")) {
			ag.setCode(trim.substring(1));
			ag.setAlreadyGendered(true);
		} else {
			ag.setCode(trim);
			ag.setAlreadyGendered(false);
		}

		String championshipName = getCellText(row, championshipColumn);
		if (championshipName == null || championshipName.isBlank()) {
			championshipName = ag.getCode();
		}
		ag.setChampionshipName(championshipName);

		ChampionshipType championshipType = null;
		if (championshipTypeColumn >= 0) {
			String cellValue = getCellText(row, championshipTypeColumn);
			championshipType = parseChampionshipType(iRow, championshipTypeColumn, cellValue, ChampionshipType.U);
			ag.setAgeDivision(cellValue);
		} else if (simplifiedAgeGroups) {
			String canonicalName = Championship.canonicalizeChampionshipName(championshipName);
			championshipType = importedChampionshipTypes.get(canonicalName);
			if (championshipType == null) {
				championshipType = ChampionshipType.U;
			}
		}
		if (championshipType == null) {
			championshipType = ChampionshipType.U;
		}
		ag.setChampionshipType(championshipType);
		if (ag.getAgeDivision() == null || ag.getAgeDivision().isBlank()) {
			ag.setAgeDivision(championshipType.name());
		}

		if (ag.getConfiguredChampionshipType().isMasters()) {
			ag.setAlreadyGendered(true);
		}

		String genderValue = getCellText(row, genderColumn);
		if (genderValue != null && !genderValue.trim().isEmpty()) {
			try {
				ag.setGender(Gender.valueOf(genderValue));
			} catch (IllegalArgumentException e) {
				ag.setGender(genderValue.contentEquals("W") ? Gender.F : Gender.M);
			}
		}
		if (ag.getGender() == null) {
			reportError(iRow, genderColumn, genderValue, new IllegalArgumentException("You must indicate a Gender M or F"));
		} else {
			validateAgeGroupIdentity(ag, iRow, genderColumn, em);
		}

		ag.setMinAge(getIntegerValue(row, iRow, fromColumn, ag.getMinAge()));
		ag.setMaxAge(getIntegerValue(row, iRow, toColumn, ag.getMaxAge()));

		boolean explicitlyActive = getBooleanValue(row, activeColumn, false);
		ChampionshipType aDiv = ag.getConfiguredChampionshipType();
		boolean forcedActive = forcedInsertion != null ? forcedInsertion.contains(aDiv) : false;
		ag.setActive(forcedInsertion != null ? forcedActive : explicitlyActive);

		return ag;
	}

	private static Map<String, ChampionshipType> createChampionships(Sheet sheet, EntityManager em) {
		Map<String, ChampionshipType> importedTypes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Iterator<Row> rowIterator = sheet.rowIterator();
		Map<String, Integer> headerColumns = new HashMap<>();
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			int iRow = row.getRowNum();
			if (iRow == 0) {
				headerColumns = readHeaderColumns(row);
				continue;
			}

			String name = getCellText(row, columnIndex(headerColumns, "name", 0));
			if (name == null || name.isBlank()) {
				continue;
			}
			String canonicalName = Championship.canonicalizeChampionshipName(name.trim());
			ChampionshipType type = parseChampionshipType(iRow, columnIndex(headerColumns, "type", 1),
			        getCellText(row, columnIndex(headerColumns, "type", 1)), ChampionshipType.U);
			Championship championship = findChampionship(em, canonicalName);
			if (championship == null) {
				championship = new Championship(canonicalName, type);
			} else {
				championship.setType(type);
			}

			championship.setUseCompetitionDefaults(getBooleanValue(row, headerColumns, "usecompetitiondefaults", true));
			championship.setScoringSystem(getRankingValue(row, iRow, headerColumns, "scoringsystem"));
			championship.setBestAthleteScoringSystem(getRankingValue(row, iRow, headerColumns, "bestathletescoringsystem"));
			championship.setBestSnatchScoringSystem(getRankingValue(row, iRow, headerColumns, "bestsnatchscoringsystem"));
			championship.setBestCJScoringSystem(getRankingValue(row, iRow, headerColumns, "bestcjscoringsystem"));
			championship.setSnatchCJTotalMedals(getBooleanValue(row, headerColumns, "snatchcjtotalmedals", false));
			championship.setTeamPoints1st(getIntegerValue(row, iRow, headerColumns, "teampoints1st"));
			championship.setTeamPoints2nd(getIntegerValue(row, iRow, headerColumns, "teampoints2nd"));
			championship.setTeamPoints3rd(getIntegerValue(row, iRow, headerColumns, "teampoints3rd"));
			championship.setMensBestN(getIntegerValue(row, iRow, headerColumns, "mensbestn"));
			championship.setWomensBestN(getIntegerValue(row, iRow, headerColumns, "womensbestn"));
			championship.setTeamScoringSystem(getRankingValue(row, iRow, headerColumns, "teamscoringsystem"));
			championship.setMaxTeamSize(getIntegerValue(row, iRow, headerColumns, "maxteamsize"));
			championship.setMaxPerCategory(getIntegerValue(row, iRow, headerColumns, "maxpercategory"));
			championship.setMixedTeamEnabled(getBooleanValue(row, headerColumns, "mixedteamenabled", false));
			championship.setMixedTeamScoringSystem(getRankingValue(row, iRow, headerColumns, "mixedteamscoringsystem"));
			championship.setExplicitMixedTeamMembers(getBooleanValue(row, headerColumns, "explicitmixedteammembers", false));
			championship.setExplicitTeamSize(getIntegerValue(row, iRow, headerColumns, "explicitteamsize"));
			championship.setMixedBestN(getIntegerValue(row, iRow, headerColumns, "mixedbestn"));
			championship.setMixedMensBestN(getIntegerValue(row, iRow, headerColumns, "mixedmensbestn"));
			championship.setMixedWomensBestN(getIntegerValue(row, iRow, headerColumns, "mixedwomensbestn"));

			if (championship.getId() == null) {
				em.persist(championship);
			} else {
				em.merge(championship);
			}
			importedTypes.put(canonicalName, type);
		}
		deleteOmittedChampionships(em, importedTypes);
		return importedTypes;
	}

	private static void deleteOmittedChampionships(EntityManager em, Map<String, ChampionshipType> importedTypes) {
		TypedQuery<Championship> query = em.createQuery("select c from Championship c", Championship.class);
		for (Championship championship : query.getResultList()) {
			String canonicalName = Championship.canonicalizeChampionshipName(championship.getName());
			if (!importedTypes.containsKey(canonicalName)) {
				em.remove(championship);
			}
		}
	}

	private static int columnIndex(Map<String, Integer> headerColumns, String header, int defaultColumn) {
		Integer column = headerColumns.get(normalizeHeader(header));
		return column != null ? column : defaultColumn;
	}

	private static Championship findChampionship(EntityManager em, String name) {
		TypedQuery<Championship> query = em.createQuery(
		        "select c from Championship c where lower(c.name) = :name", Championship.class);
		query.setParameter("name", name.toLowerCase());
		return query.getResultList().stream().findFirst().orElse(null);
	}

	private static void ensureLegacyChampionship(EntityManager em, AgeGroup ageGroup,
	        Map<String, Championship> createdLegacyChampionships) {
		String name = Championship.canonicalizeChampionshipName(ageGroup.getChampionshipName());
		if (name == null || name.isBlank()) {
			return;
		}
		Championship championship = createdLegacyChampionships.get(name);
		if (championship == null) {
			if (findChampionship(em, name) != null) {
				return;
			}
			championship = new Championship(name, ageGroup.getConfiguredChampionshipType());
			championship.populateScoringDefaults();
			em.persist(championship);
			createdLegacyChampionships.put(name, championship);
		}
		applyLegacyScoring(championship, ageGroup.getScoringSystem(), ageGroup.getBestAthleteScoringSystem());
	}

	private static void applyLegacyScoring(Championship championship, Ranking scoringSystem,
	        Ranking bestAthleteScoringSystem) {
		if (scoringSystem == null && bestAthleteScoringSystem == null) {
			return;
		}
		championship.setUseCompetitionDefaults(false);
		if (scoringSystem != null) {
			championship.setScoringSystem(scoringSystem);
		}
		if (bestAthleteScoringSystem != null) {
			championship.setBestAthleteScoringSystem(bestAthleteScoringSystem);
			championship.setBestSnatchScoringSystem(bestAthleteScoringSystem);
			championship.setBestCJScoringSystem(bestAthleteScoringSystem);
		}
	}

	private static boolean getBooleanValue(Row row, Map<String, Integer> headerColumns, String header, boolean defaultValue) {
		return getBooleanValue(row, columnIndex(headerColumns, header, -1), defaultValue);
	}

	private static boolean getBooleanValue(Row row, int column, boolean defaultValue) {
		if (column < 0) {
			return defaultValue;
		}
		Cell cell = row.getCell(column);
		if (cell == null) {
			return defaultValue;
		}
		try {
			return cell.getBooleanCellValue();
		} catch (IllegalStateException e) {
			String value = safeGetTextValue(cell).trim();
			if (value.isBlank()) {
				return defaultValue;
			}
			return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("y")
			        || value.equals("1");
		}
	}

	private static String getCellText(Row row, int column) {
		if (column < 0) {
			return "";
		}
		Cell cell = row.getCell(column);
		return cell != null ? safeGetTextValue(cell).trim() : "";
	}

	private static Integer getIntegerValue(Row row, int iRow, Map<String, Integer> headerColumns, String header) {
		return getIntegerValue(row, iRow, columnIndex(headerColumns, header, -1), null);
	}

	private static Integer getIntegerValue(Row row, int iRow, int column, Integer defaultValue) {
		if (column < 0) {
			return defaultValue;
		}
		Cell cell = row.getCell(column);
		if (cell == null) {
			return defaultValue;
		}
		try {
			if (cell.getCellType() == CellType.NUMERIC) {
				return Math.toIntExact(Math.round(cell.getNumericCellValue()));
			}
			String cellValue = safeGetTextValue(cell);
			if (cellValue == null || cellValue.isBlank()) {
				return defaultValue;
			}
			return Math.toIntExact(Math.round(Double.parseDouble(cellValue.trim())));
		} catch (Exception e) {
			reportError(iRow, column, safeGetTextValue(cell), e);
			return defaultValue;
		}
	}

	private static Ranking getRankingValue(Row row, int iRow, Map<String, Integer> headerColumns, String header) {
		return getRankingValue(row, iRow, columnIndex(headerColumns, header, -1));
	}

	private static Ranking getRankingValue(Row row, int iRow, int column) {
		if (column < 0) {
			return null;
		}
		String cellValue = getCellText(row, column);
		if (cellValue == null || cellValue.isBlank()) {
			return null;
		}
		Ranking rv = Ranking.rankingByReportingName.get(cellValue.toLowerCase());
		if (rv == null) {
			reportError(iRow, column, cellValue, new IllegalArgumentException(cellValue));
		}
		return rv;
	}

	private static Sheet getAgeGroupsSheet(Workbook workbook, boolean simplifiedAgeGroups) {
		if (!simplifiedAgeGroups) {
			return workbook.getSheetAt(workbook.getNumberOfSheets() - 1);
		}
		Sheet namedSheet = getSheet(workbook, AGE_GROUPS_SHEET_NAME);
		if (namedSheet != null) {
			return namedSheet;
		}
		for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
			Sheet sheet = workbook.getSheetAt(i);
			if (!sheet.getSheetName().equalsIgnoreCase(CHAMPIONSHIPS_SHEET_NAME)) {
				return sheet;
			}
		}
		return workbook.getSheetAt(workbook.getNumberOfSheets() - 1);
	}

	private static Sheet getSheet(Workbook workbook, String sheetName) {
		for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
			Sheet sheet = workbook.getSheetAt(i);
			if (sheet.getSheetName().equalsIgnoreCase(sheetName)) {
				return sheet;
			}
		}
		return null;
	}

	private static boolean isKnownAgeGroupColumn(int column, int... knownColumns) {
		for (int knownColumn : knownColumns) {
			if (knownColumn >= 0 && knownColumn == column) {
				return true;
			}
		}
		return false;
	}

	private static String normalizeHeader(String header) {
		return header != null ? header.trim().toLowerCase() : "";
	}

	private static ChampionshipType parseChampionshipType(int iRow, int column, String cellValue,
	        ChampionshipType defaultValue) {
		if (cellValue == null || cellValue.isBlank()) {
			return defaultValue;
		}
		try {
			return ChampionshipType.valueOf(cellValue.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			reportError(iRow, column, cellValue, new IllegalArgumentException("Unknown Championship Type " + cellValue));
			return defaultValue;
		}
	}

	private static Map<String, Integer> readHeaderColumns(Row row) {
		Map<String, Integer> headerColumns = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		while (cellIterator.hasNext()) {
			Cell cell = cellIterator.next();
			String value = safeGetTextValue(cell);
			if (value != null && !value.isBlank()) {
				headerColumns.put(normalizeHeader(value), cell.getColumnIndex());
			}
		}
		return headerColumns;
	}

	private static void setAgeGroupRanking(Row row, int iRow, int column, AgeGroup ag, boolean medalScoring) {
		Ranking ranking = getRankingValue(row, iRow, column);
		if (ranking == null) {
			return;
		}
		if (medalScoring) {
			ag.setScoringSystem(ranking);
		} else {
			ag.setBestAthleteScoringSystem(ranking);
		}
	}

	private static void validateAgeGroupIdentity(AgeGroup ag, int iRow, int iColumn, EntityManager em) {
		if (ag.getConfiguredChampionshipType() == ChampionshipType.DEFAULT) {
			countDefaults[ag.getGender().ordinal()] = countDefaults[ag.getGender().ordinal()] + 1;
			int nbDefaults = countDefaults[ag.getGender().ordinal()];
			if (nbDefaults > 1) {
				IllegalArgumentException ex = new IllegalArgumentException(
				        "You can only have one DEFAULT for Men and one DEFAULT for Women");
				reportError(iRow, 0, ag.getCode(), ex);
				em.getTransaction().setRollbackOnly();
				throw ex;
			}
		}

		String codeGenderKey = buildCodeGenderKey(ag);
		if (codeGenderKey != null && ageGroupByCodeGender.get(codeGenderKey) != null) {
			String message = Translator.translate("AgeGroup.DuplicateCodeGender", ag.getCode(), ag.getGender());
			IllegalArgumentException ex = new IllegalArgumentException(message);
			reportError(iRow, iColumn, ag.getCode() + " " + ag.getGender(), ex);
			em.getTransaction().setRollbackOnly();
			throw ex;
		} else if (codeGenderKey != null) {
			ageGroupByCodeGender.put(codeGenderKey, ag);
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
			ageGroupByCodeGender.clear();
			CategoryRepository.clearCodeMap();
			createAgeGroups(workbook, templates, forcedInsertion, localizedName);
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
			if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.BOOLEAN) {
				String strValue = formatter.formatCellValue(cell);
				return strValue;
			} else {
				logger.error("cannot extract string from cell {}", cell.getAddress());
				throw new IllegalStateException("cannot extract string from cell " + cell.getAddress());
			}
		}
	}

}
