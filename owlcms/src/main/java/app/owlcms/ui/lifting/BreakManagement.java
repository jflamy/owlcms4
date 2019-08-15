/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.lifting;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
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
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.TextRenderer;

import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.fieldofplay.BreakType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class BreakManagement extends VerticalLayout {
    public enum CountdownType {
        DURATION, TARGET, INDEFINITE
    }

    public enum DisplayType {
        COUNTDOWN, ATHLETE
    }

    final private Logger logger = (Logger) LoggerFactory.getLogger(BreakManagement.class);;
    {
        logger.setLevel(Level.INFO);
    };

    private Button breakStart = null;
    private Button breakPause = null;
    private Button breakEnd = null;
    private Object origin;
    private Label minutes;
    private HorizontalLayout timer;

    RadioButtonGroup<DisplayType> dt;
    RadioButtonGroup<CountdownType> ct;
    RadioButtonGroup<BreakType> bt;
    NumberField nf = new NumberField();
    TimePicker tp = new TimePicker();
    DatePicker dp = new DatePicker();
    private Dialog parentDialog;
    private Button breakReset;

    /**
     * Persona-specific calls (e.g. for the jury, the technical controller, etc.)
     * 
     * @param origin
     * @param brt
     * @param cdt
     */
    BreakManagement(Object origin, BreakType brt, CountdownType cdt, Dialog parentDialog) {
        this.setOrigin(origin);
        this.parentDialog = parentDialog;

        configureDisplayType();

        configureDuration();
        FlexLayout buttons = configureButtons(this);
        configureTimerDisplay();
        assembleDialog(this, buttons);

        bt.setValue(brt);
        ct.setValue(cdt);
        nf.setValue(10.0D);
    }

    /**
     * Used by the announcer -- tries to guess what type of break is pertinent based
     * on field of play state
     *
     * @param origin the origin
     */
    BreakManagement(Object origin, Dialog parentDialog) {
        this(origin, null, CountdownType.DURATION, parentDialog);
        syncWithFop();
    }

    public ComponentEventListener<ClickEvent<Button>> pauseBreak() {
        return (e) -> {
            OwlcmsSession.withFop(fop -> fop.getFopEventBus().post(new FOPEvent.BreakPaused(this.getOrigin())));
            breakStart.setEnabled(true);
            breakPause.setEnabled(false);
            breakEnd.setEnabled(true);
        };
    }

    public ComponentEventListener<ClickEvent<Button>> startBreak() {
        return (e) -> {
            OwlcmsSession.withFop(fop -> {
                long breakDuration = setBreakTimeRemaining(ct.getValue(), nf, tp, dp);
                fop.getFopEventBus()
                        .post(new FOPEvent.BreakStarted(bt.getValue(), (int) breakDuration, this.getOrigin()));
            });
            e.getSource().setEnabled(false);
            breakStart.setEnabled(false);
            breakPause.setEnabled(true);
            breakEnd.setEnabled(true);
            timer.getStyle().set("visibility", "visible");
        };
    }

    public ComponentEventListener<ClickEvent<Button>> stopBreak(Dialog dialog) {
        return (e) -> {
            OwlcmsSession.withFop(fop -> fop.getFopEventBus().post(new FOPEvent.StartLifting(this.getOrigin())));
            breakStart.setEnabled(true);
            breakPause.setEnabled(false);
            breakEnd.setEnabled(false);
            breakReset.setEnabled(false);
            bt.setValue(BreakType.TECHNICAL);
            setBreakTimeRemaining(CountdownType.INDEFINITE, nf, tp, dp);
            dialog.close();
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
     * AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        ct.addValueChangeListener(e -> {
            CountdownType cType = e.getValue();
            setValues(cType, nf, tp, dp);
            if (cType != CountdownType.TARGET) {
                switchToDuration(cType, nf, tp, dp);
            } else {
                switchToTarget(cType, nf, tp, dp);
            }
        });
    }

    private void assembleDialog(VerticalLayout dialog, FlexLayout buttons) {
        dialog.add(bt);
        dialog.add(new Hr());
        dialog.add(ct);
        dialog.add(new Hr());
        dialog.add(dt);
        dialog.add(new Hr());
        dialog.add(timer);
        timer.getStyle().set("visibility", "hidden");
        dialog.add(buttons);
    }

    private void computeRoundedTargetValues(TimePicker tp, DatePicker dp) {
        int timeStep = 30;
        tp.setStep(Duration.ofMinutes(timeStep));
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
        dp.setValue(nextDate);
        tp.setValue(LocalTime.of(nextHr, nextStepMin));
    }

    private FlexLayout configureButtons(BreakManagement breakManagement) {
        breakStart = new Button(AvIcons.PLAY_ARROW.create(), startBreak());
        breakStart.getElement().setAttribute("theme", "primary");
        breakStart.getElement().setAttribute("title", getTranslation("StartCountdown"));

        breakPause = new Button(AvIcons.PAUSE.create(), pauseBreak());
        breakPause.getElement().setAttribute("theme", "primary");
        breakPause.getElement().setAttribute("title", getTranslation("PauseCountdown"));

        breakEnd = new Button(AvIcons.STOP.create(), stopBreak(parentDialog));
        breakEnd.getElement().setAttribute("theme", "primary");
        breakEnd.getElement().setAttribute("title", getTranslation("EndBreak_StartLifting"));

        breakReset = new Button(AvIcons.REPLAY.create(), resetBreak(parentDialog));
        breakReset.getElement().setAttribute("theme", "primary");
        breakReset.getElement().setAttribute("title", getTranslation("ResetCountdown"));

        FlexLayout buttons = new FlexLayout();
        buttons.add(breakStart, breakPause, breakEnd);
        buttons.setWidth("100%");
        buttons.setJustifyContentMode(JustifyContentMode.AROUND);
        return buttons;
    }

    private ComponentEventListener<ClickEvent<Button>> resetBreak(Dialog parentDialog2) {
        // TODO Auto-generated method stub
        return null;
    }

    private void configureDisplayType() {
        dt = new RadioButtonGroup<>();
        dt.setRenderer(new TextRenderer<DisplayType>(
                (item) -> getTranslation(DisplayType.class.getSimpleName() + "." + item.name())));
        dt.setItems(DisplayType.values());
        dt.setLabel(getTranslation("DisplayType.Title"));
        dt.setValue(DisplayType.COUNTDOWN);
    }

    private void configureDuration() {
        bt = new RadioButtonGroup<BreakType>();
        BreakType[] breakTypes = BreakType.values();
        bt.setItems(Arrays.copyOfRange(breakTypes, 0, breakTypes.length - 1));
        bt.setRenderer(new TextRenderer<BreakType>(
                (item) -> getTranslation(BreakType.class.getSimpleName() + "." + item.name())));
        bt.setLabel(getTranslation("BreakType.Title"));
        bt.addValueChangeListener((event) -> {
            BreakType bType = event.getValue();
            ct.setValue(guessCountdownFromBreak(bType));
        });

        ct = new RadioButtonGroup<CountdownType>();
        ct.setItems(CountdownType.values());
        ct.setRenderer(new TextRenderer<CountdownType>(
                (item) -> getTranslation(CountdownType.class.getSimpleName() + "." + item.name())));
        ct.setLabel(getTranslation("CountdownType.Title"));
        ct.prependComponents(CountdownType.DURATION, new Paragraph(""));
        ct.prependComponents(CountdownType.INDEFINITE, new Paragraph(""));

        nf.addValueChangeListener(e -> setBreakTimeRemaining(CountdownType.DURATION, nf, tp, dp));
        Locale locale = new Locale("en", "SE"); // ISO 8601 style dates and time
        tp.setLocale(locale);
        tp.addValueChangeListener(e -> setBreakTimeRemaining(CountdownType.TARGET, nf, tp, dp));
        dp.setLocale(locale);
        tp.addValueChangeListener(e -> setBreakTimeRemaining(CountdownType.TARGET, nf, tp, dp));
        minutes = new Label("minutes");

        ct.addComponents(CountdownType.DURATION, nf, new Label(" "), minutes, new Div());
        ct.addComponents(CountdownType.TARGET, dp, new Label(" "), tp);
    }

    private void configureTimerDisplay() {
        Div countdown = new Div(new BreakTimerElement());
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

    private long setBreakTimeRemaining(CountdownType cType, NumberField nf, TimePicker tp, DatePicker dp) {
        LocalDateTime target = LocalDateTime.now();
        BreakType bType = bt.getValue();
        cType = guessCountdownFromBreak(bType);
        if (cType == CountdownType.DURATION) {
            Double value = nf.getValue();
            target = LocalDateTime.now().plusMinutes(value != null ? value.intValue() : 0);
        } else if (cType == CountdownType.TARGET) {
            LocalDate date = dp.getValue();
            LocalTime time = tp.getValue();
            target = LocalDateTime.of(date, time);
        }
        long timeRemaining = LocalDateTime.now().until(target, ChronoUnit.MILLIS);
        OwlcmsSession.withFop(fop -> fop.getBreakTimer().setTimeRemaining((int) timeRemaining));
        return timeRemaining;
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

    private void setOrigin(Object origin) {
        this.origin = origin;
    }

    private void setValues(CountdownType cType, NumberField nf, TimePicker tp, DatePicker dp) {
        LocalDateTime now = LocalDateTime.now();
        if (cType == CountdownType.DURATION) {
            Double value = nf.getValue();
            LocalDateTime target = now.plusMinutes(value != null ? value.intValue() : 0);
            dp.setValue(target.toLocalDate());
            tp.setValue(target.toLocalTime());
        } else if (cType == CountdownType.TARGET) {
            computeRoundedTargetValues(tp, dp);
        }
    }

    private void switchToDuration(CountdownType cType, NumberField nf, TimePicker tp, DatePicker dp) {
        nf.setEnabled(true);
        minutes.setEnabled(true);
        dp.setEnabled(false);
        tp.setEnabled(false);
        nf.focus();
        nf.setAutoselect(true);
    }

    private void switchToTarget(CountdownType cType, NumberField nf, TimePicker tp, DatePicker dp) {
        nf.setEnabled(false);
        minutes.setEnabled(false);
        dp.setEnabled(true);
        tp.setEnabled(true);
        tp.focus();
    }

    private void syncWithFop() {
        OwlcmsSession.withFop(fop -> {
            switch (fop.getState()) {
            case CURRENT_ATHLETE_DISPLAYED:
            case BREAK:
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
        });
    }

}
