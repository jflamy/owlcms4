/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.ui.crudui;

import java.util.List;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.CrudFormFactory;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * A factory for creating OwlcmsCrudForm objects.
 *
 * @param <T> the generic type
 */
@SuppressWarnings("serial")
public class OwlcmsCrudFormFactory<T> extends DefaultCrudFormFactory<T> implements CrudFormFactory<T> {
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(OwlcmsCrudFormFactory.class);

	protected ResponsiveStep[] responsiveSteps;

	/**
	 * Instantiates a new Form Factory
	 * 
	 * We add a delete button capability to the CrudUI forms.
	 *
	 * @param domainType the domain type
	 */
	public OwlcmsCrudFormFactory(Class<T> domainType) {
		super(domainType);
		init();
	}

	/**
	 * Instantiates a new owlcms crud form factory.
	 *
	 * @param domainType      the domain type
	 * @param responsiveSteps the responsive steps
	 */
	public OwlcmsCrudFormFactory(Class<T> domainType, ResponsiveStep... responsiveSteps) {
		super(domainType, responsiveSteps);
		this.responsiveSteps = responsiveSteps;
		init();
	}

	private void init() {
		logger.setLevel(Level.DEBUG);
		setButtonCaption(CrudOperation.DELETE, "Delete");
	}

	/**
	 * Form with a Delete button
	 *
	 * @param operation                 the operation
	 * @param domainObject              the domain object
	 * @param readOnly                  the read only
	 * @param cancelButtonClickListener the cancel button click listener
	 * @param updateButtonClickListener the update button click listener
	 * @param deleteButtonClickListener the delete button click listener
	 * @return the component
	 */
	@SuppressWarnings("rawtypes")
	public Component buildNewForm(CrudOperation operation, T domainObject, boolean readOnly,
			ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener) {
		FormLayout formLayout = new FormLayout();
		formLayout.setSizeFull();
		if (this.responsiveSteps != null) {
			formLayout.setResponsiveSteps(this.responsiveSteps);
		}

		List<HasValueAndElement> fields = buildFields(operation, domainObject, readOnly);
		fields.stream()
			.forEach(field -> formLayout.getElement().appendChild(field.getElement()));

		Component footerLayout = this.buildFooter(operation, domainObject, cancelButtonClickListener,
			updateButtonClickListener, deleteButtonClickListener);

		com.vaadin.flow.component.orderedlayout.VerticalLayout mainLayout = new VerticalLayout(
				formLayout, footerLayout);
		mainLayout.setFlexGrow(1, formLayout);
		mainLayout.setHorizontalComponentAlignment(Alignment.END, footerLayout);
		mainLayout.setMargin(false);
		mainLayout.setPadding(false);
		mainLayout.setSpacing(true);

		return mainLayout;
	}

	/**
	 * Footer with a Delete button.
	 * 
	 * Also adds a shortcut so enter submits.
	 *
	 * @param operation                 the operation
	 * @param domainObject              the domain object
	 * @param cancelButtonClickListener the cancel button click listener
	 * @param updateButtonClickListener the update button click listener
	 * @param deleteButtonClickListener the delete button click listener
	 * @return the component
	 */
	protected Component buildFooter(CrudOperation operation, T domainObject,
			ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener) {

		Button updateButton = buildOperationButton(CrudOperation.UPDATE, domainObject, updateButtonClickListener);
		Button deleteButton = buildOperationButton(CrudOperation.DELETE, domainObject, deleteButtonClickListener);
		Button cancelButton = buildCancelButton(cancelButtonClickListener);

		HorizontalLayout footerLayout = new HorizontalLayout();
		footerLayout.setWidth("100%");
		footerLayout.setSpacing(true);
		footerLayout.setPadding(false);

		if (deleteButton != null) {
			footerLayout.add(deleteButton);
		}

		Label spacer = new Label();
		footerLayout.add(spacer);

		if (cancelButton != null) {
			footerLayout.add(cancelButton);
		}

		if (updateButton != null && operation == CrudOperation.UPDATE) {
			footerLayout.add(updateButton);
			updateButton.addClickShortcut(Key.ENTER);
		}
		footerLayout.setFlexGrow(1.0, spacer);
		return footerLayout;
	}
}
