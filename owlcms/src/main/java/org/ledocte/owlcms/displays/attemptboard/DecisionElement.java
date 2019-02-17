/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Affero GNU License amended with the
 * Commons Clause.
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.displays.attemptboard;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.templatemodel.TemplateModel;

import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
@SuppressWarnings("serial")
@Tag("decision-element")
@HtmlImport("frontend://components/DecisionElement.html")
public class DecisionElement extends PolymerTemplate<DecisionElement.DecisionModel> {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(DecisionElement.class);

	/**
	 * The Interface DecisionModel.
	 */
	public interface DecisionModel extends TemplateModel {
		
		/**
		 * Checks if is ref 1.
		 *
		 * @return the boolean
		 */
		Boolean isRef1();

		/**
		 * Sets the ref 1.
		 *
		 * @param running the new ref 1
		 */
		void setRef1(Boolean running);

		/**
		 * Checks if is ref 2.
		 *
		 * @return the boolean
		 */
		Boolean isRef2();

		/**
		 * Sets the ref 2.
		 *
		 * @param running the new ref 2
		 */
		void setRef2(Boolean running);

		/**
		 * Checks if is ref 3.
		 *
		 * @return the boolean
		 */
		Boolean isRef3();

		/**
		 * Sets the ref 3.
		 *
		 * @param running the new ref 3
		 */
		void setRef3(Boolean running);
	}

	/**
	 * Instantiates a new decision element.
	 */
	public DecisionElement() {
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

	/**
	 * Reset.
	 */
	public void reset() {
		getElement().callFunction("reset");
	}

	/**
	 * Decision made.
	 *
	 * @param decision the decision
	 */
	@ClientCallable
	public void decisionMade(boolean decision) {
		logger.info("decision made " + decision);
	}
}
