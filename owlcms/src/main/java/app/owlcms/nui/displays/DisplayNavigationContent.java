/*******************************************************************************
L * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.apputils.DebugUtils;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.displays.attemptboards.AthleteFacingAttemptBoardPage;
import app.owlcms.nui.displays.attemptboards.AthleteFacingDecisionBoardPage;
import app.owlcms.nui.displays.attemptboards.PublicFacingAttemptBoardPage;
import app.owlcms.nui.displays.scoreboards.JuryDecisionsPage;
import app.owlcms.nui.displays.scoreboards.JuryScoreboardPage;
import app.owlcms.nui.displays.scoreboards.MedalsPage;
import app.owlcms.nui.displays.scoreboards.PublicMedalsPage;
import app.owlcms.nui.displays.scoreboards.PublicMultiRanksPage;
import app.owlcms.nui.displays.scoreboards.PublicNoLeadersPage;
import app.owlcms.nui.displays.scoreboards.PublicRankingOrderPage;
import app.owlcms.nui.displays.scoreboards.PublicScoreboardPage;
import app.owlcms.nui.displays.scoreboards.WarmupRankingOrderPage;
import app.owlcms.nui.displays.scoreboards.PublicStartListPage;
import app.owlcms.nui.displays.scoreboards.WarmupLiftingOrderPage;
import app.owlcms.nui.displays.scoreboards.WarmupMultiRanksPage;
import app.owlcms.nui.displays.scoreboards.WarmupNoLeadersPage;
import app.owlcms.nui.displays.scoreboards.WarmupScoreboardPage;
import app.owlcms.nui.displays.top.TopSinclairPage;
import app.owlcms.nui.displays.top.TopTeamsPage;
import app.owlcms.nui.displays.top.TopTeamsSinclairPage;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.RequireDisplayLogin;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class DisplayNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "displays", layout = OwlcmsLayout.class)
public class DisplayNavigationContent extends BaseNavigationContent
        implements NavigationPage, HasDynamicTitle, RequireDisplayLogin {

	final static Logger logger = (Logger) LoggerFactory.getLogger(DisplayNavigationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	/**
	 * Instantiates a new display navigation content.
	 */
	public DisplayNavigationContent() {
		try {
			VerticalLayout intro = new VerticalLayout();
			intro.setSpacing(false);
			addP(intro, Translator.translate("Dropdown_Select_Platform"));
			addP(intro, Translator.translate("Button_Open_Display"));
			intro.getStyle().set("margin-bottom", "0");
			fillH(intro, this);

			Button attempt = openInNewTabWithFop(PublicFacingAttemptBoardPage.class, Translator.translate("AttemptBoard"));
			highlight(attempt);
			FlexibleGridLayout warmupAttemptGrid = HomeNavigationContent.navigationGrid(attempt);

			Button decisions = openInNewTabNoParam(AthleteFacingDecisionBoardPage.class,
			        Translator.translate("Athlete_Decisions"));
			highlight(decisions);
			Button athleteFacingAttempt = openInNewTabWithFop(AthleteFacingAttemptBoardPage.class,
			        Translator.translate("Athlete_Attempt"));
			VerticalLayout warmupDevicesIntro = new VerticalLayout();
			addP(warmupDevicesIntro, Translator.translate("refereeingDevices"));
			FlexibleGridLayout warmupDevicesGrid = HomeNavigationContent.navigationGrid(decisions, athleteFacingAttempt);

			Button scoreboard = openInNewTabWithFopCurrentAttempt(WarmupNoLeadersPage.class, Translator.translate("Scoreboard"));
			highlight(scoreboard);
			Button scoreboardWLeaders = openInNewTabWithFopCurrentAttempt(WarmupScoreboardPage.class,
			        Translator.translate("ScoreboardWLeadersButton"));
			scoreboardWLeaders.getElement().setAttribute("title", Translator.translate("ScoreboardWLeadersMouseOver"));
			Button scoreboardMultiRanks = openInNewTabWithFopCurrentAttempt(WarmupMultiRanksPage.class,
			        Translator.translate("ScoreboardMultiRanksButton"));
			Button liftingOrder = openInNewTabWithFopCurrentAttempt(WarmupLiftingOrderPage.class,
			        Translator.translate("Scoreboard.LiftingOrder"));
			Button startList = openInNewTabWithFopQueryParameters(PublicStartListPage.class,
			        Translator.translate("Scoreboard.StartList"), "currentAttempt=true&public=false");
			Button scoreboardRankings = openInNewTabWithFop(WarmupRankingOrderPage.class,
			        Translator.translate("Scoreboard.RankingOrderButton"));
			Button medals = openInNewTabWithFop(MedalsPage.class, Translator.translate("CeremonyType.MEDALS"));
			VerticalLayout warmupSectionIntro = new VerticalLayout();
			addP(warmupSectionIntro, Translator.translate("WarmupScoreboards.navigationExplanation"));
			VerticalLayout warmupAttemptIntro = new VerticalLayout();
			addP(warmupAttemptIntro, Translator.translate("WarmupScoreboards.attemptBoardExplanation"));
			Button juryScoreboard = openInNewTabWithFopCurrentAttempt(JuryScoreboardPage.class,
			        Translator.translate("JuryScoreboard.Title"));
			scoreboardWLeaders.getElement().setAttribute("title", Translator.translate("ScoreboardWLeadersMouseOver"));
			FlexibleGridLayout warmupScoreboardsGrid = HomeNavigationContent.navigationGrid(
			        scoreboard,
			        scoreboardWLeaders,
			        liftingOrder,
			        scoreboardMultiRanks,
			        startList,
			        scoreboardRankings,
			        medals);
			VerticalLayout warmupScoreboardsIntro = new VerticalLayout();
			addP(warmupScoreboardsIntro, Translator.translate("WarmupScoreboards.explanation"));
			doScoreboardSection(Translator.translate("WarmupScoreboards"), warmupSectionIntro, warmupAttemptIntro,
			        warmupAttemptGrid,
			        warmupDevicesIntro, warmupDevicesGrid, warmupScoreboardsIntro, warmupScoreboardsGrid);

			Button scoreboard1 = openInNewTabWithFopNoCurrentAttempt(PublicNoLeadersPage.class, Translator.translate("Scoreboard"));
			Button scoreboardWLeaders1 = openInNewTabWithFopNoCurrentAttempt(PublicScoreboardPage.class,
			        Translator.translate("ScoreboardWLeadersButton"));
			scoreboardWLeaders1.getElement().setAttribute("title", Translator.translate("ScoreboardWLeadersMouseOver"));
			Button scoreboardMultiRanks1 = openInNewTabWithFopNoCurrentAttempt(PublicMultiRanksPage.class,
			        Translator.translate("ScoreboardMultiRanksButton"));
			Button liftingOrder1 = openInNewTabWithFopQueryParameters(WarmupLiftingOrderPage.class,
			        Translator.translate("Scoreboard.LiftingOrder"), "currentAttempt=false&public=true");
			Button scoreboardRankings1 = openInNewTabWithFopQueryParameters(PublicRankingOrderPage.class,
			        Translator.translate("Scoreboard.RankingOrderButton"), "currentAttempt=false&showMedals=true");
			Button startList1 = openInNewTabWithFopNoCurrentAttempt(PublicStartListPage.class, Translator.translate("Scoreboard.StartList"));
			Button publicStyledAttemptBoard = openInNewTabWithFopQueryParameters(PublicFacingAttemptBoardPage.class,
			        Translator.translate("AttemptBoard"), "public=true");
			Button publicStyledAthleteAttemptBoard = openInNewTabWithFopQueryParameters(AthleteFacingAttemptBoardPage.class,
			        Translator.translate("Athlete_Attempt"), "public=true");
			Button publicStyledCountdown = openInNewTabWithFopQueryParameters(AthleteFacingDecisionBoardPage.class,
			        Translator.translate("Athlete_Decisions"), "public=true");
			Button juryDecisions1 = openInNewTabWithFopNoCurrentAttempt(JuryDecisionsPage.class,
			        Translator.translate("JuryDecisions.Title"));
			Button publicMedals1 = openInNewTabWithFop(PublicMedalsPage.class, Translator.translate("CeremonyType.MEDALS"));
			VerticalLayout publicSectionIntro = new VerticalLayout();
			addP(publicSectionIntro, Translator.translate("PublicScoreboards.navigationExplanation"));
			VerticalLayout publicAttemptIntro = new VerticalLayout();
			addP(publicAttemptIntro, Translator.translate("PublicScoreboards.attemptBoardExplanation"));
			FlexibleGridLayout publicAttemptGrid = HomeNavigationContent.navigationGrid(publicStyledAttemptBoard);
			VerticalLayout publicDevicesIntro = new VerticalLayout();
			addP(publicDevicesIntro, Translator.translate("refereeingDevices"));
			FlexibleGridLayout publicDevicesGrid = HomeNavigationContent.navigationGrid(
			        publicStyledCountdown,
			        publicStyledAthleteAttemptBoard);
			FlexibleGridLayout publicScoreboardsGrid = HomeNavigationContent.navigationGrid(
			        scoreboard1,
			        scoreboardWLeaders1,
			        liftingOrder1,
			        scoreboardMultiRanks1,
			        startList1,
			        scoreboardRankings1,
			        publicMedals1);
			VerticalLayout publicScoreboardsIntro = new VerticalLayout();
			addP(publicScoreboardsIntro, Translator.translate("PublicScoreboards.explanation"));
			doScoreboardSection(Translator.translate("PublicScoreboards"), publicSectionIntro, publicAttemptIntro,
			        publicAttemptGrid,
			        publicDevicesIntro, publicDevicesGrid, publicScoreboardsIntro, publicScoreboardsGrid);

			FlexibleGridLayout juryGrid = HomeNavigationContent.navigationGrid(
			        juryScoreboard,
			        juryDecisions1);
			doGroup(Translator.translate("Jury"), juryGrid, this);

			Ranking bestAthleteScoring = Championship.of(null).getBestAthleteScoringSystem();
			String bestAthleteTitle = Ranking.getScoringTitle(bestAthleteScoring);
			Ranking teamScoring = Championship.of(null).getTeamScoringSystem() != null
			        ? Championship.of(null).getTeamScoringSystem()
			        : Ranking.TOTAL;
			String teamScoringTitle = Ranking.getScoringTitle(teamScoring);

			Button topSinclair = openInNewTabWithFop(TopSinclairPage.class,
			        Translator.translate("Scoreboard.TopScore", bestAthleteTitle));
			Button topTeams = openInNewTabWithFop(TopTeamsPage.class, Translator.translate("Scoreboard.TopTeams"));
			Button topTeamsSinclair = openInNewTabWithFop(TopTeamsSinclairPage.class,
			        Translator.translate("Scoreboard.TopTeamsScore", teamScoringTitle));
			VerticalLayout intro111 = new VerticalLayout();
			FlexibleGridLayout grid111 = HomeNavigationContent.navigationGrid(
			        topTeams,
			        topSinclair,
			        topTeamsSinclair);

			if (bestAthleteScoring == Ranking.ROBI) {
				topSinclair.setEnabled(false);
			}
			if (teamScoring == Ranking.ROBI) {
				topTeamsSinclair.setEnabled(false);
			}
			doGroup(Translator.translate("Scoreboard.RankingOrder"), intro111, grid111, this);

			DebugUtils.gc();
		} catch (Throwable x) {
			LoggerUtils.logError(logger, x);
		}
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		RequireDisplayLogin.super.beforeEnter(event);
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("StartDisplays");
	}

	@Override
	public String getPageTitle() {
		String suffix = FieldOfPlay.getFopNameIfMultiple(getFop());
		return Translator.translate("ShortTitle.Displays") + suffix;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see app.owlcms.nui.home.BaseNavigationContent#createTopBarFopField(java.lang. String, java.lang.String)
	 */
	@Override
	protected HorizontalLayout createMenuBarFopField(String label, String placeHolder) {
		NativeLabel fopLabel = new NativeLabel(label);
		formatLabel(fopLabel);

		ComboBox<FieldOfPlay> fopSelect = createFopSelect(placeHolder);
		fopSelect.setValue(getFop());
		fopSelect.addValueChangeListener(e -> {
			setFop(e.getValue());
			updateURLLocation(getLocationUI(), getLocation(), null);
		});

		HorizontalLayout fopField = new HorizontalLayout(fopLabel, fopSelect);
		fopField.setAlignItems(Alignment.CENTER);
		return fopField;
	}

	private void highlight(Button button) {
		button.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
	}

	private void doScoreboardSection(String label, VerticalLayout sectionIntro, VerticalLayout attemptIntro,
	        FlexibleGridLayout attemptGrid, VerticalLayout devicesIntro, FlexibleGridLayout devicesGrid, VerticalLayout scoreboardsIntro,
	        FlexibleGridLayout scoreboardsGrid) {
		VerticalLayout section = new VerticalLayout();
		section.setSpacing(false);
		section.setPadding(false);
		addSectionIntro(label, sectionIntro, section);

		addIntroBlock(attemptIntro, section);
		addGridBlock(attemptGrid, section);
		addIntroBlock(devicesIntro, section);
		addGridBlock(devicesGrid, section);
		addIntroBlock(scoreboardsIntro, section);
		addGridBlock(scoreboardsGrid, section);
		fillH(section, this);
	}

	private void addSectionIntro(String label, VerticalLayout intro, VerticalLayout section) {
		VerticalLayout content = new VerticalLayout();
		content.setSpacing(false);
		content.setPadding(true);
		NativeLabel heading = new NativeLabel(label);
		heading.getStyle().set("margin-bottom", "0.8ex");
		heading.getStyle().set("font-weight", "bold");
		content.add(heading);
		intro.setPadding(false);
		intro.getStyle().set("padding-left", "0");
		content.add(intro);
		content.getStyle().set("margin-bottom", "-1ex");
		fillH(content, section);
	}

	private void addIntroBlock(VerticalLayout intro, VerticalLayout section) {
		intro.setPadding(false);
		intro.getStyle().set("padding-left", "1em");
		intro.getStyle().set("padding-top", "1em");
		fillH(intro, section);
	}

	private void addGridBlock(FlexibleGridLayout grid, VerticalLayout section) {
		grid.setPadding(false);
		grid.getStyle().set("padding-left", "1em");
		fillH(grid, section);
	}
}
