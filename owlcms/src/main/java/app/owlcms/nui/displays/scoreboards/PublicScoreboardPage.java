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
import app.owlcms.displays.scoreboard.Results;
import app.owlcms.displays.scoreboard.ResultsMedals;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/publicResults")

public class PublicScoreboardPage extends AbstractResultsDisplayPage {

	Logger logger;
	Logger uiEventLogger;
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private Results resultsBoard;
	protected UI ui;
	private ResultsMedals medalsBoard;

	public PublicScoreboardPage() {
		// intentionally empty. superclass will call init() as required.
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("DisplayParameters.PublicDisplay") + OwlcmsSession.getFopNameIfMultiple();
	}

	public final Results getResultsBoard() {
		return this.resultsBoard;
	}

	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		if (e.getCeremonyType() != CeremonyType.MEDALS) {
			return;
		}
		this.ui.access(() -> {
			getMedalsBoard().getStyle().set("display", "none");
			getResultsBoard().getStyle().set("display", "block");
		});
	}

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		if (e.getCeremonyType() != CeremonyType.MEDALS) {
			return;
		}
		this.ui.access(() -> {
			/* copy current parameters from results board to medals board */
			this.getMedalsBoard().getStyle().set("display","block");
			this.getMedalsBoard().setDownSilenced(true);
			this.getMedalsBoard().setDarkMode(((DisplayParameters) getBoard()).isDarkMode());
			this.getMedalsBoard().setVideo(((DisplayParameters) getBoard()).isVideo());
			this.getMedalsBoard().setPublicDisplay(((DisplayParameters) getBoard()).isPublicDisplay());
			this.getMedalsBoard().setSingleReferee(((SoundParameters) getBoard()).isSingleReferee());
			this.getMedalsBoard().setAbbreviatedName(((DisplayParameters) getBoard()).isAbbreviatedName());
//			this.getMedalsBoard().setTeamWidth(((DisplayParameters) getBoard()).getTeamWidth());
//			this.getMedalsBoard().setEmFontSize(((DisplayParameters) getBoard()).getEmFontSize());
			computeStylesDir(this.getMedalsBoard());
			getMedalsBoard().getStyle().set("display", "block");
			this.getMedalsBoard().syncWithFOP(getFop());
			getResultsBoard().getStyle().set("display", "none");
			pushEmSize(this.getElement());
			pushTeamWidth(this.getElement());
		});
	}

	@Subscribe
	public void slaveVideoRefresh(UIEvent.VideoRefresh e) {
		// this should never have isVideo() in actual practice.

		// logger.debug("videorefresh {}",e.getFop());
		if (!isVideo()) {
			return;
		}
		this.ui.access(() -> {
			/* copy current parameters from results board to medals board */
//			this.getMedalsBoard().setVisible(true);
			this.getMedalsBoard().setDownSilenced(true);
			this.getMedalsBoard().setDarkMode(((DisplayParameters) getBoard()).isDarkMode());
			this.getMedalsBoard().setVideo(((DisplayParameters) getBoard()).isVideo());
			this.getMedalsBoard().setPublicDisplay(((DisplayParameters) getBoard()).isPublicDisplay());
			this.getMedalsBoard().setSingleReferee(((SoundParameters) getBoard()).isSingleReferee());
			this.getMedalsBoard().setAbbreviatedName(((DisplayParameters) getBoard()).isAbbreviatedName());
//			this.getMedalsBoard().setTeamWidth(((DisplayParameters) getBoard()).getTeamWidth());
//			this.getMedalsBoard().setEmFontSize(((DisplayParameters) getBoard()).getEmFontSize());
			computeStylesDir(this.getMedalsBoard());
			getMedalsBoard().getStyle().set("display", "block");
			this.getMedalsBoard().syncWithFOP(getFop());
			getResultsBoard().getStyle().set("display", "none");
			pushEmSize(this.getElement());
			pushTeamWidth(this.getElement());
		});
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
	}
	
	@Override
	final public void setTeamWidth(Double tw) {
		// subjective visual kludging.
		Double medalTw;
		if (tw == null) {
			tw = 9.0;
			medalTw= 9.0;
		} else {
			//medalFontSize = emFontSize * 1.5;
			medalTw = tw;
		}
		super.setTeamWidth(tw);
		pushTeamWidth(getElement(), tw);
		pushTeamWidth(this.getMedalsBoard().getElement(),medalTw);
	}

	
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		uiEventBusRegister(this, getFop());
		
		// overrides common to all enclosed boards
		this.getElement().getStyle().set("--medalOverride", "2em");
		
		DisplayParameters board = (DisplayParameters) this.getBoard();
		board.setFop(getFop());
		getMedalsBoard().setFop(getFop());

		this.setResultsBoard((Results) board);
		this.setMedalsBoard(getMedalsBoard());

		this.addComponent((Component) board);
		this.addComponent(getMedalsBoard());

		((Component) board).getElement().getStyle().set("display","block");
		getMedalsBoard().getElement().getStyle().set("display","none");
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

	protected void setResultsBoard(Results board) {
		this.resultsBoard = board;
	}

	private void createComponents() {
		var board = new Results();
		setMedalsBoard(new ResultsMedals());
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
		this.ui = UI.getCurrent();
	}

	private ResultsMedals getMedalsBoard() {
		return this.medalsBoard;
	}

	private void setMedalsBoard(ResultsMedals medalsBoard) {
		this.medalsBoard = medalsBoard;
	}

}
