/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.shared;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.apputils.queryparameters.ContentParameters;
import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.elements.JuryDisplayDecisionElement;
import app.owlcms.components.elements.TimerElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.IBreakTimer;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.lifting.AnnouncerContent;
import app.owlcms.nui.lifting.AthleteCardFormFactory;
import app.owlcms.nui.lifting.MarshallContent;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.IdUtils;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.NaturalOrderComparator;
import app.owlcms.utils.URLUtils;
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
@CssImport(value = "./styles/athlete-grid.css")
public abstract class AthleteGridContent extends VerticalLayout
        implements CrudListener<Athlete>, OwlcmsContent, ContentParameters, UIEventProcessor, IAthleteEditing,
        BreakDisplay {

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

	private static String computeLift(int i, Athlete a) {
		Integer lift = a.getActualLiftOrNull(i);
		Integer attemptsDone = a.getAttemptsDone();
		if (i == (attemptsDone + 1)) {
			return a.getRequestedWeightForAttempt(i).toString();
		} else if (lift == null) {
			return ("\u00a0");
		} else if (lift > 0) {
			return lift.toString();
		} else if (lift == 0) {
			return "-";
		} else {
			return "(" + Math.abs(lift) + ")";
		}
	}

	private static String computeLiftClass(int i, Athlete a) {
		Integer lift = a.getActualLiftOrNull(i);
		Integer attemptsDone = a.getAttemptsDone();
		if (i == (attemptsDone + 1)) {
			return a == OwlcmsSession.getFop().getCurAthlete() ? "yellow" : "next";
		} else if (lift == null) {
			return ("gray");
		} else if (lift > 0) {
			return "green";
		} else if (lift == 0) {
			return "red";
		} else {
			return "red";
		}
	}

	private static String computeNameClass(Athlete a) {
		return a == OwlcmsSession.getFop().getCurAthlete() ? "bold" : "";
	}

	private static Renderer<Athlete> createAttemptsRenderer() {
		return LitRenderer.<Athlete>of(
		        "<vaadin-horizontal-layout>" +
		                "<span class='${item.sn1class}'>${item.sn1}</span>" +
		                "<span class='${item.sn2class}'>${item.sn2}</span>" +
		                "<span class='${item.sn3class}'>${item.sn3}</span>" +
		                "<span class='spacer'>\u00a0\u00a0\u00a0</span>" +
		                "<span class='${item.cj1class}'>${item.cj1}</span>" +
		                "<span class='${item.cj2class}'>${item.cj2}</span>" +
		                "<span class='${item.cj3class}'>${item.cj3}</span>" +
		                "</vaadin-horizontal-layout>")
		        .withProperty("sn1", (a) -> computeLift(1, a))
		        .withProperty("sn2", (a) -> computeLift(2, a))
		        .withProperty("sn3", (a) -> computeLift(3, a))
		        .withProperty("cj1", (a) -> computeLift(4, a))
		        .withProperty("cj2", (a) -> computeLift(5, a))
		        .withProperty("cj3", (a) -> computeLift(6, a))
		        .withProperty("sn1class", (a) -> computeLiftClass(1, a))
		        .withProperty("sn2class", (a) -> computeLiftClass(2, a))
		        .withProperty("sn3class", (a) -> computeLiftClass(3, a))
		        .withProperty("cj1class", (a) -> computeLiftClass(4, a))
		        .withProperty("cj2class", (a) -> computeLiftClass(5, a))
		        .withProperty("cj3class", (a) -> computeLiftClass(6, a));
	}

	private static Renderer<Athlete> createFirstNameRenderer() {
		return LitRenderer.<Athlete>of(
		        "<div class='${item.nameClass}'>${item.name}</div>")
		        .withProperty("nameClass", (a) -> computeNameClass(a))
		        .withProperty("name", Athlete::getFirstName);
	}

	private static Renderer<Athlete> createLastNameRenderer() {
		return LitRenderer.<Athlete>of(
		        "<div class='${item.nameClass}'>${item.name}</div>")
		        .withProperty("nameClass", (a) -> computeNameClass(a))
		        .withProperty("name", a -> a.getLastName().toUpperCase());
	}

	protected TimerElement breakTimerElement;
	
	protected Button _1min;
	protected Button _2min;
	protected H2 attempt;
	protected Button breakButton;
	protected BreakDialog breakDialog;
	protected HorizontalLayout breaks;
	protected HorizontalLayout buttons;
	protected OwlcmsCrudGrid<Athlete> crudGrid;
	protected OwlcmsGridLayout crudLayout;
	protected JuryDisplayDecisionElement decisionDisplay;
	protected HorizontalLayout decisions;
	protected Span firstName;

	protected ComboBox<Gender> genderFilter = new ComboBox<>();
	protected boolean initialBar;
	/*
	 * Initial Bar
	 */
	protected Button introCountdownButton;

	protected H2 lastName;
	protected TextField lastNameFilter = new TextField();
	protected Location location;

	protected UI locationUI;

	protected Component reset;
	protected Button showResultsButton;
	protected Button startLiftingButton;
	protected Span startNumber;
	protected Button startTimeButton;
	protected Button stopTimeButton;
	protected TimerElement timer;
	/**
	 * Top part content
	 */
	protected H4 title;
	protected FlexLayout topBar;
	protected MenuBar topBarMenu;
	protected MenuBar topBarSettings;
	// protected ComboBox<Group> this;
	protected EventBus uiEventBus;

	protected H3 warning;

	protected H2 weight;
	private AthleteCardFormFactory athleteEditingFormFactory;
	private Athlete displayedAthlete;
	private H3 firstNameWrapper;
	/**
	 * groupFilter points to a hidden field on the crudGrid filtering row, which is
	 * slave to the group selection process. this allows us to use the filtering
	 * logic used everywhere else to change what is shown in the crudGrid.
	 *
	 * In the current implementation groupSelect is readOnly. If it is made
	 * editable, it needs to set the value on groupFilter.
	 */
	private ComboBox<Group> groupFilter = new ComboBox<>();
	private Long id;
	private boolean ignoreSwitchGroup;
	// array is used because of Java requires a final;
	private long previousStartMillis = 0L;
	private long previousStopMillis = 0L;
	/**
	 * Bottom part content
	 */
	private OwlcmsLayout routerLayout;
	private boolean silenced = true;

	private HorizontalLayout topBarLeft;

	private String topBarTitle;

	private HorizontalLayout attempts;

	private Integer prevWeight;

	private boolean summonNotificationSent;

	private boolean deliberationNotificationSent;
	private long previousToggleMillis;

	/**
	 * Instantiates a new announcer content. Content is created in
	 * {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public AthleteGridContent() {
		init();
		breakTimerElement = new BreakTimerElement();
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
//            logger.trace("breakButton is null\n{}", LoggerUtils. stackTrace());
			return;
		}
		breakButton.getElement().setAttribute("theme", "primary error");
		breakButton.getStyle().set("color", "white");
		breakButton.getStyle().set("background-color", "var(--lumo-error-color)");
		// breakButton.setText(getTranslation("BreakButton.Paused"));
		OwlcmsSession.withFop(fop -> {
			IBreakTimer breakTimer = fop.getBreakTimer();
			if (!breakTimer.isIndefinite()) {
				BreakTimerElement bte = (BreakTimerElement) getBreakTimerElement();
				bte.syncWithFopTimer();
				bte.setParent(this.getClass().getSimpleName() + "_" + id);
				breakButton.setIcon(bte);
				breakButton.setIconAfterText(true);
			}

			if (fop.getCeremonyType() != null) {
				breakButton.setText(getTranslation("CeremonyType." + fop.getCeremonyType()));
			} else {
				BreakType breakType = fop.getBreakType();
				if (breakType != null) {
					breakButton.setText(getTranslation("BreakType." + breakType) + "\u00a0\u00a0");
				} else {
					logger.error("null break type {}", LoggerUtils.stackTrace());
					breakButton.setText(getTranslation("BreakButton.Paused") + "\u00a0\u00a0");
				}
			}
		});

	}

	public void clearVerticalMargins(HasStyle styleable) {
		styleable.getStyle().set("margin-top", "0").set("margin-bottom", "0");
	}

	/**
	 * @see app.owlcms.nui.shared.IAthleteEditing#closeDialog()
	 */
	@Override
	public void closeDialog() {
		crudLayout.hideForm();
		crudGrid.getGrid().asSingleSelect().clear();
	}

	/**
	 * Used by the TimeKeeper and TechnicalController classes that abusively inherit
	 * from this class (they don't actually have a grid)
	 *
	 * @see app.owlcms.nui.shared.AthleteGridContent#createTopBar()
	 */
	@Override
	public FlexLayout createMenuArea() {
		topBar = new FlexLayout();
		topBar.setClassName("athleteGridTopBar");
		initialBar = false;

		HorizontalLayout topBarLeft = createTopBarLeft();

		lastName = new H2();
		lastName.setText("\u2013");
		lastName.getStyle().set("margin", "0px 0px 0px 0px");

		setFirstNameWrapper(new H3(""));
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
		startNumber.setVisible(false);
		getFirstNameWrapper().add(firstName, startNumber);
		Div fullName = new Div(lastName, getFirstNameWrapper());

		attempt = new H2();
		weight = new H2();
		weight.setText("");
		if (timer == null) {
			timer = new AthleteTimerElement(this);
		}
		timer.setSilenced(this.isSilenced());
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
		return topBar;
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
	 * @see app.owlcms.uievents.BreakDisplay#doBreak(app.owlcms.uievents.UIEvent)
	 */
	@Override
	public void doBreak(UIEvent event) {
		if (event instanceof UIEvent.BreakStarted) {
			UIEvent.BreakStarted e = (UIEvent.BreakStarted) event;
			if (breakButton != null) {
				breakButton.setText(getTranslation("BreakType." + e.getBreakType()) + "\u00a0\u00a0");
			}
		}

	}

	/**
	 * @see app.owlcms.uievents.BreakDisplay#doCeremony(app.owlcms.uievents.UIEvent.CeremonyStarted)
	 */
	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		if (breakButton != null) {
			breakButton.setText(getTranslation("CeremonyType." + e.getCeremonyType()) + "\u00a0\u00a0");
		}
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

	public H3 getFirstNameWrapper() {
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
	public OwlcmsLayout getRouterLayout() {
		return routerLayout;
	}

//    @Override
//    public void setHeaderContent() {
//        routerLayout.setMenuTitle(getPageTitle());
//        routerLayout.setMenuArea(createMenuArea());
//        routerLayout.showLocaleDropdown(false);
//        routerLayout.setDrawerOpened(false);
//        routerLayout.updateHeader(false);
//    }

	public boolean isIgnoreSwitchGroup() {
		return ignoreSwitchGroup;
	}

	@Override
	public boolean isShowInitialDialog() {
		return false;
	}

	@Override
	public boolean isSilenced() {
		return this.silenced;
	}

	public void quietBreakButton(String caption) {
		breakButton.getStyle().set("color", "var(--lumo-error-color)");
		breakButton.getStyle().set("background-color", "var(--lumo-error-color-10pct)");
		if (caption != null) {
			breakButton.getElement().setAttribute("theme", "secondary error");
			breakButton.setText(caption);
			breakButton.getElement().setAttribute("title", caption);
		} else {
			breakButton.getElement().setAttribute("theme", "secondary error icon");
			breakButton.getElement().setAttribute("title", getTranslation("BreakButton.ToStartCaption"));
		}

	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#readParams(com.vaadin.flow.router.Location,
	 *      java.util.Map)
	 */
	@Override
	public HashMap<String, List<String>> readParams(Location location,
	        Map<String, List<String>> parametersMap) {
		// handle FOP and Group by calling superclass
		HashMap<String, List<String>> params = ContentParameters.super.readParams(location, parametersMap);

		List<String> silentParams = params.get(SILENT);
		// silent is the default. silent=false will cause sound
		boolean silentMode = silentParams == null || silentParams.isEmpty()
		        || silentParams.get(0).toLowerCase().equals("true");
		switchSoundMode(this, silentMode, false);
		updateParam(params, SILENT, !isSilenced() ? "false" : null);
		setUrlParameterMap(params);
		return params;
	}

	public void setFirstNameWrapper(H3 firstNameWrapper) {
		this.firstNameWrapper = firstNameWrapper;
	}

	public void setIgnoreSwitchGroup(boolean b) {
		ignoreSwitchGroup = b;
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
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#setParameter(com.vaadin.flow.router.BeforeEvent,
	 *      java.lang.String)
	 */
	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		logger.debug("AthleteGridContent parsing URL");
		ContentParameters.super.setParameter(event, parameter);
		setLocation(event.getLocation());
		setLocationUI(event.getUI());
	}

	/**
	 * @see app.owlcms.nui.shared.OwlcmsLayoutAware#setRouterLayout(app.owlcms.nui.shared.OwlcmsLayout)
	 */
	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}

	@Override
	public void setShowInitialDialog(boolean b) {
	}

	@Override
	public void setSilenced(boolean silent) {
		// logger.trace("{} {} {}",this.getClass().getSimpleName(), silent,
		// LoggerUtils.whereFrom());
		this.silenced = silent;
	}

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			logger.debug("stopping break");
			syncWithFOP(true);
		});
	}

	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		summonNotificationSent = false;
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			if (e.isDisplayToggle()) {
				logger.debug("{} ignoring switch to break", this.getClass().getSimpleName());
				return;
			}

			if (this instanceof AnnouncerContent) {
				// logger.trace("%%%%%%%%%%%%%% starting break {}",
				// LoggerUtils./**/stackTrace());
			}
			syncWithFOP(true);
		});
	}

	@Subscribe
	public void slaveBroadcast(UIEvent.Broadcast e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			Icon close = VaadinIcon.CLOSE_CIRCLE_O.create();
			close.getStyle().set("margin-left", "2em");
			close.setSize("4em");
			Notification notification = new Notification();
			NativeLabel label = new NativeLabel();
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
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			syncWithFOP(true);
		});
	}

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			doCeremony(e);
		});
	}

	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		OwlcmsSession.withFop((fop) -> {
			UIEventProcessor.uiAccess(topBar, uiEventBus, e, () -> {
				// doUpdateTopBar(fop.getCurAthlete(), 0);
				getRouterLayout().setMenuArea(createInitialBar());
				syncWithFOP(true);
			});
		});

	}

	@Subscribe
	public void slaveJuryNotification(UIEvent.JuryNotification e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			String text = "";
			String reversalText = "";
			if (e.getReversal() != null) {
				reversalText = e.getReversal() ? Translator.translate("JuryNotification.Reversal")
				        : Translator.translate("JuryNotification.Confirmed");
			}
			String style = "warning";
			int previousAttemptNo;
			JuryDeliberationEventType et = e.getDeliberationEventType();

			// logger.debug("slaveJuryNotification {} {} {}", et,
			// e.getDeliberationEventType(), e.getTrace());
			switch (et) {
			case CALL_REFEREES:
				text = Translator.translate("JuryNotification." + et.name());
				if (!summonNotificationSent) {
					doNotification(text, style);
				}
				summonNotificationSent = true;
				return;
			case START_DELIBERATION:
				text = Translator.translate("JuryNotification." + et.name());
				if (!deliberationNotificationSent) {
					doNotification(text, style);
				}
				deliberationNotificationSent = true;
				return;
			case CHALLENGE:
				text = Translator.translate("JuryNotification." + et.name());
				if (!deliberationNotificationSent) {
					doNotification(text, style);
				}
				deliberationNotificationSent = true;
				return;
			case END_CALL_REFEREES:
			case END_DELIBERATION:
			case END_TECHNICAL_PAUSE:
			case END_CHALLENGE:
				text = Translator.translate("JuryNotification." + et.name());
				break;
			case BAD_LIFT:
				previousAttemptNo = e.getAthlete().getAttemptsDone() - 1;
				text = Translator.translate("JuryNotification.BadLift", reversalText, e.getAthlete().getFullName(),
				        previousAttemptNo % 3 + 1);
				style = "primary error";
				break;
			case CALL_TECHNICAL_CONTROLLER:
				text = Translator.translate("JuryNotification.CallTechnicalController");
				break;
			case GOOD_LIFT:
				previousAttemptNo = e.getAthlete().getAttemptsDone() - 1;
				text = Translator.translate("JuryNotification.GoodLift", reversalText, e.getAthlete().getFullName(),
				        previousAttemptNo % 3 + 1);
				style = "primary success";
				break;
			case LOADING_ERROR:
				text = Translator.translate("JuryNotification.LoadingError");
				break;
			case END_JURY_BREAK:
				summonNotificationSent = false;
				deliberationNotificationSent = false;
				text = Translator.translate("JuryNotification.END_JURY_BREAK");
				break;
			case TECHNICAL_PAUSE:
				text = Translator.translate("BreakType.TECHNICAL");
				break;
			case MARSHALL:
				text = Translator.translate("BreakType.MARSHAL");
				break;
			default:
				break;
			}
			doNotification(text, style);
		});
	}

	@Subscribe
	public void slaveNotification(UIEvent.Notification e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			String fopEventString = e.getFopEventString();
			if (fopEventString != null && fopEventString.contentEquals("TimeStarted")) {
				// time started button was selected, but denied. reset the colors
				// to show that time is not running.
				buttonsTimeStopped();
			}
			e.doNotification();
		});
	}

	@Subscribe
	public void slaveSetTimer(UIEvent.SetTime e) {
		// we use stop because it is present on most screens; either button ok for
		// locking
		if (stopTimeButton == null) {
			return;
		}
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(stopTimeButton, uiEventBus, e, this.getOrigin(),
		        () -> buttonsTimeStopped());
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			logger.trace("starting lifting");
			syncWithFOP(true);
			summonNotificationSent = false;
			deliberationNotificationSent = false;
		});
	}

	@Subscribe
	public void slaveStartTimer(UIEvent.StartTime e) {
		// we use stop because it is present on most screens; either button ok for
		// locking
		if (stopTimeButton == null) {
			return;
		}
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(stopTimeButton, uiEventBus, e, this.getOrigin(),
		        () -> buttonsTimeStarted());
	}

	@Subscribe
	public void slaveStopTimer(UIEvent.StopTime e) {
		// we use stop because it is present on most screens; either button ok for
		// locking
		if (stopTimeButton == null) {
			return;
		}
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(stopTimeButton, uiEventBus, e, this.getOrigin(),
		        () -> buttonsTimeStopped());
	}

	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			syncWithFOP(true);
			updateURLLocation(getLocationUI(), getLocation(), e.getGroup());
		});
	}

	@Subscribe
	public void slaveUpdateAnnouncerBar(UIEvent.LiftingOrderUpdated e) {
		Athlete athlete = e.getAthlete();
		OwlcmsSession.withFop(fop -> {
			// uiEventLogger.debug("slaveUpdateAnnouncerBar in {} origin {}", this,
			// LoggerUtils. stackTrace());
			// do not send weight change notification if we are the source of the weight
			// change
			// logger.debug("slaveUpdateAnnouncerBar {}\n=======\n {}",
			// LoggerUtils.stackTrace(), e.getTrace());
			UIEventProcessor.uiAccess(topBar, uiEventBus, e, () -> {
				warnOthersIfCurrent(e, athlete, fop);
				doUpdateTopBar(athlete, e.getTimeAllowed());
			});
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see app.owlcms.nui.group.UIEventProcessor#updateGrid(app.owlcms.fieldofplay.
	 * UIEvent.LiftingOrderUpdated)
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

	/**
	 * @return the athleteEditingFormFactory
	 */
	private AthleteCardFormFactory getAthleteEditingFormFactory() {
		return athleteEditingFormFactory;
	}

	private TimerElement getBreakTimerElement() {
		return this.breakTimerElement;
	}

	private void hideButtons() {
		if (introCountdownButton != null) {
			introCountdownButton.setVisible(false);
		}
		if (startLiftingButton != null) {
			startLiftingButton.setVisible(false);
		}
		if (showResultsButton != null) {
			showResultsButton.setVisible(false);
		}
		if (startNumber != null) {
			startNumber.setVisible(false);
		}
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
	 * display a warning to other Technical Officials that marshall has changed
	 * weight for current athlete
	 *
	 * @param e
	 * @param athlete
	 * @param fop
	 */
	private void warnOthersIfCurrent(UIEvent.LiftingOrderUpdated e, Athlete athlete, FieldOfPlay fop) {
		// the athlete currently displayed is not necessarily the fop curAthlete,
		// because the lifting order has been recalculated behind the scenes
		Athlete curDisplayAthlete = displayedAthlete;

		// weight change warnings not to self.
		if (this != e.getOrigin() && curDisplayAthlete != null && curDisplayAthlete.equals(e.getChangingAthlete())) {
			String text;
			int declaring = curDisplayAthlete.isDeclaring();
			if (declaring > 0) {
				text = getTranslation("Declaration_current_athlete_with_change", curDisplayAthlete.getFullName());
			} else if (declaring == 0) {
				text = getTranslation("Declaration_current_athlete", curDisplayAthlete.getFullName());
			} else {
				text = getTranslation("Weight_change_current_athlete", curDisplayAthlete.getFullName());
			}
			doNotification(text, "warning");
		}
		Integer newWeight = e.getNewWeight();
		// avoid duplicate info to officials
		if (newWeight != null && prevWeight != newWeight) {
			doNotification(Translator.translate("Notification.WeightToBeLoaded", newWeight), "info");
			prevWeight = newWeight;
		}
	}

	protected abstract HorizontalLayout announcerButtons(FlexLayout topBar2);

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#breakButtons(com.vaadin.flow.component.orderedlayout.FlexLayout)
	 */
	protected HorizontalLayout breakButtons(FlexLayout announcerBar) {
		breakButton = new Button(Translator.translateOrElseEmpty("Pause"),new Icon(VaadinIcon.TIMER), (e) -> {
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
				} else {
					if (this instanceof MarshallContent) {
						bt = BreakType.MARSHAL;
					} else {
						bt = BreakType.TECHNICAL;
					}
					ct = CountdownType.INDEFINITE;

				}
				// logger.debug("requesting breaktype {}", bt);
				breakDialog = new BreakDialog(bt, ct, null, this);
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

	protected void create1MinButton() {
		_1min = new Button("1:00", (e) -> do1Minute());
		_1min.getElement().setAttribute("theme", "icon");
	}

	protected void create2MinButton() {
		_2min = new Button("2:00", (e) -> do2Minutes());
		_2min.getElement().setAttribute("theme", "icon");
	}

	protected void do1Minute() {
		OwlcmsSession.withFop(fop -> {
			fop.fopEventPost(new FOPEvent.ForceTime(60000, this.getOrigin()));
		});
	}

	protected void do2Minutes() {
		OwlcmsSession.withFop(fop -> {
			fop.fopEventPost(new FOPEvent.ForceTime(120000, this.getOrigin()));
		});
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
		grid.getThemeNames().add("row-stripes");
		grid.getThemeNames().add("compact");
		grid.addColumn("startNumber").setHeader(getTranslation("StartNumber")).setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn(
		        createLastNameRenderer()).setHeader(getTranslation("LastName"));
		grid.addColumn(
		        createFirstNameRenderer()).setHeader(getTranslation("FirstName"));
		grid.addColumn("team").setHeader(getTranslation("Team"));
		grid.addColumn("category").setHeader(getTranslation("Category")).setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn(createAttemptsRenderer()).setHeader(Translator.translate("AthleteGrid.Attempts"))
		        .setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(
		        a -> (a.getTotal() > 0 ? a.getTotal() : "-")).setHeader(getTranslation("Total"))
		        .setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn((a) -> formatAttemptNumber(a), "attemptsDone").setHeader(getTranslation("Attempt"));

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

	protected FlexLayout createInitialBar() {
		// logger.debug("{} {} creating top bar {}", this.getClass().getSimpleName(),
		// LoggerUtils.whereFrom());
		topBar = new FlexLayout();
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
		return topBar;
	}

	protected Component createReset() {
		return null;
	}

	protected void createStartTimeButton() {
		startTimeButton = new Button(new Icon(VaadinIcon.PLAY));
		startTimeButton.addClickListener(e -> doStartTime());
		startTimeButton.getElement().setAttribute("theme", "primary success icon");
	}

	protected void createStopTimeButton() {
		stopTimeButton = new Button(new Icon(VaadinIcon.PAUSE));
		stopTimeButton.addClickListener(e -> doStopTime());
		stopTimeButton.getElement().setAttribute("theme", "secondary icon");
	}

	protected FlexLayout createTopBar() {

		topBar = new FlexLayout();
		topBar.setClassName("athleteGridTopBar");
		initialBar = false;

		HorizontalLayout topBarLeft = createTopBarLeft();

		lastName = new H2();
		lastName.setText("\u2013");
		lastName.getStyle().set("margin", "0px 0px 0px 0px");

		setFirstNameWrapper(new H3(""));
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
		startNumber.setVisible(false);
		getFirstNameWrapper().add(firstName, startNumber);
		Div fullName = new Div(lastName, getFirstNameWrapper());

		attempt = new H2();
		weight = new H2();
		weight.setText("");
		if (timer == null) {
			timer = new AthleteTimerElement(this);
		}
		timer.setSilenced(this.isSilenced());
		H1 time = new H1(timer);
		clearVerticalMargins(attempt);
		clearVerticalMargins(time);
		clearVerticalMargins(weight);

		buttons = announcerButtons(topBar);
		if (buttons != null) {
			buttons.setPadding(false);
			buttons.setMargin(false);
			buttons.setSpacing(true);
		}

		breaks = breakButtons(topBar);
		if (breaks != null) {
			breaks.setPadding(false);
			breaks.setMargin(false);
			breaks.setSpacing(true);
		}

		decisions = decisionButtons(topBar);
		if (decisions != null) {
			decisions.setPadding(false);
			decisions.setMargin(false);
			decisions.setSpacing(true);
			decisions.setAlignItems(FlexComponent.Alignment.BASELINE);
		}

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
		return topBar;
	}

	protected void createTopBarGroupSelect() {
		// there is already all the SQL filtering logic for the group attached
		// hidden field in the crudGrid part of the page so we just set that
		// filter.

		OwlcmsSession.withFop((fop) -> {
			Group group = fop.getGroup();
			logger.trace("initial setting group to {} {}", group, LoggerUtils.whereFrom());
			try {
				getGroupFilter().setValue(group);
			} catch (Exception e) {
				// no way to check for no items
			}
		});

		OwlcmsSession.withFop(fop -> {
			topBarMenu = new MenuBar();
			MenuItem item;
			if (fop.getGroup() != null) {
				item = topBarMenu.addItem(fop.getGroup().getName());
				topBarMenu.addThemeVariants(MenuBarVariant.LUMO_SMALL);
				item.setEnabled(true);
			} else {
				// no group, no menu.
			}

			createTopBarSettingsMenu();
		});

		// if this is made read-write, it needs to set values in
		// groupFilter and
		// call updateURLLocation
		// see AnnouncerContent for an example.
	}

	protected void createTopBarSettingsMenu() {
		topBarSettings = new MenuBar();
		topBarSettings.addThemeVariants(MenuBarVariant.LUMO_SMALL, MenuBarVariant.LUMO_TERTIARY_INLINE);
		MenuItem item2 = topBarSettings.addItem(new Icon(VaadinIcon.COG));
		SubMenu subMenu2 = item2.getSubMenu();
		MenuItem subItemSoundOn = subMenu2.addItem(
		        Translator.translate("Settings.TurnOnSound"),
		        e -> {
			        switchSoundMode(this, !this.isSilenced(), true);
			        e.getSource().setChecked(!this.isSilenced());
			        if (decisionDisplay != null) {
				        decisionDisplay.setSilenced(this.isSilenced());
			        }
			        if (timer != null) {
				        timer.setSilenced(this.isSilenced());
			        }
		        });
		subItemSoundOn.setCheckable(true);
		subItemSoundOn.setChecked(!this.isSilenced());
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
		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<Group>());
		getGroupFilter().setItems(groups);
		getGroupFilter().setItemLabelGenerator(Group::getName);
		// hide because the top bar has it
		getGroupFilter().getStyle().set("display", "none");
		// note: group switching is done from the announcer menu, not in the grid
		// filters.
		crudLayout.addFilterComponent(getGroupFilter());

		if (attempts == null) {
			attempts = new HorizontalLayout();
			attempts.setHeight("100%");
//            for (int i = 0; i < 6; i++) {
//                Paragraph div = new Paragraph();
//                div.getElement().setAttribute("style", "border: 1; width: 5ch; background-color: pink; text-align: center");
//                div.getElement().setProperty("innerHTML", i+1+"");
//                attempts.add(div);
//            }
		}
		attempts.getElement().setAttribute("style", "float: right");
		HorizontalLayout horizontalLayout = (HorizontalLayout) crudLayout.getFilterLayout();
		horizontalLayout.add(attempts);

		HorizontalLayout toolbarLayout = (HorizontalLayout) crudLayout.getToolbarLayout();
		toolbarLayout.setSizeUndefined();

		horizontalLayout.getParent().get().getElement().setAttribute("style", "width: 100%");
	}

	protected void doNotification(String text, String theme) {
		Notification n = new Notification();
		// Notification theme styling is done in
		// META-INF/resources/frontend/styles/shared-styles.html
		n.getElement().getThemeList().add(theme);
		n.setDuration(6000);
		n.setPosition(Position.TOP_START);
		Div label = new Div();
		label.getElement().setProperty("innerHTML", text);
		label.addClickListener((event) -> n.close());
		label.getStyle().set("font-size", "large");
		n.add(label);
		n.open();
		n.open();
		return;
	}

	protected void doStartTime() {
		OwlcmsSession.withFop(fop -> {
			long now = System.currentTimeMillis();
			long timeElapsed = now - previousStartMillis;
			boolean running = fop.getAthleteTimer().isRunning();
			if (timeElapsed > 100 && !running) {
				logger.debug("clock start {}ms running={}", timeElapsed, running);
				fop.fopEventPost(new FOPEvent.TimeStarted(this.getOrigin()));
				buttonsTimeStarted();
			} else {
				logger.debug("discarding duplicate clock start {}ms running={}", timeElapsed, running);
			}
			previousStartMillis = now;
		});
	}

	protected void doStopTime() {
		OwlcmsSession.withFop(fop -> {
			long now = System.currentTimeMillis();
			long timeElapsed = now - previousStopMillis;
			boolean running = fop.getAthleteTimer().isRunning();
			if (timeElapsed > 100 && running) {
				logger.debug("clock stop {}ms running={}", timeElapsed, running);
				fop.fopEventPost(new FOPEvent.TimeStopped(this.getOrigin()));
				buttonsTimeStopped();
			} else {
				logger.debug("discarding duplicate clock stop {}ms running={}", timeElapsed, running);
			}
			previousStopMillis = now;
		});
		OwlcmsSession.withFop(fop -> {

		});
	}

	protected void doToggleTime() {
		OwlcmsSession.withFop(fop -> {
			long now = System.currentTimeMillis();
			long timeElapsed = now - previousToggleMillis;
			if (timeElapsed > 100) {
				boolean running = fop.getAthleteTimer().isRunning();
				if (running) {
					doStopTime();
				} else {
					doStartTime();
				}
			}
			previousToggleMillis = now;
		});
	}

	protected void doUpdateTopBar(Athlete athlete, Integer timeAllowed) {
		// logger.debug("{} updateTopBar {}\\n{}", this.getClass().getSimpleName(),
		// athlete/*,LoggerUtils. stackTrace()*/);
		if (title == null) {
			return;
		}
		displayedAthlete = athlete;

		OwlcmsSession.withFop(fop -> {
			UIEventProcessor.uiAccess(topBar, uiEventBus, () -> {
				Group group = fop.getGroup();
				// ** this.setValue(group); // does nothing if already correct
				Integer attemptsDone = (athlete != null ? athlete.getAttemptsDone() : 0);
				// logger.debug("doUpdateTopBar {} {} {}", LoggerUtils.whereFrom(), athlete,
				// attemptsDone);
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
							if (startNumberText.isBlank()) {
								startNumber.setVisible(false);
							} else {
								startNumber.setVisible(true);
								startNumber.getStyle().set("font-size", "normal");
							}
						} else {
							startNumber.setText("\u26A0");
							startNumber.setTitle(getTranslation("StartNumbersNotSet"));
							startNumber.setVisible(true);
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
		title = new H4();
		title.setText(getTopBarTitle());
		title.setClassName("topBarTitle");
		title.getStyle().set("margin-top", "0px").set("margin-bottom", "0px").set("font-weight", "normal");
		getTopBarLeft().add(title);
		if (topBarMenu != null) {
			getTopBarLeft().add(topBarMenu);
		}
		if (topBarSettings != null) {
			getTopBarLeft().add(topBarSettings);
		}
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
		this.id = IdUtils.getTimeBasedId();
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
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
	 * AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// logger.debug("attaching {} initial={} \\n{}",
		// this.getClass().getSimpleName(), attachEvent.isInitialAttach(),
		// LoggerUtils. stackTrace());
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

			createTopBarGroupSelect();

			if (refreshGrid) {
				// ** this.setValue(fopGroup);
				if (crudGrid != null) {
					crudGrid.sort(null);
					crudGrid.refreshGrid();
				}
			}

			Athlete curAthlete2 = fop.getCurAthlete();
			FOPState state = fop.getState();
			if (state == FOPState.INACTIVE || (state == FOPState.BREAK && fop.getGroup() == null)) {
				getRouterLayout().setMenuTitle(getMenuTitle());
				getRouterLayout().setMenuArea(createInitialBar());
				getRouterLayout().updateHeader(false);

				warning.setText(getTranslation("IdlePlatform"));
				if (curAthlete2 == null || curAthlete2.getAttemptsDone() >= 6 || fop.getLiftingOrder().size() == 0) {
					topBarWarning(fop.getGroup(), curAthlete2 == null ? 0 : curAthlete2.getAttemptsDone(),
					        fop.getState(), fop.getLiftingOrder());
				}
			} else {
				getRouterLayout().setMenuTitle("");
				getRouterLayout().setMenuArea(createTopBar());
				getRouterLayout().updateHeader(false);
				if (state == FOPState.BREAK) {
					// logger.debug("break");
					if (buttons != null) {
						buttons.setVisible(false);
					}
					if (decisions != null) {
						decisions.setVisible(false);
					}
					busyBreakButton();
				} else {
					// logger.debug("notBreak");
					if (buttons != null) {
						buttons.setVisible(true);
					}
					if (decisions != null) {
						decisions.setVisible(true);
					}
					if (breakButton == null) {
						logger.debug("breakButton is null\n{}", LoggerUtils.stackTrace());
					}
					if (breakButton != null) {
						quietBreakButton(
						        this instanceof MarshallContent ? Translator.translate("StopCompetition") : Translator.translateOrElseEmpty("Pause"));
					}
				}
				if (breakButton != null) {
					breakButton.setEnabled(true);
				}
				Athlete curAthlete = fop.getCurAthlete();
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
				hideButtons();
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
				if (startNumber != null) {
					startNumber.setVisible(false);
				}
				warning.setText(string);
			}
		} else if (liftingOrder.size() == 0) {
			String string = getTranslation("No_weighed_in_athletes");
			String text = group == null ? "\u2013" : string;
			if (!initialBar) {
				topBarMessage(string, text);
			} else {
				hideButtons();
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
		ui.getPage().getHistory().replaceState(null,
		        new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params))));
	}

}
