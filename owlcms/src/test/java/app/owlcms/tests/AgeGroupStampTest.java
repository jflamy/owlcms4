/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.v2.CompetitionDataV2;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.JpaTransactions;
import app.owlcms.data.jpa.StaleSingletonException;
import app.owlcms.data.jpa.StaleSingletonException.Kind;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stale-save check on age groups (compliance item 200, phase 1). No UI objects.
 */
public class AgeGroupStampTest {

	private static final AuditOrigin ALICE = origin("alice");
	private static final AuditOrigin BOB = origin("bob");
	private static final AtomicInteger COUNTER = new AtomicInteger();

	private static AuditOrigin origin(String user) {
		return new AuditOrigin(new AuditActor("ACCOUNTS", user, "PREPARATION", null, true, "-", "-"), "test");
	}

	@BeforeClass
	public static void setUpDatabase() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void closeDatabase() {
		JPAService.close();
	}

	@Before
	public void competitionExists() {
		if (CompetitionRepository.findAll().isEmpty()) {
			CompetitionRepository.create(new Competition(), AuditOrigin.system("test"));
		}
	}

	/** A stored age group with one category, stamped at creation. */
	private static Long newAgeGroup() {
		AgeGroup ageGroup = new AgeGroup("STAMP" + COUNTER.incrementAndGet(), true, 0, 999, Gender.M, "U", 0);
		ageGroup.setChampionshipName("StampTest");
		ageGroup.addCategory(new Category(0.0, 61.0, Gender.M, true, 0, 0, 0, ageGroup, 0));
		AgeGroupRepository.add(ageGroup, AuditOrigin.system("test"));
		assertNotNull("fixture age group was not stored", AgeGroupRepository.loadForEdit(ageGroup.getId()));
		return ageGroup.getId();
	}

	private static Integer storedMaxAge(Long id) {
		return AgeGroupRepository.loadForEdit(id).getMaxAge();
	}

	private static Integer setMaxAge(Long id, Long draftStamp, AuditOrigin who, int maxAge) {
		return AgeGroupRepository.write(id, draftStamp, who, (em, row) -> {
			row.setMaxAge(maxAge);
			return maxAge;
		});
	}

	@Test
	public void secondDraftIsRefusedAndWritesNothing() {
		Long id = newAgeGroup();
		AgeGroup first = AgeGroupRepository.loadForEdit(id);
		AgeGroup second = AgeGroupRepository.loadForEdit(id);
		assertNotNull("creation stamps the row", first.getLastSavedAt());

		setMaxAge(id, first.getLastSavedAt(), ALICE, 30);
		AgeGroup afterFirst = AgeGroupRepository.loadForEdit(id);
		assertNotEquals(first.getLastSavedAt(), afterFirst.getLastSavedAt());

		AtomicBoolean ran = new AtomicBoolean();
		StaleSingletonException stale = assertThrows(StaleSingletonException.class,
		        () -> AgeGroupRepository.write(id, second.getLastSavedAt(), BOB, (em, row) -> {
			        ran.set(true);
			        row.setMaxAge(40);
			        return null;
		        }));
		assertEquals(Kind.AGE_GROUP, stale.getKind());
		assertFalse("the work must not run on a stale draft", ran.get());

		AgeGroup stored = AgeGroupRepository.loadForEdit(id);
		assertEquals(Integer.valueOf(30), stored.getMaxAge());
		assertEquals(afterFirst.getLastSavedAt(), stored.getLastSavedAt());
		assertEquals(afterFirst.getLastSavedBy(), stored.getLastSavedBy());
	}

	@Test
	public void staleMessageNamesTheFirstSaver() {
		Long id = newAgeGroup();
		AgeGroup first = AgeGroupRepository.loadForEdit(id);
		AgeGroup second = AgeGroupRepository.loadForEdit(id);
		setMaxAge(id, first.getLastSavedAt(), ALICE, 30);

		StaleSingletonException stale = assertThrows(StaleSingletonException.class,
		        () -> setMaxAge(id, second.getLastSavedAt(), BOB, 40));
		assertEquals("alice@PREPARATION", stale.getSavedBy());
	}

	@Test
	public void writeToAnotherAgeGroupDoesNotRefuseTheDraft() {
		Long id = newAgeGroup();
		Long other = newAgeGroup();
		AgeGroup draft = AgeGroupRepository.loadForEdit(id);
		AgeGroup otherDraft = AgeGroupRepository.loadForEdit(other);

		setMaxAge(other, otherDraft.getLastSavedAt(), BOB, 50);
		setMaxAge(id, draft.getLastSavedAt(), ALICE, 30);
		assertEquals(Integer.valueOf(30), storedMaxAge(id));
		assertEquals(Integer.valueOf(50), storedMaxAge(other));
	}

