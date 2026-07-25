package playwright;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;

/**
 * Route iterator that captures the same set of owlcms pages from two running servers and produces a
 * side-by-side screenshot comparison. Intended for validating styling refactorings (for example the
 * removal of the deprecated {@code @Theme} annotation and of {@code @CssImport(themeFor=...)}).
 * <p>
 * Both servers must be running against the same database and be on the same session/platform state.
 * <p>
 * Usage:
 *
 * <pre>
 * playwright/scripts/run-theme-compare.sh --old=http://localhost:8080 --new=http://localhost:8083
 * </pre>
 *
 * Options (all optional, defaults shown):
 * <ul>
 * <li>{@code --old=http://localhost:8080} reference server</li>
 * <li>{@code --new=http://localhost:8083} server under test</li>
 * <li>{@code --out=<module>/theme-compare/<timestamp>} output directory</li>
 * <li>{@code --tiers=1,2,3} which risk tiers to capture. Tier 4 holds routes whose difference has
 * been explained as harmless (data, version strings, sub-pixel antialiasing) and tier 5 those
 * measured pixel-identical; pass {@code --tiers=1,2,3,4,5} for a full sweep.</li>
 * <li>{@code --only=substring} keep only routes whose path or label contains the substring</li>
 * <li>{@code --fop=A} value for the {@code fop} query parameter on platform-aware routes</li>
 * <li>{@code --group=} value for the {@code group} query parameter (the legacy name for a session).
 * It is appended to every platform-aware route, plus the routes in {@link #GROUP_AWARE} that take
 * a session but no platform. On lifting pages this selects the session on the field of play, which
 * is what makes the captures deterministic.</li>
 * <li>{@code --pin=} PIN to enter if a login page appears</li>
 * <li>{@code --width=1600 --height=1000} viewport</li>
 * <li>{@code --settle=1200} extra milliseconds to wait after the Flow client goes idle</li>
 * <li>{@code --timeout=30000} per-operation timeout in milliseconds</li>
 * <li>{@code --tolerance=8} per-channel difference below which pixels are considered equal</li>
 * <li>{@code --prime=true} before capturing, open the announcer on each server and press the start
 * lifting button, so that scoreboards have a current athlete to display</li>
 * <li>{@code --startLabel=Start Lifting} label of the button the priming step clicks</li>
 * <li>{@code --channel=chrome} browser channel; pass an empty value to use the Chromium build
 * bundled with Playwright (which crashes on recent macOS)</li>
 * <li>{@code --headed} run with a visible browser</li>
 * </ul>
 * <p>
 * <b>Known intentional divergences.</b> Some differences are deliberate changes on the new server
 * and must not be read as styling regressions:
 * <ul>
 * <li><b>The filter-row magnifier is gone on the new server.</b> The crudui library adds a
 * decorative, non-clickable {@code VaadinIcon.SEARCH} in front of the filters; it is now removed in
 * {@code OwlcmsGridLayout}. On the old server it is present, and Vaadin 24 additionally let it
 * shrink to 5.4px wide against its requested 12.6px, so the two servers differ both by the icon
 * itself and by the horizontal offset it imposed on every control to its right. This affects every
 * page with a filter row: the athlete, records, teams, sessions, coaches, officials, platforms,
 * age-group, session-import and results grids, and {@code results/finalpackage}.</li>
 * </ul>
 * <p>
 * This is detected rather than assumed. Every comparison reports the bounding box of the changed
 * pixels, and a route is labelled {@code expected} only when it appears in {@link
 * #FILTER_ROW_ROUTES} <em>and</em> every changed pixel lies above {@link #FILTER_ROW_BAND_BOTTOM}.
 * A difference further down the page on one of those same routes is still counted as a problem.
 */
public class ThemeCompare {

	/**
	 * A page to capture.
	 *
	 * @param tier     1 = themeFor-related and still open, 2 = theme folder relocation and still
	 *                 open, 3 = other still-open questions, 4 = examined and the difference
	 *                 explained as harmless, 5 = measured pixel-identical
	 * @param path     route path, without leading slash
	 * @param fop      whether the {@code fop} query parameter should be appended
	 * @param fullPage whether to capture the whole scrollable page rather than just the viewport
	 */
	record Route(int tier, String path, boolean fop, boolean fullPage) {

