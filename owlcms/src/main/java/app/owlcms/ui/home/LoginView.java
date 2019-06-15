package app.owlcms.ui.home;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.login.AbstractLogin.LoginEvent;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.AppLayoutAware;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route(value = LoginView.LOGIN, layout = OwlcmsRouterLayout.class)
public class LoginView extends Composite<Div> implements AppLayoutAware {
    
    Logger logger = (Logger)LoggerFactory.getLogger(LoginView.class);

    public static final String LOGIN = "login";
    private final LoginForm loginForm = new LoginForm();
    private OwlcmsRouterLayout routerLayout;


    public LoginView() {
        loginForm.addLoginListener(event -> {
            UI current = UI.getCurrent();
            checkAuthenticated(event);
            current.navigate(OwlcmsSession.getRequestedUrl());
        });
        loginForm.setForgotPasswordButtonVisible(false);
        getContent().add(loginForm);
    }

    private void checkAuthenticated(LoginEvent event) {
        boolean isAuthenticated = OwlcmsSession.isAuthenticated();

        if (!isAuthenticated) {
            String password = event.getPassword();
            
            String whitelistedIp = System.getenv("IP");
            String pin = System.getenv("PIN");
            
            String clientIp = getClientIp();
            if ("0:0:0:0:0:0:0:1".equals(clientIp)) {
                // compensate for IPv6 returned in spite of IPv4-only configuration...
                clientIp = "127.0.0.1";
            }
            logger.debug("checking client IP={} vs configured IP={}", clientIp,whitelistedIp);
            // must come from whitelisted address and have matching PIN
            if (clientIp.equals(whitelistedIp) && (pin == null || pin.contentEquals(password))) {
                OwlcmsSession.setAuthenticated(true);
            } else {
                loginForm.setError(true);
            }
        }
    }

    @Override
    public OwlcmsRouterLayout getRouterLayout() {
        return routerLayout;
    }

    @Override
    public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
        this.routerLayout = routerLayout;
    }
    
    private String getClientIp() {
        HttpServletRequest request;
        request = VaadinServletRequest.getCurrent().getHttpServletRequest();

        String remoteAddr = "";

        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }

        return remoteAddr;
    }

}