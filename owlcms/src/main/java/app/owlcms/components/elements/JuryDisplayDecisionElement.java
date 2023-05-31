/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.Decision;
import app.owlcms.uievents.UIEvent.DecisionReset;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class JuryDisplayDecisionElement extends DecisionElement {
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JuryDisplayDecisionElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	private boolean automaticReset;

	public JuryDisplayDecisionElement() {
		super();
		this.setJury(true);
		this.getElement().getStyle().set("font-size", "19vh");
	}

	public JuryDisplayDecisionElement(boolean b) {
		this();
		this.setAutomaticReset(b);
	}

	public void doReset() {
		this.getElement().callJsFunction("reset", false);
	}

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		OwlcmsSession.withFop((fop) -> {
			UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
				uiEventLogger.debug("*** {} break start -> reset", this.getOrigin());
				doReset();
			});
		});
	}

//    @Override
//    public void slaveDownSignal(DownSignal e) {
//        // ignore
//    }

	@Subscribe
	public void slaveBreakStarted(UIEvent.BreakStarted e) {
		if (e.isDisplayToggle()) {
			return;
		}
		OwlcmsSession.withFop((fop) -> {
			if (fop.getBreakType() != BreakType.JURY) {
				// don't reset on a break we just created !
				UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
					uiEventLogger.debug("*** {} break start -> reset", this.getOrigin());
					doReset();
				});
			}
		});
	}

	@Override
	public void slaveDecisionReset(DecisionReset e) {
		// ignore
	}

	@Subscribe
	public void slaveRefereeUpdate(UIEvent.RefereeUpdate e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
			uiEventLogger.debug("*** {} referee update ({} {} {})", this.getOrigin(), e.ref1, e.ref2, e.ref3);
			this.getElement().callJsFunction("showDecisionsForJury", e.ref1, e.ref2, e.ref3, intBox(e.ref1Time),
			        intBox(e.ref2Time),
			        intBox(e.ref3Time));
		});
	}

	@Override
	public void slaveShowDecision(Decision e) {
		// ignore
	}

	@Subscribe
	public void slaveStartTime(UIEvent.StartTime e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
			uiEventLogger.debug("*** {} startTime -> reset", this.getOrigin());
			if (isAutomaticReset()) {
				doReset();
			}
		});
	}

	private Integer intBox(Long ref1Time) {
		return ref1Time != null ? ref1Time.intValue() : null;
	}

	private boolean isAutomaticReset() {
		return automaticReset;
	}

	private void setAutomaticReset(boolean automaticReset) {
		this.automaticReset = automaticReset;
	}
	
	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		//logger.debug("jury slaveDownSignal {} {} {} {}", this, this.getOrigin(), e.getOrigin(), isSilenced());
		if (isSilenced() 
				//&& (isJuryMode() || (this.getOrigin() == e.getOrigin()))
				) {
			// we emitted the down signal, don't do it again.
			// logger.trace("skipping down, {} is origin",this.getOrigin());
			return;
		}
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			uiEventLogger.debug("!!! {} down ({})", this.getOrigin(),
			        this.getParent().get().getClass().getSimpleName());
			this.getElement().callJsFunction("showDown", false,
			        isSilenced() || OwlcmsSession.getFop().isEmitSoundsOnServer());
		});
	}

}
