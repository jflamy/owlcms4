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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * RecordRepository.
 *
 */
public class RecordRepository {

    static Logger logger = (Logger) LoggerFactory.getLogger(RecordRepository.class);

    public static JsonValue buildRecordJson(List<RecordEvent> records, Integer snatchRequest, Integer cjRequest,
            Integer totalRequest) {
        
        if (records == null || records.isEmpty()) {
            return Json.createNull();
        }
        
        Multimap<Integer, RecordEvent> recordsByAgeWeight = ArrayListMultimap.create();
        TreeMap<String, String> rowOrder = new TreeMap<>();
        for (RecordEvent re : records) {
            // rows are ordered according to file name.
            rowOrder.put(re.getFileName(), re.getRecordName());
            // synthetic key to arrange records in correct column.
            recordsByAgeWeight.put(re.getAgeGrpLower() * 1000000 + re.getAgeGrpUpper() * 1000 + re.getBwCatUpper(), re);
        }

        // order columns in ascending age groups;
        List<Integer> columnOrder = recordsByAgeWeight.keySet().stream().sorted((e1, e2) -> Integer.compare(e1, e2))
                .collect(Collectors.toList());

        @SuppressWarnings("unchecked")
        List<RecordEvent>[][] recordTable = new ArrayList[rowOrder.size()][columnOrder.size()];
        ArrayList<String> rowRecordNames = new ArrayList<>(rowOrder.values());

        for (int j1 = 0; j1 < columnOrder.size(); j1++) {
            Collection<RecordEvent> recordsForCurrentCategory = recordsByAgeWeight.get(columnOrder.get(j1));
            for (int i1 = 0; i1 < rowOrder.size(); i1++) {
                String curRowRecordName = rowRecordNames.get(i1);

                List<RecordEvent> recordFound = recordsForCurrentCategory.stream()
                        .filter(r -> r.getRecordName().contentEquals(curRowRecordName)).collect(Collectors.toList());
                
                // put them in snatch/cj/total order (not needed really), then largest record first in case of multiple
                // records
                recordFound.sort(Comparator.comparing(RecordEvent::getRecordLift)
                        .thenComparing(Comparator.comparing(RecordEvent::getRecordValue).reversed()));

                // keep the largest record
                List<RecordEvent> maxRecordFound = new ArrayList<>();
                recordFound.stream().filter(r -> r.getRecordLift() == Ranking.SNATCH).findFirst()
                        .ifPresent(r -> maxRecordFound.add(r));
                recordFound.stream().filter(r -> r.getRecordLift() == Ranking.CLEANJERK).findFirst()
                        .ifPresent(r -> maxRecordFound.add(r));
                recordFound.stream().filter(r -> r.getRecordLift() == Ranking.TOTAL).findFirst()
                        .ifPresent(r -> maxRecordFound.add(r));
                recordTable[i1][j1] = maxRecordFound;
            }
        }

        JsonObject recordInfo = Json.createObject();
        JsonArray recordFederations = Json.createArray();
        JsonArray recordCategories = Json.createArray();

        int ix1 = 0;
        for (String s : rowRecordNames) {
            recordFederations.set(ix1++, s);
        }

        JsonArray columns = Json.createArray();
        for (int j = 0; j < recordTable[0].length; j++) {
            JsonObject column = Json.createObject();
            JsonArray columnCells = Json.createArray();
            for (int i = 0; i < recordTable.length; i++) {
                JsonObject cell = Json.createObject();
                cell.put(Ranking.SNATCH.name(), "\u00a0");
                cell.put(Ranking.CLEANJERK.name(), "\u00a0");
                cell.put(Ranking.TOTAL.name(), "\u00a0");
                for (RecordEvent rec : recordTable[i][j]) {
                    if (recordCategories.length() <= j || recordCategories.get(j) == null) {
                        String string = Translator.translate("Record.CategoryTitle",rec.getAgeGrp(),rec.getBwCatString());
                        recordCategories.set(j, string);
                        column.put("cat", string);
                    }
                    Double recordValue = rec.getRecordValue();
                    cell.put(rec.getRecordLift().name(), recordValue != null ? recordValue : 999.0D);

                    if (rec.getRecordLift() == Ranking.SNATCH && snatchRequest != null && recordValue != null
                            && snatchRequest > recordValue) {
                        cell.put("snatchHighlight", "highlight");
                    } else if (rec.getRecordLift() == Ranking.CLEANJERK && cjRequest != null && recordValue != null
                            && cjRequest > +recordValue) {
                        cell.put("cjHighlight", "highlight");
                    } else if (rec.getRecordLift() == Ranking.TOTAL && totalRequest != null && recordValue != null
                            && totalRequest > +recordValue) {
                        cell.put("totalHighlight", "highlight");
                    }
                }
                columnCells.set(i, cell);
            }
            column.put("records", columnCells);
            columns.set(j, column);
        }

        recordInfo.put("recordNames", recordFederations);
        recordInfo.put("recordCategories", recordCategories);
        recordInfo.put("recordTable", columns);
        recordInfo.put("nbRecords", Json.create(recordTable[0].length+1));
        
        return recordInfo;
    }

