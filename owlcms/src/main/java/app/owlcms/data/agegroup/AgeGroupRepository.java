/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.agegroup;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * AgeGroupRepository.
 *
 */
public class AgeGroupRepository {

    static Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupRepository.class);

    private static void createAgeGroups(Workbook workbook, Map<String, Category> templates, EnumSet<AgeDivision> es) {
        DataFormatter dataFormatter = new DataFormatter();

        JPAService.runInTransaction(em -> {
            Sheet sheet = workbook.getSheetAt(1);
            Iterator<Row> rowIterator = sheet.rowIterator();
            int iRow = 0;
            while (rowIterator.hasNext()) {
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
                        String cellValue = dataFormatter.formatCellValue(cell);
                        ag.setCode(cellValue.trim());
                    }
                        break;
                    case 1:
                        break;
                    case 2: {
                        String cellValue = dataFormatter.formatCellValue(cell);
                        ag.setAgeDivision(AgeDivision.getAgeDivisionFromCode(cellValue));
                    }
                        break;
                    case 3: {
                        String cellValue = dataFormatter.formatCellValue(cell);
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
                        String cellValue = dataFormatter.formatCellValue(cell);
                        boolean explicitlyActive = cellValue != null ? "true".contentEquals(cellValue.toLowerCase())
                                : false;
                        Predicate<AgeDivision> adp = (ad) -> ad.equals(ag.getAgeDivision());
                        boolean active = es == null ? explicitlyActive : es.stream().anyMatch(adp);
                        ag.setActive(active);
                    }
                        break;
                    default: {
                        String cellValue = dataFormatter.formatCellValue(cell);
                        if (cellValue != null && !cellValue.trim().isEmpty()) {
                            Category cat = createCategoryFromTemplate(cellValue, ag, templates, curMin);
                            if (cat != null) {
                                em.persist(cat);
                                logger.trace(cat.longDump());
                                curMin = cat.getMaximumWeight();
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
            return null;
        });
    }

    private static Category createCategoryFromTemplate(String cellValue, AgeGroup ag, Map<String, Category> templates,
            double curMin) {
        Category template = templates.get(cellValue);
        if (template == null) {
            logger.warn("template {} not found", cellValue);
            return null;
        } else {
            try {
                Category newCat = new Category();
                BeanUtils.copyProperties(newCat, template);
                newCat.setMinimumWeight(curMin);
                newCat.setCode(ag.getCode() + "_" + template.getCode());
                newCat.setAgeGroup(ag);
                newCat.setActive(ag.isActive());
                return newCat;
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.error("cannot create category from template\n{}", LoggerUtils.stackTrace(e));
                return null;
            }
        }
    }

    /**
     * Create category templates that will be copied to instantiate the actual categories. The world records are read
     * and included in the template.
     *
     * @param workbook
     * @return
     */
    private static Map<String, Category> createCategoryTemplates(Workbook workbook) {
        Map<String, Category> categoryMap = new HashMap<>();
        DataFormatter dataFormatter = new DataFormatter();
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        int iRow = 0;
        while (rowIterator.hasNext()) {
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
                    c.setCode(cellValue.trim());
                    categoryMap.put(cellValue, c);
                }
                    break;
                case 1: {
                    String cellValue = dataFormatter.formatCellValue(cell);
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
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

    /**
     * Delete.
     *
     * @param AgeGroup the group
     */

    public static void delete(AgeGroup groupe) {
        if (groupe.getId() == null) {
            return;
        }
        JPAService.runInTransaction(em -> {
            try {
                List<Category> cats = groupe.getCategories();
                for (Category c : cats) {
                    c.setAgeGroup(null);
                    em.remove(c);
                }
                em.remove(groupe);
                em.flush();
                em.remove(em.contains(groupe) ? groupe : em.merge(groupe));
                em.flush();
            } catch (Exception e) {
                logger.error(LoggerUtils.stackTrace(e));
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    public static AgeGroup doFindByName(String name, EntityManager em) {
        Query query = em.createQuery("select u from AgeGroup u where u.name=:name");
        query.setParameter("name", name);
        return (AgeGroup) query.getResultList().stream().findFirst().orElse(null);
    }

    private static String filteringSelection(String name, Gender gender, AgeDivision ageDivision, Integer age,
            Boolean active) {
        String joins = null;
        String where = filteringWhere(name, ageDivision, age, gender, active);
        String selection = (joins != null ? " " + joins : "") + (where != null ? " where " + where : "");
        return selection;
    }

    private static String filteringWhere(String name, AgeDivision ageDivision, Integer age, Gender gender,
            Boolean active) {
        List<String> whereList = new LinkedList<>();
        if (ageDivision != null) {
            whereList.add("ag.ageDivision = :division");
        }
        if (name != null && name.trim().length() > 0) {
            whereList.add("lower(ag.name) like :name");
        }
        if (active != null && active) {
            whereList.add("ag.active = :active");
        }
        if (gender != null) {
            whereList.add("ag.gender = :gender");
        }

        if (age != null) {
            whereList.add("(ag.minAge <= :age) and (ag.maxAge >= :age)");
        }
        if (whereList.size() == 0) {
            return null;
        } else {
            return String.join(" and ", whereList);
        }
    }

    /**
     * @return active categories
     */
    public static List<AgeGroup> findActive() {
        List<AgeGroup> findFiltered = findFiltered((String) null, (Gender) null, (AgeDivision) null, (Integer) null,
                true, -1, -1);
        return findFiltered;
    }

    /**
     * Find all.
     *
     * @return the list
     */
    @SuppressWarnings("unchecked")
    public static List<AgeGroup> findAll() {
        return JPAService.runInTransaction(em -> em
                .createQuery("select c from AgeGroup c order by c.ageDivision,c.minAge,c.maxAge").getResultList());
    }

    public static AgeGroup findByName(String name) {
        return JPAService.runInTransaction(em -> {
            return doFindByName(name, em);
        });
    }

    public static List<AgeGroup> findFiltered(String name, Gender gender, AgeDivision ageDivision, Integer age,
            boolean active, int offset, int limit) {

        List<AgeGroup> findFiltered = JPAService.runInTransaction(em -> {
            String qlString = "select ag from AgeGroup ag"
                    + filteringSelection(name, gender, ageDivision, age, active)
                    + " order by ag.ageDivision, ag.gender, ag.minAge, ag.maxAge";
            logger.debug("query = {}", qlString);

            Query query = em.createQuery(qlString);
            setFilteringParameters(name, gender, ageDivision, age, active, query);
            if (offset >= 0) {
                query.setFirstResult(offset);
            }
            if (limit > 0) {
                query.setMaxResults(limit);
            }
            @SuppressWarnings("unchecked")
            List<AgeGroup> resultList = query.getResultList();
            return resultList;
        });
        findFiltered.sort((ag1, ag2) -> {
            int compare = 0;
            compare = ag1.getAgeDivision().compareTo(ag2.getAgeDivision());
            if (compare != 0) return -compare; // most generic first
            return ag1.compareTo(ag2);
        });
        return findFiltered;
    }

    /**
     * Gets group by id
     *
     * @param id the id
     * @param em entity manager
     * @return the group, null if not found
     */
    @SuppressWarnings("unchecked")
    public static AgeGroup getById(Long id, EntityManager em) {
        Query query = em.createQuery("select u from CompetitionAgeGroup u where u.id=:id");
        query.setParameter("id", id);
        return (AgeGroup) query.getResultList().stream().findFirst().orElse(null);
    }

    public static void insertAgeGroups(EntityManager em, EnumSet<AgeDivision> es) {
        try {
            Workbook workbook = WorkbookFactory
                    .create(AgeGroupRepository.class.getResourceAsStream("/config/AgeGroups.xlsx"));
            Map<String, Category> templates = createCategoryTemplates(workbook);
            createAgeGroups(workbook, templates, es);
            workbook.close();

        } catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
            logger.error("could not process ageGroup configuration\n{}", LoggerUtils.stackTrace(e));
        }

    }

    /**
     * Save.
     *
     * @param AgeGroup the group
     * @return the group
     */
    public static AgeGroup save(AgeGroup AgeGroup) {
        return JPAService.runInTransaction(em -> em.merge(AgeGroup));
    }

    private static void setFilteringParameters(String name, Gender gender, AgeDivision ageDivision, Integer age,
            Boolean active, Query query) {
        if (name != null && name.trim().length() > 0) {
            // starts with
            query.setParameter("name", "%" + name.toLowerCase() + "%");
        }
        if (active != null && active) {
            query.setParameter("active", active);
        }
        if (age != null) {
            query.setParameter("age", age);
        }
        if (ageDivision != null) {
            query.setParameter("division", ageDivision); // ageDivision is a string
        }
        if (gender != null) {
            query.setParameter("gender", gender);
        }
    }

}
