/*******************************************************************************
 * Copyright (rec) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.records;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.query.NativeQuery;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Gender;
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

    public static void clearRecords() throws IOException {
        JPAService.runInTransaction(em -> {
            try {
                em.clear();
                Query upd = em.createNativeQuery("delete from RecordEvent");
                upd.unwrap(NativeQuery.class).addSynchronizedEntityClass(RecordEvent.class);
                upd.executeUpdate();
                em.flush();
            } catch (Exception e) {
                LoggerUtils.logError(logger, e);
            }
            return null;
        });
    }

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
                LoggerUtils.logError(logger, e);
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    public static RecordEvent doFindByName(String name, EntityManager em) {
        Query query = em.createQuery("select rec from RecordEvent rec where rec.name=:name");
        query.setParameter("name", name);
        return (RecordEvent) query.getResultList().stream().findFirst().orElse(null);
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

    public static List<RecordEvent> findFiltered(Gender gender, Integer age, Double bw) {

        List<RecordEvent> findFiltered = JPAService.runInTransaction(em -> {
            String qlString = "select rec from RecordEvent rec "
                    + filteringSelection(gender, age, bw)
                    + " order by rec.gender, rec.ageGrpLower, rec.ageGrpUpper, rec.recordValue desc";
            logger.debug("query = {}", qlString);

            Query query = em.createQuery(qlString);
            setFilteringParameters(gender, age, bw, query);
            @SuppressWarnings("unchecked")
            List<RecordEvent> resultList = query.getResultList();
            return resultList;
        });
        return findFiltered;
    }

    /**
     * Gets record by id
     *
     * @param id the id
     * @param em entity manager
     * @return the group, null if not found
     */
    @SuppressWarnings("unchecked")
    public static RecordEvent getById(Long id, EntityManager em) {
        Query query = em.createQuery("select rec from RecordEvent rec where rec.id=:id");
        query.setParameter("id", id);
        return (RecordEvent) query.getResultList().stream().findFirst().orElse(null);
    }

    public static void reloadDefinitions(String localizedFileName) throws IOException {
        clearRecords();
        InputStream is = ResourceWalker.getResourceAsStream(localizedFileName);
        RecordDefinitionReader.readZip(is);
    }

    /**
     * Save.
     *
     * @param RecordEvent the group
     * @return the group
     */
    public static RecordEvent save(RecordEvent Record) {
        RecordEvent nRecord = JPAService.runInTransaction(em -> {
            // the category objects that have a null age group must be removed.
            try {
                RecordEvent mRecord = em.merge(Record);
                em.flush();
                return mRecord;
            } catch (Exception e) {
                LoggerUtils.logError(logger, e);
            }
            return null;
        });

        return nRecord;
    }

    @SuppressWarnings("unchecked")
    private static List<RecordEvent> doFindAll(EntityManager em) {
        return em.createQuery(
                "select rec from RecordEvent rec order by rec.recordFederation,rec.gender,rec.ageGrpLower,rec.ageGrpUpper,rec.bwCatUpper")
                .getResultList();
    }

    private static String filteringSelection(Gender gender, Integer age, Double bw) {
        String joins = null;
        String where = filteringWhere(gender, age, bw);
        String selection = (joins != null ? " " + joins : "") + (where != null ? " where " + where : "");
        return selection;
    }

    private static String filteringWhere(Gender gender, Integer age, Double bw) {
        List<String> whereList = new LinkedList<>();
        if (gender != null) {
            whereList.add("rec.gender = :gender");
        }
        if (age != null) {
            whereList.add("(rec.ageGrpLower <= :age) and (rec.ageGrpUpper >= :age)");
        }
        if (bw != null) {
            whereList.add("(rec.bwCatLower*1.0 <= :bw) and (rec.bwCatUpper*1.0 >= :bw)");
        }
        if (whereList.size() == 0) {
            return null;
        } else {
            return String.join(" and ", whereList);
        }
    }

    private static void setFilteringParameters(Gender gender, Integer age, Double bw, Query query) {
        if (age != null) {
            query.setParameter("age", age);
        }
        if (bw != null) {
            query.setParameter("bw", bw);
        }
        if (gender != null) {
            query.setParameter("gender", gender);
        }
    }
    
    public static RecordEvent[][] computeRecords(Gender gender, Integer age, Double bw){
        List<RecordEvent> records = findFiltered(gender, age, bw);
        return buildRecordTable(records);
    }

    /**
     * Table where rows are record types, heaviest at bottom and columns are categories, youngest first.
     * @param records
     * @return
     */
    private static RecordEvent[][] buildRecordTable(List<RecordEvent> records) {
        records.sort((r1,r2) -> {return 0;});
        
        return null;
    }

}
