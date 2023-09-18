package app.owlcms.nui.displays.scoreboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.data.config.Config;
import app.owlcms.displays.scoreboard.ResultsLiftingOrder;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/resultsLiftingOrder")

public class ResultsLiftingOrderPage extends AbstractResultsDisplayPage {

	Logger logger = (Logger) LoggerFactory.getLogger(ResultsLiftingOrderPage.class);

	public ResultsLiftingOrderPage() {
		// intentionally empty. superclass will call init as required.
	}

	@Override
	public String getPageTitle() {
		return getTranslation("Scoreboard.LiftingOrder") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	protected void init() {
		var board = new ResultsLiftingOrder(this);
		this.setBoard(board);
		board.setLeadersDisplay(true);
		board.setRecordsDisplay(true);
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
