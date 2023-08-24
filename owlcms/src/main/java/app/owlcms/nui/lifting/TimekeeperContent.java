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

import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.GroupSelectionMenu;
import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.shared.AthleteGridContent;
import app.owlcms.nui.shared.BreakDialog;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.uievents.BreakType;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.NaturalOrderComparator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "lifting/timekeeper", layout = OwlcmsLayout.class)
public class TimekeeperContent extends AthleteGridContent implements HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(TimekeeperContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	private ShortcutRegistration startReg;
	private ShortcutRegistration stopReg;
	private ShortcutRegistration _1minReg;
	private ShortcutRegistration _2minReg;

	Map<String, List<String>> urlParameterMap = new HashMap<String, List<String>>();
	private ShortcutRegistration startReg2;
	private ShortcutRegistration toggleReg;
	private ShortcutRegistration toggleReg2;

	public TimekeeperContent() {
		super();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
	 */
	@Override
	public Athlete add(Athlete athlete) {
		// do nothing
		return athlete;
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

	/*
	 * (non-Javadoc)
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#delete(java.lang.Object)
	 */
	@Override
	public void delete(Athlete Athlete) {
		// do nothing;
	}

	@Override
	public Collection<Athlete> findAll() {
		return ImmutableList.of();
	}

	@Override
	public String getMenuTitle() {
		return getTranslation("Timekeeper") + OwlcmsSession.getFopNameIfMultiple();
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation("Timekeeper") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	public Map<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
	}

	@Override
	public boolean isIgnoreGroupFromURL() {
		return false;
	}

	@Override
	public void setHeaderContent() {
		getRouterLayout().setMenuTitle(getMenuTitle());
		getRouterLayout().setMenuArea(createInitialBar());
		getRouterLayout().showLocaleDropdown(false);
		getRouterLayout().setDrawerOpened(false);
		getRouterLayout().updateHeader(true);
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> newParameterMap) {
		this.urlParameterMap = newParameterMap;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public Athlete update(Athlete athlete) {
		// do nothing
		return athlete;
	}

	private void createBottom() {
		this.removeAll();
		if (timer == null) {
			timer = new AthleteTimerElement("");
		}
		VerticalLayout time = new VerticalLayout();
		time.setWidth("50%");

		time.getElement().getStyle().set("font-size", "15vh");
		time.getElement().getStyle().set("font-weight", "bold");
		time.setAlignItems(Alignment.CENTER);
		time.setAlignSelf(Alignment.CENTER, timer);
		centerH(timer, time);
		this.add(time);

		createStartTimeButton();
		createStopTimeButton();
		create1MinButton();
		create2MinButton();

		registerShortcuts();

//		_1min = new Button("1:00", (e2) -> {
//			OwlcmsSession.withFop(fop -> {
//				fop.fopEventPost(new FOPEvent.ForceTime(60000, this.getOrigin()));
//			});
//		});
//		_1min.getElement().setAttribute("theme", "icon");
//
//		_2min = new Button("2:00", (e3) -> {
//			OwlcmsSession.withFop(fop -> {
//				fop.fopEventPost(new FOPEvent.ForceTime(120000, this.getOrigin()));
//			});
//		});
//		_2min.getElement().setAttribute("theme", "icon");

		startTimeButton.setSizeFull();
		stopTimeButton.setSizeFull();
		_1min.setHeight("15vh");
		_1min.setWidthFull();
		_2min.setHeight("15vh");
		_2min.setWidthFull();
		// required by Vaadin v24.
		startTimeButton.getStyle().set("flex-shrink", "1");
		stopTimeButton.getStyle().set("flex-shrink", "1");
		_1min.getStyle().set("flex-shrink", "1");
		_2min.getStyle().set("flex-shrink", "1");

		VerticalLayout resets = new VerticalLayout(_1min, _2min);
		resets.setWidthFull();

		buttons = new HorizontalLayout(startTimeButton, stopTimeButton, resets);
		time.getStyle().set("margin-top", "3vh");
		time.getStyle().set("margin-bottom", "3vh");
		buttons.setWidth("75%");
		buttons.setHeight("40vh");
		buttons.setAlignItems(FlexComponent.Alignment.CENTER);
		buttons.getStyle().set("--lumo-font-size-m", "10vh");

		centerHW(buttons, this);
	}

	private void hideButtons() {
		buttons.setVisible(false);
		timer.getElement().setVisible(false);
		unregisterShortcuts();
	}

	private void registerShortcuts() {
		startReg = UI.getCurrent().addShortcutListener(() -> doStartTime(), Key.COMMA);
		startReg2 = UI.getCurrent().addShortcutListener(() -> doStartTime(), Key.SLASH);
		stopReg = UI.getCurrent().addShortcutListener(() -> doStopTime(), Key.PERIOD);
		toggleReg = UI.getCurrent().addShortcutListener(() -> doToggleTime(), Key.DIGIT_8, KeyModifier.SHIFT);
		toggleReg2 = UI.getCurrent().addShortcutListener(() -> doToggleTime(), Key.NUMPAD_MULTIPLY);
		_1minReg = UI.getCurrent().addShortcutListener(() -> do1Minute(), Key.NUMPAD_ADD);
		_1minReg = UI.getCurrent().addShortcutListener(() -> doToggleTime(), Key.EQUAL, KeyModifier.SHIFT);
		_2minReg = UI.getCurrent().addShortcutListener(() -> do2Minutes(), Key.EQUAL);

	}

	private void showButtons() {
		if (buttons != null) {
			buttons.setVisible(true);
		}
		timer.getElement().setVisible(true);
	}

	private void unregisterShortcuts() {
		if (startReg != null) {
			startReg.remove();
			startReg = null;
		}
		if (startReg2 != null) {
			startReg2.remove();
			startReg2 = null;
		}
		if (stopReg != null) {
			stopReg.remove();
			stopReg = null;
		}
		if (toggleReg != null) {
			toggleReg.remove();
			toggleReg = null;
		}
		if (toggleReg2 != null) {
			toggleReg2.remove();
			toggleReg2 = null;
		}
		if (_1minReg != null) {
			_1minReg.remove();
			_1minReg = null;
		}
		if (_2minReg != null) {
			_2minReg.remove();
			_2minReg = null;
		}
	}

	@Override
	protected HorizontalLayout announcerButtons(FlexLayout announcerBar) {
		createStartTimeButton();
		createStopTimeButton();
		create1MinButton();
		create2MinButton();

		HorizontalLayout buttons = new HorizontalLayout(startTimeButton, stopTimeButton, _1min, _2min);
		buttons.getStyle().set("background-color", "pink");
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return buttons;
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#createInitialBar()
	 */
	@Override
	protected FlexLayout createInitialBar() {

		topBar = new FlexLayout();
		initialBar = true;

		createTopBarGroupSelect();
		createTopBarLeft();

		introCountdownButton = new Button(getTranslation("introCountdown"), new Icon(VaadinIcon.TIMER), (e) -> {
			OwlcmsSession.withFop(fop -> {
				BreakDialog dialog = new BreakDialog(BreakType.BEFORE_INTRODUCTION, CountdownType.TARGET, null, this);
				dialog.open();
			});
		});
		introCountdownButton.getElement().setAttribute("theme", "primary contrast");

		startLiftingButton = new Button(getTranslation("startLifting"), new Icon(VaadinIcon.MICROPHONE), (e) -> {
			OwlcmsSession.withFop(fop -> {
				UI.getCurrent().access(() -> createTopBar());
				fop.fopEventPost(new FOPEvent.StartLifting(this));
			});
		});
		startLiftingButton.getThemeNames().add("success primary");

		showResultsButton = new Button(getTranslation("ShowResults"), new Icon(VaadinIcon.MEDAL), (e) -> {
			OwlcmsSession.withFop(fop -> {
				UI.getCurrent().access(() -> createTopBar());
				fop.fopEventPost(
				        new FOPEvent.BreakStarted(BreakType.GROUP_DONE, CountdownType.INDEFINITE, null, null, true,
				                this));
			});
		});
		showResultsButton.getThemeNames().add("success primary");
		showResultsButton.setVisible(false);

		warning = new H3();
		warning.getStyle().set("margin-top", "0").set("margin-bottom", "0");
		HorizontalLayout topBarRight = new HorizontalLayout();
		topBarRight.add(warning, introCountdownButton, startLiftingButton, showResultsButton);
		topBarRight.setSpacing(true);
		topBarRight.setPadding(true);
		topBarRight.setAlignItems(FlexComponent.Alignment.CENTER);

		topBar.removeAll();
		topBar.setSizeFull();
		topBar.add(getTopBarLeft(), topBarRight);

		topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		topBar.setAlignItems(FlexComponent.Alignment.CENTER);
		topBar.setFlexGrow(0.0, getTopBarLeft());
		topBar.setFlexGrow(1.0, topBarRight);
		return topBar;
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
			timer = new AthleteTimerElement("");
		}
		timer.setSilenced(this.isSilenced());
		H1 time = new H1(timer);
		clearVerticalMargins(attempt);
		clearVerticalMargins(time);
		clearVerticalMargins(weight);

		breaks = breakButtons(topBar);
		breaks.setPadding(false);
		breaks.setMargin(false);
		breaks.setSpacing(true);

		topBar.setSizeFull();
		topBar.add(topBarLeft, fullName, attempt, weight, time);

		if (breaks != null) {
			topBar.add(breaks);
		}

		topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
		topBar.setAlignItems(FlexComponent.Alignment.CENTER);
		topBar.setAlignSelf(Alignment.CENTER, attempt, weight, time);
		topBar.setFlexGrow(0.5, fullName);
		topBar.setFlexGrow(0.0, topBarLeft);
		return topBar;
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
		groups.sort(new NaturalOrderComparator<Group>());

		OwlcmsSession.withFop(fop -> {
			topBarMenu = new GroupSelectionMenu(groups, fop.getGroup(),
			        fop,
			        (g1) -> fop.fopEventPost(
			                new FOPEvent.SwitchGroup(g1.compareTo(fop.getGroup()) == 0 ? null : g1, this)),
			        (g1) -> fop.fopEventPost(new FOPEvent.SwitchGroup(null, this)));
			createTopBarSettingsMenu();
		});

	}

	@Override
	protected HorizontalLayout decisionButtons(FlexLayout announcerBar) {
		HorizontalLayout decisions = new HorizontalLayout();
		return decisions;
	}

	@Override
	protected void doUpdateTopBar(Athlete athlete, Integer timeAllowed) {
		super.doUpdateTopBar(athlete, timeAllowed);
	}

	@Override
	protected void init() {
		crudLayout = null;
	}

	@Override
	protected void syncWithFOP(boolean refreshGrid) {
		OwlcmsSession.withFop((fop) -> {
			Group fopGroup = fop.getGroup();
			logger.debug("syncing FOP, group = {}, {}", fopGroup, LoggerUtils.whereFrom(2));

			Athlete curAthlete2 = fop.getCurAthlete();
			FOPState state = fop.getState();
			this.removeAll();
			if (state == FOPState.INACTIVE || (state == FOPState.BREAK && fop.getGroup() == null)) {
				logger.debug("initial: {} {} {} {}", state, fop.getGroup(), curAthlete2,
				        curAthlete2 == null ? 0 : curAthlete2.getAttemptsDone());
				getRouterLayout().setMenuTitle(getMenuTitle());
				getRouterLayout().setMenuArea(createInitialBar());
				getRouterLayout().updateHeader(true);

				warning.setText(getTranslation("IdlePlatform"));
				if (curAthlete2 == null || curAthlete2.getAttemptsDone() >= 6 || fop.getLiftingOrder().size() == 0) {
					topBarWarning(fop.getGroup(), curAthlete2 == null ? 0 : curAthlete2.getAttemptsDone(),
					        fop.getState(), fop.getLiftingOrder());
				}
			} else {
				logger.debug("active: {}", state);
				getRouterLayout().setMenuTitle("");
				getRouterLayout().setMenuArea(createTopBar());
				getRouterLayout().updateHeader(true);
				createBottom();

				if (state == FOPState.BREAK) {
					if (buttons != null) {
						hideButtons();
					}
					if (decisions != null) {
						decisions.setVisible(false);
					}
					busyBreakButton();
				} else {
					if (buttons != null) {
						showButtons();
					}
					if (decisions != null) {
						decisions.setVisible(true);
					}
					if (breakButton == null) {
						// logger.debug("breakButton is null\n{}", LoggerUtils. stackTrace());
						return;
					}
					breakButton.setText("");
					quietBreakButton(Translator.translateOrElseEmpty("Pause"));
				}
				breakButton.setEnabled(true);

				Athlete curAthlete = curAthlete2;
				int timeRemaining = fop.getAthleteTimer().getTimeRemaining();
				super.doUpdateTopBar(curAthlete, timeRemaining);
			}
		});
	}
}
