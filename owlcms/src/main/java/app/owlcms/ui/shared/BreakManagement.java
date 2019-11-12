/***
 * Copyright (c) 2009-2019 Jean-François Lamy
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
     * Used by the announcer -- tries to guess what type of break is pertinent based
     * on field of play state
     *
     * @param origin the origin
     */
    BreakManagement(Object origin, Dialog parentDialog) {
        init(origin, null, CountdownType.DURATION, parentDialog);
        setRequestedBreakType(null);
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
        if (durationField.getValue() == null) {
            durationField.setValue(Duration.ofMinutes(10L));
        }
    }

    private FlexLayout createButtons(BreakManagement breakManagement) {
        breakStart = new Button(AvIcons.PLAY_ARROW.create(), (e) -> startBreak());
        breakStart.getElement().setAttribute("theme", "primary contrast");
        breakStart.getElement().setAttribute("title", getTranslation("StartCountdown"));

        breakPause = new Button(AvIcons.PAUSE.create(), (e) -> pauseBreak());
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
        Div countdown = new Div(breakTimerElement);
        countdown.getStyle().set("font-size", "x-large");
        countdown.getStyle().set("font-weight", "bold");
        timer = new HorizontalLayout(countdown);
        timer.setWidth("100%");
        timer.setJustifyContentMode(JustifyContentMode.CENTER);
        timer.getStyle().set("margin-top", "0px");
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

    private Object getOrigin() {
        return origin;
    }

    private BreakType getRequestedBreakType() {
        return requestedBreakType;
    }

    public void init(Object origin, BreakType brt, CountdownType cdt, Dialog parentDialog) {
        this.setOrigin(origin);
        this.parentDialog = parentDialog;
        FlexLayout buttons = createButtons(this);

        createDuration();
        createDisplayInfoSelection();
        createTimerDisplay();
        computeDefaultValues();

        bt.setValue(brt);
        ct.setValue(cdt);
        assembleDialog(this, buttons);
    }

    public CountdownType mapBreakTypeToCountdownType(BreakType bType) {
        CountdownType cType;
        if (bType == BreakType.FIRST_SNATCH || bType == BreakType.FIRST_CJ) {
            cType = CountdownType.DURATION;
        } else if (bType == BreakType.INTRODUCTION || bType == BreakType.GROUP_DONE) {
            cType = CountdownType.TARGET;
        } else {
            cType = CountdownType.INDEFINITE;
        }
        return cType;
    }

    /**
     * Everything has been created and has meaningful values, add value change
     * listeners now to avoid spuriuous triggering during interface build-up.
     *
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        logger.warn("attaching");
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
            BreakType bType = event.getValue();
            ct.setValue(mapBreakTypeToCountdownType(bType));
            pauseBreak();

            if (bType != null && (bType == BreakType.JURY || bType == BreakType.TECHNICAL)) {
                logger.warn("starting break immediately {}", getRequestedBreakType());
                startIndefiniteBreakImmediately(bType);
            } else {
                setBreakTimeRemaining(ct.getValue());
            }
        });
        durationField.addValueChangeListener(e -> setBreakTimeRemaining(CountdownType.DURATION));
        timePicker.addValueChangeListener(e -> setBreakTimeRemaining(CountdownType.TARGET));
        timePicker.addValueChangeListener(e -> setBreakTimeRemaining(CountdownType.TARGET));

        syncWithFop();
        if (getRequestedBreakType() != null
                && (getRequestedBreakType() == BreakType.JURY || getRequestedBreakType() == BreakType.TECHNICAL)) {
            logger.debug("starting break immediately {}", getRequestedBreakType());
            startIndefiniteBreakImmediately(getRequestedBreakType());
        } else {
            setBreakTimeRemaining(ct.getValue());
        }
    }

    public void pauseBreak() {
        OwlcmsSession.withFop(fop -> {
            ProxyBreakTimer breakTimer = fop.getBreakTimer();
            if (breakTimer.isRunning()) {
                breakTimer.stop();
                fop.getFopEventBus().post(new FOPEvent.BreakPaused(this.getOrigin()));
            }
        });
        logger.warn("paused; enabling start");
        startEnabled();
    }

    public void readFromRunningTimer(FieldOfPlay fop, ProxyBreakTimer breakTimer) {
        // FIXME: if break is a target time, should reflect this.
        int milliseconds = breakTimer.computeTimeRemaining();
        durationField.setValue(Duration.ofMillis(milliseconds));
        ct.setValue(fop.getCountdownType());
        bt.setValue(fop.getBreakType());
        startDisabled();
    }

    private void setBreakTimeRemaining(CountdownType cType) {
        LocalDateTime now = LocalDateTime.now();

        BreakType bType = bt.getValue();
        if (cType == null) {
            mapBreakTypeToCountdownType(bType);
        }

        final LocalDateTime target;
        if (cType == CountdownType.DURATION) {
            Duration value = durationField.getValue();
            target = now.plus(value != null ? value : Duration.ZERO);
        } else if (cType == CountdownType.TARGET) {
            // FIXME: values can be null
            target = getTarget();
        } else { // INDEFINITE
            target = now;
        }

        OwlcmsSession.withFop(fop -> {
            if (cType == CountdownType.TARGET) {
                logger.debug("target, indefinite");
                timeRemaining = now.until(target, ChronoUnit.MILLIS);
                breakTimerElement.slaveBreakSet(new BreakSetTime(bType, cType, target, this));
            } else if (target.isBefore(now) || target.isEqual(now)) {
                logger.debug("duration 0 or target in the past, indefinite");
                timeRemaining = null;
                breakTimerElement.slaveBreakSet(new BreakSetTime(bType, cType, 0, true, this));
            } else {
                logger.debug("duration ok or target in future");
                Duration value = durationField.getValue();
                timeRemaining = (value != null ? value.toMillis() : 0L);
                breakTimerElement.slaveBreakSet(new BreakSetTime(bType, cType, timeRemaining.intValue(), false, this));
            }
        });

        return;
    }

    private LocalDateTime getTarget() {
        final LocalDateTime target;
        LocalDate date = datePicker.getValue();
        LocalTime time = timePicker.getValue();
        target = LocalDateTime.of(date, time);
        return target;
    }

    private void setOrigin(Object origin) {
        this.origin = origin;
    }

    private void setRequestedBreakType(BreakType requestedBreakType) {
        logger.trace("requestedBreakType={} {}", requestedBreakType, LoggerUtils.whereFrom());
        this.requestedBreakType = requestedBreakType;
    }

    @Subscribe
    public void slaveBreakDone(UIEvent.BreakDone e) {
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this, e.getOrigin(),
                () -> parentDialog.close());
    }

    @Subscribe
    public void slaveBreakPause(UIEvent.BreakPaused e) {
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this, e.getOrigin(), () -> pauseBreak());
    }

    @Subscribe
    public void slaveBreakSet(UIEvent.BreakSetTime e) {
        // do nothing
    }

    @Subscribe
    public void slaveBreakStart(UIEvent.BreakStarted e) {
        if (e.isDisplayToggle()) {
            return;
        }
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this, e.getOrigin(), () -> {
            startDisabled();
            bt.setValue(e.getBreakType());
        });
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
        Integer tr = (countdownType == CountdownType.INDEFINITE ? null : timeRemaining.intValue());
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
        if (ct.getValue() == null) ct.setValue(CountdownType.INDEFINITE);
        BreakType breakType = bType != null ? bType : BreakType.TECHNICAL;
        bt.setValue(breakType);
        startBreak();
        breakTimerElement.slaveBreakStart(new BreakStarted(null, this, false, breakType, ct.getValue()));
    }

    private void switchToDuration() {
        durationField.setEnabled(true);
        minutes.setEnabled(true);
        datePicker.setEnabled(false);
        timePicker.setEnabled(false);
        durationField.focus();
        durationField.setAutoselect(true);
        startEnabled();
        setBreakTimeRemaining(CountdownType.DURATION);
    }

    private void switchToIndefinite() {
        durationField.setEnabled(false);
        minutes.setEnabled(false);
        datePicker.setEnabled(false);
        timePicker.setEnabled(false);
        startEnabled();
        setBreakTimeRemaining(CountdownType.INDEFINITE);
    }

    private void switchToTarget() {
        durationField.setEnabled(false);
        minutes.setEnabled(false);
        datePicker.setEnabled(true);
        timePicker.setEnabled(true);
        timePicker.focus();
        startEnabled();
        setBreakTimeRemaining(CountdownType.TARGET);
    }

    /**
     * Set values except for Timer, which will set itself.
     */
    private void syncWithFop() {
        OwlcmsSession.withFop(fop -> {
            ProxyBreakTimer breakTimer = fop.getBreakTimer();
            computeDefaultValues();
            if (breakTimer.isRunning()) {
                readFromRunningTimer(fop, breakTimer);
            } else {
                startEnabled();
                switch (fop.getState()) {
                case BREAK:
                    BreakType breakType = fop.getBreakType();
                    bt.setValue(breakType);
                    if (breakType == BreakType.JURY || breakType == BreakType.TECHNICAL) {
                        ct.setValue(CountdownType.INDEFINITE);
                        breakTimerElement.slaveBreakStart(
                                new BreakStarted(null, this, false, fop.getBreakType(), fop.getCountdownType()));
                    }
                    break;
                case CURRENT_ATHLETE_DISPLAYED:
                    if (fop.getCurAthlete() == null) {
                        bt.setValue(BreakType.INTRODUCTION);
                    } else if (fop.getCurAthlete().getAttemptsDone() == 3) {
                        bt.setValue(BreakType.FIRST_CJ);
                    } else if (fop.getCurAthlete().getAttemptsDone() == 0) {
                        bt.setValue(BreakType.FIRST_SNATCH);
                    } else {
                        bt.setValue(BreakType.TECHNICAL);
                        if (getRequestedBreakType() == null) {
                            setRequestedBreakType(BreakType.TECHNICAL);
                        }
                    }
                    break;
                case INACTIVE:
                    bt.setValue(BreakType.INTRODUCTION);
                    break;
                default:
                    bt.setValue(BreakType.TECHNICAL);
                    if (getRequestedBreakType() == null) {
                        setRequestedBreakType(BreakType.TECHNICAL);
                    }
                    break;
                }
            }
        });
    }

}
