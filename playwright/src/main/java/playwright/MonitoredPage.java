package playwright;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import com.microsoft.playwright.Page;

/**
 * Tracks one page (one board role) inside a monitored FOP.
 * <p>
 * A {@link MonitoredPlatform} owns one browser and one context per FOP;
 * it creates one {@code MonitoredPage} for each selected {@link UpdateCheck.BoardRole}.
 * Each page carries its own event cursor ({@code lastSequence}), watcher thread,
 * stall state, {@link UpdateCheck.SnapshotReader}, and {@link UpdateCheck.DisplayMatcher}.
 * </p>
 * Passing structured {@code fop}/{@code role} typed fields throughout means
 * no concatenated "RED attempt" strings are ever stored or passed around;
 * the combined tag is assembled only inside {@link UpdateCheck.CleanLog} methods.
 */
class MonitoredPage {
    private final String fop;
    private final UpdateCheck.BoardRole role;
    private final Page page;
    private final Object eventSignal = new Object();
    private final UpdateCheck.SnapshotReader snapshotReader;
    private final UpdateCheck.DisplayMatcher displayMatcher;

    private long lastSequence;
    private UpdateCheck.MqttEvent latestEvent;
    private Thread watcherThread;
    private volatile boolean stalled;
    private volatile String stallReason = "";

    MonitoredPage(String fop, UpdateCheck.BoardRole role, Page page,
            UpdateCheck.SnapshotReader snapshotReader, UpdateCheck.DisplayMatcher displayMatcher) {
        this.fop = fop;
        this.role = role;
        this.page = page;
        this.snapshotReader = snapshotReader;
        this.displayMatcher = displayMatcher;
    }

    String fop() {
        return this.fop;
    }

    UpdateCheck.BoardRole role() {
        return this.role;
    }

    Page page() {
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
        while (System.currentTimeMillis() < deadline) {
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
        if (!this.page.isClosed()) {
            return false;
        }
        this.stallReason = "playwright page closed";
        log.status(this.fop, this.role,
                "playwright page closed while waiting for " + expected.display()
                        + " after " + state.elapsedMillis() + "ms / " + state.snapshotReads() + " polls",
                false);
        return true;
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
