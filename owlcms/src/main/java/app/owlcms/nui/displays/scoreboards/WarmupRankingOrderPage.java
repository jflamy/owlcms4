package app.owlcms.nui.displays.scoreboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.config.Config;
import app.owlcms.displays.scoreboard.ResultsRankingOrder;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/resultsRankingOrder")

public class WarmupRankingOrderPage extends AbstractResultsDisplayPage {

	Logger logger = (Logger) LoggerFactory.getLogger(WarmupRankingOrderPage.class);

	public WarmupRankingOrderPage() {
		// intentionally empty. superclass will call init() as required.
	}

	@Override
	public String getPageTitle() {
		return getTranslation("Scoreboard.RankingOrder") + OwlcmsSession.getFopNameIfMultiple()+"X";
	}

	@Override
	protected void init() {
		var board = new ResultsRankingOrder();
		this.setBoard(board);
		this.addComponent(board);

		setDefaultParameters(QueryParameters.simple(Map.of(
		        SoundParameters.SILENT, "true",
		        SoundParameters.DOWNSILENT, "true",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "true",
		        DisplayParameters.RECORDS, "true",
		        DisplayParameters.ABBREVIATED,
		        Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")))));
	}

}
