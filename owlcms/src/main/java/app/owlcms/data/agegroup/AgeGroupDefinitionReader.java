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
import java.util.function.Predicate;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import app.owlcms.apputils.NotificationUtils;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.RobiCategories;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

public class AgeGroupDefinitionReader {

	private static Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupDefinitionReader.class);


	private static Object cellName(int iColumn, int iRow) {
		return Character.toString('A' + iColumn) + (Integer.toString(iRow + 1));
	}

	static void createAgeGroups(Workbook workbook, Map<String, Category> templates,
	        EnumSet<AgeDivision> ageDivisionOverride,
	        String localizedName) {

		JPAService.runInTransaction(em -> {
			// backward compatibility
			Sheet sheet = workbook.getSheetAt(workbook.getNumberOfSheets()-1);
			Iterator<Row> rowIterator = sheet.rowIterator();
			int iRow = 0;
			rows: while (rowIterator.hasNext()) {
				int iColumn = 0;
				Row row;
				if (iRow == 0) {
					// process header
					row = rowIterator.next();
				}
				row = rowIterator.next();

				AgeGroup ag = null;
				double curMin = 0.0D;

				Iterator<Cell> cellIterator = row.cellIterator();
				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();
					switch (iColumn) {
						case 0: {
							String cellValue = cell.getStringCellValue();
							String trim = cellValue.trim();
							if (trim.isBlank()) {
								ag = null;
								break rows;
							} else {
								ag = new AgeGroup();
								ag.setCode(trim);
							}
						}
							break;
						case 1:
							break;
						case 2: {
							String cellValue = cell.getStringCellValue();
							if (ag != null) {
								ag.setAgeDivision(AgeDivision.getAgeDivisionFromCode(cellValue));
							}
						}
							break;
						case 3: {
							String cellValue = cell.getStringCellValue();
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
							boolean explicitlyActive = cell.getBooleanCellValue();
							// age division is active according to spreadsheet, unless we are given an
							// explicit
							// list of age divisions as override (e.g. to setup tests or demos)
							if (ag != null) {
								AgeDivision aDiv = ag.getAgeDivision();
								boolean active = ageDivisionOverride == null ? explicitlyActive
								        : ageDivisionOverride.stream()
								                .anyMatch((Predicate<AgeDivision>) (ad) -> ad.equals(aDiv));
								ag.setActive(active);
							}
						}
							break;
						default: {
							String cellValue = null;
							try {
								cellValue = cell.getStringCellValue();
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
									// cat = AgeGroupRepository.createCategoryFromTemplate(catCode, ag, templates,
									// curMin, qualTotal);
									// if (cat == null) {
									// category is not IWF, no records available
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
									// }
									em.persist(cat);
									// logger.debug(cat.longDump());
									curMin = cat.getMaximumWeight();
								} catch (Exception e) {
									try {
										Throwable cause = e.getCause();
										String msg = MessageFormat.format(
										        "cannot process cell {0} (content = \"{1}\") {2} {3}",
										        cellName(iColumn, iRow), cellValue, cause.getClass().getSimpleName(),
										        cause.getMessage());
										logger.error(msg);
										NotificationUtils.errorNotification(msg);
										throw new RuntimeException(msg);
									} catch (Exception e1) {
										throw new RuntimeException(e);
									}
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

	static void doInsertRobiAndAgeGroups(EnumSet<AgeDivision> es, String localizedFileName) {
		Logger mainLogger = Main.getStartupLogger();
		Map<String, Category> templates = loadRobi(mainLogger);
		InputStream ageGroupStream = findAgeGroupFile(localizedFileName, mainLogger);
		loadAgeGroupStream(es, localizedFileName, mainLogger, templates, ageGroupStream);

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

	public static void doInsertRobiAndAgeGroups(InputStream ageGroupStream) {
		Logger mainLogger = Main.getStartupLogger();
		Map<String, Category> templates = loadRobi(mainLogger);
		loadAgeGroupStream(null, "custom upload", mainLogger, templates, ageGroupStream);

	}
	
	private static void loadAgeGroupStream(EnumSet<AgeDivision> es, String localizedName, Logger mainLogger,
	        Map<String, Category> templates, InputStream localizedResourceAsStream1) {
		try (Workbook workbook = WorkbookFactory
		        .create(localizedResourceAsStream1)) {
			logger.info("loading age group configuration file {}", localizedName);
			mainLogger.info("loading age group definitions {}", localizedName);
			createAgeGroups(workbook, templates, es, localizedName);
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
