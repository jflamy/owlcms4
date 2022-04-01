/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.results;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.components.NavigationPage;
import app.owlcms.spreadsheet.JXLSTimingStats;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.preparation.TeamSelectionContent;
import app.owlcms.ui.shared.BaseNavigationContent;
import app.owlcms.ui.shared.DownloadButtonFactory;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class ResultsNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "results", layout = OwlcmsRouterLayout.class)
public class ResultsNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(ResultsNavigationContent.class);
    static {
        logger.setLevel(Level.INFO);
    }

    /**
     * Instantiates a new wrapup navigation content.
     */
    public ResultsNavigationContent() {
        Button groupResults = openInNewTab(ResultsContent.class, getTranslation("GroupResults"));
        Button medals = openInNewTab(ResultsContent.class, getTranslation("Results.Medals"));
        Button teamResults = openInNewTabNoParam(TeamResultsContent.class, getTranslation("TeamResults.Title"));
        Button teams = openInNewTabNoParam(TeamSelectionContent.class, getTranslation(TeamSelectionContent.TITLE));
        Button categoryResults = openInNewTabNoParam(PackageContent.class, getTranslation("CategoryResults"));
        Button finalPackage = openInNewTabNoParam(PackageContent.class, getTranslation("FinalResultsPackage"));

        Div timingStats = DownloadButtonFactory.createDynamicXLSDownloadButton("timingStats",
                getTranslation("TimingStatistics"), new JXLSTimingStats(UI.getCurrent()));

        FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(groupResults, medals);
        FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(teamResults, teams);
        FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(finalPackage, categoryResults, timingStats);

        doGroup(getTranslation("ForEachCompetitionGroup"), grid1, this);
        doGroup(getTranslation("TeamResults.Title"), grid2, this);
        doGroup(getTranslation("EndOfCompetitionDocuments"), grid3, this);

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
        return getTranslation("OWLCMS_Results");
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
    protected HorizontalLayout createTopBarFopField(String label, String placeHolder) {
        return null;
    }

    @Override
    protected String getTitle() {
        return getTranslation("Results");
    }

}
