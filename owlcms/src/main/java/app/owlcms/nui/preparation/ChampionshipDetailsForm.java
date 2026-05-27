/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.List;
import java.util.function.Supplier;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.agegroup.DefaultChampionship;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.athleteSort.RankingConfig;
import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")
public class ChampionshipDetailsForm extends VerticalLayout {

	private static final String SUM_OF_POINTS = "Championship.sumOfPoints";
	private static final String SUM_OF_SCORES = "Championship.sumOfScores";
	private static final String MIXED_SELECTION_EXPLICIT = "mixedSelection.explicit";
	private static final String MIXED_SELECTION_OVERALL = "mixedSelection.overall";
	private static final String MIXED_SELECTION_PER_GENDER = "mixedSelection.perGender";

	private final Championship championship;
	private Supplier<Boolean> validateHandler = () -> true;
	private Runnable writeHandler = () -> {};

	public ChampionshipDetailsForm(Championship championship) {
		this.championship = championship;
		setPadding(false);
		setSpacing(false);
		getStyle().set("--vaadin-form-item-label-width", "18em");

		boolean templateMode = championship.isCompetitionTemplate();
		List<String> teamMethodItems = List.of(SUM_OF_POINTS, SUM_OF_SCORES);

		FormLayout typeLayout = new FormLayout();
		typeLayout.setResponsiveSteps(new ResponsiveStep("0", 2));

		TextField nameField = new TextField();
		nameField.setValue(templateMode ? Championship.COMPETITION_TEMPLATE_NAME : championship.getName());
		nameField.setRequiredIndicatorVisible(true);
		nameField.setEnabled(!templateMode);
		typeLayout.addFormItem(nameField, Translator.translate("Name"));

		ComboBox<ChampionshipType> typeField = new ComboBox<>();
		typeField.setItems(ChampionshipType.selectableValues());
		typeField.setItemLabelGenerator(type -> Translator.translate(type.labelKey()));
		typeField.setValue(championship.getType());
		typeField.setEnabled(!templateMode);
		typeField.setWidthFull();
		FormLayout.FormItem typeItem = typeLayout.addFormItem(typeField, Translator.translate("Championship.Type"));
		typeLayout.setColspan(typeItem, 2);

		Checkbox useDefaultsField = new Checkbox(Translator.translate("Championship.UseCompetitionDefaults"));
		useDefaultsField.setValue(championship.usesCompetitionDefaults());
		useDefaultsField.setEnabled(!templateMode);
		typeLayout.add(useDefaultsField);
		typeLayout.setColspan(useDefaultsField, 2);

		if (!templateMode) {
			add(typeLayout);
			add(createSeparator());
		}

		FormLayout medalsLayout = new FormLayout();
		medalsLayout.setResponsiveSteps(new ResponsiveStep("0", 2));

		NativeLabel medalsTitle = new NativeLabel(Translator.translate("Championship.Medals"));
		medalsTitle.getStyle().set("font-weight", "bold");
		medalsLayout.add(medalsTitle);
		medalsLayout.setColspan(medalsTitle, 2);

		Checkbox snatchCJTotalField = new Checkbox();
		snatchCJTotalField.setValue(championship.isSnatchCJTotalMedals());
		medalsLayout.addFormItem(snatchCJTotalField, Translator.translate("Competition.snatchCJTotalMedals"));

		ComboBox<Ranking> scoringSystemField = createRankingCombo();
		scoringSystemField.setValue(championship.getScoringSystem());
		scoringSystemField.setEnabled(!championship.isSnatchCJTotalMedals());
		medalsLayout.addFormItem(scoringSystemField, Translator.translate("Championship.totalMedalScoring"));

		snatchCJTotalField.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			scoringSystemField.setEnabled(!Boolean.TRUE.equals(useDefaultsField.getValue()) && !e.getValue());
			if (e.getValue()) {
				scoringSystemField.clear();
			}
		});

		add(medalsLayout);
		add(createSeparator());

		FormLayout scoringLayout = new FormLayout();
		scoringLayout.setResponsiveSteps(new ResponsiveStep("0", 2));

		NativeLabel scoringTitle = new NativeLabel(Translator.translate("Championship.scoringSystemTitle"));
		scoringTitle.getStyle().set("font-weight", "bold");
		scoringLayout.add(scoringTitle);
		scoringLayout.setColspan(scoringTitle, 2);

		ComboBox<Ranking> bestAthleteField = createRankingCombo();
		bestAthleteField.setValue(championship.getBestAthleteScoringSystem());
		scoringLayout.addFormItem(bestAthleteField, Translator.translate("Championship.bestAthleteScoringSystem"));

		ComboBox<Ranking> bestSnatchField = createRankingCombo();
		bestSnatchField.setValue(championship.getBestSnatchScoringSystem());
		scoringLayout.addFormItem(bestSnatchField, Translator.translate("Championship.bestSnatchScoringSystem"));

		ComboBox<Ranking> bestCJField = createRankingCombo();
		bestCJField.setValue(championship.getBestCJScoringSystem());
		scoringLayout.addFormItem(bestCJField, Translator.translate("Championship.bestCJScoringSystem"));

		add(scoringLayout);
		add(createSeparator());

		FormLayout teamPointsLayout = new FormLayout();
		teamPointsLayout.setResponsiveSteps(new ResponsiveStep("0", 2));

		NativeLabel teamPointsTitle = new NativeLabel(Translator.translate("Championship.TeamPoints"));
		teamPointsTitle.getStyle().set("font-weight", "bold");
		teamPointsLayout.add(teamPointsTitle);
		teamPointsLayout.setColspan(teamPointsTitle, 2);

		IntegerField teamPoints1stField = new IntegerField();
		teamPoints1stField.setValue(championship.getTeamPoints1st() != null ? championship.getTeamPoints1st() : 0);
		teamPointsLayout.addFormItem(teamPoints1stField, Translator.translate("Competition.teamPoints1st"));

		IntegerField teamPoints2ndField = new IntegerField();
		teamPoints2ndField.setValue(championship.getTeamPoints2nd() != null ? championship.getTeamPoints2nd() : 0);
		teamPointsLayout.addFormItem(teamPoints2ndField, Translator.translate("Competition.teamPoints2nd"));

		IntegerField teamPoints3rdField = new IntegerField();
		teamPoints3rdField.setValue(championship.getTeamPoints3rd() != null ? championship.getTeamPoints3rd() : 0);
		teamPointsLayout.addFormItem(teamPoints3rdField, Translator.translate("Competition.teamPoints3rd"));

		add(teamPointsLayout);
		add(createSeparator());

		FormLayout teamLayout = new FormLayout();
		teamLayout.setResponsiveSteps(new ResponsiveStep("0", 2));

		NativeLabel teamTitle = new NativeLabel(Translator.translate("Championship.MenWomenTeams"));
		teamTitle.getStyle().set("font-weight", "bold");
		teamLayout.add(teamTitle);
		teamLayout.setColspan(teamTitle, 2);

		RadioButtonGroup<String> teamMethodField = new RadioButtonGroup<>();
		teamMethodField.setItems(teamMethodItems);
		teamMethodField.setItemLabelGenerator(Translator::translate);
		teamMethodField.setWidthFull();
		String teamMethodInitial = championship.getTeamScoringSystem() != null ? SUM_OF_SCORES : SUM_OF_POINTS;
		teamMethodField.setValue(teamMethodInitial);
		teamLayout.addFormItem(teamMethodField, Translator.translate("Championship.teamRankingMethod"));

		ComboBox<Ranking> teamScoringSystemField = createRankingCombo();
		teamScoringSystemField.setValue(championship.getTeamScoringSystem());
		teamScoringSystemField.setEnabled(SUM_OF_SCORES.equals(teamMethodInitial));
		teamLayout.addFormItem(teamScoringSystemField, Translator.translate("Championship.teamScoringSystem"));

		teamMethodField.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			boolean isScores = SUM_OF_SCORES.equals(e.getValue());
			teamScoringSystemField.setEnabled(isScores);
			if (isScores && teamScoringSystemField.getValue() == null) {
				teamScoringSystemField.setValue(Ranking.GAMX);
			} else if (!isScores) {
				teamScoringSystemField.clear();
			}
		});

		IntegerField maxTeamSizeField = new IntegerField();
		maxTeamSizeField.setValue(championship.getMaxTeamSize());
		teamLayout.addFormItem(maxTeamSizeField, Translator.translate("Competition.AthletesPerTeam"));

		IntegerField maxPerCategoryField = new IntegerField();
		maxPerCategoryField.setValue(championship.getMaxPerCategory());
		teamLayout.addFormItem(maxPerCategoryField, Translator.translate("Competition.maxAthletesPerCategory"));

		IntegerField mensBestNField = new IntegerField();
		mensBestNField.setValue(zeroAsEmpty(championship.getMensBestN()));
		teamLayout.addFormItem(mensBestNField,
		        labelWithHelp("Championship.mixedMensBestN", "Championship.BestNHelp"));

		IntegerField womensBestNField = new IntegerField();
		womensBestNField.setValue(zeroAsEmpty(championship.getWomensBestN()));
		teamLayout.addFormItem(womensBestNField,
		        labelWithHelp("Championship.mixedWomensBestN", "Championship.BestNHelp"));

		add(teamLayout);
		add(createSeparator());

		FormLayout mixedLayout = new FormLayout();
		mixedLayout.setResponsiveSteps(new ResponsiveStep("0", 2));

		Checkbox mixedTeamEnabledField = new Checkbox();
		mixedTeamEnabledField.setValue(championship.isMixedTeamEnabled());
		NativeLabel mixedTitle = new NativeLabel(Translator.translate("Championship.MixedTeam"));
		mixedTitle.getStyle().set("font-weight", "bold");
		HorizontalLayout mixedHeader = new HorizontalLayout(mixedTeamEnabledField, mixedTitle);
		mixedHeader.setAlignItems(Alignment.CENTER);
		mixedHeader.setSpacing(true);
		mixedLayout.add(mixedHeader);
		mixedLayout.setColspan(mixedHeader, 2);

		IntegerField explicitTeamSizeField = new IntegerField();
		explicitTeamSizeField.setValue(championship.getExplicitTeamSize());

		RadioButtonGroup<String> mixedMethodField = new RadioButtonGroup<>();
		mixedMethodField.setItems(teamMethodItems);
		mixedMethodField.setItemLabelGenerator(Translator::translate);
		mixedMethodField.setWidthFull();
		String mixedMethodInitial = championship.getMixedTeamScoringSystem() != null ? SUM_OF_SCORES : SUM_OF_POINTS;
		mixedMethodField.setValue(mixedMethodInitial);
		mixedMethodField.setEnabled(championship.isMixedTeamEnabled());
		mixedLayout.addFormItem(mixedMethodField, Translator.translate("Championship.teamRankingMethod"));

		ComboBox<Ranking> mixedTeamScoringSystemField = createRankingCombo();
		mixedTeamScoringSystemField.setValue(championship.getMixedTeamScoringSystem());
		mixedTeamScoringSystemField.setEnabled(championship.isMixedTeamEnabled() && SUM_OF_SCORES.equals(mixedMethodInitial));
		mixedLayout.addFormItem(mixedTeamScoringSystemField, Translator.translate("Championship.teamScoringSystem"));

		RadioButtonGroup<String> mixedSelectionField = new RadioButtonGroup<>();
		mixedSelectionField.setItems(List.of(
		        MIXED_SELECTION_EXPLICIT,
		        MIXED_SELECTION_OVERALL,
		        MIXED_SELECTION_PER_GENDER));
		mixedSelectionField.setItemLabelGenerator(this::translateMixedSelectionMode);
		mixedSelectionField.setWidthFull();
		mixedSelectionField.setValue(determineMixedSelectionMode(championship));
		mixedSelectionField.setEnabled(championship.isMixedTeamEnabled());
		mixedLayout.add(mixedSelectionField);
		mixedLayout.setColspan(mixedSelectionField, 2);

		mixedMethodField.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			boolean isScores = SUM_OF_SCORES.equals(e.getValue());
			mixedTeamScoringSystemField.setEnabled(isScores);
			if (isScores && mixedTeamScoringSystemField.getValue() == null) {
				mixedTeamScoringSystemField.setValue(Ranking.GAMX);
			} else if (!isScores) {
				mixedTeamScoringSystemField.clear();
			}
		});

		var explicitTeamSizeItem = mixedLayout.addFormItem(explicitTeamSizeField,
		        Translator.translate("Championship.explicitTeamSize"));
		mixedLayout.setColspan(explicitTeamSizeItem, 2);

		IntegerField mixedBestNField = new IntegerField();
		mixedBestNField.setValue(zeroAsEmpty(championship.getMixedBestN()));
		var mixedBestNItem = mixedLayout.addFormItem(mixedBestNField,
		        labelWithHelp("Championship.mixedBestN", "Championship.BestNHelp"));
		mixedLayout.setColspan(mixedBestNItem, 2);

		IntegerField mixedMensBestNField = new IntegerField();
		mixedMensBestNField.setValue(zeroAsEmpty(championship.getMixedMensBestN()));
		var mixedMensBestNItem = mixedLayout.addFormItem(mixedMensBestNField,
		        labelWithHelp("Championship.mixedMensBestN", "Championship.BestNHelp"));

		IntegerField mixedWomensBestNField = new IntegerField();
		mixedWomensBestNField.setValue(zeroAsEmpty(championship.getMixedWomensBestN()));
		var mixedWomensBestNItem = mixedLayout.addFormItem(mixedWomensBestNField,
		        labelWithHelp("Championship.mixedWomensBestN", "Championship.BestNHelp"));

		Runnable refreshMixedSelectionFieldState = () -> {
			boolean mixedEnabled = Boolean.TRUE.equals(mixedTeamEnabledField.getValue());
			boolean notDefaults = !Boolean.TRUE.equals(useDefaultsField.getValue());
			boolean enableSelectedMode = mixedEnabled && notDefaults;
			String mixedSelectionMode = mixedSelectionField.getValue();
			boolean explicitSelection = MIXED_SELECTION_EXPLICIT.equals(mixedSelectionMode);
			boolean overallSelection = MIXED_SELECTION_OVERALL.equals(mixedSelectionMode);
			boolean perGenderSelection = MIXED_SELECTION_PER_GENDER.equals(mixedSelectionMode);

			mixedSelectionField.setVisible(mixedEnabled);
			mixedSelectionField.setEnabled(enableSelectedMode);

			explicitTeamSizeItem.setVisible(mixedEnabled && explicitSelection);
			explicitTeamSizeField.setEnabled(enableSelectedMode && explicitSelection);

			mixedBestNItem.setVisible(mixedEnabled && overallSelection);
			mixedBestNField.setEnabled(enableSelectedMode && overallSelection);

			mixedMensBestNItem.setVisible(mixedEnabled && perGenderSelection);
			mixedMensBestNField.setEnabled(enableSelectedMode && perGenderSelection);

			mixedWomensBestNItem.setVisible(mixedEnabled && perGenderSelection);
			mixedWomensBestNField.setEnabled(enableSelectedMode && perGenderSelection);
		};

		mixedSelectionField.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			refreshMixedSelectionFieldState.run();
		});

		Runnable refreshEffectiveDefaults = () -> {
			Championship effective = Boolean.TRUE.equals(useDefaultsField.getValue())
			        ? DefaultChampionship.getInstance()
			        : championship;
			boolean useDefaults = Boolean.TRUE.equals(useDefaultsField.getValue());

			snatchCJTotalField.setValue(effective.isSnatchCJTotalMedals());
			scoringSystemField.setValue(effective.getScoringSystem());
			bestAthleteField.setValue(effective.getBestAthleteScoringSystem());
			bestSnatchField.setValue(effective.getBestSnatchScoringSystem());
			bestCJField.setValue(effective.getBestCJScoringSystem());
			teamPoints1stField.setValue(effective.getTeamPoints1st() != null ? effective.getTeamPoints1st() : 0);
			teamPoints2ndField.setValue(effective.getTeamPoints2nd() != null ? effective.getTeamPoints2nd() : 0);
			teamPoints3rdField.setValue(effective.getTeamPoints3rd() != null ? effective.getTeamPoints3rd() : 0);
			maxTeamSizeField.setValue(effective.getMaxTeamSize());
			maxPerCategoryField.setValue(effective.getMaxPerCategory());
			mensBestNField.setValue(zeroAsEmpty(effective.getMensBestN()));
			womensBestNField.setValue(zeroAsEmpty(effective.getWomensBestN()));
			explicitTeamSizeField.setValue(effective.getExplicitTeamSize());
			mixedBestNField.setValue(zeroAsEmpty(effective.getMixedBestN()));
			mixedMensBestNField.setValue(zeroAsEmpty(effective.getMixedMensBestN()));
			mixedWomensBestNField.setValue(zeroAsEmpty(effective.getMixedWomensBestN()));
			mixedSelectionField.setValue(determineMixedSelectionMode(effective));

			snatchCJTotalField.setEnabled(!useDefaults);
			scoringSystemField.setEnabled(!useDefaults && !Boolean.TRUE.equals(snatchCJTotalField.getValue()));
			bestAthleteField.setEnabled(!useDefaults);
			bestSnatchField.setEnabled(!useDefaults);
			bestCJField.setEnabled(!useDefaults);
			teamPoints1stField.setEnabled(!useDefaults);
			teamPoints2ndField.setEnabled(!useDefaults);
			teamPoints3rdField.setEnabled(!useDefaults);
			teamMethodField.setEnabled(!useDefaults);
			teamScoringSystemField.setEnabled(!useDefaults && SUM_OF_SCORES.equals(teamMethodField.getValue()));
			maxTeamSizeField.setEnabled(!useDefaults);
			maxPerCategoryField.setEnabled(!useDefaults);
			mensBestNField.setEnabled(!useDefaults);
			womensBestNField.setEnabled(!useDefaults);
			boolean mixedEnabled = Boolean.TRUE.equals(mixedTeamEnabledField.getValue());
			mixedTeamEnabledField.setEnabled(!useDefaults);
			mixedMethodField.setEnabled(!useDefaults && mixedEnabled);
			mixedTeamScoringSystemField.setEnabled(!useDefaults && mixedEnabled && SUM_OF_SCORES.equals(mixedMethodField.getValue()));
			refreshMixedSelectionFieldState.run();
		};

		useDefaultsField.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			refreshEffectiveDefaults.run();
		});

		mixedTeamEnabledField.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			boolean enabled = Boolean.TRUE.equals(e.getValue());
			boolean notDefaults = !Boolean.TRUE.equals(useDefaultsField.getValue());
			mixedMethodField.setEnabled(enabled && notDefaults);
			mixedTeamScoringSystemField.setEnabled(enabled && notDefaults && SUM_OF_SCORES.equals(mixedMethodField.getValue()));
			refreshMixedSelectionFieldState.run();
		});

		refreshEffectiveDefaults.run();
		add(mixedLayout);

		this.validateHandler = () -> {
			String updatedName = nameField.getValue() != null ? nameField.getValue().trim() : "";
			String mixedSelectionMode = mixedSelectionField.getValue();
			boolean valid = true;
			if (updatedName.isBlank()) {
				nameField.setInvalid(true);
				nameField.setErrorMessage(Translator.translate("ThisFieldIsRequired"));
				valid = false;
			} else {
				nameField.setInvalid(false);
			}
			if (MIXED_SELECTION_EXPLICIT.equals(mixedSelectionMode)
			        && (explicitTeamSizeField.getValue() == null || explicitTeamSizeField.getValue() <= 0)) {
				explicitTeamSizeField.setInvalid(true);
				explicitTeamSizeField.setErrorMessage(Translator.translate("ThisFieldIsRequired"));
				valid = false;
			} else {
				explicitTeamSizeField.setInvalid(false);
			}
			if (MIXED_SELECTION_OVERALL.equals(mixedSelectionMode)
			        && (mixedBestNField.getValue() == null || mixedBestNField.getValue() <= 0)) {
				mixedBestNField.setInvalid(true);
				mixedBestNField.setErrorMessage(Translator.translate("ThisFieldIsRequired"));
				valid = false;
			} else {
				mixedBestNField.setInvalid(false);
			}
			return valid;
		};

		this.writeHandler = () -> {
			String updatedName = nameField.getValue() != null ? nameField.getValue().trim() : "";
			String mixedSelectionMode = mixedSelectionField.getValue();
			championship.setType(templateMode ? ChampionshipType.U : typeField.getValue());
			championship.setUseCompetitionDefaults(!templateMode && useDefaultsField.getValue());
			championship.setScoringSystem(scoringSystemField.getValue());
			championship.setBestAthleteScoringSystem(bestAthleteField.getValue());
			championship.setBestSnatchScoringSystem(bestSnatchField.getValue());
			championship.setBestCJScoringSystem(bestCJField.getValue());
			championship.setSnatchCJTotalMedals(snatchCJTotalField.getValue());
			championship.setTeamScoringSystem(
			        SUM_OF_SCORES.equals(teamMethodField.getValue()) ? teamScoringSystemField.getValue() : null);
			championship.setTeamPoints1st(teamPoints1stField.getValue());
			championship.setTeamPoints2nd(teamPoints2ndField.getValue());
			championship.setTeamPoints3rd(teamPoints3rdField.getValue());
			championship.setMensBestN(mensBestNField.getValue());
			championship.setWomensBestN(womensBestNField.getValue());
			championship.setExplicitMixedTeamMembers(MIXED_SELECTION_EXPLICIT.equals(mixedSelectionMode));
			championship.setMixedTeamEnabled(mixedTeamEnabledField.getValue());
			championship.setExplicitTeamSize(explicitTeamSizeField.getValue());
			championship.setMixedTeamScoringSystem(
			        SUM_OF_SCORES.equals(mixedMethodField.getValue()) ? mixedTeamScoringSystemField.getValue() : null);
			championship.setMixedBestN(MIXED_SELECTION_OVERALL.equals(mixedSelectionMode)
			        ? mixedBestNField.getValue() : null);
			championship.setMixedMensBestN(MIXED_SELECTION_PER_GENDER.equals(mixedSelectionMode)
			        ? mixedMensBestNField.getValue() : null);
			championship.setMixedWomensBestN(MIXED_SELECTION_PER_GENDER.equals(mixedSelectionMode)
			        ? mixedWomensBestNField.getValue() : null);
			championship.setMaxTeamSize(maxTeamSizeField.getValue());
			championship.setMaxPerCategory(maxPerCategoryField.getValue());
			if (!templateMode && !updatedName.equals(championship.getName())) {
				championship.rename(updatedName);
			}
		};
	}

	public boolean validateForm() {
		return this.validateHandler.get();
	}

	public boolean save() {
		if (!validateForm()) {
			return false;
		}
		this.writeHandler.run();
		Championship.update(this.championship);
		Championship.reset();
		return true;
	}

	private Hr createSeparator() {
		Hr hr = new Hr();
		hr.getStyle().set("margin-top", "0.5em");
		hr.getStyle().set("margin-bottom", "1.0em");
		return hr;
	}

	private Integer zeroAsEmpty(Integer value) {
		return value != null && value == 0 ? null : value;
	}

	private Span labelWithHelp(String labelKey, String helpKey) {
		Icon help = VaadinIcon.QUESTION_CIRCLE_O.create();
		help.getStyle().set("height", "1.2em");
		help.getStyle().set("vertical-align", "top");
		help.getStyle().set("font-weight", "bold");
		help.getStyle().set("cursor", "help");
		help.getElement().setAttribute("aria-label", Translator.translate(helpKey));
		Tooltip.forComponent(help).setText(Translator.translate(helpKey));

		NativeLabel label = new NativeLabel(Translator.translate(labelKey) + " ");
		Span span = new Span();
		span.add(label, help);
		return span;
	}

	private ComboBox<Ranking> createRankingCombo() {
		ComboBox<Ranking> combo = new ComboBox<>();
		combo.setItems(RankingConfig.getAllScoringRankings());
		combo.setItemLabelGenerator(r -> Ranking.getScoringExplanation(r));
		combo.setClearButtonVisible(true);
		combo.setWidth("20em");
		combo.getElement().getStyle().set("--vaadin-combo-box-overlay-width", "30em");
		return combo;
	}

	private String determineMixedSelectionMode(Championship championship) {
		if (championship != null && championship.isExplicitMixedTeamMembers()) {
			return MIXED_SELECTION_EXPLICIT;
		}
		return championship != null && championship.getMixedBestN() != null && championship.getMixedBestN() > 0
		        ? MIXED_SELECTION_OVERALL
		        : MIXED_SELECTION_PER_GENDER;
	}

	private String translateMixedSelectionMode(String mode) {
		switch (mode) {
			case MIXED_SELECTION_EXPLICIT:
				return Translator.translate("Championship.explicitMixedTeamMembers");
			case MIXED_SELECTION_OVERALL:
				return Translator.translate("Championship.mixedBestN");
			case MIXED_SELECTION_PER_GENDER:
				return Translator.translate("Championship.mixedMensBestN") + " + "
				        + Translator.translate("Championship.mixedWomensBestN");
			default:
				return mode;
		}
	}
}