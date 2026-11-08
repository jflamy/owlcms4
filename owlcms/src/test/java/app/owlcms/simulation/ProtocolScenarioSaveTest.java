package app.owlcms.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteMetadata;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;

public class ProtocolScenarioSaveTest {
	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		Competition.setCurrent(new Competition());
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	@Test
	public void simulatedWeightChangeAndWithdrawalUseTheFopMonitorAndPreserveMetadata() throws Exception {
		Athlete athlete = new Athlete();
		athlete.setValidation(false);
		athlete.setLastName("Before");
		athlete.setSnatch1Declaration("90");
		athlete = AthleteRepository.save(athlete);
		RecordingFop fop = new RecordingFop(athlete);
		AtomicInteger pauses = new AtomicInteger();
		ProtocolScenario scenario = new ProtocolScenario(fop, millis -> {
			assertFalse("simulation waits must release the FOP monitor", Thread.holdsLock(fop));
			pauses.incrementAndGet();
		}, 0, false);

		AthleteMetadata original = AthleteMetadata.from(athlete);
		AthleteMetadata corrected = new AthleteMetadata("Fixed", "Corrected", "Other team", "IWF,EWF",
		        original.eligibleForIndividualRanking(), original.individualEligibilityStatus());
		Long stamp;
		synchronized (fop) {
			stamp = AthleteRepository.saveMetadata(athlete.getId(), corrected, athlete.getMetadataUpdatedAt(),
			        AuditOrigin.system("simulation metadata test")).athlete().getMetadataUpdatedAt();
		}

		scenario.changeWeight(athlete, 95);
		Athlete changed = AthleteRepository.findById(athlete.getId());
		assertEquals("95", changed.getSnatch1Change1());
		assertEquals(corrected, AthleteMetadata.from(changed));
		assertEquals(stamp, changed.getMetadataUpdatedAt());
		assertFalse(fop.changes.getFirst().isResultChange());

		scenario.withdraw(athlete);
		Athlete withdrawn = AthleteRepository.findById(athlete.getId());
		assertEquals(6, withdrawn.getAttemptsDone().intValue());
		assertEquals(corrected, AthleteMetadata.from(withdrawn));
		assertEquals(stamp, withdrawn.getMetadataUpdatedAt());
		assertTrue(fop.changes.getLast().isResultChange());
		assertEquals(2, fop.changes.size());
		assertEquals(2, pauses.get());
	}

	private static class RecordingFop extends FieldOfPlay {
		private final Athlete athlete;
		private final List<FOPEvent.WeightChange> changes = new ArrayList<>();

		private RecordingFop(Athlete athlete) {
			this.athlete = athlete;
		}

		@Override
		public List<Athlete> getDisplayOrder() {
			assertTrue("resolve the edited athlete under its FOP monitor", Thread.holdsLock(this));
			return List.of(this.athlete);
		}

		@Override
		public void fopEventPost(FOPEvent event) {
			assertTrue("the save and event post must share the same critical section", Thread.holdsLock(this));
			assertTrue(event instanceof FOPEvent.WeightChange);
			Athlete stored = AthleteRepository.findById(this.athlete.getId());
			assertEquals("the change must be committed before its event", this.athlete.getAttemptsDone(),
			        stored.getAttemptsDone());
			assertEquals(this.athlete.getSnatch1Change1(), stored.getSnatch1Change1());
			this.changes.add((FOPEvent.WeightChange) event);
		}
	}
}
