/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;

import app.owlcms.audit.AgeGroupAudit;
import app.owlcms.audit.AthleteAudit;
import app.owlcms.audit.AthleteDiff;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.agegroup.reassign.AthleteSnapshot;
import app.owlcms.data.agegroup.reassign.ChampionshipReassigner;
import app.owlcms.data.agegroup.reassign.ReassignmentPlan;
import app.owlcms.data.agegroup.reassign.ReassignmentPlan.Mode;
import app.owlcms.data.agegroup.reassign.ReassignmentPreview;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.BestAthleteRankingService;
import app.owlcms.data.athleteSort.RankingConfig;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JpaTransactions;
import app.owlcms.data.jpa.SingletonSaves;
import app.owlcms.data.jpa.StaleSingletonException;
import app.owlcms.data.jpa.StaleSingletonException.Kind;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;

/**
 * The age-group definition save service (compliance item 200). The editor works on a detached scratchpad; every write
 * goes through {@link AgeGroupRepository#write} (or one outer transaction of the same shape for creates and multi-row
 * moves), so definition changes and reassignment commit together or not at all, under the shared save lock, with the
 * stale-stamp and session checks repeated inside. Participant counts and rankings are refreshed only after commit; a
 * refresh failure is reported on the outcome and never replays the save. No Vaadin dependencies, so the acceptance
 * tests drive this class directly.
 */
public class AgeGroupDefinitionService {

	/** The user's confirmation decision for a reassigning change. */
	public enum Decision {
		APPLY,
		LATER
	}

	/** What a committed action did, plus any post-commit refresh failures (never a rolled-back save). */
	public record SaveOutcome(AgeGroupChange.Kind change, boolean championshipCreated,
	        List<Throwable> refreshFailures) {
	}

	/** TEST ONLY: invoked with the step number after each save step, to inject failures for atomicity tests. */
	public static volatile Consumer<Integer> afterStepHookForTesting;
	/** TEST ONLY: invoked with the refresh step name before each post-commit refresh step. */
	public static volatile Consumer<String> beforeRefreshStepHookForTesting;

	// ---------------------------------------------------------------------------------------------
	// editor save and preview
	// ---------------------------------------------------------------------------------------------

	/**
	 * Save the scratchpad onto the stored age group. Preflight stale/session checks run before the confirmation
	 * flow; {@code write} repeats them authoritatively inside the transaction. The classification is enforced here,
	 * not just in the UI: NONE only stamps, DIRECT never reassigns, CHAMPIONSHIP_ONLY never reassigns,
	 * CHAMPIONSHIP_MIXED and LATER with removed categories are refused without writing.
	 *
	 * @return the outcome of the committed save; a transaction failure propagates instead
	 */
	public SaveOutcome save(Long draftStamp, AuditOrigin origin, AgeGroup scratch, Decision decision) {
		Long id = scratch.getId();
		AgeGroupRepository.assertCurrent(id, draftStamp);
		preflight(id, draftStamp, scratch);
		Object[] holder = new Object[2];
		List<Map<String, Object>> snapshots = new ArrayList<>();
		List<AthleteAudit.Changed> reassigned = new ArrayList<>();
		AgeGroupRepository.write(id, draftStamp, origin, (em, stored) -> {
			snapshots.clear();
			reassigned.clear();
			snapshots.add(AgeGroupAudit.snapshot(stored));
			AgeGroupChange.Kind kind = AgeGroupChange.classify(stored, scratch);
			if (kind == AgeGroupChange.Kind.CHAMPIONSHIP_MIXED) {
				throw new IllegalArgumentException(
				        "the championship must be changed and saved separately from any other change");
			}
			assertNoSessionConflict(em, affectedChampionships(stored, scratch, kind));
			boolean championshipCreated = false;
			switch (kind) {
				case NONE -> {
					// accepted save: no field or assignment changes, the stamp still advances (spec 3.8)
				}
				case DIRECT -> {
					copyFields(stored, scratch, false);
					championshipCreated = materializeIfMissing(em, stored);
					recomputeCategoryMetadata(stored, Set.of());
				}
				case CHAMPIONSHIP_ONLY -> {
					stored.setChampionshipName(scratch.getChampionshipName());
					championshipCreated = materializeIfMissing(em, stored);
				}
				case REASSIGN -> {
					championshipCreated = applyReassigningSave(em, stored, scratch, decision, reassigned);
				}
				case CHAMPIONSHIP_MIXED -> throw new IllegalStateException("handled above");
			}
			// new categories get the id their code is made from
			em.flush();
			snapshots.add(AgeGroupAudit.snapshot(stored));
			holder[0] = kind;
			holder[1] = championshipCreated;
			return null;
		});
		AgeGroupChange.Kind kind = (AgeGroupChange.Kind) holder[0];
		AgeGroupAudit.changed(origin, snapshots.get(0), snapshots.get(1),
		        kind == AgeGroupChange.Kind.REASSIGN ? decision.name() : null, reassigned);
		return withPostCommitRefresh(
		        new SaveOutcome(kind, Boolean.TRUE.equals(holder[1]), List.of()));
	}

