/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import java.util.Locale;

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

import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

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

	public DecisionElement() {
	}

	public boolean isDontReset() {
		return this.dontReset;
	}

	public boolean isPublicFacing() {
		return this.publicFacing;
	}

	public void setFop(FieldOfPlay fop) {
		this.fop = fop;
		logger.debug("DecisionElement.setFop: fop={} isSingleRef={} isJuryMode={} {}",
		        (fop != null ? fop.getName() : "null"), this.isSingleRef(), this.isJuryMode(),
		        LoggerUtils.whereFrom());
		getElement().setProperty("singleRef", this.isSingleRef());
	}

	/**
	 * @return the silenced
	 */
	public boolean isSilenced() {
		return this.silenced;
	}

	@ClientCallable
	/**
	 * client side only sends after timer has been started until decision reset or break
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
					logger.warn("DecisionElement solo referee update refIndex={} decision={} {}",
					        refIndex, decision, LoggerUtils.whereFrom());
					this.fop.fopEventPost(new FOPEvent.DecisionUpdate(origin, refIndex, decision));
					return;
				}
			}
			//logger.debug("masterRefereeUpdate {} {} {}",ref1, ref2, ref3);
			this.fop.fopEventPost(
			        new FOPEvent.DecisionFullUpdate(origin, this.fop.getCurAthlete(), ref1, ref2, ref3,
			                Long.valueOf(ref1Time),
			                Long.valueOf(ref2Time),
			                Long.valueOf(ref3Time), false));
		}
	}

	@ClientCallable
	/**
	 * client side only sends after timer has been started until decision reset or break
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
			this.getElement().callJsFunction("setEnabled", true);
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		if (isDontReset()) {
			return;
		}
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			getElement().setProperty("singleRef", this.isSingleRef());
			getElement().callJsFunction("reset", false);
		});
	}

	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		logger.debug("!!! slaveDownSignal  downSlave {} emitter {}", isDownSlave(), this.getOrigin() == e.getOrigin());
		if (isJuryMode()) {
			// jury mode doesn't show down signal
			return;
		}
		// Backend now controls showing down on all decision elements including the keystroke master
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			uiEventLogger.debug("!!! {} down ({})", this.getOrigin(),
			        this.getParent().get().getClass().getSimpleName());
			getElement().setProperty("singleRef", this.isSingleRef());
			boolean emitSoundsOnServer = (this.fop != null && this.fop.isEmitSoundsOnServer());
			this.getElement().callJsFunction("showDown", false,
			        isSilenced() || emitSoundsOnServer);
		});
	}

	@Subscribe
	public void slaveResetOnNewClock(UIEvent.ResetOnNewClock e) {
		if (isDontReset()) {
			return;
		}
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			getElement().setProperty("singleRef", this.isSingleRef());
			getElement().callJsFunction("reset", false);
		});
	}

	@Subscribe
	public void slaveShowDecision(UIEvent.Decision e) {
		//logger.debug("decision {} {} {} --- {}", e.ref1, e.ref2, e.ref3, e.isSingleLight());
		// Backend now controls hiding down and showing decisions on all decision elements
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			if (e.isSingleLight()) {
				getElement().setProperty("singleRef", e.isSingleLight());
				this.getElement().callJsFunction("showSingleDecision", e.decision);
				this.getElement().callJsFunction("setEnabled", false);
			} else {
				getElement().setProperty("singleRef", e.isSingleLight());
				this.getElement().callJsFunction("showDecisions", false, e.ref1, e.ref2, e.ref3);
				this.getElement().callJsFunction("setEnabled", false);
			}
		});
	}

	@Subscribe
	public void slaveStartTimer(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			// uiEventLogger.debug("!!! slaveStartTimer enable");
			this.getElement().callJsFunction("setEnabled", true);
		});
	}

	@Subscribe
	public void slaveStopTimer(UIEvent.StopTime e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			// uiEventLogger.debug("!!! slaveStopTimer enable");
			this.getElement().callJsFunction("setEnabled", true);
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
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		if (this.fop == null) {
			logger.error("DecisionElement requires explicit FOP before attach {}", LoggerUtils.whereFrom());
			return;
		}
		// defensive: needed to make sure the update is processed on the right fop
		init(this.fop.getName());
		// we send on fopEventBus, listen on uiEventBus.
		this.fopEventBus = this.fop.getFopEventBus();
		this.uiEventBus = uiEventBusRegister(this, this.fop);
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
