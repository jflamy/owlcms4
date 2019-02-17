/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.data.competition;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.ledocte.owlcms.data.category.AgeDivision;
import org.ledocte.owlcms.data.jpa.JPAService;

/**
 * The Class CompetitionRepository.
 *
 * @author Alejandro Duarte
 */
public class CompetitionRepository {
	
	/**
	 * Gets the by id.
	 *
	 * @param id the id
	 * @param em the em
	 * @return the by id
	 */
	@SuppressWarnings("unchecked")
	public static Competition getById(Long id, EntityManager em) {
		Query query = em.createQuery("select u from Competition u where u.id=:id");
		query.setParameter("id", id);

		return (Competition) query.getResultList()
			.stream()
			.findFirst()
			.orElse(null);
	}

	/**
	 * Save.
	 *
	 * @param Competition the competition
	 * @return the competition
	 */
	public static Competition save(Competition Competition) {
		return JPAService.runInTransaction(em -> em.merge(Competition));
	}

	/**
	 * Delete.
	 *
	 * @param Competition the competition
	 */
	public static void delete(Competition Competition) {
		JPAService.runInTransaction(em -> {
			em.remove(getById(Competition.getId(), em));
			return null;
		});
	}
	
	/**
	 * Find all.
	 *
	 * @return the list
	 */
	@SuppressWarnings("unchecked")
	public static List<Competition> findAll() {
		return JPAService.runInTransaction(em -> em.createQuery("select c from Competition c")
			.getResultList());
	}

	private static String byAgeDivision = "from Competition c where c.ageDivision = :division";

	/**
	 * Find by age division.
	 *
	 * @param ageDivision the age division
	 * @param offset the offset
	 * @param limit the limit
	 * @return the collection
	 */
	@SuppressWarnings("unchecked")
	public static Collection<Competition> findByAgeDivision(AgeDivision ageDivision, int offset, int limit) {
		if (ageDivision == null) {
			return JPAService.runInTransaction(em -> {
				Query query = em.createQuery("select c from Competition c");
				query.setFirstResult(offset);
				query.setMaxResults(limit);
				return query.getResultList();
			});
		} else {
			return JPAService.runInTransaction(em -> {
				Query query = em.createQuery("select c " + byAgeDivision);
				query.setParameter("division", ageDivision);
				query.setFirstResult(offset);
				query.setMaxResults(limit);
				List<Competition> resultList = query.getResultList();
				return resultList;
			});
		}
	}

	/**
	 * Count by age division.
	 *
	 * @param ageDivision the age division
	 * @return the int
	 */
	public static int countByAgeDivision(AgeDivision ageDivision) {
		if (ageDivision == null) {
			return JPAService.runInTransaction(em -> {
				Query query = em.createQuery("select count(c.id) from Competition c");
				int i = ((Long) query.getSingleResult()).intValue();
				return i;
			});
		} else {
			return JPAService.runInTransaction(em -> {
				Query query = em.createQuery("select count(c.id) " + byAgeDivision);
				query.setParameter("division", ageDivision);
				int i = ((Long) query.getSingleResult()).intValue();
				return i;
			});
		}
	}

}
