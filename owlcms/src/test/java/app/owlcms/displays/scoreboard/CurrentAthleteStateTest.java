package app.owlcms.displays.scoreboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class CurrentAthleteStateTest {

	@Test
	public void decisionPreservesAttemptedAthleteAndWeight() {
		JsonArray athletes = Json.createArray();
		JsonObject athlete = Json.createObject();
		athlete.put("fullName", "Attempted Athlete");
		athletes.set(0, athlete);
		CurrentAthleteState original = CurrentAthleteState.builder(10, "CURRENT_ATHLETE")
		        .fullName("Attempted Athlete").team("Team").startNumber(4)
		        .lift("Snatch #3").weight(142).athletes(athletes)
		        .showAthleteClock(true).showDetails(true).showAttemptResults(true).build();

		JsonObject decision = original.withDecision(11).toJson();
		assertEquals(11, decision.getNumber("sequence"), 0);
		assertEquals("CURRENT_ATHLETE", decision.getString("mode"));
		assertEquals("Attempted Athlete", decision.getString("fullName"));
		assertEquals("Team", decision.getString("team"));
		assertEquals(4, decision.getNumber("startNumber"), 0);
		assertEquals("Snatch #3", decision.getString("lift"));
		assertEquals("142", decision.getString("weight"));
		assertEquals(athletes.toJson(), decision.getArray("athletes").toJson());
		assertTrue(decision.getBoolean("showDecisions"));
		assertFalse(decision.getBoolean("showAthleteClock"));
		assertFalse(decision.getBoolean("showBreakClock"));
		assertFalse(original.toJson().getBoolean("showDecisions"));
	}

	@Test
	public void interruptionContainsCompleteClearedDisplay() {
		JsonObject interruption = CurrentAthleteState.builder(12, "INTERRUPTION")
		        .fullName("Marshal interruption").build().toJson();
		assertEquals("INTERRUPTION", interruption.getString("mode"));
		assertEquals("Marshal interruption", interruption.getString("fullName"));
		assertEquals("", interruption.getString("team"));
		assertEquals("", interruption.getString("lift"));
		assertEquals("", interruption.getString("weight"));
		assertEquals(0, interruption.getArray("athletes").length());
		assertEquals(0, interruption.getArray("snIndicators").length());
		assertEquals(0, interruption.getArray("cjIndicators").length());
		assertEquals(0, interruption.getArray("decisions").length());
		assertFalse(interruption.getBoolean("showDetails"));
		assertFalse(interruption.getBoolean("showAttemptResults"));
		assertFalse(interruption.getBoolean("showDecisions"));
		assertFalse(interruption.getBoolean("showAthleteClock"));
		assertFalse(interruption.getBoolean("showBreakClock"));
	}

	@Test
	public void completionCarriesFinalResultsWithoutActiveAttemptFields() {
		// elemental JsonArray.set does not grow the array; fill sequentially like buildIndicators does
		JsonArray indicators = Json.createArray();
		indicators.set(0, "132");
		indicators.set(1, "137");
		indicators.set(2, "142");
		JsonArray classes = Json.createArray();
		classes.set(0, "white");
		classes.set(1, "white");
		classes.set(2, "white");
		JsonObject done = CurrentAthleteState.builder(13, "SESSION_DONE")
		        .fullName("Session done").cjIndicators(indicators).cjIndicatorClasses(classes)
		        .showAttemptResults(true).build().toJson();
		assertEquals("SESSION_DONE", done.getString("mode"));
		assertEquals("142", done.getArray("cjIndicators").getString(2));
		assertEquals("white", done.getArray("cjIndicatorClasses").getString(2));
		assertTrue(done.getBoolean("showAttemptResults"));
		assertEquals("", done.getString("weight"));
		assertFalse(done.getBoolean("showDetails"));
		assertFalse(done.getBoolean("showDecisions"));
	}

	@Test
	public void snapshotsDoNotShareMutableResultArrays() {
		JsonArray athletes = Json.createArray();
		JsonObject athlete = Json.createObject();
		athlete.put("fullName", "Attempted Athlete");
		athletes.set(0, athlete);
		CurrentAthleteState original = CurrentAthleteState.builder(1, "CURRENT_ATHLETE")
		        .athletes(athletes).build();
		athlete.put("fullName", "Next Athlete");
		JsonObject serialized = original.toJson();
		assertEquals("Attempted Athlete", serialized.getArray("athletes").getObject(0).getString("fullName"));
		serialized.getArray("athletes").getObject(0).put("fullName", "Changed externally");
		assertEquals("Attempted Athlete",
		        original.withDecision(2).toJson().getArray("athletes").getObject(0).getString("fullName"));
	}

	@Test
	public void nextAthleteSnapshotClearsDecisionWithoutSeparateReset() {
		JsonArray decisions = Json.createArray();
		decisions.set(0, "white");
		CurrentAthleteState attempted = CurrentAthleteState.builder(1, "CURRENT_ATHLETE")
		        .fullName("Attempted Athlete").decisions(decisions).showDecisions(true).build();
		JsonObject next = CurrentAthleteState.builder(2, "CURRENT_ATHLETE")
		        .fullName("Next Athlete").weight(143).showDetails(true).showAthleteClock(true).build().toJson();
		assertFalse(next.getBoolean("showDecisions"));
		assertEquals(0, next.getArray("decisions").length());
		assertEquals("Next Athlete", next.getString("fullName"));
		assertEquals("143", next.getString("weight"));
		assertTrue(next.getBoolean("showAthleteClock"));
		assertTrue(attempted.toJson().getBoolean("showDecisions"));
		assertEquals("white", attempted.toJson().getArray("decisions").getString(0));
	}
}