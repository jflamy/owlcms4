/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.displays;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinServletRequest;

import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.displays.results.ResultsBoard;
import app.owlcms.ui.home.ContentWrapping;
import app.owlcms.ui.home.MainNavigationContent;
import app.owlcms.ui.preparation.CategoryContent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class DisplayNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "displays", layout = DisplayNavigationLayout.class)
public class DisplayNavigationContent extends VerticalLayout
		implements ContentWrapping {
	
	Logger logger = (Logger)LoggerFactory.getLogger(DisplayNavigationContent.class);
	{ logger.setLevel(Level.DEBUG); }

	/**
	 * Instantiates a new display navigation content.
	 */
	public DisplayNavigationContent() {
		VerticalLayout intro = new VerticalLayout();
		intro.add(new Paragraph("Use the dropdown to select the platform where the display is located."));
		intro.add(new Paragraph("Use one of the buttons below to open a display."));
		
		RouteConfiguration routeResolver = RouteConfiguration.forApplicationScope();
		String attBoard = routeResolver.getUrl(AttemptBoard.class);
		String attUrl = createURL(VaadinServletRequest.getCurrent(),attBoard);
		logger.debug("url {}",attUrl);
		
		Button attempt = new Button("Attempt Board",
				buttonClickEvent -> UI.getCurrent().getPage()
					.executeJavaScript("window.open('"+attUrl+"','_blank')"));
		Button results = new Button("Results Board",
			buttonClickEvent -> UI.getCurrent()
				.navigate(ResultsBoard.class));
		Button referee = new Button("Referee Decision Display",
				buttonClickEvent -> UI.getCurrent()
					.navigate(CategoryContent.class));
		Button jury = new Button("Jury Display",
				buttonClickEvent -> UI.getCurrent()
					.navigate(CategoryContent.class));
		Button plates = new Button("Plates Display",
				buttonClickEvent -> UI.getCurrent()
					.navigate(CategoryContent.class));
		
		FlexibleGridLayout grid = MainNavigationContent.navigationGrid(
			attempt,
			results,
			referee,
			jury,
			plates);
		
		referee.setEnabled(false);
		jury.setEnabled(false);
		plates.setEnabled(false);

		fillH(intro,this);
		fillH(grid, this);

	}
	
	protected static String createURL(VaadinServletRequest request, String resourcePath) {
		int port = request.getServerPort();
		StringBuilder result = new StringBuilder();
		result.append(request.getScheme())
			.append("://")
			.append(request.getServerName());
		if ((request.getScheme().equals("http") && port != 80)
				|| (request.getScheme().equals("https") && port != 443)) {
			result.append(':')
				.append(port);
		}
		result.append(request.getContextPath());
		if (resourcePath != null && resourcePath.length() > 0) {
			if (!resourcePath.startsWith("/")) {
				result.append("/");
			}
			result.append(resourcePath);
		}
		return result.toString();
	}

}
