/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.shared;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public interface ContentWrapping {

    public default void fillH(Component content, VerticalLayout vWrapper) {
        vWrapper.setBoxSizing(BoxSizing.BORDER_BOX);
        vWrapper.setHeight(null);
        vWrapper.setWidth("100%");
        vWrapper.add(content);
    }

    public default void fillHW(Component content, VerticalLayout vWrapper) {
//        LoggerFactory.getLogger(ContentWrapping.class)./**/warn("fillHW from {}",LoggerUtils.whereFrom());
        vWrapper.setBoxSizing(BoxSizing.BORDER_BOX);
        vWrapper.setSizeFull();
        vWrapper.add(content);
    }

}
