/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import app.owlcms.data.agegroup.AgeGapCheck;
import app.owlcms.data.agegroup.AgeGapCheck.Gap;
import app.owlcms.data.agegroup.AgeGapCheck.Finding;
import app.owlcms.data.agegroup.AgeGapCheck.Overlap;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Gender;

public class AgeGapCheckTest {

	@Test
	public void continuousCoverageHasNoGap() {
		assertTrue(AgeGapCheck.gaps(List.of(ag(Gender.M, 35, 39), ag(Gender.M, 40, 44))).isEmpty());
	}

	@Test
	public void holeInTheMiddleIsReported() {
		// narrowing M35 to 35-38 leaves men aged 39 uncovered (spec 3.4 example)
		List<Gap> gaps = AgeGapCheck.gaps(List.of(ag(Gender.M, 35, 38), ag(Gender.M, 40, 44)));
		assertEquals(List.of(new Gap(Gender.M, 39, 39)), gaps);
	}

	@Test
	public void overlapsAreAllowed() {
		assertTrue(AgeGapCheck.gaps(List.of(ag(Gender.M, 30, 35), ag(Gender.M, 35, 39), ag(Gender.M, 33, 40)))
		        .isEmpty());
	}

	@Test
	public void agesOutsideTheSpanAreNotGaps() {
		// nothing below 30 or above 39 is expected: coverage runs from lowest minimum to highest maximum
		assertTrue(AgeGapCheck.gaps(List.of(ag(Gender.M, 30, 39))).isEmpty());
	}

	@Test
	public void gendersAreCheckedSeparately() {
		List<Gap> gaps = AgeGapCheck.gaps(List.of(
		        ag(Gender.M, 35, 38), ag(Gender.M, 40, 44),
		        ag(Gender.F, 35, 39), ag(Gender.F, 40, 44)));
		assertEquals(List.of(new Gap(Gender.M, 39, 39)), gaps);
	}

	@Test
	public void aMultiYearHoleReportsBothEnds() {
		List<Gap> gaps = AgeGapCheck.gaps(List.of(ag(Gender.F, 15, 20), ag(Gender.F, 25, 30)));
		assertEquals(List.of(new Gap(Gender.F, 21, 24)), gaps);
	}

	@Test
	public void emptyAndSingleGroupsHaveNoGap() {
		assertTrue(AgeGapCheck.gaps(List.of()).isEmpty());
		assertTrue(AgeGapCheck.gaps(List.of(ag(Gender.M, 45, 49))).isEmpty());
	}

	@Test
	public void removingAMiddleGroupOpensAGap() {
		AgeGroup w75 = ag(Gender.F, 75, 79);
		AgeGroup w80 = ag(Gender.F, 80, 84);
		AgeGroup w85 = ag(Gender.F, 85, 999);
		assertEquals(List.of(new Gap(Gender.F, 80, 84)),
		        AgeGapCheck.gapsOpenedByRemoving(List.of(w75, w80, w85), w80));
	}

	@Test
	public void removingTheLowestOrHighestGroupOpensNoGap() {
		AgeGroup w75 = ag(Gender.F, 75, 79);
		AgeGroup w80 = ag(Gender.F, 80, 84);
		AgeGroup w85 = ag(Gender.F, 85, 999);
		assertTrue(AgeGapCheck.gapsOpenedByRemoving(List.of(w75, w80, w85), w75).isEmpty());
		assertTrue(AgeGapCheck.gapsOpenedByRemoving(List.of(w75, w80, w85), w85).isEmpty());
	}

	@Test
	public void aHoleThatWasAlreadyThereIsNotReportedAgain() {
		AgeGroup a = ag(Gender.M, 30, 34);
		AgeGroup b = ag(Gender.M, 40, 44);
		AgeGroup c = ag(Gender.M, 50, 54);
		// 35-39 was already open; removing the end group c opens nothing new
		assertTrue(AgeGapCheck.gapsOpenedByRemoving(List.of(a, b, c), c).isEmpty());
		// removing b widens it to 35-49, which is a new gap
		assertEquals(List.of(new Gap(Gender.M, 35, 49)), AgeGapCheck.gapsOpenedByRemoving(List.of(a, b, c), b));
	}

