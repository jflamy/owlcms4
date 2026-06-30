package playwright;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Groups the monitored pages for one FOP. Each {@link MonitoredPage} owns its own
 * Playwright instance and browser thread.
 */
class MonitoredPlatform {
    private final String fop;
    private final List<MonitoredPage> pages;

    private MonitoredPlatform(String fop, List<MonitoredPage> pages) {
        this.fop = fop;
        this.pages = pages;
    }

    static MonitoredPlatform open(UpdateCheck.Config config, String fop,
            UpdateCheck.CleanLog log) {
        List<MonitoredPage> pages = new ArrayList<>();
        for (UpdateCheck.BoardRole role : config.boards()) {
            String path = role == UpdateCheck.BoardRole.ANNOUNCER
                    ? config.announcerPath()
                    : config.attemptBoardPath();
            String url = UpdateCheck.buildUrl(config.baseUrl(), path, fop);
            UpdateCheck.SnapshotReader reader = role == UpdateCheck.BoardRole.ANNOUNCER
                    ? new AnnouncerSnapshotReader()
                    : new AttemptBoardSnapshotReader();
            UpdateCheck.DisplayMatcher matcher = role == UpdateCheck.BoardRole.ANNOUNCER
                    ? new AnnouncerDisplayMatcher()
                    : new AttemptBoardDisplayMatcher();
        MonitoredPage mp = MonitoredPage.open(fop, role, url, config, reader, matcher, log);
        UpdateCheck.SnapshotRead initial = mp.readSnapshot();
            log.testIds(fop, role, initial);
            pages.add(mp);
        }
    return new MonitoredPlatform(fop, pages);
    }

    String fop() {
        return this.fop;
    }

    List<MonitoredPage> pages() {
        return this.pages;
    }

    synchronized void updateLastSequence(long seq) {
        for (MonitoredPage p : this.pages) {
            p.updateLastSequence(seq);
        }
    }

    synchronized void acceptEvent(UpdateCheck.MqttEvent event) {
        for (MonitoredPage p : this.pages) {
            p.acceptEvent(event);
        }
    }

    void startWatching(AtomicBoolean running, Duration timeout, UpdateCheck.CleanLog log) {
        for (MonitoredPage p : this.pages) {
            p.startWatching(running, timeout, log);
        }
    }

    void stopWatching(UpdateCheck.CleanLog log) {
        for (MonitoredPage p : this.pages) {
            p.stopWatching(log);
        }
    }

    void closePlaywright(UpdateCheck.CleanLog log) {
        for (MonitoredPage p : this.pages) {
            p.closePlaywright(log);
        }
    }

    boolean stalled() {
        return this.pages.stream().anyMatch(MonitoredPage::stalled);
    }

    /** Returns the first stalled page, or {@code null}. */
    MonitoredPage firstStalledPage() {
        return this.pages.stream().filter(MonitoredPage::stalled).findFirst().orElse(null);
    }
}