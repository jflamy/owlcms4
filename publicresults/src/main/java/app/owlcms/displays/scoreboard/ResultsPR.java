/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-Fran�ois Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.Timer;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.components.elements.AthleteTimerElementPR;
import app.owlcms.components.elements.BreakTimerElementPR;
import app.owlcms.components.elements.DecisionElementPR;
import app.owlcms.components.elements.unload.UnloadObserverPR;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.i18n.Translator;
import app.owlcms.prutils.SafeEventBusRegistrationPR;
import app.owlcms.prutils.SoundUtils;
import app.owlcms.publicresults.DecisionReceiverServlet;
import app.owlcms.publicresults.TimerReceiverServlet;
import app.owlcms.publicresults.UpdateReceiverServlet;
import app.owlcms.uievents.BreakTimerEvent;
import app.owlcms.uievents.BreakTimerEvent.BreakStart;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.DecisionEvent;
import app.owlcms.uievents.DecisionEventType;
import app.owlcms.uievents.UpdateEvent;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.impl.JreJsonFactory;

/**
 * Class Scoreboard
 *
 * Show athlete 6-attempt results
 *
 */
@Tag("results-template-pr")
@JsModule("./components/ResultsPR.js")
@JsModule("./components/AudioContext.js")
@Route("results")

public class ResultsPR extends LitTemplate
        implements DisplayParameters, HasDynamicTitle, SafeEventBusRegistrationPR {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(ResultsPR.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    @Id("timer-pr")
    private AthleteTimerElementPR timer; // Flow creates it

    @Id("breaktimer-pr")
    private BreakTimerElementPR breakTimer; // Flow creates it

    @Id("decisions-pr")
    private DecisionElementPR decisions; // Flow creates it

    private boolean darkMode;
    private Location location;
    private UI locationUI;
    private boolean needReset = false;
    private boolean decisionVisible;
    private UI ui;

    protected Dialog dialog;
    private Timer dialogTimer;
    private Double emFontSize;
    private boolean initializationNeeded;
    private boolean silenced;
    private String fopName;
    private boolean recordsDisplay;
    private boolean showLeaders;
    private boolean defaultRecordsDisplay;
    private boolean defaultLeadersDisplay;
    private boolean liftingOrder;
    private boolean done;
    private int lastHashCode;

    /**
     * Instantiates a new results board.
     * 
     * @throws InterruptedException
     */
    public ResultsPR() throws InterruptedException {
        setDarkMode(true);
        setDefaultLeadersDisplay(true);
        setDefaultRecordsDisplay(true);
        setDefaultLiftingOrderDisplay(false);
        setShowInitialDialog(false);
        this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
        logger.warn("created ResultsPR");
    }

    @Override
    public void addDialogContent(Component target, VerticalLayout vl) {
        DisplayOptions.addLightingEntries(vl, target, this);
        DisplayOptions.addRule(vl);
        DisplayOptions.addSoundEntries(vl, target, this);
        DisplayOptions.addRule(vl);
        DisplayOptions.addSectionEntries(vl, target, this);
    }

    @Override
    public Dialog getDialog() {
        return this.dialog;
    }

    @Override
    public Timer getDialogTimer() {
        return this.dialogTimer;
    }

    @Override
    public Double getEmFontSize() {
        return this.emFontSize;
    }

    @Override
    public String getFopName() {
        return this.fopName;
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
        return Translator.translate("ScoreboardWLeadersTitle") + getFopName() != null ? (" " + getFopName()) : "";
    }

    @Override
    public boolean isDarkMode() {
        return this.darkMode;
    }

    @Override
    public boolean isDefaultLeadersDisplay() {
        return this.defaultLeadersDisplay;
    }

    @Override
    public boolean isDefaultRecordsDisplay() {
        return this.defaultRecordsDisplay;
    }

    @Override
    public boolean isIgnoreGroupFromURL() {
        return true;
    }

    @Override
    public boolean isLiftingOrder() {
        return this.liftingOrder;
    }

    @Override
    public boolean isRecordsDisplay() {
        return this.recordsDisplay;
    }

    @Override
    public boolean isShowInitialDialog() {
        return this.isInitializationNeeded();
    }

    @Override
    public boolean isShowLeaders() {
        return this.showLeaders;
    }

    @Override
    public boolean isSilenced() {
        return this.silenced;
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
    public void setDefaultLiftingOrderDisplay(boolean b) {
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
    }

    @Override
    public void setFopName(String decoded) {
        this.fopName = decoded;
    }

    @Override
    public void setLiftingOrder(boolean liftingOrder) {
        this.liftingOrder = liftingOrder;
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
        this.recordsDisplay = showRecords;
        this.getElement().setProperty("showRecords", showRecords);
    }

    /**
     * @see app.owlcms.apputils.queryparameters.DisplayParameters#setShowInitialDialog(boolean)
     */
    @Override
    public void setShowInitialDialog(boolean b) {
        // logger.debug("setShowInitialDialog {} {}",b, LoggerUtils.stackTrace());
        this.setInitializationNeeded(true);
    }

    @Override
    public void setShowLeaders(boolean showLeaders) {
        this.showLeaders = showLeaders;
        this.getElement().setProperty("showLeaders", showLeaders);
        if (!showLeaders || this.done) {
            logger.debug("setLeadersDisplay 0px: isLeaders = {} done = {}", showLeaders, this.done);
            this.getElement().setProperty("leaderFillerHeight", "--leaderFillerHeight: 0px");
        } else {
            logger.debug("setLeadersDisplay default: isLeaders = {} done = {}", showLeaders, this.done);
            this.getElement().setProperty("leaderFillerHeight",
                    "--leaderFillerHeight: var(--defaultLeaderFillerHeight)");
        }
    }

    @Override
    public void setSilenced(boolean silenced) {
        this.timer.setSilenced(silenced);
        this.breakTimer.setSilenced(silenced);
        this.decisions.setSilenced(silenced);
        this.silenced = silenced;
    }

    @Subscribe
    public void slaveBreakDone(BreakTimerEvent.BreakDone e) {
        this.ui.access(() -> {
            setBoardMode(e.getMode());
        });
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        logger.debug("### received BreakDone {}");
        this.needReset = false;
    }

    @Subscribe
    public void slaveDecisionEvent(DecisionEvent e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        // logger.debug("### Results received DecisionEvent {} {} {}", e.getEventType(),
        // e.getRecordKind(),
        // e.getRecordMessage());
        DecisionEventType eventType = e.getEventType();
        switch (eventType) {
            case DOWN_SIGNAL:
                this.decisionVisible = true;
                if (this.ui == null || this.ui.isClosing()) {
                    return;
                }
                this.ui.access(() -> {
                    // setBoardMode(e.getMode());
                    this.getElement().setProperty("decisionVisible", true);
                });
                break;
            case RESET:
                this.decisionVisible = false;
                if (this.ui == null || this.ui.isClosing()) {
                    return;
                }
                this.ui.access(() -> {
                    setBoardMode(e.getMode());
                    this.getElement().setProperty("decisionVisible", false);
                });
                break;
            case FULL_DECISION:
                this.decisionVisible = true;
                if (this.ui == null || this.ui.isClosing()) {
                    return;
                }
                this.ui.access(() -> {
                    setBoardMode(e.getMode());
                    this.getElement().setProperty("decisionVisible", true);
                    this.getElement().setProperty("recordKind", e.getRecordKind());
                    this.getElement().setProperty("recordMessage", e.getRecordMessage());
                });
                break;
            default:
                break;
        }
    }

    @Subscribe
    public void slaveUpdateEvent(UpdateEvent e) {
        // ignore identical updates
        if (e.getHashCode() == this.lastHashCode) {
            return;
        }
        this.lastHashCode = e.getHashCode();

        if (StartupUtils.isDebugSetting()) {
            logger./**/warn("### {} received UpdateEvent {} {} {}", System.identityHashCode(this), getFopName(),
                    e.getFopName(), e);
        }
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        String fopState = e.getFopState();
        BreakType breakType = e.getBreakType();
        String stylesDir = e.getStylesDir();

        this.ui.access(() -> {
            this.getElement().setProperty("stylesDir", stylesDir);
            this.getElement().setProperty("done", e.isDone());
            setDone(e.isDone());

            setBoardMode(e.getMode());
            String groupInfo = e.getGroupInfo();
            String description = null;
            if (groupInfo != null) {
                description = e.getGroupDescription();
                if (description == null) {
                    description = Translator.translate("Group_number", groupInfo);
                }
            }
            this.getElement().setProperty("groupDescription", description != null ? description : "");

            String athletes = e.getAthletes();
            if (isLiftingOrder()) {
                athletes = e.getLiftingOrderAthletes();
            }

            String leaders = e.getLeaders();
            String records = e.getRecords();
            String translationMap = e.getTranslationMap();

            JreJsonFactory jreJsonFactory = new JreJsonFactory();

            if (athletes != null) {
                JsonArray athleteList = (JsonArray) jreJsonFactory.parse(athletes);
                this.getElement().setPropertyJson("athletes", athleteList);
                this.getElement().setProperty("resultLines", athleteList.length() + 1);
            } else {
                this.getElement().setPropertyJson("athletes", Json.createNull());
                this.getElement().setProperty("resultLines", 1);
            }

            if (leaders != null && (breakType != BreakType.GROUP_DONE || e.isSinclairMeet())) {
                JsonArray leaderList = (JsonArray) jreJsonFactory.parse(leaders);
                this.getElement().setPropertyJson("leaders", leaderList);
                this.getElement().setProperty("leaderLines", leaderList.length() + 1);
            } else {
                this.getElement().setPropertyJson("leaders", Json.createNull());
                this.getElement().setProperty("leaderLines", 1);
            }

            if (records != null) {
                // logger.debug("records = {}", records);
                JsonObject recordList = (JsonObject) jreJsonFactory.parse(records);
                this.getElement().setPropertyJson("records", recordList);
                this.getElement().setProperty("recordKind", e.getRecordKind());
                this.getElement().setProperty("recordMessage", e.getRecordMessage());
            } else {
                // logger.debug("null records = {}", records);
                this.getElement().setPropertyJson("records", Json.createNull());
            }

            this.getElement().setPropertyJson("t",
                    translationMap != null ? jreJsonFactory.parse(translationMap) : Json.createNull());

            // following two are fixed in owlcms
            getElement().setProperty("showTotal", true);
            getElement().setProperty("showBest", true);

            getElement().setProperty("showLiftRanks", e.isShowLiftRanks());
            getElement().setProperty("showTotalRank", e.isShowTotalRank());
            getElement().setProperty("showSinclair", e.isShowSinclair());
            getElement().setProperty("showSinclairRanks", e.isShowSinclairRank());

            getElement().setProperty("competitionName", e.getCompetitionName());
            getElement().setProperty("attempt", e.getAttempt());
            getElement().setProperty("fullName", e.getFullName());
            getElement().setProperty("groupInfo", e.getGroupInfo());
            getElement().setProperty("startNumber", e.getStartNumber());
            getElement().setProperty("teamName", e.getTeamName());
            getElement().setProperty("weight", e.getWeight() != null ? e.getWeight() : 0);
            getElement().setProperty("categoryName", e.getCategoryName());
            setWideTeamNames(e.getWideTeamNames());
            String liftsDone = e.getLiftsDone();
            getElement().setProperty("liftsDone", " \u2013 " + liftsDone);

            if (StartupUtils.isDebugSetting()) {
                logger./**/warn("### state {} {}", fopState, e.getBreakType());
            }

            if (this.decisionVisible) {
                // wait for next event before doing anything.
                logger.debug("### waiting for decision reset");
            } else if ("INACTIVE".equals(fopState)
                    || ("BREAK".equals(fopState) && e.getBreakType() == BreakType.GROUP_DONE)) {
                logger.debug("### not in a group");
                doDone(e.getFullName());
                this.needReset = true;
            } else if ("BREAK".equals(fopState)) {
                logger.debug("### in a break {}", e.getBreakType());
                // also trigger a break timer event to make sure we are in sync with owlcms
                BreakStart breakStart = new BreakStart(e.getBreakRemaining(), e.isIndefinite());
                breakStart.setFopName(e.getFopName());
                TimerReceiverServlet.getEventBus().post(breakStart);
                this.needReset = true;
            } else if (!this.needReset) {
                // logger.debug("no reset");
            } else {
                logger.debug("### resetting becase of ranking update");
                // this.getElement().callJsFunction("reset");
                this.needReset = false;
            }
        });
    }

    protected boolean isVideo() {
        return false;
    }

    /**
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (!this.isSilenced()) {
            SoundUtils.enableAudioContextNotification(this.getElement());
        }

        this.ui = UI.getCurrent();
        eventBusRegister(this, UpdateReceiverServlet.getEventBus());

        // setDarkMode(this, isDarkMode(), false);
        String fopName2 = getFopName();
        logger.warn("sync {}",fopName2);
        UpdateEvent initEvent = UpdateReceiverServlet.sync(fopName2);
        //FIXME: set timers based on last received timer event.
        if (initEvent != null) {
            slaveUpdateEvent(initEvent);
            this.timer.slaveOrderUpdated(initEvent);
        } else {
            getElement().setProperty("fulName", Translator.translate("WaitingForSite"));
            getElement().setProperty("groupInfo", "");
        }
    }

    /**
     * @see com.vaadin.flow.component.Component#onDetach(com.vaadin.flow.component.DetachEvent)
     */
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        try {
            UpdateReceiverServlet.getEventBus().unregister(this);
        } catch (Exception e) {
        }
        try {
            DecisionReceiverServlet.getEventBus().unregister(this);
        } catch (Exception e) {
        }
        try {
            TimerReceiverServlet.getEventBus().unregister(this);
        } catch (Exception e) {
        }
    }

    private void doDone(String str) {
        getElement().setProperty("fullName", str);
        getElement().setProperty("liftsDone", "\u00a0");
    }

    @SuppressWarnings("unused")
    private String inferMessage(BreakType bt) {
        if (bt == null) {
            return Translator.translate("PublicMsg.CompetitionPaused");
        }
        switch (bt) {
            case FIRST_CJ:
                return Translator.translate("PublicMsg.TimeBeforeCJ");
            case FIRST_SNATCH:
                return Translator.translate("PublicMsg.TimeBeforeSnatch");
            case BEFORE_INTRODUCTION:
                return Translator.translate("PublicMsg.BeforeIntroduction");
            case TECHNICAL:
                return Translator.translate("PublicMsg.CompetitionPaused");
            case JURY:
                return Translator.translate("PublicMsg.JuryDeliberation");
            case GROUP_DONE:
                return Translator.translate("PublicMsg.GroupDone");
            default:
                return "";
        }
    }

    private boolean isInitializationNeeded() {
        return this.initializationNeeded;
    }

    private void setBoardMode(String mode) {
        // logger.debug("set board mode {} from {}", mode, LoggerUtils.whereFrom());
        this.getElement().setProperty("mode", mode);
    }

    private void setDone(boolean done) {
        this.done = done;
    }

    private void setInitializationNeeded(boolean initializationNeeded) {
        this.initializationNeeded = initializationNeeded;
    }

    private void setWideTeamNames(boolean wide) {
        this.getElement().setProperty("teamWidthClass", (wide ? "wideTeams" : "narrowTeams"));
    }
    
    @ClientCallable
    public void visibilityStatus(boolean visible) {
        UI ui = UI.getCurrent();
        logger.warn("visibilityStatus: {} {} {}",visible,this.getClass().getSimpleName(),System.identityHashCode(this));
        UnloadObserverPR eventObserver = UnloadObserverPR.get();
        if (visible) {
            eventObserver.setActivityTime(ui, this);
        } else {
            eventObserver.setInactivityTime(ui, this);
        }     
    }
    
}
