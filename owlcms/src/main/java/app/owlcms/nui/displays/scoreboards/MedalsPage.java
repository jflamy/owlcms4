package app.owlcms.nui.displays.scoreboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.config.Config;
import app.owlcms.displays.scoreboard.ResultsMedals;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/resultsMedals")

public class MedalsPage extends AbstractResultsDisplayPage {

	Logger logger = (Logger) LoggerFactory.getLogger(MedalsPage.class);

	public MedalsPage() {
		// intentionally empty. superclass will call init() as required.
	}

	@Override
	public String getPageTitle() {
		return getTranslation("CeremonyType.MEDALS") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	protected void init() {
		var board = new ResultsMedals();
		this.setBoard(board);
		this.addComponent(board);

		setDefaultParameters(QueryParameters.simple(Map.of(
		        SoundParameters.SILENT, "true",
		        SoundParameters.DOWNSILENT, "true",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "true",
		        DisplayParameters.RECORDS, "true",
		        DisplayParameters.SINGLEREF, "false",
		        DisplayParameters.PUBLIC, "false",
		        DisplayParameters.VIDEO, "false",
		        DisplayParameters.ABBREVIATED,
		        Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")))));
	}

}
