package playwright;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;

import sun.misc.Signal;

/**
 * Opens announcer pages for each configured FOP, watches MQTT decision events,
 * and verifies that the Vaadin current-athlete top bar updates after lifting-order changes.
 */
public class UpdateCheck {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(UpdateCheck.class);
    private static final String DEFAULT_BASE_URL = "http://192.168.1.177:8080";
    private static final String DEFAULT_MQTT_URI = "tcp://192.168.1.177:1883";
    private static final String DEFAULT_FOP = "WHITE";
    private static final String DEFAULT_ANNOUNCER_PATH = "/lifting/announcer";
    static final String ATHLETE_NAME_SELECTOR = "[data-testid='current-athlete-name']";
    static final String ATHLETE_ATTEMPT_SELECTOR = "[data-testid='current-athlete-attempt']";
    private static final String ATHLETE_WEIGHT_SELECTOR = "[data-testid='current-athlete-weight']";
    private static final String PLAYWRIGHT_DONE_TOPIC = "owlcms/fop/playwright/done";
    static final Duration STARTUP_SNAPSHOT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration PLAYWRIGHT_EXPECTED_TIMEOUT = Duration.ofMillis(1500);
    private static final long[] STARTUP_RETRY_DELAYS_MS = { 2000, 5000, 10000 };

    public static void main(String[] args) throws Exception {
        Config config = Config.fromArgs(args);
        AtomicBoolean running = new AtomicBoolean(true);
        CleanLog log = new CleanLog();
        installCtrlCHandler(running, log);
        log.section("CONFIG");
        log.value("OWLCMS", config.baseUrl());
        log.value("Stop URL", buildStopUrl(config.baseUrl()));
        log.value("MQTT", config.mqttUri());
        log.value("FOPs", String.join(",", config.fops()));
        log.value("Decision", config.decisionWord());
        log.value("Headless", Boolean.toString(config.headless()));
        log.value("Publish MQTT", Boolean.toString(config.publish()));

        try (MqttEventCollector mqtt = MqttEventCollector.connect(config.mqttUri());
            Playwright playwright = Playwright.create()) {
            String browserChannel = System.getenv("PLAYWRIGHT_BROWSER_CHANNEL");
            if (browserChannel != null && !browserChannel.isBlank()) {
                log.value("Browser channel", browserChannel.trim());
            }

            List<MonitoredPlatform> platforms = new ArrayList<>();
            try {
                platforms = openPlatforms(playwright, config, log);
                mqtt.monitorPlatforms(platforms);
                startEnterToStopThread(running, log);
                waitForInitialDisplay(platforms, log);
                supervisePlatforms(mqtt, config, log, platforms, running);
            } finally {
                stopPlatforms(platforms, log);
                closeBrowsers(platforms, log);
            }
        }
        System.exit(0);
    }

    private static void installCtrlCHandler(AtomicBoolean running, CleanLog log) {
        installStopSignal("INT", running, log);
        installStopSignal("TERM", running, log);
        installStopSignal("HUP", running, log);
    }

    private static void installStopSignal(String signalName, AtomicBoolean running, CleanLog log) {
        try {
            Signal.handle(new Signal(signalName), signal -> {
                if (running.compareAndSet(true, false)) {
                    log.info(signalName + " received; stopping cleanly...");
                }
            });
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.info(signalName + " signal handler unavailable.");
        }
    }

    private static void closeBrowser(Browser browser, CleanLog log) {
        try {
            browser.close();
        } catch (PlaywrightException e) {
            log.info("Browser close reported: " + e.getMessage());
        }
    }

    private static void closeBrowsers(List<MonitoredPlatform> platforms, CleanLog log) {
        for (MonitoredPlatform platform : platforms) {
            closeBrowser(platform.browser(), log);
        }
    }

