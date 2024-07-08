package app.owlcms.prutils;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.components.elements.unload.UnloadObserverPR;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class SessionCleanup {
    private static final int INACTIVITY_SECONDS = 15 * 60; // 15 minutes
    private static final int SESSION_CLEANUP_SECONDS = 60; // 60 seconds
    Logger logger = (Logger) LoggerFactory.getLogger(SessionCleanup.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> futureTask;
    private VaadinSession vaadinSession;
    private URL pageURL;
    String uiTitle;
    private UnloadObserverPR eventObserver;

    private SessionCleanup(VaadinSession vs, UI pageUI, Component c, UnloadObserverPR eventObserver) {
        this.vaadinSession = vs;
        this.eventObserver = eventObserver;
        var p = pageUI.getPage();
        if (c instanceof HasDynamicTitle) {
            uiTitle = ((HasDynamicTitle) c).getPageTitle();
        }
        if (p != null) {
            p.fetchCurrentURL((u) -> {
                pageURL = u;
            });
        }
        logger.setLevel(Level.DEBUG);
    }

    public void cleanupSession() {
        vaadinSession.access(() -> {
            @SuppressWarnings("unchecked")
            Map<UI, Long> im = (Map<UI, Long>) vaadinSession.getAttribute("inactivityMap");
            int stillAlive = 0;
            if (im != null) {
                if (im.entrySet().isEmpty()) {
//                    // the previous iteration cleaned the map and navigated out of the pages.
//                    // because of asynchronicity, we wait to the following iteration before closing
//                    // sessions.
//                    logger.debug("invalidating session {}", System.identityHashCode(vaadinSession));
//                    vaadinSession.getSession().invalidate();
//                    vaadinSession.close();
//                    stop();
                } else {
                    long now = System.currentTimeMillis();
                    logger.debug("checking session {}", System.identityHashCode(vaadinSession));
                    for (Entry<UI, Long> uiEntry : im.entrySet()) {
                        if (uiEntry.getValue() == 0) {
                            // 0 means GONE.
                            logger.debug("   UI {} gone", System.identityHashCode(uiEntry.getKey()));
                        } else {
                            // positive means visible, negative means hidden (don't kill)
                            long timeElapsed = now - Math.abs(uiEntry.getValue());

                            // Define OWLCMS_INACTIVITY_SEC for testing
                            var inactivity = StartupUtils.getIntegerParam("inactivity_sec", INACTIVITY_SECONDS) * 1000;
                            boolean alive = timeElapsed < inactivity;
                            logger.debug("   UI {} timeElapsed={} alive={}", System.identityHashCode(uiEntry.getKey()),
                                    timeElapsed, alive);
                            if (alive) {
                                stillAlive++;
                            }
                        }
                    }
                    logger.debug("   stillAlive={}", stillAlive);
                }
            } else {
                logger.error("no registered map");
            }

            if (stillAlive == 0) {
                Iterator<Entry<UI, Long>> entryIterator = im.entrySet().iterator();
                while (entryIterator.hasNext()) {
                    Entry<UI, Long> e = entryIterator.next();
                    UI ui = e.getKey();
                    logger.debug("   leaving tab {}, reload URL={}", System.identityHashCode(ui), pageURL);
                    if (ui.isAttached()) {
                        ui.access(() -> {
                            eventObserver.doReload(
                                    Translator.translate("PublicResults.sessionExpiredTitle"), 
                                    Translator.translate("PublicResults.sessionExpiredText"), 
                                    //Translator.translate("PublicResults.sessionExpiredLabel",uiTitle),
                                    uiTitle,
                                    pageURL.toExternalForm());
//                            ui.close();
                        });
                    }
                    entryIterator.remove();
                }
                try {
                    Thread.sleep(1000);
                    logger.debug("*** invalidating session {}", System.identityHashCode(vaadinSession));
                    vaadinSession.getSession().invalidate();
                    vaadinSession.close();
                    stop();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            }
        });
        return;
    }

    public void scheduleAtFixedRate(long delay, TimeUnit unit) {
        futureTask = scheduler.scheduleAtFixedRate(() -> cleanupSession(), 0, delay, unit);
    }

    public void stop() {
        if (futureTask != null) {
            futureTask.cancel(true);
        }
        scheduler.shutdown();
    }

    public static SessionCleanup get(UI ui, Component c, UnloadObserverPR eventObserver) {
        OwlcmsSession os = OwlcmsSession.getCurrent();
        VaadinSession vs = VaadinSession.getCurrent();
        synchronized (os) {
            SessionCleanup cleanup = (SessionCleanup) os.getAttributes().get("sessionCleanup");
            if (cleanup == null) {
                cleanup = new SessionCleanup(vs, ui, c, eventObserver);
                OwlcmsSession.setAttribute("sessionCleanup", cleanup);
                // Define OWLCMS_CLEANUP_SEC for testing
                var cleanupSec = (long) StartupUtils.getIntegerParam("cleanup_sec", SESSION_CLEANUP_SECONDS);
                cleanup.scheduleAtFixedRate(cleanupSec, TimeUnit.SECONDS);
            }
            return cleanup;
        }
    }

}