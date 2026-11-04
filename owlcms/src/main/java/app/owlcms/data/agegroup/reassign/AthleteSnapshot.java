/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup.reassign;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;

/**
 * The athlete data a reassignment decision needs, frozen as plain values inside the transaction, before any
 * definition is mutated. The body weight is the actual body weight or else the presumed weight; the presumed weight
 * can depend on a category that the change deletes, so it must be captured first. Collections are copied so later
 * mutations of the managed entities cannot change what was seen.
 */
public record AthleteSnapshot(Long id, String fullName, Gender gender, Integer age, Double bodyWeight,
        Integer entryTotal, Map<Long, ParticipationInfo> participations, Long registrationCategoryId) {

	/** One participation, keyed by category id; the championship name is the one stored on the category's age group. */
	public record ParticipationInfo(Long categoryId, String championshipName, boolean teamMember,
	        boolean mixedTeamMember, String categoryName) {
	}

	public AthleteSnapshot {
		participations = participations != null ? Map.copyOf(participations) : Map.of();
	}

	public static AthleteSnapshot of(Athlete a) {
		Map<Long, ParticipationInfo> parts = new LinkedHashMap<>();
		for (Participation p : a.getParticipations()) {
			Category c = p.getCategory();
			if (c == null || c.getId() == null) {
				continue;
			}
			AgeGroup ageGroup = c.getAgeGroup();
			parts.put(c.getId(), new ParticipationInfo(c.getId(),
			        ageGroup != null ? ageGroup.getChampionshipName() : null,
			        p.getTeamMember(), p.getMixedTeamMember(), c.getDisplayName()));
		}
		Double weight = a.getBodyWeight() != null ? a.getBodyWeight() : a.getPresumedBodyWeight();
		Category registration = a.getCategory();
		return new AthleteSnapshot(a.getId(), a.getFullName(), a.getGender(), a.getAge(), weight,
		        a.getQualifyingTotal(), parts, registration != null ? registration.getId() : null);
	}

	/** Snapshots of every athlete with a participation in any of the given championships. */
	public static Map<Long, AthleteSnapshot> forChampionships(EntityManager em, Collection<String> championshipNames) {
		if (championshipNames == null || championshipNames.isEmpty()) {
			return Map.of();
		}
		List<String> names = championshipNames.stream().filter(n -> n != null && !n.isBlank())
		        .map(n -> n.trim().toLowerCase()).collect(Collectors.toList());
		if (names.isEmpty()) {
			return Map.of();
		}
		List<Athlete> athletes = em.createQuery(
		        "select distinct a from Athlete a join a.participations p join p.category c join c.ageGroup ag"
		                + " where lower(ag.championshipName) in :names",
		        Athlete.class)
		        .setParameter("names", names)
		        .getResultList();
		return ofAll(athletes);
	}

	/** Snapshots of every athlete, for "Recompute all eligibilities". */
	public static Map<Long, AthleteSnapshot> all(EntityManager em) {
		return ofAll(em.createQuery("select a from Athlete a", Athlete.class).getResultList());
	}

	private static Map<Long, AthleteSnapshot> ofAll(List<Athlete> athletes) {
		Map<Long, AthleteSnapshot> result = new LinkedHashMap<>();
		for (Athlete a : athletes) {
			result.put(a.getId(), AthleteSnapshot.of(a));
		}
		return result;
	}
}
