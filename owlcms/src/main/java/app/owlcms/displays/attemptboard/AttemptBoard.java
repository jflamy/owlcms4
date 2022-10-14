/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import java.util.List;
import java.util.Timer;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.apputils.SoundUtils;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.elements.DecisionElement;
import app.owlcms.components.elements.Plates;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.RequireLogin;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Attempt board.
 */
/**
 * @author JF
 *
 */
/**
 * @author JF
 *
 */
@Theme(value = Lumo.class, variant = Lumo.DARK)
@SuppressWarnings("serial")
@Tag("attempt-board-template")
@JsModule("./components/AttemptBoard.js")
@JsModule("./components/AudioContext.js")
@CssImport(value = "./styles/shared-styles.css")
@CssImport(value = "./styles/plates.css")
@Route("displays/attemptBoard")
@Push
public class AttemptBoard extends PolymerTemplate<AttemptBoard.AttemptBoardModel> implements DisplayParameters,
        SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle, RequireLogin {

    /**
     * AttemptBoardModel
     *
     * Vaadin Flow propagates these variables to the corresponding Polymer template JavaScript properties. When the JS
     * properties are changed, a "propname-changed" event is triggered.
     *
     * {@link Element.#addPropertyChangeListener(String, String, com.vaadin.flow.dom.PropertyChangeListener)}
     */
    public interface AttemptBoardModel extends TemplateModel {
        String getAttempt();

        String getFirstName();

        String getJavaComponentId();

        String getKgSymbol();

        String getLastName();

        Integer getStartNumber();

        String getTeamName();

        Integer getWeight();

        Boolean isPublicFacing();

        Boolean isShowBarbell();

        void setAttempt(String formattedAttempt);

        void setFirstName(String firstName);

        void setJavaComponentId(String id);

        void setKgSymbol(String kgSymbol);

        void setLastName(String lastName);

        void setPublicFacing(Boolean publicFacing);

        void setShowBarbell(Boolean showBarbell);

        void setStartNumber(Integer integer);

        void setTeamName(String teamName);

        void setWeight(Integer weight);
    }

    final private static Logger logger = (Logger) LoggerFactory.getLogger(AttemptBoard.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    public static void doNotification(AttemptBoard attemptBoard, String text, String recordText, String theme,
            int duration) {
        attemptBoard.doNotification(text, recordText, theme, duration);
    }

    @Id("athleteTimer")
    protected AthleteTimerElement athleteTimer; // created by Flow during template instanciation

    @Id("breakTimer")
    protected BreakTimerElement breakTimer; // created by Flow during template instanciation

    @Id("decisions")
    protected DecisionElement decisions; // created by Flow during template instanciation

    private Dialog dialog;
    private boolean groupDone;
    private boolean initializationNeeded;
    private Location location;
    private UI locationUI;
    private Plates plates;
    private boolean silenced = true;
    private EventBus uiEventBus;
    private Timer dialogTimer;

    /**
     * Instantiates a new attempt board.
     */
    public AttemptBoard() {
        OwlcmsFactory.waitDBInitialized();
        // logger.debug("*** AttemptBoard new {}", LoggerUtils.whereFrom());
        athleteTimer.setOrigin(this);
        getModel().setJavaComponentId(this.toString());
        this.getElement().setProperty("kgSymbol", getTranslation("KgSymbol"));
        breakTimer.setParent("attemptBoard");
    }

    /**
     * @see app.owlcms.apputils.queryparameters.DisplayParameters#addDialogContent(com.vaadin.flow.component.Component,
     *      com.vaadin.flow.component.orderedlayout.VerticalLayout)
     */
    @Override
    public void addDialogContent(Component target, VerticalLayout dialog) {
        DisplayOptions.addSoundEntries(dialog, target, this);
    }

    @Override
    public void doBreak(UIEvent e) {
        // logger.trace("doBreak({})", e);
        OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            BreakType breakType = fop.getBreakType();
            // logger.trace("doBreak({}) bt={} a={}}", e, breakType, fop.getCurAthlete());
            if (breakType == BreakType.GROUP_DONE) {
                Group group = fop.getGroup();
                Athlete a = fop.getCurAthlete();
                if (a != null && a.getAttemptsDone() < 6) {
                    // the announcer has switched groups, but not started the introduction countdown.
                    doEmpty();
                } else {
                    doDone(group);
                }
                return;
            }
            this.getElement().setProperty("lastName", inferGroupName());
            this.getElement().setProperty("firstName", inferMessage(breakType, fop.getCeremonyType()));
            this.getElement().setProperty("teamName", "");

            Athlete a = fop.getCurAthlete();
            if (a != null) {
                String formattedAttempt = formatAttempt(a.getAttemptNumber());
                this.getElement().setProperty("attempt", formattedAttempt);
                this.getElement().setProperty("weight", a.getNextAttemptRequestedWeight());
                showPlates();
                // logger.trace("showingPlates {}",a.getNextAttemptRequestedWeight());
            } else {
                this.getElement().setProperty("attempt", "");
                this.getElement().setProperty("weight", "");
            }

            breakTimer.setVisible(!fop.getBreakTimer().isIndefinite());

            uiEventLogger.debug("$$$ attemptBoard calling doBreak()");
            // logger.trace("attemptBoard showWeights ? {}", fop.getCeremonyType());
            this.getElement().callJsFunction("doBreak", fop.getCeremonyType() == null);
        }));
    }

    @Override
    public void doCeremony(UIEvent.CeremonyStarted e) {
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
    public Location getLocation() {
        return this.location;
    }

    @Override
    public UI getLocationUI() {
        return this.locationUI;
    }

    @Override
    public String getPageTitle() {
        return getTranslation("Attempt") + OwlcmsSession.getFopNameIfMultiple();
    }

    @Override
    public boolean isDarkMode() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.ui.shared.QueryParameterReader#isIgnoreGroupFromURL()
     */
    @Override
    public boolean isIgnoreGroupFromURL() {
        return true;
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

    @Override
    public boolean isSilencedByDefault() {
        return true;
    }

    /**
     * @see app.owlcms.apputils.queryparameters.DisplayParameters#setDarkMode(boolean)
     */
    @Override
    public void setDarkMode(boolean dark) {
        // noop
    }

    @Override
    public void setDialog(Dialog dialog) {
        this.dialog = dialog;
    }

    @Override
    public void setDialogTimer(Timer dialogTimer) {
        this.dialogTimer = dialogTimer;
    }

    @Override
    public Timer getDialogTimer() {
        return dialogTimer;
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public void setLocationUI(UI locationUI) {
        this.locationUI = locationUI;
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
        // logger.debug("{} setSilenced = {} from {}", this.getClass().getSimpleName(), silenced,
        // LoggerUtils.whereFrom());
        this.athleteTimer.setSilenced(silenced);
        this.breakTimer.setSilenced(silenced);
        this.decisions.setSilenced(silenced);
        this.silenced = silenced;
    }

    @Subscribe
    public void slaveBarbellOrPlatesChanged(UIEvent.BarbellOrPlatesChanged e) {
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> showPlates());
    }

    @Subscribe
    public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            syncWithFOP(OwlcmsSession.getFop());
        });
    }

    @Subscribe
    public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            syncWithFOP(OwlcmsSession.getFop());
        });
    }

    @Subscribe
    public void slaveDecision(UIEvent.Decision e) {
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            spotlightRecords(OwlcmsSession.getFop());
        });
    }

    @Subscribe
    public void slaveDecisionReset(UIEvent.DecisionReset e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            if (isDone()) {
                doDone(e.getAthlete().getGroup());
            } else {
                this.getElement().callJsFunction("reset");
            }
        });
    }

    /**
     * Multiple attempt boards and athlete-facing boards can co-exist. We need to show down on the slave devices -- the
     * master device is the one where refereeing buttons are attached.
     *
     * @param e
     */
    @Subscribe
    public void slaveDownSignal(UIEvent.DownSignal e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        // don't block others
        new Thread(() -> {
            UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
                this.getElement().callJsFunction("down");
            });
        }).start();
    }

    @Subscribe
    public void slaveGroupDone(UIEvent.GroupDone e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            Group g = e.getGroup();
            doDone(g);
            setDone(true);
        });
    }

    @Subscribe
    public void slaveJuryNotification(UIEvent.JuryNotification e) {
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            String text = "";
            String reversalText = "";
            if (e.getReversal() != null) {
                reversalText = e.getReversal() ? Translator.translate("JuryNotification.Reversal")
                        : Translator.translate("JuryNotification.Confirmed");
            }
            String style = "warning";
            int previousAttemptNo;
            switch (e.getDeliberationEventType()) {
            case BAD_LIFT:
                previousAttemptNo = e.getAthlete().getAttemptsDone() - 1;
                text = Translator.translate("JuryNotification.BadLift", reversalText,
                        "<br/>" + e.getAthlete().getFullName(),
                        previousAttemptNo % 3 + 1);
                style = "primary error";
                doNotification(this, text, null, style, (int) (2 * FieldOfPlay.DECISION_VISIBLE_DURATION));
                break;
            case GOOD_LIFT:
                previousAttemptNo = e.getAthlete().getAttemptsDone() - 1;
                text = Translator.translate("JuryNotification.GoodLift", reversalText,
                        "<br/>" + e.getAthlete().getFullName(),
                        previousAttemptNo % 3 + 1);
                style = "primary success";
                doNotification(this, text,
                        (e.getNewRecord() ? "<br/>" + Translator.translate("Scoreboard.NewRecord") : ""),
                        style,
                        (int) (2 * FieldOfPlay.DECISION_VISIBLE_DURATION));
                break;
            default:
                break;
            }

        });
    }

    @Subscribe
    public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
            FOPState state = fop.getState();
            uiEventLogger.debug("### {} {} isDisplayToggle={}", state, this.getClass().getSimpleName(),
                    e.isDisplayToggle());
            if (state == FOPState.BREAK) {
                if (e.isDisplayToggle()) {
                    Athlete a = e.getAthlete();
                    doAthleteUpdate(a);
                } else {
                    doBreak(e);
                }
                return;
            } else if (state == FOPState.INACTIVE) {
                return;
            } else if (!e.isCurrentDisplayAffected()) {
                // order change does not affect current lifter
                return;
            } else {
                Athlete a = e.getAthlete();
                doAthleteUpdate(a);
            }
        }));
    }

    /**
     * Multiple attempt boards and athlete-facing boards can co-exist. We need to show decisions on the slave devices --
     * the master device is the one where refereeing buttons are attached.
     *
     * @param e
     */
    @Subscribe
    public void slaveRefereeDecision(UIEvent.Decision e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        // hide the athleteTimer except if the down signal came from this ui.
        // in which case the down has already been shown.
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
            this.getElement().callJsFunction("down");
        });
    }

    @Subscribe
    public void slaveStartBreak(UIEvent.BreakStarted e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            doBreak(e);
        });
    }

    @Subscribe
    public void slaveStartLifting(UIEvent.StartLifting e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            this.getElement().callJsFunction("reset");
        });
    }

    @Subscribe
    public void slaveStopBreak(UIEvent.BreakDone e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            Athlete a = e.getAthlete();
            if (a == null) {
                OwlcmsSession.withFop(fop -> {
                    List<Athlete> order = fop.getLiftingOrder();
                    Athlete athlete = order.size() > 0 ? order.get(0) : null;
                    doAthleteUpdate(athlete);
                });
            } else {
                doAthleteUpdate(a);
            }
        });
    }

    @Subscribe
    public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            OwlcmsSession.withFop(fop -> {
                switch (fop.getState()) {
                case INACTIVE:
                    doEmpty();
                    break;
                case BREAK:
                    if (e.getGroup() == null) {
                        doEmpty();
                    } else {
                        doBreak(e);
                    }
                    break;
                default:
                    doAthleteUpdate(fop.getCurAthlete());
                }
            });
            // uiEventLogger./**/warn("#### reloading {}", this.getElement().getClass());
            // this.getElement().callJsFunction("reload");
        });
    }

    /**
     * @see app.owlcms.apputils.queryparameters.FOPParameters#updateURLLocation(com.vaadin.flow.component.UI,
     *      com.vaadin.flow.router.Location, java.lang.String, java.lang.String)
     */
    @Override
    public void updateURLLocation(UI ui, Location location, String parameter, String mode) {
        TreeMap<String, List<String>> parametersMap = new TreeMap<>(location.getQueryParameters().getParameters());
        updateParam(parametersMap, DARK, null);
        updateParam(parametersMap, parameter, mode);
        FieldOfPlay fop = OwlcmsSession.getFop();
        updateParam(parametersMap, "fop", fop != null ? fop.getName() : null);
        Location location2 = new Location(location.getPath(), new QueryParameters(parametersMap));
        ui.getPage().getHistory().replaceState(null, location2);
        setLocation(location2);
    }

    protected void doAthleteUpdate(Athlete a) {
        FieldOfPlay fop = OwlcmsSession.getFop();
        FOPState state = fop.getState();
        if (fop.getState() == FOPState.INACTIVE
                || (state == FOPState.BREAK && fop.getBreakType() == BreakType.GROUP_DONE)) {
            doEmpty();
            return;
        }

        if (a == null) {
            doEmpty();
            return;
        } else if (a.getAttemptsDone() >= 6) {
            setDone(true);
            return;
        }

        String lastName = a.getLastName();
        this.getElement().setProperty("lastName", lastName.toUpperCase());
        this.getElement().setProperty("firstName", a.getFirstName());
        this.getElement().setProperty("teamName", a.getTeam());

        spotlightRecords(fop);

        this.getElement().setProperty("startNumber", a.getStartNumber());
        String formattedAttempt = formatAttempt(a.getAttemptNumber());
        this.getElement().setProperty("attempt", formattedAttempt);
        this.getElement().setProperty("weight", a.getNextAttemptRequestedWeight());
        showPlates();
        this.getElement().callJsFunction("reset");

        setDone(false);
    }

    private void spotlightNewRecord() {
        this.getElement().setProperty("recordKind", "recordNotification new");
        this.getElement().setProperty("recordMessage", Translator.translate("Scoreboard.NewRecord"));
    }

    private void spotlightRecordAttempt() {
        this.getElement().setProperty("recordKind", "recordNotification attempt");
        this.getElement().setProperty("recordMessage", Translator.translate("Scoreboard.RecordAttempt"));
    }

    private void spotlightRecords(FieldOfPlay fop) {
        if (fop.getNewRecords() != null && !fop.getNewRecords().isEmpty()) {
            spotlightNewRecord();
        } else if (fop.getChallengedRecords() != null && !fop.getChallengedRecords().isEmpty()) {
            spotlightRecordAttempt();
        } else {
            this.getElement().setProperty("recordKind", "recordNotification none");
            this.getElement().setProperty("teamName", fop.getCurAthlete().getTeam());
        }
    }

    /**
     * Restoring the attempt board during a break. The information about how/why the break was started is unavailable.
     *
     * @param fop
     */
    protected void doBreak(FieldOfPlay fop) {
        this.getElement().setProperty("lastName", inferGroupName(fop.getCeremonyType()));
        this.getElement().setProperty("firstName", inferMessage(fop.getBreakType(), fop.getCeremonyType()));
        this.getElement().setProperty("teamName", "");
        this.getElement().setProperty("attempt", "");
        Athlete a = fop.getCurAthlete();
        if (a != null) {
            String formattedAttempt = formatAttempt(a.getAttemptNumber());
            this.getElement().setProperty("attempt", formattedAttempt);
            this.getElement().setProperty("weight", a.getNextAttemptRequestedWeight());
            showPlates();
        }

        boolean showWeights = fop.getCeremonyType() == null;
        // logger.trace("*** doBreak {} {} {}", showWeights, fop.getCeremonyType(), LoggerUtils.whereFrom());
        this.getElement().callJsFunction("doBreak", showWeights);
        uiEventLogger.debug("$$$ attemptBoard doBreak(fop)");
    }

    protected void doEmpty() {
        hidePlates();
        this.getElement().callJsFunction("clear");
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // fop obtained via FOPParameters interface default methods.
        OwlcmsSession.withFop(fop -> {
            logger.debug("{}onAttach {}", fop.getLoggingName(), fop.getState());
            init();
            ThemeList themeList = UI.getCurrent().getElement().getThemeList();
            themeList.remove(Lumo.LIGHT);
            themeList.add(Lumo.DARK);

            SoundUtils.enableAudioContextNotification(this.getElement());

            syncWithFOP(fop);
            // we send on fopEventBus, listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        });
    }

    private void doDone(Group g) {
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
//            AttemptBoardModel model = getModel();
//            if (model != null) {
            if (g != null) {
                this.getElement().setProperty("lastName", getTranslation("Group_number_done", g.toString()));
            } else {
                this.getElement().setProperty("lastName", "");
            }
//            }
            this.getElement().callJsFunction("groupDone");
            hidePlates();
        });
    }

    private void doNotification(String text, String recordText, String theme, int duration) {
        Notification n = new Notification();
        // Notification theme styling is done in META-INF/resources/frontend/styles/shared-styles.html
        n.getElement().getThemeList().add(theme);

        n.setDuration(duration);
        n.setPosition(Position.TOP_STRETCH);
        Div label = new Div();
        label.getElement().setProperty("innerHTML", text + (recordText != null ? recordText : ""));
        label.getElement().setAttribute("style", "text: align-center");
        label.addClickListener((event) -> n.close());
        label.setWidth("70vw");
        label.getStyle().set("font-size", "7vh");
        n.add(label);

        OwlcmsSession.withFop(fop -> {
//            this.getElement().callJsFunction("reset");
//            syncWithFOP(OwlcmsSession.getFop());
            n.open();
            return;
        });

        return;
    }

    private String formatAttempt(Integer attemptNo) {
        return getTranslation("AttemptBoard_attempt_number", attemptNo);
    }

    private Object getOrigin() {
        return this;
    }

    private void hidePlates() {
        if (plates != null) {
            try {
                this.getElement().removeChild(plates.getElement());
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
        plates = null;
    }

    private void init() {
        OwlcmsSession.withFop(fop -> {
            logger.trace("{}Starting attempt board", fop.getLoggingName());
            setId("attempt-board-template");
        });
    }

    private boolean isDone() {
        return this.groupDone;
    }

    private void setDone(boolean b) {
        this.groupDone = b;
    }

    private void showPlates() {
        AttemptBoard attemptBoard = this;
        OwlcmsSession.withFop((fop) -> {
            UIEventProcessor.uiAccess(this, uiEventBus, () -> {
                try {
                    if (plates != null) {
                        attemptBoard.getElement().removeChild(plates.getElement());
                    }
                    plates = new Plates();
                    plates.computeImageArea(fop, false);
                    Element platesElement = plates.getElement();
                    // tell polymer that the plates belong in the slot named barbell of the template
                    platesElement.setAttribute("slot", "barbell");
                    platesElement.getStyle().set("font-size", "3.3vh");
                    platesElement.getClassList().set("dark", true);
                    attemptBoard.getElement().appendChild(platesElement);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        });
    }

    private void syncWithFOP(FieldOfPlay fop) {
        // sync with current status of FOP
        if (fop.getState() == FOPState.INACTIVE && fop.getCeremonyType() == null) {
            doEmpty();
        } else {
            Athlete curAthlete = fop.getCurAthlete();
            if (fop.getState() == FOPState.BREAK || fop.getState() == FOPState.INACTIVE) {
                // logger.trace("syncwithfop {} {}",fop.getBreakType(), fop.getCeremonyType());
                if (fop.getCeremonyType() != null) {
                    doBreak(fop);
                } else if (curAthlete != null && curAthlete.getAttemptsDone() >= 6) {
                    doDone(fop.getGroup());
                } else {
                    doBreak(fop);
                }
            } else {
                doAthleteUpdate(curAthlete);
            }
        }
    }

}
