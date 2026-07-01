/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import static app.owlcms.fieldofplay.FOPState.DECISION_VISIBLE;

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

import app.owlcms.data.config.Config;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.InputKind;
import app.owlcms.fieldofplay.TimingPolicy;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.DelayTimer;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * ExplicitDecision display element.
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("decision-element")
@JsModule("./components/DecisionElement.js")
@Uses(Icon.class)
public class DecisionElement extends LitTemplate
		implements SafeEventBusRegistration {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(DecisionElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	private static final long INITIAL_DECISION_FALLBACK_DELAY_MS = FieldOfPlay.REVERSAL_DELAY + 150L;

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	protected EventBus fopEventBus;
	protected EventBus uiEventBus;
	private boolean silenced;
	private boolean juryMode;
	private boolean dontReset;
	private boolean publicFacing;
	protected boolean downSlave;
	protected FieldOfPlay fop;
	private final AtomicLong decisionDisplayGeneration = new AtomicLong();
	private final AtomicLong decisionPayloadSequence = new AtomicLong();

	public DecisionElement() {
	}

	public boolean isDontReset() {
		return this.dontReset;
	}

	public boolean isPublicFacing() {
		return this.publicFacing;
	}

	public void setFop(FieldOfPlay fop) {
		FieldOfPlay previousFop = this.fop;
		boolean changed = previousFop != fop;
		if (changed && previousFop != null && this.uiEventBus != null) {
			unregister(this, previousFop.getUiEventBus());
		}
		this.fop = fop;
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
		this.uiEventBus = uiEventBusRegister(this, this.fop);
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
	public void masterRefereeUpdate(String fopName, Boolean ref1, Boolean ref2, Boolean ref3, Integer ref1Time,
			Integer ref2Time,
			Integer ref3Time) {
		Object origin = this.getOrigin();
		if (this.fop != null && fopName.contentEquals(this.fop.getName())) {
			if (this.isSingleRef()) {
				Integer refIndex = ref1 != null ? 0 : (ref2 != null ? 1 : (ref3 != null ? 2 : null));
				Boolean decision = ref1 != null ? ref1 : (ref2 != null ? ref2 : ref3);
				if (refIndex != null && decision != null) {
					if (Config.getCurrent().featureSwitch("playwright")) {
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
							Long.valueOf(ref1Time),
							Long.valueOf(ref2Time),
							Long.valueOf(ref3Time), false));
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
	}

	public void setJury(boolean juryMode) {
		this.setJuryMode(juryMode);
		getElement().setProperty("jury", juryMode);
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
	}

	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			logger.debug("slaveBreakStart disable");
			setEnabled(true);
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		long generation = this.decisionDisplayGeneration.incrementAndGet();
		if (isDontReset()) {
			return;
		}
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			if (generation != this.decisionDisplayGeneration.get()) {
				return;
			}
			resetDecisionDisplay();
		});
	}

	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		logger.debug("!!! slaveDownSignal  downSlave {} emitter {}", isDownSlave(), this.getOrigin() == e.getOrigin());
		if (Config.getCurrent().featureSwitch("playwright")) {
			logger./*playwright*/warn("{}decisionElement slaveDownSignal origin={} juryMode={}", FieldOfPlay.getLoggingName(this.fop),
				this.getOrigin(), isJuryMode());
		}
		if (isJuryMode()) {
			// jury mode doesn't show down signal
			return;
		}
		// Backend now controls showing down on all decision elements including the
		// keystroke master
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			uiEventLogger.debug("!!! {} down ({})", this.getOrigin(),
					this.getParent().get().getClass().getSimpleName());
			getElement().setProperty("singleRef", this.isSingleRef());
			boolean emitSoundsOnServer = (this.fop != null && this.fop.isEmitSoundsOnServer());
			showDownSignal(isSilenced() || emitSoundsOnServer);
		});
	}

	@Subscribe
	public void slaveResetOnNewClock(UIEvent.ResetOnNewClock e) {
		long generation = this.decisionDisplayGeneration.incrementAndGet();
		if (isDontReset()) {
			return;
		}
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			if (generation != this.decisionDisplayGeneration.get()) {
				return;
			}
			resetDecisionDisplay();
		});
	}

	@Subscribe
	public void slaveShowDecision(UIEvent.Decision e) {
		boolean announcerForced = e.getInputKind() == InputKind.ANNOUNCER_ENTRY;
		// logger.debug("decision {} {} {} --- {}", e.ref1, e.ref2, e.ref3,
		// e.isSingleLight());
		if (Config.getCurrent().featureSwitch("playwright")) {
			logger./*playwright*/warn("{}decisionElement slaveShowDecision origin={} singleLight={} announcerForced={} refs=[{},{},{}]",
				FieldOfPlay.getLoggingName(this.fop), this.getOrigin(), e.isSingleLight(), announcerForced, e.ref1,
				e.ref2, e.ref3);
		}
		// Backend now controls hiding down and showing decisions on all decision
		// elements
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			showDecisionLights(e.decision, e.ref1, e.ref2, e.ref3, e.isSingleLight(), announcerForced);
		});
	}

	@Subscribe
	public void slaveInitialDecision(UIEvent.InitialDecision e) {
		boolean announcerForced = e.getInputKind() == InputKind.ANNOUNCER_ENTRY;
		if (Config.getCurrent().featureSwitch("playwright")) {
			logger./*playwright*/warn("{}decisionElement slaveInitialDecision origin={} timingPolicy={} singleLight={} announcerForced={} refs=[{},{},{}]",
				FieldOfPlay.getLoggingName(this.fop), this.getOrigin(), e.getTimingPolicy(), e.isSingleLight(),
				announcerForced, e.ref1, e.ref2, e.ref3);
		}
		if (e.getTimingPolicy() != TimingPolicy.DELAYED) {
			return;
		}
		long generation = this.decisionDisplayGeneration.incrementAndGet();
		new DelayTimer().schedule(() -> {
			UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
				if (generation != this.decisionDisplayGeneration.get()) {
					return;
				}
				if (this.fop == null || this.fop.getState() != DECISION_VISIBLE) {
					if (Config.getCurrent().featureSwitch("playwright")) {
						logger./*playwright*/warn("{}decisionElement initialDecision fallback skipped origin={} fopState={}",
								FieldOfPlay.getLoggingName(this.fop), this.getOrigin(),
								this.fop != null ? this.fop.getState() : null);
					}
					return;
				}
				Boolean[] currentDecisions = this.fop.getRefereeDecision();
				Boolean ref1 = e.isSingleLight() ? null : currentDecisions[0];
				Boolean ref2 = currentDecisions[1];
				Boolean ref3 = e.isSingleLight() ? null : currentDecisions[2];
				Boolean goodLift = computeGoodLift(ref1, ref2, ref3, e.isSingleLight());
				if (Config.getCurrent().featureSwitch("playwright")) {
					logger./*playwright*/warn("{}decisionElement initialDecision fallback showing decision origin={} refs=[{},{},{}]",
							FieldOfPlay.getLoggingName(this.fop), this.getOrigin(), ref1, ref2, ref3);
				}
				showDecisionLights(goodLift, ref1, ref2, ref3, e.isSingleLight(), announcerForced);
			});
		}, INITIAL_DECISION_FALLBACK_DELAY_MS);
	}

	// FIXME: double listener -- JuryDisplayDecisionElement also @Subscribes to
	// UIEvent.StartTime (slaveStartTime). Both this base handler (generation++,
	// setEnabled(true)) and the jury handler (doReset) fire for one StartTime event
	// on a jury element. Consolidate into a single StartTime handler.
	@Subscribe
	public void slaveStartTimer(UIEvent.StartTime e) {
		this.decisionDisplayGeneration.incrementAndGet();
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			// uiEventLogger.debug("!!! slaveStartTimer enable");
			setEnabled(true);
		});
	}

	protected void showDecisionLights(Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3, boolean singleLight,
	        boolean announcerForced) {
		this.decisionDisplayGeneration.incrementAndGet();
		setDecisionProperties(decision, ref1, ref2, ref3, singleLight, announcerForced);
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
		JsonObject payload = Json.createObject();
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

	private void putNullableBoolean(JsonObject payload, String key, Boolean value) {
		if (value == null) {
			payload.put(key, Json.createNull());
		} else {
			payload.put(key, value.booleanValue());
		}
	}

	protected Boolean computeGoodLift(Boolean ref1, Boolean ref2, Boolean ref3, boolean singleLight) {
		int whites = 0;
		whites += Boolean.TRUE.equals(ref1) ? 1 : 0;
		whites += Boolean.TRUE.equals(ref2) ? 1 : 0;
		whites += Boolean.TRUE.equals(ref3) ? 1 : 0;
		return singleLight ? whites >= 1 : whites >= 2;
	}

	@Subscribe
	public void slaveStopTimer(UIEvent.StopTime e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			// uiEventLogger.debug("!!! slaveStopTimer enable");
			setEnabled(true);
		});
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
