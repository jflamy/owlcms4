/*******************************************************************************
L * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displayselection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.displays.scoreboard.CurrentAthlete;
import app.owlcms.displays.scoreboard.Results;
import app.owlcms.displays.scoreboard.ResultsLeadersRanks;
import app.owlcms.displays.scoreboard.ResultsLiftingOrder;
import app.owlcms.displays.scoreboard.ResultsMedals;
import app.owlcms.displays.scoreboard.ResultsNoLeaders;
import app.owlcms.displays.scoreboard.ResultsRankingOrder;
import app.owlcms.displays.topathletes.TopSinclair;
import app.owlcms.displays.topteams.TopTeams;
import app.owlcms.displays.topteams.TopTeamsSinclair;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
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
			Button currentAthlete = openInNewTab(CurrentAthlete.class, getTranslation("CurrentAthleteTitle"));
			FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(attempt, currentAthlete);
			doGroup(getTranslation("AttemptBoard"), grid3, this);

			// Button decisions = openInNewTab(AthleteFacingDecisionBoard.class, getTranslation("Athlete_Decisions"));
			Button decisions = openInNewTabNoParam(AthleteFacingDecisionBoardPage.class,
			        getTranslation("Athlete_Decisions"));
			Button athleteFacingAttempt = openInNewTab(AthleteFacingAttemptBoardPage.class,
			        getTranslation("Athlete_Attempt"));
			VerticalLayout intro2 = new VerticalLayout();
			addP(intro2, getTranslation("refereeingDevices"));
			FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(decisions, athleteFacingAttempt);
			doGroup(getTranslation("Refereeing_Displays"), intro2, grid2, this);

			Button scoreboard = openInNewTab(ResultsNoLeaders.class, getTranslation("Scoreboard"));
			Button scoreboardWLeaders = openInNewTab(Results.class, getTranslation("ScoreboardWLeadersButton"));
			scoreboardWLeaders.getElement().setAttribute("title", getTranslation("ScoreboardWLeadersMouseOver"));
			Button scoreboardMultiRanks = openInNewTab(ResultsLeadersRanks.class,
			        getTranslation("ScoreboardMultiRanksButton"));
			Button scoreboardRankings = openInNewTab(ResultsRankingOrder.class,
			        getTranslation("Scoreboard.RankingOrderButton"));
			Button liftingOrder = openInNewTab(ResultsLiftingOrder.class, getTranslation("Scoreboard.LiftingOrder"));
			VerticalLayout intro1 = new VerticalLayout();
			addP(intro1, getTranslation("darkModeSelect"));
			FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(scoreboard, scoreboardWLeaders,
			        scoreboardRankings, scoreboardMultiRanks, liftingOrder);
			doGroup(getTranslation("Scoreboards"), intro1, grid1, this);

			Button medals = openInNewTab(ResultsMedals.class, getTranslation("CeremonyType.MEDALS"));
			Button topSinclair = openInNewTab(TopSinclair.class, getTranslation("Scoreboard.TopSinclair"));
			Button topTeams = openInNewTab(TopTeams.class, getTranslation("Scoreboard.TopTeams"));
			Button topTeamsSinclair = openInNewTab(TopTeamsSinclair.class,
			        getTranslation("Scoreboard.TopTeamsSinclair"));
			VerticalLayout intro11 = new VerticalLayout();
			FlexibleGridLayout grid11 = HomeNavigationContent.navigationGrid(
			        topSinclair, topTeams, topTeamsSinclair, medals);
			doGroup(getTranslation("Scoreboard.RANKINGS"), intro11, grid11, this);

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
	public Location getLocation() {
		return this.location;
	}

	@Override
	public UI getLocationUI() {
		return this.locationUI;
	}

	@Override
	public String getMenuTitle() {
		return getTranslation("StartDisplays");
	}

	@Override
	public String getPageTitle() {
		return getTranslation("ShortTitle.Displays") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	public Map<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
	}

	@Override
	public void setLocation(Location location) {
		this.location = location;
	}

	@Override
	public void setLocationUI(UI locationUI) {
		this.locationUI = locationUI;
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> newParameterMap) {
		this.urlParameterMap = newParameterMap;
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
