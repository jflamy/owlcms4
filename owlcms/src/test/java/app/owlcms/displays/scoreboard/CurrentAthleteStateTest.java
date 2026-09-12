package app.owlcms.displays.scoreboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import app.owlcms.utils.JsonUtils;

public class CurrentAthleteStateTest {

	@Test
	public void decisionPreservesAttemptedAthleteAndWeight() {
		ArrayNode athletes = JsonUtils.array();
		ObjectNode athlete = JsonUtils.object();
		athlete.put("fullName", "Attempted Athlete");
		JsonUtils.set(athletes, 0, athlete);
		CurrentAthleteState original = CurrentAthleteState.builder(10, "CURRENT_ATHLETE")
		        .fullName("Attempted Athlete").team("Team").startNumber(4)
		        .lift("Snatch #3").weight(142).athletes(athletes)
		        .showAthleteClock(true).showDetails(true).showAttemptResults(true).build();

		ObjectNode decision = original.withDecision(11).toJson();
		assertEquals(11, decision.path("sequence").asLong());
		assertEquals("CURRENT_ATHLETE", decision.path("mode").asString());
		assertEquals("Attempted Athlete", decision.path("fullName").asString());
		assertEquals("Team", decision.path("team").asString());
		assertEquals(4, decision.path("startNumber").asInt());
		assertEquals("Snatch #3", decision.path("lift").asString());
		assertEquals("142", decision.path("weight").asString());
		assertEquals(athletes, decision.path("athletes"));
		assertTrue(decision.path("showDecisions").asBoolean());
		assertFalse(decision.path("showAthleteClock").asBoolean());
		assertFalse(decision.path("showBreakClock").asBoolean());
		assertFalse(original.toJson().path("showDecisions").asBoolean());
	}

	@Test
	public void interruptionContainsCompleteClearedDisplay() {
		ObjectNode interruption = CurrentAthleteState.builder(12, "INTERRUPTION")
		        .fullName("Marshal interruption").build().toJson();
		assertEquals("INTERRUPTION", interruption.path("mode").asString());
		assertEquals("Marshal interruption", interruption.path("fullName").asString());
		assertEquals("", interruption.path("team").asString());
		assertEquals("", interruption.path("lift").asString());
		assertEquals("", interruption.path("weight").asString());
		assertEquals(0, interruption.path("athletes").size());
		assertEquals(0, interruption.path("snIndicators").size());
		assertEquals(0, interruption.path("cjIndicators").size());
		assertEquals(0, interruption.path("decisions").size());
		assertFalse(interruption.path("showDetails").asBoolean());
		assertFalse(interruption.path("showAttemptResults").asBoolean());
		assertFalse(interruption.path("showDecisions").asBoolean());
		assertFalse(interruption.path("showAthleteClock").asBoolean());
		assertFalse(interruption.path("showBreakClock").asBoolean());
	}

	@Test
	public void completionCarriesFinalResultsWithoutActiveAttemptFields() {
		ArrayNode indicators = JsonUtils.array().add("132").add("137").add("142");
		ArrayNode classes = JsonUtils.array().add("white").add("white").add("white");
		ObjectNode done = CurrentAthleteState.builder(13, "SESSION_DONE")
		        .fullName("Session done").cjIndicators(indicators).cjIndicatorClasses(classes)
		        .showAttemptResults(true).build().toJson();
		assertEquals("SESSION_DONE", done.path("mode").asString());
		assertEquals("142", done.path("cjIndicators").path(2).asString());
		assertEquals("white", done.path("cjIndicatorClasses").path(2).asString());
		assertTrue(done.path("showAttemptResults").asBoolean());
		assertEquals("", done.path("weight").asString());
		assertFalse(done.path("showDetails").asBoolean());
		assertFalse(done.path("showDecisions").asBoolean());
	}

	@Test
	public void snapshotsDoNotShareMutableResultArrays() {
		ArrayNode athletes = JsonUtils.array();
		ObjectNode athlete = JsonUtils.object();
		athlete.put("fullName", "Attempted Athlete");
		JsonUtils.set(athletes, 0, athlete);
		CurrentAthleteState original = CurrentAthleteState.builder(1, "CURRENT_ATHLETE")
		        .athletes(athletes).build();
		athlete.put("fullName", "Next Athlete");
		ObjectNode serialized = original.toJson();
		assertEquals("Attempted Athlete", serialized.path("athletes").path(0).path("fullName").asString());
		((ObjectNode) serialized.path("athletes").path(0)).put("fullName", "Changed externally");
		assertEquals("Attempted Athlete",
		        original.withDecision(2).toJson().path("athletes").path(0).path("fullName").asString());
	}

	@Test
	public void nextAthleteSnapshotClearsDecisionWithoutSeparateReset() {
		ArrayNode decisions = JsonUtils.array().add("white");
		CurrentAthleteState attempted = CurrentAthleteState.builder(1, "CURRENT_ATHLETE")
		        .fullName("Attempted Athlete").decisions(decisions).showDecisions(true).build();
		ObjectNode next = CurrentAthleteState.builder(2, "CURRENT_ATHLETE")
		        .fullName("Next Athlete").weight(143).showDetails(true).showAthleteClock(true).build().toJson();
		assertFalse(next.path("showDecisions").asBoolean());
		assertEquals(0, next.path("decisions").size());
		assertEquals("Next Athlete", next.path("fullName").asString());
		assertEquals("143", next.path("weight").asString());
		assertTrue(next.path("showAthleteClock").asBoolean());
		assertTrue(attempted.toJson().path("showDecisions").asBoolean());
		assertEquals("white", attempted.toJson().path("decisions").path(0).asString());
	}
}