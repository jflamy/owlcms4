/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.Timer;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.components.elements.AthleteTimerElementPR;
import app.owlcms.components.elements.BreakTimerElementPR;
import app.owlcms.components.elements.DecisionElementPR;
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
@Tag("resultsPR-template")
@JsModule("./components/ResultsPR.js")
@JsModule("./components/AudioContext.js")
@Route("results")
@Theme(value = Lumo.class, variant = Lumo.DARK)
@Push
public class ResultsPR extends PolymerTemplate<TemplateModel>
        implements DisplayParameters, HasDynamicTitle, SafeEventBusRegistrationPR {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(ResultsPR.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    @Id("timer")
    private AthleteTimerElementPR timer; // Flow creates it

    @Id("breakTimer")
    private BreakTimerElementPR breakTimer; // Flow creates it

    @Id("decisions")
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
    private boolean leadersDisplay;
    private boolean defaultRecordsDisplay;
    private boolean defaultLeadersDisplay;

    /**
     * Instantiates a new results board.
     */
    public ResultsPR() {
        setDarkMode(true);
        setDefaultLeadersDisplay(true);
        setDefaultRecordsDisplay(true);
    }

    @Override
    public void addDialogContent(Component target, VerticalLayout vl) {
        DisplayOptions.addLightingEntries(vl, target, this);
        DisplayOptions.addRule(vl);
        DisplayOptions.addSoundEntries(vl, target, this);
//        DisplayOptions.addRule(vl);
//        DisplayOptions.addSwitchableEntries(vl, target, this);
        DisplayOptions.addRule(vl);
        DisplayOptions.addSectionEntries(vl, target, this);
//        DisplayOptions.addRule(vl);
//        DisplayOptions.addSizingEntries(vl, target, this);
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
        return emFontSize;
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
        return getTranslation("ScoreboardWLeadersTitle") + getFopName() != null ? (" " + getFopName()) : "";
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
        return leadersDisplay;
    }

    @Override
    public boolean isRecordsDisplay() {
        return recordsDisplay;
    }

    @Override
    public boolean isShowInitialDialog() {
        return this.initializationNeeded;
    }

    @Override
    public boolean isSilenced() {
        return silenced;
    }

    @Override
    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
    }

    @Override
    public void setDefaultLeadersDisplay(boolean b) {
        this.defaultLeadersDisplay = true;
    }

    @Override
    public void setDefaultRecordsDisplay(boolean b) {
        this.defaultRecordsDisplay = true;
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
    public void setLeadersDisplay(boolean showLeaders) {
        this.leadersDisplay = showLeaders;
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
        this.recordsDisplay = showRecords;
        if (showRecords) {
            this.getElement().setProperty("recordsDisplay", "display: block");
        } else {
            this.getElement().setProperty("recordsDisplay", "display: none");
        }
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

    @Subscribe
    public void slaveBreakDone(BreakTimerEvent.BreakDone e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        logger.debug("### received BreakDone {}");
        this.getElement().callJsFunction("reset");
        needReset = false;
    }

    @Subscribe
    public void slaveDecisionEvent(DecisionEvent e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        // logger.debug("### Results received DecisionEvent {} {} {}", e.getEventType(), e.getRecordKind(),
        // e.getRecordMessage());
        DecisionEventType eventType = e.getEventType();
        switch (eventType) {
        case DOWN_SIGNAL:
            this.decisionVisible = true;
            if (ui == null || ui.isClosing()) {
                return;
            }
            ui.access(() -> {
                setHidden(false);
                this.getElement().callJsFunction("down");
            });
            break;
        case RESET:
            this.decisionVisible = false;
            if (ui == null || ui.isClosing()) {
                return;
            }
            ui.access(() -> {
                setHidden(false);
                this.getElement().callJsFunction("reset");
            });
            break;
        case FULL_DECISION:
            this.decisionVisible = true;
            if (ui == null || ui.isClosing()) {
                return;
            }
            ui.access(() -> {
                setHidden(false);
                this.getElement().callJsFunction("refereeDecision");
                this.getElement().setProperty("recordKind", e.getRecordKind());
                this.getElement().setProperty("recordMessage", e.getRecordMessage());
            });
            break;
        default:
            break;
        }
    }

    @Subscribe
    public void slaveGlobalRankingUpdated(UpdateEvent e) {
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

        ui.access(() -> {
            String athletes = e.getAthletes();
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

            getElement().setProperty("noLiftRanks", e.getNoLiftRanks());

            getElement().setProperty("competitionName", e.getCompetitionName());
            getElement().setProperty("attempt", e.getAttempt());
            getElement().setProperty("fullName", e.getFullName());
            String groupName = e.getGroupName();
            getElement().setProperty("groupName", groupName);
            setHidden(e.getHidden());
            getElement().setProperty("startNumber", e.getStartNumber());
            getElement().setProperty("teamName", e.getTeamName());
            getElement().setProperty("weight", e.getWeight() != null ? e.getWeight() : 0);
            getElement().setProperty("categoryName", e.getCategoryName());
            setWideTeamNames(e.getWideTeamNames());
            String liftsDone = e.getLiftsDone();
            getElement().setProperty("liftsDone", liftsDone);

            if (StartupUtils.isDebugSetting()) {
                logger./**/warn("### state {} {}", fopState, e.getBreakType());
            }

            if (decisionVisible) {
                // wait for next event before doing anything.
                logger.debug("### waiting for decision reset");
            } else if ("INACTIVE".equals(fopState)
                    || ("BREAK".equals(fopState) && e.getBreakType() == BreakType.GROUP_DONE)) {
                logger.debug("### not in a group");
                doDone(e.getFullName());
                needReset = true;
            } else if ("BREAK".equals(fopState)) {
                logger.debug("### in a break {}", e.getBreakType());
                this.getElement().callJsFunction("doBreak");
                // also trigger a break timer event to make sure we are in sync with owlcms
                BreakStart breakStart = new BreakStart(e.getBreakRemaining(), e.isIndefinite());
                breakStart.setFopName(e.getFopName());
                TimerReceiverServlet.getEventBus().post(breakStart);
                needReset = true;
            } else if (!needReset) {
                // logger.debug("no reset");
            } else {
                logger.debug("### resetting becase of ranking update");
                this.getElement().callJsFunction("reset");
                needReset = false;
            }
        });
    }

    protected void doEmpty() {
        setHidden(true);
    }

    /** @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent) */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        SoundUtils.enableAudioContextNotification(this.getElement());

        ui = UI.getCurrent();

        eventBusRegister(this, TimerReceiverServlet.getEventBus());
        eventBusRegister(this, DecisionReceiverServlet.getEventBus());
        eventBusRegister(this, UpdateReceiverServlet.getEventBus());

        // setDarkMode(this, isDarkMode(), false);
        UpdateEvent initEvent = UpdateReceiverServlet.sync(getFopName());
        if (initEvent != null) {
            slaveGlobalRankingUpdated(initEvent);
            timer.slaveOrderUpdated(initEvent);
        } else {
            getElement().setProperty("fulName", Translator.translate("WaitingForSite"));
            getElement().setProperty("groupName", "");
            getElement().callJsFunction("groupDone");
        }
    }

    /** @see com.vaadin.flow.component.Component#onDetach(com.vaadin.flow.component.DetachEvent) */
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
        if (str == null) {
            doEmpty();
        } else {
            getElement().setProperty("fullName", str);
            this.getElement().callJsFunction("groupDone");
        }
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
        case DURING_INTRODUCTION:
            return Translator.translate("PublicMsg.DuringIntroduction");
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

    private void setHidden(boolean hidden) {
        this.getElement().setProperty("hiddenBlockStyle", (hidden ? "display:none" : "display:block"));
        this.getElement().setProperty("inactiveBlockStyle", (hidden ? "display:block" : "display:none"));
        this.getElement().setProperty("hiddenGridStyle", (hidden ? "display:none" : "display:grid"));
        this.getElement().setProperty("inactiveGridStyle", (hidden ? "display:grid" : "display:none"));
        this.getElement().setProperty("inactiveClass", (hidden ? "bigTitle" : ""));
    }

    private void setWideTeamNames(boolean wide) {
        this.getElement().setProperty("teamWidthClass", (wide ? "wideTeams" : "narrowTeams"));
    }
}
