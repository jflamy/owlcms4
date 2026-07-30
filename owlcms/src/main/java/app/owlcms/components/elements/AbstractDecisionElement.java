/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.littemplate.LitTemplate;

import tools.jackson.databind.node.ObjectNode;

import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.JsonUtils;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Shared behavior for decision display elements.
 */
/// Decision display behavior by view:
///
/// | View | Athlete Timer | Break Timer | Stopwatch | Decision Lights | Referee Decision Behavior |
/// |---|---|---|---|---|---|
/// | Attempt board | `AthleteTimerElement` | `BreakTimerElement` | no | `DecisionElement` | Public behavior: toggle off waits until `DECISION_VISIBLE`; toggle on shows `INITIAL_DECISION` immediately, then live reversals for 3 seconds until final. |
/// | Decision board | `AthleteTimerElement` | `BreakTimerElement` | no | `DecisionElement` | Same public behavior as attempt board. |
/// | Scoreboard | `AthleteTimerElement` | `BreakTimerElement` | decision section uses `StopwatchTimerElement` | main: `DecisionElement`; decision section: `DecisionBlockDecisionElement` | Public behavior: toggle off waits until `DECISION_VISIBLE`; toggle on shows `INITIAL_DECISION` immediately, then live reversals for 3 seconds until final. |
/// | Jury keypad | review `AthleteTimerElement` | no | no | `JuryDisplayDecisionElement` | Live referee decisions always; no 3-second public delay. |
/// | Control console | `AthleteTimerElement` | `BreakTimerElement` | no | `JuryDisplayDecisionElement` | Live referee decisions always; no 3-second public delay. |
/// | Jury console | inherited `AthleteTimerElement` | inherited `BreakTimerElement` | no | `JuryDisplayDecisionElement` | Live referee decisions always; no 3-second public delay. |
@SuppressWarnings({ "serial", "deprecation" })
@Tag("decision-element")
@JsModule("./components/DecisionElement.js")
@Uses(Icon.class)
public abstract class AbstractDecisionElement extends LitTemplate
		implements SafeEventBusRegistration, DecisionElementState.IDecisionRenderer {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AbstractDecisionElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	public static final long MINIMUM_DOWN_SIGNAL_VISIBLE_MS = FieldOfPlay.MINIMUM_DOWN_SIGNAL_VISIBLE_MS;
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	protected EventBus fopEventBus;
	protected EventBus uiEventBus;
	private final DecisionElementState decisionState = new DecisionElementState(this);
	private boolean silenced;
	private boolean juryMode;
	private boolean dontReset;
	private boolean liveRefereeUpdates;
	private boolean publicFacing;
	private boolean busDriven = true;
	protected boolean downSlave;
	protected FieldOfPlay fop;
	private final AtomicLong decisionPayloadSequence = new AtomicLong();

	protected AbstractDecisionElement() {
	}

	public boolean isDontReset() {
		return this.dontReset;
	}

	public boolean isPublicFacing() {
		return this.publicFacing;
	}

	/**
	 * When {@code false}, this element does not self-subscribe to FOP decision events. Its parent
	 * view becomes the single authority and drives the display explicitly (e.g. the results
	 * decision section, whose {@code DecisionBlockState} is the sole authority). Must be set before
	 * the element is bound to a FOP.
	 */
	public void setBusDriven(boolean busDriven) {
		this.busDriven = busDriven;
	}

	public DecisionElementState getDecisionState() {
		return this.decisionState;
	}

	public void setFop(FieldOfPlay fop) {
		FieldOfPlay previousFop = this.fop;
		boolean changed = previousFop != fop;
		if (changed && previousFop != null && this.uiEventBus != null) {
			unregister(this.decisionState, previousFop.getUiEventBus());
		}
		this.fop = fop;
		this.decisionState.setFop(fop);
		logger.debug("DecisionElement.setFop: fop={} isSingleRef={} isJuryMode={} {}",
				(fop != null ? fop.getName() : "null"), this.isSingleRef(), this.isJuryMode(),
				LoggerUtils.whereFrom());
		getElement().setProperty("singleRef", this.isSingleRef());
		if (changed && this.getUI().isPresent()) {
			bindToFopIfReady();
		}
	}

	private void bindToFopIfReady() {
		if (this.fop == null) {
			return;
		}
		init(this.fop.getName());
		this.fopEventBus = this.fop.getFopEventBus();
		if (this.busDriven) {
			this.uiEventBus = uiEventBusRegisterNoUI(this.decisionState, this.fop);
		} else {
			// Pure-renderer mode: the parent view is the single authority and drives this element
			// explicitly. Do not self-subscribe to FOP decision events. We still need the UI event
			// bus reference for uiAccess in the renderer callbacks.
			this.uiEventBus = this.fop.getUiEventBus();
		}
	}

	/**
	 * @return the silenced
	 */
	public boolean isSilenced() {
		return this.silenced;
	}

	@ClientCallable
	/**
	 * client side only sends after timer has been started until decision reset or
	 * break
	 *
	 * @param ref1
	 * @param ref2
	 * @param ref3
	 * @param ref1Time
	 * @param ref2Time
	 * @param ref3Time
	 */
	public void masterRefereeUpdate(String fopName, Boolean ref1, Boolean ref2, Boolean ref3, Long ref1Time,
			Long ref2Time,
			Long ref3Time) {
		Object origin = this.getOrigin();
		if (this.fop != null && fopName.contentEquals(this.fop.getName())) {
			if (this.isSingleRef()) {
				Integer refIndex = ref1 != null ? 0 : (ref2 != null ? 1 : (ref3 != null ? 2 : null));
				Boolean decision = ref1 != null ? ref1 : (ref2 != null ? ref2 : ref3);
				if (refIndex != null && decision != null) {
					if (Config.getCurrent().featureSwitch(FeatureSwitch.PLAYWRIGHT)) {
						logger./*playwright*/warn("DecisionElement solo referee update refIndex={} decision={} {}",
								refIndex, decision, LoggerUtils.whereFrom());
					}
					this.fop.fopEventPost(new FOPEvent.DecisionUpdate(origin, refIndex, decision));
					return;
				}
			}
			// logger.debug("masterRefereeUpdate {} {} {}",ref1, ref2, ref3);
			this.fop.fopEventPost(
					new FOPEvent.DecisionFullUpdate(origin, this.fop.getCurAthlete(), ref1, ref2, ref3,
							ref1Time, ref2Time, ref3Time, false));
		}
	}

	@ClientCallable
	/**
	 * client side only sends after timer has been started until decision reset or
	 * break
	 *
	 * @param decision
	 * @param ref1
	 * @param ref2
	 * @param ref3
	 */
	public void masterShowDown(String fopName, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3) {
		Object origin = this.getOrigin();
		getElement().setProperty("singleRef", this.isSingleRef());
		if (this.fop != null && this.fop.getName().equals(fopName)) {
			this.fop.fopEventPost(new FOPEvent.DownSignal(origin));
		}
	}

	@ClientCallable
	public void decisionPayloadApplied(String sequence, String mode, Boolean singleRef, Boolean announcerForced,
	        Boolean ref1, Boolean ref2, Boolean ref3) {
		logger.debug("{}decisionElement applied decisionPayload sequence={} mode={} singleRef={} announcerForced={} refs=[{},{},{}] parent={}",
		        FieldOfPlay.getLoggingName(this.fop), sequence, mode, singleRef, announcerForced, ref1, ref2, ref3,
		        this.getOrigin());
	}

	public void setDontReset(boolean dontReset) {
		this.dontReset = dontReset;
		this.decisionState.setDontReset(dontReset);
	}

	/**
	 * Set the minimum time the down signal stays visible before an IMMEDIATE decision replaces it.
	 * 0 (default) = show decision immediately. Attempt and decision boards should set this
	 * to a non-zero value so athletes and spectators see the down signal.
	 */
	public void setDownSignalHoldMs(long downSignalHoldMs) {
		// The FieldOfPlay controls DOWN minimum visibility and emits UIEvent.Decision
		// only when the decision should be rendered.
	}

	public void setJury(boolean juryMode) {
		this.setJuryMode(juryMode);
		getElement().setProperty("jury", juryMode);
	}

	public void setLiveReferee(boolean liveReferee) {
		this.decisionState.setLiveReferee(liveReferee);
	}

	public boolean isLiveRefereeUpdates() {
		return this.liveRefereeUpdates;
	}

	public void setLiveRefereeUpdates(boolean liveRefereeUpdates) {
		this.liveRefereeUpdates = liveRefereeUpdates;
		setLiveReferee(liveRefereeUpdates);
	}

	public void setLiveRefereeUpdatesDuringImmediateWindowOnly(boolean liveRefereeUpdatesDuringImmediateWindowOnly) {
		// Timing windows are controlled by FieldOfPlay UI events, not by the renderer.
	}

	public void setResetOnClockStart(boolean resetOnClockStart) {
		this.decisionState.setResetOnClockStart(resetOnClockStart);
	}

	public void setShowsDownSignal(boolean showsDownSignal) {
		this.decisionState.setShowsDownSignal(showsDownSignal);
	}

	public void setDisplaySize(String size) {
		String normalized = size == null ? "small" : size.toLowerCase(Locale.ROOT);
		switch (normalized) {
			case "small":
			case "large":
			case "x-large":
				getElement().setProperty("size", normalized);
				break;
			default:
				throw new IllegalArgumentException("Unsupported decision element size: " + size);
		}
	}

	public void setPublicFacing(boolean publicFacing) {
		this.publicFacing = publicFacing;
		getElement().setProperty("publicFacing", publicFacing);
	}

	public void setSilenced(boolean b) {
		getElement().setProperty("silent", b);
		this.silenced = b;
		this.decisionState.setSilenced(b);
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		this.decisionState.slaveDecisionReset(e);
	}

	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		this.decisionState.slaveDownSignal(e);
	}

	@Subscribe
	public void slaveInitialDecision(UIEvent.InitialDecision e) {
		this.decisionState.slaveInitialDecision(e);
	}

	@Subscribe
	public void slaveShowDecision(UIEvent.Decision e) {
		this.decisionState.slaveShowDecision(e);
	}

	@Subscribe
	public void slaveStartTimer(UIEvent.StartTime e) {
		this.decisionState.slaveStartTimer(e);
	}

	protected void onStartTimer(UIEvent.StartTime e) {
		setEnabled(e, true);
	}

	protected void markDownSignalVisible() {
	}

	protected void showImmediateDecisionAfterMinimumDownSignal(UIEvent.InitialDecision e, boolean announcerForced) {
		this.decisionState.slaveInitialDecision(e);
	}

	protected boolean isImmediatePreviewWindowActive() {
		return false;
	}

	protected void showDecisionLights(Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3, boolean singleLight,
	        boolean announcerForced) {
		setDecisionProperties(decision, ref1, ref2, ref3, singleLight, announcerForced);
	}

	protected Boolean computeGoodLift(Boolean ref1, Boolean ref2, Boolean ref3, boolean singleLight) {
		int whites = 0;
		whites += Boolean.TRUE.equals(ref1) ? 1 : 0;
		whites += Boolean.TRUE.equals(ref2) ? 1 : 0;
		whites += Boolean.TRUE.equals(ref3) ? 1 : 0;
		return singleLight ? whites >= 1 : whites >= 2;
	}

	@Override
	public void setEnabled(UIEvent event, boolean enabled) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, event, () -> setEnabled(enabled));
	}

	@Override
	public void resetDecisionDisplay(UIEvent event, long generation) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, event, this.getOrigin(), () -> {
			if (this.decisionState.isCurrentGeneration(generation)) {
				resetDecisionDisplay();
			}
		});
	}

	@Override
	public void showDecisionLights(UIEvent event, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3,
	        boolean singleLight, boolean announcerForced) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, event,
				() -> setDecisionProperties(decision, ref1, ref2, ref3, singleLight, announcerForced));
	}

	@Override
	public void showDownSignal(UIEvent.DownSignal event, boolean silent) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, event, () -> {
			uiEventLogger.debug("!!! {} down ({})", this.getOrigin(),
					this.getParent().get().getClass().getSimpleName());
			getElement().setProperty("singleRef", this.isSingleRef());
			showDownSignal(silent);
		});
	}

	protected void setDecisionProperties(Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3,
			boolean singleLight, boolean announcerForced) {
		Boolean displayedRef2 = singleLight ? (ref2 != null ? ref2 : decision) : ref2;
		sendDecisionPayload("decision", ref1, displayedRef2, ref3, singleLight, true, announcerForced);
		setEnabled(false);
	}

	protected void resetDecisionDisplay() {
		clearDecisionProperties(false);
	}

	protected void clearDecisionProperties(boolean showDecision) {
		sendDecisionPayload("reset", null, null, null, this.isSingleRef(), showDecision, false);
		setEnabled(false);
	}

	protected void showDownSignal(boolean silent) {
		sendDecisionPayload("down", null, null, null, this.isSingleRef(), false, false);
		if (!silent) {
			this.getElement().callJsFunction("playDownSound");
		}
	}

	protected void setEnabled(boolean enabled) {
		getElement().setProperty("enabled", enabled);
	}

	/**
	 * Batch every decision display transition (down, decision, reset) into a single
	 * ordered payload. The JS applies it atomically and uses the monotonic sequence
	 * to drop stale/out-of-order payloads, so there is never an intermediate render
	 * between the down signal and the decision boxes.
	 */
	private void sendDecisionPayload(String mode, Boolean ref1, Boolean ref2, Boolean ref3, boolean singleLight,
			boolean showDecision, boolean announcerForced) {
		ObjectNode payload = JsonUtils.object();
		payload.put("sequence", Long.toString(this.decisionPayloadSequence.incrementAndGet()));
		payload.put("mode", mode);
		payload.put("singleRef", singleLight);
		payload.put("announcerForced", announcerForced);
		payload.put("showDecision", showDecision);
		putNullableBoolean(payload, "ref1", ref1);
		putNullableBoolean(payload, "ref2", ref2);
		putNullableBoolean(payload, "ref3", ref3);
		getElement().setPropertyJson("decisionPayload", payload);
	}

	private void putNullableBoolean(ObjectNode payload, String key, Boolean value) {
		if (value == null) {
			payload.set(key, JsonUtils.nullNode());
		} else {
			payload.put(key, value.booleanValue());
		}
	}

	protected Object getOrigin() {
		// we use the identity of our parent AttemptBoard or AthleteFacingAttemptBoard
		// to identify
		// our actions.
		return this.getParent().get();
	}

	protected boolean isJuryMode() {
		return this.juryMode;
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
	 * AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		if (this.fop == null) {
			return;
		}
		bindToFopIfReady();
	}

	private void init(String fopName) {
		getElement().setProperty("fopName", fopName);
	}

	private void setJuryMode(boolean juryMode) {
		this.juryMode = juryMode;
	}

	public boolean isDownSlave() {
		return this.fop.isSingleReferee();
	}

	public boolean isSingleRef() {
		return this.fop != null ? this.fop.isSingleReferee() : false;
	}

}
