/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Non-Profit Open Software License ("Non-Profit OSL") 3.0 
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.ui.shared;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public interface ContentWrapping {
	
	public default void fillHW (Component content, VerticalLayout vWrapper) {
		HorizontalLayout hWrapper = new HorizontalLayout(content);
        hWrapper.setMargin(true);
        hWrapper.setPadding(false);
        hWrapper.setSpacing(false);
        hWrapper.setFlexGrow(1, content);
        //hWrapper.setSizeFull();
        vWrapper.add(hWrapper);
        vWrapper.setMargin(false);
        vWrapper.setPadding(false);
        vWrapper.setSpacing(false);
        vWrapper.setAlignItems(Alignment.STRETCH);
        vWrapper.setFlexGrow(1, hWrapper);
        vWrapper.setSizeFull();
	}
	
	public default void fillH(Component content, VerticalLayout vWrapper) {
		HorizontalLayout hWrapper = new HorizontalLayout(content);
        hWrapper.setMargin(true);
        hWrapper.setPadding(false);
        hWrapper.setSpacing(false);
        hWrapper.setFlexGrow(1, content);
        hWrapper.getElement().getStyle().set("margin-bottom", "0");
        //hWrapper.setSizeFull();
        vWrapper.add(hWrapper);
        vWrapper.setMargin(false);
        vWrapper.setPadding(false);
        vWrapper.setSpacing(false);
        vWrapper.setAlignItems(Alignment.STRETCH);
//        vWrapper.setFlexGrow(1, hWrapper);
//        vWrapper.setSizeFull();
	}

}
