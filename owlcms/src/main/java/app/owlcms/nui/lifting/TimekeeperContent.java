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
import app.owlcms.data.config.Config;
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
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	public TimekeeperContent() {
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

	@Override
	protected HorizontalLayout announcerButtons(FlexLayout announcerBar) {
		createStartTimeButton();
		createStopTimeButton();
		create1MinButton();
		create2MinButton();

		HorizontalLayout buttons = new HorizontalLayout(this.startTimeButton, this.stopTimeButton, this._1min,
		        this._2min);
		buttons.getStyle().set("background-color", "pink");
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return buttons;
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#createInitialBar()
	 */
	@Override
	protected FlexLayout createInitialBar() {

		this.topBar = new FlexLayout();
		this.initialBar = true;

		createTopBarGroupSelect();
		createTopBarLeft();

		this.introCountdownButton = new Button(getTranslation("introCountdown"), new Icon(VaadinIcon.TIMER), (e) -> {
			OwlcmsSession.withFop(fop -> {
				BreakDialog dialog = new BreakDialog(BreakType.BEFORE_INTRODUCTION, CountdownType.TARGET, null, this);
				dialog.open();
			});
		});
		this.introCountdownButton.getElement().setAttribute("theme", "primary contrast");

		this.startLiftingButton = new Button(getTranslation("startLifting"), new Icon(VaadinIcon.MICROPHONE), (e) -> {
			OwlcmsSession.withFop(fop -> {
				UI.getCurrent().access(() -> createTopBar());
				fop.fopEventPost(new FOPEvent.StartLifting(this));
			});
		});
		this.startLiftingButton.getThemeNames().add("success primary");

		this.showResultsButton = new Button(getTranslation("ShowResults"), new Icon(VaadinIcon.MEDAL), (e) -> {
			OwlcmsSession.withFop(fop -> {
				UI.getCurrent().access(() -> createTopBar());
				fop.fopEventPost(
				        new FOPEvent.BreakStarted(BreakType.GROUP_DONE, CountdownType.INDEFINITE, null, null, true,
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
		this.topBar.setFlexGrow(0.0, getTopBarLeft());
		this.topBar.setFlexGrow(1.0, topBarRight);
		return this.topBar;
	}

	/**
	 * Add key shortcuts to parent
	 *
	 * @see app.owlcms.nui.shared.AthleteGridContent#createStartTimeButton()
	 */
	@Override
	protected void createStartTimeButton() {
		super.createStartTimeButton();
	}

	/**
	 * Add key shortcuts to parent
	 *
	 * @see app.owlcms.nui.shared.AthleteGridContent#createStartTimeButton()
	 */
	@Override
	protected void createStopTimeButton() {
		super.createStopTimeButton();
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
			this.timer = new AthleteTimerElement("");
		}
		this.timer.setSilenced(this.isSilenced());
		H1 time = new H1(this.timer);
		clearVerticalMargins(this.attempt);
		clearVerticalMargins(time);
		clearVerticalMargins(this.weight);

		this.breaks = breakButtons(this.topBar);
		this.breaks.setPadding(false);
		this.breaks.setMargin(false);
		this.breaks.setSpacing(true);

		this.topBar.setSizeFull();
		this.topBar.add(topBarLeft, fullName, this.attempt, this.weight, time);

		if (this.breaks != null) {
			this.topBar.add(this.breaks);
		}

		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
		this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);
		this.topBar.setAlignSelf(Alignment.CENTER, this.attempt, this.weight, time);
		this.topBar.setFlexGrow(0.5, fullName);
		this.topBar.setFlexGrow(0.0, topBarLeft);
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
		if (Config.getCurrent().featureSwitch("enableTimeKeeperSessionSwitch")) {
			List<Group> groups = GroupRepository.findAll();
			groups.sort(new NaturalOrderComparator<>());

			OwlcmsSession.withFop(fop -> {
				this.topBarMenu = new GroupSelectionMenu(groups, fop.getGroup(),
				        fop,
				        (g1) -> fop.fopEventPost(
				                new FOPEvent.SwitchGroup(g1.compareTo(fop.getGroup()) == 0 ? null : g1, this)),
				        (g1) -> fop.fopEventPost(new FOPEvent.SwitchGroup(null, this)));
				createTopBarSettingsMenu();
			});
		} else {
			super.createTopBarGroupSelect();
		}

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
		this.crudLayout = null;
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

				this.warning.setText(getTranslation("IdlePlatform"));
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
					if (this.buttons != null) {
						hideButtons();
					}
					if (this.decisions != null) {
						this.decisions.setVisible(false);
					}
					busyBreakButton();
				} else {
					if (this.buttons != null) {
						showButtons();
					}
					if (this.decisions != null) {
						this.decisions.setVisible(true);
					}
					if (this.breakButton == null) {
						// logger.debug("breakButton is null\n{}", LoggerUtils. stackTrace());
						return;
					}
					this.breakButton.setText("");
					quietBreakButton(Translator.translateOrElseEmpty("Pause"));
				}
				this.breakButton.setEnabled(true);

				Athlete curAthlete = curAthlete2;
				int timeRemaining = fop.getAthleteTimer().getTimeRemaining();
				super.doUpdateTopBar(curAthlete, timeRemaining);
			}
		});
	}

	private void createBottom() {
		this.removeAll();
		if (this.timer == null) {
			this.timer = new AthleteTimerElement("");
		}
		VerticalLayout time = new VerticalLayout();
		time.setWidth("50%");

		time.getElement().getStyle().set("font-size", "15vh");
		time.getElement().getStyle().set("font-weight", "bold");
		time.setAlignItems(Alignment.CENTER);
		time.setAlignSelf(Alignment.CENTER, this.timer);
		centerH(this.timer, time);
		this.add(time);

		createStartTimeButton();
		createStopTimeButton();
		create1MinButton();
		create2MinButton();

		registerShortcuts();

		// _1min = new Button("1:00", (e2) -> {
		// OwlcmsSession.withFop(fop -> {
		// fop.fopEventPost(new FOPEvent.ForceTime(60000, this.getOrigin()));
		// });
		// });
		// _1min.getElement().setAttribute("theme", "icon");
		//
		// _2min = new Button("2:00", (e3) -> {
		// OwlcmsSession.withFop(fop -> {
		// fop.fopEventPost(new FOPEvent.ForceTime(120000, this.getOrigin()));
		// });
		// });
		// _2min.getElement().setAttribute("theme", "icon");

		this.startTimeButton.setSizeFull();
		this.stopTimeButton.setSizeFull();
		this._1min.setHeight("15vh");
		this._1min.setWidthFull();
		this._2min.setHeight("15vh");
		this._2min.setWidthFull();
		// required by Vaadin v24.
		this.startTimeButton.getStyle().set("flex-shrink", "1");
		this.stopTimeButton.getStyle().set("flex-shrink", "1");
		this._1min.getStyle().set("flex-shrink", "1");
		this._2min.getStyle().set("flex-shrink", "1");

		VerticalLayout resets = new VerticalLayout(this._1min, this._2min);
		resets.setWidthFull();

		this.buttons = new HorizontalLayout(this.startTimeButton, this.stopTimeButton, resets);
		time.getStyle().set("margin-top", "3vh");
		time.getStyle().set("margin-bottom", "3vh");
		this.buttons.setWidth("75%");
		this.buttons.setHeight("40vh");
		this.buttons.setAlignItems(FlexComponent.Alignment.CENTER);
		this.buttons.getStyle().set("--lumo-font-size-m", "10vh");

		centerHW(this.buttons, this);
	}

	private void hideButtons() {
		this.buttons.setVisible(false);
		this.timer.getElement().setVisible(false);
	}

	private void registerShortcuts() {
		UI.getCurrent().addShortcutListener(() -> doStartTime(), Key.COMMA);
		UI.getCurrent().addShortcutListener(() -> doStartTime(), Key.SLASH);
		UI.getCurrent().addShortcutListener(() -> doStartTime(), Key.NUMPAD_DIVIDE);

		UI.getCurrent().addShortcutListener(() -> doStopTime(), Key.PERIOD);
		UI.getCurrent().addShortcutListener(() -> doStopTime(), Key.NUMPAD_DECIMAL);

		UI.getCurrent().addShortcutListener(() -> doToggleTime(), Key.DIGIT_8, KeyModifier.SHIFT);
		UI.getCurrent().addShortcutListener(() -> doToggleTime(), Key.NUMPAD_MULTIPLY);

		UI.getCurrent().addShortcutListener(() -> do1Minute(), Key.EQUAL, KeyModifier.SHIFT);
		UI.getCurrent().addShortcutListener(() -> do1Minute(), Key.NUMPAD_ADD);

		UI.getCurrent().addShortcutListener(() -> do2Minutes(), Key.EQUAL);
		UI.getCurrent().addShortcutListener(() -> do2Minutes(), Key.NUMPAD_EQUAL);
		UI.getCurrent().addShortcutListener(() -> do2Minutes(), Key.SEMICOLON);
	}

	private void showButtons() {
		if (this.buttons != null) {
			this.buttons.setVisible(true);
		}
		this.timer.getElement().setVisible(true);
	}
}
