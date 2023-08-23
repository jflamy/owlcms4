/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import static app.owlcms.fieldofplay.FOPState.INACTIVE;
import static app.owlcms.uievents.BreakType.BEFORE_INTRODUCTION;
import static app.owlcms.uievents.BreakType.FIRST_SNATCH;
import static java.time.temporal.ChronoUnit.MILLIS;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.IBreakTimer;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.IdUtils;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.NaturalOrderComparator;
import ch.qos.logback.classic.Logger;
import jakarta.annotation.Nonnull;

@SuppressWarnings("serial")
public class BreakManagement extends VerticalLayout implements SafeEventBusRegistration {

	private static final Duration DEFAULT_DURATION = Duration.ofMinutes(10L);
	private Button countdownEnd = null;
	private Button countdownStart = null;
	private BreakTimerElement breakTimerElement;
	private BreakType breakType;
	private RadioButtonGroup<BreakType> countdownRadios;
	private List<BreakType> countdowns;
	private CountdownType countdownType;
	private RadioButtonGroup<CountdownType> countdownTypeRadios;
	private DatePicker datePicker = new DatePicker();
	private DurationField durationField = new DurationField();
	private Button endIntroButton;
	private Button endMedalCeremony;
	private Button endOfficials;
	private FieldOfPlay fop;
	private Long id;
	private boolean ignoreNextDisable;
	private boolean inactive = false;
	private RadioButtonGroup<BreakType> interruptionRadios;
	private List<BreakType> interruptions;
	final private Logger logger = (Logger) LoggerFactory.getLogger(BreakManagement.class);
	private Category medalCategory;
	private Group medalGroup;
	private NativeLabel minutes;
	private Paragraph noCountdown = new Paragraph(Translator.translate("No countdown selected."));
	private Object origin;
	private Dialog parentDialog;
	private Button resumeCompetition = null;
	private Button startIntroButton;
	private Button startMedalCeremony;
	private Button startOfficials;
	private Button stopCompetition = null;
	private TimePicker timePicker = new TimePicker();
	private HorizontalLayout timer;
	private Long timeRemaining = null;
	private EventBus uiEventBus;
	private Paragraph waitText = new Paragraph(Translator.translate("Wait.Text"));

	/**
	 * Persona-specific display (e.g. for the jury, the technical controller, etc.)
	 *
	 * @param requestedBreak
	 * @param requestedCountdownType
	 * @param origin
	 */
	BreakManagement(@Nonnull FieldOfPlay fop, BreakType requestedBreak, CountdownType requestedCountdownType,
	        Integer countdownSecondsRemaining, Dialog parentDialog, Object origin) {
		setPadding(false);
		setMargin(false);
		this.setSizeFull();

		if (requestedBreak == null && fop.getState() != FOPState.BREAK) {
			requestedBreak = BreakType.TECHNICAL;
			requestedCountdownType = CountdownType.INDEFINITE;
		}

		this.fop = OwlcmsSession.getFop();
		createUI(parentDialog);

		initState(origin, requestedBreak, requestedCountdownType, countdownSecondsRemaining);
	}

