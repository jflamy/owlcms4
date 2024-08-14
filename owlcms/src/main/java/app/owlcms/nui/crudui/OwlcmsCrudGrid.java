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
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;

import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * Class OwlcmsCrudGrid.
 *
 * @param <T> the generic type
 */
@SuppressWarnings("serial")
public class OwlcmsCrudGrid<T> extends GridCrud<T> {

	protected static final int DOUBLE_CLICK_MS_DELTA = 1000;
	public final static Logger logger = (Logger) LoggerFactory.getLogger(OwlcmsCrudGrid.class);

	// private OwlcmsCrudFormFactory<T> owlcmsCrudFormFactory;
	private OwlcmsGridLayout owlcmsGridLayout;
	private boolean clickable = true;
	protected long clicked = 0L;

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
		this.setOwlcmsGridLayout(crudLayout);
		initLayoutGrid();
	}

	public OwlcmsGridLayout getOwlcmsGridLayout() {
		return this.owlcmsGridLayout;
	}

	public boolean isClickable() {
		return this.clickable;
	}

	public void setClickable(boolean clickable) {
		this.clickable = clickable;
	}

	public void setOwlcmsGridLayout(OwlcmsGridLayout owlcmsGridLayout) {
		this.owlcmsGridLayout = owlcmsGridLayout;
	}

	public void sort(List<GridSortOrder<T>> sortOrder) {
		this.grid.sort(sortOrder);
	}

	protected void cancelCallback() {
		this.getOwlcmsGridLayout().hideForm();
		this.grid.asSingleSelect().clear();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.vaadin.crudui.crud.impl.GridCrud#deleteButtonClicked()
	 */
	@Override
	protected void deleteButtonClicked() {
		T domainObject = this.grid.asSingleSelect().getValue();
		try {
			this.deleteOperation.perform(domainObject);
			this.grid.asSingleSelect().clear();
			refreshGrid();
		} catch (CrudOperationException e1) {
			refreshGrid();
		} catch (Exception e2) {
			refreshGrid();
			throw e2;
		}
	}
	
	protected void deleteButtonClicked(T domainObject) {
		try {
			this.deleteOperation.perform(domainObject);
			refreshGrid();
		} catch (CrudOperationException e1) {
			refreshGrid();
		} catch (Exception e2) {
			refreshGrid();
			throw e2;
		}
	}

	protected void deleteCallBack() {
		this.getOwlcmsGridLayout().hideForm();
		this.deleteButtonClicked();
	}
	
	protected void deleteCallBack(T domainObject) {
		this.getOwlcmsGridLayout().hideForm();
		this.deleteButtonClicked(domainObject);
	}

	@Override
	protected void findAllButtonClicked() {
		this.grid.sort(null); // reset the sorting order to none - use the query result set as is.
		super.findAllButtonClicked();
	}

	/**
	 * Do nothing. Initialization must wait for crudGrid to be constructed, constuctor calls {@link #initLayoutGrid()}
	 * instead.
	 *
	 * @see org.vaadin.crudui.crud.impl.GridCrud#initLayout()
	 */
	@Override
	protected void initLayout() {
	}

	/**
	 * Replacement initialization We do not create the crudGrid automatically, but instead receive the crudGrid
	 * pre-populated.
	 */
	protected void initLayoutGrid() {
		initToolbar();

		this.grid.setSizeFull();
		this.grid.setSelectionMode(SelectionMode.SINGLE);

		// We do not use a selection listener; instead we handle clicks explicitely.
		// grid.addSelectionListener(e -> gridSelectionChanged());
		this.grid.addItemClickListener((e) -> {
			if (!this.isClickable()) {
				return;
			}
			long delta = System.currentTimeMillis() - this.clicked;
			if (delta > DOUBLE_CLICK_MS_DELTA) {
				this.grid.select(e.getItem());
				gridSelectionChanged();
			}
			this.clicked = System.currentTimeMillis();
		});
		this.grid.addItemDoubleClickListener((e) -> {
		});
		// grid.addCellFocusListener(e -> {
		// });

		for (Column<T> c : this.grid.getColumns()) {
			c.setResizable(true);
		}

		this.crudLayout.setMainComponent(this.grid);
	}

	/**
	 * Inits the toolbar.
	 */
	protected void initToolbar() {
		this.findAllButton = new Button(Translator.translate("RefreshList"), VaadinIcon.REFRESH.create(),
		        e -> findAllButtonClicked());
		this.findAllButton.getElement().setAttribute("title", Translator.translate("RefreshList"));
		this.crudLayout.addToolbarComponent(this.findAllButton);

		this.addButton = new Button(VaadinIcon.PLUS.create(), e -> addButtonClicked());
		getAddButton().setText(Translator.translate("Add"));
		getAddButton().addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		this.addButton.getElement().setAttribute("title", Translator.translate("Add"));
		this.crudLayout.addToolbarComponent(this.addButton);

		this.updateButton = new Button(VaadinIcon.PENCIL.create(), e -> updateButtonClicked());
		this.updateButton.getElement().setAttribute("title", Translator.translate("Update"));
		// crudLayout.addToolbarComponent(updateButton);

		this.deleteButton = new Button(VaadinIcon.TRASH.create(), e -> deleteButtonClicked());
		this.deleteButton.getElement().setAttribute("title", Translator.translate("Delete"));
		// crudLayout.addToolbarComponent(deleteButton);

		//updateButtons();
	}

	protected void saveCallBack(OwlcmsCrudGrid<T> owlcmsCrudGrid, String successMessage, CrudOperation operation, T domainObject) {
		try {
			//logger.debug("postOperation {}", domainObject);
			owlcmsCrudGrid.getOwlcmsGridLayout().hideForm();
			refreshGrid();
			Notification.show(successMessage);
			logger.trace("operation performed");
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
		}
	}

	/**
	 * Show form with a delete button.
	 *
	 * @see org.vaadin.crudui.crud.impl.GridCrud#showForm(org.vaadin.crudui.crud.CrudOperation, java.lang.Object,
	 *      boolean, java.lang.String, com.vaadin.flow.component.ComponentEventListener)
	 */
	@Override
	protected void showForm(CrudOperation operation, T domainObject, boolean readOnly, String successMessage,
	        ComponentEventListener<ClickEvent<Button>> unused) {
		OwlcmsCrudFormFactory<T> owlcmsCrudFormFactory = (OwlcmsCrudFormFactory<T>) this.getCrudFormFactory();
		Component form = owlcmsCrudFormFactory.buildNewForm(operation, domainObject, readOnly,
		        cancelButtonClickEvent -> {
			        cancelCallback();
		        },
		        operationButtonClickEvent -> {
			        saveCallBack(this, successMessage, operation, domainObject);
		        },
		        deleteButtonClickEvent -> {
			        deleteCallBack(domainObject);
		        });

		String caption = owlcmsCrudFormFactory.buildCaption(operation, domainObject);
		this.getOwlcmsGridLayout().showForm(operation, form, caption);
	}

}
