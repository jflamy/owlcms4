/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.export.FormatDetector;
import app.owlcms.data.export.v2.CompetitionDataV2;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.JpaTransactions;
import app.owlcms.data.jpa.SingletonSaves;
import app.owlcms.data.jpa.StaleSingletonException;
import app.owlcms.data.jpa.StaleSingletonException.Kind;
import app.owlcms.data.records.RecordConfig;

/**
 * Stale-save check on the configuration singletons (compliance item 136). No UI objects.
 */
public class SingletonStaleSaveTest {

	private static final AuditOrigin FIRST = AuditOrigin.system("first");
	private static final AuditOrigin SECOND = AuditOrigin.system("second");

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
			CompetitionRepository.create(new Competition(), FIRST);
		}
	}

	@Test
	public void configSecondDraftIsRefusedAndWritesNothing() {
		Config first = ConfigRepository.loadForEdit();
		Config second = ConfigRepository.loadForEdit();
		first.setDefaultPaperSize("A4");
		ConfigRepository.saveEdited(first, AuditOrigin.system("first saver"));

		second.setDefaultPaperSize("LETTER");
		StaleSingletonException stale = assertThrows(StaleSingletonException.class,
		        () -> ConfigRepository.saveEdited(second, SECOND));
		assertEquals(Kind.CONFIG, stale.getKind());
		assertEquals("system", stale.getSavedBy());
		assertEquals("A4", ConfigRepository.loadForEdit().getDefaultPaperSize());
		assertEquals("A4", Config.getCurrent().getDefaultPaperSize());
	}

	@Test
	public void applyMakesAnOpenDraftStale() {
		Config draft = ConfigRepository.loadForEdit();
		ConfigRepository.apply(c -> c.setIpDisplayList("10.0.0.1"), FIRST);
		draft.setIpDisplayList("10.0.0.2");
		assertThrows(StaleSingletonException.class, () -> ConfigRepository.saveEdited(draft, SECOND));
		assertEquals("10.0.0.1", Config.getCurrent().getIpDisplayList());
	}

	@Test
	public void aFormThatStaysOpenCanSaveAgain() {
		Config draft = ConfigRepository.loadForEdit();
		draft.setIpAccessList("10.0.0.3");
		ConfigRepository.saveEdited(draft, FIRST);
		draft.setIpAccessList("10.0.0.4");
		ConfigRepository.saveEdited(draft, FIRST);
		assertEquals("10.0.0.4", ConfigRepository.loadForEdit().getIpAccessList());
	}

	@Test
	public void stampsStrictlyIncrease() {
		long first = ConfigRepository.apply(c -> c.setIpAccessList("a"), FIRST).getLastSavedAt();
		long second = ConfigRepository.apply(c -> c.setIpAccessList("b"), FIRST).getLastSavedAt();
		assertTrue(second > first);
	}

	@Test
	public void aRowNeverStampedSavesNormally() {
		JpaTransactions.inTransaction(em -> em.createNativeQuery("update Config set lastSavedAt = null")
		        .executeUpdate());
		Config draft = ConfigRepository.loadForEdit();
		draft.setIpAccessList("10.0.0.5");
		Config saved = ConfigRepository.saveEdited(draft, FIRST);
		assertNotNull(saved.getLastSavedAt());
	}

	@Test
	public void runtimeForwardingActivityIsKeptByOtherSaves() {
		Config.getCurrent().setManagedForwardingActivity(Map.of("ws://tracker", Boolean.FALSE));
		ConfigRepository.apply(c -> c.setIpAccessList("c"), FIRST);
		assertEquals(Map.of("ws://tracker", Boolean.FALSE), Config.getCurrent().getManagedForwardingActivity());

		Config draft = ConfigRepository.loadForEdit();
		ConfigRepository.saveEdited(draft, FIRST);
		assertEquals(Map.of("ws://tracker", Boolean.FALSE), Config.getCurrent().getManagedForwardingActivity());
		Config.getCurrent().setManagedForwardingActivity(Map.of());
	}

	@Test
	public void competitionSecondDraftIsRefused() {
		Competition first = CompetitionRepository.loadForEdit();
		Competition second = CompetitionRepository.loadForEdit();
		first.setCompetitionCity("Montréal");
		CompetitionRepository.saveEdited(first, FIRST);

		second.setCompetitionCity("Québec");
		StaleSingletonException stale = assertThrows(StaleSingletonException.class,
		        () -> CompetitionRepository.saveEdited(second, SECOND));
		assertEquals(Kind.COMPETITION, stale.getKind());
		assertEquals("Montréal", Competition.getCurrent().getCompetitionCity());
	}

	@Test
	public void competitionDraftWhoseRowIsGoneIsRefusedWithoutInsertingAnotherRow() {
		Competition draft = CompetitionRepository.loadForEdit();
		JpaTransactions.inTransaction(em -> em.createQuery("delete from Competition").executeUpdate());
		assertThrows(StaleSingletonException.class, () -> CompetitionRepository.saveEdited(draft, SECOND));
		assertTrue(CompetitionRepository.findAll().isEmpty());
	}

	@Test
	public void recordConfigSecondDraftIsRefused() {
		RecordConfig first = RecordConfig.loadForEdit();
		RecordConfig second = RecordConfig.loadForEdit();
		first.setShowAllFederations(true);
		RecordConfig.saveEdited(first, FIRST);

		second.setShowAllFederations(false);
		StaleSingletonException stale = assertThrows(StaleSingletonException.class,
		        () -> RecordConfig.saveEdited(second, SECOND));
		assertEquals(Kind.RECORD_CONFIG, stale.getKind());
		assertTrue(RecordConfig.getCurrent().getShowAllFederations());
	}

	@Test
	public void addMissingSavesOnlyWhenTheOrderChanges() {
		RecordConfig.apply(rc -> rc.setRecordOrder(new ArrayList<>(List.of("A", "B"))), FIRST);
		RecordConfig loaded = RecordConfig.loadForEdit();

		RecordConfig unchanged = loaded.addMissing(new ArrayList<>(List.of("A", "B")));
		assertSame(loaded, unchanged);
		assertEquals(loaded.getLastSavedAt(), RecordConfig.loadForEdit().getLastSavedAt());

		RecordConfig changed = loaded.addMissing(new ArrayList<>(List.of("B", "C")));
		assertEquals(List.of("B", "C"), changed.getRecordOrder());
		assertNotEquals(loaded.getLastSavedAt(), changed.getLastSavedAt());
		assertThrows("the copy taken before addMissing saved is stale", StaleSingletonException.class,
		        () -> RecordConfig.saveEdited(loaded, SECOND));
	}

	@Test
	public void exportDoesNotSave() throws Exception {
		Long configStamp = ConfigRepository.loadForEdit().getLastSavedAt();
		Long competitionStamp = CompetitionRepository.loadForEdit().getLastSavedAt();
		try (InputStream export = new CompetitionDataV2().fromDatabase().exportData()) {
			export.readAllBytes();
		}
		assertEquals(configStamp, ConfigRepository.loadForEdit().getLastSavedAt());
		assertEquals(competitionStamp, CompetitionRepository.loadForEdit().getLastSavedAt());
	}

	@Test
	public void anImportMakesEarlierDraftsStale() throws Exception {
		byte[] json;
		try (InputStream export = new CompetitionDataV2().fromDatabase().exportData()) {
			json = export.readAllBytes();
		}
		String exported = new String(json, StandardCharsets.UTF_8);
		assertFalse("the stamp is never exported", exported.contains("lastSavedAt"));

		Config configDraft = ConfigRepository.loadForEdit();
		Competition competitionDraft = CompetitionRepository.loadForEdit();
		RecordConfig recordDraft = RecordConfig.loadForEdit();

		FormatDetector.importData(new ByteArrayInputStream(json), AuditOrigin.system("importer"));

		assertThrows(StaleSingletonException.class, () -> ConfigRepository.saveEdited(configDraft, SECOND));
		assertThrows(StaleSingletonException.class, () -> CompetitionRepository.saveEdited(competitionDraft, SECOND));
		assertThrows(StaleSingletonException.class, () -> RecordConfig.saveEdited(recordDraft, SECOND));
		assertEquals(1, CompetitionRepository.findAll().size());
	}

	@Test
	public void savedByNamesOnlyWhatIsKnownForSure() {
		AuditActor pinPage = new AuditActor("PIN", "-", "PREPARATION", null, true, "-", "-");
		AuditActor pinUnknownPage = new AuditActor("PIN", "-", "SYSTEM", null, true, "-", "-");
		AuditActor account = new AuditActor("ACCOUNTS", "alice", "PREPARATION", null, true, "-", "-");
		AuditActor accountUnknownPage = new AuditActor("ACCOUNTS", "alice", "SYSTEM", null, true, "-", "-");
		AuditActor pinAnnouncer = new AuditActor("PIN", "-", "ANNOUNCER", null, true, "-", "-");
		AuditActor device = AuditActor.device("REFEREE", 2, "-");

		assertNull("PIN mode: a station from the package fallback is a guess", SingletonSaves.savedByLabel(new AuditOrigin(pinPage, "x")));
		assertNull(SingletonSaves.savedByLabel(new AuditOrigin(pinUnknownPage, "x")));
		assertEquals("PIN mode: a page listed by exact class is certain", "ANNOUNCER",
		        SingletonSaves.savedByLabel(new AuditOrigin(pinAnnouncer, "x")));
		assertEquals("alice@PREPARATION", SingletonSaves.savedByLabel(new AuditOrigin(account, "x")));
		assertEquals("alice", SingletonSaves.savedByLabel(new AuditOrigin(accountUnknownPage, "x")));
		assertEquals("REFEREE#2", SingletonSaves.savedByLabel(new AuditOrigin(device, "x")));
		assertEquals("system", SingletonSaves.savedByLabel(AuditOrigin.system("startup")));
	}
}
