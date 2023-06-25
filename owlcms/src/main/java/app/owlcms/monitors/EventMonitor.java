/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;

import app.owlcms.apputils.queryparameters.FOPParameters;
import app.owlcms.data.athlete.LiftDefinition;
import app.owlcms.data.athlete.LiftDefinition.Stage;
import app.owlcms.data.config.Config;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.displays.VideoOverride;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class EventMonitor
 *
 * Show athlete lifting order
 *
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("eventmonitor-template")
@JsModule("./components/EventMonitor.js")
@Route("displays/notifications")

public class EventMonitor extends PolymerTemplate<TemplateModel> implements FOPParameters,
        SafeEventBusRegistration, UIEventProcessor, VideoOverride, HasDynamicTitle {

	class Status {
		BreakType breakType;
		CeremonyType ceremonyType;
		Boolean decision;
		FOPState state;
		boolean challengedRecords;
		private Stage liftType;

		public Status(FOPState state, BreakType breakType, CeremonyType ceremonyType, Boolean decision,
		        boolean challengedRecords, LiftDefinition.Stage liftType) {
			this.state = state;
			this.breakType = breakType;
			this.ceremonyType = ceremonyType;
			this.decision = decision;
			this.challengedRecords = challengedRecords;
			this.setLiftType(liftType);
		}

		public Stage getLiftType() {
			return liftType;
		}

		public void setLiftType(Stage liftType) {
			this.liftType = liftType;
		}

		@Override
		public String toString() {
			return "Status [breakType=" + breakType + ", ceremonyType=" + ceremonyType + ", decision=" + decision
			        + ", state=" + state + ", challengedRecords=" + challengedRecords + ", liftType=" + liftType + "]";
		}
	}

	final static int HISTORY_SIZE = 3;

	final private static Logger uiEventLogger = (Logger) LoggerFactory
	        .getLogger("UI" + EventMonitor.class.getSimpleName());

	static {
		uiEventLogger.setLevel(Level.WARN);
	}
	final private Logger logger = (Logger) LoggerFactory.getLogger(EventMonitor.class);

	List<Status> history = new LinkedList<>();

	private BreakType currentBreakType;
	private CeremonyType currentCeremony;
	private Boolean currentDecision;
	private boolean currentChallengedRecords;
	private String currentFOP;
	private FOPState currentState;
	private Status h0;
	private Status h1;
	private Status h2;
	private Location location;
	private UI locationUI;
	private BreakType previousBreakType;
	private CeremonyType previousCeremony;
	private Boolean previousDecision;
	private boolean previousChallengedRecords;
	private FOPState previousState;
	private String prevTitle;
	private String title;
	private EventBus uiEventBus;

	private Object currentLiftType;
	private Object previousLiftType;

	private long expiryBeforeChangingStatus;

	Map<String, List<String>> urlParameterMap = new HashMap<String, List<String>>();

	private String routeParameter;

	private boolean video;

	private boolean showLonger;

	/**
	 * Instantiates a new results board.
	 */
	public EventMonitor() {
		OwlcmsFactory.waitDBInitialized();
		this.getElement().getStyle().set("width", "100%");
		// we need two items on the stack (current + previous)
		doPush(new Status(FOPState.INACTIVE, null, null, null, false, null));
		doPush(new Status(FOPState.INACTIVE, null, null, null, false, null));
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
	public Map<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
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

	@Override
	public void setLocation(Location location) {
		this.location = location;
	}

	@Override
	public void setLocationUI(UI locationUI) {
		this.locationUI = locationUI;
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> newParameterMap) {
		this.urlParameterMap = newParameterMap;
	}

	@Subscribe
	public void slaveUIEvent(UIEvent e) {
		if (e instanceof UIEvent.SetTime) {
			// ignore events that don't change state
			return;
		} else if (e instanceof UIEvent.Notification) {
			UIEventProcessor.uiAccess(this, uiEventBus, () -> {
				OwlcmsSession.withFop(fop -> {
					logger.trace("---- notification {} {}", fop.getName(),
					        ((UIEvent.Notification) e).getNotificationString());
				});
			});
		}
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e /* , e.getTrace() */);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			if (syncWithFOP(e)) {
				// significant transition
				doUpdate();
			} else {
				// logger.debug("event ignored {} : {}", e.getClass().getSimpleName(),
				// OwlcmsSession.getFop().getState());
			}
		});
	}

	@Override
	public String toString() {
		return "EventMonitor [history=" + history + ", currentBreakType=" + currentBreakType
		        + ", currentCeremony="
		        + currentCeremony + ", currentDecision=" + currentDecision + ", currentChallengedRecords="
		        + currentChallengedRecords + ", currentFOP=" + currentFOP + ", currentState=" + currentState
		        + ", previousBreakType=" + previousBreakType + ", previousCeremony=" + previousCeremony
		        + ", previousDecision=" + previousDecision + ", previousChallengedRecords=" + previousChallengedRecords
		        + ", previousState=" + previousState + ", prevTitle=" + prevTitle + ", title=" + title + ", uiEventBus="
		        + uiEventBus + ", currentLiftType=" + currentLiftType + ", previousLiftType=" + previousLiftType + "]";
	}

	private String computePageTitle() {
		StringBuilder pageTitle = new StringBuilder();
		computeValues();

		{
			logger.debug("-- normal {} {} {}", h0, h1, h2);
		}

		if (currentState == FOPState.INACTIVE || currentState == FOPState.BREAK) {
			pageTitle.append("break=");
		} else {
			pageTitle.append("state=");
		}
		pageTitle.append(currentState.name());

		if (currentState == FOPState.BREAK && currentCeremony != null) {
			pageTitle.append(".");
			pageTitle.append(currentCeremony.name());
		} else if (currentState == FOPState.BREAK && currentBreakType != null) {
			pageTitle.append(".");
			pageTitle.append(currentBreakType.name());
		} else if (currentState == FOPState.DECISION_VISIBLE) {
			pageTitle.append(".");
			pageTitle.append(currentDecision == null ? "UNDECIDED" : (currentDecision ? "GOOD_LIFT" : "BAD_LIFT"));
			if (currentDecision && currentChallengedRecords) {
				pageTitle.append(".NEW_RECORD");
			} else if (previousState == FOPState.DECISION_VISIBLE && previousDecision && previousChallengedRecords) {
				// special case where state changes too quickly;
				pageTitle.append(".NEW_RECORD");
			}
			setExpiryBeforeChangingStatus(System.currentTimeMillis() + (FieldOfPlay.DECISION_VISIBLE_DURATION));
		} else if (currentChallengedRecords) {
			pageTitle.append(".RECORD_ATTEMPT");
		}

		pageTitle.append(";");
		pageTitle.append("previous=");
		pageTitle.append(previousState.name());
		if (previousState == FOPState.BREAK && previousCeremony != null) {
			pageTitle.append(".");
			pageTitle.append(previousCeremony.name());
		} else if (previousState == FOPState.BREAK && previousBreakType != null) {
			pageTitle.append(".");
			pageTitle.append(previousBreakType.name());
		} else if (previousState == FOPState.DECISION_VISIBLE) {
			pageTitle.append(".");
			pageTitle.append(previousDecision == null ? "UNDECIDED" : (previousDecision ? "GOOD_LIFT" : "BAD_LIFT"));
		}

		if (currentLiftType != null) {
			pageTitle.append(";");
			pageTitle.append("liftType=");
			pageTitle.append(currentLiftType.toString());
		}
		pageTitle.append(";");
		pageTitle.append("fop=");
		pageTitle.append(currentFOP);

		String string = pageTitle.toString();

		return string;
	}

	private void computeValues() {
		h0 = history.size() > 0 ? history.get(0) : null;
		h1 = history.size() > 1 ? history.get(1) : null;
		h2 = history.size() > 2 ? history.get(2) : null;
		currentState = h0 != null ? h0.state : null;
		currentBreakType = h0 != null ? h0.breakType : null;
		currentCeremony = h0 != null ? h0.ceremonyType : null;
		currentDecision = h0 != null ? h0.decision : null;
		currentChallengedRecords = h0 != null ? h0.challengedRecords : false;
		currentLiftType = h0 != null ? h0.liftType : null;

		previousState = h1 != null ? h1.state : null;
		previousBreakType = h1 != null ? h1.breakType : null;
		previousCeremony = h1 != null ? h1.ceremonyType : null;
		previousDecision = h1 != null ? h1.decision : null;
		previousChallengedRecords = h1 != null ? h1.challengedRecords : null;
		previousLiftType = h1 != null ? h1.liftType : null;
	}

	private void doPush(Status status) {
		history.add(0, status);
		if (history.size() > HISTORY_SIZE) {
			history.remove(HISTORY_SIZE);
		}
	}

	private synchronized void doUpdate() {
		title = computePageTitle();
		String comparisonTitle = title != null ? title.substring(0, title.indexOf(";")) : title;
		String comparisonPrevTitle = prevTitle != null ? prevTitle.substring(0, prevTitle.indexOf(";")) : prevTitle;
		logger.debug("comparing comparisonTitle={} with comparisonPrevTitle={}", comparisonTitle, comparisonPrevTitle);
		boolean same = false;
		if (comparisonPrevTitle == null || comparisonTitle == null) {
			// same if both null
			same = (comparisonTitle == comparisonPrevTitle);
		} else if (comparisonTitle != null) {
			// same if same content comparison
			// prevTitle cannot be null (tested in previous branch)
			same = comparisonTitle.contentEquals(comparisonPrevTitle);
		}
		if (!same && !(comparisonTitle == null) && !comparisonTitle.isBlank()) {
			long waitBeforeChangingStatus = waitBeforeChangingStatus();
			Element element = this.getElement();
			UI ui = UI.getCurrent();
			if (waitBeforeChangingStatus > 0) {
				if ((!title.startsWith("state=DECISION"))) {
					logger.info("#### DELAYING {} monitor {}", waitBeforeChangingStatus, title);
					new java.util.Timer().schedule(
					        new java.util.TimerTask() {
						        @Override
						        public void run() {
							        ui.access(() -> {
								        updateBar(element, title);
								        logger.info("#### DELAYED monitor {}", title);
							        });
						        }
					        },
					        waitBeforeChangingStatus);
				} else {
					updateBar(element, title);
					logger.info("#### DECISION monitor {}", title);
				}
			} else {
				updateBar(element, title);
				logger.info("#### notification monitor {}", title);
			}
			prevTitle = title;
		}
		if (same) {
			logger.debug("---- monitor duplicate {}", title);
		}
	}

	private void updateBar(Element element, String title) {
		if (showLonger) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			showLonger = false;
		}
		logger.warn("UpdateBar {}",title);
		element.setProperty("title", title);
		element.setProperty("notificationClass", "neutralNotification");
		//element.callJsFunction("setTitle", title);

		if (title.contains(".NEW_RECORD")) {
			element.setProperty("title", Translator.translate("NewRecord"));
			showLonger = true;
		} else if (title.contains(".RECORD_ATTEMPT")) {
			element.setProperty("notificationClass", "attemptNotification");
			element.setProperty("title", Translator.translate("VideoNotification.RecordAttempt"));
		} else if (title.contains("GOOD_LIFT.NEW_RECORD")) {
			element.setProperty("notificationClass", "successNotification");
			element.setProperty("title", Translator.translate("VideoNotification.NewRecord"));
		} else if (title.contains("JURY") && title.contains("GOOD")) {
			element.setProperty("notificationClass", "successNotification");
			element.setProperty("title", Translator.translate("VideoNotification.JuryGoodLift"));
		} else if (title.contains("CHALLENGE") && title.contains("GOOD_LIFT")) {
			element.setProperty("notificationClass", "successNotification");
			element.setProperty("title", Translator.translate("VideoNotification.JuryGoodLift"));
		} else if (title.contains("JURY") && title.contains("BAD")) {
			element.setProperty("notificationClass", "failNotification");
			element.setProperty("title", Translator.translate("VideoNotification.JuryNoLift"));
		} else if (title.contains("CHALLENGE") && title.contains("BAD_LIFT")) {
			element.setProperty("notificationClass", "failNotification");
			element.setProperty("title", Translator.translate("VideoNotification.JuryNoLift"));
		} else if (title.startsWith("break=BREAK.JURY")) {
			element.setProperty("title", Translator.translate("VideoNotification.JuryBreak"));
		} else if (title.startsWith("break=BREAK.CHALLENGE")) {
			element.setProperty("title", Translator.translate("VideoNotification.Challenge"));
		} else if (title.startsWith("break=BREAK.TECHNICAL")) {
			element.setProperty("title", Translator.translate("VideoNotification.TechnicalIssue"));
		} else if (title.startsWith("break=BREAK.MARSHAL")) {
			element.setProperty("title", Translator.translate("VideoNotification.MarshalIssue"));
		} else {
			element.setProperty("title", "");
			element.setProperty("notificationClass", "invisibleNotification");
			return;
		}

	}

	private long getExpiryBeforeChangingStatus() {
		return expiryBeforeChangingStatus;
	}

	private Object getOrigin() {
		return this;
	}

	private void init() {
		OwlcmsSession.withFop(fop -> {
			logger.trace("{}Starting notification monitor", fop.getLoggingName());
			setId("scoreboard-" + fop.getName());
		});
	}

	private boolean isNotEmpty(List<RecordEvent> list) {
		return list != null && !list.isEmpty();
	}

	private void setExpiryBeforeChangingStatus(long expiry) {
		expiryBeforeChangingStatus = expiry;
	}

	private boolean syncWithFOP(UIEvent e) {
		boolean significant[] = { false };
		OwlcmsSession.withFop(fop -> {
			currentFOP = fop.getName();
			boolean fopChallengedRecords = fop.getChallengedRecords() != null && !fop.getChallengedRecords().isEmpty();
			boolean newRecord = e instanceof UIEvent.JuryNotification && ((UIEvent.JuryNotification) e).getNewRecord();
			boolean curChallengedRecords = history.get(0).challengedRecords;

			boolean stateChanged = fop.getState() != history.get(0).state;
			boolean recordsChanged = fopChallengedRecords != curChallengedRecords;
			logger.debug(">>>>>>EventMonitor event {} fop {} history {} recordsChanged {}",
			        e != null ? e.getClass().getSimpleName() : null, fop.getState(), history.get(0).state,
			        recordsChanged);
			if (e != null && e instanceof UIEvent.DecisionReset) {
				// this event does not change state, and should always be ignored.
				// however, because it can occur very close to the lifter update, and we have
				// asynchronous events
				// there is a possibility that it comes late and out of order. So we ignore it
				// explicitly.
				logger.debug(">>>>>>EventMonitor DecisionReset ignored");
				significant[0] = false;
			} else if (stateChanged || recordsChanged) {
				doPush(new Status(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), fop.getGoodLift(),
				        isNotEmpty(fop.getChallengedRecords()) || newRecord, fop.getCurrentStage()));
				significant[0] = true;
			} else if (fop.getState() == FOPState.BREAK) {
				if (fop.getBreakType() != history.get(0).breakType
				        || fop.getCeremonyType() != history.get(0).ceremonyType) {
					doPush(new Status(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), null,
					        isNotEmpty(fop.getChallengedRecords()), null));
					significant[0] = true;
				} else {
					// logger.trace("*** EventMonitor ignored duplicate {} {}",
					// fop.getBreakType(),
					// fop.getCeremonyType());
				}
			} else {
				// logger.trace("*** EventMonitor non break {}", fop.getState());
			}
		});
		logger.debug(">>>>>>EventMonitor sync significant {}", significant[0]);
		return significant[0];
	}

	private long waitBeforeChangingStatus() {
		return getExpiryBeforeChangingStatus() - System.currentTimeMillis();
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
	 * AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via FOPParameters interface default methods.
		OwlcmsSession.withFop(fop -> {
			init();
			checkVideo(Config.getCurrent().getStylesDirectory() + "/video/currentathlete.css", routeParameter, this);
			// sync with current status of FOP
			syncWithFOP(null);
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
		doUpdate();
	}

	void uiLog(UIEvent e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
	}

	@Override
	public void setShowInitialDialog(boolean b) {
	}

	@Override
	public void setVideo(boolean b) {
		this.video = b;
	}

	@Override
	public boolean isVideo() {
		return this.video;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		FOPParameters.super.setParameter(event, parameter);
		this.routeParameter = parameter;
	}

	@Override
	public String getPageTitle() {
        return getTranslation("Video.EventMonitoringButton") + OwlcmsSession.getFopNameIfMultiple();
	}

}
