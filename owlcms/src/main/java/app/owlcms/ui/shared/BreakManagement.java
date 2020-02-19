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
import app.owlcms.fieldofplay.BreakType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.ProxyBreakTimer;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.fieldofplay.UIEvent.BreakSetTime;
import app.owlcms.fieldofplay.UIEvent.BreakStarted;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
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

    private CountdownType requestedCountdownType;

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
        setRequestedCountdownType(cdt);
    }

    private void setRequestedCountdownType(CountdownType cdt) {
        requestedCountdownType = cdt;
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
        OwlcmsSession.withFop(fop -> {
            fop.getUiEventBus().unregister(breakTimerElement);
            this.breakTimerElement = null;
        });
    }

    public ComponentEventListener<ClickEvent<Button>> endBreak(Dialog dialog) {
        return (e) -> {
            OwlcmsSession.withFop(fop -> {
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
        logger.warn("init brt={} cdt={} from {}", brt, cdt, LoggerUtils.whereFrom());
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

    public int readFromRunningTimer(FieldOfPlay fop, ProxyBreakTimer breakTimer) {
        BreakType bType = getRequestedBreakType();
        CountdownType cType = getRequestedCountdownType();

        if (bType == null) {
            bType = fop.getBreakType();
            if (bType != null) {
                // use break type from FOP
                safeSetBT(bType);
                if (cType == null) {
                    cType = mapBreakTypeToCountdownType(bType);
                }
                setCtValue(cType);
            } else {
                // no known break type
                bType = BreakType.TECHNICAL;
                cType = CountdownType.INDEFINITE;
            }
        } else {
            safeSetBT(bType);
            if (cType == null) {
                cType = mapBreakTypeToCountdownType(bType);
            }
            setCtValue(cType);
        }

        int milliseconds;
        if (cType == CountdownType.TARGET || cType == CountdownType.INDEFINITE || breakTimer.isIndefinite()) {
            logger.warn("setting default duration fop is indefinite");
            setDurationField(DEFAULT_DURATION);
            milliseconds = 0;
        } else {
            milliseconds = breakTimer.liveTimeRemaining();
            logger.warn("setting computed duration {}", DurationFormatUtils.formatDurationHMS(milliseconds));
            setDurationField(Duration.ofMillis(milliseconds));
        }


        startDisabled();
        return milliseconds;
    }

    @Subscribe
    public void slaveBreakDone(UIEvent.BreakDone e) {
        synchronized (this) {
            try {
                ignoreListeners = true;
                UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this, e.getOrigin(),
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
                UIEventProcessor.uiAccess(this, uiEventBus, () -> {
                    OwlcmsSession.withFop((fop) -> {
                        readFromRunningTimer(fop, fop.getBreakTimer());
                    });
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
                UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this, e.getOrigin(), () -> {
                    startDisabled();
                    safeSetBT(e.getBreakType());
                });
            } finally {
                ignoreListeners = false;
            }
        }

    }

    public void startBreak() {
        OwlcmsSession.withFop(fop -> {
            startBreak(fop);
        });
        // e.getSource().setEnabled(false);
        logger.debug("start break disable start");
        startDisabled();
        return;
    }

    public void startBreak(FieldOfPlay fop) {
        BreakType breakType = bt.getValue();
        CountdownType countdownType = ct.getValue();
        Integer tr;
        if (countdownType == CountdownType.INDEFINITE) {
            tr = null;
        } else if (countdownType == CountdownType.TARGET) {
            // recompute duration, in case there was a pause.
            updateEditingFields(CountdownType.TARGET);
            tr = timeRemaining.intValue();
        } else {
            tr = timeRemaining.intValue();
        }
        fop.getFopEventBus().post(new FOPEvent.BreakStarted(breakType, countdownType, tr, getTarget(), null));
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
        logger.warn("setting default duration for indefinite break");
        setDurationField(DEFAULT_DURATION);
        BreakType breakType = bType != null ? bType : BreakType.TECHNICAL;
        safeSetBT(breakType);
        startBreak();
        breakTimerElement.slaveBreakStart(new BreakStarted(null, this, false, breakType, ct.getValue()));
    }

    /**
     * Everything has been created and has meaningful values, add value change listeners now to avoid spuriuous
     * triggering during interface build-up.
     *
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
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
            logger.warn("setting countdown {} ignored={}", mapBreakTypeToCountdownType, ignoreListeners);
            setCtValue(mapBreakTypeToCountdownType);
            masterPauseBreak();

            if (bType != null && (bType == BreakType.JURY || bType == BreakType.TECHNICAL
                    || bType == BreakType.DURING_INTRODUCTION)) {
                logger.debug("starting break from radiobutton setvalue {}", bType);
                startIndefiniteBreakImmediately(bType);
            } else {
                updateEditingFields(ct.getValue());
            }
        });
        durationField.addValueChangeListener(e -> updateEditingFields(CountdownType.DURATION));
        timePicker.addValueChangeListener(e -> updateEditingFields(CountdownType.TARGET));
        datePicker.addValueChangeListener(e -> updateEditingFields(CountdownType.TARGET));
        boolean running = syncWithFop();
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
        logger.warn("setting default duration as default {}", LoggerUtils.whereFrom());
        setDurationField(DEFAULT_DURATION);
    }

    private FlexLayout createButtons(BreakManagement breakManagement) {
        breakStart = new Button(AvIcons.PLAY_ARROW.create(), (e) -> startBreak());
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
                        // FIXME: why is this "go back to display the current break"
                                .post(new UIEvent.BreakStarted(0, this, true, bt.getValue(), ct.getValue()));
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
            logger.warn("starting break on dialog creation {}", getRequestedBreakType());
            startIndefiniteBreakImmediately(getRequestedBreakType());
        } else {
            updateEditingFields(ct.getValue());
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

    private void switchToDuration() {
        durationField.setEnabled(true);
        minutes.setEnabled(true);
        datePicker.setEnabled(false);
        timePicker.setEnabled(false);
        durationField.focus();
        durationField.setAutoselect(true);
        startEnabled();
        updateEditingFields(CountdownType.DURATION);
    }

    private void switchToIndefinite() {
        durationField.setEnabled(false);
        minutes.setEnabled(false);
        datePicker.setEnabled(false);
        timePicker.setEnabled(false);
        startEnabled();
        updateEditingFields(CountdownType.INDEFINITE);
    }

    private void switchToTarget() {
        durationField.setEnabled(false);
        minutes.setEnabled(false);
        datePicker.setEnabled(true);
        timePicker.setEnabled(true);
        timePicker.focus();
        startEnabled();
        updateEditingFields(CountdownType.TARGET);
    }

    /**
     * Set values except for Timer, which will set itself.
     *
     * @return
     */
    private boolean syncWithFop() {
        ignoreListeners = true;
        final boolean[] running = new boolean[1]; // workaround final restriction for lambdas
        OwlcmsSession.withFop(fop -> {
            running[0] = false;
            ProxyBreakTimer breakTimer = fop.getBreakTimer();

            if (breakTimer.isRunning()) {
                int milliseconds = readFromRunningTimer(fop, breakTimer);
                logger.warn("sync with running break timer");
                breakTimerElement.slaveBreakStart(
                        new BreakStarted(milliseconds, this, false, fop.getBreakType(), fop.getCountdownType()));
                running[0] = true;
            } else {
                computeDefaultValues();
                int milliseconds = readFromRunningTimer(fop, breakTimer);
                startEnabled();
                ignoreListeners = false;
                switch (fop.getState()) {
                case BREAK:
                    setDurationField(Duration.ofMillis(milliseconds));
                    BreakType breakType = fop.getBreakType();
                    safeSetBT(breakType);
                    if (breakType == BreakType.JURY || breakType == BreakType.TECHNICAL) {
                        setCtValue(CountdownType.INDEFINITE);
                        breakTimerElement.slaveBreakStart(
                                new BreakStarted(null, this, false, fop.getBreakType(), fop.getCountdownType()));
                    } else {
                        updateEditingFields(fop.getCountdownType());
                    }
                    break;
                case CURRENT_ATHLETE_DISPLAYED:
                    if (fop.getCurAthlete() == null) {
                        safeSetBT(BreakType.BEFORE_INTRODUCTION);
                    } else if (fop.getCurAthlete().getAttemptsDone() == 3) {
                        safeSetBT(BreakType.FIRST_CJ);
                    } else if (fop.getCurAthlete().getAttemptsDone() == 0) {
                        safeSetBT(BreakType.FIRST_SNATCH);
                    } else {
                        safeSetBT(BreakType.TECHNICAL);
                        if (getRequestedBreakType() == null) {
                            setRequestedBreakType(BreakType.TECHNICAL);
                        }
                    }
                    break;
                case INACTIVE:
                    safeSetBT(BreakType.BEFORE_INTRODUCTION);
                    break;
                default:
                    safeSetBT(BreakType.TECHNICAL);
                    if (getRequestedBreakType() == null) {
                        setRequestedBreakType(BreakType.TECHNICAL);
                    }
                    break;
                }
            }
        });
        ignoreListeners = false;
        return running[0];
    }

    private void setCtValue(CountdownType ct2) {
        logger.warn("setting ct {}  from {}", ct2, LoggerUtils.whereFrom());
        ct.setValue(ct2);
    }

    private void updateEditingFields(CountdownType cType) {
        if (ignoreListeners) return;
        
        LocalDateTime now = LocalDateTime.now();

        BreakType bType = bt.getValue();
        if (cType == null) {
            mapBreakTypeToCountdownType(bType);
        }

        final LocalDateTime target;
        if (cType == CountdownType.DURATION) {
            Duration value;
            if (bType == BreakType.JURY || bType == BreakType.TECHNICAL) {
                value = DEFAULT_DURATION;
            } else {
                value = durationField.getValue();
                value = (value == null ? DEFAULT_DURATION : value);
            }
            target = now.plus(value);
        } else if (cType == CountdownType.TARGET) {
            target = getTarget();
        } else { // INDEFINITE
            target = now;
        }

        OwlcmsSession.withFop(fop -> {
            if (cType == CountdownType.TARGET) {
                timeRemaining = now.until(target, ChronoUnit.MILLIS);
                logger.warn("target, indefinite {}", DurationFormatUtils.formatDurationHMS(timeRemaining));
                breakTimerElement.slaveBreakSet(new BreakSetTime(bType, cType, target, this));
            } else if (target.isBefore(now) || target.isEqual(now)) {
                logger.warn("duration 0 or target in the past, indefinite");
                timeRemaining = null;
                breakTimerElement.slaveBreakSet(new BreakSetTime(bType, cType, 0, true, this));
            } else {
                logger.warn("duration ok or target in future");
                Duration value = durationField.getValue();
                timeRemaining = (value != null ? value.toMillis() : 0L);
                breakTimerElement.slaveBreakSet(new BreakSetTime(bType, cType, timeRemaining.intValue(), false, this));
            }
        });

        return;
    }

    private CountdownType getRequestedCountdownType() {
        return requestedCountdownType;
    }

}
