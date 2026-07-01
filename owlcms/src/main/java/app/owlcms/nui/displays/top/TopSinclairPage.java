
/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays.top;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
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

	@Override
	public boolean isIgnoreGroupFromURL() {
		return true;
	}

	@Override
	protected boolean shouldRegisterPageOnUiEventBus() {
		return false;
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

		com.vaadin.flow.component.combobox.ComboBox<app.owlcms.data.agegroup.Championship> championshipComboBox = new com.vaadin.flow.component.combobox.ComboBox<>();
		com.vaadin.flow.component.combobox.ComboBox<String> ageGroupPrefixComboBox = new com.vaadin.flow.component.combobox.ComboBox<>();
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
			String existingAgeGroupPrefix = getAgeGroupPrefix();
			java.util.List<String> activeAgeGroups = setAgeGroupPrefixItems(ageGroupPrefixComboBox, championship);
			if (existingAgeGroupPrefix != null && activeAgeGroups != null
			        && activeAgeGroups.contains(existingAgeGroupPrefix)) {
				ageGroupPrefixComboBox.setValue(existingAgeGroupPrefix);
			} else {
				ageGroupPrefixComboBox.clear();
			}
			updateURLLocations();
			// Restart timer after value change
			restartDialogTimer();
		});
		ageGroupPrefixComboBox.setPlaceholder(app.owlcms.i18n.Translator.translate("AgeGroup"));
		ageGroupPrefixComboBox.setClearButtonVisible(true);
		// Reset timer when user starts editing
		ageGroupPrefixComboBox.addFocusListener(e -> restartDialogTimer());
		ageGroupPrefixComboBox.addValueChangeListener(e -> {
			setAgeGroupPrefix(e.getValue());
			updateURLLocations();
			// Restart timer after value change
			restartDialogTimer();
		});
		setAgeGroupPrefixItems(ageGroupPrefixComboBox, getChampionship());
		ageGroupPrefixComboBox.setValue(getAgeGroupPrefix());
		if (championships.contains(getChampionship())) {
			championshipComboBox.setValue(getChampionship());
		} else {
			championshipComboBox.setValue(null);
		}
		vl.add(new com.vaadin.flow.component.html.NativeLabel(app.owlcms.i18n.Translator.translate("SelectAgeGroup")),
		        new com.vaadin.flow.component.orderedlayout.HorizontalLayout(championshipComboBox, ageGroupPrefixComboBox));


		// Gender selection ComboBox (M / F only, null = no filtering)
		com.vaadin.flow.component.combobox.ComboBox<app.owlcms.data.athlete.Gender> genderComboBox = new com.vaadin.flow.component.combobox.ComboBox<>();
		// Only include M, F
		genderComboBox.setItems(app.owlcms.data.athlete.Gender.M, app.owlcms.data.athlete.Gender.F);
		genderComboBox.setClearButtonVisible(true);
		genderComboBox.setItemLabelGenerator(g -> {
			if (g == null)
				return app.owlcms.i18n.Translator.translate("Gender.Mixed"); // unused
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

		// Show lifts checkbox (always visible)
		com.vaadin.flow.component.checkbox.Checkbox showLiftsCheckbox = new com.vaadin.flow.component.checkbox.Checkbox(app.owlcms.i18n.Translator.translate("TopSinclair.ShowLifts"));
		showLiftsCheckbox.setValue(isDisplayLifts());
		showLiftsCheckbox.addValueChangeListener(event -> {
			setDisplayLifts(event.getValue());
			updateURLLocations();
			restartDialogTimer();
		});
		vl.add(showLiftsCheckbox);

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
	private java.util.List<String> setAgeGroupPrefixItems(
	        com.vaadin.flow.component.combobox.ComboBox<String> ageGroupPrefixComboBox,
	        app.owlcms.data.agegroup.Championship championship) {
		java.util.List<String> activeAgeGroups = app.owlcms.data.agegroup.AgeGroupRepository
		        .findActiveAndUsedAgeGroupNames(championship);
		ageGroupPrefixComboBox.setItems(activeAgeGroups);
		return activeAgeGroups;
	}

	/**
	 * Filters the given list of athletes to only those who participate in the specified championship and/or age group.
	 * If championship is null, returns the original list.
	 * If ageGroup is null but championship is specified, returns athletes from all age groups in the championship.
	 * If both are specified, returns athletes from the specific age group.
	 */
	private java.util.List<app.owlcms.data.athlete.Athlete> filterAthletesByChampionshipAndAgeGroup(
	        java.util.List<app.owlcms.data.athlete.Athlete> rankedAthletes,
	        app.owlcms.data.agegroup.Championship championship,
	        app.owlcms.data.agegroup.AgeGroup ageGroup) {
		if (championship == null && ageGroup == null) {
			return rankedAthletes;
		}

		String ageGroupCode = getAgeGroupPrefix() != null && !getAgeGroupPrefix().isBlank()
		        ? getAgeGroupPrefix()
		        : ageGroup != null ? ageGroup.getCode() : null;
		Set<Long> eligibleAthleteIds = new HashSet<>();
		for (app.owlcms.data.athlete.Athlete athlete : AgeGroupRepository
		        .allWeighedInPAthletesForAgeGroupAgeDivision(ageGroupCode, championship)) {
			if (athlete != null && athlete.getGroup() != null && athlete.getId() != null) {
				eligibleAthleteIds.add(athlete.getId());
			}
		}

		logger.debug("Filtering {} ranked athletes with championship={} ageGroup={} eligibleAthletes={}",
		        rankedAthletes != null ? rankedAthletes.size() : 0,
		        championship != null ? championship.getName() : "null",
		        ageGroupCode,
		        eligibleAthleteIds.size());

		java.util.List<app.owlcms.data.athlete.Athlete> filtered = new java.util.ArrayList<>();
		if (rankedAthletes == null || rankedAthletes.isEmpty() || eligibleAthleteIds.isEmpty()) {
			return filtered;
		}

		for (app.owlcms.data.athlete.Athlete athlete : rankedAthletes) {
			if (athlete != null && athlete.getId() != null && eligibleAthleteIds.contains(athlete.getId())) {
				filtered.add(athlete);
			}
		}
		logger.debug("Filtered {} ranked athletes down to {} athletes",
		        rankedAthletes.size(), filtered.size());
		return filtered;
	}

	/**
	 * Refreshes the TopSinclair board with athletes filtered by the given championship and age group.
	 */
	private void refreshFilteredBoard(app.owlcms.data.agegroup.Championship championship, app.owlcms.data.agegroup.AgeGroup ageGroup) {
		if (this.getBoard() instanceof app.owlcms.displays.top.TopSinclair topSinclairBoard) {
			boolean hasAgeGroupFilter = (getAgeGroupPrefix() != null && !getAgeGroupPrefix().isBlank()) || ageGroup != null;
			if (championship == null && !hasAgeGroupFilter) {
				topSinclairBoard.setUseFilteredResults(false);
			} else {
				topSinclairBoard.setUseFilteredResults(true);
			}

			java.util.List<app.owlcms.data.athlete.Athlete> allMen = Competition.getCurrent()
			        .getGlobalRanking(app.owlcms.data.athlete.Gender.M, effectiveBestAthleteScoring(championship, ageGroup));
			java.util.List<app.owlcms.data.athlete.Athlete> allWomen = Competition.getCurrent()
			        .getGlobalRanking(app.owlcms.data.athlete.Gender.F, effectiveBestAthleteScoring(championship, ageGroup));
			java.util.List<app.owlcms.data.athlete.Athlete> filteredMen = filterAthletesByChampionshipAndAgeGroup(allMen, championship, ageGroup);
			java.util.List<app.owlcms.data.athlete.Athlete> filteredWomen = filterAthletesByChampionshipAndAgeGroup(allWomen, championship, ageGroup);
			// Use the new method that works with filtered lists instead of overriding them
			topSinclairBoard.doUpdateWithFilteredLists(filteredMen, filteredWomen);
		}
	}

	private Ranking effectiveBestAthleteScoring(Championship championship, AgeGroup ageGroup) {
		Championship effectiveChampionship = ageGroup != null && ageGroup.getChampionship() != null
		        ? ageGroup.getChampionship()
		        : championship != null ? championship : Championship.of(null);
		return effectiveChampionship.getBestAthleteScoringSystem();
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
		Championship championship = getAgeGroup() != null && getAgeGroup().getChampionship() != null
		        ? getAgeGroup().getChampionship()
		        : getChampionship() != null ? getChampionship() : Championship.of(null);
		return Translator.translate("Scoreboard.TopScore",
		        Ranking.getScoringTitle(championship.getBestAthleteScoringSystem()));
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
		refreshFilteredBoard(getChampionship(), null);
	}

	@Override
	public final void setAgeGroup(AgeGroup ag) {
		this.ageGroup = ag;
		this.ageGroupPrefix = ag != null ? ag.getCode() : null;
		((TopSinclair) this.getBoard()).setAgeGroup(ag);
		((TopSinclair) this.getBoard()).setAgeGroupPrefix(this.ageGroupPrefix);
		// Re-apply championship filtering after age group change
		refreshFilteredBoard(getChampionship(), ag);
	}

	@Override
	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
		this.ageGroup = null;
		((TopSinclair) this.getBoard()).setAgeGroup(null);
		((TopSinclair) this.getBoard()).setAgeGroupPrefix(ageGroupPrefix);
		// Re-apply championship filtering after age group prefix change
		refreshFilteredBoard(getChampionship(), null);
	}

	@Override
	public final void setCategory(Category cat) {
		this.category = cat;
		((TopSinclair) this.getBoard()).setCategory(cat);
		// Re-apply championship filtering after category change
		refreshFilteredBoard(getChampionship(), null);
	}

	@Override
	public void setChampionship(Championship ageDivision) {
		this.ageDivision = ageDivision;
		((TopSinclair) this.getBoard()).setChampionship(ageDivision);
		// Trigger filtering when championship is set programmatically (e.g., from URL parameters)
		refreshFilteredBoard(ageDivision, null);
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
		refreshFilteredBoard(getChampionship(), null);
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
		        DisplayParameters.ABBREVIATED, Boolean.toString(Config.getCurrent().featureSwitch(FeatureSwitch.SHORT_SCOREBOARD_NAMES)));
		var additionalMap = Map.of(
		        SoundParameters.LIVE_LIGHTS, Boolean.toString(!Config.getCurrent().featureSwitch(FeatureSwitch.NO_LIVE_LIGHTS)),
		        SoundParameters.SHOW_DECLARATIONS, "false",
		        SoundParameters.CENTER_NOTIFICATIONS, Boolean.toString(Config.getCurrent().featureSwitch(FeatureSwitch.CENTER_ANNOUNCER_NOTIFICATIONS)),
		        SoundParameters.START_ORDER, "false",
		        DisplayParameters.CURRENT_ATTEMPT, "false",
		        DisplayParameters.SHOW_MEDALS, "auto");
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
		refreshFilteredBoard(getChampionship(), null);
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

		   // Only propagate displayLifts if true (default is false)
		   if (isDisplayLifts()) {
			   updateURLLocation(com.vaadin.flow.component.UI.getCurrent(), getLocation(), "displayLifts", "true");
		   } else {
			   updateURLLocation(com.vaadin.flow.component.UI.getCurrent(), getLocation(), "displayLifts", null);
		   }

		// Propagate nbAthletes if different from default (10)
		int nb = getNbAthletes();
		if (nb > 0 && nb != 10) {
			updateURLLocation(com.vaadin.flow.component.UI.getCurrent(), getLocation(), "nbAthletes", Integer.toString(nb));
		} else {
			updateURLLocation(com.vaadin.flow.component.UI.getCurrent(), getLocation(), "nbAthletes", null);
		}
	   }
	@Override
	   public java.util.HashMap<String, java.util.List<String>> readParams(com.vaadin.flow.router.Location location, java.util.Map<String, java.util.List<String>> parametersMap) {
		   // Use default param reading, but treat missing/empty gender as null (all athletes)
		   var params = TopParametersReader.super.readParams(location, parametersMap);
		   java.util.List<String> ageDivisionParams = parametersMap.get("ad");
		   String ageDivisionName = (ageDivisionParams != null && !ageDivisionParams.isEmpty() && ageDivisionParams.get(0) != null && !ageDivisionParams.get(0).isEmpty())
				   ? ageDivisionParams.get(0)
				   : null;
		   Championship resolvedChampionship = Championship.resolveDisplayChampionship(ageDivisionName, true);
		   setChampionship(resolvedChampionship);
		   updateParam(params, "ad", resolvedChampionship != null ? resolvedChampionship.getName() : null);

		   java.util.List<String> ageGroupParams = parametersMap.get("ag");
		   String ageGroupCode = (ageGroupParams != null && !ageGroupParams.isEmpty() && ageGroupParams.get(0) != null
				   && !ageGroupParams.get(0).isEmpty())
					   ? ageGroupParams.get(0)
					   : null;
		   setAgeGroupPrefix(ageGroupCode);
		   updateParam(params, "ag", ageGroupCode);

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

		   // Parse displayLifts parameter (default false)
		   java.util.List<String> displayLiftsParams = params.get("displayLifts");
		   boolean displayLifts = false;
		   if (displayLiftsParams != null && !displayLiftsParams.isEmpty()) {
			   String val = displayLiftsParams.get(0);
			   displayLifts = val != null && (val.equalsIgnoreCase("true") || val.equals("1"));
		   }
		   setDisplayLifts(displayLifts);
		   if (!displayLifts) {
			   params.remove("displayLifts");
		   }

		 // Parse nbAthletes (number of athletes) parameter; default to 10 if missing/invalid
		 java.util.List<String> nbParams = params.get("nbAthletes");
		 int nbValue = 10;
		 if (nbParams != null && !nbParams.isEmpty()) {
			 try {
				 int parsed = Integer.parseInt(nbParams.get(0));
				 if (parsed > 0) {
					 nbValue = parsed;
				 }
			 } catch (NumberFormatException e) {
				 // ignore and keep default
			 }
		 }
		 setNbAthletes(nbValue);
		 if (nbValue == 10) {
			 params.remove("nb");
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
