/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.components.elements.AthleteTimerElementPR;
import app.owlcms.components.elements.BreakTimerElementPR;
import app.owlcms.components.elements.DecisionElementPR;
import app.owlcms.i18n.Translator;
import app.owlcms.prutils.SafeEventBusRegistrationPR;
import app.owlcms.publicresults.DecisionReceiverServlet;
import app.owlcms.publicresults.TimerReceiverServlet;
import app.owlcms.publicresults.UpdateReceiverServlet;
import app.owlcms.ui.parameters.DarkModeParameters;
import app.owlcms.ui.parameters.QueryParameterReader;
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
import elemental.json.impl.JreJsonFactory;

/**
 * Class Scoreboard
 *
 * Show athlete 6-attempt results
 *
 */
@Tag("scoreleader-template")
@JsModule("./components/ScoreWithLeaders.js")
@Route("displays/scoreleader")
@Theme(value = Lumo.class, variant = Lumo.DARK)
@Push
public class ScoreWithLeaders extends PolymerTemplate<ScoreWithLeaders.ScoreboardModel>
        implements QueryParameterReader, DarkModeParameters, HasDynamicTitle, SafeEventBusRegistrationPR {

    /**
     * ScoreboardModel
     *
     * Vaadin Flow propagates these variables to the corresponding Polymer template JavaScript properties. When the JS
     * properties are changed, a "propname-changed" event is triggered.
     * {@link Element.#addPropertyChangeListener(String, String, com.vaadin.flow.dom.PropertyChangeListener)}
     *
     */
    public interface ScoreboardModel extends TemplateModel {
        String getAttempt();

        String getCategoryName();

        String getCompetitionName();

        String getFullName();

        Integer getStartNumber();

        String getTeamName();

        Integer getWeight();

//        Boolean isHidden();

        Boolean isWideTeamNames();

        void setAttempt(String formattedAttempt);

        void setCategoryName(String categoryName);

        void setCompetitionName(String competitionName);

        void setFullName(String lastName);

        void setGroupName(String name);

//        void setHidden(boolean b);

        void setLiftsDone(String formattedDone);

        void setStartNumber(Integer integer);

        void setTeamName(String teamName);

        void setWeight(Integer weight);

        void setWideTeamNames(boolean b);
    }

    final private static Logger logger = (Logger) LoggerFactory.getLogger(ScoreWithLeaders.class);
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
    private ContextMenu contextMenu;
    private Location location;
    private UI locationUI;
    private String fopName;
    private boolean needReset = false;
    private boolean decisionVisible;
    private UI ui;

    /**
     * Instantiates a new results board.
     */
    public ScoreWithLeaders() {
        setDarkMode(true);
    }

    @Override
    public ContextMenu getContextMenu() {
        return contextMenu;
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
    public boolean isIgnoreGroupFromURL() {
        return true;
    }

    @Override
    public void setContextMenu(ContextMenu contextMenu) {
        this.contextMenu = contextMenu;
    }

    @Override
    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
    }

    /** @see app.owlcms.ui.parameters.QueryParameterReader#setFopName(java.lang.String) */
    @Override
    public void setFopName(String fopName) {
        this.fopName = fopName;
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public void setLocationUI(UI locationUI) {
        this.locationUI = locationUI;
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
        logger.debug("### received DecisionEvent {}", e.getEventType());
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

        ui.access(() -> {
            String athletes = e.getAthletes();
            String leaders = e.getLeaders();
            String translationMap = e.getTranslationMap();

            JreJsonFactory jreJsonFactory = new JreJsonFactory();
            this.getElement().setPropertyJson("leaders",
                    leaders != null ? jreJsonFactory.parse(leaders) : Json.createNull());
            this.getElement().setPropertyJson("athletes",
                    athletes != null ? jreJsonFactory.parse(athletes) : Json.createNull());
            this.getElement().setPropertyJson("t",
                    translationMap != null ? jreJsonFactory.parse(translationMap) : Json.createNull());

            getModel().setCompetitionName(e.getCompetitionName());
            getModel().setAttempt(e.getAttempt());
            getModel().setFullName(e.getFullName());
            String groupName = e.getGroupName();
            getModel().setGroupName(groupName);
            setHidden(e.getHidden());
            getModel().setStartNumber(e.getStartNumber());
            getModel().setTeamName(e.getTeamName());
            getModel().setWeight(e.getWeight());
            getModel().setCategoryName(e.getCategoryName());
            setWideTeamNames(e.getWideTeamNames());
            String liftsDone = e.getLiftsDone();
            getModel().setLiftsDone(liftsDone);

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
        // crude workaround -- randomly getting light or dark due to multiple themes detected in app.
        getElement().executeJs("document.querySelector('html').setAttribute('theme', 'dark');");
        ui = UI.getCurrent();

        eventBusRegister(this, TimerReceiverServlet.getEventBus());
        eventBusRegister(this, DecisionReceiverServlet.getEventBus());
        eventBusRegister(this, UpdateReceiverServlet.getEventBus());

        setDarkMode(this, isDarkMode(), false);
        UpdateEvent initEvent = UpdateReceiverServlet.sync(getFopName());
        if (initEvent != null) {
            slaveGlobalRankingUpdated(initEvent);
            timer.slaveOrderUpdated(initEvent);
        } else {
            getModel().setFullName(Translator.translate("WaitingForSite"));
            getModel().setGroupName("");
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
//            getModel().setFullName(getTranslation("Group_number_results", groupName));
            getModel().setFullName(str);
            this.getElement().callJsFunction("groupDone");
        }
    }

    private String getFopName() {
        return fopName;
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
        this.getElement().setProperty("hiddenStyle", (hidden ? "display:none" : "display:block"));
        this.getElement().setProperty("inactiveStyle", (hidden ? "display:block" : "display:none"));
        this.getElement().setProperty("inactiveClass", (hidden ? "bigTitle" : ""));
    }

    private void setWideTeamNames(boolean wide) {
        this.getElement().setProperty("teamWidthClass", (wide ? "wideTeams" : "narrowTeams"));
    }

}