/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.HasDynamicTitle;

public interface OwlcmsContent
        extends ContentWrapping, OwlcmsLayoutAware, HasDynamicTitle, SafeEventBusRegistration, RequireLogin {

    @Override
    public FlexLayout createMenuArea();

    public String getMenuTitle();

    @Override
    public default void setHeaderContent() {
        getRouterLayout().setMenuTitle(getMenuTitle());
        getRouterLayout().setMenuArea(createMenuArea());
        getRouterLayout().showLocaleDropdown(false);
        getRouterLayout().setDrawerOpened(false);
        getRouterLayout().updateHeader(true);
    }
}
