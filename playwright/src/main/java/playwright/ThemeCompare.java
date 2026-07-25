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
 * <li>{@code --tiers=1,2,3} which risk tiers to capture</li>
 * <li>{@code --only=substring} keep only routes whose path or label contains the substring</li>
 * <li>{@code --fop=A} value for the {@code fop} query parameter on platform-aware routes</li>
 * <li>{@code --pin=} PIN to enter if a login page appears</li>
 * <li>{@code --width=1600 --height=1000} viewport</li>
 * <li>{@code --settle=1200} extra milliseconds to wait after the Flow client goes idle</li>
 * <li>{@code --timeout=30000} per-operation timeout in milliseconds</li>
 * <li>{@code --tolerance=8} per-channel difference below which pixels are considered equal</li>
 * <li>{@code --headed} run with a visible browser</li>
 * </ul>
 */
public class ThemeCompare {

	/**
	 * A page to capture.
	 *
	 * @param tier     1 = directly affected by the themeFor removal, 2 = affected by the theme folder
	 *                 relocation, 3 = general Lumo loading / density regression checks
	 * @param path     route path, without leading slash
	 * @param fop      whether the {@code fop} query parameter should be appended
	 * @param fullPage whether to capture the whole scrollable page rather than just the viewport
	 */
	record Route(int tier, String path, boolean fop, boolean fullPage) {

		String label() {
			String base = this.path.isBlank() ? "home" : this.path;
			return base.replaceAll("[^A-Za-z0-9]+", "_");
		}

		String url(String baseUrl, String fopName) {
			String url = baseUrl.replaceAll("/+$", "") + "/" + this.path;
			if (this.fop && fopName != null && !fopName.isBlank()) {
				url = url + (url.contains("?") ? "&" : "?") + "fop=" + fopName;
			}
			return url;
		}
	}

	/** Outcome of one route comparison. */
	record Comparison(Route route, String oldError, String newError, double diffPercent, String dimensionNote) {

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

	private static final List<Route> ROUTES = List.of(
	        // ---- Tier 1: pages that carry the styles previously injected with themeFor ----------
	        new Route(1, "lifting/announcer", true, false),
	        new Route(1, "lifting/marshall", true, false),
	        new Route(1, "lifting/tc", true, false),
	        new Route(1, "lifting/timekeeper", true, false),
	        new Route(1, "lifting/wodkeeper", true, false),
	        new Route(1, "lifting/jury", true, false),
	        new Route(1, "preparation/weighin", true, true),

	        // ---- Tier 2: pages that depend on the relocated theme folder stylesheet -------------
	        new Route(2, "preparation/athletes", false, true),
	        new Route(2, "preparation/sessions", false, true),
	        new Route(2, "preparation/agegroup", false, true),
	        new Route(2, "preparation/platforms", false, true),
	        new Route(2, "preparation/records", false, true),
	        new Route(2, "preparation/recordsConfig", false, true),
	        new Route(2, "preparation/teams", false, true),
	        new Route(2, "preparation/officials", false, true),
	        new Route(2, "preparation/coaches", false, true),
	        new Route(2, "preparation/documents", false, true),
	        new Route(2, "results/results", false, true),
	        new Route(2, "results/finalpackage", false, true),
	        new Route(2, "results/teamresults", false, true),
	        new Route(2, "results/sessionImport", false, true),
	        new Route(2, "jurykeypad", true, false),
	        new Route(2, "displays/monitor", true, false),
	        new Route(2, "displays/notifications", true, false),

	        // ---- Tier 3: Lumo loading, compact preset, dark variants ----------------------------
	        new Route(3, "", false, true),
	        new Route(3, "preparation", false, true),
	        new Route(3, "lifting", false, true),
	        new Route(3, "displays", false, true),
	        new Route(3, "results", false, true),
	        new Route(3, "records", false, true),
	        new Route(3, "recordsPreparation", false, true),
	        new Route(3, "info", false, true),
	        new Route(3, "admin", false, true),
	        new Route(3, "preparation/competition", false, true),
	        new Route(3, "preparation/config", false, true),
	        new Route(3, "video=true", false, true),
	        new Route(3, "displays/attemptBoard", true, false),
	        new Route(3, "displays/athleteFacingAttempt", true, false),
	        new Route(3, "displays/athleteFacingDecision", true, false),
	        new Route(3, "displays/publicFacingDecision", true, false),
	        new Route(3, "displays/resultsLeaders", true, false),
	        new Route(3, "displays/publicScoreboard", true, false),
	        new Route(3, "displays/resultsSimple", true, false),
	        new Route(3, "displays/publicSimple", true, false),
	        new Route(3, "displays/resultsLiftingOrder", true, false),
	        new Route(3, "displays/resultsRankingOrder", true, false),
	        new Route(3, "displays/publicRankingOrder", true, false),
	        new Route(3, "displays/multiRanks", true, false),
	        new Route(3, "displays/publicMultiRanks", true, false),
	        new Route(3, "displays/rankings", true, false),
	        new Route(3, "displays/resultsMedals", true, false),
	        new Route(3, "displays/publicMedals", true, false),
	        new Route(3, "displays/publicStartList", true, false),
	        new Route(3, "displays/currentathlete", true, false),
	        new Route(3, "displays/ncurrentathlete", true, false),
	        new Route(3, "displays/juryScoreboard", true, false),
	        new Route(3, "displays/juryDecisions", true, false),
	        new Route(3, "displays/wod", true, false),
	        new Route(3, "displays/topsinclair", true, false),
	        new Route(3, "displays/topteams", true, false),
	        new Route(3, "displays/topteamsinclair", true, false),
	        new Route(3, "ref", true, false),
	        new Route(3, "jury", true, false));

