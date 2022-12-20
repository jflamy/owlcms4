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
    
    public default void setHeaderContent() {
        throw new UnsupportedOperationException(this.getClass()+" setHeaderContent not defined");
    }
    
    public default FlexLayout createButtonArea() {
        throw new UnsupportedOperationException(this.getClass()+" createButtonArea not defined");
    }
    

}
