/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.results;

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
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.preparation.TeamSelectionContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.spreadsheet.JXLSExportRecords;
import app.owlcms.spreadsheet.JXLSTimingStats;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class ResultsNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "nresults", layout = OwlcmsLayout.class)
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
        ((Button)timingStats.getComponentAt(0)).setWidth("95%");
        Div newRecords = DownloadButtonFactory.createDynamicXLSDownloadButton("records",
                getTranslation("Results.NewRecords"), new JXLSExportRecords(UI.getCurrent()));
        ((Button)newRecords.getComponentAt(0)).setWidth("95%");

        FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(groupResults, medals);
        FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(teamResults, teams);
        FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(finalPackage, categoryResults, newRecords, timingStats);

        doGroup(getTranslation("ForEachCompetitionGroup"), grid1, this);
        doGroup(getTranslation("TeamResults.Title"), grid2, this);
        doGroup(getTranslation("Results.EndOfCompetition"), grid3, this);

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
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public void setLocationUI(UI locationUI) {
        this.locationUI = locationUI;
    }

    @Override
    protected HorizontalLayout createMenuBarFopField(String label, String placeHolder) {
        return null;
    }

    @Override
    public String getPageTitle() {
        return getTranslation("ShortTitle.Results");
    }

    @Override
    public String getMenuTitle() {
        return getTranslation("ShortTitle.Results");
    }
}
