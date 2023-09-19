package app.owlcms.nui.displays.scoreboards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.config.Config;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.displays.scoreboard.CurrentAthlete;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/currentathlete")

public class CurrentAthletePage extends AbstractResultsDisplayPage {

	Logger logger = (Logger) LoggerFactory.getLogger(CurrentAthletePage.class);
	Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	@Override
	public String getPageTitle() {
		return getTranslation("CurrentAthleteTitle") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	protected void init() {
		var board = new CurrentAthlete(this);
		this.setBoard(board);
		board.setLeadersDisplay(true);
		board.setRecordsDisplay(true);
		this.addComponent(board);

		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied.
		setDefaultParameters(QueryParameters.simple(Map.of(
		        SoundParameters.SILENT, "true",
		        SoundParameters.DOWNSILENT, "true",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "false",
		        DisplayParameters.RECORDS, "false",
		        DisplayParameters.ABBREVIATED,
		        Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")))));
	}
	
	@Override
	public void addDialogContent(Component target, VerticalLayout vl) {
		DisplayOptions.addLightingEntries(vl, target, this);
		DisplayOptions.addRule(vl);
		DisplayOptions.addSoundEntries(vl, target, this);
	}
	
	@Override
	public boolean isShowInitialDialog() {
		return false;
	}
	

}
