/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import com.github.appreciated.app.layout.component.applayout.AppLayout;
import com.github.appreciated.app.layout.component.applayout.LeftLayouts;
import com.vaadin.flow.component.html.Label;

import app.owlcms.nui.shared.OwlcmsRouterLayout;

/**
 * The Class CategoryLayout.
 */
@SuppressWarnings("serial")
public class CompetitionLayout extends OwlcmsRouterLayout {

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.nui.home.OwlcmsRouterLayout#getLayoutConfiguration(com.github.
     * appreciated.app.layout.behaviour.Behaviour)
     */
    @Override
    protected AppLayout getLayoutConfiguration(Class<? extends AppLayout> variant) {
        variant = LeftLayouts.Left.class;
        AppLayout appLayout = super.getLayoutConfiguration(variant);
        appLayout.closeDrawer();
        appLayout.setTitleComponent(new Label(getTranslation("EditCompetitionInformation")));
        return appLayout;
    }
}
