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

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
import com.flowingcode.vaadin.addons.ironicons.IronIcons;
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
import com.vaadin.flow.component.html.Label;
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
import com.vaadin.flow.router.Route;

import app.owlcms.components.elements.JuryDisplayDecisionElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.competition.Competition;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
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
	private Label juryLabel;
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

	Map<String, List<String>> urlParameterMap = new HashMap<String, List<String>>();

	public JuryContent() {
		// we don't actually inherit behaviour from the superclass because
		// all this does is call init() -- which we override.
		super();
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
		return getTranslation("Jury") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	public Map<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> newParameterMap) {
		this.urlParameterMap = newParameterMap;
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
			UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this, () -> {
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
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			// juryDeliberationButton.setEnabled(true);
			int d = e.decision ? 1 : 0;
			String text = getTranslation("NoLift_GoodLift", d, e.getAthlete().getFullName());
			// logger.debug("setting athleteUnderReview2 {}", e.getAthlete());
			setAthleteUnderReview(e.getAthlete());

			decisionNotification = new Notification();
			// Notification theme styling is done in
			// META-INF/resources/frontend/styles/shared-styles.html
			String themeName = e.decision ? "success" : "error";
			decisionNotification.getElement().getThemeList().add(themeName);

			Div label = new Div();
			label.add(text);
			label.addClickListener((event) -> decisionNotification.close());
			label.setSizeFull();
			label.getStyle().set("font-size", "large");
			decisionNotification.add(label);
			decisionNotification.setPosition(Position.TOP_START);
			// let the lift/no lift decision go away - people can see the referee lights.
			decisionNotification.setDuration(5000);
			decisionNotification.open();

			swapRefereeLabel(e.getAthlete());
		});
	}

	@Subscribe
	public void slaveResetOnNewClock(UIEvent.ResetOnNewClock e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> syncWithFOP(true));
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#slaveStartLifting(app.owlcms.uievents.UIEvent.StartLifting)
	 */
	@Override
	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			if (juryDialog != null && juryDialog.isOpened()) {
				juryDialog.doClose(true);
			} else {
				doSync();
			}
		});
	}

	@Override
	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		super.slaveBreakStart(e);
		if (e.getBreakType() == BreakType.JURY) {
			UIEventProcessor.uiAccess(this, uiEventBus, () -> {
				resetJuryVoting();
			});
		}
	}

	@Subscribe
	public void slaveTimeStarted(UIEvent.StartTime e) {
		OwlcmsSession.withFop(fop -> {
			currentAthleteAtStart = fop.getClockOwner();
			if (currentAthleteAtStart != null) {
				currentAttemptNumber = currentAthleteAtStart.getActuallyAttemptedLifts();
			} else {
				currentAttemptNumber = 0;
			}
			newClock = e.getTimeRemaining() == 60000 || e.getTimeRemaining() == 120000;
		});
		// this is redundant because of slaveResetOnNewClock
		if ((currentAthleteAtStart != previousAthleteAtStart)
		        || (currentAttemptNumber != previousAttemptNumber)
		        || newClock) {
			// we switched lifter, or we switched attempt.
			// reset the decisions.
			// logger.debug("RESETTING");
			UIEventProcessor.uiAccess(this, uiEventBus, () -> {
				decisions.doReset();
				juryVotingButtons.removeAll();
				resetJuryVoting();
				decisions.setSilenced(this.isSilenced());
				if (decisionNotification != null) {
					decisionNotification.close();
				}
				// referee decisions handle reset on their own, nothing to do.
				// reset referee decision label
				swapRefereeLabel(null);
			});
		} else {
			// logger.debug("NOT resetting");
		}
		previousAthleteAtStart = currentAthleteAtStart;
		previousAttemptNumber = currentAttemptNumber;
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#update(app.owlcms.data.athlete.Athlete)
	 */
	@Override
	public Athlete update(Athlete athlete) {
		// do nothing
		return athlete;
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
		juryLabel = new Label(getTranslation("JuryDecisions"));
		H3 labelWrapper = new H3(juryLabel);
		labelWrapper.setWidth("15em");
		Label spacer = new Label();
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
		juryContainer.add(juryVotingCenterHorizontally);

	}

	private void buildJuryVoting() {
		// center buttons vertically, spread withing proper width
		juryVotingButtons = new HorizontalLayout();
		juryVotingButtons.setBoxSizing(BoxSizing.BORDER_BOX);
		juryVotingButtons.setJustifyContentMode(JustifyContentMode.EVENLY);
		juryVotingButtons.setDefaultVerticalComponentAlignment(Alignment.CENTER);
		juryVotingButtons.setHeight("100%");
		juryVotingButtons.setWidth(getNbJurors() == 3 ? "50%" : "85%");
		juryVotingButtons.getStyle().set("background-color", "black");
		juryVotingButtons.setPadding(false);
		juryVotingButtons.setMargin(false);

		// center the button cluster within page width
		juryVotingCenterHorizontally = new VerticalLayout();
		juryVotingCenterHorizontally.setWidthFull();
		juryVotingCenterHorizontally.setBoxSizing(BoxSizing.BORDER_BOX);
		juryVotingCenterHorizontally.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		juryVotingCenterHorizontally.setPadding(true);
		juryVotingCenterHorizontally.setMargin(true);
		juryVotingCenterHorizontally.setHeight("80%");
		juryVotingCenterHorizontally.getStyle().set("background-color", "black");

		juryVotingCenterHorizontally.add(juryVotingButtons);
		return;
	}

	private void buildRefereeBox(VerticalLayout container) {
		refereeLabelWrapper = createRefereeLabel(null);

		decisions = new JuryDisplayDecisionElement(false);
		decisions.getElement().setAttribute("theme", "dark");
		Div decisionWrapper = new Div(decisions);
		decisionWrapper.getStyle().set("width", "50%");
		decisionWrapper.getStyle().set("height", "max-content");

		refContainer = new HorizontalLayout(decisionWrapper);
		refContainer.setBoxSizing(BoxSizing.BORDER_BOX);
		refContainer.setJustifyContentMode(JustifyContentMode.CENTER);
		refContainer.getStyle().set("background-color", "black");
		refContainer.setHeight("80%");
		refContainer.setWidthFull();
		refContainer.setPadding(true);
		refContainer.setMargin(true);

		container.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		container.add(refereeLabelWrapper);
		container.setAlignSelf(Alignment.START, refereeLabelWrapper);
		container.add(refContainer);
	}

	private void checkAllVoted() {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			boolean allVoted = true;
			for (int i = 0; i < juryVotes.length; i++) {
				if (juryVotes[i] == null) {
					allVoted = false;
					break;
				}
			}

			if (allVoted) {
				juryVotingButtons.removeAll();
				for (int i = 0; i < juryVotes.length; i++) {
					Icon fullSizeIcon;
					if (juryVotes[i]) {
						fullSizeIcon = bigIcon(VaadinIcon.CHECK_CIRCLE, "white");
					} else {
						fullSizeIcon = bigIcon(VaadinIcon.CLOSE_CIRCLE, "red");
					}
					juryVotingButtons.add(fullSizeIcon);
					juryIcons[i] = fullSizeIcon;
				}
			}
		});
	}

	private Component createRefereeLabel(Athlete athlete) {
		Html refereeLabel = new Html("<span>" +
		        getTranslation("RefereeDecisions")
		        + (athlete != null
		                ? "&nbsp;&nbsp;&nbsp;" + athleteFullId(athlete) + "&nbsp;&nbsp;&nbsp;"
		                        + formatAttempt(athlete.getAttemptsDone() - 1)
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
		return athleteUnderReview;
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
		Button juryDeliberationButton = new Button(AvIcons.AV_TIMER.create(), (e) -> {
			openJuryDialog(JuryDeliberationEventType.START_DELIBERATION);
		});
		juryDeliberationButton.getElement().setAttribute("theme", "primary");
		juryDeliberationButton.setText(getTranslation("BreakButton.JuryDeliberation"));
		// juryDeliberationButton.getElement().setAttribute("title",
		// getTranslation("BreakButton.JuryDeliberation"));

		Button technicalPauseButton = new Button(AvIcons.AV_TIMER.create(), (e) -> {
			openJuryDialog(JuryDeliberationEventType.TECHNICAL_PAUSE);
		});
		technicalPauseButton.getElement().setAttribute("theme", "primary");
		technicalPauseButton.setText(getTranslation("BreakType.TECHNICAL"));

		HorizontalLayout buttons = new HorizontalLayout(juryDeliberationButton, technicalPauseButton);

		if (summonEnabled) {
			Button summonRefereesButton = new Button(AvIcons.AV_TIMER.create(), (e) -> {
				openJuryDialog(JuryDeliberationEventType.CALL_REFEREES);
			});
			summonRefereesButton.getElement().setAttribute("theme", "primary");
			summonRefereesButton.setText(getTranslation("BreakButton.SummonReferees"));
			buttons.add(summonRefereesButton);
		}

		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return buttons;
	}

	private void juryVote(Integer juryMember, Boolean goodBad, boolean sendFOPEvent) {
		if (goodBad == null) {
			Icon nonVotedIcon = bigIcon(VaadinIcon.CIRCLE_THIN, "gray");
			juryVotingButtons.replace(juryIcons[juryMember], nonVotedIcon);
			juryIcons[juryMember] = nonVotedIcon;
			juryVotes[juryMember] = null;
			return;
		}
		Icon votedIcon = bigIcon(VaadinIcon.CIRCLE, "gray");
		juryVotingButtons.replace(juryIcons[juryMember], votedIcon);
		juryIcons[juryMember] = votedIcon;
		juryVotes[juryMember] = goodBad;
		if (sendFOPEvent) {
			OwlcmsSession.withFop(fop -> {
				fop.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(this, juryMember, goodBad));
			});
		}
		checkAllVoted();
	}

	private void openJuryDialog(JuryDeliberationEventType deliberation) {
		long now = System.currentTimeMillis();
		if (now - lastOpen > 100 && (juryDialog == null || !juryDialog.isOpened())) {
			OwlcmsSession.withFop(fop -> {
				if (fop.getState() != FOPState.BREAK && deliberation != JuryDeliberationEventType.TECHNICAL_PAUSE) {
					fop.fopEventPost(
					        new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, this));
				}
				juryDialog = new JuryDialog(JuryContent.this, getAthleteUnderReview(), deliberation, summonEnabled);
				juryDialog.open();
				lastOpen = now;
			});
		}
	}

	private void resetJuryVoting() {
		//logger.debug("resetJuryVoting {} {}", UI.getCurrent(), LoggerUtils.whereFrom());
		for (ShortcutRegistration sr : registrations) {
			sr.remove();
		}
		juryIcons = new Icon[getNbJurors()];
		juryVotes = new Boolean[getNbJurors()];
		juryVotingButtons.removeAll();
		for (int i = 0; i < getNbJurors(); i++) {
			final int ix = i;
			Icon nonVotedIcon = bigIcon(VaadinIcon.CIRCLE_THIN, "gray");
			juryIcons[ix] = nonVotedIcon;
			juryVotes[ix] = null;
			juryVotingButtons.add(juryIcons[ix], nonVotedIcon);
			ShortcutRegistration reg;
			reg = UI.getCurrent().addShortcutListener(() -> {
				juryVote(ix, true, true);
			}, getGoodKey(i));
			registrations.add(reg);
			reg = UI.getCurrent().addShortcutListener(() -> {
				juryVote(ix, false, true);
			}, getBadKey(i));
			registrations.add(reg);
			reg = UI.getCurrent().addShortcutListener(
			        () -> openJuryDialog(JuryDeliberationEventType.START_DELIBERATION), Key.KEY_D);
			registrations.add(reg);
			reg = UI.getCurrent().addShortcutListener(
			        () -> openJuryDialog(JuryDeliberationEventType.TECHNICAL_PAUSE), Key.KEY_T);
			reg = UI.getCurrent().addShortcutListener(
			        () -> {
				        openJuryDialog(JuryDeliberationEventType.TECHNICAL_PAUSE);
				        summonReferee(4);
			        }, Key.KEY_C);
			registrations.add(reg);
			if (summonEnabled) {
				reg = UI.getCurrent().addShortcutListener(() -> summonReferee(1), Key.KEY_H);
				registrations.add(reg);
				reg = UI.getCurrent().addShortcutListener(() -> summonReferee(2), Key.KEY_I);
				registrations.add(reg);
				reg = UI.getCurrent().addShortcutListener(() -> summonReferee(3), Key.KEY_J);
				registrations.add(reg);
				reg = UI.getCurrent().addShortcutListener(() -> summonReferee(0), Key.KEY_K);
				registrations.add(reg);
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
		if (now - lastOpen > 100) {

			openJuryDialog(JuryDeliberationEventType.CALL_REFEREES);
			lastOpen = now;

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

	@SuppressWarnings("unused")
	private Component summonRefereeButtons() {
		Button one = new Button("1", (e) -> {
			OwlcmsSession.withFop(fop -> {
				this.summonReferee(1);
			});
		});
		one.setWidth("4em");

		Button two = new Button("2", (e) -> {
			OwlcmsSession.withFop(fop -> {
				this.summonReferee(2);
			});
		});
		two.setWidth("4em");

		Button three = new Button("3", (e) -> {
			OwlcmsSession.withFop(fop -> {
				this.summonReferee(3);
			});
		});
		three.setWidth("4em");

		Button all = new Button("*", (e) -> {
			OwlcmsSession.withFop(fop -> {
				this.summonReferee(0);
			});
		});
		all.setWidth("4em");

		HorizontalLayout selection = new HorizontalLayout(one, two, three, all);
		return selection;
	}

	private void swapRefereeLabel(Athlete athlete) {
		Component nc = createRefereeLabel(athlete);
		this.replace(refereeLabelWrapper, nc);
		refereeLabelWrapper = nc;
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
		topBarSettings = new MenuBar();
		topBarSettings.addThemeVariants(MenuBarVariant.LUMO_SMALL, MenuBarVariant.LUMO_TERTIARY_INLINE);
		MenuItem item2 = topBarSettings.addItem(IronIcons.SETTINGS.create());
		SubMenu subMenu2 = item2.getSubMenu();
		subMenu2.addItem(
		        this.isSilenced() ? Translator.translate("Settings.TurnOnSound")
		                : Translator.translate("Settings.TurnOffSound"),
		        e -> {
			        switchSoundMode(this, !this.isSilenced(), true);
			        e.getSource().setText(this.isSilenced() ? Translator.translate("Settings.TurnOnSound")
			                : Translator.translate("Settings.TurnOffSound"));
			        if (decisionDisplay != null) {
				        decisionDisplay.setSilenced(this.isSilenced());
			        }
			        if (decisions != null) {
				        decisions.setSilenced(this.isSilenced());
			        }
			        if (timer != null) {
				        timer.setSilenced(this.isSilenced());
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
		syncWithFOP(false);
		decisions.slaveDecisionReset(null);

		// OwlcmsSession.getFop().fopEventPost(new FOPEvent.StartLifting(this));
		if (decisionNotification != null) {
			decisionNotification.close();
		}
	}

	@Override
	protected void init() {
		setNbJurors(Competition.getCurrent().getJurySize());
	}

	protected void init(int nbj) {
		// logger.trace("init {}", LoggerUtils.whereFrom());
		summonEnabled = true; // works with phones/tablets
		registrations = new ArrayList<>();
		this.setBoxSizing(BoxSizing.BORDER_BOX);
		this.setSizeFull();
		Competition.getCurrent().setJurySize(nbj);
		buildJuryBox(this);
		buildRefereeBox(this);
	}

	@Override
	protected void syncWithFOP(boolean refreshGrid) {
		super.syncWithFOP(refreshGrid);
		OwlcmsSession.withFop(fop -> {
			setAthleteUnderReview(fop.getAthleteUnderReview());
			Boolean[] curDecisions = fop.getJuryMemberDecision();
			if (curDecisions != null) {
				for (int i = 0; i < getNbJurors(); i++) {
					Boolean goodBad = curDecisions[i];
//                    logger.debug("existing jury {} {}", i, goodBad);
					juryVote(i, goodBad, false);
				}
			}
			Boolean[] curRefDecisions = fop.getRefereeDecision();
			Long[] curRefTimes = fop.getRefereeTime();
			decisions.doReset();
			if (curRefDecisions != null) {
//                for (int i = 0; i < 3; i++) {
//                    Boolean goodBad = curRefDecisions[i];
//                    logger.debug("existing ref {} {}", i, goodBad);
//                }
				if (fop.isRefereeForcedDecision()) {
					decisions.slaveRefereeUpdate(new UIEvent.RefereeUpdate(athleteUnderReview, null,
					        curRefDecisions[1], null, null, curRefTimes[1], null, this));
				} else {
					decisions.slaveRefereeUpdate(new UIEvent.RefereeUpdate(athleteUnderReview, curRefDecisions[0],
					        curRefDecisions[1], curRefDecisions[2], curRefTimes[0], curRefTimes[1], curRefTimes[2],
					        this));
				}
			}
		});
	}
}
