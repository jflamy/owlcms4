/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Test;

import app.owlcms.spreadsheet.RAthlete;

public class RAthleteTest {

	@Test
	public void testAppendMembershipMarkersUsesShortTokens() {
		assertEquals("M 73", RAthlete.appendMembershipMarkers("M 73", true, false));
		assertEquals("M 73/-T", RAthlete.appendMembershipMarkers("M 73", false, false));
		assertEquals("M 73/+MT", RAthlete.appendMembershipMarkers("M 73", true, true));
		assertEquals("M 73/-T,+MT", RAthlete.appendMembershipMarkers("M 73", false, true));
	}

	@Test
	public void testParseParticipationSpecAcceptsShortTokens() throws Exception {
		Object spec = parseParticipationSpec("M 73/-T,+MT");

		assertEquals("M 73", readField(spec, "categoryName"));
		assertFalse((Boolean) readField(spec, "teamMember"));
		assertTrue((Boolean) readField(spec, "mixedTeamMember"));
	}

	@Test
	public void testParseParticipationSpecAcceptsLegacyTokens() throws Exception {
		Object spec = parseParticipationSpec("M 73/yEsTeAm,nOmIxEd");

		assertEquals("M 73", readField(spec, "categoryName"));
		assertTrue((Boolean) readField(spec, "teamMember"));
		assertFalse((Boolean) readField(spec, "mixedTeamMember"));
	}

	private Object parseParticipationSpec(String entry) throws Exception {
		RAthlete athlete = new RAthlete();
		Method method = RAthlete.class.getDeclaredMethod("parseParticipationSpec", String.class);
		method.setAccessible(true);
		return method.invoke(athlete, entry);
	}

	private Object readField(Object target, String fieldName) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(target);
	}
}