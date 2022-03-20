/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.shared;

import static app.owlcms.fieldofplay.FOPState.INACTIVE;
import static app.owlcms.ui.shared.BreakManagement.CountdownType.DURATION;
import static app.owlcms.ui.shared.BreakManagement.CountdownType.INDEFINITE;
import static app.owlcms.ui.shared.BreakManagement.CountdownType.TARGET;
import static app.owlcms.uievents.BreakType.BEFORE_INTRODUCTION;
import static app.owlcms.uievents.BreakType.FIRST_SNATCH;
import static app.owlcms.uievents.BreakType.JURY;
import static app.owlcms.uievents.BreakType.TECHNICAL;
import static java.time.temporal.ChronoUnit.MILLIS;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.TextRenderer;

import app.owlcms.components.GroupCategorySelectionMenu;
import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.fields.DurationField;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.IBreakTimer;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.BreakSetTime;
import app.owlcms.utils.IdUtils;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class BreakManagement extends VerticalLayout implements SafeEventBusRegistration {
    public enum CountdownType {
        DURATION, INDEFINITE, TARGET
    }

    private static final Duration DEFAULT_DURATION = Duration.ofMinutes(10L);

    private boolean inactive = false;
    private Long timeRemaining = null;

    private Button athleteButton;
    private Button breakEnd = null;
    private Button breakPause = null;
    private Button breakReset = null;

    private Button breakStart = null;
    private BreakTimerElement breakTimerElement;
    private Button countdownButton;

    private RadioButtonGroup<BreakType> countdownRadios;
    private DatePicker datePicker = new DatePicker();
    private HorizontalLayout dt;
    private DurationField durationField = new DurationField();
    private RadioButtonGroup<CountdownType> durationRadios;
    private Button endIntroButton;
    private Button endMedalCeremony;

    private Button endOfficials;
    private Long id;
    private boolean ignoreBreakTypeValueChange = false;
    private boolean ignoreDurationValueChange = false;
    private boolean ignoreListeners = false;
    private boolean ignoreNextDisable;

    final private Logger logger = (Logger) LoggerFactory.getLogger(BreakManagement.class);
    private Category medalCategory;

    private Group medalGroup;

    private Label minutes;

    private Object origin;
    private Dialog parentDialog;

    private BreakType requestedBreakType;

    private TimePicker timePicker = new TimePicker();

    private HorizontalLayout timer;

    private EventBus uiEventBus;

    private Button startMedalCeremony;

    {
        logger.setLevel(Level.INFO);
    }

    /**
     * Persona-specific display (e.g. for the jury, the technical controller, etc.)
     *
     * @param origin
     * @param brt
     * @param cdt
     */
    BreakManagement(Object origin, BreakType brt, CountdownType cdt, Dialog parentDialog) {
        logger.setLevel(Level.DEBUG);
        init(origin, brt, cdt, parentDialog);
        setRequestedBreakType(brt);
    }

    /**
     * Used by the announcer, no specific context
     *
     * @param origin the origin
     */
    BreakManagement(Object origin, Dialog parentDialog) {
        logger.setLevel(Level.DEBUG);
        init(origin, null, CountdownType.DURATION, parentDialog);
        setRequestedBreakType(null);
    }

    public Category getMedalCategory() {
        return medalCategory;
    }

    /**
     * Everything has been created and has meaningful values, add value change listeners now to avoid spuriuous
     * triggering during interface build-up.
     *
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // logger.debug("breakManagement attach");
        super.onAttach(attachEvent);
        OwlcmsSession.withFop(fop -> {
            // we listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        });

        durationRadios.addValueChangeListener(e -> {
            CountdownType cType = e.getValue();
            if (cType == CountdownType.DURATION) {
                switchToDuration();
            } else if (cType == CountdownType.TARGET) {
                switchToTarget();
            } else {
                switchToIndefinite();
            }
        });
        countdownRadios.addValueChangeListener(event -> {
            // prevent infinite loop
            if (ignoreBreakTypeValueChange || ignoreListeners) {
                return;
            }
            logger.debug("bt new value {} {} {} {}", event.getValue(), ignoreBreakTypeValueChange, ignoreListeners,
                    LoggerUtils.whereFrom());

            BreakType bType = event.getValue();
            if (bType == BEFORE_INTRODUCTION) {
                computeDefaultTimeValues();
            }
            CountdownType mapBreakTypeToCountdownType = mapBreakTypeToDurationValue(bType);
            logger.debug("setting countdown {} ignored={}", mapBreakTypeToCountdownType, ignoreListeners);
            setDurationValue(mapBreakTypeToCountdownType);
            masterPauseBreak(bType);

            if (bType != null && (bType.isInterruption())) {
                logger.debug("starting break from radiobutton setvalue {}", bType);
                startIndefiniteBreakImmediately(bType);
            } else {
                setBreakTimerFromFields(durationRadios.getValue());
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
                // logger.debug("not in a break {} {}", fop.getState(), getRequestedBreakType());
                if (checkImmediateBreak()) {
                    logger.debug("immediate break");
                    fop.getBreakTimer().setIndefinite();
                    startIndefiniteBreakImmediately(getRequestedBreakType());
                } else {
                    logger.debug("not immediate break");
                    setBreakTimerFromFields(durationRadios.getValue());
                    startEnabled();
                }
            } else {
                logger.debug("in a break");
                syncWithFop();
            }
            // logger.trace("onAttach fop break = {}",fop.getBreakType());
            if (fop.getCeremonyType() == CeremonyType.INTRODUCTION) {
                endIntroButton.focus();
                endIntroButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            }
        });
        // not needed after initial setup
        setRequestedBreakType(null);
    }

    void cleanup() {
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

    BreakTimerElement getBreakTimer() {
        return breakTimerElement;
    }

    private void assembleDialog(VerticalLayout dialog) {
        VerticalLayout cd = createCountdownColumn();
        VerticalLayout ce = createCeremoniesColumn();
        HorizontalLayout columns = new HorizontalLayout(cd, ce);
        columns.setWidth("100%");
        columns.setJustifyContentMode(JustifyContentMode.EVENLY);
        dialog.add(columns);
    }

    /**
     * @return true if we triggered an immediate break.
     */
    private boolean checkImmediateBreak() {
        return (getRequestedBreakType() != null
                && (getRequestedBreakType() == JURY || getRequestedBreakType() == TECHNICAL));
    }

    private void computeDefaultTimeValues() {
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
        datePicker.setWidth("16ch");
        timePicker.setValue(LocalTime.of(nextHr, nextStepMin));
        timePicker.setWidth("11ch");
        logger.debug("setting default duration as default {}", LoggerUtils.whereFrom());
        setDurationField(DEFAULT_DURATION);
    }

    private Integer computeTimerRemainingFromFields(CountdownType countdownType) {
        logger.debug("computeTimerRemainingFromFields");
        Integer tr;
        if (countdownType == INDEFINITE) {
            tr = null;
        } else if (countdownType == TARGET) {
            // recompute duration, in case there was a pause.
            setBreakTimerFromFields(TARGET);
            tr = timeRemaining.intValue();
        } else {
            setBreakTimerFromFields(DURATION);
            tr = timeRemaining.intValue();
        }
        return tr;
    }

    private void createAttemptBoardInfoSelection() {
        dt = new HorizontalLayout();
        athleteButton = new Button(
                getTranslation("DisplayType.LIFT_INFO"), (e) -> {
                    OwlcmsSession.withFop(fop -> {
                        fop.recomputeLiftingOrder();
                        fop.uiDisplayCurrentAthleteAndTime(false, new FOPEvent(null, this), true);
                    });
                });
        athleteButton.setTabIndex(-1);
        countdownButton = new Button(
                getTranslation("DisplayType.COUNTDOWN_INFO"), (e) -> {
                    OwlcmsSession.withFop(fop -> {
                        fop.recomputeLiftingOrder();
                        OwlcmsSession.getFop().getUiEventBus()
                                .post(new UIEvent.BreakStarted(0, this.getOrigin(), true, countdownRadios.getValue(),
                                        durationRadios.getValue(), LoggerUtils.stackTrace(), false));
                    });
                });
        countdownButton.setTabIndex(-1);
        athleteButton.getThemeNames().add("secondary contrast");
        countdownButton.getThemeNames().add("secondary contrast");
        dt.add(countdownButton, athleteButton);
    }

    private FlexLayout createBreakTimerButtons() {
        breakStart = new Button(AvIcons.PLAY_ARROW.create(), (e) -> {
            OwlcmsSession.withFop(fop -> {
                BreakType value = countdownRadios.getValue();
                if (value != null && value.isCountdown()) {
                    // force FOP to accept our break and value as new
                    fop.setBreakType(null);
                    fop.setCountdownType(null);
                    fop.getBreakTimer()
                            .setTimeRemaining(this.computeTimerRemainingFromFields(durationRadios.getValue()), false);
                }
            });
            masterStartBreak();
        });
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

    private VerticalLayout createCeremoniesColumn() {
        VerticalLayout ce = new VerticalLayout();

        HorizontalLayout introButtons = new HorizontalLayout();
        Button startIntroButton = new Button(
                getTranslation("BreakMgmt.startIntro"), (e) -> {
                    OwlcmsSession.withFop(fop -> {
                        // do nothing if we are already in during introduction
                        if (fop.getCeremonyType() == CeremonyType.INTRODUCTION) {
                            return;
                        }

                        // we are in a running break such as before snatch already scheduled and want to switch to intro
                        masterStartCeremony(fop, CeremonyType.INTRODUCTION);

                        // close so we can read the list of participants
                        parentDialog.close();
                    });
                });
        startIntroButton.setTabIndex(-1);
        endIntroButton = new Button(
                getTranslation("BreakMgmt.endIntro"), (e) -> {
                    OwlcmsSession.withFop(fop -> {
                        endIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                        if (fop.getCeremonyType() != CeremonyType.INTRODUCTION) {
                            return;
                        }
                        boolean switchToSnatch = true; // (fop.getBreakTimer().getBreakType() == DURING_INTRODUCTION);
                        // logger.debug("switch to snatch {} {}", fop.getBreakTimer().getBreakType() , switchToSnatch);
                        masterEndCeremony(fop, CeremonyType.INTRODUCTION);
                        if (switchToSnatch) {
                            durationField.setValue(DEFAULT_DURATION);
                            setCountdownValue(FIRST_SNATCH);
                            durationRadios.setValue(DURATION);
                            ignoreNextDisable = true;
                            startEnabled();
                        }
                    });
                });
        endIntroButton.setTabIndex(-1);
        startIntroButton.getThemeNames().add("secondary contrast");
        endIntroButton.getThemeNames().add("secondary contrast");
        introButtons.add(startIntroButton, endIntroButton);

        ce.add(label("BreakMgmt.IntroductionOfAthletes"));
        ce.add(introButtons);

        HorizontalLayout officialsButtons = new HorizontalLayout();
        Button startOfficials = new Button(
                getTranslation("BreakMgmt.startOfficials"), (e) -> {
                    endOfficials.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    OwlcmsSession.withFop(fop -> {
                        if (fop.getCeremonyType() == CeremonyType.OFFICIALS_INTRODUCTION) {
                            return;
                        }
                        masterStartCeremony(fop, CeremonyType.OFFICIALS_INTRODUCTION);
                    });
                });
        startOfficials.setTabIndex(-1);
        endOfficials = new Button(
                getTranslation("BreakMgmt.endOfficials"), (e) -> {
                    endOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    OwlcmsSession.withFop(fop -> {
                        if (fop.getCeremonyType() != CeremonyType.OFFICIALS_INTRODUCTION) {
                            return;
                        }
                        masterEndCeremony(fop, CeremonyType.OFFICIALS_INTRODUCTION);
                    });
                });
        endOfficials.setTabIndex(-1);
        startOfficials.getThemeNames().add("secondary contrast");
        endOfficials.getThemeNames().add("secondary contrast");
        officialsButtons.add(startOfficials, endOfficials);

        ce.add(new Hr());
        ce.add(label("BreakMgmt.IntroductionOfOfficials"));
        ce.add(officialsButtons);

        ce.add(new Hr());
        HorizontalLayout medalButtons = new HorizontalLayout();

        List<Group> groups = GroupRepository.findAll();
        FieldOfPlay fop2 = OwlcmsSession.getFop();
        GroupCategorySelectionMenu groupCategorySelectionMenu = new GroupCategorySelectionMenu(groups, fop2,
                // group has been selected
                (g1, c1, fop1) -> selectCeremonyCategory(g1, c1, fop1),
                // no group
                (g1, c1, fop1) -> selectCeremonyCategory(null, c1, fop1));

        startMedalCeremony = new Button(
                getTranslation("BreakMgmt.startMedals"), (e) -> {
                    startMedalCeremony.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    endMedalCeremony.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    OwlcmsSession.withFop(fop -> {
                        inactive = fop.getState() == INACTIVE;
                        fop.fopEventPost(
                                new FOPEvent.CeremonyStarted(CeremonyType.MEDALS, getMedalGroup(), getMedalCategory(),
                                        this));
                    });
                });
        startMedalCeremony.setTabIndex(-1);
        endMedalCeremony = new Button(
                getTranslation("BreakMgmt.endMedals"), (e) -> {
                    OwlcmsSession.withFop(fop -> {
                        endMedalCeremony.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                        fop.getFopEventBus()
                                .post(new FOPEvent.CeremonyDone(CeremonyType.MEDALS, this.getOrigin()));
                        if (inactive) {
                            setBreakTimerFromFields(TARGET);
                        }
                    });
                });
        endMedalCeremony.setTabIndex(-1);
        startMedalCeremony.getThemeNames().add("secondary contrast");
        endMedalCeremony.getThemeNames().add("secondary contrast");
        medalButtons.add(startMedalCeremony, endMedalCeremony);

        ce.add(label("PublicMsg.Medals"), groupCategorySelectionMenu);
        ce.add(medalButtons);

        Hr hr = new Hr();
        hr.getElement().setAttribute("style", "margin-top: 2ex");
        Label label = label("DisplayType.Title");
        ce.add(hr, label);
        ce.add(dt);
        return ce;
    }

    private void masterEndCeremony(FieldOfPlay fop, CeremonyType ceremonyType) {
        fop.getFopEventBus()
                .post(new FOPEvent.CeremonyDone(ceremonyType, this.getOrigin()));
    }

    private void masterStartCeremony(FieldOfPlay fop, CeremonyType ceremonyType) {
        fop.fopEventPost(
                new FOPEvent.CeremonyStarted(ceremonyType, fop.getGroup(), null, this));
    }

    private VerticalLayout createCountdownColumn() {
        countdownRadios = new RadioButtonGroup<>();
        countdownRadios.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

        // interruptions, then countdowns
        List<BreakType> breaks = Arrays.asList(BreakType.values()).stream()
                .filter(countdownRadios -> countdownRadios.isInterruption()).collect(Collectors.toList());
        List<BreakType> countdowns = Arrays.asList(BreakType.values()).stream().filter(bt -> bt.isCountdown())
                .collect(Collectors.toList());
        breaks.addAll(countdowns);
        countdownRadios.setItems(breaks);
        countdownRadios.setRenderer(new TextRenderer<BreakType>(
                (item) -> getTranslation(BreakType.class.getSimpleName() + "." + item.name())));

        // kludgey way to split the radio buttons in two sections
        Div div = new Div(label("BreakType.Title"));
        div.getElement().setAttribute("style", "margin-bottom: 1ex");
        countdownRadios.prependComponents(TECHNICAL, div);

        Hr hr = new Hr();
        hr.getElement().setAttribute("style", "margin-top: 1ex");
        Div div1 = new Div(label("CountdownType.Title"));
        div1.getElement().setAttribute("style", "margin-top: 2ex; margin-bottom: 1ex");
        countdownRadios.prependComponents(BEFORE_INTRODUCTION, hr, div1);

        durationRadios = new RadioButtonGroup<>();
        durationRadios.setItems(CountdownType.values());
        durationRadios.setRenderer(new TextRenderer<CountdownType>(
                (item) -> getTranslation(CountdownType.class.getSimpleName() + "." + item.name())));
        durationRadios.prependComponents(INDEFINITE, new Paragraph(""));
        durationRadios.prependComponents(TARGET, new Paragraph(""));

        Locale locale = new Locale("en", "SE"); // ISO 8601 style dates and time
        timePicker.setLocale(locale);
        datePicker.setLocale(locale);
        minutes = new Label("minutes");

        durationRadios.addComponents(DURATION, durationField, new Label(" "), minutes, new Div());
        durationRadios.addComponents(TARGET, datePicker, new Label(" "), timePicker);

        createTimerDisplay();
        FlexLayout timerButtons = createBreakTimerButtons();

        VerticalLayout cd = new VerticalLayout();

        cd.add(countdownRadios);
        cd.add(durationRadios);
        cd.add(new Hr());
        cd.add(timer);
        cd.setAlignSelf(Alignment.CENTER, timer);
        cd.add(timerButtons);

        return cd;
    }

    private void createTimerDisplay() {
        breakTimerElement = getBreakTimerElement();
        breakTimerElement.setParent("BreakManagement_" + id);
        Div countdown = new Div(breakTimerElement);
        countdown.getStyle().set("font-size", "x-large");
        countdown.getStyle().set("font-weight", "bold");
        timer = new HorizontalLayout(countdown);
        // timer.setWidth("100%");
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

    private ComponentEventListener<ClickEvent<Button>> endBreak(Dialog dialog) {
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

    private String formattedDuration(Long milliseconds) {
        return (milliseconds != null && milliseconds >= 0) ? DurationFormatUtils.formatDurationHMS(milliseconds)
                : (milliseconds != null ? milliseconds.toString() : "-");
    }

    private BreakTimerElement getBreakTimerElement() {
        if (this.breakTimerElement == null) {
            this.breakTimerElement = new BreakTimerElement("BreakManagement");
            // logger.debug("------ created {}",breakTimerElement.id);
        }
        return this.breakTimerElement;
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

    private void init(Object origin, BreakType brt, CountdownType cdt, Dialog parentDialog) {
        // logger.debug("init brt={} cdt={} from {}", brt, cdt, LoggerUtils.whereFrom());
        this.id = IdUtils.getTimeBasedId();
        ignoreBreakTypeValueChange = false;
        this.setOrigin(origin);
        this.parentDialog = parentDialog;

        createCountdownColumn();
        createAttemptBoardInfoSelection();
        computeDefaultTimeValues();

        setCountdownValue(brt);
        setDurationValue(cdt);
        assembleDialog(this);
        OwlcmsSession.withFop(fop -> {
            uiEventBusRegister((Component) origin, fop);
        });
    }

    private Label label(String key) {
        Label label = new Label(getTranslation(key));
        label.getElement().setAttribute("style", "font-weight: bold");
        return label;
    }

    private CountdownType mapBreakTypeToDurationValue(BreakType bType) {
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

    private void masterPauseBreak(BreakType bType) {
        OwlcmsSession.withFop(fop -> {
            IBreakTimer breakTimer = fop.getBreakTimer();
            if (breakTimer.isRunning()) {
                // logger.debug("pausing current break {} due to {}", fop.getBreakType(), bType);
                breakTimer.stop();
                fop.fopEventPost(
                        new FOPEvent.BreakPaused(breakTimer.getTimeRemainingAtLastStop(), this.getOrigin()));

            }
        });
        logger.debug("paused; enabling start");
        startEnabled();
    }

    /**
     * Pause and set time according to current fields
     */
    private void masterResetBreak() {
        CountdownType countdownType = durationRadios.getValue();
        Integer tr = computeTimerRemainingFromFields(countdownType);
        doResetTimer(tr);
    }

    private void masterStartBreak() {
        OwlcmsSession.withFop(fop -> {
            masterStartBreak(fop, true);
        });
        // e.getSource().setEnabled(false);
        logger.debug("start break disable start");
        startDisabled();
        return;
    }

    private void masterStartBreak(FieldOfPlay fop, boolean force) {
        BreakType breakType = countdownRadios.getValue();
        CountdownType countdownType = durationRadios.getValue();
        masterStartBreak(fop, breakType, countdownType, force);
    }

    private void masterStartBreak(FieldOfPlay fop, BreakType breakType, CountdownType countdownType, boolean force) {
        logger.warn("masterStartBreak timeRemaining {} breakType {} indefinite {} isRunning {}", timeRemaining, breakType,
                fop.getBreakTimer().isIndefinite(), fop.getBreakTimer().isRunning());
        if (timeRemaining == null && fop.getBreakTimer() != null) {
            timeRemaining = (long) fop.getBreakTimer().liveTimeRemaining();
        }
//        fop.fopEventPost(new FOPEvent.BreakStarted(breakType, countdownType, fop.getBreakTimer(), this.getOrigin()));
        fop.fopEventPost(new FOPEvent.BreakStarted(breakType, countdownType,
                countdownType == CountdownType.INDEFINITE ? null : timeRemaining.intValue(), getTarget(),
                countdownType == CountdownType.INDEFINITE,
                this.getOrigin()));
    }

    private void safeSetBT(BreakType breakType) {
        try {
            ignoreBreakTypeValueChange = true;
            setCountdownValue(breakType);
        } finally {
            ignoreBreakTypeValueChange = false;
        }
    }

    private void selectCeremonyCategory(Group g, Category c, FieldOfPlay fop) {
        endMedalCeremony.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        startMedalCeremony.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        setMedalGroup(g);
        setMedalCategory(c);
    }

    private void setBreakTimerFromFields(CountdownType cType) {
        if (ignoreListeners) {
            return;
        }
        // logger.trace("setBreakTimerFromFields curCType={} from={}", cType, LoggerUtils.whereFrom());
        LocalDateTime now = LocalDateTime.now();

        if (getRequestedBreakType() != null) {
            safeSetBT(requestedBreakType);
            cType = null;
            setRequestedBreakType(null);
        } else {
            cType = durationRadios.getValue();
        }

        BreakType bType = countdownRadios.getValue();
        if (cType == null) {
            cType = mapBreakTypeToDurationValue(bType);
            durationRadios.setValue(cType);
        }
        final CountdownType curCType = cType;
        OwlcmsSession.withFop(fop -> {
            if (curCType == CountdownType.TARGET) {
                LocalDateTime target = getTarget();
                timeRemaining = now.until(target, MILLIS);
                logger.debug("setBreakTimerFromFields target-derived duration {}",
                        formattedDuration(timeRemaining));
                fop.getBreakTimer().setTimeRemaining(timeRemaining.intValue(), false);
                fop.getBreakTimer().setBreakDuration(timeRemaining.intValue());
                fop.getBreakTimer().setEnd(null);
                breakTimerElement.slaveBreakSet(
                        new BreakSetTime(bType, curCType, timeRemaining.intValue(), target, false, this.getOrigin(),
                                LoggerUtils.stackTrace()));
            } else if (curCType == CountdownType.INDEFINITE) {
                logger.debug("setBreakTimerFromFields indefinite");
                timeRemaining = null;
                breakTimerElement.slaveBreakSet(
                        new BreakSetTime(bType, curCType, 0, null, true, this, LoggerUtils.stackTrace()));
            } else {
                Duration value;
                value = durationField.getValue();
                value = (value == null ? DEFAULT_DURATION : value);
                timeRemaining = (value != null ? value.toMillis() : 0L);
                fop.getBreakTimer().setTimeRemaining(timeRemaining.intValue(), false);
                fop.getBreakTimer().setBreakDuration(timeRemaining.intValue());
                fop.getBreakTimer().setEnd(null);
                logger.debug("setBreakTimerFromFields explicit duration {}",
                        formattedDuration(timeRemaining));
                // this sets time locally only
                breakTimerElement.slaveBreakSet(
                        new BreakSetTime(bType, curCType, timeRemaining.intValue(), null, false, this.getOrigin(),
                                LoggerUtils.stackTrace()));
            }
        });
        return;
    }

    private void setCountdownValue(BreakType breakType) {
        // logger.debug("set countdown radio value {} {}", breakType, LoggerUtils.whereFrom());
        countdownRadios.setValue(breakType);
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

    private void setDurationValue(CountdownType ct2) {
        logger.debug("setting durationRadios {}  from {}", ct2, LoggerUtils.whereFrom());
        durationRadios.setValue(ct2);
    }

    private void setMedalCategory(Category medalCategory) {
        this.medalCategory = medalCategory;
    }

    private void setMedalGroup(Group medalGroup) {
        this.medalGroup = medalGroup;
    }

    private void setOrigin(Object origin) {
        this.origin = origin;
    }

    private void setRequestedBreakType(BreakType requestedBreakType) {
        // logger.debug("requestedBreakType={} {}", requestedBreakType, LoggerUtils.stackTrace());
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
            CountdownType value = durationRadios.getValue();
            if (value != null) {
                switch (value) {
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
            }
        } finally {
            ignoreListeners = resetIgnoreListeners;
        }

    }

    @Subscribe
    private void slaveBreakDone(UIEvent.BreakDone e) {
        synchronized (this) {
            try {
                // logger.debug("Break Done {}", LoggerUtils. stackTrace());
                ignoreListeners = true;
                UIEventProcessor.uiAccess(this, uiEventBus, e,
                        () -> parentDialog.close());
            } finally {
                ignoreListeners = false;
            }
        }
    }

    @Subscribe
    private void slaveBreakPause(UIEvent.BreakPaused e) {
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
    private void slaveBreakSet(UIEvent.BreakSetTime e) {
        // do nothing
    }

    @Subscribe
    private void slaveBreakStart(UIEvent.BreakStarted e) {
        synchronized (this) {
            try {
                ignoreListeners = true;
                if (e.isDisplayToggle()) {
                    return;
                }
                UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
                    if (!e.getPaused()) {
                        startDisabled();
                    } else {
                        startEnabled();
                    }
                    safeSetBT(e.getBreakType());
                });
            } finally {
                ignoreListeners = false;
            }
        }

    }

    private void startDisabled() {
        if (ignoreNextDisable) {
            ignoreNextDisable = false;
            return;
        }
        // logger.trace("start disabled {}", LoggerUtils.whereFrom(1));
        breakStart.setEnabled(false);
        breakPause.setEnabled(true);
        breakEnd.setEnabled(true);
    }

    private void startEnabled() {
        // logger.trace("start enabled {}", LoggerUtils.whereFrom());
        breakStart.setEnabled(true);
        breakPause.setEnabled(false);
        breakEnd.setEnabled(true);
    }

    private void startIndefiniteBreakImmediately(BreakType bType) {
        timeRemaining = null;
        ignoreBreakTypeValueChange = true;
        setDurationValue(CountdownType.INDEFINITE);
        // logger.debug("setting default duration for indefinite break");
        setDurationField(DEFAULT_DURATION);
        BreakType breakType = bType != null ? bType : BreakType.TECHNICAL;
        safeSetBT(breakType);
        masterStartBreak();
//        breakTimerElement.slaveBreakStart(new BreakStarted(null, this.getOrigin(), false, breakType, durationRadios.getValue()));
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
        // logger.trace("switchToIndefinite {}", LoggerUtils.stackTrace());
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
            // boolean breakTimerRunning = fopBreakTimer.isRunning();

            logger.debug("syncWithFop {} {} {}", fopState, fop.getBreakTimer().liveTimeRemaining(),
                    fop.getBreakTimer().isRunning());

            boolean resetIgnoreListeners = ignoreListeners;
            try {
                ignoreListeners = true;
                running[0] = false;

                // default values
                computeDefaultTimeValues();
                // List<Athlete> order;

                switch (fopState) {
                case INACTIVE:
                case BREAK:
                    // logger.trace(" syncWithFOP: break under way {} {} indefinite={}", fopBreakType,
                    // fopCountdownType,fopBreakTimer.isIndefinite());

                    if (fopCountdownType == INDEFINITE) {
                        fopLiveTimeRemaining = (int) DEFAULT_DURATION.toMillis();
                    }

                    // override from FOP
                    setTimingFieldsFromBreakTimer(fopLiveTimeRemaining, fopBreakDuration);

                    BreakType breakType;
                    if (fopState == INACTIVE) {
                        breakType = BEFORE_INTRODUCTION;
                    } else {
                        breakType = fopBreakType;
                    }
                    safeSetBT(breakType);
                    setDurationValue(fopCountdownType);
                    breakTimerElement.syncWithFopBreakTimer();
                    break;
                default:
                    throw new UnsupportedOperationException("missing case statement");
                }
            } finally {
                ignoreListeners = resetIgnoreListeners;
            }

        });
        return running[0];
    }

    private Group getMedalGroup() {
        return medalGroup;
    }

}
