/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.ledocte.owlcms.ui.attemptboard;

import java.util.stream.Collectors;

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

/**
 * The main application view of the todo application.
 */
@Tag("attempt-board-template")
@HtmlImport("frontend://components/AttemptBoard.html")
@Route("")
@Theme(value = Material.class, variant = Material.DARK)

public class AttemptBoard extends PolymerTemplate<TemplateModel> {

	private static final long serialVersionUID = 1L;
	
	@Id("timer")
	private TimerElement timer; // instanciated by Flow during template instanciation
	@Id("decisions")
	private DecisionElement decisions; // instanciated by Flow during template instanciation

	/**
	 * Creates the todo list applicaton base.
	 */
	public AttemptBoard() {
		setId("attempt-board-template");
		this.getElement().setProperty("interactive",true);
	}

	void dumpElement(Element element) {
		System.err.println(" attributes: " + element.getAttributeNames().collect(Collectors.joining(", ")));
		System.err.print(" properties: " + element.getPropertyNames().collect(Collectors.joining(", ")));
	}
	
	@ClientCallable
	public void down() {
		System.err.println("down signal shown");
	}

	public void reset() {
		this.getElement().callFunction("reset");
	}
}
