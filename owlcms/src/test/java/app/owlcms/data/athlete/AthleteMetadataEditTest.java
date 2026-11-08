/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.athlete.AthleteMetadataEdit.Opening;
import app.owlcms.data.athlete.AthleteRepository.MetadataSaveResult;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.TestPlatforms;

/**
 * What the restricted name, team and records form opens with and saves. No UI objects.
 */
public class AthleteMetadataEditTest {
	private static final AuditOrigin ORIGIN = AuditOrigin.system("metadata edit test");

	private Map<String, FieldOfPlay> previousPlatforms;

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

	@Before
	public void noPlatformHoldsTheAthlete() {
		this.previousPlatforms = TestPlatforms.replace(new HashMap<>());
	}

	@After
	public void restorePlatforms() {
		TestPlatforms.replace(this.previousPlatforms);
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

	@Test
	public void normalizationTrimsAndMakesTheEligibilityStatusExplicit() {
		AthleteMetadata eligible = AthleteMetadataEdit
		        .normalize(new AthleteMetadata("  Ann ", " Lee", null, " IWF ", true, null));
		assertEquals(new AthleteMetadata("Ann", "Lee", "", "IWF", true, EligibleForIndividualRankingStatus.ELIGIBLE),
		        eligible);

		AthleteMetadata invited = AthleteMetadataEdit
		        .normalize(new AthleteMetadata("Ann", "Lee", "T", null, false, null));
		assertEquals(EligibleForIndividualRankingStatus.OOC_INVITED, invited.individualEligibilityStatus());
		assertFalse(invited.eligibleForIndividualRanking());
		assertEquals("", invited.federationCodes());
	}

	@Test
	public void theBooleanAlwaysFollowsTheStatus() {
		assertFalse(AthleteMetadataEdit.fromForm("A", "B", "T", "IWF",
		        EligibleForIndividualRankingStatus.DSQ_DOPING).eligibleForIndividualRanking());
		assertTrue(AthleteMetadataEdit.fromForm("A", "B", "T", "IWF",
		        EligibleForIndividualRankingStatus.ELIGIBLE).eligibleForIndividualRanking());
		assertFalse(AthleteMetadataEdit.normalize(new AthleteMetadata("A", "B", "T", "IWF", true,
		        EligibleForIndividualRankingStatus.OOC_OTHER)).eligibleForIndividualRanking());
		assertThrows(NullPointerException.class, () -> AthleteMetadataEdit.fromForm("A", "B", "T", "IWF", null));
	}

	@Test
	public void aFormLeftAloneEqualsWhatItOpenedWith() {
		Athlete legacy = stored("Legacy");
		JPAService.runInTransaction(em -> em.createNativeQuery(
		        "update Athlete set firstName = '  First  ', team = null, eligible_for_individual_ranking_status = null "
		                + "where id = " + legacy.getId())
		        .executeUpdate());

		Opening opening = AthleteMetadataEdit.open(legacy.getId());
		AthleteMetadata fromUntouchedForm = AthleteMetadataEdit.fromForm(opening.values().firstName(),
		        opening.values().lastName(), opening.values().team(), opening.values().federationCodes(),
		        opening.values().individualEligibilityStatus());

		assertEquals("First", opening.values().firstName());
		assertEquals("", opening.values().team());
		assertEquals(opening.values(), fromUntouchedForm);
	}

	@Test
	public void openingReadsTheStoredRowAndItsStamp() {
		Athlete created = stored("Opening");
		MetadataSaveResult changed = AthleteRepository.saveMetadata(created.getId(),
		        new AthleteMetadata("Fixed", "Opening", "Team B", "EWF", true,
		                EligibleForIndividualRankingStatus.ELIGIBLE),
		        created.getMetadataUpdatedAt(), ORIGIN);
		assertTrue(changed.saved());

		Opening opening = AthleteMetadataEdit.open(created.getId());

		assertEquals("Fixed", opening.values().firstName());
		assertEquals("EWF", opening.values().federationCodes());
		assertEquals(changed.athlete().getMetadataUpdatedAt(), opening.updatedAt());
		assertNotEquals(created.getMetadataUpdatedAt(), opening.updatedAt());
		assertTrue(opening.fullName().toLowerCase().contains("opening"));
	}

	@Test
	public void openingADeletedAthleteFails() {
		Athlete created = stored("Deleted");
		AthleteRepository.delete(created);
		assertThrows(IllegalStateException.class, () -> AthleteMetadataEdit.open(created.getId()));
	}

	@Test
	public void aSaveWithNoPlatformWritesOnlyMetadata() {
		Athlete created = stored("Offline");
		Opening opening = AthleteMetadataEdit.open(created.getId());
		AthleteMetadata values = AthleteMetadataEdit.fromForm("Ann", "Offline", "Team B", "IWF, EWF",
		        EligibleForIndividualRankingStatus.OOC_INVITED);

		MetadataSaveResult result = AthleteMetadataEdit.save(opening, values, ORIGIN);

		assertTrue(result.saved());
		Athlete reloaded = AthleteRepository.findById(created.getId());
		assertEquals(values, AthleteMetadata.from(reloaded));
		assertEquals("90", reloaded.getSnatch1Declaration());
		assertNotEquals(opening.updatedAt(), reloaded.getMetadataUpdatedAt());
	}

	@Test
	public void aFormOpenedBeforeAnotherCorrectionIsRefusedAndWritesNothing() {
		Athlete created = stored("Stale");
		Opening first = AthleteMetadataEdit.open(created.getId());
		Opening second = AthleteMetadataEdit.open(created.getId());

		assertTrue(AthleteMetadataEdit.save(first, AthleteMetadataEdit.fromForm("One", "Stale", "T1", "IWF",
		        EligibleForIndividualRankingStatus.ELIGIBLE), ORIGIN).saved());
		MetadataSaveResult refused = AthleteMetadataEdit.save(second, AthleteMetadataEdit.fromForm("Two", "Stale",
		        "T2", "EWF", EligibleForIndividualRankingStatus.ELIGIBLE), ORIGIN);

		assertTrue(refused.stale());
		Athlete reloaded = AthleteRepository.findById(created.getId());
		assertEquals("One", reloaded.getFirstName());
		assertEquals("T1", reloaded.getTeam());
	}

	@Test
	public void liftingActivityDoesNotMakeTheFormStale() {
		Athlete created = stored("Lifting");
		Opening opening = AthleteMetadataEdit.open(created.getId());
		Athlete lifting = AthleteRepository.findById(created.getId());
		lifting.setValidation(false);
		lifting.setSnatch1Declaration("95");
		AthleteRepository.saveNoMetadata(lifting);

		MetadataSaveResult result = AthleteMetadataEdit.save(opening, AthleteMetadataEdit.fromForm("Ann", "Lifting",
		        "Team B", "IWF", EligibleForIndividualRankingStatus.ELIGIBLE), ORIGIN);

		assertTrue(result.saved());
		assertEquals("95", AthleteRepository.findById(created.getId()).getSnatch1Declaration());
	}

	@Test
	public void aLegacyRowWithoutAStampOpensAndSaves() {
		Athlete legacy = stored("NoStamp");
		JPAService.runInTransaction(em -> em
		        .createQuery("update Athlete a set a.metadataUpdatedAt = null where a.id = :id")
		        .setParameter("id", legacy.getId()).executeUpdate());

		Opening opening = AthleteMetadataEdit.open(legacy.getId());
		assertNull(opening.updatedAt());
		MetadataSaveResult result = AthleteMetadataEdit.save(opening, AthleteMetadataEdit.fromForm("Ann", "NoStamp",
		        "Team B", "IWF", EligibleForIndividualRankingStatus.ELIGIBLE), ORIGIN);

		assertTrue(result.saved());
		assertNotNull(AthleteRepository.findById(legacy.getId()).getMetadataUpdatedAt());
	}
}