		String label() {
			String base = this.path.isBlank() ? "home" : this.path;
			return base.replaceAll("[^A-Za-z0-9]+", "_");
		}

		/**
		 * Display pages show a parameter dialog the first time they are opened. Clicking outside it
		 * dismisses it, which is needed to see the board itself.
		 */
		boolean dismissDialog() {
			return this.path.startsWith("displays/");
		}

		String url(String baseUrl, String fopName, String groupName) {
			String url = baseUrl.replaceAll("/+$", "") + "/" + this.path;
			if (this.fop && fopName != null && !fopName.isBlank()) {
				url = url + (url.contains("?") ? "&" : "?") + "fop=" + fopName;
			}
			if (groupName != null && !groupName.isBlank() && (this.fop || GROUP_AWARE.contains(this.path))) {
				url = url + (url.contains("?") ? "&" : "?") + "group=" + groupName;
			}
			return url;
		}
	}

	/** Outcome of one route comparison. */
	record Comparison(Route route, String oldError, String newError, double diffPercent, String dimensionNote,
	        String diffArea, boolean expected) {

		boolean failed() {
			return this.oldError != null || this.newError != null;
		}
	}

	/**
	 * Screenshots are frozen so that repeated runs are comparable: animations, transitions and the
	 * text caret are the main sources of false positives. The dev-tools gizmo is hidden because it is
	 * only present when running from the IDE.
	 */
	private static final String FREEZE_CSS = """
			*, *::before, *::after {
			  animation: none !important;
			  transition: none !important;
			  caret-color: transparent !important;
			}
			vaadin-dev-tools, vaadin-dev-tools-window, .v-system-error { display: none !important; }
			/* "click or tap to enable sound" prompt: a TOP_STRETCH error notification wrapping a
			   soundenabler-element. It appears only until the browser grants autoplay, so it differs
			   between servers for reasons unrelated to styling. Only this one card is hidden, so that
			   genuine notification variants remain visible. */
			vaadin-notification-card:has(soundenabler-element) { display: none !important; }
			""";

	/**
	 * Hides the development tooling overlays. Their element names vary between Vaadin versions, so
	 * they are matched by substring rather than by a fixed list of tags. Only the dev-mode server
	 * has them, which would otherwise show up as a difference on every single page.
	 * <p>
	 * Also removes the sound-enabler notification by walking up from the element to its notification
	 * card, which does not depend on CSS {@code :has()} support.
	 */
	private static final String HIDE_OVERLAYS_JS = """
			() => {
			  document.querySelectorAll('*').forEach((el) => {
			    const name = el.localName || '';
			    if (name.includes('dev-tools') || name.includes('copilot') || name.includes('vite-plugin')) {
			      el.style.setProperty('display', 'none', 'important');
			    }
			  });
			  document.querySelectorAll('soundenabler-element').forEach((el) => {
			    const card = el.closest('vaadin-notification-card') || el.parentElement;
			    if (card) {
			      card.style.setProperty('display', 'none', 'important');
			    }
			  });
			}
			""";

	/**
	 * Waits until no Flow client has a pending server round-trip. Pages that are not Flow pages (or
	 * that have not bootstrapped yet) resolve immediately.
	 */
	private static final String FLOW_IDLE_JS = """
			() => {
			  const flow = window.Vaadin && window.Vaadin.Flow;
			  if (!flow || !flow.clients) { return true; }
			  return Object.keys(flow.clients).every((k) => {
			    const c = flow.clients[k];
			    return !c || typeof c.isActive !== 'function' || !c.isActive();
			  });
			}
			""";

	/**
	 * Routes that take a {@code group} query parameter but no {@code fop}. Platform-aware routes get
	 * the group appended automatically and do not need to be listed here.
	 */
	private static final Set<String> GROUP_AWARE = Set.of(
	        "results/results");

	/**
	 * Bottom of the band, in pixels, that holds the page header and the crud filter row. Used to
	 * decide whether a difference is confined to the filter row.
	 */
	private static final int FILTER_ROW_BAND_BOTTOM = 220;

