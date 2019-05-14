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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.NavigationPage;
import app.owlcms.ui.displayselection.DisplayNavigationContent;
import app.owlcms.ui.lifting.LiftingNavigationContent;
import app.owlcms.ui.preparation.PreparationNavigationContent;
import app.owlcms.ui.results.ResultsNavigationContent;
import app.owlcms.ui.shared.BaseNavigationContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class HomeNavigationContent.
 */
/**
 * @author owlcms
 *
 */
@SuppressWarnings("serial")
@Route(value = "", layout = OwlcmsRouterLayout.class)
public class HomeNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(HomeNavigationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}

	public static final String PREPARE_COMPETITION = "Prepare Competition";
	public static final String RUN_LIFTING_GROUP = "Run Lifting Group";
	public static final String START_DISPLAYS = "Start Displays";
	public static final String RESULT_DOCUMENTS = "Result Documents";
	
	/**
	 * Instantiates a new main navigation content.
	 */
	public HomeNavigationContent() {
		VerticalLayout intro = buildIntro();

		Button prepare = new Button(
				PREPARE_COMPETITION,
				buttonClickEvent -> UI.getCurrent()
					.navigate(PreparationNavigationContent.class));
		Button displays = new Button(
				START_DISPLAYS,
				buttonClickEvent -> UI.getCurrent()
					.navigate(DisplayNavigationContent.class));
		Button lifting = new Button(
				RUN_LIFTING_GROUP,
				buttonClickEvent -> UI.getCurrent()
					.navigate(LiftingNavigationContent.class));
		Button documents = new Button(
				RESULT_DOCUMENTS,
				buttonClickEvent -> UI.getCurrent()
					.navigate(ResultsNavigationContent.class));
		FlexibleGridLayout grid = HomeNavigationContent.navigationGrid(
			prepare,
			lifting,
			displays,
			documents);

		documents.setEnabled(false);

		fillH(intro, this);
		fillH(grid, this);
		
		VerticalLayout license = buildLicense();
		fillH(license, this);
	}

	private VerticalLayout buildLicense() {
		VerticalLayout license = new VerticalLayout();
		addP(license,
				"This is open source software."+
				"<li>See the <a href='https://github.com/jflamy/owlcms4'>project repository</a> for full source and licensing information."+
				"<li>See also the <a href='https://https://jflamy.github.io/owlcms4/'>documentation</a> for "+
				"installation and configuration information"
				);
		return license;
	}

	public VerticalLayout buildIntro() {
		VerticalLayout intro = new VerticalLayout();
		URLUtils urlFinder = new URLUtils();
		addP(intro, "The competition system is reachable using the following address(es): ");
		for (String url : urlFinder.getRecommended()) {
			intro.add(new Div(new Anchor(url, url)));
		}
		for (String url : urlFinder.getWired()) {
			intro.add(new Div(new Anchor(url, url), new Label(" (wired)")));
		}
		for (String url : urlFinder.getWireless()) {
			intro.add(new Div(new Anchor(url, url), new Label(" (wireless)")));
		}
		for (String url : urlFinder.getLocalUrl()) {
			intro.add(new Div(new Anchor(url, url), new Label(" (only on the computer running the owlcms program)")));
		}
		intro.add(new Div());
		intro.add(new Hr());
		addP(intro,
			"Use the menu at the left to navigate to the various screens:<ul>" +
			"<li>Prepare Competition : Enter the competition coordinates, enter the athletes, etc.<br>" +
			"This section also includes the group weigh-in where you can produce a printable spreadsheet with starting weights.</li>" +
			"<li>Run lifting group : Used to start the announcer screen, the marshall, the timekeeper.  Each of these uses a separate laptop. The announcer controls which group is shown on the displays.</li>" +
			"<li>Start displays : Used to start the attempt board, the athlete-facing board, and the scoreboard. Each of these displays uses a separate laptop or mini-PC connected to a projector or screen.</li>" +
			"<li>Competition documents : After each group, the competition secretary can produce printable group results.</li>"+
			"</ul>"
		);
		intro.getElement().getStyle().set("margin-bottom", "-1em");
		return intro;
	}

	/**
	 * Navigation crudGrid.
	 *
	 * @param items the items
	 * @return the flexible crudGrid layout
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
	 * @see app.owlcms.ui.shared.BaseNavigationContent#configureTopBarTitle(java.lang.String)
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

	/**
	 * @see app.owlcms.ui.shared.BaseNavigationContent#createTopBarFopField(java.lang.String, java.lang.String)
	 */
	@Override
	protected HorizontalLayout createTopBarFopField(String label, String placeHolder) {
		return null;
	}

	/**
	 * @see app.owlcms.ui.shared.BaseNavigationContent#createTopBarGroupField(java.lang.String, java.lang.String)
	 */
	@Override
	protected HorizontalLayout createTopBarGroupField(String label, String placeHolder) {
		return null;
	}

	/**
	 * @see app.owlcms.ui.shared.QueryParameterReader#isIgnoreFopFromURL()
	 */
	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	/**
	 * @see app.owlcms.ui.shared.BaseNavigationContent#getTitle()
	 */
	@Override
	protected String getTitle() {
		return "OWLCMS - Olympic Weightlifting Competition Management System";
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return "OWLCMS - Home";
	}

}