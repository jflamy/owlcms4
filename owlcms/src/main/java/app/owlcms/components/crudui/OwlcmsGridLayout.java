/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.components.crudui;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.ThemableLayout;

import ch.qos.logback.classic.Logger;

/**
 * The Class OwlcmsGridLayout.
 */
@SuppressWarnings("serial")
public class OwlcmsGridLayout extends WindowBasedCrudLayout {
	
	@SuppressWarnings("unused")
	final private static Logger logger = (Logger)LoggerFactory.getLogger(OwlcmsGridLayout.class);

	/**
	 * Instantiates a new owlcms grid layout.
	 *
	 * @param aClass the a class
	 */
	public OwlcmsGridLayout(Class<?> aClass) {
        getContent().setPadding(false);
        ((ThemableLayout) getContent()).setMargin(false);
        ((HasComponents) getContent()).add(mainLayout);

        mainLayout.setSizeFull();
        mainLayout.setMargin(false);
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);
        setSizeFull();

        headerLayout.setVisible(false);
        headerLayout.setSpacing(true);
        headerLayout.setMargin(true);

        toolbarLayout.setVisible(false);
        headerLayout.add(toolbarLayout);

        filterLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        filterLayout.setVisible(false);
        filterLayout.setSpacing(true);
        headerLayout.add(filterLayout);

        mainComponentLayout.setWidth("100%");
        mainComponentLayout.setHeight(null);
        mainComponentLayout.setMargin(false);
        mainComponentLayout.setPadding(false);
        mainLayout.add(mainComponentLayout);
        mainLayout.expand(mainComponentLayout);
        mainComponentLayout.setId("mainComponentLayout");
        mainLayout.setId("mainLayout");

        setWindowCaption(CrudOperation.ADD, "Add "+ aClass.getSimpleName());
        setWindowCaption(CrudOperation.UPDATE, "Update "+ aClass.getSimpleName());
        setWindowCaption(CrudOperation.DELETE, "Are you sure you want to delete this item?");
        
    }

	
}
