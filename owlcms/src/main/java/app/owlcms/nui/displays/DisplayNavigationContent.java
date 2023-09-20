/*******************************************************************************
L * Copyright (c) 2009-2023 Jean-François Lamy
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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.displays.attemptboards.AthleteFacingAttemptBoardPage;
import app.owlcms.nui.displays.attemptboards.AthleteFacingDecisionBoardPage;
import app.owlcms.nui.displays.attemptboards.PublicFacingAttemptBoardPage;
import app.owlcms.nui.displays.scoreboards.CurrentAthletePage;
import app.owlcms.nui.displays.scoreboards.PublicScoreboardPage;
import app.owlcms.nui.displays.scoreboards.PublicMultiRanksPage;
import app.owlcms.nui.displays.scoreboards.PublicNoLeadersPage;
import app.owlcms.nui.displays.scoreboards.PublicRankingOrderPage;
import app.owlcms.nui.displays.scoreboards.WarmupScoreboardPage;
import app.owlcms.nui.displays.scoreboards.WarmupMultiRanksPage;
import app.owlcms.nui.displays.scoreboards.WarmupLiftingOrderPage;
import app.owlcms.nui.displays.scoreboards.MedalsPage;
import app.owlcms.nui.displays.scoreboards.WarmupNoLeadersPage;
import app.owlcms.nui.displays.top.TopSinclairPage;
import app.owlcms.nui.displays.top.TopTeamsPage;
import app.owlcms.nui.displays.top.TopTeamsSinclairPage;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.RequireDisplayLogin;
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
	Map<String, List<String>> urlParameterMap = new HashMap<String, List<String>>();

	/**
	 * Instantiates a new display navigation content.
	 */
	public DisplayNavigationContent() {
		try {
			VerticalLayout intro = new VerticalLayout();
			intro.setSpacing(false);
			addP(intro, getTranslation("Dropdown_Select_Platform"));
			addP(intro, getTranslation("Button_Open_Display"));
			intro.getStyle().set("margin-bottom", "0");
			fillH(intro, this);

			Button attempt = openInNewTab(PublicFacingAttemptBoardPage.class, getTranslation("AttemptBoard"));
			Button currentAthlete = openInNewTab(CurrentAthletePage.class, getTranslation("CurrentAthleteTitle"));
			FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(attempt, currentAthlete);
			doGroup(getTranslation("AttemptBoard"), grid3, this);

			Button decisions = openInNewTabNoParam(AthleteFacingDecisionBoardPage.class,
			        getTranslation("Athlete_Decisions"));
			Button athleteFacingAttempt = openInNewTab(AthleteFacingAttemptBoardPage.class,
			        getTranslation("Athlete_Attempt"));
			VerticalLayout intro2 = new VerticalLayout();
			addP(intro2, getTranslation("refereeingDevices"));
			FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(decisions, athleteFacingAttempt);
			doGroup(getTranslation("Refereeing_Displays"), intro2, grid2, this);

			Button scoreboard = openInNewTab(WarmupNoLeadersPage.class, getTranslation("Scoreboard"));
			Button scoreboardWLeaders = openInNewTab(WarmupScoreboardPage.class,
			        getTranslation("ScoreboardWLeadersButton"));
			scoreboardWLeaders.getElement().setAttribute("title", getTranslation("ScoreboardWLeadersMouseOver"));
			Button scoreboardMultiRanks = openInNewTab(WarmupMultiRanksPage.class,
			        getTranslation("ScoreboardMultiRanksButton"));
			Button liftingOrder = openInNewTab(WarmupLiftingOrderPage.class,
			        getTranslation("Scoreboard.LiftingOrder"));
			VerticalLayout intro1 = new VerticalLayout();
			addP(intro1, getTranslation("WarmupScoreboards.explanation"));
			FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(
					scoreboard,
			        scoreboardWLeaders,
			        liftingOrder,
			        scoreboardMultiRanks
			        );
			doGroup(getTranslation("WarmupScoreboards"), intro1, grid1, this);
			
			Button scoreboard1 = openInNewTab(PublicNoLeadersPage.class, getTranslation("Scoreboard"));
			Button scoreboardWLeaders1 = openInNewTab(PublicScoreboardPage.class,
			        getTranslation("ScoreboardWLeadersButton"));
			scoreboardWLeaders1.getElement().setAttribute("title", getTranslation("ScoreboardWLeadersMouseOver"));
			Button scoreboardMultiRanks1 = openInNewTab(PublicMultiRanksPage.class,
			        getTranslation("ScoreboardMultiRanksButton"));
			Button scoreboardRankings1 = openInNewTab(PublicRankingOrderPage.class,
			        getTranslation("Scoreboard.RankingOrderButton"));
			VerticalLayout intro11 = new VerticalLayout();
			addP(intro11, getTranslation("PublicScoreboards.explanation"));
			FlexibleGridLayout grid11 = HomeNavigationContent.navigationGrid(
					scoreboard1,
			        scoreboardWLeaders1,
			        scoreboardRankings1, 
			        scoreboardMultiRanks1);
			doGroup(getTranslation("PublicScoreboards"), intro11, grid11, this);

			Button medals = openInNewTab(MedalsPage.class, getTranslation("CeremonyType.MEDALS"));
			Button topSinclair = openInNewTab(TopSinclairPage.class, getTranslation("Scoreboard.TopSinclair"));
			Button topTeams = openInNewTab(TopTeamsPage.class, getTranslation("Scoreboard.TopTeams"));
			Button topTeamsSinclair = openInNewTab(TopTeamsSinclairPage.class,
			        getTranslation("Scoreboard.TopTeamsSinclair"));
			VerticalLayout intro111 = new VerticalLayout();
			FlexibleGridLayout grid111 = HomeNavigationContent.navigationGrid(
			        topSinclair,
			        topTeams,
			        topTeamsSinclair,
			        medals);
			doGroup(getTranslation("Scoreboard.RANKINGS"), intro111, grid111, this);

			DebugUtils.gc();
		} catch (Throwable x) {
			x.printStackTrace();
		}
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		RequireDisplayLogin.super.beforeEnter(event);
	}

	@Override
	public String getMenuTitle() {
		return getTranslation("StartDisplays");
	}

	@Override
	public String getPageTitle() {
		return getTranslation("ShortTitle.Displays") + OwlcmsSession.getFopNameIfMultiple();
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
		OwlcmsSession.withFop((fop) -> {
			fopSelect.setValue(fop);
		});
		fopSelect.addValueChangeListener(e -> {
			OwlcmsSession.setFop(e.getValue());
			updateURLLocation(getLocationUI(), getLocation(), null);
		});

		HorizontalLayout fopField = new HorizontalLayout(fopLabel, fopSelect);
		fopField.setAlignItems(Alignment.CENTER);
		return fopField;
	}
}
