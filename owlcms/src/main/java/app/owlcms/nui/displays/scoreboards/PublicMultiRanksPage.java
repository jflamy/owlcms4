package app.owlcms.nui.displays.scoreboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.config.Config;
import app.owlcms.displays.scoreboard.ResultsMedals;
import app.owlcms.displays.scoreboard.ResultsMultiRanks;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/publicMultiRanks")

public class PublicMultiRanksPage extends AbstractResultsDisplayPage {

	Logger logger;
	Logger uiEventLogger;

	private ResultsMultiRanks resultsBoard;
	private ResultsMedals medalsBoard;
	protected UI ui;

	@Override
	public String getPageTitle() {
		return getTranslation("ScoreboardMultiRanksTitle") + OwlcmsSession.getFopNameIfMultiple();
	}

	public final ResultsMultiRanks getResultsBoard() {
		return this.resultsBoard;
	}

	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		if (e.getCeremonyType() != CeremonyType.MEDALS) {
			return;
		}
		this.ui.access(() -> {
			getMedalsBoard().setVisible(false);
			getResultsBoard().setVisible(true);
		});
	}

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		if (e.getCeremonyType() != CeremonyType.MEDALS) {
			return;
		}
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
		this.logger = (Logger) LoggerFactory.getLogger(PublicScoreboardPage.class);
		this.uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());

		// each subclass must override this routine.
		// otherwise we end up with multiple instances of the Results board.
		var board = new ResultsMultiRanks();
		var medalsBoard = new ResultsMedals();
		this.setBoard(board);
		this.setResultsBoard(board);
		this.setMedalsBoard(medalsBoard);
		this.addComponent(board);
		this.addComponent(medalsBoard);
		medalsBoard.setVisible(false);
		this.ui = UI.getCurrent();

		setDefaultParameters(QueryParameters.simple(Map.of(
				SoundParameters.SILENT, "true",
				SoundParameters.DOWNSILENT, "true",
				DisplayParameters.DARK, "true",
				DisplayParameters.LEADERS, "true",
				DisplayParameters.RECORDS, "true",
				SoundParameters.SINGLEREF, "false",
				DisplayParameters.PUBLIC, "false",
				DisplayParameters.ABBREVIATED,
				Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")))));
	}

	private final ResultsMedals getMedalsBoard() {
		return this.medalsBoard;
	}

	private void setMedalsBoard(ResultsMedals medalsBoard) {
		this.medalsBoard = medalsBoard;
	}

	private void setResultsBoard(ResultsMultiRanks board) {
		this.resultsBoard = board;
	}

}
