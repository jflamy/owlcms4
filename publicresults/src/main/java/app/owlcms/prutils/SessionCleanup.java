package app.owlcms.prutils;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class SessionCleanup {
    private static final long INACTIVITY_INTERVAL_MILLIS = 15 * 60 * 1000;  // 15 minutes
    private static final long SESSION_CLEANUP_SECONDS = 60; // 60 seconds
    Logger logger = (Logger) LoggerFactory.getLogger(SessionCleanup.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> futureTask;
    private VaadinSession vaadinSession;

    private SessionCleanup(VaadinSession vs) {
        this.vaadinSession = vs;
        logger.setLevel(Level.DEBUG);
    }

    public void cleanupSession() {
        // logger.debug("cleaning up session {}",
        // System.identityHashCode(vaadinSession));
        vaadinSession.access(() -> {
            @SuppressWarnings("unchecked")
            Map<UI, Long> im = (Map<UI, Long>) vaadinSession.getAttribute("inactivityMap");
            int stillAlive = 0;
            if (im != null) {
                if (im.entrySet().isEmpty()) {
                    // the previous iteration cleaned the map and navigated out of the pages.
                    // because of asynchronicity, we wait to the following iteration before closing sessions.
                    logger.debug("invalidating session {}", System.identityHashCode(vaadinSession));
                    vaadinSession.getSession().invalidate();
                    vaadinSession.close();
                    stop();
                } else {
                    long now = System.currentTimeMillis();
                    logger.debug("checking session {}", System.identityHashCode(vaadinSession));
                    for (Entry<UI, Long> uiEntry : im.entrySet()) {
                        if (uiEntry.getValue() == 0) {
                            // 0 means GONE.
                            logger.warn("   UI {} gone", System.identityHashCode(uiEntry.getKey()));
                        } else {
                            // positive means visible, negative means hidden (don't kill)
                            long timeElapsed = now - Math.abs(uiEntry.getValue());
                            boolean alive = timeElapsed < INACTIVITY_INTERVAL_MILLIS;
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
                    logger.debug("   leaving tab {}", System.identityHashCode(ui));
                    if (ui.isAttached()) {
                        ui.access(() -> {
                            ui.removeAll();
                            ui.getPage().executeJs("window.location.assign('about:blank')");
                            ui.close();
                        });
                    }
                    entryIterator.remove();
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

    public static SessionCleanup get() {
        OwlcmsSession os = OwlcmsSession.getCurrent();
        VaadinSession vs = VaadinSession.getCurrent();
        synchronized (os) {
            SessionCleanup cleanup = (SessionCleanup) os.getAttributes().get("sessionCleanup");
            if (cleanup == null) {
                cleanup = new SessionCleanup(vs);
                OwlcmsSession.setAttribute("sessionCleanup", cleanup);
                cleanup.scheduleAtFixedRate(SESSION_CLEANUP_SECONDS, TimeUnit.SECONDS);
            }
            return cleanup;
        }
    }

}