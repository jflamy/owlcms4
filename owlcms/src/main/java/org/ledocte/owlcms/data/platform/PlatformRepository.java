package org.ledocte.owlcms.data.platform;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.ledocte.owlcms.data.jpa.JPAService;

/**
 * @author Alejandro Duarte
 */
public class PlatformRepository {

	@SuppressWarnings("unchecked")
	public static Platform getById(Long id, EntityManager em) {
		Query query = em.createQuery("select u from Platform u where u.id=:id");
		query.setParameter("id", id);

		return (Platform) query.getResultList()
			.stream()
			.findFirst()
			.orElse(null);
	}

	public static Platform save(Platform Platform) {
		return JPAService.runInTransaction(em -> em.merge(Platform));
	}

	public static void delete(Platform Platform) {
		JPAService.runInTransaction(em -> {
			em.remove(getById(Platform.getId(), em));
			return null;
		});
	}

	@SuppressWarnings("unchecked")
	public static List<Platform> findAll() {
		return JPAService.runInTransaction(em -> em.createQuery("select c from Platform c")
			.getResultList());
	}

}
