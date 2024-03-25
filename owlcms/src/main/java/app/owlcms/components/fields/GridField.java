/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.fields;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.NativeLabel;

import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class GridField<T> extends CustomField<List<T>> {

	Collection<Button> editButtons = Collections.newSetFromMap(new WeakHashMap<>());
	Logger logger = (Logger) LoggerFactory.getLogger(GridField.class);
	protected Grid<T> grid;
	protected NativeLabel message;
	private List<T> presentationStrings = new ArrayList<>();
	private Object draggedItem;

	public GridField(boolean b, String messageString) {
		this(new ArrayList<>(), b, messageString);
	}

	@SuppressWarnings("unchecked")
	public GridField(List<T> rows, boolean draggable, String messageString) {
		super(new ArrayList<>(rows));

		this.message = new NativeLabel();
		this.message.setText(messageString);
		// message.getStyle().set("background-color", "var(--lumo-error-color-10pct)");
		this.message.getStyle().set("color", "red");
		this.message.getStyle().set("margin-left", "1em");

		this.grid = new Grid<>();
		createColumns();

		this.grid.setAllRowsVisible(true);
		this.grid.setSizeUndefined();
		this.grid.setRowsDraggable(draggable);

		this.grid.addDragStartListener(
		        event -> {
			        // store current dragged item so we know what to drop
			        this.draggedItem = event.getDraggedItems().get(0);
			        this.grid.setDropMode(GridDropMode.BETWEEN);
		        });

		this.grid.addDragEndListener(
		        event -> {
			        this.draggedItem = null;
			        // Once dragging has ended, disable drop mode so that
			        // it won't look like other dragged items can be dropped
			        this.grid.setDropMode(null);
		        });

		this.grid.addDropListener(
		        event -> {
			        T dropOverItem = event.getDropTargetItem().get();
			        if (!dropOverItem.equals(this.draggedItem)) {
				        // reorder dragged item the backing gridItems container
				        this.presentationStrings.remove(this.draggedItem);
				        // calculate drop index based on the dropOverItem
				        int droppedOverIndex = this.presentationStrings.indexOf(dropOverItem);
				        int dropIndex = droppedOverIndex + (event.getDropLocation() == GridDropLocation.BELOW ? 1 : 0);
				        this.presentationStrings.add(dropIndex, (T) this.draggedItem);
				        this.grid.getDataProvider().refreshAll();
			        }
		        });

		this.setWidth("50em");

		updatePresentation();
		add(this.message, this.grid);

	}

	@Override
	public List<T> getValue() {
		return this.presentationStrings;
	}

	protected void createColumns() {
		this.grid.addColumn(T::toString);
	}

	@Override
	protected List<T> generateModelValue() {
		// the presentation objects are already model values and no conversion is
		// necessary
		// the business model can use them as is; the model ignores the categories with
		// no age group
		// the database ids are also preserved in the copy, and used to update the
		// database
		return this.presentationStrings;
	}

	@Override
	protected void setPresentationValue(List<T> iCategories) {
		this.presentationStrings = iCategories;
		updatePresentation();
	}

	private void updatePresentation() {
		if (this.presentationStrings != null && this.presentationStrings.size() > 0) {
			this.message.getStyle().set("display", "none");
			this.grid.getStyle().set("display", "block");
			this.grid.setItems(this.presentationStrings);
			this.grid.setSizeUndefined();
		} else {
			this.message.getStyle().set("display", "block");
			this.grid.getStyle().set("display", "none");
		}

	}

}