	/**
	 * The dry run for a reassigning change. Same computation as the save, in a transaction that is always rolled
	 * back: nothing is written, nothing is stamped, no championship row is created. The summary is immutable; Apply
	 * recomputes from fresh data.
	 */
	public ReassignmentPreview preview(AgeGroup scratch, Long draftStamp) {
		Long id = scratch.getId();
		AgeGroupRepository.assertCurrent(id, draftStamp);
		return inRollbackPreview(em -> {
			AgeGroup stored = SingletonSaves.findFresh(em, AgeGroup.class, id);
			if (stored == null || !Objects.equals(stored.getLastSavedAt(), draftStamp)) {
				throw new StaleSingletonException(Kind.AGE_GROUP, stored != null ? stored.getLastSavedBy() : null);
			}
			AgeGroupChange.Kind kind = AgeGroupChange.classify(stored, scratch);
			assertNoSessionConflict(em, affectedChampionships(stored, scratch, kind));
			if (kind != AgeGroupChange.Kind.REASSIGN) {
				return new ReassignmentPreview(List.of());
			}
			Map<Long, AthleteSnapshot> snapshots = AthleteSnapshot.forChampionships(em,
			        List.of(stored.getChampionshipName()));
			copyFields(stored, scratch, true);
			ChampionshipRepository.materializeForAgeGroupIn(em, stored);
			Set<Long> marked = syncCategories(stored, scratch);
			recomputeCategoryMetadata(stored, marked);
			return ChampionshipReassigner.preview(planCurrentChampionship(em, stored, snapshots, marked));
		});
	}

	// ---------------------------------------------------------------------------------------------
	// other writers
	// ---------------------------------------------------------------------------------------------

	/**
	 * ADD path: a new age group and its categories, the championship row if new, one transaction. The scratchpad is
	 * never merged; allowed values are copied onto a fresh entity. Creation changes no athlete assignment, so no
	 * session conflict is possible and none is checked. Cancel never calls this.
	 */
	public SaveOutcome create(AgeGroup scratch, AuditOrigin origin) {
		AgeGroupChange.validateNoDuplicateUpperLimits(scratch);
		boolean[] created = new boolean[1];
		List<Map<String, Object>> added = new ArrayList<>();
		synchronized (SingletonSaves.LOCK) {
			JpaTransactions.inTransaction(em -> {
				added.clear();
				if (AgeGroupRepository.hasDuplicateCodeGender(scratch, em)) {
					throw new IllegalArgumentException(
					        "duplicate age group code+gender: " + scratch.getCode() + " " + scratch.getGender());
				}
				AgeGroup fresh = new AgeGroup();
				copyFields(fresh, scratch, true);
				fresh.setChampionshipName(scratch.getChampionshipName());
				for (Category sc : scratch.getCategories()) {
					fresh.addCategory(new Category(0.0, sc.getMaximumWeight(), scratch.getGender(),
					        scratch.isActive(), 0, 0, 0, fresh, sc.getQualifyingTotal()));
				}
				recomputeCategoryMetadata(fresh, Set.of());
				created[0] = materializeIfMissing(em, fresh);
				fresh.stamp(origin, null);
				em.persist(fresh);
				em.flush();
				added.add(AgeGroupAudit.snapshot(fresh));
				return null;
			});
		}
		AgeGroupAudit.added(origin, added.get(0));
		return withPostCommitRefresh(new SaveOutcome(null, created[0], List.of()));
	}

