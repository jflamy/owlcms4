/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.shared;

import com.vaadin.flow.router.HasDynamicTitle;

public interface OwlcmsContent
        extends ContentWrapping, AppLayoutAware, HasDynamicTitle, SafeEventBusRegistration, RequireLogin {

}
