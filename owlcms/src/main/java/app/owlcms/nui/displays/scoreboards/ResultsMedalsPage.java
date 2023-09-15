package app.owlcms.nui.displays.scoreboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.ContentParameters;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.data.config.Config;
import app.owlcms.displays.scoreboard.ResultsMedals;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/resultsMedals")

public class ResultsMedalsPage extends AbstractResultsDisplayPage {

	Logger logger = (Logger) LoggerFactory.getLogger(ResultsMedalsPage.class);

	public ResultsMedalsPage() {
		setDefaultParameters(QueryParameters.simple(Map.of(
		        ContentParameters.SILENT, "true",
		        ContentParameters.DOWNSILENT, "true",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "true",
		        DisplayParameters.RECORDS, "true",
		        DisplayParameters.ABBREVIATED,
		        Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")))));

		var board = new ResultsMedals(this);
		board.setLeadersDisplay(true);
		board.setRecordsDisplay(true);
		this.addComponent(board);
	}

	@Override
	public String getPageTitle() {
		return getTranslation("CeremonyType.MEDALS") + OwlcmsSession.getFopNameIfMultiple();
	}

}
