/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.home;

import java.time.LocalDate;

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
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.NavigationPage;
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
@Route(value = "info", layout = OwlcmsRouterLayout.class)
public class InfoNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(InfoNavigationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	
	/**
	 * Instantiates a new main navigation content.
	 */
	public InfoNavigationContent() {
		VerticalLayout license = buildLicense();
		fillH(license, this);
	}

	private VerticalLayout buildLicense() {
		VerticalLayout license = new VerticalLayout();
		license.add(new H3("Copyright and License"));
		addP(license,
				"This software is Copyright \u00a9 2009-"+LocalDate.now().getYear()+" Jean-François Lamy"
				);
		addP(license,
				"This software is made available under the <a href='https://opensource.org/licenses/NPOSL-3.0'>Non-Profit Open Software License 3.0</a>."
				);
		license.add(new H3("Source and Documentation"));
		addP(license,
				"<li>See the <a href='https://github.com/jflamy/owlcms4'>project repository</a> for binary releases and the full source."+
				"<li>See also the <a href='https://jflamy.github.io/owlcms4/'>documentation</a> for "+
				"installation and configuration information."
				);
		
		license.add(new H3("Notes"));
		addP(license,
				"This software is meant to comply with the IWF Technical Competition Rules and Regulations (TCRR) and with the Masters Weightlifting rules"+
				" as published at the time of release.  As stated in the license, there is no guarantee whatsoever regarding this software, and you"+
				" are responsible for performing whatever tests are need to establish that the sofware is fit for your circumstances."
				);
		
		license.add(new H3("Credits"));
		addP(license,
				"The software is written and maintained by Jean-François Lamy, IWF International Technical Official Category 1.<br><br>"+
				"Special thanks to Anders Bendix Nielsen, Alexey Ruchev and Brock Pedersen for feedback and testing.<br>"+
				"Thanks to the Quebec Weightlifting Federation for supporting the initial development of the software."
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
			intro.add(new Div(new Anchor(url, url), new Label(" (on the computer running the owlcms program)")));
		}
		intro.add(new Div());
		intro.add(new Hr());
		addP(intro,
			"Use the menu at the left to navigate to the various screens:<ul>" +
			"<li><b>Prepare Competition</b>: Enter the competition coordinates, enter the athletes, perform weigh-in, print starting weights<br></li>" +
			"<li><b>Run lifting group</b>: Start the screens for the announcer, the marshall, the timekeeper. The announcer controls which group is shown on the displays.</li>" +
			"<li><b>Start displays</b>: Used to start the attempt board, the athlete-facing board, and the scoreboard.</li>" +
			"<li><b>Competition documents</b>: Produce printable documents for each group and the final results package.</li>"+
			"</ul>Each of the various screens or displays uses a separate laptop or mini-pc."
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