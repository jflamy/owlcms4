/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */

package app.owlcms.ui.group;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;

import com.github.appreciated.css.grid.GridLayoutComponent.ColumnAlign;
import com.github.appreciated.css.grid.GridLayoutComponent.RowAlign;
import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.css.grid.sizes.Int;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.css.grid.sizes.Repeat;
import com.github.appreciated.layout.GridLayout;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.ClassList;

import app.owlcms.components.crudui.OwlcmsCrudFormFactory;
import app.owlcms.components.fields.ValidationUtils;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class AthleteCardFormFactory extends OwlcmsCrudFormFactory<Athlete> {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteCardFormFactory.class);

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

	private TextField cj2AutomaticProgression;
	private TextField cj3AutomaticProgression;
	private TextField cj1ActualLift;
	private TextField cj2ActualLift;
	private TextField cj3ActualLift;

	private TextField snatch2AutomaticProgression;
	private TextField snatch3AutomaticProgression;
	private TextField snatch1ActualLift;
	private TextField snatch2ActualLift;
	private TextField snatch3ActualLift;

	/**
	 * text field array to facilitate setting focus when form is opened
	 */
	TextField[][] textfields = new TextField[ACTUAL][CJ3];

	private Athlete editedAthlete;
	private Athlete originalAthlete;

	private AthleteGridContent origin;

	public AthleteCardFormFactory(Class<Athlete> domainType, AthleteGridContent origin) {
		super(domainType);
		this.origin = origin;
	}

	/* (non-Javadoc)
	 * @see
	 * org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory#buildCaption(org.vaadin.crudui.
	 * crud.CrudOperation, java.lang.Object) */
	@Override
	public String buildCaption(CrudOperation operation, final Athlete aFromDb) {
		// If getFullId() is null, caller will build a defaut caption, so this is safe
		return aFromDb.getFullId();
	}

	/* (non-Javadoc)
	 * @see app.owlcms.components.crudui.OwlcmsCrudFormFactory#buildNewForm(org.vaadin.
	 * crudui.crud.CrudOperation, java.lang.Object, boolean,
	 * com.vaadin.flow.component.ComponentEventListener,
	 * com.vaadin.flow.component.ComponentEventListener,
	 * com.vaadin.flow.component.ComponentEventListener) */
	@Override
	public Component buildNewForm(CrudOperation operation, Athlete aFromDb, boolean readOnly,
			ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener) {
		logger.warn("building athlete editing form {}",LoggerUtils.whereFrom());
		FormLayout formLayout = new FormLayout();
		formLayout.setSizeFull();
		if (this.responsiveSteps != null) {
			formLayout.setResponsiveSteps(this.responsiveSteps);
		}

		GridLayout gridLayout = setupGrid();
		errorLabel = new Label();
		errorLabel.addClassName("errorMessage");

		// We use a copy so that if the user cancels, we still have the original object.
		// This allows us to use cleaner validation methods coded in the Athlete class as opposed to
		// tedious validations on the form fields using getValue().
		editedAthlete = new Athlete();
		originalAthlete = aFromDb;
		Athlete.copyLifts(editedAthlete, originalAthlete);
		
		logger.warn("aFromDb = {} {}", System.identityHashCode(aFromDb), LoggerUtils.whereFrom());
		logger.warn("originalAthlete = {} {}", System.identityHashCode(originalAthlete), LoggerUtils.whereFrom());
		logger.warn("editedAthlete = {}", System.identityHashCode(editedAthlete));
		bindGridFields(operation, gridLayout);

		Component footerLayout = this
			.buildFooter(operation, editedAthlete, cancelButtonClickListener, updateButtonClickListener,
				deleteButtonClickListener);

		com.vaadin.flow.component.orderedlayout.VerticalLayout mainLayout = new VerticalLayout(
				formLayout,
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

	public TextField createActualWeightField() {
		TextField tf = new TextField();
		tf.setPattern("^[-]{0,1}\\d*$");
		tf.setPreventInvalidInput(true);
		tf.setValueChangeMode(ValueChangeMode.ON_BLUR);
		return tf;
	}

	public TextField createPositiveWeightField() {
		TextField tf = new TextField();
		tf.setPattern("^\\d*$");
		tf.setPreventInvalidInput(true);
		tf.setValueChangeMode(ValueChangeMode.ON_BLUR);
		return tf;
	}

	protected void bindGridFields(CrudOperation operation, GridLayout gridLayout) {
		binder = buildBinder(operation, editedAthlete);

		TextField snatch1Declaration = createPositiveWeightField();
		binder.forField(snatch1Declaration)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateSnatch1Declaration(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getSnatch1Declaration, Athlete::setSnatch1Declaration);
		atRowAndColumn(gridLayout, snatch1Declaration, DECLARATION, SNATCH1);

		TextField snatch1Change1 = createPositiveWeightField();
		binder.forField(snatch1Change1)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateSnatch1Change1(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getSnatch1Change1, Athlete::setSnatch1Change1);
		atRowAndColumn(gridLayout, snatch1Change1, CHANGE1, SNATCH1);

		TextField snatch1Change2 = createPositiveWeightField();
		binder.forField(snatch1Change2)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateSnatch1Change2(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getSnatch1Change2, Athlete::setSnatch1Change2);
		atRowAndColumn(gridLayout, snatch1Change2, CHANGE2, SNATCH1);

		snatch1ActualLift = createActualWeightField();
		binder.forField(snatch1ActualLift)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateSnatch1ActualLift(v)))
			.withValidator(ValidationUtils.checkUsing(v -> setAutomaticProgressions(editedAthlete)))
			.withValidationStatusHandler(status -> setActualLiftStyle(status))
			.bind(Athlete::getSnatch1ActualLift, Athlete::setSnatch1ActualLift);
		atRowAndColumn(gridLayout, snatch1ActualLift, ACTUAL, SNATCH1);

		snatch2AutomaticProgression = new TextField();
		snatch2AutomaticProgression.setReadOnly(true);
		snatch2AutomaticProgression.setTabIndex(-1);
		binder.forField(snatch2AutomaticProgression)
			.bind(Athlete::getSnatch2AutomaticProgression, Athlete::setSnatch2AutomaticProgression);
		atRowAndColumn(gridLayout, snatch2AutomaticProgression, AUTOMATIC, SNATCH2);

		TextField snatch2Declaration = createPositiveWeightField();
		binder.forField(snatch2Declaration)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateSnatch2Declaration(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getSnatch2Declaration, Athlete::setSnatch2Declaration);
		atRowAndColumn(gridLayout, snatch2Declaration, DECLARATION, SNATCH2);

		TextField snatch2Change1 = createPositiveWeightField();
		binder.forField(snatch2Change1)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateSnatch2Change1(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getSnatch2Change1, Athlete::setSnatch2Change1);
		atRowAndColumn(gridLayout, snatch2Change1, CHANGE1, SNATCH2);

		TextField snatch2Change2 = createPositiveWeightField();
		binder.forField(snatch2Change2)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateSnatch2Change2(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getSnatch2Change2, Athlete::setSnatch2Change2);
		atRowAndColumn(gridLayout, snatch2Change2, CHANGE2, SNATCH2);

		snatch2ActualLift = createActualWeightField();
		binder.forField(snatch2ActualLift)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateSnatch2ActualLift(v)))
			.withValidator(ValidationUtils.checkUsing(v -> setAutomaticProgressions(editedAthlete)))
			.withValidationStatusHandler(status -> setActualLiftStyle(status))
			.bind(Athlete::getSnatch2ActualLift, Athlete::setSnatch2ActualLift);
		atRowAndColumn(gridLayout, snatch2ActualLift, ACTUAL, SNATCH2);

		snatch3AutomaticProgression = new TextField();
		snatch3AutomaticProgression.setReadOnly(true);
		snatch3AutomaticProgression.setTabIndex(-1);
		binder.forField(snatch3AutomaticProgression)
			.bind(Athlete::getSnatch3AutomaticProgression, Athlete::setSnatch3AutomaticProgression);
		atRowAndColumn(gridLayout, snatch3AutomaticProgression, AUTOMATIC, SNATCH3);

		TextField snatch3Declaration = createPositiveWeightField();
		binder.forField(snatch3Declaration)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateSnatch3Declaration(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getSnatch3Declaration, Athlete::setSnatch3Declaration);
		atRowAndColumn(gridLayout, snatch3Declaration, DECLARATION, SNATCH3);

		TextField snatch3Change1 = createPositiveWeightField();
		binder.forField(snatch3Change1)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateSnatch3Change1(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getSnatch3Change1, Athlete::setSnatch3Change1);
		atRowAndColumn(gridLayout, snatch3Change1, CHANGE1, SNATCH3);

		TextField snatch3Change2 = createPositiveWeightField();
		binder.forField(snatch3Change2)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateSnatch3Change2(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getSnatch3Change2, Athlete::setSnatch3Change2);
		atRowAndColumn(gridLayout, snatch3Change2, CHANGE2, SNATCH3);

		snatch3ActualLift = createActualWeightField();
		binder.forField(snatch3ActualLift)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateSnatch3ActualLift(v)))
			.withValidationStatusHandler(status -> setActualLiftStyle(status))
			.bind(Athlete::getSnatch3ActualLift, Athlete::setSnatch3ActualLift);
		atRowAndColumn(gridLayout, snatch3ActualLift, ACTUAL, SNATCH3);

		TextField cj1Declaration = createPositiveWeightField();
		binder.forField(cj1Declaration)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateCleanJerk1Declaration(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getCleanJerk1Declaration, Athlete::setCleanJerk1Declaration);
		atRowAndColumn(gridLayout, cj1Declaration, DECLARATION, CJ1);

		TextField cj1Change1 = createPositiveWeightField();
		binder.forField(cj1Change1)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateCleanJerk1Change1(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getCleanJerk1Change1, Athlete::setCleanJerk1Change1);
		atRowAndColumn(gridLayout, cj1Change1, CHANGE1, CJ1);

		TextField cj1Change2 = createPositiveWeightField();
		binder.forField(cj1Change2)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateCleanJerk1Change2(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getCleanJerk1Change2, Athlete::setCleanJerk1Change2);
		atRowAndColumn(gridLayout, cj1Change2, CHANGE2, CJ1);

		cj1ActualLift = createActualWeightField();
		binder.forField(cj1ActualLift)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateCleanJerk1ActualLift(v)))
			.withValidator(ValidationUtils.checkUsing(v -> setAutomaticProgressions(editedAthlete)))
			.withValidationStatusHandler(status -> setActualLiftStyle(status))
			.bind(Athlete::getCleanJerk1ActualLift, Athlete::setCleanJerk1ActualLift);
		atRowAndColumn(gridLayout, cj1ActualLift, ACTUAL, CJ1);

		cj2AutomaticProgression = new TextField();
		cj2AutomaticProgression.setReadOnly(true);
		cj2AutomaticProgression.setTabIndex(-1);
		binder.forField(cj2AutomaticProgression)
			.bind(Athlete::getCleanJerk2AutomaticProgression, Athlete::setCleanJerk2AutomaticProgression);
		atRowAndColumn(gridLayout, cj2AutomaticProgression, AUTOMATIC, CJ2);

		TextField cj2Declaration = createPositiveWeightField();
		binder.forField(cj2Declaration)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateCleanJerk2Declaration(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getCleanJerk2Declaration, Athlete::setCleanJerk2Declaration);
		atRowAndColumn(gridLayout, cj2Declaration, DECLARATION, CJ2);

		TextField cj2Change1 = createPositiveWeightField();
		binder.forField(cj2Change1)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateCleanJerk2Change1(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getCleanJerk2Change1, Athlete::setCleanJerk2Change1);
		atRowAndColumn(gridLayout, cj2Change1, CHANGE1, CJ2);

		TextField cj2Change2 = createPositiveWeightField();
		binder.forField(cj2Change2)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateCleanJerk2Change2(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getCleanJerk2Change2, Athlete::setCleanJerk2Change2);
		atRowAndColumn(gridLayout, cj2Change2, CHANGE2, CJ2);

		cj2ActualLift = createActualWeightField();
		binder.forField(cj2ActualLift)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateCleanJerk2ActualLift(v)))
			.withValidator(ValidationUtils.checkUsing(v -> setAutomaticProgressions(editedAthlete)))
			.withValidationStatusHandler(status -> setActualLiftStyle(status))
			.bind(Athlete::getCleanJerk2ActualLift, Athlete::setCleanJerk2ActualLift);
		atRowAndColumn(gridLayout, cj2ActualLift, ACTUAL, CJ2);

		cj3AutomaticProgression = new TextField();
		cj3AutomaticProgression.setReadOnly(true);
		cj3AutomaticProgression.setTabIndex(-1);
		binder.forField(cj3AutomaticProgression)
			.bind(Athlete::getCleanJerk3AutomaticProgression, Athlete::setCleanJerk3AutomaticProgression);
		atRowAndColumn(gridLayout, cj3AutomaticProgression, AUTOMATIC, CJ3);

		TextField cj3Declaration = createPositiveWeightField();
		binder.forField(cj3Declaration)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateCleanJerk3Declaration(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getCleanJerk3Declaration, Athlete::setCleanJerk3Declaration);
		atRowAndColumn(gridLayout, cj3Declaration, DECLARATION, CJ3);

		TextField cj3Change1 = createPositiveWeightField();
		binder.forField(cj3Change1)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateCleanJerk3Change1(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getCleanJerk3Change1, Athlete::setCleanJerk3Change1);
		atRowAndColumn(gridLayout, cj3Change1, CHANGE1, CJ3);

		TextField cj3Change2 = createPositiveWeightField();
		binder.forField(cj3Change2)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateCleanJerk3Change2(v)))
			.withValidationStatusHandler(status -> setErrorLabel(status))
			.bind(Athlete::getCleanJerk3Change2, Athlete::setCleanJerk3Change2);
		atRowAndColumn(gridLayout, cj3Change2, CHANGE2, CJ3);

		cj3ActualLift = createActualWeightField();
		binder.forField(cj3ActualLift)
			.withValidator(ValidationUtils.checkUsing(v -> editedAthlete.validateCleanJerk3ActualLift(v)))
			.withValidationStatusHandler(status -> setActualLiftStyle(status))
			.bind(Athlete::getCleanJerk3ActualLift, Athlete::setCleanJerk3ActualLift);
		atRowAndColumn(gridLayout, cj3ActualLift, ACTUAL, CJ3);

		// use setBean so that changes are immediately reflected to the working copy.
		binder.setBean(editedAthlete);
		setFocus(editedAthlete);
	}

	public void setActualLiftStyle(BindingValidationStatus<?> status) throws NumberFormatException {
		setErrorLabel(status);
		TextField field = (TextField) status.getField();
		if (status.isError()) {
			field.getElement().getClassList().set("error", true);
			field.getElement().getClassList().set("good", false);
			field.getElement().getClassList().set("bad", false);
			field.focus();
		} else {
			String value = field.getValue();
			boolean empty = value == null || value.trim().isEmpty();
			if (empty) {
				field.getElement().getClassList().clear();
			} else {
				int intValue = Integer.parseInt(value);
				field.getElement().getClassList().clear();
				field.getElement().getClassList().set(
					(intValue <= 0 ? "bad" : "good"), true);
			}
		}
	}

	public void setErrorLabel(BindingValidationStatus<?> status) throws NumberFormatException {
		if (status.isError()) {
			ClassList classNames = ((HasStyle) status.getField()).getClassNames();
			classNames.clear();
			classNames.add("error");
			errorLabel.setText(status.getMessage().orElse("Error"));
			setVisible(errorLabel, true);
			errorLabel.getClassNames().set("errorMessage", true);
		} else {
			setVisible(errorLabel, false);
			errorLabel.getClassNames().clear();
		}
	}

	private void setVisible(HasText label, boolean visible) {
		if (visible) {
			label.getElement().getStyle().remove("display");
		} else {
			label.getElement().getStyle().set("display", "none");
		}
	}

	/* (non-Javadoc)
	 * @see app.owlcms.components.crudui.OwlcmsCrudFormFactory#buildFooter(org.vaadin.
	 * crudui.crud.CrudOperation, java.lang.Object, com.vaadin.flow.component.ComponentEventListener,
	 * com.vaadin.flow.component.ComponentEventListener,
	 * com.vaadin.flow.component.ComponentEventListener) */
	@Override
	protected Component buildFooter(CrudOperation operation, Athlete unused,
			ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener) {
		return super.buildFooter(operation,
			originalAthlete,
			cancelButtonClickListener,
			// updateButtonClickListener
				(e) -> {
					Athlete.copyLifts(originalAthlete, editedAthlete);
					// FIXME don't use the result of the JPA merge; there is a broken == test somewhere
					// that breaks recomputation if you do.
					AthleteRepository.save(originalAthlete);
					OwlcmsSession.withFop((fop) -> {
						fop.getFopEventBus().post(new FOPEvent.WeightChange(this.getOrigin(), originalAthlete));
					});
					origin.closeDialog();
				},
			null);
	}

	private Object getOrigin() {
		return origin;
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
		atRowAndColumn(gridLayout, new Label("Automatic Progression"), AUTOMATIC, LEFT, RowAlign.CENTER,
			ColumnAlign.END);
		atRowAndColumn(gridLayout, new Label("Declaration"), DECLARATION, LEFT, RowAlign.CENTER, ColumnAlign.END);
		atRowAndColumn(gridLayout, new Label("Change 1"), CHANGE1, LEFT, RowAlign.CENTER, ColumnAlign.END);
		atRowAndColumn(gridLayout, new Label("Change 2"), CHANGE2, LEFT, RowAlign.CENTER, ColumnAlign.END);
		atRowAndColumn(gridLayout, new Label("Weight Lifted"), ACTUAL, LEFT, RowAlign.CENTER, ColumnAlign.END);

		return gridLayout;
	}

	private void atRowAndColumn(GridLayout gridLayout, Component component, int row,
			int column) {
		atRowAndColumn(gridLayout, component, row, column, RowAlign.CENTER, ColumnAlign.CENTER);
	}

	private void atRowAndColumn(GridLayout gridLayout, Component component, int row, int column, RowAlign ra,
			ColumnAlign ca) {
		gridLayout.add(component);
		gridLayout.setRowAndColumn(component, new Int(row), new Int(column), new Int(row), new Int(column));
		gridLayout.setRowAlign(component, ra);
		gridLayout.setColumnAlign(component, ca);
		component.getElement()
			.getStyle()
			.set("width", "6em");
		if (component instanceof TextField) {
			TextField textField = (TextField) component;
			textfields[row - 1][column - 1] = textField;
		}

	}

	/**
	 * set the automatic progressions. This is invoked as a validator because we don't want to be called
	 * if the entered value is invalid. Only the side-effect is interesting, so we return true.
	 * 
	 * @param athlete
	 * @return true always
	 */
	private boolean setAutomaticProgressions(Athlete athlete) {
		int value = Athlete.zeroIfInvalid(snatch1ActualLift.getValue());
		int autoVal = (value <= 0 ? value : value + 1);
		snatch2AutomaticProgression.setValue(Integer.toString(autoVal));
		value = Athlete.zeroIfInvalid(snatch2ActualLift.getValue());
		autoVal = (value <= 0 ? value : value + 1);
		snatch3AutomaticProgression.setValue(Integer.toString(autoVal));

		value = Athlete.zeroIfInvalid(cj1ActualLift.getValue());
		autoVal = (value <= 0 ? value : value + 1);
		cj2AutomaticProgression.setValue(Integer.toString(autoVal));
		value = Athlete.zeroIfInvalid(cj2ActualLift.getValue());
		autoVal = (value <= 0 ? value : value + 1);
		cj3AutomaticProgression.setValue(Integer.toString(autoVal));

		return true;
	}

	private void setFocus(Athlete a) {
		int targetRow = ACTUAL + 1;
		int targetCol = CJ3 + 1;

		// figure out whether we are searching for snatch or CJ
		int rightCol;
		int leftCol;
		if (a.getAttemptsDone() >= 3) {
			rightCol = CJ3;
			leftCol = CJ1;
		} else {
			rightCol = SNATCH3;
			leftCol = SNATCH1;
		}

		// remember location of last empty cell, going backwards
		search: for (int col = rightCol; col >= leftCol; col--) {
			for (int row = ACTUAL; row > AUTOMATIC; row--) {
				boolean empty = textfields[row - 1][col - 1].isEmpty();
				if (empty) {
					targetRow = row - 1;
					targetCol = col - 1;
				} else {
					// don't go back past first non-empty (leave holes)
					break search;
				}
			}
		}

		if (targetCol <= CJ3 && targetRow <= ACTUAL) {
			// a suitable empty cell was found, set focus
			textfields[targetRow][targetCol].setAutofocus(true);
			textfields[targetRow][targetCol].setAutoselect(true);
		}
	}

	public Athlete getEditedAthlete() {
		return editedAthlete;
	}

	public Athlete getOriginalAthlete() {
		return originalAthlete;
	}
}
