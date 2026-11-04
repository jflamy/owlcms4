/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup.reassign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.reassign.ReassignmentPlan.AthleteChange;
import app.owlcms.data.agegroup.reassign.ReassignmentPlan.ChampionshipChange;
import app.owlcms.data.agegroup.reassign.ReassignmentPlan.Mode;
import app.owlcms.data.agegroup.reassign.ReassignmentPreview.Change;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.config.FeatureSwitch;

/**
 * Championship reassignment engine. The caller captures athlete snapshots before mutating definitions and passes
 * the final category definitions as targets; {@link #plan} computes kept/removed/added participations from those
 * plain values, and {@link #apply} performs the mutations on the managed entities of the same transaction. The
 * engine never touches the championship caches: mixed-team settings arrive as a scalar map resolved through the
 * caller's EntityManager, because the global getters can open their own transactions or bootstrap missing rows.
 */
public final class ChampionshipReassigner {

	private ChampionshipReassigner() {
	}

	/**
	 * Compute the reassignment without writing anything. The returned plan is local to this transaction.
	 *
	 * @param snapshots pre-change athlete data, by athlete id
	 * @param targetCategoriesByChampionship final fitting categories per championship (case-insensitive keys);
	 *        categories marked for deletion are excluded even while still attached; empty lists are meaningful
	 * @param reasonOnlyCategoriesByChampionship categories of inactive final definitions, used only to explain
	 *        leaves (never as targets); deleted categories appear in neither map
	 * @param mixedTeamsByChampionship effective mixed-team policy per championship, from
	 *        {@link #resolveMixedTeams}
	 */
	public static ReassignmentPlan plan(EntityManager em, Map<Long, AthleteSnapshot> snapshots,
	        Map<String, List<Category>> targetCategoriesByChampionship,
	        Map<String, List<Category>> reasonOnlyCategoriesByChampionship,
	        Map<String, Boolean> mixedTeamsByChampionship, Mode mode) {
		Map<String, List<Category>> targets = caseInsensitive(targetCategoriesByChampionship);
		Map<String, List<Category>> reasonOnly = caseInsensitive(reasonOnlyCategoriesByChampionship);
		ReassignmentPlan plan = new ReassignmentPlan(mode, finalCategoryCounts(targets), mixedTeamsByChampionship);
		FinalCategoryPreference preference = new FinalCategoryPreference(plan.finalCategoryCounts());

		for (Map.Entry<String, List<Category>> entry : targets.entrySet()) {
			String championshipName = entry.getKey();
			List<Category> fittingCandidates = entry.getValue();
			for (AthleteSnapshot snapshot : snapshots.values()) {
				Set<Long> existingIds = participationCategoryIds(snapshot, championshipName);
				if (mode == Mode.WITHIN && existingIds.isEmpty()) {
					// never add an athlete to a championship they were not in
					continue;
				}

				List<Category> fitting = new ArrayList<>();
				for (Category c : fittingCandidates) {
					if (CategoryFit.fit(snapshot, c).fits()) {
						fitting.add(c);
					}
				}
				Set<Long> fittingIds = new LinkedHashSet<>();
				for (Category c : fitting) {
					if (c.getId() != null) {
						fittingIds.add(c.getId());
					}
				}

				ChampionshipChange change = new ChampionshipChange(championshipName);
				for (Long existingId : existingIds) {
					if (fittingIds.contains(existingId)) {
						change.keptCategoryIds().add(existingId);
					} else {
						change.removedCategoryIds().add(existingId);
						change.removedCategoryNames().add(snapshot.participations().get(existingId).categoryName());
					}
				}
				for (Category c : fitting) {
					if (c.getId() == null || !existingIds.contains(c.getId())) {
						change.addedCategories().add(c);
						change.addedCategoryNames().add(c.getDisplayName());
					}
				}
				if (!existingIds.isEmpty() && fitting.isEmpty()) {
					List<Category> reasonCandidates = new ArrayList<>(fittingCandidates);
					reasonCandidates.addAll(reasonOnly.getOrDefault(championshipName, List.of()));
					change.markLeaves(CategoryFit.leaveReason(snapshot, reasonCandidates, preference));
				}
				if (existingIds.isEmpty() && !fitting.isEmpty() && mode == Mode.ALL) {
					change.markJoins();
				}
				if (mode == Mode.ALL || !existingIds.isEmpty()) {
					plan.changeFor(snapshot).byChampionship().put(championshipName, change);
				}
			}
		}
		return plan;
	}

