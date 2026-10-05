package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.agegroup.MedalPolicy;
import app.owlcms.data.agegroup.TeamPointsPolicy;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.athleteSort.ScoreboardRankData;
import app.owlcms.data.category.Participation;
import app.owlcms.data.category.Category;
import app.owlcms.data.config.Config;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.export.v2.ParticipationDTO;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.group.Group;
import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.PAthlete;

public class ScoreboardRankDataTest {
	@BeforeClass
	public static void setup() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
		TestData.insertInitialData(2, true);
	}

	@AfterClass
	public static void teardown() {
		JPAService.close();
	}

	private Championship championship(String name, Ranking medalSystem) {
		Championship championship = new Championship(name, ChampionshipType.DEFAULT);
		championship.setUseCompetitionDefaults(false);
		championship.setMedalPolicy(MedalPolicy.TOTAL_ONLY);
		championship.setTeamPointsPolicy(TeamPointsPolicy.TOTAL_ONLY);
		championship.setScoringSystem(medalSystem);
		championship.setBestAthleteScoringSystem(Ranking.GAMX);
		return championship;
	}

	@Test
	public void columnVisibilityCoversAllFourMedalScenarios() {
		Championship totals = championship("Total medals", Ranking.TOTAL);
		Championship masters = championship("QMasters medals", Ranking.QAGE);
		Championship youth = championship("Youth medals", Ranking.GAMX_U);
		var mastersOnly = ScoreboardRankData.columns(List.of(masters), false, false, false);
		assertTrue(mastersOnly.medalScore());
		assertFalse(mastersOnly.legacyTotalRank());
		assertEquals(Translator.translate("Ranking.QAGE", Locale.ENGLISH),
		        ScoreboardRankData.title(mastersOnly.medalSystems(), Locale.ENGLISH));

		var totalsOnly = ScoreboardRankData.columns(List.of(totals), false, false, false);
		assertFalse(totalsOnly.medalScore());
		assertTrue(totalsOnly.legacyTotalRank());

		var mixedScores = ScoreboardRankData.columns(List.of(masters, youth), false, false, false);
		assertTrue(mixedScores.medalScore());
		assertFalse(mixedScores.legacyTotalRank());
		assertEquals(Translator.translate("Score", Locale.ENGLISH),
		        ScoreboardRankData.title(mixedScores.medalSystems(), Locale.ENGLISH));

		var mixedWeightsAndScores = ScoreboardRankData.columns(List.of(totals, masters), false, false, false);
		assertTrue(mixedWeightsAndScores.medalScore());
		assertTrue(mixedWeightsAndScores.legacyTotalRank());
		assertEquals(mastersOnly.medalSystems(), mixedWeightsAndScores.medalSystems());
		for (var columns : List.of(mastersOnly, totalsOnly, mixedScores, mixedWeightsAndScores)) {
			// every championship above is Total-only: no lift medals, hence no Snatch/C&J rank columns
			assertFalse(columns.liftRanks());
			assertFalse(columns.bestScore());
			assertFalse(columns.bestRank());
		}
	}

	@Test
	public void liftRankColumnsFollowTheMedalPolicyNotTheMedalSystem() {
		Championship totals = championship("Total medals", Ranking.TOTAL);
		Championship masters = championship("QMasters medals", Ranking.QAGE);
		assertFalse(ScoreboardRankData.columns(List.of(totals), false, false, false).liftRanks());

		totals.setMedalPolicy(MedalPolicy.ALL_THREE);
		assertTrue(ScoreboardRankData.columns(List.of(totals), false, false, false).liftRanks());
		// one displayed championship awarding lift medals is enough
		assertTrue(ScoreboardRankData.columns(List.of(masters, totals), false, false, false).liftRanks());

		totals.setMedalPolicy(MedalPolicy.LIFTS_ONLY);
		assertTrue(ScoreboardRankData.columns(List.of(totals), false, false, false).liftRanks());

		// a score medal system forces a Total-only policy, so lift ranks can never appear for it
		masters.setMedalPolicy(MedalPolicy.ALL_THREE);
		assertEquals(Ranking.TOTAL, masters.getScoringSystem());
		assertTrue(ScoreboardRankData.columns(List.of(masters), false, false, false).liftRanks());
		assertFalse(ScoreboardRankData.columns(List.of(masters), false, false, false).medalScore());
	}

	@Test
	public void bestAthleteRankOverrideDoesNotHideMedalColumnsOrRequireBestScore() {
		Championship masters = championship("Masters", Ranking.QAGE);
		var rankOnly = ScoreboardRankData.columns(List.of(masters), false, true, false);
		assertTrue(rankOnly.bestRank());
		assertFalse(rankOnly.bestScore());
		var hidden = ScoreboardRankData.columns(List.of(masters), true, true, true);
		assertFalse(hidden.bestRank());
		assertTrue(hidden.bestScore());
		assertTrue(hidden.medalScore());
	}

	@Test
	public void mixedMedalSystemsBelongToDifferentRegistrationCategoriesInTheSameSession() {
		Championship open = championship("Registration Total test", Ranking.TOTAL);
		Championship masters = championship("Registration QMasters test", Ranking.QAGE);
		Championship youth = championship("Registration GAMX-U test", Ranking.GAMX_U);
		JPAService.runInTransaction(em -> {
			for (Championship championship : List.of(open, masters, youth)) {
				em.persist(championship);
			}
			return null;
		});
		Championship.reset();
		try {
			List<Athlete> sources = AthleteRepository.findAll();
			Athlete first = sources.get(0);
			Athlete second = sources.get(1);
			second.setGroup(first.getGroup());
			PAthlete totalAthlete = registrationAthlete(first, open, "", 64.0, "2001-01-01");
			PAthlete mastersAthlete = registrationAthlete(second, masters, "W35Q", 999.0, "1990-01-01");
			assertFalse(totalAthlete.getId().equals(mastersAthlete.getId()));
			assertFalse(totalAthlete.getCategory().equals(mastersAthlete.getCategory()));
			assertEquals(totalAthlete.getGroup().getId(), mastersAthlete.getGroup().getId());
			assertEquals(Double.valueOf(0), mastersAthlete.getCategory().getMinimumWeight());
			assertEquals(Double.valueOf(999), mastersAthlete.getCategory().getMaximumWeight());
			assertEquals(Ranking.TOTAL, totalAthlete.getComputedScoringSystem());
			assertEquals(Ranking.QAGE, mastersAthlete.getComputedScoringSystem());
			var columns = ScoreboardRankData.columns(
			        List.of(totalAthlete.getAgeGroup().getChampionship(), mastersAthlete.getAgeGroup().getChampionship()),
			        false, false, false);
			assertTrue(columns.medalScore());
			assertEquals(Translator.translate("Ranking.QAGE", Locale.ENGLISH),
			        ScoreboardRankData.title(columns.medalSystems(), Locale.ENGLISH));
			Map<String, String> totalFields = ScoreboardRankData.fields(totalAthlete, Locale.ENGLISH, true);
			Map<String, String> mastersFields = ScoreboardRankData.fields(mastersAthlete, Locale.ENGLISH, true);
			assertEquals("1", totalFields.get("medalRank"));
			assertEquals("1", mastersFields.get("medalRank"));
			assertEquals("", totalFields.get("medalScore"));
			assertEquals(String.format(Locale.ENGLISH, "%.3f", mastersAthlete.getMedalScore()),
			        mastersFields.get("medalScore"));
			assertEquals("1", mastersFields.get("snatchRank"));
			assertEquals("1", mastersFields.get("cleanJerkRank"));

			PAthlete youthAthlete = registrationAthlete(first, youth, "U15", 64.0, "2012-01-01");
			assertFalse(youthAthlete.getCategory().equals(mastersAthlete.getCategory()));
			assertEquals(Ranking.GAMX_U, youthAthlete.getComputedScoringSystem());
			columns = ScoreboardRankData.columns(
			        List.of(youthAthlete.getAgeGroup().getChampionship(), mastersAthlete.getAgeGroup().getChampionship()),
			        false, false, false);
			assertEquals(Translator.translate("Score", Locale.ENGLISH),
			        ScoreboardRankData.title(columns.medalSystems(), Locale.ENGLISH));
			assertEquals("1", ScoreboardRankData.fields(youthAthlete, Locale.ENGLISH, true).get("medalRank"));
			assertEquals("1", ScoreboardRankData.fields(mastersAthlete, Locale.ENGLISH, true).get("medalRank"));
		} finally {
			JPAService.runInTransaction(em -> {
				for (Championship championship : List.of(open, masters, youth)) {
					em.remove(em.find(Championship.class, championship.getId()));
				}
				return null;
			});
			Championship.reset();
		}
	}

	@Test
	public void computedKgLiftRanksCanBeFifthWhileScoreMedalRankIsFirst() {
		Championship championship = championship("Computed QMasters display test", Ranking.QAGE);
		AgeGroup ageGroup = new AgeGroup();
		ageGroup.setCode("W35Q");
		ageGroup.setGender(Gender.F);
		ageGroup.setMinAge(35);
		ageGroup.setMaxAge(999);
		ageGroup.setActive(true);
		ageGroup.setChampionship(championship);
		Category category = new Category();
		category.setGender(Gender.F);
		category.setMinimumWeight(0.0);
		category.setMaximumWeight(999.0);
		ageGroup.addCategory(category);
		List<Athlete> athletes = new ArrayList<>();
		var group = AthleteRepository.findAll().get(0).getGroup();
		for (int index = 0; index < 5; index++) {
			Athlete athlete = new Athlete();
			athlete.setValidation(false);
			athlete.setFirstName("QMasters rank " + index);
			athlete.setLastName("Display test");
			athlete.setGender(Gender.F);
			athlete.setIsoBirthDate(index == 0 ? "1936-01-01" : "1990-01-01");
			athlete.setBodyWeight(63.5);
			athlete.setGroup(group);
			athlete.setSnatch1ActualLift(Integer.toString(40 + index * 5));
			athlete.setSnatch2ActualLift("0");
			athlete.setSnatch3ActualLift("0");
			athlete.setCleanJerk1ActualLift(Integer.toString(60 + index * 5));
			athlete.setCleanJerk2ActualLift("0");
			athlete.setCleanJerk3ActualLift("0");
			athlete.setCategory(category);
			athlete.setParticipations(List.of(new Participation(athlete, category)));
			athletes.add(athlete);
		}
		JPAService.runInTransaction(em -> {
			em.persist(championship);
			em.persist(ageGroup);
			for (Athlete athlete : athletes) {
				athlete.setGroup(em.getReference(Group.class, group.getId()));
				em.persist(athlete);
			}
			return null;
		});
		Championship.reset();
		try {
			Competition.getCurrent().computeMedalsByCategory(athletes);
			Athlete oldest = AthleteRepository.findById(athletes.get(0).getId());
			oldest.getCategory();
			assertEquals(5, oldest.getMainRankings().getSnatchRank());
			assertEquals(5, oldest.getMainRankings().getCleanJerkRank());
			assertEquals(5, oldest.getMainRankings().getTotalRank());
			assertEquals(1, oldest.getMainRankings().getCategoryScoreRank());
			Map<String, String> fields = ScoreboardRankData.fields(oldest, Locale.ENGLISH, true);
			assertEquals("5", fields.get("snatchRank"));
			assertEquals("5", fields.get("cleanJerkRank"));
			assertEquals("5", fields.get("totalRank"));
			assertEquals("1", fields.get("medalRank"));
			assertEquals("medal1", fields.get("medalHighlight"));
			// kg lift ranks are still computed and sent, but a Total-only championship shows no lift rank columns
			assertFalse(ScoreboardRankData.columns(List.of(championship), false, false, false).liftRanks());
		} finally {
			JPAService.runInTransaction(em -> {
				for (Athlete athlete : athletes) {
					em.remove(em.find(Athlete.class, athlete.getId()));
				}
				em.flush();
				em.remove(em.find(AgeGroup.class, ageGroup.getId()));
				em.flush();
				em.remove(em.find(Championship.class, championship.getId()));
				return null;
			});
			Championship.reset();
		}
	}

	private PAthlete registrationAthlete(Athlete athlete, Championship championship, String ageGroupCode,
	        double maximumWeight, String birthDate) {
		AgeGroup ageGroup = new AgeGroup();
		ageGroup.setCode(ageGroupCode);
		ageGroup.setGender(Gender.F);
		ageGroup.setChampionship(championship);
		Category category = new Category();
		category.setAgeGroup(ageGroup);
		category.setGender(Gender.F);
		category.setMinimumWeight(0.0);
		category.setMaximumWeight(maximumWeight);
		athlete.setValidation(false);
		athlete.setGender(Gender.F);
		athlete.setIsoBirthDate(birthDate);
		athlete.setBodyWeight(63.5);
		athlete.setSnatch1ActualLift("90");
		athlete.setSnatch2ActualLift("0");
		athlete.setSnatch3ActualLift("0");
		athlete.setCleanJerk1ActualLift("110");
		athlete.setCleanJerk2ActualLift("0");
		athlete.setCleanJerk3ActualLift("0");
		athlete.setCategory(category);
		Participation participation = new Participation(athlete, category);
		participation.setTotalRank(1);
		participation.setSnatchRank(1);
		participation.setCleanJerkRank(1);
		participation.setCategoryScoreRank(1);
		participation.setBestAthleteRank(1);
		athlete.setParticipations(List.of(participation));
		return PAthlete.copyForReporting(athlete);
	}

	@Test
	public void payloadSeparatesWeightMedalAndBestAthleteRanksAndExportsParticipationScores() {
		Athlete source = AthleteRepository.findAll().get(0);
		source.setValidation(false);
		source.setBodyWeight(70.0);
		source.setSnatch1ActualLift("100");
		source.setSnatch2ActualLift("0");
		source.setSnatch3ActualLift("0");
		source.setCleanJerk1ActualLift("120");
		source.setCleanJerk2ActualLift("0");
		source.setCleanJerk3ActualLift("0");
		source.getCategory();
		PAthlete athlete = PAthlete.copyForReporting(source);
		Participation participation = athlete.getMainRankings();
		Championship championship = participation.getCategory().getAgeGroup().getChampionship();
		Ranking originalScoring = championship.getScoringSystem();
		Ranking originalBest = championship.getBestAthleteScoringSystem();
		MedalPolicy originalMedals = championship.getMedalPolicy();
		TeamPointsPolicy originalPoints = championship.getTeamPointsPolicy();
		boolean originalDefaults = championship.computeUsesCompetitionDefaults();
		try {
			championship.setUseCompetitionDefaults(false);
			championship.setMedalPolicy(MedalPolicy.TOTAL_ONLY);
			championship.setTeamPointsPolicy(TeamPointsPolicy.TOTAL_ONLY);
			championship.setScoringSystem(Ranking.QAGE);
			championship.setBestAthleteScoringSystem(Ranking.BW_SINCLAIR);
			participation.setTotalRank(4);
			participation.setSnatchRank(3);
			participation.setCleanJerkRank(4);
			participation.setCategoryScoreRank(2);
			participation.setBestAthleteRank(11);
			participation.setTeamMember(true);
			Map<String, String> fields = ScoreboardRankData.fields(athlete, Locale.ENGLISH, true);
			assertEquals("4", fields.get("totalRank"));
			assertEquals("3", fields.get("snatchRank"));
			assertEquals("4", fields.get("cleanJerkRank"));
			assertEquals("2", fields.get("medalRank"));
			assertEquals("11", fields.get("sinclairRank"));
			assertEquals("medal2", fields.get("medalHighlight"));
			assertEquals(String.format(Locale.ENGLISH, "%.3f", participation.getCategoryScore()), fields.get("medalScore"));
			assertEquals(String.format(Locale.ENGLISH, "%.3f", Ranking.getRankingValue(source, Ranking.BW_SINCLAIR)),
			        fields.get("sinclair"));
			assertEquals(2, athlete.getMedalRank().intValue());
			assertEquals(participation.getCategoryScore(), athlete.getMedalScore());
			assertEquals(AthleteSorter.pointsFormula(2, championship), participation.getTotalPoints());
			assertFalse(fields.containsKey("sinclairMedal"));
			assertFalse(fields.containsKey("totalMedal"));

			ParticipationDTO exported = ParticipationDTO.fromParticipation(participation);
			assertEquals(Integer.valueOf(4), exported.getTotalRank());
			assertEquals(Integer.valueOf(2), exported.getCategoryScoreRank());
			assertEquals(Integer.valueOf(11), exported.getBestAthleteRank());
			assertEquals(participation.getCategoryScore(), exported.getCategoryScore());

			championship.setScoringSystem(Ranking.TOTAL);
			participation.setCategoryScoreRank(4);
			fields = ScoreboardRankData.fields(athlete, Locale.ENGLISH, true);
			assertEquals(fields.get("totalRank"), fields.get("medalRank"));
			assertEquals("", fields.get("medalScore"));
			assertEquals("", fields.get("medalHighlight"));
			assertEquals("11", fields.get("sinclairRank"));

			championship.setMedalPolicy(MedalPolicy.LIFTS_ONLY);
			participation.setCategoryScoreRank(1);
			assertEquals("", ScoreboardRankData.fields(athlete, Locale.ENGLISH, true).get("medalHighlight"));
		} finally {
			championship.setScoringSystem(originalScoring);
			championship.setBestAthleteScoringSystem(originalBest);
			championship.setMedalPolicy(originalMedals);
			championship.setTeamPointsPolicy(originalPoints);
			championship.setUseCompetitionDefaults(originalDefaults);
		}
	}
}
