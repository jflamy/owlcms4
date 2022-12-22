/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.monitor;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;

import app.owlcms.apputils.queryparameters.FOPParameters;
import app.owlcms.data.athlete.LiftDefinition;
import app.owlcms.data.athlete.LiftDefinition.Stage;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class OBSMonitor
 *
 * Show athlete lifting order
 *
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("monitor-template")
@JsModule("./components/OBSMonitor.js")
@Route("displays/monitor")

public class OBSMonitor extends PolymerTemplate<OBSMonitor.MonitorModel> implements FOPParameters,
        SafeEventBusRegistration, UIEventProcessor {

    /**
     * unused
     */
    public interface MonitorModel extends TemplateModel {
    }

    class Status {
        BreakType breakType;
        CeremonyType ceremonyType;
        Boolean decision;
        FOPState state;
        boolean challengedRecords;
        private Stage liftType;

        public Status(FOPState state, BreakType breakType, CeremonyType ceremonyType, Boolean decision,
                boolean challengedRecords, LiftDefinition.Stage liftType) {
            logger.setLevel(Level.DEBUG);
            this.state = state;
            this.breakType = breakType;
            this.ceremonyType = ceremonyType;
            this.decision = decision;
            this.challengedRecords = challengedRecords;
            this.setLiftType(liftType);
        }

        @Override
        public String toString() {
            return "Status [breakType=" + breakType + ", ceremonyType=" + ceremonyType + ", decision=" + decision
                    + ", state=" + state + ", challengedRecords=" + challengedRecords + ", liftType=" + liftType + "]";
        }

        public Stage getLiftType() {
            return liftType;
        }

        public void setLiftType(Stage liftType) {
            this.liftType = liftType;
        }
    }

    final static int HISTORY_SIZE = 3;

    final private Logger logger = (Logger) LoggerFactory.getLogger(OBSMonitor.class);

    final private static Logger uiEventLogger = (Logger) LoggerFactory
            .getLogger("UI" + OBSMonitor.class.getSimpleName());
    static {
        uiEventLogger.setLevel(Level.INFO);
    }

    List<Status> history = new LinkedList<>();

    private BreakType currentBreakType;
    private CeremonyType currentCeremony;
    private Boolean currentDecision;
    private boolean currentChallengedRecords;
    private String currentFOP;
    private FOPState currentState;
    private Status h0;
    private Status h1;
    private Status h2;
    private Location location;
    private UI locationUI;
    private BreakType previousBreakType;
    private CeremonyType previousCeremony;
    private Boolean previousDecision;
    private boolean previousChallengedRecords;
    private FOPState previousState;
    private String prevTitle;
    private String title;
    private EventBus uiEventBus;

    private Object currentLiftType;
    private Object previousLiftType;

    /**
     * Instantiates a new results board.
     */
    public OBSMonitor() {
        OwlcmsFactory.waitDBInitialized();
        this.getElement().getStyle().set("width", "100%");
        // we need two items on the stack (current + previous)
        doPush(new Status(FOPState.INACTIVE, null, null, null, false, null));
        doPush(new Status(FOPState.INACTIVE, null, null, null, false, null));
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
    public boolean isIgnoreGroupFromURL() {
        return true;
    }

    /**
     * @see app.owlcms.apputils.queryparameters.DisplayParameters#isShowInitialDialog()
     */
    @Override
    public boolean isShowInitialDialog() {
        return false;
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
    public void slaveUIEvent(UIEvent e) {
        if (e instanceof UIEvent.SetTime) {
            // ignore events that don't change state
            return;
        } else if (e instanceof UIEvent.Notification) {
            UIEventProcessor.uiAccess(this, uiEventBus, () -> {
                OwlcmsSession.withFop(fop -> {
                    logger.trace("---- notification {} {}", fop.getName(),
                            ((UIEvent.Notification) e).getNotificationString());
                });
            });
        }
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e /*, e.getTrace()*/);
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            if (syncWithFOP(e)) {
                // significant transition
                doUpdate();
            } else {
                // logger.debug("event ignored {} : {}", e.getClass().getSimpleName(),
                // OwlcmsSession.getFop().getState());
            }
        });
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // fop obtained via FOPParameters interface default methods.
        OwlcmsSession.withFop(fop -> {
            init();
            // sync with current status of FOP
            syncWithFOP(null);
            // we listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        });
        doUpdate();
    }

    void uiLog(UIEvent e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
    }

    private String computePageTitle() {
        StringBuilder pageTitle = new StringBuilder();
        computeValues();

//        if (h0 != null && h0.state == FOPState.CURRENT_ATHLETE_DISPLAYED
//                && h1 != null && h1.state == FOPState.DECISION_VISIBLE
//                && h2 != null && h2.state == FOPState.BREAK && h2.breakType == BreakType.JURY) {
//            logger.debug("!! fixing display after jury {} {} {}", h0, h1, h2);
//            history.remove(1);
//            computeValues();
//        } else 
        {
            logger.debug("-- normal {} {} {}", h0, h1, h2);
        }

        if (currentState == FOPState.INACTIVE || currentState == FOPState.BREAK) {
            pageTitle.append("break=");
        } else {
            pageTitle.append("state=");
        }
        pageTitle.append(currentState.name());

        if (currentState == FOPState.BREAK && currentCeremony != null) {
            pageTitle.append(".");
            pageTitle.append(currentCeremony.name());
        } else if (currentState == FOPState.BREAK && currentBreakType != null) {
            pageTitle.append(".");
            pageTitle.append(currentBreakType.name());
        } else if (currentState == FOPState.DECISION_VISIBLE) {
            pageTitle.append(".");
            pageTitle.append(currentDecision == null ? "UNDECIDED" : (currentDecision ? "GOOD_LIFT" : "BAD_LIFT"));
            if (currentDecision && currentChallengedRecords) {
                pageTitle.append(".NEW_RECORD");
            } else if (previousState == FOPState.DECISION_VISIBLE && previousDecision && previousChallengedRecords) {
                // special case where state changes too quickly;
                pageTitle.append(".NEW_RECORD");
            }
        } else if (currentChallengedRecords) {
            pageTitle.append(".RECORD_ATTEMPT");
        }

        pageTitle.append(";");
        pageTitle.append("previous=");
        pageTitle.append(previousState.name());
        if (previousState == FOPState.BREAK && previousCeremony != null) {
            pageTitle.append(".");
            pageTitle.append(previousCeremony.name());
        } else if (previousState == FOPState.BREAK && previousBreakType != null) {
            pageTitle.append(".");
            pageTitle.append(previousBreakType.name());
        } else if (previousState == FOPState.DECISION_VISIBLE) {
            pageTitle.append(".");
            pageTitle.append(previousDecision == null ? "UNDECIDED" : (previousDecision ? "GOOD_LIFT" : "BAD_LIFT"));
        }

        if (currentLiftType != null) {
            pageTitle.append(";");
            pageTitle.append("liftType=");
            pageTitle.append(currentLiftType.toString());
        }
        pageTitle.append(";");
        pageTitle.append("fop=");
        pageTitle.append(currentFOP);

        String string = pageTitle.toString();

        return string;
    }

    @Override
    public String toString() {
        return "OBSMonitor [history=" + history + ", currentBreakType=" + currentBreakType + ", currentCeremony="
                + currentCeremony + ", currentDecision=" + currentDecision + ", currentChallengedRecords="
                + currentChallengedRecords + ", currentFOP=" + currentFOP + ", currentState=" + currentState
                + ", previousBreakType=" + previousBreakType + ", previousCeremony=" + previousCeremony
                + ", previousDecision=" + previousDecision + ", previousChallengedRecords=" + previousChallengedRecords
                + ", previousState=" + previousState + ", prevTitle=" + prevTitle + ", title=" + title + ", uiEventBus="
                + uiEventBus + ", currentLiftType=" + currentLiftType + ", previousLiftType=" + previousLiftType + "]";
    }

    private void computeValues() {
        h0 = history.size() > 0 ? history.get(0) : null;
        h1 = history.size() > 1 ? history.get(1) : null;
        h2 = history.size() > 2 ? history.get(2) : null;
        currentState = h0 != null ? h0.state : null;
        currentBreakType = h0 != null ? h0.breakType : null;
        currentCeremony = h0 != null ? h0.ceremonyType : null;
        currentDecision = h0 != null ? h0.decision : null;
        currentChallengedRecords = h0 != null ? h0.challengedRecords : false;
        currentLiftType = h0 != null ? h0.liftType : null;

        previousState = h1 != null ? h1.state : null;
        previousBreakType = h1 != null ? h1.breakType : null;
        previousCeremony = h1 != null ? h1.ceremonyType : null;
        previousDecision = h1 != null ? h1.decision : null;
        previousChallengedRecords = h1 != null ? h1.challengedRecords : null;
        previousLiftType = h1 != null ? h1.liftType : null;
    }

    private void doPush(Status status) {
        history.add(0, status);
        if (history.size() > HISTORY_SIZE) {
            history.remove(HISTORY_SIZE);
        }
    }

    private synchronized void doUpdate() {
        title = computePageTitle();
        String comparisonTitle = title != null ? title.substring(0, title.indexOf(";")) : title;
        String comparisonPrevTitle = prevTitle != null ? prevTitle.substring(0, prevTitle.indexOf(";")) : prevTitle;
        logger.debug("comparing comparisonTitle={} with comparisonPrevTitle={}", comparisonTitle, comparisonPrevTitle);
        boolean same = false;
        if (comparisonPrevTitle == null || comparisonTitle == null) {
            // same if both null
            same = (comparisonTitle == comparisonPrevTitle);
        } else if (comparisonTitle != null) {
            // same if same content comparison
            // prevTitle cannot be null (tested in previous branch)
            same = comparisonTitle.contentEquals(comparisonPrevTitle);
        }
        if (!same && !(comparisonTitle == null) && !comparisonTitle.isBlank()) {
            this.getElement().setProperty("title", title);
            this.getElement().callJsFunction("setTitle", title);
            logger.info("#### monitor {}", title);
            prevTitle = title;
        }
        if (same) {
            logger.debug("---- monitor duplicate {}", title);
        }
    }

    private Object getOrigin() {
        return this;
    }

    private void init() {
        OwlcmsSession.withFop(fop -> {
            logger.trace("{}Starting monitoring", fop.getLoggingName());
            setId("scoreboard-" + fop.getName());
        });
    }

    private boolean syncWithFOP(UIEvent e) {
        boolean significant[] = { false };
        OwlcmsSession.withFop(fop -> {
            currentFOP = fop.getName();
            boolean fopChallengedRecords = fop.getChallengedRecords() != null && !fop.getChallengedRecords().isEmpty();
            boolean newRecord = e instanceof UIEvent.JuryNotification && ((UIEvent.JuryNotification) e).getNewRecord();
            boolean curChallengedRecords = history.get(0).challengedRecords;

            boolean stateChanged = fop.getState() != history.get(0).state;
            boolean recordsChanged = fopChallengedRecords != curChallengedRecords;
            logger.debug(">>>>>>OBSMonitor event {} fop {} history {} recordsChanged {}",e != null ? e.getClass().getSimpleName() : null, fop.getState(),history.get(0).state, recordsChanged);
            if (e != null && e instanceof UIEvent.DecisionReset) {
                // this event does not change state, and should always be ignored.
                // however, because it can occur very close to the lifter update, and we have asynchronous events
                // there is a possibility that it comes late and out of order.  So we ignore it explicitly.
                logger.debug(">>>>>>OBSMonitor DecisionReset ignored");
                significant[0] = false;
            } else if (stateChanged || recordsChanged) {
                doPush(new Status(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), fop.getGoodLift(),
                        isNotEmpty(fop.getChallengedRecords()) || newRecord, fop.getCurrentStage()));
                significant[0] = true;
            } else if (fop.getState() == FOPState.BREAK) {
                if (fop.getBreakType() != history.get(0).breakType
                        || fop.getCeremonyType() != history.get(0).ceremonyType) {
                    doPush(new Status(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), null,
                            isNotEmpty(fop.getChallengedRecords()), null));
                    significant[0] = true;
                } else {
                    // logger.trace("*** OBSMonitor ignored duplicate {} {}", fop.getBreakType(),
                    // fop.getCeremonyType());
                }
            } else {
                // logger.trace("*** OBSMonitor non break {}", fop.getState());
            }
        });
        logger.debug(">>>>>>OBSMonitor sync significant {}", significant[0]);
        return significant[0];
    }

    private boolean isNotEmpty(List<RecordEvent> list) {
        return list != null && !list.isEmpty();
    }
}
