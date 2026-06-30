package playwright;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;

/**
 * Tracks one page (one board role) inside a monitored FOP.
 * <p>
 * Each page owns its own Playwright instance, browser, context, and single Playwright
 * executor thread. All Playwright object access is marshalled through that thread.
 * The page also carries its own event cursor ({@code lastSequence}), watcher thread,
 * stall state, {@link UpdateCheck.SnapshotReader}, and {@link UpdateCheck.DisplayMatcher}.
 * </p>
 * Passing structured {@code fop}/{@code role} typed fields throughout means
 * no concatenated "RED attempt" strings are ever stored or passed around;
 * the combined tag is assembled only inside {@link UpdateCheck.CleanLog} methods.
 */
class MonitoredPage {
    private final String fop;
    private final UpdateCheck.BoardRole role;
    private final Object eventSignal = new Object();
    private final UpdateCheck.SnapshotReader snapshotReader;
    private final UpdateCheck.DisplayMatcher displayMatcher;
    private final ExecutorService playwrightExecutor;
    private final AtomicReference<Thread> playwrightThread = new AtomicReference<>();

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private long lastSequence;
    private UpdateCheck.MqttEvent latestEvent;
    private Thread watcherThread;
    private volatile boolean stalled;
    private volatile String stallReason = "";
    // Playwright reads run on this dedicated thread so the watcher thread can bound them with a
    // wall-clock timeout; an unresponsive renderer then surfaces as a fast MISS, never a hang.
    private volatile boolean unresponsive;

