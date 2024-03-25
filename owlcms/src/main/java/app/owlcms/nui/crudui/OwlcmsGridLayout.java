/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.crudui;

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
		this.mainLayout = getContent();
		this.mainLayout.removeAll();
		this.mainLayout.setBoxSizing(BoxSizing.BORDER_BOX);

		// getContent().setPadding(false);
		// ((ThemableLayout) getContent()).setMargin(false);
		// ((HasComponents) getContent()).add(mainLayout);

		this.mainLayout.setSizeFull();
		this.mainLayout.setMargin(false);
		this.mainLayout.setPadding(false);
		this.mainLayout.setSpacing(false);
		setSizeFull();

		this.headerLayout.setVisible(false);
		this.headerLayout.setSpacing(true);
		this.headerLayout.setMargin(true);
		this.headerLayout.setId("headerLayout");

		this.toolbarLayout.setVisible(false);
		this.headerLayout.add(this.toolbarLayout);

		this.filterLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		this.filterLayout.setVisible(false);
		this.filterLayout.setSpacing(true);
		this.headerLayout.add(this.filterLayout);

		this.mainComponentLayout.setWidth("100%");
		this.mainComponentLayout.setHeight("100%");
		this.mainComponentLayout.setMargin(false);
		this.mainComponentLayout.setPadding(false);
		this.mainComponentLayout.setBoxSizing(BoxSizing.BORDER_BOX);
		this.mainLayout.add(this.mainComponentLayout);
		this.mainLayout.expand(this.mainComponentLayout);
		this.mainComponentLayout.setId("mainComponentLayout");
		this.mainLayout.setId("mainLayout");

		setWindowCaption(CrudOperation.ADD, getTranslation("Add_title", aClass.getSimpleName()));
		setWindowCaption(CrudOperation.UPDATE, getTranslation("Update_title", aClass.getSimpleName()));
		setWindowCaption(CrudOperation.DELETE, getTranslation("Delete_title", aClass.getSimpleName()));
	}

	public Component getFilterLayout() {
		return this.filterLayout;
	}

	public Component getHeaderLayout() {
		return this.headerLayout;
	}

	public Component getMainLayout() {
		return this.mainLayout;
	}

	public Component getToolbarLayout() {
		return this.toolbarLayout;
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
		this.dialog = new Dialog(h3, dialogLayout);
		this.dialog.setWidth(this.formWindowWidth);
		this.dialog.open();
	}
}
