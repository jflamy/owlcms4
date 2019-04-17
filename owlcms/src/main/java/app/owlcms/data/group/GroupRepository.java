/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Non-Profit Open Software License ("Non-Profit OSL") 3.0 
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.data.group;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import app.owlcms.data.jpa.JPAService;

/**
 * GroupRepository.
 *
 */
public class GroupRepository {

	/**
	 * Gets group by id
	 *
	 * @param id the id
	 * @param em entity manager
	 * @return the group, null if not found
	 */
	@SuppressWarnings("unchecked")
	public static Group getById(Long id, EntityManager em) {
		Query query = em.createQuery("select u from CompetitionGroup u where u.id=:id");
		query.setParameter("id", id);
		return (Group) query.getResultList()
			.stream()
			.findFirst()
			.orElse(null);
	}

	/**
	 * Save.
	 *
	 * @param Group the group
	 * @return the group
	 */
	public static Group save(Group Group) {
		return JPAService.runInTransaction(em -> em.merge(Group));
	}

	/**
	 * Delete.
	 *
	 * @param Group the group
	 */
	public static void delete(Group Group) {
		JPAService.runInTransaction(em -> {
			em.remove(getById(Group.getId(), em));
			return null;
		});
	}

	/**
	 * Find all.
	 *
	 * @return the list
	 */
	@SuppressWarnings("unchecked")
	public static List<Group> findAll() {
		return JPAService.runInTransaction(em -> em.createQuery("select c from CompetitionGroup c")
			.getResultList());
	}

	public static Group findByName(String name) {
		return JPAService.runInTransaction(em -> {
			return doFindByName(name, em);
		});
	}

	@SuppressWarnings("unchecked")
	public static Group doFindByName(String name, EntityManager em) {
		Query query = em.createQuery("select u from CompetitionGroup u where u.name=:name");
		query.setParameter("name", name);
		return (Group) query.getResultList()
			.stream()
			.findFirst()
			.orElse(null);
	}

}
