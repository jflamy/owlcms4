/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.persistence.PersistenceException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import app.owlcms.audit.AthleteDiff;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.athlete.AthleteRepository.MetadataSaveResult;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.v2.V2Json;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.JpaTransactions;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.databind.json.JsonMapper;

/**
 * Metadata (name, team, federations, individual eligibility) and lifting saves own disjoint columns. No UI objects.
 */
public class AthleteMetadataSaveTest {
	private static final AuditOrigin ORIGIN = AuditOrigin.system("metadata test");
	private static final List<String> METADATA_COLUMNS = List.of("firstname=", "lastname=", "team=",
	        "federationcodes=", "eligibleforindividualranking=", "eligible_for_individual_ranking_status=",
	        "metadataupdatedat=");

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

	private static Athlete stored(String lastName) {
		Athlete athlete = new Athlete();
		athlete.setValidation(false);
		athlete.setLastName(lastName);
		athlete.setFirstName("First");
		athlete.setTeam("Team A");
		athlete.setFederationCodes("IWF");
		athlete.setSnatch1Declaration("90");
		return AthleteRepository.save(athlete);
	}

	private static Athlete copyOf(Athlete athlete) {
		Athlete copy = AthleteRepository.findById(athlete.getId());
		copy.setValidation(false);
		return copy;
	}

	private static AthleteMetadata metadata(String lastName) {
		return new AthleteMetadata("Fixed", lastName, "Team B", "IWF,EWF", true,
		        EligibleForIndividualRankingStatus.ELIGIBLE);
	}

	private static void clearStamp(Athlete athlete) {
		JPAService.runInTransaction(em -> em
		        .createQuery("update Athlete a set a.metadataUpdatedAt = null where a.id = :id")
		        .setParameter("id", athlete.getId()).executeUpdate());
	}

	private static List<String> athleteUpdates(Runnable work) {
		Logger sql = (Logger) LoggerFactory.getLogger("org.hibernate.SQL");
		Level previous = sql.getLevel();
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		sql.addAppender(appender);
		sql.setLevel(Level.DEBUG);
		try {
			work.run();
		} finally {
			sql.detachAppender(appender);
			sql.setLevel(previous);
		}
		List<String> updates = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
		        .map(String::toLowerCase).filter(message -> message.startsWith("update athlete set")).toList();
		assertFalse("the Hibernate SQL log was not captured", updates.isEmpty());
		return updates;
	}

	@Test
	public void fullSaveInitializesTheStampAndOnlyMetadataChangesAdvanceIt() {
		Athlete created = stored("Stamp");
		Long first = created.getMetadataUpdatedAt();
		assertNotNull(first);

		Athlete lifting = copyOf(created);
		lifting.setSnatch1Declaration("95");
		AthleteRepository.save(lifting);
		assertEquals(first, copyOf(created).getMetadataUpdatedAt());

		Athlete renamed = copyOf(created);
		renamed.setTeam("Other team");
		AthleteRepository.save(renamed);
		assertTrue(copyOf(created).getMetadataUpdatedAt() > first);
	}

	@Test
	public void aFullSaveOfALegacyRowWithoutAStampKeepsItWhenMetadataIsUnchanged() {
		Athlete created = stored("Legacy");
		clearStamp(created);
		Athlete lifting = copyOf(created);
		lifting.setSnatch1Declaration("97");
		AthleteRepository.save(lifting);
		Athlete result = copyOf(created);
		assertNull(result.getMetadataUpdatedAt());
		assertEquals("97", result.getSnatch1Declaration());

		result.setTeam("New team");
		AthleteRepository.save(result);
		assertNotNull(copyOf(created).getMetadataUpdatedAt());
	}

	@Test
	public void registrationAddInitializesTheStamp() {
		Athlete athlete = new Athlete();
		athlete.setValidation(false);
		athlete.setLastName("Added");
		assertNotNull(AthleteRepository.create(athlete).getMetadataUpdatedAt());
	}

