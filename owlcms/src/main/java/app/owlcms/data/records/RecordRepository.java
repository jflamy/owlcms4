/*******************************************************************************
 * Copyright (rec) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.records;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.query.NativeQuery;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

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

    public static List<RecordEvent>[][] computeRecords(Gender gender, Integer age, Double bw) {
        List<RecordEvent> records = findFiltered(gender, age, bw);
        return buildRecordTable(records);
    }

    /**
     * Table where rows are record types, heaviest at bottom and columns are categories, youngest first.
     * 
     * @param records
     * @return
     */
    public static List<RecordEvent>[][] buildRecordTable(List<RecordEvent> records) {
        // order record names according to heaviest total
        Map<String, Double> recordTypeMaxTotal = new HashMap<>();
        Multimap<Integer, RecordEvent> recordsByAgeWeight = ArrayListMultimap.create();
        for (RecordEvent re : records) {
            Double curMax = recordTypeMaxTotal.get(re.getRecordName());
            if (re.getRecordLift() == Ranking.TOTAL && (curMax == null || re.getRecordValue() > curMax)) {
                recordTypeMaxTotal.put(re.getRecordName(), re.getRecordValue());
            }
            recordsByAgeWeight.put(re.getAgeGrpLower() * 1000000 + re.getAgeGrpUpper() * 1000 + re.getBwCatUpper(), re);
        }
        List<String> rowOrder = recordTypeMaxTotal.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e1.getValue(), e2.getValue()))
                .map(e -> e.getKey()).collect(Collectors.toList());
        List<Integer> columnOrder = recordsByAgeWeight.keySet().stream().sorted((e1, e2) -> Integer.compare(e1, e2))
                .collect(Collectors.toList());
        logger.warn("rowOrder {}", rowOrder);
        logger.warn("columnOrder {}", columnOrder);

        @SuppressWarnings("unchecked")
        List<RecordEvent>[][] recordTable = new ArrayList[rowOrder.size()][columnOrder.size()];

        for (int j = 0; j < columnOrder.size(); j++) {
            Collection<RecordEvent> columnRecords = recordsByAgeWeight.get(columnOrder.get(j));
            for (int i = 0; i < rowOrder.size(); i++) {
                String curRowRecordName = rowOrder.get(i);
                List<RecordEvent> recordFound = columnRecords.stream()
                        .filter(r -> r.getRecordName() == curRowRecordName).collect(Collectors.toList());
                recordFound.sort((r1, r2) -> r1.getRecordLift().compareTo(r2.getRecordLift()));
                recordTable[i][j] = recordFound;
            }
        }

        return recordTable;
    }

    public static JsonArray buildRecordJson(List<RecordEvent> records) {
        List<RecordEvent>[][] recordTable = buildRecordTable(records);

        JsonArray rows = Json.createArray();
        for (int i = 0; i < recordTable.length; i++) {
            JsonArray rowCols = Json.createArray();
            for (int j = 0; j < recordTable[i].length; j++) {
                JsonObject cell = Json.createObject();
                for (RecordEvent rec : recordTable[i][j]) {
                    cell.put("record", rec.getRecordName());
                    cell.put("cat", rec.getAgeGrp() + " " + rec.getBwCatUpper());
                    cell.put(rec.getRecordLift().name(), rec.getRecordValue());
                }
                rowCols.set(j, cell);
            }
            rows.set(i, rowCols);
        }
        return rows;
    }

}
