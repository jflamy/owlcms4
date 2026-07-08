package playwright;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;

/**
 * Reads DOM snapshots from an attempt-board page.
 * Reads inside the {@code attempt-board-template} shadow DOM.
 * Clock detection has been removed: abnormal clock values are now caught server-side
 * by the timer element's callback to OWLCMS.
 */
class AttemptBoardSnapshotReader implements UpdateCheck.SnapshotReader {
    private static final Logger logger = LoggerFactory.getLogger(AttemptBoardSnapshotReader.class);

    @Override
    public void waitForReady(Page page, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                Integer count = (Integer) page.evaluate(
                        "() => document.querySelectorAll('attempt-board-template').length");
                if (count != null && count > 0) {
                    return;
                }
            } catch (PlaywrightException ignored) {
                // not mounted yet
            }
            UpdateCheck.sleep(250);
        }
    }

    @Override
    public UpdateCheck.SnapshotRead read(MonitoredPage mp) {
        try {
            Page page = mp.page();
            Object raw = page.evaluate("() => {"
                    + "const board = document.querySelector('attempt-board-template');"
                    + "const root = board?.shadowRoot;"
                    + "if (!root) return null;"
                    + "const text = (...selectors) => {"
                    + "  for (const sel of selectors) {"
                    + "    const value = root.querySelector(sel)?.innerText?.trim() || '';"
                    + "    if (value) return value;"
                    + "  }"
                    + "  return '';"
                    + "};"
                    + "const lastName = text('[data-testid=\\'attempt-board-last-name\\']', '.lastName, .lastNameWithPicture');"
                    + "const firstName = text('[data-testid=\\'attempt-board-first-name\\']', '.firstName, .firstNameWithPicture, .firstNameWithFlags');"
                    + "const startNumber = text('[data-testid=\\'attempt-board-start-number\\']', '.startNumber');"
                    + "const attempt = text('[data-testid=\\'attempt-board-attempt\\']', '.attemptBoard > .attempt:not(.recordNotification)');"
                    + "const weight = text('[data-testid=\\'attempt-board-weight\\']', '.weight').replace(/[^0-9]/g, '');"
                    + "return {"
                    + "  athleteName: (lastName + ' ' + firstName + ' ' + startNumber).trim().replace(/\\s+/g,' '),"
                    + "  attempt: attempt,"
                    + "  weight: weight"
                    + "};"
                    + "}");

            String pageSummary = "url=" + safeUrl(page);
            if (raw == null) {
                return UpdateCheck.SnapshotRead.empty("attempt-board-template not found or no shadow root", pageSummary);
            }
            Map<?, ?> data = snapshotData(raw);
            if (!(raw instanceof Map<?, ?>)) {
                logger.debug("[{} {}] attempt board snapshot raw type={} parsedAsJson={} value={}",
                        mp.fop(), mp.role().name().toLowerCase(), raw.getClass().getName(), data != null,
                        rawSummary(raw));
            }
            if (data == null) {
                return UpdateCheck.SnapshotRead.empty(
                        "attempt board snapshot returned " + raw.getClass().getSimpleName() + ": " + rawSummary(raw),
                        pageSummary);
            }
            String athleteName = UpdateCheck.normalize(strOf(data, "athleteName"));
            String attempt = UpdateCheck.normalize(strOf(data, "attempt"));
            String weight = strOf(data, "weight");

            if (athleteName.isBlank() && attempt.isBlank() && weight.isBlank()) {
                return UpdateCheck.SnapshotRead.empty("no readable athlete, attempt, or weight on attempt board", pageSummary);
            }
            UpdateCheck.Snapshot snapshot = new UpdateCheck.Snapshot(
                    mp.fop(), mp.role(), athleteName, attempt, weight, "");
            if (attempt.isBlank()) {
                return UpdateCheck.SnapshotRead.of(snapshot, "empty attempt", pageSummary);
            }
            if (athleteName.isBlank()) {
                return UpdateCheck.SnapshotRead.of(snapshot, "empty athlete", pageSummary);
            }
            if (weight.isBlank()) {
                return UpdateCheck.SnapshotRead.of(snapshot, "empty weight", pageSummary);
            }
            return UpdateCheck.SnapshotRead.of(snapshot, "ok", pageSummary);
        } catch (PlaywrightException e) {
            return UpdateCheck.SnapshotRead.empty("PlaywrightException: " + e.getMessage(), "");
        }
    }

    private static String strOf(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }

    private static Map<?, ?> snapshotData(Object raw) {
        if (raw instanceof Map<?, ?> data) {
            return data;
        }
        if (raw instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                return Map.of(
                        "athleteName", UpdateCheck.jsonString(trimmed, "athleteName"),
                        "attempt", UpdateCheck.jsonString(trimmed, "attempt"),
                        "weight", UpdateCheck.jsonString(trimmed, "weight"));
            }
        }
        return null;
    }

    private static String rawSummary(Object raw) {
        String value = String.valueOf(raw).replaceAll("[\\p{Z}\\s]+", " ").trim();
        return value.length() <= 1000 ? value : value.substring(0, 1000) + "...";
    }

    private static String safeUrl(Page page) {
        try { return page.url(); } catch (PlaywrightException e) { return "<unavailable>"; }
    }
}