    private static void supervisePlatforms(MqttEventCollector mqtt, Config config, CleanLog log,
            List<MonitoredPlatform> platforms,
            AtomicBoolean running) throws Exception {
        long initialSequence = mqtt.lastSequence();
        for (MonitoredPlatform platform : platforms) {
            platform.updateLastSequence(initialSequence);
        }
        boolean stalled = false;
        if (config.publish()) {
            log.section("MQTT INPUT");
        } else {
            log.section("WATCHING");
            log.info("Waiting for Playwright MQTT display instructions. Press Enter here to stop cleanly.");
        }

        for (MonitoredPlatform platform : platforms) {
            platform.startWatching(running, PLAYWRIGHT_EXPECTED_TIMEOUT, log);
        }
        if (config.publish()) {
            publishDecisionSequence(mqtt, config, log);
        }

        while (running.get()) {
            if (mqtt.done()) {
                log.info("Playwright done received; stopping cleanly.");
                running.set(false);
                break;
            }
            if (mqtt.lostAfterStart.get()) {
                stopOnStalledPage(config, log, "(all)", "MQTT connection loss");
                stalled = true;
                break;
            }
            MonitoredPlatform stalledPlatform = firstStalledPlatform(platforms);
            if (stalledPlatform != null) {
                stopOnStalledPage(config, log, stalledPlatform.fop(), stalledPlatform.stallReason());
                stalled = true;
                break;
            }
            sleep(200);
        }
        log.info("Stopped.");
        if (stalled && running.get()) {
            waitForExitRequest(running);
        }
    }

    private static MonitoredPlatform firstStalledPlatform(List<MonitoredPlatform> platforms) {
        for (MonitoredPlatform platform : platforms) {
            if (platform.stalled()) {
                return platform;
            }
        }
        return null;
    }

    private static void stopPlatforms(List<MonitoredPlatform> platforms, CleanLog log) {
        for (MonitoredPlatform platform : platforms) {
            platform.stopWatching(log);
        }
    }

    static boolean gridConfirmed(Snapshot snapshot, ExpectedDisplay expected) {
        // gridFirstCell format is "startNumber|weight" (e.g., "8|84")
        String gridCell = snapshot.gridFirstCell();
        if (gridCell == null || gridCell.isBlank() || !gridCell.contains("|")) {
            return false; // grid not visible / not parsed
        }
        String startNumber = gridCell.substring(0, gridCell.indexOf('|')).trim();
        String gridWeight = gridCell.substring(gridCell.indexOf('|') + 1).trim();
        if (startNumber.isBlank() || gridWeight.isBlank()) {
            return false;
        }
        // The grid must be consistent with the header (yellow weight == header weight)
        // and both must reflect the reference weight sent by OWLCMS. All comparisons are
        // by digits only so they are language/unit independent.
        String gridDigits = digitsOnly(gridWeight);
        return gridDigits.equals(digitsOnly(snapshot.weight()))
                && gridDigits.equals(digitsOnly(expected.weight()));
    }


    static boolean matchesExpected(Snapshot snapshot, ExpectedDisplay expected) {
        // Name is compared as text (normalized). Attempt number and weight are compared
        // by their digits only so the check is language-independent: the attempt label
        // (e.g. "Snatch #2" / "Arraché #2") and the weight unit ("88 kg") differ by
        // locale, but the significant digits do not.
        return normalize(snapshot.athleteName()).equals(normalize(expected.displayName()))
                && digitsOnly(snapshot.attempt()).equals(digitsOnly(expected.attempt()))
                && digitsOnly(snapshot.weight()).equals(digitsOnly(expected.weight()));
    }

    static String digitsOnly(String text) {
        return text == null ? "" : text.replaceAll("[^0-9]", "");
    }

    static ExpectedDisplay parseExpectedDisplay(MqttEvent event) {
        return new ExpectedDisplay(jsonString(event.payload(), "displayName"),
                jsonString(event.payload(), "attempt"),
                jsonLong(event.payload(), "sequence"),
                jsonLong(event.payload(), "requestedWeight") + "");
    }

    static String pauseReason(MqttEvent event) {
        String reason = jsonString(event.payload(), "reason");
        long sequence = jsonLong(event.payload(), "sequence");
        return reason + " seq=" + sequence;
    }

    private static String jsonString(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"")
                .matcher(json);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static long jsonLong(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(\\d+)").matcher(json);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : -1L;
    }

    private static void stopOnStalledPage(Config config, CleanLog log, String fop, String trigger) {
        log.stop(fop, "announcer page stalled after " + trigger + "; stopping watcher");
        log.browserInspection(fop, config);
        requestOwlcmsStop(buildStopUrl(config.baseUrl()), log, fop);
    }

    private static void waitForExitRequest(AtomicBoolean running) {
        while (running.get()) {
            sleep(1000);
        }
    }

