/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.crudui;

import java.util.List;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.CrudOperationException;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridSortOrder;
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

	private static final int DOUBLE_CLICK_MS_DELTA = 1000;

	final private static Logger logger = (Logger) LoggerFactory.getLogger(OwlcmsCrudGrid.class);

	// private OwlcmsCrudFormFactory<T> owlcmsCrudFormFactory;
	protected OwlcmsGridLayout owlcmsGridLayout;

	private boolean clickable = true;

	private long clicked = 0L;

	/**
	 * Instantiates a new owlcms crudGrid crudGrid.
	 *
	 * @param domainType            the domain type
	 * @param crudLayout            the crudGrid layout
	 * @param owlcmsCrudFormFactory the owlcms crudGrid form factory
	 * @param crudGrid              the crudGrid
	 */
	public OwlcmsCrudGrid(Class<T> domainType, OwlcmsGridLayout crudLayout,
	        OwlcmsCrudFormFactory<T> owlcmsCrudFormFactory, Grid<T> grid) {
		super(domainType, crudLayout);
		this.grid = grid;
		// this.owlcmsCrudFormFactory = owlcmsCrudFormFactory;
		// logger.trace("creating OwlcmsCrudGrid with formfactory {} wherefrom
		// {}",System.identityHashCode(owlcmsCrudFormFactory), LoggerUtils.whereFrom());
		this.setCrudFormFactory(owlcmsCrudFormFactory);
		this.owlcmsGridLayout = crudLayout;
		initLayoutGrid();
	}

	public boolean isClickable() {
		return clickable;
	}

	public void setClickable(boolean clickable) {
		this.clickable = clickable;
	}

	public void sort(List<GridSortOrder<T>> sortOrder) {
		grid.sort(sortOrder);
	}

	/*
	 * (non-Javadoc)
	 *
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

	@Override
	protected void findAllButtonClicked() {
		grid.sort(null); // reset the sorting order to none - use the query result set as is.
		super.findAllButtonClicked();
	}

	/**
	 * Do nothing. Initialization must wait for crudGrid to be constructed,
	 * constuctor calls {@link #initLayoutGrid()} instead.
	 *
	 * @see org.vaadin.crudui.crud.impl.GridCrud#initLayout()
	 */
	@Override
	protected void initLayout() {
	}

	/**
	 * Replacement initialization We do not create the crudGrid automatically, but
	 * instead receive the crudGrid pre-populated.
	 */
	protected void initLayoutGrid() {
		initToolbar();

		grid.setSizeFull();
		grid.setSelectionMode(SelectionMode.SINGLE);

		// We do not use a selection listener; instead we handle clicks explicitely.
		// grid.addSelectionListener(e -> gridSelectionChanged());
		grid.addItemClickListener((e) -> {
			if (!this.isClickable()) {
				return;
			}
			long delta = System.currentTimeMillis() - clicked;
			if (delta > DOUBLE_CLICK_MS_DELTA) {
				grid.select(e.getItem());
				gridSelectionChanged();
			}
			clicked = System.currentTimeMillis();
		});
		grid.addItemDoubleClickListener((e) -> {
		});
//		grid.addCellFocusListener(e -> {
//		});

		for (Column<T> c : grid.getColumns()) {
			c.setResizable(true);
		}

		crudLayout.setMainComponent(grid);
	}

	/**
	 * Inits the toolbar.
	 */
	protected void initToolbar() {
		findAllButton = new Button(getTranslation("RefreshList"), VaadinIcon.REFRESH.create(),
		        e -> findAllButtonClicked());
		findAllButton.getElement().setAttribute("title", getTranslation("RefreshList"));
		crudLayout.addToolbarComponent(findAllButton);

		addButton = new Button(VaadinIcon.PLUS.create(), e -> addButtonClicked());
		addButton.getElement().setAttribute("title", getTranslation("Add"));
		crudLayout.addToolbarComponent(addButton);

		updateButton = new Button(VaadinIcon.PENCIL.create(), e -> updateButtonClicked());
		updateButton.getElement().setAttribute("title", getTranslation("Update"));
//        crudLayout.addToolbarComponent(updateButton);

		deleteButton = new Button(VaadinIcon.TRASH.create(), e -> deleteButtonClicked());
		deleteButton.getElement().setAttribute("title", getTranslation("Delete"));
//        crudLayout.addToolbarComponent(deleteButton);

		updateButtons();
	}

	/**
	 * Show form with a delete button.
	 *
	 * @see org.vaadin.crudui.crud.impl.GridCrud#showForm(org.vaadin.crudui.crud.CrudOperation,
	 *      java.lang.Object, boolean, java.lang.String,
	 *      com.vaadin.flow.component.ComponentEventListener)
	 */
	@Override
	protected void showForm(CrudOperation operation, T domainObject, boolean readOnly, String successMessage,
	        ComponentEventListener<ClickEvent<Button>> unused) {
		OwlcmsCrudFormFactory<T> owlcmsCrudFormFactory = (OwlcmsCrudFormFactory<T>) this.getCrudFormFactory();
		Component form = owlcmsCrudFormFactory.buildNewForm(operation, domainObject, readOnly,
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
				        LoggerUtils.logError(logger, e);
			        }
		        },
		        deleteButtonClickEvent -> {
			        logger.debug("preDelete");
			        owlcmsGridLayout.hideForm();
			        this.deleteButtonClicked();
		        });

		String caption = owlcmsCrudFormFactory.buildCaption(operation, domainObject);
		owlcmsGridLayout.showForm(operation, form, caption);
	}

}
