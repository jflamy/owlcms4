/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-Fran�ois Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

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
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.UIEvent;
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
		uiEventLogger.setLevel(Level.DEBUG);
	}
	protected EventBus fopEventBus;
	protected EventBus uiEventBus;
	private boolean silenced;
	private boolean juryMode;
	private boolean dontReset;
	private boolean publicFacing;

	public DecisionElement() {
	}

	public boolean isDontReset() {
		return dontReset;
	}

	public boolean isPublicFacing() {
		return publicFacing;
	}

	/**
	 * @return the silenced
	 */
	public boolean isSilenced() {
		return silenced;
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
		OwlcmsSession.withFop((fop) -> {
			if (!fopName.contentEquals(fop.getName())) {
				return;
			}
			fop.fopEventPost(
			        new FOPEvent.DecisionFullUpdate(origin, fop.getCurAthlete(), ref1, ref2, ref3,
			                Long.valueOf(ref1Time),
			                Long.valueOf(ref2Time),
			                Long.valueOf(ref3Time), false));
		});

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
		OwlcmsSession.getFop().fopEventPost(new FOPEvent.DownSignal(origin));
	}

	public void setDontReset(boolean dontReset) {
		this.dontReset = dontReset;
	}

	public void setJury(boolean juryMode) {
		this.setJuryMode(juryMode);
		getElement().setProperty("jury", juryMode);
	}

	public void setPublicFacing(boolean publicFacing) {
		this.publicFacing = publicFacing;
		getElement().setProperty("publicFacing", publicFacing);
	}

	public void setSilenced(boolean b) {
		getElement().setProperty("silent", b);
		silenced = b;
	}

	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			logger.debug("slaveBreakStart disable");
			this.getElement().callJsFunction("setEnabled", false);
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		if (isDontReset()) {
			return;
		}
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
			getElement().callJsFunction("reset", false);
		});
	}

	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		logger.trace("!!! slaveDownSignal {} {} {}", this, this.getOrigin(), e.getOrigin());
		if (isJuryMode() || (this.getOrigin() == e.getOrigin())) {
			// we emitted the down signal, don't do it again.
			// logger.trace("skipping down, {} is origin",this.getOrigin());
			return;
		}
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			uiEventLogger.debug("!!! {} down ({})", this.getOrigin(),
			        this.getParent().get().getClass().getSimpleName());
			this.getElement().callJsFunction("showDown", false,
			        isSilenced() || OwlcmsSession.getFop().isEmitSoundsOnServer());
			// FIXME hide down signal in 1 second.
		});
	}

	@Subscribe
	public void slaveResetOnNewClock(UIEvent.ResetOnNewClock e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
			getElement().callJsFunction("reset", false);
		});
	}

	@Subscribe
	public void slaveShowDecision(UIEvent.Decision e) {
		uiEventLogger.debug("!!! {} majority decision ({})", this.getOrigin(),
		        this.getParent().get().getClass().getSimpleName());
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
			this.getElement().callJsFunction("showDecisions", false, e.ref1, e.ref2, e.ref3);
			this.getElement().callJsFunction("setEnabled", false);
		});
	}

	@Subscribe
	public void slaveStartTimer(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			uiEventLogger.debug("!!! slaveStartTimer enable");
			this.getElement().callJsFunction("setEnabled", true);
		});
	}

	@Subscribe
	public void slaveStopTimer(UIEvent.StopTime e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			uiEventLogger.debug("!!! slaveStopTimer enable");
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
		return juryMode;
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		OwlcmsSession.withFop(fop -> {
			init(fop.getName());
			// we send on fopEventBus, listen on uiEventBus.
			fopEventBus = fop.getFopEventBus();
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}

	private void init(String fopName) {
		getElement().setProperty("fopName", fopName);
	}

	private void setJuryMode(boolean juryMode) {
		this.juryMode = juryMode;
	}
}
