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
import app.owlcms.uievents.UIEvent.BreakSetTime;
import app.owlcms.utils.IdUtils;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.NaturalOrderComparator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class BreakManagement extends VerticalLayout implements SafeEventBusRegistration {
	private static final Duration DEFAULT_DURATION = Duration.ofMinutes(10L);

//    private Button athleteButton;
	private Button breakEnd = null;

	private Button breakPause = null;
	private Button breakStart = null;
	private BreakTimerElement breakTimerElement;

//    private Button countdownButton;
	private RadioButtonGroup<BreakType> countdownRadios;
	private DatePicker datePicker = new DatePicker();

	// private HorizontalLayout dt;
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
	private boolean inactive = false;
	final private Logger logger = (Logger) LoggerFactory.getLogger(BreakManagement.class);

	private Category medalCategory;
	private Group medalGroup;

	private NativeLabel minutes;

	private Object origin;

	private Dialog parentDialog;
	private BreakType requestedBreakType;

	private Button startIntroButton;

	private Button startMedalCeremony;

	private Button startOfficials;

	private TimePicker timePicker = new TimePicker();

	private HorizontalLayout timer;

	private Long timeRemaining = null;

	private EventBus uiEventBus;

	/**
	 * Persona-specific display (e.g. for the jury, the technical controller, etc.)
	 *
	 * @param origin
	 * @param brt
	 * @param cdt
	 */
	BreakManagement(Object origin, BreakType brt, CountdownType cdt, Dialog parentDialog) {
		setPadding(false);
		setMargin(false);
		this.setSizeFull();
		// logger.setLevel(Level.DEBUG);
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
		return false;
//                (getRequestedBreakType() != null
//                && (getRequestedBreakType() == JURY
//                || getRequestedBreakType() == TECHNICAL
//                || getRequestedBreakType() == MARSHAL
//                ));
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
		if (countdownType == CountdownType.INDEFINITE) {
			tr = null;
		} else if (countdownType == CountdownType.TARGET) {
			// recompute duration, in case there was a pause.
			setBreakTimerFromFields(CountdownType.TARGET);
			tr = timeRemaining != null ? timeRemaining.intValue() : null;
		} else {
			setBreakTimerFromFields(CountdownType.DURATION);
			tr = timeRemaining != null ? timeRemaining.intValue() : null;
		}
		return tr;
	}

	private Component createBreakTimerButtons() {
		breakStart = new Button(new Icon(VaadinIcon.PLAY), (e) -> {
			OwlcmsSession.withFop(fop -> {
				BreakType value = countdownRadios.getValue();
				if (value != null &&
				        (value.isCountdown()
				                || (durationRadios.getValue() != CountdownType.INDEFINITE))) {
					// force FOP to accept our break and value as new
					fop.setBreakType(null);
					fop.setCountdownType(null);
					Integer tr = this.computeTimerRemainingFromFields(durationRadios.getValue());
					if (tr == null) {
						fop.getBreakTimer().setTimeRemaining(0, true);
					} else {
						fop.getBreakTimer().setTimeRemaining(tr, false);
					}
				}
			});
			masterStartBreak();
		});
		breakStart.getElement().setAttribute("theme", "primary contrast");
		breakStart.getElement().setAttribute("title", getTranslation("StartCountdown"));

		breakPause = new Button(new Icon(VaadinIcon.PAUSE), (e) -> masterPauseBreak(null));
		breakPause.getElement().setAttribute("theme", "primary contrast");
		breakPause.getElement().setAttribute("title", getTranslation("PauseCountdown"));

		breakEnd = new Button(getTranslation("EndBreak"), new Icon(VaadinIcon.MICROPHONE), endBreak(parentDialog));
		breakEnd.getElement().setAttribute("theme", "primary success");
		breakEnd.getElement().setAttribute("title", getTranslation("EndBreak"));

		HorizontalLayout buttons = new HorizontalLayout();
		buttons.add(breakStart, breakPause, /* breakReset, */ breakEnd);
		buttons.setSpacing(true);
		buttons.setMargin(false);
		buttons.setPadding(false);
		buttons.setJustifyContentMode(JustifyContentMode.AROUND);
		return buttons;
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
				        endIntroButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

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
				        endIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				        if (fop.getCeremonyType() != CeremonyType.INTRODUCTION) {
					        return;
				        }
				        boolean switchToSnatch = true; // (fop.getBreakTimer().getBreakType() == DURING_INTRODUCTION);
				        // logger.debug("switch to snatch {} {}", fop.getBreakTimer().getBreakType() ,
				        // switchToSnatch);
				        masterEndCeremony(fop, CeremonyType.INTRODUCTION);
				        if (switchToSnatch) {
					        durationField.setValue(DEFAULT_DURATION);
					        setCountdownValue(FIRST_SNATCH);
					        durationRadios.setValue(CountdownType.DURATION);
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
		startOfficials = new Button(
		        getTranslation("BreakMgmt.startOfficials"), (e) -> {
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
		groups.sort((g1,g2) -> {
			int compare = -ObjectUtils.compare(g1.getCompetitionTime(), g2.getCompetitionTime(), true);
			if (compare != 0) return compare;
			compare = -(new NaturalOrderComparator<Group>().compare(g1,g2));
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
			    		logger.info("switching to {} {}", g.getName() != null ? g.getName() :"-", c != null ? c.getTranslatedName() : "");
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
					        setBreakTimerFromFields(CountdownType.TARGET);
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

	@SuppressWarnings("deprecation")
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
		//FIXME countdownRadios.prependComponents(TECHNICAL, div);

		Hr hr = new Hr();
		hr.getElement().setAttribute("style", "margin-top: 1ex");
		Div div1 = new Div(label("CountdownType.Title"));
		div1.getElement().setAttribute("style", "margin-top: 2ex; margin-bottom: 1ex");
		//FIXME countdownRadios.prependComponents(BEFORE_INTRODUCTION, hr, div1);

		durationRadios = new RadioButtonGroup<>();
		durationRadios.setItems(CountdownType.values());
		durationRadios.setRenderer(new TextRenderer<CountdownType>(
		        (item) -> getTranslation(CountdownType.class.getSimpleName() + "." + item.name())));
		//FIXME durationRadios.prependComponents(CountdownType.INDEFINITE, new Paragraph(""));
		//DIXME durationRadios.prependComponents(CountdownType.TARGET, new Paragraph(""));

		Locale locale = new Locale("en", "SE"); // ISO 8601 style dates and time
		timePicker.setLocale(locale);
		datePicker.setLocale(locale);
		minutes = new NativeLabel(Translator.translate("minutes"));

		//DIXME durationRadios.addComponents(CountdownType.DURATION, durationField, new NativeLabel(" "), minutes, new Div());
		//FIXME durationRadios.addComponents(CountdownType.TARGET, datePicker, new NativeLabel(" "), timePicker);

		createTimerDisplay();
		Component timerButtons = createBreakTimerButtons();

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

	private Group getMedalGroup() {
		return medalGroup;
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
		// logger.debug("init brt={} cdt={} from {}", brt, cdt,
		// LoggerUtils.whereFrom());
		this.id = IdUtils.getTimeBasedId();
		ignoreBreakTypeValueChange = false;
		this.setOrigin(origin);
		this.parentDialog = parentDialog;

		createCountdownColumn();
		// createAttemptBoardInfoSelection();
		computeDefaultTimeValues();

		setCountdownValue(brt);
		setDurationValue(cdt);
		assembleDialog(this);
		OwlcmsSession.withFop(fop -> {
			uiEventBusRegister((Component) origin, fop);
		});
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

	private void masterPauseBreak(BreakType bType) {
		OwlcmsSession.withFop(fop -> {
			IBreakTimer breakTimer = fop.getBreakTimer();
			if (breakTimer.isRunning()) {
				// logger.debug("pausing current break {} due to {}", fop.getBreakType(),
				// bType);
				breakTimer.stop();
				fop.fopEventPost(
				        new FOPEvent.BreakPaused(breakTimer.getTimeRemainingAtLastStop(), this.getOrigin()));

			}
		});
		logger.debug("paused; enabling start");
		startEnabled();
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
		// logger.trace("setBreakTimerFromFields curCType={} from={}", cType,
		// LoggerUtils.whereFrom());
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
		// logger.debug("set countdown radio value {} {}", breakType,
		// LoggerUtils.whereFrom());
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
		// logger.debug("requestedBreakType={} {}", requestedBreakType,
		// LoggerUtils.stackTrace());
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
					// logger.trace(" syncWithFOP: break under way {} {} indefinite={}",
					// fopBreakType,
					// fopCountdownType,fopBreakTimer.isIndefinite());

					if (fopCountdownType == CountdownType.INDEFINITE) {
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

	/**
	 * Everything has been created and has meaningful values, add value change
	 * listeners now to avoid spuriuous triggering during interface build-up.
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
				// startIndefiniteBreakImmediately(bType);
				setBreakTimerFromFields(durationRadios.getValue());
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
				// logger.debug("not in a break {} {}", fop.getState(),
				// getRequestedBreakType());
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

}
