package app.owlcms.data.athleteSort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.EligibleForIndividualRankingStatus;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Participation;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;
import app.owlcms.spreadsheet.PAthlete;

/**
 * Writes official championship ranks. Report builders only rank private PAthletes.
 */
public final class BestAthleteRankingService {
	public record Scope(String championship, Gender gender) {}

	public record Inputs(Set<Scope> scopes, List<Long> categories, List<String> lifts, Double bodyWeight,
	        String birthDate, Long groupId, boolean done, EligibleForIndividualRankingStatus eligibility) {}

	private BestAthleteRankingService() {}

	public static Set<Scope> scopes(Athlete athlete) {
		Set<Scope> scopes = new HashSet<>();
		if (athlete != null && athlete.getGender() != null) {
			for (Participation participation : athlete.getParticipations()) {
				Championship championship = participation.getCategory().getAgeGroup().getChampionship();
				if (!championship.isCompetitionTemplate()) {
					scopes.add(new Scope(championship.getName(), athlete.getGender()));
				}
			}
		}
		return scopes;
	}

	public static Inputs inputs(Athlete athlete) {
		if (athlete == null) {
			return null;
		}
		List<String> lifts = new ArrayList<>();
		lifts.add(athlete.getSnatch1ActualLift());
		lifts.add(athlete.getSnatch2ActualLift());
		lifts.add(athlete.getSnatch3ActualLift());
		lifts.add(athlete.getCleanJerk1ActualLift());
		lifts.add(athlete.getCleanJerk2ActualLift());
		lifts.add(athlete.getCleanJerk3ActualLift());
		return new Inputs(scopes(athlete),
		        athlete.getParticipations().stream().map(p -> p.getCategory().getId()).sorted().toList(),
		        lifts, athlete.getBodyWeight(), athlete.getIsoBirthDate(),
		        athlete.getGroup() != null ? athlete.getGroup().getId() : null,
		        athlete.isDone(), athlete.getEffectiveIndividualEligibilityStatus());
	}

	/** Do not let a detached athlete save restore an older cached rank. */
	public static void preserveRanks(Athlete current, Athlete incoming) {
		for (Participation target : incoming.getParticipations()) {
			int rank = 0;
			if (current != null) {
				for (Participation source : current.getParticipations()) {
					if (Objects.equals(source.getCategory().getId(), target.getCategory().getId())) {
						rank = source.getBestAthleteRank();
						break;
					}
				}
			}
			target.setBestAthleteRank(rank);
		}
	}

	public static void recomputeAll() {
		JPAService.runInTransaction(em -> {
			recomputeAll(em);
			return null;
		});
	}

	public static void recomputeAll(EntityManager em) {
		Set<Scope> scopes = new HashSet<>();
		for (Athlete athlete : em.createQuery("select distinct a from Athlete a join fetch a.participations",
		        Athlete.class).getResultList()) {
			scopes.addAll(scopes(athlete));
		}
		recompute(em, scopes);
	}

	public static void recompute(EntityManager em, Set<Scope> scopes) {
		// Presentation flags belong to reporting, not the stored live ranking.
		boolean hideInterim = JXLSWorkbookStreamSource.isNoInterimScoresInResults();
		JXLSWorkbookStreamSource.setNoInterimScoresInResults(false);
		try {
			for (Scope scope : scopes.stream().sorted(Comparator.comparing(Scope::championship)
			        .thenComparing(Scope::gender)).toList()) {
				Championship championship = em.createQuery(
				        "select c from Championship c where c.name = :name", Championship.class)
				        .setParameter("name", scope.championship()).setLockMode(LockModeType.PESSIMISTIC_WRITE)
				        .getSingleResult();
				List<Participation> participations = em.createQuery(
				        "select p from Participation p join fetch p.athlete a join fetch p.category c"
				                + " join fetch c.ageGroup ag where ag.championshipName = :name and a.gender = :gender",
				        Participation.class)
				        .setParameter("name", scope.championship()).setParameter("gender", scope.gender())
				        .getResultList();
				List<PAthlete> candidates = new ArrayList<>();
				for (Participation participation : participations) {
					Athlete athlete = participation.getAthlete();
					participation.setBestAthleteRank(0);
					if (athlete.getGroup() != null && athlete.getBodyWeight() != null && athlete.getBodyWeight() > 0.1) {
						candidates.add(new PAthlete(participation));
					}
				}
				AthleteSorter.assignBestAthleteRanks(candidates, championship.getBestAthleteScoringSystem());
				for (PAthlete candidate : candidates) {
					candidate._getOriginalParticipation().setBestAthleteRank(candidate.getBestAthleteRank());
				}
			}
		} finally {
			JXLSWorkbookStreamSource.setNoInterimScoresInResults(hideInterim);
		}
	}
}