	/**
	 * The dry run for deleting an age group: its championship's athletes are reassigned as if its categories no
	 * longer existed. Rolled back; nothing is written.
	 */
	public ReassignmentPreview previewDelete(Long ageGroupId, Long draftStamp) {
		AgeGroupRepository.assertCurrent(ageGroupId, draftStamp);
		return inRollbackPreview(em -> {
			AgeGroup stored = SingletonSaves.findFresh(em, AgeGroup.class, ageGroupId);
			if (stored == null || !Objects.equals(stored.getLastSavedAt(), draftStamp)) {
				throw new StaleSingletonException(Kind.AGE_GROUP, stored != null ? stored.getLastSavedBy() : null);
			}
			assertNoSessionConflict(em, List.of(stored.getChampionshipName()));
			Map<Long, AthleteSnapshot> snapshots = AthleteSnapshot.forChampionships(em,
			        List.of(stored.getChampionshipName()));
			return ChampionshipReassigner.preview(planCurrentChampionship(em, stored, snapshots, categoryIds(stored)));
		});
	}

	private static Set<Long> categoryIds(AgeGroup ageGroup) {
		Set<Long> ids = new LinkedHashSet<>();
		for (Category c : ageGroup.getCategories()) {
			ids.add(c.getId());
		}
		return ids;
	}

	/** Delete an age group: athletes reassigned as in the dry run, then the group and its categories removed, one transaction. */
	public SaveOutcome delete(Long draftStamp, AuditOrigin origin, Long ageGroupId) {
		AgeGroupRepository.assertCurrent(ageGroupId, draftStamp);
		JpaTransactions.inTransaction(em -> {
			AgeGroup stored = SingletonSaves.findFresh(em, AgeGroup.class, ageGroupId);
			if (stored != null) {
				assertNoSessionConflict(em, List.of(stored.getChampionshipName()));
			}
			return null;
		});
		List<Map<String, Object>> deleted = new ArrayList<>();
		List<AthleteAudit.Changed> reassigned = new ArrayList<>();
		AgeGroupRepository.write(ageGroupId, draftStamp, origin, (em, stored) -> {
			deleted.clear();
			reassigned.clear();
			assertNoSessionConflict(em, List.of(stored.getChampionshipName()));
			deleted.add(AgeGroupAudit.snapshot(stored));
			Map<Long, AthleteSnapshot> snapshots = AthleteSnapshot.forChampionships(em,
			        List.of(stored.getChampionshipName()));
			Map<Long, AthleteDiff.Snapshot> athletesBefore = athletesBefore(em, snapshots.keySet());
			ChampionshipReassigner.apply(em, planCurrentChampionship(em, stored, snapshots, categoryIds(stored)));
			reassigned.addAll(athletesChanged(em, athletesBefore));
			AgeGroupRepository.delete(em, stored);
			return null;
		});
		AgeGroupAudit.deleted(origin, deleted.get(0), reassigned);
		return withPostCommitRefresh(new SaveOutcome(null, false, List.of()));
	}

