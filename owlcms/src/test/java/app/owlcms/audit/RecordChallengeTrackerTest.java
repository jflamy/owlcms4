package app.owlcms.audit;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.records.RecordEvent;

public class RecordChallengeTrackerTest {
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
	public void repeatedRecomputationDoesNotRepeatChallenge() {
		RecordChallengeTracker tracker = new RecordChallengeTracker();
		Athlete athlete = athlete();
		RecordEvent record = record("IWF", 100);

		assertEquals(1, tracker.newlyChallenged(athlete, 1, 101, List.of(record)).size());
		assertEquals(0, tracker.newlyChallenged(athlete, 1, 101, List.of(record)).size());
		assertEquals(1, tracker.newlyChallenged(athlete, 1, 102, List.of(record)).size());
	}

	@Test
	public void oneAttemptCanChallengeSeveralRecords() {
		RecordChallengeTracker tracker = new RecordChallengeTracker();
		Athlete athlete = athlete();
		assertEquals(2, tracker.newlyChallenged(athlete, 1, 101,
				List.of(record("IWF", 100), record("PANAM", 99))).size());
	}

	private Athlete athlete() {
		Athlete athlete = new Athlete();
		athlete.setId(12L);
		return athlete;
	}

	private RecordEvent record(String federation, double value) {
		RecordEvent record = new RecordEvent();
		record.setRecordFederation(federation);
		record.setRecordName("Senior");
		record.setAgeGrp("SR");
		record.setBwCatString("81");
		record.setRecordLift(Ranking.SNATCH);
		record.setRecordValue(value);
		return record;
	}
}