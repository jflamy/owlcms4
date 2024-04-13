/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-Fran�ois Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.video;

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
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.FOPParametersReader;
import app.owlcms.data.athlete.LiftDefinition;
import app.owlcms.data.athlete.LiftDefinition.Stage;
import app.owlcms.data.group.Group;
import app.owlcms.data.records.RecordEvent;
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
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class StreamingEventMonitor
 *
 * This shows an overlay when competition events such as a record attempt or jury deliberation take place.
 *
 */
// TODO: show record attempt and new record for a limited duration.

@SuppressWarnings({ "serial", "deprecation" })
@Tag("eventmonitor-template")
@JsModule("./components/EventMonitor.js")
@Route("displays/notifications")

public class StreamingEventMonitor extends LitTemplate implements FOPParametersReader,
        SafeEventBusRegistration, UIEventProcessor, StylesDirSelection, HasDynamicTitle {

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
			return this.liftType;
		}

		public void setLiftType(Stage liftType) {
			this.liftType = liftType;
		}

		@Override
		public String toString() {
			return "Status [breakType=" + this.breakType + ", ceremonyType=" + this.ceremonyType + ", decision="
			        + this.decision
			        + ", state=" + this.state + ", challengedRecords=" + this.challengedRecords + ", liftType="
			        + this.liftType + "]";
		}
	}

	final static int HISTORY_SIZE = 3;
	final private static Logger uiEventLogger = (Logger) LoggerFactory
	        .getLogger("UI" + StreamingEventMonitor.class.getSimpleName());

	static {
		uiEventLogger.setLevel(Level.WARN);
	}
	final private Logger logger = (Logger) LoggerFactory.getLogger(StreamingEventMonitor.class);
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
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private String routeParameter;
	private boolean video;
	private boolean showLonger;
	private FieldOfPlay fop;
	private Group group;
	private QueryParameters defaultParameters;

	/**
	 * Instantiates a new results board.
	 */
	public StreamingEventMonitor() {
		OwlcmsFactory.waitDBInitialized();
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
		this.getElement().getStyle().set("width", "100%");
		// we need two items on the stack (current + previous)
		doPush(new Status(FOPState.INACTIVE, null, null, null, false, null));
		doPush(new Status(FOPState.INACTIVE, null, null, null, false, null));
	}

	@Override
	public QueryParameters getDefaultParameters() {
		return this.defaultParameters;
	}

	@Override
	final public FieldOfPlay getFop() {
		return this.fop;
	}

	@Override
	final public Group getGroup() {
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
	public String getPageTitle() {
		return getTranslation("Video.EventMonitoringButton") + OwlcmsSession.getFopNameIfMultiple();
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

	@Override
	public boolean isVideo() {
		return this.video;
	}

	@Override
	public void setDefaultParameters(QueryParameters qp) {
		this.defaultParameters = qp;
	}

	@Override
	final public void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	@Override
	final public void setGroup(Group group) {
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
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		FOPParametersReader.super.setParameter(event, parameter);
		this.routeParameter = parameter;
	}

	@Override
	public void setRouteParameter(String routeParameter) {
		this.routeParameter = routeParameter;
	}

	@Override
	public void setShowInitialDialog(boolean b) {
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> newParameterMap) {
		this.urlParameterMap = removeDefaultValues(newParameterMap);
	}

	@Override
	public void setVideo(boolean b) {
		this.video = b;
	}

	@Subscribe
	public void slaveUIEvent(UIEvent e) {
		if (e instanceof UIEvent.SetTime) {
			// ignore events that don't change state
			return;
		} else if (e instanceof UIEvent.Notification) {
			UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
				OwlcmsSession.withFop(fop -> {
					this.logger.trace("---- notification {} {}", fop.getName(),
					        ((UIEvent.Notification) e).getNotificationString());
				});
			});
		}
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e /* , e.getTrace() */);
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
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
		return "EventMonitor [history=" + this.history + ", currentBreakType=" + this.currentBreakType
		        + ", currentCeremony="
		        + this.currentCeremony + ", currentDecision=" + this.currentDecision + ", currentChallengedRecords="
		        + this.currentChallengedRecords + ", currentFOP=" + this.currentFOP + ", currentState="
		        + this.currentState
		        + ", previousBreakType=" + this.previousBreakType + ", previousCeremony=" + this.previousCeremony
		        + ", previousDecision=" + this.previousDecision + ", previousChallengedRecords="
		        + this.previousChallengedRecords
		        + ", previousState=" + this.previousState + ", prevTitle=" + this.prevTitle + ", title=" + this.title
		        + ", uiEventBus="
		        + this.uiEventBus + ", currentLiftType=" + this.currentLiftType + ", previousLiftType="
		        + this.previousLiftType + "]";
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via FOPParameters interface default methods.
		OwlcmsSession.withFop(fop -> {
			init();

			checkVideo(this);
			// sync with current status of FOP
			syncWithFOP(null);
			// we listen on uiEventBus.
			this.uiEventBus = uiEventBusRegister(this, fop);
		});
		doUpdate();
	}

	void uiLog(UIEvent e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
	}

	private String computePageTitle() {
		StringBuilder pageTitle = new StringBuilder();
		computeValues();

		{
			this.logger.debug("-- normal {} {} {}", this.h0, this.h1, this.h2);
		}

		if (this.currentState == FOPState.INACTIVE || this.currentState == FOPState.BREAK) {
			pageTitle.append("break=");
		} else {
			pageTitle.append("state=");
		}
		pageTitle.append(this.currentState.name());

		if (this.currentState == FOPState.BREAK && this.currentCeremony != null) {
			pageTitle.append(".");
			pageTitle.append(this.currentCeremony.name());
		} else if (this.currentState == FOPState.BREAK && this.currentBreakType != null) {
			pageTitle.append(".");
			pageTitle.append(this.currentBreakType.name());
		} else if (this.currentState == FOPState.DECISION_VISIBLE) {
			pageTitle.append(".");
			pageTitle.append(
			        this.currentDecision == null ? "UNDECIDED" : (this.currentDecision ? "GOOD_LIFT" : "BAD_LIFT"));
			if (this.currentDecision && this.currentChallengedRecords) {
				pageTitle.append(".NEW_RECORD");
			} else if (this.previousState == FOPState.DECISION_VISIBLE && this.previousDecision
			        && this.previousChallengedRecords) {
				// special case where state changes too quickly;
				pageTitle.append(".NEW_RECORD");
			}
			setExpiryBeforeChangingStatus(System.currentTimeMillis() + (FieldOfPlay.DECISION_VISIBLE_DURATION));
		} else if (this.currentChallengedRecords) {
			pageTitle.append(".RECORD_ATTEMPT");
		}

		pageTitle.append(";");
		pageTitle.append("previous=");
		pageTitle.append(this.previousState.name());
		if (this.previousState == FOPState.BREAK && this.previousCeremony != null) {
			pageTitle.append(".");
			pageTitle.append(this.previousCeremony.name());
		} else if (this.previousState == FOPState.BREAK && this.previousBreakType != null) {
			pageTitle.append(".");
			pageTitle.append(this.previousBreakType.name());
		} else if (this.previousState == FOPState.DECISION_VISIBLE) {
			pageTitle.append(".");
			pageTitle.append(
			        this.previousDecision == null ? "UNDECIDED" : (this.previousDecision ? "GOOD_LIFT" : "BAD_LIFT"));
		}

		if (this.currentLiftType != null) {
			pageTitle.append(";");
			pageTitle.append("liftType=");
			pageTitle.append(this.currentLiftType.toString());
		}
		pageTitle.append(";");
		pageTitle.append("fop=");
		pageTitle.append(this.currentFOP);

		String string = pageTitle.toString();

		return string;
	}

	private void computeValues() {
		this.h0 = this.history.size() > 0 ? this.history.get(0) : null;
		this.h1 = this.history.size() > 1 ? this.history.get(1) : null;
		this.h2 = this.history.size() > 2 ? this.history.get(2) : null;
		this.currentState = this.h0 != null ? this.h0.state : null;
		this.currentBreakType = this.h0 != null ? this.h0.breakType : null;
		this.currentCeremony = this.h0 != null ? this.h0.ceremonyType : null;
		this.currentDecision = this.h0 != null ? this.h0.decision : null;
		this.currentChallengedRecords = this.h0 != null ? this.h0.challengedRecords : false;
		this.currentLiftType = this.h0 != null ? this.h0.liftType : null;

		this.previousState = this.h1 != null ? this.h1.state : null;
		this.previousBreakType = this.h1 != null ? this.h1.breakType : null;
		this.previousCeremony = this.h1 != null ? this.h1.ceremonyType : null;
		this.previousDecision = this.h1 != null ? this.h1.decision : null;
		this.previousChallengedRecords = this.h1 != null ? this.h1.challengedRecords : null;
		this.previousLiftType = this.h1 != null ? this.h1.liftType : null;
	}

	private void doPush(Status status) {
		this.history.add(0, status);
		if (this.history.size() > HISTORY_SIZE) {
			this.history.remove(HISTORY_SIZE);
		}
	}

	private synchronized void doUpdate() {
		this.title = computePageTitle();
		String comparisonTitle = this.title != null ? this.title.substring(0, this.title.indexOf(";")) : this.title;
		String comparisonPrevTitle = this.prevTitle != null ? this.prevTitle.substring(0, this.prevTitle.indexOf(";"))
		        : this.prevTitle;
		this.logger.debug("comparing comparisonTitle={} with comparisonPrevTitle={}", comparisonTitle,
		        comparisonPrevTitle);
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
				if ((!this.title.startsWith("state=DECISION"))) {
					this.logger.info("#### DELAYING {} monitor {}", waitBeforeChangingStatus, this.title);
					new java.util.Timer().schedule(
					        new java.util.TimerTask() {
						        @Override
						        public void run() {
							        ui.access(() -> {
								        updateBar(element, StreamingEventMonitor.this.title);
								        StreamingEventMonitor.this.logger.info("#### DELAYED monitor {}",
								                StreamingEventMonitor.this.title);
							        });
						        }
					        },
					        waitBeforeChangingStatus);
				} else {
					updateBar(element, this.title);
					this.logger.info("#### DECISION monitor {}", this.title);
				}
			} else {
				updateBar(element, this.title);
				this.logger.info("#### notification monitor {}", this.title);
			}
			this.prevTitle = this.title;
		}
		if (same) {
			this.logger.debug("---- monitor duplicate {}", this.title);
		}
	}

	private long getExpiryBeforeChangingStatus() {
		return this.expiryBeforeChangingStatus;
	}

	private Object getOrigin() {
		return this;
	}

	private void init() {
		OwlcmsSession.withFop(fop -> {
			this.logger.trace("{}Starting notification monitor", FieldOfPlay.getLoggingName(fop));
			setId("scoreboard-" + fop.getName());
		});
	}

	private boolean isNotEmpty(List<RecordEvent> list) {
		return list != null && !list.isEmpty();
	}

	private void setExpiryBeforeChangingStatus(long expiry) {
		this.expiryBeforeChangingStatus = expiry;
	}

	private boolean syncWithFOP(UIEvent e) {
		boolean significant[] = { false };
		OwlcmsSession.withFop(fop -> {
			this.currentFOP = fop.getName();
			boolean fopChallengedRecords = fop.getChallengedRecords() != null && !fop.getChallengedRecords().isEmpty();
			boolean newRecord = e instanceof UIEvent.JuryNotification && ((UIEvent.JuryNotification) e).getNewRecord();
			boolean curChallengedRecords = this.history.get(0).challengedRecords;

			boolean stateChanged = fop.getState() != this.history.get(0).state;
			boolean recordsChanged = fopChallengedRecords != curChallengedRecords;
			this.logger.debug(">>>>>>EventMonitor event {} fop {} history {} recordsChanged {}",
			        e != null ? e.getClass().getSimpleName() : null, fop.getState(), this.history.get(0).state,
			        recordsChanged);
			if (e != null && e instanceof UIEvent.DecisionReset) {
				// this event does not change state, and should always be ignored.
				// however, because it can occur very close to the lifter update, and we have
				// asynchronous events
				// there is a possibility that it comes late and out of order. So we ignore it
				// explicitly.
				this.logger.debug(">>>>>>EventMonitor DecisionReset ignored");
				significant[0] = false;
			} else if (stateChanged || recordsChanged) {
				doPush(new Status(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), fop.getGoodLift(),
				        isNotEmpty(fop.getChallengedRecords()) || newRecord, fop.getCurrentStage()));
				significant[0] = true;
			} else if (fop.getState() == FOPState.BREAK) {
				if (fop.getBreakType() != this.history.get(0).breakType
				        || fop.getCeremonyType() != this.history.get(0).ceremonyType) {
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
		this.logger.debug(">>>>>>EventMonitor sync significant {}", significant[0]);
		return significant[0];
	}

	private void updateBar(Element element, String title) {
		if (this.showLonger) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			this.showLonger = false;
		}
		// logger.debug("UpdateBar {}",title);
		element.setProperty("title", title);
		element.setProperty("notificationClass", "neutralNotification");
		// element.callJsFunction("setTitle", title);

		if (title.contains(".NEW_RECORD")) {
			element.setProperty("title", Translator.translate("NewRecord"));
			this.showLonger = true;
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
		}

	}

	private long waitBeforeChangingStatus() {
		return getExpiryBeforeChangingStatus() - System.currentTimeMillis();
	}
}
