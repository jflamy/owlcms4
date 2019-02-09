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
package org.ledocte.owlcms.ui.home;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.icon.Icon;

/**
 * Server side component for
 * <a href="https://github.com/PolymerElements/iron-icon">iron-icon</a> element
 * to display an icon.
 *
 * @author Vaadin Ltd
 */
@SuppressWarnings("serial")
@Tag("full-iron-icon")
@HtmlImport("frontend://bower_components/iron-icons/iron-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/av-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/hardware-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/maps-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/social-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/places-icons.html")
public class FullIronIcon extends Icon {

    public FullIronIcon(String collection, String icon) {
        // iron-icon's icon-attribute uses the format "collection:name",
        // e.g. icon="icons:expand-more"
        super(collection,icon);
    }
    
    public Icon setRotation(Integer angle) {
        if (angle == null) {
            getStyle().remove("transformation");
        } else {
            getStyle().set("transformation", "rotate("+angle+"deg)");
        }
        return this;
    }
    
    public String getRotation() {
    	return getStyle().get("transformation");
    }
}
