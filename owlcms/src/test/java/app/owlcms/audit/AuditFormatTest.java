package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;

import org.junit.Test;

public class AuditFormatTest {
	@Test
	public void escapesSeparatorsBackslashesAndNewlines() {
		AuditEntry entry = AuditEntry.builder("A|B", "test")
				.actor(AuditActor.device("REFEREE", 2, "refbox"))
				.field("a\\b")
				.newValue("one|two\nthree")
				.build();
		String formatted = AuditFormat.format(7, entry);
		assertTrue(formatted.contains("platform=A\\|B"));
		assertTrue(formatted.contains("a\\\\b one\\|two\\nthree"));
		assertFalse(formatted.contains("inferred"));
	}

	@Test
	public void leadingColumnsAreSequenceTimeStationActionAthleteAttemptChange() {
		AuditEntry entry = AuditEntry.builder("A", "athlete.change")
				.actor(AuditActor.device("MARSHAL", null, "simulator"))
				.attempt("SN1")
				.field("snatch1.change1")
				.newValue(47)
				.detail("x=1")
				.build();
		OffsetDateTime now = OffsetDateTime.parse("2026-09-29T13:10:59.868-04:00");
		String[] columns = AuditFormat.format(30, entry, now, false).split(" \\| ");
		assertEquals(7, columns.length);
		assertEquals("30", columns[0].trim());
		assertEquals("13:10:59.868", columns[1]);
		assertEquals("MARSHAL", columns[2].trim());
		assertEquals("athlete.change", columns[3].trim());
		assertEquals("-", columns[4].trim());
		assertEquals("SN1", columns[5].trim());
		assertEquals("change1 - -> 47, x=1", columns[6]);
	}

	@Test
	public void valuesWithBlanksAreQuoted() {
		assertEquals("team=CHI,category=\"P15 F 45\",name=\"A \"\"B\"\" C\",lights=\"true,false\",lot=-",
				AuditFormat.kvs("team", "CHI", "category", "P15 F 45", "name", "A \"B\" C", "lights", "true,false",
						"lot", null));
	}

	@Test
	public void fullFormAppendsIdentificationAfterShortForm() {
		AuditEntry entry = AuditEntry.builder("A", "clock.start")
				.actor(AuditActor.device("TIMEKEEPER", null, "clock"))
				.build();
		OffsetDateTime now = OffsetDateTime.parse("2026-09-29T13:10:59.868-04:00");
		String shortForm = AuditFormat.format(5, entry, now, false);
		String fullForm = AuditFormat.format(5, entry, now, true);
		assertTrue(fullForm.startsWith(shortForm + " | cause="));
		assertTrue(fullForm.contains("timestamp=2026-09-29T13:10:59.868-04:00"));
		assertTrue(fullForm.contains("device=clock"));
	}
}