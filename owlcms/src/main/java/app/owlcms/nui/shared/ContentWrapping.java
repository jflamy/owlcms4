/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public interface ContentWrapping {

	public default void centerH(Component content, VerticalLayout vWrapper) {
		vWrapper.setBoxSizing(BoxSizing.BORDER_BOX);
		vWrapper.setHeight(null);
		vWrapper.setWidth("100%");
		vWrapper.add(content);
		vWrapper.setAlignSelf(Alignment.CENTER, content);
	}

	public default void centerHW(Component content, VerticalLayout vWrapper) {
//      LoggerFactory.getLogger(ContentWrapping.class)./**/warn("fillHW from {}",LoggerUtils.whereFrom());
		vWrapper.setBoxSizing(BoxSizing.BORDER_BOX);
		vWrapper.setSizeFull();
		vWrapper.add(content);
		vWrapper.setAlignSelf(Alignment.CENTER, content);

	}

	public default void fillH(Component content, VerticalLayout vWrapper) {
		vWrapper.setBoxSizing(BoxSizing.BORDER_BOX);
		vWrapper.setHeight(null);
		vWrapper.setWidth("95%");
		vWrapper.setSpacing(false);
		vWrapper.add(content);
	}

	public default void fillHW(Component content, VerticalLayout vWrapper) {
//        LoggerFactory.getLogger(ContentWrapping.class)./**/warn("fillHW from {}",LoggerUtils.whereFrom());
		vWrapper.setBoxSizing(BoxSizing.BORDER_BOX);
		vWrapper.setSizeFull();
		vWrapper.add(content);
	}

}
