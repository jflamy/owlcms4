package app.owlcms.ui.home;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

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
import com.vaadin.flow.server.VaadinServletRequest;

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
 * <li>If the IP environment variable is present, it is expected to be a
 * commma-separated address list of IPv4 addresses. Browser must come from one
 * of these addresses The IP address(es) will normally be those for the local
 * router or routers used at the competition site.
 * <li>if a PIN environment variable is present, the PIN will be required (even
 * if no IP whitelist)
 * <li>if PIN enviroment variable is not present, all accesses from the
 * whitelisted routers will be allowed. This can be sufficient if the router
 * password is well-protected (which is not likely). Users can type any NIP,
 * including an empty value.
 * <li>if neither IP nor PIN is present, no check is done ({@link RequireLogin}
 * does not display this view).
 * </ul>
 */
@SuppressWarnings("serial")
@Route(value = LoginView.LOGIN, layout = OwlcmsRouterLayout.class)
public class LoginView extends Composite<VerticalLayout> implements AppLayoutAware, ContentWrapping {

    static Logger logger = (Logger) LoggerFactory.getLogger(LoginView.class);

    public static final String LOGIN = "login"; //$NON-NLS-1$
    private PasswordField pinField = new PasswordField();

    private OwlcmsRouterLayout routerLayout;

    public LoginView() {
        pinField.setClearButtonVisible(true);
        pinField.setRevealButtonVisible(true);
        pinField.setLabel(getTranslation("EnterPin")); //$NON-NLS-1$
        pinField.setWidthFull();
        pinField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (!checkAuthenticated(value)) {
                pinField.setErrorMessage(getTranslation("LoginDenied")); //$NON-NLS-1$
                pinField.setInvalid(true);
            } else {
                pinField.setInvalid(false);
                String requestedUrl = OwlcmsSession.getRequestedUrl();
                if (requestedUrl != null) {
                    UI.getCurrent().navigate(requestedUrl);
                } else {
                    UI.getCurrent().navigate(HomeNavigationContent.class);
                }
            }
        });

        // brute-force the color because some display views use a white text color.
        H3 h3 = new H3(getTranslation("Log_In")); //$NON-NLS-1$
        h3.getStyle().set("color", "var(--lumo-header-text-color)"); //$NON-NLS-1$ //$NON-NLS-2$
        h3.getStyle().set("font-size", "var(--lumo-font-size-xl)"); //$NON-NLS-1$ //$NON-NLS-2$

        Button button = new Button(getTranslation("Login")); //$NON-NLS-1$
        button.addClickShortcut(Key.ENTER);
        button.setWidth("10em"); //$NON-NLS-1$
        button.getThemeNames().add("primary"); //$NON-NLS-1$
        button.getThemeNames().add("icon"); //$NON-NLS-1$

        VerticalLayout form = new VerticalLayout();
        form.add(h3, pinField, button);
        form.setWidth("20em"); //$NON-NLS-1$
        form.setAlignSelf(Alignment.CENTER, button);

        getContent().add(form);

    }

    private boolean checkAuthenticated(String password) {
        boolean isAuthenticated = OwlcmsSession.isAuthenticated();

        if (!isAuthenticated) {
            boolean whiteListed = checkWhitelist();

            // check for PIN if one is specified
            String pin = getPin();
            if (whiteListed && (pin == null || pin.contentEquals(password))) {
                OwlcmsSession.setAuthenticated(true);
                return true;
            } else {
                OwlcmsSession.setAuthenticated(false);
                return false;
            }
        }
        return true;
    }

    public static String getPin() {
        String pin = System.getenv("PIN"); //$NON-NLS-1$
        if (pin == null) pin = System.getProperty("PIN");
        return pin;
    }

    public static boolean checkWhitelist() {
        String whiteList = getWhitelist();
        String clientIp = getClientIp();
        if ("0:0:0:0:0:0:0:1".equals(clientIp)) { //$NON-NLS-1$
            // compensate for IPv6 returned in spite of IPv4-only configuration...
            clientIp = "127.0.0.1"; //$NON-NLS-1$
        }
        boolean whiteListed;
        if (whiteList != null) {
            List<String> whiteListedList = Arrays.asList(whiteList.split(",")); //$NON-NLS-1$
            logger.debug("checking client IP={} vs configured IP={}", clientIp, whiteList); //$NON-NLS-1$
            // must come from whitelisted address and have matching PIN
            whiteListed = whiteListedList.contains(clientIp);
            if (!whiteListed) {
                logger.error("login attempt from non-whitelisted host {} (whitelist={})", clientIp, whiteListedList); //$NON-NLS-1$
            }
        } else {
            // no white list, allow all IP addresses
            whiteListed = true;
        }
        return whiteListed;
    }

    public static String getWhitelist() {
        String whiteList = System.getenv("IP"); //$NON-NLS-1$
        if (whiteList == null) whiteList = System.getProperty("IP");
        return whiteList;
    }

    @Override
    public OwlcmsRouterLayout getRouterLayout() {
        return routerLayout;
    }

    @Override
    public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
        this.routerLayout = routerLayout;
    }

    public static String getClientIp() {
        HttpServletRequest request;
        request = VaadinServletRequest.getCurrent().getHttpServletRequest();

        String remoteAddr = ""; //$NON-NLS-1$

        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR"); //$NON-NLS-1$
            if (remoteAddr == null || "".equals(remoteAddr)) { //$NON-NLS-1$
                remoteAddr = request.getRemoteAddr();
            }
        }

        return remoteAddr;
    }

}