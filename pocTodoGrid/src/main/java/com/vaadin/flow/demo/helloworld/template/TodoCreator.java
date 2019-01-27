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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;

/**
 * Creator layout for creating todo items.
 */
@Tag("todo-creator")
@HtmlImport("frontend://components/TodoCreator.html")
public class TodoCreator extends PolymerTemplate<TemplateModel> {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Random rand = new Random(System.currentTimeMillis());
    private List<CreateCallback> callbacks = new ArrayList<>(0);

    /**
     * Add a creation callback to listen to for newly created todo items.
     * 
     * @param callback
     *            creation callback
     */
    public void addCreateCallback(CreateCallback callback) {
        callbacks.add(callback);
    }

    @ClientCallable
    private void createTodo(String task, String user) {
        Todo todo = new Todo();
        todo.setTask(task);
        todo.setUser(user);
        todo.setTime(LocalDateTime.now());
        todo.setCompleted(false);
        todo.setRid(rand.nextInt());

        callbacks.forEach(callback -> callback.createdNewTodo(todo));
    }

    /**
     * Creation callback interface.
     */
    @FunctionalInterface
    public interface CreateCallback extends Serializable {
        /**
         * Method called when a new {@link Todo} item is created.
         * 
         * @param todo
         *            the created {@link Todo} item
         */
        void createdNewTodo(Todo todo);
    }

}
