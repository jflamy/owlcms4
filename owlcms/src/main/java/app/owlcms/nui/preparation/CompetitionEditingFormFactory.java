/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Collection;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.CrudFormConfiguration;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.IntegerRangeValidator;

import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.CustomFormFactory;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class CompetitionEditingFormFactory
        extends OwlcmsCrudFormFactory<Competition>
        implements CustomFormFactory<Competition> {

	String browserZoneId;
	@SuppressWarnings("unused")
	private Logger logger = (Logger) LoggerFactory.getLogger(CompetitionEditingFormFactory.class);

	@SuppressWarnings("unused")
	private CompetitionContent origin;

	CompetitionEditingFormFactory(Class<Competition> domainType, CompetitionContent origin) {
		super(domainType);
		this.origin = origin;
	}

	@Override
	public Competition add(Competition c) {
		CompetitionRepository.save(c);
		return c;
	}

	@Override
	public Binder<Competition> buildBinder(CrudOperation operation, Competition domainObject) {
		return super.buildBinder(operation, domainObject);
	}

	@Override
	public String buildCaption(CrudOperation operation, Competition competition) {
		String name = competition.getCompetitionName();
		if (name == null || name.isEmpty()) {
			return Translator.translate("Competition");
		} else {
			return Translator.translate("Competition") + " " + competition.getCompetitionName();
		}
	}

	@Override
	public Component buildFooter(CrudOperation operation, Competition domainObject,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> postOperationCallBack,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, boolean shortcutEnter,
	        Button... buttons) {
		return super.buildFooter(operation, domainObject, cancelButtonClickListener, postOperationCallBack,
		        deleteButtonClickListener, false, buttons);
	}

	@Override
	public Component buildNewForm(CrudOperation operation, Competition domainObject, boolean readOnly,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> operationButtonClickListener) {
		String email = domainObject.getFederationEMail();
		String trimmedMail = email != null ? email.trim() : email;
		if (email != null && trimmedMail != null && email.length() != trimmedMail.length()) {
			// kludge to remove spurious message
			domainObject.setFederationEMail(trimmedMail);
		}
		return this.buildNewForm(operation, domainObject, readOnly, cancelButtonClickListener,
		        operationButtonClickListener, null);
	}

	@Override
	public Component buildNewForm(CrudOperation operation, Competition comp, boolean readOnly,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

		setBinder(buildBinder(operation, comp));

		FormLayout competitionLayout = competitionForm();
		FormLayout federationLayout = federationForm();
		FormLayout rulesLayout = rulesForm();
		FormLayout specialLayout = specialRulesForm();

		// footerLayout1 callbacks are not actually used -- footerLayout2 overwrites the
		// callbacks.
//        Component footerLayout1 = this.buildFooter(operation, competition, cancelButtonClickListener,
//                c -> {
//                    Competition nCompetition = this.update(competition);
//                    Locale defaultLocale = nCompetition.getDefaultLocale();
//                    Translator.setForcedLocale(defaultLocale);
//                    Translator.reset();
//                }, deleteButtonClickListener, false);
		Component footerLayout2 = this.buildFooter(operation, comp, cancelButtonClickListener,
		        c -> {
			        this.update(comp);
		        }, deleteButtonClickListener, false);

		VerticalLayout mainLayout = new VerticalLayout(
		        footerLayout2,
		        competitionLayout, separator(),
		        federationLayout, separator(),
		        rulesLayout, separator(),
		        specialLayout);
		mainLayout.setHorizontalComponentAlignment(Alignment.END, footerLayout2);
		mainLayout.setMargin(false);
		mainLayout.setPadding(false);

		binder.readBean(comp);
		return mainLayout;
	}

	@Override
	public Button buildOperationButton(CrudOperation operation, Competition domainObject,
	        ComponentEventListener<ClickEvent<Button>> gridCallBackAction) {
		return super.buildOperationButton(operation, domainObject, gridCallBackAction);
	}

	@Override
	public TextField defineOperationTrigger(CrudOperation operation, Competition domainObject,
	        ComponentEventListener<ClickEvent<Button>> action) {
		return super.defineOperationTrigger(operation, domainObject, action);
	}

	@Override
	public void delete(Competition competition) {
		CompetitionRepository.delete(competition);
	}

	@Override
	public Collection<Competition> findAll() {
		// will not be called, handled by the grid.
		return null;
	}

	@Override
	public boolean setErrorLabel(BinderValidationStatus<?> validationStatus, boolean showErrorOnFields) {
		return super.setErrorLabel(validationStatus, showErrorOnFields);
	}

	@Override
	public Competition update(Competition competition) {
		Competition saved = CompetitionRepository.save(competition);
		return saved;
	}

	private FormLayout competitionForm() {
		FormLayout competitionLayout = createLayout();
		Component title = createTitle("Competition.informationTitle");
		competitionLayout.add(title);
		competitionLayout.setColspan(title, 2);

		TextField nameField = new TextField();
		nameField.setWidthFull();
		competitionLayout.addFormItem(nameField, Translator.translate("Competition.competitionName"));
		binder.forField(nameField)
		        .withNullRepresentation("")
		        .bind(Competition::getCompetitionName, Competition::setCompetitionName);

		DatePicker dateField = new DatePicker();
		competitionLayout.addFormItem(dateField, Translator.translate("Competition.competitionDate"));
		binder.forField(dateField)
		        .bind(Competition::getCompetitionDate, Competition::setCompetitionDate);

		TextField organizerField = new TextField();
		organizerField.setWidthFull();
		competitionLayout.addFormItem(organizerField, Translator.translate("Competition.competitionOrganizer"));
		binder.forField(organizerField)
		        .withNullRepresentation("")
		        .bind(Competition::getCompetitionOrganizer, Competition::setCompetitionOrganizer);

		TextField siteField = new TextField();
		siteField.setWidthFull();
		competitionLayout.addFormItem(siteField, Translator.translate("Competition.competitionSite"));
		binder.forField(siteField)
		        .withNullRepresentation("")
		        .bind(Competition::getCompetitionSite, Competition::setCompetitionSite);

		TextField cityField = new TextField();
		cityField.setWidthFull();
		competitionLayout.addFormItem(cityField, Translator.translate("Competition.competitionCity"));
		binder.forField(cityField)
		        .withNullRepresentation("")
		        .bind(Competition::getCompetitionCity, Competition::setCompetitionCity);

		return competitionLayout;
	}

	private FormLayout createLayout() {
		FormLayout layout = new FormLayout();
//        layout.setWidth("1024px");
		layout.setResponsiveSteps(new ResponsiveStep("0", 1, LabelsPosition.TOP),
		        new ResponsiveStep("800px", 2, LabelsPosition.TOP));
		return layout;
	}

	private Component createTitle(String string) {
		H4 title = new H4(Translator.translate(string));
		title.getStyle().set("margin-top", "0");
		title.getStyle().set("margin-bottom", "0");
		return title;
	}

	private FormLayout federationForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Competition.federationTitle");
		layout.add(title);
		layout.setColspan(title, 2);

		TextField federationField = new TextField();
		federationField.setWidthFull();
		layout.addFormItem(federationField, Translator.translate("Competition.federation"));
		binder.forField(federationField)
		        .withNullRepresentation("")
		        .bind(Competition::getFederation, Competition::setFederation);

		TextField federationAddressField = new TextField();
		federationAddressField.setWidthFull();
		layout.addFormItem(federationAddressField, Translator.translate("Competition.federationAddress"));
		binder.forField(federationAddressField)
		        .withNullRepresentation("")
		        .bind(Competition::getFederationAddress, Competition::setFederationAddress);

		TextField federationEMailField = new TextField();
		federationEMailField.setWidthFull();
		layout.addFormItem(federationEMailField, Translator.translate("Competition.federationEMail"));
		binder.forField(federationEMailField)
		        .withNullRepresentation("")
		        .withValidator(new EmailValidator("Invalid Email Address"))
		        .bind(Competition::getFederationEMail, Competition::setFederationEMail);

		TextField federationWebSiteField = new TextField();
		federationWebSiteField.setWidthFull();
		layout.addFormItem(federationWebSiteField, Translator.translate("Competition.federationWebSite"));
		binder.forField(federationWebSiteField)
		        .withNullRepresentation("")
		        .bind(Competition::getFederationWebSite, Competition::setFederationWebSite);

		return layout;
	}

	private Span labelWithHelp(String string, String explanation) {
		Icon help = VaadinIcon.QUESTION_CIRCLE_O.create();
		help.getStyle().set("height", "1.2em");
		help.getStyle().set("vertical-align", "top");
		help.getStyle().set("font-weight", "bold");
		Label label = new Label(Translator.translate(string));
		Span span = new Span();
		span.setTitle(Translator.translate(explanation));
		span.add(label, help);
		return span;
	}

	private FormLayout rulesForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Competition.rulesTitle");
		layout.add(title);
		layout.setColspan(title, 2);

		Checkbox enforce20kgRuleField = new Checkbox();
		layout.addFormItem(enforce20kgRuleField, Translator.translate("Competition.enforce20kgRule"));
		binder.forField(enforce20kgRuleField)
		        .bind(Competition::isEnforce20kgRule, Competition::setEnforce20kgRule);

		Checkbox snatchCJTotalField = new Checkbox();
		layout.addFormItem(snatchCJTotalField, Translator.translate("Competition.snatchCJTotalMedals"));
		binder.forField(snatchCJTotalField)
		        .bind(Competition::isSnatchCJTotalMedals, Competition::setSnatchCJTotalMedals);

		Checkbox useBirthYearField = new Checkbox();
		layout.addFormItem(useBirthYearField, Translator.translate("Competition.useBirthYear"));
		binder.forField(useBirthYearField)
		        .bind(Competition::isUseBirthYear, Competition::setUseBirthYear);

		RadioButtonGroup<Integer> sinclairYear = new RadioButtonGroup<>();
		layout.addFormItem(sinclairYear, Translator.translate("sinclair"));
		sinclairYear.setItems(2020, 2024);
		binder.forField(sinclairYear)
		        .bind(Competition::getSinclairYear, Competition::setSinclairYear);

		IntegerField wakeUpDelayField = new IntegerField();
		layout.addFormItem(wakeUpDelayField, Translator.translate("Competition.decisionRequestDelayLabel"));
		binder.forField(wakeUpDelayField).bind(Competition::getRefereeWakeUpDelay, Competition::setRefereeWakeUpDelay);

		Checkbox mastersField = new Checkbox();
		layout.addFormItem(mastersField, Translator.translate("Competition.masters"));
		binder.forField(mastersField)
		        .bind(Competition::isMasters, Competition::setMasters);

		return layout;
	}

	private Hr separator() {
		Hr hr = new Hr();
		hr.getStyle().set("margin-top", "0.5em");
		hr.getStyle().set("margin-bottom", "1.0em");
		hr.getStyle().set("background-color", "var(--lumo-contrast-30pct)");
		hr.getStyle().set("height", "2px");
		return hr;
	}

	private void setBinder(Binder<Competition> buildBinder) {
		binder = buildBinder;
	}

	private FormLayout specialRulesForm() {
		String message = Translator.translate("Competition.teamSizeInvalid");

		FormLayout layout = createLayout();
		Component title = createTitle("Competition.specialRulesTitle");
		layout.add(title);
		layout.setColspan(title, 2);

		TextField mensTeamSizeField = new TextField();
		layout.addFormItem(mensTeamSizeField,
		        labelWithHelp("Competition.mensTeamSize", "Competition.teamSizeExplanation"));
		binder.forField(mensTeamSizeField)
		        .withNullRepresentation("")
		        .withConverter(new StringToIntegerConverter(message))
		        .withValidator(new IntegerRangeValidator(message, 0, 99))
		        .bind(Competition::getMensTeamSize, Competition::setMensTeamSize);

		TextField womensTeamSizeField = new TextField();
		layout.addFormItem(womensTeamSizeField,
		        labelWithHelp("Competition.womensTeamSize", "Competition.teamSizeExplanation"));
		binder.forField(womensTeamSizeField)
		        .withNullRepresentation("")
		        .withConverter(new StringToIntegerConverter(message))
		        .withValidator(new IntegerRangeValidator(message, 0, 99))
		        .bind(Competition::getWomensTeamSize, Competition::setWomensTeamSize);

		Checkbox roundRobinOrderField = new Checkbox();
		layout.addFormItem(roundRobinOrderField,
		        labelWithHelp("Competition.roundRobinOrder", "Competition.roundRobinOrderExplanation"));
		binder.forField(roundRobinOrderField)
		        .bind(Competition::isRoundRobinOrder, Competition::setRoundRobinOrder);

		Checkbox roundRobinFixedOrderField = new Checkbox();
		layout.addFormItem(roundRobinFixedOrderField,
		        labelWithHelp("Competition.fixedRoundRobinOrder", "Competition.fixedRoundRobinOrderExplanation"));
		binder.forField(roundRobinFixedOrderField)
		        .bind(Competition::isFixedOrder, Competition::setFixedOrder);

		Checkbox genderOrderField = new Checkbox();
		layout.addFormItem(genderOrderField,
		        labelWithHelp("Competition.genderOrder", "Competition.genderOrderExplanation"));
		binder.forField(genderOrderField)
		        .bind(Competition::isGenderOrder, Competition::setGenderOrder);

		Checkbox sinclairMeetField = new Checkbox();
		layout.addFormItem(sinclairMeetField,
		        labelWithHelp("Competition.SinclairMeet", "Competition.SinclairMeetExplanation"));
		binder.forField(sinclairMeetField)
		        .bind(Competition::isSinclair, Competition::setSinclair);

		Checkbox customScoreField = new Checkbox();
		layout.addFormItem(customScoreField,
		        labelWithHelp("Competition.customScore", "Competition.customScoreExplanation"));
		binder.forField(customScoreField)
		        .bind(Competition::isCustomScore, Competition::setCustomScore);

		return layout;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void bindField(HasValue field, String property, Class<?> propertyType, CrudFormConfiguration c) {
		binder.forField(field);
		super.bindField(field, property, propertyType, c);
	}

}