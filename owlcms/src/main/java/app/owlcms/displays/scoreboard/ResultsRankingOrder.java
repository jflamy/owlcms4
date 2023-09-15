package app.owlcms.displays.scoreboard;

import java.util.List;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.displays.scoreboards.ResultsRankingOrderPage;

@SuppressWarnings("serial")

@Tag("results-template")
@JsModule("./components/Results.js")
@JsModule("./components/AudioContext.js")

public class ResultsRankingOrder extends Results {
	
	public ResultsRankingOrder(ResultsRankingOrderPage page) {
		super();
		this.setWrapper(page);
		getWrapper().setBoard(this);
	}

	@Override
	public String getDisplayType() {
		return Translator.translate("Scoreboard.RankingOrder") + ": ";
	}

	@Override
	protected List<Athlete> getOrder(FieldOfPlay fop) {
		return fop.getResultsOrder();
	}

}