	/**
	 * Routes that display a crud filter row, and therefore lose the decorative magnifier on the new
	 * server. A difference on one of these is reported as expected only when every changed pixel
	 * falls inside {@link #FILTER_ROW_BAND_BOTTOM}; anything lower down is still flagged.
	 */
	private static final Set<String> FILTER_ROW_ROUTES = Set.of(
	        "preparation/weighin",
	        "preparation/athletes",
	        "preparation/records",
	        "preparation/teams",
	        "preparation/sessions",
	        "preparation/coaches",
	        "preparation/officials",
	        "preparation/platforms",
	        "preparation/agegroup",
	        "records",
	        "results/results",
	        "results/finalpackage",
	        "results/teamresults",
	        "results/sessionImport",
	        "lifting/marshall",
	        "lifting/announcer",
	        "lifting/tc",
	        "lifting/timekeeper",
	        "lifting/jury",
	        "lifting/wodkeeper");

	/** Why a route on {@link #FILTER_ROW_ROUTES} is allowed to differ. */
	private static final String MAGNIFIER_NOTE = "expected: filter-row magnifier removed on the new server";

	private static final List<Route> ROUTES = List.of(
	        // ---- Tier 1: themeFor-related, still open --------------------------------------------
	        new Route(1, "preparation/weighin", true, true),
	        // 15%, believed to be athlete ordering, but not verified pixel by pixel
	        new Route(1, "lifting/marshall", true, false),

	        // ---- Tier 2: theme folder relocation / grids, still open ------------------------------
	        new Route(2, "results/finalpackage", false, true),
	        new Route(2, "preparation/athletes", false, true),
	        new Route(2, "results/results", false, true),
	        // examined: input fields sit 1px right in Vaadin 25, but 0.45% is above the "ignore"
	        // threshold, so it stays in the run until confirmed harmless on screen
	        new Route(2, "records", false, true),
	        new Route(2, "preparation/records", false, true),

	        // ---- Tier 3: still open ---------------------------------------------------------------
	        new Route(3, "", false, true),
	        new Route(3, "displays/publicStartList", true, false),
	        // 404s on both servers, so the capture compares two error pages; needs a routing fix
	        new Route(3, "displays/ncurrentathlete", true, false),
	        // ~0.77% cluster. Only resultsLeaders was inspected (row permutation from differing
	        // start numbers); the others share the value but have not been checked individually.
	        new Route(3, "displays/resultsLeaders", true, false),
	        new Route(3, "displays/publicScoreboard", true, false),
	        new Route(3, "displays/resultsSimple", true, false),
	        new Route(3, "displays/publicSimple", true, false),
	        new Route(3, "displays/resultsRankingOrder", true, false),
	        new Route(3, "displays/publicRankingOrder", true, false),
	        new Route(3, "displays/multiRanks", true, false),
	        new Route(3, "displays/publicMultiRanks", true, false),
	        new Route(3, "displays/rankings", true, false),
	        new Route(3, "displays/juryScoreboard", true, false),

	        // ---- Tier 4: inspected, explained, and below 0.2% ------------------------------------
	        // version string in the header differs between builds
	        new Route(4, "info", false, true),
	        // start number 6 vs 4: the databases assign them differently
	        new Route(4, "displays/attemptBoard", true, false),
	        new Route(4, "displays/athleteFacingAttempt", true, false),
	        // below 0.1%: no visible difference
	        new Route(4, "preparation", false, true),
	        new Route(4, "results", false, true),
	        new Route(4, "preparation/recordsConfig", false, true),
	        new Route(4, "preparation/teams", false, true),
	        new Route(4, "results/teamresults", false, true),
	        new Route(4, "lifting/announcer", true, false),
	        new Route(4, "lifting/tc", true, false),
	        new Route(4, "lifting/timekeeper", true, false),
	        new Route(4, "lifting/jury", true, false),
	        new Route(4, "displays/currentathlete", true, false),
	        new Route(4, "displays/resultsLiftingOrder", true, false),

	        // ---- Tier 5: measured pixel-identical -------------------------------------------------
	        new Route(5, "ref", true, false),
	        new Route(5, "jury", true, false),
	        new Route(5, "preparation/config", false, true),
	        new Route(5, "admin", false, true),
	        new Route(5, "lifting", false, true),
	        new Route(5, "displays", false, true),
	        new Route(5, "recordsPreparation", false, true),
	        new Route(5, "video=true", false, true),
	        new Route(5, "preparation/competition", false, true),
	        new Route(5, "preparation/agegroup", false, true),
	        new Route(5, "preparation/coaches", false, true),
	        new Route(5, "preparation/documents", false, true),
	        new Route(5, "preparation/officials", false, true),
	        new Route(5, "preparation/platforms", false, true),
	        new Route(5, "preparation/sessions", false, true),
	        new Route(5, "results/sessionImport", false, true),
	        new Route(5, "lifting/wodkeeper", true, false),
	        new Route(5, "jurykeypad", true, false),
	        new Route(5, "displays/monitor", true, false),
	        new Route(5, "displays/notifications", true, false),
	        new Route(5, "displays/athleteFacingDecision", true, false),
	        new Route(5, "displays/publicFacingDecision", true, false),
	        new Route(5, "displays/juryDecisions", true, false),
	        new Route(5, "displays/resultsMedals", true, false),
	        new Route(5, "displays/publicMedals", true, false),
	        new Route(5, "displays/wod", true, false),
	        new Route(5, "displays/topsinclair", true, false),
	        new Route(5, "displays/topteams", true, false),
	        new Route(5, "displays/topteamsinclair", true, false));

