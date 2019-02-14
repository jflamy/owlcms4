package org.ledocte.owlcms.data.athlete;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.ledocte.owlcms.data.category.AgeDivision;
import org.ledocte.owlcms.data.group.Group;
import org.ledocte.owlcms.data.jpa.JPAService;

public class AthleteRepository {

	@SuppressWarnings("unchecked")
	public static Athlete getById(Long id, EntityManager em) {
		Query query = em.createQuery("select u from Athlete u where u.id=:id");
		query.setParameter("id", id);

		return (Athlete) query.getResultList()
			.stream()
			.findFirst()
			.orElse(null);
	}

	public static Athlete save(Athlete Athlete) {
		return JPAService.runInTransaction(em -> em.merge(Athlete));
	}

	public static void delete(Athlete Athlete) {
		JPAService.runInTransaction(em -> {
			em.remove(getById(Athlete.getId(), em));
			return null;
		});
	}

	@SuppressWarnings("unchecked")
	public static List<Athlete> findAll() {
		return JPAService.runInTransaction(em -> em.createQuery("select c from Athlete c")
			.getResultList());
	}

	@SuppressWarnings("unchecked")
	public static List<Athlete> findFiltered(String lastName, Group group, AgeDivision ageDivision, Boolean weighedIn,
			int offset, int limit) {
		String where = filteredWhere(lastName, group, ageDivision, weighedIn);
		return JPAService.runInTransaction(em -> {
			Query query = em.createQuery(
				"select c from Athlete c " +
						(where != null ? " where " + where : ""));
			setFilteredParameters(lastName, group, ageDivision, query);
			if (offset >= 0) query.setFirstResult(offset);
			if (limit > 0) query.setMaxResults(limit);
			List<Athlete> resultList = query.getResultList();
			return resultList;
		});
	}
	
	public static int countFiltered(String lastName, Group group, AgeDivision ageDivision, Boolean weighedIn) {
		String where = filteredWhere(lastName, group, ageDivision, weighedIn);
		return JPAService.runInTransaction(em -> {
			Query query = em.createQuery(
				"select count(c.id) from Athlete c " +
						(where != null ? " where " + where : ""));
			setFilteredParameters(lastName, group, ageDivision, query);
			int i = ((Long) query.getSingleResult()).intValue();
			return i;
		});
	}

	private static void setFilteredParameters(String lastName, Group group, AgeDivision ageDivision, Query query) {
		query.setParameter("lastName", lastName);
		query.setParameter("group", group);
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
			return null;
		} else {
			return String.join(" and ", whereList);
		}
	}


	public static List<Athlete> findAllByGroupAndWeighIn(Group group, Boolean weighedIn) {
		return findFiltered(null, group, null, weighedIn, -1, -1);
	}

}
