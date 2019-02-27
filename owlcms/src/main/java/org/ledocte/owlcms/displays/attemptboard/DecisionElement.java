/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.displays.attemptboard;

import java.util.Optional;

import org.ledocte.owlcms.init.OwlcmsSession;
import org.ledocte.owlcms.state.FOPEvent;
import org.ledocte.owlcms.state.UIEvent;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.templatemodel.TemplateModel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Decision display element.
 */
@SuppressWarnings("serial")
@Tag("decision-element")
@HtmlImport("frontend://components/DecisionElement.html")
public class DecisionElement extends PolymerTemplate<DecisionElement.DecisionModel> {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(DecisionElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("owlcms.uiEventLogger");
	static {
		logger.setLevel(Level.DEBUG);
		uiEventLogger.setLevel(Level.DEBUG);
	}
	
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
		 * @param ref1 decision
		 */
		void setRef1(Boolean decision);

		/**
		 *  Ref2 decision
		 *
		 * @return true if accepted, false if rejected, null if no decision
		 */
		Boolean isRef2();

		/**
		 * @param ref2 decision
		 */
		void setRef2(Boolean decision);

		/**
		 *  Ref3 decision
		 *
		 * @return true if accepted, false if rejected, null if no decision
		 */
		Boolean isRef3();

		/**
		 * @param ref1 decision
		 */
		void setRef3(Boolean decision);
	}

	private EventBus uiEventBus;
	private EventBus fopEventBus;
	
	public DecisionElement() {
	}

	/**
	 * Reset.
	 */
	public void reset() {
		getElement().callFunction("reset");
	}

	/**
	 * Decision made.
	 *
	 * @param decision majority decision
	 */
	@ClientCallable
	public void decisionMade(boolean decision) {
		logger.info("decision made " + decision);
	}
	
	/* @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent) */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		init();
		OwlcmsSession.withFop(fop -> {
			fopEventBus = fop.getEventBus();
			uiEventBus = fop.getUiEventBus();
			// we listen on uiEventBus.
			uiEventBus.register(this);
		});
	}

	private void init() {
		DecisionModel model = getModel();
		model.setRef1(null);
		model.setRef2(null);
		model.setRef3(null);

		Element elem = this.getElement();
		elem.addPropertyChangeListener("ref1", "ref1-changed", (e) -> {
			logger.info(e.getPropertyName() + " changed to " + e.getValue());
		});
		elem.addPropertyChangeListener("ref2", "ref2-changed", (e) -> {
			logger.info(e.getPropertyName() + " changed to " + e.getValue());
		});
		elem.addPropertyChangeListener("ref3", "ref3-changed", (e) -> {
			logger.info(e.getPropertyName() + " changed to " + e.getValue());
		});
		elem.addPropertyChangeListener("decision", "decision-changed", (e) -> {
			logger.info(e.getPropertyName() + " changed to " + e.getValue());
		});
	}

	/* @see com.vaadin.flow.component.Component#onDetach(com.vaadin.flow.component.DetachEvent) */
	@Override
	protected void onDetach(DetachEvent detachEvent) {
		try {uiEventBus.unregister(this);} catch (Exception e) {}
		try {fopEventBus.unregister(this);} catch (Exception e) {}
	}
	
	@Subscribe
	public void resetDecision(UIEvent.DecisionReset e) {
		Optional<UI> ui2 = this.getUI();
		if (ui2.isPresent()) {
			ui2.get().access(() -> {
				reset();
			});
		} else {
			uiEventLogger.debug(">>> start detached, unregistering", e);
			uiEventBus.unregister(this);
		}
	}
	
	@ClientCallable
	public void decisionsVisible(Boolean decision) {
		fopEventBus.post(new FOPEvent.RefereeDecision(this.getUI().get(),decision));
	}
}
