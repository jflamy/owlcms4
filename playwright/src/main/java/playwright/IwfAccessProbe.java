package playwright;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;

public class IwfAccessProbe {
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
	        + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";
	private static final Pattern CHALLENGE_TEXT = Pattern.compile(
	        "just a moment|verify you are human|checking your browser|cloudflare|captcha",
	        Pattern.CASE_INSENSITIVE);
	private static final List<String> CHALLENGE_SELECTORS = List.of(
	        "#challenge-running",
	        "#challenge-form",
	        ".cf-challenge",
	        "iframe[src*='challenges.cloudflare.com']",
	        "iframe[src*='recaptcha']",
	        ".g-recaptcha");
	private static final List<Target> TARGETS = List.of(
	        new Target("results", "https://iwf.sport/results/results-by-events/",
	                List.of("select[name='event_year']", "a.card", "div.single__event__filter")),
	        new Target("records", "https://iwf.sport/results/world-records/",
	                List.of("#ranking_curprog", "[name='ranking_agegroup']", "#ranking_gender")));

	record Target(String name, String url, List<String> expectedSelectors) {
	}

	record Result(String name, int status, String title, boolean passed, List<String> expectedMatches,
	        List<String> challengeMatches, boolean challengeText, long elapsedMs) {
	}

	public static void main(String[] args) throws IOException {
		boolean headed = List.of(args).contains("--headed");
		boolean firefox = List.of(args).contains("--firefox");
		String browserName = firefox ? "Firefox" : "Chromium";
		Path output = Path.of("iwf-access-probe",
		        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
		Files.createDirectories(output);

		BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
		        .setHeadless(!headed);
		if (!firefox) {
			launchOptions.setArgs(List.of("--no-sandbox", "--disable-setuid-sandbox", "--disable-dev-shm-usage"));
		}

		boolean allPassed = true;
		try (Playwright playwright = Playwright.create();
		        Browser browser = (firefox ? playwright.firefox() : playwright.chromium()).launch(launchOptions)) {
			for (Target target : TARGETS) {
				Result result = probe(browser, target, output, headed, browserName, !firefox);
				allPassed &= result.passed();
				print(result);
			}
		}

		System.out.println("Artifacts: " + output.toAbsolutePath());
		if (!allPassed) {
			System.exit(2);
		}
	}

	private static Result probe(Browser browser, Target target, Path output, boolean headed, String browserName,
	        boolean spoofUserAgent) throws IOException {
		Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
		        .setViewportSize(1280, 800);
		if (spoofUserAgent) {
			contextOptions.setUserAgent(USER_AGENT);
		}
		long started = System.currentTimeMillis();

		try (BrowserContext context = browser.newContext(contextOptions)) {
			context.setDefaultTimeout(30_000);
			Page page = context.newPage();
			Response response = page.navigate(target.url(), new Page.NavigateOptions()
			        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
			        .setTimeout(30_000));
			if (headed) {
				page.bringToFront();
				System.out.printf("%n%s opened in %s. Complete verification, then press Enter here.%n",
				        target.name(), browserName);
				new BufferedReader(new InputStreamReader(System.in)).readLine();
			} else {
				page.waitForTimeout(3_000);
			}

			String title = page.title();
			String body = bodyText(page);
			List<String> expectedMatches = matchingSelectors(page, target.expectedSelectors());
			List<String> challengeMatches = matchingSelectors(page, CHALLENGE_SELECTORS);
			boolean challengeText = CHALLENGE_TEXT.matcher(title + "\n" + body).find();
			boolean passed = !expectedMatches.isEmpty() && challengeMatches.isEmpty() && !challengeText;

			page.screenshot(new Page.ScreenshotOptions()
			        .setPath(output.resolve(target.name() + ".png"))
			        .setFullPage(true));
			Files.writeString(output.resolve(target.name() + ".html"), page.content(), StandardCharsets.UTF_8);

			return new Result(target.name(), response == null ? 0 : response.status(), title, passed,
			        expectedMatches, challengeMatches, challengeText, System.currentTimeMillis() - started);
		}
	}

	private static String bodyText(Page page) {
		try {
			String body = page.locator("body").innerText();
			return body.substring(0, Math.min(body.length(), 2_000));
		} catch (RuntimeException e) {
			return "";
		}
	}

	private static List<String> matchingSelectors(Page page, List<String> selectors) {
		return selectors.stream().filter(selector -> {
			Locator locator = page.locator(selector);
			return locator.count() > 0;
		}).toList();
	}

	private static void print(Result result) {
		System.out.printf("%s: %s%n", result.name(), result.passed() ? "EXPECTED PAGE" : "BLOCKED OR UNEXPECTED");
		System.out.printf("  HTTP: %d; title: %s%n", result.status(), result.title());
		System.out.printf("  expected selectors: %s%n",
		        result.expectedMatches().isEmpty() ? "none" : String.join(", ", result.expectedMatches()));
		System.out.printf("  challenge: %s%n", !result.challengeMatches().isEmpty()
		        ? String.join(", ", result.challengeMatches())
		        : result.challengeText() ? "text marker" : "none");
		System.out.printf("  elapsed: %d ms%n", result.elapsedMs());
	}
}