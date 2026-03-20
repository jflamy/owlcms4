/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.athleteSort.RankingConfig;
import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")
public class ChampionshipDetailsDialog extends Dialog {

	private static final String SUM_OF_POINTS = "Championship.sumOfPoints";
	private static final String SUM_OF_SCORES = "Championship.sumOfScores";

	public ChampionshipDetailsDialog(Championship championship, Runnable onSave) {
		setCloseOnEsc(true);
		setCloseOnOutsideClick(true);
		setHeaderTitle(championship.getName() + " — " + Translator.translate("Sessions.EditDetails"));
		setWidth("80em");

		VerticalLayout content = new VerticalLayout();
		content.setPadding(false);
		content.setSpacing(false);
		content.getStyle().set("--vaadin-form-item-label-width", "18em");

		List<String> teamMethodItems = List.of(SUM_OF_POINTS, SUM_OF_SCORES);

		// --- Championship Type ---
		FormLayout typeLayout = new FormLayout();
		typeLayout.setResponsiveSteps(new ResponsiveStep("0", 2));

		ComboBox<ChampionshipType> typeField = new ComboBox<>();
		typeField.setItems(Arrays.asList(ChampionshipType.values()));
		typeField.setItemLabelGenerator(type -> {
			String translated = Translator.translateOrElseNull("Division." + type.name());
			return translated != null ? translated + " (" + type.name() + ")" : type.name();
		});
		typeField.setValue(championship.getType());
		typeLayout.addFormItem(typeField, Translator.translate("Championship.Type"));

		content.add(typeLayout);

		// --- Medals section ---
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
			scoringSystemField.setEnabled(!e.getValue());
			if (e.getValue()) {
				scoringSystemField.clear();
			}
		});

		content.add(medalsLayout);

		// --- Separator before Scoring Systems ---
		content.add(createSeparator());

		// --- Scoring Systems section ---
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

		content.add(scoringLayout);

		// --- Separator before Team Points ---
		content.add(createSeparator());

		// --- Team Points section ---
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

		content.add(teamPointsLayout);

		// --- Separator before Men Women Teams ---
		content.add(createSeparator());

		// --- Men and Women Teams section ---
		FormLayout teamLayout = new FormLayout();
		teamLayout.setResponsiveSteps(new ResponsiveStep("0", 2));

		NativeLabel teamTitle = new NativeLabel(Translator.translate("Championship.MenWomenTeams"));
		teamTitle.getStyle().set("font-weight", "bold");
		teamLayout.add(teamTitle);
		teamLayout.setColspan(teamTitle, 2);

		// Team ranking method: sum of points vs sum of scores
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
			boolean isScores = SUM_OF_SCORES.equals(e.getValue());
			teamScoringSystemField.setEnabled(isScores);
			if (isScores && teamScoringSystemField.getValue() == null) {
				teamScoringSystemField.setValue(Ranking.GAMX);
			} else if (!isScores) {
				teamScoringSystemField.clear();
			}
		});

		IntegerField mensBestNField = new IntegerField();
		mensBestNField.setValue(championship.getMensBestN() != null ? championship.getMensBestN() : 0);
		teamLayout.addFormItem(mensBestNField, Translator.translate("Competition.mensTeamSize"));

		IntegerField womensBestNField = new IntegerField();
		womensBestNField.setValue(championship.getWomensBestN() != null ? championship.getWomensBestN() : 0);
		teamLayout.addFormItem(womensBestNField, Translator.translate("Competition.womensTeamSize"));

		IntegerField maxTeamSizeField = new IntegerField();
		maxTeamSizeField.setValue(championship.getMaxTeamSize());
		teamLayout.addFormItem(maxTeamSizeField, Translator.translate("Competition.AthletesPerTeam"));

		IntegerField maxPerCategoryField = new IntegerField();
		maxPerCategoryField.setValue(championship.getMaxPerCategory());
		teamLayout.addFormItem(maxPerCategoryField, Translator.translate("Competition.maxAthletesPerCategory"));

		content.add(teamLayout);

		// --- Separator before Mixed Team ---
		content.add(createSeparator());

		// --- Mixed Team section ---
		FormLayout mixedLayout = new FormLayout();
		mixedLayout.setResponsiveSteps(new ResponsiveStep("0", 2));

		NativeLabel mixedTitle = new NativeLabel(Translator.translate("Championship.MixedTeam"));
		mixedTitle.getStyle().set("font-weight", "bold");
		mixedLayout.add(mixedTitle);
		mixedLayout.setColspan(mixedTitle, 2);

		Checkbox explicitMixedField = new Checkbox();
		explicitMixedField.setValue(championship.isExplicitMixedTeamMembers());
		var explicitMixedItem = mixedLayout.addFormItem(explicitMixedField,
		        Translator.translate("Championship.explicitMixedTeamMembers"));
		mixedLayout.setColspan(explicitMixedItem, 2);

		// Mixed team ranking method: sum of points vs sum of scores
		RadioButtonGroup<String> mixedMethodField = new RadioButtonGroup<>();
		mixedMethodField.setItems(teamMethodItems);
		mixedMethodField.setItemLabelGenerator(Translator::translate);
		mixedMethodField.setWidthFull();
		String mixedMethodInitial = championship.getMixedTeamScoringSystem() != null ? SUM_OF_SCORES : SUM_OF_POINTS;
		mixedMethodField.setValue(mixedMethodInitial);
		mixedLayout.addFormItem(mixedMethodField, Translator.translate("Championship.teamRankingMethod"));

		ComboBox<Ranking> mixedTeamScoringSystemField = createRankingCombo();
		mixedTeamScoringSystemField.setValue(championship.getMixedTeamScoringSystem());
		mixedTeamScoringSystemField.setEnabled(SUM_OF_SCORES.equals(mixedMethodInitial));
		mixedLayout.addFormItem(mixedTeamScoringSystemField, Translator.translate("Championship.teamScoringSystem"));

		mixedMethodField.addValueChangeListener(e -> {
			boolean isScores = SUM_OF_SCORES.equals(e.getValue());
			mixedTeamScoringSystemField.setEnabled(isScores);
			if (isScores && mixedTeamScoringSystemField.getValue() == null) {
				mixedTeamScoringSystemField.setValue(Ranking.GAMX);
			} else if (!isScores) {
				mixedTeamScoringSystemField.clear();
			}
		});

		IntegerField mixedBestNField = new IntegerField();
		mixedBestNField.setValue(championship.getMixedBestN() != null ? championship.getMixedBestN() : 0);
		mixedLayout.addFormItem(mixedBestNField, Translator.translate("Championship.mixedTeamSize"));

		content.add(mixedLayout);

		// --- Footer buttons ---
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setWidthFull();
		buttons.setJustifyContentMode(JustifyContentMode.END);

		Button saveButton = new Button(Translator.translate("Update"), event -> {
			championship.setType(typeField.getValue());
			championship.setScoringSystem(scoringSystemField.getValue());
			championship.setBestAthleteScoringSystem(bestAthleteField.getValue());
			championship.setBestSnatchScoringSystem(bestSnatchField.getValue());
			championship.setBestCJScoringSystem(bestCJField.getValue());
			championship.setSnatchCJTotalMedals(snatchCJTotalField.getValue());
			// Men/Women team scoring: null means sum-of-points, non-null means sum-of-scores
			championship.setTeamScoringSystem(
					SUM_OF_SCORES.equals(teamMethodField.getValue()) ? teamScoringSystemField.getValue() : null);
			championship.setTeamPoints1st(teamPoints1stField.getValue());
			championship.setTeamPoints2nd(teamPoints2ndField.getValue());
			championship.setTeamPoints3rd(teamPoints3rdField.getValue());
			championship.setMensBestN(mensBestNField.getValue());
			championship.setWomensBestN(womensBestNField.getValue());
			championship.setExplicitMixedTeamMembers(explicitMixedField.getValue());
			// Mixed team scoring: null means sum-of-points, non-null means sum-of-scores
			championship.setMixedTeamScoringSystem(
					SUM_OF_SCORES.equals(mixedMethodField.getValue()) ? mixedTeamScoringSystemField.getValue() : null);
			championship.setMixedBestN(mixedBestNField.getValue());
			championship.setMaxTeamSize(maxTeamSizeField.getValue());
			championship.setMaxPerCategory(maxPerCategoryField.getValue());
			Championship.update(championship);
			if (onSave != null) {
				onSave.run();
			}
			close();
		});
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		Button cancelButton = new Button(Translator.translate("Close"), event -> close());

		buttons.add(cancelButton, saveButton);

		add(content);
		getFooter().add(buttons);
	}

	private Hr createSeparator() {
		Hr hr = new Hr();
		hr.getStyle().set("margin-top", "0.5em");
		hr.getStyle().set("margin-bottom", "1.0em");
		return hr;
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
}
