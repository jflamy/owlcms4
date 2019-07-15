/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */

package app.owlcms.ui.shared;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.displays.attemptboard.AthleteTimerElement;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsCrudGrid;
import app.owlcms.ui.crudui.OwlcmsGridLayout;
import app.owlcms.ui.lifting.AnnouncerContent;
import app.owlcms.ui.lifting.AthleteCardFormFactory;
import app.owlcms.ui.lifting.MarshallContent;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AthleteGridContent.
 * 
 * Initialization order is - content class is created - wrapping app layout is
 * created if not present - this content is inserted in the app layout slot
 * 
 */
@SuppressWarnings("serial")
public abstract class AthleteGridContent extends VerticalLayout
        implements CrudListener<Athlete>, OwlcmsContent, QueryParameterReader, UIEventProcessor {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteGridContent.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName()); //$NON-NLS-1$
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
    protected H2 attempt;
    protected H2 weight;
    protected AthleteTimerElement timeField;
    protected HorizontalLayout topBar;
    protected ComboBox<Group> groupSelect;

    /**
     * groupFilter points to a hidden field on the crudGrid filtering row, which is
     * slave to the group selection process. this allows us to use the filtering
     * logic used everywhere else to change what is shown in the crudGrid.
     * 
     * In the current implementation groupSelect is readOnly. If it is made
     * editable, it needs to set the value on groupFilter.
     */
    protected ComboBox<Group> groupFilter = new ComboBox<>();
    private String topBarTitle;

    protected TextField lastNameFilter = new TextField();

    /**
     * Bottom part content
     */
    private OwlcmsRouterLayout routerLayout;
    protected OwlcmsCrudGrid<Athlete> crudGrid;
    private AthleteCardFormFactory athleteEditingFormFactory;
    protected Component reset;
    private Athlete displayedAthlete;

    /**
     * Instantiates a new announcer content. Content is created in
     * {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
     */
    public AthleteGridContent() {
        init();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
     */
    @Override
    public Athlete add(Athlete athlete) {
        return getAthleteEditingFormFactory().add(athlete);
    }

    public void closeDialog() {
        crudGrid.getCrudLayout().hideForm();
        crudGrid.getGrid().asSingleSelect().clear();
    }

    /**
     * Gets the crudGrid.
     * 
     * @param crudFormFactory
     *
     * @return the crudGrid crudGrid
     */
    public AthleteCrudGrid createCrudGrid(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
        Grid<Athlete> grid = new Grid<>(Athlete.class, false);
        ThemeList themes = grid.getThemeNames();
        themes.add("compact"); //$NON-NLS-1$
        themes.add("row-stripes"); //$NON-NLS-1$
        grid.addColumn(athlete -> athlete.getLastName().toUpperCase()).setHeader(getTranslation("LastName")); //$NON-NLS-1$
        grid.addColumn("firstName").setHeader(getTranslation("FirstName")); //$NON-NLS-1$ //$NON-NLS-2$
        grid.addColumn("team").setHeader(getTranslation("Team")); //$NON-NLS-1$ //$NON-NLS-2$
        grid.addColumn("category").setHeader(getTranslation("Category")); //$NON-NLS-1$ //$NON-NLS-2$
        grid.addColumn("nextAttemptRequestedWeight").setHeader(getTranslation("Requested_weight")); //$NON-NLS-1$ //$NON-NLS-2$
        // format attempt
        grid.addColumn((a) -> formatAttemptNumber(a), "attemptsDone").setHeader(getTranslation("Attempt")); //$NON-NLS-1$ //$NON-NLS-2$
        grid.addColumn("startNumber").setHeader(getTranslation("StartNumber")); //$NON-NLS-1$ //$NON-NLS-2$

        OwlcmsGridLayout gridLayout = new OwlcmsGridLayout(Athlete.class);
        AthleteCrudGrid crudGrid = new AthleteCrudGrid(Athlete.class, gridLayout, crudFormFactory, grid) {
            @Override
            protected void initToolbar() {
                Component reset = createReset();
                if (reset != null) {
                    crudLayout.addToolbarComponent(reset);
                }
            }

            @Override
            protected void updateButtons() {
            }
        };

        crudGrid.setCrudListener(this);
        crudGrid.setClickRowToUpdate(true);
        crudGrid.getCrudLayout().addToolbarComponent(groupFilter);

        return crudGrid;
    }

    public void createGroupSelect() {
        groupSelect = new ComboBox<>();
        groupSelect.setPlaceholder(getTranslation("Group")); //$NON-NLS-1$
        groupSelect.setItems(GroupRepository.findAll());
        groupSelect.setItemLabelGenerator(Group::getName);
        groupSelect.setWidth("7rem"); //$NON-NLS-1$
        groupSelect.setReadOnly(true);
        // if groupSelect is made read-write, it needs to set values in groupFilter and
        // call updateURLLocation
        // see AnnouncerContent for an example.
    }

    public Component createReset() {
        return null;
    }

    /**
     * Delegate to the form factory which actually implements deletion
     * 
     * @see org.vaadin.crudui.crud.CrudListener#delete(java.lang.Object)
     */
    @Override
    public void delete(Athlete notUsed) {
        getAthleteEditingFormFactory().delete(notUsed);
    }

    /**
     * Get the content of the crudGrid. Invoked by refreshGrid.
     *
     * @see org.vaadin.crudui.crud.CrudListener#findAll()
     */
    @Override
    public Collection<Athlete> findAll() {
        FieldOfPlay fop = OwlcmsSession.getFop();
        if (fop != null) {
            logger.trace("findAll {} {} {}", fop.getName(), fop.getGroup() == null ? null : fop.getGroup().getName(), //$NON-NLS-1$
                    LoggerUtils.whereFrom());
            final String filterValue;
            if (lastNameFilter.getValue() != null) {
                filterValue = lastNameFilter.getValue().toLowerCase();
            } else
                return fop.getDisplayOrder();
            return fop.getLiftingOrder().stream().filter(a -> a.getLastName().toLowerCase().startsWith(filterValue))
                    .collect(Collectors.toList());
        } else {
            // no field of play, no group, empty list
            logger.debug("findAll fop==null"); //$NON-NLS-1$
            return ImmutableList.of();
        }
    }

    /**
     * @return the athleteEditingFormFactory
     */
    public AthleteCardFormFactory getAthleteEditingFormFactory() {
        return athleteEditingFormFactory;
    }

    /**
     * @return the groupFilter
     */
    public ComboBox<Group> getGroupFilter() {
        return groupFilter;
    }

    @Override
    public OwlcmsRouterLayout getRouterLayout() {
        return routerLayout;
    }

    /**
     * Process URL parameters, including query parameters
     * 
     * @see app.owlcms.ui.shared.QueryParameterReader#setParameter(com.vaadin.flow.router.BeforeEvent,
     *      java.lang.String)
     */
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        logger.debug("AthleteGridContent parsing URL"); //$NON-NLS-1$
        QueryParameterReader.super.setParameter(event, parameter);
        location = event.getLocation();
        locationUI = event.getUI();
        // super.setParameter sets the group, but does not reload.
        if (this instanceof AnnouncerContent) {
            OwlcmsSession.withFop(fop -> fop.initGroup(fop.getGroup(), this));
        }
    }

    @Override
    public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
        this.routerLayout = routerLayout;
    }

    @Subscribe
    public void slaveGroupDone(UIEvent.GroupDone e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(), //$NON-NLS-1$
                this.getOrigin(), e.getOrigin());
        OwlcmsSession.withFop((fop) -> {
            UIEventProcessor.uiAccess(topBar, uiEventBus, e, () -> doUpdateTopBar(fop.getCurAthlete(), 0));
        });
        crudGrid.refreshGrid();
    }

    @Subscribe
    public void slaveUpdateAnnouncerBar(UIEvent.LiftingOrderUpdated e) {
        Athlete athlete = e.getAthlete();
        OwlcmsSession.withFop(fop -> {
            // do not send weight change notification if we are the source of the weight
            // change
            UIEventProcessor.uiAccessIgnoreIfSelfOrigin(topBar, uiEventBus, e, e.getOrigin(), this.getOrigin(),
                    () -> warnAnnouncerIfCurrent(e, athlete, fop));
            UIEventProcessor.uiAccess(topBar, uiEventBus, e, () -> doUpdateTopBar(athlete, e.getTimeAllowed()));
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see app.owlcms.ui.group.UIEventProcessor#updateGrid(app.owlcms.fieldofplay.
     * UIEvent.LiftingOrderUpdated)
     */
    @Subscribe
    public void slaveUpdateGrid(UIEvent.LiftingOrderUpdated e) {
        logger.debug("{} {}", e.getOrigin(), LoggerUtils.whereFrom()); //$NON-NLS-1$
        UIEventProcessor.uiAccess(crudGrid, uiEventBus, e, () -> {
            crudGrid.refreshGrid();
        });
    }

    /**
     * @param forceUpdate
     */
    public void syncWithFOP(boolean forceUpdate) {
        logger.debug("syncWithFOP {}", LoggerUtils.whereFrom()); //$NON-NLS-1$
        OwlcmsSession.withFop((fop) -> {
            Group fopGroup = fop.getGroup();
            Group displayedGroup = groupSelect.getValue();
            if (fopGroup == null && displayedGroup == null)
                return;
            if (fopGroup != null && (forceUpdate || !fopGroup.equals(displayedGroup))) {
                groupSelect.setValue(fopGroup);
                if (forceUpdate) {
                    fop.switchGroup(fop.getGroup(), this);
                }
            } else if (fopGroup == null) {
                groupSelect.setValue(null);
            }
            Athlete curAthlete = fop.getCurAthlete();
            int timeRemaining = fop.getAthleteTimer().getTimeRemaining();
            doUpdateTopBar(curAthlete, timeRemaining);
        });
    }

    /*
     * Old kludge code to create HTML. Should recheck whether
     * getElement().setProperty("innerHTML", "...") works now.
     * 
     * // String attemptHtml = MessageFormat.
     * format("<h2>{0} {1}<sup>{1,choice,1#st|2#nd|3#rd}</sup> att.</h2>", // String
     * attemptHtml = MessageFormat.format("<h2>{0} #{1}</h2>", //
     * athlete.getAttemptsDone() > 2 ? "C & J" : "Snatch", //
     * athlete.getAttemptNumber()); // Html newAttempt = new Html(attemptHtml); //
     * topBar.replace(attempt, newAttempt); // attempt = newAttempt;
     * 
     * // Html newAttempt = new Html("<h2><span></span></h2>"); //
     * topBar.replace(attempt, newAttempt); // attempt = newAttempt;
     */

    /**
     * Update button and validation logic is in form factory
     * 
     * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
     */
    @Override
    public Athlete update(Athlete notUsed) {
        return athleteEditingFormFactory.update(notUsed);
    }

    /**
     * Update URL location on explicit group selection
     *
     * @param ui       the ui
     * @param location the location
     * @param newGroup the new group
     */
    public void updateURLLocation(UI ui, Location location, Group newGroup) {
        // change the URL to reflect fop group
        HashMap<String, List<String>> params = new HashMap<>(location.getQueryParameters().getParameters());
        params.put("fop", Arrays.asList(OwlcmsSession.getFop().getName())); //$NON-NLS-1$
        if (newGroup != null && !isIgnoreGroupFromURL()) {
            params.put("group", Arrays.asList(newGroup.getName())); //$NON-NLS-1$
        } else {
            params.remove("group"); //$NON-NLS-1$
        }
        ui.getPage().getHistory().replaceState(null, new Location(location.getPath(), new QueryParameters(params)));
    }

    protected HorizontalLayout announcerButtons(HorizontalLayout announcerBar2) {
        return null;
    }

    /**
     * Define the form used to edit a given athlete.
     * 
     * @return the form factory that will create the actual form on demand
     */
    protected OwlcmsCrudFormFactory<Athlete> createFormFactory() {
        athleteEditingFormFactory = new AthleteCardFormFactory(Athlete.class, this);
        return athleteEditingFormFactory;
    }

    /**
     * The top bar is logically is the master part of a master-detail In the current
     * implementation, the most convenient place to put it is in the top bar which
     * is managed by the layout, but this could change. So we change the surrounding
     * layout from this class. In this way, only one class (the content) listens for
     * events. Doing it the other way around would require multiple layouts, which
     * breaks the idea of a single page app.
     */
    protected void createTopBar() {
        logger.debug("AthleteGridContent creating top bar"); //$NON-NLS-1$
        topBar = getAppLayout().getAppBarElementWrapper();

        title = new H3();
        title.setText(getTopBarTitle());
        title.getStyle().set("margin", "0px 0px 0px 0px") //$NON-NLS-1$ //$NON-NLS-2$
                .set("font-weight", "normal"); //$NON-NLS-1$ //$NON-NLS-2$

        createGroupSelect();

        lastName = new H1();
        lastName.setText("\u2013"); //$NON-NLS-1$
        lastName.getStyle().set("margin", "0px 0px 0px 0px"); //$NON-NLS-1$ //$NON-NLS-2$
        firstName = new H2(""); //$NON-NLS-1$
        firstName.getStyle().set("margin", "0px 0px 0px 0px"); //$NON-NLS-1$ //$NON-NLS-2$
        Div fullName = new Div(lastName, firstName);

        attempt = new H2();
        weight = new H2();
        weight.setText(""); //$NON-NLS-1$

        timeField = new AthleteTimerElement(this);
        H1 time = new H1(timeField);

        HorizontalLayout buttons = announcerButtons(topBar);
        HorizontalLayout decisions = decisionButtons(topBar);
        decisions.setAlignItems(FlexComponent.Alignment.BASELINE);

        topBar.removeAll();
        topBar.setSizeFull();
        topBar.add(title, groupSelect, fullName, attempt, weight, time);
        if (buttons != null)
            topBar.add(buttons);
        if (decisions != null)
            topBar.add(decisions);

        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
        topBar.setAlignSelf(Alignment.CENTER, attempt, weight, time);
        topBar.setFlexGrow(0.5, fullName);
    }

    protected HorizontalLayout decisionButtons(HorizontalLayout announcerBar2) {
        return null;
    }

    /**
     * The filters at the top of the crudGrid
     *
     * @param crudGrid the crudGrid that will be filtered.
     */
    protected void defineFilters(GridCrud<Athlete> crud) {
        lastNameFilter.setPlaceholder("Last name"); //$NON-NLS-1$
        lastNameFilter.setClearButtonVisible(true);
        lastNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        lastNameFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        crud.getCrudLayout().addFilterComponent(lastNameFilter);

        groupFilter.setPlaceholder(getTranslation("Group")); //$NON-NLS-1$
        groupFilter.setItems(GroupRepository.findAll());
        groupFilter.setItemLabelGenerator(Group::getName);
        // hide because the top bar has it
        groupFilter.getStyle().set("display", "none"); //$NON-NLS-1$ //$NON-NLS-2$
        // we do not set the group filter value
        groupFilter.addValueChangeListener(e -> {
            Group newGroup = e.getValue();
            logger.debug("filter switching group to {}", newGroup != null ? newGroup.getName() : null); //$NON-NLS-1$
            OwlcmsSession.withFop((fop) -> {
                fop.switchGroup(newGroup, this.getOrigin());
            });
            crud.refreshGrid();
            updateURLLocation(locationUI, location, newGroup);
        });
        crud.getCrudLayout().addFilterComponent(groupFilter);
    }

    protected void doUpdateTopBar(Athlete athlete, Integer timeAllowed) {
        if (title == null)
            return; // createTopBar has not yet been called;
        displayedAthlete = athlete;
        logger.debug("doUpdateTopBar {}", LoggerUtils.whereFrom()); //$NON-NLS-1$
        OwlcmsSession.withFop(fop -> {
            UIEventProcessor.uiAccess(topBar, uiEventBus, () -> {
                groupSelect.setValue(fop.getGroup());
                if (athlete != null && athlete.getAttemptsDone() < 6) {
                    String lastName2 = athlete.getLastName();
                    lastName.setText(lastName2 != null ? lastName2.toUpperCase() : ""); //$NON-NLS-1$
                    firstName.setText(athlete.getFirstName());
                    timeField.getElement().getStyle().set("visibility", "visible"); //$NON-NLS-1$ //$NON-NLS-2$
                    attempt.setText(formatAttemptNumber(athlete));
                    Integer nextAttemptRequestedWeight = athlete.getNextAttemptRequestedWeight();
                    weight.setText(
                            (nextAttemptRequestedWeight != null ? nextAttemptRequestedWeight.toString() : "\u2013") //$NON-NLS-1$
                                    + "kg"); //$NON-NLS-1$
                } else {
                    lastName.setText(fop.getGroup() == null ? "\u2013" //$NON-NLS-1$
                            : MessageFormat.format(getTranslation("Group_number_done"), fop.getGroup())); //$NON-NLS-1$
                    firstName.setText(""); //$NON-NLS-1$
                    timeField.getElement().getStyle().set("visibility", "hidden"); //$NON-NLS-1$ //$NON-NLS-2$

                    attempt.setText(""); //$NON-NLS-1$
                    weight.setText(""); //$NON-NLS-1$
                }
            });
        });
    }

    protected Object getOrigin() {
        return this;
    }

    protected String getTopBarTitle() {
        return topBarTitle;
    }

    protected void init() {
        OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
        crudGrid = createCrudGrid(crudFormFactory);
        defineFilters(crudGrid);
        fillHW(crudGrid, this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
     * AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        OwlcmsSession.withFop(fop -> {
            // create the top bar.
            createTopBar();
            syncWithFOP(this instanceof AnnouncerContent);
            // we listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        });
    }

    /**
     * @param topBarTitle the topBarTitle to set
     */
    protected void setTopBarTitle(String title) {
        this.topBarTitle = title;
    }

    private String formatAttemptNumber(Athlete a) {
        Integer attemptsDone = a.getAttemptsDone();
        Integer attemptNumber = a.getAttemptNumber();
        return (attemptsDone >= 3)
                ? ((attemptsDone >= 6) ? "done" : MessageFormat.format(getTranslation("C_and_J_number"), attemptNumber)) //$NON-NLS-1$ //$NON-NLS-2$
                : MessageFormat.format(getTranslation("Snatch_number"), attemptNumber); //$NON-NLS-1$
    }

    /**
     * display a warning to other Technical Officials that marshall has changed
     * weight for current athlete
     * 
     * @param e
     * @param athlete
     * @param fop
     */
    private void warnAnnouncerIfCurrent(UIEvent.LiftingOrderUpdated e, Athlete athlete, FieldOfPlay fop) {
        // the athlete currently displayed is not necessarily the fop curAthlete,
        // because the lifting order has been recalculated behind the scenes
        Athlete curDisplayAthlete = displayedAthlete;
        if (curDisplayAthlete != null && curDisplayAthlete.equals(e.getChangingAthlete())
                && e.getOrigin() instanceof MarshallContent) {
            Notification n = new Notification();
            // Notification theme styling is done in
            // META-INF/resources/frontend/styles/shared-styles.html
            n.getElement().getThemeList().add("warning"); //$NON-NLS-1$
            String text = MessageFormat.format(getTranslation("Weight_change_current_athlete"), //$NON-NLS-1$
                    curDisplayAthlete.getFullName());
            n.setDuration(6000);
            n.setPosition(Position.TOP_START);
            Div label = new Div();
            label.getElement().setProperty("innerHTML", text); //$NON-NLS-1$
            label.addClickListener((event) -> n.close());
            label.setSizeFull();
            label.getStyle().set("font-size", "large"); //$NON-NLS-1$ //$NON-NLS-2$
            n.add(label);
            n.open();
        }
    }

}
