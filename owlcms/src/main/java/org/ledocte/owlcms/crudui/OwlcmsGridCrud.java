package org.ledocte.owlcms.crudui;

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
		System.err.println("owlcms showForm");
		Component form = this.owlcmsCrudFormFactory.buildNewForm(operation, domainObject, readOnly,
				cancelClickEvent -> {
					// make sure we can select again -- click to select causes issues due to automatic triggering of forms
					grid.asSingleSelect().clear();
					owlcmsCrudLayout.disableNextShowForm(false);
				}, operationPerformedClickEvent -> {
					// we use click to select, we don't want select to bring us back
					System.err.println("hiding after operation");
					owlcmsCrudLayout.hideForm();
					buttonClickListener.onComponentEvent(operationPerformedClickEvent);
					Notification.show(successMessage);
				}, deletePerformedClickEvent -> {
					owlcmsCrudLayout.hideForm();
					// display the confirmation form immediately
					owlcmsCrudLayout.disableNextShowForm(false);
					this.deleteButtonClicked();
				});

		crudLayout.showForm(operation, form);
	}
	
	@Override
    protected void updateButtonClicked() {
		System.err.println("owlcms pencil");
        T domainObject = grid.asSingleSelect().getValue();
        this.showForm(CrudOperation.UPDATE, domainObject, false, savedMessage, event -> {
            try {
                T updatedObject = updateOperation.perform(domainObject);
                grid.asSingleSelect().clear();
                refreshGrid();
                grid.asSingleSelect().setValue(updatedObject);
                // TODO: grid.scrollTo(updatedObject);
            } catch (IllegalArgumentException ignore) {
            } catch (CrudOperationException e1) {
                refreshGrid();
            } catch (Exception e2) {
                refreshGrid();
                throw e2;
            }
        });
    }

}
