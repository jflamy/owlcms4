package playwright;

import java.time.Duration;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;

/**
 * Reads DOM snapshots from an announcer page.
 * Contains the logic that was previously in {@code UpdateCheck.readSnapshotRead(MonitoredPlatform)}.
 */
class AnnouncerSnapshotReader implements UpdateCheck.SnapshotReader {

    @Override
    public void waitForReady(Page page, Duration timeout) {
        UpdateCheck.waitForTestIds(page, timeout);
    }

    @Override
    public UpdateCheck.SnapshotRead read(MonitoredPage mp) {
        try {
            Page page = mp.page();
            String athleteName = readTextBySelector(page, UpdateCheck.ATHLETE_NAME_SELECTOR);
            String attempt = readTextBySelector(page, UpdateCheck.ATHLETE_ATTEMPT_SELECTOR);
            String weight = readTextBySelector(page, UpdateCheck.ATHLETE_WEIGHT_SELECTOR);
            weight = weight.replaceAll("[^0-9]", "");
            String bodyText = readBodyText(page);
            String pageSummary = readPageSummary(page, bodyText);
            if (athleteName.isBlank() || attempt.isBlank()) {
                UpdateCheck.AthleteDisplay fallback = UpdateCheck.readTopBarDisplay(page);
                if (fallback.name().isBlank() || fallback.attempt().isBlank()) {
                    fallback = parseAthleteDisplay(bodyText, mp.fop());
                }
                athleteName = firstNonBlank(athleteName, fallback.name());
                attempt = firstNonBlank(attempt, fallback.attempt());
            }
            String gridFirstCell = readGridFirstCell(page);
            if (athleteName.isBlank() && attempt.isBlank() && weight.isBlank() && gridFirstCell.isBlank()) {
                return UpdateCheck.SnapshotRead.empty("no readable athlete, attempt, weight, or grid cell", pageSummary);
            }
            UpdateCheck.Snapshot snapshot = new UpdateCheck.Snapshot(
                    mp.fop(), mp.role(), athleteName, attempt, weight, gridFirstCell);
            if (attempt.isBlank()) {
                return UpdateCheck.SnapshotRead.of(snapshot, "empty attempt", pageSummary);
            }
            if (athleteName.isBlank()) {
                return UpdateCheck.SnapshotRead.of(snapshot, "empty athlete", pageSummary);
            }
            if (weight.isBlank()) {
                return UpdateCheck.SnapshotRead.of(snapshot, "empty weight", pageSummary);
            }
            if (gridFirstCell.isBlank()) {
                return UpdateCheck.SnapshotRead.of(snapshot, "empty grid cell", pageSummary);
            }
            return UpdateCheck.SnapshotRead.of(snapshot, "ok", pageSummary);
        } catch (PlaywrightException e) {
            return UpdateCheck.SnapshotRead.empty("PlaywrightException: " + e.getMessage(), "");
        }
    }

    private static String readGridFirstCell(Page page) {
        try {
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
        String url;
        try { url = page.url(); } catch (PlaywrightException e) { url = "<unavailable>"; }
        String title;
        try { title = UpdateCheck.normalize(page.title()); } catch (PlaywrightException e) { title = "<unavailable>"; }
        return "url=" + url
                + " title='" + title + "'"
                + " selectors[name=" + UpdateCheck.count(page, UpdateCheck.ATHLETE_NAME_SELECTOR)
                + " attempt=" + UpdateCheck.count(page, UpdateCheck.ATHLETE_ATTEMPT_SELECTOR)
                + " weight=" + UpdateCheck.count(page, UpdateCheck.ATHLETE_WEIGHT_SELECTOR) + "]"
                + " body='" + abbreviate(UpdateCheck.normalize(bodyText), 240) + "'";
    }

    private static UpdateCheck.AthleteDisplay parseAthleteDisplay(String bodyText, String platform) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\\b" + java.util.regex.Pattern.quote(platform)
                + "\\s+(.+?)\\s+((?:Snatch|Clean\\s+and\\s+Jerk|Clean\\s*&\\s*Jerk|C&J)\\s*#?\\d+\\s+\\d+\\s*kg)",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(UpdateCheck.normalize(bodyText));
        if (!matcher.find()) {
            return new UpdateCheck.AthleteDisplay("", "");
        }
        String athleteName = UpdateCheck.normalize(matcher.group(1)).replaceFirst("^[^\\p{Alnum}]+", "").trim();
        return new UpdateCheck.AthleteDisplay(athleteName, UpdateCheck.normalize(matcher.group(2)));
    }

    private static String readTextBySelector(Page page, String selector) {
        try {
            Object value = page.evaluate("selector => {"
                    + "const element = document.querySelector(selector);"
                    + "return element ? element.innerText : '';"
                    + "}", selector);
            return value != null ? UpdateCheck.normalize(value.toString()) : "";
        } catch (PlaywrightException e) {
            return "";
        }
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private static String abbreviate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
