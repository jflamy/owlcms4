/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.ui.shared;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsCrudGrid;
import app.owlcms.ui.crudui.OwlcmsGridLayout;
import app.owlcms.ui.lifting.AnnouncerContent;
import app.owlcms.ui.lifting.AthleteCardFormFactory;
import app.owlcms.ui.lifting.JuryContent;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.BreakManagement.CountdownType;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.URLUtils;
import app.owlcms.utils.queryparameters.FOPParameters;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AthleteGridContent.
 *
 * Initialization order is - content class is created - wrapping app layout is created if not present - this content is
 * inserted in the app layout slot
 *
 */
@SuppressWarnings("serial")
@CssImport(value = "./styles/athlete-grid.css")
public abstract class AthleteGridContent extends VerticalLayout
        implements CrudListener<Athlete>, OwlcmsContent, FOPParameters, UIEventProcessor, IAthleteEditing {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteGridContent.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    public static String formatAttemptNumber(Athlete a) {
        Integer attemptsDone = a.getAttemptsDone();
        Integer attemptNumber = a.getAttemptNumber();
        return (attemptsDone >= 3)
                ? ((attemptsDone >= 6) ? Translator.translate("Done")
                        : Translator.translate("C_and_J_number", attemptNumber))
                : Translator.translate("Snatch_number", attemptNumber);
    }

    protected Location location;
    protected UI locationUI;
    protected EventBus uiEventBus;

    /**
     * Top part content
     */
    protected H3 title;
    protected H1 lastName;
    protected Span firstName;
    protected Span startNumber;
    protected H2 attempt;
    protected H2 weight;
    protected AthleteTimerElement timer;
    protected FlexLayout topBar;
    protected ComboBox<Group> topBarGroupSelect;
    private Athlete displayedAthlete;
    protected boolean initialBar;
    protected H3 warning;
    protected Button breakButton;
    protected Button _1min;
    protected Button _2min;

    /*
     * Initial Bar
     */
    protected Button introCountdownButton;
    protected Button startLiftingButton;
    protected Button showResultsButton;

    /**
     * groupFilter points to a hidden field on the crudGrid filtering row, which is slave to the group selection
     * process. this allows us to use the filtering logic used everywhere else to change what is shown in the crudGrid.
     *
     * In the current implementation groupSelect is readOnly. If it is made editable, it needs to set the value on
     * groupFilter.
     */
    private ComboBox<Group> groupFilter = new ComboBox<>();
    protected ComboBox<Gender> genderFilter = new ComboBox<>();
    private String topBarTitle;

    protected TextField lastNameFilter = new TextField();

    /**
     * Bottom part content
     */
    private OwlcmsRouterLayout routerLayout;
    protected OwlcmsCrudGrid<Athlete> crudGrid;
    private AthleteCardFormFactory athleteEditingFormFactory;
    protected Component reset;
    private Group oldGroup = null;
    protected HorizontalLayout buttons;
    protected HorizontalLayout decisions;
    protected HorizontalLayout breaks;
    protected BreakDialog breakDialog;
    private H2 firstNameWrapper;
    protected Button startTimeButton;
    protected Button stopTimeButton;
    private HorizontalLayout topBarLeft;
    protected OwlcmsGridLayout crudLayout;
    private boolean ignoreSwitchGroup;

    // array is used because of Java requires a final;
    long[] previousStartMillis = { 0L };

    /**
     * Instantiates a new announcer content. Content is created in {@link #setParameter(BeforeEvent, String)} after URL
     * parameters are parsed.
     */
    public AthleteGridContent() {
        init();
    }

    /**
     * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
     */
    @Override
    public Athlete add(Athlete athlete) {
        return getAthleteEditingFormFactory().add(athlete);
    }

    public void busyBreakButton() {
        if (breakButton == null) {
//            logger.error("breakButton is null\n{}", LoggerUtils.stackTrace());
            return;
        }
        breakButton.getElement().setAttribute("theme", "primary error");
        breakButton.getStyle().set("color", "white");
        breakButton.getStyle().set("background-color", "var(--lumo-error-color)");
        breakButton.setText(getTranslation("BreakButton.Paused"));
        breakButton.getElement().setAttribute("title", getTranslation("BreakButton.Caption"));
    }

    public void clearVerticalMargins(HasStyle styleable) {
        styleable.getStyle().set("margin-top", "0").set("margin-bottom", "0");
    }

    /**
     * @see app.owlcms.ui.shared.IAthleteEditing#closeDialog()
     */
    @Override
    public void closeDialog() {
        crudLayout.hideForm();
        crudGrid.getGrid().asSingleSelect().clear();
    }

    public HorizontalLayout createTopBarLeft() {
        setTopBarLeft(new HorizontalLayout());
        fillTopBarLeft();
        return getTopBarLeft();
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
            logger.trace("{}findAll {} {}", fop.getLoggingName(),
                    fop.getGroup() == null ? null : fop.getGroup().getName(),
                    LoggerUtils.whereFrom());
            final String filterValue;
            if (lastNameFilter.getValue() != null) {
                filterValue = lastNameFilter.getValue().toLowerCase();
                return fop.getDisplayOrder().stream().filter(a -> a.getLastName().toLowerCase().startsWith(filterValue))
                        .collect(Collectors.toList());
            } else {
                return fop.getDisplayOrder();
            }
        } else {
            // no field of play, no group, empty list
            logger.debug("findAll fop==null");
            return ImmutableList.of();
        }
    }

    @Override
    public OwlcmsCrudGrid<?> getEditingGrid() {
        return crudGrid;
    }

    public H2 getFirstNameWrapper() {
        return firstNameWrapper;
    }

    /**
     * @return the groupFilter
     */
    public ComboBox<Group> getGroupFilter() {
        return groupFilter;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public UI getLocationUI() {
        return locationUI;
    }

    @Override
    public OwlcmsRouterLayout getRouterLayout() {
        return routerLayout;
    }

    public boolean isIgnoreSwitchGroup() {
        return ignoreSwitchGroup;
    }

    public void quietBreakButton(boolean b) {
        breakButton.getStyle().set("color", "var(--lumo-error-color)");
        breakButton.getStyle().set("background-color", "var(--lumo-error-color-10pct)");
        if (b) {
            breakButton.getElement().setAttribute("theme", "secondary error");
            breakButton.setText(getTranslation("BreakButton.JuryDeliberation"));
            breakButton.getElement().setAttribute("title", getTranslation("BreakButton.JuryDeliberation"));
        } else {
            breakButton.getElement().setAttribute("theme", "secondary error icon");
            breakButton.getElement().setAttribute("title", getTranslation("BreakButton.ToStartCaption"));
        }

    }

    public void setFirstNameWrapper(H2 firstNameWrapper) {
        this.firstNameWrapper = firstNameWrapper;
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public void setLocationUI(UI locationUI) {
        this.locationUI = locationUI;
    }

    /**
     * Process URL parameters, including query parameters
     *
     * @see app.owlcms.utils.queryparameters.FOPParameters#setParameter(com.vaadin.flow.router.BeforeEvent,
     *      java.lang.String)
     */
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        logger.debug("AthleteGridContent parsing URL");
        FOPParameters.super.setParameter(event, parameter);
        setLocation(event.getLocation());
        setLocationUI(event.getUI());
    }

    /**
     * @see app.owlcms.ui.shared.AppLayoutAware#setRouterLayout(app.owlcms.ui.shared.OwlcmsRouterLayout)
     */
    @Override
    public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
        this.routerLayout = routerLayout;
    }

    @Subscribe
    public void slaveBreakDone(UIEvent.BreakDone e) {
        UIEventProcessor.uiAccess(topBarGroupSelect, uiEventBus, e, () -> {
            logger.debug("stopping break");
            syncWithFOP(true);
        });
    }

    @Subscribe
    public void slaveBreakStart(UIEvent.BreakStarted e) {
        UIEventProcessor.uiAccess(topBarGroupSelect, uiEventBus, e, () -> {
            if (e.isDisplayToggle()) {
                logger.debug("{} ignoring switch to break", this.getClass().getSimpleName());
                return;
            }

            if (this instanceof AnnouncerContent) {
                logger.debug("starting break {}", LoggerUtils.stackTrace());
            }
            syncWithFOP(true);
        });
    }

    @Subscribe
    public void slaveBroadcast(UIEvent.Broadcast e) {
        UIEventProcessor.uiAccess(topBarGroupSelect, uiEventBus, e, () -> {
            Icon close = VaadinIcon.CLOSE_CIRCLE_O.create();
            close.getStyle().set("margin-left", "2em");
            close.setSize("4em");
            Notification notification = new Notification();
            Label label = new Label();
            label.getElement().setProperty("innerHTML", getTranslation(e.getMessage()));
            HorizontalLayout content = new HorizontalLayout(label, close);
            notification.add(content);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.setDuration(-1);
            close.addClickListener(event -> notification.close());
            notification.setPosition(Position.MIDDLE);
            notification.open();
        });
    }

    @Subscribe
    public void slaveGroupDone(UIEvent.GroupDone e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        OwlcmsSession.withFop((fop) -> {
            UIEventProcessor.uiAccess(topBar, uiEventBus, e, () -> {
                // doUpdateTopBar(fop.getCurAthlete(), 0);
                createInitialBar();
                syncWithFOP(true);
            });
        });

    }

    @Subscribe
    public void slaveSetTimer(UIEvent.SetTime e) {
        // we use stop because it is present on most screens; either button ok for locking
        if (stopTimeButton == null) {
            return;
        }
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(stopTimeButton, uiEventBus, e, this.getOrigin(),
                () -> buttonsTimeStopped());
    }

    @Subscribe
    public void slaveStartLifting(UIEvent.StartLifting e) {
        UIEventProcessor.uiAccess(topBarGroupSelect, uiEventBus, () -> {
            logger.trace("starting lifting");
            syncWithFOP(true);
        });
    }

    @Subscribe
    public void slaveStartTimer(UIEvent.StartTime e) {
        // we use stop because it is present on most screens; either button ok for locking
        if (stopTimeButton == null) {
            return;
        }
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(stopTimeButton, uiEventBus, e, this.getOrigin(),
                () -> buttonsTimeStarted());
    }

    @Subscribe
    public void slaveStopTimer(UIEvent.StopTime e) {
        // we use stop because it is present on most screens; either button ok for locking
        if (stopTimeButton == null) {
            return;
        }
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(stopTimeButton, uiEventBus, e, this.getOrigin(),
                () -> buttonsTimeStopped());
    }

    @Subscribe
    public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(topBarGroupSelect, uiEventBus, e, this, () -> {
            syncWithFOP(true);
            updateURLLocation(getLocationUI(), getLocation(), e.getGroup());
        });
    }

    @Subscribe
    public void slaveUpdateAnnouncerBar(UIEvent.LiftingOrderUpdated e) {
        Athlete athlete = e.getAthlete();
        OwlcmsSession.withFop(fop -> {
            uiEventLogger.trace("slaveUpdateAnnouncerBar in {}  origin {}", this, e.getOrigin());
            // do not send weight change notification if we are the source of the weight
            // change
            UIEventProcessor.uiAccess(topBar, uiEventBus, e, () -> {
                if (e.getOrigin() != this) {
                    warnOthersIfCurrent(e, athlete, fop);
                }
                doUpdateTopBar(athlete, e.getTimeAllowed());
            });
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.ui.group.UIEventProcessor#updateGrid(app.owlcms.fieldofplay. UIEvent.LiftingOrderUpdated)
     */
    @Subscribe
    public void slaveUpdateGrid(UIEvent.LiftingOrderUpdated e) {
        if (crudGrid == null) {
            return;
        }
        logger.debug("{} {}", e.getOrigin(), LoggerUtils.whereFrom());
        UIEventProcessor.uiAccess(crudGrid, uiEventBus, e, () -> {
            crudGrid.refreshGrid();
        });
    }

    /**
     * Update button and validation logic is in form factory
     *
     * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
     */
    @Override
    public Athlete update(Athlete notUsed) {
        return athleteEditingFormFactory.update(notUsed);
    }

    protected abstract HorizontalLayout announcerButtons(FlexLayout topBar2);

    /**
     * @see app.owlcms.ui.shared.AthleteGridContent#breakButtons(com.vaadin.flow.component.orderedlayout.FlexLayout)
     */
    protected HorizontalLayout breakButtons(FlexLayout announcerBar) {

        breakButton = new Button(AvIcons.AV_TIMER.create(), (e) -> {
            OwlcmsSession.withFop(fop -> {
                Athlete curAthlete = fop.getCurAthlete();
                List<Athlete> order = fop.getLiftingOrder();
                BreakType bt;
                FOPState fopState = fop.getState();
                CountdownType ct;
                if (curAthlete == null) {
                    bt = BreakType.BEFORE_INTRODUCTION;
                    ct = CountdownType.TARGET;
                } else if (curAthlete.getAttemptsDone() == 3 && AthleteSorter.countLiftsDone(order) == 0
                        && fopState != FOPState.TIME_RUNNING) {
                    bt = BreakType.FIRST_CJ;
                    ct = CountdownType.DURATION;
                } else if (curAthlete.getAttemptsDone() == 0 && AthleteSorter.countLiftsDone(order) == 0
                        && fopState != FOPState.TIME_RUNNING) {
                    bt = BreakType.FIRST_SNATCH;
                    ct = CountdownType.DURATION;
                } else {
                    bt = BreakType.TECHNICAL;
                    ct = CountdownType.INDEFINITE;
                }
                breakDialog = new BreakDialog(this, bt, ct);
                breakDialog.open();
            });
        });
        return layoutBreakButtons();
    }

    protected void buttonsTimeStarted() {
        if (startTimeButton != null) {
            startTimeButton.getElement().setAttribute("theme", "secondary icon");
        }
        if (stopTimeButton != null) {
            stopTimeButton.getElement().setAttribute("theme", "primary error icon");
        }
    }

    protected void buttonsTimeStopped() {
        if (startTimeButton != null) {
            startTimeButton.getElement().setAttribute("theme", "primary success icon");
        }
        if (stopTimeButton != null) {
            stopTimeButton.getElement().setAttribute("theme", "secondary icon");
        }
    }

    protected void create1minButton() {
        _1min = new Button("1:00", (e) -> {
            OwlcmsSession.withFop(fop -> {
                fop.getFopEventBus().post(new FOPEvent.ForceTime(60000, this.getOrigin()));
            });
        });
        _1min.getElement().setAttribute("theme", "icon");
    }

    protected void create2MinButton() {
        _2min = new Button("2:00", (e) -> {
            OwlcmsSession.withFop(fop -> {
                fop.getFopEventBus().post(new FOPEvent.ForceTime(120000, this.getOrigin()));
            });
        });
        _2min.getElement().setAttribute("theme", "icon");
    }

    /**
     * Gets the crudGrid.
     *
     * @param crudFormFactory
     *
     * @return the crudGrid crudGrid
     */
    protected AthleteCrudGrid createCrudGrid(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
        Grid<Athlete> grid = new Grid<>(Athlete.class, false);
        ThemeList themes = grid.getThemeNames();
        themes.add("compact");
        themes.add("row-stripes");
        grid.addColumn(athlete -> athlete.getLastName().toUpperCase(), "lastName")
                .setHeader(getTranslation("LastName"));
        grid.addColumn("firstName").setHeader(getTranslation("FirstName"));
        grid.addColumn("team").setHeader(getTranslation("Team"));
        grid.addColumn("category").setHeader(getTranslation("Category"));
        grid.addColumn("nextAttemptRequestedWeight").setHeader(getTranslation("Requested_weight"));
        // format attempt
        grid.addColumn((a) -> formatAttemptNumber(a), "attemptsDone").setHeader(getTranslation("Attempt"));
        grid.addColumn("startNumber").setHeader(getTranslation("StartNumber"));

        crudLayout = new OwlcmsGridLayout(Athlete.class);
        AthleteCrudGrid crudGrid = new AthleteCrudGrid(Athlete.class, crudLayout, crudFormFactory, grid) {
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
        crudLayout.addToolbarComponent(getGroupFilter());

        return crudGrid;
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

    protected void createInitialBar() {
        logger.debug("AthleteGridContent creating top bar {}", LoggerUtils.whereFrom());
        topBar = getAppLayout().getAppBarElementWrapper();
        topBar.removeAll();
        initialBar = true;

        createTopBarGroupSelect();
        HorizontalLayout topBarLeft = createTopBarLeft();

        warning = new H3();
        warning.getStyle().set("margin-top", "0");
        warning.getStyle().set("margin-bottom", "0");

        topBar.removeAll();
        topBar.setSizeFull();
        topBar.add(topBarLeft, warning);

        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
        topBar.setFlexGrow(0.0, topBarLeft);
    }

    protected Component createReset() {
        return null;
    }

    protected void createStartTimeButton() {
        startTimeButton = new Button(AvIcons.PLAY_ARROW.create());
        startTimeButton.addClickListener(e -> {
            OwlcmsSession.withFop(fop -> {
                long now = System.currentTimeMillis();
                long timeElapsed = now - previousStartMillis[0];
                boolean running = fop.getAthleteTimer().isRunning();
                if (timeElapsed > 50 && !running) {
                    logger.debug("clock start {}ms running={}", timeElapsed, running);
                    fop.getFopEventBus().post(new FOPEvent.TimeStarted(this.getOrigin()));
                    buttonsTimeStarted();
                } else {
                    logger.debug("discarding duplicate clock start {}ms running={}", timeElapsed, running);
                }
                previousStartMillis[0] = now;
            });
        });
        startTimeButton.getElement().setAttribute("theme", "primary success icon");
    }

    protected void createStopTimeButton() {
        stopTimeButton = new Button(AvIcons.PAUSE.create());
        stopTimeButton.addClickListener(e -> {
            OwlcmsSession.withFop(fop -> {
                fop.getFopEventBus().post(new FOPEvent.TimeStopped(this.getOrigin()));
                buttonsTimeStopped();
            });
        });
        stopTimeButton.getElement().setAttribute("theme", "secondary icon");
    }

    /**
     * The top bar is logically is the master part of a master-detail In the current implementation, the most convenient
     * place to put it is in the top bar which is managed by the layout, but this could change. So we change the
     * surrounding layout from this class. In this way, only one class (the content) listens for events. Doing it the
     * other way around would require multiple layouts, which breaks the idea of a single page app.
     */
    protected void createTopBar() {
        logger.debug("AthleteGridContent creating top bar");
        topBar = getAppLayout().getAppBarElementWrapper();
        topBar.setClassName("athleteGridTopBar");
        topBar.removeAll();
        initialBar = false;

        HorizontalLayout topBarLeft = createTopBarLeft();

        lastName = new H1();
        lastName.setText("\u2013");
        lastName.getStyle().set("margin", "0px 0px 0px 0px");

        setFirstNameWrapper(new H2(""));
        getFirstNameWrapper().getStyle().set("margin", "0px 0px 0px 0px");
        firstName = new Span("");
        firstName.getStyle().set("margin", "0px 0px 0px 0px");
        startNumber = new Span("");
        Style style = startNumber.getStyle();
        style.set("margin", "0px 0px 0px 1em");
        style.set("padding", "0px 0px 0px 0px");
        style.set("border", "2px solid var(--lumo-primary-color)");
        style.set("font-size", "90%");
        style.set("width", "1.4em");
        style.set("text-align", "center");
        style.set("display", "inline-block");
        getFirstNameWrapper().add(firstName, startNumber);
        Div fullName = new Div(lastName, getFirstNameWrapper());

        attempt = new H2();
        weight = new H2();
        weight.setText("");
        if (timer == null) {
            timer = new AthleteTimerElement(this);
        }
        timer.setSilenced(false);
        H1 time = new H1(timer);
        clearVerticalMargins(attempt);
        clearVerticalMargins(time);
        clearVerticalMargins(weight);

        buttons = announcerButtons(topBar);
        breaks = breakButtons(topBar);
        decisions = decisionButtons(topBar);
        decisions.setAlignItems(FlexComponent.Alignment.BASELINE);

        topBar.setSizeFull();
        topBar.add(topBarLeft, fullName, attempt, weight, time);
        if (buttons != null) {
            topBar.add(buttons);
        }
        if (breaks != null) {
            topBar.add(breaks);
        }
        if (decisions != null) {
            topBar.add(decisions);
        }

        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
        topBar.setAlignSelf(Alignment.CENTER, attempt, weight, time);
        topBar.setFlexGrow(0.5, fullName);
        topBar.setFlexGrow(0.0, topBarLeft);
    }

    protected void createTopBarGroupSelect() {
        topBarGroupSelect = new ComboBox<>();
        topBarGroupSelect.setClearButtonVisible(true);
        topBarGroupSelect.setPlaceholder(getTranslation("Group"));
        topBarGroupSelect.setItems(GroupRepository.findAll());
        topBarGroupSelect.setItemLabelGenerator(Group::getName);
        topBarGroupSelect.setWidth("7rem");
        topBarGroupSelect.getStyle().set("margin-left", "1em");
        topBarGroupSelect.setReadOnly(true);
        OwlcmsSession.withFop(fop -> topBarGroupSelect.setValue(fop.getGroup()));

        // if topBarGroupSelect is made read-write, it needs to set values in
        // groupFilter and
        // call updateURLLocation
        // see AnnouncerContent for an example.
    }

    protected HorizontalLayout decisionButtons(FlexLayout topBar2) {
        return null;
    }

    /**
     * The filters at the top of the crudGrid
     *
     * @param crudGrid the crudGrid that will be filtered.
     */
    protected void defineFilters(GridCrud<Athlete> crud) {
        lastNameFilter.setPlaceholder(getTranslation("LastName"));
        lastNameFilter.setClearButtonVisible(true);
        lastNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        lastNameFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        crudLayout.addFilterComponent(lastNameFilter);

        getGroupFilter().setPlaceholder(getTranslation("Group"));
        getGroupFilter().setItems(GroupRepository.findAll());
        getGroupFilter().setItemLabelGenerator(Group::getName);
        // hide because the top bar has it
        getGroupFilter().getStyle().set("display", "none");
        // we do not set the group filter value
        getGroupFilter().addValueChangeListener(e -> {
            UIEventProcessor.uiAccess(getGroupFilter(), uiEventBus, () -> {
                Group newGroup = e.getValue();
                OwlcmsSession.withFop((fop) -> {
                    oldGroup = fop.getGroup();
                    if (newGroup == null && oldGroup == null) {
                        return;
                    }
//                    if ((newGroup == null && oldGroup != null) || !newGroup.equals(oldGroup)) {
//                        logger.debug("filter switching group from {} to {}",
//                                oldGroup != null ? oldGroup.getName() : null,
//                                newGroup != null ? newGroup.getName() : null);
                    if (isIgnoreSwitchGroup()) {
                        // logger.debug("ignoring self-originating change");
                        setIgnoreSwitchGroup(false);
                    } else {
                        setIgnoreSwitchGroup(true); // prevent recursion on self-generated event.
                        // logger.debug("value changed, switching group, from \n{}",LoggerUtils.stackTrace());
                        fop.getFopEventBus().post(new FOPEvent.SwitchGroup(newGroup, this));
                    }
                    oldGroup = newGroup;
//                        // we listen to the UI switch group that will result from the FOP switchgroup
//                    } else {
//                        // loadGroup will emit FOP SwitchGroup which will emit UI switchgroup that we listen to .
                    // logger.debug("{} loading group {} {} {} {} {}", myId, newGroup,
                    // System.identityHashCode(newGroup), oldGroup, System.identityHashCode(oldGroup),
                    // LoggerUtils.stackTrace());
                    // fop.loadGroup(newGroup, this, newGroup != oldGroup);
//                    }
                });
            });
        });
        crudLayout.addFilterComponent(getGroupFilter());
    }

    protected void doUpdateTopBar(Athlete athlete, Integer timeAllowed) {
        if (title == null) {
            return;
        }
        displayedAthlete = athlete;

        OwlcmsSession.withFop(fop -> {
            UIEventProcessor.uiAccess(topBar, uiEventBus, () -> {
                Group group = fop.getGroup();
                topBarGroupSelect.setValue(group); // does nothing if already correct
                Integer attemptsDone = (athlete != null ? athlete.getAttemptsDone() : 0);
                logger.debug("doUpdateTopBar {} {} {}", LoggerUtils.whereFrom(), athlete, attemptsDone);
                if (athlete != null && attemptsDone < 6) {
                    if (!initialBar) {
                        String lastName2 = athlete.getLastName();
                        lastName.setText(lastName2 != null ? lastName2.toUpperCase() : "");
                        String firstName2 = athlete.getFirstName();
                        firstName.setText(firstName2 != null ? firstName2 : "");
                        Integer startNumber2 = athlete.getStartNumber();
                        String startNumberText = (startNumber2 != null && startNumber2 > 0 ? startNumber2.toString()
                                : null);
                        if (startNumberText != null) {
                            startNumber.setText(startNumberText);
                            startNumber.getStyle().set("visibility", "visible");
                            startNumber.getStyle().set("font-size", "normal");
                        } else {
                            startNumber.setText("\u26A0");
                            startNumber.setTitle(getTranslation("StartNumbersNotSet"));
                            startNumber.getStyle().set("visibility", "visible");
                            startNumber.getStyle().set("font-size", "smaller");
                        }
                        timer.getElement().getStyle().set("visibility", "visible");
                        attempt.setText(formatAttemptNumber(athlete));
                        Integer nextAttemptRequestedWeight = athlete.getNextAttemptRequestedWeight();
                        weight.setText(
                                (nextAttemptRequestedWeight != null ? nextAttemptRequestedWeight.toString() : "\u2013")
                                        + getTranslation("KgSymbol"));
                    }
                } else {
                    topBarWarning(group, attemptsDone, fop.getState(), fop.getLiftingOrder());
                }
                if (fop.getState() != FOPState.BREAK && breakDialog != null && breakDialog.isOpened()) {
                    breakDialog.close();
                }
            });
        });
    }

    protected void fillTopBarLeft() {
        title = new H3();
        title.setText(getTopBarTitle());
        title.setClassName("topBarTitle");
        title.getStyle().set("margin-top", "0px").set("margin-bottom", "0px").set("font-weight", "normal");
        getTopBarLeft().add(title, topBarGroupSelect);
        getTopBarLeft().setAlignItems(Alignment.CENTER);
        getTopBarLeft().setPadding(true);
        getTopBarLeft().setId("topBarLeft");
    }

    protected Object getOrigin() {
        return this;
    }

    protected HorizontalLayout getTopBarLeft() {
        return topBarLeft;
    }

    protected String getTopBarTitle() {
        return topBarTitle;
    }

    protected void init() {
        OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
        crudGrid = createCrudGrid(crudFormFactory);
        crudLayout = (OwlcmsGridLayout) crudGrid.getCrudLayout();
        defineFilters(crudGrid);
        fillHW(crudGrid, this);
    }

    protected HorizontalLayout layoutBreakButtons() {
        breakButton.getElement().setAttribute("theme", "secondary error");
        breakButton.getStyle().set("color", "var(--lumo-error-color)");
        breakButton.getStyle().set("background-color", "var(--lumo-error-color-10pct)");
        breakButton.getElement().setAttribute("title", getTranslation("BreakButton.Caption"));

        HorizontalLayout buttons = new HorizontalLayout(breakButton);
        buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
        return buttons;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        logger.debug("attaching {} initial={}", System.identityHashCode(attachEvent.getSource()),
                attachEvent.isInitialAttach());
        OwlcmsSession.withFop(fop -> {
            // create the top bar.
            syncWithFOP(true);
            // we listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        });
    }

    protected void setGroupFilter(ComboBox<Group> groupFilter) {
        this.groupFilter = groupFilter;
    }

    protected void setTopBarLeft(HorizontalLayout topBarLeft) {
        this.topBarLeft = topBarLeft;
    }

    /**
     * @param topBarTitle the topBarTitle to set
     */
    protected void setTopBarTitle(String title) {
        this.topBarTitle = title;
    }

    /**
     */
    protected void syncWithFOP(boolean refreshGrid) {
        OwlcmsSession.withFop((fop) -> {
            Group fopGroup = fop.getGroup();
            logger.debug("syncing FOP, group = {}, {}", fopGroup, LoggerUtils.whereFrom(2));
            createTopBarGroupSelect();

            if (refreshGrid) {
                topBarGroupSelect.setValue(fopGroup);
                if (crudGrid != null) {
                    crudGrid.sort(null);
                    crudGrid.refreshGrid();
                }
            }

            Athlete curAthlete2 = fop.getCurAthlete();
            FOPState state = fop.getState();
            if (state == FOPState.INACTIVE || (state == FOPState.BREAK && fop.getGroup() == null)) {
                logger.debug("initial: {} {} {} {}", state, fop.getGroup(), curAthlete2,
                        curAthlete2 == null ? 0 : curAthlete2.getAttemptsDone());
                createInitialBar();
                warning.setText(getTranslation("IdlePlatform"));
                if (curAthlete2 == null || curAthlete2.getAttemptsDone() >= 6 || fop.getLiftingOrder().size() == 0) {
                    topBarWarning(fop.getGroup(), curAthlete2 == null ? 0 : curAthlete2.getAttemptsDone(),
                            fop.getState(), fop.getLiftingOrder());
                }
            } else {
                logger.debug("active: {}", state);
                createTopBar();
                if (state == FOPState.BREAK) {
                    if (buttons != null) {
                        buttons.setVisible(false);
                    }
                    if (decisions != null) {
                        decisions.setVisible(false);
                    }
                    if (this instanceof JuryContent) {
                        busyBreakButton();
                    } else {
                        busyBreakButton();
                    }

                } else {
                    if (buttons != null) {
                        buttons.setVisible(true);
                    }
                    if (decisions != null) {
                        decisions.setVisible(true);
                    }
                    if (breakButton == null) {
                        logger.debug("breakButton is null\n{}", LoggerUtils.stackTrace());
                        return;
                    }
                    breakButton.setText("");
                    quietBreakButton(this instanceof JuryContent);
                }
                breakButton.setEnabled(true);

                Athlete curAthlete = curAthlete2;
                int timeRemaining = fop.getAthleteTimer().getTimeRemaining();
                doUpdateTopBar(curAthlete, timeRemaining);
            }
        });
    }

    protected void topBarWarning(Group group, Integer attemptsDone, FOPState state, List<Athlete> liftingOrder) {
        if (group == null) {
            String string = getTranslation("NoGroupSelected");
            String text = group == null ? "\u2013" : string;
            if (!initialBar) {
                topBarMessage(string, text);
            } else {
                if (introCountdownButton != null) {
                    introCountdownButton.setVisible(false);
                }
                if (startLiftingButton != null) {
                    startLiftingButton.setVisible(false);
                }
                if (showResultsButton != null) {
                    showResultsButton.setVisible(false);
                }
                warning.setText(string);
            }
        } else if (attemptsDone >= 6) {
            String string = getTranslation("Group_number_done", group.getName());
            String text = group == null ? "\u2013" : string;
            if (!initialBar) {
                topBarMessage(string, text);
            } else {
                if (introCountdownButton != null) {
                    introCountdownButton.setVisible(false);
                }
                if (startLiftingButton != null) {
                    startLiftingButton.setVisible(false);
                }
                if (showResultsButton != null) {
                    showResultsButton.setVisible(true);
                }
                warning.setText(string);
            }
        } else if (liftingOrder.size() == 0) {
            String string = getTranslation("No_weighed_in_athletes");
            String text = group == null ? "\u2013" : string;
            if (!initialBar) {
                topBarMessage(string, text);
            } else {
                if (introCountdownButton != null) {
                    introCountdownButton.setVisible(false);
                }
                if (startLiftingButton != null) {
                    startLiftingButton.setVisible(false);
                }
                if (showResultsButton != null) {
                    showResultsButton.setVisible(false);
                }
                warning.setText(string);
            }
        }
    }

    /**
     * Update URL location on explicit group selection
     *
     * @param ui       the ui
     * @param location the location
     * @param newGroup the new group
     */
    protected void updateURLLocation(UI ui, Location location, Group newGroup) {
        // change the URL to reflect fop group
        HashMap<String, List<String>> params = new HashMap<>(location.getQueryParameters().getParameters());
        params.put("fop", Arrays.asList(URLUtils.urlEncode(OwlcmsSession.getFop().getName())));

        if (newGroup != null && !isIgnoreGroupFromURL()) {
            params.put("group", Arrays.asList(URLUtils.urlEncode(newGroup.getName())));
        } else {
            params.remove("group");
        }
        ui.getPage().getHistory().replaceState(null, new Location(location.getPath(), new QueryParameters(params)));
    }

    /**
     * @return the athleteEditingFormFactory
     */
    private AthleteCardFormFactory getAthleteEditingFormFactory() {
        return athleteEditingFormFactory;
    }

    private void setIgnoreSwitchGroup(boolean b) {
        ignoreSwitchGroup = b;
    }

    private void topBarMessage(String string, String text) {
        lastName.setText(text);
        firstName.setText("");
        timer.getElement().getStyle().set("visibility", "hidden");
        attempt.setText("");
        weight.setText("");
        if (warning != null) {
            warning.setText(string);
        }
    }

    /**
     * display a warning to other Technical Officials that marshall has changed weight for current athlete
     *
     * @param e
     * @param athlete
     * @param fop
     */
    private void warnOthersIfCurrent(UIEvent.LiftingOrderUpdated e, Athlete athlete, FieldOfPlay fop) {
        // the athlete currently displayed is not necessarily the fop curAthlete,
        // because the lifting order has been recalculated behind the scenes
        Athlete curDisplayAthlete = displayedAthlete;
        if (curDisplayAthlete != null && curDisplayAthlete.equals(e.getChangingAthlete())) {
            Notification n = new Notification();
            // Notification theme styling is done in META-INF/resources/frontend/styles/shared-styles.html
            n.getElement().getThemeList().add("warning");
            String text;
            int declaring = curDisplayAthlete.isDeclaring();
            if (declaring > 0) {
                text = getTranslation("Declaration_current_athlete_with_change", curDisplayAthlete.getFullName());
            } else if (declaring == 0) {
                text = getTranslation("Declaration_current_athlete", curDisplayAthlete.getFullName());
            } else {
                text = getTranslation("Weight_change_current_athlete", curDisplayAthlete.getFullName());
            }
            n.setDuration(6000);
            n.setPosition(Position.TOP_START);
            Div label = new Div();
            label.getElement().setProperty("innerHTML", text);
            label.addClickListener((event) -> n.close());
            label.setSizeFull();
            label.getStyle().set("font-size", "large");
            n.add(label);
            n.open();
        }
    }

}
