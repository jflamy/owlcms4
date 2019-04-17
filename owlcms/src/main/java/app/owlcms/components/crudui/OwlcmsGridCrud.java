/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Non-Profit Open Software License ("Non-Profit OSL") 3.0 
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.components.crudui;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.CrudOperationException;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;

import ch.qos.logback.classic.Logger;

/**
 * Class OwlcmsGridCrud.
 *
 * @param <T> the generic type
 */
@SuppressWarnings("serial")
public class OwlcmsGridCrud<T> extends GridCrud<T> {
	
	@SuppressWarnings("unused")
	final private static Logger logger = (Logger)LoggerFactory.getLogger(OwlcmsGridCrud.class);

	private OwlcmsGridLayout owlcmsGridLayout;
	private OwlcmsCrudFormFactory<T> owlcmsCrudFormFactory;

	/**
	 * Instantiates a new owlcms grid grid.
	 *
	 * @param domainType the domain type
	 * @param crudLayout the grid layout
	 * @param owlcmsCrudFormFactory the owlcms grid form factory
	 * @param grid the grid
	 */
	public OwlcmsGridCrud(Class<T> domainType, OwlcmsGridLayout crudLayout, OwlcmsCrudFormFactory<T> owlcmsCrudFormFactory, Grid<T>grid) {
		super(domainType, crudLayout);
		this.grid = grid;
		this.owlcmsCrudFormFactory = owlcmsCrudFormFactory;
		this.setCrudFormFactory(owlcmsCrudFormFactory);
		this.owlcmsGridLayout = crudLayout;
		initLayoutGrid();
	}

    /**
     * Do nothing.
     * Initialization must wait for grid to be constructed, constuctor calls
     * {@link #initLayoutGrid()} instead.
     * 
     * @see org.vaadin.crudui.crud.impl.GridCrud#initLayout()
     */
    @Override
	protected void initLayout() {
    }
	
	/* (non-Javadoc)
	 * @see org.vaadin.crudui.crud.impl.GridCrud#updateButtonClicked()
	 */
	@Override
    protected void updateButtonClicked() {
        T domainObject = grid.asSingleSelect().getValue();
        // show both an update and a delete button.
        this.showForm(CrudOperation.UPDATE, domainObject, false, savedMessage, event -> {
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
	
	/* (non-Javadoc)
	 * @see org.vaadin.crudui.crud.impl.GridCrud#deleteButtonClicked()
	 */
	@Override
	protected void deleteButtonClicked() {
        T domainObject = grid.asSingleSelect().getValue();
        try {
        	deleteOperation.perform(domainObject);
        	grid.asSingleSelect().clear();
        	refreshGrid();
        } catch (CrudOperationException e1) {
        	refreshGrid();
        } catch (Exception e2) {
        	refreshGrid();
        	throw e2;
        }
    }	


	/**
	 * Show form with a delete button.
	 * 
	 * @see org.vaadin.crudui.crud.impl.GridCrud#showForm(org.vaadin.crudui.crud.CrudOperation, java.lang.Object, boolean, java.lang.String, com.vaadin.flow.component.ComponentEventListener)
	 */
	@Override
	protected void showForm(CrudOperation operation, T domainObject, boolean readOnly, String successMessage,
			ComponentEventListener<ClickEvent<Button>> buttonClickListener) {
		Component form = this.owlcmsCrudFormFactory.buildNewForm(operation, domainObject, readOnly,
			cancelClickEvent -> {
				owlcmsGridLayout.hideForm();
				grid.asSingleSelect().clear();
			}, operationPerformedClickEvent -> {
				owlcmsGridLayout.hideForm();
				buttonClickListener.onComponentEvent(operationPerformedClickEvent);
				grid.asSingleSelect().clear();
				Notification.show(successMessage);
			}, deletePerformedClickEvent -> {
				owlcmsGridLayout.hideForm();
				this.deleteButtonClicked();
			});

		String caption = this.owlcmsCrudFormFactory.buildCaption(operation, domainObject);
		owlcmsGridLayout.showForm(operation, form, caption);
	}

	
	/**
	 * Replacement initialization
	 * We do not create the grid automatically, but instead receive the grid pre-populated.
	 */
	protected void initLayoutGrid() {
        initToolbar();
        
		grid.setSizeFull();
        grid.addSelectionListener(e -> gridSelectionChanged());
        crudLayout.setMainComponent(grid);
	}

	/**
	 * Inits the toolbar.
	 */
	protected void initToolbar() {
		findAllButton = new Button(VaadinIcon.REFRESH.create(), e -> findAllButtonClicked());
        findAllButton.getElement().setAttribute("title", "Refresh list");
        crudLayout.addToolbarComponent(findAllButton);

        addButton = new Button(VaadinIcon.PLUS.create(), e -> addButtonClicked());
        addButton.getElement().setAttribute("title", "Add");
        crudLayout.addToolbarComponent(addButton);

        updateButton = new Button(VaadinIcon.PENCIL.create(), e -> updateButtonClicked());
        updateButton.getElement().setAttribute("title", "Update");
        crudLayout.addToolbarComponent(updateButton);

        deleteButton = new Button(VaadinIcon.TRASH.create(), e -> deleteButtonClicked());
        deleteButton.getElement().setAttribute("title", "Delete");
        crudLayout.addToolbarComponent(deleteButton);
        
        updateButtons();
	}

}
