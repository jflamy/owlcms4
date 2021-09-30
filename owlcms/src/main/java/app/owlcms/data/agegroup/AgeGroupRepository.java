/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

/**
 * AgeGroupRepository.
 *
 */
public class AgeGroupRepository {

    static Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupRepository.class);

    /**
     * Delete.
     *
     * @param AgeGroup the group
     */

    public static void delete(AgeGroup ageGroup) {
        if (ageGroup.getId() == null) {
            return;
        }
        JPAService.runInTransaction(em -> {
            try {
                AgeGroup mAgeGroup = em.contains(ageGroup) ? ageGroup : em.merge(ageGroup);
                List<Category> cats = ageGroup.getCategories();
                for (Category c : cats) {
                    Category mc = em.contains(c) ? c : em.merge(c);
                    cascadeCategoryRemoval(em, mAgeGroup, mc);
                }
                em.remove(mAgeGroup);
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
    public static List<AgeGroup> findAll() {
        return JPAService.runInTransaction(em -> doFindAll(em));
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
            ObjectUtils.compare(ag1.getAgeDivision(), ag2.getAgeDivision());
            if (compare != 0) {
                return -compare; // most generic first
            }
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
            String localizedName = ResourceWalker.getLocalizedResourceName("/agegroups/AgeGroups.xlsx");
            AgeGroupDefinitionReader.doInsertAgeGroup(es, localizedName);
        } catch (FileNotFoundException e1) {
            throw new RuntimeException(e1);
        }
    }
    
    public static void insertAgeGroups(EntityManager em, EnumSet<AgeDivision> es, String resourceName) {
        try {
            String localizedName = ResourceWalker.getLocalizedResourceName(resourceName);
            AgeGroupDefinitionReader.doInsertAgeGroup(es, localizedName);
        } catch (FileNotFoundException e1) {
            throw new RuntimeException(e1);
        }
    }

    public static void reloadDefinitions(String localizedFileName) {
        JPAService.runInTransaction(em -> {
            List<Athlete> athletes = AthleteRepository.doFindAll(em);
            for (Athlete a : athletes) {
                a.setCategory(null);
                a.setEligibleCategories(null);
                em.merge(a);
            }
            em.flush();
            Competition.getCurrent().setRankingsInvalid(true);
            return null;
        });
        JPAService.runInTransaction(em -> {
            try {
                Query upd = em.createQuery("delete from Category");
                upd.executeUpdate();
                upd = em.createQuery("delete from AgeGroup");
                upd.executeUpdate();
            } catch (Exception e) {
                logger.error(LoggerUtils.stackTrace(e));
            }
            return null;
        });
        AgeGroupDefinitionReader.doInsertAgeGroup(null, "/agegroups/" + localizedFileName);
        AthleteRepository.resetCategories();
    }

    /**
     * Save.
     *
     * @param AgeGroup the group
     * @return the group
     */
    public static AgeGroup save(AgeGroup ageGroup) {

        // first clean up the age group
        AgeGroup nAgeGroup = JPAService.runInTransaction(em -> {
            // the category objects that have a null age group must be removed.
            try {
                AgeGroup mAgeGroup = em.merge(ageGroup);
                List<Category> ageGroupCategories = mAgeGroup.getAllCategories();
                List<Category> obsolete = new ArrayList<>();
                for (Category c : ageGroupCategories) {
                    Category nc = em.contains(c) ? c : em.merge(c);
                    if (nc.getAgeGroup() == null) {
                        cascadeAthleteCategoryDisconnect(em, nc);
                        obsolete.add(nc);
                    } else if (nc.getId() == null) {
                        // new category
                        logger.debug("creating category for {}-{}", nc.getMinimumWeight(), nc.getMaximumWeight());
                        em.persist(nc);
                    } else {
                        logger.debug("updating category for {}-{}", nc.getMinimumWeight(), nc.getMaximumWeight());
                    }
                }

                for (Category nc : obsolete) {
                    cascadeCategoryRemoval(em, mAgeGroup, nc);
                }

                em.flush();
                return mAgeGroup;
            } catch (Exception e) {
                logger.error(LoggerUtils.stackTrace(e));
            }
            return null;
        });

        return nAgeGroup;
    }

    static void cascadeCategoryRemoval(EntityManager em, AgeGroup mAgeGroup, Category nc) {
        // so far we have not categories removed from the age group, time to do so
        logger.debug("removing category {} from age group", nc.getId());
        mAgeGroup.removeCategory(nc);
        em.remove(nc);
    }

    static Category createCategoryFromTemplate(String catCode, AgeGroup ag, Map<String, Category> templates,
            double curMin, String qualTotal) throws Exception {
        Category template = templates.get(catCode);
        if (template == null) {
            logger.error("template {} not found", catCode);
            return null;
        } else {
            try {
                Category newCat = new Category(template);
                newCat.setMinimumWeight(curMin);
                newCat.setCode(ag.getCode() + "_" + template.getCode());
                ag.addCategory(newCat);
                newCat.setActive(ag.isActive());
                try {
                    newCat.setQualifyingTotal(Integer.parseInt(qualTotal));
                } catch (NumberFormatException e) {
                    throw new Exception(e);
                }
//                logger.debug(newCat.dump());
                return newCat;
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.error("cannot create category from template\n{}", LoggerUtils.stackTrace(e));
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void cascadeAthleteCategoryDisconnect(EntityManager em, Category c) {
        Category nc = em.merge(c);

        String qlString = "select a from Athlete a where a.category = :category";
        Query query = em.createQuery(qlString);
        query.setParameter("category", nc);
        List<Athlete> as = query.getResultList();
        for (Athlete a : as) {
            logger.debug("removing athlete {} from category {}", a, nc.getId());
            Athlete na = em.contains(a) ? a : em.merge(a);
            na.setCategory(null);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<AgeGroup> doFindAll(EntityManager em) {
        return em.createQuery("select c from AgeGroup c order by c.ageDivision,c.minAge,c.maxAge").getResultList();
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
    
//    /**
//     * List all athletes for the categories present in the age group
//     * 
//     * @param agPrefix  age group code (JR, SR, U15) etc, excluding gender.
//     * @param g the gender needed.
//     * @return
//     */
//    private static List<Athlete> allAthletesForGlobalRanking(String agPrefix, Gender g) {
//        return JPAService.runInTransaction((em) -> {
//            String categoriesFromAgegroup = "(select distinct c2 from Athlete b join b.group g join b.participations p join p.category c2 join c2.ageGroup ag where ag.code = :ageGroupCode and c2.id = c.id)";
//            TypedQuery<Athlete> q = em.createQuery(
//                    "select distinct a from Athlete a join a.participations p join p.category c where a.gender = :gender and exists "
//                            + categoriesFromAgegroup,
//                    Athlete.class);
//            q.setParameter("ageGroupCode", agPrefix);
//            q.setParameter("gender", g);
//            return q.getResultList();
//        });
//    }
    
    /**
     * List all participations for the categories present in the age group
     * 
     * @param agPrefix age range (no gender) ex: JR, SR, YTH, U15, M55
     * @param g  gender
     * @return
     */
    public static List<Participation> allParticipationsForAgeGroup(String agPrefix, Gender g) {
        return (List<Participation>) JPAService.runInTransaction((em) -> {
            String categoriesFromAgegroup = "(select distinct c2 from Athlete b join b.group g join b.participations p join p.category c2 join c2.ageGroup ag where ag.code = :ageGroupCode and c2.id = c.id)";
            TypedQuery<Participation> q = em.createQuery(
                    "select distinct p from Participation p join p.athlete a join p.category c where a.gender = :gender and exists "
                            + categoriesFromAgegroup, Participation.class);
            q.setParameter("ageGroupCode", agPrefix);
            q.setParameter("gender", g);
            List<Participation> resultSet =  q.getResultList();
            return resultSet;
        });
    }

}
