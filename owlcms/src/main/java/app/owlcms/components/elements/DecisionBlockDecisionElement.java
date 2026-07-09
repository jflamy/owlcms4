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

import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.DecisionReset;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;

@SuppressWarnings("serial")
public class DecisionBlockDecisionElement extends AbstractDecisionElement {
	final private static Logger logger = (Logger) LoggerFactory.getLogger(DecisionBlockDecisionElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	private UI ui;

	public DecisionBlockDecisionElement() {
		this.setJury(true);
		setShowsDownSignal(false);
		setLiveRefereeUpdates(false);
		setResetOnClockStart(true);
		// The results decision section owns a DecisionBlockState which is the single authority for
		// this element. Do not self-subscribe to FOP decision events, so there is no second writer
		// racing the state machine (this was the cause of stale referee lights in the ready state).
		setBusDriven(false);
		setDisplaySize("large");
		logger.debug("DecisionBlockDecisionElement constructor: fop={} isSingleRef={} {}",
		        (this.fop != null ? this.fop.getName() : "null"), this.isSingleRef(), LoggerUtils.whereFrom());
		getElement().setProperty("singleRef", this.isSingleRef());
		this.getElement().getStyle().set("font-size", "100%");
		doReset();
	}

	public void doReset() {
		logger.debug("DecisionBlockDecisionElement doReset: fop={} isSingleRef={} ui={} {}",
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

	@Subscribe
	public void slaveBreakStarted(UIEvent.BreakStarted e) {
		if (e.isDisplayToggle()) {
			return;
		}
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
		logger./**/warn("DecisionBlockDecisionElement onAttach: fop={} isSingleRef={} parent={} {}",
		        (this.fop != null ? this.fop.getName() : "null"), this.isSingleRef(),
		        this.getParent().map(p -> p.getClass().getSimpleName()).orElse("none"),
		        LoggerUtils.whereFrom());
		super.onAttach(attachEvent);
		if (this.fop == null) {
			logger./**/warn("No FOP available for DecisionBlockDecisionElement onAttach {}", LoggerUtils.whereFrom());
			return;
		}
		doReset();
	}

	@Override
	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		super.slaveDownSignal(e);
	}

	@Override
	@Subscribe
	public void slaveInitialDecision(UIEvent.InitialDecision e) {
		super.slaveInitialDecision(e);
	}

	@Subscribe
	public void slaveRefereeUpdate(UIEvent.RefereeUpdate e) {
		if (!isLiveRefereeUpdates()) {
			return;
		}
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
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
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			showDecisionLights(e.decision, e.ref1, e.ref2, e.ref3, e.isSingleLight(),
			        e.getInputKind() == app.owlcms.fieldofplay.InputKind.ANNOUNCER_ENTRY);
		});
	}

	@Override
	protected void onStartTimer(UIEvent.StartTime e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			getElement().setProperty("singleRef", this.isSingleRef());
			doReset();
			if (ui != null) {
				ui.push();
			}
		});
	}

	@Override
	public void showDecisionLights(UIEvent event, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3,
	        boolean singleLight, boolean announcerForced) {
		super.showDecisionLights(event, decision, ref1, ref2, ref3, singleLight, announcerForced);
		if (event instanceof UIEvent.RefereeUpdate refereeUpdate) {
			UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, event, this.getOrigin(), () -> {
				setDecisionTimes(intBox(refereeUpdate.ref1Time), intBox(refereeUpdate.ref2Time),
				        intBox(refereeUpdate.ref3Time));
			});
		}
	}

	private Integer intBox(Long ref1Time) {
		return ref1Time != null ? ref1Time.intValue() : null;
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
