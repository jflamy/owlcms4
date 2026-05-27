/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.util.List;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.slf4j.LoggerFactory;

import app.owlcms.data.competition.Competition;
import app.owlcms.data.jpa.JPAService;
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
			Championship saved = em.merge(c);
			normalizeDefaultTypes(em);
			normalizeCompetitionDefaultFlags(em);
			return saved;
		});
	}

	public static Championship ensureCompetitionTemplate() {
		Championship template = JPAService.runInTransaction(em -> ensureCompetitionTemplate(em));
		Championship.reset();
		return template;
	}

	public static Championship findCompetitionTemplate() {
		return JPAService.runInTransaction(em -> findCompetitionTemplate(em));
	}

	public static Championship findCompetitionTemplate(EntityManager em) {
		TypedQuery<Championship> query = em.createQuery(
		        "select c from Championship c where c.competitionTemplate = true order by c.id", Championship.class);
		return query.getResultList().stream().findFirst().orElse(null);
	}

	public static Championship ensureCompetitionTemplate(EntityManager em) {
		TypedQuery<Championship> query = em.createQuery(
		        "select c from Championship c where c.competitionTemplate = true order by c.id", Championship.class);
		List<Championship> templates = query.getResultList();
		if (!templates.isEmpty()) {
			Championship template = templates.get(0);
			template.setCompetitionTemplate(true);
			template.setUseCompetitionDefaults(false);
			for (int i = 1; i < templates.size(); i++) {
				Championship duplicate = templates.get(i);
				duplicate.setCompetitionTemplate(false);
				em.merge(duplicate);
				logger.warn("Removed duplicate competition template marker from championship '{}'", duplicate.getName());
			}
			return em.merge(template);
		}

		Championship template = new Championship(Championship.COMPETITION_TEMPLATE_NAME, ChampionshipType.U);
		template.populateCompetitionTemplateDefaults(Competition.getCurrent());
		em.persist(template);
		logger.info("Created competition championship template: name='{}'", template.getName());
		return template;
	}

	public static void updateCompetitionTemplate(Consumer<Championship> updater) {
		if (updater == null) {
			return;
		}
		Boolean updated = JPAService.runInTransaction(em -> {
			Championship template = findCompetitionTemplate(em);
			if (template != null) {
				updater.accept(template);
				em.merge(template);
				normalizeCompetitionDefaultFlags(em);
				return Boolean.TRUE;
			}
			return Boolean.FALSE;
		});
		if (Boolean.TRUE.equals(updated)) {
			Championship.reset();
		}
	}

	public static Championship rename(Championship championship, String newName) {
		if (championship == null || newName == null) {
			return championship;
		}
		String canonicalNewName = Championship.canonicalizeChampionshipName(newName.trim());
		return JPAService.runInTransaction(em -> {
			Championship managed = championship.getId() != null ? em.find(Championship.class, championship.getId()) : championship;
			if (managed == null) {
				return null;
			}
			String oldName = managed.getName();
			if (oldName != null && oldName.equals(canonicalNewName)) {
				return managed;
			}

			TypedQuery<AgeGroup> query = em.createQuery(
			        "select ag from AgeGroup ag where lower(trim(ag.championshipName)) = :championshipName",
			        AgeGroup.class);
			query.setParameter("championshipName", oldName != null ? oldName.trim().toLowerCase() : null);
			for (AgeGroup ageGroup : query.getResultList()) {
				ageGroup.setChampionshipName(canonicalNewName);
			}

			managed.setName(canonicalNewName);
			return em.merge(managed);
		});
	}

	public static Championship resetToCompetitionDefaults(Championship championship) {
		if (championship == null || championship.isCompetitionTemplate()) {
			return championship;
		}
		Championship saved = JPAService.runInTransaction(em -> {
			Championship managed = championship.getId() != null
			        ? em.find(Championship.class, championship.getId())
			        : em.merge(championship);
			if (managed == null) {
				return null;
			}
			Championship template = ensureCompetitionTemplate(em);
			managed.copyCompetitionSettingsFrom(template);
			managed.setUseCompetitionDefaults(true);
			Championship merged = em.merge(managed);
			normalizeCompetitionDefaultFlags(em);
			return merged;
		});
		Championship.reset();
		return saved;
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
			ensureCompetitionTemplate();
			migrateScoringFieldsIfNeeded();
			normalizeDefaultTypes();
			normalizeCompetitionDefaultFlags();
			logger.debug("Championship table already has {} rows, skipping bootstrap", count);
			return;
		}
		logger.info("Creating competition championship template for legacy age groups");
		ensureCompetitionTemplate();
		normalizeDefaultTypes();
		normalizeCompetitionDefaultFlags();
	}

	private static void migrateScoringFieldsIfNeeded() {
		JPAService.runInTransaction(em -> {
			TypedQuery<Championship> query = em.createQuery(
			        "select c from Championship c where c.scoringSystem is null and c.competitionTemplate = false", Championship.class);
			List<Championship> championshipsNeedingMigration = query.getResultList();
			for (Championship championship : championshipsNeedingMigration) {
				championship.populateScoringDefaults();
				em.merge(championship);
				logger.info("Migrated scoring fields for championship '{}'", championship.getName());
			}
			if (!championshipsNeedingMigration.isEmpty()) {
				em.flush();
			}
			return null;
		});
	}

	/**
	 * Reconcile stored championships with the current state of persisted age groups.
	 * Legacy age groups use the competition template unless an explicit championship
	 * row already exists.
	 */
	public static void reconcileFromAgeGroups() {
		JPAService.runInTransaction(em -> {
			ensureCompetitionTemplate(em);
			TypedQuery<AgeGroup> q = em.createQuery("select ag from AgeGroup ag", AgeGroup.class);
			List<AgeGroup> ageGroups = q.getResultList();
			for (AgeGroup ag : ageGroups) {
				String champName = ag.getChampionshipName();
				if (champName == null || champName.isBlank()) {
					throw new IllegalStateException("AgeGroup " + ag.getCode() + " is missing championshipName");
				}
				String canonical = Championship.canonicalizeChampionshipName(champName);
				ChampionshipType agType = ag.getConfiguredChampionshipType();
				TypedQuery<Championship> findQ = em.createQuery(
				        "select c from Championship c where lower(c.name) = :name and c.competitionTemplate = false", Championship.class);
				findQ.setParameter("name", canonical.toLowerCase());
				List<Championship> existing = findQ.getResultList();
				if (!existing.isEmpty()) {
					Championship c = existing.get(0);
					ChampionshipType type = canonicalizeType(canonical, agType);
					if (c.getType() != type) {
						logger.info("Updated championship type: name='{}', {} -> {}", canonical, c.getType(), type);
						c.setType(type);
					}
					if (c.getScoringSystem() == null) {
						c.populateScoringDefaults();
					}
					em.merge(c);
				}
			}

			em.flush();
			normalizeDefaultTypes(em);
			normalizeCompetitionDefaultFlags(em);
			return null;
		});
		Championship.reset();
	}

	public static void normalizeDefaultTypes() {
		Boolean changed = JPAService.runInTransaction(em -> normalizeDefaultTypes(em));
		if (Boolean.TRUE.equals(changed)) {
			Championship.reset();
		}
	}

	public static void normalizeCompetitionDefaultFlags() {
		Boolean changed = JPAService.runInTransaction(em -> normalizeCompetitionDefaultFlags(em));
		if (Boolean.TRUE.equals(changed)) {
			Championship.reset();
		}
	}

	static boolean normalizeCompetitionDefaultFlags(EntityManager em) {
		Championship template = findCompetitionTemplate(em);
		boolean changed = template == null;
		template = ensureCompetitionTemplate(em);
		TypedQuery<Championship> query = em.createQuery(
		        "select c from Championship c where c.competitionTemplate = false order by c.id", Championship.class);
		for (Championship championship : query.getResultList()) {
			boolean sameAsTemplate = championship.hasSameCompetitionSettingsAs(template);
			if (championship.usesCompetitionDefaults() != sameAsTemplate) {
				championship.setUseCompetitionDefaults(sameAsTemplate);
				em.merge(championship);
				changed = true;
				logger.info("Updated competition default flag for championship '{}': {}",
				        championship.getName(), sameAsTemplate);
			}
		}
		if (changed) {
			em.flush();
		}
		return changed;
	}

	static boolean normalizeDefaultTypes(EntityManager em) {
		boolean changed = false;

		TypedQuery<Championship> championshipQuery = em.createQuery(
		        "select c from Championship c where c.competitionTemplate = false order by c.id", Championship.class);
		List<Championship> championships = championshipQuery.getResultList();
		TypedQuery<AgeGroup> ageGroupQuery = em.createQuery("select ag from AgeGroup ag order by ag.id", AgeGroup.class);
		List<AgeGroup> ageGroups = ageGroupQuery.getResultList();
		for (Championship championship : championships) {
			if (championship.getStoredType() == ChampionshipType.IWF) {
				logger.info("Reverted legacy IWF championship '{}' to U", championship.getName());
				championship.setType(ChampionshipType.U);
				em.merge(championship);
				changed = true;
			}
		}

		for (AgeGroup ageGroup : ageGroups) {
			if (ageGroup.getStoredChampionshipType() == ChampionshipType.IWF || isLegacyIwfAgeDivision(ageGroup)) {
				logger.info("Reverted legacy IWF age group '{}' championship '{}' to U",
				        ageGroup.getCode(), ageGroup.computeChampionshipName());
				ageGroup.setChampionshipType(ChampionshipType.U);
				if (ageGroup.getAgeDivision() == null || ageGroup.getAgeDivision().isBlank()
				        || ageGroup.getAgeDivision().equalsIgnoreCase(ChampionshipType.IWF.name())) {
					ageGroup.setAgeDivision(ChampionshipType.U.name());
				}
				em.merge(ageGroup);
				changed = true;
			}
		}

		String defaultChampionshipName = findDefaultChampionshipName(ageGroups, championships);

		for (Championship championship : championships) {
			if (championship.getType() != ChampionshipType.DEFAULT) {
				continue;
			}
			String canonical = Championship.canonicalizeChampionshipName(championship.getName());
			if (defaultChampionshipName != null && !defaultChampionshipName.equalsIgnoreCase(canonical)) {
				logger.info("Reverted extra DEFAULT championship '{}' to U; DEFAULT already belongs to '{}'",
				        championship.getName(), defaultChampionshipName);
				championship.setType(ChampionshipType.U);
				em.merge(championship);
				changed = true;
			}
		}

		for (AgeGroup ageGroup : ageGroups) {
			if (ageGroup.getConfiguredChampionshipType() != ChampionshipType.DEFAULT) {
				continue;
			}
			String canonical = Championship.canonicalizeChampionshipName(ageGroup.computeChampionshipName());
			if (defaultChampionshipName != null && !defaultChampionshipName.equalsIgnoreCase(canonical)) {
				logger.info("Reverted extra DEFAULT age group '{}' championship '{}' to U; DEFAULT already belongs to '{}'",
				        ageGroup.getCode(), canonical, defaultChampionshipName);
				ageGroup.setChampionshipType(ChampionshipType.U);
				if (ageGroup.getAgeDivision() == null || ageGroup.getAgeDivision().isBlank()
				        || ageGroup.getAgeDivision().equalsIgnoreCase(ChampionshipType.DEFAULT.name())) {
					ageGroup.setAgeDivision(ChampionshipType.U.name());
				}
				em.merge(ageGroup);
				changed = true;
			}
		}

		if (changed) {
			em.flush();
		}
		return changed;
	}

	private static boolean isLegacyIwfAgeDivision(AgeGroup ageGroup) {
		return ageGroup.getAgeDivision() != null
		        && ageGroup.getAgeDivision().trim().equalsIgnoreCase(ChampionshipType.IWF.name());
	}

	private static String findDefaultChampionshipName(List<AgeGroup> ageGroups, List<Championship> championships) {
		for (AgeGroup ageGroup : ageGroups) {
			String canonical = Championship.canonicalizeChampionshipName(ageGroup.computeChampionshipName());
			if (ageGroup.getConfiguredChampionshipType() == ChampionshipType.DEFAULT
			        || isStoredDefaultChampionship(canonical, championships)) {
				return canonical;
			}
		}
		for (Championship championship : championships) {
			if (championship.getType() == ChampionshipType.DEFAULT) {
				return Championship.canonicalizeChampionshipName(championship.getName());
			}
		}
		return null;
	}

	private static boolean isStoredDefaultChampionship(String canonicalName, List<Championship> championships) {
		for (Championship championship : championships) {
			String championshipName = Championship.canonicalizeChampionshipName(championship.getName());
			if (championship.getType() == ChampionshipType.DEFAULT
			        && championshipName != null
			        && championshipName.equalsIgnoreCase(canonicalName)) {
				return true;
			}
		}
		return false;
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
