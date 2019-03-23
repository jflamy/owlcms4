/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.crudui;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.ThemableLayout;

import ch.qos.logback.classic.Logger;

/**
 * The Class OwlcmsCrudLayout.
 */
@SuppressWarnings("serial")
public class OwlcmsCrudLayout extends WindowBasedCrudLayout {
	
	@SuppressWarnings("unused")
	final private static Logger logger = (Logger)LoggerFactory.getLogger(OwlcmsCrudLayout.class);

//	private boolean disableNextShowForm = false;

	/**
	 * Instantiates a new owlcms crud layout.
	 *
	 * @param aClass the a class
	 */
	public OwlcmsCrudLayout(Class<?> aClass) {
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
        // toolbarLayout.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        headerLayout.add(toolbarLayout);

        filterLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        filterLayout.setVisible(false);
        filterLayout.setSpacing(true);
        headerLayout.add(filterLayout);

//        Icon icon = VaadinIcon.SEARCH.create();
//        icon.setSize(".9em");
//        filterLayout.add(icon);

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

//	/* (non-Javadoc)
//	 * @see org.vaadin.crudui.layout.impl.WindowBasedCrudLayout#showForm(org.vaadin.crudui.crud.CrudOperation, com.vaadin.flow.component.Component)
//	 */
//	@Override
//	public void showForm(CrudOperation operation, Component form, String caption) {
//		if (isDisableNextShowForm()) {
//			logger.debug("ignoring open");
//		} else if (!operation.equals(CrudOperation.READ)) {
//        	showDialog(caption, form);
//        }
//		disableNextShowForm(false);
//	}
//	
//	
//	/**
//	 * Disable next show form.
//	 *
//	 * @param isDisabled the is disabled
//	 */
//	public void disableNextShowForm(boolean isDisabled) {
//		setDisableNextShowForm(isDisabled);
//	}
//
//	public boolean isDisableNextShowForm() {
//		return disableNextShowForm;
////	}
//
//	public void setDisableNextShowForm(boolean disableNextShowForm) {
//		this.disableNextShowForm = disableNextShowForm;
//	}
	
}