	public static void main(String[] args) throws Exception {
		Map<String, String> opts = parseArgs(args);

		String oldBase = opts.getOrDefault("old", "http://localhost:8080");
		String newBase = opts.getOrDefault("new", "http://localhost:8083");
		String fop = opts.getOrDefault("fop", "A");
		String pin = opts.get("pin");
		int width = Integer.parseInt(opts.getOrDefault("width", "1600"));
		int height = Integer.parseInt(opts.getOrDefault("height", "1000"));
		int settleMs = Integer.parseInt(opts.getOrDefault("settle", "1200"));
		int timeoutMs = Integer.parseInt(opts.getOrDefault("timeout", "30000"));
		int tolerance = Integer.parseInt(opts.getOrDefault("tolerance", "8"));
		boolean headless = !opts.containsKey("headed");

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
		System.out.println("routes   : " + routes.size());
		System.out.println("output   : " + outDir.toAbsolutePath());
		System.out.println();

		List<Comparison> results = new ArrayList<>();

		try (Playwright playwright = Playwright.create()) {
			BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(headless);
			String channel = System.getenv("PLAYWRIGHT_BROWSER_CHANNEL");
			if (channel != null && !channel.isBlank()) {
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

					int index = 0;
					for (Route route : routes) {
						index++;
						System.out.printf("[%2d/%2d] tier %d  %s%n", index, routes.size(), route.tier(),
						        route.path().isBlank() ? "(home)" : route.path());

						Path oldShot = outDir.resolve("shots").resolve(route.label() + "-old.png");
						Path newShot = outDir.resolve("shots").resolve(route.label() + "-new.png");

						String oldError = capture(oldCtx, route.url(oldBase, fop), oldShot, route.fullPage(),
						        pin, settleMs, timeoutMs);
						String newError = capture(newCtx, route.url(newBase, fop), newShot, route.fullPage(),
						        pin, settleMs, timeoutMs);

						double pct = -1;
						String dimensionNote = "";
						if (oldError == null && newError == null) {
							Path diffShot = outDir.resolve("shots").resolve(route.label() + "-diff.png");
							DiffResult diff = diff(oldShot, newShot, diffShot, tolerance);
							pct = diff.percent();
							dimensionNote = diff.note();
						}
						results.add(new Comparison(route, oldError, newError, pct, dimensionNote));
						System.out.printf("         %s%n", describe(results.get(results.size() - 1)));
					}
				}
			}
		}

