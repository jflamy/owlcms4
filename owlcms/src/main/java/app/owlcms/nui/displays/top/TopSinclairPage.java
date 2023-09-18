package app.owlcms.nui.displays.top;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.apputils.queryparameters.ResultsParametersReader;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.config.Config;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.displays.top.TopSinclair;
import app.owlcms.nui.displays.scoreboards.AbstractResultsDisplayPage;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/topsinclair")

public class TopSinclairPage extends AbstractResultsDisplayPage implements ResultsParametersReader {

	Logger logger = (Logger) LoggerFactory.getLogger(TopSinclairPage.class);
	Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private AgeGroup ageGroup;
	private Category category;
	private String ageGroupPrefix;
	private AgeDivision ageDivision;

	public TopSinclairPage() {
		// intentionally empty. superclass will call init() as required.
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#addDialogContent(com.vaadin.flow.component.Component,
	 *      com.vaadin.flow.component.orderedlayout.VerticalLayout)
	 */
	@Override
	public void addDialogContent(Component target, VerticalLayout vl) {
		DisplayOptions.addLightingEntries(vl, target, this);
	}

	@Override
	public final AgeGroup getAgeGroup() {
		return ageGroup;
	}

	@Override
	public final Category getCategory() {
		return category;
	}
	
	@Override
	public AgeDivision getAgeDivision() {
		return ageDivision;
	}
	
	@Override
	public String getAgeGroupPrefix() {
		return ageGroupPrefix;
	}
	
	@Override
	public void setAgeDivision(AgeDivision ageDivision) {
		this.ageDivision = ageDivision;
	}
	
	@Override
	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
	}


	@Override
	public String getPageTitle() {
		return getTranslation("Scoreboard.TopSinclair");
	}

	@Override
	public final void setAgeGroup(AgeGroup ag) {
		this.ageGroup = ag;
	}

	@Override
	public final void setCategory(Category cat) {
		this.category = cat;
	}

	@Override
	protected void init() {
		var board = new TopSinclair();
		this.setBoard(board);
		this.addComponent(board);

		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied.
		setDefaultParameters(QueryParameters.simple(Map.of(
		        SoundParameters.SILENT, "true",
		        SoundParameters.DOWNSILENT, "true",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "false",
		        DisplayParameters.RECORDS, "false",
		        DisplayParameters.VIDEO, "false",
		        DisplayParameters.PUBLIC, "false",
		        SoundParameters.SINGLEREF, "false",
		        DisplayParameters.ABBREVIATED,
		        Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")))));
	}

	@Override
	public Map<String, List<String>> readParams(Location location, Map<String, List<String>> parametersMap) {
		return ResultsParametersReader.super.readParams(location, parametersMap);
	}

}
