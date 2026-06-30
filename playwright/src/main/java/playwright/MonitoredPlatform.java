package playwright;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Owns the browser and browser context for one FOP. Creates one {@link MonitoredPage}
 * per selected {@link UpdateCheck.BoardRole}.
 */
class MonitoredPlatform {
    private final String fop;
    private final Browser browser;
    private final List<MonitoredPage> pages;

    private MonitoredPlatform(String fop, Browser browser, List<MonitoredPage> pages) {
        this.fop = fop;
        this.browser = browser;
        this.pages = pages;
    }

    static MonitoredPlatform open(Playwright playwright, UpdateCheck.Config config, String fop,
            UpdateCheck.CleanLog log) {
        log.browserLaunch(fop, config);
        Browser browser = playwright.chromium().launch(UpdateCheck.launchOptions(config));
        BrowserContext context = browser.newContext();
        List<MonitoredPage> pages = new ArrayList<>();
        for (UpdateCheck.BoardRole role : config.boards()) {
            Page page = context.newPage();
            page.setDefaultTimeout(1500);
            String path = role == UpdateCheck.BoardRole.ANNOUNCER
                    ? config.announcerPath()
                    : config.attemptBoardPath();
            String url = UpdateCheck.buildUrl(config.baseUrl(), path, fop);
            log.openPage(fop, role, url);
            log.navigation(fop, role, UpdateCheck.navigateWithRetry(page, url, fop, role.name().toLowerCase()));
            UpdateCheck.SnapshotReader reader = role == UpdateCheck.BoardRole.ANNOUNCER
                    ? new AnnouncerSnapshotReader()
                    : new AttemptBoardSnapshotReader();
            reader.waitForReady(page, UpdateCheck.STARTUP_SNAPSHOT_TIMEOUT);
            UpdateCheck.DisplayMatcher matcher = role == UpdateCheck.BoardRole.ANNOUNCER
                    ? new AnnouncerDisplayMatcher()
                    : new AttemptBoardDisplayMatcher();
            MonitoredPage mp = new MonitoredPage(fop, role, page, reader, matcher);
            UpdateCheck.SnapshotRead initial = reader.read(mp);
            log.testIds(fop, role, initial);
            pages.add(mp);
        }
        return new MonitoredPlatform(fop, browser, pages);
    }

    String fop() {
        return this.fop;
    }

    Browser browser() {
        return this.browser;
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

    boolean stalled() {
        return this.pages.stream().anyMatch(MonitoredPage::stalled);
    }

    /** Returns the first stalled page, or {@code null}. */
    MonitoredPage firstStalledPage() {
        return this.pages.stream().filter(MonitoredPage::stalled).findFirst().orElse(null);
    }
}