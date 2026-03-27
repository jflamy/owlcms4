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

@SuppressWarnings("serial")
public class JuryDisplayDecisionElement extends DecisionElement {
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JuryDisplayDecisionElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	private UI ui;

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
		logger.debug("JuryDisplayDecisionElement doReset: fop={} isSingleRef={} ui={} {}",
		        (this.fop != null ? this.fop.getName() : "null"), this.isSingleRef(),
		        (ui != null ? "set" : "null"), LoggerUtils.whereFrom());
		this.getElement().callJsFunction("reset", false);
		getElement().setProperty("singleRef", this.isSingleRef());
		if (this.isSingleRef()) {
			this.getElement().callJsFunction("showSingleDecisionForJury", (Boolean) null);
		} else {
			this.getElement().callJsFunction("showDecisionsForJury", (Boolean) null, (Boolean) null, (Boolean) null,
			        0, 0, 0);
		}
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
		// Only reset if this is not a jury break that we just created
		if (this.fop != null && this.fop.getBreakType() != BreakType.JURY) {
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
			this.getElement().callJsFunction("showDown", false,
			        isSilenced() || emitSoundsOnServer);
			getElement().setProperty("singleRef", this.isSingleRef());
		});
	}

	@Subscribe
	public void slaveRefereeUpdate(UIEvent.RefereeUpdate e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			// logger.debug("{} referee update ({} {} {})", this.getOrigin(), e.ref1, e.ref2, e.ref3);
			getElement().setProperty("singleRef", e.isSingleLight());
			if (e.isSingleLight()) {
				this.getElement().callJsFunction("showSingleDecisionForJury", e.ref2);
			} else {
				this.getElement().callJsFunction("showDecisionsForJury", e.ref1, e.ref2, e.ref3,
				        intBox(e.ref1Time),
				        intBox(e.ref2Time),
				        intBox(e.ref3Time));
			}
		});
	}

	@Override
	@Subscribe
	public void slaveShowDecision(UIEvent.Decision e) {
		//logger.debug("decision {} {} {} --- {}", e.ref1, e.ref2, e.ref3, e.isSingleLight());
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			if (e.isSingleLight()) {
				getElement().setProperty("singleRef", e.isSingleLight());
				this.getElement().callJsFunction("showSingleDecision", e.decision);
			} else {
				getElement().setProperty("singleRef", e.isSingleLight());
				this.getElement().callJsFunction("showDecisions", false, e.ref1, e.ref2, e.ref3);
			}
		});
	}

	@Subscribe
	public void slaveStartTime(UIEvent.StartTime e) {
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

}
