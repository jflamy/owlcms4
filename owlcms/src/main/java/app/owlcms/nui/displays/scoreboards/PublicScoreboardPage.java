/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays.scoreboards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.displays.scoreboard.Results;
import app.owlcms.displays.scoreboard.ResultsMedals;
import app.owlcms.displays.scoreboard.ResultsStartList;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/publicScoreboard")

public class PublicScoreboardPage extends AbstractResultsDisplayPage {

	Logger logger;
	Logger uiEventLogger;
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private Results resultsBoard;
	protected UI ui;
	private ResultsMedals medalsBoard;
	private ResultsStartList startListBoard;

	public PublicScoreboardPage() {
		// intentionally empty. superclass will call init() as required.
	}

	@Override
	public String getPageTitle() {
		String suffix = FieldOfPlay.getFopNameIfMultiple(getFop());
		return Translator.translate("DisplayParameters.PublicDisplay") + suffix;
	}

	public final Results getResultsBoard() {
		return this.resultsBoard;
	}

	@Subscribe
	public void reconcileDisplay(UIEvent e) {
		if (this.ui == null) {
			return;
		}
		this.ui.access(this::reconcileDisplayLocked);
	}

	private void reconcileDisplayLocked() {
		FieldOfPlay fop = getFop();
		if (fop == null || !isAttached()) {
			return;
		}
		boolean medals = fop.getActiveCeremony() != null && fop.getActiveCeremony().isMedals();
		boolean introduction = fop.getCeremonyType() == CeremonyType.INTRODUCTION;
		boolean beforeIntroduction = fop.getState() == FOPState.BREAK
		        && fop.getBreakType() == BreakType.BEFORE_INTRODUCTION;
		boolean waitingForSnatchCountdown = fop.getState() == FOPState.BREAK
		        && fop.getBreakType() == BreakType.FIRST_SNATCH
		        && !fop.getBreakTimer().isRunning();
		boolean sessionSelected = fop.getGroup() != null;
		boolean startList = !medals && sessionSelected
		        && (beforeIntroduction || introduction || waitingForSnatchCountdown);

		configureSecondaryBoard(getMedalsBoard());
		configureSecondaryBoard(getStartListBoard());
		getStartListBoard().setScoreboardTimerVisible(startList);
		getMedalsBoard().getStyle().set("display", medals ? "block" : "none");
		getStartListBoard().getStyle().set("display", startList ? "block" : "none");
		getResultsBoard().getStyle().set("display", medals || startList ? "none" : "block");
		if (medals) {
			getMedalsBoard().syncWithFOP(fop);
		}
		pushEmSize(this.getElement());
		pushTeamWidth(this.getElement());
	}

	private void configureSecondaryBoard(Results board) {
		DisplayParameters source = (DisplayParameters) getBoard();
		board.setDownSilenced(true);
		board.setDarkMode(source.isDarkMode());
		board.setVideo(source.isVideo());
		board.setPublicDisplay(source.isPublicDisplay());
		board.setSingleReferee(((SoundParameters) getBoard()).isSingleReferee());
		board.setAbbreviatedName(source.isAbbreviatedName());
		computeStylesDir(board);
	}

	@Override
	protected void init() {
		this.logger = (Logger) LoggerFactory.getLogger(this.getClass());
		this.uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
		createComponents();
		setDefaultParameters();
	}

	@Override
	public final void setEmFontSize(Double emFontSize) {
		Double medalFontSize;
		// subjective visual kludging.
		if (emFontSize == null) {
			emFontSize = 1.0;
			medalFontSize = 1.5;
		} else {
			//medalFontSize = emFontSize * 1.5;
			medalFontSize = emFontSize;
		}
		super.setEmFontSize(emFontSize);
		pushEmSize(this.getBoard().getElement(), emFontSize);
		pushEmSize(this.getMedalsBoard().getElement(),medalFontSize);
		pushEmSize(this.getStartListBoard().getElement(), emFontSize);
	}
	
