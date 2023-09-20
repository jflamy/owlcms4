package app.owlcms.nui.displays.scoreboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.config.Config;
import app.owlcms.displays.scoreboard.Results;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/publicResultsSimple")

public class PublicNoLeadersPage extends PublicScoreboardPage {

	Logger logger = (Logger) LoggerFactory.getLogger(PublicNoLeadersPage.class);

	public PublicNoLeadersPage() {
		// intentionally empty. superclass will call init() as required.
	}

	@Override
	public String getPageTitle() {
		return getTranslation("Scoreboard") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	protected void init() {
		// only difference is the default values
		var board = new Results();
		this.setBoard(board);
		this.addComponent(board);
		this.ui = UI.getCurrent();

		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied.
		setDefaultParameters(QueryParameters.simple(Map.of(
		        SoundParameters.SILENT, "true",
		        SoundParameters.DOWNSILENT, "true",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "false",
		        DisplayParameters.RECORDS, "false",
		        DisplayParameters.VIDEO, "false",
		        DisplayParameters.PUBLIC, "false",
		        SoundParameters.SINGLEREF, "false",
		        DisplayParameters.ABBREVIATED,
		        Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")))));
	}
}
