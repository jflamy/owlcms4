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
import java.util.Timer;
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
import com.vaadin.flow.component.dialog.Dialog;
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

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.apputils.queryparameters.SoundParametersReader;
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
 * Initialization order is - content class is created - wrapping app layout is created if not present - this content is
 * inserted in the app layout slot
 *
 */
@SuppressWarnings("serial")
@CssImport(value = "./styles/athlete-grid.css")
public abstract class AthleteGridContent extends BaseContent
implements CrudListener<Athlete>, OwlcmsContent, SoundParametersReader, UIEventProcessor, IAthleteEditing,
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
	 * groupFilter points to a hidden field on the crudGrid filtering row, which is slave to the group selection
	 * process. this allows us to use the filtering logic used everywhere else to change what is shown in the crudGrid.
	 *
	 * In the current implementation groupSelect is readOnly. If it is made editable, it needs to set the value on
	 * groupFilter.
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
	private HorizontalLayout topBarLeft;
	private String topBarTitle;
	private HorizontalLayout attempts;
	private Integer prevWeight;
	private boolean summonNotificationSent;
	private boolean deliberationNotificationSent;
	private long previousToggleMillis;

	/**
	 * Instantiates a new announcer content. Content is created in {@link #setParameter(BeforeEvent, String)} after URL
	 * parameters are parsed.
	 */
	public AthleteGridContent() {
		init();
		this.breakTimerElement = new BreakTimerElement();
	}

	/**
	 * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
	 */
	@Override
	public Athlete add(Athlete athlete) {
		return getAthleteEditingFormFactory().add(athlete);
	}

	@Override
	public void addDialogContent(Component page, VerticalLayout vl) {
	}

	public void busyBreakButton() {
		if (this.breakButton == null) {
			//            logger.trace("breakButton is null\n{}", LoggerUtils. stackTrace());
			return;
		}
		this.breakButton.getElement().setAttribute("theme", "primary error");
		this.breakButton.getStyle().set("color", "white");
		this.breakButton.getStyle().set("background-color", "var(--lumo-error-color)");
		// breakButton.setText(getTranslation("BreakButton.Paused"));
		OwlcmsSession.withFop(fop -> {
			IBreakTimer breakTimer = fop.getBreakTimer();
			if (!breakTimer.isIndefinite()) {
				BreakTimerElement bte = (BreakTimerElement) getBreakTimerElement();
				bte.syncWithFopTimer();
				bte.setParent(this.getClass().getSimpleName() + "_" + this.id);
				this.breakButton.setIcon(bte);
				this.breakButton.setIconAfterText(true);
			}

			if (fop.getCeremonyType() != null) {
				this.breakButton.setText(getTranslation("CeremonyType." + fop.getCeremonyType()));
			} else {
				BreakType breakType = fop.getBreakType();
				if (breakType != null) {
					this.breakButton.setText(getTranslation("BreakType." + breakType) + "\u00a0\u00a0");
				} else {
					logger.error("null break type {}", LoggerUtils.stackTrace());
					this.breakButton.setText(getTranslation("BreakButton.Paused") + "\u00a0\u00a0");
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
		this.crudLayout.hideForm();
		this.crudGrid.getGrid().asSingleSelect().clear();
	}

	/**
	 * Used by the TimeKeeper and TechnicalController classes that abusively inherit from this class (they don't
	 * actually have a grid)
	 *
	 * @see app.owlcms.nui.shared.AthleteGridContent#createTopBar()
	 */
	@Override
	public FlexLayout createMenuArea() {
		this.topBar = new FlexLayout();
		this.topBar.setClassName("athleteGridTopBar");
		this.initialBar = false;

		HorizontalLayout topBarLeft = createTopBarLeft();

		this.lastName = new H2();
		this.lastName.setText("\u2013");
		this.lastName.getStyle().set("margin", "0px 0px 0px 0px");

		setFirstNameWrapper(new H3(""));
		getFirstNameWrapper().getStyle().set("margin", "0px 0px 0px 0px");
		this.firstName = new Span("");
		this.firstName.getStyle().set("margin", "0px 0px 0px 0px");
		this.startNumber = new Span("");
		Style style = this.startNumber.getStyle();
		style.set("margin", "0px 0px 0px 1em");
		style.set("padding", "0px 0px 0px 0px");
		style.set("border", "2px solid var(--lumo-primary-color)");
		style.set("font-size", "90%");
		style.set("width", "1.4em");
		style.set("text-align", "center");
		style.set("display", "inline-block");
		this.startNumber.setVisible(false);
		getFirstNameWrapper().add(this.firstName, this.startNumber);
		Div fullName = new Div(this.lastName, getFirstNameWrapper());

		this.attempt = new H2();
		this.weight = new H2();
		this.weight.setText("");
		if (this.timer == null) {
			this.timer = new AthleteTimerElement(this);
		}
		this.timer.setSilenced(this.isSilenced());
		H1 time = new H1(this.timer);
		clearVerticalMargins(this.attempt);
		clearVerticalMargins(time);
		clearVerticalMargins(this.weight);

		this.buttons = announcerButtons(this.topBar);
		this.breaks = breakButtons(this.topBar);
		this.decisions = decisionButtons(this.topBar);
		this.decisions.setAlignItems(FlexComponent.Alignment.BASELINE);

		this.topBar.setSizeFull();
		this.topBar.add(topBarLeft, fullName, this.attempt, this.weight, time);
		if (this.buttons != null) {
			this.topBar.add(this.buttons);
		}
		if (this.breaks != null) {
			this.topBar.add(this.breaks);
		}
		if (this.decisions != null) {
			this.topBar.add(this.decisions);
		}

		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
		this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);
		this.topBar.setAlignSelf(Alignment.CENTER, this.attempt, this.weight, time);
		this.topBar.setFlexGrow(0.5, fullName);
		this.topBar.setFlexGrow(0.2, topBarLeft);
		return this.topBar;
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
			if (this.breakButton != null) {
				this.breakButton.setText(getTranslation("BreakType." + e.getBreakType()) + "\u00a0\u00a0");
			}
		}

	}

	/**
	 * @see app.owlcms.uievents.BreakDisplay#doCeremony(app.owlcms.uievents.UIEvent.CeremonyStarted)
	 */
	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		if (this.breakButton != null) {
			this.breakButton.setText(getTranslation("CeremonyType." + e.getCeremonyType()) + "\u00a0\u00a0");
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
			if (this.lastNameFilter.getValue() != null) {
				filterValue = this.lastNameFilter.getValue().toLowerCase();
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
	public Dialog getDialog() {
		return null;
	}

	@Override
	public OwlcmsCrudGrid<?> getEditingGrid() {
		return this.crudGrid;
	}

	public H3 getFirstNameWrapper() {
		return this.firstNameWrapper;
	}

	//    @Override
	//    public void setHeaderContent() {
	//        routerLayout.setMenuTitle(getPageTitle());
	//        routerLayout.setMenuArea(createMenuArea());
	//        routerLayout.showLocaleDropdown(false);
	//        routerLayout.setDrawerOpened(false);
	//        routerLayout.updateHeader(false);
	//    }

	/**
	 * @return the groupFilter
	 */
	public ComboBox<Group> getGroupFilter() {
		return this.groupFilter;
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return this.routerLayout;
	}

	public boolean isIgnoreSwitchGroup() {
		return this.ignoreSwitchGroup;
	}

	@Override
	public boolean isShowInitialDialog() {
		return false;
	}

	@Override
	public boolean isVideo() {
		return false;
	}

	public void quietBreakButton(String caption) {
		this.breakButton.getStyle().set("color", "var(--lumo-error-color)");
		this.breakButton.getStyle().set("background-color", "var(--lumo-error-color-10pct)");
		if (caption != null) {
			this.breakButton.getElement().setAttribute("theme", "secondary error");
			this.breakButton.setText(caption);
			this.breakButton.getElement().setAttribute("title", caption);
		} else {
			this.breakButton.getElement().setAttribute("theme", "secondary error icon");
			this.breakButton.getElement().setAttribute("title", getTranslation("BreakButton.ToStartCaption"));
		}

	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#readParams(com.vaadin.flow.router.Location,
	 *      java.util.Map)
	 */
	@Override
	public Map<String, List<String>> readParams(Location location,
			Map<String, List<String>> parametersMap) {
		// handle FOP and Group by calling superclass
		Map<String, List<String>> params = SoundParametersReader.super.readParams(location, parametersMap);

		List<String> silentParams = params.get(SILENT);
		// silent is the default. silent=false will cause sound
		boolean silentMode = silentParams == null || silentParams.isEmpty()
				|| silentParams.get(0).toLowerCase().equals("true");
		switchSoundMode(silentMode, false);
		updateParam(params, SILENT, !isSilenced() ? "false" : null);
		setUrlParameterMap(params);
		return params;
	}

	@Override
	public void setDialog(Dialog nDialog) {
	}

	@Override
	public void setDialogTimer(Timer timer) {
	}

	public void setFirstNameWrapper(H3 firstNameWrapper) {
		this.firstNameWrapper = firstNameWrapper;
	}

	public void setIgnoreSwitchGroup(boolean b) {
		this.ignoreSwitchGroup = b;
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
		SoundParametersReader.super.setParameter(event, parameter);
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
	public void setVideo(boolean b) {
	}

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			logger.debug("stopping break");
			syncWithFOP(true);
		});
	}

	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		this.summonNotificationSent = false;
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			if (e.isDisplayToggle()) {
				logger.debug("{} ignoring switch to break", this.getClass().getSimpleName());
				return;
			}

			// logger.debug("%%%%%%% starting break {}", LoggerUtils./**/stackTrace());
			syncWithFOP(true);
		});
	}

	@Subscribe
	public void slaveBroadcast(UIEvent.Broadcast e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
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
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			syncWithFOP(true);
		});
	}

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			doCeremony(e);
		});
	}

	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
				this.getOrigin(), e.getOrigin());
		OwlcmsSession.withFop((fop) -> {
			UIEventProcessor.uiAccess(this.topBar, this.uiEventBus, e, () -> {
				// doUpdateTopBar(fop.getCurAthlete(), 0);
				getRouterLayout().setMenuArea(createInitialBar());
				syncWithFOP(true);
			});
		});

	}

	@Subscribe
	public void slaveJuryNotification(UIEvent.JuryNotification e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
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
				if (!this.summonNotificationSent) {
					doNotification(text, style);
				}
				this.summonNotificationSent = true;
				return;
			case START_DELIBERATION:
				text = Translator.translate("JuryNotification." + et.name());
				if (!this.deliberationNotificationSent) {
					doNotification(text, style);
				}
				this.deliberationNotificationSent = true;
				return;
			case CHALLENGE:
				text = Translator.translate("JuryNotification." + et.name());
				if (!this.deliberationNotificationSent) {
					doNotification(text, style);
				}
				this.deliberationNotificationSent = true;
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
				this.summonNotificationSent = false;
				this.deliberationNotificationSent = false;
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
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
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
		if (this.stopTimeButton == null) {
			return;
		}
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this.stopTimeButton, this.uiEventBus, e, this.getOrigin(),
				() -> buttonsTimeStopped());
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			logger.trace("starting lifting");
			syncWithFOP(true);
			this.summonNotificationSent = false;
			this.deliberationNotificationSent = false;
		});
	}

	@Subscribe
	public void slaveStartTimer(UIEvent.StartTime e) {
		// we use stop because it is present on most screens; either button ok for
		// locking
		if (this.stopTimeButton == null) {
			return;
		}
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this.stopTimeButton, this.uiEventBus, e, this.getOrigin(),
				() -> buttonsTimeStarted());
	}

	@Subscribe
	public void slaveStopTimer(UIEvent.StopTime e) {
		// we use stop because it is present on most screens; either button ok for
		// locking
		if (this.stopTimeButton == null) {
			return;
		}
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this.stopTimeButton, this.uiEventBus, e, this.getOrigin(),
				() -> buttonsTimeStopped());
	}

	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
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
			UIEventProcessor.uiAccess(this.topBar, this.uiEventBus, e, () -> {
				warnOthersIfCurrent(e, athlete, fop);
				doUpdateTopBar(athlete, e.getTimeAllowed());
			});
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see app.owlcms.nui.group.UIEventProcessor#updateGrid(app.owlcms.fieldofplay. UIEvent.LiftingOrderUpdated)
	 */
	@Subscribe
	public void slaveUpdateGrid(UIEvent.LiftingOrderUpdated e) {
		if (this.crudGrid == null) {
			return;
		}
		logger.debug("{} {}", e.getOrigin(), LoggerUtils.whereFrom());
		UIEventProcessor.uiAccess(this.crudGrid, this.uiEventBus, e, () -> {
			this.crudGrid.refreshGrid();
		});
	}

	/**
	 * Update button and validation logic is in form factory
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public Athlete update(Athlete notUsed) {
		return this.athleteEditingFormFactory.update(notUsed);
	}

	protected abstract HorizontalLayout announcerButtons(FlexLayout topBar2);

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#breakButtons(com.vaadin.flow.component.orderedlayout.FlexLayout)
	 */
	protected HorizontalLayout breakButtons(FlexLayout announcerBar) {
		this.breakButton = new Button(Translator.translateOrElseEmpty("Pause"), new Icon(VaadinIcon.TIMER), (e) -> {
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
				this.breakDialog = new BreakDialog(bt, ct, null, this);
				this.breakDialog.open();
			});
		});
		return layoutBreakButtons();
	}

	protected void buttonsTimeStarted() {
		if (this.startTimeButton != null) {
			this.startTimeButton.getElement().setAttribute("theme", "secondary icon");
		}
		if (this.stopTimeButton != null) {
			this.stopTimeButton.getElement().setAttribute("theme", "primary error icon");
		}
	}

	protected void buttonsTimeStopped() {
		if (this.startTimeButton != null) {
			this.startTimeButton.getElement().setAttribute("theme", "primary success icon");
		}
		if (this.stopTimeButton != null) {
			this.stopTimeButton.getElement().setAttribute("theme", "secondary icon");
		}
	}

	protected void create1MinButton() {
		this._1min = new Button("1:00", (e) -> do1Minute());
		this._1min.getElement().setAttribute("theme", "icon");
	}

	protected void create2MinButton() {
		this._2min = new Button("2:00", (e) -> do2Minutes());
		this._2min.getElement().setAttribute("theme", "icon");
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

		this.crudLayout = new OwlcmsGridLayout(Athlete.class);
		AthleteCrudGrid crudGrid = new AthleteCrudGrid(Athlete.class, this.crudLayout, crudFormFactory, grid) {
			@Override
			protected void initToolbar() {
				Component reset = createReset();
				if (reset != null) {
					this.crudLayout.addToolbarComponent(reset);
				}
			}

			@Override
			protected void updateButtons() {
			}
		};

		crudGrid.setCrudListener(this);
		crudGrid.setClickRowToUpdate(true);
		this.crudLayout.addToolbarComponent(getGroupFilter());

		return crudGrid;
	}

	/**
	 * Define the form used to edit a given athlete.
	 *
	 * @return the form factory that will create the actual form on demand
	 */
	protected OwlcmsCrudFormFactory<Athlete> createFormFactory() {
		this.athleteEditingFormFactory = new AthleteCardFormFactory(Athlete.class, this);
		return this.athleteEditingFormFactory;
	}

	protected FlexLayout createInitialBar() {
		// logger.debug("{} {} creating top bar {}", this.getClass().getSimpleName(),
		// LoggerUtils.whereFrom());
		this.topBar = new FlexLayout();
		this.initialBar = true;

		createTopBarGroupSelect();
		HorizontalLayout topBarLeft = createTopBarLeft();

		this.warning = new H3();
		this.warning.getStyle().set("margin-top", "0");
		this.warning.getStyle().set("margin-bottom", "0");

		this.topBar.removeAll();
		this.topBar.setSizeFull();
		this.topBar.add(topBarLeft, this.warning);

		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);
		this.topBar.setFlexGrow(0.0, topBarLeft);
		return this.topBar;
	}

	protected Component createReset() {
		return null;
	}

	protected void createStartTimeButton() {
		this.startTimeButton = new Button(new Icon(VaadinIcon.PLAY));
		this.startTimeButton.addClickListener(e -> doStartTime());
		this.startTimeButton.getElement().setAttribute("theme", "primary success icon");
	}

	protected void createStopTimeButton() {
		this.stopTimeButton = new Button(new Icon(VaadinIcon.PAUSE));
		this.stopTimeButton.addClickListener(e -> doStopTime());
		this.stopTimeButton.getElement().setAttribute("theme", "secondary icon");
	}

	protected FlexLayout createTopBar() {

		this.topBar = new FlexLayout();
		this.topBar.setClassName("athleteGridTopBar");
		this.initialBar = false;

		HorizontalLayout topBarLeft = createTopBarLeft();

		this.lastName = new H2();
		this.lastName.setText("\u2013");
		this.lastName.getStyle().set("margin", "0px 0px 0px 0px");

		setFirstNameWrapper(new H3(""));
		getFirstNameWrapper().getStyle().set("margin", "0px 0px 0px 0px");
		this.firstName = new Span("");
		this.firstName.getStyle().set("margin", "0px 0px 0px 0px");
		this.startNumber = new Span("");
		Style style = this.startNumber.getStyle();
		style.set("margin", "0px 0px 0px 1em");
		style.set("padding", "0px 0px 0px 0px");
		style.set("border", "2px solid var(--lumo-primary-color)");
		style.set("font-size", "90%");
		style.set("width", "1.4em");
		style.set("text-align", "center");
		style.set("display", "inline-block");
		this.startNumber.setVisible(false);
		getFirstNameWrapper().add(this.firstName, this.startNumber);
		Div fullName = new Div(this.lastName, getFirstNameWrapper());

		this.attempt = new H2();
		this.weight = new H2();
		this.weight.setText("");
		if (this.timer == null) {
			this.timer = new AthleteTimerElement(this);
		}
		this.timer.setSilenced(this.isSilenced());
		H1 time = new H1(this.timer);
		clearVerticalMargins(this.attempt);
		clearVerticalMargins(time);
		clearVerticalMargins(this.weight);

		this.buttons = announcerButtons(this.topBar);
		if (this.buttons != null) {
			this.buttons.setPadding(false);
			this.buttons.setMargin(false);
			this.buttons.setSpacing(true);
		}

		this.breaks = breakButtons(this.topBar);
		if (this.breaks != null) {
			this.breaks.setPadding(false);
			this.breaks.setMargin(false);
			this.breaks.setSpacing(true);
		}

		this.decisions = decisionButtons(this.topBar);
		if (this.decisions != null) {
			this.decisions.setPadding(false);
			this.decisions.setMargin(false);
			this.decisions.setSpacing(true);
			this.decisions.setAlignItems(FlexComponent.Alignment.BASELINE);
		}

		this.topBar.setSizeFull();
		this.topBar.add(topBarLeft, fullName, this.attempt, this.weight, time);
		if (this.buttons != null) {
			this.topBar.add(this.buttons);
		}
		if (this.breaks != null) {
			this.topBar.add(this.breaks);
		}
		if (this.decisions != null) {
			this.topBar.add(this.decisions);
		}

		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
		this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);
		this.topBar.setAlignSelf(Alignment.CENTER, this.attempt, this.weight, time);
		this.topBar.setFlexGrow(0.5, fullName);
		this.topBar.setFlexGrow(0.0, topBarLeft);
		return this.topBar;
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
			this.topBarMenu = new MenuBar();
			MenuItem item;
			if (fop.getGroup() != null) {
				item = this.topBarMenu.addItem(fop.getGroup().getName());
				this.topBarMenu.addThemeVariants(MenuBarVariant.LUMO_SMALL);
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
		this.topBarSettings = new MenuBar();
		this.topBarSettings.addThemeVariants(MenuBarVariant.LUMO_SMALL, MenuBarVariant.LUMO_TERTIARY_INLINE);
		MenuItem item2 = this.topBarSettings.addItem(new Icon(VaadinIcon.COG));
		SubMenu subMenu2 = item2.getSubMenu();
		MenuItem subItemSoundOn = subMenu2.addItem(
				Translator.translate("Settings.TurnOnSound"),
				e -> {
					switchSoundMode(!this.isSilenced(), true);
					e.getSource().setChecked(!this.isSilenced());
					if (this.decisionDisplay != null) {
						this.decisionDisplay.setSilenced(this.isSilenced());
					}
					if (this.timer != null) {
						this.timer.setSilenced(this.isSilenced());
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

		this.lastNameFilter.setPlaceholder(getTranslation("LastName"));
		this.lastNameFilter.setClearButtonVisible(true);
		this.lastNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
		this.lastNameFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		this.crudLayout.addFilterComponent(this.lastNameFilter);

		getGroupFilter().setPlaceholder(getTranslation("Group"));
		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<>());
		getGroupFilter().setItems(groups);
		getGroupFilter().setItemLabelGenerator(Group::getName);
		// hide because the top bar has it
		getGroupFilter().getStyle().set("display", "none");
		// note: group switching is done from the announcer menu, not in the grid
		// filters.
		this.crudLayout.addFilterComponent(getGroupFilter());

		if (this.attempts == null) {
			this.attempts = new HorizontalLayout();
			this.attempts.setHeight("100%");
			//            for (int i = 0; i < 6; i++) {
			//                Paragraph div = new Paragraph();
			//                div.getElement().setAttribute("style", "border: 1; width: 5ch; background-color: pink; text-align: center");
			//                div.getElement().setProperty("innerHTML", i+1+"");
			//                attempts.add(div);
			//            }
		}
		this.attempts.getElement().setAttribute("style", "float: right");
		HorizontalLayout horizontalLayout = (HorizontalLayout) this.crudLayout.getFilterLayout();
		horizontalLayout.add(this.attempts);

		HorizontalLayout toolbarLayout = (HorizontalLayout) this.crudLayout.getToolbarLayout();
		toolbarLayout.setSizeUndefined();

		horizontalLayout.getParent().get().getElement().setAttribute("style", "width: 100%");
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
			long timeElapsed = now - this.previousStartMillis;
			boolean running = fop.getAthleteTimer().isRunning();
			if (timeElapsed > 100 && !running) {
				logger.debug("clock start {}ms running={}", timeElapsed, running);
				fop.fopEventPost(new FOPEvent.TimeStarted(this.getOrigin()));
				buttonsTimeStarted();
			} else {
				logger.debug("discarding duplicate clock start {}ms running={}", timeElapsed, running);
			}
			this.previousStartMillis = now;
		});
	}

	protected void doStopTime() {
		OwlcmsSession.withFop(fop -> {
			long now = System.currentTimeMillis();
			long timeElapsed = now - this.previousStopMillis;
			boolean running = fop.getAthleteTimer().isRunning();
			if (timeElapsed > 100 && running) {
				logger.debug("clock stop {}ms running={}", timeElapsed, running);
				fop.fopEventPost(new FOPEvent.TimeStopped(this.getOrigin()));
				buttonsTimeStopped();
			} else {
				logger.debug("discarding duplicate clock stop {}ms running={}", timeElapsed, running);
			}
			this.previousStopMillis = now;
		});
		OwlcmsSession.withFop(fop -> {

		});
	}

	protected void doToggleTime() {
		OwlcmsSession.withFop(fop -> {
			long now = System.currentTimeMillis();
			long timeElapsed = now - this.previousToggleMillis;
			if (timeElapsed > 100) {
				boolean running = fop.getAthleteTimer().isRunning();
				if (running) {
					doStopTime();
				} else {
					doStartTime();
				}
			}
			this.previousToggleMillis = now;
		});
	}

	protected void doUpdateTopBar(Athlete athlete, Integer timeAllowed) {
		// logger.debug("{} updateTopBar {}\\n{}", this.getClass().getSimpleName(),
		// athlete/*,LoggerUtils. stackTrace()*/);
		if (this.title == null) {
			return;
		}
		this.displayedAthlete = athlete;

		OwlcmsSession.withFop(fop -> {
			UIEventProcessor.uiAccess(this.topBar, this.uiEventBus, () -> {
				Group group = fop.getGroup();
				// ** this.setValue(group); // does nothing if already correct
				Integer attemptsDone = (athlete != null ? athlete.getAttemptsDone() : 0);
				// logger.debug("doUpdateTopBar {} {} {}", LoggerUtils.whereFrom(), athlete,
				// attemptsDone);
				if (athlete != null && attemptsDone < 6) {
					if (!this.initialBar) {
						String lastName2 = athlete.getLastName();
						this.lastName.setText(lastName2 != null ? lastName2.toUpperCase() : "");
						String firstName2 = athlete.getFirstName();
						this.firstName.setText(firstName2 != null ? firstName2 : "");
						Integer startNumber2 = athlete.getStartNumber();
						String startNumberText = (startNumber2 != null && startNumber2 > 0 ? startNumber2.toString()
								: null);
						if (startNumberText != null) {
							this.startNumber.setText(startNumberText);
							if (startNumberText.isBlank()) {
								this.startNumber.setVisible(false);
							} else {
								this.startNumber.setVisible(true);
								this.startNumber.getStyle().set("font-size", "normal");
							}
						} else {
							this.startNumber.setText("\u26A0");
							this.startNumber.setTitle(getTranslation("StartNumbersNotSet"));
							this.startNumber.setVisible(true);
							this.startNumber.getStyle().set("font-size", "smaller");
						}
						this.timer.getElement().getStyle().set("visibility", "visible");
						this.attempt.setText(formatAttemptNumber(athlete));
						Integer nextAttemptRequestedWeight = athlete.getNextAttemptRequestedWeight();
						this.weight.setText(
								(nextAttemptRequestedWeight != null ? nextAttemptRequestedWeight.toString() : "\u2013")
								+ getTranslation("KgSymbol"));
					}
				} else {
					topBarWarning(group, attemptsDone, fop.getState(), fop.getLiftingOrder());
				}
				if (fop.getState() != FOPState.BREAK && this.breakDialog != null && this.breakDialog.isOpened()) {
					this.breakDialog.close();
				}
			});
		});
	}

	protected void fillTopBarLeft() {
		this.title = new H4();
		this.title.setText(getTopBarTitle());
		this.title.setClassName("topBarTitle");
		this.title.getStyle().set("margin-top", "0px").set("margin-bottom", "0px").set("font-weight", "normal");
		getTopBarLeft().add(this.title);
		if (this.topBarMenu != null) {
			getTopBarLeft().add(this.topBarMenu);
		}
		if (this.topBarSettings != null) {
			getTopBarLeft().add(this.topBarSettings);
		}
		getTopBarLeft().setAlignItems(Alignment.CENTER);
		getTopBarLeft().setPadding(true);
		getTopBarLeft().setId("topBarLeft");
	}

	protected Object getOrigin() {
		return this;
	}

	protected HorizontalLayout getTopBarLeft() {
		return this.topBarLeft;
	}

	protected String getTopBarTitle() {
		return this.topBarTitle;
	}

	/**
	 * 
	 */
	protected void init() {
		this.id = IdUtils.getTimeBasedId();
		OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
		this.crudGrid = createCrudGrid(crudFormFactory);
		this.crudLayout = (OwlcmsGridLayout) this.crudGrid.getCrudLayout();
		defineFilters(this.crudGrid);
		fillHW(this.crudGrid, this);
		setDownSilenced(true);
	}

	protected HorizontalLayout layoutBreakButtons() {
		this.breakButton.getElement().setAttribute("theme", "secondary error");
		this.breakButton.getStyle().set("color", "var(--lumo-error-color)");
		this.breakButton.getStyle().set("background-color", "var(--lumo-error-color-10pct)");
		this.breakButton.getElement().setAttribute("title", getTranslation("BreakButton.Caption"));

		HorizontalLayout buttons = new HorizontalLayout(this.breakButton);
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
		// logger.debug("attaching {} initial={} \\n{}",
		// this.getClass().getSimpleName(), attachEvent.isInitialAttach(),
		// LoggerUtils. stackTrace());
		OwlcmsSession.withFop(fop -> {
			// create the top bar.
			syncWithFOP(true);
			// we listen on uiEventBus.
			this.uiEventBus = uiEventBusRegister(this, fop);
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
				if (this.crudGrid != null) {
					this.crudGrid.sort(null);
					this.crudGrid.refreshGrid();
				}
			}

			Athlete curAthlete2 = fop.getCurAthlete();
			FOPState state = fop.getState();
			if (state == FOPState.INACTIVE || (state == FOPState.BREAK && fop.getGroup() == null)) {
				getRouterLayout().setMenuTitle(getMenuTitle());
				getRouterLayout().setMenuArea(createInitialBar());
				getRouterLayout().updateHeader(false);

				this.warning.setText(getTranslation("IdlePlatform"));
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
					if (this.buttons != null) {
						this.buttons.setVisible(false);
					}
					if (this.decisions != null) {
						this.decisions.setVisible(false);
					}
					busyBreakButton();
				} else {
					// logger.debug("notBreak");
					if (this.buttons != null) {
						this.buttons.setVisible(true);
					}
					if (this.decisions != null) {
						this.decisions.setVisible(true);
					}
					if (this.breakButton == null) {
						logger.debug("breakButton is null\n{}", LoggerUtils.stackTrace());
					}
					if (this.breakButton != null) {
						quietBreakButton(
								this instanceof MarshallContent ? Translator.translate("StopCompetition")
										: Translator.translateOrElseEmpty("Pause"));
					}
				}
				if (this.breakButton != null) {
					this.breakButton.setEnabled(true);
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
			if (!this.initialBar) {
				topBarMessage(string, text);
			} else {
				hideButtons();
				this.warning.setText(string);
			}
		} else if (attemptsDone >= 6) {
			String string = getTranslation("Group_number_done", group.getName());
			String text = group == null ? "\u2013" : string;
			if (!this.initialBar) {
				topBarMessage(string, text);
			} else {
				if (this.introCountdownButton != null) {
					this.introCountdownButton.setVisible(false);
				}
				if (this.startLiftingButton != null) {
					this.startLiftingButton.setVisible(false);
				}
				if (this.showResultsButton != null) {
					this.showResultsButton.setVisible(true);
				}
				if (this.startNumber != null) {
					this.startNumber.setVisible(false);
				}
				this.warning.setText(string);
			}
		} else if (liftingOrder.size() == 0) {
			String string = getTranslation("No_weighed_in_athletes");
			String text = group == null ? "\u2013" : string;
			if (!this.initialBar) {
				topBarMessage(string, text);
			} else {
				hideButtons();
				this.warning.setText(string);
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

	/**
	 * @return the athleteEditingFormFactory
	 */
	private AthleteCardFormFactory getAthleteEditingFormFactory() {
		return this.athleteEditingFormFactory;
	}

	private TimerElement getBreakTimerElement() {
		return this.breakTimerElement;
	}

	private void hideButtons() {
		if (this.introCountdownButton != null) {
			this.introCountdownButton.setVisible(false);
		}
		if (this.startLiftingButton != null) {
			this.startLiftingButton.setVisible(false);
		}
		if (this.showResultsButton != null) {
			this.showResultsButton.setVisible(false);
		}
		if (this.startNumber != null) {
			this.startNumber.setVisible(false);
		}
	}

	private void topBarMessage(String string, String text) {
		this.lastName.setText(text);
		this.firstName.setText("");
		this.timer.getElement().getStyle().set("visibility", "hidden");
		this.attempt.setText("");
		this.weight.setText("");
		if (this.warning != null) {
			this.warning.setText(string);
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
		Athlete curDisplayAthlete = this.displayedAthlete;

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
		if (newWeight != null && this.prevWeight != newWeight) {
			doNotification(Translator.translate("Notification.WeightToBeLoaded", newWeight), "info");
			this.prevWeight = newWeight;
		}
	}

}