    public static void clearRecords() throws IOException {
        JPAService.runInTransaction(em -> {
            try {
                // do not delete records set in the current competition.
                int deletedCount = em.createQuery("DELETE FROM RecordEvent rec WHERE rec.groupNameString IS NULL").executeUpdate();
                if (deletedCount > 0) {
                    logger.info("deleted {} record entries", deletedCount);
                }
            } catch (Exception e) {
                LoggerUtils.logError(logger, e);
            }
            return null;
        });
    }

    public static JsonValue computeRecords(Gender gender, Integer age, Double bw, Integer snatchRequest,
            Integer cjRequest, Integer totalRequest) {
        List<RecordEvent> records = findFiltered(gender, age, bw, null, null);
        return buildRecordJson(records, snatchRequest, cjRequest, totalRequest);
    }
    
    public static List<RecordEvent> computeRecordsForAthlete(Athlete curAthlete) {
        List<RecordEvent> records = RecordRepository.findFiltered(curAthlete.getGender(), curAthlete.getAge(),
                curAthlete.getBodyWeight(), null, null);

        // remove duplicates for each kind of record, keep largest
        Map<String, RecordEvent> cleanMap = records.stream().collect(
                Collectors.toMap(
                        RecordEvent::getKey,
                        Function.identity(),
                        (r1, r2) -> r1.getRecordValue() > r2.getRecordValue() ? r1 : r2));

        records = cleanMap.values().stream().collect(Collectors.toList());
        return records;
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

    public static List<RecordEvent> findFiltered(Gender gender, Integer age, Double bw, String groupName, Boolean newRecords) {
        List<RecordEvent> findFiltered = JPAService.runInTransaction(em -> {
            String qlString = "select rec from RecordEvent rec "
                    + filteringSelection(gender, age, bw, groupName, newRecords)
                    + " order by rec.gender, rec.ageGrpLower, rec.ageGrpUpper, rec.bwCatUpper, rec.recordValue desc";
            logger.debug("query = {}", qlString);

            Query query = em.createQuery(qlString);
            setFilteringParameters(gender, age, bw, groupName, newRecords, query);
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

    private static String filteringSelection(Gender gender, Integer age, Double bw, String groupName, Boolean newRecords) {
        String joins = null;
        String where = filteringWhere(gender, age, bw, groupName, newRecords);
        String selection = (joins != null ? " " + joins : "") + (where != null ? " where " + where : "");
        return selection;
    }

    private static String filteringWhere(Gender gender, Integer age, Double bw, String groupName, Boolean newRecords) {
        List<String> whereList = new LinkedList<>();
        if (gender != null) {
            whereList.add("rec.gender = :gender");
        }
        if (age != null) {
            whereList.add("(rec.ageGrpLower <= :age) and (rec.ageGrpUpper >= :age)");
        }
        if (bw != null) {
            whereList.add("(rec.bwCatLower*1.0 < :bw) and (rec.bwCatUpper*1.0 >= :bw)");
        }
        if (groupName != null) {
            whereList.add("(groupNameString = :groupName)");
        }
        if (newRecords != null) {
            whereList.add("(groupNameString is not null)");
        }
        if (whereList.size() == 0) {
            return null;
        } else {
            return String.join(" and ", whereList);
        }
    }

    private static void setFilteringParameters(Gender gender, Integer age, Double bw, String groupName, Boolean newRecords, Query query) {
        if (age != null) {
            query.setParameter("age", age);
        }
        if (bw != null) {
            query.setParameter("bw", bw);
        }
        if (gender != null) {
            query.setParameter("gender", gender);
        }
        if (groupName != null) {
            query.setParameter("groupName", gender);
        }
    }

}
