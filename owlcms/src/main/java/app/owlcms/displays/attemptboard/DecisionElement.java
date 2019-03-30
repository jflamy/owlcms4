/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
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

import app.owlcms.init.OwlcmsSession;
import app.owlcms.state.FOPEvent;
import app.owlcms.state.UIEvent;
import app.owlcms.ui.group.UIEventProcessor;
import app.owlcms.ui.home.SafeEventBusRegistration;
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
		logger.setLevel(Level.DEBUG);
		uiEventLogger.setLevel(Level.DEBUG);
	}
	
	@ClientCallable
	public void masterReset() {
		logger.info("master reset");
		fopEventBus.post(new FOPEvent.DecisionReset(this.getUI().get()));
	}

	@ClientCallable
	public void masterShowDecisions(Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3) {
		logger.info("master decision={} ({} {} {})", decision, ref1, ref2, ref3);
		fopEventBus.post(new FOPEvent.RefereeDecision(this.getUI().get(), decision, ref1, ref2, ref3));
	}

	@ClientCallable
	public void masterShowDown(Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3) {
		logger.info("master down: decision={} ({} {} {})", decision, ref1, ref2, ref3);
		fopEventBus.post(new FOPEvent.DownSignal(this.getUI().get()));
	}
	
	@Subscribe
	public void slaveReset(UIEvent.DecisionReset e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, e.getOriginatingUI(), () -> {
			getElement().callFunction("reset", false);
		});
	}
	
	@Subscribe
	public void slaveShowDecisions(UIEvent.RefereeDecision e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, e.getOriginatingUI(), () -> {
			logger.info("{} referee decision ({})",this,this.getParent().get().getClass().getSimpleName());
//			getModel().setRef1(e.ref1);
//			getModel().setRef2(e.ref2);
//			getModel().setRef3(e.ref3);
			this.getElement().callFunction("showDecisions", false, e.ref1, e.ref2, e.ref3);
		});
	}
	
	@Subscribe
	public void slaveShowDown(UIEvent.DownSignal e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, e.getOriginatingUI(), () -> {
			this.getElement().callFunction("showDown", false);
		});
	}
	
	
	private void init() {
		DecisionModel model = getModel();
		model.setRef1(null);
		model.setRef2(null);
		model.setRef3(null);

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
			fopEventBus = fop.getEventBus();
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}
	

}
