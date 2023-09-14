package app.owlcms.nui.displays.scoreboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.ContentParameters;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.displays.scoreboard.Results;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/results")

public class ResultsNoLeadersPage extends AbstractScoreboardPage {
	
	Logger logger = (Logger) LoggerFactory.getLogger(ResultsNoLeadersPage.class);
	
	public ResultsNoLeadersPage() {
		setDefaultParameters(QueryParameters.simple(Map.of(
				ContentParameters.SILENT, "true",
				ContentParameters.DOWNSILENT, "true",
				DisplayParameters.DARK, "true",
				DisplayParameters.LEADERS, "false",
				DisplayParameters.RECORDS, "false"
				)));
		
		var board = new Results(this);
		board.setLeadersDisplay(false);
		board.setRecordsDisplay(false);
		this.addComponent(board);
	}
	
	@Override
	public String getPageTitle() {
		return getTranslation("Scoreboard") + OwlcmsSession.getFopNameIfMultiple();
	}
}
