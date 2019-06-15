package app.owlcms.ui.home;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.AppLayoutAware;
import app.owlcms.ui.shared.ContentWrapping;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route(value = LoginView.LOGIN, layout = OwlcmsRouterLayout.class)
public class LoginView extends Composite<VerticalLayout> implements AppLayoutAware, ContentWrapping {
    
    Logger logger = (Logger)LoggerFactory.getLogger(LoginView.class);

    public static final String LOGIN = "login";
    private final LoginForm loginForm = new LoginForm();
    
    private FormLayout form = new FormLayout();
    private PasswordField pinField = new PasswordField();
    
    private OwlcmsRouterLayout routerLayout;


    public LoginView() {
        pinField.setClearButtonVisible(true);
        pinField.setRevealButtonVisible(true);
        pinField.setLabel("Enter PIN");
        pinField.setWidthFull();
        pinField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (!checkAuthenticated(value)) {
                logger.error("Incorrect PIN {}", value);
                pinField.setErrorMessage("Incorrect PIN");
                pinField.setInvalid(true);
            } else {
                pinField.setInvalid(false);
                UI.getCurrent().navigate(OwlcmsSession.getRequestedUrl());
            }
        });
        Button button = new Button("Login");
        button.addClickShortcut(Key.ENTER);
        button.setWidth("10em");
        button.getThemeNames().add("primary");
        button.getThemeNames().add("icon");
        
        getContent().add(new H3("Log in"), pinField,button);
        getContent().setWidth("20em");
        getContent().setAlignSelf(Alignment.CENTER, button);
        
//        
//        loginForm.addLoginListener(event -> {
//            UI current = UI.getCurrent();
//            checkAuthenticated(event.getPassword());
//            current.navigate(OwlcmsSession.getRequestedUrl());
//        });
//        loginForm.setForgotPasswordButtonVisible(false);
//        getContent().add(loginForm);
    }

    private boolean checkAuthenticated(String password) {
        boolean isAuthenticated = OwlcmsSession.isAuthenticated();

        if (!isAuthenticated) {      
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
                return true;
            } else {
                loginForm.setError(true);
                return false;
            }
        }
        return true;
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