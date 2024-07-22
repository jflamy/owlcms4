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
import app.owlcms.displays.scoreboard.ResultsRankingOrder;
import app.owlcms.i18n.Translator;
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
		return Translator.translate("Scoreboard.RankingOrder") + OwlcmsSession.getFopNameIfMultiple();
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
			this.getMedalsBoard().setDownSilenced(true);
			this.getMedalsBoard().setDarkMode(((DisplayParameters) getBoard()).isDarkMode());
			this.getMedalsBoard().setVideo(((DisplayParameters) getBoard()).isVideo());
			this.getMedalsBoard().setPublicDisplay(((DisplayParameters) getBoard()).isPublicDisplay());
			this.getMedalsBoard().setSingleReferee(((SoundParameters) getBoard()).isSingleReferee());
			this.getMedalsBoard().setAbbreviatedName(((DisplayParameters) getBoard()).isAbbreviatedName());
			this.getMedalsBoard().setTeamWidth(((DisplayParameters) getBoard()).getTeamWidth());
			this.getMedalsBoard().setEmFontSize(((DisplayParameters) getBoard()).getEmFontSize());
			checkVideo(this.getMedalsBoard());
			getMedalsBoard().getStyle().set("display", "block");

			getResultsBoard().getStyle().set("display", "none");
		});
	}

	protected void createComponents() {
		var board = new ResultsRankingOrder();
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
		checkVideo(getMedalsBoard());

		getMedalsBoard().getStyle().set("display", "none");
		this.ui = UI.getCurrent();
	}
	
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		DisplayParameters board = (DisplayParameters) this.getBoard();
		board.setFop(getFop());
		getMedalsBoard().setFop(getFop());
		
		this.setResultsBoard((Results) board);
		this.setMedalsBoard(getMedalsBoard());
		
		this.addComponent((Component) board);
		getMedalsBoard().setVisible(false);
		this.addComponent(getMedalsBoard());
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

	private void setMedalsBoard(ResultsMedals medalsBoard) {
		this.medalsBoard = medalsBoard;
	}

	protected void setResultsBoard(Results board) {
		this.resultsBoard = board;
	}

	private final ResultsMedals getMedalsBoard() {
		return this.medalsBoard;
	}
}
