/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.data.athlete;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.LoggerFactory;

import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class AthleteRepository.
 */
public class AthleteRepository {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteRepository.class);
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
	public static Athlete getById(Long id, EntityManager em) {
		Query query = em.createQuery("select a from Athlete a where a.id=:id");
		query.setParameter("id", id);

		return (Athlete) query.getResultList()
			.stream()
			.findFirst()
			.orElse(null);
	}

	/**
	 * Save an athlete
	 *
	 * @param Athlete the athlete
	 * @return the athlete
	 */
	public static Athlete save(Athlete Athlete) {
		return JPAService.runInTransaction(em -> em.merge(Athlete));
	}

	/**
	 * Delete an athlete
	 * 
	 * @param Athlete the athlete
	 */
	public static void delete(Athlete Athlete) {
		JPAService.runInTransaction(em -> {
			em.remove(getById(Athlete.getId(), em));
			return null;
		});
	}

	/**
	 * @return the list of all athletes
	 */

	public static List<Athlete> findAll() {
		return JPAService.runInTransaction(em -> doFindAll(em));
	}

	@SuppressWarnings("unchecked")
	public static List<Athlete> doFindAll(EntityManager em) {
		return em.createQuery("select a from Athlete a")
			.getResultList();
	}

	/**
	 * Find filtered.
	 *
	 * @param lastName    the last name
	 * @param group       the group
	 * @param ageDivision the age division
	 * @param weighedIn   if weighed (bodyweight > 0)
	 * @param offset      the offset
	 * @param limit       the limit
	 * @return the list
	 */
	public static List<Athlete> findFiltered(String lastName, Group group, Category category, AgeDivision ageDivision, Boolean weighedIn,
			int offset, int limit) {
		return JPAService.runInTransaction(em -> {
			return doFindFiltered(em, lastName, group, category, ageDivision, weighedIn, offset, limit);
		});
	}

	public static List<Athlete> doFindFiltered(EntityManager em,
			String lastName,
			Group group,
			Category category,
			AgeDivision ageDivision,
			Boolean weighedIn,
			int offset, int limit) {
		String qlString = "select a from Athlete a" + filteringSelection(lastName, group, category, ageDivision, weighedIn);
		logger.trace("find query = {}",qlString);		
		Query query = em.createQuery(qlString);
		setFilteringParameters(lastName, group, category, ageDivision, query);
		if (offset >= 0)
			query.setFirstResult(offset);
		if (limit > 0)
			query.setMaxResults(limit);
		@SuppressWarnings("unchecked")
		List<Athlete> resultList = query.getResultList();
		return resultList;
	}

	/**
	 * Count filtered.
	 *
	 * @param lastName    the last name
	 * @param group       the group
	 * @param ageDivision the age division
	 * @param weighedIn   the weighed in
	 * @return the int
	 */
	public static int countFiltered(String lastName, Group group, Category category, AgeDivision ageDivision, Boolean weighedIn) {
		return JPAService.runInTransaction(em -> {
			return doCountFiltered(lastName, group, category, ageDivision, weighedIn, em);
		});
	}

	public static Integer doCountFiltered(String lastName, Group group, Category category, AgeDivision ageDivision, Boolean weighedIn,
			EntityManager em) {
		String selection = filteringSelection(lastName, group, category, ageDivision, weighedIn);
		String qlString = "select count(a.id) from Athlete a " + selection;
		logger.trace("count query = {}",qlString);
		Query query = em.createQuery(qlString);
		setFilteringParameters(lastName, group, category, ageDivision, query);
		int i = ((Long) query.getSingleResult()).intValue();
		return i;
	}

	private static void setFilteringParameters(String lastName, Group group, Category category, AgeDivision ageDivision, Query query) {
		if (lastName != null && lastName.trim().length() > 0)
			// starts with
			query.setParameter("lastName", lastName.toLowerCase()+"%");
		if (group != null) {
			query.setParameter("groupId", group.getId()); // group is via a relationship, we join and select on id.
		}
		if (category != null) {
			query.setParameter("categoryId", category.getId()); // category is via a relationship, we join and select on id.
		}
		if (ageDivision != null)
			query.setParameter("division", ageDivision); // ageDivision is a string
	}

	private static String filteringSelection(String lastName, Group group, Category category, AgeDivision ageDivision,
			Boolean weighedIn) {
		String joins = filteringJoins(group, category);
		String where = filteringWhere(lastName, group, category, ageDivision, weighedIn);
		String selection = (joins != null ? " " + joins : "") +
				(where != null ? " where " + where : "");
		return selection;
	}

	private static String filteringWhere(String lastName, Group group, Category category, AgeDivision ageDivision, Boolean weighedIn) {
		List<String> whereList = new LinkedList<String>();
		if (ageDivision != null)
			whereList.add("a.ageDivision = :division");
		if (group != null)
			whereList.add("g.id = :groupId");  // group is via a relationship, select the joined id.
		if (category != null)
			whereList.add("c.id = :categoryId");  // category is via a relationship, select the joined id.
		if (lastName != null && lastName.trim().length() > 0)
			whereList.add("lower(a.lastName) like :lastName");
		if (weighedIn != null && weighedIn)
			whereList.add("a.bodyWeight > 0");
		if (whereList.size() == 0) {
			return null;
		} else {
			return String.join(" and ", whereList);
		}
	}

	private static String filteringJoins(Group group, Category category) {
		List<String> fromList = new LinkedList<String>();
		if (group != null) {
			fromList.add("join a.group g"); // group is via a relationship, join on id
		}
		if (category != null) {
			fromList.add("join a.category c"); // group is via a relationship, join on id
		}
		if (fromList.size() == 0) {
			return "";
		} else {
			return String.join(" ", fromList);
		}
	}

	/**
	 * Find all by group and weigh in.
	 *
	 * @param group     the group
	 * @param weighedIn the weighed in
	 * @return the list
	 */
	public static List<Athlete> findAllByGroupAndWeighIn(Group group, Boolean weighedIn) {
		List<Athlete> findFiltered = findFiltered((String) null, group, (Category) null, (AgeDivision) null, weighedIn, -1, -1);
		logger.trace("findFiltered found {}", findFiltered.size());
		return findFiltered;
	}

	public static List<Athlete> doFindAllByGroupAndWeighIn(EntityManager em, Group group, Boolean weighedIn) {
		return doFindFiltered(em, (String) null, group, (Category) null, (AgeDivision) null, weighedIn, -1, -1);
	}

}
