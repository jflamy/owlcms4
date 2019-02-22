/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */

package org.ledocte.owlcms.ui.lifting;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import org.ledocte.owlcms.utils.ValidationUtils;
import org.vaadin.crudui.crud.CrudOperation;

import com.github.appreciated.css.grid.GridLayoutComponent.ColumnAlign;
import com.github.appreciated.css.grid.GridLayoutComponent.RowAlign;
import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.css.grid.sizes.Int;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.css.grid.sizes.Repeat;
import com.github.appreciated.layout.GridLayout;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.ClassList;

@SuppressWarnings("serial")
public class AthleteCardFormFactory extends OwlcmsCrudFormFactory<Athlete> {

	private static final int HEADER = 1;
	private static final int AUTOMATIC = HEADER + 1;
	private static final int DECLARATION = AUTOMATIC + 1;
	private static final int CHANGE1 = DECLARATION + 1;
	private static final int CHANGE2 = CHANGE1 + 1;
	private static final int ACTUAL = CHANGE2 + 1;

	private static final int LEFT = 1;
	private static final int SNATCH1 = LEFT + 1;
	private static final int SNATCH2 = SNATCH1 + 1;
	private static final int SNATCH3 = SNATCH2 + 1;
	private static final int CJ1 = SNATCH3 + 1;
	private static final int CJ2 = CJ1 + 1;
	private static final int CJ3 = CJ2 + 1;
	private Label errorLabel;

