/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.util.ArrayList;
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
	 * Find all real stored championships, excluding the competition template.
	 */
	public static List<Championship> findAll() {
		return JPAService.runInTransaction(em -> {
			return em.createQuery(
			        "select c from Championship c where c.competitionTemplate = false order by c.id", Championship.class)
			        .getResultList();
		});
	}

	/**
	 * Find all stored championships, including the competition template.
	 */
	public static List<Championship> findAllIncludingTemplate() {
		return JPAService.runInTransaction(em -> {
			return em.createQuery("select c from Championship c order by c.id", Championship.class)
			        .getResultList();
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
		applyLegacyTemplateTeamSizeDefaults(em, template);
		em.persist(template);
		logger.info("Created competition championship template: name='{}'", template.getName());
		return template;
	}

	private static void applyLegacyTemplateTeamSizeDefaults(EntityManager em, Championship template) {
		Integer inferredTeamSize = inferLegacyTemplateTeamSize(em, template.getMaxTeamSize());
		if (inferredTeamSize == null) {
			return;
		}
		template.setMaxTeamSize(inferredTeamSize);
		template.setMensBestN(capTemplateBestN(template.getMensBestN(), inferredTeamSize));
		template.setWomensBestN(capTemplateBestN(template.getWomensBestN(), inferredTeamSize));
	}

	private static Integer inferLegacyTemplateTeamSize(EntityManager em, Integer templateMaxTeamSize) {
		if (templateMaxTeamSize == null || templateMaxTeamSize != 8) {
			return null;
		}
		TypedQuery<Championship> query = em.createQuery(
		        "select c from Championship c where c.competitionTemplate = false order by c.id", Championship.class);
		List<Integer> candidateCaps = new ArrayList<>();
		for (Championship championship : query.getResultList()) {
			addLegacyTeamSizeCandidate(candidateCaps, championship.getMaxTeamSize(), templateMaxTeamSize);
			addLegacyTeamSizeCandidate(candidateCaps, championship.getMensBestN(), templateMaxTeamSize);
			addLegacyTeamSizeCandidate(candidateCaps, championship.getWomensBestN(), templateMaxTeamSize);
			addLegacyTeamSizeCandidate(candidateCaps, championship.getMixedBestN(), templateMaxTeamSize);
		}
		return candidateCaps.stream().min(Integer::compareTo).orElse(null);
	}

	private static void addLegacyTeamSizeCandidate(List<Integer> candidateCaps, Integer value, Integer templateMaxTeamSize) {
		if (value != null && value > 0 && value < templateMaxTeamSize) {
			candidateCaps.add(value);
		}
	}

	private static Integer capTemplateBestN(Integer bestN, Integer teamSize) {
		return bestN != null && bestN > teamSize ? teamSize : bestN;
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
			TypedQuery<Championship> duplicateQuery = em.createQuery(
			        "select c from Championship c where lower(trim(c.name)) = :name", Championship.class);
			duplicateQuery.setParameter("name", canonicalNewName.trim().toLowerCase());
			for (Championship duplicate : duplicateQuery.getResultList()) {
				if (!duplicate.getId().equals(managed.getId())) {
					logger.warn("Rejected championship rename from '{}' to '{}': duplicate championship '{}' already exists",
					        oldName, canonicalNewName, duplicate.getName());
					throw new IllegalArgumentException("Championship " + canonicalNewName + " already exists");
				}
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
			normalizeAgeGroupChampionshipNames(em, ageGroups);
			TypedQuery<Championship> cq = em.createQuery(
			        "select c from Championship c where c.competitionTemplate = false order by c.id", Championship.class);
			List<Championship> championships = cq.getResultList();
			materializeRequiredChampionships(em, ageGroups, championships);
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

	public static Championship materializeForAgeGroup(AgeGroup ageGroup) {
		if (ageGroup == null) {
			return null;
		}
		Championship championship = JPAService.runInTransaction(em -> {
			TypedQuery<AgeGroup> aq = em.createQuery("select ag from AgeGroup ag order by ag.id", AgeGroup.class);
			List<AgeGroup> ageGroups = aq.getResultList();
			TypedQuery<Championship> cq = em.createQuery(
			        "select c from Championship c where c.competitionTemplate = false order by c.id", Championship.class);
			List<Championship> championships = cq.getResultList();
			return materializeChampionship(em, effectiveChampionshipName(ageGroup), ageGroup.getConfiguredChampionshipType(),
			        ageGroups, championships);
		});
		Championship.reset();
		return championship;
	}

	public static void materializeIfRequired(AgeGroup ageGroup) {
		if (requiresMaterializedChampionship(ageGroup)) {
			materializeForAgeGroup(ageGroup);
		}
	}

	/**
	 * Single canonical entry point for creating a new championship row.
	 * Both the manual "Add championship" action and the auto-materialization
	 * triggered by saving an AgeGroup go through this method, so the resulting
	 * row always inherits the competition template (and any per-age-group
	 * scoring overrides) the same way.
	 */
	public static Championship createChampionship(String championshipName, ChampionshipType type) {
		if (championshipName == null || championshipName.isBlank()) {
			return null;
		}
		String canonical = Championship.canonicalizeChampionshipName(championshipName);
		Championship created = JPAService.runInTransaction(em -> {
			TypedQuery<AgeGroup> aq = em.createQuery("select ag from AgeGroup ag order by ag.id", AgeGroup.class);
			List<AgeGroup> ageGroups = aq.getResultList();
			TypedQuery<Championship> cq = em.createQuery(
			        "select c from Championship c where c.competitionTemplate = false order by c.id", Championship.class);
			List<Championship> championships = cq.getResultList();
			Championship result = materializeChampionship(em, canonical, type, ageGroups, championships);
			normalizeDefaultTypes(em);
			normalizeCompetitionDefaultFlags(em);
			return result;
		});
		Championship.reset();
		return created;
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
			championship.computeCompetitionDefaultDifferences(template, true);
		}
		if (changed) {
			em.flush();
		}
		return changed;
	}

	static boolean normalizeDefaultTypes(EntityManager em) {
		boolean changed = false;

		// One-shot migration of any legacy IWF rows (championships + age groups).
		// IWF is no longer used as a championship type; treat as U. Bulk JPQL runs
		// before loading entities so the persistence context stays in sync.
		changed |= migrateLegacyIwfRows(em);

		TypedQuery<Championship> championshipQuery = em.createQuery(
		        "select c from Championship c where c.competitionTemplate = false order by c.id", Championship.class);
		List<Championship> championships = championshipQuery.getResultList();
		TypedQuery<AgeGroup> ageGroupQuery = em.createQuery("select ag from AgeGroup ag order by ag.id", AgeGroup.class);
		List<AgeGroup> ageGroups = ageGroupQuery.getResultList();
		changed |= normalizeAgeGroupChampionshipNames(em, ageGroups);
		changed |= materializeRequiredChampionships(em, ageGroups, championships);

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

	private static boolean materializeRequiredChampionships(EntityManager em, List<AgeGroup> ageGroups,
	        List<Championship> championships) {
		boolean changed = false;
		for (AgeGroup ageGroup : ageGroups) {
			if (!requiresMaterializedChampionship(ageGroup)) {
				continue;
			}
			String championshipName = effectiveChampionshipName(ageGroup);
			Championship existing = findChampionship(championshipName, championships);
			Championship template = ensureCompetitionTemplate(em);
			boolean usedDefaults = existing != null && !existing.computeDifferentFromCompetitionDefaults(template);
			Championship championship = materializeChampionship(em, championshipName, ageGroup.getConfiguredChampionshipType(),
			        ageGroups, championships);
			changed |= championship != null && (existing == null || usedDefaults);
		}
		return changed;
	}

	private static Championship materializeChampionship(EntityManager em, String championshipName, ChampionshipType type,
	        List<AgeGroup> ageGroups, List<Championship> championships) {
		if (championshipName == null || championshipName.isBlank()) {
			return null;
		}
		Championship championship = findChampionship(championshipName, championships);
		if (championship == null) {
			Championship template = ensureCompetitionTemplate(em);
			championship = new Championship(championshipName, canonicalizeType(championshipName, type));
			championship.copyCompetitionSettingsFrom(template);
			applyAgeGroupScoringOverrides(championship, championshipName, ageGroups);
			em.persist(championship);
			championships.add(championship);
			logger.info("Materialized championship '{}' from age groups", championshipName);
		} else {
			Championship template = ensureCompetitionTemplate(em);
			ChampionshipType canonicalType = canonicalizeType(championshipName, type);
			if (championship.getType() != canonicalType) {
				championship.setType(canonicalType);
			}
			if (!championship.computeDifferentFromCompetitionDefaults(template)) {
				championship.copyCompetitionSettingsFrom(template);
				applyAgeGroupScoringOverrides(championship, championshipName, ageGroups);
			}
			em.merge(championship);
		}
		return championship;
	}

	private static Championship findChampionship(String championshipName, List<Championship> championships) {
		for (Championship championship : championships) {
			String name = Championship.canonicalizeChampionshipName(championship.getName());
			if (name != null && name.equalsIgnoreCase(championshipName)) {
				return championship;
			}
		}
		return null;
	}

	private static boolean requiresMaterializedChampionship(AgeGroup ageGroup) {
		String championshipName = effectiveChampionshipName(ageGroup);
		String selfName = Championship.canonicalizeChampionshipName(ageGroup.getCode());
		return championshipName != null && !championshipName.isBlank()
		        && (!championshipName.equalsIgnoreCase(selfName)
		                || ageGroup.getConfiguredChampionshipType() != ChampionshipType.U
		                || ageGroup.getScoringSystem() != null
		                || ageGroup.getBestAthleteScoringSystem() != null
		                || Boolean.FALSE.equals(ageGroup.getMedals()));
	}

	private static String effectiveChampionshipName(AgeGroup ageGroup) {
		String name = ageGroup.getChampionshipName();
		if (name == null || name.isBlank() || name.trim().equalsIgnoreCase(Championship.COMPETITION_TEMPLATE_NAME)) {
			name = ageGroup.getCode();
		}
		return Championship.canonicalizeChampionshipName(name != null ? name.trim() : null);
	}

	private static void applyAgeGroupScoringOverrides(Championship championship, String championshipName,
	        List<AgeGroup> ageGroups) {
		for (AgeGroup ageGroup : ageGroups) {
			String effectiveName = effectiveChampionshipName(ageGroup);
			if (effectiveName == null || !effectiveName.equalsIgnoreCase(championshipName)) {
				continue;
			}
			if (ageGroup.getScoringSystem() != null) {
				championship.setScoringSystem(ageGroup.getScoringSystem());
			} else if (ageGroup.getConfiguredChampionshipType() != ChampionshipType.U) {
				championship.setScoringSystem(ageGroup.getComputedScoringSystem());
			}
			if (ageGroup.getBestAthleteScoringSystem() != null) {
				championship.setBestAthleteScoringSystem(ageGroup.getBestAthleteScoringSystem());
			}
		}
	}

	private static boolean normalizeAgeGroupChampionshipNames(EntityManager em, List<AgeGroup> ageGroups) {
		boolean changed = false;
		for (AgeGroup ageGroup : ageGroups) {
			String name = ageGroup.getChampionshipName();
			if (name != null && !name.isBlank() && !name.trim().equalsIgnoreCase(Championship.COMPETITION_TEMPLATE_NAME)) {
				continue;
			}
			String code = ageGroup.getCode();
			if (code == null || code.isBlank()) {
				continue;
			}
			ageGroup.setChampionshipName(Championship.canonicalizeChampionshipName(code.trim()));
			em.merge(ageGroup);
			changed = true;
		}
		return changed;
	}

	/**
	 * Bulk-migrate any legacy IWF rows (Championship.type, AgeGroup.championshipType,
	 * AgeGroup.ageDivision) to U. Idempotent: a no-op when no IWF rows remain.
	 */
	private static boolean migrateLegacyIwfRows(EntityManager em) {
		int championships = em.createQuery(
		        "update Championship c set c.type = :u where c.type = :iwf")
		        .setParameter("u", ChampionshipType.U)
		        .setParameter("iwf", ChampionshipType.IWF)
		        .executeUpdate();
		int ageGroupTypes = em.createQuery(
		        "update AgeGroup ag set ag.championshipType = :u where ag.championshipType = :iwf")
		        .setParameter("u", ChampionshipType.U)
		        .setParameter("iwf", ChampionshipType.IWF)
		        .executeUpdate();
		int ageDivisions = em.createQuery(
		        "update AgeGroup ag set ag.ageDivision = :u where lower(ag.ageDivision) = :iwf")
		        .setParameter("u", ChampionshipType.U.name())
		        .setParameter("iwf", ChampionshipType.IWF.name().toLowerCase())
		        .executeUpdate();
		if (championships > 0 || ageGroupTypes > 0 || ageDivisions > 0) {
			logger.info("Migrated legacy IWF rows: championships={}, ageGroupTypes={}, ageDivisions={}",
			        championships, ageGroupTypes, ageDivisions);
			return true;
		}
		return false;
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
