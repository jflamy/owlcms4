/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.crudui;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.FlexComponent;

import ch.qos.logback.classic.Logger;

/**
 * The Class OwlcmsGridLayout.
 */
@SuppressWarnings("serial")
public class OwlcmsGridLayout extends WindowBasedCrudLayout {
	
	@SuppressWarnings("unused")
	final private static Logger logger = (Logger)LoggerFactory.getLogger(OwlcmsGridLayout.class);

	/**
	 * Instantiates a new owlcms crudGrid layout.
	 *
	 * @param aClass the a class
	 */
	public OwlcmsGridLayout(Class<?> aClass) {
	    mainLayout = getContent();
	    mainLayout.removeAll();
	    mainLayout.setBoxSizing(BoxSizing.BORDER_BOX);
	    
//        getContent().setPadding(false);
//        ((ThemableLayout) getContent()).setMargin(false);
//        ((HasComponents) getContent()).add(mainLayout);

        mainLayout.setSizeFull();
        mainLayout.setMargin(false);
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);
        setSizeFull();

        headerLayout.setVisible(false);
        headerLayout.setSpacing(true);
        headerLayout.setMargin(true);
        headerLayout.setId("headerLayout");

        toolbarLayout.setVisible(false);
        headerLayout.add(toolbarLayout);

        filterLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        filterLayout.setVisible(false);
        filterLayout.setSpacing(true);
        headerLayout.add(filterLayout);

        mainComponentLayout.setWidth("100%"); //$NON-NLS-1$
        mainComponentLayout.setHeight("100%");
        mainComponentLayout.setMargin(false);
        mainComponentLayout.setPadding(false);
        mainComponentLayout.setBoxSizing(BoxSizing.BORDER_BOX);
        mainLayout.add(mainComponentLayout);
        mainLayout.expand(mainComponentLayout);
        mainComponentLayout.setId("mainComponentLayout"); //$NON-NLS-1$
        mainLayout.setId("mainLayout"); //$NON-NLS-1$

        setWindowCaption(CrudOperation.ADD, getTranslation("Add_title", aClass.getSimpleName())); //$NON-NLS-1$
        setWindowCaption(CrudOperation.UPDATE, getTranslation("Update_title", aClass.getSimpleName())); //$NON-NLS-1$
        setWindowCaption(CrudOperation.DELETE, getTranslation("Delete_title", aClass.getSimpleName())); //$NON-NLS-1$
        
    }

	
}
