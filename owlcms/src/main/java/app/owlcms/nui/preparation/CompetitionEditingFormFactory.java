/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.IntegerRangeValidator;

import app.owlcms.components.fields.LocalizedIntegerField;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.athleteSort.RankingConfig;
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
	
	// Map of ranking checkboxes to update when mustCompute changes
	private java.util.Map<Ranking, Checkbox> rankingCheckboxes = new java.util.HashMap<>();
	
	// Reference to sinclairYear radio buttons for enabling/disabling
	private RadioButtonGroup<Integer> sinclairYear;

	CompetitionEditingFormFactory(Class<Competition> domainType, CompetitionContent origin) {
		super(domainType);
		this.origin = origin;
	}

	@Override
	public Competition add(Competition c) {
		// Save current RankingConfig state to competition before persisting
		c.saveRankingConfig();
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
		        deleteButtonClickListener, true, buttons);
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

		// Initialize RankingConfig from persisted competition settings
		comp.initializeRankingConfig();

		FormLayout competitionLayout = competitionForm();
		FormLayout federationLayout = federationForm();
		FormLayout teamsLayout = teamsForm();
		FormLayout generalRulesLayout = generalRulesForm();
		FormLayout nonMastersRulesLayout = nonMastersRulesForm();
		FormLayout mastersRulesLayout = mastersRulesForm();
		FormLayout breakDurationLayout = breakDurationForm();
		FormLayout specialLayout = specialRulesForm();
		FormLayout teamPointsLayout = teamPointsForm();
		FormLayout pointScoresForm = pointScoresForm();

		Component footer = this.buildFooter(operation, comp, cancelButtonClickListener,
		        c -> {
			        this.update(comp);
		        }, deleteButtonClickListener, false);

		TabSheet ts = new TabSheet();
		ts.add(Translator.translate("Competition.InformationTab"),
		        new VerticalLayout(
		                competitionLayout, separator(),
		                federationLayout));
		ts.add(Translator.translate("Competition.RulesTab"),
		        new VerticalLayout(
		                generalRulesLayout, separator(),
		                nonMastersRulesLayout, separator(),
		                mastersRulesLayout, separator(),
		                breakDurationLayout, separator(),
		                teamsLayout));
		ts.add(Translator.translate("Competition.specialRulesTitle"),
		        new VerticalLayout(
		                pointScoresForm, separator(),
		                specialLayout, separator(),
		                teamPointsForm()));

		VerticalLayout mainLayout = new VerticalLayout(
		        footer,
		        ts);
		mainLayout.setMargin(false);
		mainLayout.setPadding(false);

		this.binder.readBean(comp);
		return mainLayout;
	}

	@Override
	public Button buildOperationButton(CrudOperation operation, Competition domainObject,
	        ComponentEventListener<ClickEvent<Button>> gridCallBackAction) {
		return super.buildOperationButton(operation, domainObject, gridCallBackAction);
	}

	// @Override
	// public TextField defineOperationTrigger(CrudOperation operation, Competition domainObject,
	// ComponentEventListener<ClickEvent<Button>> action) {
	// return super.defineOperationTrigger(operation, domainObject, action);
	// }

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
		// Save current RankingConfig state to competition before persisting
		competition.saveRankingConfig();
		Competition saved = CompetitionRepository.save(competition);
		return saved;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void bindField(HasValue field, String property, Class<?> propertyType, CrudFormConfiguration c) {
		this.binder.forField(field);
		super.bindField(field, property, propertyType, c);
	}

	private FormLayout breakDurationForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Competition.breakParametersTitle");
		layout.add(title);
		layout.setColspan(title, 2);

		Checkbox toggle = new Checkbox();
		this.binder.forField(toggle).bind(Competition::isAutomaticCJBreak, Competition::setAutomaticCJBreak);
		layout.addFormItem(toggle, Translator.translate("Competition.automaticCJBreakQ"));

		Paragraph explain = new Paragraph(Translator.translate("Competition.breakParametersLonger"));
		layout.add(explain);
		explain.getStyle().set("margin-top", "2ex");
		explain.getStyle().set("margin-bottom", "0ex");
		layout.setColspan(explain, 2);

		LocalizedIntegerField ifLongerThreshold = new LocalizedIntegerField();
		layout.addFormItem(ifLongerThreshold, Translator.translate("Competition.longerBreakThreshold"));
		this.binder.forField(ifLongerThreshold)
		        .bind(Competition::getLongerBreakMax, Competition::setLongerBreakMax);

		LocalizedIntegerField ifLongerDuration = new LocalizedIntegerField();
		layout.addFormItem(ifLongerDuration, Translator.translate("Competition.longerBreakDuration"));
		this.binder.forField(ifLongerDuration)
		        .bind(Competition::getLongerBreakDuration, Competition::setLongerBreakDuration);

		Paragraph explain1 = new Paragraph(Translator.translate("Competition.breakParametersShorter"));
		layout.add(explain1);
		explain1.getStyle().set("margin-top", "2ex");
		explain1.getStyle().set("margin-bottom", "0ex");
		layout.setColspan(explain1, 2);

		LocalizedIntegerField ifShorterThreshold = new LocalizedIntegerField();
		layout.addFormItem(ifShorterThreshold, Translator.translate("Competition.shorterBreakThreshold"));
		this.binder.forField(ifShorterThreshold)
		        .bind(Competition::getShorterBreakMin, Competition::setShorterBreakMin);

		LocalizedIntegerField ifShorterDuration = new LocalizedIntegerField();
		layout.addFormItem(ifShorterDuration, Translator.translate("Competition.shorterBreakDuration"));
		this.binder.forField(ifShorterDuration)
		        .bind(Competition::getShorterBreakDuration, Competition::setShorterBreakDuration);

		return layout;
	}

	private FormLayout competitionForm() {
		FormLayout competitionLayout = createLayout();
		Component title = createTitle("Competition.informationTitle");
		competitionLayout.add(title);
		competitionLayout.setColspan(title, 2);

		TextField nameField = new TextField();
		nameField.setWidthFull();
		competitionLayout.addFormItem(nameField, Translator.translate("Competition.competitionName"));
		this.binder.forField(nameField)
		        .withNullRepresentation("")
		        .bind(Competition::getCompetitionName, Competition::setCompetitionName);

		DatePicker dateField = new DatePicker();
		DatePicker endDateField = new DatePicker();
		endDateField.setPlaceholder(Translator.translate("Competition.EndDate"));
		endDateField.setHelperText(Translator.translate("Competition.LeaveEmpty"));
		HorizontalLayout pickers = new HorizontalLayout(dateField, endDateField);
		competitionLayout.addFormItem(pickers, Translator.translate("Competition.competitionDate"));
		this.binder.forField(dateField)
		        .bind(Competition::getCompetitionDate, Competition::setCompetitionDate);
		this.binder.forField(endDateField)
		        .bind(Competition::getCompetitionEndDate, Competition::setCompetitionEndDate);

		TextField organizerField = new TextField();
		organizerField.setWidthFull();
		competitionLayout.addFormItem(organizerField, Translator.translate("Competition.competitionOrganizer"));
		this.binder.forField(organizerField)
		        .withNullRepresentation("")
		        .bind(Competition::getCompetitionOrganizer, Competition::setCompetitionOrganizer);

		TextField siteField = new TextField();
		siteField.setWidthFull();
		competitionLayout.addFormItem(siteField, Translator.translate("Competition.competitionSite"));
		this.binder.forField(siteField)
		        .withNullRepresentation("")
		        .bind(Competition::getCompetitionSite, Competition::setCompetitionSite);

		TextField cityField = new TextField();
		cityField.setWidthFull();
		competitionLayout.addFormItem(cityField, Translator.translate("Competition.competitionCity"));
		this.binder.forField(cityField)
		        .withNullRepresentation("")
		        .bind(Competition::getCompetitionCity, Competition::setCompetitionCity);

		return competitionLayout;
	}

	private FormLayout createLayout() {
		FormLayout layout = new FormLayout();
		// layout.setWidth("1024px");
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
		this.binder.forField(federationField)
		        .withNullRepresentation("")
		        .bind(Competition::getFederation, Competition::setFederation);

		TextField federationAddressField = new TextField();
		federationAddressField.setWidthFull();
		layout.addFormItem(federationAddressField, Translator.translate("Competition.federationAddress"));
		this.binder.forField(federationAddressField)
		        .withNullRepresentation("")
		        .bind(Competition::getFederationAddress, Competition::setFederationAddress);

		TextField federationEMailField = new TextField();
		federationEMailField.setWidthFull();
		layout.addFormItem(federationEMailField, Translator.translate("Competition.federationEMail"));
		this.binder.forField(federationEMailField)
		        .withNullRepresentation("")
		        .withValidator(new EmailValidator("Invalid Email Address"))
		        .bind(Competition::getFederationEMail, Competition::setFederationEMail);

		TextField federationWebSiteField = new TextField();
		federationWebSiteField.setWidthFull();
		layout.addFormItem(federationWebSiteField, Translator.translate("Competition.federationWebSite"));
		this.binder.forField(federationWebSiteField)
		        .withNullRepresentation("")
		        .bind(Competition::getFederationWebSite, Competition::setFederationWebSite);

		return layout;
	}

	private Span labelWithHelp(String string, String explanation) {
		Icon help = VaadinIcon.QUESTION_CIRCLE_O.create();
		help.getStyle().set("height", "1.2em");
		help.getStyle().set("vertical-align", "top");
		help.getStyle().set("font-weight", "bold");
		NativeLabel label = new NativeLabel(Translator.translate(string) + "\u00a0");
		Tooltip.forComponent(label).setText(Translator.translate(explanation));
		Span span = new Span();
		span.add(label, help);
		return span;
	}

	private FormLayout pointScoresForm() {
		FormLayout layout = createLayout();
		// (Moved) main point-scores header will be displayed above the global scoring selection

		// Main point-scores header (moved here so it labels the global scoring section)
		Component mainScoresTitle = createTitle("Competition.pointScoresTitle");
		layout.add(mainScoresTitle);
		layout.setColspan(mainScoresTitle, 2);

		ComboBox<Ranking> scoringCombo = new ComboBox<>();
		// Use unfiltered list - this dropdown SELECTS what to compute, not filtered by what's computed
		scoringCombo.setItems(RankingConfig.getAllScoringRankings());
		scoringCombo.setItemLabelGenerator(r -> Ranking.getScoringExplanation(r));
		scoringCombo.setWidth("50ch");
		scoringCombo.getElement().getStyle().set("--vaadin-combo-box-overlay-width", "50ch");
		layout.addFormItem(scoringCombo, Translator.translate("Competition.scoringSystemTitle"));
		this.binder.forField(scoringCombo).bind(Competition::getScoringSystem, Competition::setScoringSystem);
		
		// When global scoring system changes, update mustCompute and refresh checkboxes
		scoringCombo.addValueChangeListener(e -> {
			RankingConfig.updateMustCompute(e.getValue());
			refreshRankingCheckboxes();
		});

		// Visual separator before ranking selection
		Hr hrSeparator = separator();
		layout.add(hrSeparator);
		layout.setColspan(hrSeparator, 2);

		// Scoring systems section
		Component scoringTitle = createTitle("ScoringSystems");
		layout.add(scoringTitle);
		layout.setColspan(scoringTitle, 2);

		Paragraph scoringExplain = new Paragraph(Translator.translate("ScoringSystems.Explanation"));
		scoringExplain.getStyle().set("margin-top", "2ex");
		scoringExplain.getStyle().set("margin-bottom", "2ex");
		layout.add(scoringExplain);
		layout.setColspan(scoringExplain, 2);

		// Ranking system selection rows: one horizontal row of checkboxes per ranking family
		// Sinclair-based rankings
		HorizontalLayout sinclairRow = new HorizontalLayout();
		for (Ranking r : RankingConfig.getSinclairRankings()) {
			Checkbox cb = createRankingCheckbox(r);
			sinclairRow.add(cb);
		}
		layout.add(sinclairRow);
		layout.setColspan(sinclairRow, 2);

		// QPoints-based rankings
		HorizontalLayout qpointsRow = new HorizontalLayout();
		for (Ranking r : RankingConfig.getQPointsRankings()) {
			Checkbox cb = createRankingCheckbox(r);
			qpointsRow.add(cb);
		}
		layout.add(qpointsRow);
		layout.setColspan(qpointsRow, 2);

		// GAMX-based rankings
		HorizontalLayout gamxRow = new HorizontalLayout();
		for (Ranking r : RankingConfig.getGamxRankings()) {
			Checkbox cb = createRankingCheckbox(r);
			gamxRow.add(cb);
		}
		layout.add(gamxRow);
		layout.setColspan(gamxRow, 2);

		// Other global scoring systems (SMM, ROBI, etc.)
		HorizontalLayout otherRow = new HorizontalLayout();
		for (Ranking r : RankingConfig.getRobiRankings()) {
			Checkbox cb = createRankingCheckbox(r);
			otherRow.add(cb);
		}
		otherRow.getStyle().set("margin-bottom", "2ex");
		layout.add(otherRow);
		layout.setColspan(otherRow, 2);

		// Checkbox showScoressOnScoreboard = new Checkbox();
		// layout.addFormItem(showScoressOnScoreboard, Translator.translate("Competition.showScoresOnScoreboard"));
		// this.binder.forField(showScoressOnScoreboard)
		// .bind(Competition::isDisplayScores, Competition::setDisplayScores);
		//
		// Checkbox showScoreRanksOnScoreboard = new Checkbox();
		// layout.addFormItem(showScoreRanksOnScoreboard, Translator.translate("Competition.showScoreRanksOnScoreboard"));
		// this.binder.forField(showScoreRanksOnScoreboard)
		// .bind(Competition::isDisplayScoreRanks, Competition::setDisplayScoreRanks);

		RadioButtonGroup<Integer> sinclairYearLocal = new RadioButtonGroup<>();
		layout.addFormItem(sinclairYearLocal, Translator.translate("sinclair"));
		sinclairYearLocal.setItems(2020, 2024);
		this.binder.forField(sinclairYearLocal)
		        .bind(Competition::getSinclairYear, Competition::setSinclairYear);

		// Store reference for later access when checkboxes change
		this.sinclairYear = sinclairYearLocal;
		
		// Disable sinclairYear if neither Sinclair nor Category Sinclair is computed
		updateSinclairYearEnabled();
		
		// Add listeners to Sinclair checkboxes to update radio button state
		Checkbox sinclairCb = rankingCheckboxes.get(Ranking.BW_SINCLAIR);
		Checkbox catSinclairCb = rankingCheckboxes.get(Ranking.CAT_SINCLAIR);
		if (sinclairCb != null) {
			sinclairCb.addValueChangeListener(e -> updateSinclairYearEnabled());
		}
		if (catSinclairCb != null) {
			catSinclairCb.addValueChangeListener(e -> updateSinclairYearEnabled());
		}

		// Checkbox sinclairMeetField = new Checkbox();
		// layout.addFormItem(sinclairMeetField,
		// labelWithHelp("Competition.SinclairMeet", "Competition.SinclairMeetExplanation"));
		// this.binder.forField(sinclairMeetField)
		// .bind(Competition::isSinclair, Competition::setSinclair);

		return layout;
	}

	/**
	 * Create a checkbox for a ranking system.
	 * Stores reference in rankingCheckboxes map for later refresh.
	 */
	private Checkbox createRankingCheckbox(Ranking r) {
		Checkbox cb = new Checkbox(Ranking.getScoringTitle(r));
		cb.setValue(RankingConfig.shouldCompute(r));
		// Read-only if mustCompute (required by age group or global scoring)
		if (RankingConfig.isMustCompute(r)) {
			cb.setReadOnly(true);
			cb.getStyle().set("font-weight", "bold");
		}
		cb.addValueChangeListener(e -> {
			if (!cb.isReadOnly()) {
				RankingConfig.setUserEnabled(r, e.getValue());
			}
		});
		rankingCheckboxes.put(r, cb);
		return cb;
	}

	/**
	 * Refresh all ranking checkboxes to reflect current mustCompute state.
	 * Called when global scoring system changes.
	 */
	private void refreshRankingCheckboxes() {
		for (java.util.Map.Entry<Ranking, Checkbox> entry : rankingCheckboxes.entrySet()) {
			Ranking r = entry.getKey();
			Checkbox cb = entry.getValue();
			boolean mustCompute = RankingConfig.isMustCompute(r);
			cb.setReadOnly(mustCompute);
			if (mustCompute) {
				cb.getStyle().set("font-weight", "bold");
			} else {
				cb.getStyle().remove("font-weight");
			}
			cb.setValue(RankingConfig.shouldCompute(r));
		}
	}

	/**
	 * Update Sinclair year radio buttons enabled state.
	 * Disable if neither Sinclair nor Category Sinclair is being computed.
	 */
	private void updateSinclairYearEnabled() {
		if (sinclairYear == null) {
			return; // Not yet initialized
		}
		boolean sinclairEnabled = RankingConfig.shouldCompute(Ranking.BW_SINCLAIR)
				|| RankingConfig.shouldCompute(Ranking.CAT_SINCLAIR);
		sinclairYear.setEnabled(sinclairEnabled);
	}

	private FormLayout generalRulesForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Competition.rulesTitle");
		layout.add(title);
		layout.setColspan(title, 2);
		
		Checkbox enforce20kgRuleField = new Checkbox();
		layout.addFormItem(enforce20kgRuleField, Translator.translate("Competition.enforce20kgRule"));
		this.binder.forField(enforce20kgRuleField)
		        .bind(Competition::isEnforce20kgRule, Competition::setEnforce20kgRule);
		
		Checkbox snatchCJTotalField = new Checkbox();
		layout.addFormItem(snatchCJTotalField, Translator.translate("Competition.snatchCJTotalMedals"));
		this.binder.forField(snatchCJTotalField)
		        .bind(Competition::isSnatchCJTotalMedals, Competition::setSnatchCJTotalMedals);
		
		Checkbox deduct250gField = new Checkbox();
				layout.addFormItem(deduct250gField, Translator.translate("Competition.isDeduct250g"));
		this.binder.forField(deduct250gField)
		        .bind(Competition::getDeduct250g, Competition::setDeduct250g);

		Checkbox useBirthYearField = new Checkbox();
		layout.addFormItem(useBirthYearField, Translator.translate("Competition.useBirthYear"));
		this.binder.forField(useBirthYearField)
		        .bind(Competition::isUseBirthYear, Competition::setUseBirthYear);

		Checkbox announcerControlledJuryField = new Checkbox();
		layout.addFormItem(announcerControlledJuryField, Translator.translate("Competition.announcerControlledJury"));
		this.binder.forField(announcerControlledJuryField)
		        .bind(Competition::isAnnouncerControlledJuryDecision, Competition::setAnnouncerControlledJuryDecision);

		Checkbox customScoreField = new Checkbox();
		layout.addFormItem(customScoreField,
		        labelWithHelp("Competition.customScore", "Competition.customScoreExplanation"));
		this.binder.forField(customScoreField)
		        .bind(Competition::isCustomScore, Competition::setCustomScore);

		return layout;
	}
	
	private FormLayout nonMastersRulesForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Competition.nonMastersRulesTitle");
		layout.add(title);
		layout.setColspan(title, 2);
		
		Checkbox byAgeGroupField = new Checkbox();
		layout.addFormItem(byAgeGroupField, Translator.translate("Competition.startNumbersByAgeGroup"));
		this.binder.forField(byAgeGroupField)
		        .bind(Competition::isDisplayByAgeGroup, Competition::setDisplayByAgeGroup);

		return layout;
	}
	
	private FormLayout mastersRulesForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Competition.mastersRulesTitle");
		layout.add(title);
		layout.setColspan(title, 2);
		
		Checkbox mastersField = new Checkbox(Translator.translate("Competition.mastersCompetitionCheckbox"));
		layout.addFormItem(mastersField, Translator.translate("Competition.mastersCompetition"));
		mastersField.setHelperText(Translator.translate("Competition.mastersCompetitionHelper"));
		this.binder.forField(mastersField)
		        .bind(Competition::isMasters, Competition::setMasters);

		Checkbox imwaField = new Checkbox(Translator.translate("Competition.IMWARules"));
		layout.addFormItem(imwaField, Translator.translate("Competition.IMWA"));
		this.binder.forField(imwaField)
		        .bind(Competition::isImwa, Competition::setImwa);

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

	private FormLayout teamPointsForm() {
		String message = Translator.translate("Competition.teamSizeInvalid");

		FormLayout layout = createLayout();
		Component title = createTitle("Competition.teamPointsTitle");
		layout.add(title);
		layout.setColspan(title, 2);

		TextField mensTeamSizeField = new TextField();
		layout.addFormItem(mensTeamSizeField,
		        labelWithHelp("Competition.mensTeamSize", "Competition.teamSizeExplanation"));
		this.binder.forField(mensTeamSizeField)
		        .withNullRepresentation("")
		        .withConverter(new StringToIntegerConverter(message))
		        .withValidator(new IntegerRangeValidator(message, 0, 99))
		        .bind(Competition::getMensBestN, Competition::setMensBestN);

		TextField womensTeamSizeField = new TextField();
		layout.addFormItem(womensTeamSizeField,
		        labelWithHelp("Competition.womensTeamSize", "Competition.teamSizeExplanation"));
		this.binder.forField(womensTeamSizeField)
		        .withNullRepresentation("")
		        .withConverter(new StringToIntegerConverter(message))
		        .withValidator(new IntegerRangeValidator(message, 0, 99))
		        .bind(Competition::getWomensBestN, Competition::setWomensBestN);

		IntegerField teamPoints1stField = new IntegerField();
		layout.addFormItem(teamPoints1stField, Translator.translate("Competition.teamPoints1st"));
		this.binder.forField(teamPoints1stField)
		        .bind(Competition::getTeamPoints1st, Competition::setTeamPoints1st);

		IntegerField teamPoints2ndField = new IntegerField();
		layout.addFormItem(teamPoints2ndField, Translator.translate("Competition.teamPoints2nd"));
		this.binder.forField(teamPoints2ndField)
		        .bind(Competition::getTeamPoints2nd, Competition::setTeamPoints2nd);

		return layout;
	}

	private void setBinder(Binder<Competition> buildBinder) {
		this.binder = buildBinder;
	}

	private FormLayout specialRulesForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Competition.specialRulesTitle");
		layout.add(title);
		layout.setColspan(title, 2);

		Checkbox roundRobinOrderField = new Checkbox();
		layout.addFormItem(roundRobinOrderField,
		        labelWithHelp("Competition.roundRobinOrder", "Competition.roundRobinOrderExplanation"));
		this.binder.forField(roundRobinOrderField)
		        .bind(Competition::isRoundRobinOrder, Competition::setRoundRobinOrder);

		Checkbox roundRobinFixedOrderField = new Checkbox();
		layout.addFormItem(roundRobinFixedOrderField,
		        labelWithHelp("Competition.fixedRoundRobinOrder", "Competition.fixedRoundRobinOrderExplanation"));
		this.binder.forField(roundRobinFixedOrderField)
		        .bind(Competition::isFixedOrder, Competition::setFixedOrder);

		Checkbox genderOrderField = new Checkbox();
		layout.addFormItem(genderOrderField,
		        labelWithHelp("Competition.genderOrder", "Competition.genderOrderExplanation"));
		this.binder.forField(genderOrderField)
		        .bind(Competition::isGenderOrder, Competition::setGenderOrder);

		IntegerField wakeUpDelayField = new IntegerField();
		layout.addFormItem(wakeUpDelayField, Translator.translate("Competition.decisionRequestDelayLabel"));
		this.binder.forField(wakeUpDelayField).bind(Competition::getRefereeWakeUpDelay,
		        Competition::setRefereeWakeUpDelay);

		return layout;
	}

	private FormLayout teamsForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Competition.teamRules");
		layout.add(title);
		layout.setColspan(title, 2);

		LocalizedIntegerField maxTeamSize = new LocalizedIntegerField();
		layout.addFormItem(maxTeamSize, Translator.translate("Competition.AthletesPerTeam"));
		this.binder.forField(maxTeamSize)
		        .bind(Competition::getMaxTeamSize, Competition::setMaxTeamSize);

		LocalizedIntegerField maxPerCategory = new LocalizedIntegerField();
		layout.addFormItem(maxPerCategory, Translator.translate("Competition.maxAthletesPerCategory"));
		this.binder.forField(maxPerCategory)
		        .bind(Competition::getMaxPerCategory, Competition::setMaxPerCategory);

		return layout;
	}

}