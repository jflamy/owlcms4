/*******************************************************************************
 * Copyright (rec) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.records;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.competition.Competition;
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

	public static void clearByExample(RecordEvent re) {
		JPAService.runInTransaction(em -> {
			Query q = em.createQuery("DELETE FROM RecordEvent a WHERE "
			        + "a.recordFederation = :rf "
			        + "AND a.recordName = :rn "
			        + "AND a.ageGrp = :ag ");
			q.setParameter("rf", re.getRecordFederation());
			q.setParameter("rn", re.getRecordName());
			q.setParameter("ag", re.getAgeGrp());
			q.executeUpdate();
			return null;
		});
	}

	/**
	 * @throws IOException
	 */
	public static void clearLoadedRecords() throws IOException {
		JPAService.runInTransaction(em -> {
			try {
				// do not delete records set in the current competition.
				int deletedCount = em.createQuery("DELETE FROM RecordEvent rec WHERE rec.groupNameString IS NULL")
				        .executeUpdate();
				if (deletedCount > 0) {
					logger.info("deleted {} record entries", deletedCount);
				}
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return null;
		});
	}

	/**
	 * @throws IOException
	 */
	public static void clearNewRecords() throws IOException {
		JPAService.runInTransaction(em -> {
			try {
				// do not delete records set in the current competition.
				int deletedCount = em.createQuery("DELETE FROM RecordEvent rec WHERE rec.groupNameString IS NOT NULL AND TRIM(rec.groupNameString) <> ''")
				        .executeUpdate();
				if (deletedCount >= 0) {
					logger.info("deleted {} provisional record entries", deletedCount);
				}
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return null;
		});
	}

	/**
	 * Accept provisional records only for rows matching the specified filters.
	 * 
	 * @param federation           Federation filter
	 * @param recordName           Record name filter
	 * @param ageGroup             Age group filter
	 * @param gender               Gender filter
	 * @param nameFilter           Name filter
	 * @param provisionalFilter    Provisional filter
	 * @param currentHistoryFilter Current/History filter
	 * @throws IOException
	 */
	public static void acceptProvisionalRecordsWithFilters(
	        String federation,
	        String recordName,
	        String ageGroup,
	        Gender gender,
	        String nameFilter,
	        String provisionalFilter,
	        String currentHistoryFilter) throws IOException {

		JPAService.runInTransaction(em -> {
			try {
				// Accept provisional rows by clearing the session/group marker.
				StringBuilder queryBuilder = new StringBuilder("UPDATE RecordEvent rec SET rec.groupNameString = NULL WHERE rec.groupNameString IS NOT NULL");
				List<String> parameters = new ArrayList<>();

				// Federation filter
				if (federation != null && !federation.isEmpty()) {
					queryBuilder.append(" AND rec.recordFederation = :federation");
					parameters.add("federation");
				}

				// Record name filter
				if (recordName != null && !recordName.isEmpty()) {
					queryBuilder.append(" AND rec.recordName = :recordName");
					parameters.add("recordName");
				}

				// Age group filter
				if (ageGroup != null && !ageGroup.isEmpty()) {
					queryBuilder.append(" AND rec.ageGrp = :ageGroup");
					parameters.add("ageGroup");
				}

				// Gender filter
				if (gender != null) {
					queryBuilder.append(" AND rec.gender = :gender");
					parameters.add("gender");
				}

				// Name filter (search in both record name and athlete name)
				if (nameFilter != null && !nameFilter.trim().isEmpty()) {
					queryBuilder.append(" AND (LOWER(rec.recordName) LIKE :nameFilter OR LOWER(rec.athleteName) LIKE :nameFilter)");
					parameters.add("nameFilter");
				}

				// Status filter - only provisional rows can be accepted here.
				if (provisionalFilter != null && !"ALL".equals(provisionalFilter)) {
					if ("PROVISIONAL".equals(provisionalFilter)) {
						// Already included in base WHERE clause
					} else if ("OFFICIAL".equals(provisionalFilter)) {
						// Do not modify official rows in the acceptance action.
						queryBuilder.append(" AND 1=0");
					}
				}

				Query query = em.createQuery(queryBuilder.toString());

				// Set parameters
				if (parameters.contains("federation")) {
					query.setParameter("federation", federation);
				}
				if (parameters.contains("recordName")) {
					query.setParameter("recordName", recordName);
				}
				if (parameters.contains("ageGroup")) {
					query.setParameter("ageGroup", ageGroup);
				}
				if (parameters.contains("gender")) {
					query.setParameter("gender", gender);
				}
				if (parameters.contains("nameFilter")) {
					query.setParameter("nameFilter", "%" + nameFilter.toLowerCase() + "%");
				}

				int updatedCount = query.executeUpdate();
				if (updatedCount >= 0) {
					logger.info("accepted {} provisional record entries", updatedCount);
				}
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return null;
		});
	}

	/**
	 * Keep only the latest official record within the filtered subset, deleting older official history.
	 * 
	 * @param federation        Federation filter
	 * @param recordName        Record name filter
	 * @param ageGroup          Age group filter
	 * @param gender            Gender filter
	 * @param nameFilter        Name filter
	 * @throws IOException
	 */
	public static void keepLatestOfficialRecordsWithFilters(
	        String federation,
	        String recordName,
	        String ageGroup,
	        Gender gender,
	        String nameFilter) throws IOException {

		JPAService.runInTransaction(em -> {
			try {
				// This cleanup only applies to official history.
				StringBuilder queryBuilder = new StringBuilder("SELECT rec FROM RecordEvent rec WHERE (rec.groupNameString IS NULL OR rec.groupNameString = '')");
				List<String> parameters = new ArrayList<>();

				// Federation filter
				if (federation != null && !federation.isEmpty()) {
					queryBuilder.append(" AND rec.recordFederation = :federation");
					parameters.add("federation");
				}

				// Record name filter
				if (recordName != null && !recordName.isEmpty()) {
					queryBuilder.append(" AND rec.recordName = :recordName");
					parameters.add("recordName");
				}

				// Age group filter
				if (ageGroup != null && !ageGroup.isEmpty()) {
					queryBuilder.append(" AND rec.ageGrp = :ageGroup");
					parameters.add("ageGroup");
				}

				// Gender filter
				if (gender != null) {
					queryBuilder.append(" AND rec.gender = :gender");
					parameters.add("gender");
				}

				// Name filter (search in both record name and athlete name)
				if (nameFilter != null && !nameFilter.trim().isEmpty()) {
					queryBuilder.append(" AND (LOWER(rec.recordName) LIKE :nameFilter OR LOWER(rec.athleteName) LIKE :nameFilter)");
					parameters.add("nameFilter");
				}

				Query query = em.createQuery(queryBuilder.toString());

				// Set parameters
				if (parameters.contains("federation")) {
					query.setParameter("federation", federation);
				}
				if (parameters.contains("recordName")) {
					query.setParameter("recordName", recordName);
				}
				if (parameters.contains("ageGroup")) {
					query.setParameter("ageGroup", ageGroup);
				}
				if (parameters.contains("gender")) {
					query.setParameter("gender", gender);
				}
				if (parameters.contains("nameFilter")) {
					query.setParameter("nameFilter", "%" + nameFilter.toLowerCase() + "%");
				}

				@SuppressWarnings("unchecked")
				List<RecordEvent> allRecords = query.getResultList();

				// Group by logical key and keep the highest-valued official row for each key.
				Map<String, RecordEvent> bestRecords = allRecords.stream()
				        .collect(Collectors.groupingBy(
				                RecordEvent::getKey,
				                Collectors.collectingAndThen(
				                        Collectors.maxBy((r1, r2) -> Double.compare(r1.getRecordValue(), r2.getRecordValue())),
				                        record -> record.orElseThrow(() -> new IllegalStateException("No record found")))));

				// Get IDs of records to keep
				Set<Long> idsToKeep = bestRecords.values().stream()
				        .map(RecordEvent::getId)
				        .collect(Collectors.toSet());

				// Delete all records in the filtered set that are not the best for their key
				List<Long> idsToDelete = allRecords.stream()
				        .map(RecordEvent::getId)
				        .filter(id -> !idsToKeep.contains(id))
				        .collect(Collectors.toList());

				if (!idsToDelete.isEmpty()) {
					int deletedCount = em.createQuery("DELETE FROM RecordEvent rec WHERE rec.id IN :idsToDelete")
					        .setParameter("idsToDelete", idsToDelete)
					        .executeUpdate();
					logger.info("deleted {} official historical record entries, keeping only the latest official records", deletedCount);
				}

			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return null;
		});
	}

	/**
	 * Delete all records matching the specified filters
	 * 
	 * @param federation           Federation filter
	 * @param recordName           Record name filter
	 * @param ageGroup             Age group filter
	 * @param gender               Gender filter
	 * @param nameFilter           Name filter
	 * @param provisionalFilter    Provisional filter
	 * @param currentHistoryFilter Current/History filter
	 * @throws IOException
	 */
	public static void deleteRecordsWithFilters(
	        String federation,
	        String recordName,
	        String ageGroup,
	        Gender gender,
	        String nameFilter,
	        String provisionalFilter,
	        String currentHistoryFilter) throws IOException {

		JPAService.runInTransaction(em -> {
			try {
				// Build the same WHERE clause as findWithFilters but for DELETE
				StringBuilder queryBuilder = new StringBuilder("DELETE FROM RecordEvent rec WHERE 1=1");
				List<String> parameters = new ArrayList<>();

				// Federation filter
				if (federation != null && !federation.isEmpty()) {
					queryBuilder.append(" AND rec.recordFederation = :federation");
					parameters.add("federation");
				}

				// Record name filter
				if (recordName != null && !recordName.isEmpty()) {
					queryBuilder.append(" AND rec.recordName = :recordName");
					parameters.add("recordName");
				}

				// Age group filter
				if (ageGroup != null && !ageGroup.isEmpty()) {
					queryBuilder.append(" AND rec.ageGrp = :ageGroup");
					parameters.add("ageGroup");
				}

				// Gender filter
				if (gender != null) {
					queryBuilder.append(" AND rec.gender = :gender");
					parameters.add("gender");
				}

				// Name filter (search in both record name and athlete name)
				if (nameFilter != null && !nameFilter.trim().isEmpty()) {
					queryBuilder.append(" AND (LOWER(rec.recordName) LIKE :nameFilter OR LOWER(rec.athleteName) LIKE :nameFilter)");
					parameters.add("nameFilter");
				}

				// Provisional filter
				if (provisionalFilter != null && !"ALL".equals(provisionalFilter)) {
					if ("PROVISIONAL".equals(provisionalFilter)) {
						queryBuilder.append(" AND (rec.groupNameString IS NOT NULL AND rec.groupNameString != '')");
					} else if ("OFFICIAL".equals(provisionalFilter)) {
						queryBuilder.append(" AND (rec.groupNameString IS NULL OR rec.groupNameString = '')");
					}
				}

				Query query = em.createQuery(queryBuilder.toString());

				// Set parameters
				if (parameters.contains("federation")) {
					query.setParameter("federation", federation);
				}
				if (parameters.contains("recordName")) {
					query.setParameter("recordName", recordName);
				}
				if (parameters.contains("ageGroup")) {
					query.setParameter("ageGroup", ageGroup);
				}
				if (parameters.contains("gender")) {
					query.setParameter("gender", gender);
				}
				if (parameters.contains("nameFilter")) {
					query.setParameter("nameFilter", "%" + nameFilter.toLowerCase() + "%");
				}

				int deletedCount = query.executeUpdate();
				if (deletedCount >= 0) {
					logger.info("deleted {} record entries matching the specified filters", deletedCount);
				}
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return null;
		});
	}

	// public static JsonValue computeRecords(Gender gender, Integer age, Double bw, Integer snatchRequest,
	// Integer cjRequest, Integer totalRequest) {
	// List<RecordEvent> records = findFiltered(gender, age, bw, null, null);
	// return buildRecordJson(records, snatchRequest, cjRequest, totalRequest);
	// }

	/**
	 * @throws IOException
	 */
	public static void clearOfficialRecords() throws IOException {
		JPAService.runInTransaction(em -> {
			try {
				// do not delete records set in the current competition.
				int deletedCount = em.createQuery("DELETE FROM RecordEvent rec WHERE (rec.groupNameString IS NULL or rec.groupNameString = '')")
				        .executeUpdate();
				if (deletedCount >= 0) {
					logger.info("deleted {} official record entries", deletedCount);
				}
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return null;
		});
	}

	static void clearOfficialRecordsMatchingLogicalKey(EntityManager em, RecordEvent record) {
		StringBuilder queryBuilder = new StringBuilder(
		        "DELETE FROM RecordEvent rec WHERE (rec.groupNameString IS NULL OR rec.groupNameString = '')");
		Map<String, Object> parameters = new LinkedHashMap<>();
		appendLogicalKeyConditions(queryBuilder, parameters, record);

		Query query = em.createQuery(queryBuilder.toString());
		parameters.forEach(query::setParameter);
		int deletedCount = query.executeUpdate();
		if (deletedCount > 0) {
			logger.info("deleted {} official record entries for {}", deletedCount, record.getKey());
		}
	}

	static void clearMatchingProvisionalRecordsForImportedOfficial(EntityManager em, RecordEvent record) {
		StringBuilder queryBuilder = new StringBuilder(
		        "DELETE FROM RecordEvent rec WHERE rec.groupNameString IS NOT NULL AND TRIM(rec.groupNameString) <> ''");
		Map<String, Object> parameters = new LinkedHashMap<>();
		appendLogicalKeyConditions(queryBuilder, parameters, record);
		appendEqualityCondition(queryBuilder, parameters, "rec.recordValue", "recordValue", record.getRecordValue());
		appendEqualityCondition(queryBuilder, parameters, "rec.athleteName", "athleteName", record.getAthleteName());
		appendEqualityCondition(queryBuilder, parameters, "rec.recordDate", "recordDate", record.getRecordDate());
		appendEqualityCondition(queryBuilder, parameters, "rec.event", "event", record.getEvent());
		appendEqualityCondition(queryBuilder, parameters, "rec.eventLocation", "eventLocation", record.getEventLocation());

		Query query = em.createQuery(queryBuilder.toString());
		parameters.forEach(query::setParameter);
		int deletedCount = query.executeUpdate();
		if (deletedCount > 0) {
			logger.info("deleted {} provisional record entries replaced by imported official {} {}", deletedCount, record.getKey(), record.getRecordValue());
		}
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

	public static List<RecordEvent> findAllLoadedRecords() {
		ArrayList<RecordEvent> recordEventStubs = new ArrayList<>();
		JPAService.runInTransaction(em -> {
			// temporary diagnostic: track unexpected records without fileName
			Query missing = em.createQuery(
			        "SELECT rec.id FROM RecordEvent rec WHERE rec.fileName IS NULL OR TRIM(rec.fileName) = ''");
			if (!missing.getResultList().isEmpty()) {
				logger.error("findAllLoadedRecords detected {} records missing fileName {}", missing.getResultList().size(), LoggerUtils.whereFrom());
			}

			Query q = em.createNativeQuery(
			        "SELECT DISTINCT a.fileName, a.recordFederation, a.recordName, a.ageGrp, a.active FROM RecordEvent a");
			@SuppressWarnings("unchecked")
			List<Object[]> records = q.getResultList();

			for (Object[] a : records) {
				RecordEvent e = new RecordEvent();
				e.setFileName((String) a[0]);
				e.setRecordFederation((String) a[1]);
				e.setRecordName((String) a[2]);
				e.setAgeGrp((String) a[3]);
				Object activeVal = a[4];
				e.setActive(activeVal == null ? true : (Boolean) activeVal);
				recordEventStubs.add(e);
			}
			return null;
		});
		return recordEventStubs;
	}

	public static List<String> findAllRecordNames() {
		ArrayList<String> names = new ArrayList<>();
		JPAService.runInTransaction(em -> {
			Query q = em.createNativeQuery("SELECT DISTINCT a.recordName FROM RecordEvent a WHERE (a.active IS NULL OR a.active = true)");
			@SuppressWarnings("unchecked")
			List<Object> records = q.getResultList();

			for (Object a : records) {
				names.add((String) a);
			}
			return null;
		});
		return names;
	}

	public static RecordEvent findByName(String name) {
		return JPAService.runInTransaction(em -> {
			return doFindByName(name, em);
		});
	}

	public static List<RecordEvent> findFiltered(Gender gender, Integer age, Double bw, String groupName,
	        Boolean newRecords) {
		List<RecordEvent> findFiltered = JPAService.runInTransaction(em -> {
			String qlString = "select rec from RecordEvent rec "
			        + filteringSelection(gender, age, bw, groupName, newRecords)
			        + " order by rec.gender, rec.ageGrpLower, rec.ageGrpUpper, rec.bwCatUpper, rec.recordValue asc";
			// logger.debug("query = {}", qlString);

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
		clearLoadedRecords();
		InputStream is = ResourceWalker.getResourceAsStream(localizedFileName);
		new RecordDefinitionReader().readZip(is);
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
				if (isProvisional(Record)) {
					RecordEvent duplicate = findExactDuplicate(em, Record);
					if (duplicate != null) {
						logger.info("skipping duplicate provisional record {} {}", duplicate.getKey(), duplicate.getRecordValue());
						return duplicate;
					}
				}
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

	static RecordEvent findExactDuplicate(EntityManager em, RecordEvent candidate) {
		StringBuilder queryBuilder = new StringBuilder("SELECT rec FROM RecordEvent rec WHERE 1=1");
		Map<String, Object> parameters = new LinkedHashMap<>();

		appendLogicalKeyConditions(queryBuilder, parameters, candidate);
		appendEqualityCondition(queryBuilder, parameters, "rec.recordValue", "recordValue", candidate.getRecordValue());
		appendEqualityCondition(queryBuilder, parameters, "rec.groupNameString", "groupNameString", candidate.getGroupNameString());
		appendEqualityCondition(queryBuilder, parameters, "rec.athleteName", "athleteName", candidate.getAthleteName());
		appendEqualityCondition(queryBuilder, parameters, "rec.recordDate", "recordDate", candidate.getRecordDate());
		appendEqualityCondition(queryBuilder, parameters, "rec.event", "event", candidate.getEvent());
		appendEqualityCondition(queryBuilder, parameters, "rec.eventLocation", "eventLocation", candidate.getEventLocation());

		Query query = em.createQuery(queryBuilder.toString());
		parameters.forEach(query::setParameter);

		@SuppressWarnings("unchecked")
		List<RecordEvent> matches = query.getResultList();
		return matches.stream().filter(match -> isSameDuplicateProvisional(candidate, match)).findFirst().orElse(null);
	}

	static boolean isProvisional(RecordEvent record) {
		return record.getGroupNameString() != null && !record.getGroupNameString().isBlank();
	}

	private static boolean isSameDuplicateProvisional(RecordEvent left, RecordEvent right) {
		return Objects.equals(left.getRecordFederation(), right.getRecordFederation())
		        && Objects.equals(left.getRecordName(), right.getRecordName())
		        && Objects.equals(left.getGender(), right.getGender())
		        && Objects.equals(left.getRecordLift(), right.getRecordLift())
		        && Objects.equals(left.getAgeGrpLower(), right.getAgeGrpLower())
		        && Objects.equals(left.getAgeGrpUpper(), right.getAgeGrpUpper())
		        && Objects.equals(left.getBwCatLower(), right.getBwCatLower())
		        && Objects.equals(left.getBwCatUpper(), right.getBwCatUpper())
		        && Objects.equals(left.getRecordValue(), right.getRecordValue())
		        && Objects.equals(left.getAthleteName(), right.getAthleteName())
		        && Objects.equals(left.getRecordDate(), right.getRecordDate())
		        && Objects.equals(left.getEvent(), right.getEvent())
		        && Objects.equals(left.getEventLocation(), right.getEventLocation())
		        && Objects.equals(left.getGroupNameString(), right.getGroupNameString());
	}

	@SuppressWarnings("unchecked")
	private static List<RecordEvent> doFindAll(EntityManager em) {
		return em.createQuery(
		        "select rec from RecordEvent rec order by rec.recordFederation,rec.gender,rec.ageGrpLower,rec.ageGrpUpper,rec.bwCatUpper")
		        .getResultList();
	}

	private static String filteringSelection(Gender gender, Integer age, Double bw, String groupName,
	        Boolean newRecords) {
		String joins = null;
		String where = filteringWhere(gender, age, bw, groupName, newRecords);
		String selection = (joins != null ? " " + joins : "") + (where != null ? " where " + where : "");
		return selection;
	}

	private static String filteringWhere(Gender gender, Integer age, Double bw, String groupName, Boolean newRecords) {
		List<String> whereList = new LinkedList<>();
		// only return active records (treat null as active for backward compatibility)
		whereList.add("(rec.active IS NULL OR rec.active = true)");
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
		if (newRecords != null && newRecords) {
			whereList.add("((groupNameString is not null) or (groupNameString != ''))");
		}
		if (whereList.size() == 0) {
			// logger.debug("where = {}", "");
			return null;
		} else {
			String join = String.join(" and ", whereList);
			// logger.debug("where = {}", join);
			return join;
		}
	}

	private static void setFilteringParameters(Gender gender, Integer age, Double bw, String groupName,
	        Boolean newRecords, Query query) {
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
			query.setParameter("groupName", groupName);
		}
	}

	public static void recomputeNewRecords() {
		try {
			clearNewRecords();
		} catch (IOException e) {
		}
		LinkedList<ActualLiftInfo> lifts = new LinkedList<>();
		for (Athlete a : AthleteRepository.findAll()) {
			for (int i = 1; i <= 6; i++) {
				Integer lift = a.getActualLiftOrNull(i);
				// logger.debug("a {} i {}",a.getAbbreviatedName(), i);
				if (lift != null) {
					var ali = new ActualLiftInfo();
					ali.setA(a);
					ali.setLift(lift);
					ali.setLiftNo(i);
					LocalDateTime liftTime = a.getLiftTime(i);
					if (liftTime == null) {
						System.err.println(a.getAbbreviatedName() + " " + i);
					}
					ali.setT(liftTime);
					lifts.add(ali);
				}
			}
		}

		lifts.sort((ali1, ali2) -> ObjectUtils.compare(ali1.getT(), ali2.getT()));

		List<RecordEvent> matchingRecords = new ArrayList<>();
		for (ActualLiftInfo ali : lifts) {
			Athlete a = ali.getA();
			// matchingRecords = findFiltered(a.getGender(), a.getAge(), a.getBodyWeight(), null, null);
			matchingRecords = RecordFilter.computeDisplayableRecordsForAthlete(a);

			List<RecordEvent> improvedRecords = new ArrayList<>();
			RecordEvent improvedRecord;
			for (RecordEvent mr : matchingRecords) {
				// check for record federation.
				String federationCodes = a.getFederationCodes();
				if (federationCodes != null) {
					if (!federationCodes.isBlank() && !federationCodes.contains(mr.getRecordFederation())) {
						// athlete is not eligible
						continue;
					}
				}

				if (ali.getLiftNo() <= 3 && mr.getRecordLift() == Ranking.SNATCH && ali.getLift() > mr.getRecordValue()) {
					improvedRecord = improveRecord(ali, mr, ali.getLift());
					if (improvedRecord != null)
						improvedRecords.add(improvedRecord);
				} else {
					// cj lift may improve CJ and may improve Total
					var bestSnatch = ali.getA().getBestSnatch();
					var total = 0;
					if (bestSnatch > 0 && ali.getLift() > 0) {
						total = bestSnatch + ali.getLift();
					}
					if (ali.getLiftNo() > 3 && mr.getRecordLift() == Ranking.CLEANJERK && ali.getLift() > mr.getRecordValue()) {
						improvedRecord = improveRecord(ali, mr, ali.getLift());
						if (improvedRecord != null)
							improvedRecords.add(improvedRecord);
					}
					if (ali.getLiftNo() > 3 && mr.getRecordLift() == Ranking.TOTAL && total > mr.getRecordValue()) {
						// logger.debug("checking total for {} {} --- {} ",ali.getA(),ali.getLiftNo(), mr.getRecordValue());
						improvedRecord = improveRecord(ali, mr, total);
						if (improvedRecord != null)
							improvedRecords.add(improvedRecord);
					}
				}
			}

			for (RecordEvent r : improvedRecords) {
				save(r);
			}
		}

	}

	public static RecordEvent improveRecord(ActualLiftInfo ali, RecordEvent mr, int newValue) {
		RecordEvent nmr = new RecordEvent();

		nmr.setAthleteName(ali.getA().getFullName());
		nmr.setBirthDate(ali.getA().getFullBirthDate());
		nmr.setBirthYear(ali.getA().getYearOfBirth());
		nmr.setAthleteAge(ali.getA().getAge());
		nmr.setAthleteBW(ali.getA().getBodyWeight());
		nmr.setGender(ali.getA().getGender());
		nmr.setNation(ali.getA().getClub());

		nmr.setAgeGrp(mr.getAgeGrp());
		nmr.setAgeGrpLower(mr.getAgeGrpLower());
		nmr.setAgeGrpUpper(mr.getAgeGrpUpper());
		nmr.setBwCatLower(mr.getBwCatLower());
		nmr.setBwCatUpper(mr.getBwCatUpper());
		nmr.setBwCatString(mr.getBwCatString());
		nmr.setCategoryString(mr.getCategoryString());

		nmr.setRecordLift(mr.getRecordLift());
		nmr.setRecordName(mr.getRecordName());
		nmr.setRecordValue(newValue);
		nmr.setRecordDate(ali.getT().toLocalDate());
		nmr.setRecordYear(ali.getT().getYear());
		nmr.setRecordFederation(mr.getRecordFederation());
		nmr.setEvent(Competition.getCurrent().getCompetitionName());
		nmr.setEventLocation(Competition.getCurrent().getCompetitionCity());

		// this marks the record as provisional
		nmr.setGroupNameString(ali.getA().getGroup().getName());
		logger.info("!!! recomputed record {} {} {} {}", nmr.getAthleteName(), nmr.getAgeGrp(), nmr.getRecordLift(), nmr.getRecordValue());
		return nmr;
	}

	/**
	 * Find all distinct federations
	 *
	 * @return the list of federations
	 */
	public static List<String> findDistinctFederations() {
		return JPAService.runInTransaction(em -> {
			return em.createQuery(
			        "SELECT DISTINCT rec.recordFederation FROM RecordEvent rec WHERE rec.recordFederation IS NOT NULL AND (rec.active IS NULL OR rec.active = true) ORDER BY rec.recordFederation",
			        String.class)
			        .getResultList();
		});
	}

	/**
	 * Find all distinct record names
	 *
	 * @return the list of record names
	 */
	public static List<String> findDistinctRecordNames() {
		return JPAService.runInTransaction(em -> {
			return em.createQuery(
			        "SELECT DISTINCT rec.recordName FROM RecordEvent rec WHERE rec.recordName IS NOT NULL AND (rec.active IS NULL OR rec.active = true) ORDER BY rec.recordName",
			        String.class)
			        .getResultList();
		});
	}

	public static List<String> findDistinctRecordNames(String federation, String ageGroup) {
		if (federation == null || federation.isBlank()) {
			return List.of();
		}
		return JPAService.runInTransaction(em -> {
			StringBuilder queryBuilder = new StringBuilder(
			        "SELECT DISTINCT rec.recordName FROM RecordEvent rec WHERE rec.recordName IS NOT NULL AND rec.recordFederation = :federation AND (rec.active IS NULL OR rec.active = true)");
			if (ageGroup != null && !ageGroup.isBlank()) {
				queryBuilder.append(" AND rec.ageGrp = :ageGroup");
			}
			queryBuilder.append(" ORDER BY rec.recordName");

			Query query = em.createQuery(queryBuilder.toString(), String.class);
			query.setParameter("federation", federation);
			if (ageGroup != null && !ageGroup.isBlank()) {
				query.setParameter("ageGroup", ageGroup);
			}

			@SuppressWarnings("unchecked")
			List<String> resultList = query.getResultList();
			return resultList;
		});
	}

	/**
	 * Find all distinct age groups
	 *
	 * @return the list of age groups
	 */
	public static List<String> findDistinctAgeGroups() {
		return JPAService.runInTransaction(em -> {
			return em.createQuery(
			        "SELECT DISTINCT rec.ageGrp FROM RecordEvent rec WHERE rec.ageGrp IS NOT NULL AND (rec.active IS NULL OR rec.active = true) ORDER BY rec.ageGrp",
			        String.class)
			        .getResultList();
		});
	}

	public static List<String> findDistinctAgeGroups(String federation, String recordName) {
		if (federation == null || federation.isBlank()) {
			return List.of();
		}
		return JPAService.runInTransaction(em -> {
			StringBuilder queryBuilder = new StringBuilder(
			        "SELECT DISTINCT rec.ageGrp FROM RecordEvent rec WHERE rec.ageGrp IS NOT NULL AND rec.recordFederation = :federation AND (rec.active IS NULL OR rec.active = true)");
			if (recordName != null && !recordName.isBlank()) {
				queryBuilder.append(" AND rec.recordName = :recordName");
			}
			queryBuilder.append(" ORDER BY rec.ageGrp");

			Query query = em.createQuery(queryBuilder.toString(), String.class);
			query.setParameter("federation", federation);
			if (recordName != null && !recordName.isBlank()) {
				query.setParameter("recordName", recordName);
			}

			@SuppressWarnings("unchecked")
			List<String> resultList = query.getResultList();
			return resultList;
		});
	}

	/**
	 * Set active status for all records matching the given record set (federation + recordName + ageGrp).
	 * This is used to activate or deactivate entire record sets.
	 * Inactive records behave as if they had never been loaded.
	 *
	 * @param recordFederation the federation
	 * @param recordName       the record name
	 * @param ageGrp           the age group
	 * @param active           true to activate, false to deactivate
	 */
	public static void setActiveForRecordSet(String recordFederation, String recordName, String ageGrp, boolean active) {
		JPAService.runInTransaction(em -> {
			Query q = em.createQuery("UPDATE RecordEvent a SET a.active = :active WHERE "
			        + "a.recordFederation = :rf "
			        + "AND a.recordName = :rn "
			        + "AND a.ageGrp = :ag ");
			q.setParameter("active", active);
			q.setParameter("rf", recordFederation);
			q.setParameter("rn", recordName);
			q.setParameter("ag", ageGrp);
			int updated = q.executeUpdate();
			logger.info("{} {} record entries for {} {} {}", active ? "activated" : "deactivated", updated, recordFederation, recordName, ageGrp);
			return null;
		});
	}

	/**
	 * Set active status for ALL record events.
	 *
	 * @param active true to activate all, false to deactivate all
	 */
	public static void setActiveForAll(boolean active) {
		JPAService.runInTransaction(em -> {
			Query q = em.createQuery("UPDATE RecordEvent a SET a.active = :active");
			q.setParameter("active", active);
			int updated = q.executeUpdate();
			logger.info("{} all {} record entries", active ? "activated" : "deactivated", updated);
			return null;
		});
	}

	/**
	 * Comprehensive filtering method that supports all filters from RecordContent. This ensures consistency between grid display and export functionality.
	 * 
	 * @param federation           Filter by federation (null for no filter)
	 * @param ageGroup             Filter by age group (null for no filter)
	 * @param gender               Filter by gender (null for no filter)
	 * @param nameFilter           Filter by record name or athlete name (null for no filter)
	 * @param provisionalFilter    Filter by provisional status (null for ALL)
	 * @param currentHistoryFilter Filter by current/history (null for ALL)
	 * @param session              Filter to current session
	 * @return Filtered and sorted list of records
	 */
	public static List<RecordEvent> findWithFilters(
	        String federation,
	        String recordName,
	        String ageGroup,
	        Gender gender,
	        String nameFilter,
	        String provisionalFilter, // "ALL", "PROVISIONAL", "OFFICIAL"
	        String currentHistoryFilter, // "CURRENT", "HISTORY"
	        String session) {
		String effectiveCurrentHistoryFilter = normalizeCurrentHistoryFilter(provisionalFilter, currentHistoryFilter);
		@SuppressWarnings("unchecked")
		List<RecordEvent> allResults = JPAService.runInTransaction(em -> {
			// Start with base query
			StringBuilder queryBuilder = new StringBuilder("SELECT rec FROM RecordEvent rec WHERE (rec.active IS NULL OR rec.active = true)");
			List<String> parameters = new ArrayList<>();

			// Federation filter
			if (federation != null && !federation.isEmpty()) {
				queryBuilder.append(" AND rec.recordFederation = :federation");
				parameters.add("federation");
			}

			// Record name filter
			if (recordName != null && !recordName.isEmpty()) {
				queryBuilder.append(" AND rec.recordName = :recordName");
				parameters.add("recordName");
			}

			// Age group filter
			if (ageGroup != null && !ageGroup.isEmpty()) {
				queryBuilder.append(" AND rec.ageGrp = :ageGroup");
				parameters.add("ageGroup");
			}

			// Gender filter
			if (gender != null) {
				queryBuilder.append(" AND rec.gender = :gender");
				parameters.add("gender");
			}

			// Name filter (search in both record name and athlete name)
			if (nameFilter != null && !nameFilter.trim().isEmpty()) {
				queryBuilder.append(" AND (LOWER(rec.recordName) LIKE :nameFilter OR LOWER(rec.athleteName) LIKE :nameFilter)");
				parameters.add("nameFilter");
			}

			// Provisional filter
			if (provisionalFilter != null && !"ALL".equals(provisionalFilter)) {
				if ("PROVISIONAL".equals(provisionalFilter)) {
					if (session == null) {
						queryBuilder.append(" AND (rec.groupNameString IS NOT NULL AND rec.groupNameString != '')");
					} else {
						queryBuilder.append(" AND (rec.groupNameString IS NOT NULL AND rec.groupNameString LIKE :session)");
						parameters.add("session");
					}
				} else if ("OFFICIAL".equals(provisionalFilter)) {
					queryBuilder.append(" AND (rec.groupNameString IS NULL OR rec.groupNameString = '')");
				}
			}

			// Add ordering - category information before lift type
			queryBuilder.append(
			        " ORDER BY rec.recordFederation, rec.recordName, rec.gender, rec.ageGrpUpper, rec.ageGrpLower, rec.bwCatUpper, rec.recordLift, rec.recordValue");

			Query query = em.createQuery(queryBuilder.toString());

			// Set parameters
			if (parameters.contains("federation")) {
				query.setParameter("federation", federation);
			}
			if (parameters.contains("recordName")) {
				query.setParameter("recordName", recordName);
			}
			if (parameters.contains("ageGroup")) {
				query.setParameter("ageGroup", ageGroup);
			}
			if (parameters.contains("gender")) {
				query.setParameter("gender", gender);
			}
			if (parameters.contains("nameFilter")) {
				query.setParameter("nameFilter", "%" + nameFilter.toLowerCase() + "%");
			}
			if (parameters.contains("session")) {
				query.setParameter("session", session);
			}

			List<RecordEvent> queryResults;
			queryResults = query.getResultList();
			return queryResults;
		});

		// logger.debug("findWithFilters fetched {} records (federation={}, ageGroup={}, gender={}, nameFilter={}, provisional={}, currentHistory={})", //$NON-NLS-1$
		//         allResults.size(), federation, ageGroup, gender, nameFilter, provisionalFilter, currentHistoryFilter);
		// logger.debug(LoggerUtils.whereFrom());

		// Apply current/history filter in Java (since it requires grouping logic)
		if ("CURRENT".equals(effectiveCurrentHistoryFilter)) {
			// Group by record key and keep only the best (highest recordValue) record for each key (i.e., for each lift)
			return allResults.stream()
			        .collect(Collectors.groupingBy(
			                RecordEvent::getKey,
			                Collectors.collectingAndThen(
			                        Collectors.maxBy((r1, r2) -> Double.compare(r1.getRecordValue(), r2.getRecordValue())),
			                        record -> record.orElseThrow(() -> new IllegalStateException("No record found")))))
			        .values()
			        .stream()
			        .sorted((r1, r2) -> {
				        // Re-apply the same ordering as the query - category before lift
				        int fedComp = ObjectUtils.compare(r1.getRecordFederation(), r2.getRecordFederation());
				        if (fedComp != 0)
					        return fedComp;

				        int nameComp = ObjectUtils.compare(r1.getRecordName(), r2.getRecordName());
				        if (nameComp != 0)
					        return nameComp;

				        int genderComp = ObjectUtils.compare(r1.getGender(), r2.getGender());
				        if (genderComp != 0)
					        return genderComp;

				        int ageUpperComp = ObjectUtils.compare(r1.getAgeGrpUpper(), r2.getAgeGrpUpper());
				        if (ageUpperComp != 0)
					        return ageUpperComp;

				        int ageLowerComp = ObjectUtils.compare(r1.getAgeGrpLower(), r2.getAgeGrpLower());
				        if (ageLowerComp != 0)
					        return ageLowerComp;

				        int bwComp = ObjectUtils.compare(r1.getBwCatUpper(), r2.getBwCatUpper());
				        if (bwComp != 0)
					        return bwComp;

				        int liftComp = ObjectUtils.compare(r1.getRecordLift(), r2.getRecordLift());
				        if (liftComp != 0)
					        return liftComp;

				        return ObjectUtils.compare(r1.getRecordValue(), r2.getRecordValue());
			        })
			        .collect(Collectors.toList());
		}

		// For HISTORY or null, return all results as-is
		return allResults;
	}

	public static String normalizeCurrentHistoryFilter(String provisionalFilter, String currentHistoryFilter) {
		if ("PROVISIONAL".equals(provisionalFilter)) {
			return "HISTORY";
		}
		return currentHistoryFilter;
	}

	private static void appendLogicalKeyConditions(StringBuilder queryBuilder, Map<String, Object> parameters, RecordEvent record) {
		appendEqualityCondition(queryBuilder, parameters, "rec.recordFederation", "recordFederation", record.getRecordFederation());
		appendEqualityCondition(queryBuilder, parameters, "rec.recordName", "recordName", record.getRecordName());
		appendEqualityCondition(queryBuilder, parameters, "rec.gender", "gender", record.getGender());
		appendEqualityCondition(queryBuilder, parameters, "rec.recordLift", "recordLift", record.getRecordLift());
		appendEqualityCondition(queryBuilder, parameters, "rec.ageGrpLower", "ageGrpLower", record.getAgeGrpLower());
		appendEqualityCondition(queryBuilder, parameters, "rec.ageGrpUpper", "ageGrpUpper", record.getAgeGrpUpper());
		appendEqualityCondition(queryBuilder, parameters, "rec.bwCatLower", "bwCatLower", record.getBwCatLower());
		appendEqualityCondition(queryBuilder, parameters, "rec.bwCatUpper", "bwCatUpper", record.getBwCatUpper());
	}

	private static void appendEqualityCondition(
	        StringBuilder queryBuilder,
	        Map<String, Object> parameters,
	        String fieldName,
	        String parameterName,
	        Object value) {
		if (value == null) {
			queryBuilder.append(" AND ").append(fieldName).append(" IS NULL");
		} else {
			queryBuilder.append(" AND ").append(fieldName).append(" = :").append(parameterName);
			parameters.put(parameterName, value);
		}
	}

}
