/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.displayselection;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;

import app.owlcms.components.NavigationPage;
import app.owlcms.displays.attemptboard.AthleteFacingAttemptBoard;
import app.owlcms.displays.attemptboard.AthleteFacingDecisionBoard;
import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.displays.liftingorder.LiftingOrder;
import app.owlcms.displays.scoreboard.ScoreWithLeaders;
import app.owlcms.displays.scoreboard.Scoreboard;
import app.owlcms.displays.topathletes.TopSinclair;
import app.owlcms.displays.topteams.TopTeams;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.shared.BaseNavigationContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.utils.DebugUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class DisplayNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "displays", layout = OwlcmsRouterLayout.class)
public class DisplayNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

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
        intro.getStyle().set(getTranslation("margin-bottom"), "0");

        Button attempt = openInNewTab(AttemptBoard.class, getTranslation("AttemptBoard"));
        Button referee = openInNewTab(AthleteFacingDecisionBoard.class, getTranslation("Athlete_Decisions"));
        Button athleteFacingAttempt = openInNewTab(AthleteFacingAttemptBoard.class, getTranslation("Athlete_Attempt"));

        Button scoreboard = openInNewTab(Scoreboard.class, getTranslation("Scoreboard"));
        Button scoreboardWLeaders = openInNewTab(ScoreWithLeaders.class, getTranslation("ScoreboardWLeadersButton"));
        scoreboardWLeaders.getElement().setAttribute("title", getTranslation("ScoreboardWLeadersMouseOver"));
        Button liftingOrder = openInNewTab(LiftingOrder.class, getTranslation("Scoreboard.LiftingOrder"));
        Button topSinclair = openInNewTab(TopSinclair.class, getTranslation("Scoreboard.TopSinclair"));
        Button topTeams = openInNewTab(TopTeams.class, getTranslation("Scoreboard.TopTeams"));

        fillH(intro, this);

        VerticalLayout intro1 = new VerticalLayout();
        addP(intro1, getTranslation("darkModeSelect"));
        FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(scoreboard, scoreboardWLeaders, liftingOrder,
                topSinclair, topTeams);
        doGroup(getTranslation("Scoreboards"), intro1, grid1, this);

        FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(attempt, athleteFacingAttempt);
        doGroup(getTranslation("AttemptBoard"), grid3, this);

        VerticalLayout intro2 = new VerticalLayout();
        addP(intro2, getTranslation("refereeingDevices"));
        FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(referee);
        doGroup(getTranslation("Refereeing_Displays"), intro2, grid2, this);

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

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("OWLCMS_Displays");
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
     * @see app.owlcms.ui.home.BaseNavigationContent#createTopBarFopField(java.lang. String, java.lang.String)
     */
    @Override
    protected HorizontalLayout createTopBarFopField(String label, String placeHolder) {
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

    /**
     * @see app.owlcms.ui.shared.BaseNavigationContent#createTopBarGroupField(java.lang.String, java.lang.String)
     */
    @Override
    protected HorizontalLayout createTopBarGroupField(String label, String placeHolder) {
        return null;
    }

    /**
     * @see app.owlcms.ui.shared.BaseNavigationContent#getTitle()
     */
    @Override
    protected String getTitle() {
        return getTranslation("StartDisplays");
    }
}
