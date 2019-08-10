/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.crudui;

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

import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * Class OwlcmsCrudGrid.
 *
 * @param <T> the generic type
 */
@SuppressWarnings("serial")
public class OwlcmsCrudGrid<T> extends GridCrud<T> {
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(OwlcmsCrudGrid.class);

	private OwlcmsGridLayout owlcmsGridLayout;
	private OwlcmsCrudFormFactory<T> owlcmsCrudFormFactory;

	/**
	 * Instantiates a new owlcms crudGrid crudGrid.
	 *
	 * @param domainType the domain type
	 * @param crudLayout the crudGrid layout
	 * @param owlcmsCrudFormFactory the owlcms crudGrid form factory
	 * @param crudGrid the crudGrid
	 */
	public OwlcmsCrudGrid(Class<T> domainType, OwlcmsGridLayout crudLayout, OwlcmsCrudFormFactory<T> owlcmsCrudFormFactory, Grid<T>grid) {
		super(domainType, crudLayout);
		this.grid = grid;
		this.owlcmsCrudFormFactory = owlcmsCrudFormFactory;
		this.setCrudFormFactory(owlcmsCrudFormFactory);
		this.owlcmsGridLayout = crudLayout;
		initLayoutGrid();
	}

    /**
     * Do nothing.
     * Initialization must wait for crudGrid to be constructed, constuctor calls
     * {@link #initLayoutGrid()} instead.
     * 
     * @see org.vaadin.crudui.crud.impl.GridCrud#initLayout()
     */
    @Override
	protected void initLayout() {
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
			ComponentEventListener<ClickEvent<Button>> unused) {
		Component form = this.owlcmsCrudFormFactory.buildNewForm(operation, domainObject, readOnly,
			cancelButtonClickEvent -> {
				logger.debug("cancelButtonClickEvent");
				owlcmsGridLayout.hideForm();
				grid.asSingleSelect().clear();
			}, 
			operationButtonClickEvent -> {
				try {
					logger.debug("postOperation");
					grid.asSingleSelect().clear();
					owlcmsGridLayout.hideForm();
					refreshGrid();
					Notification.show(successMessage);
					logger.trace("operation performed");
				} catch (Exception e) {
					logger.error(LoggerUtils.stackTrace(e));
				}
			}, 
			deleteButtonClickEvent -> {
				logger.debug("preDelete");
				owlcmsGridLayout.hideForm();
				this.deleteButtonClicked();
			});

		String caption = this.owlcmsCrudFormFactory.buildCaption(operation, domainObject);
		owlcmsGridLayout.showForm(operation, form, caption);
	}

	
	/**
	 * Replacement initialization
	 * We do not create the crudGrid automatically, but instead receive the crudGrid pre-populated.
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
        findAllButton.getElement().setAttribute("title", getTranslation("RefreshList"));
        crudLayout.addToolbarComponent(findAllButton);

        addButton = new Button(VaadinIcon.PLUS.create(), e -> addButtonClicked());
        addButton.getElement().setAttribute("title", getTranslation("Add"));
        crudLayout.addToolbarComponent(addButton);

        updateButton = new Button(VaadinIcon.PENCIL.create(), e -> updateButtonClicked());
        updateButton.getElement().setAttribute("title", getTranslation("Update"));
        crudLayout.addToolbarComponent(updateButton);

        deleteButton = new Button(VaadinIcon.TRASH.create(), e -> deleteButtonClicked());
        deleteButton.getElement().setAttribute("title", getTranslation("Delete"));
        crudLayout.addToolbarComponent(deleteButton);
        
        updateButtons();
	}
	
	

}
