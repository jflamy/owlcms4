/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.CrudFormConfiguration;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;

import app.owlcms.components.ConfirmationDialog;
import app.owlcms.components.fields.CategoryGridField;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.AssignedAthletesException;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.RankingConfig;
import app.owlcms.data.competition.Competition;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.CustomFormFactory;
import ch.qos.logback.classic.Logger;

@SuppressWarnings({ "serial", "removal" })
public class AgeGroupEditingFormFactory
        extends OwlcmsCrudFormFactory<AgeGroup>
        implements CustomFormFactory<AgeGroup> {

	private CategoryGridField catField;
	@SuppressWarnings("unused")
	private Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupEditingFormFactory.class);
	private AgeGroupContent origin;
	private Checkbox medalsAwarded;
	private String pendingChampionshipName;

	AgeGroupEditingFormFactory(Class<AgeGroup> domainType, AgeGroupContent origin) {
		super(domainType);
		this.origin = origin;
	}

	@Override
	public AgeGroup add(AgeGroup AgeGroup) {
		AgeGroupRepository.add(AgeGroup);
		return AgeGroup;
	}

	@Override
	public Binder<AgeGroup> buildBinder(CrudOperation operation, AgeGroup domainObject) {
		return super.buildBinder(operation, domainObject);
	}

	@Override
	public String buildCaption(CrudOperation operation, AgeGroup domainObject) {
		String name = domainObject.getName();
		if (name == null || name.isEmpty()) {
			return Translator.translate("AgeGroup");
		} else {
			return Translator.translate("AgeGroup") + " " + domainObject.getName();
		}
	}

	@Override
	public Component buildFooter(CrudOperation operation, AgeGroup domainObject,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> postOperationCallBack,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, boolean shortcutEnter,
	        Button... buttons) {
		return super.buildFooter(operation, domainObject, cancelButtonClickListener, postOperationCallBack,
		        deleteButtonClickListener, false, buttons);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Component buildNewForm(CrudOperation operation, AgeGroup aFromDb, boolean readOnly,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

		FormLayout formLayout = new FormLayout();
		formLayout.setResponsiveSteps(new ResponsiveStep("0", 1, LabelsPosition.ASIDE));
		formLayout.getStyle().set("--vaadin-form-item-label-width", "15em");
		formLayout.setWidth("50em");

		this.binder = buildBinder(null, aFromDb);
		String message = Translator.translate("AgeFormat");

		TextField codeField = new TextField();
		int maxLength = 10;
		codeField.setRequired(true);
		codeField.setMaxLength(maxLength);

		ComboBox<Gender> genderField = new ComboBox<>();
		genderField.setPlaceholder(Translator.translate("Gender"));
		if (Competition.getCurrent().isGenderInclusive()) {
			genderField.setItems(Gender.M, Gender.F, Gender.I);
			genderField.setItemLabelGenerator((i) -> {
				return i.asGenderName();
			});
		} else {
			genderField.setItems(Gender.M, Gender.F);
			genderField.setItemLabelGenerator((i) -> {
				return i.asGenderName();
			});
		}

		this.binder.forField(codeField)
		        .withValidator(
		                new StringLengthValidator(Translator.translate("ThisFieldIsRequired", maxLength), 1, null))
		        .withValidator(
		                new StringLengthValidator(Translator.translate("CodeMustBeShort", maxLength), 0, maxLength))
		        .withValidator((code, ctx) -> {
		                Gender gender = genderField.getValue();
		                if (code != null && gender != null && hasDuplicateCodeGender(aFromDb, code, gender)) {
		                        return ValidationResult.error(
		                                Translator.translate("AgeGroup.CodeAlreadyExists", code, gender));
		                } else {
		                        return ValidationResult.ok();
		                }
		        })
		        .bind(AgeGroup::getCode, AgeGroup::setCode);

		Checkbox gendered = new Checkbox(Translator.translate("CodeIncludesGender"));
		this.binder.forField(gendered).bind(AgeGroup::isAlreadyGendered, AgeGroup::setAlreadyGendered);

		HorizontalLayout codeInfo = new HorizontalLayout(codeField, gendered);
		codeInfo.setAlignItems(Alignment.CENTER);
		formLayout.addFormItem(codeInfo, createLabel(Translator.translate("AgeGroupCode")));

		ComboBox<Championship> championshipField = new ComboBox<>();
		List<Championship> list = new ArrayList<>(Championship.findAll());
		championshipField.setItems(new ListDataProvider<>(list));
		championshipField.setItemLabelGenerator((ad) -> ad.getName());
		championshipField.setRequired(true);
		championshipField.setRequiredIndicatorVisible(false);
		championshipField.setAllowCustomValue(true);

		// If age group references a championship that doesn't exist yet, track its name
		Championship initialChampionship = aFromDb.getChampionship();
		String initialName = aFromDb.getChampionshipName();
		if (initialChampionship == null && initialName != null && !initialName.isBlank()) {
			this.pendingChampionshipName = initialName;
			championshipField.setPlaceholder(initialName);
		} else {
			this.pendingChampionshipName = null;
		}

		Button editChampionshipButton = new Button(Translator.translate("Sessions.EditDetails"), e -> {
			Championship selected = championshipField.getValue();
			if (selected != null && selected.getId() != null) {
				new ChampionshipDetailsDialog(selected, null).open();
			}
		});
		editChampionshipButton.setEnabled(initialChampionship != null && initialChampionship.getId() != null);

		// Handle user typing a new championship name
		championshipField.addCustomValueSetListener(event -> {
			String customValue = event.getDetail();
			if (customValue == null || customValue.isBlank()) {
				return;
			}
			Championship found = list.stream()
				.filter(c -> c.getName().equalsIgnoreCase(customValue.trim()))
				.findFirst().orElse(null);
			if (found != null) {
				championshipField.setValue(found);
				this.pendingChampionshipName = null;
				editChampionshipButton.setEnabled(true);
			} else {
				this.pendingChampionshipName = customValue.trim();
				championshipField.setValue(null);
				championshipField.setPlaceholder(customValue.trim());
				editChampionshipButton.setEnabled(false);
			}
		});

		// Update details button when selection changes
		championshipField.addValueChangeListener(e -> {
			Championship val = e.getValue();
			if (val != null) {
				this.pendingChampionshipName = null;
				editChampionshipButton.setEnabled(val.getId() != null);
			} else if (this.pendingChampionshipName == null) {
				editChampionshipButton.setEnabled(false);
			}
		});

		// Binder preserves pending championship name when ComboBox value is null
		this.binder.forField(championshipField).bind(
			AgeGroup::getChampionship,
			(ag, championship) -> {
				if (championship != null) {
					ag.setChampionship(championship);
				} else if (this.pendingChampionshipName != null) {
					ag.setChampionshipName(this.pendingChampionshipName);
				}
			}
		);

		HorizontalLayout championshipRow = new HorizontalLayout(championshipField, editChampionshipButton);
		championshipRow.setAlignItems(Alignment.CENTER);
		formLayout.addFormItem(championshipRow, createLabel(Translator.translate("Championship")));

		TextField minAgeField = new TextField();
		formLayout.addFormItem(minAgeField, createLabel(Translator.translate("MinimumAge")));
		// we don't use asRequired because of weird placement of required indicator
		this.binder.forField(minAgeField)
		        .withValidator(
		                new StringLengthValidator(Translator.translate("ThisFieldIsRequired"), 1, 3))
		        .withConverter(new StringToIntegerConverter(message))
		        .withValidator(new IntegerRangeValidator(message, 0, 999))
		        .bind(AgeGroup::getMinAge, AgeGroup::setMinAge);

		TextField maxAgeField = new TextField();
		formLayout.addFormItem(maxAgeField, createLabel(Translator.translate("MaximumAge")));
		// we don't use asRequired because of weird placement of required indicator
		this.binder.forField(maxAgeField)
		        .withValidator(
		                new StringLengthValidator(Translator.translate("ThisFieldIsRequired"), 1, 3))
		        .withConverter(new StringToIntegerConverter(message))
		        .withValidator(new IntegerRangeValidator(message, 0, 999))
		        .bind(AgeGroup::getMaxAge, AgeGroup::setMaxAge);

		this.binder.forField(genderField)
		        .asRequired(Translator.translate("ThisFieldIsRequired"))
		        .withValidator((gender, ctx) -> {
		                String code = codeField.getValue();
		                if (code != null && gender != null && hasDuplicateCodeGender(aFromDb, code, gender)) {
		                        return ValidationResult.error(
		                                Translator.translate("AgeGroup.CodeAlreadyExists", code, gender));
		                } else {
		                        return ValidationResult.ok();
		                }
		        })
		        .bind(AgeGroup::getGender, AgeGroup::setGender);
		formLayout.addFormItem(genderField, createLabel(Translator.translate("Gender")));

		this.catField = new CategoryGridField(aFromDb);
		this.catField.setWidthFull();
		this.binder.forField(this.catField).bind(AgeGroup::getCategories, AgeGroup::setCategories);
		formLayout.addFormItem(this.catField, createLabel(Translator.translate("BodyWeightCategories")));
		
		this.medalsAwarded = new Checkbox();
		this.binder.forField(this.medalsAwarded).bind(AgeGroup::getMedals, AgeGroup::setMedals);
		formLayout.addFormItem(this.medalsAwarded, createLabel(Translator.translate("AwardMedals")));
		

		// if (minAgeField.getValue().isEmpty()) {
		// minAgeField.setValue("0");
		// }
		// if (maxAgeField.getValue().isEmpty()) {
		// maxAgeField.setValue("999");
		// }
		// if (genderField.getValue() == null) {
		// genderField.setValue(Gender.F);
		// }
		// if (championshipField.getValue() == null) {
		// championshipField.setValue(Championship.ofType(ChampionshipType.U));
		// }

		this.binder.readBean(aFromDb);

		Component footerLayout = this.buildFooter(operation, aFromDb, cancelButtonClickListener,
		        updateButtonClickListener, deleteButtonClickListener, false);

		VerticalLayout mainLayout = new VerticalLayout(footerLayout, formLayout);
		mainLayout.setHorizontalComponentAlignment(Alignment.END, footerLayout);
		mainLayout.setMargin(false);
		mainLayout.setPadding(false);
		return mainLayout;
	}

	@Override
	public Button buildOperationButton(CrudOperation operation, AgeGroup domainObject,
	        ComponentEventListener<ClickEvent<Button>> gridCallBackAction) {
		return super.buildOperationButton(operation, domainObject, gridCallBackAction);
	}

	@Override
	public void delete(AgeGroup ageGroup) {
		AgeGroupRepository.delete(ageGroup);
	}

	// @Override
	// public TextField defineOperationTrigger(CrudOperation operation, AgeGroup domainObject,
	// ComponentEventListener<ClickEvent<Button>> action) {
	// return super.defineOperationTrigger(operation, domainObject, action);
	// }

	@Override
	public Collection<AgeGroup> findAll() {
		// will not be called, handled by the grid.
		return null;
	}

	@Override
	public boolean setErrorLabel(BinderValidationStatus<?> validationStatus, boolean showErrorOnFields) {
		return super.setErrorLabel(validationStatus, showErrorOnFields);
	}

	/**
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public AgeGroup update(AgeGroup ageGroup) {
		// Create championship if it doesn't exist yet
		String champName = ageGroup.getChampionshipName();
		if (champName != null && !champName.isBlank() && Championship.of(champName) == null) {
			Championship.addChampionship(champName, ChampionshipType.U);
		}

		// array is used to workaround Java language restriction on setting variables in lambda
		AgeGroup[] saved = new AgeGroup[1];
		try {
			saved[0] = AgeGroupRepository.save(ageGroup);
			RankingConfig.updateMustCompute();
			this.origin.closeDialog();
			this.origin.highlightResetButton();
			return saved[0];
		} catch (AssignedAthletesException e) {
			ConfirmationDialog cd = new ConfirmationDialog(
			        Translator.translate("CategoryAssignment.Title"),
			        Translator.translate("CategoryAssignment.Warning"),
			        null,
			        () -> {
				        ageGroup.setForceSave(true);
				        try {
					        saved[0] = AgeGroupRepository.save(ageGroup);
					        RankingConfig.updateMustCompute();
				        } catch (AssignedAthletesException e1) {
					        saved[0] = ageGroup;
				        }
				        this.origin.closeDialog();
				        this.origin.getCrud().refreshGrid();
				        this.origin.highlightResetButton();
			        });
			cd.open();
			return saved[0];
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void bindField(HasValue field, String property, Class<?> propertyType, CrudFormConfiguration c) {
		this.binder.forField(field);
		super.bindField(field, property, propertyType, c);
	}

	private Component createLabel(String translate) {
		Div label = new Div(translate);
		return label;
	}

	private boolean hasDuplicateCodeGender(AgeGroup currentAgeGroup, String code, Gender gender) {
		if (code == null || code.isBlank() || gender == null) {
			return false;
		}
		String normalizedCode = code.trim();
		List<AgeGroup> all = AgeGroupRepository.findAll();
		for (AgeGroup ag : all) {
			if (ag == null) {
				continue;
			}
			// Skip self when editing existing age group
			if (currentAgeGroup != null && currentAgeGroup.getId() != null 
			        && currentAgeGroup.getId().equals(ag.getId())) {
				continue;
			}
			String otherCode = ag.getCode();
			if (otherCode == null || otherCode.isBlank()) {
				continue;
			}
			if (normalizedCode.equalsIgnoreCase(otherCode.trim()) && gender == ag.getGender()) {
				return true;
			}
		}
		return false;
	}

}