	/**
	 * Move age groups to another championship (the championship page's selection dialog). One transaction, every row
	 * freshly checked against the stamp captured when the dialog opened; participations and assignments untouched.
	 */
	public SaveOutcome moveToChampionship(AuditOrigin origin, Map<Long, Long> stampsById, String championshipName) {
		boolean[] created = new boolean[1];
		List<Map<String, Object>> before = new ArrayList<>();
		List<Map<String, Object>> after = new ArrayList<>();
		synchronized (SingletonSaves.LOCK) {
			JpaTransactions.inTransaction(em -> {
				before.clear();
				after.clear();
				List<AgeGroup> rows = new ArrayList<>();
				Set<String> affected = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
				for (Map.Entry<Long, Long> entry : stampsById.entrySet()) {
					AgeGroup row = SingletonSaves.findFresh(em, AgeGroup.class, entry.getKey());
					if (row == null || !Objects.equals(row.getLastSavedAt(), entry.getValue())) {
						throw new StaleSingletonException(Kind.AGE_GROUP,
						        row != null ? row.getLastSavedBy() : null);
					}
					rows.add(row);
					affected.add(row.getChampionshipName());
				}
				affected.add(championshipName);
				assertNoSessionConflict(em, affected);
				for (AgeGroup row : rows) {
					before.add(AgeGroupAudit.snapshot(row));
					row.setChampionshipName(championshipName);
					after.add(AgeGroupAudit.snapshot(row));
				}
				if (!rows.isEmpty()) {
					created[0] = materializeIfMissing(em, rows.get(0));
				}
				for (AgeGroup row : rows) {
					row.stamp(origin, row.getLastSavedAt());
				}
				em.flush();
				return null;
			});
		}
		AgeGroupAudit.moved(origin, before, after);
		return withPostCommitRefresh(new SaveOutcome(null, created[0], List.of()));
	}

	/** "Reassign within championships": the section 4 rule over every championship, adding nobody. */
	public SaveOutcome reassignWithin(AuditOrigin origin) {
		return globalReassign(origin, Mode.WITHIN);
	}

	/** "Recompute all eligibilities": every athlete placed in every category they fit in all active age groups. */
	public SaveOutcome recomputeAllEligibilities(AuditOrigin origin) {
		return globalReassign(origin, Mode.ALL);
	}

	/** Dry run for "Reassign within championships". */
	public ReassignmentPreview previewReassignWithin() {
		return globalPreview(Mode.WITHIN);
	}

	/** Dry run for "Recompute all eligibilities". */
	public ReassignmentPreview previewRecomputeAll() {
		return globalPreview(Mode.ALL);
	}

	// ---------------------------------------------------------------------------------------------
	// session guard (spec 3.7, decision 9): athletes of a session in progress on any platform
	// ---------------------------------------------------------------------------------------------

	/** Read-only preflight for the UI: refuse if the change can touch an athlete of a session in progress. */
	public static void assertNoSessionConflict(Collection<String> championshipNames) {
		JpaTransactions.inTransaction(em -> {
			assertNoSessionConflict(em, championshipNames);
			return null;
		});
	}

	static void assertNoSessionConflict(EntityManager em, Collection<String> championshipNames) {
		List<FieldOfPlay> inProgress = OwlcmsFactory.getFOPsWithSessionInProgress();
		if (inProgress.isEmpty() || championshipNames == null || championshipNames.isEmpty()) {
			return;
		}
		List<String> names = championshipNames.stream().filter(Objects::nonNull)
		        .map(n -> n.trim().toLowerCase()).toList();
		if (names.isEmpty()) {
			return;
		}
		List<SessionsInProgressException.SessionPlatform> conflicts = new ArrayList<>();
		for (FieldOfPlay fop : inProgress) {
			Group group = fop.getGroup();
			if (group == null || group.getId() == null) {
				continue;
			}
			Long count = em.createQuery(
			        "select count(distinct a) from Athlete a join a.participations p join p.category c"
			                + " join c.ageGroup ag where a.group.id = :groupId"
			                + " and lower(ag.championshipName) in :names",
			        Long.class)
			        .setParameter("groupId", group.getId())
			        .setParameter("names", names)
			        .getSingleResult();
			if (count != null && count > 0) {
				conflicts.add(new SessionsInProgressException.SessionPlatform(group.getName(), fop.getName()));
			}
		}
		if (!conflicts.isEmpty()) {
			throw new SessionsInProgressException(conflicts);
		}
	}

