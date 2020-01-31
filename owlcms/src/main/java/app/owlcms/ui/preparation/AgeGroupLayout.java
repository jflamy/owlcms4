/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import com.github.appreciated.app.layout.component.applayout.AppLayout;
import com.github.appreciated.app.layout.component.applayout.LeftLayouts;
import com.vaadin.flow.component.html.Label;

import app.owlcms.ui.shared.OwlcmsRouterLayout;

@SuppressWarnings("serial")
public class AgeGroupLayout extends OwlcmsRouterLayout {

    /**
     * @see app.owlcms.ui.shared.OwlcmsRouterLayout#getLayoutConfiguration(java.lang.Class)
     */
    @Override
    protected AppLayout getLayoutConfiguration(Class<? extends AppLayout> variant) {
        variant = LeftLayouts.Left.class;
        AppLayout appLayout = super.getLayoutConfiguration(variant);
        appLayout.closeDrawer();
        appLayout.setTitleComponent(new Label(getTranslation("EditAgeGroups")));
        return appLayout;
    }
}
