/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.home;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.css.grid.GridLayoutComponent.AutoFlow;
import com.github.appreciated.css.grid.GridLayoutComponent.Overflow;
import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.css.grid.sizes.MinMax;
import com.github.appreciated.css.grid.sizes.Repeat;
import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.components.NavigationPage;
import app.owlcms.ui.displayselection.DisplayNavigationContent;
import app.owlcms.ui.group.GroupNavigationContent;
import app.owlcms.ui.preparation.PreparationNavigationContent;
import app.owlcms.ui.results.ResultsNavigationContent;
import app.owlcms.ui.shared.BaseNavigationContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.utils.URLFinder;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class HomeNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "", layout = OwlcmsRouterLayout.class)
public class HomeNavigationContent extends BaseNavigationContent implements NavigationPage {
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(HomeNavigationContent.class);
	static { logger.setLevel(Level.INFO);}

	/**
	 * Instantiates a new main navigation content.
	 */
	public HomeNavigationContent() {
		Button prepare = new Button("Prepare Competition",
				buttonClickEvent -> UI.getCurrent()
					.navigate(PreparationNavigationContent.class));
		Button lifting = new Button("Run Lifting Group",
				buttonClickEvent -> UI.getCurrent()
					.navigate(GroupNavigationContent.class));
		Button displays = new Button("Start Displays",
			buttonClickEvent -> UI.getCurrent()
				.navigate(DisplayNavigationContent.class));
		Button documents = new Button("Final Documents",
				buttonClickEvent -> UI.getCurrent()
					.navigate(ResultsNavigationContent.class));
		FlexibleGridLayout grid = HomeNavigationContent.navigationGrid(
			prepare,
			lifting,
			displays,
			documents);

		documents.setEnabled(false);
		
		VerticalLayout intro = new VerticalLayout();
		URLFinder urlFinder = new URLFinder();
		addP(intro, "owlcms is reachable using the following URLs: ");
		for (String url : urlFinder.getRecommended()) {
			intro.add(new Anchor(url, url + " (recommended)"));
		}
		for (String url : urlFinder.getWired()) {
			intro.add(new Anchor(url, url + " (wired)"));
		}
		for (String url : urlFinder.getWireless()) {
			intro.add(new Anchor(url, url + " (wireless)"));
		}
		for (String url : urlFinder.getLocalUrl()) {
			intro.add(new Anchor(url, url + " (only on the computer running the owlcms program)"));
		}
		intro.getElement().getStyle().set("margin-bottom", "0");
		
		fillH(grid, this);
		fillH(intro, this);
	}

	/**
	 * Navigation grid.
	 *
	 * @param items the items
	 * @return the flexible grid layout
	 */
	public static FlexibleGridLayout navigationGrid(Component... items) {
		FlexibleGridLayout layout = new FlexibleGridLayout();
		layout.withColumns(Repeat.RepeatMode.AUTO_FILL, new MinMax(new Length("300px"), new Flex(1)))
			.withAutoRows(new Length("1fr"))
			.withItems(items)
			.withGap(new Length("2vmin"))
			.withOverflow(Overflow.AUTO)
			.withAutoFlow(AutoFlow.ROW)
			.withMargin(false)
			.withPadding(true)
			.withSpacing(false);
		layout.setSizeUndefined();
		layout.setWidth("80%");
		layout.setBoxSizing(BoxSizing.BORDER_BOX);
		return layout;
	}
	

	/**
	 * The left part of the top bar.
	 * @param topBarTitle
	 * @param appLayoutComponent
	 */
	@Override
	protected void configureTopBarTitle(String topBarTitle) {
		AbstractLeftAppLayoutBase appLayout = getAppLayout();
		appLayout.getTitleWrapper()
		.getElement()
		.getStyle()
		.set("flex", "0 1 40em");
		Label label = new Label(getTitle());
		appLayout.setTitleComponent(label);
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
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	@Override
	protected String getTitle() {
		return "OWLCMS - Olympic Weightlifting Competition Management System";
	}
	

}