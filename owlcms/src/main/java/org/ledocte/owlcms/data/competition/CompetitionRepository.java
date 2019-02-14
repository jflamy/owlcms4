package org.ledocte.owlcms.data.competition;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.ledocte.owlcms.data.category.AgeDivision;
import org.ledocte.owlcms.data.jpa.JPAService;

/**
 * @author Alejandro Duarte
 */
public class CompetitionRepository {
	
	@SuppressWarnings("unchecked")
	public static Competition getById(Long id, EntityManager em) {
		Query query = em.createQuery("select u from Competition u where u.id=:id");
		query.setParameter("id", id);

		return (Competition) query.getResultList()
			.stream()
			.findFirst()
			.orElse(null);
	}

	public static Competition save(Competition Competition) {
		return JPAService.runInTransaction(em -> em.merge(Competition));
	}

	public static void delete(Competition Competition) {
		JPAService.runInTransaction(em -> {
			em.remove(getById(Competition.getId(), em));
			return null;
		});
	}
	
	@SuppressWarnings("unchecked")
	public static List<Competition> findAll() {
		return JPAService.runInTransaction(em -> em.createQuery("select c from Competition c")
			.getResultList());
	}

	private static String byAgeDivision = "from Competition c where c.ageDivision = :division";

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
