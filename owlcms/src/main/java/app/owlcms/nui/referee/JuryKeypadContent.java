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
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
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
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.NotificationUtils;
import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.apputils.queryparameters.FOPParametersReader;
import app.owlcms.components.elements.JuryDisplayDecisionElement;
import app.owlcms.data.competition.Competition;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.InputKind;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.AuthorizationDispatch;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings({ "serial", "deprecation" })
@Route(value = "jurykeypad")
@CssImport(value = "./styles/shared-styles.css")
public class JuryKeypadContent extends BaseContent implements FOPParametersReader, SafeEventBusRegistration,
        UIEventProcessor, HasDynamicTitle, AuthorizationDispatch, BeforeEnterListener {

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
	private Button noLiftButton;
	private Button goodLiftButton;
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
		AuthorizationDispatch.super.beforeEnter(event);
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
	}

	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> updateJuryDecisionActions(e.getFop()));
	}

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> setJuryDecisionActionsEnabled(false));
	}

	@Subscribe
	public void slaveJuryNotification(UIEvent.JuryNotification e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			JuryDeliberationEventType deliberationType = e.getDeliberationEventType();
			if (deliberationType == JuryDeliberationEventType.START_DELIBERATION
			        || deliberationType == JuryDeliberationEventType.CHALLENGE) {
				resetJuryVoting();
			}
			updateJuryDecisionActions(e.getFop());
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			resetJuryVoting();
			updateJuryDecisionActions(getFop());
		});
	}

	@Subscribe
	public void slaveJuryMemberDecision(UIEvent.JuryUpdate e) {
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
			updateJuryDecisionActions(getFop());
		});
	}

	@Subscribe
	public void slaveTimeStarted(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			resetJuryVoting();
			updateJuryDecisionActions(getFop());
		});
	}

	protected ComboBox<FieldOfPlay> createFopSelect() {
		ComboBox<FieldOfPlay> fopSelect = new ComboBox<>();
		fopSelect.setPlaceholder(Translator.translate("SelectPlatform"));
		fopSelect.setItems(OwlcmsFactory.getFOPs());
		fopSelect.setItemLabelGenerator(FieldOfPlay::getName);
		fopSelect.setWidth("12rem");
		return fopSelect;
	}

	protected void init() {
		this.setBoxSizing(BoxSizing.BORDER_BOX);
		this.setSizeFull();
		buildContent(this);
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		getElement().executeJs("document.querySelector('html').setAttribute('theme', 'dark');");
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
		H2 title = new H2(Translator.translate("Jury_Keypad"));
		title.getStyle().set("margin", "0");
		ComboBox<FieldOfPlay> fopSelect = createFopSelect();
		fopSelect.setValue(OwlcmsSession.getFop());
		fopSelect.addValueChangeListener((e) -> {
			setFop(e.getValue());
			OwlcmsSession.setFop(e.getValue());
			this.uiEventBus = uiEventBusRegister(this, e.getValue());
			attachLiveDecisions(e.getValue());
			syncWithFopState(e.getValue());
			if (this.location != null && this.locationUI != null) {
				Location location2 = new Location(this.location.getPath(), new QueryParameters(this.urlParams));
				URLUtils.replaceState(this.locationUI.getPage().getHistory(), null, location2, this.location);
			}
		});
		toolbar.add(title, fopSelect);
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
		this.noLiftButton = createKeypadButton(Translator.translate("JuryDialog.BadLiftLabel"), "error primary", () -> submitJuryDecision(false));
		this.noLiftButton.getStyle().set("grid-column", "1").set("grid-row", "8");
		this.goodLiftButton = createKeypadButton(Translator.translate("JuryDialog.GoodLiftLabel"), "success primary", () -> submitJuryDecision(true));
		this.goodLiftButton.getStyle().set("grid-column", "2").set("grid-row", "8");
		Button deliberate = createKeypadButton(Translator.translate("BreakButton.JuryDeliberation"), "primary contrast", this::startDeliberation);
		deliberate.getStyle().set("grid-column", "3").set("grid-row", "8")
		        .set("background-color", "#FFC107").set("color", "#000000");
		Button challenge = createKeypadButton(Translator.translate("BreakButton.CHALLENGE"), "primary contrast", this::startChallenge);
		challenge.getStyle().set("grid-column", "4").set("grid-row", "8")
		        .set("background-color", "#FFC107").set("color", "#000000");
		Button technical = createKeypadButton(Translator.translate("BreakType.TECHNICAL"), "primary", this::startTechnicalBreak);
		technical.getStyle().set("grid-column", "5").set("grid-row", "8");

		// Add all elements to grid
		grid.add(toolbar, refLabel, refDecisionArea,
		        leftSummon, centerSummon, rightSummon, allSummon, resumeBtn,
		        spacer, juryLabel);
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
		return Competition.getCurrent().getJurySize();
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
	}

	private void syncWithFopState(FieldOfPlay fop) {
		if (fop == null) {
			return;
		}

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

		if (this.liveDecisions == null) {
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

	private void submitJuryDecision(boolean success) {
		OwlcmsSession.withFop(fop -> {
			if (!isJuryDecisionActive(fop)) {
				return;
			}
			fop.fopEventPost(new FOPEvent.JuryDecision(fop.getAthleteUnderReview(), this, success, true));
		});
	}

	private void updateJuryDecisionActions(FieldOfPlay fop) {
		boolean enabled = isJuryDecisionActive(fop);
		setJuryDecisionActionsEnabled(enabled);
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
			if (refNumber > 0) {
				fop.fopEventPost(new FOPEvent.SummonReferee(this, refNumber));
			} else {
				for (int i = 1; i <= 3; i++) {
					fop.fopEventPost(new FOPEvent.SummonReferee(this, i));
				}
			}
		});
	}
}