/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Non-Profit Open Software License ("Non-Profit OSL") 3.0 
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.ui.shared;

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

    /**
     * Instantiates a new full iron icon.
     *
     * @param collection the collection
     * @param icon the icon
     */
    public FullIronIcon(String collection, String icon) {
        // iron-icon's icon-attribute uses the format "collection:name",
        // e.g. icon="icons:expand-more"
        super(collection,icon);
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
            getStyle().set("transformation", "rotate("+angle+"deg)");
        }
        return this;
    }
    
    /**
     * Gets the rotation.
     *
     * @return the rotation
     */
    public String getRotation() {
    	return getStyle().get("transformation");
    }
}
