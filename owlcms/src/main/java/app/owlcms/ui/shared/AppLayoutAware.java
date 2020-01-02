/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.shared;

import com.github.appreciated.app.layout.component.applayout.AbstractLeftAppLayoutBase;

public interface AppLayoutAware {
    /**
     * @return
     */
    public default AbstractLeftAppLayoutBase getAppLayout() {
        return (AbstractLeftAppLayoutBase) getRouterLayout().getAppLayout();
    }

    /**
     * A Vaadin RouterLayout contains an instance of an AppLayout.
     *
     * A RouterLayout is referenced as a layout by some Content, meaning that the content will be inserted inside and
     * laid out (i.e. displayed). OwlcmsRouterLayout delegates to an AppLayout which actually does the layouting.
     * AppLayout is a Java API to the Google app-layout web component.
     *
     * @return the RouterLayout which is the target of the Vaadin Flow Route
     */
    public OwlcmsRouterLayout getRouterLayout();

    /**
     * @param routerLayout
     */
    public void setRouterLayout(OwlcmsRouterLayout routerLayout);

}
