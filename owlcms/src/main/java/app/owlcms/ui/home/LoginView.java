/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.home;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.AppLayoutAware;
import app.owlcms.ui.shared.ContentWrapping;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.ui.shared.RequireLogin;
import ch.qos.logback.classic.Logger;

/**
 * Check for proper credentials.
 *
 * Scenarios:
 * <ul>
 * <li>If the IP environment variable is present, it is expected to be a commma-separated address list of IPv4
 * addresses. Browser must come from one of these addresses The IP address(es) will normally be those for the local
 * router or routers used at the competition site.
 * <li>if a PIN environment variable is present, the PIN will be required (even if no IP whitelist)
 * <li>if PIN enviroment variable is not present, all accesses from the whitelisted routers will be allowed. This can be
 * sufficient if the router password is well-protected (which is not likely). Users can type any NIP, including an empty
 * value.
 * <li>if neither IP nor PIN is present, no check is done ({@link RequireLogin} does not display this view).
 * </ul>
 */
@SuppressWarnings("serial")
@Route(value = LoginView.LOGIN, layout = OwlcmsRouterLayout.class)
public class LoginView extends Composite<VerticalLayout> implements AppLayoutAware, ContentWrapping {

    public static final String LOGIN = "login";

    static Logger logger = (Logger) LoggerFactory.getLogger(LoginView.class);

    private PasswordField pinField = new PasswordField();

    private OwlcmsRouterLayout routerLayout;

    public LoginView() {
        pinField.setClearButtonVisible(true);
        pinField.setRevealButtonVisible(true);
        pinField.setLabel(getTranslation("EnterPin"));
        pinField.setWidthFull();
        pinField.addValueChangeListener(event -> {
            String value = event.getValue();
            logger.warn("login input {}",value);
            if (checkAuthenticated(value)) {
                pinField.setErrorMessage(getTranslation("LoginDenied"));
                pinField.setInvalid(true);
            } else {
                pinField.setInvalid(false);
                redirect();
            }
        });

        // brute-force the color because some display views use a white text color.
        H3 h3 = new H3(getTranslation("Log_In"));
        h3.getStyle().set("color", "var(--lumo-header-text-color)");
        h3.getStyle().set("font-size", "var(--lumo-font-size-xl)");

        Button button = new Button(getTranslation("Login"));
        button.addClickShortcut(Key.ENTER);
        button.setWidth("10em");
        button.getThemeNames().add("primary");
        button.getThemeNames().add("icon");

        VerticalLayout form = new VerticalLayout();
        form.add(h3, pinField, button);
        form.setWidth("20em");
        form.setAlignSelf(Alignment.CENTER, button);

        getContent().add(form);

    }

    protected void redirect() {
        String requestedUrl = OwlcmsSession.getRequestedUrl();
        if (requestedUrl != null) {
            UI.getCurrent().navigate(requestedUrl);
        } else {
            UI.getCurrent().navigate(HomeNavigationContent.class);
        }
    }

    protected boolean checkAuthenticated(String value) {
        return !AccessUtils.checkAuthenticated(value);
    }

    @Override
    public OwlcmsRouterLayout getRouterLayout() {
        return routerLayout;
    }

    @Override
    public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
        this.routerLayout = routerLayout;
    }

}