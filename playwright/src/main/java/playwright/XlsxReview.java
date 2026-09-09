package playwright;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;

/** Generates live XLSX output for every jury, competition-results, and session-results template. */
public class XlsxReview {

	private static final Path TEMPLATE_ROOT = Path.of("owlcms/src/main/resources/templates");
	private static final Pattern INCLUDE_RECORDS = Pattern.compile(
	        ".*(Results\\.IncludeRecords|Include Records|Incluir.*r.cord|Inclure.*record).*",
	        Pattern.CASE_INSENSITIVE);

	record Flow(String name, String path, String templateDirectory, String outputDirectory,
	        List<String> buttonLabels, boolean includeRecords, boolean lazyDownload) {
	}

	public static void main(String[] args) throws Exception {
		Map<String, String> options = parseArgs(args);
		String base = stripTrailingSlash(options.getOrDefault("base", "http://localhost"));
		String group = options.getOrDefault("group", "F45/49");
		String competitionPath = options.getOrDefault("competitionPath",
		        "results/finalpackage?ad=P15&gender=F&agp=P15");
		Path downloads = Path.of(options.getOrDefault("downloads",
		        Path.of(System.getProperty("user.home"), "Downloads").toString()));
		String channel = options.getOrDefault("channel", "chrome");
		boolean headed = options.containsKey("headed");

		List<Flow> flows = List.of(
		        new Flow("jury",
		                "preparation/weighin?group=" + encode(encode(group)),
		                "jury", "owlcms-jury",
		                List.of("Jury", "Jurado"), false, true),
		        new Flow("competition results", competitionPath,
		                "competitionResults", "owlcms-competition-results",
		                List.of("Eligibility Category Results", "Resultados por categorías de elegibilidad"), true, false),
		        new Flow("session results",
		                "results/results?group=" + encode(group),
		                "protocol", "owlcms-session-results",
		                List.of("Registration Category Results", "Resultados por categorías de registro"), false, false));

		BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(!headed);
		if (!channel.isBlank()) {
			launchOptions.setChannel(channel);
		}

		try (Playwright playwright = Playwright.create();
		        Browser browser = playwright.chromium().launch(launchOptions);
		        BrowserContext context = browser.newContext()) {
			context.setDefaultTimeout(30_000);
			Page page = context.newPage();
			for (Flow flow : flows) {
				runFlow(context, page, base, downloads, flow);
			}
		}
	}

	private static void runFlow(BrowserContext context, Page page, String base, Path downloads, Flow flow)
	        throws IOException {
		Path output = downloads.resolve(flow.outputDirectory());
		Files.createDirectories(output);

		page.navigate(base + "/" + flow.path());
		page.waitForLoadState(LoadState.DOMCONTENTLOADED);
		page.locator("body").waitFor();
		if (flow.includeRecords()) {
			enableIncludeRecords(page);
		}
		List<String> templates = templateNames(flow.templateDirectory());
		System.out.printf("%n%s: %d template(s) -> %s%n", flow.name(), templates.size(), output);

		for (String template : templates) {
			page.keyboard().press("Escape");
			openDialog(page, flow.buttonLabels());
			String previousHref = flow.lazyDownload() ? null : downloadHref(page);
			if (!selectTemplate(page, template, previousHref)) {
				System.out.printf("  %-45s skipped (not offered by live dialog)%n", template);
				continue;
			}
			byte[] bytes;
			if (flow.lazyDownload()) {
				Download download = page.waitForDownload(() -> page.locator(
				        "vaadin-button:has-text(\"Download\"):visible, vaadin-button:has-text(\"Descargar\"):visible")
				        .last().click());
				bytes = Files.readAllBytes(download.path());
			} else {
				String href = downloadHref(page);
				APIResponse response = context.request().get(href);
				try {
					if (response.status() != 200) {
						throw new IllegalStateException(template + " returned HTTP " + response.status());
					}
					bytes = response.body();
				} finally {
					response.dispose();
				}
			}
			validateXlsx(template, bytes);
			Files.write(output.resolve(template), bytes);
			System.out.printf("  %-45s %8d bytes%n", template, bytes.length);
		}
	}

