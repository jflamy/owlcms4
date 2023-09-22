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
		return getTranslation("DisplayParameters.PublicDisplay") + OwlcmsSession.getFopNameIfMultiple();
	}

	public final Results getResultsBoard() {
		return this.resultsBoard;
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

	/**
	 * We make this method final because we want to force the {@link #createComponents()} method
	 * to be called so the assumptions made by switching from scoreboard to medals are met.
	 * 
	 * @see app.owlcms.nui.displays.scoreboards.AbstractResultsDisplayPage#init()
	 */
	@Override
	protected final void init() {
		@SuppressWarnings("unused")
		Logger logger = (Logger) LoggerFactory.getLogger(this.getClass());
		createComponents();
		setDefaultParameters();
	}

	protected void setDefaultParameters() {
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

	protected void createComponents() {
		this.logger = (Logger) LoggerFactory.getLogger(PublicScoreboardPage.class);
		this.uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());

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
	}

	private final ResultsMedals getMedalsBoard() {
		return this.medalsBoard;
	}

	private void setMedalsBoard(ResultsMedals medalsBoard) {
		this.medalsBoard = medalsBoard;
	}

	private void setResultsBoard(Results board) {
		this.resultsBoard = board;
	}
}
