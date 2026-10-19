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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
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
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.platform.Platform;
import app.owlcms.fieldofplay.AdjudicationPanel;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
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

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	private JuryDisplayDecisionElement liveDecisions;
	private Icon[] juryIcons;
	private Boolean[] juryVotes;
	private Div[] juryVoteCells;
	private Div refDecisionHost;
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
	/** seven-person Adjudication Panel with VPT: 7-cell panel row, Call for Votes, Waiting for Votes screen */
	private boolean panelMode;
	private Div[] panelRefCells;
	private Button callForVotesButton;
	private Dialog waitingForVotesDialog;
	private Dialog noLiftReasonDialog;
	private Div[] waitingCells;
	private Button[] recallButtons;
	private Div waitingCounter;
	private Div waitingResult;
	private boolean reasonDialogOpenedForVote;
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
			// an Adjudication Panel Review keeps the jury votes that caused it
			if ((deliberationType == JuryDeliberationEventType.START_DELIBERATION && !e.isPanelReview())
			        || deliberationType == JuryDeliberationEventType.CHALLENGE) {
				resetJuryVoting();
			}
			updateJuryDecisionActions(e.getFop());
			updateReviewHeader(e.getFop());
		});
	}

	@Subscribe
	public void slavePanelVoteCalled(UIEvent.PanelVoteCalled e) {
		// a new review vote: the jury votes again
		if (e.getRef() == 0) {
			UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
				resetJuryVoting();
				resetPanelRefCells();
				this.reasonDialogOpenedForVote = false;
				updateJuryDecisionActions(getFop());
				openWaitingForVotes();
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
			updateJuryDecisionActions(getFop());
			if (!e.isOpen()) {
				closeWaitingForVotes();
				return;
			}
			openWaitingForVotes();
			if (this.waitingResult != null) {
				Boolean majority = e.getMajority();
				this.waitingResult.setText(majority == null ? ""
				        : Translator.translate(majority ? "JuryDialog.GoodLiftLabel" : "JuryDialog.BadLiftLabel"));
				this.waitingResult.getStyle().set("color", Boolean.FALSE.equals(majority) ? "red" : "white");
			}
			if (Boolean.FALSE.equals(e.getMajority()) && !this.reasonDialogOpenedForVote) {
				// a No Lift by the panel needs the technical rule from the Chair
				this.reasonDialogOpenedForVote = true;
				openNoLiftReasonDialog();
			}
		});
	}

	@Subscribe
	public void slaveJuryVotesPending(UIEvent.JuryVotesPending e) {
		if (!e.isPending()) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			Notification n = Notification.show(Translator.translate("JuryNotification.ClockStartPending"), 5000,
			        Position.MIDDLE);
			n.addThemeVariants(NotificationVariant.LUMO_WARNING);
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			FieldOfPlay fop = getFop();
			// Adjudication Panel: the jury keeps voting on the lift after its decision is cleared
			if (!(this.panelMode && fop != null && fop.isPanelVotePending())) {
				resetJuryVoting();
			}
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
			showInitialRefereeVotes(e.getFop());
		});
	}

	@Subscribe
	public void slaveResetOnNewClock(UIEvent.ResetOnNewClock e) {
		// Header stays visible until next clock start (mirrors JuryDisplayDecisionElement lifecycle)
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
		if (juryMember != null && goodBad != null) {
			UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this, () -> {
				juryVote(juryMember, goodBad, false);
			});
		}
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			resetJuryVoting();
			resetPanelRefCells();
			closeWaitingForVotes();
			updateJuryDecisionActions(getFop());
		});
	}

	@Subscribe
	public void slaveTimeStarted(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			resetJuryVoting();
			resetPanelRefCells();
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

	/** Default / 0 / 3 / 5 jury member selection; rebuilds the keypad when changed. */
	private Select<Integer> createJurySizeSelect() {
		Select<Integer> jurySizeSelect = new Select<>();
		jurySizeSelect.setItems(0, 3, 5);
		jurySizeSelect.setEmptySelectionAllowed(true);
		jurySizeSelect.setEmptySelectionCaption(
		        Translator.translate("Competition") + " (" + Competition.getCurrent().getJurySize() + ")");
		jurySizeSelect.setAriaLabel(Translator.translate("Jury"));
		jurySizeSelect.setWidth("12rem");
		FieldOfPlay currentFop = getFop();
		Platform platform = currentFop != null ? currentFop.getPlatform() : null;
		jurySizeSelect.setValue(platform != null ? platform.getJurySize() : null);
		jurySizeSelect.addValueChangeListener((e) -> {
			if (!e.isFromClient()) {
				return;
			}
			FieldOfPlay fop = getFop();
			if (fop == null) {
				return;
			}
			fop.setJurySize(e.getValue());
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
		icon.setSize("min(10rem, 18vh)");
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
		// 5 columns: [Left Ref] [Center Ref] [Right Ref] [All Refs / Challenge] [Resume / Technical]
		// 8 rows: toolbar | ref-label | ref-LEDs | summon-btns | spacer | jury-label | jury-LEDs | action-btns
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
		toolbar.add(title, fopSelect);
		// the jury size is derived (4) when the seven-person adjudication panel is in use
		if (!Config.getCurrent().featureSwitch(FeatureSwitch.SEVEN_PERSON_ADJUDICATION)) {
			toolbar.add(createJurySizeSelect());
		}
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

		// --- Row 4: Summon buttons (compact) ---
		Button allSummon = createKeypadButton(Translator.translate("JuryKeypad.SummonReferees"), "contrast", () -> summonReferee(0));
		styleSummonButton(allSummon);
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
		Button resumeBtn = createKeypadButton(Translator.translate("JuryNotification.END_JURY_BREAK"), "success primary", this::resumeCompetition);
		resumeBtn.getStyle().set("grid-column", "5").set("grid-row", "4");

		// --- Row 5: Spacer ---
		Div spacer = new Div();
		spacer.getStyle().set("grid-column", "1 / -1").set("grid-row", "5");

		// --- Row 6: Jury Decisions label ---
		FieldOfPlay buildFop = getFop();
		this.panelMode = buildFop != null && buildFop.isPanelProcedure();
		Div juryLabel = new Div();
		juryLabel.setText(Translator.translate(this.panelMode ? "JuryKeypad.AdjudicationPanel" : "JuryDecisions"));
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
		// Center jurors in columns 2..4 (skip 1 and 5) for 3-juror setup
		int nbJurors = getNbJurors();
		this.juryVoteCells = new Div[nbJurors];
		this.juryIcons = new Icon[nbJurors];
		this.juryVotes = new Boolean[nbJurors];
		int offset = nbJurors < 5 ? 1 : 0; // skip first column to center 3 jurors
		for (int i = 0; i < nbJurors; i++) {
			Div cell = new Div();
			cell.getStyle()
			        .set("background", "black")
			        .set("display", "flex")
			        .set("align-items", "center")
			        .set("justify-content", "center")
			        .set("grid-column", String.valueOf(i + 1 + offset))
			        .set("grid-row", "7");
			Icon nonVotedIcon = bigIcon(VaadinIcon.CIRCLE_THIN, "gray");
			cell.add(nonVotedIcon);
			this.juryVoteCells[i] = cell;
			this.juryIcons[i] = nonVotedIcon;
			this.juryVotes[i] = null;
		}
		// Fill empty cells with black (before and after jurors)
		int totalEmpty = 5 - nbJurors;
		Div[] emptyJuryCells = new Div[totalEmpty];
		int emptyIdx = 0;
		for (int col = 1; col <= 5; col++) {
			boolean isJurorCol = (col > offset && col <= offset + nbJurors);
			if (!isJurorCol) {
				Div emptyCell = new Div();
				emptyCell.getStyle()
				        .set("background", "black")
				        .set("grid-column", String.valueOf(col))
				        .set("grid-row", "7");
				emptyJuryCells[emptyIdx++] = emptyCell;
			}
		}

		// --- Row 8: Action buttons ---
		Button deliberate = createKeypadButton(Translator.translate("BreakButton.JuryDeliberation"), "primary contrast", this::startDeliberation);
		deliberate.getStyle().set("grid-column", "1").set("grid-row", "8")
		        .set("background-color", "#FFC107").set("color", "#000000");
		Button challenge = createKeypadButton(Translator.translate("BreakButton.CHALLENGE"), "primary contrast", this::startChallenge);
		challenge.getStyle().set("grid-column", "2").set("grid-row", "8")
		        .set("background-color", "#FFC107").set("color", "#000000");
		Button technical = createKeypadButton(Translator.translate("BreakType.TECHNICAL"), "primary", this::startTechnicalBreak);
		technical.getStyle().set("grid-column", "5").set("grid-row", "8");

		// Add all elements to grid
		grid.add(toolbar, refLabel, refDecisionArea,
		        leftSummon, centerSummon, rightSummon, allSummon, resumeBtn,
		        spacer, juryLabel);
		if (this.panelMode) {
			// the Jury Chair decides only on the Waiting for Votes screen (override)
			this.noLiftButton = null;
			this.goodLiftButton = null;
			this.callForVotesButton = createKeypadButton(Translator.translate("JuryKeypad.CallForVotes"), "primary",
			        this::callForVotes);
			this.callForVotesButton.getStyle().set("grid-column", "3 / 5").set("grid-row", "8");
			grid.add(buildPanelRow(), this.callForVotesButton, deliberate, challenge, technical);
			updateJuryDecisionActions(getFop());
			container.add(grid);
			container.setFlexGrow(1, grid);
			return;
		}
		this.callForVotesButton = null;
		this.noLiftButton = createKeypadButton(Translator.translate("JuryDialog.BadLiftLabel"), "error primary", this::openNoLiftReasonDialog);
		this.noLiftButton.getStyle().set("grid-column", "3").set("grid-row", "8");
		this.goodLiftButton = createKeypadButton(Translator.translate("JuryDialog.GoodLiftLabel"), "success primary", () -> submitJuryDecision(true, null));
		this.goodLiftButton.getStyle().set("grid-column", "4").set("grid-row", "8");
		for (Div cell : this.juryVoteCells) {
			grid.add(cell);
		}
		for (Div emptyCell : emptyJuryCells) {
			grid.add(emptyCell);
		}
		grid.add(this.noLiftButton, this.goodLiftButton, deliberate, challenge, technical);
		updateJuryDecisionActions(getFop());

		container.add(grid);
		container.setFlexGrow(1, grid);
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

	/**
	 * Seven-person Adjudication Panel row: R1 R2 R3 | J1 J2 J3 J4. The jury cells are the usual jury vote cells.
	 * Each jury cell has a per-member class (panel-jury-1 is the Jury Chair) for later styling.
	 */
	private Div buildPanelRow() {
		Div row = new Div();
		row.getStyle()
		        .set("grid-column", "1 / -1")
		        .set("grid-row", "7")
		        .set("display", "grid")
		        .set("grid-template-columns", "repeat(3, 1fr) 0.3fr repeat(4, 1fr)")
		        .set("background", "black");
		this.panelRefCells = new Div[AdjudicationPanel.NB_REFEREES];
		for (int i = 0; i < this.panelRefCells.length; i++) {
			this.panelRefCells[i] = createPanelCell("panel-referee");
			row.add(this.panelRefCells[i]);
		}
		row.add(createPanelSeparator());
		for (int i = 0; i < this.juryVoteCells.length; i++) {
			Div cell = this.juryVoteCells[i];
			cell.getStyle().remove("grid-column").remove("grid-row");
			cell.addClassNames("panel-jury", "panel-jury-" + (i + 1));
			row.add(cell);
		}
		return row;
	}

	private Div createPanelCell(String className) {
		Div cell = new Div();
		cell.addClassName(className);
		cell.getStyle()
		        .set("background", "black")
		        .set("display", "flex")
		        .set("align-items", "center")
		        .set("justify-content", "center");
		renderPanelCell(cell, false, null);
		return cell;
	}

	private Div createPanelSeparator() {
		Div separator = new Div();
		separator.getStyle()
		        .set("justify-self", "center")
		        .set("width", "0")
		        .set("border-left", "2px solid #51565c")
		        .set("margin", "15% 0");
		return separator;
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

	/** Phase A: the referee cells show the referees' initial decision once it is shown. */
	private void showInitialRefereeVotes(FieldOfPlay fop) {
		if (!this.panelMode || this.panelRefCells == null || fop == null || fop.isPanelVoteOpen()) {
			return;
		}
		Boolean[] votes = fop.getRefereeDecision();
		for (int i = 0; i < this.panelRefCells.length; i++) {
			Boolean vote = votes != null && votes.length > i ? votes[i] : null;
			renderPanelCell(this.panelRefCells[i], vote != null, vote);
		}
	}

	private void resetPanelRefCells() {
		if (this.panelRefCells == null) {
			return;
		}
		for (Div cell : this.panelRefCells) {
			renderPanelCell(cell, false, null);
		}
	}

	/**
	 * Render the review vote on the panel row and on the Waiting for Votes screen.
	 *
	 * @param voted referees 1..3 then jury members 1..4
	 * @param votes same order, null until all seven are in
	 */
	private void renderPanelVote(boolean[] voted, Boolean[] votes) {
		int nbRefs = AdjudicationPanel.NB_REFEREES;
		for (int i = 0; i < AdjudicationPanel.PANEL_SIZE; i++) {
			Boolean vote = votes != null ? votes[i] : null;
			Div rowCell = i < nbRefs ? (this.panelRefCells != null ? this.panelRefCells[i] : null)
			        : (this.juryVoteCells != null && i - nbRefs < this.juryVoteCells.length
			                ? this.juryVoteCells[i - nbRefs]
			                : null);
			if (rowCell != null) {
				renderPanelCell(rowCell, voted[i], vote);
			}
			if (this.waitingCells != null) {
				renderPanelCell(this.waitingCells[i], voted[i], vote);
			}
		}
		if (this.recallButtons != null) {
			for (int i = 0; i < this.recallButtons.length; i++) {
				this.recallButtons[i].setVisible(votes == null && !voted[i]);
			}
		}
		if (this.waitingCounter != null) {
			int received = 0;
			for (boolean v : voted) {
				received += v ? 1 : 0;
			}
			this.waitingCounter.setText(Translator.translate("JuryKeypad.VotesReceived", received));
		}
	}

	private void callForVotes() {
		OwlcmsSession.withFop(fop -> {
			if (fop.isPanelVoteCallable()) {
				fop.fopEventPost(new FOPEvent.PanelVoteCalled(this));
			}
		});
	}

	private void cancelVote() {
		OwlcmsSession.withFop(fop -> fop.fopEventPost(new FOPEvent.PanelVoteCancelled(this)));
	}

	/**
	 * The Waiting for Votes screen: the seven votes, re-call buttons for the referees who have not voted, restart,
	 * the Jury Chair override and cancel. All Jury Control pages of the platform open it when the vote is called.
	 */
	private void openWaitingForVotes() {
		if (!this.panelMode || (this.waitingForVotesDialog != null && this.waitingForVotesDialog.isOpened())) {
			return;
		}
		Dialog dialog = new Dialog();
		dialog.getElement().getThemeList().add(Lumo.DARK);
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);
		dialog.setWidth("95vw");
		dialog.setHeaderTitle(Translator.translate("JuryKeypad.WaitingForVotes"));

		Athlete athlete = resolveReviewAthlete(getFop());
		Div athleteLine = new Div();
		athleteLine.setText(athlete != null ? athlete.getFullName() + " \u2014 " + formatReviewAttempt(athlete) : "");
		athleteLine.getStyle().set("font-size", "1.5rem").set("font-weight", "600");

		Div cells = new Div();
		cells.getStyle()
		        .set("display", "grid")
		        .set("grid-template-columns", "repeat(3, 1fr) 0.3fr repeat(4, 1fr)")
		        .set("height", "22vh");
		Div recalls = new Div();
		recalls.getStyle()
		        .set("display", "grid")
		        .set("grid-template-columns", "repeat(3, 1fr) 0.3fr repeat(4, 1fr)")
		        .set("gap", "4px");
		this.waitingCells = new Div[AdjudicationPanel.PANEL_SIZE];
		this.recallButtons = new Button[AdjudicationPanel.NB_REFEREES];
		for (int i = 0; i < AdjudicationPanel.PANEL_SIZE; i++) {
			if (i == AdjudicationPanel.NB_REFEREES) {
				cells.add(createPanelSeparator());
				recalls.add(new Div());
			}
			boolean referee = i < AdjudicationPanel.NB_REFEREES;
			this.waitingCells[i] = createPanelCell(referee ? "panel-referee"
			        : "panel-jury-" + (i - AdjudicationPanel.NB_REFEREES + 1));
			cells.add(this.waitingCells[i]);
			if (referee) {
				int refNumber = i + 1;
				Button recall = new Button(Translator.translate("JuryKeypad.Recall"), e -> summonReferee(refNumber));
				styleSummonButton(recall);
				this.recallButtons[i] = recall;
				recalls.add(recall);
			} else {
				recalls.add(new Div());
			}
		}
		this.waitingCounter = new Div();
		this.waitingCounter.getStyle().set("font-size", "1.5rem");
		this.waitingResult = new Div();
		this.waitingResult.getStyle().set("font-size", "2rem").set("font-weight", "bold");
		dialog.add(athleteLine, cells, recalls, this.waitingCounter, this.waitingResult);

		Button restart = new Button(Translator.translate("JuryKeypad.RestartVote"), e -> callForVotes());
		Span chairLabel = new Span(Translator.translate("JuryKeypad.ChairOverride"));
		chairLabel.getStyle().set("margin-left", "auto").set("align-self", "center");
		Button noLift = new Button(Translator.translate("JuryDialog.BadLiftLabel"), e -> openNoLiftReasonDialog());
		noLift.getElement().setAttribute("theme", "error primary");
		Button goodLift = new Button(Translator.translate("JuryDialog.GoodLiftLabel"),
		        e -> submitJuryDecision(true, null));
		goodLift.getElement().setAttribute("theme", "success primary");
		Button cancel = new Button(Translator.translate("Cancel"), e -> cancelVote());
		dialog.getFooter().add(restart, chairLabel, noLift, goodLift, cancel);

		dialog.addOpenedChangeListener(e -> {
			if (!e.isOpened()) {
				this.waitingCells = null;
				this.recallButtons = null;
				this.waitingCounter = null;
				this.waitingResult = null;
			}
		});
		this.waitingForVotesDialog = dialog;
		dialog.open();
		FieldOfPlay fop = getFop();
		if (fop != null) {
			renderPanelVote(currentPanelVoted(fop), null);
		}
	}

	private boolean[] currentPanelVoted(FieldOfPlay fop) {
		boolean[] voted = new boolean[AdjudicationPanel.PANEL_SIZE];
		Boolean[] refs = fop.getReviewRefereeDecision();
		Boolean[] jury = fop.getJuryMemberDecision();
		for (int i = 0; i < AdjudicationPanel.NB_REFEREES; i++) {
			voted[i] = refs[i] != null;
		}
		for (int i = 0; i < AdjudicationPanel.NB_JURORS; i++) {
			voted[AdjudicationPanel.NB_REFEREES + i] = jury != null && jury.length > i && jury[i] != null;
		}
		return voted;
	}

	private void closeWaitingForVotes() {
		if (this.waitingForVotesDialog != null) {
			this.waitingForVotesDialog.close();
			this.waitingForVotesDialog = null;
		}
		// another Jury Control page may have given the decision
		if (this.noLiftReasonDialog != null) {
			this.noLiftReasonDialog.close();
			this.noLiftReasonDialog = null;
		}
	}

	private void updateCallForVotesButton(FieldOfPlay fop) {
		if (this.callForVotesButton == null) {
			return;
		}
		boolean enabled = fop != null && fop.isPanelVoteCallable();
		// prompt the Chair after an automatic review or a challenge, until the vote is called
		boolean prompt = enabled && !fop.isPanelVoteOpen()
		        && (fop.isPanelReview() || fop.getBreakType() == BreakType.CHALLENGE);
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
		        .set("background-color", "#2f343a")
		        .set("background-image", "none")
		        .set("color", "#ffffff")
		        .set("border", "1px solid #51565c");
	}

	private int getNbJurors() {
		FieldOfPlay fop = getFop();
		return fop != null ? fop.getJurySize() : Competition.getCurrent().getJurySize();
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
		registerShortcut(this::startDeliberation, Key.KEY_D);
		registerShortcut(this::startChallenge, Key.KEY_C);
		registerShortcut(this::startTechnicalBreak, Key.KEY_T);
		registerShortcut(() -> summonReferee(1), Key.KEY_H);
		registerShortcut(() -> summonReferee(2), Key.KEY_I);
		registerShortcut(() -> summonReferee(3), Key.KEY_J);
		registerShortcut(() -> summonReferee(0), Key.KEY_K);
		if (this.panelMode) {
			registerShortcut(this::callForVotes, Key.KEY_V);
		}
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
		if (this.panelMode) {
			if (fop.isPanelVoteOpen()) {
				openWaitingForVotes();
				renderPanelVote(currentPanelVoted(fop), null);
			} else {
				showInitialRefereeVotes(fop);
			}
		}
	}

	private void syncLiveRefereeDecisions(FieldOfPlay fop) {
		if (fop == null || this.liveDecisions == null) {
			return;
		}

		this.liveDecisions.setFop(fop);
		this.liveDecisions.doReset();

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
			fop.fopEventPost(new FOPEvent.StartLifting(this));
		});
		resetJuryVoting();
		clearReviewHeader();
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

	private void submitJuryDecision(boolean success, Integer reasonCode) {
		OwlcmsSession.withFop(fop -> {
			if (!isJuryDecisionActive(fop)) {
				return;
			}
			fop.fopEventPost(new FOPEvent.JuryDecision(fop.getAthleteUnderReview(), this, success, true, reasonCode));
		});
	}

	private void openNoLiftReasonDialog() {
		if (!isJuryDecisionActive(getFop())) {
			return;
		}
		if (this.noLiftReasonDialog != null) {
			this.noLiftReasonDialog.close();
		}
		Dialog dialog = new Dialog();
		this.noLiftReasonDialog = dialog;
		dialog.getElement().getThemeList().add(Lumo.DARK);
		dialog.setHeaderTitle(Translator.translate("JuryDialog.BadLiftLabel"));
		dialog.setWidth("95vw");

		Div reasons = new Div();
		// columns fill top to bottom; at most 3 of at least 17rem, so 3 columns from 1024px-wide tablets up
		reasons.getStyle()
		        .set("columns", "3 17rem")
		        .set("column-gap", "var(--lumo-space-l)");
		for (Integer reasonCode : JuryRejectionReasons.REASONS.keySet()) {
			Button reasonButton = new Button();
			reasonButton.addClickListener(e -> {
				dialog.close();
				submitJuryDecision(false, reasonCode);
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
			        .set("min-height", "3.25rem")
			        .set("white-space", "normal")
			        .set("justify-content", "flex-start")
			        .set("text-align", "left")
			        .set("color", "white")
			        .set("--vaadin-button-text-color", "white")
			        .set("font-size", "var(--lumo-font-size-l)");
			reasons.add(reasonButton);
		}
		dialog.add(reasons);

		Button cancel = new Button(Translator.translate("Cancel"), e -> dialog.close());
		dialog.getFooter().add(cancel);
		dialog.open();
	}

	private void updateJuryDecisionActions(FieldOfPlay fop) {
		boolean enabled = isJuryDecisionActive(fop);
		setJuryDecisionActionsEnabled(enabled);
		updateCallForVotesButton(fop);
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
			if (refNumber == 0 && this.panelMode && fop.isPanelVoteCallable()) {
				// during a review, the Referee call buttons request the new decision (TCRR 7.5.7)
				fop.fopEventPost(new FOPEvent.PanelVoteCalled(this));
			} else if (refNumber > 0) {
				fop.fopEventPost(new FOPEvent.SummonReferee(this, refNumber));
			} else {
				for (int i = 1; i <= 3; i++) {
					fop.fopEventPost(new FOPEvent.SummonReferee(this, i));
				}
			}
		});
	}
}