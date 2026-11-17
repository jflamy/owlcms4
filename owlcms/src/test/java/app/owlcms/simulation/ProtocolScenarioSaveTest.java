package app.owlcms.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditContext;
import app.owlcms.audit.AuditLog;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteMetadata;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.tests.MockCountdownTimer;
import app.owlcms.tests.TestStartup;

public class ProtocolScenarioSaveTest {
	@BeforeClass
	public static void setupTests() {
		TestStartup.init();
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

	@Test
	public void simulatedWeightChangesRecordTheClockAtEachSave() throws Exception {
		String name = "CLOCK" + System.nanoTime();
		Athlete athlete = new Athlete();
		athlete.setValidation(false);
		athlete.setLastName(name);
		athlete.setFirstName("Test");
		athlete.setSnatch1Declaration("90");
		athlete = AthleteRepository.save(athlete);
		RecordingFop fop = new RecordingFop(athlete);
		MockCountdownTimer timer = new MockCountdownTimer() {
			@Override
			public int liveTimeRemaining() {
				return getTimeRemaining();
			}
		};
		fop.setAthleteTimer(timer);
		ProtocolScenario scenario = new ProtocolScenario(fop, millis -> { }, 0, false);
		Athlete target = athlete;

		AuditContext.run(AuditActor.device("MARSHAL", null, "test"), "weight change", () -> {
			try {
				timer.setTimeRemaining(30_010, false);
				scenario.changeWeight(target, 95);
				timer.setTimeRemaining(29_990, false);
				scenario.changeWeight(target, 96);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			}
		});

		Path log = AuditLog.openFiles().stream()
				.filter(path -> path.getFileName().toString().startsWith("competition_")).findFirst().orElseThrow();
		String athleteId = "\"athlete\":{\"id\":\"" + target.getId() + "\"";
		List<String> lines = Files.readAllLines(log).stream().filter(line -> line.contains(athleteId)).toList();
		assertTrue(lines.toString(), lines.stream().anyMatch(line ->
				line.contains("change1 - -> 95 (+5), clock=0:31 (30010ms)")
						&& line.contains("\"field\":\"snatch1.change1\",\"before\":\"\",\"after\":\"95\","
								+ "\"priorRequested\":90,")
						&& line.contains("\"details\":{\"clockMillis\":30010}")));
		assertTrue(lines.toString(), lines.stream().anyMatch(line ->
				line.contains("change1 95 -> 96 (+1), clock=0:30 (29990ms)")
						&& line.contains("\"before\":\"95\",\"after\":\"96\",\"priorRequested\":95,")
						&& line.contains("\"details\":{\"clockMillis\":29990}")));
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
