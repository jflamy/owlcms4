package app.owlcms.nui.displays.scoreboards;

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
import app.owlcms.displays.scoreboard.ResultsMedals;
import app.owlcms.displays.scoreboard.ResultsMultiRanks;
import app.owlcms.i18n.Translator;
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
		return Translator.translate("ScoreboardMultiRanksTitle") + OwlcmsSession.getFopNameIfMultiple();
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
		this.setMedalsBoard(new ResultsMedals());
		this.setBoard(board);
		this.setResultsBoard(board);

		this.ui = UI.getCurrent();

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
		board.setFop(getFop());
		getMedalsBoard().setFop(getFop());
		
		this.setResultsBoard((ResultsMultiRanks) board);
		this.setMedalsBoard(getMedalsBoard());
		
		this.addComponent((Component) board);
		getMedalsBoard().setVisible(false);
		this.addComponent(getMedalsBoard());
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
