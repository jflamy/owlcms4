/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.shared;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
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
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.TextRenderer;

import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.fields.DurationField;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.BreakType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.ProxyBreakTimer;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
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
    private boolean ignoreBreakTypeValueChange;

    private boolean ignoreListeners = false;

    /**
     * Persona-specific calls (e.g. for the jury, the technical controller, etc.)
     *
     * @param origin
     * @param brt
     * @param cdt
     */
    BreakManagement(Object origin, BreakType brt, CountdownType cdt, Dialog parentDialog) {
        init(origin, brt, cdt, parentDialog);
        if (brt == BreakType.JURY || brt == BreakType.TECHNICAL) {
            setRequestedBreakType(brt);
        } else {
            setRequestedBreakType(null);
        }
    }

    /**
     * Used by the announcer -- no specific requested break type, will guess.
     *
     * @param origin the origin
     */
    BreakManagement(Object origin, Dialog parentDialog) {
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
                fop.getFopEventBus().post(new FOPEvent.StartLifting(this.getOrigin()));
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

        createDuration();
        createDisplayInfoSelection();
        createTimerDisplay();
        computeDefaultValues();

        bt.setValue(brt);
        setCtValue(cdt);
        assembleDialog(this, buttons);
        OwlcmsSession.withFop(fop -> {
            fopEventBusRegister((Component) origin, fop);
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

    public void masterPauseBreak() {
        OwlcmsSession.withFop(fop -> {
            ProxyBreakTimer breakTimer = fop.getBreakTimer();
            if (breakTimer.isRunning()) {
                breakTimer.stop();
                fop.getFopEventBus().post(new FOPEvent.BreakPaused(this.getOrigin()));
            }
        });
        logger.debug("paused; enabling start");
        startEnabled();
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
        Integer tr;
        logger.debug("start break bt={} ct={}", bt, ct);
        if (countdownType == CountdownType.INDEFINITE) {
            tr = null;
        } else if (countdownType == CountdownType.TARGET) {
            // recompute duration, in case there was a pause.
            setBreakTimerFromFields(CountdownType.TARGET);
            tr = timeRemaining.intValue();
        } else {
            tr = timeRemaining.intValue();
        }
        fop.getFopEventBus()
                .post(new FOPEvent.BreakStarted(breakType, countdownType, tr, getTarget(), this.getOrigin()));
    }

    @Subscribe
    public void slaveBreakDone(UIEvent.BreakDone e) {
        synchronized (this) {
            try {
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
                UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
//                            OwlcmsSession.withFop((fop) -> {
//                                readFromRunningTimer(fop, fop.getBreakTimer());
//                            });
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
                UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
                    startDisabled();
                    safeSetBT(e.getBreakType());
                });
            } finally {
                ignoreListeners = false;
            }
        }

    }

    public void startDisabled() {
        breakStart.setEnabled(false);
        breakPause.setEnabled(true);
        breakEnd.setEnabled(true);
    }

    public void startEnabled() {
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
            if (ignoreBreakTypeValueChange) {
                return;
            }

            BreakType bType = event.getValue();
            CountdownType mapBreakTypeToCountdownType = mapBreakTypeToCountdownType(bType);
            logger.debug("setting countdown {} ignored={}", mapBreakTypeToCountdownType, ignoreListeners);
            setCtValue(mapBreakTypeToCountdownType);
            masterPauseBreak();

            if (bType != null && (bType == BreakType.JURY || bType == BreakType.TECHNICAL
                    || bType == BreakType.DURING_INTRODUCTION)) {
                logger.debug("starting break from radiobutton setvalue {}", bType);
                startIndefiniteBreakImmediately(bType);
            } else {
                setBreakTimerFromFields(ct.getValue());
            }
        });
        durationField.addValueChangeListener(e -> setBreakTimerFromFields(CountdownType.DURATION));
        timePicker.addValueChangeListener(e -> setBreakTimerFromFields(CountdownType.TARGET));
        datePicker.addValueChangeListener(e -> setBreakTimerFromFields(CountdownType.TARGET));

        boolean running = syncWithFop();
        logger.debug("running ? = {}", running);
        if (!running) {
            doSync();
        }
    }

    private void assembleDialog(VerticalLayout dialog, FlexLayout buttons) {
        dialog.add(bt);
        dialog.add(new Hr());
        dialog.add(ct);
        dialog.add(new Hr());
        dialog.add(new Label(getTranslation("DisplayType.Title")));
        dialog.add(dt);
        dialog.add(new Hr());
        dialog.add(timer);
        dialog.add(buttons);
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

    private FlexLayout createButtons(BreakManagement breakManagement) {
        breakStart = new Button(AvIcons.PLAY_ARROW.create(), (e) -> masterStartBreak());
        breakStart.getElement().setAttribute("theme", "primary contrast");
        breakStart.getElement().setAttribute("title", getTranslation("StartCountdown"));

        breakPause = new Button(AvIcons.PAUSE.create(), (e) -> masterPauseBreak());
        breakPause.getElement().setAttribute("theme", "primary contrast");
        breakPause.getElement().setAttribute("title", getTranslation("PauseCountdown"));

        breakEnd = new Button(getTranslation("EndBreak"), PlacesIcons.FITNESS_CENTER.create(), endBreak(parentDialog));
        breakEnd.getElement().setAttribute("theme", "primary success");
        breakEnd.getElement().setAttribute("title", getTranslation("EndBreak"));

        FlexLayout buttons = new FlexLayout();
        buttons.add(breakStart, breakPause, breakEnd);
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
        countdownButton = new Button(
                getTranslation(DisplayType.class.getSimpleName() + "." + DisplayType.COUNTDOWN_INFO.name()), (e) -> {
                    OwlcmsSession.withFop(fop -> {
                        fop.recomputeLiftingOrder();
                        OwlcmsSession.getFop().getUiEventBus()
                                .post(new UIEvent.BreakStarted(0, this.getOrigin(), true, bt.getValue(),
                                        ct.getValue()));
                    });
                });
        athleteButton.getThemeNames().add("secondary contrast");
        countdownButton.getThemeNames().add("secondary contrast");
        dt.add(countdownButton, athleteButton);
    }

    private void createDuration() {
        bt = new RadioButtonGroup<>();
        BreakType[] breakTypes = BreakType.values();
        bt.setItems(Arrays.copyOfRange(breakTypes, 0, breakTypes.length - 1));
        bt.setRenderer(new TextRenderer<BreakType>(
                (item) -> getTranslation(BreakType.class.getSimpleName() + "." + item.name())));
        bt.setLabel(getTranslation("BreakType.Title"));
        bt.prependComponents(BreakType.TECHNICAL, new Paragraph(""));

        ct = new RadioButtonGroup<>();
        ct.setItems(CountdownType.values());
        ct.setRenderer(new TextRenderer<CountdownType>(
                (item) -> getTranslation(CountdownType.class.getSimpleName() + "." + item.name())));
        ct.setLabel(getTranslation("CountdownType.Title"));
        ct.prependComponents(CountdownType.DURATION, new Paragraph(""));
        ct.prependComponents(CountdownType.INDEFINITE, new Paragraph(""));

        Locale locale = new Locale("en", "SE"); // ISO 8601 style dates and time
        timePicker.setLocale(locale);
        datePicker.setLocale(locale);
        minutes = new Label("minutes");

        ct.addComponents(CountdownType.DURATION, durationField, new Label(" "), minutes, new Div());
        ct.addComponents(CountdownType.TARGET, datePicker, new Label(" "), timePicker);
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

    private void doSync() {
        if (getRequestedBreakType() != null
                && (getRequestedBreakType() == BreakType.JURY || getRequestedBreakType() == BreakType.TECHNICAL)) {
            logger.debug("starting break on dialog creation {}", getRequestedBreakType());
            startIndefiniteBreakImmediately(getRequestedBreakType());
        } else {
            setBreakTimerFromFields(ct.getValue());
        }
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
        logger.debug("updateEditingFields cType={}", cType);
        LocalDateTime now = LocalDateTime.now();

        BreakType bType = bt.getValue();
        if (cType == null) {
            mapBreakTypeToCountdownType(bType);
        }
        OwlcmsSession.withFop(fop -> {
            if (cType == CountdownType.TARGET) {
                LocalDateTime target = getTarget();
                timeRemaining = now.until(target, ChronoUnit.MILLIS);
                logger.debug("setBreakTimerFromFields target-derived duration {}",
                        DurationFormatUtils.formatDurationHMS(timeRemaining));
                breakTimerElement.slaveBreakSet(new BreakSetTime(bType, cType, 0, target, false, this.getOrigin()));
            } else if (cType == CountdownType.INDEFINITE) {
                logger.debug("setBreakTimerFromFields indefinite");
                timeRemaining = null;
                breakTimerElement.slaveBreakSet(new BreakSetTime(bType, cType, 0, null, true, this));
            } else {
                Duration value;
                if (bType == BreakType.JURY || bType == BreakType.TECHNICAL) {
                    value = DEFAULT_DURATION;
                } else {
                    value = durationField.getValue();
                    value = (value == null ? DEFAULT_DURATION : value);
                }
                timeRemaining = (value != null ? value.toMillis() : 0L);
                logger.debug("setBreakTimerFromFields explicit duration {}",
                        DurationFormatUtils.formatDurationHMS(timeRemaining));
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
//        logger./**/warn(LoggerUtils.stackTrace());
        durationField.setValue(duration);
    }

    private void setOrigin(Object origin) {
        this.origin = origin;
    }

    private void setRequestedBreakType(BreakType requestedBreakType) {
        logger.trace("requestedBreakType={} {}", requestedBreakType, LoggerUtils.whereFrom());
        this.requestedBreakType = requestedBreakType;
    }

    private void setTimingFieldsFromMillis(int milliseconds) {
        setDurationField(milliseconds != 0 ? Duration.ofMillis(milliseconds) : DEFAULT_DURATION);
        LocalDateTime target = LocalDateTime.now().plus(milliseconds, ChronoUnit.MILLIS);
        datePicker.setValue(target.toLocalDate());
        timePicker.setValue(target.toLocalTime());
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
     * Set values based on current state of Field of Play
     *
     * @return
     */
    private boolean syncWithFop() {

        final boolean[] running = new boolean[1]; // wrapper to allow value to be set from lambda
        OwlcmsSession.withFop(fop -> {

            ignoreListeners = true;
            logger.debug("syncWithFop {} {}", fop.getState());
            running[0] = false;
            ProxyBreakTimer breakTimer = fop.getBreakTimer();

            // default vaules
            computeDefaultValues();
            startEnabled();
            ignoreListeners = false;

            switch (fop.getState()) {
            case BREAK:
                logger.debug("syncWithFOP - break under way");
                int milliseconds = breakTimer.liveTimeRemaining();
                if (fop.getCountdownType() == CountdownType.INDEFINITE) {
                    ignoreListeners = true;
                    milliseconds = (int) DEFAULT_DURATION.toMillis();
                    ignoreListeners = false;
                }
                setTimingFieldsFromMillis(milliseconds);
                // override from FOP
                BreakType breakType = fop.getBreakType();
                CountdownType countdownType = fop.getCountdownType();
                safeSetBT(breakType);
                setCtValue(countdownType);
                setBreakTimerFromFields(fop.getCountdownType());
                if (breakTimer.isRunning()) {
                    startDisabled();
                    // start only ourself -- the rest of the world is already running
                    breakTimerElement.slaveBreakStart(
                            new BreakStarted(milliseconds, this.getOrigin(), false, breakType, countdownType));
                    running[0] = true;
                }
                break;
            case INACTIVE:
                safeSetBT(BreakType.BEFORE_INTRODUCTION);
                setCtValue(CountdownType.TARGET);
                break;
            default:
                Athlete curAthlete = fop.getCurAthlete();
                logger.debug("syncWithFOP currentAthlete {}", curAthlete);
                if (curAthlete == null) {
                    safeSetBT(BreakType.BEFORE_INTRODUCTION);
                    setCtValue(CountdownType.TARGET);
                } else if (curAthlete.getAttemptsDone() == 3) {
                    safeSetBT(BreakType.FIRST_CJ);
                    setCtValue(CountdownType.DURATION);
                } else if (curAthlete.getAttemptsDone() == 0) {
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
        });
        return running[0];
    }

}
