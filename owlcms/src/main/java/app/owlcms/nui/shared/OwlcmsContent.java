/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.HasDynamicTitle;

public interface OwlcmsContent
        extends ContentWrapping, OwlcmsLayoutAware, HasDynamicTitle, SafeEventBusRegistration, RequireLogin {
    
    public default void setHeaderContent() {
        LoggerFactory.getLogger("OwlcmsContent").warn("!!!!! not implemented");
    }

}
