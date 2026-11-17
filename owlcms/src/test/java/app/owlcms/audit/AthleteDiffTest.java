package app.owlcms.audit;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.audit.AthleteDiff.Change;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.tests.TestStartup;

public class AthleteDiffTest {
	@BeforeClass
	public static void setupTests() {
		TestStartup.init();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	@Test
	public void reportsEachChangedFieldAndAttempt() {
		Athlete athlete = new Athlete();
		athlete.setLastName("Before");
		athlete.setBodyWeight(80.0);
		athlete.setSnatch1Declaration("90");
		AthleteDiff.Snapshot before = AthleteDiff.snapshot(athlete);

		athlete.setLastName("After");
		athlete.setBodyWeight(81.2);
		athlete.setSnatch1Declaration("91");
		List<Change> changes = AthleteDiff.diff(before, AthleteDiff.snapshot(athlete));

		assertEquals(3, changes.size());
		assertEquals("SN1", changes.stream().filter(change -> change.field().equals("snatch1.declaration"))
				.findFirst().orElseThrow().attempt());
	}

	@Test
	public void automaticProgressionIsMarkedPlusOneOrSame() {
		Athlete athlete = new Athlete();
		athlete.setSnatch1Declaration("46");
		assertEquals("47 (+1)", AthleteAudit.annotate(athlete, new Change("snatch2.automatic", 0, 47, "SN2")));
		assertEquals("46 (SAME)", AthleteAudit.annotate(athlete, new Change("snatch2.automatic", 0, 46, "SN2")));
		assertEquals(47, AthleteAudit.annotate(athlete, new Change("snatch2.declaration", null, 47, "SN2")));
	}

	@Test
	public void liftResultAndProgressionHaveTheirOwnActions() {
		assertEquals("athlete.lift", AthleteAudit.action(new Change("snatch1.actualLift", null, 35, "SN1")));
		assertEquals("athlete.progression", AthleteAudit.action(new Change("snatch2.automatic", 0, 36, "SN2")));
		assertEquals("athlete.change", AthleteAudit.action(new Change("snatch2.change1", null, 37, "SN2")));
	}

	@Test
	public void declarationsAndChangesShowDifferenceFromPriorRequest() {
		Athlete athlete = new Athlete();
		athlete.setValidation(false);
		athlete.setSnatch1Declaration("44");
		AthleteDiff.Snapshot before = AthleteDiff.snapshot(athlete);
		athlete.setSnatch1Change1("46");
		Change change1 = change(before, athlete, "snatch1.change1");
		assertEquals("46 (+2)", AthleteAudit.annotate(athlete, change1));

		before = AthleteDiff.snapshot(athlete);
		athlete.setSnatch1Change2("45");
		Change change2 = change(before, athlete, "snatch1.change2");
		assertEquals("45 (-1)", AthleteAudit.annotate(athlete, change2));

		before = AthleteDiff.snapshot(athlete);
		athlete.setSnatch1Change2("46");
		Change revised = change(before, athlete, "snatch1.change2");
		assertEquals("46 (+1)", AthleteAudit.annotate(athlete, revised));

		athlete = new Athlete();
		athlete.setValidation(false);
		athlete.setSnatch1Declaration("46");
		before = AthleteDiff.snapshot(athlete);
		athlete.setSnatch1Change1("46");
		Change same = change(before, athlete, "snatch1.change1");
		assertEquals("46 (SAME)", AthleteAudit.annotate(athlete, same));
	}

	private static Change change(AthleteDiff.Snapshot before, Athlete athlete, String field) {
		return AthleteDiff.diff(before, AthleteDiff.snapshot(athlete)).stream()
				.filter(change -> change.field().equals(field)).findFirst().orElseThrow();
	}

	@Test
	public void noChangeProducesNoEntries() {
		Athlete athlete = new Athlete();
		AthleteDiff.Snapshot snapshot = AthleteDiff.snapshot(athlete);
		assertEquals(List.of(), AthleteDiff.diff(snapshot, AthleteDiff.snapshot(athlete)));
	}
}