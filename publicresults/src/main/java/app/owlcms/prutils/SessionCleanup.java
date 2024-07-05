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
import ch.qos.logback.classic.Logger;

public class SessionCleanup {
    private static final long INACTIVITY_INTERVAL = 15 * 1000;
    static Logger logger = (Logger) LoggerFactory.getLogger(SessionCleanup.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> futureTask;
    private VaadinSession vaadinSession;

    private SessionCleanup(VaadinSession vs) {
        this.vaadinSession = vs;
    }

    public void cleanupSession() {
        logger.warn("cleaning up session {}", System.identityHashCode(vaadinSession));
        vaadinSession.access(() -> {
            logger.warn("   inside");
            @SuppressWarnings("unchecked")
            Map<UI, Long> im = (Map<UI, Long>) vaadinSession.getAttribute("inactivityMap");
            logger.warn("   after get");
            int stillAlive = 0;
            if (im != null) {
                long now = System.currentTimeMillis();

                for (Entry<UI, Long> e : im.entrySet()) {
                    if (e.getValue() <= 0 || (now - e.getValue() < INACTIVITY_INTERVAL)) {
                        stillAlive++;
                    }
                }

                logger.warn("   cleaning up session {}: stillAlive: {}", System.identityHashCode(vaadinSession),
                        stillAlive);
                if (im.entrySet().isEmpty()) {
                    logger.warn("   invalidating sessions {}", System.identityHashCode(vaadinSession));
                    vaadinSession.getSession().invalidate();
                    vaadinSession.close();
                    stop();
                }
            } else {
                logger.warn("   no registered map");
            }

            if (stillAlive == 0) {
                Iterator<Entry<UI, Long>> entryIterator = im.entrySet().iterator();
                while (entryIterator.hasNext()) {
                    Entry<UI, Long> e = entryIterator.next();
                    UI ui = e.getKey();
                    logger.warn("   going away from UI {}", ui);
                    if (ui.isAttached()) {
                        ui.access(() -> {
                            ui.removeAll();
                            ui.getPage().executeJs("window.location='about:blank'");
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
                cleanup.scheduleAtFixedRate(20, TimeUnit.SECONDS);
            }
            return cleanup;
        }
    }
}