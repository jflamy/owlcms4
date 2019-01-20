/*
 * Copyright 2000-2016 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.demo.testutil.AbstractChromeTest;

public class TodoIT extends AbstractChromeTest {

    @Test
    public void basicFunctionality() {
        open();

        WebElement template = findElement(By.id("template"));

        WebElement createTemplate = getInShadowRoot(template,
                By.id("creator"));

        WebElement todo = getInShadowRoot(createTemplate, By.id("task-input"));
        todo.sendKeys("Important task");

        WebElement user = getInShadowRoot(createTemplate,
                By.id("user-name-input"));
        user.sendKeys("Teuvo testi");

        WebElement createButton = getInShadowRoot(createTemplate,
                By.id("create-button"));
        createButton.click();

        WebElement todoElement = template
                .findElement(By.tagName("todo-element"));
        Assert.assertEquals("Important task",
                getInShadowRoot(todoElement, By.id("task"))
                        .getText());

        getInShadowRoot(todoElement, By.id("checkbox")).click();

        todoElement = template
                .findElement(By.tagName("todo-element"));
        Assert.assertEquals("done", todoElement.getAttribute("slot"));
    }
}