	/**
	 * Perform the computed changes on the managed entities of the plan's transaction. Kept participations keep
	 * their row, team flag and ranks; removed ones leave both collections and are removed; added ones get the
	 * import-default team flags. The registration category is then the most preferred of all the athlete's
	 * participations, in every championship.
	 */
	public static void apply(EntityManager em, ReassignmentPlan plan) {
		FinalCategoryPreference preference = new FinalCategoryPreference(plan.finalCategoryCounts());
		boolean defaultTeamMember = !FeatureSwitch.EXPLICIT_TEAMS.isEffective();
		for (AthleteChange change : plan.changes()) {
			Athlete athlete = em.find(Athlete.class, change.athleteId());
			if (athlete == null) {
				continue;
			}
			for (ChampionshipChange cc : change.byChampionship().values()) {
				for (var it = athlete.getParticipations().iterator(); it.hasNext();) {
					Participation p = it.next();
					Category c = p.getCategory();
					if (c != null && cc.removedCategoryIds().contains(c.getId())) {
						it.remove();
						c.getParticipations().remove(p);
						em.remove(p);
					}
				}
				for (Category c : cc.addedCategories()) {
					athlete.addEligibleCategory(c, defaultTeamMember, false);
				}
				if (!Boolean.TRUE.equals(plan.mixedTeamsByChampionship().get(cc.championshipName()))) {
					for (Participation p : athlete.getParticipations()) {
						Category c = p.getCategory();
						if (c != null && cc.keptCategoryIds().contains(c.getId()) && p.getMixedTeamMember()) {
							p.setMixedTeamMember(false);
						}
					}
				}
			}
			Category best = athlete.getParticipations().stream()
			        .map(Participation::getCategory)
			        .filter(Objects::nonNull)
			        .sorted(preference)
			        .findFirst()
			        .orElse(null);
			// setCategory, not computeCategory: keep the presumed body weight used for the reassignment
			athlete.setCategory(best);
			athlete.computeMainRankings();
		}
	}

	/** The immutable dry-run summary; build it inside the plan's transaction, before any rollback or commit. */
	public static ReassignmentPreview preview(ReassignmentPlan plan) {
		List<ReassignmentPreview.Entry> entries = new ArrayList<>();
		for (AthleteChange change : plan.changes()) {
			for (ChampionshipChange cc : change.byChampionship().values()) {
				Change kind = cc.leaves() ? Change.LEAVES
				        : cc.joins() ? Change.JOINS
				        : !cc.removedCategoryNames().isEmpty() || !cc.addedCategoryNames().isEmpty() ? Change.MOVES
				        : null;
				if (kind != null) {
					entries.add(new ReassignmentPreview.Entry(change.athleteId(), change.athleteName(),
					        cc.championshipName(), kind, cc.leaveReason(), cc.removedCategoryNames(),
					        cc.addedCategoryNames()));
				}
			}
		}
		return new ReassignmentPreview(entries);
	}

	/**
	 * Effective mixed-team policy per championship, resolved with the supplied EntityManager: the stored row if
	 * any, else the competition template, else the built-in default (no mixed teams). The global championship
	 * cache is never consulted.
	 */
	public static Map<String, Boolean> resolveMixedTeams(EntityManager em, Collection<String> championshipNames) {
		Map<String, Boolean> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		if (championshipNames == null || championshipNames.isEmpty()) {
			return result;
		}
		List<Championship> rows = em.createQuery("select c from Championship c", Championship.class).getResultList();
		Map<String, Championship> byName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Championship template = null;
		for (Championship row : rows) {
			if (row.isCompetitionTemplate()) {
				template = row;
			} else if (row.getName() != null) {
				byName.put(row.getName(), row);
			}
		}
		for (String name : championshipNames) {
			if (name == null) {
				continue;
			}
			Championship row = byName.get(name);
			boolean mixed = row != null ? row.isExplicitMixedTeamMembers()
			        : template != null ? template.isExplicitMixedTeamMembers()
			        : false;
			result.put(name, mixed);
		}
		return result;
	}

	private static Map<String, List<Category>> caseInsensitive(Map<String, List<Category>> map) {
		Map<String, List<Category>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		if (map != null) {
			map.forEach((k, v) -> result.put(k, v != null ? v : List.of()));
		}
		return result;
	}

	/** Category ids of the snapshot's participations in one championship. */
	private static Set<Long> participationCategoryIds(AthleteSnapshot snapshot, String championshipName) {
		Set<Long> ids = new LinkedHashSet<>();
		for (AthleteSnapshot.ParticipationInfo info : snapshot.participations().values()) {
			if (info.championshipName() != null && info.championshipName().equalsIgnoreCase(championshipName)) {
				ids.add(info.categoryId());
			}
		}
		return ids;
	}

	/** Final category count per age group id, from the target definitions (deleted categories excluded). */
	private static Map<Long, Integer> finalCategoryCounts(Map<String, List<Category>> targets) {
		Map<Long, Integer> counts = new LinkedHashMap<>();
		for (List<Category> categories : targets.values()) {
			for (Category c : categories) {
				AgeGroup ageGroup = c.getAgeGroup();
				if (ageGroup != null && ageGroup.getId() != null) {
					counts.merge(ageGroup.getId(), 1, Integer::sum);
				}
			}
		}
		return counts;
	}
}
