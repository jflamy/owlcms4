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
import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.Location;

import app.owlcms.components.fields.LocalizedIntegerField;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.config.TcrrCompliance;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.CustomFormFactory;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class CompetitionEditingFormFactory
        extends OwlcmsCrudFormFactory<Competition>
        implements CustomFormFactory<Competition> {

	// Zero-based tab indices. The URL uses these values directly, so "/0" is equivalent to "/".
	public static final int INFORMATION_TAB = 0;
	public static final int RULES_TAB = 1;
	/** Index of the tab holding the championships (medals and teams) editor. */
	public static final int MEDALS_AND_TEAMS_TAB = 2;
	public static final int SPECIAL_RULES_TAB = 3;

	private static final String COMPETITION_ROUTE = "preparation/competition";

	String browserZoneId;
	@SuppressWarnings("unused")
	private Logger logger = (Logger) LoggerFactory.getLogger(CompetitionEditingFormFactory.class);
	@SuppressWarnings("unused")
	private CompetitionContent origin;
	
	private RadioButtonGroup<StartNumberOrder> startNumberOrderField;
	private TabSheet tabSheet;
	private boolean jurySizeChanged;

	private enum StartNumberOrder {
		BY_BODY_WEIGHT_CATEGORY,
		BY_BODY_WEIGHT_CATEGORY_THEN_AGE_GROUP,
		BY_ASCENDING_AGE_GROUP_THEN_BODY_WEIGHT_CATEGORY,
		BY_DESCENDING_AGE_GROUP_THEN_BODY_WEIGHT_CATEGORY
	}

	CompetitionEditingFormFactory(Class<Competition> domainType, CompetitionContent origin) {
		super(domainType);
		this.origin = origin;
	}

	@Override
	public Competition add(Competition c) {
		// Save current RankingConfig state to competition before persisting
		c.saveRankingConfig();
		applyStartNumberOrder(c);
		CompetitionRepository.save(c);
		saveStartNumberOrderConfiguration();
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
		FormLayout generalRulesLayout = generalRulesForm();
		FormLayout juryRulesLayout = juryRulesForm();
		FormLayout mastersRulesLayout = mastersRulesForm();
		FormLayout breakDurationLayout = breakDurationForm();
		FormLayout startNumberLayout = startNumberForm(comp);
		FormLayout liftingOrderLayout = liftingOrderForm();
		FormLayout otherRulesLayout = otherRulesForm();
		FormLayout pointScoresForm = pointScoresForm();
		EditChampionshipsPanel championshipsPanel = new EditChampionshipsPanel();
		boolean tcrrCompliance = TcrrCompliance.isEnabled();
		mastersRulesLayout.setVisible(!tcrrCompliance);
		liftingOrderLayout.setVisible(!tcrrCompliance);

		Component footer = this.buildFooter(operation, comp, cancelButtonClickListener,
		        c -> {
			        this.update(comp);
		        }, deleteButtonClickListener, false);

		TabSheet ts = new TabSheet();
		this.tabSheet = ts;
		ts.add(Translator.translate("Competition.InformationTab"),
		        new VerticalLayout(
		                competitionLayout, separator(),
		                federationLayout));
		ts.add(Translator.translate("Competition.RulesTab"),
		        new VerticalLayout(
		                generalRulesLayout, separator(),
		                juryRulesLayout, separator(),
		                mastersRulesLayout, separator(),
		                breakDurationLayout));
		ts.add(Translator.translate("Competition.MedalsAndTeamsTab"),
		        new VerticalLayout(
		                championshipsPanel, separator(),
		                pointScoresForm));
		ts.add(Translator.translate("Competition.scoreboardAndLiftingOrder"),
		        new VerticalLayout(
		                startNumberLayout, separator(),
		                liftingOrderLayout, separator(),
		                otherRulesLayout));

		// Listen for tab changes to preserve the selection in the URL.
		ts.addSelectedChangeListener(event -> updateTabLocation());

		// Keep the action buttons visible below the scrollable tab content.
		ts.setSizeFull();
		ts.setMinHeight("0");

		VerticalLayout mainLayout = new VerticalLayout(
		        ts,
		        footer);
		mainLayout.setMargin(false);
		mainLayout.setPadding(false);
		mainLayout.setSizeFull();
		mainLayout.setFlexGrow(1.0, ts);
		mainLayout.setFlexShrink(0, footer);

		this.binder.readBean(comp);
		return mainLayout;
	}

	@Override
	public Button buildOperationButton(CrudOperation operation, Competition domainObject,
	        ComponentEventListener<ClickEvent<Button>> gridCallBackAction) {
		if ((operation == CrudOperation.UPDATE || operation == CrudOperation.ADD) && gridCallBackAction != null) {
			Button button = doBuildButton(operation);
			button.addClickListener(event -> {
				performOperationAndCallback(operation, domainObject, gridCallBackAction, false);
			});
			return button;
		}
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
		// The championships panel saves this flag immediately on Competition.getCurrent(),
		// which may be a newer instance than the one being edited by this form.
		competition.setHideEmptyChampionships(Competition.getCurrent().isHideEmptyChampionships());
		// Save current RankingConfig state to competition before persisting
		competition.saveRankingConfig();
		applyStartNumberOrder(competition);
		Competition saved = CompetitionRepository.save(competition);
		saveStartNumberOrderConfiguration();
		if (this.jurySizeChanged) {
			for (FieldOfPlay fop : OwlcmsFactory.getFOPs()) {
				if (fop.getPlatform() != null && fop.getPlatform().getJurySize() == null) {
					// the platform follows the competition default: the jury box and the jury pages follow too
					fop.fopEventPost(new FOPEvent.JurySizeChange(this, null));
				}
			}
			this.jurySizeChanged = false;
		}
		// Capture the target tab now and defer the navigation to the very end of the
		// round-trip so it runs AFTER the CRUD grid callback, restoring the selected tab.
		UI ui = UI.getCurrent();
		String target = "/" + selectedTabLocation();
		ui.beforeClientResponse(ui, ctx -> ui.getPage().setLocation(target));
		return saved;
	}

	/**
	 * Select the given tab in the editing TabSheet (no-op if out of range).
	 *
	 * @param index zero-based tab index
	 */
	public void selectTab(int index) {
		if (this.tabSheet != null && index >= 0 && index < this.tabSheet.getTabCount()) {
			this.tabSheet.setSelectedIndex(index);
		}
	}

	private void updateTabLocation() {
		UI.getCurrent().getPage().getHistory().replaceState(null, new Location(selectedTabLocation()));
	}

	private String selectedTabLocation() {
		return COMPETITION_ROUTE + "/" + this.tabSheet.getSelectedIndex();
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
		        .withConverter(Converter.from(
		                displayedMaximum -> Result.ok(displayedMaximum == null ? null : displayedMaximum + 1),
		                storedMaximum -> storedMaximum == null ? null : storedMaximum - 1))
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
		        .withConverter(Converter.from(
		                displayedMinimum -> Result.ok(displayedMinimum == null ? null : displayedMinimum - 1),
		                storedMinimum -> storedMinimum == null ? null : storedMinimum + 1))
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

	private Component createDescription(String string) {
		Paragraph description = new Paragraph(Translator.translate(string));
		description.getStyle().set("margin-top", "0");
		description.getStyle().set("margin-bottom", "var(--lumo-space-s)");
		description.getStyle().set("color", "var(--lumo-secondary-text-color)");
		return description;
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
		        .withValidator(new EmailValidator(Translator.translate("InvalidEmailAddress")))
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
		help.getStyle().set("cursor", "help");
		help.getElement().setAttribute("aria-label", Translator.translate(explanation));
		NativeLabel label = new NativeLabel(Translator.translate(string) + "\u00a0");
		Tooltip.forComponent(help).setText(Translator.translate(explanation));
		Span span = new Span();
		span.add(label, help);
		return span;
	}

	private FormLayout pointScoresForm() {
		FormLayout layout = createLayout();

		Component scoringTitle = createTitle("ScoringSystems");
		layout.add(scoringTitle);
		layout.setColspan(scoringTitle, 2);

		// Scores are always available and the best-athlete rank is computed per championship;
		// there is no longer a per-competition selection of which scoring systems to compute.

		RadioButtonGroup<Integer> sinclairYearLocal = new RadioButtonGroup<>();
		layout.addFormItem(sinclairYearLocal, Translator.translate("sinclair"));
		sinclairYearLocal.setItems(2020, 2024, 2028);
		this.binder.forField(sinclairYearLocal)
		        .bind(Competition::getSinclairYear, Competition::setSinclairYear);

		return layout;
	}

	private FormLayout generalRulesForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Competition.rulesTitle");
		layout.add(title);
		layout.setColspan(title, 2);
		
		Checkbox enforce20kgRuleField = new Checkbox();
		layout.addFormItem(enforce20kgRuleField, Translator.translate("Competition.enforce20kgRule"));
		if (TcrrCompliance.isEnabled()) {
			enforce20kgRuleField.setValue(true);
			enforce20kgRuleField.setEnabled(false);
		} else {
			this.binder.forField(enforce20kgRuleField)
			        .bind(Competition::isEnforce20kgRule, Competition::setEnforce20kgRule);
		}
		
		Checkbox deduct250gField = new Checkbox();
				layout.addFormItem(deduct250gField, Translator.translate("Competition.isDeduct250g"));
		this.binder.forField(deduct250gField)
		        .bind(Competition::getDeduct250g, Competition::setDeduct250g);

		Checkbox useBirthYearField = new Checkbox();
		layout.addFormItem(useBirthYearField, Translator.translate("Competition.useBirthYear"));
		this.binder.forField(useBirthYearField)
		        .bind(Competition::isUseBirthYear, Competition::setUseBirthYear);

		Checkbox customScoreField = new Checkbox();
		layout.addFormItem(customScoreField,
		        labelWithHelp("Competition.customScore", "Competition.customScoreExplanation"));
		this.binder.forField(customScoreField)
		        .bind(Competition::isCustomScore, Competition::setCustomScore);

		return layout;
	}
	
	private FormLayout juryRulesForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Jury");
		layout.add(title);
		layout.setColspan(title, 2);

		ComboBox<Integer> jurySizeField = new ComboBox<>();
		if (TcrrCompliance.isEnabled()) {
			jurySizeField.setItems(TcrrCompliance.JURY_SIZE);
			jurySizeField.setValue(TcrrCompliance.JURY_SIZE);
			jurySizeField.setEnabled(false);
		} else {
			jurySizeField.setItems(0, 3, 4, 5);
			this.binder.forField(jurySizeField)
			        .bind(Competition::getJurySize, Competition::setJurySize);
		}
		layout.addFormItem(jurySizeField,
		        Translator.translate("Competition.jurySize"));
		jurySizeField.addValueChangeListener(e -> {
			if (e.isFromClient()) {
				this.jurySizeChanged = true;
			}
		});

		Checkbox videoPlaybackTechnologyField = new Checkbox();
		layout.addFormItem(videoPlaybackTechnologyField, Translator.translate("Competition.videoPlaybackTechnology"));
		this.binder.forField(videoPlaybackTechnologyField)
		        .bind(Competition::isVideoPlaybackTechnology, Competition::setVideoPlaybackTechnology);

		return layout;
	}

	private FormLayout mastersRulesForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Competition.mastersRulesTitle");
		layout.add(title);
		layout.setColspan(title, 2);
		
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

	private void setBinder(Binder<Competition> buildBinder) {
		this.binder = buildBinder;
	}

	private FormLayout startNumberForm(Competition competition) {
		FormLayout layout = createLayout();

		Component startNumberTitle = createTitle("StartNumber");
		layout.add(startNumberTitle);
		layout.setColspan(startNumberTitle, 2);

		Component startNumberDescription = createDescription("Competition.startNumberOrder.Description");
		layout.add(startNumberDescription);
		layout.setColspan(startNumberDescription, 2);

		this.startNumberOrderField = new RadioButtonGroup<>();
		this.startNumberOrderField.setItems(StartNumberOrder.values());
		this.startNumberOrderField.setItemLabelGenerator(this::startNumberOrderLabel);
		this.startNumberOrderField.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
		this.startNumberOrderField.setValue(currentStartNumberOrder(competition));
		layout.add(this.startNumberOrderField);
		layout.setColspan(this.startNumberOrderField, 2);

		return layout;
	}

	private void applyStartNumberOrder(Competition competition) {
		StartNumberOrder selectedOrder = this.startNumberOrderField.getValue();
		competition.setMasters(selectedOrder == StartNumberOrder.BY_DESCENDING_AGE_GROUP_THEN_BODY_WEIGHT_CATEGORY);
		competition.setDisplayByAgeGroup(selectedOrder == StartNumberOrder.BY_ASCENDING_AGE_GROUP_THEN_BODY_WEIGHT_CATEGORY);
	}

	private StartNumberOrder currentStartNumberOrder(Competition competition) {
		if (competition.isMasters()) {
			return StartNumberOrder.BY_DESCENDING_AGE_GROUP_THEN_BODY_WEIGHT_CATEGORY;
		}
		if (competition.isDisplayByAgeGroup()) {
			return StartNumberOrder.BY_ASCENDING_AGE_GROUP_THEN_BODY_WEIGHT_CATEGORY;
		}
		if (Config.getCurrent().getFeatureSwitchValue(FeatureSwitch.BW_CLASS_THEN_AGE_GROUP)) {
			return StartNumberOrder.BY_BODY_WEIGHT_CATEGORY_THEN_AGE_GROUP;
		}
		return StartNumberOrder.BY_BODY_WEIGHT_CATEGORY;
	}

	private void saveStartNumberOrderConfiguration() {
		Config config = Config.getCurrent();
		config.setFeatureSwitchValue(FeatureSwitch.BW_CLASS_THEN_AGE_GROUP,
		        this.startNumberOrderField.getValue() == StartNumberOrder.BY_BODY_WEIGHT_CATEGORY_THEN_AGE_GROUP);
		Config.setCurrent(config);
	}

	private String startNumberOrderLabel(StartNumberOrder startNumberOrder) {
		return Translator.translate("Competition.startNumberOrder." + startNumberOrder.name());
	}

	private FormLayout liftingOrderForm() {
		FormLayout layout = createLayout();

		Component liftingOrderTitle = createTitle("Scoreboard.LiftingOrder");
		layout.add(liftingOrderTitle);
		layout.setColspan(liftingOrderTitle, 2);

		Checkbox genderOrderField = new Checkbox();
		layout.addFormItem(genderOrderField,
		        labelWithHelp("Competition.genderOrder", "Competition.genderOrderExplanation"));
		this.binder.forField(genderOrderField)
		        .bind(Competition::isGenderOrder, Competition::setGenderOrder);

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

		return layout;
	}

	private FormLayout otherRulesForm() {
		FormLayout layout = createLayout();

		Component otherTitle = createTitle("Competition.other");
		layout.add(otherTitle);
		layout.setColspan(otherTitle, 2);

		IntegerField wakeUpDelayField = new IntegerField();
		layout.addFormItem(wakeUpDelayField, Translator.translate("Competition.decisionRequestDelayLabel"));
		this.binder.forField(wakeUpDelayField).bind(Competition::getRefereeWakeUpDelay,
		        Competition::setRefereeWakeUpDelay);

		return layout;
	}

}