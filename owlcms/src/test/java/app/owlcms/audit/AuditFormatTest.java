package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.tests.MockCountdownTimer;
import app.owlcms.tests.TestStartup;
import tools.jackson.databind.json.JsonMapper;

public class AuditFormatTest {
	@BeforeClass
	public static void startUp() {
		TestStartup.init();
	}

	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	@Test
	public void clockRoundsUpLikeTheScoreboard() {
		assertEquals("0:31", FopAudit.formatClock(30_010));
		assertEquals("0:30", FopAudit.formatClock(29_990));
		assertEquals("0:30", FopAudit.formatClock(30_000));
		assertEquals("2:00", FopAudit.formatClock(119_999));
		assertEquals("0:01", FopAudit.formatClock(1));
		assertEquals("0:00", FopAudit.formatClock(-1));
		assertEquals("1:00:01", FopAudit.formatClock(3_600_001));
		assertEquals("596:31:24", FopAudit.formatClock(Integer.MAX_VALUE));
		assertEquals("clock=2:00 (119999ms)", FopAudit.timeDetail("clock", 119_999));
		assertEquals("clock=0:00 (-1ms)", FopAudit.timeDetail("clock", -1));
		assertEquals("remaining=indefinite", FopAudit.timeDetail("remaining", null));
	}

	@Test
	public void clockStartAndStopLogTheDisplayedTimeAndOneRawSample() throws Exception {
		String platform = "clock-audit-" + UUID.randomUUID();
		FieldOfPlay fop = new FieldOfPlay();
		fop.setName(platform);
		AtomicInteger reads = new AtomicInteger();
		MockCountdownTimer timer = new MockCountdownTimer() {
			@Override
			public int liveTimeRemaining() {
				reads.incrementAndGet();
				return getTimeRemaining();
			}
		};
		fop.setAthleteTimer(timer);

		timer.setTimeRemaining(30_010, false);
		FopAudit.write(fop, new FOPEvent.TimeStarted(null), false, "TIME_RUNNING", null);
		timer.setTimeRemaining(29_990, false);
		FopAudit.write(fop, new FOPEvent.TimeStopped(null), false, "TIME_STOPPED", null);

		Path log = AuditLog.openFiles().stream()
				.filter(path -> path.getFileName().toString().startsWith(platform + "_")).findFirst().orElseThrow();
		List<String> lines = Files.readAllLines(log);
		String start = lines.stream().filter(line -> line.contains("clock.start")).findFirst().orElseThrow();
		String stop = lines.stream().filter(line -> line.contains("clock.stop")).findFirst().orElseThrow();
		assertTrue(start, start.contains("clock=0:31 (30010ms)"));
		assertTrue(stop, stop.contains("clock=0:30 (29990ms)"));
		assertEquals(30_010, record(start).details().get("clockMillis"));
		assertEquals(29_990, record(stop).details().get("clockMillis"));
		assertEquals(2, reads.get());
	}

	@Test
	public void escapesSeparatorsBackslashesAndNewlines() {
		AuditEntry entry = AuditEntry.builder("A|B", "test")
				.actor(AuditActor.device("REFEREE", 2, "refbox"))
				.field("a\\b")
				.newValue("one|two\nthree")
				.details(AuditFormat.fields("quoted", "\"x|y\"\nnext", "count", 3, "valid", true))
				.build();
		String formatted = AuditFormat.format(7, entry);
		assertTrue(formatted.contains("a\\\\b one\\|two\\nthree"));
		AuditRecord record = record(formatted);
		assertEquals("A|B", record.platform());
		assertEquals("one|two\nthree", record.change().after());
		assertEquals("\"x|y\"\nnext", record.details().get("quoted"));
		assertEquals(3, record.details().get("count"));
		assertEquals(true, record.details().get("valid"));
		assertFalse(record.actor().inferred());
		assertEquals(formatted.substring(0, formatted.indexOf(" | json=")), AuditFormat.readable(record));
	}

	@Test
	public void leadingColumnsAreSequenceTimeStationActionAthleteSessionAttemptChange() {
		AuditEntry entry = AuditEntry.builder("A", "athlete.change")
				.actor(AuditActor.device("MARSHAL", null, "simulator"))
				.session("M1")
				.attempt("SN1")
				.field("snatch1.change1")
				.newValue(47)
				.details(AuditFormat.fields("x", 1))
				.build();
		OffsetDateTime now = OffsetDateTime.parse("2026-09-29T13:10:59.868-04:00");
		String[] columns = AuditFormat.format(30, entry, now, false).split(" \\| ");
		assertEquals(8, columns.length);
		assertEquals("30", columns[0].trim());
		assertEquals("13:10:59.868", columns[1]);
		assertEquals("MARSHAL", columns[2].trim());
		assertEquals("athlete.change", columns[3].trim());
		assertEquals("-", columns[4].trim());
		assertEquals("M1", columns[5].trim());
		assertEquals("SN1", columns[6].trim());
		assertEquals("change1 - -> 47, x=1", columns[7]);
		assertEquals("M1", record(AuditFormat.format(30, entry, now, true)).session());
	}

