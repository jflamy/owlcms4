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

import elemental.json.JsonObject;

public class AttemptBoardStateTest {

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
				.weight(null)
				.build()
				.toJson();

		assertEquals("", json.getString("mode"));
		assertEquals("", json.getString("athleteImg"));
		assertEquals("", json.getString("firstName"));
		assertEquals("", json.getString("teamFlagImg"));
		assertEquals("", json.getString("weight"));
		assertEquals("", json.getString("recordMessage"));
		assertFalse(json.getBoolean("decisionVisible"));
	}

	@Test
	public void copyPreservesFieldsUnderNewSequence() {
		AttemptBoardState original = AttemptBoardState.builder(5, "CURRENT_ATHLETE")
				.lastName("LAST")
				.weight("190")
				.decisionVisible(true)
				.recordAttempt(true)
				.build();
		JsonObject json = original.copy(6).recordAttempt(false).build().toJson();

		assertEquals(6, json.getNumber("sequence"), 0);
		assertEquals("CURRENT_ATHLETE", json.getString("mode"));
		assertEquals("LAST", json.getString("lastName"));
		assertEquals("190", json.getString("weight"));
		assertTrue(json.getBoolean("decisionVisible"));
		assertFalse(json.getBoolean("recordAttempt"));
	}
}