/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.shared;

import static app.owlcms.ui.shared.BreakManagement.CountdownType.DURATION;
import static app.owlcms.ui.shared.BreakManagement.CountdownType.INDEFINITE;
import static app.owlcms.ui.shared.BreakManagement.CountdownType.TARGET;
import static app.owlcms.uievents.BreakType.DURING_INTRODUCTION;
import static app.owlcms.uievents.BreakType.GROUP_DONE;
import static app.owlcms.uievents.BreakType.MEDALS;
import static app.owlcms.uievents.BreakType.TECHNICAL;
import static java.time.temporal.ChronoUnit.MILLIS;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.flowingcode.vaadin.addons.ironicons.PlacesIcons;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.TextRenderer;

import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.fields.DurationField;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.IBreakTimer;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.BreakSetTime;
import app.owlcms.uievents.UIEvent.BreakStarted;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class BreakManagement extends VerticalLayout implements SafeEventBusRegistration {
    public enum CountdownType {
        DURATION, TARGET, INDEFINITE
    }

    public enum DisplayType {
        COUNTDOWN_INFO, LIFT_INFO
    }

    private static final Duration DEFAULT_DURATION = Duration.ofMinutes(10L);

    final private Logger logger = (Logger) LoggerFactory.getLogger(BreakManagement.class);
    {
        logger.setLevel(Level.INFO);
    }

    private Button breakStart = null;
    private Button breakPause = null;
    private Button breakEnd = null;
    private Button breakReset = null;

    private Object origin;
    private Label minutes;
    private HorizontalLayout timer;

    private HorizontalLayout dt;
    private RadioButtonGroup<CountdownType> ct;
    private RadioButtonGroup<BreakType> bt;
    private DurationField durationField = new DurationField();
    private TimePicker timePicker = new TimePicker();
    private DatePicker datePicker = new DatePicker();
    private Dialog parentDialog;

    Long timeRemaining = null;
    private BreakTimerElement breakTimerElement;
    private Button athleteButton;
    private Button countdownButton;
    private BreakType requestedBreakType;
    private EventBus uiEventBus;

    private boolean ignoreBreakTypeValueChange = false;
    private boolean ignoreDurationValueChange = false;
    private boolean ignoreListeners = false;

    /**
     * Persona-specific display (e.g. for the jury, the technical controller, etc.)
     *
     * @param origin
     * @param brt
     * @param cdt
     */
    public BreakManagement(Object origin, BreakType brt, CountdownType cdt, Dialog parentDialog) {
        init(origin, brt, cdt, parentDialog);
        if (brt == BreakType.JURY || brt == BreakType.TECHNICAL) {
            setRequestedBreakType(brt);
        } else {
            setRequestedBreakType(null);
        }
    }

    /**
     * Used by the announcer, no specific context
     *
     * @param origin the origin
     */
    public BreakManagement(Object origin, Dialog parentDialog) {
        init(origin, null, CountdownType.DURATION, parentDialog);
        setRequestedBreakType(null);
    }

    public void cleanup() {
        logger.debug("removing {}", breakTimerElement);
        OwlcmsSession.withFop(fop -> {
            try {
                fop.getUiEventBus().unregister(breakTimerElement);
            } catch (Exception e) {
            }
            try {
                fop.getFopEventBus().unregister(breakTimerElement);
            } catch (Exception e) {
            }
            this.breakTimerElement = null;
        });
    }

    public ComponentEventListener<ClickEvent<Button>> endBreak(Dialog dialog) {
        return (e) -> {
            OwlcmsSession.withFop(fop -> {
                logger.debug("endBreak start lifting");
                fop.fopEventPost(new FOPEvent.StartLifting(this.getOrigin()));
                logger.debug("endbreak enabling start");
                breakStart.setEnabled(true);
                breakPause.setEnabled(false);
                breakEnd.setEnabled(false);
                fop.getUiEventBus().unregister(this);
                dialog.close();
            });
        };
    }

    public void init(Object origin, BreakType brt, CountdownType cdt, Dialog parentDialog) {
        logger.debug("init brt={} cdt={} from {}", brt, cdt, LoggerUtils.whereFrom());
        ignoreBreakTypeValueChange = false;
        this.setOrigin(origin);
        this.parentDialog = parentDialog;
        FlexLayout buttons = createButtons(this);

        createCountdowns();
        createDisplayInfoSelection();
        createTimerDisplay();
        computeDefaultValues();

        bt.setValue(brt);
        setCtValue(cdt);
        assembleDialog(this, buttons);
        OwlcmsSession.withFop(fop -> {
            uiEventBusRegister((Component) origin, fop);
        });
    }

    public CountdownType mapBreakTypeToCountdownType(BreakType bType) {
        CountdownType cType;
        if (bType == BreakType.FIRST_SNATCH || bType == BreakType.FIRST_CJ) {
            cType = CountdownType.DURATION;
        } else if (bType == BreakType.BEFORE_INTRODUCTION || bType == BreakType.GROUP_DONE) {
            cType = CountdownType.TARGET;
        } else {
            cType = CountdownType.INDEFINITE;
        }
        return cType;
    }

    public void masterPauseBreak(BreakType bType) {
        OwlcmsSession.withFop(fop -> {
            IBreakTimer breakTimer = fop.getBreakTimer();
            if (breakTimer.isRunning()) {
                // do not stop warmup timer for medal ceremonies between groups
                if (bType == null
                        || fop.getBreakType() != BreakType.FIRST_SNATCH
                        || (bType != BreakType.DURING_INTRODUCTION && bType != BreakType.MEDALS)) {
                    logger.warn("pausing current break {} due to {}", fop.getBreakType(), bType);
                    breakTimer.stop();
                    fop.fopEventPost(
                            new FOPEvent.BreakPaused(breakTimer.getTimeRemainingAtLastStop(), this.getOrigin()));
                }

            }
        });
        logger.debug("paused; enabling start");
        startEnabled();
    }

    /**
     * Pause and set time according to current fields
     */
    public void masterResetBreak() {
        CountdownType countdownType = ct.getValue();
        Integer tr = computeTimerRemainingFromFields(countdownType);
        doResetTimer(tr);
    }

    public void masterStartBreak() {
        OwlcmsSession.withFop(fop -> {
            masterStartBreak(fop);
        });
        // e.getSource().setEnabled(false);
        logger.debug("start break disable start");
        startDisabled();
        return;
    }

    public void masterStartBreak(FieldOfPlay fop) {
        BreakType breakType = bt.getValue();
        CountdownType countdownType = ct.getValue();
        fop.getFopEventBus()
                .post(new FOPEvent.BreakStarted(breakType, countdownType,
                        countdownType == CountdownType.INDEFINITE ? null : timeRemaining.intValue(), getTarget(),
                        this.getOrigin()));
    }

    @Subscribe
    public void slaveBreakDone(UIEvent.BreakDone e) {
        synchronized (this) {
            try {
                // logger.debug("Break Done {}", LoggerUtils. stackTrace());
                ignoreListeners = true;
                UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(),
                        () -> parentDialog.close());
            } finally {
                ignoreListeners = false;
            }
        }
    }

    @Subscribe
    public void slaveBreakPause(UIEvent.BreakPaused e) {
        synchronized (this) {
            try {
                ignoreListeners = true;
                UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
                    startEnabled();
                });
            } finally {
                ignoreListeners = false;
            }
        }

    }

    @Subscribe
    public void slaveBreakSet(UIEvent.BreakSetTime e) {
        // do nothing
    }

    @Subscribe
    public void slaveBreakStart(UIEvent.BreakStarted e) {
        synchronized (this) {
            try {
                ignoreListeners = true;
                if (e.isDisplayToggle()) {
                    return;
                }
                UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
                    startDisabled();
                    safeSetBT(e.getBreakType());
                });
            } finally {
                ignoreListeners = false;
            }
        }

    }

    public void startDisabled() {
        logger.debug("start disabled {}", LoggerUtils.whereFrom());
        breakStart.setEnabled(false);
        breakPause.setEnabled(true);
        breakEnd.setEnabled(true);
    }

    public void startEnabled() {
        logger.debug("start enabled {}", LoggerUtils.whereFrom());
        breakStart.setEnabled(true);
        breakPause.setEnabled(false);
        breakEnd.setEnabled(true);
    }

    public void startIndefiniteBreakImmediately(BreakType bType) {
        timeRemaining = null;
        ignoreBreakTypeValueChange = true;
        setCtValue(CountdownType.INDEFINITE);
        logger.debug("setting default duration for indefinite break");
        setDurationField(DEFAULT_DURATION);
        BreakType breakType = bType != null ? bType : BreakType.TECHNICAL;
        safeSetBT(breakType);
        masterStartBreak();
//        breakTimerElement.slaveBreakStart(new BreakStarted(null, this.getOrigin(), false, breakType, ct.getValue()));
    }

    /**
     * Everything has been created and has meaningful values, add value change listeners now to avoid spuriuous
     * triggering during interface build-up.
     *
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        logger.debug("breakManagement attach");
        super.onAttach(attachEvent);
        OwlcmsSession.withFop(fop -> {
            // we listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        });

        ct.addValueChangeListener(e -> {
            CountdownType cType = e.getValue();
            if (cType == CountdownType.DURATION) {
                switchToDuration();
            } else if (cType == CountdownType.TARGET) {
                switchToTarget();
            } else {
                switchToIndefinite();
            }
        });
        bt.addValueChangeListener((event) -> {
            // prevent infinite loop
            if (ignoreBreakTypeValueChange || ignoreListeners) {
                return;
            }

            BreakType bType = event.getValue();
            CountdownType mapBreakTypeToCountdownType = mapBreakTypeToCountdownType(bType);
            logger.debug("setting countdown {} ignored={}", mapBreakTypeToCountdownType, ignoreListeners);
            setCtValue(mapBreakTypeToCountdownType);
            masterPauseBreak(bType);

            if (bType != null && (bType == BreakType.JURY || bType == BreakType.TECHNICAL
                    || bType == BreakType.DURING_INTRODUCTION || bType == BreakType.MEDALS)) {
                logger.debug("starting break from radiobutton setvalue {}", bType);
                startIndefiniteBreakImmediately(bType);
            } else {
                setBreakTimerFromFields(ct.getValue());
            }
        });
        durationField.addValueChangeListener(e -> {
            if (ignoreDurationValueChange || ignoreListeners) {
                return;
            }
            computeTimerRemainingFromFields(CountdownType.DURATION);
            doResetTimer(timeRemaining.intValue());
        });
        timePicker.addValueChangeListener(e -> {
            if (ignoreListeners) {
                return;
            }
            computeTimerRemainingFromFields(CountdownType.TARGET);
            doResetTimer(timeRemaining.intValue());
        });
        datePicker.addValueChangeListener(e -> {
            if (ignoreListeners) {
                return;
            }
            computeTimerRemainingFromFields(CountdownType.TARGET);
            doResetTimer(timeRemaining.intValue());
        });

        OwlcmsSession.withFop(fop -> {
            if (fop.getState() != FOPState.BREAK) {
                logger.debug("not in a break");
                if (checkImmediateBreak()) {
                    logger.debug("immediate");
                    fop.getBreakTimer().setIndefinite();
                    startIndefiniteBreakImmediately(getRequestedBreakType());
                } else {
                    logger.debug("not immediate");
                    setBreakTimerFromFields(ct.getValue());
                    startEnabled();
                }
            } else {
                logger.debug("in a break");
                syncWithFop();
            }
        });

    }

    private void assembleDialog(VerticalLayout dialog, FlexLayout buttons) {

        Tab countdowns = new Tab(Translator.translate("BreakMgmt.Countdowns"));
        Tab ceremonies = new Tab(Translator.translate("BreakMgmt.Ceremonies"));

        VerticalLayout cd = createCountdownTab(buttons);
        VerticalLayout ce = createCeremoniesTab();
        
        Tabs tabs = new Tabs(countdowns, ceremonies);
        VerticalLayout contents = new VerticalLayout();
        contents.setWidth("50em");
        contents.setHeight("60ex");
        contents.add(cd);
        dialog.add(tabs, contents);
        
        tabs.addSelectedChangeListener( e -> {
            Tab selected = e.getSelectedTab();
            if (selected == countdowns) {
                contents.removeAll();
                contents.add(cd);
            } else if (selected == ceremonies) {
                contents.removeAll();
                contents.add(ce);
            } 
            /* else if (selected == interrupts) {
                contents.removeAll();
                contents.add(in);
            }*/
        });
    }

    private VerticalLayout createCeremoniesTab() {
        VerticalLayout ce = new VerticalLayout();
        ce.setWidth("50em");
        
        HorizontalLayout introButtons = new HorizontalLayout();
        Button startIntroButton = new Button(
                getTranslation("CeremonyDialog.startIntro"), (e) -> {
                    OwlcmsSession.withFop(fop -> {
                        // TODO send FOPEvent for intro
                    });
                });
        startIntroButton.setTabIndex(-1);
        Button endIntroButton = new Button(
                getTranslation("CeremonyDialog.endIntro"), (e) -> {
                    OwlcmsSession.withFop(fop -> {
                        // TODO send FOP Event to end intro -- do not touch timer or switch breaks if running
                    });
                });
        endIntroButton.setTabIndex(-1);
        startIntroButton.getThemeNames().add("secondary contrast");
        endIntroButton.getThemeNames().add("secondary contrast");
        introButtons.add(endIntroButton, startIntroButton);
        
        ce.add(new Label(getTranslation("BreakType.INTRODUCTION")));
        ce.add(introButtons);
        
        ce.add(new Hr());
        HorizontalLayout medalButtons = new HorizontalLayout();
        // TODO select group - default is the last one to have lifted on FOP
        // TODO button to open medals scoreboard
        Button startMedalCeremony = new Button(
                getTranslation("CeremonyDialog.startMedals"), (e) -> {
                    OwlcmsSession.withFop(fop -> {
                        // TODO send FOPEvent for medals
                    });
                });
        startMedalCeremony.setTabIndex(-1);
        Button endMedalCeremony = new Button(
                getTranslation("CeremonyDialog.endMedals"), (e) -> {
                    OwlcmsSession.withFop(fop -> {
                        // TODO send FOP Event to end medals -- do not touch timer
                    });
                });
        endMedalCeremony.setTabIndex(-1);
        startMedalCeremony.getThemeNames().add("secondary contrast");
        endMedalCeremony.getThemeNames().add("secondary contrast");
        medalButtons.add(endMedalCeremony, startMedalCeremony);
        
        ce.add(new Label(getTranslation("BreakType.MEDALS")));
        ce.add(medalButtons);
        return ce;
    }

    private VerticalLayout createCountdownTab(FlexLayout buttons) {
        VerticalLayout cd = new VerticalLayout();
        cd.setWidth("50em");
        cd.add(bt);
        cd.add(new Hr());
        cd.add(ct);
        cd.add(new Hr());
        cd.add(new Label(getTranslation("DisplayType.Title")));
        cd.add(dt);
        cd.add(new Hr());
        cd.add(timer);
        cd.add(buttons);
        return cd;
    }

    /**
     * @return true if we triggered an immediate break.
     */
    private boolean checkImmediateBreak() {
        return (getRequestedBreakType() != null
                && (getRequestedBreakType() == BreakType.JURY || getRequestedBreakType() == BreakType.TECHNICAL));
    }

    private void computeDefaultValues() {
        int timeStep = 30;
        timePicker.setStep(Duration.ofMinutes(timeStep));
        LocalTime nowTime = LocalTime.now();
        int nowMin = nowTime.getMinute();
        int nowHr = nowTime.getHour();
        int previousStepMin = (nowMin / timeStep) * timeStep; // between 0 and 50
        int nextStepMin = (previousStepMin + timeStep) % 60;
        logger.trace("previousStepMin = {} nextStepMin = {}", previousStepMin, nextStepMin);
        int nextHr = (nextStepMin == 0 ? nowHr + 1 : nowHr);
        LocalDate nextDate = LocalDate.now();
        if (nextHr >= 24) {
            nextDate.plusDays(1);
            nextHr = nextHr % 24;
        }
        datePicker.setValue(nextDate);
        timePicker.setValue(LocalTime.of(nextHr, nextStepMin));
        logger.debug("setting default duration as default {}", LoggerUtils.whereFrom());
        setDurationField(DEFAULT_DURATION);
    }

    private Integer computeTimerRemainingFromFields(CountdownType countdownType) {
        logger.debug("computeTimerRemainingFromFields");
        Integer tr;
        if (countdownType == CountdownType.INDEFINITE) {
            tr = null;
        } else if (countdownType == CountdownType.TARGET) {
            // recompute duration, in case there was a pause.
            setBreakTimerFromFields(CountdownType.TARGET);
            tr = timeRemaining.intValue();
        } else {
            setBreakTimerFromFields(CountdownType.DURATION);
            tr = timeRemaining.intValue();
        }
        return tr;
    }

    private FlexLayout createButtons(BreakManagement breakManagement) {
        breakStart = new Button(AvIcons.PLAY_ARROW.create(), (e) -> masterStartBreak());
        breakStart.getElement().setAttribute("theme", "primary contrast");
        breakStart.getElement().setAttribute("title", getTranslation("StartCountdown"));

        breakPause = new Button(AvIcons.PAUSE.create(), (e) -> masterPauseBreak(null));
        breakPause.getElement().setAttribute("theme", "primary contrast");
        breakPause.getElement().setAttribute("title", getTranslation("PauseCountdown"));

        breakReset = new Button(IronIcons.RESTORE.create(), (e) -> masterResetBreak());
        breakReset.getElement().setAttribute("theme", "primary contrast");
        breakReset.getElement().setAttribute("title", getTranslation("ResetBreakTimer"));

        breakEnd = new Button(getTranslation("EndBreak"), PlacesIcons.FITNESS_CENTER.create(), endBreak(parentDialog));
        breakEnd.getElement().setAttribute("theme", "primary success");
        breakEnd.getElement().setAttribute("title", getTranslation("EndBreak"));

        FlexLayout buttons = new FlexLayout();
        buttons.add(breakStart, breakPause, /* breakReset, */ breakEnd);
        buttons.setWidth("100%");
        buttons.setJustifyContentMode(JustifyContentMode.AROUND);
        return buttons;
    }

    private void createDisplayInfoSelection() {
        dt = new HorizontalLayout();
        athleteButton = new Button(
                getTranslation(DisplayType.class.getSimpleName() + "." + DisplayType.LIFT_INFO.name()), (e) -> {
                    OwlcmsSession.withFop(fop -> {
                        fop.recomputeLiftingOrder();
                        fop.uiDisplayCurrentAthleteAndTime(false, new FOPEvent(null, this), true);
                    });
                });
        athleteButton.setTabIndex(-1);
        countdownButton = new Button(
                getTranslation(DisplayType.class.getSimpleName() + "." + DisplayType.COUNTDOWN_INFO.name()), (e) -> {
                    OwlcmsSession.withFop(fop -> {
                        fop.recomputeLiftingOrder();
                        OwlcmsSession.getFop().getUiEventBus()
                                .post(new UIEvent.BreakStarted(0, this.getOrigin(), true, bt.getValue(),
                                        ct.getValue(), LoggerUtils.stackTrace()));
                    });
                });
        countdownButton.setTabIndex(-1);
        athleteButton.getThemeNames().add("secondary contrast");
        countdownButton.getThemeNames().add("secondary contrast");
        dt.add(countdownButton, athleteButton);
    }

    private void createCountdowns() {
        bt = new RadioButtonGroup<>();

        // exclude break types that do not affect the timer
        Set<BreakType> noTimer = Stream.of(GROUP_DONE, MEDALS, DURING_INTRODUCTION).collect(Collectors.toSet());
        Set<BreakType> breakTypes = new TreeSet<BreakType>();
        Collections.addAll(breakTypes, BreakType.values());
        breakTypes.removeAll(noTimer);

        bt.setItems(breakTypes);
        bt.setRenderer(new TextRenderer<BreakType>(
                (item) -> getTranslation(BreakType.class.getSimpleName() + "." + item.name())));
        bt.setLabel(getTranslation("BreakType.Title"));
        bt.prependComponents(TECHNICAL, new Paragraph(""));

        ct = new RadioButtonGroup<>();
        ct.setItems(CountdownType.values());
        ct.setRenderer(new TextRenderer<CountdownType>(
                (item) -> getTranslation(CountdownType.class.getSimpleName() + "." + item.name())));
        ct.setLabel(getTranslation("CountdownType.Title"));
        ct.prependComponents(DURATION, new Paragraph(""));
        ct.prependComponents(INDEFINITE, new Paragraph(""));

        Locale locale = new Locale("en", "SE"); // ISO 8601 style dates and time
        timePicker.setLocale(locale);
        datePicker.setLocale(locale);
        minutes = new Label("minutes");

        ct.addComponents(DURATION, durationField, new Label(" "), minutes, new Div());
        ct.addComponents(TARGET, datePicker, new Label(" "), timePicker);
    }

    private void createTimerDisplay() {
        breakTimerElement = new BreakTimerElement();
        breakTimerElement.setParent("BreakManagement");
        Div countdown = new Div(breakTimerElement);
        countdown.getStyle().set("font-size", "x-large");
        countdown.getStyle().set("font-weight", "bold");
        timer = new HorizontalLayout(countdown);
        timer.setWidth("100%");
        timer.setJustifyContentMode(JustifyContentMode.CENTER);
        timer.getStyle().set("margin-top", "0px");
    }

    private void doResetTimer(Integer tr) {
        OwlcmsSession.withFop(fop -> {
            IBreakTimer breakTimer = fop.getBreakTimer();
            if (breakTimer.isRunning()) {
                breakTimer.stop();
                fop.fopEventPost(new FOPEvent.BreakPaused(tr, this.getOrigin()));
            }
        });
        logger.debug("paused; enabling start");
        startEnabled();
    }

    private String formattedDuration(Long milliseconds) {
        return (milliseconds != null && milliseconds >= 0) ? DurationFormatUtils.formatDurationHMS(milliseconds)
                : (milliseconds != null ? milliseconds.toString() : "-");
    }

    private Object getOrigin() {
        return origin;
    }

    private BreakType getRequestedBreakType() {
        return requestedBreakType;
    }

    private LocalDateTime getTarget() {
        final LocalDateTime target;
        LocalDate date = datePicker.getValue();
        LocalTime time = timePicker.getValue();
        target = LocalDateTime.of(date, time);
        return target;
    }

    private void safeSetBT(BreakType breakType) {
        try {
            ignoreBreakTypeValueChange = true;
            bt.setValue(breakType);
        } finally {
            ignoreBreakTypeValueChange = false;
        }
    }

    private void setBreakTimerFromFields(CountdownType cType) {
        if (ignoreListeners) {
            return;
        }
        logger.debug("setBreakTimerFromFields cType={} from={}", cType, LoggerUtils.whereFrom());
        LocalDateTime now = LocalDateTime.now();

        BreakType bType = bt.getValue();
        if (cType == null) {
            mapBreakTypeToCountdownType(bType);
        }
        OwlcmsSession.withFop(fop -> {
            if (cType == CountdownType.TARGET) {
                LocalDateTime target = getTarget();
                timeRemaining = now.until(target, MILLIS);
                logger.debug("setBreakTimerFromFields target-derived duration {}",
                        formattedDuration(timeRemaining));
                breakTimerElement.slaveBreakSet(
                        new BreakSetTime(bType, cType, timeRemaining.intValue(), target, false, this.getOrigin()));
            } else if (cType == CountdownType.INDEFINITE) {
                logger.debug("setBreakTimerFromFields indefinite");
                timeRemaining = null;
                breakTimerElement.slaveBreakSet(new BreakSetTime(bType, cType, 0, null, true, this));
            } else {
                Duration value;
                value = durationField.getValue();
                value = (value == null ? DEFAULT_DURATION : value);
                timeRemaining = (value != null ? value.toMillis() : 0L);
                fop.getBreakTimer().setTimeRemaining(timeRemaining.intValue());
                fop.getBreakTimer().setBreakDuration(timeRemaining.intValue());
                logger.debug("setBreakTimerFromFields explicit duration {}",
                        formattedDuration(timeRemaining));
                // this sets time locally only
                breakTimerElement.slaveBreakSet(
                        new BreakSetTime(bType, cType, timeRemaining.intValue(), null, false, this.getOrigin()));
            }
        });
        return;
    }

    private void setCtValue(CountdownType ct2) {
        logger.debug("setting ct {}  from {}", ct2, LoggerUtils.whereFrom());
        ct.setValue(ct2);
    }

    private void setDurationField(Duration duration) {
        logger.debug("{} {}", duration, LoggerUtils.whereFrom());
        try {
            ignoreDurationValueChange = true;
            durationField.setValue(duration);
        } finally {
            ignoreDurationValueChange = false;
        }
    }

    private void setOrigin(Object origin) {
        this.origin = origin;
    }

    private void setRequestedBreakType(BreakType requestedBreakType) {
        logger.debug("requestedBreakType={} {}", requestedBreakType, LoggerUtils.whereFrom());
        this.requestedBreakType = requestedBreakType;
    }

    private void setTimingFieldsFromBreakTimer(int targetTimeDuration, Integer breakDuration) {
        boolean resetIgnoreListeners = ignoreListeners;
        try {
            logger.debug("setTimingFieldsFromBreakTimer target={} duration={}", targetTimeDuration, breakDuration);
            ignoreListeners = true;
            setDurationField(breakDuration != null ? Duration.ofMillis(breakDuration) : DEFAULT_DURATION);
            LocalDateTime target = LocalDateTime.now().plus(targetTimeDuration, MILLIS);
            datePicker.setValue(target.toLocalDate());
            timePicker.setValue(target.toLocalTime());
            switch (ct.getValue()) {
            case DURATION:
                timeRemaining = (long) targetTimeDuration;
                break;
            case INDEFINITE:
                timeRemaining = null;
                break;
            case TARGET:
                timeRemaining = (long) targetTimeDuration;
                break;
            }
        } finally {
            ignoreListeners = resetIgnoreListeners;
        }

    }

    private void switchToDuration() {
        durationField.setEnabled(true);
        minutes.setEnabled(true);
        datePicker.setEnabled(false);
        timePicker.setEnabled(false);
        durationField.focus();
        durationField.setAutoselect(true);
        startEnabled();
        setBreakTimerFromFields(CountdownType.DURATION);
    }

    private void switchToIndefinite() {
        durationField.setEnabled(false);
        minutes.setEnabled(false);
        datePicker.setEnabled(false);
        timePicker.setEnabled(false);
        startEnabled();
        setBreakTimerFromFields(CountdownType.INDEFINITE);
    }

    private void switchToTarget() {
        durationField.setEnabled(false);
        minutes.setEnabled(false);
        datePicker.setEnabled(true);
        timePicker.setEnabled(true);
        timePicker.focus();
        startEnabled();
        setBreakTimerFromFields(CountdownType.TARGET);
    }

    /**
     * Set values based on current state of Field of Play.
     *
     * @return true if a break timer is running and not paused.
     */
    private boolean syncWithFop() {

        final boolean[] running = new boolean[1]; // wrapper to allow value to be set from lambda
        OwlcmsSession.withFop(fop -> {
            FOPState fopState = fop.getState();
            IBreakTimer fopBreakTimer = fop.getBreakTimer();
            int fopLiveTimeRemaining = fopBreakTimer.liveTimeRemaining();
            Integer fopBreakDuration = fopBreakTimer.getBreakDuration();
            CountdownType fopCountdownType = fop.getCountdownType();
            BreakType fopBreakType = fop.getBreakType();
            boolean breakTimerRunning = fopBreakTimer.isRunning();

            logger.debug("syncWithFop {} {}", fopState);

            boolean resetIgnoreListeners = ignoreListeners;
            try {
                ignoreListeners = true;
                running[0] = false;

                // default values
                computeDefaultValues();
                List<Athlete> order;

                switch (fopState) {
                case INACTIVE:
                case BREAK:
                    logger.debug("   syncWithFOP: break under way {} {} indefinite={}", fopBreakType, fopCountdownType,
                            fopBreakTimer.isIndefinite());

                    if (fopCountdownType == CountdownType.INDEFINITE) {
                        fopLiveTimeRemaining = (int) DEFAULT_DURATION.toMillis();
                    }

                    // override from FOP
                    setTimingFieldsFromBreakTimer(fopLiveTimeRemaining, fopBreakDuration);

                    BreakType breakType;
                    if (fopState == FOPState.INACTIVE) {
                        breakType = BreakType.BEFORE_INTRODUCTION;
                    } else {
                        breakType = fopBreakType;
                    }

                    safeSetBT(breakType);
                    setCtValue(fopCountdownType);

                    logger.debug("   syncWithFOP: running = {} ct={}", breakTimerRunning, fopCountdownType);
                    if (breakTimerRunning) {
                        startDisabled();
                        // start our own timer to follow the others that are already displaying
                        if (fopCountdownType != CountdownType.INDEFINITE) {
                            breakTimerElement.slaveBreakStart(
                                    new BreakStarted(fopLiveTimeRemaining, this.getOrigin(), false, breakType,
                                            fopCountdownType, LoggerUtils.stackTrace()));
                        }
                        running[0] = true;
                    }
                    break;
                default:
                    // the following is likely dead code; AthleteGridContent and subclasses for
                    // Announcer/TimeKeeper/Marshall
                    // provide this information explicitly on open.

                    Athlete curAthlete = fop.getCurAthlete();
                    order = fop.getLiftingOrder();
                    logger.debug("   syncWithFOP: currentAthlete {}", curAthlete);
                    if (curAthlete == null) {
                        safeSetBT(BreakType.BEFORE_INTRODUCTION);
                        setCtValue(CountdownType.TARGET);
                    } else if (curAthlete.getAttemptsDone() == 3 && AthleteSorter.countLiftsDone(order) == 0
                            && fopState != FOPState.TIME_RUNNING) {
                        safeSetBT(BreakType.FIRST_CJ);
                        setCtValue(CountdownType.DURATION);
                    } else if (curAthlete.getAttemptsDone() == 0 && AthleteSorter.countLiftsDone(order) == 0
                            && fopState != FOPState.TIME_RUNNING) {
                        safeSetBT(BreakType.FIRST_SNATCH);
                        setCtValue(CountdownType.DURATION);
                    } else {
                        breakType = getRequestedBreakType();
                        if (breakType == null) {
                            breakType = BreakType.TECHNICAL;
                            setRequestedBreakType(BreakType.TECHNICAL);
                        }
                        safeSetBT(breakType);
                        setCtValue(CountdownType.INDEFINITE);
                    }
                    break;
                }
            } finally {
                ignoreListeners = resetIgnoreListeners;
            }

        });
        return running[0];
    }

}
