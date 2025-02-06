/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.technicalofficial;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.slf4j.LoggerFactory;

import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Logger;

/**
 * TechnicalOfficialRepository.
 *
 */
public class TechnicalOfficialRepository {

	@SuppressWarnings("unused")
	final private static Logger logger = (Logger) LoggerFactory.getLogger(TechnicalOfficialRepository.class);

	public static void delete(TechnicalOfficial to) {
		JPAService.runInTransaction(em -> {
			em.remove(em.contains(to) ? to : em.merge(to));
			return null;
		});
	}

	public static List<TechnicalOfficial> findAll() {
		return JPAService
				.runInTransaction(em -> em.createQuery("select c from TechnicalOfficial c order by c.id", TechnicalOfficial.class)
						.getResultList());
	}

	public static TechnicalOfficial findByName(String string) {
		return JPAService.runInTransaction(em -> {
			TypedQuery<TechnicalOfficial> query = em.createQuery("select c from TechnicalOfficial c where lower(name) = lower(:string)", TechnicalOfficial.class);
			query.setParameter("string", string);
			List<TechnicalOfficial> resultList = query.getResultList();
			return resultList.isEmpty() ? null : resultList.get(0);
		});
	}

	public static TechnicalOfficial getById(Long id, EntityManager em) {
		TypedQuery<TechnicalOfficial> query = em.createQuery("select u from TechnicalOfficial u where u.id=:id",
				TechnicalOfficial.class);
		query.setParameter("id", id);

		return query.getResultList().stream().findFirst().orElse(null);
	}

	public static TechnicalOfficial save(TechnicalOfficial technicalOfficial) {
		TechnicalOfficial nTechnicalOfficial = JPAService.runInTransaction(em -> em.merge(technicalOfficial));
		return nTechnicalOfficial;
	}

	public static void deleteAll(EntityManager em) {
		// use JPQL to delete all rows
		em.createQuery("delete from TechnicalOfficial").executeUpdate();
	}
}
