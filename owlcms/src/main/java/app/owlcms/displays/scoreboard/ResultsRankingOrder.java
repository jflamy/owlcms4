package app.owlcms.displays.scoreboard;

import java.util.List;

import com.vaadin.flow.router.Route;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;

@SuppressWarnings("serial")
@Route("displays/resultsRankingOrder")
public class ResultsRankingOrder extends Results {

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
