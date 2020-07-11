/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.shared;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import app.owlcms.data.config.Config;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.home.LoginView;
import app.owlcms.utils.AccessUtils;

public interface RequireLogin extends BeforeEnterObserver {

    @Override
    public default void beforeEnter(BeforeEnterEvent event) {

        boolean isAuthenticated = OwlcmsSession.isAuthenticated();
        if (isAuthenticated) {
            // no check required
            OwlcmsSession.setAuthenticated(true);
            return;
        }

        String path = event.getLocation().getPath();
        String whiteList = Config.getCurrent().getParamAccessList();
        String pin = Config.getCurrent().getParamPin();

        boolean noPin = pin == null || pin.trim().isEmpty();
        boolean noWhiteList = whiteList == null || whiteList.trim().isEmpty();
        if ((noPin && noWhiteList)) {
            // no check required
            OwlcmsSession.setAuthenticated(true);
            return;
        } else if (noPin && AccessUtils.checkWhitelist()) {
            // no pin required, proper origin, no need to challenge
            OwlcmsSession.setAuthenticated(true);
            return;
        } else if (!path.equals(LoginView.LOGIN)) {
            // prompt user for PIN
            // (if whitelist membership is required, will be prompted even if no PIN is
            // required, so that an error message is shown)
            OwlcmsSession.setRequestedUrl(path);
            event.forwardTo(LoginView.LOGIN);
        } else {
            // already on login view, do nothing.
            // login will send to home.
        }
    }

}
