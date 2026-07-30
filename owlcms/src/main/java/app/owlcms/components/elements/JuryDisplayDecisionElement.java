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

import tools.jackson.databind.node.NullNode;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.DecisionReset;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class JuryDisplayDecisionElement extends AbstractDecisionElement {
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JuryDisplayDecisionElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	private UI ui;

	public JuryDisplayDecisionElement() {
		this.setJury(true);
		setShowsDownSignal(false);
		setLiveRefereeUpdates(true);
		setResetOnClockStart(true);
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
		logger.debug("JuryDisplayDecisionElement doReset: fop={} isSingleRef={} ui={} {}",
		        (this.fop != null ? this.fop.getName() : "null"), this.isSingleRef(),
		        (ui != null ? "set" : "null"), LoggerUtils.whereFrom());
		getElement().setProperty("singleRef", this.isSingleRef());
		getElement().setProperty("jury", true);
		clearDecisionProperties(true);
		setDecisionTimes(0L, 0L, 0L);
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
		logger.debug("{}JuryDisplayDecisionElement onAttach: fop={} isSingleRef={} parent={} {}",
		        FieldOfPlay.getLoggingName(this.fop), (this.fop != null ? this.fop.getName() : "null"), this.isSingleRef(),
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
			// logger.debug("{} referee update ({} {} {})", this.getOrigin(), e.ref1, e.ref2, e.ref3);
			getElement().setProperty("singleRef", e.isSingleLight());
			getElement().setProperty("jury", true);
			setDecisionProperties(e.isSingleLight() ? e.ref2 : computeGoodLift(e.ref1, e.ref2, e.ref3, false),
			        e.ref1, e.ref2, e.ref3, e.isSingleLight(), false);
			setDecisionTimes(e.ref1Time, e.ref2Time, e.ref3Time);
		});
	}

	@Override
	@Subscribe
	public void slaveShowDecision(UIEvent.Decision e) {
		//logger.debug("decision {} {} {} --- {}", e.ref1, e.ref2, e.ref3, e.isSingleLight());
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
				setDecisionTimes(refereeUpdate.ref1Time, refereeUpdate.ref2Time, refereeUpdate.ref3Time);
			});
		}
	}

	private void setDecisionTimes(Long ref1Time, Long ref2Time, Long ref3Time) {
		setNullableLongProperty("ref1Time", ref1Time);
		setNullableLongProperty("ref2Time", ref2Time);
		setNullableLongProperty("ref3Time", ref3Time);
	}

	private void setNullableLongProperty(String propertyName, Long value) {
		if (value == null) {
			getElement().setPropertyJson(propertyName, NullNode.instance);
		} else {
			getElement().setProperty(propertyName, value.doubleValue());
		}
	}

}
