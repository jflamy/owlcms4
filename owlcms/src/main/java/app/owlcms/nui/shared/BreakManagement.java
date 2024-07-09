/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
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
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.function.SerializableSupplier;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.components.GroupCategorySelectionMenu;
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
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.NaturalOrderComparator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class BreakManagement extends BaseContent implements SafeEventBusRegistration {

	private static final Duration DEFAULT_DURATION = Duration.ofMinutes(10L);
	final private Logger logger = (Logger) LoggerFactory.getLogger(BreakManagement.class);
	private Button endIntroButton;
	private Button endMedalCeremony;
	private Button endOfficials;
	private Button endInterruption = null;
	private Button startIntroButton;
	private Button startMedalCeremony;
	private Button startOfficials;
	private Button stopCompetition = null;
	private Button endCountdown = null;
	private Button startCountdown = null;
	private BreakType breakType;
	private RadioButtonGroup<BreakType> countdownRadios;
	private List<BreakType> countdowns;
	private CountdownType countdownType;
	private RadioButtonGroup<CountdownType> countdownTypeRadios;
	private DatePicker datePicker = new DatePicker();
	private DurationField durationField = new DurationField();
	private FieldOfPlay fop;
	private boolean inactive = false;
	private RadioButtonGroup<BreakType> interruptionRadios;
	private List<BreakType> interruptions;
	private Category medalCategory;
	private Group medalGroup;
	private NativeLabel minutes;
	private Paragraph noCountdown = new Paragraph();
	private Object origin;
	private Dialog parentDialog;
	private TimePicker timePicker = new TimePicker();
	private Long timeRemaining = null;
	private EventBus uiEventBus;
	private Paragraph waitText = new Paragraph(Translator.translate("Wait.Text"));
	// private Paragraph countdownActiveError = new Paragraph(
	// Translator.translate("BreakManagement.countdownActiveError"));
	// private Paragraph interruptionActiveError = new Paragraph(
	// Translator.translate("BreakManagement.interruptionActiveError"));
	private Paragraph countdownSelectionRequired = new Paragraph(
	        Translator.translate("BreakManagement.needCountdownSelection"));
	private Paragraph interruptionSelectionRequired = new Paragraph(
	        Translator.translate("BreakManagement.needInterruptionSelection"));
	private VerticalLayout countdownTypeLayout;
	private BreakType requestedBreak;
	private CountdownType requestedCountdownType;

	/**
	 * Set the break according to the requester (announcer/marshall typically)
	 *
	 * @param fop
	 * @param requestedBreak
	 * @param requestedCountdownType
	 * @param countdownSecondsRemaining
	 * @param parentDialog
	 * @param origin
	 */
	BreakManagement(FieldOfPlay fop, BreakType requestedBreak, CountdownType requestedCountdownType,
	        Integer countdownSecondsRemaining, Dialog parentDialog, Object origin) {
		logger.setLevel(Level.DEBUG);
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("BreakManagement request {} {} {} {}", requestedBreak, requestedCountdownType,
			        fop.getState(),
			        fop.getBreakType());
		}
		setPadding(false);
		setMargin(false);
		this.setSizeFull();
		this.fop = OwlcmsSession.getFop();
		if (fop.getState() == FOPState.INACTIVE || fop.getBreakType() == BreakType.GROUP_DONE) {
			this.requestedBreak = BreakType.BEFORE_INTRODUCTION;
			this.requestedCountdownType = CountdownType.TARGET;
		} else if (fop.getState() == FOPState.BREAK) {
			this.requestedBreak = null;
			this.requestedCountdownType = null;
			initFromFOP(parentDialog);
			return;
		} else if (requestedBreak == null && fop.getState() != FOPState.BREAK) {
			this.requestedBreak = BreakType.TECHNICAL;
			this.requestedCountdownType = CountdownType.INDEFINITE;
		} else {
			this.requestedBreak = requestedBreak;
			this.requestedCountdownType = requestedCountdownType;
		}
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("request {}", this.requestedBreak);
		}
		this.setBreakType(this.requestedBreak);
		this.setCountdownType(this.requestedCountdownType);

		initState(origin, this.requestedBreak, this.requestedCountdownType);
		createUI(parentDialog);
		setEnablement();
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("request done {}", this.requestedBreak);
		}
	}

	/**
	 * Set the break according to field of play.
	 *
	 * @param origin the origin
	 */
	BreakManagement(FieldOfPlay fop, Dialog parentDialog, Object origin) {
		logger.setLevel(Level.DEBUG);
		this.requestedBreak = null;
		this.requestedCountdownType = null;
		initFromFOP(parentDialog);
	}

	private Category getMedalCategory() {
		return this.medalCategory;
	}

	/**
	 * Everything has been created and has meaningful values, add value change listeners now to avoid spuriuous triggering during interface build-up.
	 *
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		OwlcmsSession.withFop(fop -> {
			// we listen on uiEventBus.
			this.uiEventBus = uiEventBusRegister(this, fop);
		});

		addListeners();
	}

	private void addListeners() {
		this.countdownTypeRadios.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			CountdownType cType = e.getValue();
			setCountdownFieldVisibility(cType);
			this.startCountdown.setEnabled(true);
		});
		this.interruptionRadios.addValueChangeListener(event -> {
			if (!event.isFromClient()) {
				return;
			}
			setBreakValue(event.getValue());
			setBreakType(event.getValue());
		});
		this.countdownRadios.addValueChangeListener(event -> {
			if (!event.isFromClient()) {
				return;
			}
			// logger.debug("bt new value {} {}", event.getValue(), LoggerUtils.whereFrom());

			BreakType bType = event.getValue();
			if (bType == BreakType.BEFORE_INTRODUCTION || bType == BreakType.CEREMONY) {
				computeDefaultTimeValues();
			}
			CountdownType mapBreakTypeToCountdownType = mapBreakTypeToDurationValue(bType);
			// logger.debug("setting countdown {} ignored={}", mapBreakTypeToCountdownType);
			setCountdownTypeValue(mapBreakTypeToCountdownType);
			setBreakValue(bType);
			setBreakType(bType);
			this.startCountdown.setEnabled(true);
		});
		this.durationField.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			// computeTimerRemainingFromFields(CountdownType.DURATION);
			// doResetTimer(this.timeRemaining.intValue());
		});
		this.timePicker.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			// computeTimerRemainingFromFields(CountdownType.TARGET);
			// doResetTimer(this.timeRemaining.intValue());
		});
		this.datePicker.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			// computeTimerRemainingFromFields(CountdownType.TARGET);
			// doResetTimer(this.timeRemaining.intValue());
		});
	}

	public class LazyComponent extends Div {
		public LazyComponent(
		        SerializableSupplier<? extends Component> supplier) {
			addAttachListener(e -> {
				if (getElement().getChildCount() == 0) {
					add(supplier.get());
				}
			});
		}
	}

	private void assembleDialog(VerticalLayout dialogLayout) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("assembleDialog {} {}", this.requestedBreak, LoggerUtils.whereFrom());
			// createTimerDisplay();
			// dialogLayout.add(timer);
		}

		VerticalLayout ci = createInterruptionColumn();
		VerticalLayout cb = createCountdownColumn();


		TabSheet ts = new TabSheet();
		Tab iTab = ts.add(Translator.translate("BreakManagement.InterruptionsAndJury"), ci);
		Tab bTab = ts.add(Translator.translate("BreakManagement.BreaksAndCeremonies"), 
				new LazyComponent(
		            () -> {
		            	// createCeremoniesColumn is expensive due to medals
		            	VerticalLayout cc = createCeremoniesColumn();
		            	HorizontalLayout bc = new HorizontalLayout(cb, cc);
		            	bc.setSizeFull();
		            	bc.setJustifyContentMode(JustifyContentMode.EVENLY);
		            	return bc;
		            }));
		
		ts.addSelectedChangeListener((e) -> {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("selectedTab {} requestedBreak={}", e.getSelectedTab().getLabel(),
				        this.requestedBreak);
			}
			if (this.requestedBreak != null) {
				setBreakValue(this.requestedBreak);
				computeDefaultTimeValues();
				// if we entered as an interruption, the switch to a break would be for a duration
				// (e.g. broken platform, fixed, give athletes a warm-up duration before resuming).
				setCountdownTypeValue(
					this.requestedBreak.isInterruption() ? CountdownType.DURATION : this.requestedCountdownType);
				setEnablement();
			} else {
				syncWithFop();
			}
		});
		this.setSizeFull();
		ts.setSizeFull();

		if (this.requestedBreak != null
		        || this.fop.getState() == FOPState.INACTIVE
		        || (this.fop.getState() == FOPState.BREAK
		                && (this.fop.getBreakType().isCountdown()
		                        || this.fop.getBreakType() == BreakType.GROUP_DONE))) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("selecting tab");
			}
			ts.setSelectedTab(getBreakType().isInterruption() ? iTab : bTab);
		}

		dialogLayout.add(ts);
	}

	private void computeDefaultTimeValues() {
		logger.debug("setting default duration as default {}", LoggerUtils.whereFrom());
		setDurationField(DEFAULT_DURATION);

		if (fop.getGroup() != null
		        && fop.getGroup().getCompetitionTime() != null
		        && fop.getGroup().getCompetitionTime().isAfter(LocalDateTime.now())) {
			this.datePicker.setValue(fop.getGroup().getCompetitionTime().toLocalDate());
			this.timePicker.setValue(fop.getGroup().getCompetitionTime().toLocalTime());
			return;
		}

		int timeStep = 30;
		this.timePicker.setStep(Duration.ofMinutes(timeStep));
		LocalTime nowTime = LocalTime.now();
		int nowMin = nowTime.getMinute();
		int nowHr = nowTime.getHour();
		int previousStepMin = (nowMin / timeStep) * timeStep; // between 0 and 50
		int nextStepMin = (previousStepMin + timeStep) % 60;
		this.logger.trace("previousStepMin = {} nextStepMin = {}", previousStepMin, nextStepMin);
		int nextHr = (nextStepMin == 0 ? nowHr + 1 : nowHr);
		LocalDate nextDate = LocalDate.now();
		if (nextHr >= 24) {
			nextDate.plusDays(1);
			nextHr = nextHr % 24;
		}
		this.datePicker.setValue(nextDate);
		this.datePicker.setWidth("16ch");
		this.timePicker.setValue(LocalTime.of(nextHr, nextStepMin));
		this.timePicker.setWidth("11ch");
	}

	private Integer computeTimerRemainingFromFields(boolean interruption, CountdownType countdownType) {
		// logger.debug("computeTimerRemainingFromFields");
		Integer tr;
		if (interruption || countdownType == CountdownType.INDEFINITE) {
			tr = null;
		} else if (countdownType == CountdownType.TARGET) {
			// recompute duration, in case there was a pause.
			setBreakTimerFromFields(false);
			tr = this.timeRemaining != null ? this.timeRemaining.intValue() : null;
		} else {
			setBreakTimerFromFields(false);
			tr = this.timeRemaining != null ? this.timeRemaining.intValue() : null;
		}
		return tr;
	}

	private VerticalLayout createCeremoniesColumn() {
		VerticalLayout ce = new VerticalLayout();

		HorizontalLayout introButtons = new HorizontalLayout();
		this.startIntroButton = new Button(
		        Translator.translate("BreakMgmt.startIntro"), (e) -> {
			        OwlcmsSession.withFop(fop -> {
				        // do nothing if we are already in during introduction
				        if (fop.getCeremonyType() == CeremonyType.INTRODUCTION) {
					        return;
				        }

				        startBreakIfNeeded(fop);
				        this.startIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				        this.endIntroButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
				        this.startOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				        this.endOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

				        masterStartCeremony(fop, CeremonyType.INTRODUCTION);

				        // close so we can read the list of participants
				        // parentDialog.close();
			        });
		        });
		this.startIntroButton.setTabIndex(-1);
		this.endIntroButton = new Button(
		        Translator.translate("BreakMgmt.endIntro"), (e) -> {
			        OwlcmsSession.withFop(fop -> {
				        this.startIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				        this.endIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				        this.startOfficials.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
				        this.endOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				        if (fop.getCeremonyType() != CeremonyType.INTRODUCTION) {
					        return;
				        }
				        masterEndCeremony(fop, CeremonyType.INTRODUCTION);

				        boolean switchToSnatch = true;
				        if (switchToSnatch) {
					        this.durationField.setValue(DEFAULT_DURATION);
					        setBreakValue(BreakType.FIRST_SNATCH);
					        this.countdownTypeRadios.setValue(CountdownType.DURATION);
					        setEnablement();
					        this.startCountdown.setEnabled(true);
				        }
			        });
		        });
		this.endIntroButton.setTabIndex(-1);
		this.startIntroButton.getThemeNames().add("secondary contrast");
		this.endIntroButton.getThemeNames().add("secondary contrast");

		introButtons.add(this.startIntroButton, this.endIntroButton);

		ce.add(label("BreakMgmt.IntroductionOfAthletes"));
		ce.add(introButtons);

		HorizontalLayout officialsButtons = new HorizontalLayout();
		this.startOfficials = new Button(
		        Translator.translate("BreakMgmt.startOfficials"), (e) -> {
			        this.startIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			        this.endIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			        this.startOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			        this.endOfficials.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			        OwlcmsSession.withFop(fop -> {
				        if (fop.getCeremonyType() == CeremonyType.OFFICIALS_INTRODUCTION) {
					        return;
				        }
				        startBreakIfNeeded(fop);
				        masterStartCeremony(fop, CeremonyType.OFFICIALS_INTRODUCTION);
			        });
		        });
		this.startOfficials.setTabIndex(-1);
		this.endOfficials = new Button(
		        Translator.translate("BreakMgmt.endOfficials"), (e) -> {
			        this.startIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			        this.endIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			        this.startOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			        this.endOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			        OwlcmsSession.withFop(fop -> {
				        if (fop.getCeremonyType() != CeremonyType.OFFICIALS_INTRODUCTION) {
					        return;
				        }
				        masterEndCeremony(fop, CeremonyType.OFFICIALS_INTRODUCTION);
			        });
			        this.startCountdown.setEnabled(true);
		        });
		this.endOfficials.setTabIndex(-1);
		this.startOfficials.getThemeNames().add("secondary contrast");
		this.endOfficials.getThemeNames().add("secondary contrast");
		officialsButtons.add(this.startOfficials, this.endOfficials);

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
		        (g1, c1, fop1) -> selectCeremonyCategory(g1, c1),
		        // no group
		        (g1, c1, fop1) -> selectCeremonyCategory(null, c1));
		Checkbox includeNotCompleted = new Checkbox();
		includeNotCompleted.addValueChangeListener(e -> {
			groupCategorySelectionMenu.setIncludeNotCompleted(e.getValue());
			groupCategorySelectionMenu.recompute();
		});
		includeNotCompleted.setLabel(Translator.translate("Video.includeNotCompleted"));
		HorizontalLayout hl = new HorizontalLayout();
		hl.add(groupCategorySelectionMenu, includeNotCompleted);

		this.startMedalCeremony = new Button(
		        Translator.translate("BreakMgmt.startMedals"), (e) -> {
			        this.startMedalCeremony.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			        this.endMedalCeremony.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			        OwlcmsSession.withFop(fop -> {
				        this.inactive = fop.getState() == FOPState.INACTIVE;
				        startBreakIfNeeded(fop);
				        Group g = getMedalGroup();
				        Category c = getMedalCategory();
				        if (g != null) {
					        fop.fopEventPost(
					                new FOPEvent.CeremonyStarted(CeremonyType.MEDALS, g, c,
					                        this));
					        setMedalGroup(g);
					        setMedalCategory(c);
					        this.logger.info("switching to {} {}", g.getName() != null ? g.getName() : "-",
					                c != null ? c.getNameWithAgeGroup() : "");
					        fop.getUiEventBus().post(new UIEvent.VideoRefresh(this, g, c, getFop()));
				        }
			        });

		        });
		this.startMedalCeremony.setTabIndex(-1);
		this.endMedalCeremony = new Button(
		        Translator.translate("BreakMgmt.endMedals"), (e) -> {
			        OwlcmsSession.withFop(fop -> {
				        this.endMedalCeremony.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				        fop.fopEventPost(new FOPEvent.CeremonyDone(CeremonyType.MEDALS, this.getOrigin()));
				        if (this.inactive) {
					        setBreakTimerFromFields(false);
				        }
			        });
		        });
		this.endMedalCeremony.setTabIndex(-1);
		this.startMedalCeremony.getThemeNames().add("secondary contrast");
		this.endMedalCeremony.getThemeNames().add("secondary contrast");
		medalButtons.add(this.startMedalCeremony, this.endMedalCeremony);

		ce.add(label("PublicMsg.Medals"), hl);
		ce.add(medalButtons);

		return ce;
	}

	private Component createCountdownButtons() {
		this.startCountdown = new Button(Translator.translate("StartCountdown"), new Icon(VaadinIcon.TIMER), (e) -> {
			if (!e.isFromClient()) {
				return;
			}
			OwlcmsSession.withFop(fop -> {
				BreakType value = this.countdownRadios.getValue();
				if (value != null &&
				        (value.isCountdown()
				                || (this.countdownTypeRadios.getValue() != CountdownType.INDEFINITE))) {
					// force FOP to accept our break and value as new
					fop.setBreakType(null);
					fop.setCountdownType(null);
					Integer tr = this.computeTimerRemainingFromFields(false, this.countdownTypeRadios.getValue());
					if (tr == null) {
						fop.getBreakTimer().setTimeRemaining(0, true);
					} else {
						fop.getBreakTimer().setTimeRemaining(tr, false);
					}
				}
			});
			masterStartBreak(false);
		});
		this.startCountdown.getElement().setAttribute("theme", "primary contrast");
		this.startCountdown.getElement().setAttribute("title", Translator.translate("StartCountdown"));
		if (this.countdownRadios.getValue() != null) {
			enableStartCountdown(false);
		}

		this.endCountdown = new Button(Translator.translate("EndBreak"), new Icon(VaadinIcon.MICROPHONE),
		        endBreak(this.parentDialog));
		this.endCountdown.getElement().setAttribute("theme", "primary success");
		this.endCountdown.getElement().setAttribute("title", Translator.translate("EndBreak"));

		HorizontalLayout buttons = new HorizontalLayout();
		buttons.add(this.startCountdown, this.endCountdown);
		buttons.setSpacing(true);
		buttons.setMargin(false);
		buttons.setPadding(false);
		buttons.setJustifyContentMode(JustifyContentMode.AROUND);
		return buttons;
	}

	private VerticalLayout createCountdownColumn() {
		this.countdownRadios = new RadioButtonGroup<>();
		this.countdownRadios.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

		this.countdowns = Arrays.asList(BreakType.values()).stream()
		        .filter(bt -> bt.isCountdown() && bt != BreakType.GROUP_DONE)
		        .collect(Collectors.toList());

		this.countdownRadios.setItems(this.countdowns);
		this.countdownRadios.setRenderer(new TextRenderer<>(
		        (item) -> {
			        String s = Translator.translate(BreakType.class.getSimpleName() + ".Label_" + item.name());
			        return s;
		        }));

		this.countdownTypeLayout = new VerticalLayout();
		this.countdownTypeRadios = new RadioButtonGroup<>();
		CountdownType[] values = CountdownType.values();
		List<CountdownType> list = Arrays.asList(values).stream()
		        .filter(t -> t != CountdownType.INDEFINITE)
		        .collect(Collectors.toList());
		this.countdownTypeRadios.setItems(list);
		this.countdownTypeRadios.setRenderer(new TextRenderer<>(
		        (item) -> Translator.translate(CountdownType.class.getSimpleName() + "." + item.name())));

		Locale locale = new Locale("en", "SE"); // ISO 8601 style dates and time
		this.timePicker.setLocale(locale);
		this.datePicker.setLocale(locale);
		this.minutes = new NativeLabel(Translator.translate("minutes"));

		Component countdownButtons = createCountdownButtons();

		VerticalLayout cd = new VerticalLayout();

		Div title2 = new Div(label("BreakType.Title"));
		title2.getElement().setAttribute("style", "margin-top: 1ex");
		this.noCountdown.getElement().setProperty("innerHTML", "&nbsp;");

		cd.add(title2);
		// cd.add(this.interruptionActiveError);
		cd.add(this.countdownSelectionRequired);
		cd.add(this.countdownRadios);

		this.countdownTypeLayout.add(this.countdownTypeRadios);
		this.countdownTypeLayout.add(this.noCountdown);
		this.countdownTypeLayout.add(this.waitText);
		this.countdownTypeLayout.add(this.durationField);
		this.countdownTypeLayout.add(new HorizontalLayout(this.datePicker, this.timePicker));
		this.countdownTypeLayout.setMargin(false);
		this.countdownTypeLayout.setSpacing(false);
		this.countdownTypeLayout.setPadding(false);

		cd.add(new Hr(), this.countdownTypeLayout, countdownButtons);
		cd.setSpacing(false);
		cd.setMargin(false);

		return cd;
	}

	private Component createInterruptionButtons(RadioButtonGroup<BreakType> interruptionRadios2) {
		this.stopCompetition = new Button(Translator.translate("StopCompetition"), new Icon(VaadinIcon.EXCLAMATION),
		        (e) -> {
			        OwlcmsSession.withFop(fop -> {
				        if (!e.isFromClient()) {
					        return;
				        }
				        BreakType value = this.interruptionRadios.getValue();
				        fop.setBreakType(value);
				        fop.setCountdownType(null);
				        fop.getBreakTimer().setTimeRemaining(0, true);
			        });
			        masterStartBreak(true);
		        });
		this.stopCompetition.getElement().setAttribute("theme", "primary error");
		this.stopCompetition.getElement().setAttribute("title", Translator.translate("StopCompetition"));
		if (interruptionRadios2.getValue() != null) {
			this.stopCompetition.setEnabled(true);
		}

		// breakPause = new Button(new Icon(VaadinIcon.PAUSE), (e) -> masterPauseBreak(null));
		// breakPause.getElement().setAttribute("theme", "primary contrast");
		// breakPause.getElement().setAttribute("title", Translator.translate("PauseCountdown"));

		this.endInterruption = new Button(Translator.translate("ResumeCompetition"), new Icon(VaadinIcon.MICROPHONE),
		        endBreak(this.parentDialog));
		this.endInterruption.getElement().setAttribute("theme", "primary success");
		this.endInterruption.getElement().setAttribute("title", Translator.translate("ResumeCompetition"));

		HorizontalLayout buttons = new HorizontalLayout();
		buttons.add(this.stopCompetition, this.endInterruption);
		buttons.setSpacing(true);
		buttons.setMargin(false);
		buttons.setPadding(false);
		buttons.setJustifyContentMode(JustifyContentMode.AROUND);
		return buttons;
	}

	private VerticalLayout createInterruptionColumn() {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("createInterruptionColumn {}", LoggerUtils.whereFrom());
		}
		this.interruptionRadios = new RadioButtonGroup<>();
		this.interruptionRadios.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
		this.interruptions = Arrays.asList(BreakType.values()).stream()
		        .filter(countdownRadios -> countdownRadios.isInterruption()).collect(Collectors.toList());

		this.interruptionRadios.setRenderer(new TextRenderer<>(
		        (item) -> {
			        String s = Translator.translate(BreakType.class.getSimpleName() + "." + item.name());
			        return s;
		        }));
		this.interruptionRadios.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("setting interruption radios to {} from {}", e.getValue(), LoggerUtils.whereFrom());
			}
			setEnablement();
			this.stopCompetition.setEnabled(true);
		});
		Component interruptionButtons = createInterruptionButtons(this.interruptionRadios);
		VerticalLayout cd = new VerticalLayout();
		this.interruptionRadios.setItems(this.interruptions);

		BreakType bt = getBreakType();
		setInterruptionRadios(this.interruptions.contains(bt) ? bt : BreakType.TECHNICAL);
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("interruptionRadios {} value={}", this.interruptions, this.interruptionRadios.getValue());
		}

		Div title1 = new Div(label("InterruptionType.Title"));
		cd.add(title1);
		// cd.add(this.countdownActiveError);
		cd.add(this.interruptionSelectionRequired);
		cd.add(this.interruptionRadios);
		cd.add(interruptionButtons);

		return cd;
	}

	private void createUI(Dialog parentDialog) {
		this.parentDialog = parentDialog;
		assembleDialog(this);
	}

	// private void doResetTimer(Integer tr) {
	// OwlcmsSession.withFop(fop -> {
	// IBreakTimer breakTimer = fop.getBreakTimer();
	// if (breakTimer.isRunning()) {
	// breakTimer.stop();
	// fop.fopEventPost(new FOPEvent.BreakPaused(tr, this.getOrigin()));
	// }
	// });
	// setEnablement();
	// }

	private void enableStartCountdown(boolean b) {
		// logger.debug("enabled {} {}",b,LoggerUtils.whereFrom());
		this.startCountdown.setEnabled(b);
	}

	private ComponentEventListener<ClickEvent<Button>> endBreak(Dialog dialog) {
		return (e) -> {
			OwlcmsSession.withFop(fop -> {
				// logger.debug("endBreak start lifting");
				fop.fopEventPost(new FOPEvent.StartLifting(this.getOrigin()));
				// logger.debug("endbreak enabling start");
				enableStartCountdown(true);
				this.endCountdown.setEnabled(false);
				fop.getUiEventBus().unregister(this);
				dialog.close();
			});
		};
	}

	private BreakType getBreakType() {
		return this.breakType;
	}

	private CountdownType getCountdownType() {
		// logger.debug("get countdown type {} from {}", countdownType, LoggerUtils.whereFrom());
		return this.countdownType;
	}

	private Group getMedalGroup() {
		return this.medalGroup;
	}

	private Object getOrigin() {
		return this.origin;
	}

	private LocalDateTime getTarget() {
		if (getCountdownType() == CountdownType.INDEFINITE || getBreakType().isInterruption()) {
			return null;
		}
		final LocalDateTime target;
		LocalDate date = this.datePicker.getValue();
		LocalTime time = this.timePicker.getValue();
		target = LocalDateTime.of(date, time);
		return target;
	}

	private void initFromFOP(Dialog parentDialog) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("BreakManagement FOP");
		}
		this.fop = OwlcmsSession.getFop();

		CountdownType countdownType2 = this.fop.getCountdownType();
		BreakType breakType2 = this.fop.getBreakType();
		initState(this.origin,
		        breakType2 != null ? breakType2
		                : (this.requestedBreak != null ? this.requestedBreak : BreakType.TECHNICAL),
		        countdownType2 != null ? countdownType2 : mapBreakTypeToDurationValue(breakType2));
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("after initState");
		}
		createUI(parentDialog);
		setEnablement();
	}

	private void initState(Object origin, BreakType brt, CountdownType cdt) {
		this.setOrigin(origin);
		this.setBreakType(brt);
		this.setCountdownType(cdt);
	}

	private NativeLabel label(String key) {
		NativeLabel label = new NativeLabel(Translator.translate(key));
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

	private void masterStartBreak(boolean interruption) {
		if (!interruption) {
			this.startIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			this.endIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			this.startMedalCeremony.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			this.endMedalCeremony.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			this.startOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			this.endOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
		}

		OwlcmsSession.withFop(fop -> {
			masterStartBreak(fop, interruption);
		});
		// e.getSource().setEnabled(false);
		// logger.debug("start break disable start");
		setEnablement();
	}

	private void masterStartBreak(FieldOfPlay fop, boolean interruption) {
		if (this.timeRemaining == null && fop.getBreakTimer() != null) {
			this.timeRemaining = (long) fop.getBreakTimer().liveTimeRemaining();
		}
		if (interruption) {
			fop.fopEventPost(new FOPEvent.BreakStarted(
			        interruptionRadios.getValue(),
			        CountdownType.INDEFINITE,
			        null,
			        null,
			        true,
			        this.getOrigin()));
		} else {
			// this.setBreakTimerFromFields(false);
			fop.fopEventPost(new FOPEvent.BreakStarted(
			        getBreakType(),
			        getCountdownType(),
			        getCountdownType() == CountdownType.INDEFINITE
			                ? null
			                : this.timeRemaining.intValue(),
			        getTarget(),
			        getCountdownType() == CountdownType.INDEFINITE,
			        this.getOrigin()));
		}
	}

	private void masterStartCeremony(FieldOfPlay fop, CeremonyType ceremonyType) {
		fop.fopEventPost(
		        new FOPEvent.CeremonyStarted(ceremonyType, fop.getGroup(), null, this));
	}

	private void selectCeremonyCategory(Group g, Category c) {
		this.endMedalCeremony.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
		this.startMedalCeremony.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		setMedalGroup(g);
		setMedalCategory(c);
	}

	private void setBreakTimerFromFields(boolean interruption) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("setBreakTimerFromFields from={}", LoggerUtils.whereFrom());
		}
		LocalDateTime now = LocalDateTime.now();

		CountdownType cType = this.countdownTypeRadios.getValue();
		BreakType bType;

		if (!interruption) {
			bType = this.countdownRadios.getValue();
			cType = this.countdownTypeRadios.getValue();
			this.setBreakType(bType);
			this.setCountdownType(cType);
		} else {
			bType = this.interruptionRadios.getValue();
			if (bType != null && bType.isInterruption()) {
				this.fop.getBreakTimer().setIndefinite();
			}
		}

		final CountdownType curCType = cType;
		logger.debug("--- interruption {} {} {}", interruption, curCType, bType);
		OwlcmsSession.withFop(fop -> {
			if (!interruption && curCType == CountdownType.TARGET) {
				LocalDateTime target = getTarget();
				this.timeRemaining = now.until(target, ChronoUnit.MILLIS);
				// logger.debug("setBreakTimerFromFields target-derived duration {}", formattedDuration(timeRemaining));
				fop.getBreakTimer().setTimeRemaining(this.timeRemaining.intValue(), false);
				fop.getBreakTimer().setBreakDuration(this.timeRemaining.intValue());
				fop.getBreakTimer().setEnd(null);
			} else if (interruption || curCType == CountdownType.INDEFINITE) {
				// logger.debug("setBreakTimerFromFields indefinite");
				fop.getBreakTimer().setIndefinite(); // CHECK
				this.timeRemaining = null;
			} else {
				Duration value;
				value = this.durationField.getValue();
				value = (value == null ? DEFAULT_DURATION : value);
				this.timeRemaining = (value != null ? value.toMillis() : 0L);
				fop.getBreakTimer().setTimeRemaining(this.timeRemaining.intValue(), false);
				fop.getBreakTimer().setBreakDuration(this.timeRemaining.intValue());
				fop.getBreakTimer().setEnd(null);
			}
		});
	}

	private void setBreakType(BreakType breakType) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("set break type {} from {}", breakType, LoggerUtils.whereFrom());
		}
		this.breakType = breakType;
	}

	private void setBreakValue(BreakType breakType) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("setBreakValue {} from {}", breakType, LoggerUtils.whereFrom());
		}

		// figure out what the best countdown match would be
		if (fop.getState() == FOPState.INACTIVE) {
			if (this.countdownRadios.getValue() == null) {
				// default value
				this.countdownRadios.setValue(BreakType.BEFORE_INTRODUCTION);
			}
		} else if (fop.getState() != FOPState.BREAK) {
			// we are lifting.
			if (this.countdownRadios.getValue() == null) {
				// default value
				inferCountdownFromStage();
			}
		} else if (fop.getBreakType() != null) {
			// not lifting.
			if (this.countdownRadios.getValue() == null) {
				// default value
				switch (fop.getBreakType()) {
					case BEFORE_INTRODUCTION -> this.countdownRadios.setValue(BreakType.BEFORE_INTRODUCTION);
					case FIRST_CJ -> this.countdownRadios.setValue(BreakType.FIRST_CJ);
					case FIRST_SNATCH -> this.countdownRadios.setValue(BreakType.FIRST_SNATCH);
					case GROUP_DONE -> this.countdownRadios.setValue(BreakType.FIRST_CJ);
					case SNATCH_DONE -> this.countdownRadios.setValue(BreakType.FIRST_CJ);
					default -> inferCountdownFromStage();
				}
			}
		}

		logger.debug("countdownRadios inferred as {}", this.countdownRadios.getValue());

		// this.countdownRadios.setValue(this.countdowns.contains(breakType) ? breakType : null);
		// setInterruptionRadios(this.interruptions.contains(breakType) ? breakType : null);

		// logger.debug("set countdown radio value {} from {}", countdownRadios.getValue(), LoggerUtils.whereFrom());
		// logger.debug("set interruption radio value {} from {}", interruptionRadios.getValue(),
		// LoggerUtils.whereFrom());
		boolean inactiveOrBreak = (this.fop.getState() == FOPState.BREAK) || this.fop.getState() == FOPState.INACTIVE;
		if (this.countdownRadios.getValue() == null) {
			setCountdownTypeValue(null);
			enableStartCountdown(false);
			this.endCountdown.setEnabled(false);
		} else {
			if (inactiveOrBreak) {
				enableStartCountdown(this.countdownRadios.getValue() != null);
				this.endCountdown.setEnabled(true);
			} else {
				if (this.logger.isDebugEnabled()) {
					this.logger.debug("disabling countdown start");
				}
				enableStartCountdown(true);
				this.endCountdown.setEnabled(true);
			}
		}

		if (this.interruptionRadios.getValue() == null) {
			this.stopCompetition.setEnabled(false);
			this.endInterruption.setEnabled(false);
		} else {
			// this.stopCompetition
			// .setEnabled(getBreakType().isInterruption() || this.fop.getBreakType() == BreakType.GROUP_DONE ? true :
			// false);
			this.stopCompetition.setEnabled(true);
			this.endInterruption.setEnabled(true);
		}

	}

	private void inferCountdownFromStage() {
		switch (fop.getCurrentStage()) {
			case CLEANJERK -> this.countdownRadios.setValue(BreakType.FIRST_CJ);
			case SNATCH -> this.countdownRadios.setValue(BreakType.FIRST_SNATCH);
			default -> throw new IllegalArgumentException("Unexpected value: " + fop.getCurrentStage());
		}
		this.durationField.setValue(DEFAULT_DURATION);
		this.setCountdownType(CountdownType.DURATION);
	}

	private void setCeremonyButtonHighlight(BreakType breakType2) {
		if (breakType2 == BreakType.FIRST_SNATCH) {
			if (this.fop.getCeremonyType() == CeremonyType.INTRODUCTION) {
				this.startIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				this.endIntroButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
				this.startOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				this.endOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			} else if (this.fop.getCeremonyType() == CeremonyType.OFFICIALS_INTRODUCTION) {
				this.startIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				this.endIntroButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				this.startOfficials.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
				this.endOfficials.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			}
		}
	}

	private void setCountdownFieldVisibility(CountdownType cType) {
		// logger.debug("setCountdownFieldVisibility {} from {}", cType, LoggerUtils.whereFrom());
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
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("setCountdownType {} from {}", countdownType, LoggerUtils.whereFrom());
		}
		this.countdownType = countdownType;
	}

	private void setCountdownTypeValue(CountdownType ct2) {
		logger.debug("setting countdownTypeRadios {} from {}", ct2, LoggerUtils.whereFrom());
		setCountdownType(ct2);
		this.countdownTypeRadios.setValue(ct2);
		setCountdownFieldVisibility(ct2);
		setCeremonyButtonHighlight(getBreakType());
	}

	private void setDurationField(Duration duration) {
		// logger.debug("{} {}", duration, LoggerUtils.whereFrom());
		this.durationField.setValue(duration);
	}

	private void setDurationFields(int targetTimeDuration, Integer breakDuration) {
		// logger.debug("setDurationFieldsFromBreakTimer target={} duration={}", targetTimeDuration, breakDuration);
		setDurationField(breakDuration != null ? Duration.ofMillis(breakDuration) : DEFAULT_DURATION);
		LocalDateTime target = LocalDateTime.now().plus(targetTimeDuration, ChronoUnit.MILLIS);
		this.datePicker.setValue(target.toLocalDate());
		this.timePicker.setValue(target.toLocalTime());
		CountdownType value = this.countdownTypeRadios.getValue();
		if (value != null) {
			switch (value) {
				case DURATION:
					this.timeRemaining = (long) targetTimeDuration;
					break;
				case INDEFINITE:
					this.timeRemaining = null;
					break;
				case TARGET:
					this.timeRemaining = (long) targetTimeDuration;
					break;
			}
		}
	}

	private void setEnablement() {
		if (this.fop.getState() == FOPState.BREAK && this.fop.getBreakType() != BreakType.GROUP_DONE) {
			// this.countdownActiveError.getStyle().set("display",
			// this.fop.getBreakType().isCountdown() ? "block" : "none");
			// this.countdownActiveError.getStyle().set("background-color",
			// this.fop.getBreakType().isCountdown() ? "var(--lumo-error-color-10pct)" : "var(--lumo-base-color)");

			// this.interruptionActiveError.getStyle().set("display",
			// this.fop.getBreakType().isInterruption() ? "block" : "none");
			// this.interruptionActiveError.getStyle().set("background-color",
			// this.fop.getBreakType().isInterruption() ? "var(--lumo-error-color-10pct)"
			// : "var(--lumo-base-color)");
			// this.interruptionRadios.setEnabled(!this.fop.getBreakType().isCountdown());

			// this.countdownRadios.setEnabled(this.fop.getBreakType().isCountdown());
			// this.countdownTypeLayout.setEnabled(this.fop.getBreakType().isCountdown());
			this.waitText.getStyle().set("visibility", "hidden");

		} else {
			// logger.debug("setEnablement not break");
			// this.countdownActiveError.getStyle().set("display", "none");
			// this.interruptionActiveError.getStyle().set("display", "none");
			this.interruptionRadios.setEnabled(true);
			this.countdownRadios.setEnabled(true);
			this.countdownTypeLayout.setEnabled(true);
		}
		boolean requireCountdown = this.countdownRadios.getValue() == null && this.countdownRadios.isEnabled();
		this.countdownSelectionRequired.getStyle().set("display", requireCountdown ? "block" : "none");
		this.countdownSelectionRequired.getStyle().set("font-weight", requireCountdown ? "bold" : "normal");
		this.interruptionSelectionRequired.getStyle().set("display",
		        this.interruptionRadios.getValue() == null && this.interruptionRadios.isEnabled() ? "block" : "none");
	}

	private void setInterruptionRadios(BreakType breakType) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("set interruption radios to {} from {}", breakType, LoggerUtils.whereFrom());
		}
		this.interruptionRadios.setValue(breakType);
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
			// //logger.debug("Break Done {}", LoggerUtils. stackTrace());
			UIEventProcessor.uiAccess(this, this.uiEventBus, e,
			        () -> this.parentDialog.close());
		}
	}

	@Subscribe
	private void slaveBreakPause(UIEvent.BreakPaused e) {
		synchronized (this) {
			UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
				setEnablement();
			});
		}
	}

	@Subscribe
	private void slaveBreakStart(UIEvent.BreakStarted e) {
		synchronized (this) {
			if (e.isDisplayToggle()) {
				return;
			}
			UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
				if (!e.getPaused()) {
					setEnablement();
				} else {
					setEnablement();
				}
				setBreakValue(e.getBreakType());
			});
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
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("switchToDuration from {}", LoggerUtils.whereFrom());
		}
		this.noCountdown.setVisible(false);
		this.waitText.setVisible(false);
		this.durationField.setVisible(true);
		this.minutes.setVisible(true);
		this.datePicker.setVisible(false);
		this.timePicker.setVisible(false);
		this.durationField.focus();
		this.durationField.setAutoselect(true);
		setEnablement();
		// setBreakTimerFromFields(false);
	}

	private void switchToIndefinite() {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("switchToIndefinite from {}", LoggerUtils.stackTrace());
		}
		this.noCountdown.setVisible(false);
		this.waitText.setVisible(true);
		this.durationField.setVisible(false);
		this.minutes.setVisible(false);
		this.datePicker.setVisible(false);
		this.timePicker.setVisible(false);
		setEnablement();
		// setBreakTimerFromFields(true);
	}

	private void switchToNoCountdown() {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("switchToNoCountdown from {}", LoggerUtils.whereFrom());
		}
		this.noCountdown.setVisible(true);
		this.waitText.setVisible(false);
		this.durationField.setVisible(false);
		this.minutes.setVisible(true);
		this.datePicker.setVisible(false);
		this.timePicker.setVisible(false);
		setEnablement();
		// setBreakTimerFromFields(true);
	}

	private void switchToTarget() {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("switchToTarget breaktype={} from {} ", getBreakType(), LoggerUtils.stackTrace());
		}
		this.noCountdown.setVisible(false);
		this.waitText.setVisible(false);
		this.durationField.setVisible(false);
		this.minutes.setVisible(false);
		this.datePicker.setVisible(true);
		this.timePicker.setVisible(true);
		this.timePicker.focus();
		setEnablement();
		if (getBreakType() == BreakType.BEFORE_INTRODUCTION) {
			computeDefaultTimeValues();
		}
		// setBreakTimerFromFields(false);
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

			// logger.debug("syncWithFop {} {} {}", fopState,
			// fop.getBreakTimer().liveTimeRemaining(),fop.getBreakTimer().isRunning());

			running[0] = false;

			switch (fopState) {
				case BREAK:
				case INACTIVE:
					BreakType breakType2 = fop.getBreakType();
					if (breakType2 == BreakType.GROUP_DONE || fopState == FOPState.INACTIVE) {
						if (this.logger.isDebugEnabled()) {
							this.logger.debug("syncWithFOP: explicit BEFORE_INTRODUCTION");
						}
						breakType2 = BreakType.BEFORE_INTRODUCTION;
						this.countdownRadios.setValue(breakType2);
						computeDefaultTimeValues();
						setCountdownTypeValue(CountdownType.TARGET);
						setBreakType(breakType2);
						return;
					}

					setBreakValue(breakType2);
					CountdownType ct = fop.getCountdownType();
					setCountdownType(ct);
					if (ct == CountdownType.INDEFINITE) {
						fopLiveTimeRemaining = (int) DEFAULT_DURATION.toMillis();
					}

					// override from FOP
					setDurationFields(fopLiveTimeRemaining, fopBreakDuration);
					if (ct != null) {
						setCountdownTypeValue(ct);
					} else {
						setCountdownTypeValue(mapBreakTypeToDurationValue(this.getBreakType()));
					}

					if (breakType2 == BreakType.GROUP_DONE) {
						computeDefaultTimeValues();
						setCountdownTypeValue(CountdownType.TARGET);
					}
					break;
				default:
					break;
			}

		});
	}

}