	@Test
	public void aVersionOneRecordIsRenderedWithoutTheSessionColumn() {
		String line = "   30 | 13:10:59.868 | MARSHAL       | athlete.change      | -                                        "
				+ "| SN1 | change1 - -> 47";
		String json = "{\"schemaVersion\":1,\"sequence\":30,\"timestamp\":\"2026-09-29T13:10:59.868-04:00\","
				+ "\"platform\":\"A\",\"actor\":{\"mode\":\"PIN\",\"user\":\"-\",\"station\":\"MARSHAL\",\"index\":null,"
				+ "\"inferred\":false,\"client\":\"-\",\"device\":\"simulator\"},\"action\":\"athlete.change\","
				+ "\"athlete\":null,\"attempt\":\"SN1\",\"change\":{\"field\":\"snatch1.change1\",\"before\":null,"
				+ "\"after\":47,\"priorRequested\":null,\"previousAttemptRequested\":null},\"cause\":null,\"details\":{}}";
		assertEquals(line, AuditFormat.readable(record(line + " | json=" + json)));
	}

	@Test
	public void valuesWithBlanksAreQuoted() {
		assertEquals("team=CHI,category=\"P15 F 45\",name=\"A \"\"B\"\" C\",lights=\"true,false\",lot=-",
				AuditFormat.kvs("team", "CHI", "category", "P15 F 45", "name", "A \"B\" C", "lights", "true,false",
						"lot", null));
	}

	@Test
	public void fullFormContainsACompleteReproducibleJsonRecord() {
		AuditEntry entry = AuditEntry.builder("A", "clock.start")
				.actor(AuditActor.device("TIMEKEEPER", null, "clock"))
				.build();
		OffsetDateTime now = OffsetDateTime.parse("2026-09-29T13:10:59.868-04:00");
		String shortForm = AuditFormat.format(5, entry, now, false);
		String fullForm = AuditFormat.format(5, entry, now, true);
		assertTrue(fullForm.startsWith(shortForm + " | json={"));
		AuditRecord record = record(fullForm);
		assertEquals(2, record.schemaVersion());
		assertEquals(5, record.sequence());
		assertEquals("2026-09-29T13:10:59.868-04:00", record.timestamp());
		assertEquals("A", record.platform());
		assertEquals("clock.start", record.action());
		assertEquals("TIMEKEEPER", record.actor().station());
		assertEquals("clock", record.actor().device());
		assertEquals(shortForm, AuditFormat.readable(record));
	}

	@Test
	public void stationColumnFitsJuryMemberNames() {
		AuditEntry entry = AuditEntry.builder("A", "jury.vote")
				.actor(AuditActor.device("JURY_MEMBER", 1, "jurybox"))
				.build();

		String line = AuditFormat.format(5, entry, OffsetDateTime.parse("2026-09-29T13:10:59.868-04:00"),
				false);
		String[] columns = line.split(" \\| ", -1);

		assertEquals("JURY_MEMBER#1", columns[2].strip());
		assertEquals(13, columns[2].length());
	}

	@Test
	public void decisionsBreaksAndForcedClocksKeepTheirRawEvidence() throws Exception {
		String platform = "events-audit-" + UUID.randomUUID();
		FieldOfPlay fop = new FieldOfPlay();
		fop.setName(platform);
		FopAudit.write(fop, new FOPEvent.DecisionUpdate(null, 0, true), false, "DECISION", null);
		FopAudit.write(fop, new FOPEvent.ExplicitDecision(null, null, false, true, false, null),
				false, "DECISION", null);
		FopAudit.write(fop, new FOPEvent.BreakPaused(null, null), false, "BREAK", null);
		FopAudit.write(fop, new FOPEvent.ForceTime(60_000, null), false, "TIME_STOPPED", 29_990);

		Path log = AuditLog.openFiles().stream()
				.filter(path -> path.getFileName().toString().startsWith(platform + "_")).findFirst().orElseThrow();
		List<String> lines = Files.readAllLines(log);
		String vote = lines.stream().filter(line -> line.contains("referee.vote")).findFirst().orElseThrow();
		AuditRecord voteRecord = record(vote);
		assertTrue(vote, vote.contains("REFEREE#1") && vote.contains("decision=\"GOOD LIFT\""));
		assertEquals(true, voteRecord.details().get("decision"));
		assertEquals(Integer.valueOf(1), voteRecord.actor().index());

		String decision = lines.stream().filter(line -> line.contains("referee.decision")).findFirst().orElseThrow();
		assertTrue(decision, decision.contains("result=\"NO LIFT\",lights=\"true,false,null\""));
		assertEquals(Arrays.asList(true, false, null), record(decision).details().get("lights"));

		String paused = lines.stream().filter(line -> line.contains("break.pause")).findFirst().orElseThrow();
		assertTrue(paused, paused.contains("remaining=indefinite"));
		assertTrue(record(paused).details().containsKey("remainingMillis"));
		assertEquals(null, record(paused).details().get("remainingMillis"));

		String forced = lines.stream().filter(line -> line.contains("clock.set")).findFirst().orElseThrow();
		assertTrue(forced, forced.contains("clock 0:30 (29990ms) -> 1:00 (60000ms)"));
		assertEquals(29_990, record(forced).change().before());
		assertEquals(60_000, record(forced).change().after());
	}

	private static AuditRecord record(String line) {
		int separator = line.indexOf(" | json=");
		assertTrue(line, separator >= 0);
		return MAPPER.readValue(line.substring(separator + " | json=".length()), AuditRecord.class);
	}
}