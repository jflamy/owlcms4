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

import app.owlcms.apputils.DebugUtils;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.competition.Competition;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.displays.attemptboards.AthleteFacingAttemptBoardPage;
import app.owlcms.nui.displays.attemptboards.AthleteFacingDecisionBoardPage;
import app.owlcms.nui.displays.attemptboards.PublicFacingAttemptBoardPage;
import app.owlcms.nui.displays.scoreboards.CurrentAthletePage;
import app.owlcms.nui.displays.scoreboards.JuryDecisionsPage;
import app.owlcms.nui.displays.scoreboards.JuryScoreboardPage;
import app.owlcms.nui.displays.scoreboards.MedalsPage;
import app.owlcms.nui.displays.scoreboards.PublicMultiRanksPage;
import app.owlcms.nui.displays.scoreboards.PublicNoLeadersPage;
import app.owlcms.nui.displays.scoreboards.PublicRankingOrderPage;
import app.owlcms.nui.displays.scoreboards.PublicScoreboardPage;
import app.owlcms.nui.displays.scoreboards.RankingsPage;
import app.owlcms.data.config.Config;
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
			Button currentAthlete = openInNewTabWithFop(CurrentAthletePage.class, Translator.translate("CurrentAthleteTitle"));
			FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(attempt, currentAthlete);
			doGroup(Translator.translate("AttemptBoard"), grid3, this);

			Button decisions = openInNewTabNoParam(AthleteFacingDecisionBoardPage.class,
			        Translator.translate("Athlete_Decisions"));
			highlight(decisions);
			Button athleteFacingAttempt = openInNewTabWithFop(AthleteFacingAttemptBoardPage.class,
			        Translator.translate("Athlete_Attempt"));
			VerticalLayout intro2 = new VerticalLayout();
			addP(intro2, Translator.translate("refereeingDevices"));
			FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(decisions, athleteFacingAttempt);
			doGroup(Translator.translate("Refereeing_Displays"), intro2, grid2, this);

			Button scoreboard = openInNewTabWithFopCurrentAttempt(WarmupNoLeadersPage.class, Translator.translate("Scoreboard"));
			highlight(scoreboard);
			Button scoreboardWLeaders = openInNewTabWithFopCurrentAttempt(WarmupScoreboardPage.class,
			        Translator.translate("ScoreboardWLeadersButton"));
			scoreboardWLeaders.getElement().setAttribute("title", Translator.translate("ScoreboardWLeadersMouseOver"));
			Button scoreboardMultiRanks = openInNewTabWithFopCurrentAttempt(WarmupMultiRanksPage.class,
			        Translator.translate("ScoreboardMultiRanksButton"));
			Button liftingOrder = openInNewTabWithFopCurrentAttempt(WarmupLiftingOrderPage.class,
			        Translator.translate("Scoreboard.LiftingOrder"));
			VerticalLayout intro1 = new VerticalLayout();
			addP(intro1, Translator.translate("WarmupScoreboards.explanation"));
			Button juryScoreboard = openInNewTabWithFopCurrentAttempt(JuryScoreboardPage.class,
			        Translator.translate("JuryScoreboard.Title"));
			scoreboardWLeaders.getElement().setAttribute("title", Translator.translate("ScoreboardWLeadersMouseOver"));
			FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(
			        scoreboard,
			        scoreboardWLeaders,
			        liftingOrder,
			        scoreboardMultiRanks,
			        juryScoreboard);
			doGroup(Translator.translate("WarmupScoreboards"), intro1, grid1, this);

			Button scoreboard1 = openInNewTabWithFopNoCurrentAttempt(PublicNoLeadersPage.class, Translator.translate("Scoreboard"));
			Button scoreboardWLeaders1 = openInNewTabWithFopNoCurrentAttempt(PublicScoreboardPage.class,
			        Translator.translate("ScoreboardWLeadersButton"));
			scoreboardWLeaders1.getElement().setAttribute("title", Translator.translate("ScoreboardWLeadersMouseOver"));
			Button scoreboardMultiRanks1 = openInNewTabWithFopNoCurrentAttempt(PublicMultiRanksPage.class,
			        Translator.translate("ScoreboardMultiRanksButton"));
			// Default: RankingsPage (no current athlete). Toggle: PublicRankingOrderPage (with current athlete)
			Button scoreboardRankings1 = Config.getCurrent().featureSwitch("rankingsWithCurrentAttempt")
			        ? openInNewTabWithFopNoCurrentAttempt(PublicRankingOrderPage.class,
			                Translator.translate("Scoreboard.RankingOrderButton"))
			        : openInNewTabWithFop(RankingsPage.class,
			                Translator.translate("Scoreboard.RankingOrderButton"));
			Button startList1 = openInNewTabWithFopNoCurrentAttempt(PublicStartListPage.class, Translator.translate("Scoreboard.StartList"));
			Button juryDecisions1 = openInNewTabWithFopNoCurrentAttempt(JuryDecisionsPage.class,
			        Translator.translate("JuryDecisions.Title"));
			VerticalLayout intro11 = new VerticalLayout();
			addP(intro11, Translator.translate("PublicScoreboards.explanation"));
			FlexibleGridLayout grid11 = HomeNavigationContent.navigationGrid(
			        scoreboard1,
			        scoreboardWLeaders1,
			        scoreboardRankings1,
			        scoreboardMultiRanks1,
			        startList1,
			        juryDecisions1);
			doGroup(Translator.translate("PublicScoreboards"), intro11, grid11, this);

			Ranking scoringSystem = Competition.getCurrent().getScoringSystem();
			String scoringTitle = Ranking.getScoringTitle(scoringSystem);

			Button medals = openInNewTabWithFop(MedalsPage.class, Translator.translate("CeremonyType.MEDALS"));
			Button topSinclair = openInNewTabWithFop(TopSinclairPage.class,
			        Translator.translate("Scoreboard.TopScore", scoringTitle));
			Button topTeams = openInNewTabWithFop(TopTeamsPage.class, Translator.translate("Scoreboard.TopTeams"));
			Button topTeamsSinclair = openInNewTabWithFop(TopTeamsSinclairPage.class,
			        Translator.translate("Scoreboard.TopTeamsScore", scoringTitle));
			VerticalLayout intro111 = new VerticalLayout();
			FlexibleGridLayout grid111 = HomeNavigationContent.navigationGrid(
			        topTeams,
			        medals,
			        topSinclair,
			        topTeamsSinclair);

			if (scoringSystem == Ranking.ROBI) {
				topSinclair.setEnabled(false);
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
}
