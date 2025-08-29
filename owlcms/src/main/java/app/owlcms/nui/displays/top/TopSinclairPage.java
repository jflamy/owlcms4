
/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays.top;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.apputils.queryparameters.TopParametersReader;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.displays.top.TopSinclair;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.displays.scoreboards.AbstractResultsDisplayPage;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/topsinclair")

public class TopSinclairPage extends AbstractResultsDisplayPage implements TopParametersReader {
	/**
	 * Ignore the 'fop' (field of play) parameter for this view.
	 */
	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	Logger logger = (Logger) LoggerFactory.getLogger(TopSinclairPage.class);
	Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private AgeGroup ageGroup;
	private Category category;
	private String ageGroupPrefix;
	private Championship ageDivision;
	private app.owlcms.data.athlete.Gender gender;
	private boolean displayLifts;
	private int nbAthletes = 10;

	public TopSinclairPage() {
		// intentionally empty. superclass will call init() as required.
	}

	// nbAthletes getter/setter
	public int getNbAthletes() {
		return nbAthletes;
	}

	public void setNbAthletes(int nbAthletes) {
		this.nbAthletes = nbAthletes;
		if (this.getBoard() instanceof TopSinclair) {
			((TopSinclair) this.getBoard()).setNbAthletes(nbAthletes);
			// Re-apply championship filtering after nbAthletes change
			refreshFilteredBoard(getChampionship());
		}
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#addDialogContent(com.vaadin.flow.component.Component,
	 *      com.vaadin.flow.component.orderedlayout.VerticalLayout)
	 */
	@Override
	public void addDialogContent(Component target, VerticalLayout vl) {
		DisplayOptions.addLightingEntries(vl, target, this);

		// Championship and AgeGroupPrefix selection (filtered)
		com.vaadin.flow.component.combobox.ComboBox<app.owlcms.data.agegroup.Championship> championshipComboBox = new com.vaadin.flow.component.combobox.ComboBox<>();
		com.vaadin.flow.component.combobox.ComboBox<String> ageGroupPrefixComboBox = new com.vaadin.flow.component.combobox.ComboBox<>();
		// Use the same logic as TopTeamsSinclairPage: show all championships, no null
		java.util.List<app.owlcms.data.agegroup.Championship> championships = app.owlcms.data.agegroup.Championship.findAll();
		championshipComboBox.setItems(championships);
		championshipComboBox.setItemLabelGenerator(c -> c.getName());
		championshipComboBox.setPlaceholder(app.owlcms.i18n.Translator.translate("Championship"));
		championshipComboBox.setClearButtonVisible(true);
		championshipComboBox.addValueChangeListener(e -> {
			app.owlcms.data.agegroup.Championship championship = e.getValue();
			setChampionship(championship);
			String existingAgeGroupPrefix = getAgeGroupPrefix();
			java.util.List<String> activeAgeGroups = setAgeGroupPrefixItems(ageGroupPrefixComboBox, championship);
			// Add 'no filter' for age group
			java.util.List<String> ageGroupOptions = new java.util.ArrayList<>();
			ageGroupOptions.add(null);
			if (activeAgeGroups != null)
				ageGroupOptions.addAll(activeAgeGroups);
			ageGroupPrefixComboBox.setItems(ageGroupOptions);
			if (existingAgeGroupPrefix != null && activeAgeGroups != null && activeAgeGroups.contains(existingAgeGroupPrefix)) {
				ageGroupPrefixComboBox.setValue(existingAgeGroupPrefix);
			} else {
				ageGroupPrefixComboBox.setValue(null);
			}
			// Always refresh the board with filtered athletes
			refreshFilteredBoard(championship);
			updateURLLocations();
		});
		ageGroupPrefixComboBox.setPlaceholder(app.owlcms.i18n.Translator.translate("AgeGroup"));
		ageGroupPrefixComboBox.setClearButtonVisible(true);
		ageGroupPrefixComboBox.addValueChangeListener(e -> {
			setAgeGroupPrefix(e.getValue());
			updateURLLocations();
		});
		// Set up age group options for initial load
		java.util.List<String> validAgeGroups = setAgeGroupPrefixItems(ageGroupPrefixComboBox, getChampionship());
		java.util.List<String> ageGroupOptions = new java.util.ArrayList<>();
		ageGroupOptions.add(null);
		if (validAgeGroups != null)
			ageGroupOptions.addAll(validAgeGroups);
		ageGroupPrefixComboBox.setItems(ageGroupOptions);
		if (validAgeGroups != null && validAgeGroups.contains(getAgeGroupPrefix())) {
			ageGroupPrefixComboBox.setValue(getAgeGroupPrefix());
		} else {
			ageGroupPrefixComboBox.setValue(null);
		}
		if (championships.contains(getChampionship())) {
			championshipComboBox.setValue(getChampionship());
		} else {
			championshipComboBox.setValue(null);
		}
		vl.add(new com.vaadin.flow.component.html.NativeLabel(app.owlcms.i18n.Translator.translate("SelectAgeGroup")),
		        new com.vaadin.flow.component.orderedlayout.HorizontalLayout(championshipComboBox, ageGroupPrefixComboBox));

		// Gender selection ComboBox (M / F / MF only)
		com.vaadin.flow.component.combobox.ComboBox<app.owlcms.data.athlete.Gender> genderComboBox = new com.vaadin.flow.component.combobox.ComboBox<>();
		// Only include M, F, MF
		genderComboBox.setItems(app.owlcms.data.athlete.Gender.M, app.owlcms.data.athlete.Gender.F, app.owlcms.data.athlete.Gender.MF);
		genderComboBox.setClearButtonVisible(true);
		genderComboBox.setItemLabelGenerator(g -> {
			if (g == null)
				return "";
			switch (g) {
				case M:
					return app.owlcms.i18n.Translator.translate("Gender.Men");
				case F:
					return app.owlcms.i18n.Translator.translate("Gender.Women");
				case MF:
					return app.owlcms.i18n.Translator.translate("Gender.Mixed");
				default:
					return "";
			}
		});
		// Only set value if it is M, F, or MF, otherwise default to MF
		app.owlcms.data.athlete.Gender currentGender = getGender();
		if (currentGender == app.owlcms.data.athlete.Gender.M ||
		        currentGender == app.owlcms.data.athlete.Gender.F ||
		        currentGender == app.owlcms.data.athlete.Gender.MF) {
			genderComboBox.setValue(currentGender);
		} else {
			genderComboBox.setValue(app.owlcms.data.athlete.Gender.MF);
		}
		genderComboBox.addValueChangeListener(event -> {
			setGender(event.getValue());
			updateURLLocations();
		});
		vl.add(new com.vaadin.flow.component.html.NativeLabel(app.owlcms.i18n.Translator.translate("Scoreboard.SelectGenders")),
		        new com.vaadin.flow.component.orderedlayout.HorizontalLayout(genderComboBox));

		// Number of athletes field (at the bottom)
		com.vaadin.flow.component.textfield.NumberField nbAthletesField = new com.vaadin.flow.component.textfield.NumberField();
		nbAthletesField.setLabel(app.owlcms.i18n.Translator.translate("TopSinclair.NbAthletes"));
		nbAthletesField.setMin(1);
		nbAthletesField.setStep(1);
		nbAthletesField.setValue((double) getNbAthletes());
		nbAthletesField.addValueChangeListener(e -> {
			int value = e.getValue() != null ? e.getValue().intValue() : 10;
			setNbAthletes(value);
			updateURLLocations();
		});
		vl.add(nbAthletesField);

	}

