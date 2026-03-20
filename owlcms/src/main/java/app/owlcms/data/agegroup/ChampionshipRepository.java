/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.TypedQuery;

import org.slf4j.LoggerFactory;

import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

/**
 * Repository for persisted Championship entities.
 */
public class ChampionshipRepository {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(ChampionshipRepository.class);

	/**
	 * Find all stored championships.
	 */
	@SuppressWarnings("unchecked")
	public static List<Championship> findAll() {
		return JPAService.runInTransaction(em -> {
			return em.createQuery("select c from Championship c").getResultList();
		});
	}

	/**
	 * Find a championship by its canonical name (exact match).
	 */
	public static Championship findByName(String name) {
		if (name == null) {
			return null;
		}
		String canonical = Championship.canonicalizeChampionshipName(name);
		return JPAService.runInTransaction(em -> {
			TypedQuery<Championship> q = em.createQuery(
			        "select c from Championship c where c.name = :name", Championship.class);
			q.setParameter("name", canonical);
			List<Championship> results = q.getResultList();
			return results.isEmpty() ? null : results.get(0);
		});
	}

	/**
	 * Save (create or update) a championship.
	 */
	public static Championship save(Championship c) {
		return JPAService.runInTransaction(em -> {
			return em.merge(c);
		});
	}

	/**
	 * Delete a championship.
	 */
	public static void delete(Championship c) {
		JPAService.runInTransaction(em -> {
			Championship managed = em.find(Championship.class, c.getId());
			if (managed != null) {
				em.remove(managed);
			}
			return null;
		});
	}

	/**
	 * Bootstrap stored championships from persisted age groups when the Championship
	 * table is empty. Called once on first startup after upgrade.
	 */
	public static void bootstrapFromAgeGroups() {
		long count = JPAService.runInTransaction(em -> {
			return (Long) em.createQuery("select count(c) from Championship c").getSingleResult();
		});
		if (count > 0) {
			logger.debug("Championship table already has {} rows, skipping bootstrap", count);
			return;
		}
		logger.info("Bootstrapping Championship table from persisted age groups");
		reconcileFromAgeGroups();
	}

	/**
	 * Reconcile stored championships with the current state of persisted age groups.
	 * Creates missing Championship rows and updates types where age groups disagree.
	 */
	public static void reconcileFromAgeGroups() {
		JPAService.runInTransaction(em -> {
			TypedQuery<AgeGroup> q = em.createQuery("select ag from AgeGroup ag", AgeGroup.class);
			List<AgeGroup> ageGroups = q.getResultList();

			// Group age groups by canonical championship name, preserving order.
			// "Last age group read wins" for type resolution.
			Map<String, ChampionshipType> nameToType = new LinkedHashMap<>();
			for (AgeGroup ag : ageGroups) {
				String champName = ag.getChampionshipName();
				if (champName == null || champName.isBlank()) {
					throw new IllegalStateException("AgeGroup " + ag.getCode() + " is missing championshipName");
				}
				String canonical = Championship.canonicalizeChampionshipName(champName);
				ChampionshipType agType = ag.getChampionshipType();
				if (agType == null) {
					agType = ChampionshipType.U;
				}
				nameToType.put(canonical, agType); // last wins
			}

			// Ensure DEFAULT and MASTERS are always present
			String defaultName = Translator.translate("Division." + ChampionshipType.DEFAULT.name());
			nameToType.putIfAbsent(defaultName, ChampionshipType.DEFAULT);
			String mastersName = Translator.translate("Division." + ChampionshipType.MASTERS.name());
			mastersName = Championship.canonicalizeChampionshipName(mastersName);
			nameToType.putIfAbsent(mastersName, ChampionshipType.MASTERS);

			// Create or update Championship rows
			for (Map.Entry<String, ChampionshipType> entry : nameToType.entrySet()) {
				String name = entry.getKey();
				ChampionshipType type = entry.getValue();
				// Apply canonical type resolution for known names
				type = canonicalizeType(name, type);

				TypedQuery<Championship> findQ = em.createQuery(
				        "select c from Championship c where lower(c.name) = :name", Championship.class);
				findQ.setParameter("name", name.toLowerCase());
				List<Championship> existing = findQ.getResultList();

				if (existing.isEmpty()) {
					Championship c = new Championship(name, type);
					em.persist(c);
					logger.info("Created stored championship: name='{}', type='{}'", name, type);
				} else {
					Championship c = existing.get(0);
					if (c.getType() != type) {
						logger.info("Updated championship type: name='{}', {} -> {}", name, c.getType(), type);
						c.setType(type);
						em.merge(c);
					}
				}
			}

			em.flush();
			return null;
		});
	}

	/**
	 * Same canonical type logic as Championship.canonicalizeChampionshipType.
	 */
	private static ChampionshipType canonicalizeType(String name, ChampionshipType type) {
		if (name != null && name.equals("Masters")) {
			return ChampionshipType.MASTERS;
		}
		return type != null ? type : ChampionshipType.U;
	}
}
