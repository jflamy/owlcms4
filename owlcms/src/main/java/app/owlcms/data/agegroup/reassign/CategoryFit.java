/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup.reassign;

import java.util.Collection;
import java.util.Comparator;

import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.category.Category;
import ch.qos.logback.classic.Logger;

/**
 * The single fit rule for edits, global actions and the dry run: active age group, age in range, gender match,
 * body weight above the category minimum and at most its maximum, and entry total meeting the category QT.
 * Missing athlete data is reported before any per-category check.
 */
public final class CategoryFit {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(CategoryFit.class);

	/**
	 * How far a candidate got through the closeness order (gender, then age band, then active, then weight, then
	 * QT). Higher means closer to the athlete; the failure of the closest candidate is the reported leave reason.
	 */
	enum Stage {
		GENDER(LeaveReason.GENDER),
		AGE(LeaveReason.AGE),
		INACTIVE(LeaveReason.INACTIVE),
		BODY_WEIGHT(LeaveReason.QT),
		QT(LeaveReason.QT),
		FITS(null);

		private final LeaveReason reason;

		Stage(LeaveReason reason) {
			this.reason = reason;
		}

		LeaveReason reason() {
			return this.reason;
		}
	}

	public record FitResult(boolean fits, LeaveReason reason) {
		private static final FitResult FITS = new FitResult(true, null);
	}

	private CategoryFit() {
	}

	public static FitResult fit(AthleteSnapshot a, Category c) {
		if (a.age() == null) {
			return new FitResult(false, LeaveReason.MISSING_BIRTH_DATE);
		}
		if (a.bodyWeight() == null) {
			return new FitResult(false, LeaveReason.MISSING_BODY_WEIGHT);
		}
		Stage stage = stage(a, c);
		// a weight miss is an ordinary non-fit; only a leave reported as BODY_WEIGHT is the anomaly
		return stage == Stage.FITS ? FitResult.FITS : new FitResult(false, stage.reason);
	}

	/**
	 * The reason an athlete leaves a championship, or null when they fit at least one candidate. Missing birth
	 * date and missing body weight are reported first. Otherwise the reason comes from the closest candidate:
	 * gender match, then age band, then active, then weight in bounds, then QT. Ties go to the most preferred
	 * category. An empty candidate set reports INACTIVE.
	 */
	public static LeaveReason leaveReason(AthleteSnapshot a, Collection<Category> candidates,
	        Comparator<Category> preference) {
		if (a.age() == null) {
			return LeaveReason.MISSING_BIRTH_DATE;
		}
		if (a.bodyWeight() == null) {
			return LeaveReason.MISSING_BODY_WEIGHT;
		}
		if (candidates == null || candidates.isEmpty()) {
			return LeaveReason.INACTIVE;
		}
		Category best = null;
		Stage bestStage = null;
		for (Category c : candidates) {
			Stage stage = stage(a, c);
			if (stage == Stage.FITS) {
				return null;
			}
			if (best == null || stage.ordinal() > bestStage.ordinal()
			        || (stage == bestStage && preference != null && preference.compare(c, best) < 0)) {
				best = c;
				bestStage = stage;
			}
		}
		return publicReason(best, bestStage);
	}

	/** Closeness stage; the caller has already ruled out missing birth date and body weight. */
	private static Stage stage(AthleteSnapshot a, Category c) {
		AgeGroup ageGroup = c.getAgeGroup();
		if (ageGroup == null || c.getGender() != a.gender()) {
			return Stage.GENDER;
		}
		Integer minAge = ageGroup.getMinAge();
		Integer maxAge = ageGroup.getMaxAge();
		if ((minAge != null && a.age() < minAge) || (maxAge != null && a.age() > maxAge)) {
			return Stage.AGE;
		}
		if (!Boolean.TRUE.equals(c.isActive())) {
			return Stage.INACTIVE;
		}
		Double min = c.getMinimumWeight();
		Double max = c.getMaximumWeight();
		if (min == null || max == null || !(a.bodyWeight() > min && a.bodyWeight() <= max)) {
			return Stage.BODY_WEIGHT;
		}
		if (c.requiresEntryTotalConfirmation(a.entryTotal())) {
			return Stage.QT;
		}
		return Stage.FITS;
	}

	private static LeaveReason publicReason(Category c, Stage stage) {
		if (stage == Stage.BODY_WEIGHT) {
			// unreachable with contiguous limits; if seen, the category definitions have a body-weight hole
			logger./**/warn("category definitions leave a body-weight gap around category {}", c != null ? c.getId() : null);
		}
		return stage.reason();
	}
}
