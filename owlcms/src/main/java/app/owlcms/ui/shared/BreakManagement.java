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
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class BreakManagement extends VerticalLayout implements SafeEventBusRegistration {
    public enum CountdownType {
        DURATION, TARGET, INDEFINITE
    }

    public enum DisplayType {
        COUNTDOWN, ATHLETE
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
            startIndefiniteBreakImmediately();
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
        syncWithFop();
    }

    public ComponentEventListener<ClickEvent<Button>> endBreak(Dialog dialog) {
        return (e) -> {
            OwlcmsSession.withFop(fop -> fop.getFopEventBus().post(new FOPEvent.StartLifting(this.getOrigin())));
            breakStart.setEnabled(true);
            breakPause.setEnabled(false);
            breakEnd.setEnabled(false);
            bt.setValue(BreakType.TECHNICAL);
            setBreakTimeRemaining(CountdownType.INDEFINITE);
            dialog.close();
        };
    }

    public CountdownType guessCountdownFromBreak(BreakType bType) {
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

    public void init(Object origin, BreakType brt, CountdownType cdt, Dialog parentDialog) {
        this.setOrigin(origin);
        this.parentDialog = parentDialog;

        createDisplayType();

        createDuration();
        bt.setValue(brt);
        ct.setValue(cdt);

        FlexLayout buttons = createButtons(this);
        createTimerDisplay();
        computeDefaultValues();
        assembleDialog(this, buttons);
    }

    public ComponentEventListener<ClickEvent<Button>> pauseBreak() {
        return (e) -> {
            OwlcmsSession.withFop(fop -> {
                ProxyBreakTimer breakTimer = fop.getBreakTimer();
                breakTimer.stop();
                if (breakTimer.isIndefinite()) {
                    ct.setValue(CountdownType.INDEFINITE);
                } else if (ct.getValue() != CountdownType.INDEFINITE) {
                    durationField.setValue(Duration.ofSeconds(breakTimer.computeTimeRemaining() / 1000));
                    ct.setValue(CountdownType.DURATION);
                }
                fop.getFopEventBus().post(new FOPEvent.BreakPaused(this.getOrigin()));
            });
            breakStart.setEnabled(true);
            breakPause.setEnabled(false);
            breakEnd.setEnabled(true);
        };
    }

    public void readFromRunningTimer(FieldOfPlay fop, ProxyBreakTimer breakTimer) {
        int milliseconds = breakTimer.computeTimeRemaining();
        durationField.setValue(Duration.ofMillis(milliseconds));
        ct.setValue(CountdownType.DURATION);
        bt.setValue(fop.getBreakType());
    }

    public ComponentEventListener<ClickEvent<Button>> startBreak() {
        return (e) -> {
            OwlcmsSession.withFop(fop -> {
                startBreak(fop);
            });
            e.getSource().setEnabled(false);
            breakStart.setEnabled(false);
            breakPause.setEnabled(true);
            breakEnd.setEnabled(true);
        };
    }

    public void startBreak(FieldOfPlay fop) {
        ProxyBreakTimer breakTimer = fop.getBreakTimer();
        boolean indefinite = (timeRemaining == null);
        if (indefinite) {
            breakTimer.setIndefinite();
        } else {
            breakTimer.setTimeRemaining(timeRemaining.intValue());
        }

        fop.getFopEventBus().post(new FOPEvent.BreakStarted(bt.getValue(), this.getOrigin()));
    }

    public void startIndefiniteBreakImmediately() {
        // FIXME: starting jury/technical break via FOPEvent during init
        timeRemaining = null;
        ct.setValue(CountdownType.INDEFINITE);
        startBreak(OwlcmsSession.getFop());
        breakTimerElement.slaveBreakStart(new BreakStarted(null, this, false));
    }

    /**
     * Everything has been created and has meaningful values, add value change
     * listeners now to avoid spuriuous triggering during interface build-up.
     *
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
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
            ct.setValue(guessCountdownFromBreak(bType));
        });
        durationField.addValueChangeListener(e -> setBreakTimeRemaining(CountdownType.DURATION));
        timePicker.addValueChangeListener(e -> setBreakTimeRemaining(CountdownType.TARGET));
        timePicker.addValueChangeListener(e -> setBreakTimeRemaining(CountdownType.TARGET));
        setBreakTimeRemaining(ct.getValue());
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
        breakStart = new Button(AvIcons.PLAY_ARROW.create(), startBreak());
        breakStart.getElement().setAttribute("theme", "primary contrast");
        breakStart.getElement().setAttribute("title", getTranslation("StartCountdown"));

        breakPause = new Button(AvIcons.PAUSE.create(), pauseBreak());
        breakPause.getElement().setAttribute("theme", "primary contrast");
        breakPause.getElement().setAttribute("title", getTranslation("PauseCountdown"));

        breakEnd = new Button(getTranslation("EndBreak"), PlacesIcons.FITNESS_CENTER.create(), endBreak(parentDialog));
        breakEnd.getElement().setAttribute("theme", "success primary");
        breakEnd.getElement().setAttribute("title", getTranslation("EndBreak"));

        FlexLayout buttons = new FlexLayout();
        buttons.add(breakStart, breakPause, breakEnd);
        buttons.setWidth("100%");
        buttons.setJustifyContentMode(JustifyContentMode.AROUND);
        return buttons;
    }

    private void createDisplayType() {

        dt = new HorizontalLayout();
        athleteButton = new Button(getTranslation(DisplayType.class.getSimpleName() + "." + DisplayType.ATHLETE.name()),
                (e) -> {
                    OwlcmsSession.withFop(fop -> {
                        fop.recomputeLiftingOrder();
                        fop.uiDisplayCurrentAthleteAndTime(false, new FOPEvent(null, this), true);
                    });
                });
        countdownButton = new Button(
                getTranslation(DisplayType.class.getSimpleName() + "." + DisplayType.COUNTDOWN.name()), (e) -> {
                    OwlcmsSession.withFop(fop -> {
                        fop.recomputeLiftingOrder();
                        OwlcmsSession.getFop().getUiEventBus().post(new UIEvent.BreakStarted(0, this, true));
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

    private Object getOrigin() {
        return origin;
    }

    private void setBreakTimeRemaining(CountdownType cType) {
        LocalDateTime now = LocalDateTime.now();

        BreakType bType = bt.getValue();
        if (cType == null) {
            guessCountdownFromBreak(bType);
        }

        final LocalDateTime target;
        if (cType == CountdownType.DURATION) {
            Duration value = durationField.getValue();
            target = now.plus(value != null ? value : Duration.ZERO);
        } else if (cType == CountdownType.TARGET) {
            LocalDate date = datePicker.getValue();
            LocalTime time = timePicker.getValue();
            target = LocalDateTime.of(date, time);
        } else { // INDEFINITE
            target = now;
        }

        OwlcmsSession.withFop(fop -> {
            if (cType == CountdownType.TARGET) {
                logger.debug("target, indefinite");
                timeRemaining = now.until(target, ChronoUnit.MILLIS);
                breakTimerElement.slaveBreakSet(new BreakSetTime(0, true, this));
            } else if (target.isBefore(now) || target.isEqual(now)) {
                logger.debug("duration 0 or target in the past, indefinite");
                timeRemaining = null;
                breakTimerElement.slaveBreakSet(new BreakSetTime(0, true, this));
            } else {
                logger.debug("duration ok or target in future");
                timeRemaining = now.until(target, ChronoUnit.MILLIS);
                breakTimerElement.slaveBreakSet(new BreakSetTime(timeRemaining.intValue(), false, this));
            }

            ProxyBreakTimer breakTimer = fop.getBreakTimer();
            if (breakTimer.isRunning()) {
                breakStart.setEnabled(false);
                if (breakTimer.isIndefinite()) {
                    ct.setValue(CountdownType.INDEFINITE);
                    breakTimerElement.slaveBreakStart(new BreakStarted(null, this, false));
                } else {
                    breakTimerElement.slaveBreakStart(new BreakStarted(timeRemaining == null ? null : timeRemaining.intValue(), this, false));
                }

            }
        });

        return;
    }

    private void setOrigin(Object origin) {
        this.origin = origin;
    }

    private void switchToDuration() {
        durationField.setEnabled(true);
        minutes.setEnabled(true);
        datePicker.setEnabled(false);
        timePicker.setEnabled(false);
        durationField.focus();
        durationField.setAutoselect(true);
        setBreakTimeRemaining(CountdownType.DURATION);
    }

    private void switchToIndefinite() {
        durationField.setEnabled(false);
        minutes.setEnabled(false);
        datePicker.setEnabled(false);
        timePicker.setEnabled(false);
        setBreakTimeRemaining(CountdownType.INDEFINITE);
    }

    private void switchToTarget() {
        durationField.setEnabled(false);
        minutes.setEnabled(false);
        datePicker.setEnabled(true);
        timePicker.setEnabled(true);
        timePicker.focus();
        setBreakTimeRemaining(CountdownType.TARGET);
    }

    private void syncWithFop() {
        OwlcmsSession.withFop(fop -> {
            ProxyBreakTimer breakTimer = fop.getBreakTimer();
            computeDefaultValues();
            if (breakTimer.isRunning()) {
                readFromRunningTimer(fop, breakTimer);
            } else {
                switch (fop.getState()) {
                case BREAK:
                    BreakType breakType = fop.getBreakType();
                    bt.setValue(breakType);
                    if (breakType == BreakType.JURY) {
                        ct.setValue(CountdownType.INDEFINITE);
                        breakTimerElement.slaveBreakStart(new BreakStarted(null, this, false));
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
                    }
                    break;
                case INACTIVE:
                    bt.setValue(BreakType.INTRODUCTION);
                    break;
                default:
                    bt.setValue(BreakType.TECHNICAL);
                    break;
                }
            }
        });
    }

}
