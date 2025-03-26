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
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
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

import app.owlcms.Main;
import app.owlcms.apputils.DebugUtils;
import app.owlcms.apputils.LogbackConfigReloader;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.jetty.EmbeddedJetty;
import app.owlcms.monitors.MQTTMonitor;
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
import jakarta.servlet.http.HttpServletRequest;

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
	static private String usageStr;

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
		return layout;
	}

	String INFO = Translator.translate("About");
	String PREPARE_COMPETITION = Translator.translate("PrepareCompetition");
	String RESULT_DOCUMENTS = Translator.translate("Results");
	String RUN_LIFTING_GROUP = Translator.translate("RunLiftingGroup");
	String VIDEO_STREAMING = Translator.translate("VideoStreaming");
	String START_DISPLAYS = Translator.translate("StartDisplays");
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	String referenceVersionString;
	String currentVersionString = "";
	int comparison = 999;

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

		owlcmsManagementTwister();

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

	public void owlcmsManagementTwister() {
		if (Config.getCurrent().featureSwitch("manageOwlcms")) {
			ConfirmationDialog cdRestart = new ConfirmationDialog(
			        Translator.translate("ManageOwlcms.Restart"),
			        Translator.translate("ManageOwlcms.RestartWarning"),
			        null,
			        null);
			Button restart = new Button(Translator.translate("ManageOwlcms.Restart"),
			        buttonClickEvent -> {
				        cdRestart.setAction(() -> {
					        cdRestart.close();
					        UI.getCurrent().push();

					        MQTTMonitor.reset();
					        EmbeddedJetty.stop(true);
					        Main.stopMQTT();
					        LogbackConfigReloader.reloadLogbackConfiguration();
					        Main.doRun();
				        });
				        cdRestart.open();
			        });

			ConfirmationDialog cdStop = new ConfirmationDialog(
			        Translator.translate("ManageOwlcms.Stop"),
			        Translator.translate("ManageOwlcms.StopWarning"),
			        null,
			        null);
			Button stop = new Button(Translator.translate("ManageOwlcms.Stop"),
			        buttonClickEvent -> {
				        cdStop.setAction(() -> {
					        cdStop.close();
					        UI.getCurrent().push();

					        EmbeddedJetty.stop(false);
					        System.exit(0);
				        });
				        cdStop.open();
			        });
			FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(restart, stop);

			Html explanation = new Html(
			        "<div style='color: red; font-weight: bold'>" + "\uD83D\uDED1 " + Translator.translate("ManageOwlcms.Warning") + "</div>");
			explanation.getStyle().set("width", "40em");
			doHiddenGroup(Translator.translate("ManageOwlcms.Title"),
			        explanation, grid2, this, true);
		}
	}

	/**
	 * @see app.owlcms.nui.shared.BaseNavigationContent#createMenuBarFopField(java.lang.String, java.lang.String)
	 */
	@Override
	protected HorizontalLayout createMenuBarFopField(String label, String placeHolder) {
		return null;
	}

	private VerticalLayout buildIntro() {

		Html div = checkVersion();
		if (OwlcmsSession.getAttribute(USAGE_STR) == null) {
			logUsage();
		}
		// String launcherVersion = System.getenv("OWLCMS_LAUNCHER");
		// launcherVersion = "2.1.0";
		// if (launcherVersion != null) {
		// 	checkControlPanelVersion(launcherVersion);
		// }

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
		intro.add(div);

		div.getStyle().set("margin-bottom", "1ex");
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
    private static final String FILE_PATH = "controlPanelVersion.txt";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/contents/" + FILE_PATH;


	public static String checkControlPanelVersion(String curVer) {
		HttpClient client = HttpClient.newBuilder()
		        .connectTimeout(Duration.ofSeconds(2))
		        .build();
		HttpRequest request = HttpRequest.newBuilder()
		        .uri(URI.create(GITHUB_API_URL))
		        .timeout(Duration.ofSeconds(2))
		        .build();

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new IOException("Unexpected code " + response.statusCode());
			}

			String contentType = response.headers().firstValue("Content-Type").orElse("");
			if (!contentType.contains("application/json")) {
				throw new IOException("Unexpected content type: " + contentType);
			}

			String responseBody = response.body();
			JSONObject json = new JSONObject(responseBody);
			String content = json.getString("content");
			content = content.strip();
			String string = new String(Base64.getDecoder().decode(content));

			// compare the versions using semantic versioning conventions.
			ComparableVersion currentVersion = new ComparableVersion(curVer);
			ComparableVersion requiredVersion = new ComparableVersion(string);
			int comparison = currentVersion.compareTo(requiredVersion);
			if (comparison < 0) {
				logger.error("Control panel version is out of date. Current version: {}, required version: {}", curVer, string);
			} else {
				logger.info("Control panel version is up to date. Current version: {}, required version: {}", curVer, string);
			}

			return string;
		} catch (Exception e) {
			logger.error("Error fetching control panel version: {} {}", e.getMessage(), request.uri());
			return null;
		}
	}

	private Html checkVersion() {
		this.currentVersionString = OwlcmsFactory.getVersion();
		String suffix = this.currentVersionString.contains("-") ? "-prerelease" : "";

		String apiUrl = "https://api.github.com/repos/owlcms/owlcms4" + suffix + "/releases";
		HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
		        .header("Accept", "application/vnd.github.v3+json")
		        .build();
		HttpClient client = HttpClient.newHttpClient();
		CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, BodyHandlers.ofString());
		try {
			future.orTimeout(3000, TimeUnit.MILLISECONDS).whenComplete((response, exception) -> {
				if (exception != null) {
					return;
				}
				JSONArray releases = new JSONArray(response.body());
				if (releases.length() > 0) {
					List<ComparableVersion> versions = new ArrayList<>();
					for (int i = 0; i < releases.length(); i++) {
						JSONObject release = releases.getJSONObject(i);
						versions.add(new ComparableVersion(release.getString("tag_name")));
					}
					versions.sort((v1, v2) -> v2.compareTo(v1)); // Sort in descending order
					this.referenceVersionString = versions.get(0).toString();
					ComparableVersion currentVersion = new ComparableVersion(this.currentVersionString);
					ComparableVersion referenceVersion = new ComparableVersion(this.referenceVersionString);
					this.comparison = currentVersion.compareTo(referenceVersion);
				}
			}).join();
		} catch (Throwable e) {
			logger.error("version fetch timed out");
		}

		Html div = new Html("<div></div>");

		if (this.comparison < 999) {
			String runningMsg = Translator.translate("CheckVersion.running", this.currentVersionString);
			String referenceVersionMsg = Translator.translate(
			        "CheckVersion.reference" + (this.referenceVersionString.contains("-") ? "Prerelease" : "Stable"),
			        this.referenceVersionString);
			String okVersionMsg = Translator.translate("CheckVersion.ok");
			String behindVersionMsg = Translator.translate("CheckVersion.behind");

			String owlcmsLauncher = System.getenv("OWLCMS_LAUNCHER");

			if (JPAService.isLocalDb()) {
				HttpServletRequest httpRequest = (HttpServletRequest) VaadinService.getCurrentRequest();
				String remoteAddr = httpRequest.getRemoteAddr();
				InetAddress inetAddress;
				try {
					inetAddress = InetAddress.getByName(remoteAddr);
					if (inetAddress.isLoopbackAddress()) {
						if (owlcmsLauncher != null && !owlcmsLauncher.isBlank()) {
							behindVersionMsg = "<b>" + Translator.translate("CheckVersion.ControlPanelUpdate") + "</b>";
						}
					}
				} catch (UnknownHostException e) {
					logger.error("Error checking remote address: {}", e.getMessage());
				}
			} else {
				behindVersionMsg = """
				                   <a href='https://owlcms-cloud.fly.dev/apps' style='text-decoration:underline'>%s</a>
				                   """
				        .formatted(Translator.translate("CheckVersion.clickCloudUpdate"));
			}

			String aheadVersionMsg = Translator.translate("CheckVersion.ahead");

			if (this.referenceVersionString.contains("-alpha")) {
				// do not recommend update to an alpha version.
				this.comparison = 0;
			}
			String warningUnicode = this.comparison < 0 ? "\u26A0 " : "";
			String formatted = MessageFormat.format(
			        "<div>{6}{1} {0, choice, 0#{2} {3}|1#{4}|2#{2} {5}}</div>",
			        this.comparison + 1, runningMsg, referenceVersionMsg, behindVersionMsg, okVersionMsg, aheadVersionMsg, warningUnicode);
			div.setHtmlContent(formatted);
			if (this.comparison < 0) {
				div.getStyle().set("color", "red");
			}
		}
		return div;
	}

	private void logUsage() {
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
		usageStr = "http://143.110.208.71/?"
		        + "&version=" + this.currentVersionString
		        + "&localdate=" + LocalDate.now().toString()
		        + "&localtime=" + LocalTime.now().toString()
		        + "&timezone=" + tzId
		        + "&locale=" + Locale.getDefault()
		        + (local ? "" : "&origin=" + ipAddress)
		        + (JPAService.isLocalDb() ? "&local=true" : "&local=false");

		Properties attributes = OwlcmsSession.getCurrent().getAttributes();
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
					attributes.setProperty(USAGE_STR, usageStr);
					logger.info("logged usage {}", attributes.getProperty(USAGE_STR));
					break;
				} catch (Throwable e) {
					logger.error("could not log usage - attempt {}: {}", i, e.getMessage());
				}
			}
		}).start();
	}
}