	@Test
	public void registrationCreatePropagatesFailureWithoutInsertingTheAthlete() {
		Athlete athlete = new Athlete();
		athlete.setLastName("x".repeat(256));
		assertThrows(PersistenceException.class, () -> AthleteRepository.create(athlete));
		assertNull(AthleteRepository.findById(athlete.getId()));
	}

	@Test
	public void creationTransactionRollsBackAndPropagatesPostgresFailures() {
		Athlete athlete = new Athlete();
		athlete.setLastName("Rolled back");
		PersistenceException failure = new PersistenceException(
		        new PSQLException("test failure", PSQLState.DATA_ERROR));

		assertSame(failure, assertThrows(PersistenceException.class, () -> JpaTransactions.inTransaction(em -> {
			em.persist(athlete);
			em.flush();
			throw failure;
		})));
		assertNull(AthleteRepository.findById(athlete.getId()));
	}

	@Test
	public void rollbackOnlyTransactionReturnsItsResultWithoutPersisting() {
		Athlete athlete = new Athlete();
		Long id = JpaTransactions.inRollbackOnlyTransaction(em -> {
			em.persist(athlete);
			em.flush();
			return athlete.getId();
		});
		assertEquals(athlete.getId(), id);
		assertNull(AthleteRepository.findById(id));
	}

	@Test
	public void aLiftingSaveFromAStaleCopyKeepsTheCorrectedMetadata() {
		Athlete created = stored("Original");
		Athlete staleCopy = copyOf(created);
		MetadataSaveResult corrected = AthleteRepository.saveMetadata(created.getId(), metadata("Corrected"),
		        created.getMetadataUpdatedAt(), ORIGIN);
		assertTrue(corrected.saved());

		staleCopy.setSnatch1Declaration("100");
		AthleteRepository.saveNoMetadata(staleCopy);

		Athlete result = copyOf(created);
		assertEquals("100", result.getSnatch1Declaration());
		assertEquals("Fixed", result.getFirstName());
		assertEquals("Corrected", result.getLastName());
		assertEquals("Team B", result.getTeam());
		assertEquals("IWF,EWF", result.getFederationCodes());
		assertEquals(EligibleForIndividualRankingStatus.ELIGIBLE, result.getIndividualEligibilityStatus());
		assertEquals(corrected.athlete().getMetadataUpdatedAt(), result.getMetadataUpdatedAt());
	}

	@Test
	public void aMetadataSaveKeepsLiftingDataSavedMeanwhile() {
		Athlete created = stored("Lifting");
		Long opened = created.getMetadataUpdatedAt();
		Athlete lifting = copyOf(created);
		lifting.setSnatch1Declaration("95");
		AthleteRepository.saveNoMetadata(lifting);

		assertTrue("lift activity does not make metadata stale",
		        AthleteRepository.saveMetadata(created.getId(), metadata("Lifting"), opened, ORIGIN).saved());
		assertEquals("95", copyOf(created).getSnatch1Declaration());
	}

	@Test
	public void theSecondMetadataDraftFromTheSameStampIsRefused() {
		Athlete created = stored("Draft");
		Long opened = created.getMetadataUpdatedAt();
		MetadataSaveResult first = AthleteRepository.saveMetadata(created.getId(), metadata("One"), opened, ORIGIN);
		MetadataSaveResult second = AthleteRepository.saveMetadata(created.getId(), metadata("Two"), opened,
		        ORIGIN);

		assertTrue(first.saved());
		assertTrue(second.stale());
		assertEquals("One", copyOf(created).getLastName());
		assertTrue(first.athlete().getMetadataUpdatedAt() > opened);

		MetadataSaveResult third = AthleteRepository.saveMetadata(created.getId(), metadata("Three"),
		        first.athlete().getMetadataUpdatedAt(), ORIGIN);
		assertTrue(third.saved());
		assertTrue(third.athlete().getMetadataUpdatedAt() > first.athlete().getMetadataUpdatedAt());
	}

