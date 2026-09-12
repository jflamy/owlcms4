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

import tools.jackson.databind.node.ObjectNode;

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
		ObjectNode json = AttemptBoardState.builder(42, "CURRENT_ATHLETE")
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

		assertEquals(42, json.path("sequence").asLong());
		assertEquals("CURRENT_ATHLETE", json.path("mode").asString());
		assertEquals("TECHNICAL", json.path("breakType").asString());
		assertEquals("Competition", json.path("competitionName").asString());
		assertEquals("LAST", json.path("lastName").asString());
		assertEquals("First", json.path("firstName").asString());
		assertEquals("Team", json.path("teamName").asString());
		assertEquals("flag", json.path("teamFlagImg").asString());
		assertEquals("athlete", json.path("athleteImg").asString());
		assertEquals("M89", json.path("category").asString());
		assertEquals(7, json.path("startNumber").asInt());
		assertTrue(json.path("decisionVisible").asBoolean());
		assertEquals("second attempt", json.path("attempt").asString());
		assertEquals("190", json.path("weight").asString());
		assertEquals("<div class=\"loadChart\">plates</div>", json.path("platesHtml").asString());
		assertTrue(json.path("recordAttempt").asBoolean());
		assertFalse(json.path("recordBroken").asBoolean());
		assertEquals("Record attempt", json.path("recordMessage").asString());
		assertEquals(10, json.path("recordMessageSpeed").asInt());
		assertEquals("last-size", json.path("nameSizeOverride").asString());
		assertEquals("first-size", json.path("firstNameSizeOverride").asString());
	}

	@Test
	public void nullStringsAreSerializedAsEmptyStrings() {
		ObjectNode json = AttemptBoardState.builder(1, null)
				.athleteImg(null)
				.firstName(null)
				.teamFlagImg(null)
				.platesHtml(null)
				.weight(null)
				.build()
				.toJson();

		assertEquals("", json.path("mode").asString());
		assertEquals("", json.path("athleteImg").asString());
		assertEquals("", json.path("firstName").asString());
		assertEquals("", json.path("teamFlagImg").asString());
		assertEquals("", json.path("weight").asString());
		assertEquals("", json.path("platesHtml").asString());
		assertEquals("", json.path("recordMessage").asString());
		assertFalse(json.path("decisionVisible").asBoolean());
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
		ObjectNode json = original.copy(6).recordAttempt(false).build().toJson();

		assertEquals(6, json.path("sequence").asLong());
		assertEquals("CURRENT_ATHLETE", json.path("mode").asString());
		assertEquals("LAST", json.path("lastName").asString());
		assertEquals("190", json.path("weight").asString());
		assertEquals("attempted plates", json.path("platesHtml").asString());
		assertTrue(json.path("decisionVisible").asBoolean());
		assertFalse(json.path("recordAttempt").asBoolean());
	}

	@Test
	public void handoffSnapshotsAreSelfContained() {
		AttemptBoardState current = AttemptBoardState.builder(1, "CURRENT_ATHLETE")
				.lastName("ATTEMPTED").weight("190").platesHtml("attempted plates").build();
		ObjectNode decision = current.copy(2).decisionVisible(true).build().toJson();
		assertEquals("ATTEMPTED", decision.path("lastName").asString());
		assertEquals("190", decision.path("weight").asString());
		assertEquals("attempted plates", decision.path("platesHtml").asString());
		assertTrue(decision.path("decisionVisible").asBoolean());
		assertFalse(current.toJson().path("decisionVisible").asBoolean());

		ObjectNode next = AttemptBoardState.builder(3, "CURRENT_ATHLETE")
				.lastName("NEXT").weight("191").platesHtml("next plates").build().toJson();
		assertFalse(next.path("decisionVisible").asBoolean());
		assertEquals("191", next.path("weight").asString());
		assertEquals("next plates", next.path("platesHtml").asString());

		ObjectNode waiting = AttemptBoardState.builder(4, "WAIT").build().toJson();
		assertFalse(waiting.path("decisionVisible").asBoolean());
		assertEquals("", waiting.path("weight").asString());
		assertEquals("", waiting.path("platesHtml").asString());
	}
}