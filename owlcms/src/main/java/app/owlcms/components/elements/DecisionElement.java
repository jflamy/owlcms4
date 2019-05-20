/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.templatemodel.TemplateModel;

import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Decision display element.
 */
@SuppressWarnings("serial")
@Tag("decision-element")
@HtmlImport("frontend://components/DecisionElement.html")
public class DecisionElement extends PolymerTemplate<DecisionElement.DecisionModel> implements SafeEventBusRegistration {
	
	/**
	 * The Interface DecisionModel.
	 */
	public interface DecisionModel extends TemplateModel {
		
		/**
		 *  Ref1 decision time
		 *
		 * @return time at which decision was made by ref (ms), null if no decision
		 */
		Integer getRef1Time();
		
		/**
		 *  Ref2 decision time
		 *
		 * @return time at which decision was made by ref (ms), null if no decision
		 */
		Integer getRef2Time();

		/**
		 *  Ref3 decision time
		 *
		 * @return time at which decision was made by ref (ms), null if no decision
		 */
		Integer getRef3Time();

		/**
		 * @return true if good lift, false if not, null if no majority yet
		 */
		Boolean isDecision();
		
		/**
		 * @return true if operating in Jury mode (display immediately, no down signal)
		 */
		Boolean isJury();

		Boolean isPublicFacing();
		
		/**
		 *  Ref1 decision
		 *
		 * @return true if accepted, false if rejected, null if no decision
		 */
		Boolean isRef1();

		/**
		 *  Ref2 decision
		 *
		 * @return true if accepted, false if rejected, null if no decision
		 */
		Boolean isRef2();

		/**
		 *  Ref3 decision
		 *
		 * @return true if accepted, false if rejected, null if no decision
		 */
		Boolean isRef3();
		
		/**
		 * @param isGood true if good lift, false if no lift, null if no decision yet.
		 */
		void setDecision(Boolean isGood);
		
		/**
		 * @param juryMode
		 */
		void setJury(Boolean juryMode);

		void setPublicFacing(Boolean publicFacing);

		/**
		 * @param ref1 decision
		 */
		void setRef1(Boolean decision);
		

		/**
		 * @param ms time at which decision made
		 */
		void setRef1Time(Integer ms);
		
		/**
		 * @param ref2 decision
		 */
		void setRef2(Boolean decision);
		
		/**
		 *  Ref1 decision time
		 *
		 */
		void setRef2Time(Integer ms);
		
		/**
		 * @param ref1 decision
		 */
		void setRef3(Boolean decision);
		/**
		 *  Ref3 decision time
		 *
		 */
		void setRef3Time(Integer ms);
	}

	final private static Logger logger = (Logger) LoggerFactory.getLogger(DecisionElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	
	private EventBus uiEventBus;
	private EventBus fopEventBus;
	
	public DecisionElement() {
	}
	
	public boolean isPublicFacing() {
		return Boolean.TRUE.equals(getModel().isPublicFacing());
	}

	@ClientCallable
	public void masterDecisionsUpdated() {
		logger.debug("master decisions updated");
		Object origin = this.getOrigin();
		DecisionModel model = this.getModel();
		OwlcmsSession.withFop((fop) -> {
			logger.info("{} decision={} ({} {} {})", fop.getCurAthlete(), model.isRef1(), model.isRef2(), model.isRef3(), model.getRef1Time(), model.getRef2Time(), model.getRef3Time());
			fopEventBus.post(new FOPEvent.RefereeUpdate(origin, fop.getCurAthlete(), model.isDecision(), model.isRef1(), model.isRef2(), model.isRef3(), model.getRef1Time(), model.getRef2Time(), model.getRef3Time()));
		});

	}

	@ClientCallable
	public void masterReset() {
		logger.debug("master reset");
		fopEventBus.post(new FOPEvent.DecisionReset(this.getOrigin()));
	}
	
	@ClientCallable
	public void masterShowDecisions(Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3) {
		Object origin = this.getOrigin();
		OwlcmsSession.withFop((fop) -> {
			logger.info("{} decision={} ({} {} {})", fop.getCurAthlete(), decision, ref1, ref2, ref3);
			fopEventBus.post(new FOPEvent.RefereeDecision(fop.getCurAthlete(), origin, decision, ref1, ref2, ref3));
		});
	}
	
	@ClientCallable
	public void masterShowDown(Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3) {
		Object origin = this.getOrigin();
		logger.debug("=== master {} down: decision={} ({} {} {})", origin, decision.getClass().getSimpleName(), ref1, ref2, ref3);
		fopEventBus.post(new FOPEvent.DownSignal(origin));
	}
	
	public void setJury(boolean juryMode) {
		getModel().setJury(juryMode);
	}

	public void setPublicFacing(boolean publicFacing) {
		getModel().setPublicFacing(publicFacing);
	}
	
	@Subscribe
	public void slaveReset(UIEvent.DecisionReset e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
			getElement().callFunction("reset", false);
		});
	}
	
	
	@Subscribe
	public void slaveShowDecisions(UIEvent.RefereeDecision e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
			uiEventLogger.debug("*** {} referee decision ({})",this.getOrigin(),this.getParent().get().getClass().getSimpleName());
			this.getElement().callFunction("showDecisions", false, e.ref1, e.ref2, e.ref3);
		});
	}
	
	@Subscribe
	public void slaveShowDown(UIEvent.DownSignal e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
			uiEventLogger.debug("!!! {} down ({})",this.getOrigin(),this.getParent().get().getClass().getSimpleName());
			this.getElement().callFunction("showDown", false);
		});
	}
	
	/* @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent) */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		init();
		OwlcmsSession.withFop(fop -> {
			// we send on fopEventBus, listen on uiEventBus.
			fopEventBus = fop.getFopEventBus();
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}
	
	private Object getOrigin() {
		// we use the identity of our parent AttemptBoard or AthleteFacingAttemptBoard to identify
		// our actions.
		return this.getParent().get();
	}
	
	private void init() {
		DecisionModel model = getModel();
		model.setRef1(null);
		model.setRef2(null);
		model.setRef3(null);
		model.setPublicFacing(true);

		Element elem = this.getElement();
		elem.addPropertyChangeListener("ref1", "ref1-changed", (e) -> {
			uiEventLogger.trace(e.getPropertyName() + " changed to " + e.getValue());
		});
		elem.addPropertyChangeListener("ref2", "ref2-changed", (e) -> {
			uiEventLogger.trace(e.getPropertyName() + " changed to " + e.getValue());
		});
		elem.addPropertyChangeListener("ref3", "ref3-changed", (e) -> {
			uiEventLogger.trace(e.getPropertyName() + " changed to " + e.getValue());
		});
		elem.addPropertyChangeListener("decision", "decision-changed", (e) -> {
			uiEventLogger.debug(e.getPropertyName() + " changed to " + e.getValue());
		});
	}
}
