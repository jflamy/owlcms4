/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
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
import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.displays.attemptboard.PublicFacingDecisionBoard;
import app.owlcms.displays.monitor.OBSMonitor;
import app.owlcms.displays.scoreboard.CurrentAthlete;
import app.owlcms.displays.scoreboard.Results;
import app.owlcms.displays.scoreboard.ResultsLeadersRanks;
import app.owlcms.displays.scoreboard.ResultsLiftingOrder;
import app.owlcms.displays.scoreboard.ResultsMedals;
import app.owlcms.displays.scoreboard.ResultsNoLeaders;
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
@Route(value = "video", layout = OwlcmsLayout.class)
public class VideoNavigationContent extends BaseNavigationContent
        implements NavigationPage, HasDynamicTitle, RequireDisplayLogin {

    final static Logger logger = (Logger) LoggerFactory.getLogger(VideoNavigationContent.class);
    static {
        logger.setLevel(Level.INFO);
    }

    /**
     * Instantiates a new display navigation content.
     */
    public VideoNavigationContent() {
        VerticalLayout intro = new VerticalLayout();
        intro.setSpacing(false);
        addP(intro, getTranslation("VideoStreaming.Intro"));
        addP(intro, getTranslation("Button_Open_Display"));
        intro.getStyle().set("margin-bottom", "0");

        Button attempt = openInNewTab(AttemptBoard.class, getTranslation("AttemptBoard"), "foo");

        Button scoreboard;
        Button scoreboardWLeaders;
        Button scoreboardMultiRanks;
        Button currentAthlete;
        Button medals;

        scoreboard = openInNewTab(ResultsNoLeaders.class, getTranslation("Scoreboard"), "video");
        scoreboardWLeaders = openInNewTab(Results.class, getTranslation("ScoreboardWLeadersButton"));
        scoreboardWLeaders.getElement().setAttribute("title", getTranslation("ScoreboardWLeadersMouseOver"));
        scoreboardMultiRanks = openInNewTab(ResultsLeadersRanks.class,
                getTranslation("ScoreboardMultiRanksButton"));
        medals = openInNewTab(ResultsMedals.class, getTranslation("CeremonyType.MEDALS"));
        currentAthlete = openInNewTab(CurrentAthlete.class, getTranslation("CurrentAthleteTitle"));

        // Button liftingOrder = openInNewTab(LiftingOrder.class, getTranslation("Scoreboard.LiftingOrder"));
        Button liftingOrder = openInNewTab(ResultsLiftingOrder.class, getTranslation("Scoreboard.LiftingOrder"));
        Button topSinclair = openInNewTab(TopSinclair.class, getTranslation("Scoreboard.TopSinclair"));
        Button topTeams = openInNewTab(TopTeams.class, getTranslation("Scoreboard.TopTeams"));
        Button topTeamsSinclair = openInNewTab(TopTeamsSinclair.class, getTranslation("Scoreboard.TopTeamsSinclair"));

        Button obsMonitor = openInNewTab(OBSMonitor.class, getTranslation("OBS.MonitoringButton"));
        Button publicDecisions = openInNewTab(PublicFacingDecisionBoard.class, getTranslation("RefereeDecisions"));

        fillH(intro, this);
        
        FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(attempt, currentAthlete, publicDecisions);
        doGroup(getTranslation("AttemptBoard"), grid3, this);

        VerticalLayout intro1 = new VerticalLayout();
        addP(intro1, getTranslation("darkModeSelect"));
        FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(scoreboard, scoreboardWLeaders,
                scoreboardMultiRanks, liftingOrder,
                topSinclair, topTeams, topTeamsSinclair, medals);
        doGroup(getTranslation("Scoreboards"), intro1, grid1, this);

        VerticalLayout intro4 = new VerticalLayout();
        addP(intro4, getTranslation("OBS.MonitoringExplanation"));
        FlexibleGridLayout grid4 = HomeNavigationContent.navigationGrid(obsMonitor);
        doGroup(getTranslation("OBS.MonitoringButton"), intro4, grid4, this);

        DebugUtils.gc();
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
        return getTranslation("VideoStreaming");
    }

    @Override
    public String getPageTitle() {
        return getTranslation("VideoStreaming") + OwlcmsSession.getFopNameIfMultiple();
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

}
