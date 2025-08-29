/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.lifting;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
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
import app.owlcms.components.elements.BreakTimerElement;
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
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;

/**
 * WodkeeperContent is a near-copy of TimekeeperContent but adjusted to control
 * the break timer with a 30:00 preset.
 */
@SuppressWarnings("serial")
@Route(value = "lifting/wodkeeper", layout = OwlcmsLayout.class)
public class WodkeeperContent extends AthleteGridContent implements HasDynamicTitle {

    final private Logger logger;
    // remembered break duration/remaining in milliseconds (set by 30:00 button or remembered on pause)
    private Integer rememberedBreakMillis = null;


    public WodkeeperContent() {
        // initialize loggers at construction time to ensure they are available during attach
        this.logger = LoggerFactory.getLogger(WodkeeperContent.class);
    }

    @Override
    public Athlete add(Athlete athlete) {
        return athlete;
    }

    @Override
    public FlexLayout createMenuArea() {
        return null;
    }

    @Override
    public void delete(Athlete Athlete) {
    }

    @Override
    public Collection<Athlete> findAll() {
        return ImmutableList.of();
    }

    @Override
    public String getMenuTitle() {
        return Translator.translate("Wodkeeper") + OwlcmsSession.getFopNameIfMultiple();
    }

    @Override
    public String getPageTitle() {
        return Translator.translate("Wodkeeper") + OwlcmsSession.getFopNameIfMultiple();
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
    public Athlete update(Athlete athlete) {
        return athlete;
    }

    @Override
    protected HorizontalLayout announcerButtons(FlexLayout announcerBar) {
    createStartTimeButton();
    createStopTimeButton();
    // override the 1min button to be a 30:00 break preset
    create1MinButton();
    HorizontalLayout buttons = new HorizontalLayout(this.startTimeButton, this.stopTimeButton, this._1min);
        buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
        return buttons;
    }

    @Override
    protected FlexLayout createInitialBar() {

        this.topBar = new FlexLayout();
        this.initialBar = true;

        createTopBarGroupSelect();
        createTopBarLeft();

        this.introCountdownButton = new Button(Translator.translate("introCountdown"), new Icon(VaadinIcon.TIMER), (e) -> {
            OwlcmsSession.withFop(fop -> {
                BreakDialog dialog = new BreakDialog(BreakType.BEFORE_INTRODUCTION, CountdownType.TARGET, null, this);
                dialog.open();
            });
        });
        this.introCountdownButton.getElement().setAttribute("theme", "primary contrast");

        this.startLiftingButton = new Button(Translator.translate("startLifting"), new Icon(VaadinIcon.MICROPHONE), (e) -> {
            OwlcmsSession.withFop(fop -> {
                UI.getCurrent().access(() -> createTopBar());
                fop.fopEventPost(new FOPEvent.StartLifting(this));
            });
        });
        this.startLiftingButton.getThemeNames().add("success primary");

        this.showResultsButton = new Button(Translator.translate("ShowResults"), new Icon(VaadinIcon.MEDAL), (e) -> {
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

    @Override
    protected void createStartTimeButton() {
        super.createStartTimeButton();
    }

    @Override
    protected void createStopTimeButton() {
        super.createStopTimeButton();
    }

    /**
     * Start or resume time. If the FieldOfPlay is in BREAK state or a rememberedBreakMillis is present,
     * start/resume the break timer using FOP APIs. Otherwise fall back to athlete clock behaviour.
     */
    @Override
    protected void doStartTime() {
        // delegate to FOP when break is intended
        OwlcmsSession.withFop(fop -> {
            // if remembered break is set or we are already in BREAK state, start/resume break
            if (this.rememberedBreakMillis != null || fop.getState() == FOPState.BREAK) {
                int ms = this.rememberedBreakMillis != null ? this.rememberedBreakMillis : fop.getBreakTimer().getTimeRemaining();
                if (ms <= 0) {
                    // nothing to start
                    return;
                }
                // configure FOP break timer to remembered value, change state and start
                fop.setBreakType(app.owlcms.uievents.BreakType.TECHNICAL);
                fop.getBreakTimer().setTimeRemaining(ms, false);
                fop.getBreakTimer().setBreakDuration(ms);
                fop.getBreakTimer().setEnd(null);
        fop.fopEventPost(new FOPEvent.BreakStarted(BreakType.TECHNICAL, CountdownType.DURATION, ms, null,
            true, this));
        // push UI event so displays start
        fop.pushOutUIEvent(new UIEvent.BreakStarted(ms, this, false, BreakType.TECHNICAL,
            CountdownType.DURATION, LoggerUtils.stackTrace(), Boolean.FALSE, fop));
                // once started, clear remembered value
                this.rememberedBreakMillis = null;
                return;
            }
            // Not a break start: fallback to athlete clock start behaviour - call parent behaviour
            super.doStartTime();
        });
    }

    /**
     * Stop or pause time. If currently in BREAK state, pause the break timer and remember remaining ms.
     * Otherwise delegate to parent behaviour (athlete timer stop).
     */
    @Override
    protected void doStopTime() {
        OwlcmsSession.withFop(fop -> {
            if (fop.getState() == FOPState.BREAK) {
                // read remaining time and stop the break timer
                int remaining = fop.getBreakTimer().liveTimeRemaining();
                // stop the server break timer
                fop.getBreakTimer().stop();
                // remember remaining time for next start
                this.rememberedBreakMillis = remaining;
                // push UI paused event so clients stop and display remembered time
                fop.pushOutUIEvent(new UIEvent.BreakPaused(remaining, this, false, fop.getBreakType(),
                        fop.getCountdownType(), fop));
                return;
            }
            // fallback to athlete stop
            super.doStopTime();
        });
    }

    @Override
    protected void doToggleTime() {
        // toggle should respect break pause/start semantics
        OwlcmsSession.withFop(fop -> {
            if (fop.getState() == FOPState.BREAK || this.rememberedBreakMillis != null) {
                if (fop.getState() == FOPState.BREAK) {
                    doStopTime();
                } else {
                    doStartTime();
                }
                return;
            }
            super.doToggleTime();
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
            this.timer = new BreakTimerElement("");
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

    @Override
    protected void createTopBarGroupSelect() {
        if (Config.getCurrent().featureSwitch("enableTimeKeeperSessionSwitch")) {
            List<Group> groups = GroupRepository.findAll();
            groups.sort(Group.groupSelectionComparator);

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
    protected void syncWithFop(boolean refreshGrid, FieldOfPlay fop) {
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

            this.warning.setText(Translator.translate("IdlePlatform"));
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
                // For Wodkeeper we keep the control buttons visible during breaks
                if (this.buttons != null) {
                    this.buttons.setVisible(true);
                }
                if (this.timer != null) {
                    this.timer.getElement().setVisible(true);
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
    }

    private void createBottom() {
        this.removeAll();
        if (this.timer == null) {
            this.timer = new BreakTimerElement("");
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

        registerShortcuts();

        this.startTimeButton.setSizeFull();
        this.stopTimeButton.setSizeFull();
    this._1min.setHeight("15vh");
    this._1min.setWidthFull();
        this.startTimeButton.getStyle().set("flex-shrink", "1");
        this.stopTimeButton.getStyle().set("flex-shrink", "1");
    this._1min.getStyle().set("flex-shrink", "1");

    VerticalLayout resets = new VerticalLayout(this._1min);
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

    private void registerShortcuts() {
        UI.getCurrent().addShortcutListener(() -> doStartTime(), Key.COMMA);
        boolean notSpanish = !OwlcmsSession.getLocale().getLanguage().startsWith("es");
        boolean keepSpanishKeypadShortcut = Config.getCurrent().featureSwitch("keepSpanishHyphenShortcut");
        if (notSpanish || keepSpanishKeypadShortcut) {
            UI.getCurrent().addShortcutListener(() -> doStartTime(), Key.SLASH);
        }
        UI.getCurrent().addShortcutListener(() -> doStartTime(), Key.NUMPAD_DIVIDE);

        UI.getCurrent().addShortcutListener(() -> doStopTime(), Key.PERIOD);
        UI.getCurrent().addShortcutListener(() -> doStopTime(), Key.NUMPAD_DECIMAL);

        UI.getCurrent().addShortcutListener(() -> doToggleTime(), Key.DIGIT_8, KeyModifier.SHIFT);
        UI.getCurrent().addShortcutListener(() -> doToggleTime(), Key.NUMPAD_MULTIPLY);

        // keep the same shortcut keys as Timekeeper but redirect to 30:00
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

    // Override create1MinButton to provide a 30:00 preset for breaks
    @Override
    protected void create1MinButton() {
        this._1min = new Button("30:00", (e) -> do30Minutes());
        this._1min.getElement().setAttribute("theme", "icon");
    }

    // Override do1Minute to call our 30:00 action so shortcuts and callers work unchanged
    @Override
    protected void do1Minute() {
        do30Minutes();
    }

    // Implement 30-minute force break
    protected void do30Minutes() {
        OwlcmsSession.withFop(fop -> {
        int ms = 30 * 60 * 1000;
        // Set break type and configure the FOP break timer
        fop.setBreakType(BreakType.TECHNICAL);
        fop.getBreakTimer().setTimeRemaining(ms, false);
        fop.getBreakTimer().setBreakDuration(ms);
        fop.getBreakTimer().setEnd(null);
    // remember this preset so Start will use it (does not start immediately)
    this.rememberedBreakMillis = ms;

    // Do NOT start the break; instead update the server-side break timer values
    // and push a UIEvent.BreakSetTime so connected UIs display the 30:00 without starting.
    fop.pushOutUIEvent(new UIEvent.BreakSetTime(BreakType.TECHNICAL, CountdownType.DURATION, ms, null, false,
        this, LoggerUtils.stackTrace(), fop));
        });
    }

}
