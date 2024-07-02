package app.owlcms.prutils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinSession;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

public class SessionCleanup {
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
        logger.warn("cleaning up session {}",LoggerUtils.fullStackTrace());
        VaadinSession vs = VaadinSession.getCurrent();
        vs.access(() -> vs.close());
        return;
    }

    public void schedule(long delay, TimeUnit unit) {
        futureTask = scheduler.schedule(task, delay, unit);
    }

    public void reschedule(long delay, TimeUnit unit) {
        if (futureTask != null) {
            futureTask.cancel(false);
        }
        schedule(delay, unit);
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