/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.data.competition;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import app.owlcms.data.jpa.JPAService;

/**
 * CompetitionRepository.
 *
 */
public class CompetitionRepository {
	
	/**
	 * Gets Competition by id.
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
		 Competition mc = JPAService.runInTransaction(em -> {
			Competition nc = em.merge(Competition);
			// needed because some classes get competition parameters from getCurrent()
			app.owlcms.data.competition.Competition.setCurrent(nc);
			return nc;
		});
		return mc;
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

}
