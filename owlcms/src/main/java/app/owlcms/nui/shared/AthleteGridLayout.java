/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.component.applayout.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.component.applayout.AppLayout;
import com.github.appreciated.app.layout.component.applayout.LeftLayouts;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.PageConfigurator;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerLayout.
 */
@SuppressWarnings({ "serial", "deprecation" })
@Push
@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
@CssImport(value = "./styles/shared-styles.css")
@CssImport(value = "./styles/notification-theme.css", themeFor = "vaadin-notification-card")
@CssImport(value = "./styles/text-field-theme.css", themeFor = "vaadin-text-field")

public class AthleteGridLayout extends OwlcmsRouterLayout implements PageConfigurator {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteGridLayout.class);
    static {
        logger.setLevel(Level.INFO);
    }

    public AthleteGridLayout() {
        super();
        logger.debug("created AthleteGridLayout");
    }

    @Override
    public void configurePage(InitialPageSettings settings) {
        settings.addMetaTag("mobile-web-app-capable", "yes");
        settings.addMetaTag("apple-mobile-web-app-capable", "yes");
        settings.addLink("shortcut icon", "frontend/images/owlcms.ico");
        settings.addFavIcon("icon", "frontend/images/logo.png", "96x96");
        settings.setViewport("width=device-width, minimum-scale=1, initial-scale=1, user-scalable=yes");
    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        logger.debug("AthleteGridLayout setting bi-directional link");
        super.showRouterLayoutContent(content);
    }

    /**
     * Hide the menu and the default title
     *
     * @see app.owlcms.nui.shared.OwlcmsRouterLayout#getLayoutConfiguration(com.github.appreciated.app.layout.behaviour.Behaviour)
     */
    @Override
    protected AppLayout getLayoutConfiguration(Class<? extends AppLayout> variant) {
        logger.debug("AthleteGridLayout getLayoutConfiguration ");
        variant = LeftLayouts.Left.class;
        AbstractLeftAppLayoutBase appLayout = (AbstractLeftAppLayoutBase) super.getLayoutConfiguration(variant);
        // hide the title and icon
        appLayout.getTitleWrapper().getElement().getStyle().set("display", "none");
        // .set("flex", "0 1 0px");
        return appLayout;
    }
}