	@Override
	final public void setTeamWidth(Double tw) {
		super.setTeamWidth(tw);
		pushTeamWidth(getElement(), tw);
		pushTeamWidth(this.getMedalsBoard().getElement(), tw);
		pushTeamWidth(this.getStartListBoard().getElement(), tw);
	}

	
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		uiEventBusRegister(this, getFop());
		
		// overrides common to all enclosed boards
		this.getElement().getStyle().set("--medalOverride", "2em");
		
		DisplayParameters board = (DisplayParameters) this.getBoard();
		board.setFop(getFop());
		getMedalsBoard().setFop(getFop());
		getStartListBoard().setFop(getFop());

		this.setResultsBoard((Results) board);
		this.setMedalsBoard(getMedalsBoard());

		this.addComponent((Component) board);
		this.addComponent(getMedalsBoard());
		this.addComponent(getStartListBoard());
		reconcileDisplayLocked();
		pushEmSize(this.getElement());
		pushTeamWidth(this.getElement());

	}

	protected void setDefaultParameters() {
		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied.
		var initialMap = Map.of(
		        SoundParameters.SILENT, "true",
		        SoundParameters.DOWNSILENT, "true",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "true",
		        DisplayParameters.RECORDS, "true",
		        DisplayParameters.VIDEO, "false",
		        DisplayParameters.PUBLIC, "true",
		        SoundParameters.SINGLEREF, "false",
		        DisplayParameters.ABBREVIATED, Boolean.toString(Config.getCurrent().featureSwitch(FeatureSwitch.SHORT_SCOREBOARD_NAMES)));
		var additionalMap = Map.of(
		        SoundParameters.LIVE_LIGHTS, Boolean.toString(!Config.getCurrent().featureSwitch(FeatureSwitch.NO_LIVE_LIGHTS)),
		        SoundParameters.SHOW_DECLARATIONS, "false",
		        SoundParameters.CENTER_NOTIFICATIONS, Boolean.toString(Config.getCurrent().featureSwitch(FeatureSwitch.CENTER_ANNOUNCER_NOTIFICATIONS)),
		        SoundParameters.START_ORDER, "false",
		        DisplayParameters.CURRENT_ATTEMPT, "false",
		        DisplayParameters.SHOW_MEDALS, "auto");
		Map<String, String> fullMap = new TreeMap<>();
		fullMap.putAll(initialMap);
		fullMap.putAll(additionalMap);
		setDefaultParameters(QueryParameters.simple(fullMap));
	}

	protected void setResultsBoard(Results board) {
		this.resultsBoard = board;
	}

	private void createComponents() {
		var board = new Results();
		setMedalsBoard(new ResultsMedals());
		setStartListBoard(new ResultsStartList());
		this.setBoard(board);

		getMedalsBoard().setDownSilenced(true);
		getMedalsBoard().setDarkMode(board.isDarkMode());
		getMedalsBoard().setVideo(board.isVideo());
		getMedalsBoard().setPublicDisplay(board.isPublicDisplay());
		getMedalsBoard().setSingleReferee(board.isSingleReferee());
		getMedalsBoard().setAbbreviatedName(board.isAbbreviatedName());
		getMedalsBoard().setTeamWidth(board.getTeamWidth());
		getMedalsBoard().setEmFontSize(board.getEmFontSize());
		computeStylesDir(getMedalsBoard());

		getMedalsBoard().getStyle().set("display", "none");
		getStartListBoard().getStyle().set("display", "none");
		this.ui = UI.getCurrent();
	}

	private ResultsMedals getMedalsBoard() {
		return this.medalsBoard;
	}

	private void setMedalsBoard(ResultsMedals medalsBoard) {
		this.medalsBoard = medalsBoard;
	}

	private ResultsStartList getStartListBoard() {
		if (this.startListBoard == null) {
			this.startListBoard = new ResultsStartList();
			this.startListBoard.setFop(getFop());
			this.startListBoard.getStyle().set("display", "none");
			if (isAttached() && this.startListBoard.getParent().isEmpty()) {
				addComponent(this.startListBoard);
			}
		}
		return this.startListBoard;
	}

	private void setStartListBoard(ResultsStartList startListBoard) {
		this.startListBoard = startListBoard;
	}

}
