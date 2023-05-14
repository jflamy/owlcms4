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
import com.vaadin.flow.component.html.Div;

import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class StringGridField extends CustomField<List<String>> {

	Collection<Button> editButtons = Collections.newSetFromMap(new WeakHashMap<>());

	Logger logger = (Logger) LoggerFactory.getLogger(StringGridField.class);

	private Grid<String> catGrid;

	private List<String> presentationStrings = new ArrayList<>();

	private Div validationStatus;

	public StringGridField(List<String> strings) {
		super(new ArrayList<String>(strings));

		validationStatus = new Div();

		catGrid = new Grid<>();
		catGrid
		        .addColumn(String::toString);
//		        .setHeader(Translator.translate("LimitForCategory"));
		catGrid.setAllRowsVisible(true);
		catGrid.setSizeUndefined();
		this.setWidth("50em");

		updatePresentation();
		add(validationStatus);
		add(catGrid);

	}

	public StringGridField() {
		this(new ArrayList<String>());
	}

	@Override
	public List<String> getValue() {
		return presentationStrings;
	}

	private void updatePresentation() {
		catGrid.setItems(presentationStrings);
		catGrid.setSizeUndefined();
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