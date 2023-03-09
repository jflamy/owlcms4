/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Arrays;
import java.util.Collection;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.CrudFormConfiguration;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;

import app.owlcms.components.fields.CategoryGridField;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.competition.Competition;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.CustomFormFactory;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class AgeGroupEditingFormFactory
        extends OwlcmsCrudFormFactory<AgeGroup>
        implements CustomFormFactory<AgeGroup> {

	private CategoryGridField catField;
	@SuppressWarnings("unused")
	private Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupEditingFormFactory.class);
	private AgeGroupContent origin;

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

	@Override
	public Component buildNewForm(CrudOperation operation, AgeGroup aFromDb, boolean readOnly,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

		FormLayout formLayout = new FormLayout();
		formLayout.setResponsiveSteps(new ResponsiveStep("0", 1, LabelsPosition.ASIDE));
		formLayout.setWidth("50em");

		binder = buildBinder(null, aFromDb);
		String message = Translator.translate("AgeFormat");

		TextField codeField = new TextField();
		formLayout.addFormItem(codeField, Translator.translate("AgeGroupCode"));
		int maxLength = 5;
		codeField.setRequired(true);
		binder.forField(codeField)
		        .withValidator(
		                new StringLengthValidator(Translator.translate("ThisFieldIsRequired", maxLength), 1, maxLength))
		        .withValidator(
		                new StringLengthValidator(Translator.translate("CodeMustBeShort", maxLength), 0, maxLength))
		        .bind(AgeGroup::getCode, AgeGroup::setCode);

		ComboBox<AgeDivision> ageDivisionField = new ComboBox<>();
		ageDivisionField.setItems(new ListDataProvider<>(Arrays.asList(AgeDivision.values())));
		ageDivisionField.setItemLabelGenerator((ad) -> Translator.translate("Division." + ad.name()));
		binder.forField(ageDivisionField).bind(AgeGroup::getAgeDivision, AgeGroup::setAgeDivision);
		formLayout.addFormItem(ageDivisionField, Translator.translate("AgeDivision"));

		TextField minAgeField = new TextField();
		formLayout.addFormItem(minAgeField, Translator.translate("MinimumAge"));
		binder.forField(minAgeField)
		        .withNullRepresentation("")
		        .withConverter(new StringToIntegerConverter(message))
		        .withValidator(new IntegerRangeValidator(message, 0, 999))
		        .bind(AgeGroup::getMinAge, AgeGroup::setMinAge);

		TextField maxAgeField = new TextField();
		formLayout.addFormItem(maxAgeField, Translator.translate("MaximumAge"));
		binder.forField(maxAgeField)
		        .withNullRepresentation("")
		        .withConverter(new StringToIntegerConverter(message))
		        .withValidator(new IntegerRangeValidator(message, 0, 999))
		        .bind(AgeGroup::getMaxAge, AgeGroup::setMaxAge);

		ComboBox<Gender> genderField = new ComboBox<>();
		genderField.setPlaceholder(Translator.translate("Gender"));
		if (Competition.getCurrent().isGenderInclusive()) {
			genderField.setItems(Gender.M, Gender.F);
			genderField.setItemLabelGenerator((i) -> {
				switch (i) {
				case M:
					return Translator.translate("Gender.Men");
				case F:
					return Translator.translate("Gender.Women");
				default:
					throw new IllegalStateException("can't happen");
				}
			});
		} else {
			genderField.setItems(Gender.M, Gender.F, Gender.I);
			genderField.setItemLabelGenerator((i) -> {
				switch (i) {
				case M:
					return Translator.translate("Gender.Men");
				case F:
					return Translator.translate("Gender.Women");
				case I:
					return Translator.translate("Gender.Inclusive");
				default:
					throw new IllegalStateException("can't happen");
				}
			});
		}
		binder.forField(genderField).bind(AgeGroup::getGender, AgeGroup::setGender);
		formLayout.addFormItem(genderField, Translator.translate("Gender"));

		catField = new CategoryGridField(aFromDb);
		catField.setWidthFull();

		binder.forField(catField).bind(AgeGroup::getCategories, AgeGroup::setCategories);
		formLayout.addFormItem(catField, Translator.translate("BodyWeightCategories"));

		binder.readBean(aFromDb);
		if (minAgeField.getValue().isEmpty()) {
			minAgeField.setValue("0");
		}
		if (maxAgeField.getValue().isEmpty()) {
			maxAgeField.setValue("999");
		}
		if (genderField.getValue() == null) {
			genderField.setValue(Gender.F);
		}
		if (ageDivisionField.getValue() == null) {
			ageDivisionField.setValue(AgeDivision.U);
		}

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
	public TextField defineOperationTrigger(CrudOperation operation, AgeGroup domainObject,
	        ComponentEventListener<ClickEvent<Button>> action) {
		return super.defineOperationTrigger(operation, domainObject, action);
	}

	@Override
	public void delete(AgeGroup ageGroup) {
		AgeGroupRepository.delete(ageGroup);
	}

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
		AgeGroup saved = AgeGroupRepository.save(ageGroup);
		// logger.trace("saved {}", saved.getCategories().get(0).longDump());
		origin.closeDialog();
		origin.highlightResetButton();
		return saved;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void bindField(HasValue field, String property, Class<?> propertyType, CrudFormConfiguration c) {
		binder.forField(field);
		super.bindField(field, property, propertyType, c);
	}

}