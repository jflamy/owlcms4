/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.function.BiPredicate;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;

import app.owlcms.apputils.SoundUtils;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.FOPParameters;
import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.elements.DecisionElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.RequireDisplayLogin;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.LiftingOrderUpdated;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class Scoreboard
 *
 * Show athlete 6-attempt results and leaders for the athlete's category
 *
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("results-template")
@JsModule("./components/Results.js")
@JsModule("./components/AudioContext.js")
@Route("displays/resultsLeaders")

public class Results extends PolymerTemplate<TemplateModel>
        implements DisplayParameters, SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle,
        RequireDisplayLogin {

	protected JsonArray cattempts;
	protected Group curGroup;
	protected Dialog dialog;
	protected JsonArray sattempts;
	protected List<Athlete> displayOrder;
	protected int liftsDone;

	@Id("timer")
	protected AthleteTimerElement timer; // Flow creates it
	@Id("breakTimer")
	private BreakTimerElement breakTimer; // Flow creates it
	@Id("decisions")
	private DecisionElement decisions; // Flow creates it

	private Category ceremonyCategory;
	private Group ceremonyGroup = null;
	private boolean darkMode = true;
	private boolean initializationNeeded;
	private Location location;
	private UI locationUI;
	private final Logger logger = (Logger) LoggerFactory.getLogger(Results.class);
	private boolean silenced = true;
	private boolean switchableDisplay = true;
	private boolean showRecords = true;
	protected EventBus uiEventBus;
	private final Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	protected Double emFontSize = null;
	private Timer dialogTimer;
	private boolean showLeaders;
	private boolean defaultLeadersDisplay;
	private boolean defaultRecordsDisplay;
	{
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	private String routeParameter;

	/**
	 * Instantiates a new results board.
	 */
	public Results() {
		OwlcmsFactory.waitDBInitialized();
		timer.setOrigin(this);
		setDarkMode(true);
		setDefaultLeadersDisplay(true);
		setDefaultRecordsDisplay(true);
		setLeadersDisplay(isDefaultLeadersDisplay());
		setRecordsDisplay(isDefaultRecordsDisplay());
		// js files add the build number to file names in order to prevent cache
		// collisions
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#addDialogContent(com.vaadin.flow.component.Component,
	 *      com.vaadin.flow.component.orderedlayout.VerticalLayout)
	 */
	@Override
	public void addDialogContent(Component target, VerticalLayout vl) {
		DisplayOptions.addLightingEntries(vl, target, this);
		DisplayOptions.addRule(vl);
		DisplayOptions.addSoundEntries(vl, target, this);
		DisplayOptions.addRule(vl);
		DisplayOptions.addSwitchableEntries(vl, target, this);
		DisplayOptions.addRule(vl);
		DisplayOptions.addSectionEntries(vl, target, this);
		DisplayOptions.addRule(vl);
		DisplayOptions.addSizingEntries(vl, target, this);
	}

	/**
	 * @see app.owlcms.uievents.BreakDisplay#doBreak(app.owlcms.uievents.UIEvent)
	 */
	@Override
	public void doBreak(UIEvent event) {
		doFopBreak();
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		ceremonyGroup = e.getCeremonyGroup();
		ceremonyCategory = e.getCeremonyCategory();
		// logger.trace("------ ceremony event = {} {}", e, e.getTrace());
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			if (e.getCeremonyType() == CeremonyType.MEDALS && this.isSwitchableDisplay() && ceremonyGroup != null) {
				Map<String, String> map = new HashMap<>(Map.of(
				        FOPParameters.FOP, fop.getName(),
				        FOPParameters.GROUP, ceremonyGroup.getName(),
				        DisplayParameters.DARK, Boolean.toString(darkMode)));
				if (ceremonyCategory != null) {
					map.put(DisplayParameters.CATEGORY, ceremonyCategory.getCode());
				} else {
					// logger.trace("no ceremonyCategory");
				}
				UI.getCurrent().navigate("displays/resultsMedals", QueryParameters.simple(map));
			}

			String title = inferGroupName() + " &ndash; "
			        + inferMessage(fop.getBreakType(), fop.getCeremonyType(), this.isSwitchableDisplay());
			this.getElement().setProperty("fullName", title);
			this.getElement().setProperty("teamName", "");
			this.getElement().setProperty("groupName", "");
			breakTimer.setVisible(!fop.getBreakTimer().isIndefinite());
			setDisplay(false);

			updateBottom(computeLiftType(fop.getCurAthlete()), fop);
			this.getElement().callJsFunction("doBreak");
		}));
	}

	/**
	 * return dialog, but only on first call.
	 *
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#getDialog()
	 */
	@Override
	public Dialog getDialog() {
		return dialog;
	}

	@Override
	public Timer getDialogTimer() {
		return this.dialogTimer;
	}

	@Override
	public Double getEmFontSize() {
		return emFontSize;
	}

	@Override
	public Location getLocation() {
		return this.location;
	}

	@Override
	public UI getLocationUI() {
		return this.locationUI;
	}

	@Override
	public String getPageTitle() {
		return getTranslation("ScoreboardWLeadersTitle") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	public String getRouteParameter() {
		return this.routeParameter;
	}

	@Override
	public boolean isDarkMode() {
		return darkMode;
	}

	@Override
	public boolean isDefaultLeadersDisplay() {
		return defaultLeadersDisplay;
	}

	@Override
	public boolean isDefaultRecordsDisplay() {
		return defaultRecordsDisplay;
	}

	@Override
	public boolean isIgnoreGroupFromURL() {
		return true;
	}

	@Override
	public boolean isLeadersDisplay() {
		return showLeaders;
	}

	@Override
	public boolean isRecordsDisplay() {
		return showRecords;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#isShowInitialDialog()
	 */
	@Override
	public boolean isShowInitialDialog() {
		return this.initializationNeeded;
	}

	@Override
	public boolean isSilenced() {
		return silenced;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#isSwitchableDisplay()
	 */
	@Override
	public boolean isSwitchableDisplay() {
		return switchableDisplay;
	}

	/**
	 * Reset.
	 */
	public void reset() {
		displayOrder = ImmutableList.of();
	}

	@Override
	public void setDarkMode(boolean dark) {
		this.darkMode = dark;
	}

	@Override
	public void setDefaultLeadersDisplay(boolean b) {
		this.defaultLeadersDisplay = b;
	}

	@Override
	public void setDefaultRecordsDisplay(boolean b) {
		this.defaultRecordsDisplay = b;
	}

	@Override
	public void setDialog(Dialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public void setDialogTimer(Timer timer) {
		this.dialogTimer = timer;
	}

	@Override
	public void setEmFontSize(Double emFontSize) {
		this.emFontSize = emFontSize;
		doChangeEmSize();
	}

	@Override
	public void setLeadersDisplay(boolean showLeaders) {
		this.showLeaders = showLeaders;
		if (showLeaders) {
			this.getElement().setProperty("leadersVisibility", "");
			this.getElement().setProperty("leadersLineHeight", "min-content");
		} else {
			this.getElement().setProperty("leadersVisibility", "visibility: hidden;");
			this.getElement().setProperty("leadersLineHeight", "0px");
		}
	}

	@Override
	public void setLocation(Location location) {
		this.location = location;
	}

	@Override
	public void setLocationUI(UI locationUI) {
		this.locationUI = locationUI;
	}

	@Override
	public void setRecordsDisplay(boolean showRecords) {
		this.showRecords = showRecords;
		if (showRecords) {
			this.getElement().setProperty("recordsDisplay", "display: block");
		} else {
			this.getElement().setProperty("recordsDisplay", "display: none");
		}
	}

	@Override
	public void setRouteParameter(String routeParameter) {
		this.routeParameter = routeParameter;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#setShowInitialDialog(boolean)
	 */
	@Override
	public void setShowInitialDialog(boolean b) {
		this.initializationNeeded = true;
	}

	@Override
	public void setSilenced(boolean silenced) {
		this.timer.setSilenced(silenced);
		this.breakTimer.setSilenced(silenced);
		this.decisions.setSilenced(silenced);
		this.silenced = silenced;
	}

	@Override
	public void setSwitchableDisplay(boolean warmUpDisplay) {
		this.switchableDisplay = warmUpDisplay;
	}

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
			Athlete a = e.getAthlete();
			setDisplay(false);
			if (a == null) {
				displayOrder = fop.getLiftingOrder();
				a = displayOrder.size() > 0 ? displayOrder.get(0) : null;
				liftsDone = AthleteSorter.countLiftsDone(displayOrder);
				doUpdate(a, e);
			} else {
				liftsDone = AthleteSorter.countLiftsDone(displayOrder);
				doUpdate(a, e);
			}
		}));
	}

	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		// logger.trace"------- slaveCeremonyDone {}", e.getCeremonyType());
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay(false);
			// revert to current break
			doBreak(null);
		});
	}

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		// logger.trace"------- slaveCeremonyStarted {}", e.getCeremonyType());
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay(false);
			doCeremony(e);
		});
	}

	@Subscribe
	public void slaveDecision(UIEvent.Decision e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setDisplay(false);
			doUpdateBottomPart(e);
			this.getElement().callJsFunction("refereeDecision");
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setDisplay(false);
			doUpdateBottomPart(e);
			this.getElement().callJsFunction("reset");
		});
	}

	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setDisplay(false);
			this.getElement().callJsFunction("down");
		});
	}

	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay(false);
			doDone(e.getGroup());
		});
	}

	@Subscribe
	public void slaveJuryNotification(UIEvent.JuryNotification e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay(false);
			if (e.getNewRecord()) {
				spotlightNewRecord();
			}
		});
	}

	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			Athlete a = e.getAthlete();
			displayOrder = getOrder(OwlcmsSession.getFop());
			liftsDone = AthleteSorter.countLiftsDone(displayOrder);
			doUpdate(a, e);
		});
	}

	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay(false);
			doBreak(e);
		});
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setDisplay(false);
			syncWithFOP();
		});
	}

	@Subscribe
	public void slaveStopBreak(UIEvent.BreakDone e) {
		uiLog(e);
//        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
//            setHidden(false);
//            Athlete a = e.getAthlete();
//            this.getElement().callJsFunction("reset");
//            doUpdate(a, e);
//        });
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			syncWithFOP();
		});
	}

	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			syncWithFOP(e);
		});
	}

	private String computeLiftType(Athlete a) {
		if (a == null || a.getAttemptsDone() > 6) {
			return null;
		}
		String liftType = a.getAttemptsDone() >= 3 ? Translator.translate("Clean_and_Jerk")
		        : Translator.translate("Snatch");
		return liftType;
	}

	private void doChangeEmSize() {
		if (getEmFontSize() != null) {
			this.getElement().setProperty("sizeOverride", " --tableFontSize:" + getEmFontSize() + "rem;");
		}
	}

	private void doDone(Group g) {
		logger.debug("doDone {}", g == null ? null : g.getName());
		if (g == null) {
			doEmpty();
		} else {
			OwlcmsSession.withFop(fop -> {
				this.getElement().setProperty("fullName", getTranslation("Group_number_results", g.toString()));
				this.getElement().callJsFunction("groupDone");
			});
		}
	}

	private void doFopBreak() {
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			String title = inferGroupName() + " &ndash; "
			        + inferMessage(fop.getBreakType(), fop.getCeremonyType(), this.isSwitchableDisplay());
			this.getElement().setProperty("fullName", title);
			this.getElement().setProperty("teamName", "");
			this.getElement().setProperty("attempt", "");
			this.getElement().setProperty("kgSymbol", Translator.translate("kgSymbol"));
			Athlete a = fop.getCurAthlete();

			this.getElement().setProperty("weight", "");
			boolean showWeight = false;
			Integer nextAttemptRequestedWeight = null;
			if (a != null) {
				nextAttemptRequestedWeight = a.getNextAttemptRequestedWeight();
			}
			if (fop.getCeremonyType() == null && a != null && nextAttemptRequestedWeight != null
			        && nextAttemptRequestedWeight > 0) {
				this.getElement().setProperty("weight", nextAttemptRequestedWeight);
				showWeight = true;
			}
			breakTimer.setVisible(!fop.getBreakTimer().isIndefinite());
			setDisplay(false);
			updateBottom(computeLiftType(a), fop);
			// logger.trace("doBreak results {} {} {}", fop.getCeremonyType(), a,
			// showWeight);
			this.getElement().callJsFunction("doBreak", showWeight);
		}));
	}

	private void doUpdateBottomPart(UIEvent e) {
		Athlete a = e.getAthlete();
		updateBottom(computeLiftType(a), OwlcmsSession.getFop());
	}

	private String formatAttempt(Integer attemptNo) {
		String translate = Translator.translate("AttemptBoard_attempt_number", (attemptNo % 3) + 1);
		return translate;
	}

	private String formatKg(String total) {
		return (total == null || total.trim().isEmpty()) ? "-"
		        : (total.startsWith("-") ? "(" + total.substring(1) + ")" : total);
	}

	private void init() {
		OwlcmsSession.withFop(fop -> {
			logger.trace("{}Starting result board on FOP {}", fop.getLoggingName());
			setId("scoreboard-" + fop.getName());
			curGroup = fop.getGroup();
			setWideTeamNames(false);
			this.getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
		});
		setTranslationMap();
		displayOrder = ImmutableList.of();
	}

	private void setDisplay(boolean hidden) {
		this.getElement().setProperty("hiddenBlockStyle", (hidden ? "display:none" : "display:block"));
		this.getElement().setProperty("inactiveBlockStyle", (hidden ? "display:block" : "display:none"));
		this.getElement().setProperty("hiddenGridStyle", (hidden ? "display:none" : "display:grid"));
		this.getElement().setProperty("inactiveGridStyle", (hidden ? "display:grid" : "display:none"));
		this.getElement().setProperty("inactiveClass", (hidden ? "bigTitle" : ""));
		this.getElement().setProperty("videoHeaderDisplay", (hidden || ! isVideo() ? "display:none" : "display:block"));
		this.getElement().setProperty("normalHeaderDisplay", (hidden || isVideo() ? "display:none" : "display:block"));
	}

	private boolean isVideo() {
		return routeParameter != null && routeParameter.contentEquals("video");
	}

	private void setWideTeamNames(boolean wide) {
		this.getElement().setProperty("teamWidthClass", (wide ? "wideTeams" : "narrowTeams"));
	}

	private boolean showCurrent(FieldOfPlay fop) {
		// blink only on warmup scoreboards during ceremonies/breaks
//        if (fop.getState() == FOPState.BREAK) {
//            if (isSwitchableDisplay() && (fop.getBreakType() == BreakType.FIRST_SNATCH
//                    || fop.getBreakType() == BreakType.FIRST_CJ
//                    || fop.getBreakType() == BreakType.GROUP_DONE)) {
//                return false;
//            }
//        }

		if (isSwitchableDisplay() && fop.getState() == FOPState.BREAK && fop.getCeremonyType() != null) {
			return false;
		}
		return true;
	}

	private void spotlightNewRecord() {
		this.getElement().setProperty("recordKind", "new");
		this.getElement().setProperty("recordMessage", Translator.translate("Scoreboard.NewRecord"));
	}

	private void spotlightRecordAttempt() {
		this.getElement().setProperty("recordKind", "attempt");
		this.getElement().setProperty("recordMessage",
		        Translator.translate("Scoreboard.RecordAttempt"));
	}

	private void spotlightRecords(FieldOfPlay fop) {
		if (fop.getNewRecords() != null && !fop.getNewRecords().isEmpty()) {
			spotlightNewRecord();
		} else if (fop.getChallengedRecords() != null && !fop.getChallengedRecords().isEmpty()) {
			spotlightRecordAttempt();
		} else {
			this.getElement().setProperty("recordKind", "none");
		}
	}

	private void syncWithFOP() {
		OwlcmsSession.withFop(fop -> {
			syncWithFOP(new UIEvent.SwitchGroup(fop.getGroup(), fop.getState(), fop.getCurAthlete(), this));
		});

	}

	private void syncWithFOP(UIEvent.SwitchGroup e) {
		switch (OwlcmsSession.getFop().getState()) {
		case INACTIVE:
			doEmpty();
			break;
		case BREAK:
			if (e.getGroup() == null) {
				doEmpty();
			} else {
				doUpdate(e.getAthlete(), e);
				doBreak(e);
			}
			break;
		default:
			setDisplay(false);
			doUpdate(e.getAthlete(), e);
		}
	}

	protected void computeLeaders(boolean done) {
		OwlcmsSession.withFop(fop -> {
			Athlete curAthlete = fop.getCurAthlete();
			if (curAthlete != null && curAthlete.getGender() != null) {
				this.getElement().setProperty("categoryName", curAthlete.getCategory().getName());

				if (Competition.getCurrent().isSinclair()) {
					List<Athlete> sortedAthletes = new ArrayList<>(
					        Competition.getCurrent().getGlobalSinclairRanking(curAthlete.getGender()));
					displayOrder = AthleteSorter.topSinclair(sortedAthletes, 3).topAthletes;
					this.getElement().setProperty("categoryName", Translator.translate("sinclair"));
				} else {
					displayOrder = fop.getLeaders();
				}
				if ((!done || Competition.getCurrent().isSinclair()) && displayOrder != null
				        && displayOrder.size() > 0) {
					// null as second argument because we do not highlight current athletes in the
					// leaderboard
					this.getElement().setPropertyJson("leaders", getAthletesJson(displayOrder, null, fop));
					this.getElement().setProperty("leaderLines", displayOrder.size() + 2); // spacer + title
				} else {
					// nothing to show
					this.getElement().setPropertyJson("leaders", Json.createNull());
					this.getElement().setProperty("leaderLines", 1); // must be > 0
				}
			}
		});
	}

	protected void computeRecords(boolean done) {
		// always compute
//        if (!this.isRecordsDisplay()) {
//            this.getElement().setPropertyJson("records", Json.createNull());
//            return;
//        }
		OwlcmsSession.withFop(fop -> {
			Athlete curAthlete = fop.getCurAthlete();
			if (curAthlete != null && curAthlete.getGender() != null) {
				if (!done && showCurrent(fop)) {
					this.getElement().setPropertyJson("records", fop.getRecordsJson());
				} else {
					// nothing to show
					this.getElement().setPropertyJson("records", Json.createNull());
				}
			}
		});
	}

	/**
	 * @param groupAthletes, List<Athlete> liftOrder
	 * @return
	 */
	protected int countCategories(List<Athlete> displayOrder) {
		int nbCats = 0;
		Category prevCat = null;
		List<Athlete> athletes = displayOrder != null ? Collections.unmodifiableList(displayOrder)
		        : Collections.emptyList();
		for (Athlete a : athletes) {
			Category curCat = a.getCategory();
			if (curCat != null && !curCat.sameAs(prevCat)) {
				// changing categories, put marker before athlete
				prevCat = curCat;
				nbCats++;
			}
		}
		return nbCats;
	}

	protected int countSubsets(List<Athlete> order) {
		return countCategories(order) + 1;
	}

	protected void doEmpty() {
		this.setDisplay(true);
	}

	protected void doUpdate(Athlete a, UIEvent e) {
		// logger.trace("doUpdate {} {} {}", e != null ? e.getClass().getSimpleName() :
		// "no event", a, a != null ?
		// a.getAttemptsDone() : null);
		boolean leaveTopAlone = false;
		if (e instanceof UIEvent.LiftingOrderUpdated) {
			LiftingOrderUpdated e2 = (UIEvent.LiftingOrderUpdated) e;
			if (e2.isInBreak()) {
				leaveTopAlone = !e2.isDisplayToggle();
				this.getElement().setProperty("weight", a.getNextAttemptRequestedWeight());
				doBreak(e);
			} else {
				leaveTopAlone = !e2.isCurrentDisplayAffected();
			}
		}

		FieldOfPlay fop = OwlcmsSession.getFop();
		if (!leaveTopAlone) {
			if (a != null) {
				Group group = fop.getGroup();
				if (group != null && !group.isDone()) {
					// logger.debug("updating top {} {} {}", a.getFullName(), group,
					// System.identityHashCode(group));
					this.getElement().setProperty("fullName", a.getFullName());
					this.getElement().setProperty("teamName", a.getTeam());
					this.getElement().setProperty("startNumber", a.getStartNumber());
					String formattedAttempt = formatAttempt(a.getAttemptsDone());
					this.getElement().setProperty("attempt", formattedAttempt);
					this.getElement().setProperty("weight", a.getNextAttemptRequestedWeight());
				} else {
					logger.debug("group done {} {}", group, System.identityHashCode(group));
					doBreak(e);
				}
			}
			this.getElement().callJsFunction("reset");
		}
		// logger.debug("updating bottom");
		updateBottom(computeLiftType(a), fop);
	}

	protected String formatInt(Integer total) {
		if (total == null || total == 0) {
			return "-";
		} else if (total == -1) {
			return "inv.";// invited lifter, not eligible.
		} else if (total < 0) {
			return "(" + Math.abs(total) + ")";
		} else {
			return total.toString();
		}
	}

	protected String formatRank(Integer total) {
		if (total == null || total == 0) {
			return "&nbsp;";
		} else if (total == -1) {
			return "inv.";// invited lifter, not eligible.
		} else {
			return total.toString();
		}
	}

	protected JsonArray getAgeGroupNamesJson(LinkedHashMap<String, Participation> currentAthleteParticipations) {
		JsonArray ageGroups = Json.createArray();
		return ageGroups;
	}

	protected void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank, FieldOfPlay fop) {
		String category;
		category = curCat != null ? curCat.getName() : "";
		ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
		ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
		ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
		Integer startNumber = a.getStartNumber();
		ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
		ja.put("category", category != null ? category : "");
		getAttemptsJson(a, liftOrderRank, fop);
		ja.put("sattempts", sattempts);
		ja.put("bestSnatch", formatInt(a.getBestSnatch()));
		ja.put("cattempts", cattempts);
		ja.put("bestCleanJerk", formatInt(a.getBestCleanJerk()));
		ja.put("total", formatInt(a.getTotal()));
		Participation mainRankings = a.getMainRankings();
		if (mainRankings != null) {
			ja.put("snatchRank", formatRank(mainRankings.getSnatchRank()));
			ja.put("cleanJerkRank", formatRank(mainRankings.getCleanJerkRank()));
			ja.put("totalRank", formatRank(mainRankings.getTotalRank()));
		} else {
			logger.error("main rankings null for {}", a);
		}
		ja.put("group", a.getGroup() != null ? a.getGroup().getName() : "");
		Double double1 = a.getAttemptsDone() <= 3 ? a.getSinclairForDelta()
		        : a.getSinclair();
		ja.put("sinclair", double1 > 0.001 ? String.format("%.3f", double1) : "-");
		ja.put("custom1", a.getCustom1() != null ? a.getCustom1() : "");
		ja.put("custom2", a.getCustom2() != null ? a.getCustom2() : "");
		ja.put("sinclairRank", a.getSinclairRank() != null && a.getSinclairRank() > 0 ? "" + a.getSinclairRank() : "-");

		boolean notDone = a.getAttemptsDone() < 6;
		String blink = (notDone ? " blink" : "");
		String highlight = "";
		if (fop.getState() != FOPState.DECISION_VISIBLE && notDone && showCurrent(fop)) {
			switch (liftOrderRank) {
			case 1:
				highlight = (" current" + blink);
				break;
			case 2:
				highlight = " next";
				break;
			default:
				highlight = "";
			}
		}
		// logger.debug("{} {} {}", a.getShortName(), fop.getState(), highlight);
		ja.put("classname", highlight);
	}

	/**
	 * @param groupAthletes, List<Athlete> liftOrder
	 * @return
	 */
	protected JsonValue getAthletesJson(List<Athlete> displayOrder, List<Athlete> liftOrder, FieldOfPlay fop) {
		JsonArray jath = Json.createArray();
		int athx = 0;

		Athlete prevAthlete = null;
		long currentId = (liftOrder != null && liftOrder.size() > 0) ? liftOrder.get(0).getId() : -1L;
		long nextId = (liftOrder != null && liftOrder.size() > 1) ? liftOrder.get(1).getId() : -1L;
		List<Athlete> athletes = displayOrder != null ? Collections.unmodifiableList(displayOrder)
		        : Collections.emptyList();
		for (Athlete a : athletes) {
			JsonObject ja = Json.createObject();
			if (getSeparatorPredicate().test(a, prevAthlete)) {
				// changing categories, put marker before athlete
				ja.put("isSpacer", true);
				jath.set(athx, ja);
				ja = Json.createObject();
				athx++;
			}
			// compute the blinking rank (1 = current, 2 = next)
			getAthleteJson(a, ja, a.getCategory(), (a.getId() == currentId)
			        ? 1
			        : ((a.getId() == nextId)
			                ? 2
			                : 0),
			        fop);
			String team = a.getTeam();
			if (team != null && team.trim().length() > Competition.SHORT_TEAM_LENGTH) {
				setWideTeamNames(true);
			}
			jath.set(athx, ja);
			athx++;
			prevAthlete = a;
		}
		return jath;
	}

	/**
	 * Compute Json string ready to be used by web component template
	 *
	 * CSS classes are pre-computed and passed along with the values; weights are
	 * formatted.
	 *
	 * @param a
	 * @param fop
	 * @return json string with nested attempts values
	 */
	protected void getAttemptsJson(Athlete a, int liftOrderRank, FieldOfPlay fop) {
		sattempts = Json.createArray();
		cattempts = Json.createArray();
		XAthlete x = new XAthlete(a);
		Integer curLift = x.getAttemptsDone();
		int ix = 0;
		for (LiftInfo i : x.getRequestInfoArray()) {
			JsonObject jri = Json.createObject();
			String stringValue = i.getStringValue();
			boolean notDone = x.getAttemptsDone() < 6;
			String blink = (notDone ? " blink" : "");

			jri.put("goodBadClassName", "narrow empty");
			jri.put("stringValue", "");
			if (i.getChangeNo() >= 0) {
				String trim = stringValue != null ? stringValue.trim() : "";
				switch (Changes.values()[i.getChangeNo()]) {
				case ACTUAL:
					if (!trim.isEmpty()) {
						if (trim.contentEquals("-") || trim.contentEquals("0")) {
							jri.put("goodBadClassName", "narrow fail");
							jri.put("stringValue", "-");
						} else {
							boolean failed = stringValue != null && stringValue.startsWith("-");
							jri.put("goodBadClassName", failed ? "narrow fail" : "narrow good");
							jri.put("stringValue", formatKg(stringValue));
						}
					}
					break;
				default:
					if (stringValue != null && !trim.isEmpty()) {
						// logger.debug("{} {} {}", fop.getState(), x.getShortName(), curLift);

						String highlight = "";
						// don't blink while decision is visible. wait until lifting displayOrder has
						// been
						// recomputed and we get DECISION_RESET
						int liftBeingDisplayed = i.getLiftNo();
						if (liftBeingDisplayed == curLift && (fop.getState() != FOPState.DECISION_VISIBLE)
						        && showCurrent(fop)) {
							switch (liftOrderRank) {
							case 1:
								highlight = (" current" + blink);
								break;
							case 2:
								highlight = " next";
								break;
							default:
								highlight = "";
							}
						}
						jri.put("goodBadClassName", "narrow request");
						if (notDone) {
							jri.put("className", highlight);
						}
						jri.put("stringValue", stringValue);
					}
					break;
				}
			}

			if (ix < 3) {
				sattempts.set(ix, jri);
			} else {
				cattempts.set(ix % 3, jri);
			}
			ix++;
		}
	}

	protected String getDisplayType() {
		return "";
	}

	protected List<Athlete> getOrder(FieldOfPlay fop) {
		return fop.getDisplayOrder();
	}

	/**
	 * @return the separator
	 */
	protected BiPredicate<Athlete, Athlete> getSeparatorPredicate() {
		BiPredicate<Athlete, Athlete> separator = (cur, prev) -> prev == null
		        || (cur.getCategory() != null
		                && !cur.getCategory().sameAs(prev.getCategory()));
		return separator;
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
	 * AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via FOPParameters interface default methods.
		OwlcmsSession.withFop(fop -> {
			init();

			// get the global category rankings (attached to each athlete)
			displayOrder = getOrder(fop);

			liftsDone = AthleteSorter.countLiftsDone(displayOrder);
			syncWithFOP(new UIEvent.SwitchGroup(fop.getGroup(), fop.getState(), fop.getCurAthlete(), this));
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});

		if (Competition.getCurrent().isSinclair()) {
			getElement().setProperty("noLiftRanks", "noranks sinclair");
		} else if (!Competition.getCurrent().isSnatchCJTotalMedals()) {
			getElement().setProperty("noLiftRanks", "noranks nosinclair");
		} else {
			getElement().setProperty("noLiftRanks", "nosinclair");
		}
		this.getElement().setProperty("video", routeParameter != null ? routeParameter+"/" : "");
		SoundUtils.enableAudioContextNotification(this.getElement());
		storeReturnURL();
	}

	protected void setTranslationMap() {
		JsonObject translations = Json.createObject();
		Enumeration<String> keys = Translator.getKeys();
		while (keys.hasMoreElements()) {
			String curKey = keys.nextElement();
			if (curKey.startsWith("Scoreboard.")) {
				translations.put(curKey.replace("Scoreboard.", ""), Translator.translate(curKey));
			}
		}
		this.getElement().setPropertyJson("t", translations);
	}

	protected void uiLog(UIEvent e) {
//        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(), e.getOrigin(), LoggerUtils.whereFrom());
	}

	protected void updateBottom(String liftType, FieldOfPlay fop) {
		curGroup = fop.getGroup();
		String groupDescription = curGroup != null ? curGroup.getDescription() : null;
		displayOrder = getOrder(fop);
		spotlightRecords(fop);

		doChangeEmSize();
		if (liftType != null && curGroup != null) {
			this.getElement().setProperty("displayType", getDisplayType());
		}
		if (liftType != null) {
			this.getElement().setProperty("groupName",
			        curGroup != null
			                ? Translator.translate("Scoreboard.GroupLiftType", curGroup.getName(), liftType)
			                : "");
			liftsDone = AthleteSorter.countLiftsDone(displayOrder);
			if (isSwitchableDisplay() && groupDescription != null) {
				this.getElement().setProperty("liftsDone", groupDescription);
			} else {
				this.getElement().setProperty("liftsDone", Translator.translate("Scoreboard.AttemptsDone", liftsDone));
			}
		} else {
			if (isSwitchableDisplay() && groupDescription != null) {
				this.getElement().setProperty("liftsDone", groupDescription);
			}
			this.getElement().setProperty("groupName", "");
			this.getElement().callJsFunction("groupDone");
		}
		this.getElement().setPropertyJson("ageGroups", getAgeGroupNamesJson(fop.getAgeGroupMap()));
		this.getElement().setPropertyJson("athletes",
		        getAthletesJson(displayOrder, fop.getLiftingOrder(), fop));

		List<Athlete> order = getOrder(OwlcmsSession.getFop());
		int resultLines = (order != null ? order.size() : 0) + countSubsets(order);
		this.getElement().setProperty("resultLines", resultLines);
		boolean done = fop.getState() == FOPState.BREAK && fop.getBreakType() == BreakType.GROUP_DONE;
		computeLeaders(done);
		computeRecords(done);
		if (!showCurrent(fop)) {
			this.getElement().callJsFunction("groupDone");
		}
	}
}