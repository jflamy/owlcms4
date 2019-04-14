/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 *
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */

package app.owlcms.ui.shared;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.components.crudui.OwlcmsCrudFormFactory;
import app.owlcms.components.crudui.OwlcmsGridCrud;
import app.owlcms.components.crudui.OwlcmsGridLayout;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.displays.attemptboard.TimerElement;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.group.AthleteCardFormFactory;
import app.owlcms.ui.group.UIEventProcessor;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 * 
 * Initialization order is
 * - content class is created
 * - wrapping app layout is created if not present
 * - this content is inserted in the app layout slot
 * 
 */
@SuppressWarnings("serial")
public class AthleteGridContent extends VerticalLayout
implements CrudListener<Athlete>, QueryParameterReader, ContentWrapping, AppLayoutAware, SafeEventBusRegistration, UIEventProcessor {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteGridContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	protected Location location;
	protected UI locationUI;
	protected EventBus uiEventBus;

	/**
	 * Top part content
	 */
	protected H3 title;
	protected H1 lastName;
	protected H2 firstName;
	protected Html attempt;
	protected H2 weight;
	protected TimerElement timeField;
	protected HorizontalLayout topBar;
	protected ComboBox<Group> groupSelect;
	
	/**
	 * groupFilter points to a hidden field on the grid filtering row, which is slave
	 * to the group selection process. this allows us to use the filtering
	 * logic used everywhere else to change what is shown in the grid.
	 * 
	 * In the current implementation groupSelect is readOnly.  If it is made editable,
	 * it needs to set the value on groupFilter.
	 */
	protected ComboBox<Group> groupFilter = new ComboBox<>();
	private String topBarTitle;

	/**
	 * Bottom part content
	 */
	protected GridCrud<Athlete> grid;
	private OwlcmsRouterLayout routerLayout;
	
	/**
	 * Instantiates a new announcer content.
	 * Content is created in {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public AthleteGridContent() {
		logger.debug("AthleteGridContent constructor");
	}
	
	/**
	 * Process URL parameters, including query parameters
	 * @see app.owlcms.ui.shared.QueryParameterReader#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
	 */
	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		logger.debug("AthleteGridContent parsing URL");
		QueryParameterReader.super.setParameter(event, parameter);
		location = event.getLocation();
		locationUI = event.getUI();
		// super.setParamet sets the group, but does not reload.
		OwlcmsSession.withFop(fop -> fop.initGroup(fop.getGroup(), this));
	}

	/**
	 * Update URL location on explicit group selection
	 *
	 * @param ui the ui
	 * @param location the location
	 * @param newGroup the new group
	 */
	public void updateURLLocation(UI ui, Location location, Group newGroup) {
		// change the URL to reflect fop group
		HashMap<String, List<String>> params = new HashMap<>(location.getQueryParameters().getParameters());
		params.put("fop",Arrays.asList(OwlcmsSession.getFop().getName()));
		if (newGroup != null && !isIgnoreGroupFromURL()) {
			params.put("group",Arrays.asList(newGroup.getName()));
		} else {
			params.remove("group");
		}
		ui.getPage().getHistory().replaceState(null, new Location(location.getPath(),new QueryParameters(params)));
	}

	/* (non-Javadoc)
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		grid = getGrid();
		fillHW(grid, this);
		OwlcmsSession.withFop(fop -> {
			// create the top bar.
			createTopBar();
			syncWithFOP(true);
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}

	
	protected String getTopBarTitle() {
		return topBarTitle;
	}

	/**
	 * @param topBarTitle the topBarTitle to set
	 */
	protected void setTopBarTitle(String title) {
		this.topBarTitle = title;
	}
	
	/**
	 * The top bar is logically is the master part of a master-detail
	 * In the current implementation, the most convenient place to put it is in the top bar
	 * which is managed by the layout, but this could change. So we change the surrounding layout
	 * from this class.  In this way, only one class (the content) listens for events.
	 * Doing it the other way around would require multiple layouts, which breaks the idea of
	 * a single page app.
	 */
	protected void createTopBar() {
		logger.debug("AthleteGridContent creating top bar");
		topBar = getAppLayout().getAppBarElementWrapper();

		
		title = new H3();
		title.setText(getTopBarTitle());
		title.getStyle()
			.set("margin", "0px 0px 0px 0px")
			.set("font-weight", "normal");

		createGroupSelect();

		lastName = new H1();
		lastName.setText("\u2013");
		lastName.getStyle().set("margin", "0px 0px 0px 0px");
		firstName = new H2("");
		firstName.getStyle().set("margin", "0px 0px 0px 0px");
		Div fullName = new Div(lastName,firstName);

		attempt = new Html("<h2><span></span></h2");
		weight = new H2();
		weight.setText("");

		timeField = new TimerElement();
		timeField.setTimeRemaining(0);
		H1 time = new H1(timeField);

		HorizontalLayout buttons = announcerButtons(topBar);
		HorizontalLayout decisions = decisionButtons(topBar);
		decisions.setAlignItems(FlexComponent.Alignment.BASELINE);

		topBar.removeAll();
//		topBar.getElement()
//			.getStyle()
//			.set("flex", "100 1");
		topBar.setSizeFull();
		topBar.add(title, groupSelect, fullName, attempt, weight, time);
		if (buttons != null) topBar.add(buttons);
		if (decisions != null) topBar.add(decisions);
		
		topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
		topBar.setAlignItems(FlexComponent.Alignment.CENTER);
		topBar.setAlignSelf(Alignment.CENTER, attempt, weight, time);
		topBar.setFlexGrow(0.5, fullName);
	}

	public void createGroupSelect() {
		groupSelect = new ComboBox<>();
		groupSelect.setPlaceholder("Select Group");
		groupSelect.setItems(GroupRepository.findAll());
		groupSelect.setItemLabelGenerator(Group::getName);
		groupSelect.setWidth("8rem");
		groupSelect.setReadOnly(true);
		// if groupSelect is made read-write, it needs to set groupFilter and call updateURLLocation
	}


	@Subscribe
	public void setTime(UIEvent.SetTime e) {
		UIEventProcessor.uiAccess(topBar, uiEventBus, e, () -> {
			Integer timeRemaining = e.getTimeRemaining();
			timeField.setTimeRemaining(timeRemaining);
		});
	}

	@Subscribe
	public void startTime(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(topBar, uiEventBus, e, () -> {
			Integer timeRemaining = e.getTimeRemaining();
			timeField.setTimeRemaining(timeRemaining);
		});
	}

	@Subscribe
	public void stopTime(UIEvent.StopTime e) {
		UIEventProcessor.uiAccess(topBar, uiEventBus, e, () -> {
			Integer timeRemaining = e.getTimeRemaining();
			timeField.setTimeRemaining(timeRemaining);
		});
	}

	@Subscribe
	public void updateAnnouncerBar(UIEvent.LiftingOrderUpdated e) {
		UIEventProcessor.uiAccess(topBar, uiEventBus, e, () -> {
			Athlete athlete = e.getAthlete();
			Integer timeAllowed = e.getTimeAllowed();
			doUpdateTopBar(athlete, timeAllowed);
		});
	}

	protected void doUpdateTopBar(Athlete athlete, Integer timeAllowed) {
		logger.debug("doUpdateTopBar {}",LoggerUtils.whereFrom());
		OwlcmsSession.withFop(fop -> {
			groupSelect.setValue(fop.getGroup());
		});
		if (athlete != null) {
			lastName.setText(athlete.getLastName());
			firstName.setText(athlete.getFirstName());
			timeField.setTimeRemaining(timeAllowed);
			Html newAttempt = new Html(
				"<h2>" + athlete.getAttemptNumber() + "<sup>st</sup> att.</h2>");
			topBar.replace(attempt, newAttempt);
			attempt = newAttempt;
			Integer nextAttemptRequestedWeight = athlete.getNextAttemptRequestedWeight();
			weight.setText((nextAttemptRequestedWeight != null ? nextAttemptRequestedWeight.toString() :"\u2013")+ "kg");
		} else {
			lastName.setText("\u2013");
			firstName.setText("");
			Html newAttempt = new Html("<h2><span></span></h2>");
			topBar.replace(attempt, newAttempt);
			attempt = newAttempt;
			weight.setText("");
		}
	}

	public void syncWithFOP(boolean forceUpdate) {
		logger.debug("syncWithFOP {}",LoggerUtils.whereFrom());
		OwlcmsSession.withFop((fop) -> {
			Group fopGroup = fop.getGroup();
			Group displayedGroup = groupSelect.getValue();
			if (fopGroup == null && displayedGroup == null) return;
			if (fopGroup != null && (forceUpdate || ! fopGroup.equals(displayedGroup))) {
				groupSelect.setValue(fopGroup);
				if (forceUpdate) {
					fop.switchGroup(fop.getGroup(), this);
				}
			} else if (fopGroup == null) {
				groupSelect.setValue(null);
			}
			Athlete curAthlete = fop.getCurAthlete();
			int timeRemaining = fop.getTimer().getTimeRemaining();
			doUpdateTopBar(curAthlete, timeRemaining);
		});
	}

	protected HorizontalLayout decisionButtons(HorizontalLayout announcerBar2) {
		return null;
	}

	protected HorizontalLayout announcerButtons(HorizontalLayout announcerBar2) {
		return null;
	}

	public void shrinkTitle(AppLayout appLayout) {
		appLayout.getTitleWrapper()
		.getElement()
		.getStyle()
		.set("flex", "0 1 0px");
	}

	/* (non-Javadoc)
	 * @see app.owlcms.ui.group.UIEventProcessor#updateGrid(app.owlcms.fieldofplay.UIEvent.LiftingOrderUpdated)
	 */
	@Subscribe
	public void updateGrid(UIEvent.LiftingOrderUpdated e) {
		UIEventProcessor.uiAccess(grid, uiEventBus, e, () -> {
			grid.refreshGrid();
		});
	}

	/**
	 * Gets the grid.
	 *
	 * @return the grid grid
	 */
	public GridCrud<Athlete> getGrid() {
		OwlcmsCrudFormFactory<Athlete> formFactory = new AthleteCardFormFactory(Athlete.class);

		Grid<Athlete> grid = new Grid<>(Athlete.class, false);
		ThemeList themes = grid.getThemeNames();
		themes.add("compact");
		themes.add("row-stripes");
		grid.setColumns("lastName", "firstName", "team", "category", "nextAttemptRequestedWeight", "attemptNumber", "startNumber");
		grid.getColumnByKey("lastName")
		.setHeader("Last Name");
		grid.getColumnByKey("firstName")
		.setHeader("First Name");
		grid.getColumnByKey("team")
		.setHeader("Team");
		grid.getColumnByKey("category")
		.setHeader("Category");
		grid.getColumnByKey("nextAttemptRequestedWeight")
		.setHeader("Requested Weight");
		grid.getColumnByKey("attemptNumber")
		.setHeader("Attempt");
		grid.getColumnByKey("startNumber")
		.setHeader("Start Number");

		OwlcmsGridLayout gridLayout = new OwlcmsGridLayout(Athlete.class);
		GridCrud<Athlete> crud = new OwlcmsGridCrud<Athlete>(Athlete.class,
				gridLayout,
				formFactory,
				grid) {
			@Override
			protected void initToolbar() {}
			@Override
			protected void updateButtons() {}
		};

		groupFilter.setPlaceholder("Group");
		groupFilter.setItems(GroupRepository.findAll());
		groupFilter.setItemLabelGenerator(Group::getName);
		// hide because the top bar has it
		groupFilter.getStyle().set("display", "none");
		// we do not set the group filter value
		groupFilter.addValueChangeListener(e -> {
			Group newGroup = e.getValue();
			logger.debug("filter switching group to {}",newGroup != null ? newGroup.getName() : null);
			OwlcmsSession.withFop((fop) -> {
				fop.switchGroup(newGroup, this.getOrigin());
			});
			crud.refreshGrid();
			updateURLLocation(locationUI, location, newGroup);
		});

		crud.setCrudListener(this);
		crud.setClickRowToUpdate(true);
		crud.getCrudLayout().addToolbarComponent(groupFilter);

		return crud;
	}
	
	protected Object getOrigin() {
		return this;
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
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object) */
	@Override
	public Athlete update(Athlete Athlete) {
		Athlete savedAthlete = AthleteRepository.save(Athlete);
		FieldOfPlay fop = (FieldOfPlay) OwlcmsSession.getAttribute("fop");
		fop.getFopEventBus()
			.post(new FOPEvent.WeightChange(this.getOrigin(), savedAthlete));
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
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop != null) {
			logger.debug("findAll {} {} {}",fop.getName(), fop.getGroup() == null ? null : fop.getGroup().getName(), LoggerUtils.whereFrom());
			return fop.getLiftingOrder();
		} else {
			// no field of play, no group, empty list
			logger.debug("findAll fop==null");
			return ImmutableList.of();
		}
	}

	/**
	 * @return the groupFilter
	 */
	public ComboBox<Group> getGroupFilter() {
		return groupFilter;
	}

	public void refresh() {
		grid.refreshGrid();
	}
	
	@Override
	public OwlcmsRouterLayout getRouterLayout() {
		return routerLayout;
	}

	@Override
	public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
		this.routerLayout = routerLayout;
	}

}
