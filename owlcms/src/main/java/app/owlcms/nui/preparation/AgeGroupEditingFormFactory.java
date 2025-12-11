/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Arrays;
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
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
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
		int maxLength = 5;
		codeField.setRequired(true);
		codeField.setMaxLength(maxLength);
		this.binder.forField(codeField)
		        .withValidator(
		                new StringLengthValidator(Translator.translate("ThisFieldIsRequired", maxLength), 1, null))
		        .withValidator(
		                new StringLengthValidator(Translator.translate("CodeMustBeShort", maxLength), 0, maxLength))
		        .bind(AgeGroup::getCode, AgeGroup::setCode);

		Checkbox gendered = new Checkbox(Translator.translate("CodeIncludesGender"));
		this.binder.forField(gendered).bind(AgeGroup::isAlreadyGendered, AgeGroup::setAlreadyGendered);

		HorizontalLayout codeInfo = new HorizontalLayout(codeField, gendered);
		codeInfo.setAlignItems(Alignment.CENTER);
		formLayout.addFormItem(codeInfo, createLabel(Translator.translate("AgeGroupCode")));

		ComboBox<Championship> championshipField = new ComboBox<>();
		List<Championship> list = Championship.getMap().values().stream().sorted().toList();
		championshipField.setItems(new ListDataProvider<>(list));
		championshipField.setItemLabelGenerator((ad) -> ad.getName());
		championshipField.setRequired(true);
		championshipField.setRequiredIndicatorVisible(false);
		this.binder.forField(championshipField).bind(AgeGroup::getChampionship, AgeGroup::setChampionship);
		formLayout.addFormItem(championshipField, createLabel(Translator.translate("Championship")));

		ComboBox<Ranking> medalScoreSystemField = new ComboBox<>();
		medalScoreSystemField.setClearButtonVisible(true);
		List<Ranking> rankings = Arrays.asList(Ranking.values());
		List<Ranking> medalScoreRankings = rankings.stream().filter(r -> r.isMedalScore()).toList();
		medalScoreSystemField.setItems(new ListDataProvider<>(medalScoreRankings));
		medalScoreSystemField.setItemLabelGenerator((ad) -> Translator.translate("Ranking." + ad.name()));
		// logger.debug("***** scoring system {}", aFromDb.getMedalScoringSystem());
		this.binder.forField(medalScoreSystemField).bind(AgeGroup::getMedalScoringSystem, AgeGroup::setScoringSystem);
		// When medals scoring system changes, recompute mustCompute immediately
		medalScoreSystemField.addValueChangeListener(e -> {
			RankingConfig.updateMustCompute();
		});
		formLayout.addFormItem(medalScoreSystemField, createLabel(Translator.translate("MedalScoringSystem")));
		
		ComboBox<Ranking> bestLifterSystemField = new ComboBox<>();
		bestLifterSystemField.setClearButtonVisible(true);
		bestLifterSystemField.setItems(new ListDataProvider<>(medalScoreRankings));
		bestLifterSystemField.setItemLabelGenerator((ad) -> Translator.translate("Ranking." + ad.name()));
		// logger.debug("***** scoring system {}", aFromDb.getMedalScoringSystem());
		this.binder.forField(bestLifterSystemField).bind(AgeGroup::getBestAthleteScoringSystem, AgeGroup::setBestAthleteScoringSystem);
		// When best athlete scoring system changes, recompute mustCompute immediately
		bestLifterSystemField.addValueChangeListener(e -> {
			RankingConfig.updateMustCompute();
		});
		formLayout.addFormItem(bestLifterSystemField, createLabel(Translator.translate("AgeGroup.BestAthleteScoringSystem")));
		bestLifterSystemField.setHelperText(Translator.translate("AgeGroup.BestAthleteScoringSystemExplanation")
				.replaceAll(" ", "\u00A0")
				.replaceAll("-", "\u2011"));

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
		this.binder.forField(genderField)
		        .asRequired(Translator.translate("ThisFieldIsRequired"))
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

}