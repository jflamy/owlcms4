package app.owlcms.nui.displays.topteams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import app.owlcms.apputils.queryparameters.ContentParameters;
import app.owlcms.apputils.queryparameters.ContextFreeParametersReader;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.config.Config;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.displays.topteams.TopTeams;
import app.owlcms.nui.displays.scoreboards.AbstractResultsDisplayPage;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/topteams")

public class TopTeamsPage extends AbstractResultsDisplayPage implements ContextFreeParametersReader {

	Logger logger = (Logger) LoggerFactory.getLogger(TopTeamsPage.class);
	Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private AgeDivision ageDivision;
	private String ageGroupPrefix;
	private AgeGroup ageGroup;
	private Category category;

	public TopTeamsPage() {
		// intentionally empty. superclass will call init() as required.
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#addDialogContent(com.vaadin.flow.component.Component,
	 *      com.vaadin.flow.component.orderedlayout.VerticalLayout)
	 */
	@Override
	public void addDialogContent(Component target, VerticalLayout vl) {
		// logger.debug("addDialogContent ad={} ag={} darkMode={}", getAgeDivision(),
		// getAgeGroupPrefix(),
		// isDarkMode());

		DisplayOptions.addLightingEntries(vl, target, this);
		ComboBox<AgeDivision> ageDivisionComboBox = new ComboBox<>();
		ComboBox<String> ageGroupPrefixComboBox = new ComboBox<>();
		List<AgeDivision> ageDivisions = AgeGroupRepository.allAgeDivisionsForAllAgeGroups();
		ageDivisionComboBox.setItems(ageDivisions);
		ageDivisionComboBox.setPlaceholder(getTranslation("AgeDivision"));
		ageDivisionComboBox.setClearButtonVisible(true);
		ageDivisionComboBox.addValueChangeListener(e -> {
			AgeDivision ageDivision = e.getValue();
			setAgeDivision(ageDivision);
			String existingAgeGroupPrefix = getAgeGroupPrefix();
			List<String> activeAgeGroups = setAgeGroupPrefixItems(ageGroupPrefixComboBox, ageDivision);
			if (existingAgeGroupPrefix != null) {
				ageGroupPrefixComboBox.setValue(existingAgeGroupPrefix);
			} else if (activeAgeGroups != null && !activeAgeGroups.isEmpty() && ageDivision != AgeDivision.MASTERS) {
				ageGroupPrefixComboBox.setValue(activeAgeGroups.get(0));
			}
		});
		ageGroupPrefixComboBox.setPlaceholder(getTranslation("AgeGroup"));
		ageGroupPrefixComboBox.setClearButtonVisible(true);
		ageGroupPrefixComboBox.addValueChangeListener(e -> {
			setAgeGroupPrefix(e.getValue());
			updateURLLocations();

			// FIXME: is doUpdate needed after updating the locations
			// doUpdate(Competition.getCurrent());
		});
		setAgeGroupPrefixItems(ageGroupPrefixComboBox, getAgeDivision());
		ageGroupPrefixComboBox.setValue(getAgeGroupPrefix());
		ageDivisionComboBox.setValue(getAgeDivision());

		vl.add(new NativeLabel(getTranslation("SelectAgeGroup")),
		        new HorizontalLayout(ageDivisionComboBox, ageGroupPrefixComboBox));
	}

	public final AgeDivision getAgeDivision() {
		return ageDivision;
	}

	public final AgeGroup getAgeGroup() {
		return ageGroup;
	}

	public final String getAgeGroupPrefix() {
		return ageGroupPrefix;
	}

	public final Category getCategory() {
		return category;
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation("Scoreboard.TopTeams");
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#readParams(com.vaadin.flow.router.Location,
	 *      java.util.Map)
	 */
	@Override
	public HashMap<String, List<String>> readParams(Location location, Map<String, List<String>> parametersMap) {
		HashMap<String, List<String>> params1 = new HashMap<>(parametersMap);

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
			setAgeDivision(AgeDivision.valueOf(ageDivisionName));
		} catch (Exception e) {
			List<AgeDivision> ageDivisions = AgeGroupRepository.allAgeDivisionsForAllAgeGroups();
			setAgeDivision((ageDivisions != null && !ageDivisions.isEmpty()) ? ageDivisions.get(0) : null);
		}
		// remove if now null
		String value = getAgeDivision() != null ? getAgeDivision().name() : null;
		updateParam(params1, "ad", value);

		List<String> ageGroupParams = params1.get("ag");
		// no age group is the default
		String ageGroupPrefix = (ageGroupParams != null && !ageGroupParams.isEmpty() ? ageGroupParams.get(0) : null);
		setAgeGroupPrefix(ageGroupPrefix);
		String value2 = getAgeGroupPrefix() != null ? getAgeGroupPrefix() : null;
		updateParam(params1, "ag", value2);

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

	public void setAgeDivision(AgeDivision ageDivision) {
		this.ageDivision = ageDivision;
	}

	@Override
	public void setAgeGroup(AgeGroup ag) {
		this.ageGroup = ag;
	}

	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
	}

	@Override
	public void setCategory(Category cat) {
		this.category = cat;
	}

	@Override
	protected void init() {
		var board = new TopTeams();
		this.setBoard(board);
		board.setLeadersDisplay(true);
		board.setRecordsDisplay(true);
		this.addComponent(board);

		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied.
		setDefaultParameters(QueryParameters.simple(Map.of(
		        ContentParameters.SILENT, "true",
		        ContentParameters.DOWNSILENT, "true",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "false",
		        DisplayParameters.RECORDS, "false",
		        DisplayParameters.ABBREVIATED,
		        Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")))));
	}

	private List<String> setAgeGroupPrefixItems(ComboBox<String> ageGroupPrefixComboBox,
	        AgeDivision ageDivision2) {
		List<String> activeAgeGroups = AgeGroupRepository.findActiveAndUsed(ageDivision2);
		ageGroupPrefixComboBox.setItems(activeAgeGroups);
		return activeAgeGroups;
	}

	private void updateURLLocations() {
		updateURLLocation(UI.getCurrent(), getLocation(), DARK,
		        !isDarkMode() ? Boolean.TRUE.toString() : null);
		updateURLLocation(UI.getCurrent(), getLocation(), "ag",
		        getAgeGroupPrefix() != null ? getAgeGroupPrefix() : null);
		updateURLLocation(UI.getCurrent(), getLocation(), "ad",
		        getAgeDivision() != null ? getAgeDivision().name() : null);
	}

}
