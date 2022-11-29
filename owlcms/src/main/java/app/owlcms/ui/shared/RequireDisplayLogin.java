/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.shared;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.data.config.Config;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.home.DisplayLoginView;
import app.owlcms.ui.home.LoginView;
import ch.qos.logback.classic.Logger;

public interface RequireDisplayLogin extends BeforeEnterObserver {

    Logger logger = (Logger) LoggerFactory.getLogger(RequireDisplayLogin.class);

    @Override
    public default void beforeEnter(BeforeEnterEvent event) {
        OwlcmsFactory.waitDBInitialized();
        boolean isDisplayAuthenticated = OwlcmsSession.isDisplayAuthenticated();
        if (isDisplayAuthenticated) {
            // no check required
            OwlcmsSession.setDisplayAuthenticated(true);
            return;
        }

        String path = event.getLocation().getPath();
        QueryParameters queryParameters = event.getLocation().getQueryParameters();
        String displayList = Config.getCurrent().getParamDisplayList();
        String displayPin = Config.getCurrent().getParamDisplayPin();
        String backdoorList = Config.getCurrent().getParamBackdoorList();

        boolean noDisplayPin = displayPin == null || displayPin.isBlank();
        boolean noDisplayList = displayList == null || displayList.isBlank();
        boolean backdoor = backdoorList != null && !backdoorList.isBlank();
        if ((noDisplayPin && noDisplayList)) {
            // no check required
            OwlcmsSession.setDisplayAuthenticated(true);
            return;
        } else if (backdoor && AccessUtils.checkBackdoor(AccessUtils.getClientIp())) {
            // explicit backdoor access allowed (e.g. for video capture of browser screens)
            logger.info("allowing backdoor access from {}", AccessUtils.getClientIp());
            OwlcmsSession.setDisplayAuthenticated(true);
            return;
        } else if (noDisplayPin && AccessUtils.isIpAllowedForDisplay(AccessUtils.getClientIp())) {
            // no pin required, proper origin, no need to challenge
            OwlcmsSession.setDisplayAuthenticated(true);
            return;
        } else if (!path.equals(LoginView.LOGIN)) {
            // prompt user for PIN
            // (if whitelist membership is required, will be prompted even if no PIN is
            // required, so that an error message is shown)
            OwlcmsSession.setRequestedUrl(path);
            OwlcmsSession.setRequestedQueryParameters(queryParameters);
            event.forwardTo(DisplayLoginView.LOGIN);
        } else {
            // already on login view, do nothing.
            // login will send to home.
        }
    }

}
