/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.category;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class CategoryRepository.
 *
 */
public class CategoryRepository {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(CategoryRepository.class);
    static {
        logger.setLevel(Level.INFO);
    }

    /**
     * Gets the by id.
     *
     * @param id the id
     * @param em the em
     * @return the by id
     */
    @SuppressWarnings("unchecked")
    public static Category getById(Long id, EntityManager em) {
        Query query = em.createQuery("select u from Category u where u.id=:id");
        query.setParameter("id", id);

        return (Category) query.getResultList().stream().findFirst().orElse(null);
    }

    /**
     * Save.
     *
     * @param Category the category
     * @return the category
     */
    public static Category save(Category Category) {
        return JPAService.runInTransaction(em -> em.merge(Category));
    }

    /**
     * Delete.
     *
     * @param Category the category
     */
    public static void delete(Category Category) {
        JPAService.runInTransaction(em -> {
            em.remove(getById(Category.getId(), em));
            return null;
        });
    }

    /**
     * Find all.
     *
     * @return the list
     */
    @SuppressWarnings("unchecked")
    public static List<Category> findAll() {
        return JPAService.runInTransaction(em -> em.createQuery("select c from Category c").getResultList());
    }

    /**
     * Find by name.
     *
     * @param string the string
     * @return the category
     */
    public static Category findByName(String string) {
        return JPAService.runInTransaction(em -> {
            return doFindByName(string, em);
        });
    }

    @SuppressWarnings("unchecked")
    public static Category doFindByName(String string, EntityManager em) {
        Query query = em.createQuery("select c from Category c where lower(name) = lower(:string)");
        query.setParameter("string", string);
        return (Category) query.getResultList().stream().findFirst().orElse(null);
    }

    /**
     * Find filtered.
     *
     * @param name        the last name
     * @param ageDivision the age division
     * @param active      if category is active
     * @param offset      the offset
     * @param limit       the limit
     * @return the list
     */
    public static List<Category> findFiltered(String name, AgeDivision ageDivision, Gender gender, Boolean active,
            int offset, int limit) {
        return JPAService.runInTransaction(em -> {
            return doFindFiltered(em, name, ageDivision, gender, active, offset, limit);
        });
    }

    /**
     * @return active categories
     */
    public static List<Category> findActive() {
        return JPAService.runInTransaction(em -> {
            return doFindFiltered(em, null, null, null, true, -1, -1);
        });
    }

    /**
     * @return active categories
     */
    public static List<Category> findActive(Gender gender) {
        return JPAService.runInTransaction(em -> {
            return doFindFiltered(em, null, null, gender, true, -1, -1);
        });
    }

    public static List<Category> doFindFiltered(EntityManager em, String name, AgeDivision ageDivision, Gender gender,
            Boolean active, int offset, int limit) {
        String qlString = "select c from Category c" + filteringSelection(name, ageDivision, gender, active);
        logger.trace("query = {}", qlString);

        Query query = em.createQuery(qlString);
        setFilteringParameters(name, ageDivision, gender, active, query);
        if (offset >= 0)
            query.setFirstResult(offset);
        if (limit > 0)
            query.setMaxResults(limit);
        @SuppressWarnings("unchecked")
        List<Category> resultList = query.getResultList();
        return resultList;
    }

    /**
     * Count filtered.
     *
     * @param name        the name
     * @param ageDivision the age division
     * @param active      active category
     * @return the int
     */
    public static int countFiltered(String name, AgeDivision ageDivision, Gender gender, Boolean active) {
        return JPAService.runInTransaction(em -> {
            return doCountFiltered(name, ageDivision, gender, active, em);
        });
    }

    public static Integer doCountFiltered(String name, AgeDivision ageDivision, Gender gender, Boolean active,
            EntityManager em) {
        String selection = filteringSelection(name, ageDivision, gender, active);
        String qlString = "select count(c.id) from Category c " + selection;
        logger.trace("count = {}", qlString);
        Query query = em.createQuery(qlString);
        setFilteringParameters(name, ageDivision, gender, true, query);
        int i = ((Long) query.getSingleResult()).intValue();
        return i;
    }

    private static void setFilteringParameters(String name, AgeDivision ageDivision, Gender gender, Boolean active,
            Query query) {
        if (name != null && name.trim().length() > 0) {
            // starts with
            query.setParameter("name", "%" + name.toLowerCase() + "%");
        }
//		if (active != null) {
//			query.setParameter("active", active);
//		}
        if (ageDivision != null) {
            query.setParameter("division", ageDivision); // ageDivision is a string
        }
        if (gender != null) {
            query.setParameter("gender", gender);
        }
//		if (group != null) {
//			query.setParameter("groupId", group.getId()); // group is via a relationship, we join and select on id.
//		}
    }

    private static String filteringSelection(String name, AgeDivision ageDivision, Gender gender, Boolean active) {
        String joins = null; // filteringJoins(group, category);
        String where = filteringWhere(name, ageDivision, gender, active);
        String selection = (joins != null ? " " + joins : "") + (where != null ? " where " + where : "");
        return selection;
    }

