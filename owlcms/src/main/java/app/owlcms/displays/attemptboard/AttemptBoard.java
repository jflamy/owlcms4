/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import java.util.List;
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
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.RequireLogin;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
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

    @Id("athleteTimer")
    protected AthleteTimerElement athleteTimer; // created by Flow during template instanciation

    @Id("breakTimer")
    protected BreakTimerElement breakTimer; // created by Flow during template instanciation

    @Id("decisions")
    protected DecisionElement decisions; // created by Flow during template instanciation

    private EventBus uiEventBus;
    private Plates plates;
    private Location location;
    private UI locationUI;
    private boolean groupDone;
    private boolean silenced = true;
    private Dialog dialog;
    private boolean initializationNeeded;

    /**
     * Instantiates a new attempt board.
     */
    public AttemptBoard() {
        OwlcmsFactory.waitDBInitialized();
        logger.debug("*** AttemptBoard new {}", LoggerUtils.whereFrom());
        athleteTimer.setOrigin(this);
        getModel().setJavaComponentId(this.toString());
        getModel().setKgSymbol(getTranslation("KgSymbol"));
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
    public void doBreak() {
        OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            BreakType breakType = fop.getBreakType();
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
            getModel().setLastName(inferGroupName());
            getModel().setFirstName(inferMessage(breakType));
            getModel().setTeamName("");
            getModel().setAttempt("");

            uiEventLogger.debug("$$$ attemptBoard calling doBreak()");
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
//        if (dialog == null) {
            dialog = new Dialog();
            return dialog;
//        } else {
//            return null;
//        }
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
        return getTranslation("Attempt");
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

    /**
     * @see app.owlcms.apputils.queryparameters.DisplayParameters#setDarkMode(boolean)
     */
    @Override
    public void setDarkMode(boolean dark) {
        // noop
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
        //logger.debug("{} setSilenced = {} from {}", this.getClass().getSimpleName(), silenced, LoggerUtils.whereFrom());
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
//        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
//            this.getElement().callJsFunction("down");
//        });
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
//            Group g = e.getGroup();
            setDone(true);
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
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
            this.getElement().callJsFunction("down");
        });
    }

    @Subscribe
    public void slaveStartBreak(UIEvent.BreakStarted e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            doBreak();
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
                        doBreak();
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

        logger.debug("$$$  a {} state {}", a, state);
        if (a == null) {
            doEmpty();
            return;
        } else if (a.getAttemptsDone() >= 6) {
            setDone(true);
            return;
        }

        AttemptBoardModel model = getModel();
        String lastName = a.getLastName();
        model.setLastName(lastName.toUpperCase());
        model.setFirstName(a.getFirstName());
        model.setTeamName(a.getTeam());
        model.setStartNumber(a.getStartNumber());
        String formattedAttempt = formatAttempt(a.getAttemptNumber());
        model.setAttempt(formattedAttempt);
        model.setWeight(a.getNextAttemptRequestedWeight());
        showPlates();
        this.getElement().callJsFunction("reset");

        setDone(false);
    }

    /**
     * Restoring the attempt board during a break. The information about how/why the break was started is unavailable.
     *
     * @param fop
     */
    protected void doBreak(FieldOfPlay fop) {
        getModel().setLastName(inferGroupName());
        getModel().setFirstName(inferMessage(fop.getBreakType()));
        getModel().setTeamName("");
        getModel().setAttempt("");
        this.getElement().callJsFunction("doBreak", 5 * 60);
        uiEventLogger.debug("$$$ attemptBoard doBreak(fop)");
    }

    protected void doEmpty() {
        logger.debug("doEmpty {}", LoggerUtils.whereFrom());
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

            // sync with current status of FOP
            if (fop.getState() == FOPState.INACTIVE) {
                doEmpty();
            } else {
                Athlete curAthlete = fop.getCurAthlete();
                if (fop.getState() == FOPState.BREAK) {
                    if (curAthlete != null && curAthlete.getAttemptsDone() >= 6) {
                        doDone(fop.getGroup());
                    } else {
                        doBreak(fop);
                    }
                } else {
                    doAthleteUpdate(curAthlete);
                }
            }
            // we send on fopEventBus, listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        });
    }

    private void doDone(Group g) {
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            AttemptBoardModel model = getModel();
            if (model != null) {
                if (g != null) {
                    model.setLastName(getTranslation("Group_number_done", g.toString()));
                } else {
                    model.setLastName("");
                }
            }
            this.getElement().callJsFunction("groupDone");
            hidePlates();
        });
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

}
