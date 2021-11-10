/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.records;

import java.io.FileNotFoundException;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

/**
 * RecordRepository.
 *
 */
public class RecordRepository {

    static Logger logger = (Logger) LoggerFactory.getLogger(RecordRepository.class);

    /**
     * Delete.
     *
     * @param RecordEvent the group
     */

    public static void delete(RecordEvent Record) {
        if (Record.getId() == null) {
            return;
        }
        JPAService.runInTransaction(em -> {
            try {
                RecordEvent mRecord = em.contains(Record) ? Record : em.merge(Record);
                em.remove(mRecord);
                em.flush();
            } catch (Exception e) {
                LoggerUtils.logError(logger,e);
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    public static RecordEvent doFindByName(String name, EntityManager em) {
        Query query = em.createQuery("select u from RecordEvent u where u.name=:name");
        query.setParameter("name", name);
        return (RecordEvent) query.getResultList().stream().findFirst().orElse(null);
    }

    /**
     * @return active categories
     */
    public static List<RecordEvent> findActive() {
        List<RecordEvent> findFiltered = findFiltered((String) null, (Gender) null, (AgeDivision) null, (Integer) null,
                true, -1, -1);
        return findFiltered;
    }

    /**
     * Find all.
     *
     * @return the list
     */
    public static List<RecordEvent> findAll() {
        return JPAService.runInTransaction(em -> doFindAll(em));
    }

    public static RecordEvent findByName(String name) {
        return JPAService.runInTransaction(em -> {
            return doFindByName(name, em);
        });
    }

    public static List<RecordEvent> findFiltered(String name, Gender gender, AgeDivision ageDivision, Integer age,
            boolean active, int offset, int limit) {

        List<RecordEvent> findFiltered = JPAService.runInTransaction(em -> {
            String qlString = "select ag from RecordEvent ag"
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
            List<RecordEvent> resultList = query.getResultList();
            return resultList;
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
    public static RecordEvent getById(Long id, EntityManager em) {
        Query query = em.createQuery("select u from CompetitionRecord u where u.id=:id");
        query.setParameter("id", id);
        return (RecordEvent) query.getResultList().stream().findFirst().orElse(null);
    }

    public static void insertRecords(EntityManager em, EnumSet<AgeDivision> es) {
        try {
            String localizedName = ResourceWalker.getLocalizedResourceName("/records/IWF Records.xlsx");
            RecordDefinitionReader.doInsertRecords(es, localizedName);
        } catch (FileNotFoundException e1) {
            throw new RuntimeException(e1);
        }

    }

    public static void reloadDefinitions(String localizedFileName) {
        JPAService.runInTransaction(em -> {
            try {
                Query upd = em.createQuery("update Athlete set category = null");
                upd.executeUpdate();
                upd = em.createQuery("delete from Category");
                upd.executeUpdate();
                upd = em.createQuery("delete from RecordEvent");
                upd.executeUpdate();
                em.flush();
            } catch (Exception e) {
                LoggerUtils.logError(logger,e);
            }
            return null;
        });
        RecordDefinitionReader.doInsertRecords(null, "/records/" + localizedFileName);
        AthleteRepository.resetParticipations();
    }

    /**
     * Save.
     *
     * @param RecordEvent the group
     * @return the group
     */
    public static RecordEvent save(RecordEvent Record) {

        // first clean up the age group
        RecordEvent nRecord = JPAService.runInTransaction(em -> {
            // the category objects that have a null age group must be removed.
            try {
                RecordEvent mRecord = em.merge(Record);
                em.flush();
                return mRecord;
            } catch (Exception e) {
                LoggerUtils.logError(logger,e);
            }
            return null;
        });

        return nRecord;
    }

    @SuppressWarnings("unchecked")
    private static List<RecordEvent> doFindAll(EntityManager em) {
        return em.createQuery("select c from RecordEvent c order by c.ageDivision,c.minAge,c.maxAge").getResultList();
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
