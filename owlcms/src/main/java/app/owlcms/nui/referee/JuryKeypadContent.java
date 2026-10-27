/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.referee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import app.owlcms.access.RequiresRole;
import app.owlcms.access.AccessUi;
import app.owlcms.access.Role;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.NotificationUtils;
import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.apputils.queryparameters.FOPParametersReader;
import app.owlcms.components.elements.JuryDisplayDecisionElement;
import app.owlcms.components.elements.PassiveTimerElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.config.TcrrCompliance;
import app.owlcms.displays.DecisionDisplayRules;
import app.owlcms.fieldofplay.AdjudicationPanel;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.IProxyTimer;
import app.owlcms.fieldofplay.InputKind;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.JuryRejectionReasons;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings({ "serial", "deprecation" })
@RequiresRole(value = Role.JURY, platformBound = true)
@Route(value = "jurykeypad")
@CssImport(value = "./styles/shared-styles.css")
public class JuryKeypadContent extends BaseContent implements FOPParametersReader, SafeEventBusRegistration,
        UIEventProcessor, HasDynamicTitle, BeforeEnterListener {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(JuryKeypadContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	private static final String BOX_MODE = "box";

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	private JuryDisplayDecisionElement liveDecisions;
	private Icon[] juryIcons;
	private Boolean[] juryVotes;
	private Div[] juryVoteCells;
	private Div refDecisionHost;
	/** seven-person panel: the original referee decision, shown in the toolbar once the review vote is revealed */
	private Div originalRefereeDecisions;
	/** hidden while the original referee decision is shown in their place */
	private ComboBox<FieldOfPlay> fopSelect;
	private Select<Integer> jurySizeSelect;
	private Div reviewAttempt;
	private Div reviewWeight;
	private Div reviewFiller;
	private boolean reviewFrozen;
	private Div reviewHeader;
	private Div reviewStartNumber;
	private Div reviewName;
	private PassiveTimerElement reviewTimer;
	private Div reviewTimerFrozen;
	private Button noLiftButton;
	private Button goodLiftButton;
	private Button deliberationButton;
	private Button challengeButton;
	/**
	 * seven-person Adjudication Panel with VPT: Call for Votes; during a review the Deliberation and Challenge buttons
	 * become the Jury Chair's No Lift and Good Lift
	 */
	private boolean panelMode;
	private Button callForVotesButton;
	private Button allSummonButton;
	private Dialog noLiftReasonDialog;
	/** true when the open rule dialog is for the No Lift result of a vote (its Cancel cancels the vote result) */
	private boolean noLiftReasonForVote;
	private Dialog confirmGoodLiftDialog;
	/**
	 * Testing aid ({@code ?box=true}): the page acts as the physical jury box. Good Lift / No Lift post the same events
	 * as the box (No Lift asks the other Jury Control pages for the rule), no jury dialog is shown, Call for Votes is
	 * disabled and Summon Referees (all) keeps the box's meaning.
	 */
	private boolean boxMode;
	private Location location;
	private UI locationUI;
	private EventBus uiEventBus;
	private Map<String, List<String>> urlParams;
	private final List<ShortcutRegistration> registrations = new ArrayList<>();
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	public JuryKeypadContent() {
		OwlcmsFactory.waitDBInitialized();
		init();
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		UI.getCurrent().getPage().setTitle(getPageTitle());
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("Jury_Keypad") + FieldOfPlay.getFopNameIfMultiple(getFop());
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		this.location = event.getLocation();
		this.locationUI = event.getUI();
		QueryParameters queryParameters = this.location.getQueryParameters();
		Map<String, List<String>> parametersMap = queryParameters.getParameters();
		List<String> boxValues = parametersMap.get(BOX_MODE);
		this.boxMode = boxValues != null && !boxValues.isEmpty() && Boolean.parseBoolean(boxValues.get(0));
		this.urlParams = readParams(this.location, parametersMap);
		rebuild();
	}

	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			updateJuryDecisionActions(e.getFop());
			BreakType bt = e.getBreakType();
			if (bt != BreakType.JURY && bt != BreakType.CHALLENGE) {
				clearReviewHeader();
			}
			getUI().ifPresent(ui -> ui.beforeClientResponse(this,
			        ignored -> syncLiveRefereeDecisions(e.getFop())));
		});
	}

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			setJuryDecisionActionsEnabled(false);
			getUI().ifPresent(ui -> ui.beforeClientResponse(this,
			        ignored -> syncLiveRefereeDecisions(e.getFop())));
		});
	}

	@Subscribe
	public void slaveJuryNotification(UIEvent.JuryNotification e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			JuryDeliberationEventType deliberationType = e.getDeliberationEventType();
			// with the seven-person panel, or after an automatic stop, the review keeps the jury votes on the lift
			FieldOfPlay notificationFop = e.getFop();
			boolean votesKept = notificationFop != null && notificationFop.getPanelReviewInitialVotes() != null;
			if ((deliberationType == JuryDeliberationEventType.START_DELIBERATION
			        || deliberationType == JuryDeliberationEventType.CHALLENGE) && !votesKept) {
				resetJuryVoting();
			}
			updateJuryDecisionActions(e.getFop());
			updateReviewHeader(e.getFop());
			// the decision has been given, possibly from another Jury Control page or the jury box
			syncJuryDialogs(e.getFop());
		});
	}

	@Subscribe
	public void slaveJuryRuleRequired(UIEvent.JuryRuleRequired e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> syncJuryDialogs(e.getFop()));
	}

	@Subscribe
	public void slavePanelVoteCalled(UIEvent.PanelVoteCalled e) {
		// a new review vote: the jury votes again
		if (e.getRef() == 0) {
			UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
				resetJuryVoting();
				hideOriginalRefereeDecisions();
				updateJuryDecisionActions(getFop());
				syncJuryDialogs(getFop());
			});
		}
	}

	@Subscribe
	public void slavePanelVoteUpdate(UIEvent.PanelVoteUpdate e) {
		if (!this.panelMode) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			renderPanelVote(e.getVoted(), e.getVotes());
			if (e.isComplete()) {
				showOriginalRefereeDecisions(e.getFop());
			}
			updateJuryDecisionActions(getFop());
			// the result of the vote waits for the Chair: Confirm Good Lift, or the rule for a No Lift
			syncJuryDialogs(getFop());
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		// the jury votes are kept when the referee lights are cleared; like the FOP, they are cleared on clock start
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			updateJuryDecisionActions(getFop());
			freezeReviewTimer();
		});
	}

	@Subscribe
	public void slaveShowDecision(UIEvent.Decision e) {
		// Decision is now displayed on the attempt board.  Freeze the header so
		// the live timer does not pick up the next athlete's SetTime event
		// (particularly important for announcer/solo decisions where clockOwner
		// is cleared before the decision is shown).
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			freezeReviewTimer();
			// the referees' decision is known: Challenge may become available
			updateJuryDecisionActions(getFop());
		});
	}

	@Subscribe
	public void slaveResetOnNewClock(UIEvent.ResetOnNewClock e) {
		// Header stays visible until next clock start (mirrors JuryDisplayDecisionElement lifecycle)
	}

	@Subscribe
	public void slaveJurySizeChanged(UIEvent.JurySizeChanged e) {
		// another page (or the competition settings) changed the number of jurors on this platform
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			FieldOfPlay fop = getFop();
			boolean panelMode = fop != null && fop.hasJuryReviewVote();
			int shown = this.juryVoteCells != null ? this.juryVoteCells.length : -1;
			if (shown != getNbJurors() || panelMode != this.panelMode) {
				rebuild();
			}
		});
	}

	@Subscribe
	public void slaveJuryDisplayReset(UIEvent.JuryDisplayReset e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			resetJuryVoting();
			hideOriginalRefereeDecisions();
			clearReviewHeader();
			// Cancel vote result clears the displays while the review goes on
			updateReviewHeader(getFop());
			updateJuryDecisionActions(getFop());
			syncJuryDialogs(getFop());
		});
	}

	@Subscribe
	public void slaveJuryMemberDecision(UIEvent.JuryUpdate e) {
		FieldOfPlay fop = getFop();
		if (this.panelMode && fop != null && fop.isPanelVoteOpen()) {
			// the review vote is rendered from PanelVoteUpdate (hidden until all seven are in)
			return;
		}
		Boolean[] decision = e.getJuryMemberDecision();
		Integer juryMember = e.getJuryMemberUpdated();
		Boolean goodBad = juryMember != null ? decision[juryMember] : null;
		// the summon prompt and Challenge depend on all the votes, including this page's own
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> updateJuryDecisionActions(fop));
		if (juryMember != null && goodBad != null) {
			UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this, () -> {
				juryVote(juryMember, goodBad, false);
			});
		}
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			closeJuryDialogs();
			updateJuryDecisionActions(getFop());
		});
	}

	@Subscribe
	public void slaveTimeStarted(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			resetJuryVoting();
			hideOriginalRefereeDecisions();
			updateJuryDecisionActions(getFop());
			clearReviewHeader();
			updateReviewHeader(e.getFop());
			if (this.reviewTimer != null) {
				this.reviewTimer.start(e.getTimeRemaining(), e.getStart());
			}
		});
	}

	@Subscribe
	public void slaveStopTimer(UIEvent.StopTime e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			if (this.reviewTimer != null) {
				this.reviewTimer.pause(e.getTimeRemaining());
			}
		});
	}

	@Subscribe
	public void slaveSetTimer(UIEvent.SetTime e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			if (this.reviewTimer != null) {
				this.reviewTimer.display(e.getTimeRemaining());
			}
		});
	}

	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		if (!e.isTimerStateValid()) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			if (this.reviewTimer != null) {
				this.reviewTimer.applyState(e.isTimerShouldRun(), e.getTimerMillisRemaining());
			}
		});
	}

	protected ComboBox<FieldOfPlay> createFopSelect() {
		ComboBox<FieldOfPlay> fopSelect = new ComboBox<>();
		List<FieldOfPlay> fops = AccessUi.selectableFops(Set.of(Role.JURY));
		fopSelect.setPlaceholder(Translator.translate("SelectPlatform"));
		fopSelect.setItems(fops);
		fopSelect.setItemLabelGenerator(FieldOfPlay::getName);
		fopSelect.setWidth("12rem");
		fopSelect.setReadOnly(AccessUi.fopSelectorReadOnly(Set.of(Role.JURY), fops));
		return fopSelect;
	}

	/** 0 / 3 / 4 / 5 jury member selection; rebuilds the keypad when changed. */
	private Select<Integer> createJurySizeSelect() {
		Select<Integer> jurySizeSelect = new Select<>();
		jurySizeSelect.setItems(0, 3, 4, 5);
		jurySizeSelect.setAriaLabel(Translator.translate("Jury"));
		jurySizeSelect.setWidth("5.5rem");
		FieldOfPlay currentFop = getFop();
		// the platform's own setting, or the competition default when it has none
		jurySizeSelect.setValue(
		        currentFop != null ? currentFop.getJurySize() : Competition.getCurrent().getEffectiveJurySize());
		jurySizeSelect.addValueChangeListener((e) -> {
			if (!e.isFromClient() || e.getValue() == null) {
				return;
			}
			FieldOfPlay fop = getFop();
			if (fop == null) {
				return;
			}
			fop.fopEventPost(new FOPEvent.JurySizeChange(this, e.getValue()));
			rebuild();
		});
		return jurySizeSelect;
	}

	/** Rebuild the keypad layout (used when the number of jurors changes). */
	private void rebuild() {
		this.removeAll();
		buildContent(this);
		registerShortcuts();
		FieldOfPlay fop = getFop();
		attachLiveDecisions(fop);
		syncWithFopState(fop);
	}

	protected void init() {
		this.setBoxSizing(BoxSizing.BORDER_BOX);
		this.setSizeFull();
		// jury keypads are used in darkened venues: black page, Lumo dark palette.
		// The theme is set on this component rather than on the document, so that it does not
		// leak to the other pages visited in the same browser tab.
		this.addClassName("refereeJuryDevice");
		this.getElement().getThemeList().add(Lumo.DARK);
		buildContent(this);
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		registerShortcuts();
		OwlcmsSession.withFop(fop -> {
			setFop(fop);
			this.uiEventBus = uiEventBusRegister(this, fop);
			attachLiveDecisions(fop);
			syncWithFopState(fop);
		});
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		clearShortcutRegistrations();
		super.onDetach(detachEvent);
	}

	private Icon bigIcon(VaadinIcon iconDef, String color) {
		Icon icon = iconDef.create();
		icon.setSize("min(10rem, 18vh, 13vw)");
		icon.getStyle().set("color", color);
		return icon;
	}

	private Key getBadKey(int index) {
		switch (index) {
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

	private Key getGoodKey(int index) {
		switch (index) {
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

	private void attachLiveDecisions(FieldOfPlay fop) {
		if (fop == null || this.refDecisionHost == null) {
			return;
		}
		this.refDecisionHost.removeAll();
		this.liveDecisions = new JuryDisplayDecisionElement();
		this.liveDecisions.setFop(fop);
		this.liveDecisions.setDisplaySize("large");
		this.liveDecisions.getStyle().set("--attemptFontSize", "18vh");
		this.liveDecisions.getStyle().set("--soloDecisionSize", "14vh");
		this.liveDecisions.setSilenced(true);
		// the jury members are shown in their own row on this page
		this.liveDecisions.setHidePanelJury(true);
		this.liveDecisions.getElement().setAttribute("theme", "dark");
		this.liveDecisions.getStyle().set("background-color", "black");
		this.liveDecisions.getStyle().set("font-size", "100%");
		this.liveDecisions.getStyle().set("max-height", "100%");
		this.liveDecisions.getStyle().set("overflow", "hidden");
		Div refDecisionWrapper = new Div(this.liveDecisions);
		refDecisionWrapper.getStyle().set("width", "60%").set("height", "100%").set("overflow", "hidden");
		this.refDecisionHost.add(refDecisionWrapper);
	}

	private void buildContent(VerticalLayout container) {
		container.setPadding(false);
		container.setSpacing(false);
		container.setMargin(false);
		container.setSizeFull();

		// CSS grid matching physical keypad layout
		// 5 columns: [All Refs / Deliberation] [Left Ref / Challenge] [Center Ref / No Lift] [Right Ref / Good Lift]
		// [Technical / Resume]
		// 8 rows: toolbar | ref-label | ref-LEDs | call-btns | spacer | jury-label | jury-LEDs | action-btns
		Div grid = new Div();
		grid.setSizeFull();
		grid.getStyle()
		        .set("display", "grid")
		        .set("grid-template-columns", "repeat(5, 1fr)")
		        .set("grid-template-rows", "auto auto 22vh 12vh 2vh auto 22vh 12vh")
		        .set("gap", "4px")
		        .set("background", "black")
		        .set("padding", "0.5rem")
		        .set("box-sizing", "border-box")
		        .set("max-height", "100vh")
		        .set("overflow", "hidden");

		// --- Row 1: Toolbar ---
		HorizontalLayout toolbar = new HorizontalLayout();
		toolbar.setAlignItems(Alignment.CENTER);
		toolbar.setWidthFull();
		Div title = buildReviewHeader();
		ComboBox<FieldOfPlay> fopSelect = createFopSelect();
		fopSelect.setValue(OwlcmsSession.getFop());
		fopSelect.addValueChangeListener((e) -> {
			setFop(e.getValue());
			OwlcmsSession.setFop(e.getValue());
			this.uiEventBus = uiEventBusRegister(this, e.getValue());
			rebuild();
			if (this.location != null && this.locationUI != null) {
				Location location2 = new Location(this.location.getPath(), new QueryParameters(this.urlParams));
				URLUtils.replaceState(this.locationUI.getPage().getHistory(), null, location2, this.location);
			}
		});
		toolbar.add(title, buildOriginalRefereeDecisions(), fopSelect);
		this.fopSelect = fopSelect;
		this.jurySizeSelect = createJurySizeSelect();
		this.jurySizeSelect.setVisible(!TcrrCompliance.isEnabled());
		toolbar.add(this.jurySizeSelect);
		toolbar.expand(title);
		toolbar.getStyle().set("grid-column", "1 / -1").set("grid-row", "1");

		// --- Row 2: Referee Decisions label ---
		Div refLabel = new Div();
		refLabel.setText(Translator.translate("RefereeDecisions"));
		refLabel.getStyle()
		        .set("grid-column", "1 / -1").set("grid-row", "2")
		        .set("background", "black")
		        .set("color", "var(--lumo-secondary-text-color)")
		        .set("display", "flex")
		        .set("align-items", "flex-end")
		        .set("font-size", "0.85rem")
		        .set("font-weight", "bold")
		        .set("padding", "0 0 0.25rem 0.5rem")
		        .set("text-transform", "uppercase")
		        .set("letter-spacing", "0.05em");

		// --- Row 3: Referee decision display (tall, above summon buttons) ---
		this.refDecisionHost = new Div();
		this.refDecisionHost.setSizeFull();
		this.refDecisionHost.getStyle()
		        .set("display", "flex")
		        .set("align-items", "flex-end")
		        .set("justify-content", "center");
		Div refDecisionArea = new Div(this.refDecisionHost);
		refDecisionArea.setSizeFull();
		refDecisionArea.getStyle()
		        .set("background", "black")
		        .set("display", "flex")
		        .set("align-items", "flex-end")
		        .set("justify-content", "center")
		        .set("grid-column", "1 / -1")
		        .set("grid-row", "3")
		        .set("overflow", "hidden");

		// --- Row 4: calls to the officials: referees, and the technical controller (technical issue) ---
		Button allSummon = createKeypadButton(Translator.translate("JuryKeypad.SummonReferees"), "contrast", () -> summonReferee(0));
		styleSummonButton(allSummon);
		this.allSummonButton = allSummon;
		allSummon.getStyle().set("grid-column", "1").set("grid-row", "4");
		Button leftSummon = createKeypadButton(Translator.translate("JuryKeypad.LeftRefereeSummon"), "contrast", () -> summonReferee(1));
		styleSummonButton(leftSummon);
		leftSummon.getStyle().set("grid-column", "2").set("grid-row", "4");
		Button centerSummon = createKeypadButton(Translator.translate("JuryKeypad.CenterRefereeSummon"), "contrast", () -> summonReferee(2));
		styleSummonButton(centerSummon);
		centerSummon.getStyle().set("grid-column", "3").set("grid-row", "4");
		Button rightSummon = createKeypadButton(Translator.translate("JuryKeypad.RightRefereeSummon"), "contrast", () -> summonReferee(3));
		styleSummonButton(rightSummon);
		rightSummon.getStyle().set("grid-column", "4").set("grid-row", "4");
		Button technical = createKeypadButton(Translator.translate("BreakType.TECHNICAL"), "primary", this::startTechnicalBreak);
		technical.getStyle().set("grid-column", "5").set("grid-row", "4");

		// --- Row 5: Spacer ---
		Div spacer = new Div();
		spacer.getStyle().set("grid-column", "1 / -1").set("grid-row", "5");

		// --- Row 6: Jury Decisions label ---
		FieldOfPlay buildFop = getFop();
		// review vote, Call for Votes and the Chair's confirmation for any jury (3, 4 or 5 members)
		this.panelMode = buildFop != null && buildFop.hasJuryReviewVote();
		Div juryLabel = new Div();
		juryLabel.setText(Translator.translate("JuryDecisions"));
		juryLabel.getStyle()
		        .set("grid-column", "1 / -1").set("grid-row", "6")
		        .set("background", "black")
		        .set("color", "var(--lumo-secondary-text-color)")
		        .set("display", "flex")
		        .set("align-items", "flex-end")
		        .set("font-size", "0.85rem")
		        .set("font-weight", "bold")
		        .set("padding", "0 0 0.25rem 0.5rem")
		        .set("text-transform", "uppercase")
		        .set("letter-spacing", "0.05em");

		// --- Row 7: Jury voting indicators (tall, above action buttons) ---
		// 3 jurors sit under columns 2..4, exactly under the referee boxes. 4 jurors are squeezed in the same span, so
		// that they do not line up with the buttons below. 5 jurors span the full width.
		int nbJurors = getNbJurors();
		this.juryVoteCells = new Div[nbJurors];
		this.juryIcons = new Icon[nbJurors];
		this.juryVotes = new Boolean[nbJurors];
		boolean squeezed = nbJurors == 3 || nbJurors == 4;
		int sideColumns = !this.panelMode && !squeezed && nbJurors < 5 ? 1 : 0;
		Div juryRow = new Div();
		juryRow.getStyle()
		        .set("grid-column", squeezed ? "2 / 5" : "1 / -1")
		        .set("grid-row", "7")
		        .set("display", "grid")
		        .set("grid-template-columns",
		                this.panelMode || squeezed ? "repeat(" + nbJurors + ", 1fr)" : juryRowColumns(nbJurors))
		        .set("gap", "4px")
		        .set("background", "black");
		for (int i = 0; i < nbJurors; i++) {
			Div cell = new Div();
			cell.getStyle()
			        .set("background", "black")
			        .set("display", "flex")
			        .set("align-items", "center")
			        .set("justify-content", "center")
			        .set("grid-column", String.valueOf(i + 1 + sideColumns))
			        .set("grid-row", "1");
			Icon nonVotedIcon = bigIcon(VaadinIcon.CIRCLE_THIN, "gray");
			cell.add(nonVotedIcon);
			this.juryVoteCells[i] = cell;
			this.juryIcons[i] = nonVotedIcon;
			this.juryVotes[i] = null;
		}

		// --- Row 8: Action buttons ---
		Button deliberate = createKeypadButton(Translator.translate("BreakButton.JuryDeliberation"), "primary contrast", this::startDeliberation);
		deliberate.getStyle().set("grid-column", "1").set("grid-row", "8")
		        .set("background-color", "#FFC107").set("color", "#000000");
		Button challenge = createKeypadButton(Translator.translate("BreakButton.CHALLENGE"), "primary contrast", this::startChallenge);
		challenge.getStyle().set("grid-column", "2").set("grid-row", "8")
		        .set("background-color", "#FFC107").set("color", "#000000");
		Button resumeBtn = createKeypadButton(Translator.translate("JuryNotification.END_JURY_BREAK"), "success primary", this::resumeCompetition);
		resumeBtn.getStyle().set("grid-column", "5").set("grid-row", "8");
		this.deliberationButton = deliberate;
		this.challengeButton = challenge;
		// the Good Lift / No Lift buttons are the Jury Chair entering the decision instead of a jury vote
		this.noLiftButton = createKeypadButton(Translator.translate("JuryDialog.BadLiftLabel"), "error primary",
		        this::chairNoLift);
		this.goodLiftButton = createKeypadButton(Translator.translate("JuryDialog.GoodLiftLabel"),
		        "success primary", () -> submitJuryDecision(true, null, true));

		// Add all elements to grid
		grid.add(toolbar, refLabel, refDecisionArea,
		        leftSummon, centerSummon, rightSummon, allSummon, technical,
		        spacer, juryLabel);
		for (int i = 0; i < this.juryVoteCells.length; i++) {
			if (this.panelMode) {
				// per-member class (panel-jury-1 is the Jury Chair) for later styling
				this.juryVoteCells[i].addClassNames("panel-jury", "panel-jury-" + (i + 1));
			}
			juryRow.add(this.juryVoteCells[i]);
		}
		if (this.panelMode) {
			this.callForVotesButton = createKeypadButton(Translator.translate("JuryKeypad.CallForVotes"), "primary",
			        this::callForVotes);
			// four equal buttons across the full width. During a review, the Jury Chair's No Lift and Good Lift take
			// the place of Deliberation and Challenge.
			Div actionRow = new Div();
			actionRow.getStyle()
			        .set("grid-column", "1 / -1")
			        .set("grid-row", "8")
			        .set("display", "grid")
			        .set("grid-template-columns", "repeat(4, 1fr)")
			        .set("gap", "4px");
			deliberate.getStyle().set("grid-column", "1").remove("grid-row");
			this.noLiftButton.getStyle().set("grid-column", "1");
			challenge.getStyle().set("grid-column", "2").remove("grid-row");
			this.goodLiftButton.getStyle().set("grid-column", "2");
			this.callForVotesButton.getStyle().set("grid-column", "3");
			resumeBtn.getStyle().set("grid-column", "4").remove("grid-row");
			actionRow.add(deliberate, this.noLiftButton, challenge, this.goodLiftButton, this.callForVotesButton,
			        resumeBtn);
			grid.add(juryRow, actionRow);
		} else {
			this.callForVotesButton = null;
			this.noLiftButton.getStyle().set("grid-column", "3").set("grid-row", "8");
			this.goodLiftButton.getStyle().set("grid-column", "4").set("grid-row", "8");
			grid.add(juryRow, this.noLiftButton, this.goodLiftButton, deliberate, challenge, resumeBtn);
		}
		updateJuryDecisionActions(getFop());

		container.add(grid);
		container.setFlexGrow(1, grid);
	}

	/**
	 * Juror columns as wide as the five button columns, with the remaining space split evenly on each side.
	 */
	private static String juryRowColumns(int nbJurors) {
		if (nbJurors >= 5) {
			return "repeat(" + nbJurors + ", 1fr)";
		}
		String side = ((5 - nbJurors) / 2.0) + "fr";
		return side + " repeat(" + nbJurors + ", 1fr) " + side;
	}

	private void checkAllVoted() {
		FieldOfPlay fop = getFop();
		if (fop != null && fop.isPanelVoteOpen()) {
			// review vote: the jury votes are revealed with the referees' when all seven are in
			return;
		}
		boolean allVoted = true;
		for (Boolean juryVote : this.juryVotes) {
			if (juryVote == null) {
				allVoted = false;
				break;
			}
		}

		if (allVoted) {
			for (int i = 0; i < this.juryVotes.length; i++) {
				Icon fullSizeIcon = this.juryVotes[i]
				        ? bigIcon(VaadinIcon.CHECK_CIRCLE, "white")
				        : bigIcon(VaadinIcon.CLOSE_CIRCLE, "red");
				this.juryVoteCells[i].removeAll();
				this.juryVoteCells[i].add(fullSizeIcon);
				this.juryIcons[i] = fullSizeIcon;
			}
		}
	}

	/** not voted: thin ring; voted (hidden): grey dot; revealed: white or red */
	private void renderPanelCell(Div cell, boolean voted, Boolean vote) {
		Icon icon;
		if (vote != null) {
			icon = vote ? bigIcon(VaadinIcon.CHECK_CIRCLE, "white") : bigIcon(VaadinIcon.CLOSE_CIRCLE, "red");
		} else {
			icon = bigIcon(voted ? VaadinIcon.CIRCLE : VaadinIcon.CIRCLE_THIN, "gray");
		}
		cell.removeAll();
		cell.add(icon);
	}

	/**
	 * Render the jury members' part of the review vote on the jury circles; the referees' part is shown by the
	 * referee decision display.
	 *
	 * @param voted referees 1..3 then jury members 1..4
	 * @param votes same order, null until all seven are in
	 */
	private void renderPanelVote(boolean[] voted, Boolean[] votes) {
		int nbRefs = AdjudicationPanel.NB_REFEREES;
		for (int i = 0; this.juryVoteCells != null && i < this.juryVoteCells.length; i++) {
			if (nbRefs + i < voted.length) {
				renderPanelCell(this.juryVoteCells[i], voted[nbRefs + i], votes != null ? votes[nbRefs + i] : null);
			}
		}
	}

	private void callForVotes() {
		OwlcmsSession.withFop(fop -> {
			if (fop.isPanelVoteCallable()) {
				fop.fopEventPost(new FOPEvent.PanelVoteCalled(this));
			}
		});
	}

	private void updateCallForVotesButton(FieldOfPlay fop) {
		updateSummonAllButton(fop);
		if (this.callForVotesButton == null) {
			return;
		}
		// the physical jury box has no Call for Votes button (its Summon All key calls the vote)
		boolean enabled = !this.boxMode && fop != null && fop.isPanelVoteCallable();
		// prompt the Chair in every review until the vote is called, and again after Cancel vote result
		boolean prompt = enabled && !fop.isPanelVoteOpen();
		this.callForVotesButton.setEnabled(enabled);
		this.callForVotesButton.getStyle()
		        .set("background-color", prompt ? "#f97316" : (enabled ? "#2563eb" : "#1e293b"))
		        .set("background-image", "none")
		        .set("color", enabled ? "#ffffff" : "#64748b")
		        .set("border", prompt ? "3px solid #ffffff" : "1px solid #51565c");
	}

	private Button createKeypadButton(String label, String theme, Runnable action) {
		Button button = new Button(label, e -> action.run());
		button.addClassName("jury-keypad-button");
		button.addThemeVariants(ButtonVariant.LUMO_LARGE);
		button.getElement().setAttribute("theme", theme);
		button.getStyle().set("width", "calc(100% - 6px)");
		button.getStyle().set("height", "calc(100% - 6px)");
		button.getStyle().set("place-self", "center");
		button.getStyle().set("white-space", "normal");
		button.getStyle().set("text-align", "center");
		button.getStyle().set("font-size", "clamp(0.7rem, 1.6vw, 1rem)");
		return button;
	}

	private void styleSummonButton(Button button) {
		button.getStyle()
		        .set("background-color", button.isEnabled() ? "#2f343a" : "#1e293b")
		        .set("background-image", "none")
		        .set("color", button.isEnabled() ? "#ffffff" : "#64748b")
		        .set("border", "1px solid #51565c");
	}

	/**
	 * Without the seven-person panel (the referees are called at the Chair's discretion): when the jury votes on the
	 * lift, counted with the jury's rule, would reverse the referees, prompt the Chair to summon the referees, in the
	 * same orange as Call for Votes. Back to normal once the deliberation has started, whether the Chair opened it or
	 * the automatic break did.
	 */
	private void updateSummonAllButton(FieldOfPlay fop) {
		if (this.allSummonButton == null) {
			return;
		}
		// on the screen, Summon Referees (all) always means summon: when the keypad's Summon All means Vote, the button
		// is disabled and the orange Call for Votes is used instead (the box page keeps the box's meaning)
		this.allSummonButton.setEnabled(this.boxMode || fop == null || !fop.isSummonAllVote());
		boolean prompt = fop != null && fop.getState() != FOPState.BREAK && fop.isJuryVoteAgainstReferees();
		if (prompt) {
			this.allSummonButton.getStyle()
			        .set("background-color", "#f97316")
			        .set("border", "3px solid #ffffff");
		} else {
			styleSummonButton(this.allSummonButton);
		}
	}

	private int getNbJurors() {
		FieldOfPlay fop = getFop();
		return fop != null ? fop.getJurySize() : Competition.getCurrent().getEffectiveJurySize();
	}

	private void clearShortcutRegistrations() {
		for (ShortcutRegistration registration : this.registrations) {
			registration.remove();
		}
		this.registrations.clear();
	}

	private boolean hasRefereeDecision(Boolean[] decisions) {
		if (decisions == null) {
			return false;
		}
		for (Boolean decision : decisions) {
			if (decision != null) {
				return true;
			}
		}
		return false;
	}

	private void postJuryMemberDecision(int juryMember, boolean goodBad) {
		OwlcmsSession.withFop(fop -> {
			if (juryMember >= getNbJurors()) {
				return;
			}
			fop.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(this, juryMember, goodBad));
		});
	}

	private void registerShortcut(Runnable action, Key key) {
		UI currentUi = UI.getCurrent();
		if (currentUi == null || key == Key.UNIDENTIFIED) {
			return;
		}
		this.registrations.add(currentUi.addShortcutListener(action::run, key));
	}

	private void registerShortcuts() {
		clearShortcutRegistrations();
		for (int index = 0; index < getNbJurors(); index++) {
			final int juryMember = index;
			registerShortcut(() -> postJuryMemberDecision(juryMember, true), getGoodKey(index));
			registerShortcut(() -> postJuryMemberDecision(juryMember, false), getBadKey(index));
		}
		if (FeatureSwitch.NO_JURY_ALPHABETICAL_SHORTCUTS.isEffective()) {
			// a stray letter typed on the Jury Control computer must not start a break or summon the referees;
			// the digit shortcuts used by USB jury keypads remain
			return;
		}
		// the keys of the Jury Chair keypads documented for the Jury Console are kept; what they do depends on the
		// phase, as the Console's deliberation dialog did: see docs/100-AdvancedTopics/180-Keypads.md
		registerShortcut(this::startDeliberation, Key.KEY_D);
		registerShortcut(() -> {
			if (isReviewInProgress()) {
				// during a review "c" calls the Technical Controller (loading error...)
				summonReferee(4);
			} else {
				startChallenge();
			}
		}, Key.KEY_C);
		registerShortcut(this::startTechnicalBreak, Key.KEY_T);
		registerShortcut(() -> summonReferee(1), Key.KEY_H);
		registerShortcut(() -> summonReferee(2), Key.KEY_I);
		registerShortcut(() -> summonReferee(3), Key.KEY_J);
		// Summon All: the FOP makes it Call for Votes when the vote is called that way
		registerShortcut(() -> summonReferee(0), Key.KEY_K);
		// the Chair's decision during a review; "b" asks for the rule like the No Lift button
		registerShortcut(() -> {
			if (isReviewInProgress()) {
				submitJuryDecision(true, null, true);
			}
		}, Key.KEY_G);
		registerShortcut(() -> {
			if (isReviewInProgress()) {
				chairNoLift();
			}
		}, Key.KEY_B);
		// Escape ends the jury break, as it closed the Console's deliberation dialog
		registerShortcut(() -> {
			if (isReviewInProgress()) {
				resumeCompetition();
			}
		}, Key.ESCAPE);
	}

	private boolean isReviewInProgress() {
		FieldOfPlay fop = getFop();
		return isJuryDecisionActive(fop);
	}

	private void syncWithFopState(FieldOfPlay fop) {
		if (fop == null) {
			return;
		}

		syncReviewTimer(fop);
		updateReviewHeader(fop);
		updateJuryDecisionActions(fop);
		resetJuryVoting();
		Boolean[] curJuryDecisions = fop.getJuryMemberDecision();
		if (curJuryDecisions != null) {
			for (int i = 0; i < Math.min(curJuryDecisions.length, this.juryVotes.length); i++) {
				if (curJuryDecisions[i] != null) {
					juryVote(i, curJuryDecisions[i], false);
				}
			}
		}

		syncLiveRefereeDecisions(fop);
		syncJuryDialogs(fop);
		if (this.panelMode && fop.isPanelVoteOpen()) {
			UIEvent.PanelVoteUpdate vote = fop.getPanelVoteUpdate(this);
			renderPanelVote(vote.getVoted(), vote.getVotes());
		}
		if (fop.getPanelVoteUpdate(this).isComplete()) {
			showOriginalRefereeDecisions(fop);
		} else {
			hideOriginalRefereeDecisions();
		}
	}

	private Div buildOriginalRefereeDecisions() {
		this.originalRefereeDecisions = new Div();
		this.originalRefereeDecisions.setId("originalRefereeDecisions");
		this.originalRefereeDecisions.setTitle(Translator.translate("RefereeDecisions"));
		this.originalRefereeDecisions.getStyle()
		        .set("display", "flex")
		        .set("flex-shrink", "0")
		        .set("gap", "0.5rem");
		this.originalRefereeDecisions.setVisible(false);
		return this.originalRefereeDecisions;
	}

	private void showOriginalRefereeDecisions(FieldOfPlay fop) {
		if (this.originalRefereeDecisions == null || fop == null) {
			return;
		}
		if (!fop.isPanelProcedure() && !(DecisionDisplayRules.SHOW_ORIGINAL_DECISIONS && fop.hasJuryReviewVote())) {
			return;
		}
		Boolean[] decisions = fop.getRefereeDecision();
		if (decisions == null || decisions.length < AdjudicationPanel.NB_REFEREES) {
			hideOriginalRefereeDecisions();
			return;
		}
		this.originalRefereeDecisions.removeAll();
		for (int i = 0; i < AdjudicationPanel.NB_REFEREES; i++) {
			if (decisions[i] == null) {
				hideOriginalRefereeDecisions();
				return;
			}
			Div light = new Div();
			light.getElement().setAttribute("data-decision", decisions[i] ? "good" : "bad");
			light.getStyle()
			        .set("width", "3.5rem")
			        .set("height", "2.2rem")
			        .set("border", "1px solid white")
			        .set("border-radius", "0.2rem")
			        .set("box-sizing", "border-box")
			        .set("background", decisions[i] ? "white" : "red");
			this.originalRefereeDecisions.add(light);
		}
		setPlatformSelectorsVisible(false);
		this.originalRefereeDecisions.setVisible(true);
	}

	private void hideOriginalRefereeDecisions() {
		if (this.originalRefereeDecisions != null) {
			this.originalRefereeDecisions.removeAll();
			this.originalRefereeDecisions.setVisible(false);
		}
		setPlatformSelectorsVisible(true);
	}

	private void setPlatformSelectorsVisible(boolean visible) {
		if (this.fopSelect != null) {
			this.fopSelect.setVisible(visible);
		}
		if (this.jurySizeSelect != null) {
			this.jurySizeSelect.setVisible(visible && !TcrrCompliance.isEnabled());
		}
	}

	private void syncLiveRefereeDecisions(FieldOfPlay fop) {
		if (fop == null || this.liveDecisions == null) {
			return;
		}

		this.liveDecisions.setFop(fop);
		this.liveDecisions.doReset();
		if (fop.isPanelVoteOpen()) {
			this.liveDecisions.getDecisionState().syncReviewVotes(null);
			return;
		}

		Boolean[] curRefDecisions = fop.getRefereeDecision();
		Long[] curRefTimes = fop.getRefereeTime();
		if (!hasRefereeDecision(curRefDecisions)) {
			return;
		}

		InputKind inputKind = fop.getCurrentInputKind();
		boolean singleRef = inputKind == InputKind.ANNOUNCER_ENTRY || inputKind == InputKind.SOLO_INPUT;
		if (singleRef) {
			this.liveDecisions.slaveRefereeUpdate(new UIEvent.RefereeUpdate(
			        fop.getAthleteUnderReview(),
			        null,
			        curRefDecisions[1],
			        null,
			        null,
			        curRefTimes[1],
			        null,
			        this,
			        true,
			        fop));
		} else {
			this.liveDecisions.slaveRefereeUpdate(new UIEvent.RefereeUpdate(
			        fop.getAthleteUnderReview(),
			        curRefDecisions[0],
			        curRefDecisions[1],
			        curRefDecisions[2],
			        curRefTimes[0],
			        curRefTimes[1],
			        curRefTimes[2],
			        this,
			        false,
			        fop));
		}
	}

	private void juryVote(Integer juryMember, Boolean goodBad, boolean sendFOPEvent) {
		if (juryMember >= this.juryVoteCells.length) return;
		if (goodBad == null) {
			Icon nonVotedIcon = bigIcon(VaadinIcon.CIRCLE_THIN, "gray");
			this.juryVoteCells[juryMember].removeAll();
			this.juryVoteCells[juryMember].add(nonVotedIcon);
			this.juryIcons[juryMember] = nonVotedIcon;
			this.juryVotes[juryMember] = null;
			return;
		}
		Icon votedIcon = bigIcon(VaadinIcon.CIRCLE, "gray");
		this.juryVoteCells[juryMember].removeAll();
		this.juryVoteCells[juryMember].add(votedIcon);
		this.juryIcons[juryMember] = votedIcon;
		this.juryVotes[juryMember] = goodBad;
		if (sendFOPEvent) {
			getFop().fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(this, juryMember, goodBad));
		}
		checkAllVoted();
	}

	private void resetJuryVoting() {
		if (this.juryVoteCells == null) return;
		this.juryIcons = new Icon[this.juryVoteCells.length];
		this.juryVotes = new Boolean[this.juryVoteCells.length];
		for (int i = 0; i < this.juryVoteCells.length; i++) {
			this.juryVoteCells[i].removeAll();
			Icon nonVotedIcon = bigIcon(VaadinIcon.CIRCLE_THIN, "gray");
			this.juryVoteCells[i].add(nonVotedIcon);
			this.juryIcons[i] = nonVotedIcon;
			this.juryVotes[i] = null;
		}
	}

	private void resumeCompetition() {
		OwlcmsSession.withFop(fop -> {
			fop.fopEventPost(new FOPEvent.JuryResume(this));
		});
	}

	private Div buildReviewHeader() {
		this.reviewHeader = new Div();
		this.reviewHeader.getStyle()
		        .set("display", "flex")
		        .set("align-items", "baseline")
		        .set("gap", "1.5rem")
		        .set("min-width", "0");

		this.reviewFiller = new Div();
		this.reviewFiller.setText(Translator.translate("Jury_Keypad"));
		this.reviewFiller.getStyle()
		        .set("margin", "0")
		        .set("font-size", "2rem")
		        .set("font-weight", "600")
		        .set("white-space", "nowrap")
		        .set("color", "white")
		        .set("line-height", "1");

		this.reviewStartNumber = new Div();
		this.reviewStartNumber.getStyle()
		        .set("display", "inline-flex")
		        .set("align-items", "center")
		        .set("justify-content", "center")
		        .set("min-width", "3rem")
		        .set("height", "3rem")
		        .set("flex-shrink", "0")
		        .set("padding", "0 0.5rem")
		        .set("background", "#b91c1c")
		        .set("color", "#ffffff")
		        .set("font-size", "1.8rem")
		        .set("font-weight", "700");

		this.reviewName = new Div();
		this.reviewName.getStyle()
		        .set("min-width", "0")
		        .set("overflow", "hidden")
		        .set("text-overflow", "ellipsis")
		        .set("white-space", "nowrap")
		        .set("font-size", "1.9rem")
		        .set("font-weight", "600");

		this.reviewAttempt = new Div();
		this.reviewAttempt.getStyle()
		        .set("white-space", "nowrap")
		        .set("font-size", "1.5rem")
		        .set("color", "var(--lumo-secondary-text-color)");

		this.reviewWeight = new Div();
		this.reviewWeight.getStyle()
		        .set("white-space", "nowrap")
		        .set("font-size", "1.9rem")
		        .set("color", "aqua");

		this.reviewTimer = new PassiveTimerElement();
		this.reviewTimer.setWarningThresholds(
		        Competition.athleteTimerInitialWarning / 1000.0D,
		        Competition.athleteTimerFinalWarning / 1000.0D);
		this.reviewTimer.setSilent(true);
		// timer commands sent before attach can be missed by the client
		this.reviewTimer.addAttachListener(e -> syncReviewTimer(getFop()));
		this.reviewTimer.getStyle()
		        .set("min-width", "5rem")
		        .set("font-size", "1.9rem")
		        .set("font-weight", "bold")
		        .set("color", "yellow")
		        .set("display", "block");

		this.reviewTimerFrozen = new Div();
		this.reviewTimerFrozen.getStyle()
		        .set("min-width", "5rem")
		        .set("font-size", "1.9rem")
		        .set("font-weight", "bold")
		        .set("color", "yellow")
		        .set("white-space", "nowrap")
		        .set("font-variant-numeric", "tabular-nums");

		this.reviewHeader.add(this.reviewFiller, this.reviewStartNumber, this.reviewName,
		        this.reviewAttempt, this.reviewWeight, this.reviewTimer, this.reviewTimerFrozen);
		clearReviewHeader();
		return this.reviewHeader;
	}

	private void clearReviewHeader() {
		if (this.reviewFiller == null) {
			return;
		}
		this.reviewFrozen = false;
		this.reviewFiller.setVisible(true);
		if (this.reviewStartNumber != null) this.reviewStartNumber.setVisible(false);
		if (this.reviewName != null) this.reviewName.setVisible(false);
		if (this.reviewAttempt != null) this.reviewAttempt.setVisible(false);
		if (this.reviewWeight != null) this.reviewWeight.setVisible(false);
		if (this.reviewTimer != null) this.reviewTimer.setVisible(false);
		if (this.reviewTimerFrozen != null) this.reviewTimerFrozen.setVisible(false);
	}

	private String formatReviewAttempt(Athlete athlete) {
		if (athlete == null) {
			return "";
		}
		String liftType = athlete.getAttemptsDone() >= 3
		        ? Translator.translate("Clean_and_Jerk")
		        : Translator.translate("Snatch");
		return liftType + " " + athlete.getAttemptNumber();
	}

	private Athlete resolveReviewAthlete(FieldOfPlay fop) {
		if (fop == null) {
			return null;
		}
		Athlete clockOwner = fop.getClockOwner();
		if (clockOwner != null) {
			return clockOwner;
		}
		return isJuryDecisionActive(fop) ? fop.getAthleteUnderReview() : null;
	}

	private void syncReviewTimer(FieldOfPlay fop) {
		if (fop == null || this.reviewTimer == null) {
			return;
		}
		IProxyTimer athleteTimer = fop.getAthleteTimer();
		if (athleteTimer == null) {
			return;
		}
		if (athleteTimer.isRunning()) {
			this.reviewTimer.start(athleteTimer.liveTimeRemaining(), System.currentTimeMillis());
		} else {
			this.reviewTimer.display(athleteTimer.getTimeRemaining());
		}
	}

	private void updateReviewHeader(FieldOfPlay fop) {
		if (this.reviewFiller == null || this.reviewFrozen) {
			return;
		}
		Athlete athlete = resolveReviewAthlete(fop);
		if (athlete == null) {
			clearReviewHeader();
			return;
		}
		this.reviewStartNumber
		        .setText(athlete.getStartNumber() != null ? athlete.getStartNumber().toString() : "-");
		this.reviewName.setText(athlete.getFullName() != null ? athlete.getFullName() : "");
		this.reviewAttempt.setText(formatReviewAttempt(athlete));
		Integer weight = athlete.getNextAttemptRequestedWeight();
		String kgSymbol = Translator.translate("KgSymbol");
		this.reviewWeight.getElement().setProperty("innerHTML",
		        weight + "<span style=\"font-size:75%\">" + kgSymbol + "</span>");
		this.reviewFiller.setVisible(false);
		this.reviewStartNumber.setVisible(true);
		this.reviewName.setVisible(true);
		this.reviewAttempt.setVisible(true);
		this.reviewWeight.setVisible(true);
		// If clockOwner is null the athlete came from athleteUnderReview
		// (jury break) — show the frozen stopped time, not the live timer.
		if (fop.getClockOwner() != null) {
			syncReviewTimer(fop);
			this.reviewTimer.setVisible(true);
			this.reviewTimerFrozen.setVisible(false);
		} else {
			freezeReviewTimer();
		}
	}

	private void freezeReviewTimer() {
		if (this.reviewTimerFrozen == null || this.reviewTimer == null) {
			return;
		}
		FieldOfPlay fop = getFop();
		if (fop != null) {
			int millis = fop.getAthleteTimer().getTimeRemainingAtLastStop();
			this.reviewTimerFrozen.setText(formatTime(millis));
		}
		this.reviewTimer.setVisible(false);
		this.reviewTimerFrozen.setVisible(true);
		this.reviewFrozen = true;
	}

	private String formatTime(int milliseconds) {
		if (milliseconds <= 0) {
			return "0:00";
		}
		int totalSeconds = (milliseconds + 999) / 1000;
		int minutes = totalSeconds / 60;
		int seconds = totalSeconds % 60;
		return minutes + ":" + String.format("%02d", seconds);
	}

	private void startChallenge() {
		FieldOfPlay fop = getFop();
		if (fop == null || !fop.isChallengeAllowed()) {
			return;
		}
		startBreak(BreakType.CHALLENGE);
	}

	private void startBreak(BreakType breakType) {
		OwlcmsSession.withFop(fop -> {
			if (isJuryDecisionPhase(breakType) && fop.getAthleteUnderReview() == null) {
				NotificationUtils.errorNotification(Translator.translate("JuryDialog.NoCurrentAthlete"));
				return;
			}
			fop.fopEventPost(new FOPEvent.BreakStarted(breakType, CountdownType.INDEFINITE, 0, null, true, this));
		});
	}

	private void startDeliberation() {
		startBreak(BreakType.JURY);
	}

	private void startTechnicalBreak() {
		startBreak(BreakType.TECHNICAL);
	}

	private boolean isJuryDecisionPhase(BreakType breakType) {
		return breakType == BreakType.JURY || breakType == BreakType.CHALLENGE;
	}

	private boolean isJuryDecisionActive(FieldOfPlay fop) {
		return fop != null
		        && isJuryDecisionPhase(fop.getBreakType())
		        && fop.getAthleteUnderReview() != null;
	}

	/**
	 * @param chairEntered true when the Jury Chair used the Good Lift / No Lift buttons, false when the decision is
	 *                     the result of a jury vote (the Chair only supplied the rule)
	 */
	private void submitJuryDecision(boolean success, Integer reasonCode, boolean chairEntered) {
		OwlcmsSession.withFop(fop -> {
			if (!isJuryDecisionActive(fop)) {
				return;
			}
			fop.fopEventPost(new FOPEvent.JuryDecision(fop.getAthleteUnderReview(), this, success, true, reasonCode,
			        chairEntered));
		});
	}

	/**
	 * No Lift on Jury Control, like the jury box's No Lift: the FOP asks the Jury Control pages for the technical rule.
	 */
	private void chairNoLift() {
		OwlcmsSession.withFop(fop -> fop.fopEventPost(new FOPEvent.JuryRuleRequest(this, false)));
	}

	/**
	 * Show the dialog that matches the FOP state, so that every Jury Control page on the platform shows the same one:
	 * the rule dialog for the No Lift result of a vote or for a No Lift pressed by the Chair, Confirm Good Lift for the
	 * Good Lift result of a vote, nothing otherwise. The box page (testing) never shows them.
	 */
	private void syncJuryDialogs(FieldOfPlay fop) {
		if (this.boxMode || !isJuryDecisionActive(fop)) {
			closeJuryDialogs();
			return;
		}
		Boolean majority = fop.isPanelVoteOpen() ? fop.getPanelVoteMajority() : null;
		if (Boolean.FALSE.equals(majority)) {
			closeConfirmGoodLiftDialog();
			openNoLiftReasonDialog(true);
		} else if (fop.isJuryRuleRequested()) {
			closeConfirmGoodLiftDialog();
			openNoLiftReasonDialog(false);
		} else if (Boolean.TRUE.equals(majority)) {
			closeNoLiftReasonDialog();
			openConfirmGoodLiftDialog();
		} else {
			closeJuryDialogs();
		}
	}

	private void closeJuryDialogs() {
		closeNoLiftReasonDialog();
		closeConfirmGoodLiftDialog();
	}

	private void closeNoLiftReasonDialog() {
		if (this.noLiftReasonDialog != null) {
			this.noLiftReasonDialog.close();
			this.noLiftReasonDialog = null;
		}
	}

	private void closeConfirmGoodLiftDialog() {
		if (this.confirmGoodLiftDialog != null) {
			this.confirmGoodLiftDialog.close();
			this.confirmGoodLiftDialog = null;
		}
	}

	private void cancelVoteResult() {
		OwlcmsSession.withFop(fop -> fop.fopEventPost(new FOPEvent.PanelVoteCancelled(this)));
	}

	/**
	 * The Good Lift result of a vote waits for the Chair: Confirm Good Lift applies it, Cancel vote result clears the
	 * votes so that a new vote can be called.
	 */
	private void openConfirmGoodLiftDialog() {
		if (this.confirmGoodLiftDialog != null && this.confirmGoodLiftDialog.isOpened()) {
			return;
		}
		Dialog dialog = new Dialog();
		this.confirmGoodLiftDialog = dialog;
		dialog.getElement().getThemeList().add(Lumo.DARK);
		dialog.setHeaderTitle(Translator.translate("JuryDialog.GoodLiftLabel"));
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);
		// Confirm Good Lift the size of a keypad button, so that the Jury Chair cannot miss it; Cancel vote result
		// intentionally smaller, centered in a slot of the same size
		Button cancelResult = createKeypadButton(Translator.translate("JuryKeypad.CancelVoteResult"), "contrast",
		        () -> {
			        closeConfirmGoodLiftDialog();
			        cancelVoteResult();
		        });
		styleSmallDialogButton(cancelResult);
		Div cancelSlot = new Div(cancelResult);
		cancelSlot.getStyle()
		        .set("width", "24vw")
		        .set("height", "12vh")
		        .set("display", "flex")
		        .set("align-items", "center")
		        .set("justify-content", "center");
		Button confirm = createKeypadButton(Translator.translate("JuryKeypad.ConfirmGoodLift"), "primary contrast",
		        () -> {
			        closeConfirmGoodLiftDialog();
			        submitJuryDecision(true, null, true);
		        });
		confirm.getStyle()
		        .set("width", "24vw")
		        .set("height", "12vh")
		        .set("background-color", "white")
		        .set("color", "black")
		        .set("font-weight", "700");
		Div buttons = new Div(confirm, cancelSlot);
		buttons.getStyle()
		        .set("display", "flex")
		        .set("gap", "var(--lumo-space-m)");
		dialog.add(buttons);
		dialog.open();
	}

	/**
	 * @param forVote true for the No Lift result of a vote (Cancel cancels the vote result), false for a No Lift
	 *                pressed by the Chair (Cancel withdraws the request)
	 */
	private void openNoLiftReasonDialog(boolean forVote) {
		if (this.noLiftReasonDialog != null && this.noLiftReasonDialog.isOpened()
		        && this.noLiftReasonForVote == forVote) {
			return;
		}
		closeNoLiftReasonDialog();
		this.noLiftReasonForVote = forVote;
		Dialog dialog = new Dialog();
		this.noLiftReasonDialog = dialog;
		dialog.getElement().getThemeList().add(Lumo.DARK);
		dialog.setHeaderTitle(Translator.translate("JuryDialog.BadLiftLabel"));
		dialog.setWidth("95vw");
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);

		Div reasons = new Div();
		// columns fill top to bottom; at most 3 of at least 17rem, so 3 columns from 1024px-wide tablets up
		reasons.getStyle()
		        .set("columns", "3 17rem")
		        .set("column-gap", "var(--lumo-space-l)");
		for (Integer reasonCode : JuryRejectionReasons.REASONS.keySet()) {
			Button reasonButton = new Button();
			reasonButton.addClickListener(e -> {
				closeNoLiftReasonDialog();
				// the FOP decides whether this is the result of the vote or the Chair's own decision
				submitJuryDecision(false, reasonCode, true);
			});
			// number in a fixed-width column so wrapped lines hang under the reason text
			Span number = new Span(reasonCode + " -");
			number.getStyle()
			        .set("flex", "none")
			        .set("min-width", "2.2em")
			        .set("text-align", "right");
			Span reasonText = new Span(Translator.translate(JuryRejectionReasons.REASONS.get(reasonCode)));
			Div reasonLabel = new Div(number, reasonText);
			reasonLabel.getStyle()
			        .set("display", "flex")
			        .set("gap", "0.4em")
			        .set("align-items", "baseline");
			reasonButton.getElement().appendChild(reasonLabel.getElement());
			reasonButton.setAriaLabel(JuryRejectionReasons.label(reasonCode));
			reasonButton.addClassName("jury-reason-button");
			reasonButton.getStyle()
			        .set("display", "flex")
			        .set("width", "100%")
			        .set("margin", "0 0 var(--lumo-space-m) 0")
			        .set("break-inside", "avoid")
			        .set("height", "auto")
			        .set("min-height", "4.5rem")
			        .set("white-space", "normal")
			        .set("justify-content", "flex-start")
			        .set("text-align", "left")
			        .set("color", "white")
			        .set("--vaadin-button-text-color", "white")
			        .set("font-size", "var(--lumo-font-size-l)");
			reasons.add(reasonButton);
		}
		dialog.add(reasons);

		Button cancel;
		if (forVote) {
			cancel = createKeypadButton(Translator.translate("JuryKeypad.CancelVoteResult"), "contrast", () -> {
				closeNoLiftReasonDialog();
				cancelVoteResult();
			});
		} else {
			cancel = createKeypadButton(Translator.translate("Cancel"), "contrast", () -> {
				closeNoLiftReasonDialog();
				OwlcmsSession.withFop(fop -> fop.fopEventPost(new FOPEvent.JuryRuleRequest(this, true)));
			});
		}
		styleSmallDialogButton(cancel);
		dialog.getFooter().add(cancel);
		dialog.open();
	}

	/** the cancel button of the jury dialogs: intentionally smaller than a keypad button (60% by 55%) */
	private void styleSmallDialogButton(Button button) {
		styleSummonButton(button);
		button.getStyle()
		        .set("width", "calc(24vw * 0.6)")
		        .set("height", "calc(12vh * 0.55)");
	}

	private void updateJuryDecisionActions(FieldOfPlay fop) {
		boolean enabled = isJuryDecisionActive(fop);
		setJuryDecisionActionsEnabled(enabled);
		if (this.panelMode) {
			// during a review, the Jury Chair's No Lift and Good Lift replace Deliberation and Challenge
			setVisible(this.noLiftButton, enabled);
			setVisible(this.goodLiftButton, enabled);
			setVisible(this.deliberationButton, !enabled);
			setVisible(this.challengeButton, !enabled);
		}
		updateChallengeButton(fop);
		updateCallForVotesButton(fop);
	}

	/** Challenge only against a referees' No Lift that the jury or the panel agreed with */
	private void updateChallengeButton(FieldOfPlay fop) {
		if (this.challengeButton == null) {
			return;
		}
		boolean allowed = fop != null && fop.isChallengeAllowed();
		this.challengeButton.setEnabled(allowed);
		this.challengeButton.getStyle()
		        .set("background-color", allowed ? "#FFC107" : "#1e293b")
		        .set("color", allowed ? "#000000" : "#64748b");
	}

	private static void setVisible(Button button, boolean visible) {
		if (button != null) {
			button.setVisible(visible);
		}
	}

	private void setJuryDecisionActionsEnabled(boolean enabled) {
		if (this.noLiftButton != null) {
			this.noLiftButton.setEnabled(enabled);
			styleJuryDecisionButton(this.noLiftButton, enabled, "#b91c1c", "#7f1d1d", "#ffffff", "#d97777");
		}
		if (this.goodLiftButton != null) {
			this.goodLiftButton.setEnabled(enabled);
			styleJuryDecisionButton(this.goodLiftButton, enabled, "#ffffff", "#d9d9d9", "#000000", "#5c5c5c");
		}
	}

	private void styleJuryDecisionButton(Button button, boolean enabled, String enabledBackground,
	        String disabledBackground, String enabledTextColor, String disabledTextColor) {
		button.getStyle()
		        .set("background-color", enabled ? enabledBackground : disabledBackground)
		        .set("background-image", "none")
		        .set("color", enabled ? enabledTextColor : disabledTextColor)
		        .set("border", enabled ? "1px solid rgba(200, 200, 200, 0.75)" : "1px solid rgba(160, 160, 160, 0.45)")
		        .set("opacity", enabled ? "1" : "0.8");
	}

	private void summonReferee(int refNumber) {
		OwlcmsSession.withFop(fop -> {
			// 0 summons all referees as a single event, same as the hardware jury box; the FOP decides whether it
			// means Call for Votes (TCRR 7.5.7, the Referee call buttons request the new decision)
			fop.fopEventPost(new FOPEvent.SummonReferee(this, refNumber));
		});
	}
}