	/** "Recompute all eligibilities" can add anyone to a championship, so any athlete of a started session blocks it. */
	private static void assertNoAthleteInStartedSession(EntityManager em) {
		List<FieldOfPlay> inProgress = OwlcmsFactory.getFOPsWithSessionInProgress();
		List<SessionsInProgressException.SessionPlatform> conflicts = new ArrayList<>();
		for (FieldOfPlay fop : inProgress) {
			Group group = fop.getGroup();
			if (group == null || group.getId() == null) {
				continue;
			}
			Long count = em.createQuery("select count(a) from Athlete a where a.group.id = :groupId", Long.class)
			        .setParameter("groupId", group.getId())
			        .getSingleResult();
			if (count != null && count > 0) {
				conflicts.add(new SessionsInProgressException.SessionPlatform(group.getName(), fop.getName()));
			}
		}
		if (!conflicts.isEmpty()) {
			throw new SessionsInProgressException(conflicts);
		}
	}

	// ---------------------------------------------------------------------------------------------
	// internals
	// ---------------------------------------------------------------------------------------------

	/** Read-only preflight: classify against a fresh row and check the session guard before the dialog flow. */
	private void preflight(Long id, Long draftStamp, AgeGroup scratch) {
		JpaTransactions.inTransaction(em -> {
			AgeGroup stored = SingletonSaves.findFresh(em, AgeGroup.class, id);
			AgeGroupChange.Kind kind = AgeGroupChange.classify(stored, scratch);
			assertNoSessionConflict(em, affectedChampionships(stored, scratch, kind));
			return null;
		});
	}

	private static Set<String> affectedChampionships(AgeGroup stored, AgeGroup scratch, AgeGroupChange.Kind kind) {
		Set<String> affected = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		if (stored.getChampionshipName() != null) {
			affected.add(stored.getChampionshipName());
		}
		if ((kind == AgeGroupChange.Kind.CHAMPIONSHIP_ONLY || kind == AgeGroupChange.Kind.CHAMPIONSHIP_MIXED)
		        && scratch.getChampionshipName() != null) {
			affected.add(scratch.getChampionshipName());
		}
		return affected;
	}

	/** Steps 2 to 7 of a reassigning save, inside the write transaction. */
	private boolean applyReassigningSave(EntityManager em, AgeGroup stored, AgeGroup scratch, Decision decision,
	        List<AthleteAudit.Changed> reassigned) {
		// step 2: snapshot the championship's athletes before any definition change
		Map<Long, AthleteSnapshot> snapshots = AthleteSnapshot.forChampionships(em,
		        List.of(stored.getChampionshipName()));
		Map<Long, AthleteDiff.Snapshot> athletesBefore = decision == Decision.APPLY
		        ? athletesBefore(em, snapshots.keySet())
		        : Map.of();
		afterStep(2);
		// step 3: fields; the championship row if missing (a legacy age group may never have been materialized)
		copyFields(stored, scratch, true);
		boolean championshipCreated = materializeIfMissing(em, stored);
		afterStep(3);
		// step 4: match categories by upper limit
		Set<Long> markedForDeletion = syncCategories(stored, scratch);
		if (decision == Decision.LATER && !markedForDeletion.isEmpty()) {
			throw new IllegalArgumentException("Later is not offered when categories are removed");
		}
		afterStep(4);
		// step 5: lower limits, genders and codes of the final list
		recomputeCategoryMetadata(stored, markedForDeletion);
		afterStep(5);
		// step 6: Apply reassigns from the snapshot; Later keeps participations as they are
		if (decision == Decision.APPLY) {
			ChampionshipReassigner.apply(em, planCurrentChampionship(em, stored, snapshots, markedForDeletion));
		}
		afterStep(6);
		// step 7: remove the marked categories; nothing may reference them at this point
		removeMarkedCategories(em, stored, markedForDeletion);
		reassigned.addAll(athletesChanged(em, athletesBefore));
		afterStep(7);
		return championshipCreated;
	}