    private static String filteringWhere(String name, AgeDivision ageDivision, Gender gender, Boolean active) {
        List<String> whereList = new LinkedList<String>();
        if (ageDivision != null)
            whereList.add("c.ageDivision = :division");
        if (name != null && name.trim().length() > 0)
            whereList.add("lower(c.name) like :name");
        if (active != null && active)
            whereList.add("c.active = true");
        if (gender != null)
            whereList.add("c.gender = :gender");
        if (whereList.size() == 0) {
            return null;
        } else {
            return String.join(" and ", whereList);
        }
    }

//	@SuppressWarnings("unused")
//	private static String filteringJoins(Group group, Category category) {
//		List<String> fromList = new LinkedList<String>();
//		if (group != null) {
//			fromList.add("join c.group g"); // group is via a relationship, join on id
//		}
//		if (fromList.size() == 0) {
//			return "";
//		} else {
//			return String.join(" ", fromList);
//		}
//	}

    /**
     * Insert kids categories.
     *
     * @param curAG  the cur AG
     * @param active the active
     */
    static void insertKidsCategories(EntityManager em, AgeDivision curAG, boolean active) {
        em.persist(new Category(0.0, 35.0, Gender.F, active, curAG, 0));
        em.persist(new Category(35.0, 40.0, Gender.F, active, curAG, 0));
        em.persist(new Category(40.0, 45.0, Gender.F, active, curAG, 0));
        em.persist(new Category(45.0, 49.0, Gender.F, active, curAG, 0));
        em.persist(new Category(49.0, 55.0, Gender.F, active, curAG, 0));
        em.persist(new Category(55.0, 59.0, Gender.F, active, curAG, 0));
        em.persist(new Category(59.0, 64.0, Gender.F, active, curAG, 0));
        em.persist(new Category(64.0, 71.0, Gender.F, active, curAG, 0));
        em.persist(new Category(71.0, 76.0, Gender.F, active, curAG, 0));
        em.persist(new Category(76.0, 999.0, Gender.F, active, curAG, 0));

        em.persist(new Category(0.0, 44.0, Gender.M, active, curAG, 0));
        em.persist(new Category(44.0, 49.0, Gender.M, active, curAG, 0));
        em.persist(new Category(49.0, 55.0, Gender.M, active, curAG, 0));
        em.persist(new Category(55.0, 61.0, Gender.M, active, curAG, 0));
        em.persist(new Category(61.0, 67.0, Gender.M, active, curAG, 0));
        em.persist(new Category(67.0, 73.0, Gender.M, active, curAG, 0));
        em.persist(new Category(73.0, 81.0, Gender.M, active, curAG, 0));
        em.persist(new Category(81.0, 89.0, Gender.M, active, curAG, 0));
        em.persist(new Category(89.0, 96.0, Gender.M, active, curAG, 0));
        em.persist(new Category(96.0, 999.0, Gender.M, active, curAG, 0));
    }

    private static void insertNewCategories(EntityManager em, AgeDivision curAG, boolean active) {
        em.persist(new Category(0.0, 45.0, Gender.F, active, curAG, 191));
        em.persist(new Category(45.0, 49.0, Gender.F, active, curAG, 203));
        em.persist(new Category(49.0, 55.0, Gender.F, active, curAG, 221));
        em.persist(new Category(55.0, 59.0, Gender.F, active, curAG, 232));
        em.persist(new Category(59.0, 64.0, Gender.F, active, curAG, 245));
        em.persist(new Category(64.0, 71.0, Gender.F, active, curAG, 261));
        em.persist(new Category(71.0, 76.0, Gender.F, active, curAG, 272));
        em.persist(new Category(76.0, 81.0, Gender.F, active, curAG, 283));
        em.persist(new Category(81.0, 87.0, Gender.F, active, curAG, 294));
        em.persist(new Category(87.0, 999.0, Gender.F, active, curAG, 320));

        em.persist(new Category(0.0, 55.0, Gender.M, active, curAG, 293));
        em.persist(new Category(55.0, 61.0, Gender.M, active, curAG, 312));
        em.persist(new Category(61.0, 67.0, Gender.M, active, curAG, 331));
        em.persist(new Category(67.0, 73.0, Gender.M, active, curAG, 348));
        em.persist(new Category(73.0, 81.0, Gender.M, active, curAG, 368));
        em.persist(new Category(81.0, 89.0, Gender.M, active, curAG, 387));
        em.persist(new Category(89.0, 96.0, Gender.M, active, curAG, 401));
        em.persist(new Category(96.0, 102.0, Gender.M, active, curAG, 412));
        em.persist(new Category(102.0, 109.0, Gender.M, active, curAG, 424));
        em.persist(new Category(109.0, 999.0, Gender.M, active, curAG, 453));
    }

