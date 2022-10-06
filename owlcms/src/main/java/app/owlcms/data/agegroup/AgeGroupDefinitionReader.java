/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
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
import java.util.function.Predicate;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
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
import app.owlcms.data.competition.Competition;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

public class AgeGroupDefinitionReader {

    private static Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupDefinitionReader.class);

    /**
     * Create category templates that will be copied to instantiate the actual categories. The world records are read
     * and included in the template.
     *
     * @param workbook
     * @return
     */
    public static Map<String, Category> createCategoryTemplates(Workbook workbook) {
        Map<String, Category> categoryMap = new HashMap<>();
        DataFormatter dataFormatter = new DataFormatter();
        Sheet sheet = workbook.getSheetAt(0);
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

            Category c = new Category();

            Iterator<Cell> cellIterator = row.cellIterator();

            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                switch (iColumn) {
                case 0: {
                    String cellValue = dataFormatter.formatCellValue(cell);
                    String trim = cellValue.trim();
                    if (trim.isBlank()) {
                        break rows;
                    }
                    c.setCode(trim);
                    categoryMap.put(cellValue, c);
                }
                    break;
                case 1: {
                    String cellValue = dataFormatter.formatCellValue(cell);
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        // TODO: gender X. do not assume not F == M !
                        c.setGender(cellValue.contentEquals("F") ? Gender.F : Gender.M);
                    }
                }
                    break;
                case 2: {
                    c.setMaximumWeight(cell.getNumericCellValue());
                }
                    break;
                case 3: {
                    c.setWrSr((int) Math.round(cell.getNumericCellValue()));
                }
                    break;
                case 4: {
                    c.setWrJr((int) Math.round(cell.getNumericCellValue()));
                }
                    break;
                case 5: {
                    c.setWrYth((int) Math.round(cell.getNumericCellValue()));
                }
                    break;
                }
                iColumn++;
            }
            iRow++;
        }
        return categoryMap;
    }

    static void createAgeGroups(Workbook workbook, Map<String, Category> templates,
            EnumSet<AgeDivision> ageDivisionOverride,
            String localizedName) {

        JPAService.runInTransaction(em -> {
            Sheet sheet = workbook.getSheetAt(1);
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

                AgeGroup ag = new AgeGroup();
                double curMin = 0.0D;

                Iterator<Cell> cellIterator = row.cellIterator();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    switch (iColumn) {
                    case 0: {
                        String cellValue = cell.getStringCellValue();
                        String trim = cellValue.trim();
                        if (trim.isBlank()) {
                            break rows;
                        }
                        ag.setCode(trim);
                    }
                        break;
                    case 1:
                        break;
                    case 2: {
                        String cellValue = cell.getStringCellValue();
                        ag.setAgeDivision(AgeDivision.getAgeDivisionFromCode(cellValue));
                    }
                        break;
                    case 3: {
                        String cellValue = cell.getStringCellValue();
                        if (cellValue != null && !cellValue.trim().isEmpty()) {
                            ag.setGender(cellValue.contentEquals("F") ? Gender.F : Gender.M);
                        }
                    }
                        break;
                    case 4: {
                        long cellValue = Math.round(cell.getNumericCellValue());
                        ag.setMinAge(Math.toIntExact(cellValue));
                    }
                        break;
                    case 5: {
                        long cellValue = Math.round(cell.getNumericCellValue());
                        ag.setMaxAge(Math.toIntExact(cellValue));
                    }
                        break;
                    case 6: {
                        boolean explicitlyActive = cell.getBooleanCellValue();
                        // age division is active according to spreadsheet, unless we are given an explicit
                        // list of age divisions as override (e.g. to setup tests or demos)
                        boolean active = ageDivisionOverride == null ? explicitlyActive
                                : ageDivisionOverride.stream()
                                        .anyMatch((Predicate<AgeDivision>) (ad) -> ad.equals(ag.getAgeDivision()));
                        ag.setActive(active);
                    }
                        break;
                    default: {
                        String cellValue = cell.getStringCellValue();
                        if (cellValue != null && !cellValue.trim().isEmpty()) {
                            String[] parts = cellValue.split("[-_. /]");
                            String catCode = parts.length > 0 ? parts[0] : cellValue;
                            String qualTotal = parts.length > 1 ? parts[1] : "0";
                            Category cat;
                            try {
                                cat = AgeGroupRepository.createCategoryFromTemplate(catCode, ag, templates,
                                        curMin, qualTotal);
                                if (cat != null) {
                                    em.persist(cat);
                                    // logger.debug(cat.longDump());
                                    curMin = cat.getMaximumWeight();
                                }
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
                em.persist(ag);
                iRow++;
            }
            Competition comp = Competition.getCurrent();
            Competition comp2 = em.contains(comp) ? comp : em.merge(comp);
            comp2.setAgeGroupsFileName(localizedName);

            return null;
        });
    }

    static void doInsertAgeGroup(EnumSet<AgeDivision> es, String localizedName) {
        // InputStream localizedResourceAsStream = AgeGroupRepository.class.getResourceAsStream(localizedName);
        InputStream localizedResourceAsStream;
        Logger mainLogger = Main.getStartupLogger();
        try {
            localizedResourceAsStream = ResourceWalker.getResourceAsStream(localizedName);
            try (Workbook workbook = WorkbookFactory
                    .create(localizedResourceAsStream)) {
                logger.info("loading age group configuration file {}", localizedName);
                mainLogger.info("loading age group definitions {}", localizedName);
                Map<String, Category> templates = createCategoryTemplates(workbook);
                createAgeGroups(workbook, templates, es, localizedName);
                workbook.close();
            } catch (Exception e) {
                logger.error("could not process ageGroup configuration\n{}", LoggerUtils./**/stackTrace(e));
                mainLogger.error("could not process ageGroup configuration. See logs for details");
            }
        } catch (FileNotFoundException e1) {
            logger.error("could not find ageGroup configuration\n{}", LoggerUtils./**/stackTrace(e1));
            mainLogger.error("could not find ageGroup configuration. See logs for details");
        }

    }

    private static Object cellName(int iColumn, int iRow) {
        return Character.toString('A' + iColumn) + (Integer.toString(iRow + 1));
    }

}
