package org.ledocte.owlcms.data.athlete;

import java.util.Collection;
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

	private static String byAgeDivision = "where c.ageDivision = :division";
	private static String byGroup = "where c.group = :group";
	private static String byName = "where c.lastName like %:lastName%";

	@SuppressWarnings("unchecked")
	public static Collection<Athlete> findFiltered(String lastName, Group group, AgeDivision ageDivision,
			int offset, int limit) {
		List<String> whereList = new LinkedList<String>();
		if (ageDivision != null)
			whereList.add(byAgeDivision);
		if (group != null)
			whereList.add(byGroup);
		if (lastName != null)
			whereList.add(byName);
		String where = String.join(" and ", whereList);

		return JPAService.runInTransaction(em -> {
			Query query = em.createQuery(
				"select c from Athlete c " +
						(whereList.size() > 0 ? " where " + where : ""));
			query.setParameter("lastName", lastName);
			query.setParameter("group", group);
			query.setParameter("division", ageDivision);
			query.setFirstResult(offset);
			query.setMaxResults(limit);
			List<Athlete> resultList = query.getResultList();
			return resultList;
		});
	}

	public static int countByAgeDivision(AgeDivision ageDivision) {
		if (ageDivision == null) {
			return JPAService.runInTransaction(em -> {
				Query query = em.createQuery("select count(c.id) from Athlete c");
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
