/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.access.Principal;
import app.owlcms.access.Principal.AuthSource;
import app.owlcms.access.Role;
import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;

public class AthleteEditAccessTest {
	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		Competition.setCurrent(new Competition());
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	private static Principal account(String loginPlatform, RoleGrant... grants) {
		return new Principal(AuthSource.ACCOUNT, 1L, "user", List.of(grants), loginPlatform);
	}

	private static Principal account(Role role) {
		return account(null, new RoleGrant(role, null));
	}

	private static Athlete athleteOn(String platformName) {
		Athlete athlete = new Athlete();
		if (platformName != null) {
			Group session = new Group("S");
			session.setPlatform(new Platform(platformName));
			athlete.setGroup(session);
		}
		return athlete;
	}

	@Test
	public void fullRegistrationOnResultsPagesRequiresPreparation() {
		for (Role role : List.of(Role.PREPARATION, Role.COMPETITION_DIRECTOR, Role.COMPETITION_SUPERVISOR,
		        Role.ADMIN)) {
			assertTrue(role.name(), AthleteEditAccess.registration(account(role), false, true));
		}
		for (Role role : List.of(Role.REGISTRATION, Role.WEIGHIN, Role.RESULTS, Role.EXTERNAL, Role.MARSHAL)) {
			assertFalse(role.name(), AthleteEditAccess.registration(account(role), false, true));
		}
	}

	@Test
	public void dedicatedPagesKeepTheirOwnFullEditingPermission() {
		assertTrue(AthleteEditAccess.registration(account(Role.REGISTRATION), false, false));
		assertFalse("preparation alone does not edit on the registration page",
		        AthleteEditAccess.registration(account(Role.PREPARATION), false, false));
		assertFalse(AthleteEditAccess.registration(account(Role.WEIGHIN), false, false));
		assertTrue(AthleteEditAccess.registration(account(Role.WEIGHIN), true, false));
		assertFalse(AthleteEditAccess.registration(account(Role.REGISTRATION), true, false));
	}

	@Test
	public void metadataIsEditableThroughItsOwnPermission() {
		for (Role role : List.of(Role.PREPARATION, Role.REGISTRATION, Role.WEIGHIN, Role.COMPETITION_DIRECTOR,
		        Role.COMPETITION_SUPERVISOR, Role.ADMIN)) {
			assertTrue(role.name(), AthleteEditAccess.metadata(account(role)));
		}
		for (Role role : List.of(Role.RESULTS, Role.EXTERNAL, Role.MARSHAL, Role.ANNOUNCER)) {
			assertFalse(role.name(), AthleteEditAccess.metadata(account(role)));
		}
	}

	@Test
	public void cardEditingFollowsTheAthletesScheduledPlatformWhenNoPlatformHoldsTheSession() {
		Principal marshalA = account(null, new RoleGrant(Role.MARSHAL, "A"));
		assertTrue(AthleteEditAccess.card(marshalA, athleteOn("A")));
		assertFalse(AthleteEditAccess.card(marshalA, athleteOn("B")));
	}

	@Test
	public void scopedCardEditingIsDeniedWhenNoPlatformIsIdentifiable() {
		Principal marshalA = account(null, new RoleGrant(Role.MARSHAL, "A"));
		assertFalse(AthleteEditAccess.card(marshalA, athleteOn(null)));
		Principal lockedToA = account("A", new RoleGrant(Role.MARSHAL, null));
		assertFalse("the operator's own platform does not authorize an unrelated athlete",
		        AthleteEditAccess.card(lockedToA, athleteOn(null)));
		assertFalse(AthleteEditAccess.card(lockedToA, athleteOn("B")));
		assertTrue(AthleteEditAccess.card(lockedToA, athleteOn("A")));
	}

	@Test
	public void globalAccountsEditCardsOnAnyPlatform() {
		for (Role role : List.of(Role.PREPARATION, Role.COMPETITION_SUPERVISOR, Role.ADMIN)) {
			assertTrue(role.name(), AthleteEditAccess.card(account(role), athleteOn("B")));
			assertTrue(role.name(), AthleteEditAccess.card(account(role), athleteOn(null)));
		}
	}

	@Test
	public void readOnlyRolesNeverEditTheCard() {
		for (Role role : List.of(Role.RESULTS, Role.EXTERNAL, Role.COMPETITION_DIRECTOR, Role.REGISTRATION,
		        Role.WEIGHIN, Role.TC, Role.TIMEKEEPER)) {
			assertFalse(role.name(), AthleteEditAccess.card(account(role), athleteOn("A")));
		}
	}
}
