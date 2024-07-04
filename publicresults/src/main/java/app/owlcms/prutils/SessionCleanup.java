package app.owlcms.prutils;

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
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

public class SessionCleanup {
    private static final long INACTIVITY_INTERVAL = 15 * 1000;
    Logger logger = (Logger) LoggerFactory.getLogger(SessionCleanup.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Runnable task;
    private ScheduledFuture<?> futureTask;

    private SessionCleanup(Runnable task) {
        this.task = task;
    }

    private SessionCleanup() {
        task = () -> cleanupSession();
    }

    private void cleanupSession() {
        logger.warn("cleaning up session {}", LoggerUtils.fullStackTrace());
        VaadinSession vs = VaadinSession.getCurrent();
        vs.access(() -> {
            @SuppressWarnings("unchecked")
            Map<UI, Long> im = (Map<UI, Long>) vs.getAttribute("inactivityMap");
            long now = System.currentTimeMillis();
            int stillAlive = 0;
            for (Entry<UI, Long> e : im.entrySet()) {
                if (e.getValue() <= 0 || (now - e.getValue() < INACTIVITY_INTERVAL)) {
                    stillAlive++;
                }
            }
            if (stillAlive == 0) {
                for (Entry<UI, Long> e : im.entrySet()) {
                    UI ui = e.getKey();
                    ui.access(() -> {
                        ui.removeAll();
                        ui.getPage().executeJs("window.location='about.blank'");
                    });
                }
                // we can close the session now that all the pages have been redirected.
                vs.close();
            }
        });
        return;
    }

    public void scheduleAtFixedRate(long delay, TimeUnit unit) {
        futureTask = scheduler.scheduleAtFixedRate(task, delay, delay, unit);
    }

    public void stop() {
        if (futureTask != null) {
            futureTask.cancel(true);
        }
        scheduler.shutdown();
    }

    public static SessionCleanup get() {
        OwlcmsSession os = OwlcmsSession.getCurrent();
        synchronized (os) {
            SessionCleanup cleanup = (SessionCleanup) OwlcmsSession.getAttribute("sessionCleanup");
            if (cleanup == null) {
                cleanup = new SessionCleanup();
                OwlcmsSession.setAttribute("sessionCleanup", cleanup);
                cleanup.scheduleAtFixedRate(20, TimeUnit.SECONDS);
            }
            return cleanup;
        }
    }

    public static SessionCleanup get(Runnable task) {
        OwlcmsSession os = OwlcmsSession.getCurrent();
        synchronized (os) {
            SessionCleanup cleanup = (SessionCleanup) OwlcmsSession.getAttribute("sessionCleanup");
            if (cleanup == null) {
                cleanup = new SessionCleanup(task);
                OwlcmsSession.setAttribute("sessionCleanup", cleanup);
            }
            return cleanup;
        }
    }
}