package playwright;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

class MonitoredPlatform {
    private final String fop;
    private final Browser browser;
    private final Page page;
    private final Object eventSignal = new Object();
    private long lastSequence;
    private UpdateCheck.MqttEvent latestEvent;
    private Thread watcherThread;
    private volatile boolean stalled;
    private volatile String stallReason = "";

    private MonitoredPlatform(String fop, Browser browser, Page page) {
        this.fop = fop;
        this.browser = browser;
        this.page = page;
    }

    static MonitoredPlatform open(Playwright playwright, UpdateCheck.Config config, String fop, UpdateCheck.CleanLog log) {
        Browser browser = playwright.chromium().launch(UpdateCheck.launchOptions(config));
        BrowserContext context = browser.newContext();
        Page page = context.newPage();
        page.setDefaultTimeout(1500);
        String url = UpdateCheck.buildUrl(config.baseUrl(), config.announcerPath(), fop);
        log.openPage(fop, "announcer", url);
        log.navigation(fop, "announcer", UpdateCheck.navigateWithRetry(page, url, fop, "announcer"));
        UpdateCheck.waitForTestIds(page, UpdateCheck.STARTUP_SNAPSHOT_TIMEOUT);
        log.testIds(fop, UpdateCheck.count(page, UpdateCheck.ATHLETE_NAME_SELECTOR),
                UpdateCheck.count(page, UpdateCheck.ATHLETE_ATTEMPT_SELECTOR),
                UpdateCheck.readTopBarDisplay(page));
        return new MonitoredPlatform(fop, browser, page);
    }

    String fop() {
        return this.fop;
    }

    Browser browser() {
        return this.browser;
    }

    Page page() {
        return this.page;
    }

    synchronized void updateLastSequence(long lastSequence) {
        this.lastSequence = Math.max(this.lastSequence, lastSequence);
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
        this.watcherThread = new Thread(() -> watch(running, timeout, log), "playwright-watch-" + this.fop);
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
                log.stop(this.fop, "watcher join interrupted");
            }
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
                    log.pause(this.fop, UpdateCheck.pauseReason(event));
                    continue;
                }
                UpdateCheck.VerificationResult result = verifyExpectedDisplay(event, timeout, log);
                updateLastSequence(result.lastSequence());
                if (!result.matched()) {
                    this.stalled = true;
                    this.stallReason = "playwright expected display";
                    return;
                }
            }
        } catch (RuntimeException e) {
            this.stalled = true;
            this.stallReason = "playwright watcher error: " + e.getMessage();
            log.status(this.fop, this.stallReason, false);
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
        log.expecting(this.fop, expected);
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
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
            if (expectedDisplayVisible(expected, state, log)) {
                return new UpdateCheck.VerificationResult(true, currentSequence, this.fop);
            }
            // A newer reference may have arrived while we were polling the DOM above.
            // If so, abandon the current target immediately instead of sleeping first.
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
        if (expectedDisplayVisible(expected, state, log)) {
            return new UpdateCheck.VerificationResult(true, currentSequence, this.fop);
        }
        logExpectedMiss(expected, log);
        return new UpdateCheck.VerificationResult(false, currentSequence, this.fop);
    }

    private UpdateCheck.SupersedeResult applySupersede(UpdateCheck.MqttEvent newer,
            UpdateCheck.ExpectedDisplay expected, Duration timeout, UpdateCheck.CleanLog log) {
        if (newer == null) {
            return UpdateCheck.SupersedeResult.notApplied();
        }
        if (UpdateCheck.isPlaywrightPause(newer)) {
            log.superceded(this.fop, expected, "PAUSE " + UpdateCheck.pauseReason(newer));
            log.pause(this.fop, UpdateCheck.pauseReason(newer));
            return UpdateCheck.SupersedeResult.done(new UpdateCheck.VerificationResult(true, newer.sequence(), this.fop));
        }
        UpdateCheck.ExpectedDisplay newerExpected = UpdateCheck.parseExpectedDisplay(newer);
        log.superceded(this.fop, expected, newerExpected.display());
        log.expecting(this.fop, newerExpected);
        return UpdateCheck.SupersedeResult.applied(newer.sequence(), newerExpected,
                System.currentTimeMillis() + timeout.toMillis());
    }

    private boolean expectedDisplayVisible(UpdateCheck.ExpectedDisplay expected, UpdateCheck.ExpectationState state,
            UpdateCheck.CleanLog log) {
        UpdateCheck.Snapshot snapshot = UpdateCheck.readSnapshot(this);
        if (snapshot == null) {
            return false;
        }
        // Header confirmation: name + attempt match the expected display
        boolean athleteOk = UpdateCheck.matchesExpected(snapshot, expected);
        // Grid confirmation: the current-athlete row in the grid is consistent
        // with the header (yellow weight == header weight) AND reflects the
        // reference weight, so a stale-but-self-consistent weight cannot confirm.
        boolean gridOk = UpdateCheck.gridConfirmed(snapshot, expected);
        if (athleteOk && !state.athleteConfirmed()) {
            state.confirmAthlete();
            log.confirmedAthlete(this.fop, state.elapsedMillis(), snapshot);
        }
        if (gridOk && !state.gridConfirmed()) {
            state.confirmGrid();
            log.confirmedGrid(this.fop, state.elapsedMillis());
        }
        // A transient disagreement (grid still rendering the previous weight, or
        // weight not yet at the reference value) is normal and not an error: we
        // simply keep polling. Only a disagreement that persists through the whole
        // window is reported as a miss. Require both conditions true in the SAME
        // snapshot read so we never confirm on an inconsistent intermediate state.
        return athleteOk && gridOk;
    }

    private void logExpectedMiss(UpdateCheck.ExpectedDisplay expected, UpdateCheck.CleanLog log) {
        UpdateCheck.Snapshot current = UpdateCheck.readSnapshot(this);
        if (current == null) {
            log.status(this.fop, "playwright expected " + expected.display() + " but got (no snapshot)", false);
            return;
        }
        // Report exactly what failed
        if (!UpdateCheck.normalize(current.athleteName()).equals(UpdateCheck.normalize(expected.displayName()))) {
            log.status(this.fop, "playwright expected athlete " + expected.displayName() + " but saw "
                    + current.athleteName(), false);
        } else if (!UpdateCheck.digitsOnly(current.attempt()).equals(UpdateCheck.digitsOnly(expected.attempt()))) {
            log.status(this.fop, "playwright expected attempt " + expected.attempt() + " but saw "
                    + current.attempt(), false);
        } else if (!UpdateCheck.digitsOnly(current.weight()).equals(UpdateCheck.digitsOnly(expected.weight()))) {
            log.status(this.fop, "playwright expected weight " + expected.weight() + " but header shows "
                    + current.weight(), false);
        } else if (!UpdateCheck.gridConfirmed(current, expected)) {
            log.status(this.fop, "playwright grid not confirmed: header weight=" + current.weight()
                    + " expected weight=" + expected.weight()
                    + " gridCell='" + current.gridFirstCell() + "'", false);
        } else {
            log.status(this.fop, "playwright expected " + expected.display() + " but confirmation failed", false);
        }
    }
}