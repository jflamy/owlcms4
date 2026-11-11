/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import app.owlcms.access.PublicPage;
import app.owlcms.access.RequiresFeature;
import app.owlcms.data.config.FeatureSwitch;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.FOPParametersReader;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class OBSMonitor
 *
 * Show athlete lifting order
 *
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("monitor-template")
@JsModule("./components/OBSMonitor.js")
@PublicPage
@RequiresFeature(FeatureSwitch.OBS_MONITORS)
@Route("displays/monitor")

public class OBSMonitor extends LitTemplate implements FOPParametersReader,
        SafeEventBusRegistration, UIEventProcessor {

	final private static Logger uiEventLogger = (Logger) LoggerFactory
	        .getLogger("UI" + OBSMonitor.class.getSimpleName());

	static {
		uiEventLogger.setLevel(Level.INFO);
	}
	final private Logger logger = (Logger) LoggerFactory.getLogger(OBSMonitor.class);
	private Location location;
	private UI locationUI;
	private EventBus uiEventBus;
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private FieldOfPlay fop;
	private Group group;
	private QueryParameters defaultParameters;
	private String routeParameter;
	private OBSMonitorStatus status;

	/**
	 * Instantiates a new results board.
	 */
	public OBSMonitor() {
		OwlcmsFactory.waitDBInitialized();
		this.getElement().getStyle().set("width", "100%");
	}

	@Override
	public QueryParameters getDefaultParameters() {
		return this.defaultParameters;
	}

	@Override
	public FieldOfPlay getFop() {
		return this.fop;
	}

	@Override
	public Group getGroup() {
		return this.group;
	}

	@Override
	public Location getLocation() {
		return this.location;
	}

	@Override
	public UI getLocationUI() {
		return this.locationUI;
	}

	@Override
	public String getRouteParameter() {
		return this.routeParameter;
	}

	@Override
	public Map<String, List<String>> getUrlParameterMap() {
		return this.urlParameterMap;
	}

	@Override
	public boolean isIgnoreGroupFromURL() {
		return true;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#isShowInitialDialog()
	 */
	@Override
	public boolean isShowInitialDialog() {
		return false;
	}

	/**
	 * The monitored platform comes from the {@code fop} parameter of the URL, never from the session: the monitors of
	 * several platforms are normally open in the same browser. The URL may omit it only when there is a single
	 * platform; the URL is rewritten with the platform actually monitored.
	 */
	@Override
	public Map<String, List<String>> readParams(Location location, Map<String, List<String>> parametersMap) {
		OwlcmsFactory.waitDBInitialized();
		FieldOfPlay monitoredFop = OBSMonitorStatus.fopToMonitor(parametersMap, OwlcmsFactory.getFOPs());
		if (monitoredFop == null) {
			this.logger./**/warn("{}OBS monitor URL does not name an existing platform (fop={}), monitoring nothing",
			        FieldOfPlay.getLoggingName(null), parametersMap.get(FOP));
		}
		setFop(monitoredFop);

		Map<String, List<String>> newParameterMap = new HashMap<>(parametersMap);
		newParameterMap.remove(GROUP);
		if (monitoredFop != null) {
			newParameterMap.put(FOP, List.of(monitoredFop.getName()));
		}
		setUrlParameterMap(newParameterMap);
		return getUrlParameterMap();
	}

	@Override
	public void setDefaultParameters(QueryParameters qp) {
		this.defaultParameters = qp;
	}

	@Override
	public void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	@Override
	public void setGroup(Group group) {
		this.group = group;
	}

	@Override
	public void setLocation(Location location) {
		this.location = location;
	}

	@Override
	public void setLocationUI(UI locationUI) {
		this.locationUI = locationUI;
	}

	@Override
	public void setRouteParameter(String routeParameter) {
		this.routeParameter = routeParameter;
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> newParameterMap) {
		this.urlParameterMap = removeDefaultValues(newParameterMap);
	}

	@Subscribe
	public void slaveUIEvent(UIEvent e) {
		if (e instanceof UIEvent.SetTime) {
			// ignore events that don't change state
			return;
		}
		uiEventLogger.debug("### {} {}", this.getClass().getSimpleName(), e);
		OBSMonitorStatus monitorStatus = this.status;
		if (monitorStatus == null) {
			return;
		}
		// computed here, on the event thread: it waits for the platform to finish what it is doing
		if (monitorStatus.onEvent(e) != null) {
			// UI updates may run in a different order than the events: each one shows the latest title
			UIEventProcessor.uiAccess(this, this.uiEventBus,
			        () -> publish(monitorStatus, monitorStatus.getTitle()));
		}
	}

	@Override
	public String toString() {
		return "OBSMonitor [status=" + this.status + "]";
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// the platform named in the URL, see readParams
		FieldOfPlay monitoredFop = getFop();
		if (monitoredFop == null) {
			String names = OwlcmsFactory.getFOPs().stream().map(FieldOfPlay::getName).sorted()
			        .collect(Collectors.joining(", "));
			String message = Translator.translate("OBS.PlatformRequired", names);
			this.getElement().setProperty("title", message);
			this.getElement().callJsFunction("setTitle", message);
			return;
		}
		if (this.status == null || this.status.getFop() != monitoredFop) {
			this.status = new OBSMonitorStatus(monitoredFop);
		}
		this.logger.trace("{}Starting monitoring", FieldOfPlay.getLoggingName(monitoredFop));
		setId("scoreboard-" + monitoredFop.getName());
		String title = this.status.attach();
		this.uiEventBus = uiEventBusRegister(this, monitoredFop);
		publish(this.status, title);
	}

	private synchronized void publish(OBSMonitorStatus monitorStatus, String title) {
		if (title == null) {
			return;
		}
		long waitBeforeChangingStatus = monitorStatus.waitBeforeChangingStatus();
		Element element = this.getElement();
		UI ui = getUI().orElse(null);
		if (ui != null && waitBeforeChangingStatus > 0 && !title.startsWith("state=DECISION")) {
			this.logger.info("#### DELAYING {} monitor {}", waitBeforeChangingStatus, title);
			new Timer().schedule(
			        new TimerTask() {
				        @Override
				        public void run() {
					        ui.access(() -> {
						        // as before: the title current when the wait ends
						        String latest = monitorStatus.getTitle();
						        element.setProperty("title", latest);
						        element.callJsFunction("setTitle", latest);
						        OBSMonitor.this.logger.info("#### DELAYED monitor {}", latest);
					        });
				        }
			        },
			        waitBeforeChangingStatus);
		} else {
			element.setProperty("title", title);
			element.callJsFunction("setTitle", title);
			this.logger.info("#### monitor {}", title);
		}
	}
}
