/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 *
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.home;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.components.appLayout.AppLayoutContent;
import app.owlcms.data.group.Group;
import app.owlcms.ui.group.UIEventProcessor;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class NavigationLayout.
 * 
 * Define the connection between the top bar and the content slot on navigation pages
 * so they can refer to each other.
 */
@SuppressWarnings("serial")
@HtmlImport("frontend://bower_components/vaadin-lumo-styles/presets/compact.html")
@HtmlImport("frontend://styles/shared-styles.html")
@Theme(Lumo.class)
@Push
public class NavigationLayout extends OwlcmsAppLayoutRouterLayout implements SafeEventBusRegistration, UIEventProcessor {

	final private Logger logger = (Logger) LoggerFactory.getLogger(NavigationLayout.class);
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());
	{
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	protected EventBus uiEventBus;

	protected ComboBox<Group> groupSelect;
	protected HorizontalLayout topBar;
	protected AbstractLeftAppLayoutBase appLayoutBase;
	
	public NavigationLayout () {
	}

	/**
	 * The layout is created before the content. This routine sets the content, we can then refer to
	 * our content using {@link #getLayoutContent()} and we call the content's setParentLayout so it can refer to us via
	 * {@link AppLayoutContent#getParentLayout()}
	 *
	 * @see com.github.appreciated.app.layout.router.AppLayoutRouterLayoutBase#showRouterLayoutContent(com.vaadin.flow.component.HasElement)
	 */
	@Override
	public void showRouterLayoutContent(HasElement content) {
		super.showRouterLayoutContent(content);
		BaseNavigationContent baseNavigationContent = (BaseNavigationContent) getLayoutContent();
		baseNavigationContent.setParentLayout(this);
		baseNavigationContent.configureTopBar("", appLayoutBase);
	}

	/**
	 * Expose appLayoutBase so that {@link #showRouterLayoutContent(HasElement)} can
	 * use it to expose the top bar.
	 * 
	 * @see app.owlcms.ui.home.OwlcmsAppLayoutRouterLayout#getLayoutConfiguration(com.github.appreciated.app.layout.behaviour.Behaviour)
	 */
	@Override
	protected AppLayout getLayoutConfiguration(Behaviour variant) {
		AppLayout appLayout = super.getLayoutConfiguration(variant);
		appLayoutBase = (AbstractLeftAppLayoutBase) appLayout;
		return appLayout;
	}
}
