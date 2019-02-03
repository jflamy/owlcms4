package org.ledocte.owlcms.ui.crudui;

import java.util.List;

import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.CrudFormFactory;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

@SuppressWarnings("serial")
public class OwlcmsCrudFormFactory<T> extends DefaultCrudFormFactory<T> implements CrudFormFactory<T> {

	private ResponsiveStep[] responsiveSteps;

	@SuppressWarnings("unchecked")
	public OwlcmsCrudFormFactory(Class<T> domainType) {
		super(domainType);
		init();
	}

	public OwlcmsCrudFormFactory(Class<T> domainType, ResponsiveStep... responsiveSteps) {
		super(domainType, responsiveSteps);
		this.responsiveSteps = responsiveSteps;
		init();
	}

	private void init() {
		setButtonCaption(CrudOperation.DELETE, "Delete");
	}


	@SuppressWarnings("rawtypes")
	public Component buildNewForm(CrudOperation operation, T domainObject, boolean readOnly,
			ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener) {
//		System.err.println("owlcms buildNewForm");
		FormLayout formLayout = new FormLayout();
		formLayout.setSizeFull();
		if (this.responsiveSteps != null) {
			formLayout.setResponsiveSteps(this.responsiveSteps);
		}
		
		List<HasValueAndElement> fields = buildFields(operation, domainObject, readOnly);
		fields.stream()
		        .forEach(field ->
		                formLayout.getElement().appendChild(field.getElement()));
		
		Component footerLayout = this.buildFooter(operation, domainObject, cancelButtonClickListener, updateButtonClickListener, deleteButtonClickListener);
		
		com.vaadin.flow.component.orderedlayout.VerticalLayout mainLayout = new VerticalLayout(formLayout, footerLayout);
		mainLayout.setFlexGrow(1, formLayout);
		mainLayout.setHorizontalComponentAlignment(Alignment.END, footerLayout);
		mainLayout.setMargin(false);
		mainLayout.setPadding(false);
		mainLayout.setSpacing(true);
		
		return mainLayout;
	}


	protected Component buildFooter(CrudOperation operation, T domainObject,
			ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener) {
		// TODO Auto-generated method stub
		Button updateButton = buildOperationButton(CrudOperation.UPDATE, domainObject, updateButtonClickListener);
		Button deleteButton = buildOperationButton(CrudOperation.DELETE, domainObject, deleteButtonClickListener);
		Button cancelButton = buildCancelButton(cancelButtonClickListener);
		
		HorizontalLayout footerLayout = new HorizontalLayout();
		footerLayout.setSizeUndefined();
		footerLayout.setSpacing(true);
		footerLayout.setPadding(false);
		
		if (cancelButton != null) {
		    footerLayout.add(cancelButton);
		}
		
		if (updateButton != null && operation == CrudOperation.UPDATE) {
		    footerLayout.add(updateButton);
		}
		
		if (deleteButton != null) {
		    footerLayout.add(deleteButton);
		}
		
		return footerLayout;
	}

	
	
	
}
