/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
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
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeButton;
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
import app.owlcms.data.config.Config;
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
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.Decision;
import app.owlcms.utils.LoggerUtils;
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
	private Dialog juryConfirmationDialog;
	private boolean liveLights;
	private boolean declarations;
	private boolean centerNotifications;
	private UI ui;

	public AnnouncerContent() {
		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied.
		setDefaultParameters(QueryParameters.simple(Map.of(
		        SoundParameters.SILENT, "true",
		        SoundParameters.DOWNSILENT, "true",
		        SoundParameters.IMMEDIATE, "true",
		        SoundParameters.SINGLEREF, "false",
		        SoundParameters.LIVE_LIGHTS, Boolean.toString(!Config.getCurrent().featureSwitch("noLiveLights")),
		        SoundParameters.SHOW_DECLARATIONS, "false",
		        SoundParameters.CENTER_NOTIFICATIONS, Boolean.toString(Config.getCurrent().featureSwitch("centerAnnouncerNotifications")),
		        SoundParameters.START_ORDER, "false")));
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
		FieldOfPlay fop = getFop();
		if (fop != null) {
			logger.trace("{}findAll {} {}", FieldOfPlay.getLoggingName(fop),
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
		String suffix = FieldOfPlay.getFopNameIfMultiple(getFop());
		return Translator.translate("Announcer") + suffix;
	}

	@Override
	public boolean isCenterNotifications() {
		return this.centerNotifications;
	}

	@Override
	public boolean isDeclarations() {
		return this.declarations;
	}

	/**
	 * The URL contains the group, contrary to other screens.
	 *
	 * Normally there is only one announcer. If we have to restart the program the announcer screen will have the URL correctly set. if there is no current
	 * group in the FOP, the announcer will (exceptionally set it)
	 *
	 * @see app.owlcms.nui.shared.AthleteGridContent#isIgnoreGroupFromURL()
	 */
	@Override
	public boolean isIgnoreGroupFromURL() {
		return false;
	}

	@Override
	public boolean isLiveLights() {
		// logger.debug("is live lights {} -- {}",this.liveLights, LoggerUtils.whereFrom());
		return this.liveLights;
	}

	@Override
	public boolean isSingleReferee() {
		return this.singleReferee;
	}

	@Override
	public void setCenterNotifications(boolean centerNotifications) {
		// logger.debug"setCenterNotifications {} {}",centerNotifications,LoggerUtils.whereFrom());
		this.centerNotifications = centerNotifications;
	}

	@Override
	public void setDeclarations(boolean showDeclarations) {
		this.declarations = showDeclarations;
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
	public void setLiveLights(boolean showLiveLights) {
		this.liveLights = showLiveLights;
	}

	@Override
	public void setSingleReferee(boolean b) {
		this.singleReferee = b;
	}

	@Override
	@Subscribe
	public void slaveJuryNotification(UIEvent.JuryNotification e) {
		ui.access(() -> {
			JuryDeliberationEventType et = e.getDeliberationEventType();
			
			String text = "";
			String style = "warning";

			logger.debug("slaveJuryNotification {} sent={} {}", et, this.deliberationNotificationSent, LoggerUtils.whereFrom());
			switch (et) {
				case CALL_REFEREES:
					text = Translator.translate("JuryNotification." + et.name());
					if (!this.deliberationNotificationSent) {
						doStoppageDialog(text, style);
					}
					this.summonNotificationSent = true;
					return;
				case START_DELIBERATION:
				case CHALLENGE:
					// Show jury decision dialog immediately with Good Lift/No Lift buttons
					if (e.isWaitForAnnouncer() && !this.deliberationNotificationSent) {
						juryDecisionDialog(e, null);
					}
					this.deliberationNotificationSent = true;
					return;
			case GOOD_LIFT:
			case BAD_LIFT:
				// Only update dialog if waiting for announcer (decision from jury)
				// Don't reopen if announcer gave the decision themselves
				if (e.isWaitForAnnouncer()) {
					juryDecisionDialog(e, et);
				}
				return;
				case END_CALL_REFEREES:
				case END_DELIBERATION:
				case END_TECHNICAL_PAUSE:
				case END_CHALLENGE:
					text = Translator.translate("JuryNotification." + et.name());
					if (this.stoppageAckNotification != null) {
						this.stoppageAckNotification.close();
					}
					break;
				case CALL_TECHNICAL_CONTROLLER:
					text = Translator.translate("JuryNotification.CallTechnicalController");
					doStoppageDialog(text, style);
					break;
				case LOADING_ERROR:
					text = Translator.translate("JuryNotification.LoadingError");
					break;
			case END_JURY_BREAK:
				this.summonNotificationSent = false;
				this.deliberationNotificationSent = false;
				// Close jury decision dialog when competition resumes
				if (this.juryConfirmationDialog != null) {
					this.juryConfirmationDialog.close();
					this.juryConfirmationDialog = null;
				}
				text = Translator.translate("JuryNotification.END_JURY_BREAK");
				break;
				case TECHNICAL_PAUSE:
					text = Translator.translate("BreakType.TECHNICAL");
					doStoppageDialog(text, style);
					return;
				case MARSHALL:
					text = Translator.translate("BreakType.MARSHAL");
					doStoppageDialog(text, style);
					return;
				default:
					break;
			}
			doNotification(text, style);
		});
	}

	private void doStoppageDialog(String text, String style) {

		if (this.stoppageAckNotification != null) {
			this.stoppageAckNotification.close();
		}
		this.stoppageAckNotification = new Notification();
		this.stoppageAckNotification.getElement().getThemeList().add("warning");
		this.stoppageAckNotification.setDuration(6000);

		Div label = new Div(text);

		NativeButton ackButton = new NativeButton(Translator.translate("JuryNotification.ACK"));
		ackButton.getStyle().set("margin-left", "1em");
		ackButton.addClickListener((event) -> {
			this.stoppageAckNotification.close();
			this.stoppageAckNotification = null;
		});

		NativeButton resumeButton = new NativeButton(Translator.translate("JuryNotification.END_JURY_BREAK"));
		resumeButton.getStyle().set("background-color", "darkgreen");
		resumeButton.getStyle().set("color", "white");
		resumeButton.getStyle().set("margin-left", "1em");
		resumeButton.addClickListener((event) -> {
			getFop().fopEventPost(new FOPEvent.StartLifting(null));
			this.stoppageAckNotification.close();
			this.stoppageAckNotification = null;
		});

		if (isCenterNotifications()) {
			label.getStyle().set("font-size", "x-large");
			ackButton.getStyle().set("font-size", "large");
			resumeButton.getStyle().set("font-size", "large");
			this.stoppageAckNotification.setPosition(Position.MIDDLE);
		} else {
			label.getStyle().set("font-size", "large");
			ackButton.getStyle().set("font-size", "normal");
			resumeButton.getStyle().set("font-size", "normal");
			this.stoppageAckNotification.setPosition(Position.TOP_START);
		}

		this.stoppageAckNotification.setDuration(0);
		this.stoppageAckNotification.add(label);
		this.stoppageAckNotification.add(ackButton);
		this.stoppageAckNotification.add(resumeButton);
		this.stoppageAckNotification.open();
	}

	@Subscribe
	@Override
	public void slaveStartLifting(UIEvent.StartLifting s) {
		currentUI.access(() -> {
			super.slaveStartLifting(s);
			if (this.stoppageAckNotification != null) {
				this.stoppageAckNotification.close();
				this.stoppageAckNotification = null;
			}
		});
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

			if (e == null || e.decision == null) {
				return;
			}
			// this.slaveUpdateGrid(e);
			int d = e.decision ? 1 : 0;
			String text = Translator.translate("NoLift_GoodLift", d, e.getAthlete().getFullName());

			Notification n = new Notification();
			String themeName = e.decision ? "success" : "error";
			n.getElement().getThemeList().add(themeName);

			Div label = new Div();
			label.add(text);
			label.addClickListener((event) -> n.close());
			label.setSizeFull();
			label.getStyle().set("font-size", "large");
			n.add(label);
			if (isCenterNotifications()) {
				n.setPosition(Position.MIDDLE);
				label.getStyle().set("font-size", "x-large");
			} else {
				n.setPosition(Position.TOP_START);
			}
			n.setDuration(5000);
			n.open();
			setDecisionLights(null);
		});
	}

	@Subscribe
	public void slaveStartTime(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			buttonsTimeStarted();
			displayLiveDecisions();
		});
	}

	@Override
	@Subscribe
	public void slaveUpdateGrid(Decision e) {
		// do nothing, prevents premature update of lifting order grid
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
		currentUI.access(() -> {
			currentUI.addShortcutListener(() -> do1Minute(), Key.NUMPAD_ADD);
			currentUI.addShortcutListener(() -> do1Minute(), Key.EQUAL, KeyModifier.SHIFT);
		});
	}

	@Override
	protected void create2MinButton() {
		super.create2MinButton();
		currentUI.access(() -> {
			currentUI.addShortcutListener(() -> do2Minutes(), Key.EQUAL);
			currentUI.addShortcutListener(() -> do2Minutes(), Key.NUMPAD_EQUAL);
			currentUI.addShortcutListener(() -> do2Minutes(), Key.SEMICOLON);
		});
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

		this.introCountdownButton = new Button(Translator.translate("introCountdown"), new Icon(VaadinIcon.TIMER),
		        (e) -> {
		        	BreakDialog dialog = new BreakDialog(getFop(), BreakType.BEFORE_INTRODUCTION, CountdownType.TARGET, null,
			                this);
			        dialog.open();
		        });
		this.introCountdownButton.getElement().setAttribute("theme", "primary contrast");

		this.startLiftingButton = new Button(Translator.translate("startLifting"), new Icon(VaadinIcon.MICROPHONE),
		        (e) -> {
			        currentUI.access(() -> getRouterLayout().setMenuArea(createTopBar()));
			        getFop().fopEventPost(new FOPEvent.StartLifting(this));
		        });
		this.startLiftingButton.getThemeNames().add("success primary");

		this.showResultsButton = new Button(Translator.translate("ShowResults"), new Icon(VaadinIcon.MEDAL),
		        (e) -> {
			        var fop = getFop();
			        currentUI.access(() -> getRouterLayout().setMenuArea(createTopBar()));
			        fop.fopEventPost(
			                new FOPEvent.BreakStarted(BreakType.GROUP_DONE, CountdownType.INDEFINITE, null, null,
			                        true,
			                        this));
		        });
		this.showResultsButton.getThemeNames().add("success primary");
		this.showResultsButton.setVisible(false);

		this.warning = new H3();
		this.warning.getStyle().set("margin-top", "0").set("margin-bottom", "0");

		HorizontalLayout topBarRight = new HorizontalLayout();
		this.breaks = breakButtons(topBar);
		this.breaks.setPadding(true);

		topBarRight.add(this.warning, this.introCountdownButton, this.startLiftingButton, this.showResultsButton, this.breaks);
		topBarRight.setWidthFull();
		topBarRight.setSpacing(true);
		topBarRight.setPadding(true);
		topBarRight.setAlignItems(FlexComponent.Alignment.CENTER);
		topBarRight.setAlignSelf(Alignment.CENTER, this.breaks);

		this.topBar.removeAll();
		this.topBar.setSizeFull();
		this.topBar.add(getTopBarLeft(), topBarRight, this.breaks);

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
		this.reset = new Button(Translator.translate("Announcer.ReloadGroup"), new Icon(VaadinIcon.REFRESH),
		        (e) -> {
			        var fop = getFop();
			        Group group = fop.getGroup();
			        logger.info("resetting {} from database", group);
			        // fop.loadGroup(group, this, true);
			        fop.fopEventPost(new FOPEvent.SwitchGroup(group, this));
			        syncWithFop(true, fop); // loadgroup does not refresh grid, true=ask for refresh
		        });
		this.reset.getElement().setProperty("title", Translator.translate("Announcer.ReloadGroupTooltip"));

		this.reset.getElement().setAttribute("title", Translator.translate("Reload_group"));
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
		boolean notSpanish = !OwlcmsSession.getLocale().getLanguage().startsWith("es");
		boolean keepSpanishKeypadShortcut = Config.getCurrent().featureSwitch("keepSpanishHyphenShortcut");
		currentUI.access(() -> {
			currentUI.addShortcutListener(() -> doStartTime(), Key.COMMA);
			if (notSpanish || keepSpanishKeypadShortcut) {
				currentUI.addShortcutListener(() -> doStartTime(), Key.SLASH);
			}
			currentUI.addShortcutListener(() -> doStartTime(), Key.NUMPAD_DIVIDE);
			currentUI.addShortcutListener(() -> doToggleTime(), Key.NUMPAD_MULTIPLY);
			currentUI.addShortcutListener(() -> doToggleTime(), Key.DIGIT_8, KeyModifier.SHIFT);
		});
	}

	/**
	 * Add key shortcuts to parent
	 *
	 * @see app.owlcms.nui.shared.AthleteGridContent#createStartTimeButton()
	 */
	@Override
	protected void createStopTimeButton() {
		super.createStopTimeButton();
		currentUI.access(() -> {
			currentUI.addShortcutListener(() -> doStopTime(), Key.PERIOD);
			currentUI.addShortcutListener(() -> doStopTime(), Key.NUMPAD_DECIMAL);
		});
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
		this.timer.setFop(getFop());
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
		groups.sort(Group.groupSelectionComparator);

		var fop = getFop();
		Group group = fop.getGroup();
		logger.trace("initial setting group to {} {}", group, LoggerUtils.whereFrom());
		currentUI.access(() -> getGroupFilter().setValue(group));

		this.topBarMenu = new GroupSelectionMenu(groups, fop.getGroup(),
		        fop,
		        (g1) -> fop.fopEventPost(
		                new FOPEvent.SwitchGroup(g1.compareTo(fop.getGroup()) == 0 ? null : g1, this)),
		        (g1) -> fop.fopEventPost(new FOPEvent.SwitchGroup(null, this)));
		createTopBarSettingsMenu();
	}

	@Override
	protected void createTopBarSettingsMenu() {
		this.topBarSettings = new MenuBar();
		this.topBarSettings.addThemeVariants(MenuBarVariant.LUMO_SMALL, MenuBarVariant.LUMO_TERTIARY_INLINE);
		MenuItem item2 = this.topBarSettings.addItem(new Icon(VaadinIcon.COG));
		SubMenu subMenu2 = item2.getSubMenu();

		// FieldOfPlay fop = OwlcmsSession.getFop();
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

		// MenuItem immediateDecision = subMenu2.addItem(
		// Translator.translate("Settings.ImmediateDecision"));
		// immediateDecision.setCheckable(true);
		// immediateDecision.setChecked(fop.isAnnouncerDecisionImmediate());

		MenuItem showLights = subMenu2.addItem(
		        Translator.translate("DisplayParameters.showDecisionLights"),
		        e -> {
			        switchLiveLightsMode(this, !this.isLiveLights(), true);
			        FieldOfPlay fop2 = getFop();
			        if (fop2 != null) {
				        fop2.setAnnouncerDecisionImmediate(false);
				        fop2.setSingleReferee(false);
			        }
			        switchImmediateDecisionMode(this, false, true);
			        // switchSingleRefereeMode(this, false, true);
			        e.getSource().setChecked(this.isLiveLights());
			        subItemSingleRef.setChecked(false);
		        });
		showLights.setCheckable(true);
		showLights.setChecked(this.isLiveLights());

		MenuItem centerDeclarations = subMenu2.addItem(
		        Translator.translate("DisplayParameters.centerNotifications"),
		        e -> {
			        switchCenteringMode(this, !this.isCenterNotifications(), true);
			        e.getSource().setChecked(this.isCenterNotifications());
		        });
		centerDeclarations.setCheckable(true);
		centerDeclarations.setChecked(this.isCenterNotifications());

		MenuItem showDeclarations = subMenu2.addItem(
		        Translator.translate("DisplayParameters.showDeclarationNotifications"),
		        e -> {
			        switchDeclarationsMode(this, !this.isDeclarations(), true);
			        e.getSource().setChecked(this.isDeclarations());
		        });
		showDeclarations.setCheckable(true);
		showDeclarations.setChecked(this.isDeclarations());

		// immediateDecision.addClickListener(e -> {
		// boolean announcerDecisionImmediate = !fop.isAnnouncerDecisionImmediate();
		// switchImmediateDecisionMode(this, announcerDecisionImmediate, true);
		// switchSingleRefereeMode(this, !announcerDecisionImmediate, true);
		// switchLiveLightsMode(this, !announcerDecisionImmediate, true);
		// subItemSingleRef.setChecked(!announcerDecisionImmediate);
		// immediateDecision.setChecked(announcerDecisionImmediate);
		// showLights.setChecked(isLiveLights());
		// });

		subItemSingleRef.addClickListener(e -> {
			boolean singleReferee2 = !this.isSingleReferee();
			switchSingleRefereeMode(this, singleReferee2, true);
			FieldOfPlay fop2 = getFop();
			if (fop2 != null) {
				// fop2.setAnnouncerDecisionImmediate(false);
				fop2.setSingleReferee(singleReferee2);
			}
			if (singleReferee2) {
				switchImmediateDecisionMode(this, false, true);
				// immediateDecision.setChecked(false);
			}
			// switchLiveLightsMode(this, !singleReferee2, true);
			subItemSingleRef.setChecked(singleReferee2);
			// immediateDecision.setChecked(!singleReferee2);
			showDeclarations.setChecked(isLiveLights());
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

		Button bad = new Button(new Icon(VaadinIcon.CLOSE), (e) -> badLift());
		bad.getElement().setAttribute("theme", "error icon");

		currentUI.access(() -> {
			currentUI.addShortcutListener(() -> goodLift(), Key.F2);
			currentUI.addShortcutListener(() -> badLift(), Key.F4);
		});

		HorizontalLayout decisions = new HorizontalLayout(good, bad);
		return decisions;
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		this.ui = UI.getCurrent();
		createTopBarGroupSelect();
		// setLiveLights(!Config.getCurrent().featureSwitch("noLiveLights"));
		// setCenterNotifications(Config.getCurrent().featureSwitch("centerAnnouncerNotifications"));
		defineFilters(this.getCrudGrid());
	}

	private void badLift() {
		var fop = getFop();
		long now = System.currentTimeMillis();
		long timeElapsed = now - this.previousBadMillis;
		if (timeElapsed > 2000 || isSingleReferee()) {
			if (isSingleReferee() && !fop.isAnnouncerDecisionImmediate()
			        && (fop.getState() == FOPState.TIME_STOPPED || fop.getState() == FOPState.TIME_RUNNING)) {
				fop.fopEventPost(new FOPEvent.DownSignal(this));
			}
			fop.fopEventPost(new FOPEvent.ExplicitDecision(fop.getCurAthlete(), this.getOrigin(), false,
			        false, false, false));
		}
		this.previousBadMillis = now;
	}

	private void goodLift() {
		var fop = getFop();
		long now = System.currentTimeMillis();
		long timeElapsed = now - this.previousGoodMillis;
		// no reason to give two decisions close together
		if (timeElapsed > 2000 || isSingleReferee()) {
			if (isSingleReferee() && !fop.isAnnouncerDecisionImmediate()
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
	}

	private void juryDecisionDialog(UIEvent.JuryNotification e, JuryDeliberationEventType juryDecision) {
		logger.debug("juryDecisionDialog called: juryDecision={} existingDialog={} {}", 
			juryDecision, (this.juryConfirmationDialog != null), LoggerUtils.whereFrom());
		if (this.juryConfirmationDialog != null && juryDecision == null) {
			// Dialog already exists, don't recreate it on START_DELIBERATION
			logger.debug("Dialog already exists, not recreating for START_DELIBERATION");
			return;
		}
		
		if (this.juryConfirmationDialog != null) {
			// Jury made a decision, close existing dialog
			logger.debug("Closing existing dialog for jury decision update");
			this.juryConfirmationDialog.close();
		}
		if (this.stoppageAckNotification != null) {
			this.stoppageAckNotification.close();
		}
		
		// Create custom dialog with proper footer
		logger.debug("Creating new JuryDecisionDialog");
		JuryDecisionDialog dialog = new JuryDecisionDialog(e, juryDecision, () -> {
			this.deliberationNotificationSent = false;
			this.juryConfirmationDialog = null;
		});
		
		this.juryConfirmationDialog = dialog;
		dialog.open();
		logger.debug("Dialog opened");
	}

}

