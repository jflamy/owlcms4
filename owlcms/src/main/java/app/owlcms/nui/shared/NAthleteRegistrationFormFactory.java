/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.CrudFormConfiguration;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexDirection;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.validator.DoubleRangeValidator;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;

import app.owlcms.access.AccessUi;
import app.owlcms.access.Principal;
import app.owlcms.apputils.NotificationUtils;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.components.fields.LocalDateField;
import app.owlcms.components.fields.LocalizedDecimalField;
import app.owlcms.components.fields.LocalizedIntegerField;
import app.owlcms.components.fields.ValidationUtils;
import app.owlcms.components.fields.WrappedTextField;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteEditAccess;
import app.owlcms.data.athlete.AthleteMetadata;
import app.owlcms.data.athlete.AthleteMetadataEdit;
import app.owlcms.data.athlete.AthleteMetadataEdit.Opening;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.EligibleForIndividualRankingStatus;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athlete.RuleViolationException;
import app.owlcms.data.athlete.RegistrationEditCheck;
import app.owlcms.data.athlete.RegistrationTabPolicy;
import app.owlcms.data.athlete.RegistrationTabPolicy.CardEntry;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.category.RegistrationQtHelper;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.lifting.AnnouncerContent;
import app.owlcms.nui.lifting.AthleteCardFormFactory;
import app.owlcms.nui.lifting.NextAthleteAble;
import app.owlcms.nui.lifting.WeighinContent;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.NaturalOrderComparator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public final class NAthleteRegistrationFormFactory extends OwlcmsCrudFormFactory<Athlete> implements NavigationPage {
	final private Logger logger = (Logger) LoggerFactory.getLogger(NAthleteRegistrationFormFactory.class);
	final private static int NB_COLUMNS = 3;
	HasValue<?, ?> dateField = null;
	private List<Category> allEligible;
	private LocalizedDecimalField bodyWeightField;
	private LocalizedDecimalField scaleWeightField;
	private ComboBox<Category> categoryField;
	private boolean catGenderOk;
	private boolean changeListenersEnabled;
	private boolean checkOther20kgFields;
	private LocalizedIntegerField cleanJerk1DeclarationField;
	private TextField coachField;
	private Group currentGroup;
	private TextField custom1Field;
	private TextField custom2Field;
	private Athlete editedAthlete = null;
	private RegistrationEditCheck registrationEditCheck;
	private FormSaveTask saveTask;
	private ComponentEventListener<ClickEvent<Button>> cancelListener;
	private CheckboxGroup<Category> eligibleField;
	private CheckboxGroup<String> ageGroupTeamField;
	private CheckboxGroup<String> mixedAgeGroupTeamField;
	private Div mixedTeamSpacer;
	private FormItem mixedAgeGroupTeamFormItem;
	private TextField federationCodesField;
	private Map<HasValue<?, ?>, Binding<Athlete, ?>> fieldToBinding = new HashMap<>();
	private TextField firstNameField;
	private LocalDateField fullBirthDateField;
	private boolean genderCatOk;
	private ComboBox<Gender> genderField;
	private ComboBox<Group> groupField;
	private Button operationButton;
	private Button acceptWarningsButton;
	private Button cancelButton;
	private String cancelButtonTheme;
	private Component footerLayout;
	private boolean acceptingWarnings;
	private boolean updateRefused;
	private boolean warningRefreshPending;
	private Category initialCategory;
	private TextField lastNameField;
	private LocalizedIntegerField lotNumberField;
	private TextField membershipField;
	private LocalizedIntegerField qualifyingTotalField;
	private LocalizedIntegerField snatch1DeclarationField;
	private LocalizedIntegerField startNumberField;
	private ComboBox<String> teamField;
	private TextField wrappedBWTextField;
	private LocalizedIntegerField yobField;
	private ComboBox<EligibleForIndividualRankingStatus> individualEligibilityField;
	private TextField subCategoryField;
	private NextAthleteAble previousNext;
	private CardEntry cardEntry = CardEntry.NONE;
	private boolean resultsEntry;
	private IAthleteEditing cardOrigin;
	private Principal editor;
	private boolean registrationWritable;
	private boolean metadataWritable;
	private boolean keepOpen;
	private boolean cardSelected;
	private Opening metadataOpening;

	public NAthleteRegistrationFormFactory(Class<Athlete> domainType, Group group,
	        NextAthleteAble parentGrid) {
		super(domainType);
		this.setCurrentGroup(group);
		this.setPreviousNext(parentGrid);
		logger.setLevel(Level.DEBUG);
	}

	/**
	 * Ask for the independent lifting card after lifting has started (see {@link RegistrationTabPolicy}).
	 * Without this call there is no Card tab, as in weigh-in.
	 *
	 * @param entry      what the opening page wants
	 * @param pageOrigin the page, when its field of play and results editing must be kept for the card; null otherwise.
	 *                   A Card-first form closes after a successful save; an available Card tab remains open.
	 */
	public NAthleteRegistrationFormFactory setCardEntry(CardEntry entry, IAthleteEditing pageOrigin) {
		this.cardEntry = entry != null ? entry : CardEntry.NONE;
		this.cardOrigin = pageOrigin;
		this.resultsEntry = entry == CardEntry.CARD_FIRST;
		return this;
	}

	/**
	 * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
	 */
	@Override
	public Athlete add(Athlete athlete) {
		requireRegistration();
		return AthleteRepository.create(athlete);
	}

	/**
	 * Create a binder with our special validation status handling
	 *
	 * @see org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory#buildBinder(org.vaadin.crudui.crud.CrudOperation, java.lang.Object)
	 */
	@Override
	public Binder<Athlete> buildBinder(CrudOperation operation, Athlete ignored) {
		this.binder = super.buildBinder(operation, getEditedAthlete());
		this.initialCategory = getEditedAthlete().getCategory();
		setValidationStatusHandler(true);
		return this.binder;
	}

	/**
	 * Change the caption to show the current athlete name and group
	 *
	 * @see org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory#buildCaption(org.vaadin.crudui. crudGrid.CrudOperation, java.lang.Object)
	 */
	@Override
	public String buildCaption(CrudOperation operation, Athlete a) {
		if (a.getLastName() == null && a.getFirstName() == null) {
			return null;
		}
		// If null, CrudLayout.showForm will build its own, for backward compatibility
		return a.getFullId();
	}

	/**
	 * @see app.owlcms.nui.crudui.OwlcmsCrudFormFactory#buildFooter(org.vaadin.crudui.crud.CrudOperation, java.lang.Object,
	 *      com.vaadin.flow.component.ComponentEventListener, com.vaadin.flow.component.ComponentEventListener,
	 *      com.vaadin.flow.component.ComponentEventListener)
	 */
	@Override
	public Component buildFooter(CrudOperation operation,
	        Athlete athlete,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> postOperationCallBack,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener,
	        boolean shortcutEnter,
	        Button... buttons) {

		Button operationButton = null;
		if (operation == CrudOperation.UPDATE) {
			operationButton = buildOperationButton(CrudOperation.UPDATE, athlete, postOperationCallBack);
		} else if (operation == CrudOperation.ADD) {
			operationButton = buildOperationButton(CrudOperation.ADD, athlete, postOperationCallBack);
		}
		Button deleteButton = buildDeleteButton(CrudOperation.DELETE, athlete, deleteButtonClickListener);
		Button cancelButton = buildCancelButton(cancelButtonClickListener);
		this.operationButton = operationButton;
		this.acceptWarningsButton = operationButton != null && this.registrationWritable
		        ? buildAcceptWarningsButton(operation, athlete, postOperationCallBack)
		        : null;
		this.cancelButton = cancelButton;
		this.cancelButtonTheme = cancelButton != null ? cancelButton.getElement().getAttribute("theme") : null;
		this.updateRefused = false;
		this.warningRefreshPending = false;

		HorizontalLayout footerLayout = new HorizontalLayout();
		this.footerLayout = footerLayout;
		// the initial validation runs before the form is attached
		footerLayout.addAttachListener(e -> scheduleWarningRefresh());
		footerLayout.setWidth("100%");
		footerLayout.setSpacing(true);
		footerLayout.setPadding(false);

		if (deleteButton != null && operation != CrudOperation.ADD) {
			footerLayout.add(deleteButton);
		}

		NativeLabel spacer = new NativeLabel();
		footerLayout.add(spacer);// , operationTrigger);
		footerLayout.setAlignItems(Alignment.CENTER);

		if (buttons != null) {
			for (Button b : buttons) {
				footerLayout.add(b);
			}
		}
		// in warning mode: accept, then cancel; otherwise: cancel, then update
		if (this.acceptWarningsButton != null) {
			footerLayout.add(this.acceptWarningsButton);
		}
		if (cancelButton != null) {
			footerLayout.add(cancelButton);
		}

		if (this.previousNext instanceof WeighinContent) {
			boolean nextMode = ((WeighinContent) this.previousNext).isNextMode();
			if (nextMode && operationButton != null) {
				operationButton.setText(Translator.translate("WeighIn.UpdateAndNext"));
			}
		}
		if (operationButton != null) {
			footerLayout.add(operationButton);
			footerLayout.add(buildEnterButton(operation, athlete, postOperationCallBack));
		}
		footerLayout.setFlexGrow(1.0, spacer);
		return footerLayout;
	}

	/**
	 * @see app.owlcms.nui.crudui.OwlcmsCrudFormFactory#buildNewForm(org.vaadin.crudui.crud.CrudOperation, java.lang.Object, boolean,
	 *      com.vaadin.flow.component.ComponentEventListener, com.vaadin.flow.component.ComponentEventListener,
	 *      com.vaadin.flow.component.ComponentEventListener, com.vaadin.flow.component.button.Button[])
	 */
	@Override
	public Component buildNewForm(CrudOperation operation, Athlete aFromList, boolean readOnly,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> operationButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

		setupAthlete(operation, aFromList);
		this.editor = AccessUi.currentPrincipal();
		this.registrationWritable = !readOnly && AthleteEditAccess.registration(this.editor,
		        this.previousNext instanceof WeighinContent, this.resultsEntry);
		this.metadataWritable = !readOnly && AthleteEditAccess.metadata(this.editor);
		this.metadataOpening = new Opening(getEditedAthlete().getId(), getEditedAthlete().getFullName(),
		        AthleteMetadataEdit.normalize(AthleteMetadata.from(getEditedAthlete())),
		        getEditedAthlete().getMetadataUpdatedAt());
		this.cardSelected = false;
		this.fieldToBinding.clear();
		this.cancelListener = cancelButtonClickListener;
		boolean existingAthlete = operation == CrudOperation.UPDATE;
		RegistrationTabPolicy.Layout layout = existingAthlete
		        ? RegistrationTabPolicy.choose(this.cardEntry, getEditedAthlete())
		        : RegistrationTabPolicy.forNewAthlete();
		if (layout.restrictedRegistration()) {
			return createRestrictedForm(layout, cancelButtonClickListener, operationButtonClickListener, readOnly);
		}
		this.keepOpen = layout.cardTab();
		// the session the athlete is stored in; the form below may later change the athlete's session
		Group storedGroup = getEditedAthlete().getGroup();
		this.binder = buildBinder(operation, getEditedAthlete());

		// when in weigh-in, the validations need to ignore the weight that was last
		// loaded.
		OwlcmsSession.setAttribute("weighIn", getEditedAthlete());

		// createPrintButton();
		setChangeListenersEnabled(false);

		Component footer = this.buildFooter(operation, getEditedAthlete(), cancelButtonClickListener,
		        this.registrationWritable || this.metadataWritable ? operationButtonClickListener : null,
		        this.registrationWritable ? deleteButtonClickListener : null, false);

		dumpCategories(aFromList, null, new ArrayList<Category>(aFromList.getEligibleCategories()));

		Component form = createTabSheets(footer, layout, readOnly);
		if (existingAthlete) {
			new RegistrationSessionWatcher(form, storedGroup, fop -> {
				cancelButtonClickListener.onComponentEvent(null);
				LiftingSessionGuard.notifyLifting(fop, LiftingSessionGuard.REGISTRATION_CLOSED);
			});
		}
		if (this.registrationWritable && this.getCurrentGroup() != null && !this.getCurrentGroup().getName().equals("*")) {
			getEditedAthlete().setGroup(getCurrentGroup());
		}
		this.binder.readBean(getEditedAthlete());

		// binder has read bean.
		filterCategories(getEditedAthlete().getCategory(), operation != CrudOperation.ADD);
		applyEditingAccess();

		setFocus(form);
		return form;
	}

	/**
	 * @see org.vaadin.crudui.crud.CrudListener#delete(java.lang.Object)
	 */
	@Override
	public void delete(Athlete athlete) {
		requireRegistration();
		AthleteRepository.delete(athlete);
	}

	@Override
	public Collection<Athlete> findAll() {
		throw new UnsupportedOperationException(); // should be called on the grid, not on the form
	}

	// public void enablePrint(Athlete domainObject) {
	// if (domainObject == null || domainObject.getId() == null) {
	// this.printButton.setEnabled(false);
	// } else {
	// this.printButton.setEnabled(true);
	// this.hiddenButton.getElement().setAttribute("onClick",
	// getWindowOpenerFromClass(AthleteCard.class, domainObject.getId().toString()));
	// }
	// }

	public void setCurrentGroup(Group currentGroup) {
		this.currentGroup = currentGroup;
	}

	@Override
	public void setValidationStatusHandler(boolean showErrorsOnFields) {
		this.binder.setValidationStatusHandler((s) -> {
			List<BindingValidationStatus<?>> fieldValidationErrors = s.getFieldValidationErrors();
			for (BindingValidationStatus<?> error : fieldValidationErrors) {
				HasValue<?, ?> field = error.getField();
				// logger.trace("error message: {} field: {}", error.getMessage(), field);
				if (field instanceof HasValidation) {
					logger.trace("has validation");
					HasValidation vf = (HasValidation) field;
					vf.setInvalid(true);
					vf.setErrorMessage(error.getMessage().get());

				}
			}
			setValid(!s.hasErrors());
			s.notifyBindingValidationStatusHandlers();
			// a new validation supersedes an earlier refusal; a refusal in progress sets it after this
			this.updateRefused = false;
			scheduleWarningRefresh();
		});
	}

	/**
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public Athlete update(Athlete athlete) {
		requireRegistration();
		logger.trace("updating athlete {}", athlete.toString());
		logger.debug("{}update before save {} same as edited={} eligibles={} participations={} category={}",
		        FieldOfPlay.getLoggingName(null), athlete.getShortName(), athlete == this.editedAthlete,
		        codes(athlete.getEligibleCategories()), athlete.getParticipations().size(),
		        athlete.getCategory() != null ? athlete.getCategory().getCode() : null);
		athlete.enforceCategoryIsEligible();
		Athlete saved = AthleteRepository.save(athlete);
		logger.debug("{}update after save {} eligibles={} reloaded eligibles={}", FieldOfPlay.getLoggingName(null),
		        athlete.getShortName(), codes(saved.getEligibleCategories()),
		        codes(AthleteRepository.findById(athlete.getId()).getEligibleCategories()));
		return saved;
	}

	/**
	 * A full form is not downgraded during save: the session may have started since opening, or the athlete may be
	 * moved into a live session. Refuse and require reopening the restricted form instead.
	 */
	@Override
	protected void confirmUpdate(Athlete athlete, Runnable update, Runnable completed) {
		RegistrationEditCheck check = this.registrationEditCheck;
		FormSaveTask task = this.saveTask;
		String athleteName = athlete.getFullName();
		task.execute(() -> {
			requireRegistration();
			return check.trySave(athlete, task::isActive, this::update);
		}, result -> {
			if (result.lifting() != null) {
				LiftingSessionGuard.notifyLifting(result.lifting(), LiftingSessionGuard.CANNOT_SAVE_REGISTRATION);
			} else if (result.stale()) {
				ConcurrentEditDialog.refuseRegistration(athleteName,
				        () -> this.cancelListener.onComponentEvent(null));
			} else if (result.saved()) {
				if (this.keepOpen) {
					setEditedAthlete(result.athlete());
					this.binder.readBean(result.athlete());
					applyEditingAccess();
					NotificationUtils.saved();
					refreshGridBehind();
				} else {
					completed.run();
				}
			}
		}, e -> showError(CrudOperation.UPDATE, e));
	}

	/**
	 * Add the field-level validations
	 *
	 * @see org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory#bindField(com.vaadin.flow.component.HasValue, java.lang.String, java.lang.Class)
	 */
	@Override
	protected void bindField(@SuppressWarnings("rawtypes") HasValue field, String property, Class<?> propertyType,
	        CrudFormConfiguration c) {
		throw new UnsupportedOperationException("should not be calling this method");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void validateBodyWeight(Binder.BindingBuilder<Athlete, Double> bindingBuilder, boolean isRequired) {
		Validator<Double> v1 = new DoubleRangeValidator(Translator.translate("Weight_under_X", "200"), 0.1D, 200.0D);
		bindingBuilder.withValidator(v1);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void validateCategory(Binder.BindingBuilder<Athlete, Category> bindingBuilder) {
		// we have age and bodyweight, is registration category selected
		Validator<Category> v0 = Validator.from((category) -> {
			// logger.debug("v0 {} {}", category, isIgnoreErrors());
			if (isIgnoreErrors()) {
				return true;
			}
			try {
				Double bw = this.bodyWeightField.getValue();
				if ((bw == null || bw < 0.1) && this.dateField != null && this.dateField.getValue() == null) {
					return true;
				}
				if (category == null && (bw != null && bw > 0.0)) {
					// logger.trace("0 category {} {} bw {}", category, cat, bw);
					return false;
				} else {
					return true;
				}
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("Category_noEligiblity_check_age_entry"));
		bindingBuilder.withValidator(v0);

		// check that registration category is consistent with body weight
		Validator<Category> v1 = Validator.from((category) -> {
			// logger.debug("v1");
			try {
				Double bw = this.bodyWeightField.getValue();
				if (category == null && bw == null) {
					// logger.trace("1 category {} {} bw {}", category, cat, bw);
					return true;
				} else if (bw == null) {
					// logger.trace("2 category {} {} bw {}", category != null ?
					// category.getComputedName() : null, cat, bw);
					// no body weight - no contradiction
					return true;
				} else if (bw != null && category == null) {
					// logger.trace("3 category {} {} bw {}", category, cat, bw);
					return isIgnoreErrors();
				}
				Double min = category.getMinimumWeight();
				Double max = category.getMaximumWeight();
				// logger.trace("comparing {} ]{},{}] with body weight {}",
				// category.getComputedName(), min, max, bw);
				return (bw > min && bw <= max);
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("Category_no_match_body_weight"));
		bindingBuilder.withValidator(v1);

		// check that category is consistent with age
		Validator<Category> v2 = Validator.from((category) -> {
			// logger.debug("v2");
			try {
				Category cat = this.categoryField.getValue();
				Integer age = getAgeFromFields();
				if (category == null && age == null) {
					logger.debug("1 category {} {} age {}", category, cat, age);
					return true;
				} else if (age == null) {
					logger.debug("2 category {} {} age {}", category != null ? category.getCode() : null, cat, age);
					// no body weight - no contradiction
					return true;
				}
				if (category != null && age != null && category.getAgeGroup() != null) {
					int min = category.getAgeGroup().getMinAge();
					int max = category.getAgeGroup().getMaxAge();
					logger.trace("comparing {} [{},{}] with age {}", category.getCode(), min, max, age);
					return (age >= min && age <= max);
				} else {
					return true;
				}
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("Category_no_match_age"));
		bindingBuilder.withValidator(v2);

		// check that category is consistent with gender
		Validator<Category> v3 = Validator.from((category) -> {
			// logger.debug("v3");
			try {
				if (category == null) {
					return true;
				}
				Binding<Athlete, ?> genderBinding = this.fieldToBinding.get(this.genderField);
				Gender g = this.genderField.getValue();
				Gender catGender = category != null ? category.getGender() : null;
				logger.trace("categoryValidation: validating gender {} vs category {}: {} {}", g, catGender,
				        catGender == g);
				if (g == null) {
					// no gender - no contradiction
					return true;
				}
				this.catGenderOk = catGender == g;
				if (this.catGenderOk && !this.genderCatOk) {
					// validate() does not validate if no change, ugly workaround
					logger.trace("checking gender");
					genderBinding.validate();
				}
				return catGender == g;
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("Category_no_match_gender"));
		bindingBuilder.withValidator(v3);

		// category must be eligible
		Validator<Category> v4 = Validator.from((category) -> {
			if (category == null) {
				// logger.trace("v4 skipped");
				return true;
			}
			// logger.debug("v4 {} {}", category, category.getName(), category.getCode());
			try {
				List<Category> eligibles = this.allEligible;
				if (eligibles == null || eligibles.isEmpty()) {
					return category == null;
				}
				Set<String> selectedEligibles = this.eligibleField.getValue().stream().map(c -> c.getCode()).collect(Collectors.toSet());
				return !selectedEligibles.isEmpty() && selectedEligibles.contains(category.getCode());
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("CategoryChange_CategoryMustBeEligible", this.initialCategory));
		bindingBuilder.withValidator(v4);

		// a category change requires explicit ok.
		Validator<Category> v5 = Validator.from((category) -> {
			// logger.debug("v5");
			try {
				if (isIgnoreErrors() || this.initialCategory == null
				        || (getEditedAthlete().getBodyWeight() == null && category == null)) {
					return true;
				}
				logger.trace("initialCategory = {}  new category = {}", this.initialCategory,
				        getEditedAthlete().getCategory());
				return category != null && category.sameAs(this.initialCategory);
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("CategoryChange_MustForce", this.initialCategory));
		bindingBuilder.withValidator(v5);

		// category is required
		Validator<Category> v6 = Validator.from((category) -> {
			// logger.debug("v6 ignoreErrors={}", isIgnoreErrors());
			try {
				return isIgnoreErrors() ? true : category != null;
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("CategoryRequired", this.initialCategory));
		bindingBuilder.withValidator(v6);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void validateFullBirthDate(Binder.BindingBuilder bindingBuilder) {
		Validator<LocalDate> fv = this.fullBirthDateField.formatValidation(OwlcmsSession.getLocale());
		bindingBuilder.withValidator(fv);

		Validator<LocalDate> v = Validator.from(ld -> {
			if (ld == null) {
				return true;
			}
			return ld.compareTo(LocalDate.now()) <= 0;
		}, Translator.translate("BirthDate_cannot_future"));
		bindingBuilder.withValidator(v);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void validateGender(Binder.BindingBuilder<Athlete, Gender> bindingBuilder) {
		// check that category is consistent with gender
		Validator<Gender> v2 = Validator.from((g) -> {
			try {
				if (g == null) {
					return true;
				}
				Binding<Athlete, ?> catBinding = this.fieldToBinding.get(this.categoryField);
				Category category = this.categoryField.getValue();
				Gender catGender = category != null ? category.getGender() : null;
				logger.trace("genderValidation: validating gender {} vs category {}: {}", g, catGender, catGender == g);
				this.genderCatOk = catGender == null || catGender == g;
				if (this.genderCatOk && !this.catGenderOk) {
					// turn off message if present.
					logger.trace("checking category");
					catBinding.validate();
				}
				return this.genderCatOk;
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("Category_no_match_gender"));
		bindingBuilder.withValidator(v2);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void validateYearOfBirth(Binder.BindingBuilder bindingBuilder) {
		Validator<Integer> v1 = new IntegerRangeValidator(Translator.translate("InvalidYearFormat"), 1900, 2050);
		bindingBuilder.withValidator(v1);
	}

	private Category bestMatch(List<Category> allEligible2) {
		return allEligible2 != null ? (allEligible2.size() > 0 ? allEligible2.get(0) : null) : null;
	}

	private <T> BindingBuilder<Athlete, T> bindField(BindingBuilder<Athlete, T> bindingBuilder, HasValue<?, T> field,
	        ValueProvider<Athlete, T> getter, Setter<Athlete, T> setter) {
		Binding<Athlete, T> binding;
		binding = bindingBuilder.bind(getter, setter);
		this.fieldToBinding.put(field, binding);
		return bindingBuilder;
	}

	/**
	 * Holds the Enter shortcut. The operation button is hidden while a warning is shown, and a shortcut on a hidden
	 * button is ignored; the value typed before Enter (e.g. the C&J completing the 20 kg rule) only clears the warning
	 * in that same round trip. This button is hidden by its style only, so its shortcut stays active: Enter attempts
	 * the update with the current values, never accepts warnings, and a refused update shows them again.
	 */
	private Button buildEnterButton(CrudOperation operation, Athlete athlete,
	        ComponentEventListener<ClickEvent<Button>> callBack) {
		Button button = new Button();
		button.getStyle().set("display", "none");
		button.addClickListener(e -> performOperationAndCallback(operation, athlete, callBack, false));
		button.addClickShortcut(Key.ENTER);
		return button;
	}

	// same look as the athlete card "Accept Change" button
	private Button buildAcceptWarningsButton(CrudOperation operation, Athlete athlete,
	        ComponentEventListener<ClickEvent<Button>> callBack) {
		Button button = new Button(Translator.translate("AcceptWarningsAndUpdate"), new Icon(VaadinIcon.WARNING));
		button.getElement().setAttribute("theme", "primary contrast");
		button.getStyle().set("background-color", "#FFC107");
		button.getStyle().set("color", "#000000");
		button.setVisible(false);
		button.addClickListener(e -> {
			logger./**/warn/**/("{}!Errors ignored - warnings accepted for athlete {}",
			        FieldOfPlay.getLoggingName(OwlcmsSession.getFop()), this.getEditedAthlete().getShortName());
			// validators pass while accepting warnings
			this.acceptingWarnings = true;
			try {
				performOperationAndCallback(operation, athlete, callBack, false);
			} finally {
				this.acceptingWarnings = false;
			}
		});
		return button;
	}

	@Override
	protected void performOperationAndCallback(CrudOperation operation, Athlete domainObject,
	        ComponentEventListener<ClickEvent<Button>> gridCallback, boolean ignoreErrors) {
		if (this.cardSelected) {
			return;
		}
		if (this.saveTask != null && this.saveTask.isPending()) {
			return;
		}
		if (!this.registrationWritable) {
			saveMetadataOnly(gridCallback);
			return;
		}
		super.performOperationAndCallback(operation, domainObject, gridCallback, ignoreErrors);
		if (operation != CrudOperation.DELETE && !ignoreErrors && !isValid()) {
			// the refusal is authoritative even when no field is shown invalid
			this.updateRefused = true;
			logger.debug("{}update refused acceptingWarnings={}", FieldOfPlay.getLoggingName(null),
			        this.acceptingWarnings);
			scheduleWarningRefresh();
		}
	}

	// recompute once per round trip, after all the validators have updated the fields
	private void scheduleWarningRefresh() {
		if (this.warningRefreshPending || this.footerLayout == null) {
			return;
		}
		this.footerLayout.getUI().ifPresent(ui -> {
			this.warningRefreshPending = true;
			ui.beforeClientResponse(this.footerLayout, ctx -> {
				this.warningRefreshPending = false;
				refreshWarningMode();
			});
		});
	}

	private void refreshWarningMode() {
		applyEditingAccess();
		if (!this.registrationWritable) {
			return;
		}
		// fieldToBinding also holds the fields of earlier openings of this reused factory
		List<String> invalidFields = this.binder.getFields()
		        .filter(f -> f instanceof HasValidation hv && hv.isInvalid())
		        .map(f -> f.getClass().getSimpleName() + "@" + System.identityHashCode(f) + ":"
		                + ((HasValidation) f).getErrorMessage())
		        .toList();
		boolean warnings = !invalidFields.isEmpty() || this.updateRefused;
		logger.debug("{}refreshWarningMode warnings={} updateRefused={} invalidFields={}",
		        FieldOfPlay.getLoggingName(null), warnings, this.updateRefused, invalidFields);
		setWarningMode(warnings);
	}

	private void setWarningMode(boolean warnings) {
		if (this.operationButton == null || this.acceptWarningsButton == null) {
			return;
		}
		// Enter stays active in warning mode (see buildEnterButton)
		this.operationButton.setVisible(!warnings);
		this.acceptWarningsButton.setVisible(warnings);
		if (this.cancelButton != null) {
			if (warnings) {
				this.cancelButton.getElement().setAttribute("theme", "primary");
			} else if (this.cancelButtonTheme == null) {
				this.cancelButton.getElement().removeAttribute("theme");
			} else {
				this.cancelButton.getElement().setAttribute("theme", this.cancelButtonTheme);
			}
		}
	}

	private boolean categoryIsEligible(Category category, List<Category> eligibles) {
		return eligibles.stream().anyMatch(c -> c.sameAs(category));
	}

	@SuppressWarnings("unused")
	private List<Championship> championshipsForCategories(Set<Category> value) {
		return value.stream()
		        .map(c -> c.getAgeGroup() != null ? c.getAgeGroup().getChampionship() : null)
		        .filter(championship -> championship != null)
		        .distinct()
		        .toList();
	}

	@SuppressWarnings("unused")
	private List<AgeGroup> ageGroupsForCategories(Set<Category> value) {
		return value.stream().map(c -> c.getAgeGroup()).distinct().toList();
	}

	private Set<String> possibleAgeGroupTeamsForEligibleField() {
		if (this.eligibleField == null || this.eligibleField.getValue() == null) {
			return new LinkedHashSet<>();
		}
		return ageGroupsForCategories(this.eligibleField.getValue()).stream()
		        .filter(ag -> ag != null)
		        .map(AgeGroup::getDisplayName)
		        .collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private Set<String> possibleMixedAgeGroupTeamsForEligibleField() {
		if (this.eligibleField == null || this.eligibleField.getValue() == null) {
			return new LinkedHashSet<>();
		}
		return ageGroupsForCategories(this.eligibleField.getValue()).stream()
		        .filter(ag -> ag != null && ag.isMixedTeams())
		        .map(AgeGroup::getCode)
		        .collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private void refreshTeamMembershipFields() {
		refreshTeamMembershipField(this.ageGroupTeamField, possibleAgeGroupTeamsForEligibleField(),
		        getEditedAthlete() != null ? getEditedAthlete().getAgeGroupTeams() : new LinkedHashSet<>());
		refreshTeamMembershipField(this.mixedAgeGroupTeamField, possibleMixedAgeGroupTeamsForEligibleField(),
		        getEditedAthlete() != null ? getEditedAthlete().getMixedAgeGroupTeams() : new LinkedHashSet<>());
	}

	private void refreshTeamMembershipField(CheckboxGroup<String> field, Set<String> possibleValues,
	        Set<String> fallbackSelectedValues) {
		if (field == null) {
			return;
		}

		Set<String> selectedValues = new LinkedHashSet<>();
		if (field.getValue() != null && !field.getValue().isEmpty()) {
			selectedValues.addAll(field.getValue());
		} else if (fallbackSelectedValues != null) {
			selectedValues.addAll(fallbackSelectedValues);
		}
		selectedValues.retainAll(possibleValues);

		boolean listenerStatus = isChangeListenersEnabled();
		try {
			setChangeListenersEnabled(false);
			field.setItems(possibleValues);
			field.setVisible(!possibleValues.isEmpty());
			if (field == this.mixedAgeGroupTeamField) {
				boolean visible = !possibleValues.isEmpty();
				if (this.mixedTeamSpacer != null) {
					this.mixedTeamSpacer.setVisible(visible);
				}
				if (this.mixedAgeGroupTeamFormItem != null) {
					this.mixedAgeGroupTeamFormItem.setVisible(visible);
				}
			}
			field.setValue(selectedValues);
		} finally {
			setChangeListenersEnabled(listenerStatus);
		}
	}

	private void checkOther20kgFields(LocalizedIntegerField fieldA,
	        LocalizedIntegerField fieldB) {
		logger.trace("entering checkOther20kgFields {} {}", isCheckOther20kgFields(),
		        LoggerUtils.whereFrom());
		if (isCheckOther20kgFields()) {
			setCheckOther20kgFields(false); // prevent recursion
			this.fieldToBinding.get(fieldA).validate();
			this.fieldToBinding.get(fieldB).validate();
		}
	}

	private void clearErrors(LocalizedIntegerField cleanJerk1DeclarationField2,
	        LocalizedIntegerField qualifyingTotalField2) {
		cleanJerk1DeclarationField2.setInvalid(false);
		// a passing 20kg rule must not hide the entry total QT error
		if (qualifyingTotalField2 != this.qualifyingTotalField
		        || entryTotalMeetsSelectedQt(this.qualifyingTotalField.getValue())) {
			qualifyingTotalField2.setInvalid(false);
		}
		scheduleWarningRefresh();
		logger.debug("{}clearErrors entry total {}", FieldOfPlay.getLoggingName(null), LoggerUtils.whereFrom(1));
	}

	private boolean entryTotalMeetsSelectedQt(Integer entryTotal) {
		try {
			Set<Category> selected = new LinkedHashSet<>();
			if (this.categoryField != null && this.categoryField.getValue() != null) {
				selected.add(this.categoryField.getValue());
			}
			if (this.eligibleField != null && this.eligibleField.getValue() != null) {
				selected.addAll(this.eligibleField.getValue());
			}
			if (selected.isEmpty() && this.allEligible != null) {
				// nothing selected: check the offered categories, as their (ET<QT) markers do
				selected.addAll(this.allEligible);
			}
			return isIgnoreErrors() || RegistrationQtHelper.categoriesFailingQt(selected, entryTotal).isEmpty();
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
		}
		return true;
	}

	private String computeDesc(Group g) {
		String desc = g.getDescription();
		if (desc != null && !desc.isBlank()) {
			return (g.getName() + " - " + desc);
		} else {
			return g.getName();
		}
	}

	private void configure20kgWeightField(LocalizedIntegerField field) {
		TextField textField = field.getWrappedTextField();
		textField.setAutoselect(true);
		textField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
		textField.setPattern("^(-?\\d+)|()$"); // optional minus and at least one digit, or empty.
		textField.setAllowedCharPattern("[0-9-]");
		// on the field itself, registered before the binding: the other two fields must be revalidated by the
		// validation that this change triggers (a listener on the wrapped text field runs after it)
		field.addValueChangeListener((e) -> {
			if (!isChangeListenersEnabled()) {
				return;
			}
			setCheckOther20kgFields(true);
		});
	}

	private FormLayout createCategoryAndSessionForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Athlete.GroupCatTitle");
		layout.add(title);
		layout.setColspan(title, NB_COLUMNS);

		this.categoryField = new ComboBox<>();
		BindingBuilder<Athlete, Category> bb = this.binder.forField(this.categoryField);
		validateCategory(bb);
		bindField(bb, this.categoryField, Athlete::getCategory, Athlete::computeCategory);
		safeCategorySetItems(CategoryRepository.findActive());
		this.categoryField.setRenderer(new TextRenderer<>(Category::getDisplayName));
		FormItem fi = layoutAddFormItem(layout, this.categoryField, Translator.translate("Category"));
		layout.setColspan(fi, 1);

		this.eligibleField = new CheckboxGroup<>();
		bindField(this.binder.forField(this.eligibleField), this.eligibleField, Athlete::getEligibleCategories,

		        Athlete::setEligibleCategories);
		this.eligibleField.setRenderer(new TextRenderer<>(this::eligibleCategoryLabel));
		FormItem fi1 = layoutAddFormItem(layout, this.eligibleField,
		        Translator.translate("Weighin.EligibleCategories"));
		layout.setColspan(fi1, NB_COLUMNS - 1);

		this.subCategoryField = new TextField();
		this.subCategoryField.setWidth("2.2em");
		this.subCategoryField.setAllowedCharPattern("\\p{Lu}");
		bindField(this.binder.forField(this.subCategoryField), this.subCategoryField, Athlete::getSubCategory,
		        Athlete::setSubCategory);
		FormItem fi11 = layoutAddFormItem(layout, this.subCategoryField, Translator.translate("SubCategory"));
		layout.setColspan(fi11, 1);

		this.ageGroupTeamField = new CheckboxGroup<>(null, getEditedAthlete().getPossibleAgeGroupTeams());
		bindField(this.binder.forField(this.ageGroupTeamField), this.ageGroupTeamField, Athlete::getAgeGroupTeams,
		        Athlete::setAgeGroupTeams);
		this.ageGroupTeamField.setValue(getEditedAthlete().getAgeGroupTeams());
		FormItem fi4 = layoutAddFormItem(layout, this.ageGroupTeamField, Translator.translate("TeamMembership.Title"));
		layout.setColspan(fi4, NB_COLUMNS - 1);

		this.mixedAgeGroupTeamField = new CheckboxGroup<>(null, getEditedAthlete().getPossibleMixedAgeGroupTeams());
		bindField(this.binder.forField(this.mixedAgeGroupTeamField), this.mixedAgeGroupTeamField,
		        Athlete::getMixedAgeGroupTeams, Athlete::setMixedAgeGroupTeams);
		this.mixedAgeGroupTeamField.setRenderer(new TextRenderer<>(this::formatMixedAgeGroupLabel));
		this.mixedAgeGroupTeamField.setItemLabelGenerator(this::formatMixedAgeGroupLabel);
		this.mixedAgeGroupTeamField.setValue(getEditedAthlete().getMixedAgeGroupTeams());
		this.mixedTeamSpacer = new Div();
		layout.add(this.mixedTeamSpacer);
		this.mixedAgeGroupTeamFormItem = layoutAddFormItem(layout, this.mixedAgeGroupTeamField,
		        Translator.translate("TeamMembership.MixedTeamMember"));
		layout.setColspan(this.mixedAgeGroupTeamFormItem, NB_COLUMNS - 1);
		boolean hasMixedTeamItems = !getEditedAthlete().getPossibleMixedAgeGroupTeams().isEmpty();
		this.mixedTeamSpacer.setVisible(hasMixedTeamItems);
		this.mixedAgeGroupTeamFormItem.setVisible(hasMixedTeamItems);

		this.groupField = new ComboBox<>();
		bindField(this.binder.forField(this.groupField), this.groupField, Athlete::getGroup, Athlete::setGroup);
		List<Group> allGroups = GroupRepository.findAll();
		allGroups.sort(new NaturalOrderComparator<>());
		this.groupField.setItems(allGroups);
		this.groupField.setWidth("25em");
		this.groupField.setRenderer(new TextRenderer<>(g -> computeDesc(g)));
		this.groupField.setItemLabelGenerator(g -> computeDesc(g));
		this.groupField.setClearButtonVisible(true);
		FormItem fi2 = layoutAddFormItem(layout, this.groupField, Translator.translate("Group"));
		layout.setColspan(fi2, 2);

		this.individualEligibilityField = new ComboBox<>();
		this.individualEligibilityField.setItems(EligibleForIndividualRankingStatus.values());
		this.individualEligibilityField.setItemLabelGenerator(s -> Translator.translate("Eligibility." + s.name()));
		bindField(this.binder.forField(this.individualEligibilityField), this.individualEligibilityField,
		        Athlete::getEffectiveIndividualEligibilityStatus, Athlete::setIndividualEligibilityStatus);
		FormItem fi3 = layoutAddFormItem(layout, this.individualEligibilityField, Translator.translate("Eligibility.Status"));
		layout.setColspan(fi3, NB_COLUMNS - 2);

		return layout;
	}

	private String formatMixedAgeGroupLabel(String value) {
		if (value == null) {
			return "";
		}
		return value.replaceFirst("\\s+(M|F|I)$", "");
	}

	private FormLayout createDrawForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Athlete.DrawTitle");
		layout.add(title);
		layout.setColspan(title, NB_COLUMNS);

		this.lotNumberField = new LocalizedIntegerField();
		bindField(this.binder.forField(this.lotNumberField), this.lotNumberField, Athlete::getLotNumber,
		        Athlete::setLotNumber);
		layoutAddFormItem(layout, this.lotNumberField, Translator.translate("Lot"));

		this.startNumberField = new LocalizedIntegerField();
		bindField(this.binder.forField(this.startNumberField), this.startNumberField, Athlete::getStartNumber,
		        Athlete::setStartNumber);
		layoutAddFormItem(layout, this.startNumberField, Translator.translate("StartNumber"));
		return layout;
	}

	private FormLayout createIdForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Athlete.IdentificationTitle");
		title.getElement().getStyle().set("margin-top", "-1.5ex");
		layout.add(title);
		layout.setColspan(title, NB_COLUMNS);

		this.lastNameField = new TextField();
		bindField(this.binder.forField(this.lastNameField), this.lastNameField, Athlete::getLastName,
		        Athlete::setLastName);
		this.lastNameField.setSizeFull();
		layoutAddFormItem(layout, this.lastNameField, Translator.translate("LastName"));

		this.firstNameField = new TextField();
		this.firstNameField.setSizeFull();
		bindField(this.binder.forField(this.firstNameField), this.firstNameField, Athlete::getFirstName,
		        Athlete::setFirstName);
		layoutAddFormItem(layout, this.firstNameField, Translator.translate("FirstName"));

		Competition competition = Competition.getCurrent();
		if (competition.isUseBirthYear()) {
			this.yobField = new LocalizedIntegerField();
			this.yobField.getWrappedTextField().setPlaceholder("yyyy");
			BindingBuilder<Athlete, Integer> bb = this.binder.forField(this.yobField);
			validateYearOfBirth(bb);
			bindField(bb, this.yobField, Athlete::getYearOfBirth, Athlete::setYearOfBirth);
			layoutAddFormItem(layout, this.yobField, Translator.translate("YearOfBirth"));
		} else {
			this.fullBirthDateField = new LocalDateField();
			this.fullBirthDateField.getWrappedTextField().setPlaceholder("yyyy-mm-dd");
			this.fullBirthDateField.getWrappedTextField().setValueChangeMode(ValueChangeMode.ON_CHANGE);
			BindingBuilder<Athlete, LocalDate> bb = this.binder.forField(this.fullBirthDateField);
			validateFullBirthDate(bb);
			bindField(bb, this.fullBirthDateField, Athlete::getFullBirthDate, Athlete::setFullBirthDate);
			layoutAddFormItem(layout, this.fullBirthDateField, Translator.translate("BirthDate"));
		}

		this.genderField = new ComboBox<>();
		BindingBuilder<Athlete, Gender> bb = this.binder.forField(this.genderField);
		validateGender(bb);
		bindField(bb, this.genderField, Athlete::getGender, Athlete::setGender);
		this.genderField.setItems(Arrays.asList(Gender.mfValues()));
		layoutAddFormItem(layout, this.genderField, Translator.translate("Gender"));

		this.teamField = new ComboBox<>();
		List<String> allTeams = new ArrayList<>(AthleteRepository.findAllTeams());
		allTeams.sort(new NaturalOrderComparator<>());
		this.teamField.setItems(allTeams);
		this.teamField.setAllowCustomValue(true);
		this.teamField.addCustomValueSetListener(e -> this.teamField.setValue(e.getDetail()));
		this.teamField.setClearButtonVisible(true);
		this.teamField.getStyle().set("--vaadin-combo-box-overlay-width", "30em");
		this.teamField.setWidthFull();
		bindField(this.binder.forField(this.teamField), this.teamField, Athlete::getTeam,
		        (athlete, team) -> athlete.setTeam(team != null ? team : ""));
		layoutAddFormItem(layout, this.teamField, Translator.translate("Team"));

		this.membershipField = new TextField();
		bindField(this.binder.forField(this.membershipField), this.membershipField, Athlete::getMembership,
		        Athlete::setMembership);
		layoutAddFormItem(layout, this.membershipField, Translator.translate("Membership"));

		return layout;
	}

	private FormLayout createInfoForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Athlete.InfoTitle");
		layout.add(title);
		layout.setColspan(title, NB_COLUMNS);

		this.coachField = new TextField();
		bindField(this.binder.forField(this.coachField), this.coachField, Athlete::getCoach, Athlete::setCoach);
		FormItem formItem = layoutAddFormItem(layout, this.coachField, Translator.translate("Coach"));
		this.coachField.setSizeFull();
		layout.add(new Div());
		layout.setColspan(formItem, 2);

		this.custom1Field = new TextField();
		bindField(this.binder.forField(this.custom1Field), this.custom1Field, Athlete::getCustom1, Athlete::setCustom1);
		this.custom1Field.setSizeFull();
		layoutAddFormItem(layout, this.custom1Field, Translator.translate("Custom1.Title"));

		this.custom2Field = new TextField();
		bindField(this.binder.forField(this.custom2Field), this.custom2Field, Athlete::getCustom2, Athlete::setCustom2);
		this.custom2Field.setSizeFull();
		layoutAddFormItem(layout, this.custom2Field, Translator.translate("Custom2.Title"));

		return layout;
	}

	private FormLayout createLayout() {
		FormLayout layout = new FormLayout();
		layout.setResponsiveSteps(new ResponsiveStep("0", 1, LabelsPosition.TOP),
		        new ResponsiveStep("800px", NB_COLUMNS, LabelsPosition.ASIDE));
		layout.setRowSpacing("var(--lumo-space-s)");
		return layout;
	}

	private FormLayout createRecordForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Athlete.RecordsTitle");
		layout.add(title);
		layout.setColspan(title, NB_COLUMNS);

		this.federationCodesField = new TextField();
		this.federationCodesField.setSizeFull();
		this.federationCodesField.setHelperText(Translator.translate("Registration.FederationCodes"));
		bindField(this.binder.forField(this.federationCodesField), this.federationCodesField,
		        Athlete::getFederationCodes, Athlete::setFederationCodes);
		FormItem formItem2 = layoutAddFormItem(layout, this.federationCodesField,
		        Translator.translate("Registration.Federations"));
		layout.setColspan(formItem2, 2);
		layout.add(new Div());

		LocalizedIntegerField bestSnatchField = new LocalizedIntegerField();
		bindField(this.binder.forField(bestSnatchField), bestSnatchField, Athlete::getPersonalBestSnatch,
		        Athlete::setPersonalBestSnatch);
		layoutAddFormItem(layout, bestSnatchField, Translator.translate("PersonalBestSnatch"));

		LocalizedIntegerField bestCleanJerkField = new LocalizedIntegerField();
		bindField(this.binder.forField(bestCleanJerkField), bestCleanJerkField, Athlete::getPersonalBestCleanJerk,
		        Athlete::setPersonalBestCleanJerk);
		layoutAddFormItem(layout, bestCleanJerkField, Translator.translate("PersonalBestCleanJerk"));

		LocalizedIntegerField bestTotalField = new LocalizedIntegerField();
		bindField(this.binder.forField(bestTotalField), bestTotalField, Athlete::getPersonalBestTotal,
		        Athlete::setPersonalBestTotal);
		layoutAddFormItem(layout, bestTotalField, Translator.translate("PersonalBestTotal"));

		return layout;
	}

	private Component createRestrictedForm(RegistrationTabPolicy.Layout layout,
	        ComponentEventListener<ClickEvent<Button>> cancelListener,
	        ComponentEventListener<ClickEvent<Button>> doneListener, boolean readOnly) {
		Tab informationTab = new Tab(Translator.translate("Athlete.InformationTab"));
		Tabs tabs = new Tabs(informationTab);
		Div panels = new Div();
		// Both panels contribute their natural size; switching tabs must not resize or stretch the Card.
		panels.getStyle().set("display", "grid").set("grid-template-columns", "max-content")
		        .set("margin-top", "var(--lumo-space-m)").set("overflow", "auto");
		FlexLayout mainLayout = new FlexLayout(tabs, panels);
		this.saveTask = new FormSaveTask(mainLayout);
		Component information = layout.cardTab()
		        ? AthleteMetadataForm.createEmbedded(getEditedAthlete(), () -> cancelListener.onComponentEvent(null),
		                this::refreshGridBehind, readOnly, this.saveTask)
		        : AthleteMetadataForm.create(getEditedAthlete(), () -> cancelListener.onComponentEvent(null),
		                () -> doneListener.onComponentEvent(null), readOnly);
		linkRestrictedPanel(informationTab, information);
		if (layout.cardTab()) {
			// Zero intrinsic width, then stretched: the Card alone sets the column width, so the panel never widens the dialog.
			information.getStyle().set("width", "0").set("min-width", "100%");
		} else {
			information.getStyle().set("width", "auto").set("min-width", "40em");
		}
		information.getStyle().set("align-self", "stretch").set("justify-self", "stretch");
		panels.add(information);
		if (layout.cardTab()) {
			Tab cardTab = new Tab(Translator.translate("Card"));
			Div cardContent = new Div();
			cardContent.setWidth("max-content");
			linkRestrictedPanel(cardTab, cardContent);
			panels.add(cardContent);
			tabs.add(cardTab);
			this.cardSelected = layout.initialTab() == RegistrationTabPolicy.Tab.CARD;
			AtomicReference<AthleteCardFormFactory> card = new AtomicReference<>();
			Runnable selectPanel = () -> {
				setRestrictedPanelSelected(information, !this.cardSelected);
				setRestrictedPanelSelected(cardContent, this.cardSelected);
				if (card.get() != null) {
					card.get().setCardTabSelected(this.cardSelected);
				}
			};
			tabs.addSelectedChangeListener(e -> {
				this.cardSelected = e.getSelectedTab() == cardTab;
				selectPanel.run();
			});
			tabs.setSelectedTab(this.cardSelected ? cardTab : informationTab);
			selectPanel.run();
			card.set(openCard(cardContent, getEditedAthlete().getId(), card, readOnly, true));
			card.get().setCardTabSelected(this.cardSelected);
		}
		mainLayout.setFlexDirection(FlexDirection.COLUMN);
		mainLayout.setWidth("max-content");
		mainLayout.setMaxWidth("95vw");
		return mainLayout;
	}

	private void linkRestrictedPanel(Tab tab, Component panel) {
		String id = "athlete-panel-" + UUID.randomUUID();
		tab.setId(id + "-tab");
		tab.getElement().setAttribute("aria-controls", id);
		panel.setId(id);
		panel.getElement().setAttribute("role", "tabpanel");
		panel.getElement().setAttribute("aria-labelledby", id + "-tab");
		panel.getStyle().set("grid-area", "1 / 1").set("align-self", "start").set("justify-self", "start");
	}

	private void setRestrictedPanelSelected(Component panel, boolean selected) {
		// Keep inactive Vaadin fields rendered so a Card-first dialog can initialize the Information panel.
		panel.getStyle().remove("visibility");
		panel.getStyle().set("opacity", selected ? "1" : "0")
		        .set("pointer-events", selected ? "auto" : "none");
		panel.getElement().setAttribute("aria-hidden", Boolean.toString(!selected));
		if (selected) {
			panel.getElement().removeAttribute("inert");
		} else {
			panel.getElement().setAttribute("inert", "");
		}
	}

	private FlexLayout createTabSheets(Component footer, RegistrationTabPolicy.Layout layout, boolean readOnly) {
		TabSheet ts = new TabSheet();
		FlexLayout mainLayout = new FlexLayout(ts, footer);
		this.saveTask = new FormSaveTask(mainLayout);

		VerticalLayout registrationContent = new VerticalLayout(new Div(),
		        createIdForm(),
		        separator(),
		        createWeighInForm(),
		        separator(),
		        createCategoryAndSessionForm());
		ts.add(Translator.translate("Athlete.IdTab"), registrationContent);
		VerticalLayout additionalContent = new VerticalLayout(new Div(),
		        createInfoForm(),
		        separator(),
		        createRecordForm(),
		        separator(),
		        createDrawForm());
		ts.add(Translator.translate("Athlete.InfoTab"), additionalContent);
		if (layout.cardTab()) {
			addCardTab(ts, footer, layout, getEditedAthlete().getId(), readOnly);
		}
		mainLayout.setFlexDirection(FlexDirection.COLUMN);
		mainLayout.setFlexGrow(1.0D, ts);
		// 37rem is the dialog height for the first tab - fits on a 1366x768 laptop with
		// bookmark bar open. We don't need bigger than 37rem
		mainLayout.setHeight("min(85vh, 37rem)");
		mainLayout.setWidth("60rem");
		mainLayout.setMaxWidth("95vw");
		return mainLayout;
	}

	/**
	 * The lifting card as a tab. It is an independent editor with its own buttons, saved through its own conflict
	 * check: while it is shown, the buttons of the registration form are disabled, and "Update" never saves it. It is
	 * retained across tab switches. Cancel reloads only this editor, not the registration draft.
	 */
	private void addCardTab(TabSheet ts, Component footer, RegistrationTabPolicy.Layout layout, Long athleteId,
	        boolean readOnly) {
		Div cardContent = new Div();
		cardContent.setWidth("max-content");
		cardContent.setMaxWidth("100%");
		cardContent.getStyle().set("overflow-y", "auto");
		AtomicReference<AthleteCardFormFactory> card = new AtomicReference<>();
		Runnable load = () -> {
			card.set(openCard(cardContent, athleteId, card, readOnly, layout.restrictedRegistration()));
			card.get().setCardTabSelected(this.cardSelected);
		};
		Tab cardTab = ts.add(Translator.translate("Card"), cardContent);
		ts.addSelectedChangeListener(e -> {
			this.cardSelected = e.getSelectedTab() == cardTab;
			footer.getElement().setEnabled(!this.cardSelected);
			if (this.cardSelected && card.get() == null) {
				load.run();
			}
			if (card.get() != null) {
				card.get().setCardTabSelected(this.cardSelected);
			}
		});
		cardContent.addAttachListener(e -> {
			if (card.get() == null) {
				load.run();
			}
		});
		if (layout.initialTab() == RegistrationTabPolicy.Tab.CARD) {
			this.cardSelected = true;
			ts.setSelectedTab(cardTab);
			footer.getElement().setEnabled(false);
		}
	}

	private AthleteCardFormFactory openCard(Div container, Long athleteId, AtomicReference<AthleteCardFormFactory> card,
	        boolean readOnly, boolean allowInProgress) {
		try {
			container.removeAll();
			Athlete stored = AthleteRepository.findById(athleteId);
			if (stored == null) {
				throw new IllegalStateException("Athlete no longer exists: " + athleteId);
			}
			IAthleteEditing page = this.cardOrigin;
			// Saving the card leaves the form open: the page's closing behavior is for a card opened on its own.
			IAthleteEditing origin = new IAthleteEditing() {
				@Override
				public void closeDialog() {
				}

				@Override
				public void setDialogCloseOnOutsideClick(boolean closeOnOutsideClick) {
					if (page != null) {
						page.setDialogCloseOnOutsideClick(closeOnOutsideClick);
					}
				}

				@Override
				public OwlcmsCrudGrid<?> getEditingGrid() {
					return page != null ? page.getEditingGrid() : null;
				}

				@Override
				public FieldOfPlay getFop() {
					return page != null ? page.getFop() : null;
				}
			};
			Runnable discard = () -> {
				card.set(openCard(container, athleteId, card, readOnly, allowInProgress));
				card.get().setCardTabSelected(this.cardSelected);
			};
			Runnable saved = () -> {
				refreshGridBehind();
				if (this.resultsEntry && page != null) {
					page.closeDialog();
				}
			};
			AthleteCardFormFactory cardFactory = new AthleteCardFormFactory(Athlete.class, origin)
			        .setResultsUnlocked(page instanceof AnnouncerContent)
			        .setSharedEditor(this.saveTask, saved, discard, allowInProgress);
			container.add(cardFactory.buildNewForm(CrudOperation.UPDATE, stored, readOnly,
			        e -> discard.run(), e -> {}, null));
			return cardFactory;
		} catch (RuntimeException e) {
			NotificationUtils.unexpectedError(logger, e, null, athleteId);
			throw e;
		}
	}

	private void requireRegistration() {
		if (!this.registrationWritable || !AthleteEditAccess.registration(this.editor,
		        this.previousNext instanceof WeighinContent, this.resultsEntry)) {
			throw new SecurityException("Athlete registration editing is not permitted");
		}
	}

	private void applyEditingAccess() {
		if (this.binder == null || this.registrationWritable) {
			return;
		}
		this.binder.getFields().forEach(field -> field.setReadOnly(true));
		if (this.metadataWritable) {
			this.firstNameField.setReadOnly(false);
			this.lastNameField.setReadOnly(false);
			this.teamField.setReadOnly(false);
			this.federationCodesField.setReadOnly(false);
			this.individualEligibilityField.setReadOnly(false);
		}
	}

	private void saveMetadataOnly(ComponentEventListener<ClickEvent<Button>> completed) {
		AuditOrigin origin = AuditOrigin.fromUi("Athlete name, team and records form");
		Opening opening = this.metadataOpening;
		if (this.individualEligibilityField.isEmpty()) {
			this.individualEligibilityField.setInvalid(true);
			this.individualEligibilityField.setErrorMessage(Translator.translate("ThisFieldIsRequired"));
			return;
		}
		AthleteMetadata values = AthleteMetadataEdit.fromForm(this.firstNameField.getValue(), this.lastNameField.getValue(),
		        this.teamField.getValue(), this.federationCodesField.getValue(), this.individualEligibilityField.getValue());
		this.saveTask.execute(() -> {
			if (!this.metadataWritable) {
				throw new SecurityException("Athlete metadata is read-only");
			}
			AthleteEditAccess.requireMetadata(this.editor);
			return AthleteMetadataEdit.save(opening, values, origin);
		}, result -> {
			if (result.stale()) {
				NotificationUtils.centeredError(Translator.translate("Athlete.MetadataStale", opening.fullName()));
			} else {
				this.metadataOpening = new Opening(opening.athleteId(), result.athlete().getFullName(),
				        AthleteMetadataEdit.normalize(AthleteMetadata.from(result.athlete())),
				        result.athlete().getMetadataUpdatedAt());
				NotificationUtils.saved();
				if (this.keepOpen) {
					refreshGridBehind();
				} else {
					completed.onComponentEvent(null);
				}
			}
		}, error -> NotificationUtils.unexpectedError(logger, error, null, opening.athleteId()));
	}

	private Component createTitle(String string) {
		H4 title = new H4(Translator.translate(string));
		title.getStyle().set("margin-top", "0");
		title.getStyle().set("margin-bottom", "0.5ex");
		return title;
	}

	private FormLayout createWeighInForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Athlete.WeighInTitle");
		layout.add(title);
		layout.setColspan(title, NB_COLUMNS);

		this.scaleWeightField = new LocalizedDecimalField();
		this.scaleWeightField.getWrappedTextField().setValueChangeMode(ValueChangeMode.ON_CHANGE);
		this.scaleWeightField.getWrappedTextField().setAllowedCharPattern("[0-9.,]");
		BindingBuilder<Athlete, Double> bsw = this.binder.forField(this.scaleWeightField);
		validateBodyWeight(bsw, false);
		bindField(bsw, this.scaleWeightField, Athlete::getScaleWeight, Athlete::setScaleWeight);

		this.bodyWeightField = new LocalizedDecimalField();
		boolean deduct250g = Boolean.TRUE.equals(Competition.getCurrent().getDeduct250g());
		logger.trace("{} deduct250g initial state={} (expect false to hide scaleWeight)", LoggerUtils.whereFrom(), deduct250g);
		if (deduct250g) {
			logger.trace("{} deduct250g=true -> adding scaleWeight field", LoggerUtils.whereFrom());
			if (this.registrationWritable) {
				this.bodyWeightField.setReadOnly(true);
			}
			layoutAddFormItem(layout, this.scaleWeightField, Translator.translate("ScaleWeight"));
		} else {
			logger.trace("{} deduct250g=false -> not adding scaleWeight field", LoggerUtils.whereFrom());
		}
		this.bodyWeightField.getWrappedTextField().setAllowedCharPattern("[0-9.,]");
		this.bodyWeightField.getWrappedTextField().setValueChangeMode(ValueChangeMode.ON_CHANGE);
		BindingBuilder<Athlete, Double> bb = this.binder.forField(this.bodyWeightField);
		validateBodyWeight(bb, false);
		bindField(bb, this.bodyWeightField, Athlete::getBodyWeight, Athlete::setBodyWeight);
		layoutAddFormItem(layout, this.bodyWeightField,
		        Competition.getCurrent().getDeduct250g()
		                ? Translator.translate("BodyWeightWithDeduction")
		                : Translator.translate("BodyWeight"));

		this.scaleWeightField.addValueChangeListener(v -> {
			logger.debug("{} scaleWeight value change old={} new={}", LoggerUtils.whereFrom(), v.getOldValue(), v.getValue());
			if (v.getOldValue() != null && v.getValue() == null) {
				this.bodyWeightField.setValue(null);
				if (this.registrationWritable) {
					this.bodyWeightField.setReadOnly(false);
				}
			}
			if (v.getValue() != null) {
				boolean deduct250gCurrent = Boolean.TRUE.equals(Competition.getCurrent().getDeduct250g());
				logger.debug("{} deduct250g current state={} on value change", LoggerUtils.whereFrom(), deduct250gCurrent);
				if (deduct250gCurrent && this.registrationWritable) {
					this.bodyWeightField.setValue(v.getValue() - 0.250);
				}
				try {
					// this can fail upon first loading due to fields not fully initialized
					// we take the easy way out.
					recomputeCategories(this.genderField, this.bodyWeightField, this.categoryField.getValue(), this.eligibleField,
					        this.dateField,
					        this.qualifyingTotalField);
				} catch (Exception e) {
				}
				if (this.registrationWritable) {
					this.bodyWeightField.setReadOnly(true);
				}
			} else if (this.registrationWritable) {
				this.bodyWeightField.setReadOnly(false);
			}
			if (this.registrationWritable) {
				this.bodyWeightField.setReadOnly(Boolean.TRUE.equals(Competition.getCurrent().getDeduct250g()));
			}
		});

		if (!Boolean.TRUE.equals(Competition.getCurrent().getDeduct250g())) {
			// no scale weight field shown
			layout.add(new NativeLabel());
		}
		// skip to next row
		layout.add(new NativeLabel());

		this.snatch1DeclarationField = new LocalizedIntegerField();
		BindingBuilder<Athlete, Integer> bb1 = this.binder.forField(this.snatch1DeclarationField);
		configure20kgWeightField(this.snatch1DeclarationField);
		Validator<? super Integer> v1 = ValidationUtils.<Integer>checkUsingException((unused) -> {
			return validateStartingTotals(this.snatch1DeclarationField, this.cleanJerk1DeclarationField,
			        this.qualifyingTotalField);
		});
		bb1.withValidator(v1);
		Validator<Integer> v1a = new IntegerRangeValidator(Translator.translate("Weight_under_X", "350"), 0, 350);
		bb1.withValidator(v1a);
		bindField(bb1, this.snatch1DeclarationField,
		        a -> {
			        String snatch1Declaration = a.getSnatch1Declaration();
			        return snatch1Declaration != null && !snatch1Declaration.isBlank()
			                ? Integer.valueOf(snatch1Declaration)
			                : null;
		        },
		        (a, v) -> {
			        a.setSnatch1Declaration(v != null ? v.toString() : "");
		        });
		layoutAddFormItem(layout, this.snatch1DeclarationField, Translator.translate("SnatchDecl_"));

		this.cleanJerk1DeclarationField = new LocalizedIntegerField();
		BindingBuilder<Athlete, Integer> bb2 = this.binder.forField(this.cleanJerk1DeclarationField);
		configure20kgWeightField(this.cleanJerk1DeclarationField);
		Validator<? super Integer> v2 = ValidationUtils.<Integer>checkUsingException((unused) -> {
			return validateStartingTotals(this.cleanJerk1DeclarationField, this.snatch1DeclarationField,
			        this.qualifyingTotalField);
		});
		bb2.withValidator(v2);
		Validator<Integer> v2a = new IntegerRangeValidator(Translator.translate("Weight_under_X", "350"), 0, 350);
		bb2.withValidator(v2a);
		bindField(bb2, this.cleanJerk1DeclarationField,
		        a -> {
			        String cleanJerk1Declaration = a.getCleanJerk1Declaration();
			        return cleanJerk1Declaration != null && !cleanJerk1Declaration.isBlank()
			                ? Integer.valueOf(cleanJerk1Declaration)
			                : null;
		        },
		        (a, v) -> {
			        a.setCleanJerk1Declaration(v != null ? v.toString() : "");
		        });
		layoutAddFormItem(layout, this.cleanJerk1DeclarationField, Translator.translate("C_and_J_decl"));

		this.qualifyingTotalField = new LocalizedIntegerField();
		BindingBuilder<Athlete, Integer> bb3 = this.binder.forField(this.qualifyingTotalField);
		configure20kgWeightField(this.qualifyingTotalField);
		Validator<? super Integer> v3 = ValidationUtils.<Integer>checkUsingException((unused) -> {
			return validateStartingTotals(this.qualifyingTotalField, this.cleanJerk1DeclarationField,
			        this.snatch1DeclarationField);
		});
		bb3.withValidator(v3);
		Validator<Integer> v3a = Validator.from(this::entryTotalMeetsSelectedQt,
		        Translator.translate("EntryTotalBelowCategoryQualifyingTotal_MustForce"));
		bb3.withValidator(v3a);
		bindField(bb3, this.qualifyingTotalField, Athlete::getQualifyingTotal, Athlete::setQualifyingTotal);
		this.qualifyingTotalField.addValueChangeListener(ev -> {
			// repaint the eligible category labels so the QT marker follows the entry total
			// refreshAll() would discard the selection, refreshing each item keeps it
			if (this.eligibleField != null) {
				DataProvider<Category, ?> dp = this.eligibleField.getDataProvider();
				this.eligibleField.getGenericDataView().getItems().toList().forEach(dp::refreshItem);
			}
		});
		layoutAddFormItem(layout, this.qualifyingTotalField, Translator.translate("EntryTotal"));

		return layout;
	}

	@SuppressWarnings("unused")
	private void dumpCategories(Athlete editedAthlete2, Double bw, List<Category> allEligible2) {
		// logger.debug("{} bw={} {}", editedAthlete2, bw, allEligible2);
	}

	@SuppressWarnings({ "unchecked" })
	private void filterCategories(Category category, boolean initCategories) {
		setChangeListenersEnabled(false);

		Group curGroup = this.groupField.getValue();
		// force reading all groups
		this.groupField.setValue(curGroup);

		this.categoryField.setClearButtonVisible(true);

		if (initCategories && category != null) {
			// Compute ALL eligible categories to show as checkbox items
			// This is the same behavior as when user selects a category from the dropdown
			this.allEligible = findEligibleCategories(this.genderField, getAgeFromFields(), this.bodyWeightField,
			        category, this.qualifyingTotalField);
			eligibleField.setItems(this.allEligible);
			
			// Check only the ones that are stored in the database (or all if none stored)
			Set<Category> dbEligibles = getEditedAthlete() != null ? getEditedAthlete().getEligibleCategories() : null;
			if (dbEligibles != null && !dbEligibles.isEmpty()) {
				// Filter to only include eligibles that are in allEligible (by code match)
				Set<Category> toCheck = new LinkedHashSet<>();
				for (Category dbCat : dbEligibles) {
					for (Category eligible : this.allEligible) {
						if (eligible.getCode().equals(dbCat.getCode())) {
							toCheck.add(eligible);
							break;
						}
					}
				}
				setEligibleField(toCheck);
			} else {
				setEligibleField(new LinkedHashSet<>(this.allEligible));
			}
			setCategoryFieldValue(category);
		} else if (initCategories) {
			this.allEligible = findEligibleCategories(this.genderField, getAgeFromFields(), this.bodyWeightField,
			        this.categoryField.getValue(), this.qualifyingTotalField);
			updateCategoryFields(categoryField.getValue(), category, this.eligibleField, this.qualifyingTotalField,
			        this.allEligible, this.allEligible, false);
		}

		this.genderField.addValueChangeListener((vc) -> {
			if (!isChangeListenersEnabled()) {
				return;
			}
			List<Category> pertinentCategories = CategoryRepository.findByGenderAgeBW(getGenderFieldValue(),
			        getAgeFromFields(), null);
			safeCategorySetItems(pertinentCategories);
			recomputeCategoriesForDemographicChange();
		});

		if (Competition.getCurrent().isUseBirthYear()) {
			this.dateField = this.yobField;
			// Workaround for bug
			// https://stackoverflow.com/questions/55532055/java-casting-java-11-throws-lambdaconversionexception-while-1-8-does-not
			ValueChangeListener<ValueChangeEvent<?>> listener = (vc) -> {
				if (!isChangeListenersEnabled()) {
					return;
				}
				List<Category> pertinentCategories = CategoryRepository.findByGenderAgeBW(getGenderFieldValue(),
				        getAgeFromFields(), null);
				safeCategorySetItems(pertinentCategories);
				recomputeCategoriesForDemographicChange();
			};
			this.yobField.addValueChangeListener(listener);
		} else {
			this.dateField = this.fullBirthDateField;
			ValueChangeListener<ValueChangeEvent<?>> listener = (vc) -> {
				if (!isChangeListenersEnabled()) {
					return;
				}
				List<Category> pertinentCategories = CategoryRepository.findByGenderAgeBW(getGenderFieldValue(),
				        getAgeFromFields(), null);
				safeCategorySetItems(pertinentCategories);
				recomputeCategoriesForDemographicChange();
			};
			this.fullBirthDateField.addValueChangeListener(listener);
		}
		this.wrappedBWTextField = this.bodyWeightField.getWrappedTextField();
		this.wrappedBWTextField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
		this.wrappedBWTextField.setAutoselect(true);
		this.wrappedBWTextField.addValueChangeListener((vc) -> {
			// logger.trace("wrappedBWTextField listenersEnabled={} invalid={}",
			// isChangeListenersEnabled(), bodyWeightField.isInvalid());
			if (!vc.isFromClient() || !isChangeListenersEnabled() || this.bodyWeightField.isInvalid()) {
				return;
			}
			recomputeCategories(this.genderField, this.bodyWeightField, this.categoryField.getValue(), this.eligibleField,
			        this.dateField,
			        this.qualifyingTotalField);
		});

		this.categoryField.addValueChangeListener((vc) -> {
			if (!vc.isFromClient()) {
				return;
			}
			Category value = vc.getValue();
			// logger.trace("category new value {} listenersEnabled {} {}", value, isChangeListenersEnabled(),(value ==
			// null ? LoggerUtils.fullStackTrace() : LoggerUtils.whereFrom()));
			if (!isChangeListenersEnabled()) {
				return;
			}
			setChangeListenersEnabled(false); // prevent recursion.
			if (this.genderField.getValue() == null) {
				this.genderField.setValue(value.getGender());
			}

			recomputeCategories(this.genderField, this.bodyWeightField, vc.getValue(), this.eligibleField,
			        this.dateField,
			        this.qualifyingTotalField, true, vc.getOldValue());
			setChangeListenersEnabled(true);
		});

		this.eligibleField.addValueChangeListener((vc) -> {
			if (!isChangeListenersEnabled()) {
				return;
			}
			setChangeListenersEnabled(false); // prevent recursion.
			// false as last argument: do not reset to all eligible categories
			Category categor = this.categoryField.getValue();
			Set<Category> selectedEligibilityCategories = vc.getValue();

			this.allEligible = findEligibleCategories(this.genderField, getAgeFromFields(), this.bodyWeightField,
			        categor,
			        this.qualifyingTotalField);

			// this.allEligible = CategoryRepository.doFindEligibleCategories(this.getEditedAthlete(), genderField.getValue(),
			// getAgeFromFields(), inferBWFromCat(this.bodyWeightField.getValue(), categor), zeroIfNull(qualifyingTotalField));

			logger.debug("{}eligibleField change fromClient={} old={} new={} category={} allEligible={}",
			        FieldOfPlay.getLoggingName(OwlcmsSession.getFop()), vc.isFromClient(), codes(vc.getOldValue()),
			        codes(selectedEligibilityCategories), categor != null ? categor.getCode() : null, codes(this.allEligible));
			Stream<Category> filter = this.allEligible.stream().filter(c -> c.sameAsAny(selectedEligibilityCategories));
			Category category2 = filter.findFirst().orElse(null);
			setCategoryFieldValue(category2);
			refreshTeamMembershipFields();
			revalidateEntryTotal();
			logger.debug("{}eligibleField after change eligible={} category={}", FieldOfPlay.getLoggingName(OwlcmsSession.getFop()),
			        codes(this.eligibleField.getValue()),
			        this.categoryField.getValue() != null ? this.categoryField.getValue().getCode() : null);
			setChangeListenersEnabled(true);
		});

		this.qualifyingTotalField.addValueChangeListener((vc) -> {
			if (!isChangeListenersEnabled() || this.qualifyingTotalField.isInvalid()) {
				return;
			}
			recomputeCategories(this.genderField, this.bodyWeightField, this.categoryField.getValue(), this.eligibleField,
			        this.dateField,
			        this.qualifyingTotalField);
		});
		this.qualifyingTotalField.setAutoselect(true);
		refreshTeamMembershipFields();

		setChangeListenersEnabled(true);
	}

	private List<Category> findEligibleCategories(ComboBox<Gender> genderField, Integer ageFromFields,
	        LocalizedDecimalField bodyWeightField, Category cat,
	        LocalizedIntegerField qualifyingTotalField2) {
		// best match is first
		Double bw = bodyWeightField.getValue();
		Double catW = cat != null ? cat.getMaximumWeight() : null;
		
		// Only expand eligibility when there's no actual body weight entered
		// Once a body weight is entered, use standard eligibility based on actual weight
		boolean hasActualBodyWeight = (bw != null);
		
		if (bw == null && catW != null) {
			bw = adjustBW(catW, ageFromFields);
		}
		List<Category> baseEligible = CategoryRepository.doFindEligibleCategories(this.getEditedAthlete(), genderField.getValue(),
		        ageFromFields, bw, zeroIfNull(qualifyingTotalField2));
		
		// Expand eligibility based on registration category weight window ONLY when no actual body weight
		if (hasActualBodyWeight) {
			return baseEligible;
		}
		return expandEligibilityForWeightWindow(baseEligible, cat, genderField.getValue(), ageFromFields);
	}

	/**
	 * Expand eligible categories based on the registration category's weight window.
	 * 
	 * For each age group eligible for the age currently entered in the form,
	 * include all categories whose weight bounds fall within the registration category's
	 * possible weight range [registration_min_weight, registration_max_weight].
	 * 
	 * @param baseEligible the initial list of eligible categories
	 * @param registrationCategory the athlete's registration category
	 * @param gender the athlete's gender
	 * @return expanded list of eligible categories
	 */
	private List<Category> expandEligibilityForWeightWindow(List<Category> baseEligible,
	        Category registrationCategory, Gender gender, Integer age) {
		if (registrationCategory == null || gender == null || age == null) {
			return baseEligible;
		}
		
		// Compute weight window from registration category
		// registration_min_weight = lower bound + 0.01
		// registration_max_weight = upper bound (999 for superheavy)
		double registrationMinWeight = registrationCategory.getMinimumWeight() + 0.01;
		double registrationMaxWeight = registrationCategory.getMaximumWeight();
		
		// Keep expansion constrained to age groups eligible for the values currently
		// entered in the form. Stored categories may reflect an earlier birth date.
		Set<AgeGroup> eligibilityAgeGroups = new LinkedHashSet<>();
		List<Category> allAgeEligible = CategoryRepository.findByGenderAgeBW(gender, age, null);
		for (Category c : allAgeEligible) {
			if (c.getAgeGroup() != null && Boolean.TRUE.equals(c.isActive())) {
				eligibilityAgeGroups.add(c.getAgeGroup());
			}
		}
		
		// Include age groups from base eligible categories (computed from age/gender/weight)
		for (Category c : baseEligible) {
			if (c.getAgeGroup() != null) {
				eligibilityAgeGroups.add(c.getAgeGroup());
			}
		}
		
		// Build expanded eligibility map, keyed by code to avoid duplicates
		Map<String, Category> expandedEligibles = new LinkedHashMap<>();
		for (Category c : baseEligible) {
			expandedEligibles.put(c.getCode(), c);
		}
		
		// For each eligibility age group, find all categories within the weight window
		for (AgeGroup ageGroup : eligibilityAgeGroups) {
			// Get all active categories for this age group and gender
			List<Category> ageGroupCategories = CategoryRepository.findFiltered(
			        null, gender, null, ageGroup, null, null, true, -1, -1);
			
			for (Category c : ageGroupCategories) {
				if (c.getAgeGroup() == null || !Boolean.TRUE.equals(c.isActive())) {
					continue;
				}
				
				// Use integer comparisons to avoid round-off errors per spec:
				// floor(lower_bound) >= floor(registration_min_weight) && 
				// round(upper_bound) <= round(registration_max_weight)
				int catLowerBound = (int) Math.floor(c.getMinimumWeight());
				int catUpperBound = (int) Math.round(c.getMaximumWeight());
				int regMinWeight = (int) Math.floor(registrationMinWeight);
				int regMaxWeight = (int) Math.round(registrationMaxWeight);
				
				if (catLowerBound >= regMinWeight && catUpperBound <= regMaxWeight) {
					expandedEligibles.putIfAbsent(c.getCode(), c);
				}
			}
		}
		
		// Sort by specificity (U15 before M/Open), then by weight within each age group
		List<Category> result = new ArrayList<>(expandedEligibles.values());
		result.sort(Category.specificityComparator.thenComparing(Category::getMinimumWeight));
		
		return result;
	}

	private Double adjustBW(Double catW, Integer ageFromFields) {
		if (catW > 990) {
			Gender gender = getGenderFieldValue();
			if (gender == Gender.M) {
				// a superheavy youth male will be +98 but a 110 JR
				return ageFromFields != null && ageFromFields <= 17 ? 103D : 111D;
			} else {
				// a superheavy youth female will be +77 but a 86 JR
				return ageFromFields != null && ageFromFields <= 17 ? 80D : 90D;
			}
		} else {
			return catW;
		}
	}

	@SuppressWarnings("unchecked")
	private Integer getAgeFromFields() {
		Integer age = null;
		LocalDate now;
		if (Config.getCurrent().isUseCompetitionDate()) {
			now = Competition.getCurrent().getCompetitionDate();
			if (now == null) {
				now = LocalDate.now();
				logger./**/warn("Competition date is not set; using {} for registration age calculations", now);
			}
		} else {
			now = LocalDate.now();
		}
		if (Competition.getCurrent().isUseBirthYear()) {
			Integer yob = this.yobField.getValue();
			if (yob != null) {
				age = now.getYear() - yob;
			}
		} else {
			LocalDate date = this.fullBirthDateField.getValue();
			if (date != null) {
				age = (now.getYear() - date.getYear());
			}
		}
		return age;
	}

	private Group getCurrentGroup() {
		return this.currentGroup;
	}

	private Athlete getEditedAthlete() {
		return this.editedAthlete;
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private Gender getGenderFieldValue() {
		// HasValue<?, Gender> genderField = (HasValue<?, Gender>)
		// binder.getBinding("gender").get().getField();
		Gender gender = this.genderField.getValue();
		return gender;
	}

	@SuppressWarnings("unused")
	private NextAthleteAble getPreviousNext() {
		return this.previousNext;
	}

	private boolean isChangeListenersEnabled() {
		return this.changeListenersEnabled;
	}

	private boolean isCheckOther20kgFields() {
		return this.checkOther20kgFields;
	}

	private boolean isIgnoreErrors() {
		return this.acceptingWarnings;
	}

	// marks eligible categories whose qualifying total exceeds the current entry total
	private String eligibleCategoryLabel(Category category) {
		Integer entryTotal = null;
		try {
			entryTotal = this.qualifyingTotalField != null ? this.qualifyingTotalField.getValue() : null;
		} catch (Exception e) {
			entryTotal = null;
		}
		if (category != null && category.requiresEntryTotalConfirmation(entryTotal)) {
			return category.getDisplayName() + " "
			        + Translator.translate("EntryTotalBelowCategoryQualifyingTotal_Suffix");
		}
		return category != null ? category.getDisplayName() : "";
	}

	private FormItem layoutAddFormItem(FormLayout layout, Component field,
	        String translate) {
		if (field instanceof TextField textField) {
			textField.setAutocomplete(Autocomplete.OFF);
		} else if (field instanceof WrappedTextField<?> wrappedField) {
			wrappedField.getWrappedTextField().setAutocomplete(Autocomplete.OFF);
		}
		Div label = new Div();
		label.setText(translate);
		label.getStyle().set("text-align", "right");
		FormItem fi = layout.addFormItem(field, label);
		fi.getStyle().set("align-items", "center");
		fi.getStyle().set("align-self", "center");
		return fi;
	}

	private void migrateEligibleChampionships(ComboBox<Gender> genderField,
	        LocalizedDecimalField bodyWeightField, Category selectedCategory,
	        CheckboxGroup<Category> eligibleField, LocalizedIntegerField qualifyingTotalField,
	        Integer age, Category previousRegistrationCategory) {
		Set<Category> previousEligibles = new LinkedHashSet<>(eligibleField.getValue());
		this.allEligible = findEligibleCategories(genderField, age, bodyWeightField, selectedCategory,
		        qualifyingTotalField);

		Double assumedBodyWeight = bodyWeightField.getValue() != null
		        ? bodyWeightField.getValue()
		        : inferBW(selectedCategory);
		List<Category> candidates = CategoryRepository.doFindEligibleCategories(this.getEditedAthlete(),
		        genderField.getValue(), age, assumedBodyWeight, zeroIfNull(qualifyingTotalField));
		Set<Category> migratedEligibles = CategoryRepository.migrateEligibleCategories(
		        candidates, previousEligibles, previousRegistrationCategory, qualifyingTotalField.getValue());

		Map<String, Category> allItemsByCode = this.allEligible.stream()
		        .collect(Collectors.toMap(Category::getCode, category -> category, (first, duplicate) -> first,
		                LinkedHashMap::new));
		candidates.forEach(category -> allItemsByCode.putIfAbsent(category.getCode(), category));
		this.allEligible = new ArrayList<>(allItemsByCode.values());
		Set<String> migratedCodes = migratedEligibles.stream().map(Category::getCode).collect(Collectors.toSet());
		Set<Category> checkboxEligibles = this.allEligible.stream()
		        .filter(category -> migratedCodes.contains(category.getCode()))
		        .collect(Collectors.toCollection(LinkedHashSet::new));

		eligibleField.setItems(this.allEligible);
		setEligibleField(checkboxEligibles);
		String registrationChampionship = previousRegistrationCategory != null
		        && previousRegistrationCategory.getAgeGroup() != null
		                ? previousRegistrationCategory.getAgeGroup().getChampionshipName()
		                : null;
		Category bestMatchCategory = checkboxEligibles.stream()
		        .filter(category -> category.getAgeGroup() != null)
		        .filter(category -> Objects.equals(registrationChampionship,
		                category.getAgeGroup().getChampionshipName()))
		        .findFirst()
		        .orElseGet(() -> bestMatch(new ArrayList<>(checkboxEligibles)));
		setCategoryFieldValue(bestMatchCategory);
		Binding<Athlete, ?> categoryBinding = this.fieldToBinding.get(this.categoryField);
		if (categoryBinding != null) {
			categoryBinding.validate();
		}
		refreshTeamMembershipFields();
	}

	private void recomputeCategories(
	        ComboBox<Gender> genderField, LocalizedDecimalField bodyWeightField,
	        Category selectedCategory, CheckboxGroup<Category> eligibleField,
	        HasValue<?, ?> dateField, LocalizedIntegerField qualifyingTotalField2) {
		recomputeCategories(genderField, bodyWeightField, selectedCategory, eligibleField, dateField,
		        qualifyingTotalField2, false, null);
	}

	private void recomputeCategoriesForDemographicChange() {
		Category selectedCategory = this.categoryField.getValue();
		this.allEligible = findEligibleCategories(this.genderField, getAgeFromFields(), this.bodyWeightField,
		        selectedCategory, this.qualifyingTotalField);
		Category bestMatchCategory = selectedCategory != null && categoryIsEligible(selectedCategory, this.allEligible)
		        ? selectedCategory
		        : bestMatch(this.allEligible);
		updateCategoryFields(selectedCategory, bestMatchCategory, this.eligibleField, this.qualifyingTotalField,
		        this.allEligible, this.allEligible, true);
	}

	private void recomputeCategories(
	        ComboBox<Gender> genderField, LocalizedDecimalField bodyWeightField,
	        Category selectedCategory, CheckboxGroup<Category> eligibleField,
	        HasValue<?, ?> dateField, LocalizedIntegerField qualifyingTotalField2,
	        boolean migrateEligibleChampionships, Category previousRegistrationCategory) {

		Integer age = getAgeFromFields();
		// List<Championship> previousChampionships = championshipsForCategories(eligibleField.getValue());
		List<String> previousAgeGroups = ageGroupsForCategories(eligibleField.getValue()).stream().map(ag -> ag.getCode()).toList();
		logger.debug("previous age groups {}", previousAgeGroups);
		if (migrateEligibleChampionships && selectedCategory != null && genderField.getValue() != null
		        && age != null && age > 5 && age < 120) {
			migrateEligibleChampionships(genderField, bodyWeightField, selectedCategory, eligibleField,
			        qualifyingTotalField2, age, previousRegistrationCategory);
			return;
		}
		if (bodyWeightField.getValue() != null) {
			if (genderField.getValue() != null && age != null) {
				// body weight, gender, date
				this.allEligible = findEligibleCategories(genderField, getAgeFromFields(), bodyWeightField,
				        selectedCategory, qualifyingTotalField2);
				if (selectedCategory != null && categoryIsEligible(selectedCategory, this.allEligible) && selectedCategory.getMaximumWeight() < 999) {
					// current registration category is amongst eligibles. Don't recompute anything.
					//logger.debug("leave alone");
					if (FeatureSwitch.BEST_MATCH_CATEGORIES.isEffective()) {
						Category bestMatchCategory = bestMatch(this.allEligible);
						updateCategoryFields(selectedCategory, bestMatchCategory, eligibleField, qualifyingTotalField2,
						        this.allEligible, this.allEligible, false);
					}
				} else {
					// category is null or not within eligibles, recompute
					//logger.debug("recompute, cat={} allEligible = {}", selectedCategory, this.allEligible);

					List<Category> filteredEligibles = this.allEligible.stream()
					        .filter(e -> previousAgeGroups.isEmpty() || previousAgeGroups.contains(e.getAgeGroupCode())).toList();
					logger.debug("eligibilty filtered on age groups {} {} {}", allEligible, filteredEligibles,
					        filteredEligibles.stream().map(Category::getId).collect(Collectors.toList()));

					Category bestMatchCategory = bestMatch(filteredEligibles);
					//logger.debug("best match category {} {}", bestMatchCategory, bestMatchCategory != null ? bestMatchCategory.getAgeGroup() : null);
					updateCategoryFields(selectedCategory, bestMatchCategory, eligibleField, qualifyingTotalField2,
					        filteredEligibles, this.allEligible, true);
				}
			} else {
				logger.debug("bw, but need age and gender");
				// cannot compute eligibility and category
			}
		} else {
			// no body weight, but category available.
			if (age != null && selectedCategory != null) {
				Integer ageFromFields = getAgeFromFields();
				logger.debug("no body weight, but category available:  ageFromFields={}", ageFromFields);
				if (ageFromFields != null && ageFromFields > 5 && ageFromFields < 120) {
					// Use findEligibleCategories which will expand based on registration category weight window
					this.allEligible = findEligibleCategories(genderField, ageFromFields, bodyWeightField,
					        selectedCategory, qualifyingTotalField2);

					List<Category> filteredEligibles = this.allEligible.stream()
					        .filter(category -> previousAgeGroups.isEmpty()
					                || previousAgeGroups.contains(category.getAgeGroupCode()))
					        .toList();
					updateCategoryFields(selectedCategory, selectedCategory, eligibleField, qualifyingTotalField2,
					        filteredEligibles, this.allEligible, false);
				}
			} else if (genderField.getValue() != null && age != null) {
				// use age, gender and qualifying total
				logger.debug("using age gender and total");
				this.allEligible = findEligibleCategories(genderField, getAgeFromFields(), bodyWeightField,
				        selectedCategory, qualifyingTotalField2);
				updateCategoryFields(selectedCategory, null, eligibleField, qualifyingTotalField2,
				        this.allEligible, this.allEligible, false); /* !!! was true */
			} else if (selectedCategory != null) {
				// only the category, no age
				int qualifyingTotal = qualifyingTotalField2.getValue();
				Double bw = selectedCategory.getMaximumWeight();
				int catAge = selectedCategory.getAgeGroup().getMaxAge();
				this.allEligible = CategoryRepository.doFindEligibleCategories(this.getEditedAthlete(),
				        selectedCategory.getGender(), catAge, bw, qualifyingTotal);
				// logger.debug("cat-based allEligible {} {}", selectedCategory, this.allEligible);
				this.eligibleField.setItems(allEligible);
				Category bestMatchCategory = bestMatch(this.allEligible);
				updateCategoryFields(selectedCategory, bestMatchCategory, eligibleField, qualifyingTotalField2,
				        this.allEligible,
				        this.allEligible, true);
			} else {
				// cannot compute eligibility and category
				logger.debug("not enough information to compute eligibility");
			}
		}

	}

	public Double inferBW(Double bw1) {
		// Use all assigned categories to guess a bodyweight
		List<Category> cats = editedAthlete.getParticipations().stream()
		        .map(p -> p.getCategory())
		        .filter(c -> isRegularBWCategory(c)) // not an open category due to lower bound
		        // .peek(c -> logger.debug("cat {} {} {}", c, c.getMinimumWeight(), c.getMaximumWeight()))
		        .toList();
		if (cats.isEmpty()) {
			Integer age = editedAthlete.getAge();
			Gender gender = editedAthlete.getGender();
			logger.debug("age {} gender {} cats {}", age, gender, cats);
			if (age != null && gender != null) {
				if (bw1 > 900) {
					if (age <= 17) {
						return gender == Gender.M ? 103D : 82D;
					} else {
						return gender == Gender.M ? 111D : 88D;
					}
				}
			} else {
				return bw1;
			}
		}
		return inferBWFromCats(cats);
	}

	public Double inferBWFromCats(List<Category> cats) {
		Double bw1 = null;
		// use smallest category upper bound for normal categories
		Optional<Double> max = cats.stream().map(c -> c.getMaximumWeight()).min(Double::compareTo);
		// for superheavies, the lowerbound of the categories is used. For youth, can be several eligible, use smallest.
		Optional<Double> min = cats.stream().map(c -> c.getMinimumWeight()).min(Double::compareTo);
		if (max.isPresent()) {
			if (max.get() < 900) {
				// normal case, weight is at upper bound of category.
				bw1 = max.get();
			} else if (min.isPresent()) {
				// super heavy
				// we want a youth 102+ to be 102.1
				bw1 = min.get() + 0.1;
			}
		}
		logger.trace("inferred weight {}", bw1);
		return bw1;
	}

	public Double inferBWFromCat(Double double1, Category selectedCategory) {
		Double bw;
		if (double1 != null) {
			return double1;
		}
		if (selectedCategory == null) {
			return null;
		}
		if (selectedCategory.getMaximumWeight() > 990) {
			bw = selectedCategory.getMinimumWeight() + 0.1;
		} else {
			bw = selectedCategory.getMaximumWeight() - 0.1;
		}
		return bw;
	}

	public boolean isRegularBWCategory(Category c) {
		boolean regularNotSuperHeavy = c.getMaximumWeight() < 900;
		boolean superHeavy = c.getMinimumWeight() > 10 && c.getMaximumWeight() > 900;
		logger.debug("c {}  isRegular = {} c.getMaximumWeight() < 900 {}  (c.getMinimumWeight() > 10 && c.getMaximumWeight() > 900) {}", c,
		        regularNotSuperHeavy || superHeavy, regularNotSuperHeavy, superHeavy);
		return regularNotSuperHeavy || superHeavy;
	}

	public Double inferBW(Category cat) {
		Double bw = cat.getMaximumWeight();
		var bw2 = adjustBW(bw, cat.getAgeGroup().getMaxAge());
		return bw2;
	}

	private void safeCategorySetItems(List<Category> categories) {
		Category curCat = this.categoryField.getValue();
		// logger.debug("safeCategorySetItems curCat={}", curCat);
		boolean listenersEnabled = isChangeListenersEnabled();
		try {
			setChangeListenersEnabled(false);
			setCategoryFieldItems(categories);
		} finally {
			setChangeListenersEnabled(listenersEnabled);
		}
		setCategoryFieldValue(curCat);
	}

	private Hr separator() {
		Hr hr = new Hr();
		hr.getStyle().set("margin-top", "0.4em");
		hr.getStyle().set("margin-bottom", "0.4em");
		hr.getStyle().set("background-color", "var(--lumo-contrast-30pct)");
		hr.getStyle().set("height", "2px");
		return hr;
	}

	private void setCategoryFieldItems(List<Category> categories) {
		this.categoryField.setItems(categories);
	}

	private void setCategoryFieldValue(Category category2) {
		// logger.debug("setCategoryFieldValue {} {}",category2, LoggerUtils.stackTrace());
		this.categoryField.setValue(category2);
	}

	private void setChangeListenersEnabled(boolean changeListenersEnabled) {
		this.changeListenersEnabled = changeListenersEnabled;
	}

	private void setCheckOther20kgFields(boolean checkOther20kgFields) {
		logger.debug("checkOther20kgFields={} {}", checkOther20kgFields, LoggerUtils.whereFrom());
		this.checkOther20kgFields = checkOther20kgFields;
	}

	private void setEditedAthlete(Athlete editedAthlete) {
		this.editedAthlete = editedAthlete;
	}

	private void setFocus(Component form) {
		form.addAttachListener((e) -> {
			String lastNameValue;
			Double bwValue;
			if ((lastNameValue = this.lastNameField.getValue()) == null || lastNameValue.isBlank()) {
				this.lastNameField.focus();
			} else if (Competition.getCurrent().getDeduct250g() && ((bwValue = this.scaleWeightField.getValue()) == null || bwValue < 0.01D)) {
				this.scaleWeightField.focus();
				this.bodyWeightField.getWrappedTextField().setTabIndex(-1);
			} else if ((bwValue = this.bodyWeightField.getValue()) == null || bwValue < 0.01D) {
				this.bodyWeightField.focus();
			} else {
				this.groupField.focus();
			}
		});
	}

	private void setPreviousNext(NextAthleteAble parentGrid) {
		this.previousNext = parentGrid;
	}

	private void setupAthlete(CrudOperation operation, Athlete aFromList) {
		this.registrationEditCheck = null;
		if (operation == CrudOperation.ADD) {
			Athlete editedAthlete2 = new Athlete();

			// "*" is used as wildcard in certain URLs and a fake "*" group is circulated, do not use!
			if (getCurrentGroup() != null && !getCurrentGroup().getName().contentEquals("*")) {
				editedAthlete2.setGroup(getCurrentGroup());
			}
			logger.debug("created new Athlete {}", System.identityHashCode(editedAthlete2));
			setEditedAthlete(editedAthlete2);
		} else if (aFromList != null) {
			setEditedAthlete(AthleteRepository.findById(aFromList.getId()));
			this.registrationEditCheck = RegistrationEditCheck.open(getEditedAthlete());
		}
	}

	/**
	 * @param selectedCategory
	 * @param bestMatch
	 * @param eligibleField
	 * @param qualifyingTotalField2
	 * @param filteredEligibles
	 * @param allEligibles
	 * @param recomputeEligibles
	 * @param categoryField
	 */
	private void updateCategoryFields(Category selectedCategory, Category bestMatch,
	        CheckboxGroup<Category> eligibleField, LocalizedIntegerField qualifyingTotalField2,
	        List<Category> filteredEligibles,
	        List<Category> allEligibles,
	        boolean recomputeEligibles) {

		// logger.debug("updating category fields {} {}\n{}", selectedCategory, bestMatch, LoggerUtils.stackTrace());
		LinkedHashSet<Category> newEligibles = new LinkedHashSet<>();
		Set<Category> prevEligibles;
		if (recomputeEligibles) {
			prevEligibles = new LinkedHashSet<>();
			prevEligibles.addAll(allEligibles);
		} else {
			prevEligibles = eligibleField.getValue();
		}

		if (prevEligibles != null) {
			// update the list of eligible categories. Must use the matching items in
			// allEligibles so that database updates work.
			for (Category oldEligible : prevEligibles) {
				for (Category newEligible : filteredEligibles) {
					if (newEligible.getCode().contentEquals(oldEligible.getCode()) && Boolean.TRUE.equals(newEligible.isActive())) {
						//logger.debug("substituting eligibles {} {}", newEligible.longDump(), System.identityHashCode(newEligible));
						newEligibles.add(newEligible);
						break;
					}
				}
			}
			// logger.trace("updateCategoryFields filteredEligibles {}", filteredEligibles.stream().map(v -> v.shortDump()).collect(Collectors.toList()));

			// List<Category> pertinentCategories = CategoryRepository.findByGenderAgeBW(getGenderFieldValue(),
			// getAgeFromFields(), null);

			boolean listenerStatus = isChangeListenersEnabled();
			try {
				setChangeListenersEnabled(false);
				// logger.trace("setting eligible checkboxes {}", allEligibles);
				eligibleField.setItems(allEligibles);
			} finally {
				setChangeListenersEnabled(listenerStatus);
			}
			//logger.debug("setting field new eligibles {} {}", newEligibles.size(), newEligibles.stream().map(v -> v.longDump()).collect(Collectors.toList()));
			setEligibleField(newEligibles);
		} else {
			//logger.debug("no previous eligibles");
		}

		Category matchingEligible = null;
		if (bestMatch != null) {
			// find the matching eligible with the same code.
			matchingEligible = null;
			String selectedCode = selectedCategory != null ? selectedCategory.getCode() : null;
			for (Category eligible : newEligibles) {
				// always allow currently selected if eligible
				String eligibleCode = eligible != null ? eligible.getCode() : "";

				// logger.debug("selected {} eligible {}",selectedCode, eligibleCode);
				if (eligibleCode != null && selectedCode != null && eligibleCode.contentEquals(selectedCode)) {
					matchingEligible = eligible;
					break;
				}
			}

			if (matchingEligible == null) {
				for (Category eligible : newEligibles) {
					if (eligible != null && eligible.getCode().contentEquals(bestMatch.getCode())) {
						matchingEligible = eligible;
						break;
					}
				}
			}

			if (matchingEligible == null && selectedCategory != null) {
				Set<Category> priorEligibles = eligibleField.getValue();
				// need to recompute the eligibility categories outright before declaring there is none.
				Double inferBW = inferBW(selectedCategory);
				allEligibles = CategoryRepository.doFindEligibleCategories(this.getEditedAthlete(), genderField.getValue(),
				        getAgeFromFields(), inferBW, zeroIfNull(qualifyingTotalField2));
				eligibleField.setItems(allEligibles);
				for (Category eligible : allEligibles) {
					if (eligible.getCode().contentEquals(selectedCode)) {
						matchingEligible = eligible;
						break;
					}
				}

				// filter the new eligibilities to only the previous championships.
				List<String> prevChampionships = priorEligibles.stream().map(e -> e.getAgeGroup().getChampionshipName()).toList();
				Set<Category> filteredEligibleCategories = allEligibles.stream()
				        .filter(e -> prevChampionships.contains(e.getAgeGroup().getChampionshipName()))
				        .collect(Collectors.toSet());

				setEligibleField(filteredEligibleCategories);
			}
			setCategoryFieldValue(matchingEligible);
		} else {
		}
		refreshTeamMembershipFields();
	}

	public void setEligibleField(Set<Category> newEligibles) {
		//logger.debug("setEligibleField {} {}", newEligibles.size(), newEligibles.stream().map(v -> v.longDump()).collect(Collectors.toList()));
		eligibleField.setValue(newEligibles);
		logger.debug("{}setEligibleField {} {}", FieldOfPlay.getLoggingName(null), codes(newEligibles), LoggerUtils.whereFrom());
		revalidateEntryTotal();
	}

	// the entry total validator also checks the qualified categories against their qualifying totals
	private void revalidateEntryTotal() {
		Binding<Athlete, ?> binding = this.fieldToBinding.get(this.qualifyingTotalField);
		if (binding != null) {
			BindingValidationStatus<?> status = binding.validate();
			logger.debug("{}revalidateEntryTotal error={} message={} fieldInvalid={} entryTotal={} eligibles={} {}",
			        FieldOfPlay.getLoggingName(null), status.isError(), status.getMessage().orElse(null),
			        this.qualifyingTotalField.isInvalid(), this.qualifyingTotalField.getValue(),
			        codes(this.eligibleField.getValue()), LoggerUtils.whereFrom(1));
		} else {
			logger.debug("{}revalidateEntryTotal no binding yet", FieldOfPlay.getLoggingName(null));
		}
	}

	private static List<String> codes(Collection<Category> categories) {
		return categories == null ? null : categories.stream().map(c -> c != null ? c.getCode() : "null").toList();
	}

	private boolean validateStartingTotals(
	        LocalizedIntegerField snatch1DeclarationField2, LocalizedIntegerField cleanJerk1DeclarationField2,
	        LocalizedIntegerField qualifyingTotalField2) throws RuleViolationException.StartingWeightDeclarationViolation {
		if (isIgnoreErrors()) {
			return true;
		}
		try {
			// logger.trace("before {} validation", snatch1DeclarationField2);
			getEditedAthlete().validateStartingTotalsRule(
			        zeroIfNull(this.snatch1DeclarationField),
			        zeroIfNull(this.cleanJerk1DeclarationField),
			        zeroIfNull(this.qualifyingTotalField));
			// clear errors on other fields.
			// logger.trace("clearing errors on {} {}", cleanJerk1DeclarationField2,
			// qualifyingTotalField2);
			clearErrors(cleanJerk1DeclarationField2, qualifyingTotalField2);
			// logger.trace("checking again on {} {}", cleanJerk1DeclarationField2,
			// qualifyingTotalField2);
			checkOther20kgFields(cleanJerk1DeclarationField2, qualifyingTotalField2);
		} catch (Exception e1) {
			// logger.trace("{} validation failed", snatch1DeclarationField2);
			// signal exception on all fields that must be mutually-consistent
			checkOther20kgFields(cleanJerk1DeclarationField2, qualifyingTotalField2);
			throw e1;
		}
		return true;
	}

	private int zeroIfNull(LocalizedIntegerField intField) {
		Integer value = intField.getValue();
		return value != null ? value : 0;
	}

}