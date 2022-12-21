/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displayselection;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.data.config.Config;
import app.owlcms.displays.attemptboard.AthleteFacingAttemptBoard;
import app.owlcms.displays.attemptboard.AthleteFacingDecisionBoard;
import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.displays.attemptboard.PublicFacingDecisionBoard;
import app.owlcms.displays.monitor.OBSMonitor;
import app.owlcms.displays.scoreboard.CurrentAthlete;
import app.owlcms.displays.scoreboard.Medals;
import app.owlcms.displays.scoreboard.Results;
import app.owlcms.displays.scoreboard.ResultsLeadersRanks;
import app.owlcms.displays.scoreboard.ResultsLiftingOrder;
import app.owlcms.displays.scoreboard.ResultsMedals;
import app.owlcms.displays.scoreboard.ResultsNoLeaders;
import app.owlcms.displays.scoreboard.ScoreMultiRanks;
import app.owlcms.displays.scoreboard.ScoreWithLeaders;
import app.owlcms.displays.scoreboard.Scoreboard;
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
@Route(value = "ndisplays", layout = OwlcmsLayout.class)
public class DisplayNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle, RequireDisplayLogin {

    final static Logger logger = (Logger) LoggerFactory.getLogger(DisplayNavigationContent.class);
    static {
        logger.setLevel(Level.INFO);
    }

    /**
     * Instantiates a new display navigation content.
     */
    public DisplayNavigationContent() {
        VerticalLayout intro = new VerticalLayout();
        addP(intro, getTranslation("Dropdown_Select_Platform"));
        addP(intro, getTranslation("Button_Open_Display"));
        intro.getStyle().set("margin-bottom", "0");

        Button attempt = openInNewTab(AttemptBoard.class, getTranslation("AttemptBoard"));
        Button athleteFacingAttempt = openInNewTab(AthleteFacingAttemptBoard.class, getTranslation("Athlete_Attempt"));
        Button decisions = openInNewTab(AthleteFacingDecisionBoard.class, getTranslation("Athlete_Decisions"));

        Button scoreboard;
        Button scoreboardWLeaders;
        Button scoreboardMultiRanks;
        Button currentAthlete;
        Button medals;
        if (Config.getCurrent().isOldScoreboards()) {
            scoreboard = openInNewTab(Scoreboard.class, getTranslation("Scoreboard"));
            scoreboardWLeaders = openInNewTab(ScoreWithLeaders.class, getTranslation("ScoreboardWLeadersButton"));
            scoreboardWLeaders.getElement().setAttribute("title", getTranslation("ScoreboardWLeadersMouseOver"));
            scoreboardMultiRanks = openInNewTab(ScoreMultiRanks.class, getTranslation("ScoreboardMultiRanksButton"));
            medals = openInNewTab(Medals.class, getTranslation("CeremonyType.MEDALS"));
            currentAthlete = openInNewTab(CurrentAthlete.class, getTranslation("CurrentAthleteTitle"));
        } else {
            scoreboard = openInNewTab(ResultsNoLeaders.class, getTranslation("Scoreboard"));
            scoreboardWLeaders = openInNewTab(Results.class, getTranslation("ScoreboardWLeadersButton"));
            scoreboardWLeaders.getElement().setAttribute("title", getTranslation("ScoreboardWLeadersMouseOver"));
            scoreboardMultiRanks = openInNewTab(ResultsLeadersRanks.class, getTranslation("ScoreboardMultiRanksButton"));
            medals = openInNewTab(ResultsMedals.class, getTranslation("CeremonyType.MEDALS"));
            currentAthlete = openInNewTab(CurrentAthlete.class, getTranslation("CurrentAthleteTitle"));
        }
            

        //Button liftingOrder = openInNewTab(LiftingOrder.class, getTranslation("Scoreboard.LiftingOrder"));
        Button liftingOrder = openInNewTab(ResultsLiftingOrder.class, getTranslation("Scoreboard.LiftingOrder"));
        Button topSinclair = openInNewTab(TopSinclair.class, getTranslation("Scoreboard.TopSinclair"));
        Button topTeams = openInNewTab(TopTeams.class, getTranslation("Scoreboard.TopTeams"));
        Button topTeamsSinclair = openInNewTab(TopTeamsSinclair.class, getTranslation("Scoreboard.TopTeamsSinclair"));

        Button obsMonitor = openInNewTab(OBSMonitor.class, getTranslation("OBS.MonitoringButton"));
        Button publicDecisions = openInNewTab(PublicFacingDecisionBoard.class, getTranslation("RefereeDecisions"));

        fillH(intro, this);

        VerticalLayout intro1 = new VerticalLayout();
        addP(intro1, getTranslation("darkModeSelect"));
        FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(scoreboard, scoreboardWLeaders,
                scoreboardMultiRanks, liftingOrder,
                topSinclair, topTeams, topTeamsSinclair, medals);
        doGroup(getTranslation("Scoreboards"), intro1, grid1, this);

        FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(attempt, athleteFacingAttempt);
        doGroup(getTranslation("AttemptBoard"), grid3, this);

        VerticalLayout intro2 = new VerticalLayout();
        addP(intro2, getTranslation("refereeingDevices"));
        FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(decisions);
        doGroup(getTranslation("Refereeing_Displays"), intro2, grid2, this);

        VerticalLayout intro4 = new VerticalLayout();
        addP(intro4, getTranslation("OBS.MonitoringExplanation"));
        FlexibleGridLayout grid4 = HomeNavigationContent.navigationGrid(obsMonitor, currentAthlete, publicDecisions);
        doGroup(getTranslation("OBS.MonitoringTitle"), intro4, grid4, this);

        DebugUtils.gc();
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
    public String getPageTitle() {
        return getTranslation("ShortTitle.Displays") + OwlcmsSession.getFopNameIfMultiple();
    }
    
    @Override
    public String getMenuTitle() {
        return getTranslation("ShortTitle.Displays");
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public void setLocationUI(UI locationUI) {
        this.locationUI = locationUI;
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.nui.home.BaseNavigationContent#createTopBarFopField(java.lang. String, java.lang.String)
     */
    @Override
    protected HorizontalLayout createMenuBarFopField(String label, String placeHolder) {
        Label fopLabel = new Label(label);
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


    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RequireDisplayLogin.super.beforeEnter(event);
    }

}
