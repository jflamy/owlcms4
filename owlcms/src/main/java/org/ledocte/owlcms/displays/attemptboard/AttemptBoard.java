/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Affero GNU License amended with the
 * Commons Clause.
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.displays.attemptboard;

import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.material.Material;

import ch.qos.logback.classic.Logger;

/**
 * The Class AttemptBoard.
 */
@SuppressWarnings("serial")
@Tag("attempt-board-template")
@HtmlImport("frontend://components/AttemptBoard.html")
@Route("displays/attemptBoard")
@Theme(value = Material.class, variant = Material.DARK)

public class AttemptBoard extends PolymerTemplate<TemplateModel> {
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(AttemptBoard.class);
	
	@Id("timer")
	private TimerElement timer; // instanciated by Flow during template instanciation
	@Id("decisions")
	private DecisionElement decisions; // instanciated by Flow during template instanciation


	/**
	 * Instantiates a new attempt board.
	 */
	public AttemptBoard() {
		setId("attempt-board-template");
		this.getElement().setProperty("interactive",true);
	}

	/**
	 * Dump element.
	 *
	 * @param element the element
	 */
	void dumpElement(Element element) {
		logger.debug(" attributes: " + element.getAttributeNames().collect(Collectors.joining(", ")));
		logger.debug(" properties: " + element.getPropertyNames().collect(Collectors.joining(", ")));
	}
	
	/**
	 * Down.
	 */
	@ClientCallable
	public void down() {
		logger.info("down signal shown");
	}

	/**
	 * Reset.
	 */
	public void reset() {
		this.getElement().callFunction("reset");
	}
}
