
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
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
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

		com.vaadin.flow.component.combobox.ComboBox<app.owlcms.data.agegroup.Championship> championshipComboBox = new com.vaadin.flow.component.combobox.ComboBox<>();
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
			updateURLLocations();
			restartDialogTimer();
		});
		if (championships.contains(getChampionship())) {
			championshipComboBox.setValue(getChampionship());
		} else {
			championshipComboBox.setValue(null);
		}
		vl.add(new com.vaadin.flow.component.html.NativeLabel(app.owlcms.i18n.Translator.translate("Championship")),
		        championshipComboBox);


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

	/**
	 * Filters the given list of athletes to only those who participate in the specified championship.
	 * If championship is null, returns the original list.
	 */
	private List<Athlete> getChampionshipRanking(Championship championship, Gender gender) {
		List<Athlete> championshipAthletes = AgeGroupRepository
		        .allWeighedInPAthletesForAgeGroupAgeDivision(null, championship).stream()
		        .filter(athlete -> athlete.getGroup() != null && athlete.getGender() == gender)
		        .toList();
		return AthleteSorter.resultsOrderCopy(championshipAthletes, effectiveBestAthleteScoring(championship));
	}

	/**
	 * Refreshes the TopSinclair board with athletes filtered by the given championship.
	 */
	private void refreshFilteredBoard(app.owlcms.data.agegroup.Championship championship) {
		if (this.getBoard() instanceof app.owlcms.displays.top.TopSinclair topSinclairBoard) {
			if (championship == null) {
				topSinclairBoard.setUseFilteredResults(false);
			} else {
				topSinclairBoard.setUseFilteredResults(true);
			}

			List<Athlete> rankedMen = championship != null
			        ? getChampionshipRanking(championship, Gender.M)
			        : Competition.getCurrent().getGlobalRanking(Gender.M, effectiveBestAthleteScoring(null));
			List<Athlete> rankedWomen = championship != null
			        ? getChampionshipRanking(championship, Gender.F)
			        : Competition.getCurrent().getGlobalRanking(Gender.F, effectiveBestAthleteScoring(null));
			topSinclairBoard.doUpdateWithFilteredLists(rankedMen, rankedWomen);
		}
	}

	private Ranking effectiveBestAthleteScoring(Championship championship) {
		Championship effectiveChampionship = championship != null ? championship : Championship.of(null);
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
		Championship championship = getChampionship() != null ? getChampionship() : Championship.of(null);
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
		refreshFilteredBoard(getChampionship());
	}

	@Override
	public final void setAgeGroup(AgeGroup ag) {
		this.ageGroup = null;
		this.ageGroupPrefix = null;
		((TopSinclair) this.getBoard()).setAgeGroup(null);
		((TopSinclair) this.getBoard()).setAgeGroupPrefix(null);
		refreshFilteredBoard(getChampionship());
	}

	@Override
	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = null;
		this.ageGroup = null;
		((TopSinclair) this.getBoard()).setAgeGroup(null);
		((TopSinclair) this.getBoard()).setAgeGroupPrefix(null);
		refreshFilteredBoard(getChampionship());
	}

	@Override
	public final void setCategory(Category cat) {
		this.category = cat;
		((TopSinclair) this.getBoard()).setCategory(cat);
		refreshFilteredBoard(getChampionship());
	}

	@Override
	public void setChampionship(Championship ageDivision) {
		this.ageDivision = ageDivision;
		this.ageGroup = null;
		this.ageGroupPrefix = null;
		((TopSinclair) this.getBoard()).setChampionship(ageDivision);
		((TopSinclair) this.getBoard()).setAgeGroup(null);
		((TopSinclair) this.getBoard()).setAgeGroupPrefix(null);
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
		refreshFilteredBoard(getChampionship());
	}

	   private void updateURLLocations() {
		   if (getLocation() == null) {
			   // sometimes called from routines outside of normal event flow
			   return;
		   }
		   updateURLLocation(com.vaadin.flow.component.UI.getCurrent(), getLocation(), DisplayParameters.DARK,
				   !isDarkMode() ? Boolean.TRUE.toString() : null);

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

		   setAgeGroupPrefix(null);
		   updateParam(params, "ag", null);

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