	@Test
	public void removingAnInactiveGroupOpensNothing() {
		AgeGroup m35 = ag(Gender.M, 35, 39);
		AgeGroup inactive = ag(Gender.M, 36, 38);
		assertTrue(AgeGapCheck.gapsOpenedByRemoving(List.of(m35), inactive).isEmpty());
	}

	@Test
	public void sharedAgesAreReportedAsAnOverlap() {
		// VM60 typed as 60-65 instead of 60-64 shares age 65 with M65
		assertEquals(List.of(new Overlap(Gender.M, "VM60", "M65", 65, 65)),
		        AgeGapCheck.overlaps(List.of(named("M65", Gender.M, 65, 69), named("VM60", Gender.M, 60, 65))));
	}

	@Test
	public void touchingAgeGroupsDoNotOverlap() {
		assertTrue(AgeGapCheck.overlaps(List.of(ag(Gender.M, 65, 69), ag(Gender.M, 60, 64))).isEmpty());
	}

	@Test
	public void otherGendersDoNotOverlap() {
		assertTrue(AgeGapCheck.overlaps(List.of(ag(Gender.F, 60, 64), ag(Gender.M, 60, 64))).isEmpty());
	}

	@Test
	public void aWiderGroupOverlapsOverTheWholeRangeOfTheNarrowerOne() {
		assertEquals(List.of(new Overlap(Gender.F, "JR", "U25", 15, 20)),
		        AgeGapCheck.overlaps(List.of(named("U25", Gender.F, 15, 25), named("JR", Gender.F, 15, 20))));
	}

	@Test
	public void everyOverlapOfTheChampionshipIsReported() {
		// not only those of the age group being changed: M30 and M35 share 35, M35 and M40 share 38-39
		List<Overlap> overlaps = AgeGapCheck.overlaps(List.of(
		        named("M30", Gender.M, 30, 35), named("M35", Gender.M, 35, 39), named("M40", Gender.M, 38, 44)));
		assertEquals(List.of(new Overlap(Gender.M, "M30", "M35", 35, 35), new Overlap(Gender.M, "M35", "M40", 38, 39)),
		        overlaps);
	}

	@Test
	public void findingsAreGroupedPerChampionshipAndIgnoreInactiveGroups() {
		AgeGroup m35 = inChampionship("Masters", named("M35", Gender.M, 35, 39));
		AgeGroup m40 = inChampionship("Masters", named("M40", Gender.M, 40, 44));
		m40.setActive(false);
		AgeGroup m45 = inChampionship("Masters", named("M45", Gender.M, 45, 49));
		AgeGroup u15 = inChampionship("Youth", named("U15", Gender.M, 13, 15));
		AgeGroup u17 = inChampionship("Youth", named("U17", Gender.M, 15, 17));
		AgeGroup tidy = inChampionship("Open", named("OPEN", Gender.M, 0, 999));

		List<Finding> findings = AgeGapCheck.findings(List.of(m35, m40, m45, u15, u17, tidy));

		assertEquals(2, findings.size());
		assertEquals(new Finding("Masters", List.of(new Gap(Gender.M, 40, 44)), List.of()), findings.get(0));
		assertEquals(new Finding("Youth", List.of(), List.of(new Overlap(Gender.M, "U15", "U17", 15, 15))),
		        findings.get(1));
	}

	@Test
	public void agesAreWrittenAsOneNumberOrARange() {
		assertEquals("59", AgeGapCheck.ages(59, 59));
		assertEquals("60-64", AgeGapCheck.ages(60, 64));
	}

	private AgeGroup inChampionship(String championshipName, AgeGroup ageGroup) {
		ageGroup.setChampionshipName(championshipName);
		return ageGroup;
	}

	private AgeGroup named(String code, Gender gender, int minAge, int maxAge) {
		AgeGroup ag = ag(gender, minAge, maxAge);
		ag.setCode(code);
		return ag;
	}

	private AgeGroup ag(Gender gender, int minAge, int maxAge) {
		AgeGroup ag = new AgeGroup();
		ag.setGender(gender);
		ag.setMinAge(minAge);
		ag.setMaxAge(maxAge);
		ag.setActive(true);
		return ag;
	}
}