    private static void requestOwlcmsStop(String stopUrl, CleanLog log, String fop) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(stopUrl))
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.stop(fop, "OWLCMS stop requested status=" + response.statusCode() + " url=" + stopUrl);
        } catch (IllegalArgumentException | IOException e) {
            log.stop(fop, "OWLCMS stop request failed url=" + stopUrl + " error=" + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.stop(fop, "OWLCMS stop request interrupted url=" + stopUrl);
        }
    }

    private static void startEnterToStopThread(AtomicBoolean running, CleanLog log) {
        Thread thread = new Thread(() -> {
            try {
                System.in.read();
                running.set(false);
            } catch (Exception e) {
                running.set(false);
            }
        }, "playwright-stop-listener");
        thread.setDaemon(true);
        thread.start();
        log.info("Press Enter to stop without Ctrl-C.");
    }

    private static void waitForInitialDisplay(List<MonitoredPlatform> platforms, CleanLog log) {
        for (MonitoredPlatform platform : platforms) {
            Snapshot snapshot = waitForSnapshot(platform, STARTUP_SNAPSHOT_TIMEOUT);
            if (snapshot != null) {
                log.status(platform.fop(), "initial athlete: " + snapshot.display(), true);
            } else {
                log.status(platform.fop(), "no initial athlete displayed", false);
            }
        }
    }

    private static List<MonitoredPlatform> openPlatforms(Playwright playwright, Config config,
            CleanLog log) {
        List<MonitoredPlatform> platforms = new ArrayList<>();
        for (String fop : config.fops()) {
            platforms.add(MonitoredPlatform.open(playwright, config, fop, log));
        }
        return platforms;
    }

    static BrowserType.LaunchOptions launchOptions(Config config) {
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(config.headless());
        String browserChannel = browserChannel();
        if (browserChannel != null && !browserChannel.isBlank()) {
            launchOptions.setChannel(browserChannel.trim());
        }
        return launchOptions;
    }

    static String browserChannel() {
        return System.getenv("PLAYWRIGHT_BROWSER_CHANNEL");
    }

    static String browserLaunchDetails(Config config) {
        return "browser=chromium headless=" + config.headless()
                + " channel=" + firstNonBlank(browserChannel(), "default")
                + " skipDownload=" + firstNonBlank(System.getenv("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD"), "false");
    }

    static Response navigateWithRetry(Page page, String url, String fop, String pageName) {
        int attempt = 1;
        while (true) {
            try {
                return page.navigate(url);
            } catch (PlaywrightException e) {
                long retryDelay = retryDelayForAttempt(attempt);
                logger.info("[{}] NAV  {} attempt {} failed ({}); retrying in {}s", fop, pageName, attempt,
                        e.getMessage(), retryDelay / 1000);
                sleep(retryDelay);
                attempt++;
            }
        }
    }

    static void waitForTestIds(Page page, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (count(page, ATHLETE_NAME_SELECTOR) > 0 && count(page, ATHLETE_ATTEMPT_SELECTOR) > 0) {
                return;
            }
            sleep(250);
        }
    }

    static int count(Page page, String selector) {
        try {
            return page.locator(selector).count();
        } catch (PlaywrightException e) {
            return -1;
        }
    }

    private static Snapshot waitForSnapshot(MonitoredPlatform platform, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        Snapshot last = null;
        while (System.currentTimeMillis() < deadline) {
            last = readSnapshot(platform);
            if (last != null && !last.athleteName().isBlank() && !last.athleteName().equals("-")
                    && !last.athleteName().equals("–")) {
                return last;
            }
            sleep(250);
        }
        return null;
    }

    static Snapshot readSnapshot(MonitoredPlatform platform) {
        return readSnapshotRead(platform).snapshot();
    }

    static SnapshotRead readSnapshotRead(MonitoredPlatform platform) {
        try {
            Page page = platform.page();
            String athleteName = readTextBySelector(page, ATHLETE_NAME_SELECTOR);
            String attempt = readTextBySelector(page, ATHLETE_ATTEMPT_SELECTOR);
            String weight = readTextBySelector(page, ATHLETE_WEIGHT_SELECTOR);
            // Extract just the numeric part of weight (e.g., "84 kg" -> "84")
            weight = weight.replaceAll("[^0-9]", "");
            String bodyText = readBodyText(page);
            String pageSummary = readPageSummary(page, bodyText);
            if (athleteName.isBlank() || attempt.isBlank()) {
                AthleteDisplay fallback = readTopBarDisplay(page);
                if (fallback.name().isBlank() || fallback.attempt().isBlank()) {
                    fallback = parseAthleteDisplay(bodyText, platform.fop());
                }
                athleteName = firstNonBlank(athleteName, fallback.name());
                attempt = firstNonBlank(attempt, fallback.attempt());
            }
            String gridFirstCell = readGridFirstCell(page);
            if (athleteName.isBlank() && attempt.isBlank() && weight.isBlank() && gridFirstCell.isBlank()) {
                return SnapshotRead.empty("no readable athlete, attempt, weight, or grid cell", pageSummary);
            }
            Snapshot snapshot = new Snapshot(platform.fop(), athleteName, attempt, weight, gridFirstCell);
            if (attempt.isBlank()) {
                return SnapshotRead.of(snapshot, "empty attempt", pageSummary);
            }
            if (athleteName.isBlank()) {
                return SnapshotRead.of(snapshot, "empty athlete", pageSummary);
            }
            if (weight.isBlank()) {
                return SnapshotRead.of(snapshot, "empty weight", pageSummary);
            }
            if (gridFirstCell.isBlank()) {
                return SnapshotRead.of(snapshot, "empty grid cell", pageSummary);
            }
            return SnapshotRead.of(snapshot, "ok", pageSummary);
        } catch (PlaywrightException e) {
            return SnapshotRead.empty("PlaywrightException: " + e.getMessage(), "");
        }
    }

    static AthleteDisplay readTopBarDisplay(Page page) {
        try {
            Object value = page.evaluate("() => {"
                    + "const topBar = document.querySelector('.athleteGridTopBar');"
                    + "if (!topBar) return {name: '', attempt: ''};"
                    + "const children = Array.from(topBar.children);"
                    + "const fullName = children.find(child => child.querySelector(':scope > h2') && child.querySelector(':scope > h3'));"
                    + "const lastName = fullName?.querySelector(':scope > h2')?.innerText || '';"
                    + "const firstName = fullName?.querySelector(':scope > h3 > span')?.innerText"
                    + "    || fullName?.querySelector(':scope > h3')?.innerText || '';"
                    + "const attempt = children.find(child => child.tagName === 'H2')?.innerText || '';"
                    + "return {name: `${lastName} ${firstName}`.trim(), attempt};"
                    + "}");
            if (value instanceof Map<?, ?> map) {
                Object name = map.get("name");
                Object attempt = map.get("attempt");
                return new AthleteDisplay(name != null ? normalize(name.toString()) : "",
                        attempt != null ? normalize(attempt.toString()) : "");
            }
        } catch (PlaywrightException e) {
            // fall through to body-based extraction
        }
        return new AthleteDisplay("", "");
    }

    private static AthleteDisplay parseAthleteDisplay(String bodyText, String platform) {
        String normalizedBody = normalize(bodyText);
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(platform)
                + "\\s+(.+?)\\s+((?:Snatch|Clean\\s+and\\s+Jerk|Clean\\s*&\\s*Jerk|C&J)\\s*#?\\d+\\s+\\d+\\s*kg)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(normalizedBody);
        if (!matcher.find()) {
            return new AthleteDisplay("", "");
        }
        String athleteName = normalize(matcher.group(1)).replaceFirst("^[^\\p{Alnum}]+", "").trim();
        return new AthleteDisplay(athleteName, normalize(matcher.group(2)));
    }

    private static String readGridFirstCell(Page page) {
        try {
            // For Vaadin Grid, get the first data row's start number and weight.
            // The weight in the current athlete row is highlighted with class="yellow".
            // We locate the yellow span structurally (no translatable text), follow its
            // slot into the grid's shadow DOM to find its <tr>, then read the first cell
            // of that same row to get the start number.
            Object cellData = page.evaluate("() => {"
                    + "const grid = document.querySelector('vaadin-grid');"
                    + "if (!grid) return '';"
                    + "const yellowSpan = grid.querySelector('span.yellow');"
                    + "if (!yellowSpan) return '';"
                    + "const weight = yellowSpan.innerText.trim();"
                    + "const yellowCell = yellowSpan.closest('vaadin-grid-cell-content');"
                    + "const yellowSlot = yellowCell ? yellowCell.getAttribute('slot') : null;"
                    + "const shadowRoot = grid.shadowRoot;"
                    + "let startNumber = '';"
                    + "if (shadowRoot && yellowSlot) {"
                    + "  const slotEl = shadowRoot.querySelector('slot[name=\"' + yellowSlot + '\"]');"
                    + "  if (slotEl) {"
                    + "    const tr = slotEl.closest('tr');"
                    + "    if (tr) {"
                    + "      const firstTd = tr.querySelector('td');"
                    + "      const firstSlot = firstTd ? firstTd.querySelector('slot') : null;"
                    + "      const firstSlotName = firstSlot ? firstSlot.getAttribute('name') : null;"
                    + "      if (firstSlotName) {"
                    + "        const firstCellContent = grid.querySelector('vaadin-grid-cell-content[slot=\"' + firstSlotName + '\"]');"
                    + "        if (firstCellContent) {"
                    + "          startNumber = firstCellContent.innerText.trim();"
                    + "        }"
                    + "      }"
                    + "    }"
                    + "  }"
                    + "}"
                    + "return startNumber + '|' + weight;"
                    + "}");
            String result = cellData != null ? cellData.toString().trim() : "";
            // Only return if we have a valid start number and weight (e.g., "8|84")
            if (!result.isBlank() && !result.equals("|") && !result.startsWith("|")) {
                return result;
            }
        } catch (Exception e) {
            // fall through
        }
        return "";
    }

    private static String readBodyText(Page page) {
        try {
            Object value = page.evaluate("() => document.body ? document.body.innerText : ''");
            return value != null ? value.toString() : "";
        } catch (PlaywrightException e) {
            return "";
        }
    }

    private static String readPageSummary(Page page, String bodyText) {
        return "url=" + readUrl(page)
                + " title='" + readTitle(page) + "'"
                + " selectors[name=" + count(page, ATHLETE_NAME_SELECTOR)
                + " attempt=" + count(page, ATHLETE_ATTEMPT_SELECTOR)
                + " weight=" + count(page, ATHLETE_WEIGHT_SELECTOR) + "]"
                + " body='" + abbreviate(normalize(bodyText), 240) + "'";
    }

    private static String readUrl(Page page) {
        try {
            return page.url();
        } catch (PlaywrightException e) {
            return "<unavailable>";
        }
    }

    private static String readTitle(Page page) {
        try {
            return normalize(page.title());
        } catch (PlaywrightException e) {
            return "<unavailable>";
        }
    }

    private static String abbreviate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private static String readTextBySelector(Page page, String selector) {
        try {
            Object value = page.evaluate("selector => {"
                    + "const element = document.querySelector(selector);"
                    + "return element ? element.innerText : '';"
                    + "}", selector);
            return value != null ? normalize(value.toString()) : "";
        } catch (PlaywrightException e) {
            return "";
        }
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private static String fopFromTopic(String topic) {
        int lastSlash = topic.lastIndexOf('/');
        return lastSlash >= 0 ? topic.substring(lastSlash + 1) : "?";
    }

    static boolean isPlaywrightPause(MqttEvent event) {
        return event.topic().startsWith("owlcms/fop/playwright/pause/");
    }

    private static void publishDecisionSequence(MqttEventCollector mqtt, Config config, CleanLog log) throws Exception {
        for (String fop : config.fops()) {
            if (config.publishDown()) {
                String topic = "owlcms/refbox/downEmitted/" + fop;
                mqtt.publish(topic, "");
                log.publish(fop, topic, "");
                sleep(150);
            }
            for (int referee = 1; referee <= 3; referee++) {
                String topic = "owlcms/refbox/decision/" + fop;
                String payload = referee + " " + config.decisionWord();
                mqtt.publish(topic, payload);
                log.publish(fop, topic, payload);
                sleep(config.decisionSpacingMillis());
            }
        }
    }

    static String buildUrl(String baseUrl, String path, String platform) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        String separator = normalizedPath.contains("?") ? "&" : "?";
        String encodedPlatform = URLEncoder.encode(platform, StandardCharsets.UTF_8);
        return normalizedBase + normalizedPath + separator + "fop=" + encodedPlatform;
    }

    private static String buildStopUrl(String baseUrl) {
        // Always connect via 127.0.0.1 so ControlPanelServlet's isLocalhost() check passes.
        // The base URL may use a LAN IP; the server only accepts stop requests from localhost.
        try {
            java.net.URI uri = java.net.URI.create(baseUrl);
            int port = uri.getPort() > 0 ? uri.getPort() : 8080;
            return "http://127.0.0.1:" + port + "/controlpanel/stop";
        } catch (IllegalArgumentException e) {
            return "http://127.0.0.1:8080/controlpanel/stop";
        }
    }

    static String normalize(String text) {
        return text == null ? "" : text.replaceAll("[\\p{Z}\\s]+", " ").trim();
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        }
    }

    private static long retryDelayForAttempt(int attempt) {
        return STARTUP_RETRY_DELAYS_MS[Math.min(attempt - 1, STARTUP_RETRY_DELAYS_MS.length - 1)];
    }

    record Config(String baseUrl, String mqttUri, List<String> fops, String announcerPath,
            Duration timeout, boolean headless, boolean publish, boolean publishDown,
            String decisionWord, long decisionSpacingMillis) {
        static Config fromArgs(String[] args) {
            Map<String, String> values = Arrays.stream(args)
                    .filter(arg -> arg.startsWith("--") && arg.contains("="))
                    .map(arg -> arg.substring(2).split("=", 2))
                    .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1], (left, right) -> right));

            List<String> fops = parseFops(firstNonBlank(values.get("fops"), values.get("platforms"), values.get("fop"),
                    values.get("platform"), env("OWLCMS_FOPS"), env("OWLCMS_FOP"), DEFAULT_FOP));
            boolean headless = Boolean.parseBoolean(firstNonBlank(values.get("headless"), env("OWLCMS_PLAYWRIGHT_HEADLESS"),
                    env("PLAYWRIGHT_HEADLESS"), "false"));
            String decisionWord = firstNonBlank(values.get("decision"), env("OWLCMS_DECISION"), "good").toLowerCase(Locale.ROOT);
            if (!decisionWord.equals("good") && !decisionWord.equals("bad")) {
                throw new IllegalArgumentException("--decision must be good or bad");
            }
                String baseUrl = firstNonBlank(values.get("baseUrl"), values.get("url"), env("OWLCMS_BASE_URL"),
                    env("OWLCMS_URL"), DEFAULT_BASE_URL);
            return new Config(
                    baseUrl,
                    firstNonBlank(values.get("mqtt"), env("OWLCMS_MQTT_URI"), DEFAULT_MQTT_URI),
                    fops,
                    firstNonBlank(values.get("announcerPath"), env("OWLCMS_ANNOUNCER_PATH"), DEFAULT_ANNOUNCER_PATH),
                    Duration.ofSeconds(Long.parseLong(firstNonBlank(values.get("timeoutSeconds"), env("OWLCMS_TIMEOUT_SECONDS"), "20"))),
                    headless,
                    Boolean.parseBoolean(firstNonBlank(values.get("publish"), env("OWLCMS_PUBLISH"), "false")),
                    Boolean.parseBoolean(firstNonBlank(values.get("publishDown"), env("OWLCMS_PUBLISH_DOWN"), "true")),
                    decisionWord,
                    Long.parseLong(firstNonBlank(values.get("decisionSpacingMillis"), env("OWLCMS_DECISION_SPACING_MILLIS"), "250")));
        }

        private static List<String> parseFops(String raw) {
            List<String> fops = Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(platform -> !platform.isBlank())
                    .distinct()
                    .toList();
            if (fops.isEmpty()) {
                throw new IllegalArgumentException("At least one FOP is required");
            }
            return fops;
        }

        private static String env(String name) {
            return System.getenv(name);
        }

        private static String firstNonBlank(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
            throw new IllegalArgumentException("No non-blank value supplied");
        }
    }

    record Snapshot(String platform, String athleteName, String attempt, String weight, String gridFirstCell) {
        String display() {
            if (attempt.isBlank()) {
                return athleteName;
            }
            return athleteName + " | " + attempt;
        }
    }

    record SnapshotRead(Snapshot snapshot, String reason, String pageSummary) {
        static SnapshotRead of(Snapshot snapshot, String reason, String pageSummary) {
            return new SnapshotRead(snapshot, reason, pageSummary);
        }

        static SnapshotRead empty(String reason, String pageSummary) {
            return new SnapshotRead(null, reason, pageSummary);
        }

        String summary() {
            String suffix = pageSummary == null || pageSummary.isBlank() ? "" : "; " + pageSummary;
            if (snapshot == null) {
                return reason + suffix;
            }
            return reason + " snapshot[name='" + snapshot.athleteName()
                    + "' attempt='" + snapshot.attempt()
                    + "' weight='" + snapshot.weight()
                    + "' gridCell='" + snapshot.gridFirstCell() + "']" + suffix;
        }
    }

    record AthleteDisplay(String name, String attempt) {
    }

    record ExpectedDisplay(String displayName, String attempt, long sequence, String weight) {
        String display() {
            return displayName + " | " + attempt + " | " + weight + " seq=" + sequence;
        }
    }

    record VerificationResult(boolean matched, long lastSequence, String fop) {
    }

    record MqttEvent(String topic, String payload, long sequence, long receivedAtMillis) {
    }

    static class ExpectationState {
        private boolean athleteConfirmed;
        private boolean gridConfirmed;
        private int snapshotReads;
        private SnapshotRead lastSnapshotRead;
        private final long startedAtMillis;

        ExpectationState() {
            this.startedAtMillis = System.currentTimeMillis();
        }

        boolean athleteConfirmed() {
            return this.athleteConfirmed;
        }

        void confirmAthlete() {
            this.athleteConfirmed = true;
        }

        boolean gridConfirmed() {
            return this.gridConfirmed;
        }

        void confirmGrid() {
            this.gridConfirmed = true;
        }

        long elapsedMillis() {
            return Math.max(0, System.currentTimeMillis() - this.startedAtMillis);
        }

        void recordSnapshotRead(SnapshotRead snapshotRead) {
            this.snapshotReads++;
            this.lastSnapshotRead = snapshotRead;
        }

        int snapshotReads() {
            return this.snapshotReads;
        }

        String lastSnapshotReadSummary() {
            return this.lastSnapshotRead != null ? this.lastSnapshotRead.summary() : "no DOM poll attempted";
        }
    }

    record SupersedeResult(boolean applied, boolean done, long sequence, ExpectedDisplay expected,
            long deadline, VerificationResult result) {
        static SupersedeResult notApplied() {
            return new SupersedeResult(false, false, 0, null, 0, null);
        }

        static SupersedeResult applied(long sequence, ExpectedDisplay expected, long deadline) {
            return new SupersedeResult(true, false, sequence, expected, deadline, null);
        }

        static SupersedeResult done(VerificationResult result) {
            return new SupersedeResult(false, true, result.lastSequence(), null, 0, result);
        }
    }

    private static class MqttEventCollector implements AutoCloseable {
        private final MqttClient client;
        private final AtomicLong sequence = new AtomicLong();
        private volatile List<MonitoredPlatform> platforms = List.of();
        final AtomicBoolean connected = new AtomicBoolean(true);
        final AtomicBoolean lostAfterStart = new AtomicBoolean(false);
        private final AtomicBoolean done = new AtomicBoolean(false);

        private MqttEventCollector(MqttClient client) {
            this.client = client;
        }

        static MqttEventCollector connect(String mqttUri) throws Exception {
            String clientId = "owlcms-playwright-" + System.currentTimeMillis();
            MqttClient client = new MqttClient(mqttUri, clientId, new MemoryPersistence());
            MqttEventCollector collector = new MqttEventCollector(client);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    if (collector.connected.getAndSet(false)) {
                        // was connected = we are past startup; treat as fatal for the watcher
                        logger./**/warn("MQTT connection lost ({}); stopping watcher", cause.getMessage());
                        collector.lostAfterStart.set(true);
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    collector.connected.set(true);
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    MqttEvent event = new MqttEvent(topic, normalize(payload), collector.sequence.incrementAndGet(),
                            System.currentTimeMillis());
                    if (collector.isPlaywrightDone(event)) {
                        collector.done.set(true);
                        return;
                    }
                    MonitoredPlatform platform = collector.platformForTopic(topic);
                    if (platform != null && collector.isPlaywrightInstructionForFop(event, platform.fop())) {
                        platform.acceptEvent(event);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // not needed for this runner
                }
            });
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            connectWithRetry(client, options, mqttUri);
            collector.connected.set(true);
            client.subscribe("owlcms/fop/#", 0);
            client.subscribe("owlcms/refbox/#", 0);
            return collector;
        }

        private static void connectWithRetry(MqttClient client, MqttConnectOptions options, String mqttUri)
                throws Exception {
            int attempt = 1;
            while (true) {
                try {
                    client.connect(options);
                    logger.info("MQTT connected to {}", mqttUri);
                    return;
                } catch (Exception e) {
                    long retryDelay = retryDelayForAttempt(attempt);
                    logger./**/warn("MQTT connection attempt {} to {} failed; retrying in {}s", attempt, mqttUri,
                            retryDelay / 1000, e);
                    sleep(retryDelay);
                    attempt++;
                }
            }
        }

        void monitorPlatforms(List<MonitoredPlatform> platforms) {
            this.platforms = List.copyOf(platforms);
        }

        void publish(String topic, String payload) throws Exception {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(0);
            message.setRetained(false);
            client.publish(topic, message);
        }

        long lastSequence() {
            return sequence.get();
        }

        boolean done() {
            return this.done.get();
        }

        private MonitoredPlatform platformForTopic(String topic) {
            String fop = fopFromTopic(topic);
            for (MonitoredPlatform platform : this.platforms) {
                if (platform.fop().equals(fop)) {
                    return platform;
                }
            }
            return null;
        }

        private boolean isPlaywrightInstructionForFop(MqttEvent event, String fop) {
            return event.topic().equals("owlcms/fop/playwright/currentAthlete/" + fop)
                    || event.topic().equals("owlcms/fop/playwright/pause/" + fop);
        }

        private boolean isPlaywrightDone(MqttEvent event) {
            return event.topic().equals(PLAYWRIGHT_DONE_TOPIC);
        }

        @Override
        public void close() throws Exception {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        }
    }

    static class CleanLog {
        void section(String title) {
            logger.info("");
            logger.info("== {} ==", title);
        }

        void value(String label, String value) {
            logger.info("{}: {}", label, value);
        }

        void info(String message) {
            logger.info(message);
        }

        void openPage(String fop, String role, String url) {
            logger.info("[{}] OPEN {} {}", fop, role, url);
        }

        void browserLaunch(String fop, Config config) {
            logger.info("[{}] LAUNCH {} inspect=browser remains open after stall until Enter/Ctrl-C", fop,
                browserLaunchDetails(config));
        }

        void browserInspection(String fop, Config config) {
            logger./**/warn("[{}] INSPECT {} url={} browser remains open while stalled; press Enter/Ctrl-C to finish",
                fop,
                browserLaunchDetails(config),
                buildUrl(config.baseUrl(), config.announcerPath(), fop));
        }

        void navigation(String fop, String role, Response response) {
            String status = response != null ? Integer.toString(response.status()) : "no-response";
            String url = response != null ? response.url() : "";
            logger.info("[{}] NAV  {} status={} {}", fop, role, status, url);
        }

        void testIds(String fop, int nameCount, int attemptCount, AthleteDisplay fallback) {
            boolean ok = nameCount > 0 && attemptCount > 0;
            if (ok) {
                status(fop, "test ids name=" + nameCount + " attempt=" + attemptCount, true);
            } else if (!fallback.name().isBlank() && !fallback.attempt().isBlank()) {
                status(fop, "top-bar fallback name=" + fallback.name() + " attempt=" + fallback.attempt(), true);
            } else {
                status(fop, "test ids name=" + nameCount + " attempt=" + attemptCount, false);
            }
        }

        void publish(String fop, String topic, String payload) {
            logger.info("[{}] PUB  {}{}", fop, topic, payload.isBlank() ? "" : " " + payload);
        }

        void status(String fop, String label, boolean ok) {
            if (ok) {
                logger.info("[{}] OK   {}", fop, label);
            } else {
                logger./**/warn("[{}] MISS {}", fop, label);
            }
        }

        void stop(String fop, String label) {
            logger./**/warn("[{}] STOP {}", fop, label);
        }

        void expecting(String fop, ExpectedDisplay expected) {
            logger.info("[{}] EXPECTING {}", fop, expected.display());
        }

        void superceded(String fop, ExpectedDisplay previous, String replacement) {
            logger.info("[{}] SUPERCEDED {} -> {}", fop, previous.display(), replacement);
        }

        void pause(String fop, String payload) {
            logger.info("[{}] PAUSE{}", fop, payload.isBlank() ? "" : " " + payload);
        }

        void confirmedAthlete(String fop, long elapsedMillis, Snapshot snapshot) {
            logger.info("[{}] CONFIRMED HEADER [{} ms] name={} attempt#={} weight={}", fop, elapsedMillis,
                    snapshot.athleteName(), digitsOnly(snapshot.attempt()), digitsOnly(snapshot.weight()));
        }

        void confirmedGrid(String fop, long elapsedMillis) {
            logger.info("[{}] CONFIRMED GRID [{} ms]", fop, elapsedMillis);
        }

    }
}
