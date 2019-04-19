/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.shared;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

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
	static {logger.setLevel(Level.INFO);}
	
	public AthleteGridLayout() {
		super();
		logger.debug("created AthleteGridLayout");
	}

	/**
	 * Hide the menu and the default title
	 * @see app.owlcms.ui.shared.OwlcmsRouterLayout#getLayoutConfiguration(com.github.appreciated.app.layout.behaviour.Behaviour)
	 */
	@Override
	protected AppLayout getLayoutConfiguration(Behaviour variant) {
		logger.debug("AthleteGridLayout getLayoutConfiguration ");
		variant = Behaviour.LEFT;
		AbstractLeftAppLayoutBase appLayout = (AbstractLeftAppLayoutBase) super.getLayoutConfiguration(variant);
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