	/** The WITHIN plan for the edited age group's championship, against the final definitions. */
	private static ReassignmentPlan planCurrentChampionship(EntityManager em, AgeGroup stored,
	        Map<Long, AthleteSnapshot> snapshots, Set<Long> markedForDeletion) {
		String championshipName = stored.getChampionshipName();
		List<Category> targets = em.createQuery(
		        "select c from Category c join c.ageGroup ag"
		                + " where lower(ag.championshipName) = lower(:name) and ag.active = true",
		        Category.class)
		        .setParameter("name", championshipName)
		        .getResultList().stream()
		        .filter(c -> !markedForDeletion.contains(c.getId()))
		        .toList();
		List<Category> reasonOnly = em.createQuery(
		        "select c from Category c join c.ageGroup ag"
		                + " where lower(ag.championshipName) = lower(:name) and ag.active = false",
		        Category.class)
		        .setParameter("name", championshipName)
		        .getResultList().stream()
		        .filter(c -> !markedForDeletion.contains(c.getId()))
		        .toList();
		return ChampionshipReassigner.plan(em, snapshots, Map.of(championshipName, targets),
		        Map.of(championshipName, reasonOnly),
		        ChampionshipReassigner.resolveMixedTeams(em, List.of(championshipName)), Mode.WITHIN);
	}

	/** Copy the editable fields; never the scoring model (a championship setting), never the stamp. */
	private static void copyFields(AgeGroup stored, AgeGroup scratch, boolean includeStructure) {
		stored.setCode(scratch.getCode());
		stored.setMedals(scratch.getMedals());
		stored.setAlreadyGendered(scratch.isAlreadyGendered());
		if (includeStructure) {
			stored.setGender(scratch.getGender());
			stored.setMinAge(scratch.getMinAge());
			stored.setMaxAge(scratch.getMaxAge());
			stored.setActive(scratch.isActive());
		}
	}

	/**
	 * Match the scratchpad's categories to the stored ones by upper limit: kept ones get the new QT, new upper
	 * limits create categories, stored categories with no counterpart are marked for deletion (removed in step 7).
	 *
	 * @return ids of the stored categories marked for deletion
	 */
	private static Set<Long> syncCategories(AgeGroup stored, AgeGroup scratch) {
		Map<Double, Category> storedByMax = new LinkedHashMap<>();
		for (Category c : stored.getCategories()) {
			storedByMax.put(c.getMaximumWeight(), c);
		}
		for (Category sc : scratch.getCategories()) {
			Category existing = storedByMax.get(sc.getMaximumWeight());
			if (existing != null) {
				existing.setQualifyingTotal(sc.getQualifyingTotal());
			} else {
				stored.addCategory(new Category(0.0, sc.getMaximumWeight(), stored.getGender(), stored.isActive(),
				        0, 0, 0, stored, sc.getQualifyingTotal()));
			}
		}
		Set<Long> marked = new LinkedHashSet<>();
		for (Category c : stored.getCategories()) {
			if (!scratch.getCategories().stream()
			        .anyMatch(sc -> Objects.equals(sc.getMaximumWeight(), c.getMaximumWeight()))) {
				marked.add(c.getId());
			}
		}
		return marked;
	}