    /**
     * Insert standard categories.
     * 
     * @param em
     */
    public static void insertStandardCategories(EntityManager em) {
        if (findAll().size() == 0) {
            insertNewCategories(em, AgeDivision.DEFAULT, true);
            insertNewCategories(em, AgeDivision.SENIOR, false);
            insertNewCategories(em, AgeDivision.JUNIOR, false);
            insertYouthCategories(em, AgeDivision.YOUTH, false);
            insertYouthCategories(em, AgeDivision.KIDS, false);
            insertTraditionalCategories(em, AgeDivision.TRADITIONAL, false);
        }
    }

    private static void insertTraditionalCategories(EntityManager em, AgeDivision curAG, boolean active) {
        em.persist(new Category(0.0, 40.0, Gender.F, false, curAG, 0));
        em.persist(new Category(0.0, 44.0, Gender.F, false, curAG, 0));
        em.persist(new Category(0.0, 48.0, Gender.F, active, curAG, 217));
        em.persist(new Category(48.0, 53.0, Gender.F, active, curAG, 233));
        em.persist(new Category(53.0, 58.0, Gender.F, active, curAG, 252));
        em.persist(new Category(63.0, 69.0, Gender.F, active, curAG, 262));
        em.persist(new Category(58.0, 63.0, Gender.F, active, curAG, 276));
        em.persist(new Category(69.0, 75.0, Gender.F, active, curAG, 296));
        em.persist(new Category(75.0, 90.0, Gender.F, active, curAG, 283));
        em.persist(new Category(90.0, 999.0, Gender.F, active, curAG, 348));
        em.persist(new Category(63.0, 999.0, Gender.F, false, curAG, 0));
        em.persist(new Category(69.0, 999.0, Gender.F, false, curAG, 0));
        em.persist(new Category(75.0, 999.0, Gender.F, false, curAG, 0));

        em.persist(new Category(0.0, 46.0, Gender.M, false, curAG, 0));
        em.persist(new Category(0.0, 51.0, Gender.M, false, curAG, 0));
        em.persist(new Category(0.0, 56.0, Gender.M, active, curAG, 307));
        em.persist(new Category(56.0, 62.0, Gender.M, active, curAG, 333));
        em.persist(new Category(62.0, 69.0, Gender.M, active, curAG, 359));
        em.persist(new Category(69.0, 77.0, Gender.M, active, curAG, 380));
        em.persist(new Category(77.0, 85.0, Gender.M, active, curAG, 396));
        em.persist(new Category(85.0, 94.0, Gender.M, active, curAG, 417));
        em.persist(new Category(94.0, 105.0, Gender.M, active, curAG, 437));
        em.persist(new Category(105.0, 999.0, Gender.M, active, curAG, 477));
        em.persist(new Category(77.0, 999.0, Gender.M, false, curAG, 0));
        em.persist(new Category(85.0, 999.0, Gender.M, false, curAG, 0));
        em.persist(new Category(94.0, 999.0, Gender.M, false, curAG, 0));
    }

    /**
     * Insert youth categories.
     * 
     * @param em
     *
     * @param curAG  the cur AG
     * @param active the active
     */
    static void insertYouthCategories(EntityManager em, AgeDivision curAG, boolean active) {
        em.persist(new Category(0.0, 40.0, Gender.F, active, curAG, 0));
        em.persist(new Category(40.0, 45.0, Gender.F, active, curAG, 0));
        em.persist(new Category(45.0, 49.0, Gender.F, active, curAG, 0));
        em.persist(new Category(49.0, 55.0, Gender.F, active, curAG, 0));
        em.persist(new Category(55.0, 59.0, Gender.F, active, curAG, 0));
        em.persist(new Category(59.0, 64.0, Gender.F, active, curAG, 0));
        em.persist(new Category(64.0, 71.0, Gender.F, active, curAG, 0));
        em.persist(new Category(71.0, 76.0, Gender.F, active, curAG, 0));
        em.persist(new Category(76.0, 81.0, Gender.F, active, curAG, 0));
        em.persist(new Category(81.0, 999.0, Gender.F, active, curAG, 0));

        em.persist(new Category(0.0, 49.0, Gender.M, active, curAG, 0));
        em.persist(new Category(49.0, 55.0, Gender.M, active, curAG, 0));
        em.persist(new Category(55.0, 61.0, Gender.M, active, curAG, 0));
        em.persist(new Category(61.0, 67.0, Gender.M, active, curAG, 0));
        em.persist(new Category(67.0, 73.0, Gender.M, active, curAG, 0));
        em.persist(new Category(73.0, 81.0, Gender.M, active, curAG, 0));
        em.persist(new Category(81.0, 89.0, Gender.M, active, curAG, 0));
        em.persist(new Category(89.0, 96.0, Gender.M, active, curAG, 0));
        em.persist(new Category(96.0, 102.0, Gender.M, active, curAG, 0));
        em.persist(new Category(102.0, 999.0, Gender.M, active, curAG, 0));
    }

}