	private static void enableIncludeRecords(Page page) {
		Locator checkbox = page.getByRole(AriaRole.CHECKBOX,
		        new Page.GetByRoleOptions().setName(INCLUDE_RECORDS)).first();
		checkbox.waitFor();
		if (!checkbox.isChecked()) {
			checkbox.check();
		}
	}

	private static void openDialog(Page page, List<String> buttonLabels) {
		String selector = buttonLabels.stream()
		        .map(label -> "vaadin-button:has-text(\"" + label + "\")")
		        .collect(java.util.stream.Collectors.joining(", "));
		Locator button = page.locator(selector).first();
		try {
			button.waitFor(new Locator.WaitForOptions().setTimeout(10_000));
		} catch (RuntimeException e) {
			String body = page.locator("body").innerText().replaceAll("\\s+", " ").trim();
			throw new IllegalStateException("No report button " + buttonLabels + " at " + page.url()
			        + " (title: " + page.title() + ", body: " + body.substring(0, Math.min(body.length(), 500)) + ")", e);
		}
		button.click();
		page.locator("vaadin-dialog-overlay[opened]:visible").last().waitFor();
	}

	private static boolean selectTemplate(Page page, String template, String previousHref) {
		Locator combo = page.locator("vaadin-combo-box:visible").last();
		String currentTemplate = combo.evaluate("element => element.value").toString();
		if (template.equals(currentTemplate)) {
			return true;
		}
		combo.click();
		Locator options = page.getByRole(AriaRole.OPTION);
		options.first().waitFor();
		Locator option = page.getByRole(AriaRole.OPTION,
		        new Page.GetByRoleOptions().setName(template).setExact(true));
		try {
			option.click(new Locator.ClickOptions().setTimeout(2_000));
			if (previousHref != null) {
				page.waitForFunction("previous => {"
				        + "const anchors = document.querySelectorAll(\"a[download][href*='VAADIN/dynamic/resource/']\");"
				        + "const visible = [...anchors].filter(anchor => anchor.getClientRects().length > 0);"
				        + "const anchor = visible[visible.length - 1];"
				        + "return anchor && anchor.href !== previous;"
				        + "}", previousHref);
			}
			return true;
		} catch (TimeoutError e) {
			page.keyboard().press("Escape");
			return false;
		}
	}

	private static String downloadHref(Page page) {
		Locator anchor = page.locator("a[download][href*='VAADIN/dynamic/resource/']:visible").last();
		anchor.waitFor();
		String href = anchor.evaluate("element => element.href").toString();
		if (href == null || href.isBlank()) {
			throw new IllegalStateException("The report dialog has no download resource URL");
		}
		return href;
	}

	private static List<String> templateNames(String directory) throws IOException {
		Path path = TEMPLATE_ROOT.resolve(directory);
		try (var files = Files.list(path)) {
			return files.filter(Files::isRegularFile)
			        .map(file -> file.getFileName().toString())
			        .filter(name -> name.endsWith(".xlsx"))
			        .sorted()
			        .toList();
		}
	}

	private static void validateXlsx(String name, byte[] bytes) throws IOException {
		boolean contentTypes = false;
		boolean workbook = false;
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
			for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
				contentTypes |= "[Content_Types].xml".equals(entry.getName());
				workbook |= "xl/workbook.xml".equals(entry.getName());
			}
		}
		if (!contentTypes || !workbook) {
			throw new IllegalStateException(name + " is not a valid XLSX workbook");
		}
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String stripTrailingSlash(String value) {
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}

	private static Map<String, String> parseArgs(String[] args) {
		Map<String, String> options = new LinkedHashMap<>();
		for (String arg : new ArrayList<>(Arrays.asList(args))) {
			if (!arg.startsWith("--")) {
				throw new IllegalArgumentException("Expected --name=value or --headed, got " + arg);
			}
			int equals = arg.indexOf('=');
			if (equals < 0) {
				options.put(arg.substring(2), "true");
			} else {
				options.put(arg.substring(2, equals), arg.substring(equals + 1));
			}
		}
		return options;
	}
}