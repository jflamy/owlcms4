/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.shared;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
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
@JsModule("@polymer/iron-icon/iron-icon.js")
@JsModule("@polymer/iron-icons/iron-icons.js")
@JsModule("@polymer/iron-icons/av-icons.js")
@JsModule("@polymer/iron-icons/hardware-icons.js")
@JsModule("@polymer/iron-icons/maps-icons.js")
@JsModule("@polymer/iron-icons/social-icons.js")
@JsModule("@polymer/iron-icons/places-icons.js")
public class FullIronIcon extends Icon {

    /**
     * Instantiates a new full iron icon.
     *
     * @param collection the collection
     * @param icon       the icon
     */
    public FullIronIcon(String collection, String icon) {
        // iron-icon's icon-attribute uses the format "collection:name",
        // e.g. icon="icons:expand-more"
        super(collection, icon);
    }

    /**
     * Gets the rotation.
     *
     * @return the rotation
     */
    public String getRotation() {
        return getStyle().get("transformation");
    }

    /**
     * Sets the rotation.
     *
     * @param angle the angle
     * @return the icon
     */
    public Icon setRotation(Integer angle) {
        if (angle == null) {
            getStyle().remove("transformation");
        } else {
            getStyle().set("transformation", "rotate(" + angle + "deg)");
        }
        return this;
    }
}
