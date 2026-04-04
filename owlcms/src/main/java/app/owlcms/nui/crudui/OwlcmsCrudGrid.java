/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.crudui;

import java.util.List;
import java.util.Optional;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.CrudOperationException;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.UI;
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

	// Focus management
	private T triggeringItem;

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

		// Initialize layout and toolbar first so buttons (findAll/add) are created
		initLayoutGrid();

		// Set up the grid reference for dialog close callbacks after init
		crudLayout.setOwlcmsCrudGrid(this);
	}

	/**
	 * Try to find and focus a TextField or ComboBox in the filter layout. Returns true if focus was moved to a component, false otherwise.
	 */
	protected Focusable<?> focusInFilterArea() {
		Component filterLayout = this.getOwlcmsGridLayout().getFilterLayout();
		if (!(filterLayout instanceof Component)) {
			return null;
		}
		// Focus first focusable child found in the filter layout
		Optional<Component> focusable = ((Component) filterLayout).getChildren()
		        .filter(c -> c instanceof com.vaadin.flow.component.Focusable)
		        .findFirst();
		if (focusable.isPresent()) {
			var f = (Focusable<?>) focusable.get();
			f.focus();
			return f;
		}
		return null;
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
		focusOutsideThenBackToTriggeringItem();
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
		// For delete, do not return to triggering item: clear it then focus outside
		this.triggeringItem = null;
		focusOutsideThenBackToTriggeringItem();
	}

	protected void deleteCallBack(T domainObject) {
		this.getOwlcmsGridLayout().hideForm();
		this.deleteButtonClicked(domainObject);
		// For delete, do not return to triggering item: clear it then focus outside
		this.triggeringItem = null;
		focusOutsideThenBackToTriggeringItem();
	}

	@Override
	protected void findAllButtonClicked() {
		this.grid.sort(null); // reset the sorting order to none - use the query result set as is.
		super.findAllButtonClicked();
	}

	/**
	 * Focus management: Focus outside the grid, then back to the triggering item. This performs a two-step focus to clear any unwanted highlights.
	 */
	protected void focusOutsideThenBackToTriggeringItem() {
		UI current = UI.getCurrent();

		// Step 1: Clear selection and focus outside the grid

		this.grid.asSingleSelect().clear();
		var focused = focusInFilterArea();
		logger.debug("focusing on filter area component {}", focused);
		current.push();

		// Step 2: After a short delay, re-select and focus the triggering item
		if (triggeringItem != null) {
			logger.debug("refocusing on triggering item {}", triggeringItem);
			if (focused != null) {
				focused.blur();
			}
			this.grid.select(triggeringItem);
			current.push();
		} else {
			// No triggering item: just focus back to the grid
			logger.debug("no triggering item, just refocusing on grid");
			this.grid.focus();
			current.push();

		}
	}

	// focusOnAddButton removed; focusOutsideThenBackToTriggeringItem handles filter-area focus

	@Override
	protected void addButtonClicked() {
		triggeringItem = null; // For add operations, no triggering item
		super.addButtonClicked();
	}

	@Override
	protected void updateButtonClicked() {
		triggeringItem = this.grid.asSingleSelect().getValue(); // Capture the selected item
		super.updateButtonClicked();
	}

	/**
	 * Do nothing. Initialization must wait for crudGrid to be constructed, constuctor calls {@link #initLayoutGrid()} instead.
	 *
	 * @see org.vaadin.crudui.crud.impl.GridCrud#initLayout()
	 */
	@Override
	protected void initLayout() {
	}

	/**
	 * Replacement initialization We do not create the crudGrid automatically, but instead receive the crudGrid pre-populated.
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

		// Ensure the toolbar layout is visible now that we've added toolbar components
		try {
			OwlcmsGridLayout gl = this.getOwlcmsGridLayout();
			if (gl != null) {
				Component toolbar = (Component) gl.getToolbarLayout();
				if (toolbar != null) {
					toolbar.setVisible(true);
				}
			}
		} catch (Exception e) {
			logger.trace("could not show toolbar layout", e);
		}

		this.updateButton = new Button(VaadinIcon.PENCIL.create(), e -> updateButtonClicked());
		this.updateButton.getElement().setAttribute("title", Translator.translate("Update"));
		// crudLayout.addToolbarComponent(updateButton);

		this.deleteButton = new Button(VaadinIcon.TRASH.create(), e -> deleteButtonClicked());
		this.deleteButton.getElement().setAttribute("title", Translator.translate("Delete"));
		// crudLayout.addToolbarComponent(deleteButton);

		// updateButtons();
	}

	protected void saveCallBack(OwlcmsCrudGrid<T> owlcmsCrudGrid, String successMessage, CrudOperation operation, T domainObject) {
		try {
			// logger.debug("postOperation {}", domainObject);
			owlcmsCrudGrid.getOwlcmsGridLayout().hideForm();
			refreshGrid();
			Notification.show(successMessage);
			logger.trace("operation performed");
			focusOutsideThenBackToTriggeringItem();
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
		}
	}

	/**
	 * Show form with a delete button.
	 *
	 * @see org.vaadin.crudui.crud.impl.GridCrud#showForm(org.vaadin.crudui.crud.CrudOperation, java.lang.Object, boolean, java.lang.String,
	 *      com.vaadin.flow.component.ComponentEventListener)
	 */
	@Override
	protected void showForm(CrudOperation operation, T domainObject, boolean readOnly, String successMessage,
	        ComponentEventListener<ClickEvent<Button>> unused) {
		// Store the item that triggered the dialog (could be new for add, or existing for update)
		triggeringItem = domainObject;

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

	/**
	 * Handle dialog close events (Escape key, clicking outside, etc.) This is called by the grid layout when the dialog is closed.
	 */
	public void handleDialogClose() {
		focusOutsideThenBackToTriggeringItem();
	}

	public void updateDialogCaption(String caption) {
		if (this.getOwlcmsGridLayout() != null) {
			this.getOwlcmsGridLayout().updateDialogCaption(caption);
		}
	}

	/**
	 * Replace the grid with a new one (e.g., when columns need to change).
	 * Preserves all filter components and layout, only replacing the grid itself.
	 *
	 * @param newGrid the new grid to use
	 */
	public void replaceGrid(Grid<T> newGrid) {
		// Remove old grid from layout
		if (this.grid != null) {
			this.grid.removeFromParent();
		}
		
		// Store the new grid
		this.grid = newGrid;
		
		// Setup the new grid the same way initLayoutGrid does
		this.grid.setSizeFull();
		this.grid.setSelectionMode(SelectionMode.SINGLE);
		
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
		
		for (Column<T> c : this.grid.getColumns()) {
			c.setResizable(true);
		}
		
		// Add to layout
		this.crudLayout.setMainComponent(this.grid);
	}

}
