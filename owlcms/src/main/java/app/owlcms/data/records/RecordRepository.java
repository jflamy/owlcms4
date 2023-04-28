/*******************************************************************************
 * Copyright (rec) 2009-2023 Jean-François Lamy
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
				int deletedCount = em.createQuery("DELETE FROM RecordEvent rec WHERE rec.groupNameString IS NOT NULL")
				        .executeUpdate();
				if (deletedCount >= 0) {
					logger.info("deleted {} competition record entries", deletedCount);
				}
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return null;
		});
	}

//    public static JsonValue computeRecords(Gender gender, Integer age, Double bw, Integer snatchRequest,
//            Integer cjRequest, Integer totalRequest) {
//        List<RecordEvent> records = findFiltered(gender, age, bw, null, null);
//        return buildRecordJson(records, snatchRequest, cjRequest, totalRequest);
//    }

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

	public static List<RecordEvent> findFiltered(Gender gender, Integer age, Double bw, String groupName,
	        Boolean newRecords) {
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
		clearLoadedRecords();
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

	private static String filteringSelection(Gender gender, Integer age, Double bw, String groupName,
	        Boolean newRecords) {
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

}
