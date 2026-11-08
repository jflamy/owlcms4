/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

import java.util.Objects;

import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.athlete.AthleteRepository.MetadataSaveResult;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;

/**
 * Editing of the name, team, record federations and individual eligibility of an athlete, without touching lifting
 * data. The restricted metadata form uses this class: it opens from the stored row and saves under the monitor of the
 * platform holding the athlete's session.
 */
public final class AthleteMetadataEdit {

	/** Serializes metadata saves for athletes whose session is not selected on any platform. */
	private static final Object OFFLINE_MONITOR = new Object();

	private AthleteMetadataEdit() {
	}

	/**
	 * What the form was opened with.
	 *
	 * @param athleteId the athlete
	 * @param fullName  for captions and messages
	 * @param values    the stored values as the form shows them (see {@link #normalize})
	 * @param updatedAt the metadata timestamp to present when saving; null for a legacy row
	 */
	public record Opening(Long athleteId, String fullName, AthleteMetadata values, Long updatedAt) {
	}

	/**
	 * Reads the athlete from the database, so that a grid row that is out of date does not provide the values.
	 *
	 * @throws IllegalStateException if the athlete no longer exists
	 */
	public static Opening open(Long athleteId) {
		Athlete stored = AthleteRepository.findById(Objects.requireNonNull(athleteId));
		if (stored == null) {
			throw new IllegalStateException("Athlete no longer exists: " + athleteId);
		}
		return new Opening(athleteId, stored.getFullName(), normalize(AthleteMetadata.from(stored)),
		        stored.getMetadataUpdatedAt());
	}

	/**
	 * The values the form shows and saves: names, federation codes trimmed, no null team, an explicit eligibility
	 * status (a legacy row with only the boolean gets the status that boolean implies), and the boolean derived from
	 * the status.
	 */
	public static AthleteMetadata normalize(AthleteMetadata raw) {
		EligibleForIndividualRankingStatus status = raw.individualEligibilityStatus() != null
		        ? raw.individualEligibilityStatus()
		        : (raw.eligibleForIndividualRanking() ? EligibleForIndividualRankingStatus.ELIGIBLE
		                : EligibleForIndividualRankingStatus.OOC_INVITED);
		return new AthleteMetadata(trim(raw.firstName()), trim(raw.lastName()), trim(raw.team()),
		        trim(raw.federationCodes()), status.isInclusion(), status);
	}

	/** The values entered in the form. */
	public static AthleteMetadata fromForm(String firstName, String lastName, String team, String federationCodes,
	        EligibleForIndividualRankingStatus status) {
		Objects.requireNonNull(status, "Eligibility status");
		return normalize(new AthleteMetadata(firstName, lastName, team, federationCodes, status.isInclusion(), status));
	}

	/**
	 * Saves metadata, checking the timestamp the form was opened with. The platform holding the athlete's session
	 * (selected, whether or not lifting has started) serializes the check and the save with its live writers; it is
	 * resolved at each attempt. No UI work or waiting for the user may happen here.
	 *
	 * @param origin captured on the UI thread where the action started
	 * @return {@link MetadataSaveResult#stale()} when the athlete's metadata was saved by someone else since opening;
	 *         nothing was written then
	 * @throws RuntimeException for any unexpected failure; the caller reports it, nothing is recovered here
	 */
	public static MetadataSaveResult save(Opening opening, AthleteMetadata values, AuditOrigin origin) {
		Long athleteId = opening.athleteId();
		Athlete current = AthleteRepository.findById(athleteId);
		if (current == null) {
			throw new IllegalStateException("Athlete no longer exists: " + athleteId);
		}
		FieldOfPlay fop = OwlcmsFactory.getFOPWithGroup(current.getGroup());
		if (fop == null) {
			synchronized (OFFLINE_MONITOR) {
				return AthleteRepository.saveMetadata(athleteId, values, opening.updatedAt(), origin);
			}
		}
		synchronized (fop) {
			if (OwlcmsFactory.getFOPWithGroup(current.getGroup()) != fop) {
				throw new IllegalStateException("Platform selection changed while saving athlete " + athleteId);
			}
			return fop.updateAthlete(athleteId, values, opening.updatedAt(), origin);
		}
	}

	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}
}
