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
package com.vaadin.flow.demo.helloworld.template;

import java.util.stream.Collectors;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;

/**
 * The main application view of the todo application.
 */
@Tag("todo-template")
@HtmlImport("frontend://components/TodoTemplate.html")
@Route("")
public class TodoList extends PolymerTemplate<TemplateModel> {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id("creator")
    private TodoCreator creator;

    /**
     * Creates the todo list applicaton base.
     */
    public TodoList() {
        setId("todo-template");

        creator.addCreateCallback(todo -> addNewTodoItem(todo));
    }

    private void addNewTodoItem(Todo todo) {
        TodoElement todoElement = new TodoElement(todo);

        todoElement.getElement().addEventListener("remove",
                e -> getElement().removeChild(todoElement.getElement()));

        todoElement.addStateChangeListener(() -> {
            if (todoElement.isCompleted()) {
                // ensure that DOM element will show on the left hand side
                Element element = todoElement.getElement();
                //dumpElement(element);
                // the parent renders the elements with slot="done" in its "done" slot
				element.setAttribute("slot", "done");
            }
        });

        getElement().appendChild(todoElement.getElement());
    }

	void dumpElement(Element element) {
		System.err.println(" attributes: "+element.getAttributeNames().collect(Collectors.joining(", ")));
		System.err.print(" properties: "+element.getPropertyNames().collect(Collectors.joining(", ")));
	}

}
