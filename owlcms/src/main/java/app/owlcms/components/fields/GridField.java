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
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;

import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class GridField<T> extends CustomField<List<T>> {

	Collection<Button> editButtons = Collections.newSetFromMap(new WeakHashMap<>());

	Logger logger = (Logger) LoggerFactory.getLogger(GridField.class);

	protected Grid<T> grid;
	protected Label message;

	private List<T> presentationStrings = new ArrayList<>();

	private Object draggedItem;

	@SuppressWarnings("unchecked")
	public GridField(List<T> rows, boolean draggable, String messageString) {
		super(new ArrayList<T>(rows));
		
		message = new Label();
		message.setText(messageString);
		//message.getStyle().set("background-color", "var(--lumo-error-color-10pct)");
		message.getStyle().set("color", "red");
		message.getStyle().set("margin-left", "1em");
		
		grid = new Grid<>();
		createColumns();

		grid.setAllRowsVisible(true);
		grid.setSizeUndefined();
		grid.setRowsDraggable(draggable);
		
        grid.addDragStartListener(
            event -> {
                // store current dragged item so we know what to drop
                draggedItem = event.getDraggedItems().get(0);
                grid.setDropMode(GridDropMode.BETWEEN);
            }
        );

        grid.addDragEndListener(
            event -> {
                draggedItem = null;
                // Once dragging has ended, disable drop mode so that
                // it won't look like other dragged items can be dropped
                grid.setDropMode(null);
            }
        );

        grid.addDropListener(
            event -> {
                T dropOverItem = event.getDropTargetItem().get();
                if (!dropOverItem.equals(draggedItem)) {
                    // reorder dragged item the backing gridItems container
                    presentationStrings.remove(draggedItem);
                    // calculate drop index based on the dropOverItem
                    int droppedOverIndex = presentationStrings.indexOf(dropOverItem);
					int dropIndex =  droppedOverIndex + (event.getDropLocation() == GridDropLocation.BELOW ? 1 : 0);
                    presentationStrings.add(dropIndex, (T) draggedItem);
                    grid.getDataProvider().refreshAll();
                }
            }
        );
		
		this.setWidth("50em");

		updatePresentation();
		add(message,grid);

	}

	protected void createColumns() {
		grid.addColumn(T::toString);
	}

	public GridField(boolean b, String messageString) {
		this(new ArrayList<T>(), b, messageString);
	}

	@Override
	public List<T> getValue() {
		return presentationStrings;
	}

	private void updatePresentation() {
		if (presentationStrings != null && presentationStrings.size() > 0) {
			message.getStyle().set("display", "none");
			grid.getStyle().set("display", "block");
			grid.setItems(presentationStrings);
			grid.setSizeUndefined();
		} else {
			message.getStyle().set("display", "block");
			grid.getStyle().set("display", "none");
		}

	}

	@Override
	protected List<T> generateModelValue() {
		// the presentation objects are already model values and no conversion is
		// necessary
		// the business model can use them as is; the model ignores the categories with
		// no age group
		// the database ids are also preserved in the copy, and used to update the
		// database
		return presentationStrings;
	}

	@Override
	protected void setPresentationValue(List<T> iCategories) {
		presentationStrings = iCategories;
		updatePresentation();
	}

}