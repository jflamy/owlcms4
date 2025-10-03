/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.technicalofficial;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.slf4j.LoggerFactory;

import app.owlcms.data.group.Group;
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
				.runInTransaction(em -> em.createQuery("select c from TechnicalOfficial c order by c.lastName, c.firstName", TechnicalOfficial.class)
						.getResultList());
	}

	public static TechnicalOfficial findByName(String string) {
		String[] t = string.split("[, ]+");
		String lastName = t[0];
		String firstName = t[1];
		return JPAService.runInTransaction(em -> {
			TypedQuery<TechnicalOfficial> query = em.createQuery("select c from TechnicalOfficial c where (lower(lastName) = lower(:lastName) and lower(firstName) = lower(:firstName))", TechnicalOfficial.class);
			query.setParameter("lastName", lastName);
			query.setParameter("firstName", firstName);
			List<TechnicalOfficial> resultList = query.getResultList();
			return resultList.isEmpty() ? null : resultList.get(0);
		});
	}
	
	public static TechnicalOfficial safeFindByName(String string) {
		// Return null if the input is null or blank - this indicates no official is assigned
		if (string == null || string.isBlank()) {
			return null;
		}
		TechnicalOfficial to = findByName(string);
		if (to == null) {
			to = new TechnicalOfficial();
			to.setLastName(string);
		}
		return to;
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

	/**
	 * Find active technical officials.
	 * Returns officials that are either:
	 * 1. Explicitly marked as active (active flag = true)
	 * 2. Implicitly active because they are assigned to a session/group
	 * 
	 * @return List of active technical officials (no duplicates)
	 */
	public static List<TechnicalOfficial> findActive() {
		return JPAService.runInTransaction(em -> {
			// Get all explicitly active officials
			TypedQuery<TechnicalOfficial> query = em.createQuery(
				"select t from TechnicalOfficial t where t.active = true order by t.lastName, t.firstName", 
				TechnicalOfficial.class);
			List<TechnicalOfficial> activeOfficials = query.getResultList();
			
			// Use a Set to avoid duplicates
			Set<TechnicalOfficial> resultSet = new LinkedHashSet<>(activeOfficials);
			
		// Get all groups and extract TOs assigned to them
		TypedQuery<Group> groupQuery = em.createQuery(
			"select g from app.owlcms.data.group.Group g", 
			Group.class);
		List<Group> groups = groupQuery.getResultList();			// For each group, collect all assigned TOs
			for (Group group : groups) {
				List<TechnicalOfficial> assignedOfficials = group.findAssignedTechnicalOfficials();
				resultSet.addAll(assignedOfficials);
			}
			
			// Convert set back to list and sort
			List<TechnicalOfficial> result = new ArrayList<>(resultSet);
			result.sort((a, b) -> {
				int lastNameCompare = (a.getLastName() != null ? a.getLastName() : "").compareTo(
					b.getLastName() != null ? b.getLastName() : "");
				if (lastNameCompare != 0) return lastNameCompare;
				return (a.getFirstName() != null ? a.getFirstName() : "").compareTo(
					b.getFirstName() != null ? b.getFirstName() : "");
			});
			
			return result;
		});
	}
}