	public static void main(String[] args) throws Exception {
		Map<String, String> opts = parseArgs(args);

		String oldBase = opts.getOrDefault("old", "http://localhost:8080");
		String newBase = opts.getOrDefault("new", "http://localhost:8083");
		String fop = opts.getOrDefault("fop", "A");
		String group = opts.getOrDefault("group", "");
		String pin = opts.get("pin");
		int width = Integer.parseInt(opts.getOrDefault("width", "1600"));
		int height = Integer.parseInt(opts.getOrDefault("height", "1000"));
		int settleMs = Integer.parseInt(opts.getOrDefault("settle", "1200"));
		int timeoutMs = Integer.parseInt(opts.getOrDefault("timeout", "30000"));
		int tolerance = Integer.parseInt(opts.getOrDefault("tolerance", "8"));
		boolean headless = !opts.containsKey("headed");
		boolean prime = !"false".equalsIgnoreCase(opts.getOrDefault("prime", "true"));
		String startLabel = opts.getOrDefault("startLabel", "Start Lifting");
		String closeLabel = opts.getOrDefault("closeLabel", "Close");
		// The Chromium build bundled with Playwright 1.41 segfaults on recent macOS, so the locally
		// installed Chrome is used by default. Pass --channel= (empty) to force bundled Chromium.
		String channel = opts.getOrDefault("channel",
		        firstNonBlank(System.getenv("PLAYWRIGHT_BROWSER_CHANNEL"), "chrome"));

		Set<Integer> tiers = Arrays.stream(opts.getOrDefault("tiers", "1,2,3").split(","))
		        .map(String::trim).filter(s -> !s.isEmpty()).map(Integer::parseInt)
		        .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
		String only = opts.get("only");

		List<Route> routes = ROUTES.stream()
		        .filter(r -> tiers.contains(r.tier()))
		        .filter(r -> only == null || r.path().contains(only) || r.label().contains(only))
		        .toList();

		Path outDir = opts.containsKey("out")
		        ? Path.of(opts.get("out"))
		        : defaultOutDir();
		Files.createDirectories(outDir.resolve("shots"));

		System.out.println("old      : " + oldBase);
		System.out.println("new      : " + newBase);
		System.out.println("viewport : " + width + "x" + height);
		System.out.println("fop      : " + fop);
		System.out.println("group    : " + (group.isBlank() ? "(not set)" : group));
		System.out.println("browser  : " + (channel.isBlank() ? "bundled chromium" : channel));
		System.out.println("routes   : " + routes.size());
		System.out.println("output   : " + outDir.toAbsolutePath());
		System.out.println();

		List<Comparison> results = new ArrayList<>();

		try (Playwright playwright = Playwright.create()) {
			BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(headless);
			if (!channel.isBlank()) {
				launchOptions.setChannel(channel.trim());
			}
			try (Browser browser = playwright.chromium().launch(launchOptions)) {
				Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
				        .setViewportSize(width, height)
				        .setDeviceScaleFactor(1)
				        .setColorScheme(com.microsoft.playwright.options.ColorScheme.LIGHT)
				        .setReducedMotion(com.microsoft.playwright.options.ReducedMotion.REDUCE);

				try (BrowserContext oldCtx = browser.newContext(contextOptions);
				        BrowserContext newCtx = browser.newContext(contextOptions)) {
					oldCtx.setDefaultTimeout(timeoutMs);
					newCtx.setDefaultTimeout(timeoutMs);

					if (prime) {
						prime(oldCtx, oldBase, fop, group, startLabel, pin, settleMs, timeoutMs, "old");
						prime(newCtx, newBase, fop, group, startLabel, pin, settleMs, timeoutMs, "new");
						System.out.println();
					}

					int index = 0;
					for (Route route : routes) {
						index++;
						System.out.printf("[%2d/%2d] tier %d  %s%n", index, routes.size(), route.tier(),
						        route.path().isBlank() ? "(home)" : route.path());

						Path oldShot = outDir.resolve("shots").resolve(route.label() + "-old.png");
						Path newShot = outDir.resolve("shots").resolve(route.label() + "-new.png");

						String oldError = capture(oldCtx, route.url(oldBase, fop, group), oldShot, route.fullPage(),
						        pin, settleMs, timeoutMs, route.dismissDialog(), closeLabel);
						String newError = capture(newCtx, route.url(newBase, fop, group), newShot, route.fullPage(),
						        pin, settleMs, timeoutMs, route.dismissDialog(), closeLabel);

						double pct = -1;
						String dimensionNote = "";
						String diffArea = "";
						boolean expected = false;
						if (oldError == null && newError == null) {
							Path diffShot = outDir.resolve("shots").resolve(route.label() + "-diff.png");
							DiffResult diff = diff(oldShot, newShot, diffShot, tolerance);
							pct = diff.percent();
							dimensionNote = diff.note();
							diffArea = diff.area();
							expected = !diff.empty()
							        && FILTER_ROW_ROUTES.contains(route.path())
							        && diff.maxY() <= FILTER_ROW_BAND_BOTTOM;
						}
						results.add(new Comparison(route, oldError, newError, pct, dimensionNote, diffArea, expected));
						System.out.printf("         %s%n", describe(results.get(results.size() - 1)));
					}
				}
			}
		}

		writeCsv(outDir.resolve("report.csv"), results);
		writeHtml(outDir.resolve("report.html"), results, oldBase, newBase, width, height, tolerance);

		System.out.println();
		System.out.println("report: " + outDir.resolve("report.html").toAbsolutePath());
		long problems = results.stream().filter(c -> c.failed() || (c.diffPercent() > 0.1 && !c.expected())).count();
		long accounted = results.stream().filter(Comparison::expected).count();
		System.out.println(problems + " of " + results.size() + " routes need a look."
		        + (accounted > 0 ? "  (" + accounted + " differ only in the filter row, as expected)" : ""));
	}