	// Helper for age group prefix items (copied from TopTeamsSinclairPage)
	private java.util.List<String> setAgeGroupPrefixItems(com.vaadin.flow.component.combobox.ComboBox<String> ageGroupPrefixComboBox,
	        app.owlcms.data.agegroup.Championship ageDivision2) {
		java.util.List<String> activeAgeGroups = app.owlcms.data.agegroup.AgeGroupRepository.findActiveAndUsedAgeGroupNames(ageDivision2);
		ageGroupPrefixComboBox.setItems(activeAgeGroups);
		return activeAgeGroups;
	}

	/**
	 * Filters the given list of athletes to only those who participate in any age group associated with the given championship. If championship is null,
	 * returns the original list.
	 */
	private java.util.List<app.owlcms.data.athlete.Athlete> filterAthletesByChampionship(
	        java.util.List<app.owlcms.data.athlete.Athlete> athletes,
	        app.owlcms.data.agegroup.Championship championship) {
		if (championship == null)
			return athletes;
		java.util.List<String> champAgeGroups = app.owlcms.data.agegroup.AgeGroupRepository.findActiveAndUsedAgeGroupNames(championship);
		java.util.List<app.owlcms.data.athlete.Athlete> filtered = new java.util.ArrayList<>();
		for (app.owlcms.data.athlete.Athlete athlete : athletes) {
			java.util.List<app.owlcms.data.category.Participation> participations = athlete.getParticipations();
			if (participations != null) {
				for (app.owlcms.data.category.Participation p : participations) {
					app.owlcms.data.category.Category cat = p.getCategory();
					if (cat != null && cat.getAgeGroup() != null) {
						String agCode = cat.getAgeGroup().getCode();
						if (champAgeGroups.contains(agCode)) {
							filtered.add(athlete);
							break;
						}
					}
				}
			}
		}
		return filtered;
	}

