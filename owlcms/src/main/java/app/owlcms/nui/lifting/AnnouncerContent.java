/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.lifting;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.components.GroupSelectionMenu;
import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.shared.AthleteGridContent;
import app.owlcms.nui.shared.BreakDialog;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.NaturalOrderComparator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */

@SuppressWarnings("serial")
@Route(value = "lifting/announcer", layout = OwlcmsLayout.class)
@CssImport(value = "./styles/shared-styles.css")
@CssImport(value = "./styles/notification-theme.css", themeFor = "vaadin-notification-card")
@CssImport(value = "./styles/text-field-theme.css", themeFor = "vaadin-text-field")
public class AnnouncerContent extends AthleteGridContent implements HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AnnouncerContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	private long previousBadMillis = 0L;
	private long previousGoodMillis = 0L;
	private HorizontalLayout timerButtons;
	private boolean singleReferee;
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	public AnnouncerContent() {
		super();

		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied.
		setDefaultParameters(QueryParameters.simple(Map.of(
		        SoundParameters.SILENT, "true",
		        SoundParameters.DOWNSILENT, "true",
		        SoundParameters.IMMEDIATE, "true",
		        SoundParameters.SINGLEREF, "false")));
		createTopBarGroupSelect();
		defineFilters(this.crudGrid);
	}

	/**
	 * Not used in this class. We use createInitialBar and createTopBar as required.
	 *
	 * @see app.owlcms.nui.shared.OwlcmsContent#createMenuArea()
	 */
	@Override
	public FlexLayout createMenuArea() {
		return null;
	}

	/**
	 * Use lifting order instead of display order
	 *
	 * @see app.owlcms.nui.shared.AthleteGridContent#findAll()
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
				return fop.getLiftingOrder().stream().filter(a -> a.getLastName().toLowerCase().startsWith(filterValue))
				        .collect(Collectors.toList());
			} else {
				return fop.getLiftingOrder();
			}
		} else {
			// no field of play, no group, empty list
			logger.debug("findAll fop==null");
			return ImmutableList.of();
		}
	}

	@Override
	public String getMenuTitle() {
		return getPageTitle();
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation("Announcer") + OwlcmsSession.getFopNameIfMultiple();
	}

	/**
	 * The URL contains the group, contrary to other screens.
	 *
	 * Normally there is only one announcer. If we have to restart the program the announcer screen will have the URL
	 * correctly set. if there is no current group in the FOP, the announcer will (exceptionally set it)
	 *
	 * @see app.owlcms.nui.shared.AthleteGridContent#isIgnoreGroupFromURL()
	 */
	@Override
	public boolean isIgnoreGroupFromURL() {
		return false;
	}

	@Override
	public boolean isSingleReferee() {
		return this.singleReferee;
	}

	@Override
	public void setHeaderContent() {
		getRouterLayout().setMenuTitle(getMenuTitle());
		getRouterLayout().setMenuArea(new FlexLayout());
		getRouterLayout().showLocaleDropdown(false);
		getRouterLayout().setDrawerOpened(false);
		getRouterLayout().updateHeader(false);
	}

	@Override
	public void setSingleReferee(boolean b) {
		this.singleReferee = b;
	}

	@Override
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
	public void slaveRefereeDecision(UIEvent.Decision e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			hideLiveDecisions();

			int d = e.decision ? 1 : 0;
			String text = getTranslation("NoLift_GoodLift", d, e.getAthlete().getFullName());

			Notification n = new Notification();
			String themeName = e.decision ? "success" : "error";
			n.getElement().getThemeList().add(themeName);

			Div label = new Div();
			label.add(text);
			label.addClickListener((event) -> n.close());
			label.setSizeFull();
			label.getStyle().set("font-size", "large");
			n.add(label);
			n.setPosition(Position.TOP_START);
			n.setDuration(5000);
			n.open();
		});
	}

	@Subscribe
	public void slaveStartTime(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			buttonsTimeStarted();
			displayLiveDecisions();
		});
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#announcerButtons(com.vaadin.flow.component.orderedlayout.FlexLayout)
	 */
	@Override
	protected HorizontalLayout announcerButtons(FlexLayout announcerBar) {
		createStartTimeButton();
		createStopTimeButton();
		create1MinButton();
		create2MinButton();

		this.timerButtons = new HorizontalLayout(
		        this.startTimeButton, this.stopTimeButton, this._1min, this._2min);
		this.timerButtons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return this.timerButtons;
	}

	@Override
	protected void create1MinButton() {
		super.create1MinButton();
		UI.getCurrent().addShortcutListener(() -> do1Minute(), Key.NUMPAD_ADD);
		UI.getCurrent().addShortcutListener(() -> do1Minute(), Key.EQUAL, KeyModifier.SHIFT);
	}

	@Override
	protected void create2MinButton() {
		super.create2MinButton();
		UI.getCurrent().addShortcutListener(() -> do2Minutes(), Key.EQUAL);

	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#createInitialBar()
	 */
	@Override
	protected FlexLayout createInitialBar() {
		logger.debug("AnnouncerContent creating top bar {}", LoggerUtils.whereFrom());
		this.topBar = new FlexLayout();
		this.initialBar = true;

		createTopBarGroupSelect();
		createTopBarLeft();

		this.introCountdownButton = new Button(getTranslation("introCountdown"), new Icon(VaadinIcon.TIMER),
		        (e) -> {
			        OwlcmsSession.withFop(fop -> {
				        BreakDialog dialog = new BreakDialog(BreakType.BEFORE_INTRODUCTION, CountdownType.TARGET, null,
				                this);
				        dialog.open();
			        });
		        });
		this.introCountdownButton.getElement().setAttribute("theme", "primary contrast");

		this.startLiftingButton = new Button(getTranslation("startLifting"), new Icon(VaadinIcon.MICROPHONE),
		        (e) -> {
			        OwlcmsSession.withFop(fop -> {
				        UI.getCurrent().access(() -> getRouterLayout().setMenuArea(createTopBar()));
				        fop.fopEventPost(new FOPEvent.StartLifting(this));
			        });
		        });
		this.startLiftingButton.getThemeNames().add("success primary");

		this.showResultsButton = new Button(getTranslation("ShowResults"), new Icon(VaadinIcon.MEDAL),
		        (e) -> {
			        OwlcmsSession.withFop(fop -> {
				        UI.getCurrent().access(() -> getRouterLayout().setMenuArea(createTopBar()));
				        fop.fopEventPost(
				                new FOPEvent.BreakStarted(BreakType.GROUP_DONE, CountdownType.INDEFINITE, null, null,
				                        true,
				                        this));
			        });
		        });
		this.showResultsButton.getThemeNames().add("success primary");
		this.showResultsButton.setVisible(false);

		this.warning = new H3();
		this.warning.getStyle().set("margin-top", "0").set("margin-bottom", "0");

		HorizontalLayout topBarRight = new HorizontalLayout();
		topBarRight.add(this.warning, this.introCountdownButton, this.startLiftingButton, this.showResultsButton);
		topBarRight.setSpacing(true);
		topBarRight.setPadding(true);
		topBarRight.setAlignItems(FlexComponent.Alignment.CENTER);

		this.topBar.removeAll();
		this.topBar.setSizeFull();
		this.topBar.add(getTopBarLeft(), topBarRight);

		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);
		this.topBar.setFlexGrow(0.2, getTopBarLeft());
		this.topBar.setFlexGrow(0.5, topBarRight);
		return this.topBar;
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#createReset()
	 */
	@Override
	protected Component createReset() {
		this.reset = new Button(getTranslation("Announcer.ReloadGroup"), new Icon(VaadinIcon.REFRESH),
		        (e) -> OwlcmsSession.withFop((fop) -> {
			        Group group = fop.getGroup();
			        logger.info("resetting {} from database", group);
			        // fop.loadGroup(group, this, true);
			        fop.fopEventPost(new FOPEvent.SwitchGroup(group, this));
			        syncWithFOP(true); // loadgroup does not refresh grid, true=ask for refresh
		        }));
		this.reset.getElement().setProperty("title", Translator.translate("Announcer.ReloadGroupTooltip"));

		this.reset.getElement().setAttribute("title", getTranslation("Reload_group"));
		this.reset.getElement().setAttribute("theme", "secondary contrast small icon");
		return this.reset;
	}

	/**
	 * Add key shortcuts to parent
	 *
	 * @see app.owlcms.nui.shared.AthleteGridContent#createStartTimeButton()
	 */
	@Override
	protected void createStartTimeButton() {
		super.createStartTimeButton();
		UI.getCurrent().addShortcutListener(() -> doStartTime(), Key.COMMA);
		UI.getCurrent().addShortcutListener(() -> doStartTime(), Key.SLASH);
		UI.getCurrent().addShortcutListener(() -> doToggleTime(), Key.NUMPAD_MULTIPLY);
		UI.getCurrent().addShortcutListener(() -> doToggleTime(), Key.DIGIT_8, KeyModifier.SHIFT);
	}

	/**
	 * Add key shortcuts to parent
	 *
	 * @see app.owlcms.nui.shared.AthleteGridContent#createStartTimeButton()
	 */
	@Override
	protected void createStopTimeButton() {
		super.createStopTimeButton();
		UI.getCurrent().addShortcutListener(() -> doStopTime(), Key.PERIOD);
	}

	@Override
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
		this.buttons.setPadding(false);
		this.buttons.setMargin(false);
		this.buttons.setSpacing(true);

		this.breaks = breakButtons(this.topBar);
		this.breaks.setPadding(false);
		this.breaks.setMargin(false);
		this.breaks.setSpacing(true);

		this.decisions = decisionButtons(this.topBar);
		this.decisions.setPadding(false);
		this.decisions.setMargin(false);
		this.decisions.setSpacing(true);
		this.decisions.setAlignItems(FlexComponent.Alignment.BASELINE);

		this.topBar.setSizeFull();
		this.topBar.add(topBarLeft, fullName, this.attempt, this.weight, time);
		if (this.buttons != null) {
			this.topBar.add(this.buttons);
		}
		if (this.decisions != null) {
			this.topBar.add(this.decisions);
		}
		if (this.breaks != null) {
			this.topBar.add(this.breaks);
		}

		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
		this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);
		this.topBar.setAlignSelf(Alignment.CENTER, this.attempt, this.weight, time);
		this.topBar.setFlexGrow(0.5, fullName);
		this.topBar.setFlexGrow(0.2, topBarLeft);
		return this.topBar;
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#createTopBarGroupSelect()
	 */
	@Override
	protected void createTopBarGroupSelect() {
		// there is already all the SQL filtering logic for the group attached
		// hidden field in the crudGrid part of the page so we just set that
		// filter.

		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<>());

		OwlcmsSession.withFop((fop) -> {
			Group group = fop.getGroup();
			logger.trace("initial setting group to {} {}", group, LoggerUtils.whereFrom());
			UI.getCurrent().access(()->getGroupFilter().setValue(group));
		});

		OwlcmsSession.withFop(fop -> {
			this.topBarMenu = new GroupSelectionMenu(groups, fop.getGroup(),
			        fop,
			        (g1) -> fop.fopEventPost(
			                new FOPEvent.SwitchGroup(g1.compareTo(fop.getGroup()) == 0 ? null : g1, this)),
			        (g1) -> fop.fopEventPost(new FOPEvent.SwitchGroup(null, this)));
			createTopBarSettingsMenu();
		});
	}

	@Override
	protected void createTopBarSettingsMenu() {
		this.topBarSettings = new MenuBar();
		this.topBarSettings.addThemeVariants(MenuBarVariant.LUMO_SMALL, MenuBarVariant.LUMO_TERTIARY_INLINE);
		MenuItem item2 = this.topBarSettings.addItem(new Icon(VaadinIcon.COG));
		SubMenu subMenu2 = item2.getSubMenu();

		FieldOfPlay fop = OwlcmsSession.getFop();
		MenuItem subItemSoundOn = subMenu2.addItem(
		        Translator.translate("DisplayParameters.ClockSoundOn"),
		        e -> {
			        switchSoundMode(!this.isSilenced(), true);
			        e.getSource().setChecked(!this.isSilenced());
			        if (this.timer != null) {
				        this.timer.setSilenced(this.isSilenced());
			        }
		        });
		subItemSoundOn.setCheckable(true);
		subItemSoundOn.setChecked(!this.isSilenced());

		MenuItem subItemDownOn = subMenu2.addItem(
		        Translator.translate("DisplayParameters.DownSoundOn"),
		        e -> {
			        switchDownMode(!this.isDownSilenced(), true);
			        e.getSource().setChecked(!this.isDownSilenced());
			        if (this.decisionDisplay != null) {
				        this.decisionDisplay.setSilenced(this.isDownSilenced());
			        }
		        });
		subItemDownOn.setCheckable(true);
		subItemDownOn.setChecked(!this.isDownSilenced());

		MenuItem subItemSingleRef = subMenu2.addItem(
		        Translator.translate("Settings.SingleReferee"));
		subItemSingleRef.setCheckable(true);
		subItemSingleRef.setChecked(this.isSingleReferee());

		MenuItem immediateDecision = subMenu2.addItem(
		        Translator.translate("Settings.ImmediateDecision"));
		immediateDecision.setCheckable(true);
		immediateDecision.setChecked(fop.isAnnouncerDecisionImmediate());

		immediateDecision.addClickListener(e -> {
			boolean announcerDecisionImmediate = !fop.isAnnouncerDecisionImmediate();
			switchImmediateDecisionMode(this, announcerDecisionImmediate, true);
			switchSingleRefereeMode(this, !announcerDecisionImmediate, true);
			subItemSingleRef.setChecked(!announcerDecisionImmediate);
			immediateDecision.setChecked(announcerDecisionImmediate);
		});
		subItemSingleRef.addClickListener(e -> {
			// single referee implies not immediate so down is shown
			boolean singleReferee2 = !this.isSingleReferee();
			switchSingleRefereeMode(this, singleReferee2, true);
			switchImmediateDecisionMode(this, !singleReferee2, true);
			subItemSingleRef.setChecked(singleReferee2);
			immediateDecision.setChecked(!singleReferee2);
			e.getSource().setChecked(singleReferee2);
		});

	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#decisionButtons(com.vaadin.flow.component.orderedlayout.HorizontalLayout)
	 */
	@Override
	protected HorizontalLayout decisionButtons(FlexLayout announcerBar) {
		Button good = new Button(new Icon(VaadinIcon.CHECK), (e) -> goodLift());
		good.getElement().setAttribute("theme", "success icon");
		UI.getCurrent().addShortcutListener(() -> goodLift(), Key.F2);

		Button bad = new Button(new Icon(VaadinIcon.CLOSE), (e) -> badLift());
		bad.getElement().setAttribute("theme", "error icon");
		UI.getCurrent().addShortcutListener(() -> badLift(), Key.F4);

		HorizontalLayout decisions = new HorizontalLayout(good, bad);
		return decisions;
	}

	private void badLift() {
		OwlcmsSession.withFop(fop -> {
			long now = System.currentTimeMillis();
			long timeElapsed = now - this.previousBadMillis;
			if (timeElapsed > 2000 || isSingleReferee()) {
				if (isSingleReferee()
				        && (fop.getState() == FOPState.TIME_STOPPED || fop.getState() == FOPState.TIME_RUNNING)) {
					fop.fopEventPost(new FOPEvent.DownSignal(this));
				}
				fop.fopEventPost(new FOPEvent.ExplicitDecision(fop.getCurAthlete(), this.getOrigin(), false,
				        false, false, false));
			}
			this.previousBadMillis = now;
		});
	}

	private void goodLift() {
		OwlcmsSession.withFop(fop -> {
		    long now = System.currentTimeMillis();
		    long timeElapsed = now - this.previousGoodMillis;
		    // no reason to give two decisions close together
		    if (timeElapsed > 2000 || isSingleReferee()) {
		        if (isSingleReferee()
		                && (fop.getState() == FOPState.TIME_STOPPED
		                        || fop.getState() == FOPState.TIME_RUNNING)) {
			        fop.fopEventPost(new FOPEvent.DownSignal(this));
			        try {
				        Thread.sleep(1000);
			        } catch (InterruptedException e1) {

			        }
		        }
		        fop.fopEventPost(
		                new FOPEvent.ExplicitDecision(fop.getCurAthlete(), this.getOrigin(), true, true,
		                        true,
		                        true));
		    }
		    this.previousGoodMillis = now;
		});
	}


}
