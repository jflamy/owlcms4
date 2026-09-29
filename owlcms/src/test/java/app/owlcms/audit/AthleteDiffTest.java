package app.owlcms.audit;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.audit.AthleteDiff.Change;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;

public class AthleteDiffTest {
	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
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
	public void noChangeProducesNoEntries() {
		Athlete athlete = new Athlete();
		AthleteDiff.Snapshot snapshot = AthleteDiff.snapshot(athlete);
		assertEquals(List.of(), AthleteDiff.diff(snapshot, AthleteDiff.snapshot(athlete)));
	}
}