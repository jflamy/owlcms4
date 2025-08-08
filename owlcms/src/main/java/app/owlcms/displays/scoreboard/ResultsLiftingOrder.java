/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.List;
import java.util.function.BiPredicate;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.displays.scoreboards.WarmupLiftingOrderPage;
import elemental.json.JsonValue;

@SuppressWarnings({ "serial", "deprecation" })

@Tag("results-template")
@JsModule("./components/Results.js")
@JsModule("./components/AudioContext.js")

public class ResultsLiftingOrder extends Results {

	public ResultsLiftingOrder(WarmupLiftingOrderPage page) {
	}

	@Override
	public String getDisplayType() {
		return Translator.translate("Scoreboard.LiftingOrder") + ": ";
	}
	
	int nbSubsets = 0;

	@Override
	protected int countSubsets(List<Athlete> athletes) {
		return nbSubsets;
	}

	@Override
	protected List<Athlete> getOrder(FieldOfPlay fop) {
		return fop.getLiftingOrder();
	}

	
	@Override
	protected JsonValue getAthletesJson(List<Athlete> displayOrder, List<Athlete> liftOrder, FieldOfPlay fop) {
		nbSubsets = 1;
		return super.getAthletesJson(displayOrder, liftOrder, fop);
	}
	/**
	 * return true if change of lifts
	 */
	@Override
	protected BiPredicate<Athlete, Athlete> getSeparatorPredicate() {
		BiPredicate<Athlete, Athlete> separator = (cur, prev) -> {
			// during snatch, we want a separator between athletes still snatching and athletes done snatching but not in cj
			boolean first = (prev == null);
			if (first) {
				nbSubsets++;
				return true;
			}
			@SuppressWarnings("null")
			boolean prevSnatchNotDone = prev.getAttemptsDone() < 3;
			boolean curSnatchDoneCJNotStarted = cur.getAttemptsDone() == 3 || cur.withdrawnFromSnatch();
			if (prevSnatchNotDone && curSnatchDoneCJNotStarted) {
//				logger.debug("**** {} separator1 {} prev {} prevSnatchNotDone={} cur {} curSnatchDoneCJNotStarted={}",
//						System.identityHashCode(this),
//						prev.getAbbreviatedName(), prevSnatchNotDone,
//				        cur.getAbbreviatedName(), curSnatchDoneCJNotStarted);
				nbSubsets++;
				return true;
			}
			boolean prevCJNotDone = prev.getAttemptsDone() < 6;
			boolean curIsCJDone = (cur.getAttemptsDone() >= 6 || cur.withdrawnFromCJ());
//			logger.debug("**** {} separator2 {} prev {} prevCJNotDone={} cur {} curIsCJDone={}",
//					System.identityHashCode(this),
//					prev.getAbbreviatedName(), prevCJNotDone,
//			        cur.getAbbreviatedName(), curIsCJDone);
			if (prevCJNotDone && curIsCJDone) {
				nbSubsets++;
				return true;
			}
			return false;
		};
		return separator;
	}

}
