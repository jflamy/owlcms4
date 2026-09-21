/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.MedalPolicy;
import app.owlcms.data.agegroup.TeamPointsPolicy;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.config.Config;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Logger;

public class QualifyingTotalMedalsTest {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(QualifyingTotalMedalsTest.class);

	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
		TestData.insertInitialData(2, true);
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	@Test
	public void qualifyingTotalChangesMedalsButNotImwaRanks() {
		Competition competition = Competition.getCurrent();
		competition.setMigrated(false);
		competition.setSnatchCJTotalMedals(true);
		List<Athlete> athletes = AthleteRepository.findAll();
		assertEquals(4, athletes.size());
		athletes.stream().map(Athlete::getGroup).distinct().forEach(group -> group.setMasters(true));

		athletes.sort(Comparator.comparing(Athlete::getLotNumber));
		setResult(athletes.get(0), 90, 110);
		setResult(athletes.get(1), 85, 105);
		setResult(athletes.get(2), 80, 100);
		setResult(athletes.get(3), 75, 95);

		Category category = athletes.get(0).getCategory();
		Championship championship = category.getAgeGroup().getChampionship();
		championship.setMedalPolicy(MedalPolicy.TOTAL_ONLY);
		championship.setTeamPointsPolicy(TeamPointsPolicy.TOTAL_ONLY);
		category.setQualifyingTotal(160);
		competition.setImwa(false);
		competition.computeMedalsByCategory(athletes);
		printMedalTable("All above QT (non-IMWA)", athletes, category);
		assertRanks(athletes, 1, 2, 3, 4);
		assertTotalMedals(athletes, true, true, true, false);
		assertTeamPoints(athletes, 28, 25, 23, 22);

		category.setQualifyingTotal(185);
		competition.setImwa(true);
		competition.computeMedalsByCategory(athletes);
		printMedalTable("Two below QT (IMWA)", athletes, category);
		assertRanks(athletes, 1, 2, 3, 4);
		assertTotalMedals(athletes, true, true, false, false);
		assertTeamPoints(athletes, 28, 25, 23, 22);

		competition.setImwa(false);
		competition.computeMedalsByCategory(athletes);
		printMedalTable("Two below QT (non-IMWA)", athletes, category);
		assertRanks(athletes, 1, 2, -1, -1);
		assertTotalMedals(athletes, true, true, false, false);
		assertTeamPoints(athletes, 28, 25, 0, 0);
	}

	private static void printMedalTable(String title, List<Athlete> athletes, Category category) {
		StringBuilder table = new StringBuilder("\n").append(title).append('\n');
		boolean showLiftMedals = category.getAgeGroup().getChampionship().getMedalPolicy().includesSnatchAndCleanJerk();
		if (showLiftMedals) {
			table.append(String.format(Locale.ROOT,
			        "%-22s %7s %7s %7s %6s %8s %8s %8s %7s %6s %6s %6s %8s %-18s %-28s%n",
			        "Athlete", "Snatch", "C&J", "Total", "QT", "S Medal", "CJ Medal", "T Medal", "T Rank",
			        "S Pts", "CJ Pts", "T Pts", "Team Pts", "Eligibility", "No Total medal reason"));
		} else {
			table.append(String.format(Locale.ROOT,
			        "%-22s %7s %7s %7s %6s %8s %7s %8s %-18s %-28s%n",
			        "Athlete", "Snatch", "C&J", "Total", "QT", "T Medal", "T Rank", "Team Pts", "Eligibility",
			        "No Total medal reason"));
		}
		for (Athlete athlete : athletes) {
			if (showLiftMedals) {
				table.append(String.format(Locale.ROOT,
				        "%-22s %7d %7d %7d %6d %8s %8s %8s %7d %6d %6d %6d %8d %-18s %-28s%n",
				        athlete.getAbbreviatedName(), athlete.getSnatchTotal(), athlete.getCleanJerkTotal(),
				        athlete.getTotal(), category.getQualifyingTotal(), medal(athlete, Ranking.SNATCH),
				        medal(athlete, Ranking.CLEANJERK), medal(athlete, Ranking.TOTAL), athlete.getTotalRank(),
				        athlete.getSnatchPoints(), athlete.getCleanJerkPoints(), athlete.getTotalPoints(),
				        athlete.getCombinedPoints(),
				        AthleteSorter.getEffectiveIndividualEligibilityStatus(athlete, category),
				        noTotalMedalReason(athlete, category)));
			} else {
				table.append(String.format(Locale.ROOT,
				        "%-22s %7d %7d %7d %6d %8s %7d %8d %-18s %-28s%n",
				        athlete.getAbbreviatedName(), athlete.getSnatchTotal(), athlete.getCleanJerkTotal(),
				        athlete.getTotal(), category.getQualifyingTotal(), medal(athlete, Ranking.TOTAL),
				        athlete.getTotalRank(), athlete.getCombinedPoints(),
				        AthleteSorter.getEffectiveIndividualEligibilityStatus(athlete, category),
				        noTotalMedalReason(athlete, category)));
			}
		}
		logger.info("{}", table);
	}

	private static String noTotalMedalReason(Athlete athlete, Category category) {
		if (AthleteSorter.isMedalist(athlete, Ranking.TOTAL)) {
			return "";
		}
		if (athlete.getTotalRank() < 1) {
			return "OOC: below QT";
		}
		boolean belowQualifyingTotal = athlete.getTotal() < category.getQualifyingTotal();
		if (athlete.getTotalRank() > 3) {
			return belowQualifyingTotal ? "Outside top 3; below QT" : "Outside top 3";
		}
		return belowQualifyingTotal ? "Below QT (IMWA)" : "Not awarded";
	}

	private static String medal(Athlete athlete, Ranking ranking) {
		if (!AthleteSorter.isMedalist(athlete, ranking)) {
			return "-";
		}
		return switch (AthleteSorter.getRank(athlete, ranking)) {
			case 1 -> "Gold";
			case 2 -> "Silver";
			case 3 -> "Bronze";
			default -> "-";
		};
	}

	private static void assertRanks(List<Athlete> athletes, int... expectedRanks) {
		for (int index = 0; index < athletes.size(); index++) {
			assertEquals(expectedRanks[index], athletes.get(index).getTotalRank());
		}
	}

	private static void assertTotalMedals(List<Athlete> athletes, boolean... expectedMedals) {
		for (int index = 0; index < athletes.size(); index++) {
			if (expectedMedals[index]) {
				assertTrue(AthleteSorter.isMedalist(athletes.get(index), Ranking.TOTAL));
			} else {
				assertFalse(AthleteSorter.isMedalist(athletes.get(index), Ranking.TOTAL));
			}
		}
	}

	private static void assertTeamPoints(List<Athlete> athletes, int... expectedPoints) {
		for (int index = 0; index < athletes.size(); index++) {
			assertEquals(expectedPoints[index], athletes.get(index).getCombinedPoints().intValue());
		}
	}

	private static void setResult(Athlete athlete, int snatch, int cleanJerk) {
		athlete.setValidation(false);
		athlete.setSnatch1ActualLift(Integer.toString(snatch));
		athlete.setSnatch2ActualLift(Integer.toString(-snatch));
		athlete.setSnatch3ActualLift(Integer.toString(-snatch));
		athlete.setCleanJerk1ActualLift(Integer.toString(cleanJerk));
		athlete.setCleanJerk2ActualLift(Integer.toString(-cleanJerk));
		athlete.setCleanJerk3ActualLift(Integer.toString(-cleanJerk));
		athlete.setValidation(true);
	}
}