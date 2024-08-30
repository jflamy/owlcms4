package app.owlcms.publicresults;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.servlet.StopProcessingException;
import ch.qos.logback.classic.Logger;

/**
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 */
//@PWA(name = "owlcms remote scoreboard", shortName = "publicresults")
@Theme(variant = Lumo.DARK)
@Push
public class AppShell implements AppShellConfigurator, VaadinServiceInitListener {
    
    Logger logger = (Logger) LoggerFactory.getLogger(AppShell.class);
    private static final AtomicInteger activeSessions = new AtomicInteger(0);

    /**
     * @see com.vaadin.flow.server.VaadinServiceInitListener#serviceInit(com.vaadin.flow.server.ServiceInitEvent)
     */
    @Override
    public void serviceInit(ServiceInitEvent serviceInitEvent) {
        
        serviceInitEvent.getSource().addUIInitListener(uiInitEvent -> {
            LoadingIndicatorConfiguration conf = uiInitEvent.getUI().getLoadingIndicatorConfiguration();

            // disable default theme on loading indicator -> loading indicator isn't shown
            // conf.setApplyDefaultTheme(false);
            /*
             * Delay for showing the indicator and setting the 'first' class name.
             */
            conf.setFirstDelay(2000); // 300ms is the default

            /* Delay for setting the 'second' class name */
            conf.setSecondDelay(3500); // 1500ms is the default

            /* Delay for setting the 'third' class name */
            conf.setThirdDelay(5000); // 5000ms is the default
        });

        serviceInitEvent.getSource().addSessionInitListener(sessionInitEvent -> {
            VaadinSession session = sessionInitEvent.getSession();
            activeSessions.incrementAndGet();
            Main.logSessionMemUsage("nbSessions++");
            ErrorHandler handler = new ErrorHandler() {
                @Override
                public void error(ErrorEvent errorEvent) {
                    Throwable t = errorEvent.getThrowable();
                    if (!(t instanceof StopProcessingException)) {
                        LoggerFactory.getLogger("app.owlcms.errorHandler").warn(t.toString());
                    }
                }
            };
            session.setErrorHandler(handler);
        });
        
        serviceInitEvent.getSource().addSessionDestroyListener(sde -> {
            logger.trace("Session {} destroyed.", sde.getSession());
            activeSessions.decrementAndGet();
            Main.logSessionMemUsage("nbSessions--");
        });
    }

    public static AtomicInteger getActiveSessions() {
        return activeSessions;
    }
}
