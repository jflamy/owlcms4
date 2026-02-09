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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.apputils.queryparameters.TopParametersReader;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.displays.top.TopTeams;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.displays.scoreboards.AbstractResultsDisplayPage;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/topteams")

public class TopTeamsPage extends AbstractResultsDisplayPage implements TopParametersReader {

	Logger logger;
	Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private Championship ageDivision;
	private String ageGroupPrefix;
	private AgeGroup ageGroup;
	private Category category;
	private Gender gender;

	public TopTeamsPage() {
		// intentionally empty. superclass will call init() as required.
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#addDialogContent(com.vaadin.flow.component.Component,
	 *      com.vaadin.flow.component.orderedlayout.VerticalLayout)
	 */
	@Override
	public void addDialogContent(Component target, VerticalLayout vl) {
		DisplayOptions.addLightingEntries(vl, target, this);
		ComboBox<Championship> championshipComboBox = new ComboBox<>();
		ComboBox<String> ageGroupPrefixComboBox = new ComboBox<>();
		List<Championship> championships = Championship.findAllUsed(true);
		championshipComboBox.setItemLabelGenerator(c -> c.getName());
		championshipComboBox.setItems(championships);
		championshipComboBox.setPlaceholder(Translator.translate("Championship"));
		championshipComboBox.setClearButtonVisible(true);
		championshipComboBox.addValueChangeListener(e -> {
			Championship championship = e.getValue();
			setChampionship(championship);
			String existingAgeGroupPrefix = getAgeGroupPrefix();
			List<String> activeAgeGroups = setAgeGroupPrefixItems(ageGroupPrefixComboBox, championship);
			if (existingAgeGroupPrefix != null) {
				ageGroupPrefixComboBox.setValue(existingAgeGroupPrefix);
			} else if (activeAgeGroups != null && !activeAgeGroups.isEmpty() && championship.getType() != ChampionshipType.MASTERS) {
				ageGroupPrefixComboBox.setValue(activeAgeGroups.get(0));
			}
		});
		ageGroupPrefixComboBox.setPlaceholder(Translator.translate("AgeGroup"));
		ageGroupPrefixComboBox.setClearButtonVisible(true);
		ageGroupPrefixComboBox.addValueChangeListener(e -> {
			setAgeGroupPrefix(e.getValue());
			updateURLLocations();
		});
		setAgeGroupPrefixItems(ageGroupPrefixComboBox, getChampionship());
		ageGroupPrefixComboBox.setValue(getAgeGroupPrefix());
		championshipComboBox.setValue(getChampionship());

		vl.add(new NativeLabel(Translator.translate("SelectAgeGroup")),
		        new HorizontalLayout(championshipComboBox, ageGroupPrefixComboBox));
		
		ComboBox<Gender> genderComboBox = new ComboBox<>();
		genderComboBox.setItems(Gender.values());
		genderComboBox.setClearButtonVisible(true);
		genderComboBox.setItemLabelGenerator(g -> {
		    return switch (g) {
		        case M -> Translator.translate("Gender.Men");
		        case F -> Translator.translate("Gender.Women");
		        case I -> Translator.translate("Gender.Women") + ", " + Translator.translate("Gender.Men");
		        case MF -> Translator.translate("Gender.Mixed");
		    };
		});
		genderComboBox.addValueChangeListener(event -> {
			setGender(event.getValue());
			updateURLLocations();
		});
		vl.add(new NativeLabel(Translator.translate("Scoreboard.SelectGenders")),
		        new HorizontalLayout(genderComboBox));
	}

	@Override
	public final AgeGroup getAgeGroup() {
		return this.ageGroup;
	}

	@Override
	public final String getAgeGroupPrefix() {
		return this.ageGroupPrefix;
	}

	@Override
	public final Category getCategory() {
		return this.category;
	}