	/**
	 * Set the break according to field of play.
	 *
	 * @param origin the origin
	 */
	BreakManagement(FieldOfPlay fop, Dialog parentDialog, Object origin) {
		// logger.warn("******* no request");
		this.fop = OwlcmsSession.getFop();
		createUI(parentDialog);

		CountdownType countdownType2 = fop.getCountdownType();
		initState(origin, fop.getBreakType(),
		        countdownType2 != null ? countdownType2 : mapBreakTypeToDurationValue(fop.getBreakType()),
		        fop.getBreakTimer().getTimeRemaining());
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
		super.onAttach(attachEvent);
		OwlcmsSession.withFop(fop -> {
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});

		setCountdownTypeValue(countdownType);
		setBreakValue(breakType);
		syncWithFop();
		setCountdownFieldVisibility(countdownType);

		countdownTypeRadios.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			CountdownType cType = e.getValue();
			setCountdownFieldVisibility(cType);
		});
		interruptionRadios.addValueChangeListener(event -> {
			if (!event.isFromClient()) {
				return;
			}
			setBreakValue(event.getValue());
			setBreakType(event.getValue());
		});
		countdownRadios.addValueChangeListener(event -> {
			if (!event.isFromClient()) {
				return;
			}
			logger.warn("bt new value {} {}", event.getValue(), LoggerUtils.whereFrom());

			BreakType bType = event.getValue();
			if (bType == BEFORE_INTRODUCTION || bType == BreakType.CEREMONY) {
				computeDefaultTimeValues();
			}
			CountdownType mapBreakTypeToCountdownType = mapBreakTypeToDurationValue(bType);
			logger.warn("setting countdown {} ignored={}", mapBreakTypeToCountdownType);
			setCountdownTypeValue(mapBreakTypeToCountdownType);
			setBreakValue(bType);
			setBreakType(bType);

			if (bType != null && (bType.isInterruption())) {
				logger.warn("starting break from radiobutton setvalue {}", bType);
				// startIndefiniteBreakImmediately(bType);
				setBreakTimerFromFields();
			} else {
				setBreakTimerFromFields();
			}
		});
		durationField.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			computeTimerRemainingFromFields(CountdownType.DURATION);
			doResetTimer(timeRemaining.intValue());
		});
		timePicker.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			computeTimerRemainingFromFields(CountdownType.TARGET);
			doResetTimer(timeRemaining.intValue());
		});
		datePicker.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			computeTimerRemainingFromFields(CountdownType.TARGET);
			doResetTimer(timeRemaining.intValue());
		});
	}

	void cleanup() {
		logger.warn("removing {}", breakTimerElement);
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

	private void assembleDialog(VerticalLayout dialogLayout) {
		createTimerDisplay();
		// dialogLayout.add(timer);

		VerticalLayout cd = createBreakColumn();
		VerticalLayout ce = createCeremoniesColumn();
		HorizontalLayout columns = new HorizontalLayout(cd, ce);
		columns.setWidth("100%");
		columns.setJustifyContentMode(JustifyContentMode.EVENLY);
		dialogLayout.add(new Hr());
		dialogLayout.add(columns);
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
		logger.warn("setting default duration as default {}", LoggerUtils.whereFrom());
		setDurationField(DEFAULT_DURATION);
	}

	private Integer computeTimerRemainingFromFields(CountdownType countdownType) {
		logger.warn("computeTimerRemainingFromFields");
		Integer tr;
		if (countdownType == CountdownType.INDEFINITE) {
			tr = null;
		} else if (countdownType == CountdownType.TARGET) {
			// recompute duration, in case there was a pause.
			setBreakTimerFromFields();
			tr = timeRemaining != null ? timeRemaining.intValue() : null;
		} else {
			setBreakTimerFromFields();
			tr = timeRemaining != null ? timeRemaining.intValue() : null;
		}
		return tr;
	}

	@SuppressWarnings("deprecation")
	private VerticalLayout createBreakColumn() {
		interruptionRadios = new RadioButtonGroup<>();
		interruptionRadios.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

		countdownRadios = new RadioButtonGroup<>();
		countdownRadios.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

		// interruptions, then countdowns
		interruptions = Arrays.asList(BreakType.values()).stream()
		        .filter(countdownRadios -> countdownRadios.isInterruption()).collect(Collectors.toList());
		countdowns = Arrays.asList(BreakType.values()).stream().filter(bt -> bt.isCountdown())
		        .collect(Collectors.toList());

		interruptionRadios.setItems(interruptions);
		interruptionRadios.setRenderer(new TextRenderer<>(
		        (item) -> {
			        String s = getTranslation(BreakType.class.getSimpleName() + "." + item.name());
			        return s;
		        }));

		countdownRadios.setItems(countdowns);
		countdownRadios.setRenderer(new TextRenderer<>(
		        (item) -> {
			        String s = getTranslation(BreakType.class.getSimpleName() + "." + item.name());
			        return s;
		        }));

		countdownTypeRadios = new RadioButtonGroup<>();
		countdownTypeRadios.setItems(CountdownType.values());
		countdownTypeRadios.setRenderer(new TextRenderer<CountdownType>(
		        (item) -> getTranslation(CountdownType.class.getSimpleName() + "." + item.name())));

		Locale locale = new Locale("en", "SE"); // ISO 8601 style dates and time
		timePicker.setLocale(locale);
		datePicker.setLocale(locale);
		minutes = new NativeLabel(Translator.translate("minutes"));

		Component countdownButtons = createCountdownButtons(countdownTypeRadios);
		Component interruptionButtons = createInterruptionButtons(interruptionRadios);

		VerticalLayout cd = new VerticalLayout();

		Div title1 = new Div(label("InterruptionType.Title"));
		Div title2 = new Div(label("CountdownType.Title"));
		title2.getElement().setAttribute("style", "margin-top: 1ex");

		cd.add(title1);
		cd.add(interruptionRadios);
		cd.add(interruptionButtons);
		cd.add(new Hr());
		cd.add(title2);
		cd.add(countdownRadios);
		cd.add(countdownTypeRadios);
		cd.add(noCountdown);
		cd.add(waitText);
		cd.add(durationField);
		cd.add(new HorizontalLayout(datePicker, timePicker));
		cd.add(countdownButtons);

		return cd;
	}

	private VerticalLayout createCeremoniesColumn() {
		VerticalLayout ce = new VerticalLayout();

		HorizontalLayout introButtons = new HorizontalLayout();
		startIntroButton = new Button(
		        getTranslation("BreakMgmt.startIntro"), (e) -> {
			        OwlcmsSession.withFop(fop -> {
				        // do nothing if we are already in during introduction
				        if (fop.getCeremonyType() == CeremonyType.INTRODUCTION) {
					        return;
				        }

				        startBreakIfNeeded(fop);
						startIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
						endIntroButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
						startOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
						endOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

				        // we are in a running break such as before snatch already scheduled and want to
				        // switch to intro
				        masterStartCeremony(fop, CeremonyType.INTRODUCTION);

				        // close so we can read the list of participants
				        // parentDialog.close();
			        });
		        });
		startIntroButton.setTabIndex(-1);
		endIntroButton = new Button(
		        getTranslation("BreakMgmt.endIntro"), (e) -> {
			        OwlcmsSession.withFop(fop -> {
						startIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
						endIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
						startOfficials.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
						endOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				        if (fop.getCeremonyType() != CeremonyType.INTRODUCTION) {
					        return;
				        }
				        boolean switchToSnatch = true; // (fop.getBreakTimer().getBreakType() == DURING_INTRODUCTION);
				        // logger.warn("switch to snatch {} {}", fop.getBreakTimer().getBreakType() ,
				        // switchToSnatch);
				        masterEndCeremony(fop, CeremonyType.INTRODUCTION);
				        if (switchToSnatch) {
					        durationField.setValue(DEFAULT_DURATION);
					        setBreakValue(FIRST_SNATCH);
					        countdownTypeRadios.setValue(CountdownType.DURATION);
					        ignoreNextDisable = true;
					        startBreakEnabled();
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
		startOfficials = new Button(
		        getTranslation("BreakMgmt.startOfficials"), (e) -> {
					startIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
					endIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
					startOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
					endOfficials.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			        OwlcmsSession.withFop(fop -> {
				        if (fop.getCeremonyType() == CeremonyType.OFFICIALS_INTRODUCTION) {
					        return;
				        }
				        startBreakIfNeeded(fop);
				        masterStartCeremony(fop, CeremonyType.OFFICIALS_INTRODUCTION);
			        });
		        });
		startOfficials.setTabIndex(-1);
		endOfficials = new Button(
		        getTranslation("BreakMgmt.endOfficials"), (e) -> {
					startIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
					endIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
					startOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
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
		groups.sort((g1, g2) -> {
			int compare = -ObjectUtils.compare(g1.getCompetitionTime(), g2.getCompetitionTime(), true);
			if (compare != 0) {
				return compare;
			}
			compare = -(new NaturalOrderComparator<Group>().compare(g1, g2));
			return compare;
		});
		FieldOfPlay fop2 = OwlcmsSession.getFop();
		GroupCategorySelectionMenu groupCategorySelectionMenu = new GroupCategorySelectionMenu(groups, fop2,
		        // group has been selected
		        (g1, c1, fop1) -> selectCeremonyCategory(g1, c1, fop1),
		        // no group
		        (g1, c1, fop1) -> selectCeremonyCategory(null, c1, fop1));
		Checkbox includeNotCompleted = new Checkbox();
		includeNotCompleted.addValueChangeListener(e -> {
			groupCategorySelectionMenu.setIncludeNotCompleted(e.getValue());
			groupCategorySelectionMenu.recompute();
		});
		includeNotCompleted.setLabel(Translator.translate("Video.includeNotCompleted"));
		HorizontalLayout hl = new HorizontalLayout();
		hl.add(groupCategorySelectionMenu, includeNotCompleted);

		startMedalCeremony = new Button(
		        getTranslation("BreakMgmt.startMedals"), (e) -> {
			        startMedalCeremony.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			        endMedalCeremony.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			        OwlcmsSession.withFop(fop -> {
				        inactive = fop.getState() == INACTIVE;
				        startBreakIfNeeded(fop);
				        Group g = getMedalGroup();
				        Category c = getMedalCategory();
				        fop.fopEventPost(
				                new FOPEvent.CeremonyStarted(CeremonyType.MEDALS, g, c,
				                        this));
				        fop.setVideoGroup(g);
				        fop.setVideoCategory(c);
				        setMedalGroup(g);
				        setMedalCategory(c);
				        logger.info("switching to {} {}", g.getName() != null ? g.getName() : "-",
				                c != null ? c.getTranslatedName() : "");
				        fop.getUiEventBus().post(new UIEvent.VideoRefresh(this, g, c));
			        });

		        });
		startMedalCeremony.setTabIndex(-1);
		endMedalCeremony = new Button(
		        getTranslation("BreakMgmt.endMedals"), (e) -> {
			        OwlcmsSession.withFop(fop -> {
				        endMedalCeremony.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				        fop.fopEventPost(new FOPEvent.CeremonyDone(CeremonyType.MEDALS, this.getOrigin()));
				        if (inactive) {
					        setBreakTimerFromFields();
				        }
			        });
		        });
		endMedalCeremony.setTabIndex(-1);
		startMedalCeremony.getThemeNames().add("secondary contrast");
		endMedalCeremony.getThemeNames().add("secondary contrast");
		medalButtons.add(startMedalCeremony, endMedalCeremony);

		ce.add(label("PublicMsg.Medals"), hl);
		ce.add(medalButtons);

		return ce;
	}

	private Component createCountdownButtons(RadioButtonGroup<CountdownType> countdownTypeRadios2) {
		countdownStart = new Button(Translator.translate("StartCountdown"), new Icon(VaadinIcon.TIMER), (e) -> {
			if (!e.isFromClient()) {
				return;
			}
			OwlcmsSession.withFop(fop -> {
				BreakType value = countdownRadios.getValue();
				if (value != null &&
				        (value.isCountdown()
				                || (countdownTypeRadios.getValue() != CountdownType.INDEFINITE))) {
					// force FOP to accept our break and value as new
					fop.setBreakType(null);
					fop.setCountdownType(null);
					Integer tr = this.computeTimerRemainingFromFields(countdownTypeRadios.getValue());
					if (tr == null) {
						fop.getBreakTimer().setTimeRemaining(0, true);
					} else {
						fop.getBreakTimer().setTimeRemaining(tr, false);
					}
				}
			});
			masterStartBreak();
		});
		countdownStart.getElement().setAttribute("theme", "primary contrast");
		countdownStart.getElement().setAttribute("title", getTranslation("StartCountdown"));
		if (countdownRadios.getValue() != null) {
			countdownStart.setEnabled(false);
		}

		countdownEnd = new Button(getTranslation("EndBreak"), new Icon(VaadinIcon.MICROPHONE), endBreak(parentDialog));
		countdownEnd.getElement().setAttribute("theme", "primary success");
		countdownEnd.getElement().setAttribute("title", getTranslation("EndBreak"));

		HorizontalLayout buttons = new HorizontalLayout();
		buttons.add(countdownStart, countdownEnd);
		buttons.setSpacing(true);
		buttons.setMargin(false);
		buttons.setPadding(false);
		buttons.setJustifyContentMode(JustifyContentMode.AROUND);
		return buttons;
	}

	private Component createInterruptionButtons(RadioButtonGroup<BreakType> interruptionRadios2) {
		stopCompetition = new Button(Translator.translate("StopCompetition"), new Icon(VaadinIcon.EXCLAMATION), (e) -> {
			OwlcmsSession.withFop(fop -> {
				if (!e.isFromClient()) {
					return;
				}
				BreakType value = countdownRadios.getValue();
				if (value != null &&
				        (value.isCountdown()
				                || (countdownTypeRadios.getValue() != CountdownType.INDEFINITE))) {
					// force FOP to accept our break and value as new
					fop.setBreakType(null);
					fop.setCountdownType(null);
					Integer tr = this.computeTimerRemainingFromFields(countdownTypeRadios.getValue());
					if (tr == null) {
						fop.getBreakTimer().setTimeRemaining(0, true);
					} else {
						fop.getBreakTimer().setTimeRemaining(tr, false);
					}
				}
			});
			masterStartBreak();
		});
		stopCompetition.getElement().setAttribute("theme", "primary error");
		stopCompetition.getElement().setAttribute("title", getTranslation("StopCompetition"));
		if (interruptionRadios2.getValue() != null) {
			stopCompetition.setEnabled(true);
		}

//		breakPause = new Button(new Icon(VaadinIcon.PAUSE), (e) -> masterPauseBreak(null));
//		breakPause.getElement().setAttribute("theme", "primary contrast");
//		breakPause.getElement().setAttribute("title", getTranslation("PauseCountdown"));

		resumeCompetition = new Button(getTranslation("ResumeCompetition"), new Icon(VaadinIcon.MICROPHONE),
		        endBreak(parentDialog));
		resumeCompetition.getElement().setAttribute("theme", "primary success");
		resumeCompetition.getElement().setAttribute("title", getTranslation("ResumeCompetition"));

		HorizontalLayout buttons = new HorizontalLayout();
		buttons.add(stopCompetition, resumeCompetition);
		buttons.setSpacing(true);
		buttons.setMargin(false);
		buttons.setPadding(false);
		buttons.setJustifyContentMode(JustifyContentMode.AROUND);
		return buttons;
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
		this.setAlignSelf(Alignment.CENTER, timer);
		timer.getStyle().set("margin-top", "0px");
	}

	private void createUI(Dialog parentDialog) {
		this.parentDialog = parentDialog;

		createBreakColumn();
		if (fop.getState() != FOPState.BREAK) {
			computeDefaultTimeValues();
		} else {
			setDurationFieldsFromBreakTimer(0, null);
			setCountdownTypeValue(getCountdownType());
			setBreakValue(getBreakType());
		}

		assembleDialog(this);
	}

	private void doResetTimer(Integer tr) {
		OwlcmsSession.withFop(fop -> {
			IBreakTimer breakTimer = fop.getBreakTimer();
			if (breakTimer.isRunning()) {
				breakTimer.stop();
				fop.fopEventPost(new FOPEvent.BreakPaused(tr, this.getOrigin()));
			}
		});
		logger.warn("paused; enabling start");
		startBreakEnabled();
	}

	private ComponentEventListener<ClickEvent<Button>> endBreak(Dialog dialog) {
		return (e) -> {
			OwlcmsSession.withFop(fop -> {
				logger.warn("endBreak start lifting");
				fop.fopEventPost(new FOPEvent.StartLifting(this.getOrigin()));
				logger.warn("endbreak enabling start");
				countdownStart.setEnabled(true);
				countdownEnd.setEnabled(false);
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
			// logger.warn("------ created {}",breakTimerElement.id);
		}
		return this.breakTimerElement;
	}

	private BreakType getBreakType() {
		return breakType;
	}

	private CountdownType getCountdownType() {
		// logger.warn("set countdown type {} from {}", countdownType,
		// LoggerUtils.whereFrom());
		return countdownType;
	}

	private Group getMedalGroup() {
		return medalGroup;
	}

	private Object getOrigin() {
		return origin;
	}

	private LocalDateTime getTarget() {
		final LocalDateTime target;
		LocalDate date = datePicker.getValue();
		LocalTime time = timePicker.getValue();
		target = LocalDateTime.of(date, time);
		return target;
	}

	private void initState(Object origin, BreakType brt, CountdownType cdt, Integer countdownSecondsRemaining) {
		logger.warn("initState brt={} cdt={} from {}", brt, cdt, LoggerUtils.whereFrom());
		this.id = IdUtils.getTimeBasedId();
		this.setOrigin(origin);
		this.setBreakType(brt);
		this.setCountdownType(cdt);
	}

	private NativeLabel label(String key) {
		NativeLabel label = new NativeLabel(getTranslation(key));
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

	private void masterEndCeremony(FieldOfPlay fop, CeremonyType ceremonyType) {
		fop.fopEventPost(new FOPEvent.CeremonyDone(ceremonyType, this.getOrigin()));
	}

	private void masterStartBreak() {
		startIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
		endIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
		startMedalCeremony.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
		endMedalCeremony.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
		startOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
		endOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

		OwlcmsSession.withFop(fop -> {
			masterStartBreak(fop, true);
		});
		// e.getSource().setEnabled(false);
		logger.warn("start break disable start");
		startBreakDisabled();
		return;
	}

	private void masterStartBreak(FieldOfPlay fop, boolean force) {
		BreakType breakType = countdownRadios.getValue();
		if (breakType == null) {
			breakType = interruptionRadios.getValue();
		}
		if (breakType == null) {
			throw new RuntimeException("no break type selected");
		}
		// logger.warn("****** masterStartBreak {}", breakType);
		CountdownType countdownType = countdownTypeRadios.getValue();
		masterStartBreak(fop, breakType, countdownType, force);
	}

	private void masterStartBreak(FieldOfPlay fop, BreakType breakType, CountdownType countdownType, boolean force) {
		// logger.trace("masterStartBreak timeRemaining {} breakType {} indefinite {}
		// isRunning {}", timeRemaining,
		// breakType, fop.getBreakTimer().isIndefinite(),
		// fop.getBreakTimer().isRunning());
		if (timeRemaining == null && fop.getBreakTimer() != null) {
			timeRemaining = (long) fop.getBreakTimer().liveTimeRemaining();
		}
		fop.fopEventPost(new FOPEvent.BreakStarted(breakType, countdownType,
		        countdownType == CountdownType.INDEFINITE ? null : timeRemaining.intValue(), getTarget(),
		        countdownType == CountdownType.INDEFINITE,
		        this.getOrigin()));
	}

	private void masterStartCeremony(FieldOfPlay fop, CeremonyType ceremonyType) {
		fop.fopEventPost(
		        new FOPEvent.CeremonyStarted(ceremonyType, fop.getGroup(), null, this));
	}

	private void selectCeremonyCategory(Group g, Category c, FieldOfPlay fop) {
		endMedalCeremony.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
		startMedalCeremony.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		setMedalGroup(g);
		setMedalCategory(c);
	}

	private void setBreakTimerFromFields() {
		// logger.warn("setBreakTimerFromFields from={}", LoggerUtils.whereFrom());
		LocalDateTime now = LocalDateTime.now();

		CountdownType cType = countdownTypeRadios.getValue();
		BreakType bType = countdownRadios.getValue();
		if (bType != null && bType.isInterruption()) {
			fop.getBreakTimer().setIndefinite();
		}
		if (cType == null && bType != null) {
			cType = mapBreakTypeToDurationValue(bType);
			countdownTypeRadios.setValue(cType);
		}
		final CountdownType curCType = cType;
		OwlcmsSession.withFop(fop -> {
			if (curCType == CountdownType.TARGET) {
				LocalDateTime target = getTarget();
				timeRemaining = now.until(target, MILLIS);
				logger.warn("setBreakTimerFromFields target-derived duration {}", formattedDuration(timeRemaining));
				fop.getBreakTimer().setTimeRemaining(timeRemaining.intValue(), false);
				fop.getBreakTimer().setBreakDuration(timeRemaining.intValue());
				fop.getBreakTimer().setEnd(null);
//				breakTimerElement.slaveBreakSet(
//				        new BreakSetTime(bType, curCType, timeRemaining.intValue(), target, false, this.getOrigin(),
//				                LoggerUtils.stackTrace()));
			} else if (curCType == CountdownType.INDEFINITE) {
				logger.warn("setBreakTimerFromFields indefinite");
				fop.getBreakTimer().setIndefinite(); // CHECK
				timeRemaining = null;
//				breakTimerElement.slaveBreakSet(
//				        new BreakSetTime(bType, curCType, 0, null, true, this, LoggerUtils.stackTrace()));
			} else {
				Duration value;
				value = durationField.getValue();
				value = (value == null ? DEFAULT_DURATION : value);
				timeRemaining = (value != null ? value.toMillis() : 0L);
				fop.getBreakTimer().setTimeRemaining(timeRemaining.intValue(), false);
				fop.getBreakTimer().setBreakDuration(timeRemaining.intValue());
				fop.getBreakTimer().setEnd(null);
				logger.warn("setBreakTimerFromFields explicit duration {}",
				        formattedDuration(timeRemaining));
//				breakTimerElement.slaveBreakSet(
//				        new BreakSetTime(bType, curCType, timeRemaining.intValue(), null, false, this.getOrigin(),
//				                LoggerUtils.stackTrace()));
			}
		});
		return;
	}

	private void setBreakType(BreakType breakType) {
		// logger.warn("set break type {} from {}", breakType,
		// LoggerUtils.whereFrom());
		this.breakType = breakType;
	}

	private void setBreakValue(BreakType breakType) {
		logger.warn("setBreakValue {} from {}", breakType, LoggerUtils.whereFrom());
		countdownRadios.setValue(countdowns.contains(breakType) ? breakType : null);
		interruptionRadios.setValue(interruptions.contains(breakType) ? breakType : null);

		logger.warn("set countdown radio value {} from {}",countdownRadios.getValue(), LoggerUtils.whereFrom());
		logger.warn("set interruption radio value {} from {}", interruptionRadios.getValue(), LoggerUtils.whereFrom());
		if (countdownRadios.getValue() == null) {
			setCountdownTypeValue(null);
			countdownStart.setEnabled(false);
			countdownEnd.setEnabled(false);
		} else {
			countdownStart.setEnabled(fop.getState() == FOPState.BREAK ? false : true);
			countdownEnd.setEnabled(!countdownStart.isEnabled());
		}

		if (interruptionRadios.getValue() == null) {
			stopCompetition.setEnabled(false);
			resumeCompetition.setEnabled(false);
		} else {
			stopCompetition.setEnabled(fop.getState() == FOPState.BREAK ? false : true);
			resumeCompetition.setEnabled(!stopCompetition.isEnabled());
		}

	}

	private void setCountdownFieldVisibility(CountdownType cType) {
		logger.warn("setCountdownFieldVisibility {} from {}", cType, LoggerUtils.whereFrom());
		if (cType == CountdownType.DURATION) {
			switchToDuration();
		} else if (cType == CountdownType.TARGET) {
			switchToTarget();
		} else if (cType == CountdownType.INDEFINITE) {
			switchToIndefinite();
		} else {
			switchToNoCountdown();
		}
	}

	private void setCountdownType(CountdownType countdownType) {
		this.countdownType = countdownType;
	}

	private void setCountdownTypeValue(CountdownType ct2) {
		logger.warn("setting countdownTypeRadios {} from {}", ct2, LoggerUtils.whereFrom());
		countdownTypeRadios.setValue(countdownRadios.getValue() != null ? ct2 : null);
		setCountdownFieldVisibility(ct2);
		setCountdownType(ct2);
		setButtonHighlight(getBreakType(),ct2);
	}

	private void setButtonHighlight(BreakType breakType2, CountdownType ct2) {
		if (breakType2 == BreakType.FIRST_SNATCH) {
			if (fop.getCeremonyType() == CeremonyType.INTRODUCTION) {
				startIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				endIntroButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
				startOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				endOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			} else if (fop.getCeremonyType() == CeremonyType.OFFICIALS_INTRODUCTION) {
				startIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				endIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				startOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				endOfficials.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			}
		}
	}

	private void setDurationField(Duration duration) {
		logger.warn("{} {}", duration, LoggerUtils.whereFrom());
		durationField.setValue(duration);
	}

	private void setDurationFieldsFromBreakTimer(int targetTimeDuration, Integer breakDuration) {
		logger.warn("setDurationFieldsFromBreakTimer target={} duration={}", targetTimeDuration, breakDuration);
		setDurationField(breakDuration != null ? Duration.ofMillis(breakDuration) : DEFAULT_DURATION);
		LocalDateTime target = LocalDateTime.now().plus(targetTimeDuration, MILLIS);
		datePicker.setValue(target.toLocalDate());
		timePicker.setValue(target.toLocalTime());
		CountdownType value = countdownTypeRadios.getValue();
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

	@Subscribe
	private void slaveBreakDone(UIEvent.BreakDone e) {
		synchronized (this) {
			// logger.warn("Break Done {}", LoggerUtils. stackTrace());
			UIEventProcessor.uiAccess(this, uiEventBus, e,
			        () -> parentDialog.close());
		}
	}

	@Subscribe
	private void slaveBreakPause(UIEvent.BreakPaused e) {
		synchronized (this) {
			UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
				startBreakEnabled();
			});
		}
	}

	@Subscribe
	private void slaveBreakSet(UIEvent.BreakSetTime e) {
		// do nothing
	}

	@Subscribe
	private void slaveBreakStart(UIEvent.BreakStarted e) {
		synchronized (this) {
			if (e.isDisplayToggle()) {
				return;
			}
			UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
				if (!e.getPaused()) {
					startBreakDisabled();
				} else {
					startBreakEnabled();
				}
				setBreakValue(e.getBreakType());
			});
		}

	}

	private void startBreakDisabled() {
		if (ignoreNextDisable) {
			ignoreNextDisable = false;
			return;
		}
		if (countdownRadios.getValue() == null) {
			countdownStart.setEnabled(false);
			countdownEnd.setEnabled(false);
		} else {
			// logger.trace("start countdown disabled {}", LoggerUtils.whereFrom(1));
			countdownStart.setEnabled(false);
			countdownEnd.setEnabled(true);
		}
	}

	private void startBreakEnabled() {
		// logger.trace("start countdown enabled {}", LoggerUtils.whereFrom());
		if (countdownRadios.getValue() == null) {
			countdownStart.setEnabled(false);
			countdownEnd.setEnabled(false);
		} else {
			countdownStart.setEnabled(fop.getState() != FOPState.BREAK);
			countdownEnd.setEnabled(!countdownStart.isEnabled());
		}
	}

	private void startBreakIfNeeded(FieldOfPlay fop) {
		// don't start a break if already in a break
		if (fop.getState() == FOPState.INACTIVE || fop.getState() != FOPState.BREAK) {
			fop.getBreakTimer().setIndefinite();
			fop.setWeightAtLastStart(0);
			fop.fopEventPost(new FOPEvent.BreakStarted(BreakType.FIRST_SNATCH, CountdownType.INDEFINITE,
			        null, null, true,
			        this.getOrigin()));
		}
	}

	private void switchToDuration() {
		noCountdown.setVisible(false);
		waitText.setVisible(false);
		durationField.setVisible(true);
		minutes.setVisible(true);
		datePicker.setVisible(false);
		timePicker.setVisible(false);
		durationField.focus();
		durationField.setAutoselect(true);
		startBreakEnabled();
		setBreakTimerFromFields();
	}

	private void switchToIndefinite() {
		noCountdown.setVisible(false);
		waitText.setVisible(true);
		durationField.setVisible(false);
		minutes.setVisible(false);
		datePicker.setVisible(false);
		timePicker.setVisible(false);
		startBreakEnabled();
		setBreakTimerFromFields();
	}

	private void switchToNoCountdown() {
		// logger.warn("switchToNoCountdown from {}", LoggerUtils.whereFrom());
		noCountdown.setVisible(true);
		waitText.setVisible(false);
		durationField.setVisible(false);
		minutes.setVisible(true);
		datePicker.setVisible(false);
		timePicker.setVisible(false);
		startBreakDisabled();
		setBreakTimerFromFields();
	}

	private void switchToTarget() {
		noCountdown.setVisible(false);
		waitText.setVisible(false);
		durationField.setVisible(false);
		minutes.setVisible(false);
		datePicker.setVisible(true);
		timePicker.setVisible(true);
		timePicker.focus();
		startBreakEnabled();
		setBreakTimerFromFields();
	}

	/**
	 * Set values based on current state of Field of Play.
	 *
	 * @return true if a break timer is running and not paused.
	 */
	/**
	 * @return
	 */
	private void syncWithFop() {
		final boolean[] running = new boolean[1]; // wrapper to allow value to be set from lambda
		OwlcmsSession.withFop(fop -> {
			FOPState fopState = fop.getState();
			IBreakTimer fopBreakTimer = fop.getBreakTimer();
			int fopLiveTimeRemaining = fopBreakTimer.liveTimeRemaining();
			Integer fopBreakDuration = fopBreakTimer.getBreakDuration();

			logger.warn("syncWithFop {} {} {}", fopState, fop.getBreakTimer().liveTimeRemaining(),fop.getBreakTimer().isRunning());

			running[0] = false;

			switch (fopState) {
			case BREAK:
				CountdownType ct = fop.getCountdownType();
				setCountdownType(ct);
				setBreakType(fop.getBreakType());

				if (ct == CountdownType.INDEFINITE) {
					fopLiveTimeRemaining = (int) DEFAULT_DURATION.toMillis();
				}

				// override from FOP
				setDurationFieldsFromBreakTimer(fopLiveTimeRemaining, fopBreakDuration);

				if (fopState == INACTIVE) {
					setBreakType(BEFORE_INTRODUCTION);
				}
				if (ct != null) setCountdownTypeValue(ct);
				setBreakValue(getBreakType());
				if (ct == null) {
					logger.warn("setting to ***** {}",breakType);
					setCountdownTypeValue(mapBreakTypeToDurationValue(breakType));
				}

				breakTimerElement.syncWithFopBreakTimer();
				break;
			default:
				break;
			}

		});
	}

}
