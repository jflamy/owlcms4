/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.shared;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.component.applayout.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.component.applayout.AppLayout;
import com.github.appreciated.app.layout.component.applayout.LeftLayouts;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerLayout.
 */
@SuppressWarnings("serial")
@Theme(value = Lumo.class, variant = Lumo.LIGHT)
@Push
@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
@CssImport(value = "./styles/shared-styles.css")
@CssImport(value = "./styles/notification-theme.css", themeFor = "vaadin-notification-card")
@CssImport(value = "./styles/text-field-theme.css", themeFor = "vaadin-text-field")

public class AthleteGridLayout extends OwlcmsRouterLayout {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteGridLayout.class);
    static {
        logger.setLevel(Level.INFO);
    }

    public AthleteGridLayout() {
        super();
        logger.debug("created AthleteGridLayout");
    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        logger.debug("AthleteGridLayout setting bi-directional link");
        super.showRouterLayoutContent(content);
    }

    /**
     * Hide the menu and the default title
     *
     * @see app.owlcms.ui.shared.OwlcmsRouterLayout#getLayoutConfiguration(com.github.appreciated.app.layout.behaviour.Behaviour)
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
