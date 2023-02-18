/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import java.util.Collection;

import org.vaadin.crudui.crud.CrudOperation;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;

/**
 * Methods that need to be implemented to have a manually-generated editing form
 * that interacts with the Crud framework.
 *
 */
public interface CustomFormFactory<T> {

	/**
	 * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
	 */
	T add(T athlete);

	Binder<T> buildBinder(CrudOperation operation, T doNotUse);

	/**
	 * @see org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory#buildCaption(org.vaadin.crudui.crud.CrudOperation,
	 *      java.lang.Object)
	 */
	String buildCaption(CrudOperation operation, T aFromDb);

	Component buildFooter(CrudOperation operation, T unused,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> unused2, ComponentEventListener<ClickEvent<Button>> unused3,
	        boolean shortcutEnter, Button... buttons);

	/**
	 * We create a copy of the edited object so that we can validate live
	 *
	 * @see app.owlcms.nui.crudui.OwlcmsCrudFormFactory#buildNewForm(org.vaadin.crudui.crud.CrudOperation,
	 *      java.lang.Object, boolean,
	 *      com.vaadin.flow.component.ComponentEventListener,
	 *      com.vaadin.flow.component.ComponentEventListener,
	 *      com.vaadin.flow.component.ComponentEventListener)
	 */
	Component buildNewForm(CrudOperation operation, T aFromDb, boolean readOnly,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons);

	Button buildOperationButton(CrudOperation operation, T domainObject,
	        ComponentEventListener<ClickEvent<Button>> callBack);

	/**
	 * Workaround for the fact that ENTER as keyboard shortcut prevents the value
	 * being typed from being set in the underlying object.
	 *
	 * i.e. Typing TAB followed by ENTER works (because tab causes ON_BLUR), but
	 * ENTER alone doesn't. We work around this issue by causing focus to move, and
	 * reacting to the focus being set.
	 *
	 * @param operation
	 *
	 * @param operation
	 * @param gridLayout
	 *
	 * @see app.owlcms.nui.crudui.OwlcmsCrudFormFactory#defineUpdateTrigger(org.vaadin.crudui.crud.CrudOperation,
	 *      com.github.appreciated.layout.GridLayout)
	 */
	TextField defineOperationTrigger(CrudOperation operation, T domainObject,
	        ComponentEventListener<ClickEvent<Button>> action);

	/**
	 * @see org.vaadin.crudui.crud.CrudListener#delete(java.lang.Object)
	 */
	void delete(T notUsed);

	/**
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	Collection<T> findAll();

	boolean setErrorLabel(BinderValidationStatus<?> validationStatus, boolean updateFieldStatus);

	/**
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	T update(T athleteFromDb);

}