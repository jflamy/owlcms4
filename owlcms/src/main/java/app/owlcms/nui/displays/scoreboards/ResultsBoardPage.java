package app.owlcms.nui.displays.scoreboards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.ContentParameters;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.data.config.Config;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.displays.scoreboard.Results;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/resultsLeaders")

public class ResultsBoardPage extends AbstractResultsDisplayPage {

	Logger logger = (Logger) LoggerFactory.getLogger(ResultsBoardPage.class);
	Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	
	private static final int DEBOUNCE = 50;
	protected int liftsDone;

	protected EventBus uiEventBus;
	protected Double emFontSize = null;
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	private long now;
	private long lastShortcut;

	public ResultsBoardPage() {
		setDefaultParameters(QueryParameters.simple(Map.of(
		        ContentParameters.SILENT, "true",
		        ContentParameters.DOWNSILENT, "true",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "true",
		        DisplayParameters.RECORDS, "true",
		        DisplayParameters.ABBREVIATED,
		        Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")))));

		var board = new Results(this);
		board.setLeadersDisplay(true);
		board.setRecordsDisplay(true);
		this.addComponent(board);
	}

	@Override
	public String getPageTitle() {
		return getTranslation("ScoreboardWLeadersTitle") + OwlcmsSession.getFopNameIfMultiple();
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#addDialogContent(com.vaadin.flow.component.Component,
	 *      com.vaadin.flow.component.orderedlayout.VerticalLayout)
	 */
	@Override
	public void addDialogContent(Component target, VerticalLayout vl) {
		DisplayOptions.addLightingEntries(vl, target, this);
		DisplayOptions.addRule(vl);
		DisplayOptions.addSoundEntries(vl, target, this);
		DisplayOptions.addRule(vl);
		DisplayOptions.addSwitchableEntries(vl, target, this);
		DisplayOptions.addRule(vl);
		DisplayOptions.addSectionEntries(vl, target, this);
		DisplayOptions.addRule(vl);
		DisplayOptions.addSizingEntries(vl, target, this);

		UI.getCurrent().addShortcutListener(() -> {
			now = System.currentTimeMillis();
			if (now - lastShortcut > DEBOUNCE) {
				setEmFontSize(getEmFontSize() + 0.005);
			}
			lastShortcut = now;
		}, Key.ARROW_UP);
		UI.getCurrent().addShortcutListener(() -> {
			now = System.currentTimeMillis();
			if (now - lastShortcut > DEBOUNCE) {
				setEmFontSize(getEmFontSize() - 0.005);
			}
			lastShortcut = now;
		}, Key.ARROW_DOWN);
		UI.getCurrent().addShortcutListener(() -> {
			now = System.currentTimeMillis();
			if (now - lastShortcut > DEBOUNCE) {
				setTeamWidth(getTeamWidth() + 0.5);
			}
			lastShortcut = now;
		}, Key.ARROW_RIGHT);
		UI.getCurrent().addShortcutListener(() -> {
			now = System.currentTimeMillis();
			if (now - lastShortcut > DEBOUNCE) {

				setTeamWidth(getTeamWidth() - 0.5);
			}
			lastShortcut = now;
		}, Key.ARROW_LEFT);
	}

}
