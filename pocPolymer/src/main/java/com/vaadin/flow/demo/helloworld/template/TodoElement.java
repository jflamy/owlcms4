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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;

/**
 * An element for displaying the Todo item. Task can be edited and completion of
 * task can be set in the element.
 */
@Tag("todo-element")
@HtmlImport("frontend://components/TodoElement.html")
public class TodoElement extends PolymerTemplate<TodoElement.TodoModel> {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<Runnable> changeListeners = new ArrayList<>(0);

    /**
     * The TodoModel.
     */
    public interface TodoModel extends TemplateModel {
        /**
         * Get task text.
         * 
         * @return task
         */
        String getTask();

        /**
         * Set task.
         * 
         * @param task
         *            task
         */
        void setTask(String task);

        /**
         * Get user.
         * 
         * @return user
         */
        String getUser();

        /**
         * Set user.
         * 
         * @param user
         *            user
         */
        void setUser(String user);

        /**
         * Get Id.
         * 
         * @return id
         */
        int getRid();

        /**
         * Set id.
         * 
         * @param rid
         *            id
         */
        void setRid(int rid);

        /**
         * Get the stored time string.
         * 
         * @return time
         */
        String getTime();

        /**
         * Set the time string.
         * 
         * @param time
         *            time
         */
        void setTime(String time);

        /**
         * Get task completion.
         * 
         * @return task completion
         */
        boolean isCompleted();

        /**
         * Set task completion.
         * 
         * @param completed
         *            task completion.
         */
        void setCompleted(boolean completed);
    }

    private Todo todo;

    /**
     * Todo element constructor.
     * 
     * @param todo
     *            todo item for this element
     */
    public TodoElement(Todo todo) {
        this.todo = todo;

        populateModel(todo);
        addChangeListeners(todo);
    }

    private void populateModel(Todo todo) {
        getModel().setTask(todo.getTask());
        getModel().setUser(todo.getUser());
        getModel().setRid(todo.getRid());
        getModel().setTime(todo.getTime()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm")));
    }

    private void addChangeListeners(Todo todo) {
        getElement().addPropertyChangeListener("completed",
                event -> taskCompleted());
        getElement().addPropertyChangeListener("task",
                event -> todo.setTask(getModel().getTask()));
    }

    private void taskCompleted() {
        todo.setCompleted(getModel().isCompleted());

        changeListeners.forEach(Runnable::run);
    }

    /**
     * Get the {@link Todo} item for this TodoElement.
     * 
     * @return todo item
     */
    public Todo getTodo() {
        return todo;
    }

    /**
     * Returns completion state of this {@link Todo} item.
     * 
     * @return todo item completion status
     */
    public boolean isCompleted() {
        return getModel().isCompleted();
    }

    /**
     * Add a state change listener that is informed when the completed state
     * changes.
     * 
     * @param listener
     *            runnable method to be used as a listener
     */
    public void addStateChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }
}
