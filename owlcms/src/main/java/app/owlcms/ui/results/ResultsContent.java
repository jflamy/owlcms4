/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */

package app.owlcms.ui.results;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import app.owlcms.components.crudui.OwlcmsCrudFormFactory;
import app.owlcms.components.crudui.OwlcmsGridCrud;
import app.owlcms.components.crudui.OwlcmsGridLayout;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.AthleteSorter.Ranking;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.spreadsheet.JXLSResultSheet;
import app.owlcms.ui.group.AthleteCardFormFactory;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.ui.shared.AthleteGridLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "results/results", layout = AthleteGridLayout.class)
public class ResultsContent extends AthleteGridContent {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(ResultsContent.class);
	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
	}

	private Button download;
	private Anchor groupResults;
	private Group currentGroup;
	private FieldOfPlay currentFop;
	private JXLSResultSheet xlsWriter;

	/**
	 * Instantiates a new announcer content.
	 * Does nothing. Content is created in {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public ResultsContent() {
		setTopBarTitle("Group Results");
	}

	/** We do not connect to the event bus, and we do not track a field of play
	 * (non-Javadoc)
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		grid = getGrid();
		fillHW(grid, this);
		createTopBar();
	}
	
	/**
	 * Create the top bar.
	 * 
	 * Note: the top bar is created before the content.
	 * @see #showRouterLayoutContent(HasElement) for how to content to layout and vice-versa
	 * 
	 * @param topBar
	 */
	@Override
	protected void createTopBar() {
		// show arrow but close menu
		getAppLayout().setMenuVisible(true);
		getAppLayout().closeDrawer();
		
		topBar = getAppLayout().getAppBarElementWrapper();
		
		H3 title = new H3();
		title.setText("Group Results");
		title.add();
		title.getStyle()
			.set("margin", "0px 0px 0px 0px")
			.set("font-weight", "normal");
		
		groupSelect = new ComboBox<>();
		groupSelect.setPlaceholder("Select Group");
		groupSelect.setItems(GroupRepository.findAll());
		groupSelect.setItemLabelGenerator(Group::getName);
		groupSelect.setValue(null);
		groupSelect.setWidth("8em");
		setGroupSelectionListener();

		xlsWriter = new JXLSResultSheet();
		StreamResource href = new StreamResource("resultSheet.xls", xlsWriter);
		groupResults = new Anchor(href, "");
		download = new Button("Group Results",new Icon(VaadinIcon.DOWNLOAD_ALT));
		groupResults.add(download);
			
		HorizontalLayout buttons = new HorizontalLayout(
				groupResults);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		topBar
			.getElement()
			.getStyle()
			.set("flex", "100 1");
		topBar.removeAll();
		topBar.add(title, groupSelect, buttons);
		topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		topBar.setFlexGrow(0.2, title);
		topBar.setSpacing(true);
		topBar.setAlignItems(FlexComponent.Alignment.CENTER);
	}

	protected void setGroupSelectionListener() {
		groupSelect.setValue(getGridGroup());
		groupSelect.addValueChangeListener(e -> {
			setGridGroup(e.getValue());
			currentGroup = e.getValue();
			// the name of the resulting file is set as an attribute on the <a href tag that surrounds
			// the download button.
			xlsWriter.setGroup(currentGroup);
			groupResults.getElement().setAttribute("download", "results"+(currentGroup != null ? "_"+currentGroup : "_all") +".xls");
		});
	}
	
	/**
	 * Gets the grid grid.
	 *
	 * @return the grid grid
	 */
	@Override
	public GridCrud<Athlete> getGrid() {
		OwlcmsCrudFormFactory<Athlete> formFactory = new AthleteCardFormFactory(Athlete.class);

		Grid<Athlete> grid = new Grid<Athlete>(Athlete.class, false);
		ThemeList themes = grid.getThemeNames();
		themes.add("compact");
		themes.add("row-stripes");
		grid.setColumns("lastName", "firstName", "team", "category", "bestSnatch", "snatchRank", "bestCleanJerk",
			"cleanJerkRank", "total", "totalRank");
		grid.getColumnByKey("lastName")
			.setHeader("Last Name");
		grid.getColumnByKey("firstName")
			.setHeader("First Name");
		grid.getColumnByKey("team")
			.setHeader("Team");
		grid.getColumnByKey("category")
			.setHeader("Category");
		grid.getColumnByKey("bestSnatch")
			.setHeader("Snatch");
		grid.getColumnByKey("snatchRank")
			.setHeader("Snatch Rank");
		grid.getColumnByKey("bestCleanJerk")
			.setHeader("Clean&Jerk");
		grid.getColumnByKey("cleanJerkRank")
			.setHeader("Clean&Jerk Rank");
		grid.getColumnByKey("total")
			.setHeader("Total");
		grid.getColumnByKey("totalRank")
			.setHeader("Rank");

		OwlcmsGridLayout gridLayout = new OwlcmsGridLayout(Athlete.class);
		GridCrud<Athlete> crud = new OwlcmsGridCrud<Athlete>(Athlete.class,
				gridLayout,
				formFactory,
				grid) {
			@Override
			protected void initToolbar() {}
			@Override
			protected void updateButtons() {}
			
			@Override
		    protected void updateButtonClicked() {
				// only edit non-lifting groups
				if (!checkFOP()) {
					super.updateButtonClicked();
				}
		    }
		};
		
		groupFilter.setPlaceholder("Group");
		groupFilter.setItems(GroupRepository.findAll());
		groupFilter.setItemLabelGenerator(Group::getName);
		// hide because the top bar has it
		groupFilter.getStyle().set("display", "none");
		groupFilter.addValueChangeListener(e -> {
				logger.debug("updating filters: group={}",e.getValue());
				currentGroup = e.getValue();
				updateURLLocation(locationUI, location, currentGroup);
				subscribeIfLifting(e.getValue());
		});
		
		crud.setCrudListener(this);
		crud.setClickRowToUpdate(true);
		crud.getCrudLayout()
			.addToolbarComponent(groupFilter);

		return crud;
	}


	/* (non-Javadoc)
	 * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
	 */
	@Override
	public Athlete add(Athlete Athlete) {
		AthleteRepository.save(Athlete);
		return Athlete;
	}

	/* (non-Javadoc)
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public Athlete update(Athlete Athlete) {
		Athlete savedAthlete = AthleteRepository.save(Athlete);
		if (currentFop != null) {
			currentFop.getFopEventBus()
				.post(new FOPEvent.WeightChange(this.getOrigin(), savedAthlete));
		}
		return savedAthlete;
	}

	/* (non-Javadoc)
	 * @see org.vaadin.crudui.crud.CrudListener#delete(java.lang.Object)
	 */
	@Override
	public void delete(Athlete Athlete) {
		AthleteRepository.delete(Athlete);
	}

	/**
	 * Get the content of the grid.
	 * Invoked by refreshGrid.
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<Athlete> findAll() {
		List<Athlete> athletes = AthleteSorter.resultsOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(groupFilter.getValue(), true), Ranking.TOTAL);
		AthleteSorter.assignCategoryRanks(athletes, Ranking.TOTAL);
		AthleteSorter.resultsOrder(athletes, Ranking.SNATCH);
		AthleteSorter.assignCategoryRanks(athletes, Ranking.SNATCH);
		AthleteSorter.resultsOrder(athletes, Ranking.CLEANJERK);
		AthleteSorter.assignCategoryRanks(athletes, Ranking.CLEANJERK);
		return athletes;
	}

	/**
	 * @return the groupFilter
	 */
	@Override
	public ComboBox<Group> getGroupFilter() {
		return groupFilter;
	}

	@Override
	public void refresh() {
		grid.refreshGrid();
	}
	
	private void subscribeIfLifting(Group nGroup) {
		logger.debug("subscribeIfLifting {}",nGroup);
		Collection<FieldOfPlay> fops = OwlcmsFactory.getFOPs();
		currentFop = null;
		currentGroup = nGroup;
		
		// go through all the FOPs
		for (FieldOfPlay fop: fops) {
			// unsubscribe from FOP -- ensures that we clean up if no group is lifting
			try {fop.getUiEventBus().unregister(this);} catch (Exception ex) {}
			try {fop.getFopEventBus().unregister(this);} catch (Exception ex) {}
			
			// subscribe to fop and start tracking if actually lifting
			if (fop.getGroup() != null && fop.getGroup().equals(nGroup)) {
				logger.debug("subscribing to {} {}", fop, nGroup);
				try {fopEventBusRegister(this, fop);} catch (Exception ex) {}
				try {uiEventBusRegister(this, fop);} catch (Exception ex) {}
				currentFop = fop;
			}
		}
		
	}
	
	/**
	 * @return true if the current group is safe for editing -- i.e. not lifting currently
	 */
	private boolean checkFOP() {
		Collection<FieldOfPlay> fops = OwlcmsFactory.getFOPs();
		FieldOfPlay liftingFop = null;
		search: for (FieldOfPlay fop: fops) {
			if (fop.getGroup() != null && fop.getGroup().equals(currentGroup)) {
				liftingFop = fop;
				break search;
			}
		}
		if (liftingFop != null) {
			Notification.show("This group is currently lifting on platform "+liftingFop.getName()+". You cannot edit the results.", 3000, Position.MIDDLE);
			logger.debug("Group {} lifting on {}, cannot edit", currentGroup, liftingFop);
			subscribeIfLifting(currentGroup);
		} else {
			logger.debug("Group {} lifting on {}, editing", currentGroup, liftingFop);
		}
		return liftingFop != null;
	}
	
	public void setGridGroup(Group group) {
		subscribeIfLifting(group);
		groupFilter.setValue(group);
		refresh();
	}
	
	public Group getGridGroup() {
		return groupFilter.getValue();
	}
	
	/**
	 * Parse the http query parameters
	 * 
	 * Note: because we have the @Route, the parameters are parsed *before* our parent layout is created.
	 * 
	 * @param event Vaadin navigation event
	 * @param parameter null in this case -- we don't want a vaadin "/" parameter. This allows us to add query parameters instead.
	 * 
	 * @see app.owlcms.ui.shared.QueryParameterReader#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
	 */
	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		location = event.getLocation();
		locationUI = event.getUI();		
		QueryParameters queryParameters = location.getQueryParameters();
		Map<String, List<String>> parametersMap = queryParameters.getParameters(); // immutable
		HashMap<String, List<String>> params = new HashMap<String, List<String>>(parametersMap);
		
		logger.debug("parsing query parameters");
		List<String> groupNames = params.get("group");
		if (!isIgnoreGroupFromURL() && groupNames != null && !groupNames.isEmpty()) {
			String groupName = groupNames.get(0);
			currentGroup = GroupRepository.findByName(groupName);
		} else {
			currentGroup = null;
		}
		if (currentGroup != null) {
			params.put("group",Arrays.asList(currentGroup.getName()));
		} else {
			params.remove("group");
		}
		params.remove("fop");
		
		// change the URL to reflect group
		event.getUI().getPage().getHistory().replaceState(null, new Location(location.getPath(),new QueryParameters(params)));
	}
	
	@Override
	public void updateURLLocation(UI ui, Location location, Group newGroup) {
		// change the URL to reflect fop group
		HashMap<String, List<String>> params = new HashMap<String, List<String>>(location.getQueryParameters().getParameters());
		if (!isIgnoreGroupFromURL() && newGroup != null) {
			params.put("group",Arrays.asList(newGroup.getName()));
		} else {
			params.remove("group");
		}
		ui.getPage().getHistory().replaceState(null, new Location(location.getPath(),new QueryParameters(params)));
	}


	@Override
	public boolean isIgnoreGroupFromURL() {
		return false;
	}
}