	/**
	 * Navigates to a URL, logs in if a PIN page appears, waits for the Flow client to settle and
	 * writes a screenshot.
	 *
	 * @return null on success, or a message describing why the capture failed
	 */
	private static String capture(BrowserContext context, String url, Path target, boolean fullPage,
	        String pin, int settleMs, int timeoutMs, boolean dismissDialog, String closeLabel) {
		Page page = context.newPage();
		try {
			page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD));
			login(page, pin, timeoutMs);
			settle(page, settleMs, timeoutMs);
			if (dismissDialog) {
				dismissParameterDialog(page, closeLabel, settleMs, timeoutMs);
			}
			page.addStyleTag(new Page.AddStyleTagOptions().setContent(FREEZE_CSS));
			page.evaluate(HIDE_OVERLAYS_JS);
			page.waitForTimeout(150);
			page.screenshot(new Page.ScreenshotOptions().setPath(target).setFullPage(fullPage));
			return null;
		} catch (Exception e) {
			return url + " -> " + summarize(e);
		} finally {
			try {
				page.close();
			} catch (Exception ignored) {
				// page already gone
			}
		}
	}

	private static void login(Page page, String pin, int timeoutMs) {
		if (pin == null) {
			return;
		}
		Locator pinInput = page.locator("vaadin-password-field input").first();
		if (pinInput.count() == 0) {
			return;
		}
		pinInput.fill(pin);
		pinInput.press("Enter");
		page.waitForTimeout(500);
		settle(page, 0, timeoutMs);
	}

	/**
	 * Opens the announcer on one server and presses the start lifting button, so that the field of
	 * play has a current athlete and the scoreboards have something to show.
	 */
	private static void prime(BrowserContext context, String baseUrl, String fop, String group,
	        String startLabel, String pin, int settleMs, int timeoutMs, String which) {
		String url = baseUrl.replaceAll("/+$", "") + "/lifting/announcer?fop=" + fop
		        + (group == null || group.isBlank() ? "" : "&group=" + group);
		Page page = context.newPage();
		try {
			page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD));
			login(page, pin, timeoutMs);
			settle(page, settleMs, timeoutMs);
			Locator button = page.locator("vaadin-button:has-text(\"" + startLabel + "\")").first();
			if (button.count() > 0) {
				button.click();
				settle(page, settleMs, timeoutMs);
				System.out.println("prime " + which + ": clicked \"" + startLabel + "\"");
			} else {
				System.out.println("prime " + which + ": button \"" + startLabel + "\" not found");
			}
		} catch (Exception e) {
			System.out.println("prime " + which + ": failed - " + summarize(e));
		} finally {
			try {
				page.close();
			} catch (Exception ignored) {
				// page already gone
			}
		}
	}

	/**
	 * Display pages open a parameter dialog the first time they are shown. It is modal, so a click
	 * outside it does not close it: Escape is tried first, then its close button.
	 */
	private static void dismissParameterDialog(Page page, String closeLabel, int settleMs, int timeoutMs) {
		Locator overlay = page.locator("vaadin-dialog-overlay");
		if (overlay.count() == 0) {
			return;
		}
		page.keyboard().press("Escape");
		page.waitForTimeout(200);
		if (overlay.count() > 0) {
			Locator close = page.locator("vaadin-dialog-overlay vaadin-button:has-text(\"" + closeLabel + "\")")
			        .first();
			if (close.count() > 0) {
				close.click();
			}
		}
		settle(page, settleMs, timeoutMs);
	}

	private static void settle(Page page, int settleMs, int timeoutMs) {
		try {
			page.waitForLoadState(LoadState.NETWORKIDLE,
			        new Page.WaitForLoadStateOptions().setTimeout(Math.min(timeoutMs, 10000)));
		} catch (Exception e) {
			// push connections and periodic polling can keep the network busy; not fatal
		}
		try {
			page.waitForFunction(FLOW_IDLE_JS, null,
			        new Page.WaitForFunctionOptions().setTimeout(Math.min(timeoutMs, 15000)));
		} catch (Exception e) {
			// non-Flow page, or a client that stays active; the settle delay below still applies
		}
		if (settleMs > 0) {
			page.waitForTimeout(settleMs);
		}
	}

	// ---------------------------------------------------------------- image diff

	private record DiffResult(double percent, String note, int minX, int minY, int maxX, int maxY) {

		boolean empty() {
			return this.maxY < this.minY;
		}

		/** Extent of the changed pixels, or an empty string when nothing changed. */
		String area() {
			return empty() ? ""
			        : "rows " + this.minY + "-" + this.maxY + ", cols " + this.minX + "-" + this.maxX;
		}
	}

	/**
	 * Produces a diff image where unchanged pixels are dimmed to greyscale and changed pixels are
	 * painted red. Images of different sizes are compared over the intersection; the surplus area
	 * counts as different.
	 */
	private static DiffResult diff(Path oldShot, Path newShot, Path diffShot, int tolerance) throws IOException {
		BufferedImage a = ImageIO.read(oldShot.toFile());
		BufferedImage b = ImageIO.read(newShot.toFile());
		if (a == null || b == null) {
			return new DiffResult(-1, "unreadable screenshot", 0, 1, 0, 0);
		}

		int w = Math.max(a.getWidth(), b.getWidth());
		int h = Math.max(a.getHeight(), b.getHeight());
		String note = (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight())
		        ? a.getWidth() + "x" + a.getHeight() + " vs " + b.getWidth() + "x" + b.getHeight()
		        : "";

		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		long different = 0;
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				boolean inA = x < a.getWidth() && y < a.getHeight();
				boolean inB = x < b.getWidth() && y < b.getHeight();
				if (!inA || !inB) {
					different++;
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
					out.setRGB(x, y, 0x00FF00FF);
					continue;
				}
				int pa = a.getRGB(x, y);
				int pb = b.getRGB(x, y);
				int dr = Math.abs(((pa >> 16) & 0xFF) - ((pb >> 16) & 0xFF));
				int dg = Math.abs(((pa >> 8) & 0xFF) - ((pb >> 8) & 0xFF));
				int db = Math.abs((pa & 0xFF) - (pb & 0xFF));
				if (dr > tolerance || dg > tolerance || db > tolerance) {
					different++;
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
					out.setRGB(x, y, 0x00FF0000);
				} else {
					int grey = (int) (0.299 * ((pa >> 16) & 0xFF) + 0.587 * ((pa >> 8) & 0xFF) + 0.114 * (pa & 0xFF));
					grey = 200 + (grey * 55 / 255);
					out.setRGB(x, y, (grey << 16) | (grey << 8) | grey);
				}
			}
		}
		ImageIO.write(out, "png", diffShot.toFile());
		return new DiffResult(different * 100.0 / ((long) w * h), note, minX, minY, maxX, maxY);
	}

	// ---------------------------------------------------------------- reporting

	private static void writeCsv(Path target, List<Comparison> results) throws IOException {
		try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(target, StandardCharsets.UTF_8))) {
			w.println("tier,path,diffPercent,expected,diffArea,note,oldError,newError");
			for (Comparison c : results) {
				w.printf(Locale.ROOT, "%d,%s,%.4f,%s,%s,%s,%s,%s%n",
				        c.route().tier(),
				        csv(c.route().path()),
				        c.diffPercent(),
				        c.expected() ? "magnifier" : "",
				        csv(c.diffArea()),
				        csv(c.dimensionNote()),
				        csv(c.oldError()),
				        csv(c.newError()));
			}
		}
	}

	private static void writeHtml(Path target, List<Comparison> results, String oldBase, String newBase,
	        int width, int height, int tolerance) throws IOException {
		List<Comparison> sorted = new ArrayList<>(results);
		sorted.sort((x, y) -> {
			if (x.failed() != y.failed()) {
				return x.failed() ? -1 : 1;
			}
			return Double.compare(y.diffPercent(), x.diffPercent());
		});

		StringBuilder sb = new StringBuilder();
		sb.append("""
				<!DOCTYPE html>
				<html lang="en"><head><meta charset="utf-8"><title>owlcms theme comparison</title>
				<style>
				body { font-family: system-ui, sans-serif; margin: 2rem; background: #fafafa; }
				h1 { margin-bottom: 0.2rem; }
				.meta { color: #555; margin-bottom: 2rem; }
				.route { background: #fff; border: 1px solid #ddd; padding: 1rem; margin-bottom: 2rem; }
				.route h2 { margin: 0 0 0.3rem 0; font-size: 1.1rem; }
				.badge { display: inline-block; padding: 0.1rem 0.5rem; margin-right: 0.5rem;
				         font-size: 0.8rem; color: #fff; background: #666; }
				.ok { background: #2e7d32; } .warn { background: #ef6c00; } .bad { background: #c62828; }
				.info { background: #1565c0; }
				.expected { color: #1565c0; margin-bottom: 0.5rem; }
				.shots { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 0.5rem; }
				.shots figure { margin: 0; }
				.shots figcaption { font-size: 0.8rem; color: #555; }
				.shots img { width: 100%; border: 1px solid #ccc; background: #fff; }
				.error { color: #c62828; font-family: monospace; font-size: 0.85rem; }
				</style></head><body>
				""");
		sb.append("<h1>owlcms theme comparison</h1>\n");
		sb.append("<div class=\"meta\">old <code>").append(escape(oldBase)).append("</code> &middot; new <code>")
		        .append(escape(newBase)).append("</code> &middot; viewport ").append(width).append("&times;")
		        .append(height).append(" &middot; tolerance ").append(tolerance)
		        .append(" &middot; generated ").append(LocalDateTime.now()).append("</div>\n");

		for (Comparison c : sorted) {
			String label = c.route().label();
			String cls = c.failed() ? "bad" : (c.expected() ? "info" : (c.diffPercent() > 0.1 ? "warn" : "ok"));
			String pct = c.failed() ? "not compared" : String.format(Locale.ROOT, "%.3f%% different", c.diffPercent());

			sb.append("<div class=\"route\">\n");
			sb.append("<h2><span class=\"badge ").append(cls).append("\">tier ").append(c.route().tier())
			        .append("</span>/").append(escape(c.route().path())).append("</h2>\n");
			sb.append("<div class=\"meta\">").append(pct);
			if (!c.diffArea().isBlank()) {
				sb.append(" &middot; ").append(escape(c.diffArea()));
			}
			if (!c.dimensionNote().isBlank()) {
				sb.append(" &middot; size ").append(escape(c.dimensionNote()));
			}
			sb.append("</div>\n");
			if (c.expected()) {
				sb.append("<div class=\"expected\">").append(escape(MAGNIFIER_NOTE))
				        .append(" &mdash; every changed pixel is inside the filter row (y &le; ")
				        .append(FILTER_ROW_BAND_BOTTOM).append(")</div>\n");
			}
			if (c.oldError() != null) {
				sb.append("<div class=\"error\">old: ").append(escape(c.oldError())).append("</div>\n");
			}
			if (c.newError() != null) {
				sb.append("<div class=\"error\">new: ").append(escape(c.newError())).append("</div>\n");
			}
			sb.append("<div class=\"shots\">\n");
			sb.append(figure("shots/" + label + "-old.png", "old"));
			sb.append(figure("shots/" + label + "-new.png", "new"));
			if (!c.failed()) {
				sb.append(figure("shots/" + label + "-diff.png", "diff (red = changed)"));
			}
			sb.append("</div>\n</div>\n");
		}
		sb.append("</body></html>\n");
		Files.writeString(target, sb.toString(), StandardCharsets.UTF_8);
	}

	private static String figure(String src, String caption) {
		return "<figure><a href=\"" + src + "\"><img src=\"" + src + "\" loading=\"lazy\" alt=\"" + caption
		        + "\"></a><figcaption>" + caption + "</figcaption></figure>\n";
	}

	// ---------------------------------------------------------------- helpers

	private static Path defaultOutDir() {
		String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
		return Path.of("playwright", "theme-compare", stamp);
	}

	private static Map<String, String> parseArgs(String[] args) {
		Map<String, String> opts = new LinkedHashMap<>();
		for (String arg : args) {
			if (!arg.startsWith("--")) {
				throw new IllegalArgumentException("unexpected argument: " + arg);
			}
			String body = arg.substring(2);
			int eq = body.indexOf('=');
			if (eq < 0) {
				opts.put(body, "");
			} else {
				opts.put(body.substring(0, eq), body.substring(eq + 1));
			}
		}
		return opts;
	}

	private static String describe(Comparison c) {
		if (c.oldError() != null) {
			return "OLD FAILED - " + c.oldError();
		}
		if (c.newError() != null) {
			return "NEW FAILED - " + c.newError();
		}
		String note = c.dimensionNote().isBlank() ? "" : "  (" + c.dimensionNote() + ")";
		String where = c.diffArea().isBlank() ? "" : "  [" + c.diffArea() + "]";
		String why = c.expected() ? "  " + MAGNIFIER_NOTE : "";
		return String.format(Locale.ROOT, "%.3f%% different%s%s%s", c.diffPercent(), note, where, why);
	}

	/**
	 * Playwright messages are multi-line and start with a useless {@code Error {} line, so the whole
	 * message is flattened and truncated instead of taking the first line.
	 */
	private static String summarize(Exception e) {
		String message = e.getMessage() == null ? "" : e.getMessage().replaceAll("\\s+", " ").trim();
		if (message.length() > 200) {
			message = message.substring(0, 200) + "...";
		}
		return e.getClass().getSimpleName() + ": " + message;
	}

	private static String firstNonBlank(String preferred, String fallback) {
		return (preferred == null || preferred.isBlank()) ? fallback : preferred;
	}

	private static String csv(String s) {
		if (s == null) {
			return "";
		}
		String escaped = s.replace("\"", "\"\"");
		return escaped.contains(",") ? "\"" + escaped + "\"" : escaped;
	}

	private static String escape(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
