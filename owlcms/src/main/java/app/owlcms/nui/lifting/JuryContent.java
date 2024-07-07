/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.lifting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.components.elements.JuryDisplayDecisionElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.shared.AthleteGridContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "lifting/jury", layout = OwlcmsLayout.class)
public class JuryContent extends AthleteGridContent implements HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(JuryContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	static {
		logger.setLevel(Level.DEBUG);
		uiEventLogger.setLevel(Level.INFO);
	}
	Notification decisionNotification;
	List<ShortcutRegistration> registrations;
	private Athlete athleteUnderReview;
	private JuryDisplayDecisionElement decisions;
	private JuryDialog juryDialog;
	private Icon[] juryIcons;
	private NativeLabel juryLabel;
	private Boolean[] juryVotes;
	private HorizontalLayout juryVotingButtons;
	private VerticalLayout juryVotingCenterHorizontally;
	private long lastOpen;
	private HorizontalLayout refContainer;
	private Component refereeLabelWrapper;
	private boolean summonEnabled;
	Athlete previousAthleteAtStart;
	int previousAttemptNumber;
	Athlete currentAthleteAtStart;
	int currentAttemptNumber;
	private boolean newClock;
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	public JuryContent() {
		// we don't actually inherit behaviour from the superclass because
		// all this does is call init() -- which we override.
		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied
		
		setDefaultParameters(QueryParameters.simple(Map.of(
		        SoundParameters.SILENT, "true",
		        SoundParameters.DOWNSILENT, "true",
		        SoundParameters.IMMEDIATE, "true",
		        SoundParameters.SINGLEREF, "false",
		        SoundParameters.LIVE_LIGHTS, "true",
		        SoundParameters.SHOW_DECLARATIONS, "false",
		        SoundParameters.CENTER_NOTIFICATIONS, Boolean.toString(Config.getCurrent().featureSwitch("centerAnnouncerNotifications")),
		        SoundParameters.START_ORDER, "false")));
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#add(app.owlcms.data.athlete.Athlete)
	 */
	@Override
	public Athlete add(Athlete athlete) {
		// do nothing
		return athlete;
	}

	public String athleteFullId(final Athlete athlete) {
		Integer startNumber = athlete.getStartNumber();
		return (startNumber != null ? "[" + startNumber + "] " : "") + athlete.getFullId();
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#delete(app.owlcms.data.athlete.Athlete)
	 */
	@Override
	public void delete(Athlete Athlete) {
		// do nothing;
	}

	@Override
	public String getMenuTitle() {
		return getPageTitle();
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return Translator.translate("Jury") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		super.slaveBreakStart(e);
		if (e.getBreakType() == BreakType.JURY || e.getBreakType() == BreakType.CHALLENGE) {
			UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
				resetJuryVoting();
			});
		}
	}

	@Subscribe
	public void slaveDown(UIEvent.DownSignal e) {
		// Ignore down signal
	}

	@Subscribe
	public void slaveJuryMemberDecision(UIEvent.JuryUpdate e) {
		Boolean[] decision = e.getJuryMemberDecision();
		Integer juryMember = e.getJuryMemberUpdated();
		Boolean goodBad = juryMember != null ? decision[juryMember] : null;
		// logger.debug("update jury decisions {} {} {} {}", goodBad, juryMember, this,
		// e.getOrigin());
		if (juryMember != null && goodBad != null) {
			UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this, () -> {
				// logger.debug("updating");
				juryVote(juryMember, goodBad, false);
			});
		}
	}

	@Subscribe
	public void slaveRefereeDecision(UIEvent.Decision e) {
		// uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(),
		// e.getClass().getSimpleName(),
		// e.getAthlete());
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			// juryDeliberationButton.setEnabled(true);
			int d = e.decision ? 1 : 0;
			String text = Translator.translate("NoLift_GoodLift", d, e.getAthlete().getFullName());
			// logger.debug("setting athleteUnderReview2 {}", e.getAthlete());
			setAthleteUnderReview(e.getAthlete());

			this.decisionNotification = new Notification();
			// Notification theme styling is done in
			// META-INF/resources/frontend/styles/shared-styles.html
			String themeName = e.decision ? "success" : "error";
			this.decisionNotification.getElement().getThemeList().add(themeName);

			Div label = new Div();
			label.add(text);
			label.addClickListener((event) -> this.decisionNotification.close());
			label.setSizeFull();
			label.getStyle().set("font-size", "large");
			this.decisionNotification.add(label);
			this.decisionNotification.setPosition(Position.TOP_START);
			// let the lift/no lift decision go away - people can see the referee lights.
			this.decisionNotification.setDuration(5000);
			this.decisionNotification.open();

			swapRefereeLabel(e.getAthlete());
		});
	}

	@Subscribe
	public void slaveResetOnNewClock(UIEvent.ResetOnNewClock e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> syncWithFop(true, getFop()));
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#slaveStartLifting(app.owlcms.uievents.UIEvent.StartLifting)
	 */
	@Override
	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			if (this.juryDialog != null && this.juryDialog.isOpened()) {
				this.juryDialog.doClose(true);
			} else {
				doSync();
			}
		});
	}

	@Subscribe
	public void slaveTimeStarted(UIEvent.StartTime e) {
		OwlcmsSession.withFop(fop -> {
			this.currentAthleteAtStart = fop.getClockOwner();
			if (this.currentAthleteAtStart != null) {
				this.currentAttemptNumber = this.currentAthleteAtStart.getActuallyAttemptedLifts();
			} else {
				this.currentAttemptNumber = 0;
			}
			this.newClock = e.getTimeRemaining() == 60000 || e.getTimeRemaining() == 120000;
		});
		// this is redundant because of slaveResetOnNewClock
		if ((this.currentAthleteAtStart != this.previousAthleteAtStart)
		        || (this.currentAttemptNumber != this.previousAttemptNumber)
		        || this.newClock) {
			// we switched lifter, or we switched attempt.
			// reset the decisions.
			// logger.debug("RESETTING");
			UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
				this.decisions.doReset();
				this.juryVotingButtons.removeAll();
				resetJuryVoting();
				this.decisions.setSilenced(true);
				if (this.decisionNotification != null) {
					this.decisionNotification.close();
				}
				// referee decisions handle reset on their own, nothing to do.
				// reset referee decision label
				swapRefereeLabel(null);
			});
		} else {
			// logger.debug("NOT resetting");
		}
		this.previousAthleteAtStart = this.currentAthleteAtStart;
		this.previousAttemptNumber = this.currentAttemptNumber;
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#update(app.owlcms.data.athlete.Athlete)
	 */
	@Override
	public Athlete update(Athlete athlete) {
		// do nothing
		return athlete;
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#announcerButtons(com.vaadin.flow.component.orderedlayout.HorizontalLayout)
	 */
	@Override
	protected HorizontalLayout announcerButtons(FlexLayout announcerBar) {
		// moved down to the jury section
		return new HorizontalLayout(); // juryDeliberationButtons();
	}

	@Override
	protected void createTopBarSettingsMenu() {
		this.topBarSettings = new MenuBar();
		this.topBarSettings.addThemeVariants(MenuBarVariant.LUMO_SMALL, MenuBarVariant.LUMO_TERTIARY_INLINE);
		MenuItem item2 = this.topBarSettings.addItem(
		        new Icon(VaadinIcon.COG)// IronIcons.SETTINGS.create()
		);
		SubMenu subMenu2 = item2.getSubMenu();
		subMenu2.addItem(
		        this.isSilenced() ? Translator.translate("Settings.TurnOnSound")
		                : Translator.translate("Settings.TurnOffSound"),
		        e -> {
			        switchSoundMode(!this.isSilenced(), true);
			        e.getSource().setText(this.isSilenced() ? Translator.translate("Settings.TurnOnSound")
			                : Translator.translate("Settings.TurnOffSound"));
			        if (this.decisionDisplay != null) {
				        this.decisionDisplay.setSilenced(true);
			        }
			        if (this.decisions != null) {
				        this.decisions.setSilenced(true);
			        }
			        if (this.timer != null) {
				        this.timer.setSilenced(this.isSilenced());
			        }
		        });
		subMenu2.addItem("3", (e) -> {
			OwlcmsSession.withFop(fop -> {
				this.setNbJurors(3);
			});
		});
		subMenu2.addItem("5", (e) -> {
			OwlcmsSession.withFop(fop -> {
				this.setNbJurors(5);
			});
		});
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#decisionButtons(com.vaadin.flow.component.orderedlayout.HorizontalLayout)
	 */
	@Override
	protected HorizontalLayout decisionButtons(FlexLayout announcerBar) {
		// moved down to the jury section
		return new HorizontalLayout(); // juryDecisionButtons();
	}

	protected void doSync() {
		syncWithFop(false, getFop());
		this.decisions.slaveDecisionReset(null);

		// OwlcmsSession.getFop().fopEventPost(new FOPEvent.StartLifting(this));
		if (this.decisionNotification != null) {
			this.decisionNotification.close();
		}
	}

	@Override
	protected void init() {
		setNbJurors(Competition.getCurrent().getJurySize());
	}

	protected void init(int nbj) {
		// logger.trace("init {}", LoggerUtils.whereFrom());
		this.summonEnabled = true; // works with phones/tablets
		this.registrations = new ArrayList<>();
		this.setBoxSizing(BoxSizing.BORDER_BOX);
		this.setSizeFull();
		Competition.getCurrent().setJurySize(nbj);
		buildJuryBox(this);
		buildRefereeBox(this);
	}

	@Override
	protected void syncWithFop(boolean refreshGrid, FieldOfPlay fop) {
		super.syncWithFop(refreshGrid, fop);
		setAthleteUnderReview(fop.getAthleteUnderReview());
		Boolean[] curDecisions = fop.getJuryMemberDecision();
		if (curDecisions != null) {
			for (int i = 0; i < getNbJurors(); i++) {
				Boolean goodBad = curDecisions[i];
				// logger.debug("existing jury {} {}", i, goodBad);
				juryVote(i, goodBad, false);
			}
		}
		Boolean[] curRefDecisions = fop.getRefereeDecision();
		Long[] curRefTimes = fop.getRefereeTime();
		this.decisions.doReset();
		if (curRefDecisions != null) {
			// for (int i = 0; i < 3; i++) {
			// Boolean goodBad = curRefDecisions[i];
			// logger.debug("existing ref {} {}", i, goodBad);
			// }
			if (fop.isRefereeForcedDecision()) {
				this.decisions.slaveRefereeUpdate(new UIEvent.RefereeUpdate(this.athleteUnderReview, null,
				        curRefDecisions[1], null, null, curRefTimes[1], null, this, fop));
			} else {
				this.decisions.slaveRefereeUpdate(new UIEvent.RefereeUpdate(this.athleteUnderReview,
				        curRefDecisions[0],
				        curRefDecisions[1], curRefDecisions[2], curRefTimes[0], curRefTimes[1], curRefTimes[2],
				        this, fop));
			}
		}
	}

	private Icon bigIcon(VaadinIcon iconDef, String color) {
		Icon icon = iconDef.create();
		icon.setSize("80%");
		icon.getStyle().set("color", color);
		return icon;
	}

	private void buildJuryBox(VerticalLayout juryContainer) {
		// logger.trace("buildJuryBox {}", LoggerUtils.whereFrom());
		HorizontalLayout topRow = new HorizontalLayout();
		this.juryLabel = new NativeLabel(Translator.translate("JuryDecisions"));
		H3 labelWrapper = new H3(this.juryLabel);
		labelWrapper.setWidth("15em");
		NativeLabel spacer = new NativeLabel();
		spacer.setWidth("3em");
		topRow.add(labelWrapper, juryDeliberationButtons(),
		        juryDecisionButtons());
		topRow.setDefaultVerticalComponentAlignment(Alignment.CENTER);

		buildJuryVoting();
		resetJuryVoting();

		juryContainer.setBoxSizing(BoxSizing.BORDER_BOX);
		juryContainer.setMargin(false);
		juryContainer.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		juryContainer.add(topRow);
		juryContainer.setAlignSelf(Alignment.START, topRow);
		juryContainer.add(this.juryVotingCenterHorizontally);

	}

	private void buildJuryVoting() {
		// center buttons vertically, spread withing proper width
		this.juryVotingButtons = new HorizontalLayout();
		this.juryVotingButtons.setBoxSizing(BoxSizing.BORDER_BOX);
		this.juryVotingButtons.setJustifyContentMode(JustifyContentMode.EVENLY);
		this.juryVotingButtons.setDefaultVerticalComponentAlignment(Alignment.CENTER);
		this.juryVotingButtons.setHeight("100%");
		this.juryVotingButtons.setWidth(getNbJurors() == 3 ? "50%" : "85%");
		this.juryVotingButtons.getStyle().set("background-color", "black");
		this.juryVotingButtons.setPadding(false);
		this.juryVotingButtons.setMargin(false);

		// center the button cluster within page width
		this.juryVotingCenterHorizontally = new VerticalLayout();
		this.juryVotingCenterHorizontally.setWidthFull();
		this.juryVotingCenterHorizontally.setBoxSizing(BoxSizing.BORDER_BOX);
		this.juryVotingCenterHorizontally.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		this.juryVotingCenterHorizontally.setPadding(true);
		this.juryVotingCenterHorizontally.setMargin(true);
		this.juryVotingCenterHorizontally.setHeight("80%");
		this.juryVotingCenterHorizontally.getStyle().set("background-color", "black");

		this.juryVotingCenterHorizontally.add(this.juryVotingButtons);
	}

	private void buildRefereeBox(VerticalLayout container) {
		this.refereeLabelWrapper = createRefereeLabel(null);

		this.decisions = new JuryDisplayDecisionElement(false);
		this.decisions.getElement().setAttribute("theme", "dark");
		Div decisionWrapper = new Div(this.decisions);
		decisionWrapper.getStyle().set("width", "50%");
		// decisionWrapper.getStyle().set("height", "max-content");

		this.refContainer = new HorizontalLayout(decisionWrapper);
		this.refContainer.setBoxSizing(BoxSizing.BORDER_BOX);
		this.refContainer.setJustifyContentMode(JustifyContentMode.CENTER);
		this.refContainer.getStyle().set("background-color", "black");
		this.refContainer.setHeight("80%");
		this.refContainer.setWidthFull();
		this.refContainer.setPadding(true);
		this.refContainer.setMargin(true);

		container.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		container.add(this.refereeLabelWrapper);
		container.setAlignSelf(Alignment.START, this.refereeLabelWrapper);
		container.add(this.refContainer);
	}

	private void checkAllVoted() {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			boolean allVoted = true;
			for (Boolean juryVote : this.juryVotes) {
				if (juryVote == null) {
					allVoted = false;
					break;
				}
			}

			if (allVoted) {
				this.juryVotingButtons.removeAll();
				for (int i = 0; i < this.juryVotes.length; i++) {
					Icon fullSizeIcon;
					if (this.juryVotes[i]) {
						fullSizeIcon = bigIcon(VaadinIcon.CHECK_CIRCLE, "white");
					} else {
						fullSizeIcon = bigIcon(VaadinIcon.CLOSE_CIRCLE, "red");
					}
					this.juryVotingButtons.add(fullSizeIcon);
					this.juryIcons[i] = fullSizeIcon;
				}
			}
		});
	}

	private Component createRefereeLabel(Athlete athlete) {
		Html refereeLabel = new Html("<span style='font-size: 110%'>" +
		        Translator.translate("RefereeDecisions")
		        + (athlete != null
		                ? "&nbsp;&nbsp;&nbsp;" + athleteFullId(athlete) + "&nbsp;&nbsp;&nbsp;"
		                        + (formatAttempt(athlete.getAttemptsDone() - 1))
		                        + "&nbsp;&nbsp;&nbsp;" +
		                        athlete.getRequestedWeightForAttempt(athlete.getAttemptsDone() - 1)
		                        + Translator.translate("KgSymbol")
		                : "")
		        + "</span>");
		H3 refereeLabelWrapper = new H3(refereeLabel);
		refereeLabelWrapper.setHeight("5%");
		return refereeLabelWrapper;
	}

	private String formatAttempt(Integer attemptNo) {
		String translate = Translator.translate("AttemptBoard_attempt_number", (attemptNo % 3) + 1);
		return translate;
	}

	private Athlete getAthleteUnderReview() {
		return this.athleteUnderReview;
	}

	private Key getBadKey(int i) {
		switch (i) {
			case 0:
				return Key.DIGIT_2;
			case 1:
				return Key.DIGIT_4;
			case 2:
				return Key.DIGIT_6;
			case 3:
				return Key.DIGIT_8;
			case 4:
				return Key.DIGIT_0;
			default:
				return Key.UNIDENTIFIED;
		}
	}

	private Key getGoodKey(int i) {
		switch (i) {
			case 0:
				return Key.DIGIT_1;
			case 1:
				return Key.DIGIT_3;
			case 2:
				return Key.DIGIT_5;
			case 3:
				return Key.DIGIT_7;
			case 4:
				return Key.DIGIT_9;
			default:
				return Key.UNIDENTIFIED;
		}
	}

	private int getNbJurors() {
		return Competition.getCurrent().getJurySize();
	}

	private HorizontalLayout juryDecisionButtons() {
		HorizontalLayout decisions = new HorizontalLayout();
		return decisions;
	}

	private HorizontalLayout juryDeliberationButtons() {
		Button juryDeliberationButton = new Button(
		        new Icon(VaadinIcon.TIMER),
		        (e) -> {
			        FieldOfPlay fop = OwlcmsSession.getFop();
			        if (fop.getState() == FOPState.BREAK && fop.getBreakType().isCountdown()) {
				        slaveNotification(
				                new UIEvent.Notification(null, this,
				                        UIEvent.Notification.Level.ERROR,
				                        "BreakButton.cannotInterruptBreak",
				                        3000, fop));
			        } else {
				        openJuryDialog(JuryDeliberationEventType.START_DELIBERATION);
			        }
		        });
		juryDeliberationButton.getElement().setAttribute("theme", "primary");
		juryDeliberationButton.setText(Translator.translate("BreakButton.JuryDeliberation"));

		Button challengeButton = new Button(
		        new Icon(VaadinIcon.TIMER),
		        (e) -> {
			        FieldOfPlay fop = OwlcmsSession.getFop();
			        if (fop.getState() == FOPState.BREAK && fop.getBreakType().isCountdown()) {
				        slaveNotification(
				                new UIEvent.Notification(null, this,
				                        UIEvent.Notification.Level.ERROR,
				                        "BreakButton.cannotInterruptBreak",
				                        3000, fop));
			        } else {
				        openJuryDialog(JuryDeliberationEventType.CHALLENGE);
			        }
		        });
		challengeButton.getElement().setAttribute("theme", "primary");
		challengeButton.setText(Translator.translate("BreakButton.CHALLENGE"));
		// juryDeliberationButton.getElement().setAttribute("title",
		// Translator.translate("BreakButton.JuryDeliberation"));

		Button technicalPauseButton = new Button(
		        new Icon(VaadinIcon.TIMER),
		        (e) -> {
			        FieldOfPlay fop = OwlcmsSession.getFop();
			        if (fop.getState() == FOPState.BREAK && fop.getBreakType().isCountdown()) {
				        slaveNotification(
				                new UIEvent.Notification(null, this,
				                        UIEvent.Notification.Level.ERROR,
				                        "BreakButton.cannotInterruptBreak",
				                        3000, fop));
			        } else {
				        openJuryDialog(JuryDeliberationEventType.TECHNICAL_PAUSE);
			        }
		        });
		technicalPauseButton.getElement().setAttribute("theme", "primary");
		technicalPauseButton.setText(Translator.translate("BreakType.TECHNICAL"));

		HorizontalLayout buttons = new HorizontalLayout(juryDeliberationButton, challengeButton, technicalPauseButton);

		if (this.summonEnabled) {
			Button summonRefereesButton = new Button(
			        new Icon(VaadinIcon.TIMER),
			        (e) -> {
				        openJuryDialog(JuryDeliberationEventType.CALL_REFEREES);
			        });
			summonRefereesButton.getElement().setAttribute("theme", "primary");
			summonRefereesButton.setText(Translator.translate("BreakButton.SummonReferees"));
			buttons.add(summonRefereesButton);
		}

		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return buttons;
	}

	private void juryVote(Integer juryMember, Boolean goodBad, boolean sendFOPEvent) {
		if (goodBad == null) {
			Icon nonVotedIcon = bigIcon(VaadinIcon.CIRCLE_THIN, "gray");
			this.juryVotingButtons.replace(this.juryIcons[juryMember], nonVotedIcon);
			this.juryIcons[juryMember] = nonVotedIcon;
			this.juryVotes[juryMember] = null;
			return;
		}
		Icon votedIcon = bigIcon(VaadinIcon.CIRCLE, "gray");
		this.juryVotingButtons.replace(this.juryIcons[juryMember], votedIcon);
		this.juryIcons[juryMember] = votedIcon;
		this.juryVotes[juryMember] = goodBad;
		if (sendFOPEvent) {
			OwlcmsSession.withFop(fop -> {
				fop.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(this, juryMember, goodBad));
			});
		}
		checkAllVoted();
	}

	private void openJuryDialog(JuryDeliberationEventType deliberation) {
		long now = System.currentTimeMillis();
		if (now - this.lastOpen > 100 && (this.juryDialog == null || !this.juryDialog.isOpened())) {
			OwlcmsSession.withFop(fop -> {
				if (fop.getState() != FOPState.BREAK && deliberation != JuryDeliberationEventType.TECHNICAL_PAUSE) {
					fop.fopEventPost(
					        new FOPEvent.BreakStarted(
					                deliberation == JuryDeliberationEventType.CHALLENGE ? BreakType.CHALLENGE
					                        : BreakType.JURY,
					                CountdownType.INDEFINITE, 0, null, true, this));
				}
				this.juryDialog = new JuryDialog(JuryContent.this, getAthleteUnderReview(), deliberation,
				        this.summonEnabled);
				this.juryDialog.open();
				this.lastOpen = now;
			});
		}
	}

	private void resetJuryVoting() {
		// logger.debug("resetJuryVoting {} {}", UI.getCurrent(), LoggerUtils.whereFrom());
		for (ShortcutRegistration sr : this.registrations) {
			sr.remove();
		}
		this.juryIcons = new Icon[getNbJurors()];
		this.juryVotes = new Boolean[getNbJurors()];
		this.juryVotingButtons.removeAll();
		for (int i = 0; i < getNbJurors(); i++) {
			final int ix = i;
			Icon nonVotedIcon = bigIcon(VaadinIcon.CIRCLE_THIN, "gray");
			this.juryIcons[ix] = nonVotedIcon;
			this.juryVotes[ix] = null;
			this.juryVotingButtons.add(this.juryIcons[ix], nonVotedIcon);
			ShortcutRegistration reg;
			reg = UI.getCurrent().addShortcutListener(() -> {
				juryVote(ix, true, true);
			}, getGoodKey(i));
			this.registrations.add(reg);
			reg = UI.getCurrent().addShortcutListener(() -> {
				juryVote(ix, false, true);
			}, getBadKey(i));
			this.registrations.add(reg);
			reg = UI.getCurrent().addShortcutListener(
			        () -> openJuryDialog(JuryDeliberationEventType.START_DELIBERATION), Key.KEY_D);
			this.registrations.add(reg);
			reg = UI.getCurrent().addShortcutListener(
			        () -> openJuryDialog(JuryDeliberationEventType.CHALLENGE), Key.KEY_C);
			this.registrations.add(reg);
			reg = UI.getCurrent().addShortcutListener(
			        () -> openJuryDialog(JuryDeliberationEventType.TECHNICAL_PAUSE), Key.KEY_T);
			reg = UI.getCurrent().addShortcutListener(
			        () -> {
				        openJuryDialog(JuryDeliberationEventType.TECHNICAL_PAUSE);
				        summonReferee(4);
			        }, Key.KEY_C);
			this.registrations.add(reg);
			if (this.summonEnabled) {
				reg = UI.getCurrent().addShortcutListener(() -> summonReferee(1), Key.KEY_H);
				this.registrations.add(reg);
				reg = UI.getCurrent().addShortcutListener(() -> summonReferee(2), Key.KEY_I);
				this.registrations.add(reg);
				reg = UI.getCurrent().addShortcutListener(() -> summonReferee(3), Key.KEY_J);
				this.registrations.add(reg);
				reg = UI.getCurrent().addShortcutListener(() -> summonReferee(0), Key.KEY_K);
				this.registrations.add(reg);
			}
		}
	}

	private void setAthleteUnderReview(Athlete athleteUnderReview) {
		this.athleteUnderReview = athleteUnderReview;
	}

	private void setNbJurors(int nbJurors) {
		this.removeAll();
		Competition.getCurrent().setJurySize(nbJurors);
		init(nbJurors);
	}

	private void summonReferee(int i) {
		long now = System.currentTimeMillis();
		if (now - this.lastOpen > 100) {

			openJuryDialog(JuryDeliberationEventType.CALL_REFEREES);
			this.lastOpen = now;

			OwlcmsSession.withFop(fop -> {
				if (i > 0) {
					fop.fopEventPost(new FOPEvent.SummonReferee(this.getOrigin(), i));
				} else {
					// i = 0 means call all refs.
					for (int j = 1; j <= 3; j++) {
						fop.fopEventPost(new FOPEvent.SummonReferee(this.getOrigin(), j));
					}
				}
			});
		}
	}

	private void swapRefereeLabel(Athlete athlete) {
		Component nc = createRefereeLabel(athlete);
		this.replace(this.refereeLabelWrapper, nc);
		this.refereeLabelWrapper = nc;
	}
}
