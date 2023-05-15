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

import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class StringGridField extends CustomField<List<String>> {

	Collection<Button> editButtons = Collections.newSetFromMap(new WeakHashMap<>());

	Logger logger = (Logger) LoggerFactory.getLogger(StringGridField.class);

	private Grid<String> grid;

	private List<String> presentationStrings = new ArrayList<>();

	private Object draggedItem;

	public StringGridField(List<String> strings) {
		super(new ArrayList<String>(strings));
		grid = new Grid<>();
		grid.addColumn(String::toString);

		grid.setAllRowsVisible(true);
		grid.setSizeUndefined();
		grid.setRowsDraggable(true);
		
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
                String dropOverItem = event.getDropTargetItem().get();
                if (!dropOverItem.equals(draggedItem)) {
                    // reorder dragged item the backing gridItems container
                    presentationStrings.remove(draggedItem);
                    // calculate drop index based on the dropOverItem
                    int droppedOverIndex = presentationStrings.indexOf(dropOverItem);
                    logger.warn("{} {}", dropOverItem, droppedOverIndex);
					int dropIndex =  droppedOverIndex + (event.getDropLocation() == GridDropLocation.BELOW ? 1 : 0);
                    presentationStrings.add(dropIndex, (String) draggedItem);
                    grid.getDataProvider().refreshAll();
                }
            }
        );
		
		this.setWidth("50em");

		updatePresentation();
		add(grid);

	}

	public StringGridField() {
		this(new ArrayList<String>());
	}

	@Override
	public List<String> getValue() {
		return presentationStrings;
	}

	private void updatePresentation() {
		grid.setItems(presentationStrings);
		logger.warn("grid items {}",presentationStrings);
		grid.setSizeUndefined();
	}

	@Override
	protected List<String> generateModelValue() {
		// the presentation objects are already model values and no conversion is
		// necessary
		// the business model can use them as is; the model ignores the categories with
		// no age group
		// the database ids are also preserved in the copy, and used to update the
		// database
		return presentationStrings;
	}

	@Override
	protected void setPresentationValue(List<String> iCategories) {
		presentationStrings = iCategories;
		updatePresentation();
	}

}