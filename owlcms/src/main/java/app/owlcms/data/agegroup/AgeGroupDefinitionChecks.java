/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.owlcms.data.agegroup.AgeGapCheck.Finding;
import app.owlcms.data.agegroup.AgeGapCheck.Gap;
import app.owlcms.data.agegroup.AgeGapCheck.Overlap;
import app.owlcms.i18n.Translator;

/**
 * The age gap and overlap checks applied to all the definitions at once, for imports. Produces the same messages as
 * the editing dialogs; the caller decides where to show them.
 */
public final class AgeGroupDefinitionChecks {

	/** @param hasGaps true when at least one line is a gap; the other lines are overlaps */
	public record Warnings(boolean hasGaps, List<String> lines) {
	}

	private AgeGroupDefinitionChecks() {
	}

	/** One line per gap and per overlap of each championship; the locale is captured by the caller on the UI thread. */
	public static Warnings warnings(Locale locale) {
		List<String> lines = new ArrayList<>();
		boolean hasGaps = false;
		for (Finding finding : AgeGapCheck.findings(AgeGroupRepository.findAll())) {
			for (Gap gap : finding.gaps()) {
				hasGaps = true;
				lines.add(Translator.translate("AgeGroup.AgeGap.Warning", locale, finding.championshipName(),
				        gap.gender().asGenderName(locale), AgeGapCheck.ages(gap.fromAge(), gap.toAge())));
			}
			for (Overlap overlap : finding.overlaps()) {
				lines.add(Translator.translate("AgeGroup.AgeOverlap.Warning", locale, finding.championshipName(),
				        overlap.firstCode(), overlap.secondCode(), overlap.gender().asGenderName(locale),
				        AgeGapCheck.ages(overlap.fromAge(), overlap.toAge())));
			}
		}
		return new Warnings(hasGaps, List.copyOf(lines));
	}
}
