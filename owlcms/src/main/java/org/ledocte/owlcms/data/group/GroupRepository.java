package org.ledocte.owlcms.data.group;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.ledocte.owlcms.data.jpa.JPAService;

/**
 * @author Alejandro Duarte
 */
public class GroupRepository {

	@SuppressWarnings("unchecked")
	public static Group getById(Long id, EntityManager em) {
		Query query = em.createQuery("select u from Group u where u.id=:id");
		query.setParameter("id", id);

		return (Group) query.getResultList()
			.stream()
			.findFirst()
			.orElse(null);
	}

	public static Group save(Group Group) {
		return JPAService.runInTransaction(em -> em.merge(Group));
	}

	public static void delete(Group Group) {
		JPAService.runInTransaction(em -> {
			em.remove(getById(Group.getId(), em));
			return null;
		});
	}

	@SuppressWarnings("unchecked")
	public static List<Group> findAll() {
		return JPAService.runInTransaction(em -> em.createQuery("select c from Group c")
			.getResultList());
	}

}
