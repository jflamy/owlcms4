package app.owlcms.displays.scoreboard;

import java.util.List;
import java.util.function.BiPredicate;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.displays.scoreboards.ResultsLiftingOrderPage;

@SuppressWarnings({ "serial", "deprecation" })

@Tag("results-template")
@JsModule("./components/Results.js")
@JsModule("./components/AudioContext.js")

public class ResultsLiftingOrder extends Results {

	public ResultsLiftingOrder(ResultsLiftingOrderPage page) {
		super(page);
	}

	@Override
	public String getDisplayType() {
		return Translator.translate("Scoreboard.LiftingOrder") + ": ";
	}

	@Override
	protected int countSubsets(List<Athlete> athlete) {
		boolean snatchPresent = (athlete.get(0).getActuallyAttemptedLifts() < 3);
		boolean cjPresent = (athlete.get(athlete.size() - 1).getAttemptsDone() >= 3);
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
