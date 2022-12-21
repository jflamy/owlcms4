/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import com.vaadin.flow.component.orderedlayout.FlexLayout;

public interface OwlcmsLayoutAware {

    /**
     * A Vaadin RouterLayout contains an instance of an AppLayout.
     *
     * A RouterLayout is referenced as a layout by some Content, meaning that the content will be inserted inside and
     * laid out (i.e. displayed). OwlcmsLayout delegates to an AppLayout which actually does the layouting.
     * AppLayout is a Java API to the Google app-layout web component.
     *
     * @return the RouterLayout which is the target of the Vaadin Flow Route
     */
    public OwlcmsLayout getRouterLayout();

    /**
     * @param owlcmsLayout
     */
    public void setRouterLayout(OwlcmsLayout owlcmsLayout);
    
    public default OwlcmsLayout getAppLayout() {
        return getRouterLayout();
    }
    
    public String getPageTitle();

    public void setHeaderContent();

    public FlexLayout createMenuArea();

}