	@Override
	public final Championship getChampionship() {
		return this.ageDivision;
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return Translator.translate("Scoreboard.TopTeams");
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParametersReader#readParams(com.vaadin.flow.router.Location, java.util.Map)
	 */
	@Override
	public HashMap<String, List<String>> readParams(Location location, Map<String, List<String>> parametersMap) {
		HashMap<String, List<String>> params1 = new HashMap<>(parametersMap);
		// logger.debug("TopTeamsPage readParams");

		List<String> darkParams = params1.get(DARK);
		// dark is the default. dark=false or dark=no or ... will turn off dark mode.
		boolean darkMode = darkParams == null || darkParams.isEmpty() || darkParams.get(0).toLowerCase().equals("true");
		setDarkMode(darkMode);
		updateParam(params1, DARK, !isDarkMode() ? "false" : null);

		List<String> silentParams = params1.get(SILENT);
		// dark is the default. dark=false or dark=no or ... will turn off dark mode.
		boolean silentMode = silentParams == null || silentParams.isEmpty()
		        || silentParams.get(0).toLowerCase().equals("true");
		setSilenced(silentMode);
		updateParam(params1, SILENT, !isSilenced() ? "false" : null);

		List<String> ageDivisionParams = params1.get("ad");
		// no age division
		String ageDivisionName = (ageDivisionParams != null && !ageDivisionParams.isEmpty() ? ageDivisionParams.get(0)
		        : null);
		try {
			setChampionship(Championship.of(ageDivisionName));
		} catch (Exception e) {
			List<Championship> ageDivisions = Championship.findAllUsed(true);
			setChampionship((ageDivisions != null && !ageDivisions.isEmpty()) ? ageDivisions.get(0) : null);
		}
		// remove if now null
		String value = getChampionship() != null ? getChampionship().getName() : null;
		updateParam(params1, "ad", value);

		List<String> ageGroupParams = params1.get("ag");
		// no age group is the default
		String ageGroupPrefix = (ageGroupParams != null && !ageGroupParams.isEmpty() ? ageGroupParams.get(0) : null);
		setAgeGroupPrefix(ageGroupPrefix);
		String value2 = getAgeGroupPrefix() != null ? getAgeGroupPrefix() : null;
		updateParam(params1, "ag", value2);
		
		List<String> genderParams = params1.get("gender");
		// no age group is the default
		String genderString = (genderParams != null && !genderParams.isEmpty() ? genderParams.get(0) : null);
		Gender gValue = null;
		try {
			gValue = Gender.valueOf(genderString);
			setGender(gValue);
		} catch (Exception e) {
		}
		updateParam(params1, "gender", gValue == null ? null : gValue.toString());

		switchLightingMode(darkMode, false);
		updateURLLocations();
		setShowInitialDialog(
		        darkParams == null && ageDivisionParams == null && ageGroupParams == null && silentParams == null);

		if (getDialog() == null) {
			buildDialog(this);
		}
		setUrlParameterMap(params1);
		return params1;
	}
	
	@Override
	public void setGender(Gender gender) {
		this.gender = gender;
		((TopTeams) this.getBoard()).setGender(gender);
		((TopTeams) this.getBoard()).doUpdate(Competition.getCurrent());
	}

	@Override
	public final void setAgeGroup(AgeGroup ag) {
		this.ageGroup = ag;
		((TopTeams) this.getBoard()).setAgeGroup(ag);
		((TopTeams) this.getBoard()).doUpdate(Competition.getCurrent());
	}

	@Override
	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
		((TopTeams) this.getBoard()).setAgeGroupPrefix(ageGroupPrefix);
		((TopTeams) this.getBoard()).doUpdate(Competition.getCurrent());
	}

	@Override
	public final void setCategory(Category cat) {
		this.category = cat;
		((TopTeams) this.getBoard()).setCategory(cat);
		((TopTeams) this.getBoard()).doUpdate(Competition.getCurrent());
	}

	@Override
	public void setChampionship(Championship ageDivision) {
		this.ageDivision = ageDivision;
		((TopTeams) this.getBoard()).setChampionship(ageDivision);
		((TopTeams) this.getBoard()).doUpdate(Competition.getCurrent());
	}

	@Override
	protected void init() {
		this.logger = (Logger) LoggerFactory.getLogger(TopTeamsPage.class);
		// logger.debug("TopTeamsPage init");
		var board = new TopTeams();
		this.setBoard(board);
		// logger.debug("TopTeamsPage board {}", this.getBoard());

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
		        SoundParameters.START_ORDER, "false",
		        DisplayParameters.CURRENT_ATTEMPT, "false",
		        DisplayParameters.SHOW_MEDALS, "auto");
		Map<String, String> fullMap = new TreeMap<>();
		fullMap.putAll(initialMap);
		fullMap.putAll(additionalMap);
		setDefaultParameters(QueryParameters.simple(fullMap));
		this.addComponent(board);
	}

	private List<String> setAgeGroupPrefixItems(ComboBox<String> ageGroupPrefixComboBox,
	        Championship ageDivision2) {
		List<String> activeAgeGroups = AgeGroupRepository.findActiveAndUsedAgeGroupNames(ageDivision2);
		ageGroupPrefixComboBox.setItems(activeAgeGroups);
		return activeAgeGroups;
	}

	private void updateURLLocations() {
		if (getLocation() == null) {
			// sometimes called from routines outside of normal event flow.
			return;
		}
		updateURLLocation(UI.getCurrent(), getLocation(), DARK,
		        !isDarkMode() ? Boolean.TRUE.toString() : null);
		updateURLLocation(UI.getCurrent(), getLocation(), "ag",
		        getAgeGroupPrefix() != null ? getAgeGroupPrefix() : null);
		updateURLLocation(UI.getCurrent(), getLocation(), "ad",
		        getChampionship() != null ? getChampionship().getName() : null);
		updateURLLocation(UI.getCurrent(), getLocation(), "gender",
		        getGender() != null ? getGender().name(): null);
	}

	@Override
	public Gender getGender() {
		return gender;
	}

}
