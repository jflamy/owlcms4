package org.ledocte.owlcms.ui.crudui;

import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.CrudOperationException;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;

@SuppressWarnings("serial")
public class OwlcmsGridCrud<T> extends GridCrud<T> {

	private OwlcmsCrudLayout owlcmsCrudLayout;
	private OwlcmsCrudFormFactory<T> owlcmsCrudFormFactory;

	public OwlcmsGridCrud(Class<T> domainType, OwlcmsCrudLayout crudLayout) {
		super(domainType, crudLayout);
		this.owlcmsCrudFormFactory = new OwlcmsCrudFormFactory<T>(domainType);
		this.setCrudFormFactory(owlcmsCrudFormFactory);
		this.owlcmsCrudLayout = crudLayout;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.vaadin.crudui.crud.impl.GridCrud#showForm(org.vaadin.crudui.crud.
	 * CrudOperation, java.lang.Object, boolean, java.lang.String,
	 * com.vaadin.flow.component.ComponentEventListener)
	 */
	@Override
	protected void showForm(CrudOperation operation, T domainObject, boolean readOnly, String successMessage,
			ComponentEventListener<ClickEvent<Button>> buttonClickListener) {
		showFormWithDeleteButton(operation, domainObject, readOnly, successMessage, buttonClickListener);
	}

	private void showFormWithDeleteButton(CrudOperation operation, T domainObject, boolean readOnly,
			String successMessage, ComponentEventListener<ClickEvent<Button>> buttonClickListener) {
		Component form = this.owlcmsCrudFormFactory.buildNewForm(operation, domainObject, readOnly,
				cancelClickEvent -> {
					grid.asSingleSelect().clear();
					// make sure we can select again
					owlcmsCrudLayout.disableNextShowForm(false);
				}, operationPerformedClickEvent -> {
					// update re-selects the item, which (because of click-to-select) displays the form again...
					owlcmsCrudLayout.disableNextShowForm(true);
					owlcmsCrudLayout.hideForm();
					buttonClickListener.onComponentEvent(operationPerformedClickEvent);
					grid.asSingleSelect().clear();
					Notification.show(successMessage);
				}, deletePerformedClickEvent -> {
					owlcmsCrudLayout.hideForm();
					// we want a confirmation dialog, the same as clicking on the trash can
					owlcmsCrudLayout.disableNextShowForm(false);
					this.deleteButtonClicked();
				});

		owlcmsCrudLayout.showForm(operation, form);
	}
	
	@Override
    protected void updateButtonClicked() {
        T domainObject = grid.asSingleSelect().getValue();
        // show both an update and a delete button.
        this.showFormWithDeleteButton(CrudOperation.UPDATE, domainObject, false, savedMessage, event -> {
            try {
                T updatedObject = updateOperation.perform(domainObject);
                grid.asSingleSelect().clear();
                refreshGrid();
                grid.asSingleSelect().setValue(updatedObject);
            } catch (IllegalArgumentException ignore) {
            } catch (CrudOperationException e1) {
                refreshGrid();
            } catch (Exception e2) {
                refreshGrid();
                throw e2;
            }
        });
    }
	
	@Override
	protected void deleteButtonClicked() {
        T domainObject = grid.asSingleSelect().getValue();
        // make sure we use the original delete code, else we get into a loop.
        super.showForm(CrudOperation.DELETE, domainObject, true, deletedMessage, event -> {
            try {
                deleteOperation.perform(domainObject);
                refreshGrid();
                grid.asSingleSelect().clear();
            } catch (CrudOperationException e1) {
                refreshGrid();
            } catch (Exception e2) {
                refreshGrid();
                throw e2;
            }
        });
    }

}
