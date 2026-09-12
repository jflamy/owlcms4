/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import elemental.json.JsonObject;

public class AttemptBoardStateTest {

	@Test
	public void emptyWeightFeedbackMatchesBrowserWhitespaceTrimming() {
		AttemptBoardRenderCheck check = new AttemptBoardRenderCheck();
		check.published(AttemptBoardState.builder(1, "WAIT").build(), "\u00a0kg", 0);
		assertNull(check.rendered("1", "", "0", "kg", "WAIT", false, 100));
		assertNull(check.timeout(6000));
		check.published(AttemptBoardState.builder(2, "WAIT").build(), "\ufeff\u202fkg\u00a0\n", 7000);
		assertNull(check.rendered("2", "", "0", "kg", "WAIT", false, 7100));
	}

	@Test
	public void renderFeedbackPreservesInternalNonbreakingSpace() {
		AttemptBoardRenderCheck check = new AttemptBoardRenderCheck();
		AttemptBoardState state = AttemptBoardState.builder(1, "CURRENT_ATHLETE").startNumber(14).weight("64").build();
		check.published(state, "\u00a0kg", 0);
		assertNull(check.rendered("1", "64", "14", "64\u00a0kg", "CURRENT_ATHLETE", true, 100));
		check.published(state.copy(2).build(), "\u00a0kg", 200);
		assertNotNull(check.rendered("2", "64", "14", "64kg", "CURRENT_ATHLETE", true, 300));
		check.published(state.copy(3).build(), "\u00a0kg", 400);
		assertNotNull(check.rendered("3", "64", "14", "63\u00a0kg", "CURRENT_ATHLETE", true, 500));
	}

	@Test
	public void renderAcknowledgementAcceptsCoalescedSnapshotsAndUnitSuffix() {
		AttemptBoardRenderCheck check = new AttemptBoardRenderCheck();
		check.published(AttemptBoardState.builder(1, "CURRENT_ATHLETE").startNumber(14).weight("63").build(), "kg", 0);
		check.published(AttemptBoardState.builder(2, "CURRENT_ATHLETE").startNumber(14).weight("64").build(), "kg", 100);
		assertNull(check.rendered("2", "64", "14", "64kg", "CURRENT_ATHLETE", true, 200));
		assertNull(check.timeout(6000));
		assertNull(check.rendered("1", "63", "14", "63kg", "CURRENT_ATHLETE", true, 6100));
	}

	@Test
	public void renderAcknowledgementReportsWrongTextAndVisibility() {
		AttemptBoardRenderCheck check = new AttemptBoardRenderCheck();
		AttemptBoardState state = AttemptBoardState.builder(1, "CURRENT_ATHLETE").startNumber(14).weight("64").build();
		check.published(state, "kg", 0);
		assertNotNull(check.rendered("1", "64", "14", "63kg", "CURRENT_ATHLETE", true, 100));
		check.published(state.copy(2).build(), "kg", 200);
		assertNotNull(check.rendered("2", "64", "14", "64kg", "CURRENT_ATHLETE", false, 300));
	}

	@Test
	public void missingRenderFeedbackTimesOutDespiteNewPublications() {
		AttemptBoardRenderCheck check = new AttemptBoardRenderCheck();
		AttemptBoardState state = AttemptBoardState.builder(1, "CURRENT_ATHLETE").weight("64").build();
		check.published(state, "kg", 0);
		check.published(state.copy(2).build(), "kg", 4900);
		assertNull(check.timeout(4999));
		assertNotNull(check.timeout(5000));
		assertNull(check.timeout(6000));
		check.published(state.copy(3).build(), "kg", 7000);
		check.clear();
		assertNull(check.timeout(13000));
	}

