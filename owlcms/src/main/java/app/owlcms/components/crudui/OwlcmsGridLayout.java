/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
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