	@Test
	public void draftWhoseRowWasDeletedIsRefusedWithoutInsertingARow() {
		Long id = newAgeGroup();
		AgeGroup draft = AgeGroupRepository.loadForEdit(id);
		JpaTransactions.inTransaction(em -> {
			em.remove(em.find(AgeGroup.class, id));
			return null;
		});
		assertNull(AgeGroupRepository.loadForEdit(id));

		StaleSingletonException stale = assertThrows(StaleSingletonException.class,
		        () -> setMaxAge(id, draft.getLastSavedAt(), ALICE, 30));
		assertEquals(Kind.AGE_GROUP, stale.getKind());
		assertNull(stale.getSavedBy());
		assertNull("no row may be created", AgeGroupRepository.loadForEdit(id));
	}

	@Test
	public void stampsStrictlyIncrease() {
		Long id = newAgeGroup();
		Long created = AgeGroupRepository.loadForEdit(id).getLastSavedAt();
		setMaxAge(id, created, ALICE, 30);
		Long first = AgeGroupRepository.loadForEdit(id).getLastSavedAt();
		setMaxAge(id, first, ALICE, 31);
		Long second = AgeGroupRepository.loadForEdit(id).getLastSavedAt();
		assertTrue(first > created);
		assertTrue(second > first);
	}

	@Test
	public void assertCurrentIsAPreflightOnly() {
		Long id = newAgeGroup();
		AgeGroup draft = AgeGroupRepository.loadForEdit(id);

		AgeGroupRepository.assertCurrent(id, draft.getLastSavedAt());
		assertEquals("preflight reserves and stamps nothing", draft.getLastSavedAt(),
		        AgeGroupRepository.loadForEdit(id).getLastSavedAt());

		// becomes stale after the preflight passed: the transactional check still refuses it
		setMaxAge(id, draft.getLastSavedAt(), BOB, 50);
		assertThrows(StaleSingletonException.class, () -> AgeGroupRepository.assertCurrent(id, draft.getLastSavedAt()));
		StaleSingletonException stale = assertThrows(StaleSingletonException.class,
		        () -> setMaxAge(id, draft.getLastSavedAt(), ALICE, 30));
		assertEquals("bob@PREPARATION", stale.getSavedBy());
		assertEquals(Integer.valueOf(50), storedMaxAge(id));
	}

	@Test
	public void assertCurrentRefusesAMissingRow() {
		Long id = newAgeGroup();
		AgeGroup draft = AgeGroupRepository.loadForEdit(id);
		JpaTransactions.inTransaction(em -> {
			em.remove(em.find(AgeGroup.class, id));
			return null;
		});
		assertThrows(StaleSingletonException.class, () -> AgeGroupRepository.assertCurrent(id, draft.getLastSavedAt()));
	}

	@Test
	public void aRowWithoutStampIsWrittenOnceThenRefused() {
		Long id = newAgeGroup();
		JpaTransactions.inTransaction(em -> em.createNativeQuery("update AgeGroup set lastSavedAt = null where id = ?1")
		        .setParameter(1, id).executeUpdate());
		AgeGroup legacy = AgeGroupRepository.loadForEdit(id);
		assertNull(legacy.getLastSavedAt());

		AgeGroupRepository.assertCurrent(id, null);
		setMaxAge(id, null, ALICE, 30);
		assertNotNull(AgeGroupRepository.loadForEdit(id).getLastSavedAt());

		assertThrows(StaleSingletonException.class, () -> setMaxAge(id, null, BOB, 40));
		assertEquals(Integer.valueOf(30), storedMaxAge(id));
	}

	@Test
	public void rowRemovedByTheCallbackIsNotStamped() {
		Long id = newAgeGroup();
		AgeGroup draft = AgeGroupRepository.loadForEdit(id);
		AgeGroupRepository.write(id, draft.getLastSavedAt(), ALICE, (em, row) -> {
			em.remove(row);
			return null;
		});
		assertNull(AgeGroupRepository.loadForEdit(id));
	}

	@Test
	public void stampsAreNotExported() throws Exception {
		Long id = newAgeGroup();
		AgeGroup row = AgeGroupRepository.loadForEdit(id);
		assertNotNull(row.getLastSavedAt());
		assertNotNull(row.getLastSavedBy());

		// V1: plain Jackson on the entity
		String v1 = JsonMapper.builder().build().writeValueAsString(row);
		assertTrue("the age group is serialized", v1.contains(row.getCode()));
		assertFalse(v1.contains("lastSavedAt"));
		assertFalse(v1.contains("lastSavedBy"));

		// V2: the full export, which maps age groups through the V2 mixins
		try (InputStream export = new CompetitionDataV2().fromDatabase().exportData()) {
			String v2 = new String(export.readAllBytes(), StandardCharsets.UTF_8);
			assertTrue("the age group is exported", v2.contains(row.getCode()));
			assertFalse(v2.contains("lastSavedAt"));
			assertFalse(v2.contains("lastSavedBy"));
		}
	}
}