	/** Lower limits from the sorted final upper limits, then gender and codes. Marked categories stay attached. */
	private static void recomputeCategoryMetadata(AgeGroup stored, Set<Long> markedForDeletion) {
		List<Category> finalCategories = stored.getCategories().stream()
		        .filter(c -> !markedForDeletion.contains(c.getId()))
		        .sorted(Comparator.comparing(Category::getMaximumWeight))
		        .toList();
		double lower = 0.0;
		for (Category c : finalCategories) {
			c.setMinimumWeight(lower);
			lower = c.getMaximumWeight();
			c.setGender(stored.getGender());
			c.setCode(c.getComputedCode());
		}
	}

	private static void removeMarkedCategories(EntityManager em, AgeGroup stored, Set<Long> markedForDeletion) {
		for (Long id : markedForDeletion) {
			Category marked = em.find(Category.class, id);
			if (marked == null) {
				continue;
			}
			Long participations = em.createQuery(
			        "select count(p) from Participation p where p.category = :category", Long.class)
			        .setParameter("category", marked).getSingleResult();
			Long registrations = em.createQuery(
			        "select count(a) from Athlete a where a.category = :category", Long.class)
			        .setParameter("category", marked).getSingleResult();
			if (participations > 0 || registrations > 0) {
				throw new IllegalStateException("category " + id + " is still referenced before removal");
			}
			stored.removeCategory(marked);
		}
	}

	/** @return true when the materialization created the championship row. */
	private static boolean materializeIfMissing(EntityManager em, AgeGroup ageGroup) {
		String name = Championship.canonicalizeChampionshipName(ageGroup.getChampionshipName());
		if (name == null || name.isBlank() || name.equalsIgnoreCase(Championship.COMPETITION_TEMPLATE_NAME)) {
			return false;
		}
		Long existing = em.createQuery(
		        "select count(c) from Championship c where lower(trim(c.name)) = :name", Long.class)
		        .setParameter("name", name.trim().toLowerCase()).getSingleResult();
		boolean missing = existing == null || existing == 0;
		ChampionshipRepository.materializeForAgeGroupIn(em, ageGroup);
		return missing;
	}

	/** Athlete values before any definition change, so the audit shows the categories as they were. */
	static Map<Long, AthleteDiff.Snapshot> athletesBefore(EntityManager em, Collection<Long> athleteIds) {
		Map<Long, AthleteDiff.Snapshot> before = new LinkedHashMap<>();
		for (Long id : athleteIds) {
			Athlete athlete = em.find(Athlete.class, id);
			if (athlete != null) {
				before.put(id, AthleteDiff.snapshot(athlete));
			}
		}
		return before;
	}

	static List<AthleteAudit.Changed> athletesChanged(EntityManager em,
	        Map<Long, AthleteDiff.Snapshot> before) {
		if (before.isEmpty()) {
			return List.of();
		}
		// new categories get the id their code is made from
		em.flush();
		List<AthleteAudit.Changed> changed = new ArrayList<>();
		for (Map.Entry<Long, AthleteDiff.Snapshot> entry : before.entrySet()) {
			Athlete athlete = em.find(Athlete.class, entry.getKey());
			if (athlete == null) {
				continue;
			}
			List<AthleteDiff.Change> diffs = AthleteDiff.diff(entry.getValue(), AthleteDiff.snapshot(athlete));
			if (!diffs.isEmpty()) {
				changed.add(new AthleteAudit.Changed(athlete, AthleteAudit.platformOf(athlete), diffs));
			}
		}
		return changed;
	}

	private SaveOutcome globalReassign(AuditOrigin origin, Mode mode) {
		List<AthleteAudit.Changed> reassigned = new ArrayList<>();
		synchronized (SingletonSaves.LOCK) {
			JpaTransactions.inTransaction(em -> {
				reassigned.clear();
				if (mode == Mode.ALL) {
					assertNoAthleteInStartedSession(em);
				} else {
					assertNoSessionConflict(em, allChampionshipNames(em));
				}
				ReassignmentPlan plan = globalPlan(em, mode);
				Map<Long, AthleteDiff.Snapshot> athletesBefore = athletesBefore(em,
				        plan.changes().stream().map(c -> c.athleteId()).toList());
				ChampionshipReassigner.apply(em, plan);
				reassigned.addAll(athletesChanged(em, athletesBefore));
				return null;
			});
		}
		AgeGroupAudit.reassigned(origin, mode == Mode.ALL ? "agegroup.recompute-all" : "agegroup.reassign-within",
		        reassigned);
		return withPostCommitRefresh(new SaveOutcome(null, false, List.of()));
	}

