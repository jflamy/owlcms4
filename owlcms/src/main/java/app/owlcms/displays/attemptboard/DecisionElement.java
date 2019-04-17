/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Non-Profit Open Software License ("Non-Profit OSL") 3.0 
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.displays.attemptboard;

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
import app.owlcms.ui.group.UIEventProcessor;
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

		void setPublicFacing(Boolean publicFacing);
		
		/**
		 * @param ref1 decision
		 */
		void setRef1(Boolean decision);

		/**
		 * @param ref2 decision
		 */
		void setRef2(Boolean decision);

		/**
		 * @param ref1 decision
		 */
		void setRef3(Boolean decision);
	}
	final private static Logger logger = (Logger) LoggerFactory.getLogger(DecisionElement.class);
	
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());

	private EventBus uiEventBus;
	private EventBus fopEventBus;
	
	public DecisionElement() {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	
	@ClientCallable
	public void masterReset() {
		logger.info("master reset");
		fopEventBus.post(new FOPEvent.DecisionReset(this.getOrigin()));
	}

	@ClientCallable
	public void masterShowDecisions(Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3) {
		Object origin = this.getOrigin();
		logger.info("+++ master {} decision={} ({} {} {})", origin, decision, ref1, ref2, ref3);
		fopEventBus.post(new FOPEvent.RefereeDecision(origin, decision, ref1, ref2, ref3));
	}

	@ClientCallable
	public void masterShowDown(Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3) {
		Object origin = this.getOrigin();
		logger.info("=== master {} down: decision={} ({} {} {})", origin, decision, ref1, ref2, ref3);
		fopEventBus.post(new FOPEvent.DownSignal(origin));
	}
	
	@Subscribe
	public void slaveReset(UIEvent.DecisionReset e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
			getElement().callFunction("reset", false);
		});
	}
	
	private Object getOrigin() {
		// we use the identity of our parent AttemptBoard or AthleteFacingAttemptBoard to identify
		// our actions.
		return this.getParent().get();
	}

	@Subscribe
	public void slaveShowDecisions(UIEvent.RefereeDecision e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
			logger.debug("*** {} referee decision ({})",this.getOrigin(),this.getParent().get().getClass().getSimpleName());
			this.getElement().callFunction("showDecisions", false, e.ref1, e.ref2, e.ref3);
		});
	}
	
	@Subscribe
	public void slaveShowDown(UIEvent.DownSignal e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
			logger.debug("!!! {} down ({})",this.getOrigin(),this.getParent().get().getClass().getSimpleName());
			this.getElement().callFunction("showDown", false);
		});
	}
	
	
	private void init() {
		DecisionModel model = getModel();
		model.setRef1(null);
		model.setRef2(null);
		model.setRef3(null);
		model.setPublicFacing(true);

		Element elem = this.getElement();
		elem.addPropertyChangeListener("ref1", "ref1-changed", (e) -> {
			logger.trace(e.getPropertyName() + " changed to " + e.getValue());
		});
		elem.addPropertyChangeListener("ref2", "ref2-changed", (e) -> {
			logger.trace(e.getPropertyName() + " changed to " + e.getValue());
		});
		elem.addPropertyChangeListener("ref3", "ref3-changed", (e) -> {
			logger.trace(e.getPropertyName() + " changed to " + e.getValue());
		});
		elem.addPropertyChangeListener("decision", "decision-changed", (e) -> {
			logger.info(e.getPropertyName() + " changed to " + e.getValue());
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
	
	public void setPublicFacing(boolean publicFacing) {
		getModel().setPublicFacing(publicFacing);
	}
	
	public boolean isPublicFacing() {
		return Boolean.TRUE.equals(getModel().isPublicFacing());
	}
	
}