	@Test
	public void aLegacyRowWithoutAStampAllowsOneFirstMetadataEdit() {
		Athlete created = stored("Legacy");
		clearStamp(created);
		assertNull(copyOf(created).getMetadataUpdatedAt());

		assertTrue(AthleteRepository.saveMetadata(created.getId(), metadata("One"), null, ORIGIN).saved());
		assertTrue(AthleteRepository.saveMetadata(created.getId(), metadata("Two"), null, ORIGIN).stale());
		assertEquals("One", copyOf(created).getLastName());
	}

	@Test
	public void aLiftingSaveKeepsLegacyRawValuesAndTheMissingStamp() {
		Athlete created = stored("Legacy");
		JPAService.runInTransaction(em -> em.createQuery("update Athlete a set a.individualEligibilityStatus = null,"
		        + " a.eligibleForIndividualRanking = false, a.metadataUpdatedAt = null, a.lastName = ' Padded '"
		        + " where a.id = :id").setParameter("id", created.getId()).executeUpdate());

		Athlete lifting = copyOf(created);
		lifting.setSnatch1Declaration("99");
		AthleteRepository.saveNoMetadata(lifting);

		Object[] row = (Object[]) JPAService.runInTransaction(em -> em.createNativeQuery(
		        "select lastName, eligibleForIndividualRanking, eligible_for_individual_ranking_status,"
		                + " metadataUpdatedAt, snatch1Declaration from Athlete where id = :id")
		        .setParameter("id", created.getId()).getSingleResult());
		assertEquals(" Padded ", row[0]);
		assertEquals(Boolean.FALSE, row[1]);
		assertNull(row[2]);
		assertNull(row[3]);
		assertEquals("99", row[4]);
	}

	@Test
	public void aDeletedAthleteIsNeverRecreated() {
		Athlete created = stored("Deleted");
		Athlete copy = copyOf(created);
		Long opened = created.getMetadataUpdatedAt();
		AthleteRepository.delete(created);

		assertThrows(IllegalStateException.class, () -> AthleteRepository.saveNoMetadata(copy));
		assertThrows(IllegalStateException.class,
		        () -> AthleteRepository.saveMetadata(created.getId(), metadata("Deleted"), opened, ORIGIN));
		assertNull(AthleteRepository.findById(created.getId()));
	}

	@Test
	public void theSecondLevelCacheSeesBothSavesInEitherOrder() {
		Athlete created = stored("Cache");
		Athlete staleCopy = copyOf(created);
		MetadataSaveResult corrected = AthleteRepository.saveMetadata(created.getId(), metadata("Corrected"),
		        created.getMetadataUpdatedAt(), ORIGIN);
		staleCopy.setSnatch1Declaration("101");
		AthleteRepository.saveNoMetadata(staleCopy);

		Athlete cached = JPAService.runInTransaction(em -> em.find(Athlete.class, created.getId()));
		assertEquals("Corrected", cached.getLastName());
		assertEquals("101", cached.getSnatch1Declaration());
		assertEquals(corrected.athlete().getMetadataUpdatedAt(), cached.getMetadataUpdatedAt());

		Athlete another = copyOf(created);
		another.setSnatch1Declaration("102");
		AthleteRepository.saveNoMetadata(another);
		Athlete cachedLifting = JPAService.runInTransaction(em -> em.find(Athlete.class, created.getId()));
		assertEquals("Corrected", cachedLifting.getLastName());
		assertEquals("102", cachedLifting.getSnatch1Declaration());
		assertEquals(cached.getMetadataUpdatedAt(), cachedLifting.getMetadataUpdatedAt());

		MetadataSaveResult correctedAgain = AthleteRepository.saveMetadata(created.getId(), metadata("Again"),
		        cached.getMetadataUpdatedAt(), ORIGIN);
		assertTrue(correctedAgain.saved());
		Athlete cachedAgain = JPAService.runInTransaction(em -> em.find(Athlete.class, created.getId()));
		assertEquals("Again", cachedAgain.getLastName());
		assertEquals("102", cachedAgain.getSnatch1Declaration());
		assertEquals(correctedAgain.athlete().getMetadataUpdatedAt(), cachedAgain.getMetadataUpdatedAt());
		assertTrue(cachedAgain.getMetadataUpdatedAt() > cachedLifting.getMetadataUpdatedAt());
	}

