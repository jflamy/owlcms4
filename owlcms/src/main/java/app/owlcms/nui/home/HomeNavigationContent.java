/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.home;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.http.conn.util.InetAddressUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.LoggerFactory;

import com.github.appreciated.css.grid.GridLayoutComponent.AutoFlow;
import com.github.appreciated.css.grid.GridLayoutComponent.Overflow;
import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.css.grid.sizes.MinMax;
import com.github.appreciated.css.grid.sizes.Repeat;
import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
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
import app.owlcms.utils.LoggerUtils;
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
	static private String usageStr;

	/**
	 * Instantiates a new main navigation content.
	 */
	public HomeNavigationContent() {
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
		return getTranslation("OWLCMS_Top");
	}

	/**
	 * @see app.owlcms.nui.shared.BaseNavigationContent#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation("OWLCMS_Top");
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

		Div div = checkVersion();
		if (OwlcmsSession.getAttribute(USAGE_STR) == null) {
			logUsage();
		}
		
		VerticalLayout intro = new VerticalLayout();
		intro.setSpacing(false);
		intro.setId("homeIntro");
		IPInterfaceUtils urlFinder = new IPInterfaceUtils();
		urlFinder.checkRequest();
		addP(intro, getTranslation("SystemURL"));
		UnorderedList ul = new UnorderedList();
		for (String url : urlFinder.getRecommended()) {
			ul.add(new ListItem(new Anchor(url, url)));
		}
		for (String url : urlFinder.getWired()) {
			ul.add(new ListItem(new Anchor(url, url), new NativeLabel(getTranslation("Wired"))));
		}
		for (String url : urlFinder.getWireless()) {
			ul.add(new ListItem(new Anchor(url, url), new NativeLabel(getTranslation("Wireless"))));
		}
		for (String url : urlFinder.getNetworking()) {
			ul.add(new ListItem(new Anchor(url, url), new NativeLabel("")));
		}
		for (String url : urlFinder.getLocalUrl()) {
			ul.add(new ListItem(new Anchor(url, url), new NativeLabel(getTranslation("LocalComputer"))));
		}
		intro.add(ul);
		intro.add(div);

		div.getStyle().set("margin-bottom", "1ex");
		Hr hr = new Hr();
		hr.getStyle().set("margin-bottom", "2ex");
		intro.add(hr);
		addP(intro,
		        getTranslation("LeftMenuNavigate")
		                + getTranslation("PrepareCompatition_description", this.PREPARE_COMPETITION)
		                + getTranslation("RunLiftingGroup_description", this.RUN_LIFTING_GROUP)
		                + getTranslation("StartDisplays_description", this.START_DISPLAYS)
		                + getTranslation("VideoStreaming_description", this.VIDEO_STREAMING)
		                + getTranslation("CompetitionDocuments_description", this.RESULT_DOCUMENTS)
		                + getTranslation("SeparateLaptops"));
		intro.getStyle().set("margin-bottom", "-1em");
		return intro;
	}

	private Div checkVersion() {
		// if (Config.getCurrent().featureSwitch("checkForUpdate"))
		{
			this.currentVersionString = OwlcmsFactory.getVersion();
			String suffix = this.currentVersionString.contains("-") ? "-prerelease" : "";

			String str = "https://raw.githubusercontent.com/owlcms/owlcms4" + suffix + "/master/version.txt";
			HttpRequest request1 = HttpRequest.newBuilder(URI.create(str)).build();
			HttpClient client1 = HttpClient.newHttpClient();
			CompletableFuture<HttpResponse<String>> future = client1.sendAsync(request1, BodyHandlers.ofString());
			try {
				future
				        .orTimeout(500, TimeUnit.MILLISECONDS)
				        .whenComplete((response, exception) -> {
					        if (exception != null) {
						        return;
					        }
					        ComparableVersion currentVersion = new ComparableVersion(this.currentVersionString);
					        this.referenceVersionString = response.body();
					        ComparableVersion referenceVersion = new ComparableVersion(this.referenceVersionString);
					        this.comparison = currentVersion.compareTo(referenceVersion);
				        })
				        .join();
			} catch (Throwable e) {
				logger.error("version fetch timed out");
			}
		}
		Div div = new Div();
		if (
		// (Config.getCurrent().featureSwitch("checkForUpdate")) &&
		this.comparison < 999) {
			String runningMsg = Translator.translate("CheckVersion.running", this.currentVersionString);
			String referenceVersionMsg = Translator.translate(
			        "CheckVersion.reference" + (this.referenceVersionString.contains("-") ? "Prerelease" : "Stable"),
			        this.referenceVersionString);
			String okVersionMsg = Translator.translate("CheckVersion.ok");
			String behindVersionMsg = Translator.translate("CheckVersion.behind");
			String aheadVersionMsg = Translator.translate("CheckVersion.ahead");

			String formatted = MessageFormat.format(
			        "{1} {0, choice, 0#{2} {3}|1#{4}|2#{2} {5}}",
			        this.comparison + 1, runningMsg, referenceVersionMsg, behindVersionMsg, okVersionMsg,
			        aheadVersionMsg);
			div.setText(formatted);
			if (this.comparison < 0) {
				div.getStyle().set("font-weight", "bold");
				div.getStyle().set("color", "red");
			}
		}
		return div;
	}

	private void logUsage() {
		HttpClient client = HttpClient.newHttpClient();
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

		usageStr = "https://usage.jflamy.dev?"
		        + "&version=" + this.currentVersionString
		        + "&localdate=" + LocalDate.now().toString()
		        + "&localtime=" + LocalTime.now().toString()
		        + "&timezone=" + tzId
		        + (local ? "" : "&origin=" + ipAddress)
		        + (JPAService.isLocalDb() ? "&local=true" : "&local=false");

		HttpRequest usageRequest = HttpRequest.newBuilder(URI.create(usageStr)).timeout(Duration.ofMillis(200)).build();

		Properties attributes = OwlcmsSession.getCurrent().getAttributes();
		// fire and forget
		new Thread(() -> {
			try {
				client.send(usageRequest, BodyHandlers.ofString());
				attributes.setProperty(USAGE_STR, usageStr);
				logger.info("logged usage {}", attributes.getProperty(USAGE_STR));
			} catch (Throwable e) {
				LoggerUtils.logError(logger, e);
			}
		}).start();
	}
}