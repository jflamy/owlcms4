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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.spreadsheet.RAthlete;

public class RAthleteTest {

	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	@After
	public void resetExplicitTeams() {
		setExplicitTeams(false);
	}

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

	@Test
	public void testParseParticipationSpecDefaultsToTeamMemberWhenImplicit() throws Exception {
		assertTrue((Boolean) readField(parseParticipationSpec("M 73"), "teamMember"));
		assertTrue((Boolean) readField(parseParticipationSpec("M 73/+MT"), "teamMember"));
	}

	@Test
	public void testParseParticipationSpecDefaultsToNoTeamWhenExplicitTeams() throws Exception {
		setExplicitTeams(true);

		assertFalse((Boolean) readField(parseParticipationSpec("M 73"), "teamMember"));
		assertFalse((Boolean) readField(parseParticipationSpec("M 73/+MT"), "teamMember"));
		assertTrue((Boolean) readField(parseParticipationSpec("M 73/+T"), "teamMember"));
		assertTrue((Boolean) readField(parseParticipationSpec("M 73/YesTeam,+MT"), "teamMember"));
		assertFalse((Boolean) readField(parseParticipationSpec("M 73/-T"), "teamMember"));
	}

	@Test
	public void testMembershipMarkersRoundTripWithEitherSwitchSetting() throws Exception {
		for (boolean explicitTeams : new boolean[] { false, true }) {
			setExplicitTeams(explicitTeams);
			for (boolean teamMember : new boolean[] { false, true }) {
				for (boolean mixedTeamMember : new boolean[] { false, true }) {
					String exported = RAthlete.appendMembershipMarkers("M 73", teamMember, mixedTeamMember);
					Object imported = parseParticipationSpec(exported);
					assertEquals(exported, "M 73", readField(imported, "categoryName"));
					assertEquals(exported, teamMember, readField(imported, "teamMember"));
					assertEquals(exported, mixedTeamMember, readField(imported, "mixedTeamMember"));
				}
			}
		}
	}

	@Test
	public void testExplicitExportIncludesTeamOptIn() {
		setExplicitTeams(true);
		assertEquals("M 73/+T", RAthlete.appendMembershipMarkers("M 73", true, false));
		assertEquals("M 73/+T,+MT", RAthlete.appendMembershipMarkers("M 73", true, true));
	}

	private static void setExplicitTeams(boolean enabled) {
		Config config = Config.getCurrent();
		config.setFeatureSwitchValue(FeatureSwitch.EXPLICIT_TEAMS, enabled);
		Config.setCurrent(config);
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