	private ReassignmentPreview globalPreview(Mode mode) {
		return inRollbackPreview(em -> {
			if (mode == Mode.ALL) {
				assertNoAthleteInStartedSession(em);
			} else {
				assertNoSessionConflict(em, allChampionshipNames(em));
			}
			return ChampionshipReassigner.preview(globalPlan(em, mode));
		});
	}

	private static ReassignmentPlan globalPlan(EntityManager em, Mode mode) {
		Map<String, List<Category>> targets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Map<String, List<Category>> reasonOnly = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (AgeGroup ageGroup : em.createQuery("select ag from AgeGroup ag", AgeGroup.class).getResultList()) {
			String name = ageGroup.getChampionshipName();
			if (name == null || name.isBlank()) {
				continue;
			}
			if (ageGroup.isActive()) {
				targets.computeIfAbsent(name, k -> new ArrayList<>()).addAll(ageGroup.getCategories());
			} else {
				reasonOnly.computeIfAbsent(name, k -> new ArrayList<>()).addAll(ageGroup.getCategories());
			}
		}
		return ChampionshipReassigner.plan(em, AthleteSnapshot.all(em), targets, reasonOnly,
		        ChampionshipReassigner.resolveMixedTeams(em, targets.keySet()), mode);
	}

	private static Set<String> allChampionshipNames(EntityManager em) {
		Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (AgeGroup ageGroup : em.createQuery("select ag from AgeGroup ag", AgeGroup.class).getResultList()) {
			if (ageGroup.getChampionshipName() != null && !ageGroup.getChampionshipName().isBlank()) {
				names.add(ageGroup.getChampionshipName());
			}
		}
		return names;
	}

	/** A read-compute-discard transaction for dry runs: never commits, so nothing is written or bootstrapped. */
	private static <T> T inRollbackPreview(Function<EntityManager, T> work) {
		return JpaTransactions.inRollbackOnlyTransaction(work);
	}

	private static void afterStep(int step) {
		Consumer<Integer> hook = afterStepHookForTesting;
		if (hook != null) {
			hook.accept(step);
		}
	}

	/**
	 * Spec section 11 step 8, after commit: rankings invalid, championship cache, participant counts, best-athlete
	 * ranks, ranking configuration. Every step runs even if an earlier one failed; failures are collected, never
	 * replayed as a save.
	 */
	private static SaveOutcome withPostCommitRefresh(SaveOutcome base) {
		List<Throwable> failures = new ArrayList<>();
		runRefresh(failures, "markInvalid", () -> {
			Competition current = Competition.getCurrent();
			if (current != null) {
				current.setRankingsInvalid(true);
			}
		});
		if (base.championshipCreated()) {
			runRefresh(failures, "championshipCache", Championship::reset);
		}
		runRefresh(failures, "participantCounts", Championship::recomputeParticipantCounts);
		runRefresh(failures, "bestAthleteRanks",
		        () -> JpaTransactions.inTransaction(em -> {
			        BestAthleteRankingService.recomputeAll(em);
			        return null;
		        }));
		runRefresh(failures, "rankingConfig", RankingConfig::updateMustCompute);
		return new SaveOutcome(base.change(), base.championshipCreated(), List.copyOf(failures));
	}

	private static void runRefresh(List<Throwable> failures, String step, Runnable work) {
		try {
			Consumer<String> hook = beforeRefreshStepHookForTesting;
			if (hook != null) {
				hook.accept(step);
			}
			work.run();
		} catch (Throwable t) {
			failures.add(t);
		}
	}
}
