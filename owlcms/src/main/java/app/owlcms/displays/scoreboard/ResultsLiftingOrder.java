package app.owlcms.displays.scoreboard;

import java.util.List;
import java.util.function.BiPredicate;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.displays.scoreboards.WarmupLiftingOrderPage;

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

	@Override
	protected int countSubsets(List<Athlete> athletes) {
		// when the decision is shown, 1st athlete has completed lift, but is still shown at top as
		// with other snatching athletes, if any, below.
		// so we must check if there are any snatching still.
		boolean snatchPresent = (athletes.stream().anyMatch(s -> s.getActuallyAttemptedLifts() < 3));
		boolean cjPresent = (athletes.get(athletes.size() - 1).getAttemptsDone() >= 3);
		return (snatchPresent ? 1 : 0) + (cjPresent ? 1 : 0) + 1;
	}

	@Override
	protected List<Athlete> getOrder(FieldOfPlay fop) {
		return fop.getLiftingOrder();
	}

	/**
	 * return true if change of lifts
	 */
	@Override
	protected BiPredicate<Athlete, Athlete> getSeparatorPredicate() {
		BiPredicate<Athlete, Athlete> separator = (cur, prev) -> (prev == null) ||
		        cur.getAttemptsDone() >= 3
		                && (prev.getAttemptsDone() < 3);
		return separator;
	}

}
