/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.shared;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.home.LoginView;

public interface RequireLogin extends BeforeEnterObserver {

    @Override
    public default void beforeEnter(BeforeEnterEvent event) {

        String path = event.getLocation().getPath();
        String ipAddress = System.getenv("IP");
        String pin = System.getenv("PIN");

        boolean isAuthenticated = OwlcmsSession.isAuthenticated();
        if ((ipAddress != null || pin != null) && !isAuthenticated && !path.equals(LoginView.LOGIN)) {
            OwlcmsSession.setRequestedUrl(path);
            event.forwardTo(LoginView.LOGIN);
        }
    }

}