		writeCsv(outDir.resolve("report.csv"), results);
		writeHtml(outDir.resolve("report.html"), results, oldBase, newBase, width, height, tolerance);

		System.out.println();
		System.out.println("report: " + outDir.resolve("report.html").toAbsolutePath());
		long problems = results.stream().filter(c -> c.failed() || c.diffPercent() > 0.1).count();
		System.out.println(problems + " of " + results.size() + " routes need a look.");
	}

	/**
	 * Navigates to a URL, logs in if a PIN page appears, waits for the Flow client to settle and
	 * writes a screenshot.
	 *
	 * @return null on success, or a message describing why the capture failed
	 */
	private static String capture(BrowserContext context, String url, Path target, boolean fullPage,
	        String pin, int settleMs, int timeoutMs) {
		Page page = context.newPage();
		try {
			page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD));
			login(page, pin, timeoutMs);
			settle(page, settleMs, timeoutMs);
			page.addStyleTag(new Page.AddStyleTagOptions().setContent(FREEZE_CSS));
			page.waitForTimeout(150);
			page.screenshot(new Page.ScreenshotOptions().setPath(target).setFullPage(fullPage));
			return null;
		} catch (Exception e) {
			return e.getClass().getSimpleName() + ": " + firstLine(e.getMessage());
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

	private record DiffResult(double percent, String note) {
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
			return new DiffResult(-1, "unreadable screenshot");
		}

		int w = Math.max(a.getWidth(), b.getWidth());
		int h = Math.max(a.getHeight(), b.getHeight());
		String note = (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight())
		        ? a.getWidth() + "x" + a.getHeight() + " vs " + b.getWidth() + "x" + b.getHeight()
		        : "";

		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		long different = 0;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				boolean inA = x < a.getWidth() && y < a.getHeight();
				boolean inB = x < b.getWidth() && y < b.getHeight();
				if (!inA || !inB) {
					different++;
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
					out.setRGB(x, y, 0x00FF0000);
				} else {
					int grey = (int) (0.299 * ((pa >> 16) & 0xFF) + 0.587 * ((pa >> 8) & 0xFF) + 0.114 * (pa & 0xFF));
					grey = 200 + (grey * 55 / 255);
					out.setRGB(x, y, (grey << 16) | (grey << 8) | grey);
				}
			}
		}
		ImageIO.write(out, "png", diffShot.toFile());
		return new DiffResult(different * 100.0 / ((long) w * h), note);
	}

	// ---------------------------------------------------------------- reporting

	private static void writeCsv(Path target, List<Comparison> results) throws IOException {
		try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(target, StandardCharsets.UTF_8))) {
			w.println("tier,path,diffPercent,note,oldError,newError");
			for (Comparison c : results) {
				w.printf(Locale.ROOT, "%d,%s,%.4f,%s,%s,%s%n",
				        c.route().tier(),
				        csv(c.route().path()),
				        c.diffPercent(),
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
			String cls = c.failed() ? "bad" : (c.diffPercent() > 0.1 ? "warn" : "ok");
			String pct = c.failed() ? "not compared" : String.format(Locale.ROOT, "%.3f%% different", c.diffPercent());

			sb.append("<div class=\"route\">\n");
			sb.append("<h2><span class=\"badge ").append(cls).append("\">tier ").append(c.route().tier())
			        .append("</span>/").append(escape(c.route().path())).append("</h2>\n");
			sb.append("<div class=\"meta\">").append(pct);
			if (!c.dimensionNote().isBlank()) {
				sb.append(" &middot; size ").append(escape(c.dimensionNote()));
			}
			sb.append("</div>\n");
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
		return String.format(Locale.ROOT, "%.3f%% different%s", c.diffPercent(), note);
	}

	private static String firstLine(String s) {
		if (s == null) {
			return "";
		}
		int nl = s.indexOf('\n');
		return nl < 0 ? s : s.substring(0, nl);
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
