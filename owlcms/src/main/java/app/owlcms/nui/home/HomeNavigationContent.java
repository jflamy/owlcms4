/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.home;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.http.conn.util.InetAddressUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.github.appreciated.css.grid.GridLayoutComponent.AutoFlow;
import com.github.appreciated.css.grid.GridLayoutComponent.Overflow;
import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.css.grid.sizes.MinMax;
import com.github.appreciated.css.grid.sizes.Repeat;
import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.displays.DisplayNavigationContent;
import app.owlcms.nui.displays.VideoNavigationContent;
import app.owlcms.nui.lifting.LiftingNavigationContent;
import app.owlcms.nui.preparation.PreparationNavigationContent;
import app.owlcms.nui.results.ResultsNavigationContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.utils.IPInterfaceUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class HomeNavigationContent.
 */
/**
 * @author owlcms
 *
 */
@SuppressWarnings("serial")
@Route(value = "", layout = OwlcmsLayout.class)
public class HomeNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

	private static final String USAGE_STR = "usageStr";
	final private static Logger logger = (Logger) LoggerFactory.getLogger(HomeNavigationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}

	/**
	 * Navigation crudGrid.
	 *
	 * @param items the items
	 * @return the flexible crudGrid layout
	 */
	public static FlexibleGridLayout navigationGrid(Component... items) {
		FlexibleGridLayout layout = new FlexibleGridLayout();
		layout.withColumns(Repeat.RepeatMode.AUTO_FILL, new MinMax(new Length("300px"), new Flex(1)))
		        .withAutoRows(new Length("1fr")).withItems(items)
		        .withOverflow(Overflow.AUTO).withAutoFlow(AutoFlow.ROW).withMargin(false).withPadding(true)
		        .withSpacing(false);
		layout.getContent().setGap(new Length("0.5em"), new Length("1.0em"));
		layout.setSizeUndefined();
		layout.setWidth("80%");
		layout.setBoxSizing(BoxSizing.BORDER_BOX);
		layout.setPadding(true);
		layout.getElement().getStyle().set("padding-right", "1em");
		return layout;
	}

	String INFO = Translator.translate("About");
	String PREPARE_COMPETITION = Translator.translate("PrepareCompetition");
	String RESULT_DOCUMENTS = Translator.translate("Results");
	String RUN_LIFTING_GROUP = Translator.translate("RunLiftingGroup");
	String VIDEO_STREAMING = Translator.translate("VideoStreaming");
	String START_DISPLAYS = Translator.translate("StartDisplays");
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	String currentVersionString = "";
	private Div versionPlaceholder;
	private Div motdPlaceholder;
	private Div controlPanelVersionPlaceholder;
	private String remoteAddr;
	private String motdFileName;
	private String launcherVersion;

	private record VersionCheckResult(String referenceVersion, int comparison) {
	}
	
	/**
	 * Instantiates a new main navigation content.
	 */
	public HomeNavigationContent() {
		OwlcmsFactory.waitDBInitialized();
		VerticalLayout intro = buildIntro();
		intro.setSpacing(false);

		Button prepare = new Button(this.PREPARE_COMPETITION,
		        buttonClickEvent -> UI.getCurrent().navigate(PreparationNavigationContent.class));
		Button displays = new Button(this.START_DISPLAYS,
		        buttonClickEvent -> UI.getCurrent().navigate(DisplayNavigationContent.class));
		Button video = new Button(this.VIDEO_STREAMING,
		        buttonClickEvent -> UI.getCurrent().navigate(VideoNavigationContent.class));
		Button lifting = new Button(this.RUN_LIFTING_GROUP,
		        buttonClickEvent -> UI.getCurrent().navigate(LiftingNavigationContent.class));
		Button documents = new Button(this.RESULT_DOCUMENTS,
		        buttonClickEvent -> UI.getCurrent().navigate(ResultsNavigationContent.class));

		FlexibleGridLayout grid = HomeNavigationContent.navigationGrid(prepare, lifting, displays, video, documents);
		fillH(intro, this);
		fillH(grid, this);

		DebugUtils.gc();
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("OWLCMS_Top");
	}

	/**
	 * @see app.owlcms.nui.shared.BaseNavigationContent#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return Translator.translate("OWLCMS_Top");
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#isIgnoreFopFromURL()
	 */
	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	/**
	 * @see app.owlcms.nui.shared.BaseNavigationContent#createMenuBarFopField(java.lang.String, java.lang.String)
	 */
	@Override
	protected HorizontalLayout createMenuBarFopField(String label, String placeHolder) {
		return null;
	}

	private VerticalLayout buildIntro() {
		this.currentVersionString = OwlcmsFactory.getVersion();
		this.versionPlaceholder = new Div();
		if (OwlcmsSession.getAttribute(USAGE_STR) == null) {
			logUsage(UI.getCurrent().getLocale());
		}

		this.launcherVersion = System.getenv("OWLCMS_CONTROLPANEL");
		if (this.launcherVersion == null) {
			this.launcherVersion = System.getenv("OWLCMS_LAUNCHER");
		}
		VaadinRequest currentRequest = VaadinRequest.getCurrent();
		if (currentRequest != null) {
			this.remoteAddr = currentRequest.getRemoteAddr();
		}

		VerticalLayout intro = new VerticalLayout();
		intro.setSpacing(false);
		intro.setId("homeIntro");
		IPInterfaceUtils urlFinder = new IPInterfaceUtils();
		urlFinder.checkRequest();
		addP(intro, Translator.translate("SystemURL"));
		UnorderedList ul = new UnorderedList();
		ArrayList<String> recommended = urlFinder.getRecommended();
		for (String url : recommended) {
			ul.add(new ListItem(new Anchor(url, url)));
		}
		ArrayList<String> wired = urlFinder.getWired();
		for (String url : wired) {
			ul.add(new ListItem(new Anchor(url, url), new NativeLabel(Translator.translate("Wired"))));
		}
		ArrayList<String> wireless = urlFinder.getWireless();
		for (String url : wireless) {
			ul.add(new ListItem(new Anchor(url, url), new NativeLabel(Translator.translate("Wireless"))));
		}
		ArrayList<String> networking = urlFinder.getNetworking();
		for (String url : networking) {
			ul.add(new ListItem(new Anchor(url, url), new NativeLabel("")));
		}
		var addresses = new ArrayList<String>();
		addresses.addAll(recommended);
		addresses.addAll(wired);
		addresses.addAll(wireless);
		addresses.addAll(networking);
		if (addresses.isEmpty()) {
			for (String url : urlFinder.getLocalUrl()) {
				ul.add(new ListItem(new Anchor(url, url), new NativeLabel(Translator.translate("LocalComputer"))));
			}
		}
		intro.add(ul);
		intro.add(this.versionPlaceholder);

		var osName = System.getProperty("os.name");
		if (osName.startsWith("Windows") || osName.startsWith("windows")) {
			osName = "windows";
		} else if (osName.startsWith("Mac") || osName.startsWith("mac")) {
			osName = "macos";
		} else if (osName.startsWith("Linux") || osName.startsWith("linux")) {
			osName = "linux";
		}
		if (osName.equals("Linux") && !JPAService.isLocalDb()) {
			osName = "cloud";
		}

		this.motdFileName = osName + ".html";
		this.motdPlaceholder = new Div();
		intro.add(this.motdPlaceholder);
		if (this.launcherVersion != null) {
			this.controlPanelVersionPlaceholder = new Div();
			intro.add(this.controlPanelVersionPlaceholder);
		}

		this.versionPlaceholder.getStyle().set("margin-bottom", "1ex");
		Hr hr = new Hr();
		hr.getStyle().set("margin-bottom", "2ex");
		intro.add(hr);
		addP(intro,
		        Translator.translate("LeftMenuNavigate")
		                + Translator.translate("PrepareCompatition_description", this.PREPARE_COMPETITION)
		                + Translator.translate("RunLiftingGroup_description", this.RUN_LIFTING_GROUP)
		                + Translator.translate("StartDisplays_description", this.START_DISPLAYS)
		                + Translator.translate("VideoStreaming_description", this.VIDEO_STREAMING)
		                + Translator.translate("CompetitionDocuments_description", this.RESULT_DOCUMENTS)
		                + Translator.translate("SeparateLaptops"));
		intro.getStyle().set("margin-bottom", "-1em");
		return intro;
	}

	private static final String REPO_OWNER = "jflamy";
	private static final String REPO_NAME = "owlcms4";
	private static final String CONTROL_PANEL_VERSION = "controlPanelVersion.txt";
	private static final String GITHUB_RAW_URL = "https://raw.githubusercontent.com/" + REPO_OWNER + "/" + REPO_NAME + "/master/";
	private static LocalDateTime cpWarningEmitted = LocalDateTime.MIN;
	private static LocalDateTime motdEmitted = LocalDateTime.MIN;
	/** Shared: each HttpClient carries its own connection pool and threads, and is never closed. */
	private static final HttpClient githubClient = HttpClient.newBuilder()
	        .connectTimeout(Duration.ofSeconds(2))
	        .build();

	public static String checkControlPanelVersion(String curVer) {
		if (LocalDateTime.now().minusHours(1).isBefore(cpWarningEmitted)) {
			return null;
		}
		cpWarningEmitted = LocalDateTime.now();
		HttpRequest request = HttpRequest.newBuilder()
		        .uri(URI.create(GITHUB_RAW_URL + CONTROL_PANEL_VERSION))
		        .timeout(Duration.ofSeconds(2))
		        .build();

		try {
			HttpResponse<String> response = githubClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new IOException(request.uri() + " Unexpected code " + response.statusCode());
			}
			String string = response.body().strip();

			// compare the versions using semantic versioning conventions.
			ComparableVersion currentVersion = new ComparableVersion(curVer);
			ComparableVersion requiredVersion = new ComparableVersion(string);
			int comparison = currentVersion.compareTo(requiredVersion);
			if (comparison < 0) {
				logger.error("Control panel version is out of date. Current version: {}, required version: {}", curVer, string);
				return getMotd("controlPanel.html");
			} else {
				logger.info("Control panel version is up to date. Current version: {}, required version: {}", curVer, string);
				return null;
			}
		} catch (Exception e) {
			logger.error("Error fetching control panel version: {} {}", e.getMessage(), request.uri());
			return null;
		}
	}

	static boolean localFileTesting = false;
	public static String getMotd(String fileName) {
		// testing
		if (localFileTesting) {
			try {
				Path parentDir = Paths.get("").toAbsolutePath().getParent();
				Path filePath = parentDir.resolve(fileName);
				return new String(java.nio.file.Files.readAllBytes(filePath));
			} catch (IOException e) {
				return null;
			}
		}

		if (LocalDateTime.now().minusHours(1).isBefore(motdEmitted)) {
			return null;
		}
		motdEmitted = LocalDateTime.now();
		HttpRequest request = HttpRequest.newBuilder()
		        .uri(URI.create(GITHUB_RAW_URL + fileName))
		        .timeout(Duration.ofSeconds(2))
		        .build();

		try {
			HttpResponse<String> response = githubClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new IOException("Unexpected code " + response.statusCode());
			}
			return response.body();
		} catch (Exception e) {
			logger.debug("Error fetching motd: {} {}", e.getMessage(), request.uri());
			return null;
		}
	}

	private VersionCheckResult checkVersion() {
		String apiUrl = this.currentVersionString.contains("-")
		        ? "https://api.github.com/repos/owlcms/prereleases/releases"
		        : "https://api.github.com/repos/owlcms/releases/releases";
		// the request timeout aborts the exchange; CompletableFuture.orTimeout would leave it running.
		HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
		        .header("Accept", "application/vnd.github.v3+json")
		        .timeout(Duration.ofSeconds(3))
		        .build();
		try {
			HttpResponse<String> response = githubClient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new IllegalStateException("Unexpected code " + response.statusCode());
			}
			JSONArray releases = new JSONArray(response.body());
			if (releases.length() == 0) {
				throw new IllegalStateException("no releases returned");
			}
			List<ComparableVersion> versions = new ArrayList<>();
			for (int i = 0; i < releases.length(); i++) {
				JSONObject release = releases.getJSONObject(i);
				versions.add(new ComparableVersion(release.getString("tag_name")));
			}
			versions.sort((v1, v2) -> v2.compareTo(v1)); // Sort in descending order
			String referenceVersion = versions.get(0).toString();
			ComparableVersion currentVersion = new ComparableVersion(this.currentVersionString);
			int versionComparison = currentVersion.compareTo(new ComparableVersion(referenceVersion));
			if (referenceVersion.contains("-alpha")) {
				versionComparison = 0;
			}
			return new VersionCheckResult(referenceVersion, versionComparison);
		} catch (Exception e) {
			logger.warn("version check failed for {} : {} {}", request.uri(), e.getClass().getSimpleName(),
			        e.getMessage());
			return null;
		}
	}

	private Html formatVersion(VersionCheckResult result, Locale locale, String remoteAddr) {
		String runningMsg = Translator.translate("CheckVersion.running", locale, this.currentVersionString);

		// Escape curly braces for MessageFormat - already translated strings shouldn't be re-interpreted
		String referenceVersionMsg = Translator.translate(
		        "CheckVersion.reference" + (result.referenceVersion().contains("-") ? "Prerelease" : "Stable"),
		        locale, result.referenceVersion()).replace("{", "'{'").replace("}", "'}'");

		String okVersionMsg = Translator.translate("CheckVersion.ok", locale);

		String behindVersionMsg = Translator.translate("CheckVersion.behind", locale);

		String owlcmsLauncher = this.launcherVersion;

		if (JPAService.isLocalDb() && remoteAddr != null) {
			InetAddress inetAddress;
			try {
				inetAddress = InetAddress.getByName(remoteAddr);
				if (inetAddress.isLoopbackAddress()) {
					if (owlcmsLauncher != null && !owlcmsLauncher.isBlank()) {
						String controlPanelUpdate = Translator.translate("CheckVersion.ControlPanelUpdate", locale);
						if (controlPanelUpdate != null && !controlPanelUpdate.isBlank()
						        && !"#ERROR!".contentEquals(controlPanelUpdate)) {
							behindVersionMsg = "<b>" + controlPanelUpdate + "</b>";
						}
					}
				}
			} catch (UnknownHostException e) {
				logger.error("Error checking remote address: {}", e.getMessage());
			}
		} else {
			String clickCloudUpdate = Translator.translate("CheckVersion.clickCloudUpdate", locale);
			behindVersionMsg = """
			                   <a href='https://owlcms-cloud.fly.dev/apps' style='text-decoration:underline'>%s</a>
			                   """
			        .formatted(clickCloudUpdate);
		}

		String aheadVersionMsg = Translator.translate("CheckVersion.ahead", locale);
		String warningUnicode = result.comparison() < 0 ? "\u26A0 " : "";
			
		String formatted = MessageFormat.format(
			        "<div>{6}{1} {0, choice, 0#{2} {3}|1#{4}|2#{2} {5}}</div>",
			        result.comparison() + 1, runningMsg, referenceVersionMsg, behindVersionMsg, okVersionMsg, aheadVersionMsg, warningUnicode);
			
		Html div = new Html(formatted);
		if (result.comparison() < 0) {
			div.getStyle().set("color", "red");
		}
		return div;
	}
	
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		UI ui = attachEvent.getUI();
		Locale locale = ui.getLocale();
		long start = System.currentTimeMillis();
		VaadinService.getCurrent().getExecutor().execute(() -> {
			VersionCheckResult versionResult = checkVersion();
			String motd = getMotd(this.motdFileName);
			String controlPanelVersion = this.launcherVersion == null ? null
			        : checkControlPanelVersion(this.launcherVersion);
			long elapsed = System.currentTimeMillis() - start;
			if (!ui.isAttached()) {
				logger.warn("home ui={} already detached after {}ms; version check results discarded", ui.getUIId(), elapsed);
				return;
			}
			try {
				ui.access(() -> {
					if (!isAttached()) {
						logger.warn("home ui={} alive but view detached after {}ms; results discarded", ui.getUIId(), elapsed);
						return;
					}
					if (versionResult != null) {
						this.versionPlaceholder.removeAll();
						this.versionPlaceholder.add(formatVersion(versionResult, locale, this.remoteAddr));
					}
					if (motd != null && !motd.isBlank()) {
						this.motdPlaceholder.removeAll();
						this.motdPlaceholder.add(new Hr(), new Html(motd));
					}
					if (controlPanelVersion != null && !controlPanelVersion.isBlank()) {
						this.controlPanelVersionPlaceholder.removeAll();
						this.controlPanelVersionPlaceholder.add(new Hr(), new Html(controlPanelVersion));
					}
				});
			} catch (UIDetachedException ignored) {
				logger.warn("home ui={} detached during access dispatch after {}ms", ui.getUIId(), elapsed);
			}
		});
	}

	private void logUsage(Locale locale) {
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
		VaadinRequest request = VaadinRequest.getCurrent();
		String forwarded = request.getHeader("X-FORWARDED-FOR");
		String ipAddress;
		if (forwarded == null) {
			ipAddress = request.getRemoteAddr();
		} else {
			// original address is first in list, by definition
			String[] path = forwarded.split(", ");
			ipAddress = path[0];
		}

		boolean local = false;
		InetAddress a = null;
		if (InetAddressUtils.isIPv4Address(ipAddress)) {
			try {
				a = InetAddress.getByName(ipAddress);
			} catch (UnknownHostException e) {
				// can't happen, will be a numerical address
			}
		} else {
			try {
				a = InetAddress.getByName(ipAddress);
			} catch (UnknownHostException e) {
				// can't happen, will be a numerical address
			}
		}

		// the origin will be localhost (ipv4 or ipv6) or an ip local address when running locally
		// the remote logger will use the x-forwarded-for header to obtain the public ip address
		if (a != null && (a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress())) {
			local = true;
		}

		// When running in the cloud,the remote logger gets the cloud server as the originating address, which is
		// useless.
		// But owlcms receives the browser address in x-forwarded-for so owlcms injects the browser in the logging data.

		// The default time zone has already been overridden if specified in the database or environment.
		String tzId = TimeZone.getDefault().getID().replaceAll("/", "_");

		// use numeric address to avoid possible issues with DNS caching
		String usageStr = "http://143.110.208.71/?"
		        + "&version=" + this.currentVersionString
		        + "&localdate=" + LocalDate.now().toString()
		        + "&localtime=" + LocalTime.now().toString()
		        + "&timezone=" + tzId
		        + "&locale=" + locale
		        + (local ? "" : "&origin=" + ipAddress)
		        + (JPAService.isLocalDb() ? "&local=true" : "&local=false");

		// fire and forget
		new Thread(() -> {
			// try 3 times, increasing timeout by 1 second.
			for (int i = 0; i < 3; i++) {
				try {
					HttpRequest usageRequest = HttpRequest
					        .newBuilder(URI.create(usageStr))
					        .timeout(Duration.ofMillis(2000 + (i * 1000)))
					        .build();
					client.send(usageRequest, BodyHandlers.ofString());
					OwlcmsSession.setAttribute(USAGE_STR, usageStr);
					logger.info("logged usage {}", OwlcmsSession.getAttribute(USAGE_STR));
					break;
				} catch (Throwable e) {
					logger.error("could not log usage - attempt {}: {}", i, e.getMessage());
				}
			}
		}).start();
	}
}