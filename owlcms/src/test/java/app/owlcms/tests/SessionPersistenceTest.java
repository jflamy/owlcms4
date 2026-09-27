/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;

/**
 * Session edits made in the session editing form must survive saves made from stale copies of the session held by
 * athletes or by the field of play.
 */
public class SessionPersistenceTest {

	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		JPAService.close();
		JPAService.init(true, true);
		Config.initConfig();
		Competition.setCurrent(new Competition());
	}

	@AfterClass
	public static void tearDownTests() {
		Competition.setCurrent(null);
		JPAService.close();
	}

	@Test
	public void doDoneKeepsFormEditsAndSavesRuntimeState() {
		Long id = persistGroup("done-test");
		Group fopCopy = GroupRepository.getById(id);

		Group edited = GroupRepository.getById(id);
		edited.setCleanJerkBreakDuration(5);
		edited.setAnnouncer("Announcer X");
		GroupRepository.save(edited);

		LocalDateTime t = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		fopCopy.setFirstSnatchTime(t, null);
		fopCopy.setFirstCJTime(t.plusMinutes(20), null);
		fopCopy.setLastSnatchDecisionTime(t.plusMinutes(15), fopCopy, null);
		fopCopy.setLastCJDecisionTime(t.plusMinutes(40), fopCopy, null);
		// no weighed-in athletes, so the session becomes done and is persisted
		fopCopy.doDone();

		Group reloaded = GroupRepository.getById(id);
		assertTrue(reloaded.isDone());
		assertEquals(Integer.valueOf(5), reloaded.getCleanJerkBreakDuration());
		assertEquals(Integer.valueOf(5), GroupRepository.getCleanJerkBreakDuration(id));
		assertEquals("Announcer X", reloaded.getAnnouncer());
		assertEquals(t, reloaded.getFirstSnatchTime());
		assertEquals(t.plusMinutes(20), reloaded.getFirstCJTime());
		assertEquals(t.plusMinutes(15), reloaded.getLastSnatchDecisionTime());
		assertEquals(t.plusMinutes(40), reloaded.getLastCJDecisionTime());
	}

	@Test
	public void athleteSaveDoesNotOverwriteSessionEdits() {
		Long groupId = persistGroup("athlete-test");
		Long athleteId = JPAService.runInTransaction(em -> {
			Athlete a = new Athlete();
			a.setGroup(em.find(Group.class, groupId));
			a.setFirstName("Test");
			a.setLastName("Stale");
			a.setGender(Gender.M);
			em.persist(a);
			return a.getId();
		});
		Athlete staleAthlete = JPAService.runInTransaction(em -> em.find(Athlete.class, athleteId));

		Group edited = GroupRepository.getById(groupId);
		edited.setCleanJerkBreakDuration(7);
		GroupRepository.save(edited);

		staleAthlete.setCoach("Coach Y");
		JPAService.runInTransaction(em -> em.merge(staleAthlete));

		assertEquals(Integer.valueOf(7), GroupRepository.getCleanJerkBreakDuration(groupId));
		Athlete reloaded = JPAService.runInTransaction(em -> em.find(Athlete.class, athleteId));
		assertEquals("Coach Y", reloaded.getCoach());
		assertEquals(groupId, reloaded.getGroup().getId());
	}

	private static Long persistGroup(String name) {
		return JPAService.runInTransaction(em -> {
			Group g = new Group(name);
			em.persist(g);
			return g.getId();
		});
	}
}
