/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.data.athlete;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.ledocte.owlcms.data.category.AgeDivision;
import org.ledocte.owlcms.data.group.Group;
import org.ledocte.owlcms.data.jpa.JPAService;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class AthleteRepository.
 */
public class AthleteRepository {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteRepository.class);
	static {
		logger.setLevel(Level.DEBUG);
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
		Query query = em.createQuery("select u from Athlete u where u.id=:id");
		query.setParameter("id", id);

		return (Athlete) query.getResultList()
			.stream()
			.findFirst()
			.orElse(null);
	}

	/**
	 * Save.
	 *
	 * @param Athlete the athlete
	 * @return the athlete
	 */
	public static Athlete save(Athlete Athlete) {
		return JPAService.runInTransaction(em -> em.merge(Athlete));
	}

	/**
	 * Delete.
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
	 * Find all.
	 *
	 * @return the list
	 */


	public static List<Athlete> findAll() {
		return JPAService.runInTransaction(em -> doFindAll(em));
	}

	@SuppressWarnings("unchecked")
	public static List<Athlete> doFindAll(EntityManager em) {
		return em.createQuery("select c from Athlete c")
			.getResultList();
	}

	/**
	 * Find filtered.
	 *
	 * @param lastName    the last name
	 * @param group       the group
	 * @param ageDivision the age division
	 * @param weighedIn   the weighed in
	 * @param offset      the offset
	 * @param limit       the limit
	 * @return the list
	 */
	public static List<Athlete> findFiltered(String lastName, Group group, AgeDivision ageDivision, Boolean weighedIn,
			int offset, int limit) {
		return JPAService.runInTransaction(em -> {
			return doFindFiltered(em, lastName, group, ageDivision, weighedIn, offset, limit);
		});
	}

	protected static List<Athlete> doFindFiltered(EntityManager em, String lastName, Group group,
			AgeDivision ageDivision, Boolean weighedIn,
			int offset, int limit) {
		String where = filteredWhere(lastName, group, ageDivision, weighedIn);
		String qlString = "select c from Athlete c " +
				(where != null ? where : "");
		Query query = em.createQuery(
			qlString);
		setFilteredParameters(lastName, group, ageDivision, query);
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
	public static int countFiltered(String lastName, Group group, AgeDivision ageDivision, Boolean weighedIn) {
		String where = filteredWhere(lastName, group, ageDivision, weighedIn);
		return JPAService.runInTransaction(em -> {
			Query query = em.createQuery(
				"select count(c.id) from Athlete c " + where);
			setFilteredParameters(lastName, group, ageDivision, query);
			int i = ((Long) query.getSingleResult()).intValue();
			return i;
		});
	}

	private static void setFilteredParameters(String lastName, Group group, AgeDivision ageDivision, Query query) {
		if (lastName != null)
			query.setParameter("lastName", lastName);
		if (group != null)
			query.setParameter("group", group);
		if (ageDivision != null)
			query.setParameter("division", ageDivision);
	}

	private static String filteredWhere(String lastName, Group group, AgeDivision ageDivision, Boolean weighedIn) {
		String byAgeDivision = "where c.ageDivision = :division";
		String byGroup = "where c.group = :group";
		String byName = "where c.lastName like %:lastName%";
		String byWeighIn = "where c.bodyWeight > 0";
		List<String> whereList = new LinkedList<String>();
		if (ageDivision != null)
			whereList.add(byAgeDivision);
		if (group != null)
			whereList.add(byGroup);
		if (lastName != null)
			whereList.add(byName);
		if (lastName != null)
			whereList.add(byWeighIn);
		if (whereList.size() == 0) {
			return "";
		} else {
			return String.join(" and ", whereList);
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
		return findFiltered(null, group, null, weighedIn, -1, -1);
	}

	public static List<Athlete> doFindAllByGroupAndWeighIn(EntityManager em, Group group, Boolean weighedIn) {
		return doFindFiltered(em, (String) null, group, (AgeDivision) null, weighedIn, -1, -1);
	}

}
