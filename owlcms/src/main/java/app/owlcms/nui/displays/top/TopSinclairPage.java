
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
			refreshFilteredBoard(getChampionship(), getAgeGroup());
		}
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#addDialogContent(com.vaadin.flow.component.Component,
	 *      com.vaadin.flow.component.orderedlayout.VerticalLayout)
	 */
	@Override
	public void addDialogContent(Component target, VerticalLayout vl) {
		DisplayOptions.addLightingEntries(vl, target, this);

		// Championship and AgeGroup selection (filtered)
		com.vaadin.flow.component.combobox.ComboBox<app.owlcms.data.agegroup.Championship> championshipComboBox = new com.vaadin.flow.component.combobox.ComboBox<>();
		com.vaadin.flow.component.combobox.ComboBox<app.owlcms.data.agegroup.AgeGroup> ageGroupComboBox = new com.vaadin.flow.component.combobox.ComboBox<>();
		// Use the same logic as TopTeamsSinclairPage: show all championships, no null
		java.util.List<app.owlcms.data.agegroup.Championship> championships = app.owlcms.data.agegroup.Championship.findAllUsed(true);
		championshipComboBox.setItems(championships);
		championshipComboBox.setItemLabelGenerator(c -> c.getName());
		championshipComboBox.setPlaceholder(app.owlcms.i18n.Translator.translate("Championship"));
		championshipComboBox.setClearButtonVisible(true);
		// Reset timer when user starts editing
		championshipComboBox.addFocusListener(e -> restartDialogTimer());
		championshipComboBox.addValueChangeListener(e -> {
			app.owlcms.data.agegroup.Championship championship = e.getValue();
			setChampionship(championship);
			// Clear age group selection when championship changes
			setAgeGroup(null);
			// Populate age groups for the selected championship
			java.util.List<app.owlcms.data.agegroup.AgeGroup> championshipAgeGroups = getAgeGroupsForChampionship(championship);
			ageGroupComboBox.setItems(championshipAgeGroups);
			ageGroupComboBox.setValue(null); // No age group selected by default
			// Always refresh the board with filtered athletes
			refreshFilteredBoard(championship, null);
			updateURLLocations();
			// Restart timer after value change
			restartDialogTimer();
		});
		ageGroupComboBox.setPlaceholder(app.owlcms.i18n.Translator.translate("AgeGroup"));
		ageGroupComboBox.setItemLabelGenerator(ag -> ag != null ? ag.getName() : "");
		ageGroupComboBox.setClearButtonVisible(true);
		// Reset timer when user starts editing
		ageGroupComboBox.addFocusListener(e -> restartDialogTimer());
		ageGroupComboBox.addValueChangeListener(e -> {
			app.owlcms.data.agegroup.AgeGroup selectedAgeGroup = e.getValue();
			setAgeGroup(selectedAgeGroup);
			// Refresh with both championship and age group filters
			refreshFilteredBoard(getChampionship(), selectedAgeGroup);
			updateURLLocations();
			// Restart timer after value change
			restartDialogTimer();
		});
		// Set up age group options for initial load
		java.util.List<app.owlcms.data.agegroup.AgeGroup> initialAgeGroups = getAgeGroupsForChampionship(getChampionship());
		ageGroupComboBox.setItems(initialAgeGroups);
		if (getAgeGroup() != null && initialAgeGroups.contains(getAgeGroup())) {
			ageGroupComboBox.setValue(getAgeGroup());
		} else {
			ageGroupComboBox.setValue(null);
		}
		if (championships.contains(getChampionship())) {
			championshipComboBox.setValue(getChampionship());
		} else {
			championshipComboBox.setValue(null);
		}
		vl.add(new com.vaadin.flow.component.html.NativeLabel(app.owlcms.i18n.Translator.translate("SelectAgeGroup")),
		        new com.vaadin.flow.component.orderedlayout.HorizontalLayout(championshipComboBox, ageGroupComboBox));

		// Gender selection ComboBox (M / F only, null = no filtering)
		com.vaadin.flow.component.combobox.ComboBox<app.owlcms.data.athlete.Gender> genderComboBox = new com.vaadin.flow.component.combobox.ComboBox<>();
		// Only include M, F
		genderComboBox.setItems(app.owlcms.data.athlete.Gender.M, app.owlcms.data.athlete.Gender.F);
		genderComboBox.setClearButtonVisible(true);
		genderComboBox.setItemLabelGenerator(g -> {
			if (g == null)
				return app.owlcms.i18n.Translator.translate("Gender.All");
			switch (g) {
				case M:
					return app.owlcms.i18n.Translator.translate("Gender.Men");
				case F:
					return app.owlcms.i18n.Translator.translate("Gender.Women");
				default:
					return "";
			}
		});
		// Set value if M or F, otherwise clear (null = no filtering)
		app.owlcms.data.athlete.Gender currentGender = getGender();
		if (currentGender == app.owlcms.data.athlete.Gender.M ||
				currentGender == app.owlcms.data.athlete.Gender.F) {
			genderComboBox.setValue(currentGender);
		} else {
			genderComboBox.clear();
		}
		genderComboBox.addValueChangeListener(event -> {
			setGender(event.getValue()); // null means no filtering
			updateURLLocations();
			restartDialogTimer();
		});
		// Reset timer when user starts editing
		genderComboBox.addFocusListener(e -> restartDialogTimer());
		genderComboBox.addValueChangeListener(event -> {
			setGender(event.getValue());
			updateURLLocations();
			// Restart timer after value change
			restartDialogTimer();
		});
		vl.add(new com.vaadin.flow.component.html.NativeLabel(app.owlcms.i18n.Translator.translate("Scoreboard.SelectGenders")),
		        new com.vaadin.flow.component.orderedlayout.HorizontalLayout(genderComboBox));

		// Number of athletes field (at the bottom)
		com.vaadin.flow.component.textfield.NumberField nbAthletesField = new com.vaadin.flow.component.textfield.NumberField();
		nbAthletesField.setLabel(app.owlcms.i18n.Translator.translate("TopSinclair.NbAthletes"));
		nbAthletesField.setMin(1);
		nbAthletesField.setStep(1);
		nbAthletesField.setValue((double) getNbAthletes());
		// Reset timer when user starts editing
		nbAthletesField.addFocusListener(e -> restartDialogTimer());
	   // Ensure dialog closes on Escape
	   if (getDialog() != null) {
		   getDialog().setCloseOnEsc(true);
	   }
		nbAthletesField.addValueChangeListener(e -> {
			int value = e.getValue() != null ? e.getValue().intValue() : 10;
			setNbAthletes(value);
			updateURLLocations();
			// Restart timer after value change
			restartDialogTimer();
		});
		vl.add(nbAthletesField);

	}

	// Helper for age group prefix items (copied from TopTeamsSinclairPage)

	/**
	 * Gets the age groups associated with a championship.
	 */
	private java.util.List<app.owlcms.data.agegroup.AgeGroup> getAgeGroupsForChampionship(app.owlcms.data.agegroup.Championship championship) {
		if (championship == null) {
			return new java.util.ArrayList<>();
		}
		return app.owlcms.data.agegroup.AgeGroupRepository.findFiltered(null, null, championship, null, true, -1, -1);
	}

	/**
	 * Filters the given list of athletes to only those who participate in the specified championship and/or age group.
	 * If championship is null, returns the original list.
	 * If ageGroup is null but championship is specified, returns athletes from all age groups in the championship.
	 * If both are specified, returns athletes from the specific age group.
	 */
	private java.util.List<app.owlcms.data.athlete.Athlete> filterAthletesByChampionshipAndAgeGroup(
	        java.util.List<app.owlcms.data.athlete.Athlete> athletes,
	        app.owlcms.data.agegroup.Championship championship,
	        app.owlcms.data.agegroup.AgeGroup ageGroup) {
		if (championship == null && ageGroup == null)
			return athletes;

		logger.debug("Filtering {} athletes with championship={} (name={}), ageGroup={} (code={})",
		        athletes.size(),
		        championship != null ? championship.getName() : "null",
		        championship != null ? championship.getName() : "null",
		        ageGroup != null ? ageGroup.getCode() : "null",
		        ageGroup != null ? ageGroup.getCode() : "null");

		java.util.List<app.owlcms.data.athlete.Athlete> filtered = new java.util.ArrayList<>();

		for (app.owlcms.data.athlete.Athlete athlete : athletes) {
			java.util.List<app.owlcms.data.category.Participation> participations = athlete.getParticipations();
			if (participations != null) {
				for (app.owlcms.data.category.Participation p : participations) {
					app.owlcms.data.category.Category cat = p.getCategory();
					if (cat != null && cat.getAgeGroup() != null) {
						app.owlcms.data.agegroup.AgeGroup athleteAgeGroup = cat.getAgeGroup();

						// Check championship match
						String selectedChampionshipName = championship != null ? championship.getName() : null;
						String athleteChampionshipName = athleteAgeGroup.computeChampionshipName();
						boolean championshipMatch = championship == null ||
						        (selectedChampionshipName != null && selectedChampionshipName.equals(athleteChampionshipName));

						// Check age group match (if specified)
						boolean ageGroupMatch = ageGroup == null ||
						        (ageGroup.getCode() != null && ageGroup.getCode().equals(athleteAgeGroup.getCode()));

						// Debug logging
						if (logger.isDebugEnabled()) {
							logger.debug("Filtering athlete {}: selected championship={}, selected ageGroup={}, athlete championship={}, athlete ageGroup={}, championshipMatch={}, ageGroupMatch={}",
							        athlete.getFullName(),
							        championship != null ? championship.getName() : "null",
							        ageGroup != null ? ageGroup.getCode() : "null",
							        athleteChampionshipName,
							        athleteAgeGroup.getCode(),
							        championshipMatch,
							        ageGroupMatch);
						}

						if (championshipMatch && ageGroupMatch) {
							filtered.add(athlete);
							break;
						}
					}
				}
			}
		}
		logger.debug("Filtered {} athletes down to {} athletes", athletes.size(), filtered.size());
		return filtered;
	}

	/**
	 * Refreshes the TopSinclair board with athletes filtered by the given championship and age group.
	 */
	private void refreshFilteredBoard(app.owlcms.data.agegroup.Championship championship, app.owlcms.data.agegroup.AgeGroup ageGroup) {
		if (this.getBoard() instanceof app.owlcms.displays.top.TopSinclair topSinclairBoard) {
			// Reset filtered results flag if no filtering is applied
			if (championship == null && ageGroup == null) {
				topSinclairBoard.setUseFilteredResults(false);
			} else {
				// If any filtering is applied, use filtered results
				topSinclairBoard.setUseFilteredResults(true);
			}

			java.util.List<app.owlcms.data.athlete.Athlete> allMen = app.owlcms.data.competition.Competition.getCurrent()
			        .getGlobalScoreRanking(app.owlcms.data.athlete.Gender.M);
			java.util.List<app.owlcms.data.athlete.Athlete> allWomen = app.owlcms.data.competition.Competition.getCurrent()
			        .getGlobalScoreRanking(app.owlcms.data.athlete.Gender.F);
			java.util.List<app.owlcms.data.athlete.Athlete> filteredMen = filterAthletesByChampionshipAndAgeGroup(allMen, championship, ageGroup);
			java.util.List<app.owlcms.data.athlete.Athlete> filteredWomen = filterAthletesByChampionshipAndAgeGroup(allWomen, championship, ageGroup);
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
		refreshFilteredBoard(getChampionship(), getAgeGroup());
	}

	@Override
	public final void setAgeGroup(AgeGroup ag) {
		this.ageGroup = ag;
		((TopSinclair) this.getBoard()).setAgeGroup(ag);
		// Re-apply championship filtering after age group change
		refreshFilteredBoard(getChampionship(), ag);
	}

	@Override
	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
		((TopSinclair) this.getBoard()).setAgeGroupPrefix(ageGroupPrefix);
		// Re-apply championship filtering after age group prefix change
		refreshFilteredBoard(getChampionship(), getAgeGroup());
	}

	@Override
	public final void setCategory(Category cat) {
		this.category = cat;
		((TopSinclair) this.getBoard()).setCategory(cat);
		// Re-apply championship filtering after category change
		refreshFilteredBoard(getChampionship(), getAgeGroup());
	}

	@Override
	public void setChampionship(Championship ageDivision) {
		this.ageDivision = ageDivision;
		((TopSinclair) this.getBoard()).setChampionship(ageDivision);
		// Trigger filtering when championship is set programmatically (e.g., from URL parameters)
		refreshFilteredBoard(ageDivision, getAgeGroup());
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
		refreshFilteredBoard(getChampionship(), getAgeGroup());
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
		refreshFilteredBoard(getChampionship(), getAgeGroup());
	}

	private void updateURLLocations() {
		if (getLocation() == null) {
			// sometimes called from routines outside of normal event flow
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
			updateURLLocation(com.vaadin.flow.component.UI.getCurrent(), getLocation(), "ad", null);
		}

		// Only propagate gender if not null (null = all athletes, remove from URL)
		app.owlcms.data.athlete.Gender gender = getGender();
		updateURLLocation(com.vaadin.flow.component.UI.getCurrent(), getLocation(), "gender", gender != null ? gender.name() : null);
	}
	@Override
	public java.util.HashMap<String, java.util.List<String>> readParams(com.vaadin.flow.router.Location location, java.util.Map<String, java.util.List<String>> parametersMap) {
		// Use default param reading, but treat missing/empty gender as null (all athletes)
		var params = TopParametersReader.super.readParams(location, parametersMap);
		java.util.List<String> genderParams = params.get("gender");
		String genderString = (genderParams != null && !genderParams.isEmpty() && genderParams.get(0) != null && !genderParams.get(0).isEmpty())
				? genderParams.get(0)
				: null;
		app.owlcms.data.athlete.Gender gValue = null;
		if (genderString != null) {
			try {
				gValue = app.owlcms.data.athlete.Gender.valueOf(genderString);
			} catch (Exception e) {
				// ignore invalid value, treat as all
			}
		}
		setGender(gValue); // null means all
		// Remove gender from URL if null
		if (gValue == null) {
			params.remove("gender");
		}
		return new java.util.HashMap<>(params);
	}

	/**
	 * Cancels the dialog timer when user starts editing
	 */
	private void cancelDialogTimer() {
		if (getDialogTimer() != null) {
			getDialogTimer().cancel();
			getDialogTimer().purge();
		}
	}

	/**
	 * Restarts the dialog timer when user stops editing
	 */
	private void restartDialogTimer() {
		if (getDialog() != null && getDialog().isOpened()) {
			// Cancel any existing timer
			cancelDialogTimer();

			// Create new timer to close dialog after 8 seconds of inactivity
			com.vaadin.flow.component.UI ui = com.vaadin.flow.component.UI.getCurrent();
			java.util.Timer timer = new java.util.Timer();
			timer.schedule(
					new java.util.TimerTask() {
						@Override
						public void run() {
							try {
								if (ui != null) {
									ui.access(() -> {
										if (getDialog() != null && getDialog().isOpened()) {
											getDialog().close();
										}
									});
								}
							} catch (Throwable e) {
								// ignore
							}
						}
					}, 8 * 1000L); // 8 seconds
			setDialogTimer(timer);
		}
	}

}
