/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;

import app.owlcms.fieldofplay.InputKind;
import app.owlcms.fieldofplay.TimingPolicy;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.DecisionReset;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;

@SuppressWarnings("serial")
public class JuryDisplayDecisionElement extends DecisionElement {
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JuryDisplayDecisionElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	private UI ui;
	private boolean finalOnly = false;
	private boolean liveRefereeUpdates = true;
	private boolean liveRefereeUpdatesDuringImmediateWindowOnly = false;
	private boolean immediateWindowActive = false;

	public JuryDisplayDecisionElement() {
		this.setJury(true);
		setDisplaySize("large");
		logger.debug("JuryDisplayDecisionElement constructor: fop={} isSingleRef={} {}",
		        (this.fop != null ? this.fop.getName() : "null"), this.isSingleRef(), LoggerUtils.whereFrom());
		getElement().setProperty("singleRef", this.isSingleRef());
		this.getElement().getStyle().set("font-size", "100%");
		doReset();
	}

	public JuryDisplayDecisionElement(boolean b) {
		this();
	}

	public void doReset() {
		this.immediateWindowActive = false;
		logger.debug("JuryDisplayDecisionElement doReset: fop={} isSingleRef={} ui={} {}",
		        (this.fop != null ? this.fop.getName() : "null"), this.isSingleRef(),
		        (ui != null ? "set" : "null"), LoggerUtils.whereFrom());
		getElement().setProperty("singleRef", this.isSingleRef());
		getElement().setProperty("jury", true);
		clearDecisionProperties(true);
		setDecisionTimes(0, 0, 0);
		if (ui != null) {
			ui.push();
		}
	}

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			uiEventLogger.debug("{} break start -> reset", this.getOrigin());
			doReset();
		});
	}

	// @Override
	// public void slaveDownSignal(DownSignal e) {
	// // ignore
	// }

	@Subscribe
	public void slaveBreakStarted(UIEvent.BreakStarted e) {
		if (e.isDisplayToggle()) {
			return;
		}
		// Only reset if this is not a jury deliberation or challenge break: those
		// deliberate about the decision currently shown, which must stay visible.
		if (this.fop != null && this.fop.getBreakType() != BreakType.JURY
		        && this.fop.getBreakType() != BreakType.CHALLENGE) {
			UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
				uiEventLogger.debug("{} break start -> reset", this.getOrigin());
				doReset();
			});
		}
	}

	@Override
	public void slaveDecisionReset(DecisionReset e) {
		// ignore
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		ui = UI.getCurrent();
		logger./**/warn("JuryDisplayDecisionElement onAttach: fop={} isSingleRef={} parent={} {}",
		        (this.fop != null ? this.fop.getName() : "null"), this.isSingleRef(),
		        this.getParent().map(p -> p.getClass().getSimpleName()).orElse("none"),
		        LoggerUtils.whereFrom());
		super.onAttach(attachEvent);
		if (this.fop == null) {
			logger./**/warn("No FOP available for JuryDisplayDecisionElement onAttach {}", LoggerUtils.whereFrom());
			return;
		}
		doReset();
	}

	@Override
	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		if (this.finalOnly) {
			return;
		}
		markDownSignalVisible();
		// logger.debug("jury slaveDownSignal {} {} {} {}", this, this.getOrigin(), e.getOrigin(), isSilenced());
		if (isSilenced()) {
			// we emitted the down signal, don't do it again.
			UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
				getElement().setProperty("singleRef", this.isSingleRef());
			});
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			uiEventLogger.debug("!!! {} down ({})", this.getOrigin(),
			        this.getParent().get().getClass().getSimpleName());
			boolean emitSoundsOnServer = (this.fop != null && this.fop.isEmitSoundsOnServer());
			getElement().setProperty("singleRef", this.isSingleRef());
			showDownSignal(isSilenced() || emitSoundsOnServer);
		});
	}

	@Override
	@Subscribe
	public void slaveInitialDecision(UIEvent.InitialDecision e) {
		if (this.finalOnly) {
			// Wait for the full decision; do not show the partial (majority-based) decision.
			return;
		}
		if (e.getTimingPolicy() == TimingPolicy.IMMEDIATE) {
			this.immediateWindowActive = true;
			showImmediateDecisionAfterMinimumDownSignal(e, e.getInputKind() == InputKind.ANNOUNCER_ENTRY);
			return;
		}
		this.immediateWindowActive = false;
		super.slaveInitialDecision(e);
	}

	@Subscribe
	public void slaveRefereeUpdate(UIEvent.RefereeUpdate e) {
		if (!this.liveRefereeUpdates) {
			return;
		}
		if (this.liveRefereeUpdatesDuringImmediateWindowOnly && !this.immediateWindowActive) {
			return;
		}
		if (this.downSignalHoldPending) {
			return;
		}
		if (this.finalOnly) {
			return;
		}
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			if (this.downSignalHoldPending) {
				return;
			}
			// logger.debug("{} referee update ({} {} {})", this.getOrigin(), e.ref1, e.ref2, e.ref3);
			getElement().setProperty("singleRef", e.isSingleLight());
			getElement().setProperty("jury", true);
			setDecisionProperties(e.isSingleLight() ? e.ref2 : computeGoodLift(e.ref1, e.ref2, e.ref3, false),
			        e.ref1, e.ref2, e.ref3, e.isSingleLight(), false);
			setDecisionTimes(intBox(e.ref1Time), intBox(e.ref2Time), intBox(e.ref3Time));
		});
	}

	@Override
	@Subscribe
	public void slaveShowDecision(UIEvent.Decision e) {
		this.immediateWindowActive = false;
		this.downSignalHoldPending = false;
		//logger.debug("decision {} {} {} --- {}", e.ref1, e.ref2, e.ref3, e.isSingleLight());
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			showDecisionLights(e.decision, e.ref1, e.ref2, e.ref3, e.isSingleLight(),
			        e.getInputKind() == InputKind.ANNOUNCER_ENTRY);
		});
	}

	@Override
	protected void onStartTimer(UIEvent.StartTime e) {
		this.immediateWindowActive = false;
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			getElement().setProperty("singleRef", this.isSingleRef());
			doReset();
			if (ui != null) {
				ui.push();
			}
		});
	}

	private Integer intBox(Long ref1Time) {
		return ref1Time != null ? ref1Time.intValue() : null;
	}

	public void setFinalOnly(boolean finalOnly) {
		this.finalOnly = finalOnly;
	}

	public void setLiveRefereeUpdates(boolean liveRefereeUpdates) {
		this.liveRefereeUpdates = liveRefereeUpdates;
	}

	public void setLiveRefereeUpdatesDuringImmediateWindowOnly(boolean liveRefereeUpdatesDuringImmediateWindowOnly) {
		this.liveRefereeUpdatesDuringImmediateWindowOnly = liveRefereeUpdatesDuringImmediateWindowOnly;
	}

	@Override
	protected boolean isImmediatePreviewWindowActive() {
		return this.immediateWindowActive;
	}

	private void setDecisionTimes(Integer ref1Time, Integer ref2Time, Integer ref3Time) {
		setNullableIntegerProperty("ref1Time", ref1Time);
		setNullableIntegerProperty("ref2Time", ref2Time);
		setNullableIntegerProperty("ref3Time", ref3Time);
	}

	private void setNullableIntegerProperty(String propertyName, Integer value) {
		if (value == null) {
			getElement().setPropertyJson(propertyName, Json.createNull());
		} else {
			getElement().setProperty(propertyName, value.intValue());
		}
	}

}