	/**
	 * Refreshes the TopSinclair board with athletes filtered by the given championship.
	 */
	private void refreshFilteredBoard(app.owlcms.data.agegroup.Championship championship) {
		if (this.getBoard() instanceof app.owlcms.displays.top.TopSinclair topSinclairBoard) {
			java.util.List<app.owlcms.data.athlete.Athlete> allMen = app.owlcms.data.competition.Competition.getCurrent()
			        .getGlobalScoreRanking(app.owlcms.data.athlete.Gender.M);
			java.util.List<app.owlcms.data.athlete.Athlete> allWomen = app.owlcms.data.competition.Competition.getCurrent()
			        .getGlobalScoreRanking(app.owlcms.data.athlete.Gender.F);
			java.util.List<app.owlcms.data.athlete.Athlete> filteredMen = filterAthletesByChampionship(allMen, championship);
			java.util.List<app.owlcms.data.athlete.Athlete> filteredWomen = filterAthletesByChampionship(allWomen, championship);
			// Use the new method that works with filtered lists instead of overriding them
			topSinclairBoard.doUpdateWithFilteredLists(filteredMen, filteredWomen);
		}
	}

	@Override
	public final AgeGroup getAgeGroup() {
		return this.ageGroup;
	}

	@Override
	public String getAgeGroupPrefix() {
		return this.ageGroupPrefix;
	}

	@Override
	public final Category getCategory() {
		return this.category;
	}

	@Override
	public Championship getChampionship() {
		return this.ageDivision;
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("Scoreboard.TopScore",
		        Ranking.getScoringTitle(Competition.getCurrent().getScoringSystem()));
	}

	@Override
	public app.owlcms.data.athlete.Gender getGender() {
		return this.gender;
	}

	@Override
	public void setGender(app.owlcms.data.athlete.Gender gender) {
		this.gender = gender;
		((TopSinclair) this.getBoard()).setGender(gender);
		// Re-apply championship filtering after gender change
		refreshFilteredBoard(getChampionship());
	}

	@Override
	public final void setAgeGroup(AgeGroup ag) {
		this.ageGroup = ag;
		((TopSinclair) this.getBoard()).setAgeGroup(ag);
		// Re-apply championship filtering after age group change
		refreshFilteredBoard(getChampionship());
	}