	public AthleteCardFormFactory(Class<Athlete> domainType) {
		super(domainType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ledocte.owlcms.ui.crudui.OwlcmsCrudFormFactory#buildNewForm(org.vaadin.
	 * crudui.crud.CrudOperation, java.lang.Object, boolean,
	 * com.vaadin.flow.component.ComponentEventListener,
	 * com.vaadin.flow.component.ComponentEventListener,
	 * com.vaadin.flow.component.ComponentEventListener)
	 */
	@Override
	public Component buildNewForm(CrudOperation operation, Athlete a, boolean readOnly,
			ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener) {
		FormLayout formLayout = new FormLayout();
		formLayout.setSizeFull();
		if (this.responsiveSteps != null) {
			formLayout.setResponsiveSteps(this.responsiveSteps);
		}

		GridLayout gridLayout = setupGrid();
		errorLabel = new Label();
		bindGridFields(operation, a, gridLayout);

		Component footerLayout = this
			.buildFooter(operation, a, cancelButtonClickListener, updateButtonClickListener, deleteButtonClickListener);

		com.vaadin.flow.component.orderedlayout.VerticalLayout mainLayout = new VerticalLayout(formLayout,
				gridLayout,
				errorLabel,
				footerLayout);
		gridLayout.setSizeFull();
		mainLayout.setFlexGrow(1, gridLayout);
		mainLayout.setHorizontalComponentAlignment(Alignment.END, footerLayout);
		mainLayout.setMargin(false);
		mainLayout.setPadding(false);
		return mainLayout;
	}

	protected void bindGridFields(CrudOperation operation, Athlete a, GridLayout gridLayout) {
		binder = buildBinder(operation, a);

		TextField snatch2AutomaticProgression = new TextField();
		snatch2AutomaticProgression.setReadOnly(true);
		binder.forField(snatch2AutomaticProgression)
			.bind(Athlete::getSnatch2AutomaticProgression, Athlete::setSnatch2AutomaticProgression);
		atRowAndColumn(gridLayout, snatch2AutomaticProgression, AUTOMATIC, SNATCH2);
		TextField snatch3AutomaticProgression = new TextField();
		snatch3AutomaticProgression.setReadOnly(true);
		binder.forField(snatch3AutomaticProgression)
			.bind(Athlete::getSnatch3AutomaticProgression, Athlete::setSnatch3AutomaticProgression);
		atRowAndColumn(gridLayout, snatch3AutomaticProgression, AUTOMATIC, SNATCH3);

		TextField snatch1Declaration = new TextField();
		binder.bind(snatch1Declaration, Athlete::getSnatch1Declaration, Athlete::setSnatch1Declaration);
		atRowAndColumn(gridLayout, snatch1Declaration, DECLARATION, SNATCH1);
		TextField snatch2Declaration = new TextField();
		binder.bind(snatch2Declaration, Athlete::getSnatch2Declaration, Athlete::setSnatch2Declaration);
		atRowAndColumn(gridLayout, snatch2Declaration, DECLARATION, SNATCH2);
		TextField snatch3Declaration = new TextField();
		binder.forField(snatch3Declaration)
			.bind(Athlete::getSnatch3Declaration, Athlete::setSnatch3Declaration);
		atRowAndColumn(gridLayout, snatch3Declaration, DECLARATION, SNATCH3);

		TextField snatch1Change1 = new TextField();
		binder.forField(snatch1Change1)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateSnatch1Change1(v)))
			.bind(Athlete::getSnatch1Change1, Athlete::setSnatch1Change1);
		atRowAndColumn(gridLayout, snatch1Change1, CHANGE1, SNATCH1);
		TextField snatch2Change1 = new TextField();
		binder.forField(snatch2Change1)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateSnatch2Change1(v)))
			.bind(Athlete::getSnatch2Change1, Athlete::setSnatch2Change1);
		atRowAndColumn(gridLayout, snatch2Change1, CHANGE1, SNATCH2);
		TextField snatch3Change1 = new TextField();
		binder.forField(snatch3Change1)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateSnatch3Change1(v)))
			.bind(Athlete::getSnatch3Change1, Athlete::setSnatch3Change1);
		atRowAndColumn(gridLayout, snatch3Change1, CHANGE1, SNATCH3);

		TextField snatch1Change2 = new TextField();
		binder.forField(snatch1Change2)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateSnatch1Change2(v)))
			.bind(Athlete::getSnatch1Change2, Athlete::setSnatch1Change2);
		atRowAndColumn(gridLayout, snatch1Change2, CHANGE2, SNATCH1);
		TextField snatch2Change2 = new TextField();
		binder.forField(snatch2Change2)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateSnatch2Change2(v)))
			.bind(Athlete::getSnatch2Change2, Athlete::setSnatch2Change2);
		atRowAndColumn(gridLayout, snatch2Change2, CHANGE2, SNATCH2);
		TextField snatch3Change2 = new TextField();
		binder.forField(snatch3Change2)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateSnatch3Change2(v)))
			.bind(Athlete::getSnatch3Change2, Athlete::setSnatch3Change2);
		atRowAndColumn(gridLayout, snatch3Change2, CHANGE2, SNATCH3);

		TextField snatch1ActualLift = new TextField();
		binder.forField(snatch1ActualLift)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateSnatch1ActualLift(v)))
			.withStatusLabel(errorLabel)
			.bind(Athlete::getSnatch1ActualLift, Athlete::setSnatch1ActualLift);
		atRowAndColumn(gridLayout, snatch1ActualLift, ACTUAL, SNATCH1);
		TextField snatch2ActualLift = new TextField();
		binder.forField(snatch2ActualLift)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateSnatch2ActualLift(v)))
			.bind(Athlete::getSnatch2ActualLift, Athlete::setSnatch2ActualLift);
		atRowAndColumn(gridLayout, snatch2ActualLift, ACTUAL, SNATCH2);
		TextField snatch3ActualLift = new TextField();
		binder.forField(snatch3ActualLift)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateSnatch3ActualLift(v)))
			.bind(Athlete::getSnatch3ActualLift, Athlete::setSnatch3ActualLift);
		atRowAndColumn(gridLayout, snatch3ActualLift, ACTUAL, SNATCH3);
		

		TextField cj2AutomaticProgression = new TextField();
		cj2AutomaticProgression.setReadOnly(true);
		binder.forField(cj2AutomaticProgression)
			.bind(Athlete::getCleanJerk2AutomaticProgression, Athlete::setCleanJerk2AutomaticProgression);
		atRowAndColumn(gridLayout, cj2AutomaticProgression, AUTOMATIC, CJ2);
		TextField cj3AutomaticProgression = new TextField();
		cj3AutomaticProgression.setReadOnly(true);
		binder.forField(cj3AutomaticProgression)
			.bind(Athlete::getCleanJerk3AutomaticProgression, Athlete::setCleanJerk3AutomaticProgression);
		atRowAndColumn(gridLayout, cj3AutomaticProgression, AUTOMATIC, CJ3);

		TextField cj1Declaration = new TextField();
		binder.bind(cj1Declaration, Athlete::getCleanJerk1Declaration, Athlete::setCleanJerk1Declaration);
		atRowAndColumn(gridLayout, cj1Declaration, DECLARATION, CJ1);
		TextField cj2Declaration = new TextField();
		binder.bind(cj2Declaration, Athlete::getCleanJerk2Declaration, Athlete::setCleanJerk2Declaration);
		atRowAndColumn(gridLayout, cj2Declaration, DECLARATION, CJ2);
		TextField cj3Declaration = new TextField();
		binder.forField(cj3Declaration)
			.bind(Athlete::getCleanJerk3Declaration, Athlete::setCleanJerk3Declaration);
		atRowAndColumn(gridLayout, cj3Declaration, DECLARATION, CJ3);

		TextField cj1Change1 = new TextField();
		binder.forField(cj1Change1)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateCleanJerk1Change1(v)))
			.bind(Athlete::getCleanJerk1Change1, Athlete::setCleanJerk1Change1);
		atRowAndColumn(gridLayout, cj1Change1, CHANGE1, CJ1);
		TextField cj2Change1 = new TextField();
		binder.forField(cj2Change1)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateCleanJerk2Change1(v)))
			.bind(Athlete::getCleanJerk2Change1, Athlete::setCleanJerk2Change1);
		atRowAndColumn(gridLayout, cj2Change1, CHANGE1, CJ2);
		TextField cj3Change1 = new TextField();
		binder.forField(cj3Change1)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateCleanJerk3Change1(v)))
			.bind(Athlete::getCleanJerk3Change1, Athlete::setCleanJerk3Change1);
		atRowAndColumn(gridLayout, cj3Change1, CHANGE1, CJ3);

		TextField cj1Change2 = new TextField();
		binder.forField(cj1Change2)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateCleanJerk1Change2(v)))
			.bind(Athlete::getCleanJerk1Change2, Athlete::setCleanJerk1Change2);
		atRowAndColumn(gridLayout, cj1Change2, CHANGE2, CJ1);
		TextField cj2Change2 = new TextField();
		binder.forField(cj2Change2)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateCleanJerk2Change2(v)))
			.bind(Athlete::getCleanJerk2Change2, Athlete::setCleanJerk2Change2);
		atRowAndColumn(gridLayout, cj2Change2, CHANGE2, CJ2);
		TextField cj3Change2 = new TextField();
		binder.forField(cj3Change2)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateCleanJerk3Change2(v)))
			.bind(Athlete::getCleanJerk3Change2, Athlete::setCleanJerk3Change2);
		atRowAndColumn(gridLayout, cj3Change2, CHANGE2, CJ3);

		TextField cj1ActualLift = new TextField();
		binder.forField(cj1ActualLift)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateCleanJerk1ActualLift(v)))
			.bind(Athlete::getCleanJerk1ActualLift, Athlete::setCleanJerk1ActualLift);
		atRowAndColumn(gridLayout, cj1ActualLift, ACTUAL, CJ1);
		TextField cj2ActualLift = new TextField();
		binder.forField(cj2ActualLift)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateCleanJerk2ActualLift(v)))
			.bind(Athlete::getCleanJerk2ActualLift, Athlete::setCleanJerk2ActualLift);
		atRowAndColumn(gridLayout, cj2ActualLift, ACTUAL, CJ2);
		TextField cj3ActualLift = new TextField();
		binder.forField(cj3ActualLift)
			.withValidator(ValidationUtils.checkUsing(v -> a.validateCleanJerk3ActualLift(v)))
			.bind(Athlete::getCleanJerk3ActualLift, Athlete::setCleanJerk3ActualLift);
		atRowAndColumn(gridLayout, cj3ActualLift, ACTUAL, CJ3);

		binder.readBean(a);
	}

	private void atRowAndColumn(GridLayout gridLayout, Component component, int row,
			int column) {
		atRowAndColumn(gridLayout, component, row, column, RowAlign.CENTER, ColumnAlign.CENTER);
	}

	protected GridLayout setupGrid() {
		GridLayout gridLayout = new GridLayout();
		gridLayout.setTemplateRows(new Repeat(ACTUAL, new Flex(1)));
		gridLayout.setTemplateColumns(new Repeat(CJ3, new Flex(1)));
		gridLayout.setGap(new Length("0.8ex"), new Length("1.2ex"));

		// column headers
		atRowAndColumn(gridLayout, new Label("snatch 1"), HEADER, SNATCH1, RowAlign.CENTER, ColumnAlign.CENTER);
		atRowAndColumn(gridLayout, new Label("snatch 2"), HEADER, SNATCH2, RowAlign.CENTER, ColumnAlign.CENTER);
		atRowAndColumn(gridLayout, new Label("snatch 3"), HEADER, SNATCH3, RowAlign.CENTER, ColumnAlign.CENTER);
		atRowAndColumn(gridLayout, new Label("C&J 1"), HEADER, CJ1, RowAlign.CENTER, ColumnAlign.CENTER);
		atRowAndColumn(gridLayout, new Label("C&J 2"), HEADER, CJ2, RowAlign.CENTER, ColumnAlign.CENTER);
		atRowAndColumn(gridLayout, new Label("C&J 3"), HEADER, CJ3, RowAlign.CENTER, ColumnAlign.CENTER);

		// row headings
		atRowAndColumn(gridLayout, new Label("Automatic Progression"), AUTOMATIC, LEFT, RowAlign.CENTER, ColumnAlign.END);
		atRowAndColumn(gridLayout, new Label("Declaration"), DECLARATION, LEFT, RowAlign.CENTER, ColumnAlign.END);
		atRowAndColumn(gridLayout, new Label("Change 1"), CHANGE1, LEFT, RowAlign.CENTER, ColumnAlign.END);
		atRowAndColumn(gridLayout, new Label("Change 2"), CHANGE2, LEFT, RowAlign.CENTER, ColumnAlign.END);
		atRowAndColumn(gridLayout, new Label("Weight Lifted"), ACTUAL, LEFT, RowAlign.CENTER, ColumnAlign.END);

		return gridLayout;
	}

	private void atRowAndColumn(GridLayout gridLayout, Component component, int row, int column, RowAlign ra, ColumnAlign ca) {
		gridLayout.add(component);
		gridLayout.setRowAndColumn(component, new Int(row), new Int(column), new Int(row), new Int(column));
		gridLayout.setRowAlign(component, ra);
		gridLayout.setColumnAlign(component, ca);
		component.getElement()
			.getStyle()
			.set("width", "6em");
		if (row == ACTUAL && column > LEFT) {
			TextField field = (TextField) component;
			field.addValueChangeListener(e -> setGoodBadStyle(e));
		}

	}

	private void setGoodBadStyle(ComponentValueChangeEvent<TextField, String> e) {
		String value = e.getValue();
		ClassList classNames = e.getSource()
			.getClassNames();
		classNames.remove("bad");
		classNames.remove("good");
		if (value != null && value.trim()
			.length() > 0) {
			if (value.startsWith("-")) {
				classNames.add("bad");
			} else {
				classNames.add("good");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ledocte.owlcms.ui.crudui.OwlcmsCrudFormFactory#buildFooter(org.vaadin.
	 * crudui.crud.CrudOperation, java.lang.Object,
	 * com.vaadin.flow.component.ComponentEventListener,
	 * com.vaadin.flow.component.ComponentEventListener,
	 * com.vaadin.flow.component.ComponentEventListener)
	 */
	@Override
	protected Component buildFooter(CrudOperation operation, Athlete domainObject,
			ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener) {
		return super.buildFooter(operation,
			domainObject,
			cancelButtonClickListener,
			updateButtonClickListener,
			null);
	}

}