	@Test
	public void serializesCompleteSnapshot() {
		JsonObject json = AttemptBoardState.builder(42, "CURRENT_ATHLETE")
				.athleteImg("athlete")
				.attempt("second attempt")
				.breakType("TECHNICAL")
				.category("M89")
				.competitionName("Competition")
				.decisionVisible(true)
				.firstName("First")
				.firstNameSizeOverride("first-size")
				.lastName("LAST")
				.nameSizeOverride("last-size")
				.platesHtml("<div class=\"loadChart\">plates</div>")
				.recordAttempt(true)
				.recordBroken(false)
				.recordMessage("Record attempt")
				.recordMessageSpeed(10)
				.startNumber(7)
				.teamFlagImg("flag")
				.teamName("Team")
				.weight("190")
				.build()
				.toJson();

		assertEquals(42, json.getNumber("sequence"), 0);
		assertEquals("CURRENT_ATHLETE", json.getString("mode"));
		assertEquals("TECHNICAL", json.getString("breakType"));
		assertEquals("Competition", json.getString("competitionName"));
		assertEquals("LAST", json.getString("lastName"));
		assertEquals("First", json.getString("firstName"));
		assertEquals("Team", json.getString("teamName"));
		assertEquals("flag", json.getString("teamFlagImg"));
		assertEquals("athlete", json.getString("athleteImg"));
		assertEquals("M89", json.getString("category"));
		assertEquals(7, json.getNumber("startNumber"), 0);
		assertTrue(json.getBoolean("decisionVisible"));
		assertEquals("second attempt", json.getString("attempt"));
		assertEquals("190", json.getString("weight"));
		assertEquals("<div class=\"loadChart\">plates</div>", json.getString("platesHtml"));
		assertTrue(json.getBoolean("recordAttempt"));
		assertFalse(json.getBoolean("recordBroken"));
		assertEquals("Record attempt", json.getString("recordMessage"));
		assertEquals(10, json.getNumber("recordMessageSpeed"), 0);
		assertEquals("last-size", json.getString("nameSizeOverride"));
		assertEquals("first-size", json.getString("firstNameSizeOverride"));
	}

	@Test
	public void nullStringsAreSerializedAsEmptyStrings() {
		JsonObject json = AttemptBoardState.builder(1, null)
				.athleteImg(null)
				.firstName(null)
				.teamFlagImg(null)
				.platesHtml(null)
				.weight(null)
				.build()
				.toJson();

		assertEquals("", json.getString("mode"));
		assertEquals("", json.getString("athleteImg"));
		assertEquals("", json.getString("firstName"));
		assertEquals("", json.getString("teamFlagImg"));
		assertEquals("", json.getString("weight"));
		assertEquals("", json.getString("platesHtml"));
		assertEquals("", json.getString("recordMessage"));
		assertFalse(json.getBoolean("decisionVisible"));
	}

	@Test
	public void copyPreservesFieldsUnderNewSequence() {
		AttemptBoardState original = AttemptBoardState.builder(5, "CURRENT_ATHLETE")
				.lastName("LAST")
				.weight("190")
				.platesHtml("attempted plates")
				.decisionVisible(true)
				.recordAttempt(true)
				.build();
		JsonObject json = original.copy(6).recordAttempt(false).build().toJson();

		assertEquals(6, json.getNumber("sequence"), 0);
		assertEquals("CURRENT_ATHLETE", json.getString("mode"));
		assertEquals("LAST", json.getString("lastName"));
		assertEquals("190", json.getString("weight"));
		assertEquals("attempted plates", json.getString("platesHtml"));
		assertTrue(json.getBoolean("decisionVisible"));
		assertFalse(json.getBoolean("recordAttempt"));
	}

	@Test
	public void handoffSnapshotsAreSelfContained() {
		AttemptBoardState current = AttemptBoardState.builder(1, "CURRENT_ATHLETE")
				.lastName("ATTEMPTED").weight("190").platesHtml("attempted plates").build();
		JsonObject decision = current.copy(2).decisionVisible(true).build().toJson();
		assertEquals("ATTEMPTED", decision.getString("lastName"));
		assertEquals("190", decision.getString("weight"));
		assertEquals("attempted plates", decision.getString("platesHtml"));
		assertTrue(decision.getBoolean("decisionVisible"));
		assertFalse(current.toJson().getBoolean("decisionVisible"));

		JsonObject next = AttemptBoardState.builder(3, "CURRENT_ATHLETE")
				.lastName("NEXT").weight("191").platesHtml("next plates").build().toJson();
		assertFalse(next.getBoolean("decisionVisible"));
		assertEquals("191", next.getString("weight"));
		assertEquals("next plates", next.getString("platesHtml"));

		JsonObject waiting = AttemptBoardState.builder(4, "WAIT").build().toJson();
		assertFalse(waiting.getBoolean("decisionVisible"));
		assertEquals("", waiting.getString("weight"));
		assertEquals("", waiting.getString("platesHtml"));
	}
}