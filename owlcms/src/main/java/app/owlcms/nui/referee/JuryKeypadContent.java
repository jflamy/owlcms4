/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.referee;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
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

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.apputils.queryparameters.FOPParametersReader;
import app.owlcms.components.elements.JuryDisplayDecisionElement;
import app.owlcms.data.competition.Competition;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.AuthorizationDispatch;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakType;
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
	private Location location;
	private UI locationUI;
	private EventBus uiEventBus;
	private Map<String, List<String>> urlParams;
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
		return Translator.translate("Jury") + " keypad" + FieldOfPlay.getFopNameIfMultiple(getFop());
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
		if (e.getBreakType() == BreakType.JURY || e.getBreakType() == BreakType.CHALLENGE) {
			UIEventProcessor.uiAccess(this, this.uiEventBus, this::resetJuryVoting);
		}
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, this::resetJuryVoting);
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
		UIEventProcessor.uiAccess(this, this.uiEventBus, this::resetJuryVoting);
	}

	@Subscribe
	public void slaveTimeStarted(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, this::resetJuryVoting);
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
		OwlcmsSession.withFop(fop -> this.uiEventBus = uiEventBusRegister(this, fop));
	}

	private Icon bigIcon(VaadinIcon iconDef, String color) {
		Icon icon = iconDef.create();
		icon.setSize("min(8rem, 15vh)");
		icon.getStyle().set("color", color);
		return icon;
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
		        .set("gap", "2px")
		        .set("padding", "0.5rem")
		        .set("box-sizing", "border-box")
		        .set("max-height", "100vh")
		        .set("overflow", "hidden");

		// --- Row 1: Toolbar ---
		HorizontalLayout toolbar = new HorizontalLayout();
		toolbar.setAlignItems(Alignment.CENTER);
		toolbar.setWidthFull();
		H2 title = new H2(Translator.translate("Jury") + " keypad");
		title.getStyle().set("margin", "0");
		ComboBox<FieldOfPlay> fopSelect = createFopSelect();
		fopSelect.setValue(OwlcmsSession.getFop());
		fopSelect.addValueChangeListener((e) -> {
			OwlcmsSession.setFop(e.getValue());
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
		refLabel.setText("Referee Decisions");
		refLabel.getStyle()
		        .set("grid-column", "1 / -1").set("grid-row", "2")
		        .set("color", "var(--lumo-secondary-text-color)")
		        .set("font-size", "0.85rem")
		        .set("font-weight", "bold")
		        .set("padding", "0.25rem 0 0 0.25rem")
		        .set("text-transform", "uppercase")
		        .set("letter-spacing", "0.05em");

		// --- Row 3: Referee decision display (tall, above summon buttons) ---
		this.liveDecisions = new JuryDisplayDecisionElement();
		if (getFop() != null) {
			this.liveDecisions.setFop(getFop());
		}
		this.liveDecisions.setDisplaySize("large");
		this.liveDecisions.setSilenced(true);
		this.liveDecisions.getElement().setAttribute("theme", "dark");
		this.liveDecisions.getStyle().set("background-color", "black");
		this.liveDecisions.getStyle().set("font-size", "100%");
		Div refDecisionWrapper = new Div(this.liveDecisions);
		refDecisionWrapper.getStyle().set("width", "60%").set("height", "100%");
		Div refDecisionArea = new Div(refDecisionWrapper);
		refDecisionArea.setSizeFull();
		refDecisionArea.getStyle()
		        .set("background", "black")
		        .set("display", "flex")
		        .set("align-items", "center")
		        .set("justify-content", "center")
		        .set("grid-column", "1 / -1")
		        .set("grid-row", "3")
		        .set("overflow", "hidden");

		// --- Row 4: Summon buttons (compact) ---
		Button leftSummon = createKeypadButton("Left Referee\nSummon", "contrast", () -> summonReferee(1));
		leftSummon.getStyle().set("grid-column", "1").set("grid-row", "4");
		Button centerSummon = createKeypadButton("Center Referee\nSummon", "contrast", () -> summonReferee(2));
		centerSummon.getStyle().set("grid-column", "2").set("grid-row", "4");
		Button rightSummon = createKeypadButton("Right Referee\nSummon", "contrast", () -> summonReferee(3));
		rightSummon.getStyle().set("grid-column", "3").set("grid-row", "4");
		Button allSummon = createKeypadButton(Translator.translate("BreakButton.SummonReferees"), "contrast", () -> summonReferee(0));
		allSummon.getStyle().set("grid-column", "4").set("grid-row", "4");
		Button resumeBtn = createKeypadButton(Translator.translate("JuryNotification.END_JURY_BREAK"), "success primary", this::resumeCompetition);
		resumeBtn.getStyle().set("grid-column", "5").set("grid-row", "4");

		// --- Row 5: Spacer ---
		Div spacer = new Div();
		spacer.getStyle().set("grid-column", "1 / -1").set("grid-row", "5");

		// --- Row 6: Jury Decisions label ---
		Div juryLabel = new Div();
		juryLabel.setText("Jury Decisions");
		juryLabel.getStyle()
		        .set("grid-column", "1 / -1").set("grid-row", "6")
		        .set("color", "var(--lumo-secondary-text-color)")
		        .set("font-size", "0.85rem")
		        .set("font-weight", "bold")
		        .set("padding", "0.25rem 0 0 0.25rem")
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
		Button noLift = createKeypadButton(Translator.translate("JuryDialog.BadLiftLabel"), "error primary", () -> submitJuryDecision(false));
		noLift.getStyle().set("grid-column", "1").set("grid-row", "8");
		Button goodLift = createKeypadButton(Translator.translate("JuryDialog.GoodLiftLabel"), "success primary", () -> submitJuryDecision(true));
		goodLift.getStyle().set("grid-column", "2").set("grid-row", "8");
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
		grid.add(noLift, goodLift, deliberate, challenge, technical);

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
		button.addThemeVariants(ButtonVariant.LUMO_LARGE);
		button.getElement().setAttribute("theme", theme);
		button.setSizeFull();
		button.getStyle().set("white-space", "normal");
		button.getStyle().set("text-align", "center");
		button.getStyle().set("font-size", "clamp(0.7rem, 1.6vw, 1rem)");
		return button;
	}

	private int getNbJurors() {
		return Competition.getCurrent().getJurySize();
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
			fop.fopEventPost(new FOPEvent.BreakStarted(breakType, CountdownType.INDEFINITE, 0, null, true, this));
		});
	}

	private void startDeliberation() {
		startBreak(BreakType.JURY);
	}

	private void startTechnicalBreak() {
		startBreak(BreakType.TECHNICAL);
	}

	private void submitJuryDecision(boolean success) {
		OwlcmsSession.withFop(fop -> {
			if (fop.getAthleteUnderReview() == null) {
				return;
			}
			fop.fopEventPost(new FOPEvent.JuryDecision(fop.getAthleteUnderReview(), this, success, true));
		});
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