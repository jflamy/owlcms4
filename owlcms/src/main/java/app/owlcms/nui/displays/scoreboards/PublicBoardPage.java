package app.owlcms.nui.displays.scoreboards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.config.Config;
import app.owlcms.displays.scoreboard.Results;
import app.owlcms.displays.scoreboard.ResultsMedals;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/resultsPublic")

public class PublicBoardPage extends AbstractResultsDisplayPage {

	Logger logger;
	Logger uiEventLogger;
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private Results resultsBoard;
	private UI ui;
	private ResultsMedals medalsBoard;

	public PublicBoardPage() {
		// intentionally empty. superclass will call init() as required.
	}

	@Override
	public String getPageTitle() {
		return getTranslation("ScoreboardWLeadersTitle") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		this.ui.access(() -> {
			getMedalsBoard().setVisible(false);
			getResultsBoard().setVisible(true);
		});
	}

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		this.ui.access(() -> {
			getMedalsBoard().setDarkMode(getResultsBoard().isDarkMode());
			getMedalsBoard().setTeamWidth(getResultsBoard().getTeamWidth());
			getMedalsBoard().setEmFontSize(getResultsBoard().getEmFontSize());
			getMedalsBoard().setVisible(true);
			getResultsBoard().setVisible(false);
		});
	}

	@Override
	protected void init() {
		logger = (Logger) LoggerFactory.getLogger(PublicBoardPage.class);
		uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());

		// each superclass must this routine.
		// otherwise we end up with multiple instances of the Results board.
		var board = new Results();
		var medalsBoard = new ResultsMedals();
		this.setBoard(board);
		this.setResultsBoard(board);
		this.setMedalsBoard(medalsBoard);
		this.addComponent(board);
		this.addComponent(medalsBoard);
		medalsBoard.setVisible(false);
		this.ui = UI.getCurrent();

		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied.
		setDefaultParameters(QueryParameters.simple(Map.of(
		        SoundParameters.SILENT, "true",
		        SoundParameters.DOWNSILENT, "true",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "true",
		        DisplayParameters.RECORDS, "true",
		        DisplayParameters.VIDEO, "false",
		        DisplayParameters.PUBLIC, "true",
		        SoundParameters.SINGLEREF, "false",
		        DisplayParameters.ABBREVIATED,
		        Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")))));
	}

	private void setMedalsBoard(ResultsMedals medalsBoard) {
		this.medalsBoard = medalsBoard;
	}

	private void setResultsBoard(Results board) {
		this.resultsBoard = board;
	}

	public final Results getResultsBoard() {
		return resultsBoard;
	}

	private final ResultsMedals getMedalsBoard() {
		return medalsBoard;
	}

}
