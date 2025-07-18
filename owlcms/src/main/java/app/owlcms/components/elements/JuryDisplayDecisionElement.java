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

import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
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

	private UI ui;

	public JuryDisplayDecisionElement() {
		this.setJury(true);
		getElement().setProperty("singleRef", this.isSingleRef());
		this.getElement().getStyle().set("font-size", "100%");
		doReset();
	}

	public JuryDisplayDecisionElement(boolean b) {
		this();
	}

	public void doReset() {
		this.getElement().callJsFunction("reset", false);
		getElement().setProperty("singleRef", this.isSingleRef());
		if (this.isSingleRef()) {
			this.getElement().callJsFunction("showSingleDecisionForJury", (Boolean) null);
		} else {
			this.getElement().callJsFunction("showDecisionsForJury", (Boolean) null, (Boolean) null, (Boolean) null,
			        0, 0, 0);
		}
		UI.getCurrent().push();
	}

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		OwlcmsSession.withFop((fop) -> {
			UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
				uiEventLogger.debug("*** {} break start -> reset", this.getOrigin());
				doReset();
			});
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
		OwlcmsSession.withFop((fop) -> {
			if (fop.getBreakType() != BreakType.JURY) {
				// don't reset on a break we just created !
				UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
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

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		ui = UI.getCurrent();
		super.onAttach(attachEvent);
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
			this.getElement().callJsFunction("showDown", false,
			        isSilenced() || OwlcmsSession.getFop().isEmitSoundsOnServer());
			getElement().setProperty("singleRef", this.isSingleRef());
		});
	}

	@Subscribe
	public void slaveRefereeUpdate(UIEvent.RefereeUpdate e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			// logger.debug("{} referee update ({} {} {})", this.getOrigin(), e.ref1, e.ref2, e.ref3);
			getElement().setProperty("singleRef", this.isSingleRef());
			if (e.isSingleReferee()) {
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
		//logger.debug("decision {} {} {} --- {}", e.ref1, e.ref2, e.ref3, e.isSingleReferee());
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			if (e.isSingleReferee()) {
				getElement().setProperty("singleRef", e.isSingleReferee());
				this.getElement().callJsFunction("showSingleDecision", e.decision);
			} else {
				getElement().setProperty("singleRef", e.isSingleReferee());
				this.getElement().callJsFunction("showDecisions", false, e.ref1, e.ref2, e.ref3);
			}
		});
	}

	@Subscribe
	public void slaveStartTime(UIEvent.StartTime e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			getElement().setProperty("singleRef", this.isSingleRef());
			doReset();
			UI.getCurrent().push();
		});
	}

	private Integer intBox(Long ref1Time) {
		return ref1Time != null ? ref1Time.intValue() : null;
	}

}
