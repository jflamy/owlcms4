/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
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
import java.util.stream.Collectors;

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
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.spreadsheet.PAthlete;
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
     * Save.
     *
     * @param AgeGroup the group
     * @return the group
     */
    public static AgeGroup add(AgeGroup ageGroup) {
        // first clean up the age group
        AgeGroup nAgeGroup = JPAService.runInTransaction(em -> {
            try {
                em.persist(ageGroup);
            } catch (Exception e) {
                LoggerUtils.logError(logger, e);
            }
            return null;
        });
        return nAgeGroup;
    }

    public static List<AgeDivision> allAgeDivisionsForAllAgeGroups() {
        return JPAService.runInTransaction((em) -> {
            TypedQuery<AgeDivision> q = em.createQuery(
                    "select distinct ag.ageDivision from Participation p join p.category c join c.ageGroup ag",
                    AgeDivision.class);
            List<AgeDivision> resultSet = q.getResultList();
            return resultSet;
        });
    }

    /**
     * List all participations for the categories present in the age group
     *
     * @param agPrefix age range (no gender) ex: JR, SR, YTH, U15, M55
     * @param g        gender
     * @return
     */
    public static List<Participation> allParticipationsForAgeGroup(String agPrefix, Gender g) {
        return JPAService.runInTransaction((em) -> {
            String categoriesFromAgegroup = "(select distinct c2 from Athlete b join b.group g join b.participations p join p.category c2 join c2.ageGroup ag where ag.code = :ageGroupCode and c2.id = c.id)";
            TypedQuery<Participation> q = em.createQuery(
                    "select distinct p from Participation p join p.athlete a join p.category c where a.gender = :gender and exists "
                            + categoriesFromAgegroup,
                    Participation.class);
            q.setParameter("ageGroupCode", agPrefix);
            q.setParameter("gender", g);
            List<Participation> resultSet = q.getResultList();
            return resultSet;
        });
    }
    // allParticipationsForAgeDivision

    public static List<Participation> allParticipationsForAgeGroupAgeDivision(String ageGroupPrefix,
            AgeDivision ageDivision) {
        List<Participation> participations = JPAService.runInTransaction(em -> {

            List<String> whereList = new ArrayList<>();
            if (ageGroupPrefix != null && !ageGroupPrefix.isBlank()) {
                whereList.add("ag.code = :ageGroupPrefix");
            }
            if (ageDivision != null) {
                whereList.add("ag.ageDivision = :ageDivision");
            }
            String whereClause = "";
            if (whereList.size() > 0) {
                whereClause = " where " + whereList.stream().collect(Collectors.joining(" and "));
            }

            TypedQuery<Participation> q = em.createQuery(
                    "select distinct p from Participation p join p.category c join c.ageGroup ag "
                            + whereClause,
                    Participation.class);
            if (ageGroupPrefix != null && !ageGroupPrefix.isBlank()) {
                q.setParameter("ageGroupPrefix", ageGroupPrefix);
            }
            if (ageDivision != null) {
                q.setParameter("ageDivision", ageDivision);
            }

            List<Participation> resultSet = q.getResultList();
            return resultSet;
        });
        return participations;
    }

    /**
     * List all participations for the categories present in the age group
     *
     * @param agPrefix age range (no gender) ex: JR, SR, YTH, U15, M55
     * @param g        gender
     * @return
     */
    public static List<Athlete> allPAthletesForAgeGroup(String agPrefix) {
        if (agPrefix == null || agPrefix.isBlank()) {
            List<Athlete> athletes = AthleteRepository.findAll();
            return athletes.stream().map(a -> new PAthlete(a.getMainRankings())).collect(Collectors.toList());
        } else {
            List<Participation> parts = JPAService.runInTransaction((em) -> {
                String categoriesFromAgegroup = "(select distinct c2 from Athlete b join b.group g join b.participations p join p.category c2 join c2.ageGroup ag where ag.code = :ageGroupCode and c2.id = c.id)";
                TypedQuery<Participation> q = em.createQuery(
                        "select distinct p from Participation p join p.athlete a join p.category c where exists "
                                + categoriesFromAgegroup,
                        Participation.class);
                q.setParameter("ageGroupCode", agPrefix);
                List<Participation> resultSet = q.getResultList();
                return resultSet;
            });
            return parts.stream().map(p -> new PAthlete(p)).collect(Collectors.toList());
        }
    }

    /**
     * List all participations for the categories present in the age group
     *
     * @param agPrefix age range (no gender) ex: JR, SR, YTH, U15, M55
     * @param g        gender
     * @return
     */
    public static List<PAthlete> allPAthletesForAgeGroup(String agPrefix, Gender g) {
        List<Participation> parts = JPAService.runInTransaction((em) -> {
            String categoriesFromAgegroup = "(select distinct c2 from Athlete b join b.group g join b.participations p join p.category c2 join c2.ageGroup ag where ag.code = :ageGroupCode and c2.id = c.id)";
            TypedQuery<Participation> q = em.createQuery(
                    "select distinct p from Participation p join p.athlete a join p.category c where a.gender = :gender and exists "
                            + categoriesFromAgegroup,
                    Participation.class);
            q.setParameter("ageGroupCode", agPrefix);
            q.setParameter("gender", g);
            List<Participation> resultSet = q.getResultList();
            return resultSet;
        });
        return parts.stream().map(p -> new PAthlete(p)).collect(Collectors.toList());
    }
    
    /**
     * List all participations for the categories present in the age group
     *
     * @param agPrefix age range (no gender) ex: JR, SR, YTH, U15, M55
     * @param g        gender
     * @return
     */
    public static List<PAthlete> allPAthletesForGroup(Group gr) {
        List<Participation> parts = JPAService.runInTransaction((em) -> {
           TypedQuery<Participation> q = em.createQuery(
                    "select distinct p from Participation p join p.athlete a where a.group = :competitionGroup",
                    Participation.class);
            q.setParameter("competitionGroup", gr);
            List<Participation> resultSet = q.getResultList();
            return resultSet;
        });
        return parts.stream().map(p -> new PAthlete(p)).collect(Collectors.toList());
    }

    public static List<Athlete> allWeighedInPAthletesForAgeGroupAgeDivision(String ageGroupPrefix, AgeDivision ageDivision) {
        List<Participation> participations = allParticipationsForAgeGroupAgeDivision(ageGroupPrefix, ageDivision);
        List<Athlete> collect = participations.stream().map(p -> new PAthlete(p))
                .filter(a -> a.getBodyWeight() != null && a.getBodyWeight() > 0.1).collect(Collectors.toList());
        return collect;
    }
    
    public static List<Athlete> allPAthletesForAgeGroupAgeDivision(String ageGroupPrefix, AgeDivision ageDivision) {
        List<Participation> participations = allParticipationsForAgeGroupAgeDivision(ageGroupPrefix, ageDivision);
        List<Athlete> collect = participations.stream().map(p -> new PAthlete(p)).collect(Collectors.toList());
        return collect;
    }

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
                LoggerUtils.logError(logger, e);
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

    public static List<String> findActiveAndUsed(AgeDivision ageDivisionValue) {
       
        return JPAService.runInTransaction((em) -> {
            if (ageDivisionValue == null) {
                TypedQuery<String> q = em.createQuery(
                        "select distinct ag.code from Participation p join p.category c join c.ageGroup ag",
                        String.class);
                List<String> resultSet = q.getResultList();
                return resultSet;
            } else {
                TypedQuery<String> q = em.createQuery(
                    "select distinct ag.code from Participation p join p.category c join c.ageGroup ag where ag.ageDivision = :agv",
                    String.class);
                q.setParameter("agv", ageDivisionValue);
                List<String> resultSet = q.getResultList();
                return resultSet;
            }

        });
    }

    /**
     * Fetch all age groups present in the current group
     *
     * @param g
     * @return
     */
    public static List<AgeGroup> findAgeGroups(Group g) {
        if (g == null) {
            return new ArrayList<>();
        } else {
            return JPAService.runInTransaction((em) -> {
                TypedQuery<AgeGroup> q = em.createQuery(
                        "select distinct ag from Athlete a join a.group g join a.participations p join p.category c join c.ageGroup ag where g.id = :groupId order by ag.maxAge, ag.minAge",
                        AgeGroup.class);
                q.setParameter("groupId", g.getId());
                return q.getResultList();
            });
        }
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
            //ignore
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
                LoggerUtils.logError(logger, e);
            }
            return null;
        });
        AgeGroupDefinitionReader.doInsertAgeGroup(null, "/agegroups/" + localizedFileName);
        AthleteRepository.resetParticipations();
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
            try {
                return cleanUp(ageGroup, em);
            } catch (Exception e) {
                LoggerUtils.logError(logger, e);
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
                newCat.setAgeGroup(ag);
                // logger.debug("code = {} {}",newCat.getCode(), newCat.getComputedCode());
                ag.addCategory(newCat);
                newCat.setActive(ag.isActive());
                try {
                    newCat.setQualifyingTotal(Integer.parseInt(qualTotal));
                } catch (NumberFormatException e) {
                    throw new Exception(e);
                }
                // logger.debug(newCat.dump());
                return newCat;
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.error("cannot create category from template\n{}", LoggerUtils./**/stackTrace(e));
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

    private static AgeGroup cleanUp(AgeGroup ageGroup, EntityManager em) {
        // cascade carefully the deleted categories.
        Long id = ageGroup.getId();
        if (id == null) {
            return null;
        }
        AgeGroup old = em.find(AgeGroup.class, id);
        List<Category> oldCats = old.getAllCategories();
        // logger.debug("old categories {}", oldCats);
        List<Category> newCats = ageGroup.getAllCategories();
        // logger.debug("new categories {}", newCats);

        List<Category> obsolete = new ArrayList<>();
        for (Category oldC : oldCats) {
            boolean found = false;
            for (Category newC : newCats) {
                Long newId = newC.getId();
                if (newId == null) {
                    // new category without an Id. Not obsolete.
                    continue;
                }
                found = Long.compare(newId, oldC.getId()) == 0;
                if (found) {
                    break;
                }
            }
            if (!found) {
                obsolete.add(oldC);
            }
        }

        for (Category newC : newCats) {
            em.merge(newC);
        }

        // logger.debug("obsolete categories {}",obsolete);
        for (Category obs : obsolete) {
            cascadeAthleteCategoryDisconnect(em, obs);
            cascadeCategoryRemoval(em, old, obs);
        }

        AgeGroup mAgeGroup = em.merge(ageGroup);
        // List<Category> mergedCats = mAgeGroup.getCategories();
        // logger.debug("merged categories {}", mergedCats);

        em.flush();
        return mAgeGroup;
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

}
