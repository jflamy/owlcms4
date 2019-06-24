/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.results;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.NavigationPage;
import app.owlcms.spreadsheet.JXLSCompetitionBook;
import app.owlcms.ui.home.HomeNavigationContent;
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
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(ResultsNavigationContent.class);
	static { logger.setLevel(Level.INFO);}

	/**
	 * Instantiates a new wrapup navigation content.
	 */
	public ResultsNavigationContent() {
		Button groupResults = new Button(getTranslation("GroupResults"), //$NON-NLS-1$
			buttonClickEvent -> UI.getCurrent().navigate(ResultsContent.class));
		
		Div finalResultsButton = DownloadButtonFactory.createDynamicDownloadButton(
			"finalResults", //$NON-NLS-1$
			getTranslation("FinalResultsPackage"), //$NON-NLS-1$
			new JXLSCompetitionBook(true));
		
		Button timingStats = new Button(getTranslation("TimingStatistics"),  //$NON-NLS-1$
			buttonClickEvent -> UI.getCurrent().navigate(ResultsContent.class));
		
		finalResultsButton.setEnabled(true);
		timingStats.setEnabled(false);
		FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(
			groupResults
			);
		FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(
			finalResultsButton,
			timingStats
			);
		
		doGroup(getTranslation("ForEachCompetitionGroup"), grid1, this); //$NON-NLS-1$
		doGroup(getTranslation("EndOfCompetitionDocuments"), grid2, this); //$NON-NLS-1$
    }


	
	@Override
	protected String getTitle() {
		return getTranslation("ProduceResults"); //$NON-NLS-1$
	}
	
	@Override
	protected HorizontalLayout createTopBarFopField(String label, String placeHolder) {
		return null;
	}
	
	@Override
	protected HorizontalLayout createTopBarGroupField(String label, String placeHolder) {
		return null;
	}

	@Override
	public String getPageTitle() {
		return getTranslation("OWLCMS_Results"); //$NON-NLS-1$
	}
	
}
