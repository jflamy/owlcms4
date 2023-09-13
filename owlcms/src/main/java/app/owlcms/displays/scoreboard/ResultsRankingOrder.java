package app.owlcms.displays.scoreboard;

import java.util.List;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;

@SuppressWarnings("serial")
@Route("displays/resultsRankingOrder")

@Tag("results-template")
@JsModule("./components/Results.js")
@JsModule("./components/AudioContext.js")

public class ResultsRankingOrder extends Results implements DisplayParameters {

	@Override
	public String getDisplayType() {
		return Translator.translate("Scoreboard.RankingOrder") + ": ";
	}

	@Override
	public String getPageTitle() {
		return getTranslation("Scoreboard.RankingOrder") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	protected List<Athlete> getOrder(FieldOfPlay fop) {
		return fop.getResultsOrder();
	}

}
