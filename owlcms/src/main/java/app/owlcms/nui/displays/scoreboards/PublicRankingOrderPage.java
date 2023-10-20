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
import app.owlcms.displays.scoreboard.ResultsRankingOrder;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/publicRankingOrder")

public class PublicRankingOrderPage extends AbstractResultsDisplayPage {

	Logger logger;
	Logger uiEventLogger;
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private Results resultsBoard;
	protected UI ui;
	private ResultsMedals medalsBoard;

	public PublicRankingOrderPage() {
		// intentionally empty. superclass will call init() as required.

	}

	@Override
	public String getPageTitle() {
		return getTranslation("Scoreboard.RankingOrder") + OwlcmsSession.getFopNameIfMultiple();
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
			this.medalsBoard.setDownSilenced(true);
			this.medalsBoard.setDarkMode(((DisplayParameters) getBoard()).isDarkMode());
			this.medalsBoard.setVideo(((DisplayParameters) getBoard()).isVideo());
			this.medalsBoard.setPublicDisplay(((DisplayParameters) getBoard()).isPublicDisplay());
			this.medalsBoard.setSingleReferee(((SoundParameters) getBoard()).isSingleReferee());
			this.medalsBoard.setAbbreviatedName(((DisplayParameters) getBoard()).isAbbreviatedName());
			this.medalsBoard.setTeamWidth(((DisplayParameters) getBoard()).getTeamWidth());
			this.medalsBoard.setEmFontSize(((DisplayParameters) getBoard()).getEmFontSize());
			checkVideo(this.medalsBoard);
			getMedalsBoard().getStyle().set("display", "block");

			getResultsBoard().getStyle().set("display", "none");
		});
	}

	protected void createComponents() {
		var board = new ResultsRankingOrder();
		var medalsBoard = new ResultsMedals();

		this.setBoard(board);
		this.setResultsBoard(board);
		this.setMedalsBoard(medalsBoard);
		this.addComponent(board);
		this.addComponent(medalsBoard);

		medalsBoard.setDownSilenced(true);
		medalsBoard.setDarkMode(board.isDarkMode());
		medalsBoard.setVideo(board.isVideo());
		medalsBoard.setPublicDisplay(board.isPublicDisplay());
		medalsBoard.setSingleReferee(board.isSingleReferee());
		medalsBoard.setAbbreviatedName(board.isAbbreviatedName());
		medalsBoard.setTeamWidth(board.getTeamWidth());
		medalsBoard.setEmFontSize(board.getEmFontSize());
		checkVideo(medalsBoard);

		medalsBoard.getStyle().set("display", "none");
		this.ui = UI.getCurrent();
	}

	@Override
	protected void init() {
		this.logger = (Logger) LoggerFactory.getLogger(this.getClass());
		this.uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
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

	protected void setMedalsBoard(ResultsMedals medalsBoard) {
		this.medalsBoard = medalsBoard;
	}

	protected void setResultsBoard(Results board) {
		this.resultsBoard = board;
	}

	private final ResultsMedals getMedalsBoard() {
		return this.medalsBoard;
	}
}
