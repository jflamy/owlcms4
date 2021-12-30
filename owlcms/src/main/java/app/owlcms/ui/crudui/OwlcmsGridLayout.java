/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.crudui;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import ch.qos.logback.classic.Logger;

/**
 * The Class OwlcmsGridLayout.
 */
@SuppressWarnings("serial")
public class OwlcmsGridLayout extends WindowBasedCrudLayout {

    @SuppressWarnings("unused")
    final private static Logger logger = (Logger) LoggerFactory.getLogger(OwlcmsGridLayout.class);

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

        mainComponentLayout.setWidth("100%");
        mainComponentLayout.setHeight("100%");
        mainComponentLayout.setMargin(false);
        mainComponentLayout.setPadding(false);
        mainComponentLayout.setBoxSizing(BoxSizing.BORDER_BOX);
        mainLayout.add(mainComponentLayout);
        mainLayout.expand(mainComponentLayout);
        mainComponentLayout.setId("mainComponentLayout");
        mainLayout.setId("mainLayout");

        setWindowCaption(CrudOperation.ADD, getTranslation("Add_title", aClass.getSimpleName()));
        setWindowCaption(CrudOperation.UPDATE, getTranslation("Update_title", aClass.getSimpleName()));
        setWindowCaption(CrudOperation.DELETE, getTranslation("Delete_title", aClass.getSimpleName()));
    }

    public Component getFilterLayout() {
        return filterLayout;
    }

    public Component getHeaderLayout() {
        return headerLayout;
    }

    public Component getMainLayout() {
        return mainLayout;
    }

    public Component getToolbarLayout() {
        return toolbarLayout;
    }

    @Override
    public void showDialog(String caption, Component form) {
        VerticalLayout dialogLayout = new VerticalLayout(form);
        dialogLayout.setWidth("100%");
        dialogLayout.setMargin(false);
        dialogLayout.setPadding(false);

        H3 h3 = new H3(caption);
        h3.getStyle().set("margin-top", "0");
        h3.getStyle().set("margin-bottom", "0");
        dialog = new Dialog(h3, dialogLayout);
        dialog.setWidth(formWindowWidth);
        dialog.open();
    }
}