    private MonitoredPage(String fop, UpdateCheck.BoardRole role,
            UpdateCheck.SnapshotReader snapshotReader, UpdateCheck.DisplayMatcher displayMatcher) {
        this.fop = fop;
        this.role = role;
        this.snapshotReader = snapshotReader;
        this.displayMatcher = displayMatcher;
        this.playwrightExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "playwright-page-" + fop + "-" + role.name().toLowerCase());
            t.setDaemon(true);
            this.playwrightThread.set(t);
            return t;
        });
    }

    static MonitoredPage open(String fop, UpdateCheck.BoardRole role, String url, UpdateCheck.Config config,
            UpdateCheck.SnapshotReader snapshotReader, UpdateCheck.DisplayMatcher displayMatcher,
            UpdateCheck.CleanLog log) {
        MonitoredPage mp = new MonitoredPage(fop, role, snapshotReader, displayMatcher);
        log.browserLaunch(fop, role, config);
        log.openPage(fop, role, url);
        mp.callOnPlaywrightThread(() -> {
            mp.playwright = Playwright.create();
            mp.browser = mp.playwright.chromium().launch(UpdateCheck.launchOptions(config));
            mp.context = mp.browser.newContext();
            mp.page = mp.context.newPage();
            mp.page.setDefaultTimeout(1500);
            log.navigation(fop, role, UpdateCheck.navigateWithRetry(mp.page, url, fop, role.name().toLowerCase()));
            snapshotReader.waitForReady(mp.page, UpdateCheck.STARTUP_SNAPSHOT_TIMEOUT);
            return null;
        });
        return mp;
    }

    boolean unresponsive() {
        return this.unresponsive;
    }

    /**
     * Reads a DOM snapshot with a hard wall-clock ceiling ({@link UpdateCheck#SNAPSHOT_READ_TIMEOUT}).
     * If the underlying {@code page.evaluate} does not return in time (hung/crashed renderer), the
     * page is flagged {@code unresponsive} and an empty read is returned so the polling loop fails
     * fast instead of blocking. Once unresponsive, further reads short-circuit immediately.
     */
    UpdateCheck.SnapshotRead readSnapshot() {
        if (this.unresponsive) {
            return UpdateCheck.SnapshotRead.empty("page unresponsive (prior read timed out)", "");
        }
        if (isOnPlaywrightThread()) {
            return UpdateCheck.SnapshotRead.empty("read rejected: timeout guard unavailable on Playwright owner thread", "");
        }
        Future<UpdateCheck.SnapshotRead> future = this.playwrightExecutor.submit(this::readSnapshotOnPlaywrightThread);
        try {
            return future.get(UpdateCheck.SNAPSHOT_READ_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            this.unresponsive = true;
            this.stallReason = "playwright snapshot read exceeded "
                    + UpdateCheck.SNAPSHOT_READ_TIMEOUT.toMillis() + "ms (renderer unresponsive)";
            future.cancel(true);
            return UpdateCheck.SnapshotRead.empty("read timed out after "
                    + UpdateCheck.SNAPSHOT_READ_TIMEOUT.toMillis() + "ms (renderer unresponsive)", "");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            return UpdateCheck.SnapshotRead.empty("read failed: "
                    + (cause == null ? e.toString() : cause.toString()), "");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return UpdateCheck.SnapshotRead.empty("read interrupted", "");
        } catch (RejectedExecutionException e) {
            return UpdateCheck.SnapshotRead.empty("read rejected: Playwright executor stopped", "");
        }
    }

    private UpdateCheck.SnapshotRead readSnapshotOnPlaywrightThread() {
        return this.snapshotReader.read(this);
    }

    String fop() {
        return this.fop;
    }

    UpdateCheck.BoardRole role() {
        return this.role;
    }

    Page page() {
        if (!isOnPlaywrightThread()) {
            throw new IllegalStateException("Playwright page access outside owning thread for "
                    + this.fop + " " + this.role.name().toLowerCase());
        }
        if (this.page == null) {
            throw new IllegalStateException("Playwright page not opened for "
                    + this.fop + " " + this.role.name().toLowerCase());
        }
        return this.page;
    }

    UpdateCheck.SnapshotReader snapshotReader() {
        return this.snapshotReader;
    }

    UpdateCheck.DisplayMatcher displayMatcher() {
        return this.displayMatcher;
    }

    synchronized void updateLastSequence(long seq) {
        this.lastSequence = Math.max(this.lastSequence, seq);
    }

    synchronized void acceptEvent(UpdateCheck.MqttEvent event) {
        if (this.latestEvent == null || event.sequence() > this.latestEvent.sequence()) {
            this.latestEvent = event;
        }
        synchronized (this.eventSignal) {
            this.eventSignal.notifyAll();
        }
    }

    synchronized UpdateCheck.MqttEvent latestEventAfterLastSequence() {
        return latestEventAfterSequence(this.lastSequence);
    }

    synchronized UpdateCheck.MqttEvent latestEventAfterSequence(long afterSequence) {
        if (this.latestEvent != null && this.latestEvent.sequence() > afterSequence) {
            return this.latestEvent;
        }
        return null;
    }

    void startWatching(AtomicBoolean running, Duration timeout, UpdateCheck.CleanLog log) {
        String threadName = "playwright-watch-" + this.fop + "-" + this.role.name().toLowerCase();
        this.watcherThread = new Thread(() -> watch(running, timeout, log), threadName);
        this.watcherThread.start();
    }

    void stopWatching(UpdateCheck.CleanLog log) {
        synchronized (this.eventSignal) {
            this.eventSignal.notifyAll();
        }
        if (this.watcherThread != null) {
            try {
                this.watcherThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.stop(this.fop, this.role, "watcher join interrupted");
            }
        }
    }

    void closePlaywright(UpdateCheck.CleanLog log) {
        try {
            Future<Void> future = this.playwrightExecutor.submit(() -> {
                closePageObjects(log);
                return null;
            });
            future.get(UpdateCheck.SNAPSHOT_READ_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.stop(this.fop, this.role, "Playwright close timed out");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            log.stop(this.fop, this.role, "Playwright close failed: "
                    + (cause == null ? e.toString() : cause.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.stop(this.fop, this.role, "Playwright close interrupted");
        } catch (RejectedExecutionException e) {
            log.stop(this.fop, this.role, "Playwright close rejected: executor stopped");
        } finally {
            this.playwrightExecutor.shutdownNow();
        }
    }

    boolean stalled() {
        return this.stalled;
    }

    String stallReason() {
        return this.stallReason;
    }

    private void watch(AtomicBoolean running, Duration timeout, UpdateCheck.CleanLog log) {
        try {
            while (running.get()) {
                UpdateCheck.MqttEvent event = waitForNextEvent(running);
                if (event == null) {
                    break;
                }
                updateLastSequence(event.sequence());
                if (UpdateCheck.isPlaywrightPause(event)) {
                    log.pause(this.fop, this.role, UpdateCheck.pauseReason(event));
                    continue;
                }
                UpdateCheck.VerificationResult result = verifyExpectedDisplay(event, timeout, log);
                updateLastSequence(result.lastSequence());
                if (!result.matched()) {
                    this.stalled = true;
                    if (this.stallReason.isBlank()) {
                        this.stallReason = "playwright expected display";
                    }
                    return;
                }
            }
        } catch (RuntimeException e) {
            this.stalled = true;
            this.stallReason = "playwright watcher error: " + e.getMessage();
            log.status(this.fop, this.role, this.stallReason, false);
        }
    }

    private UpdateCheck.MqttEvent waitForNextEvent(AtomicBoolean running) {
        while (running.get()) {
            UpdateCheck.MqttEvent event = latestEventAfterLastSequence();
            if (event != null) {
                return event;
            }
            synchronized (this.eventSignal) {
                try {
                    this.eventSignal.wait(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted", e);
                }
            }
        }
        return null;
    }

    UpdateCheck.VerificationResult verifyExpectedDisplay(UpdateCheck.MqttEvent event, Duration timeout,
            UpdateCheck.CleanLog log) {
        long currentSequence = event.sequence();
        UpdateCheck.ExpectedDisplay expected = UpdateCheck.parseExpectedDisplay(event);
        UpdateCheck.ExpectationState state = new UpdateCheck.ExpectationState();
        log.expecting(this.fop, this.role, expected);
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline && !this.unresponsive) {
            if (failIfPageClosed(expected, state, log)) {
                return new UpdateCheck.VerificationResult(false, currentSequence, this.fop, this.role);
            }
            UpdateCheck.MqttEvent newer = latestEventAfterSequence(currentSequence);
            UpdateCheck.SupersedeResult supersede = applySupersede(newer, expected, timeout, log);
            if (supersede.done()) {
                return supersede.result();
            }
            if (supersede.applied()) {
                currentSequence = supersede.sequence();
                expected = supersede.expected();
                state = new UpdateCheck.ExpectationState();
                deadline = supersede.deadline();
            }
            if (failIfPageClosed(expected, state, log)) {
                return new UpdateCheck.VerificationResult(false, currentSequence, this.fop, this.role);
            }
            if (this.displayMatcher.expectedDisplayVisible(expected, state, log, this)) {
                return new UpdateCheck.VerificationResult(true, currentSequence, this.fop, this.role);
            }
            newer = latestEventAfterSequence(currentSequence);
            supersede = applySupersede(newer, expected, timeout, log);
            if (supersede.done()) {
                return supersede.result();
            }
            if (supersede.applied()) {
                currentSequence = supersede.sequence();
                expected = supersede.expected();
                state = new UpdateCheck.ExpectationState();
                deadline = supersede.deadline();
                continue;
            }
            UpdateCheck.sleep(50);
        }
        if (this.unresponsive) {
            // Renderer stopped responding: do not touch the page again; report a fast MISS.
            this.displayMatcher.logExpectedMiss(expected, state, log, this);
            return new UpdateCheck.VerificationResult(false, currentSequence, this.fop, this.role);
        }
        if (failIfPageClosed(expected, state, log)) {
            return new UpdateCheck.VerificationResult(false, currentSequence, this.fop, this.role);
        }
        if (this.displayMatcher.expectedDisplayVisible(expected, state, log, this)) {
            return new UpdateCheck.VerificationResult(true, currentSequence, this.fop, this.role);
        }
        this.displayMatcher.logExpectedMiss(expected, state, log, this);
        return new UpdateCheck.VerificationResult(false, currentSequence, this.fop, this.role);
    }

    private boolean failIfPageClosed(UpdateCheck.ExpectedDisplay expected, UpdateCheck.ExpectationState state,
            UpdateCheck.CleanLog log) {
        if (!isPageClosed()) {
            return false;
        }
        this.stallReason = "playwright page closed";
        log.status(this.fop, this.role,
                "playwright page closed while waiting for " + expected.display()
                        + " after " + state.elapsedMillis() + "ms / " + state.snapshotReads() + " polls",
                false);
        return true;
    }

    private <T> T callOnPlaywrightThread(Callable<T> callable) {
        if (isOnPlaywrightThread()) {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        try {
            return this.playwrightExecutor.submit(callable).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(cause == null ? e : cause);
        }
    }

    private boolean isPageClosed() {
        if (this.unresponsive) {
            return false;
        }
        if (isOnPlaywrightThread()) {
            return this.page == null || this.page.isClosed();
        }
        Future<Boolean> future = this.playwrightExecutor.submit(() -> this.page == null || this.page.isClosed());
        try {
            return future.get(UpdateCheck.SNAPSHOT_READ_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            this.unresponsive = true;
            this.stallReason = "playwright page state read exceeded "
                    + UpdateCheck.SNAPSHOT_READ_TIMEOUT.toMillis() + "ms (renderer unresponsive)";
            future.cancel(true);
            return false;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            this.stallReason = "playwright page state read failed: "
                    + (cause == null ? e.toString() : cause.toString());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        } catch (RejectedExecutionException e) {
            this.stallReason = "playwright page state read rejected: executor stopped";
            return false;
        }
    }

    private boolean isOnPlaywrightThread() {
        return Thread.currentThread() == this.playwrightThread.get();
    }

    private void closePageObjects(UpdateCheck.CleanLog log) {
        closeContext(log);
        closeBrowser(log);
        closePlaywrightDriver(log);
    }

    private void closeContext(UpdateCheck.CleanLog log) {
        if (this.context == null) {
            return;
        }
        try {
            this.context.close();
        } catch (PlaywrightException e) {
            log.info("Context close reported for " + this.fop + " " + this.role.name().toLowerCase()
                    + ": " + e.getMessage());
        }
    }

    private void closeBrowser(UpdateCheck.CleanLog log) {
        if (this.browser == null) {
            return;
        }
        try {
            this.browser.close();
        } catch (PlaywrightException e) {
            log.info("Browser close reported for " + this.fop + " " + this.role.name().toLowerCase()
                    + ": " + e.getMessage());
        }
    }

    private void closePlaywrightDriver(UpdateCheck.CleanLog log) {
        if (this.playwright == null) {
            return;
        }
        try {
            this.playwright.close();
        } catch (PlaywrightException e) {
            log.info("Playwright close reported for " + this.fop + " " + this.role.name().toLowerCase()
                    + ": " + e.getMessage());
        }
    }

    private UpdateCheck.SupersedeResult applySupersede(UpdateCheck.MqttEvent newer,
            UpdateCheck.ExpectedDisplay expected, Duration timeout, UpdateCheck.CleanLog log) {
        if (newer == null) {
            return UpdateCheck.SupersedeResult.notApplied();
        }
        if (UpdateCheck.isPlaywrightPause(newer)) {
            log.superceded(this.fop, this.role, expected, "PAUSE " + UpdateCheck.pauseReason(newer));
            log.pause(this.fop, this.role, UpdateCheck.pauseReason(newer));
            return UpdateCheck.SupersedeResult.done(
                    new UpdateCheck.VerificationResult(true, newer.sequence(), this.fop, this.role));
        }
        UpdateCheck.ExpectedDisplay newerExpected = UpdateCheck.parseExpectedDisplay(newer);
        log.superceded(this.fop, this.role, expected, newerExpected.display());
        log.expecting(this.fop, this.role, newerExpected);
        return UpdateCheck.SupersedeResult.applied(newer.sequence(), newerExpected,
                System.currentTimeMillis() + timeout.toMillis());
    }
}
