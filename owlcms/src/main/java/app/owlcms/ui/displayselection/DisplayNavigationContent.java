/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.displayselection;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.NavigationPage;
import app.owlcms.displays.attemptboard.AthleteFacingAttemptBoard;
import app.owlcms.displays.attemptboard.AthleteFacingDecisionBoard;
import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.displays.liftingorder.LiftingOrder;
import app.owlcms.displays.scoreboard.Scoreboard;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.shared.BaseNavigationContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
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
        Button scoreboard = openInNewTab(Scoreboard.class, getTranslation("Scoreboard"));
        Button liftingOrder = openInNewTab(LiftingOrder.class, getTranslation("Scoreboard.LiftingOrder"));
        Button referee = openInNewTab(AthleteFacingDecisionBoard.class, getTranslation("Athlete_Decisions"));
        Button athleteFacingAttempt = openInNewTab(AthleteFacingAttemptBoard.class, getTranslation("Athlete_Attempt"));

        FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(scoreboard, attempt, liftingOrder);
        FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(referee, athleteFacingAttempt);

        fillH(intro, this);
        doGroup(getTranslation("FieldOfPlayDisplays"), grid1, this);
        doGroup(getTranslation("Refereeing_Displays"), grid2, this);
    }

    /**
     * @see app.owlcms.ui.shared.BaseNavigationContent#createTopBarGroupField(java.lang.String,
     *      java.lang.String)
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

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("OWLCMS_Displays");
    }

}