	@Test
	public void conditionalCopyWithoutMetadataLeavesNamesAndFederationCodes() {
		Athlete src = new Athlete();
		src.setLastName("Source");
		src.setFirstName("Src");
		src.setFederationCodes("A");
		src.setCoach("Coach");
		Athlete dest = new Athlete();
		dest.setLastName("Dest");
		dest.setFirstName("Dst");
		dest.setFederationCodes("B");

		Athlete.conditionalCopy(dest, src, true, true, false, false);
		assertEquals("Dest", dest.getLastName());
		assertEquals("Dst", dest.getRawFirstName());
		assertEquals("B", dest.getFederationCodes());
		assertEquals("lifting values are still copied", "Coach", dest.getCoach());

		Athlete.conditionalCopy(dest, src, true, true, false);
		assertEquals("form display copies still receive everything", "Source", dest.getLastName());
		assertEquals("Src", dest.getRawFirstName());
		assertEquals("A", dest.getFederationCodes());
	}

	@Test
	public void theUpdateStatementsOnlyNameTheirOwnColumns() {
		Athlete created = stored("Columns");
		Athlete lifting = copyOf(created);
		lifting.setSnatch1Declaration("96");
		List<String> liftingUpdates = athleteUpdates(() -> AthleteRepository.saveNoMetadata(lifting));
		assertEquals(1, liftingUpdates.size());
		assertTrue(liftingUpdates.getFirst().contains("snatch1declaration="));
		for (String column : METADATA_COLUMNS) {
			assertFalse(column + " in " + liftingUpdates.getFirst(), liftingUpdates.getFirst().contains(column));
		}

		List<String> metadataUpdates = athleteUpdates(() -> AthleteRepository.saveMetadata(created.getId(),
		        metadata("Renamed"), created.getMetadataUpdatedAt(), ORIGIN));
		assertEquals(1, metadataUpdates.size());
		assertTrue(metadataUpdates.getFirst().contains("lastname="));
		assertTrue(metadataUpdates.getFirst().contains("metadataupdatedat="));
		assertFalse(metadataUpdates.getFirst().contains("snatch"));
		assertFalse(metadataUpdates.getFirst().contains("cleanjerk"));
	}

	@Test
	public void federationCodesAndEligibilityStatusAreAudited() {
		Athlete athlete = new Athlete();
		athlete.setFederationCodes("IWF");
		athlete.setIndividualEligibilityStatus(EligibleForIndividualRankingStatus.ELIGIBLE);
		AthleteDiff.Snapshot before = AthleteDiff.snapshot(athlete);
		athlete.setFederationCodes("IWF,EWF");
		athlete.setIndividualEligibilityStatus(EligibleForIndividualRankingStatus.OOC_INVITED);
		List<String> fields = AthleteDiff.diff(before, AthleteDiff.snapshot(athlete)).stream()
		        .map(AthleteDiff.Change::field).toList();
		assertTrue(fields.contains("federationCodes"));
		assertTrue(fields.contains("individualEligibilityStatus"));
	}

	@Test
	public void theStampIsNotPartOfAnyJsonFormat() {
		Athlete created = stored("Json");
		assertNotNull(created.getMetadataUpdatedAt());
		assertFalse(V2Json.toJson(created).has("metadataUpdatedAt"));
		assertFalse(JsonMapper.builder().build().valueToTree(created).has("metadataUpdatedAt"));
	}
}
