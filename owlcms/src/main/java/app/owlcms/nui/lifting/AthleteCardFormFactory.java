/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.lifting;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
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
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.converter.StringToDoubleConverter;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.ClassList;

import app.owlcms.components.fields.ValidationUtils;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.CustomFormFactory;
import app.owlcms.nui.shared.IAthleteEditing;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.Notification;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class AthleteCardFormFactory extends OwlcmsCrudFormFactory<Athlete> implements CustomFormFactory<Athlete> {

	private static final String CHECKBOX_MARGIN = "0em";
	final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteCardFormFactory.class);
	private static final int HEADER = 1;
	private static final int AUTOMATIC = HEADER + 1;
	private static final int DECLARATION = AUTOMATIC + 1;
	private static final int CHANGE1 = DECLARATION + 1;
	private static final int CHANGE2 = CHANGE1 + 1;
	private static final int ACTUAL = CHANGE2 + 1;
	private static final int SCORE = ACTUAL + 1;
	private static final int LEFT = 1;
	private static final int SNATCH1 = LEFT + 1;
	private static final int SNATCH2 = SNATCH1 + 1;
	private static final int SNATCH3 = SNATCH2 + 1;
	private static final int CJ1 = SNATCH3 + 1;
	private static final int CJ2 = CJ1 + 1;
	private static final int CJ3 = CJ2 + 1;
	/**
	 * text field array to facilitate setting focus when form is opened
	 */
	private TextField[][] textfields = new TextField[SCORE][CJ3];
	private TextField cj1ActualLift;
	private TextField cj1Change1;
	private TextField cj1Change2;
	private TextField cj1Declaration;
	private TextField cj2ActualLift;
	private TextField cj2AutomaticProgression;
	private TextField cj2Change1;
	private TextField cj2Change2;
	private TextField cj2Declaration;
	private TextField cj3ActualLift;
	private TextField cj3AutomaticProgression;
	private TextField cj3Change1;
	private TextField cj3Change2;
	private TextField cj3Declaration;
	private Athlete editedAthlete;
	private GridLayout gridLayout;
	private Checkbox ignoreErrorsCheckbox;
	private BinderValidationStatus<Athlete> initialValidationStatus;
	private Boolean liftResultChanged;
	private Button operationButton;
	private IAthleteEditing origin;
	private Athlete originalAthlete;
	private TextField snatch1ActualLift;
	private TextField snatch1Change1;
	private TextField snatch1Change2;
	private TextField snatch1Declaration;
	private TextField snatch2ActualLift;
	private TextField snatch2AutomaticProgression;
	private TextField snatch2Change1;
	private TextField snatch2Change2;
	private TextField snatch2Declaration;
	private TextField snatch3ActualLift;
	private TextField snatch3AutomaticProgression;
	private TextField snatch3Change1;
	private TextField snatch3Change2;
	private TextField snatch3Declaration;
	private BinderValidationStatus<Athlete> status;
	private Boolean updatingResults;

	public AthleteCardFormFactory(Class<Athlete> domainType, IAthleteEditing origin) {
		super(domainType);
		this.origin = origin;
	}

	/**
	 * @see app.owlcms.nui.shared.CustomFormFactory#add(app.owlcms.data.athlete.Athlete)
	 */
	@Override
	public Athlete add(Athlete athlete) {
		AthleteRepository.save(athlete);
		return athlete;
	}

	/**
	 * Add bean-level validations
	 *
	 * @see org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory#buildBinder(org.vaadin.crudui.crud.CrudOperation,
	 *      java.lang.Object)
	 */
	@Override
	public Binder<Athlete> buildBinder(CrudOperation operation, Athlete doNotUse) {
		// we do *not* use the athlete provided by the grid selection. For some reason,
		// the grid selector returns a copy on the first invocation, instead of the
		// underlying object.
		// we use editedAthlete, which this form retrieves from the underlying data
		// source
		Athlete editedAthlete2 = getEditedAthlete();
		this.binder = super.buildBinder(operation, editedAthlete2);
		logger.trace("athlete from grid={} edited={}", doNotUse, editedAthlete2);
		return this.binder;

	}

	/**
	 * @see app.owlcms.nui.shared.CustomFormFactory#buildCaption(org.vaadin.crudui.crud.CrudOperation,
	 *      app.owlcms.data.athlete.Athlete)
	 */
	@Override
	public String buildCaption(CrudOperation operation, final Athlete aFromDb) {
		// If getFullId() is null, caller will build a default caption, so this is safe
		Integer startNumber = aFromDb.getStartNumber();
		Integer entryTotal = aFromDb.getEntryTotal();
		String entryString = "";
		if (entryTotal != null && entryTotal > 0 && Competition.getCurrent().isEnforce20kgRule()
		        && Config.getCurrent().featureSwitch("AthleteCardEntryTotal")) {
			entryString = " (" + Translator.translate("Results.Entry_abbrev") + " = " + entryTotal + ")";
		}
		return (startNumber != null ? "[" + startNumber + "] " : "") + aFromDb.getFullId() + entryString;
	}

	/**
	 * @see app.owlcms.nui.crudui.OwlcmsCrudFormFactory#buildFooter(org.vaadin.crudui.crud.CrudOperation,
	 *      java.lang.Object, com.vaadin.flow.component.ComponentEventListener,
	 *      com.vaadin.flow.component.ComponentEventListener, com.vaadin.flow.component.ComponentEventListener)
	 */
	@Override
	public Component buildFooter(CrudOperation operation, Athlete unused,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> unused2, ComponentEventListener<ClickEvent<Button>> unused3,
	        boolean shortcutEnter,
	        Button... buttons) {
		ComponentEventListener<ClickEvent<Button>> postOperationCallBack = (e) -> {
		};
		this.operationButton = null;
		if (operation == CrudOperation.UPDATE) {
			this.operationButton = buildOperationButton(CrudOperation.UPDATE, getEditedAthlete(),
			        postOperationCallBack);
		} else if (operation == CrudOperation.ADD) {
			this.operationButton = buildOperationButton(CrudOperation.ADD, getEditedAthlete(), postOperationCallBack);
		}
		Button deleteButton = buildDeleteButton(CrudOperation.DELETE, getEditedAthlete(), null);
		Component withdrawButtons = buildWithdrawButtons();
		Checkbox forcedCurrentCheckbox = buildForcedCurrentCheckbox();
		Checkbox validateEntries = buildIgnoreErrorsCheckbox();
		Checkbox allowResultsEditing = buildAllowResultsEditingCheckbox();
		Button cancelButton = buildCancelButton(cancelButtonClickListener);

		HorizontalLayout footerLayout = new HorizontalLayout();
		footerLayout.setWidth("100%");
		footerLayout.setSpacing(true);
		footerLayout.setPadding(false);

		if (deleteButton != null && operation != CrudOperation.ADD) {
			footerLayout.add(deleteButton);
		}
		if (withdrawButtons != null && operation != CrudOperation.ADD) {
			footerLayout.add(withdrawButtons);
		}
		VerticalLayout vl = new VerticalLayout();
		if (forcedCurrentCheckbox != null && operation != CrudOperation.ADD) {
			vl.setSizeUndefined();
			vl.setPadding(false);
			vl.setMargin(false);
			vl.add(validateEntries);
			vl.add(allowResultsEditing);
			vl.add(forcedCurrentCheckbox);
			footerLayout.add(vl);
		}

		NativeLabel spacer = new NativeLabel();

		footerLayout.add(spacer);// , operationTrigger);

		if (cancelButton != null) {
			footerLayout.add(cancelButton);
		}

		if (this.operationButton != null) {
			footerLayout.add(this.operationButton);
			if (operation == CrudOperation.UPDATE && shortcutEnter) {
				ShortcutRegistration reg = this.operationButton.addClickShortcut(Key.ENTER);
				reg.allowBrowserDefault();
			}
		}
		footerLayout.setFlexGrow(1.0, vl);
		return footerLayout;
	}

	/**
	 * @see app.owlcms.nui.shared.CustomFormFactory#buildNewForm(org.vaadin.crudui.crud.CrudOperation,
	 *      app.owlcms.data.athlete.Athlete, boolean, com.vaadin.flow.component.ComponentEventListener,
	 *      com.vaadin.flow.component.ComponentEventListener, com.vaadin.flow.component.ComponentEventListener,
	 *      com.vaadin.flow.component.button.Button)
	 */
	@Override
	public Component buildNewForm(CrudOperation operation, Athlete aFromList, boolean readOnly,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {
		this.setLiftResultChanged(false);

		FormLayout formLayout = new FormLayout();
		formLayout.setSizeFull();
		if (this.responsiveSteps != null) {
			formLayout.setResponsiveSteps(this.responsiveSteps);
		}
		setUpdatingResults(this.origin instanceof AnnouncerContent);

		this.gridLayout = setupGrid();
		this.errorLabel = new Paragraph("initial");
		HorizontalLayout labelWrapper = new HorizontalLayout(this.errorLabel);
		// labelWrapper.addClassName("errorMessage");
		labelWrapper.setWidthFull();
		labelWrapper.setJustifyContentMode(JustifyContentMode.CENTER);

		// We use a copy so that if the user cancels, we still have the original object.
		// This allows us to use the rich validation methods coded in the Athlete class
		// as opposed to tedious validations on the form fields using getValue().
		setEditedAthlete(new Athlete());

		// don't trust the athlete received as parameter. Fetch from database in case
		// the athlete was edited
		// on some other screen.
		if (aFromList instanceof PAthlete) {
			this.originalAthlete = ((PAthlete) aFromList)._getAthlete();
		} else {
			this.originalAthlete = aFromList;
		}
		Athlete aFromDb = AthleteRepository.findById(aFromList.getId());
		Athlete.conditionalCopy(getEditedAthlete(), aFromDb, true);

		getEditedAthlete().setValidation(false); // turn off validation in the Athlete setters; binder will call
		                                         // the validation routines explicitly

		// logger.trace("aFromDb = {} {}", System.identityHashCode(aFromList), aFromList);
		// logger.trace("originalAthlete = {} {}", System.identityHashCode(originalAthlete),
		// originalAthlete.isValidation());
		// logger.trace("editedAthlete = {} {}", System.identityHashCode(getEditedAthlete()),
		// getEditedAthlete().isValidation());

		bindGridFields(operation);

		Component footerLayout = this.buildFooter(operation, getEditedAthlete(), cancelButtonClickListener,
		        updateButtonClickListener, deleteButtonClickListener, true);

		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.add(formLayout);
		mainLayout.add(this.gridLayout);
		mainLayout.add(labelWrapper);
		mainLayout.add(footerLayout);
		this.gridLayout.setSizeFull();
		mainLayout.setFlexGrow(1, this.gridLayout);
		mainLayout.setHorizontalComponentAlignment(Alignment.END, footerLayout);
		mainLayout.setMargin(false);
		mainLayout.setPadding(false);

		getEditedAthlete().setValidation(true);
		getEditedAthlete().setStartingTotalViolation(false);
		getEditedAthlete().setCheckTiming(false);

		this.initialValidationStatus = this.binder.validate();
		setErrorLabel(this.initialValidationStatus, false);
		StringBuilder sb = getInitialErrors(this.initialValidationStatus);
		if (logger.isDebugEnabled()) {
			logger.debug("initial validation done field errors={} bean errors={}\n{}",
			        this.initialValidationStatus.getFieldValidationErrors().size(),
			        this.initialValidationStatus.getBeanValidationErrors().size(),
			        sb);
		}

		setFocus(getEditedAthlete());
		return mainLayout;
	}

	/**
	 * Special version because we use setBean instead of readBean
	 *
	 * @see app.owlcms.nui.crudui.OwlcmsCrudFormFactory#buildOperationButton(org.vaadin.crudui.crud.CrudOperation,
	 *      java.lang.Object, com.vaadin.flow.component.ComponentEventListener)
	 */
	@Override
	public Button buildOperationButton(CrudOperation operation, Athlete domainObject,
	        ComponentEventListener<ClickEvent<Button>> callBack) {
		if (callBack == null) {
			return null;
		}
		Button button = doBuildButton(operation);
		button.addClickListener((f) -> {
			performOperationAndCallback(operation, domainObject, callBack, isIgnoreErrors());
			// the field value change listener will set the following to true if the user edits using the interface
			// already initialized correctly in the form, should not be reset here.  see #
			//domainObject.setCheckTiming(false);
		});
		return button;
	}

	/**
	 * @see app.owlcms.nui.shared.CustomFormFactory#delete(app.owlcms.data.athlete.Athlete)
	 */
	@Override
	public void delete(Athlete notUsed) {
		AthleteRepository.delete(this.originalAthlete);
	}

	/**
	 * @see app.owlcms.nui.shared.CustomFormFactory#findAll()
	 */
	@Override
	public Collection<Athlete> findAll() {
		throw new UnsupportedOperationException(); // should be called on the grid
	}

	/**
	 *
	 * @param validationStatus
	 * @return
	 */
	@Override
	public boolean setErrorLabel(BinderValidationStatus<?> validationStatus, boolean updateFieldStatus) {
		this.errorLabel.setVisible(true);
		if (this.initialValidationStatus != null) {
			validationStatus = this.initialValidationStatus;
		}

		String simpleName = this.getClass().getSimpleName();

		int nberrors = validationStatus.getFieldValidationErrors().size();
		boolean hasErrors = nberrors > 0;

		validationStatus.getBinder().getFields().forEach(f -> {
			ClassList fieldClasses = ((Component) f).getElement().getClassList();
			fieldClasses.set("error", false);
			// f.setReadOnly(hasErrors);
		});
		StringBuilder sb = new StringBuilder();
		TextField field = getErrors(validationStatus, sb);
		if (logger.isDebugEnabled()) {
			logger.debug("from {} {} field errors {} bean errors -- details {}",
			        LoggerUtils.whereFrom(),
			        validationStatus.getFieldValidationErrors().size(),
			        validationStatus.getBeanValidationErrors().size(), sb.toString());
		}

		doSetErrorLabel(simpleName, sb);
		if (!hasErrors) {
			resetReadOnlyFields();
		} else if (field != null) {
			field.focus();
		}
		return hasErrors;
	}

	/**
	 * @see app.owlcms.nui.shared.CustomFormFactory#update(app.owlcms.data.athlete.Athlete)
	 */
	@Override
	public Athlete update(Athlete athleteFromDb) {
		doUpdate();
		return this.originalAthlete;
	}

	@Override
	protected void performOperationAndCallback(CrudOperation operation, Athlete domainObject,
	        ComponentEventListener<ClickEvent<Button>> gridCallback, boolean ignoreErrors) {
		if (ignoreErrors) {
			domainObject.setValidation(false);
			this.binder.setFieldsValidationStatusChangeListenerEnabled(false);
			this.binder.setValidatorsDisabled(true);
			this.binder.writeBeanAsDraft(domainObject, true);
		} else {
			boolean validObject = false;
			try {
				validObject = this.binder.writeBeanIfValid(domainObject);
				setValid(validObject);
			} catch (Throwable e) {
				// panic mode to write the object.
				setValid(false);
				this.status = this.binder.validate();
				if (!this.status.hasErrors()) {
					domainObject.setValidation(false);
					this.binder.setFieldsValidationStatusChangeListenerEnabled(false);
					this.binder.setValidatorsDisabled(true);
					this.binder.writeBeanAsDraft(domainObject, true);
					logger.error("missing user interface validation: {}", e.getMessage());
				} else {
					logger.error("uncaught errors {}", dumpErrors(this.status));
				}
			}
		}
		if (ignoreErrors || isValid()) {
			if (operation == CrudOperation.ADD) {
				logger.debug("adding {} {}", System.identityHashCode(domainObject), domainObject);
				this.add(domainObject);
				gridCallback.onComponentEvent(this.operationTriggerEvent);
			} else if (operation == CrudOperation.UPDATE) {
				logger.debug("updating 	{}", domainObject);
				this.update(domainObject);
				this.notif.setPosition(Position.TOP_END);
				this.notif.setDuration(2500);
				this.notif.open();
				gridCallback.onComponentEvent(this.operationTriggerEvent);
			} else if (operation == CrudOperation.DELETE) {
				logger.debug("deleting 	{}", domainObject);
				this.delete(domainObject);
				gridCallback.onComponentEvent(this.operationTriggerEvent);
			}
		} else {
			logger.debug("not valid {}", domainObject);
		}
	}

	private void adjustResultsFields(boolean readOnly) {
		setLiftResultClass(this.snatch1ActualLift);
		setLiftResultClass(this.snatch2ActualLift);
		setLiftResultClass(this.snatch3ActualLift);
		setLiftResultClass(this.cj1ActualLift);
		setLiftResultClass(this.cj2ActualLift);
		setLiftResultClass(this.cj3ActualLift);
	}

	private void atRowAndColumn(GridLayout gridLayout, Component component, int row, int column) {
		atRowAndColumn(gridLayout, component, row, column, RowAlign.CENTER, ColumnAlign.CENTER);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void atRowAndColumn(GridLayout gridLayout, Component component, int row, int column, RowAlign ra,
	        ColumnAlign ca) {
		gridLayout.add(component);
		gridLayout.setRowAndColumn(component, new Int(row), new Int(column), new Int(row), new Int(column));
		gridLayout.setRowAlign(component, ra);
		gridLayout.setColumnAlign(component, ca);
		component.getElement().getStyle().set("width", "6em");
		if (component instanceof TextField) {
			TextField textField = (TextField) component;
			this.textfields[row - 1][column - 1] = textField;
		}
		// if (component instanceof Focusable) {
		// ((Focusable<?>)component).setTabIndex(-1);
		// }
		if (component instanceof HasValue) {
			component.setId((row - 1) + "_" + (column - 1));
			((HasValue) component).addValueChangeListener((e) -> {
				if (!e.isFromClient()) {
					return;
				}
				getEditedAthlete().setCheckTiming(true);
				// logger.debug("setting {} to {}", component.getId().get(), e.getValue());
			});
		}
	}

	/**
	 * @param operation
	 * @param operation
	 * @param gridLayout
	 */
	private Binder<Athlete> bindGridFields(CrudOperation operation) {
		this.binder = buildBinder(null, getEditedAthlete());
		this.binder.setValidatorsDisabled(true);

		this.snatch1Declaration = createPositiveWeightField(DECLARATION, SNATCH1);
		this.binder.forField(this.snatch1Declaration)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateSnatch1Declaration(v),
		                        (s) -> doSetErrorLabel(s, this.snatch1Declaration)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.snatch1Declaration, this.snatch1ActualLift);
		        })
		        .bind(Athlete::getSnatch1Declaration, Athlete::setSnatch1Declaration);
		atRowAndColumn(this.gridLayout, this.snatch1Declaration, DECLARATION, SNATCH1);

		this.snatch1Change1 = createPositiveWeightField(CHANGE1, SNATCH1);
		this.binder.forField(this.snatch1Change1)
		        .withValidator(ValidationUtils
		                .checkUsingException(v -> isIgnoreErrors() || getEditedAthlete().validateSnatch1Change1(v),
		                        (s) -> doSetErrorLabel(s, this.snatch1Change1)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.snatch1Change1, this.snatch1ActualLift);
		        })
		        .bind(Athlete::getSnatch1Change1, Athlete::setSnatch1Change1);
		atRowAndColumn(this.gridLayout, this.snatch1Change1, CHANGE1, SNATCH1);

		this.snatch1Change2 = createPositiveWeightField(CHANGE2, SNATCH1);
		this.binder.forField(this.snatch1Change2)
		        .withValidator(ValidationUtils
		                .checkUsingException(v -> isIgnoreErrors() || getEditedAthlete().validateSnatch1Change2(v),
		                        (s) -> doSetErrorLabel(s, this.snatch1Change2)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.snatch1Change2, this.snatch1ActualLift);
		        })
		        .bind(Athlete::getSnatch1Change2, Athlete::setSnatch1Change2);
		atRowAndColumn(this.gridLayout, this.snatch1Change2, CHANGE2, SNATCH1);

		this.snatch1ActualLift = createActualWeightField(ACTUAL, SNATCH1);
		this.binder.forField(this.snatch1ActualLift)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateSnatch1ActualLift(v),
		                        (s) -> doSetErrorLabel(s, this.snatch1ActualLift)))
		        .withValidator(ValidationUtils.checkUsingException(v -> setAutomaticProgressions(getEditedAthlete())))
		        .withValidationStatusHandler(status -> setActualLiftClass(status))
		        .bind(Athlete::getSnatch1ActualLift, Athlete::setSnatch1ActualLift);
		atRowAndColumn(this.gridLayout, this.snatch1ActualLift, ACTUAL, SNATCH1);

		this.snatch2AutomaticProgression = new TextField();
		this.snatch2AutomaticProgression.setReadOnly(true);
		this.snatch2AutomaticProgression.setTabIndex(-1);
		this.binder.forField(this.snatch2AutomaticProgression).bind(Athlete::getSnatch2AutomaticProgression,
		        Athlete::setSnatch2AutomaticProgression);
		atRowAndColumn(this.gridLayout, this.snatch2AutomaticProgression, AUTOMATIC, SNATCH2);

		this.snatch2Declaration = createPositiveWeightField(DECLARATION, SNATCH2);
		this.binder.forField(this.snatch2Declaration)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateSnatch2Declaration(v),
		                        (s) -> doSetErrorLabel(s, this.snatch2Declaration)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.snatch2Declaration, this.snatch2ActualLift);
		        })
		        .bind(Athlete::getSnatch2Declaration, Athlete::setSnatch2Declaration);
		atRowAndColumn(this.gridLayout, this.snatch2Declaration, DECLARATION, SNATCH2);

		this.snatch2Change1 = createPositiveWeightField(CHANGE1, SNATCH2);
		this.binder.forField(this.snatch2Change1)
		        .withValidator(ValidationUtils
		                .checkUsingException(v -> isIgnoreErrors() || getEditedAthlete().validateSnatch2Change1(v),
		                        (s) -> doSetErrorLabel(s, this.snatch2Change1)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.snatch2Change1, this.snatch2ActualLift);
		        })
		        .bind(Athlete::getSnatch2Change1, Athlete::setSnatch2Change1);
		atRowAndColumn(this.gridLayout, this.snatch2Change1, CHANGE1, SNATCH2);

		this.snatch2Change2 = createPositiveWeightField(CHANGE2, SNATCH2);
		this.binder.forField(this.snatch2Change2)
		        .withValidator(ValidationUtils
		                .checkUsingException(v -> isIgnoreErrors() || getEditedAthlete().validateSnatch2Change2(v),
		                        (s) -> doSetErrorLabel(s, this.snatch2Change2)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.snatch2Change2, this.snatch2ActualLift);
		        })
		        .bind(Athlete::getSnatch2Change2, Athlete::setSnatch2Change2);
		atRowAndColumn(this.gridLayout, this.snatch2Change2, CHANGE2, SNATCH2);

		this.snatch2ActualLift = createActualWeightField(ACTUAL, SNATCH2);
		this.binder.forField(this.snatch2ActualLift)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateSnatch2ActualLift(v),
		                        (s) -> doSetErrorLabel(s, this.snatch2ActualLift)))
		        .withValidator(ValidationUtils.checkUsingException(v -> setAutomaticProgressions(getEditedAthlete())))
		        .withValidationStatusHandler(status -> setActualLiftClass(status))
		        .bind(Athlete::getSnatch2ActualLift, Athlete::setSnatch2ActualLift);
		atRowAndColumn(this.gridLayout, this.snatch2ActualLift, ACTUAL, SNATCH2);

		this.snatch3AutomaticProgression = new TextField();
		this.snatch3AutomaticProgression.setReadOnly(true);
		this.snatch3AutomaticProgression.setTabIndex(-1);
		this.binder.forField(this.snatch3AutomaticProgression).bind(Athlete::getSnatch3AutomaticProgression,
		        Athlete::setSnatch3AutomaticProgression);
		atRowAndColumn(this.gridLayout, this.snatch3AutomaticProgression, AUTOMATIC, SNATCH3);

		this.snatch3Declaration = createPositiveWeightField(DECLARATION, SNATCH3);
		this.binder.forField(this.snatch3Declaration)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateSnatch3Declaration(v),
		                        (s) -> doSetErrorLabel(s, this.snatch3Declaration)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.snatch3Declaration, this.snatch3ActualLift);
		        }).bind(Athlete::getSnatch3Declaration, Athlete::setSnatch3Declaration);
		atRowAndColumn(this.gridLayout, this.snatch3Declaration, DECLARATION, SNATCH3);

		this.snatch3Change1 = createPositiveWeightField(CHANGE1, SNATCH3);
		this.binder.forField(this.snatch3Change1)
		        .withValidator(ValidationUtils
		                .checkUsingException(v -> isIgnoreErrors() || getEditedAthlete().validateSnatch3Change1(v),
		                        (s) -> doSetErrorLabel(s, this.snatch3Change1)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.snatch3Change1, this.snatch3ActualLift);
		        }).bind(Athlete::getSnatch3Change1, Athlete::setSnatch3Change1);
		atRowAndColumn(this.gridLayout, this.snatch3Change1, CHANGE1, SNATCH3);

		this.snatch3Change2 = createPositiveWeightField(CHANGE2, SNATCH3);
		this.binder.forField(this.snatch3Change2)
		        .withValidator(ValidationUtils
		                .checkUsingException(v -> isIgnoreErrors() || getEditedAthlete().validateSnatch3Change2(v),
		                        (s) -> doSetErrorLabel(s, this.snatch3Change2)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.snatch3Change2, this.snatch3ActualLift);
		        })
		        .bind(Athlete::getSnatch3Change2, Athlete::setSnatch3Change2);
		atRowAndColumn(this.gridLayout, this.snatch3Change2, CHANGE2, SNATCH3);

		this.snatch3ActualLift = createActualWeightField(ACTUAL, SNATCH3);
		this.binder.forField(this.snatch3ActualLift)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateSnatch3ActualLift(v),
		                        (s) -> doSetErrorLabel(s, this.snatch3ActualLift)))
		        .withValidationStatusHandler(status -> setActualLiftClass(status))
		        .bind(Athlete::getSnatch3ActualLift, Athlete::setSnatch3ActualLift);
		atRowAndColumn(this.gridLayout, this.snatch3ActualLift, ACTUAL, SNATCH3);

		this.cj1Declaration = createPositiveWeightField(DECLARATION, CJ1);
		this.binder.forField(this.cj1Declaration)
		        .withValidator(
		                ValidationUtils.checkUsingException2(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk1Declaration(v),
		                        (s) -> doSetErrorLabel(s, this.cj1Declaration)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.cj1Declaration, this.cj1ActualLift);
		        })
		        .bind(Athlete::getCleanJerk1Declaration, Athlete::setCleanJerk1Declaration);
		atRowAndColumn(this.gridLayout, this.cj1Declaration, DECLARATION, CJ1);

		this.cj1Change1 = createPositiveWeightField(CHANGE1, CJ1);
		this.binder.forField(this.cj1Change1)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk1Change1(v),
		                        (s) -> doSetErrorLabel(s, this.cj1Change1)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.cj1Change1, this.cj1ActualLift);
		        }).bind(Athlete::getCleanJerk1Change1, Athlete::setCleanJerk1Change1);
		atRowAndColumn(this.gridLayout, this.cj1Change1, CHANGE1, CJ1);

		this.cj1Change2 = createPositiveWeightField(CHANGE2, CJ1);
		this.binder.forField(this.cj1Change2)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk1Change2(v),
		                        (s) -> doSetErrorLabel(s, this.cj1Change2)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.cj1Change2, this.cj1ActualLift);
		        }).bind(Athlete::getCleanJerk1Change2, Athlete::setCleanJerk1Change2);
		atRowAndColumn(this.gridLayout, this.cj1Change2, CHANGE2, CJ1);

		this.cj1ActualLift = createActualWeightField(ACTUAL, CJ1);
		this.binder.forField(this.cj1ActualLift)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk1ActualLift(v),
		                        (s) -> doSetErrorLabel(s, this.cj1ActualLift)))
		        .withValidator(ValidationUtils.checkUsingException(v -> setAutomaticProgressions(getEditedAthlete())))
		        .withValidationStatusHandler(status -> setActualLiftClass(status))
		        .bind(Athlete::getCleanJerk1ActualLift, Athlete::setCleanJerk1ActualLift);
		atRowAndColumn(this.gridLayout, this.cj1ActualLift, ACTUAL, CJ1);

		this.cj2AutomaticProgression = new TextField();
		this.cj2AutomaticProgression.setReadOnly(true);
		this.cj2AutomaticProgression.setTabIndex(-1);
		this.binder.forField(this.cj2AutomaticProgression).bind(Athlete::getCleanJerk2AutomaticProgression,
		        Athlete::setCleanJerk2AutomaticProgression);
		atRowAndColumn(this.gridLayout, this.cj2AutomaticProgression, AUTOMATIC, CJ2);

		this.cj2Declaration = createPositiveWeightField(DECLARATION, CJ2);
		this.binder.forField(this.cj2Declaration)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk2Declaration(v),
		                        (s) -> doSetErrorLabel(s, this.cj2Declaration)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.cj2Declaration, this.cj2ActualLift);
		        }).bind(Athlete::getCleanJerk2Declaration, Athlete::setCleanJerk2Declaration);
		atRowAndColumn(this.gridLayout, this.cj2Declaration, DECLARATION, CJ2);

		this.cj2Change1 = createPositiveWeightField(CHANGE1, CJ2);
		this.binder.forField(this.cj2Change1)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk2Change1(v),
		                        (s) -> doSetErrorLabel(s, this.cj2Change1)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.cj2Change1, this.cj2ActualLift);
		        }).bind(Athlete::getCleanJerk2Change1, Athlete::setCleanJerk2Change1);
		atRowAndColumn(this.gridLayout, this.cj2Change1, CHANGE1, CJ2);

		this.cj2Change2 = createPositiveWeightField(CHANGE2, CJ2);
		this.binder.forField(this.cj2Change2)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk2Change2(v),
		                        (s) -> doSetErrorLabel(s, this.cj2Change2)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.cj2Change2, this.cj2ActualLift);
		        }).bind(Athlete::getCleanJerk2Change2, Athlete::setCleanJerk2Change2);
		atRowAndColumn(this.gridLayout, this.cj2Change2, CHANGE2, CJ2);

		this.cj2ActualLift = createActualWeightField(ACTUAL, CJ2);
		this.binder.forField(this.cj2ActualLift)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk2ActualLift(v),
		                        (s) -> doSetErrorLabel(s, this.cj2ActualLift)))
		        .withValidator(ValidationUtils.checkUsingException(v -> setAutomaticProgressions(getEditedAthlete())))
		        .withValidationStatusHandler(status -> setActualLiftClass(status))
		        .bind(Athlete::getCleanJerk2ActualLift, Athlete::setCleanJerk2ActualLift);
		atRowAndColumn(this.gridLayout, this.cj2ActualLift, ACTUAL, CJ2);

		this.cj3AutomaticProgression = new TextField();
		this.cj3AutomaticProgression.setReadOnly(true);
		this.cj3AutomaticProgression.setTabIndex(-1);
		this.binder.forField(this.cj3AutomaticProgression).bind(Athlete::getCleanJerk3AutomaticProgression,
		        Athlete::setCleanJerk3AutomaticProgression);
		atRowAndColumn(this.gridLayout, this.cj3AutomaticProgression, AUTOMATIC, CJ3);

		this.cj3Declaration = createPositiveWeightField(DECLARATION, CJ3);
		this.binder.forField(this.cj3Declaration)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk3Declaration(v),
		                        (s) -> doSetErrorLabel(s, this.cj3Declaration)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.cj3Declaration, this.cj3ActualLift);
		        })
		        .bind(Athlete::getCleanJerk3Declaration, Athlete::setCleanJerk3Declaration);
		atRowAndColumn(this.gridLayout, this.cj3Declaration, DECLARATION, CJ3);

		this.cj3Change1 = createPositiveWeightField(CHANGE1, CJ3);
		this.binder.forField(this.cj3Change1)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk3Change1(v),
		                        (s) -> doSetErrorLabel(s, this.cj3Change1)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.cj3Change1, this.cj3ActualLift);
		        })
		        .bind(Athlete::getCleanJerk3Change1, Athlete::setCleanJerk3Change1);
		atRowAndColumn(this.gridLayout, this.cj3Change1, CHANGE1, CJ3);

		this.cj3Change2 = createPositiveWeightField(CHANGE2, CJ3);
		this.binder.forField(this.cj3Change2)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk3Change2(v),
		                        (s) -> doSetErrorLabel(s, this.cj3Change2)))
		        .withValidationStatusHandler(status -> {
			        checkErrorsAndWithdrawl(status, this.cj3Change2, this.cj3ActualLift);
		        })
		        .bind(Athlete::getCleanJerk3Change2, Athlete::setCleanJerk3Change2);
		atRowAndColumn(this.gridLayout, this.cj3Change2, CHANGE2, CJ3);

		this.cj3ActualLift = createActualWeightField(ACTUAL, CJ3);
		this.binder.forField(this.cj3ActualLift)
		        .withValidator(
		                ValidationUtils.checkUsingException(
		                        v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk3ActualLift(v),
		                        (s) -> doSetErrorLabel(s, this.cj3ActualLift)))
		        .withValidationStatusHandler(status -> setActualLiftClass(status))
		        .bind(Athlete::getCleanJerk3ActualLift, Athlete::setCleanJerk3ActualLift);
		atRowAndColumn(this.gridLayout, this.cj3ActualLift, ACTUAL, CJ3);

		if (Competition.getCurrent().isCustomScore()) {
			TextField custom = createPositiveWeightField(SCORE, CJ3);
			this.binder.forField(custom)
			        .withConverter(new StringToDoubleConverter(0.0D, Translator.translate("NumberExpected")))
			        .bind(Athlete::getCustomScoreComputed, Athlete::setCustomScore);
			atRowAndColumn(this.gridLayout, custom, SCORE, CJ3);
		}

		this.binder.withValidator((a, v) -> {
			ValidationResult vr = ValidationUtils
			        .checkUsingException(u -> getEditedAthlete().validateStartingTotalsRule()).apply(a, v);
			logger.debug("binder-level validation! error={} {}", vr.isError(),
			        vr.isError() ? vr.getErrorMessage() : "");
			if (vr.isError()) {
				doSetErrorLabel("binder-level", new StringBuilder(vr.getErrorMessage()));
			} else {
				doSetErrorLabel("binder-level", (StringBuilder) null);
			}
			return vr;
		});

		// use setBean so that changes are immediately reflected to the working copy
		// otherwise the changes are only visible in the fields, and the validation
		// routines in the
		// Athlete class don't work
		this.binder.setBean(getEditedAthlete());

		for (int i = SNATCH1; i <= CJ3; i++) {
			this.textfields[ACTUAL - 1][i - 1].addValueChangeListener(e -> setLiftResultChanged(true));
		}

		if (!isUpdatingResults()) {
			adjustResultsFields(true);
		}
		setFocus(getEditedAthlete());
		this.binder.setValidatorsDisabled(false);
		return this.binder;
	}

	private Checkbox buildAllowResultsEditingCheckbox() {
		Checkbox checkbox = new Checkbox(Translator.translate("AllowResultsEditing"));
		checkbox.getStyle().set("margin-left", CHECKBOX_MARGIN);
		this.binder.forField(checkbox).bind((a) -> isUpdatingResults(), (a, readonly) -> {
			setUpdatingResults(readonly);
			adjustResultsFields(readonly);
		});
		return checkbox;
	}

	private Checkbox buildForcedCurrentCheckbox() {
		Checkbox checkbox = new Checkbox(Translator.translate("ForcedAsCurrent"));
		checkbox.getStyle().set("margin-left", CHECKBOX_MARGIN);
		this.binder.forField(checkbox).bind(Athlete::isForcedAsCurrent, Athlete::setForcedAsCurrent);
		return checkbox;
	}

	private Checkbox buildIgnoreErrorsCheckbox() {
		this.ignoreErrorsCheckbox = new Checkbox(Translator.translate("RuleViolation.ignoreErrors"), e -> {
			if (BooleanUtils.isTrue(isIgnoreErrors())) {
				logger./**/warn/**/("{}!Errors ignored - checkbox override for athlete {}",
				        FieldOfPlay.getLoggingName(OwlcmsSession.getFop()), this.getEditedAthlete().getShortName());
				// binder.validate();
				boolean validationReset = this.editedAthlete.isValidation();
				try {
					this.editedAthlete.setValidation(false);
					this.binder.writeBeanAsDraft(this.editedAthlete, true);
				} finally {
					this.editedAthlete.setValidation(validationReset);
				}

			}

		});
		this.ignoreErrorsCheckbox.getStyle().set("margin-left", CHECKBOX_MARGIN);
		return this.ignoreErrorsCheckbox;
	}

	private Component buildWithdrawButtons() {
		Integer attemptsDone = getEditedAthlete().getAttemptsDone();
		VerticalLayout vl = new VerticalLayout();

		Button snatchWithdrawalButton = new Button(Translator.translate("SnatchWithdrawal"),
		        new Icon(VaadinIcon.SIGN_OUT),
		        (e) -> {
			        Athlete.conditionalCopy(this.originalAthlete, getEditedAthlete(), true);
			        this.originalAthlete.withdrawFromSnatch();
			        AthleteRepository.save(this.originalAthlete);
			        OwlcmsSession.withFop((fop) -> {
				        fop.pushOutUIEvent(new UIEvent.Notification(
				                this.originalAthlete, this, Notification.Level.WARNING,
				                "SnatchWithdrawalNotification", 5000, this.originalAthlete.getFullName()));
				        fop.fopEventPost(new FOPEvent.WeightChange(this.getOrigin(), this.originalAthlete, true));
			        });
			        this.origin.closeDialog();
		        });
		snatchWithdrawalButton.getElement().setAttribute("theme", "error");

		Button withdrawalButton = new Button(Translator.translate("Withdrawal"),
		        new Icon(VaadinIcon.SIGN_OUT),
		        (e) -> {
			        Athlete.conditionalCopy(this.originalAthlete, getEditedAthlete(), true);
			        this.originalAthlete.withdraw();
			        AthleteRepository.save(this.originalAthlete);
			        OwlcmsSession.withFop((fop) -> {
				        fop.pushOutUIEvent(new UIEvent.Notification(
				                this.originalAthlete, this, Notification.Level.WARNING,
				                "FullWithdrawalNotification", 5000, this.originalAthlete.getFullName()));
				        fop.fopEventPost(new FOPEvent.WeightChange(this.getOrigin(), this.originalAthlete, true));
			        });
			        this.origin.closeDialog();
		        });
		withdrawalButton.getElement().setAttribute("theme", "error");

		if (attemptsDone < 3) {
			vl.add(snatchWithdrawalButton, withdrawalButton);
		} else {
			vl.add(withdrawalButton);
		}

		vl.setSizeUndefined();
		return vl;
	}

	private void checkErrorsAndWithdrawl(BindingValidationStatus<?> status, TextField field,
	        TextField lift) {
		getFieldErrors(field, status);
		if (!status.isError()) {
			if (field.getValue() != null) {
				// logger.debug("field {} = {}", field.getId().get(), field.getValue());
				if (field.getValue().contentEquals("0")) {
					lift.setValue("0");
					setFocus(getEditedAthlete());
				}
			}
		}
	}

	private TextField createActualWeightField(int row, int col) {
		TextField tf = new TextField();
		tf.setPattern("^[-]{0,1}\\d*$");
		tf.setAllowedCharPattern("[0-9-]");
		tf.setValueChangeMode(ValueChangeMode.ON_CHANGE);

		tf.addValueChangeListener(e -> {
			if (e.isFromClient()) {
				setFocus(getEditedAthlete());
			}
		});
		return tf;
	}

	private TextField createPositiveWeightField(int row, int col) {
		TextField tf = new TextField();
		tf.setPattern("^(350|3[0-4][0-9]|2[0-9]{2}|1[0-9]{2}|[1-9][0-9]?|0)$");
		tf.setMaxLength(3);
		tf.setAllowedCharPattern("[0-9]");
		tf.setValueChangeMode(ValueChangeMode.ON_CHANGE);
		tf.addValueChangeListener(e -> {
			if (e.isFromClient()) {
				setFocus(getEditedAthlete());
				tf.focus();
			}
		});
		return tf;
	}

	private void doSetErrorLabel(String simpleName, StringBuilder sb) {
		if (sb != null && sb.length() > 0) {
			String message = sb.toString();
			logger.debug("{} setting message {}", simpleName, message);
			this.errorLabel.setVisible(true);
			this.errorLabel.getElement().setProperty("innerHTML", "\u26A0 " + message);
			this.errorLabel.getClassNames().set("errorMessage", true);
		} else {
			logger.debug("{} setting EMPTY", simpleName);
			this.errorLabel.setVisible(true);
			this.errorLabel.getElement().setProperty("innerHTML", "&nbsp;");
			this.errorLabel.getClassNames().clear();
		}
	}

	private void doSetErrorLabel(String message, TextField field) {
		if (message != null && !message.isBlank()) {
			logger.debug("{} setting message {}", message);
			this.errorLabel.setVisible(true);
			this.errorLabel.getElement().setProperty("innerHTML", message);
			this.errorLabel.getClassNames().set("errorMessage", true);
			field.setInvalid(true);
		} else {
			logger.debug("{} setting EMPTY");
			this.errorLabel.setVisible(true);
			this.errorLabel.getElement().setProperty("innerHTML", "&nbsp;");
			this.errorLabel.getClassNames().clear();
			field.setInvalid(false);
		}
		resetReadOnlyFields();
	}

	/**
	 * Update the original athlete so that the lifting order picks up the change.
	 */
	private void doUpdate() {
		BinderValidationStatus<Athlete> val = this.binder.validate();
		if (!val.isOk()) {
			return;
		}
		Athlete.conditionalCopy(this.originalAthlete, getEditedAthlete(), true);
		AthleteRepository.save(this.originalAthlete);
		OwlcmsSession.withFop((fop) -> {
			fop.fopEventPost(new FOPEvent.WeightChange(this.getOrigin(), this.originalAthlete, isLiftResultChanged()));
		});
		this.origin.closeDialog();
	}

	private Athlete getEditedAthlete() {
		return this.editedAthlete;
	}

	private TextField getErrors(BinderValidationStatus<?> validationStatus, StringBuilder sb) {
		TextField field = null;
		for (BindingValidationStatus<?> ve : validationStatus.getFieldValidationErrors()) {
			field = (TextField) ve.getField();
			ClassList fieldClasses = field.getElement().getClassList();
			fieldClasses.clear();
			fieldClasses.set("error", true);
			field.setReadOnly(false);
			field.setAutoselect(true);
			field.focus();
			if (sb.length() > 0) {
				sb.append("; ");
			}
			String message = ve.getMessage().orElse(Translator.translate("Error"));
			logger.debug("field message: {}", message);
			sb.append(message);
		}
		for (ValidationResult ve : validationStatus.getBeanValidationErrors()) {
			if (sb.length() > 0) {
				sb.append("; ");
			}
			String message = ve.getErrorMessage();
			logger.debug("bean message: {}", message);
			sb.append(message);
		}
		return field;
	}

	private void getFieldErrors(TextField change, BindingValidationStatus<?> status) {
		List<ValidationResult> r = status.getValidationResults();
		for (ValidationResult vr : r) {
			if (vr.isError()) {
				logger.debug("field {} : error {}", change.getId().get(), vr.getErrorMessage());
			} else {
				logger.trace("field {}: no error", change.getId().get());
			}
		}
	}

	private StringBuilder getInitialErrors(BinderValidationStatus<?> validationStatus) {
		StringBuilder sb = new StringBuilder();
		for (BindingValidationStatus<?> ve : validationStatus.getFieldValidationErrors()) {

			if (sb.length() > 0) {
				sb.append("; ");
			}
			String message = ve.getMessage().orElse(("Error"));
			sb.append(message);
		}
		for (ValidationResult ve : validationStatus.getBeanValidationErrors()) {
			if (sb.length() > 0) {
				sb.append("; ");
			}
			String message = ve.getErrorMessage();
			// logger.debug("bean message: {}",message);
			sb.append(message);
		}
		logger.debug("getInitialErrors from {} '{}'", LoggerUtils.whereFrom(), sb);
		return sb;
	}

	private Object getOrigin() {
		return this.origin;
	}

	private boolean isIgnoreErrors() {
		return BooleanUtils.isTrue(this.ignoreErrorsCheckbox == null ? null : this.ignoreErrorsCheckbox.getValue());
	}

	private Boolean isLiftResultChanged() {
		return this.liftResultChanged;
	}

	private boolean isUpdatingResults() {
		return this.updatingResults;
	}

	private void resetReadOnlyFields() {
		// setErrorLabel undoes the readOnly status of fields
		this.snatch2AutomaticProgression.setReadOnly(true);
		this.snatch3AutomaticProgression.setReadOnly(true);
		this.cj2AutomaticProgression.setReadOnly(true);
		this.cj3AutomaticProgression.setReadOnly(true);

		this.snatch1Declaration.setReadOnly(false);
		this.snatch1Change1.setReadOnly(false);
		this.snatch1Change2.setReadOnly(false);

		this.snatch2Declaration.setReadOnly(false);
		this.snatch2Change1.setReadOnly(false);
		this.snatch2Change2.setReadOnly(false);

		this.snatch3Declaration.setReadOnly(false);
		this.snatch3Change1.setReadOnly(false);
		this.snatch3Change2.setReadOnly(false);

		this.cj1Declaration.setReadOnly(false);
		this.cj1Change1.setReadOnly(false);
		this.cj1Change2.setReadOnly(false);

		this.cj2Declaration.setReadOnly(false);
		this.cj2Change1.setReadOnly(false);
		this.cj2Change2.setReadOnly(false);

		this.cj3Declaration.setReadOnly(false);
		this.cj3Change1.setReadOnly(false);
		this.cj3Change2.setReadOnly(false);

		this.snatch1ActualLift.setReadOnly(!isUpdatingResults());
		this.snatch2ActualLift.setReadOnly(!isUpdatingResults());
		this.snatch3ActualLift.setReadOnly(!isUpdatingResults());
		this.cj1ActualLift.setReadOnly(!isUpdatingResults());
		this.cj2ActualLift.setReadOnly(!isUpdatingResults());
		this.cj3ActualLift.setReadOnly(!isUpdatingResults());
	}

	private void setActualLiftClass(BindingValidationStatus<?> status) throws NumberFormatException {
		TextField field = (TextField) status.getField();
		if (status.isError()) {
			field.getElement().getClassList().set("error", true);
			field.getElement().getClassList().set("good", false);
			field.getElement().getClassList().set("bad", false);
			field.focus();
			field.setAutofocus(true);
		} else {
			setLiftResultClass(field);
		}
	}

	/**
	 * set the automatic progressions. This is invoked as a validator because we don't want to be called if the entered
	 * value is invalid. Only the side-effects are interesting, so we return true.
	 *
	 * @param athlete
	 * @return true always
	 */
	private boolean setAutomaticProgressions(Athlete athlete) {
		String autoVal = athlete.getSnatch2AutomaticProgression(); // computeAutomaticProgression(value);
		this.snatch2AutomaticProgression.setValue(autoVal);
		autoVal = athlete.getSnatch3AutomaticProgression();
		this.snatch3AutomaticProgression.setValue(autoVal);

		autoVal = athlete.getCleanJerk2AutomaticProgression();
		this.cj2AutomaticProgression.setValue(autoVal);
		autoVal = athlete.getCleanJerk3AutomaticProgression();
		this.cj3AutomaticProgression.setValue(autoVal);

		return true;
	}

	private void setEditedAthlete(Athlete editedAthlete) {
		this.editedAthlete = editedAthlete;
	}

	private void setFocus(Athlete a) {
		int targetRow = ACTUAL + 1;
		int targetCol = CJ3 + 1;

		// reset current marker -- can be anywhere
		for (int col = CJ3; col >= SNATCH1; col--) {
			for (int row = ACTUAL; row > AUTOMATIC; row--) {
				this.textfields[row - 1][col - 1].removeClassName("current");
			}
		}

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

		// set current marker to first empty cell after last lift.
		for (int col = rightCol; col >= leftCol; col--) {
			for (int row = ACTUAL; row > AUTOMATIC; row--) {
				boolean empty = this.textfields[row - 1][col - 1].getValue().isBlank();
				if (empty) {
					targetRow = row - 1;
					targetCol = col - 1;
				} else {
					break;
				}
			}
		}

		if (targetCol <= CJ3 && targetRow <= ACTUAL) {
			// a suitable empty cell was found, set focus
			this.textfields[targetRow][targetCol].setAutofocus(true);
			this.textfields[targetRow][targetCol].setAutoselect(true);
			this.textfields[targetRow][targetCol].focus();
			this.textfields[targetRow][targetCol].addClassName("current");
		}
	}

	private void setLiftResultChanged(Boolean liftResultChanged) {
		// logger.debug("*** liftResultChanged {}", liftResultChanged);
		this.liftResultChanged = liftResultChanged;
	}

	private void setLiftResultClass(TextField field) {
		String value = field.getValue();
		boolean empty = value == null || value.trim().isEmpty();
		if (empty) {
			field.getElement().getClassList().clear();
			if (!isUpdatingResults()) {
				field.getElement().getClassList().add("readonly");
				field.setReadOnly(true);
			} else {
				field.setReadOnly(false);
			}
		} else if (value != null && value.equals("-")) {
			field.getElement().getClassList().clear();
			field.getElement().getClassList().add("bad");
			if (!isUpdatingResults()) {
				field.getElement().getClassList().add("readonly");
				field.setReadOnly(true);
			} else {
				field.setReadOnly(false);
			}
		} else {
			int intValue = Integer.parseInt(value);
			field.getElement().getClassList().clear();
			field.getElement().getClassList().add((intValue <= 0 ? "bad" : "good"));
			if (!isUpdatingResults()) {
				field.getElement().getClassList().add("readonly");
				field.setReadOnly(true);
			} else {
				field.setReadOnly(false);
			}
		}
	}

	private void setUpdatingResults(Boolean b) {
		this.updatingResults = b;
	}

	private GridLayout setupGrid() {
		GridLayout gridLayout = new GridLayout();
		gridLayout.setTemplateRows(new Repeat(ACTUAL, new Flex(1)));
		gridLayout.setTemplateColumns(new Repeat(CJ3, new Flex(1)));
		gridLayout.setGap(new Length("0.8ex"), new Length("1.2ex"));

		// column headers
		atRowAndColumn(gridLayout, new NativeLabel(Translator.translate("Snatch1")), HEADER, SNATCH1, RowAlign.CENTER,
		        ColumnAlign.CENTER);
		atRowAndColumn(gridLayout, new NativeLabel(Translator.translate("Snatch2")), HEADER, SNATCH2, RowAlign.CENTER,
		        ColumnAlign.CENTER);
		atRowAndColumn(gridLayout, new NativeLabel(Translator.translate("Snatch3")), HEADER, SNATCH3, RowAlign.CENTER,
		        ColumnAlign.CENTER);
		atRowAndColumn(gridLayout, new NativeLabel(Translator.translate("C_and_J_1")), HEADER, CJ1, RowAlign.CENTER,
		        ColumnAlign.CENTER);
		atRowAndColumn(gridLayout, new NativeLabel(Translator.translate("C_and_J_2")), HEADER, CJ2, RowAlign.CENTER,
		        ColumnAlign.CENTER);
		atRowAndColumn(gridLayout, new NativeLabel(Translator.translate("C_and_J_3")), HEADER, CJ3, RowAlign.CENTER,
		        ColumnAlign.CENTER);

		// row headings
		atRowAndColumn(gridLayout, new NativeLabel(Translator.translate("AutomaticProgression")), AUTOMATIC, LEFT,
		        RowAlign.CENTER, ColumnAlign.END);
		atRowAndColumn(gridLayout, new NativeLabel(Translator.translate("Declaration")), DECLARATION, LEFT,
		        RowAlign.CENTER, ColumnAlign.END);
		atRowAndColumn(gridLayout, new NativeLabel(Translator.translate("Change_1")), CHANGE1, LEFT, RowAlign.CENTER,
		        ColumnAlign.END);
		atRowAndColumn(gridLayout, new NativeLabel(Translator.translate("Change_2")), CHANGE2, LEFT, RowAlign.CENTER,
		        ColumnAlign.END);
		atRowAndColumn(gridLayout, new NativeLabel(Translator.translate("WeightLifted")), ACTUAL, LEFT, RowAlign.CENTER,
		        ColumnAlign.END);
		if (Competition.getCurrent().isCustomScore()) {
			atRowAndColumn(gridLayout, new NativeLabel(Translator.translate("Score")), SCORE, LEFT, RowAlign.CENTER,
			        ColumnAlign.END);
		}

		return gridLayout;
	}
}
