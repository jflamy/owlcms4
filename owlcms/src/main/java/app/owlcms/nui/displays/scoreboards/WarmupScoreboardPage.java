package app.owlcms.nui.displays.scoreboards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.config.Config;
import app.owlcms.displays.scoreboard.Results;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/resultsLeaders")

public class WarmupScoreboardPage extends AbstractResultsDisplayPage {

	Logger logger = (Logger) LoggerFactory.getLogger(WarmupScoreboardPage.class);
	Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	public WarmupScoreboardPage() {
		// intentionally empty.  init() will be called as required.
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("ScoreboardWLeadersTitle") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	protected void init() {
		logger = (Logger) LoggerFactory.getLogger(WarmupScoreboardPage.class);
		uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
		
		// each subclass must define this routine.
		// otherwise we end up with multiple instances of the Results board.
		var board = new Results();
		this.setBoard(board);

		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied.
		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied.
		var initialMap = Map.of(
		        SoundParameters.SILENT, "true",
		        SoundParameters.DOWNSILENT, "true",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "true",
		        DisplayParameters.RECORDS, "true",
		        DisplayParameters.VIDEO, "false",
		        DisplayParameters.PUBLIC, "false",
		        SoundParameters.SINGLEREF, "false",
		        DisplayParameters.ABBREVIATED, Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")));
		var additionalMap = Map.of(
		        SoundParameters.LIVE_LIGHTS, Boolean.toString(!Config.getCurrent().featureSwitch("noLiveLights")),
		        SoundParameters.SHOW_DECLARATIONS, "false",
		        SoundParameters.CENTER_NOTIFICATIONS, Boolean.toString(Config.getCurrent().featureSwitch("centerAnnouncerNotifications")),
		        SoundParameters.START_ORDER, "false");
		Map<String, String> fullMap = new TreeMap<>();
		fullMap.putAll(initialMap);
		fullMap.putAll(additionalMap);
		setDefaultParameters(QueryParameters.simple(fullMap));
	}
	
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		DisplayParameters board = (DisplayParameters) this.getBoard();
		board.setFop(this.getFop());
		board.setLeadersDisplay(true);
		board.setRecordsDisplay(true);

		this.addComponent((Component) board);
	}

}
