/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.results;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.components.JXLSDownloader;
import app.owlcms.data.competition.Competition;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.preparation.RecordContent;
import app.owlcms.nui.preparation.TeamSelectionContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.spreadsheet.JXLSMedalSchedule;
import app.owlcms.spreadsheet.JXLSTimingStats;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class ResultsNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "results", layout = OwlcmsLayout.class)
public class ResultsNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(ResultsNavigationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	/**
	 * Instantiates a new wrapup navigation content.
	 */
	public ResultsNavigationContent() {

		Button groupResults = openInNewTab(SessionResultsContent.class, Translator.translate("GroupResults"));
		highlight(groupResults);

		var medalScheduleWriter = new JXLSMedalSchedule(UI.getCurrent());
		JXLSDownloader dd2 = new JXLSDownloader(
		        () -> {
			        return medalScheduleWriter;
		        },
		        "/templates/medalSchedule",
		        // template name used only to generate the results file name. Localized template determined by
		        // JXLSTimingStats
		        Competition::getComputedMedalScheduleTemplateFileName,
		        Competition::setMedalScheduleTemplateFileName,
		        Translator.translate("Results.MedalSchedule"),
		        Translator.translate("Download"));
		Div medalScheduleDiv = new Div();
		medalScheduleDiv.add(dd2.createDownloadButton());
		Optional<Component> medalScheduleButton = medalScheduleDiv.getChildren().findFirst();
		medalScheduleButton.ifPresent(c -> ((Button) c).setWidth("100%"));
		medalScheduleDiv.setWidthFull();

		Button teamResults = openInNewTabNoParam(TeamResultsContent.class, Translator.translate("TeamResults.Title"));
		Button teams = openInNewTabNoParam(TeamSelectionContent.class, Translator.translate(TeamSelectionContent.TITLE));
		// Button categoryResults = openInNewTabNoParam(PackageContent.class,
		// Translator.translate("CategoryResults"));
		Button finalPackage = openInNewTabNoParam(PackageContent.class, Translator.translate("CompetitionResults"));
		highlight(finalPackage);

		var timingWriter = new JXLSTimingStats(UI.getCurrent());
		JXLSDownloader dd1 = new JXLSDownloader(
		        () -> {
			        return timingWriter;
		        },
		        "/templates/timing",
		        // template name used only to generate the results file name. Localized template determined by
		        // JXLSTimingStats
		        "TimingStats.xlsx",
		        Translator.translate("TimingStatistics"),
		        fileName -> fileName.endsWith(".xlsx"));
		Div timingStats = new Div();
		timingStats.add(dd1.createImmediateDownloadButton());
		timingStats.setWidthFull();

		Button editExportRecords = openInNewTabNoParam(RecordContent.class,
		        Translator.translate("RecordEvent.EditExportRecords"));
		editExportRecords.setWidthFull();

		FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(groupResults, medalScheduleDiv);
		FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(teamResults, teams);
		FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(finalPackage,
		     editExportRecords, timingStats);

		doGroup(Translator.translate("ForEachCompetitionGroup"), grid1, this);
		doGroup(Translator.translate("TeamResults.Title"), grid2, this);
		doGroup(Translator.translate("Results.EndOfCompetition"), grid3, this);

		Button importSessions = openInNewTabNoParam(SessionImportContent.class, Translator.translate("ImportSessions.PageTitle"));
		doHiddenGroup(Translator.translate("ImportSessions.PageTitle"),
		        new Div(Translator.translate("ImportSessions.Explanation")),
		        HomeNavigationContent.navigationGrid(importSessions), this);

		DebugUtils.gc();
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("ShortTitle.Results");
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("ShortTitle.Results");
	}

	@Override
	protected HorizontalLayout createMenuBarFopField(String label, String placeHolder) {
		return null;
	}

	private void highlight(Button button) {
		button.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
	}
}