	@Override
	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
		((TopSinclair) this.getBoard()).setAgeGroupPrefix(ageGroupPrefix);
		// Re-apply championship filtering after age group prefix change
		refreshFilteredBoard(getChampionship());
	}

	@Override
	public final void setCategory(Category cat) {
		this.category = cat;
		((TopSinclair) this.getBoard()).setCategory(cat);
		// Re-apply championship filtering after category change
		refreshFilteredBoard(getChampionship());
	}

	@Override
	public void setChampionship(Championship ageDivision) {
		this.ageDivision = ageDivision;
		((TopSinclair) this.getBoard()).setChampionship(ageDivision);
		// Trigger filtering when championship is set programmatically (e.g., from URL parameters)
		refreshFilteredBoard(ageDivision);
	}

	@Override
	public boolean isDisplayLifts() {
		return this.displayLifts;
	}

	@Override
	public void setDisplayLifts(boolean displayLifts) {
		this.displayLifts = displayLifts;
		((TopSinclair) this.getBoard()).setDisplayLifts(displayLifts);
		// Re-apply championship filtering after display lifts change
		refreshFilteredBoard(getChampionship());
	}

	@Override
	protected void init() {
		var board = new TopSinclair();
		board.setNbAthletes(this.nbAthletes);
		this.setBoard(board);
		this.addComponent(board);

		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied.
		var initialMap = Map.of(
		        SoundParameters.SILENT, "true",
		        SoundParameters.DOWNSILENT, "true",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "false",
		        DisplayParameters.RECORDS, "false",
		        DisplayParameters.VIDEO, "false",
		        DisplayParameters.PUBLIC, "false",
		        SoundParameters.SINGLEREF, "false",
		        DisplayParameters.ABBREVIATED, Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")));
		var additionalMap = Map.of(
		        SoundParameters.LIVE_LIGHTS, Boolean.toString(!Config.getCurrent().featureSwitch("noLiveLights")),
		        SoundParameters.SHOW_DECLARATIONS, "false",
		        SoundParameters.CENTER_NOTIFICATIONS, Boolean.toString(Config.getCurrent().featureSwitch("centerAnnouncerNotifications")),
		        SoundParameters.START_ORDER, "false");
		Map<String, String> fullMap = new TreeMap<>();
		fullMap.putAll(initialMap);
		fullMap.putAll(additionalMap);
		setDefaultParameters(QueryParameters.simple(fullMap));
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		((TopSinclair) this.getBoard()).setDisplayLifts(this.displayLifts);
		// Always refresh the board with the current championship filter on attach
		refreshFilteredBoard(getChampionship());
	}

	// ...existing code...

	private void updateURLLocations() {
		if (getLocation() == null) {
			// sometimes called from routines outside of normal event flow.
			return;
		}
		updateURLLocation(com.vaadin.flow.component.UI.getCurrent(), getLocation(), DisplayParameters.DARK,
		        !isDarkMode() ? Boolean.TRUE.toString() : null);

		// Only propagate non-null, non-empty age group
		String agPrefix = getAgeGroupPrefix();
		if (agPrefix != null && !agPrefix.isEmpty()) {
			updateURLLocation(com.vaadin.flow.component.UI.getCurrent(), getLocation(), "ag", agPrefix);
		} else {
			updateURLLocation(com.vaadin.flow.component.UI.getCurrent(), getLocation(), "ag", null);
		}

		// Only propagate non-null, non-empty championship (no empty 'ad' in URL)
		Championship champ = getChampionship();
		if (champ != null && champ.getName() != null && !champ.getName().isEmpty()) {
			updateURLLocation(com.vaadin.flow.component.UI.getCurrent(), getLocation(), "ad", champ.getName());
		} else {
			// Only remove 'ad' from URL if it was not set by the user
			updateURLLocation(com.vaadin.flow.component.UI.getCurrent(), getLocation(), "ad", null);
		}

		// Only propagate non-null gender
		app.owlcms.data.athlete.Gender gender = getGender();
		if (gender != null) {
			updateURLLocation(com.vaadin.flow.component.UI.getCurrent(), getLocation(), "gender", gender.name());
		} else {
			updateURLLocation(com.vaadin.flow.component.UI.getCurrent(), getLocation(), "gender", null);
		}
	}

}
