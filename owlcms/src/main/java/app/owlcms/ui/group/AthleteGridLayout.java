/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.group;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.ui.home.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerLayout.
 */
@SuppressWarnings("serial")
@HtmlImport("frontend://bower_components/vaadin-lumo-styles/presets/compact.html")
@HtmlImport("frontend://styles/shared-styles.html")
@Theme(Lumo.class)
@Push
public class AthleteGridLayout extends OwlcmsRouterLayout {
	
	final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteGridLayout.class);
	static {logger.setLevel(Level.DEBUG);}
	
	public AthleteGridLayout() {
		super();
		logger.debug("created AthleteGridLayout");
	}

	/**
	 * Hide the menu and the default title
	 * @see app.owlcms.ui.home.OwlcmsRouterLayout#getLayoutConfiguration(com.github.appreciated.app.layout.behaviour.Behaviour)
	 */
	@Override
	protected AppLayout getLayoutConfiguration(Behaviour variant) {
		logger.debug("AthleteGridLayout getLayoutConfiguration ");
		variant = Behaviour.LEFT;
		AbstractLeftAppLayoutBase appLayout = (AbstractLeftAppLayoutBase) super.getLayoutConfiguration(variant);
		// hide arrow because we open in new page
		appLayout.setMenuVisible(false);
		appLayout.closeDrawer();
		// hide the title and icon
		appLayout.getTitleWrapper()
			.getElement()
			.getStyle()
			.set("display", "none");
			//.set("flex", "0 1 0px");
		return appLayout;
	}
	
	@Override
	public void showRouterLayoutContent(HasElement content) {
		logger.debug("AthleteGridLayout setting bi-directional link");
		super.showRouterLayoutContent(content);
	}
}
