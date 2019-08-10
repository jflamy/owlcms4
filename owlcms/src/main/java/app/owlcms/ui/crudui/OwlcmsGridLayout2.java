/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.crudui;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.layout.CrudLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.ThemableLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import ch.qos.logback.classic.Logger;

/**
 * The Class OwlcmsGridLayout.
 */
@SuppressWarnings("serial")
public class OwlcmsGridLayout2 extends Composite<VerticalLayout> implements CrudLayout, HasSize {
	
	@SuppressWarnings("unused")
	final private static Logger logger = (Logger)LoggerFactory.getLogger(OwlcmsGridLayout2.class);
    private FlexLayout stackLayout;
    private FlexLayout bottomLayout;
    
    protected HorizontalLayout headerLayout = new HorizontalLayout();
    protected HorizontalLayout toolbarLayout = new HorizontalLayout();
    protected HorizontalLayout filterLayout = new HorizontalLayout();
    
    private Dialog dialog;
    private String formWindowWidth;

	/**
	 * Instantiates a new owlcms crudGrid layout.
	 *
	 * @param aClass the a class
	 */
	public OwlcmsGridLayout2(Class<?> aClass) {
        stackLayout = new FlexLayout();
        stackLayout.getElement().getStyle().set("flex-direction", "column");
        stackLayout.setSizeFull();
        
        bottomLayout = new FlexLayout();
        bottomLayout.getElement().getStyle().set("flex-direction", "column");
        bottomLayout.setHeight("100%");
        bottomLayout.setWidthFull();
        
        getContent().setPadding(false);
        ((ThemableLayout) getContent()).setMargin(false);
        ((HasComponents) getContent()).add(stackLayout);

        headerLayout.setVisible(false);
        headerLayout.setSpacing(true);
        headerLayout.setMargin(true);

        toolbarLayout.setVisible(false);
        headerLayout.add(toolbarLayout);

        filterLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        filterLayout.setVisible(false);
        filterLayout.setSpacing(true);
        headerLayout.add(filterLayout);

        bottomLayout.setWidth("100%");
        bottomLayout.getElement().getStyle().set("flex-direction", "column");
        bottomLayout.setId("mainComponentLayout");

        setMainComponent(bottomLayout);
        stackLayout.add(new Paragraph("testTop"));
        bottomLayout.add(new Paragraph("testInner"));
        
        setWindowCaption(CrudOperation.ADD, getTranslation("Add_title", aClass.getSimpleName()));
        setWindowCaption(CrudOperation.UPDATE, getTranslation("Update_title", aClass.getSimpleName()));
        setWindowCaption(CrudOperation.DELETE, getTranslation("Delete_title", aClass.getSimpleName()));
        
    }

    @Override
    public void setMainComponent(Component component) {
        stackLayout.removeAll();
        stackLayout.add(bottomLayout);
        stackLayout.setFlexGrow(1.0, bottomLayout);
    }
    
    @Override
    public void addFilterComponent(Component component) {
        if (!headerLayout.isVisible()) {
            headerLayout.setVisible(true);
            stackLayout.getElement().insertChild(stackLayout.getComponentCount() - 1, headerLayout.getElement());
        }

        filterLayout.setVisible(true);
        filterLayout.add(component);
    }

    @Override
    public void addToolbarComponent(Component component) {
        if (!headerLayout.isVisible()) {
            headerLayout.setVisible(true);
            stackLayout.getElement().insertChild(stackLayout.getComponentCount() - 1, headerLayout.getElement());
        }

        toolbarLayout.setVisible(true);
        toolbarLayout.add(component);
    }

    @Override
    public void showDialog(String caption, Component form) {
        VerticalLayout dialogLayout = new VerticalLayout(form);
        dialogLayout.setWidth("100%");
        dialogLayout.setMargin(false);
        dialogLayout.setPadding(false);

        dialog = new Dialog(new H3(caption), dialogLayout);
        dialog.setWidth(formWindowWidth);
        dialog.open();
    }
    
    protected Map<CrudOperation, String> windowCaptions = new HashMap<>();

    @Override
    public void showForm(CrudOperation operation, Component form, String formCaption) {
        if (!operation.equals(CrudOperation.READ)) {
            String caption = (formCaption != null ? formCaption : windowCaptions.get(operation));
            showDialog(caption, form);
        }
    }
    
    @Override
    public void hideForm() {
        if (dialog != null) {
            dialog.close();
        }
    }

    public void setWindowCaption(CrudOperation operation, String caption) {
        windowCaptions.put(operation, caption);
    }

    public void setFormWindowWidth(String formWindowWidth) {
        this.formWindowWidth = formWindowWidth;
    }
}
