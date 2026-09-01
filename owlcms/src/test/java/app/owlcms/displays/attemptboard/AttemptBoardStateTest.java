/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import tools.jackson.databind.node.ObjectNode;

public class AttemptBoardStateTest {

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
				.weight(null)
				.build()
				.toJson();

		assertEquals("", json.path("mode").asString());
		assertEquals("", json.path("athleteImg").asString());
		assertEquals("", json.path("firstName").asString());
		assertEquals("", json.path("teamFlagImg").asString());
		assertEquals("", json.path("weight").asString());
		assertEquals("", json.path("recordMessage").asString());
		assertFalse(json.path("decisionVisible").asBoolean());
	}

	@Test
	public void copyPreservesFieldsUnderNewSequence() {
		AttemptBoardState original = AttemptBoardState.builder(5, "CURRENT_ATHLETE")
				.lastName("LAST")
				.weight("190")
				.decisionVisible(true)
				.recordAttempt(true)
				.build();
		ObjectNode json = original.copy(6).recordAttempt(false).build().toJson();

		assertEquals(6, json.path("sequence").asLong());
		assertEquals("CURRENT_ATHLETE", json.path("mode").asString());
		assertEquals("LAST", json.path("lastName").asString());
		assertEquals("190", json.path("weight").asString());
		assertTrue(json.path("decisionVisible").asBoolean());
		assertFalse(json.path("recordAttempt").asBoolean());
	}
}