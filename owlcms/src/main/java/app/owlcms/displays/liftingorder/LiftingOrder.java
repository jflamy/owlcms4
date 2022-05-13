/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.liftingorder;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

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
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.ui.shared.RequireLogin;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.Decision;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class LiftingOrder
 *
 * Show athlete lifting order
 *
 */
@SuppressWarnings("serial")
@Tag("liftingorder-template")
@JsModule("./components/LiftingOrder.js")
@Route("displays/liftingorder")
@Theme(value = Lumo.class, variant = Lumo.DARK)
@Push
public class LiftingOrder extends PolymerTemplate<LiftingOrder.LiftingOrderModel> implements DisplayParameters,
        SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle, RequireLogin {

    /**
     * LiftingOrderModel
     *
     * Vaadin Flow propagates these variables to the corresponding Polymer template JavaScript properties. When the JS
     * properties are changed, a "propname-changed" event is triggered.
     * {@link Element.#addPropertyChangeListener(String, String, com.vaadin.flow.dom.PropertyChangeListener)}
     *
     */
    public interface LiftingOrderModel extends TemplateModel {
        String getAttempt();

        String getFullName();

        Integer getStartNumber();

        String getTeamName();

        Integer getWeight();

        Boolean isHidden();

        Boolean isWideCategory();

        void setAttempt(String formattedAttempt);

        void setFullName(String lastName);

        void setGroupName(String name);

        void setHidden(boolean b);

        void setLiftsDone(String formattedDone);

        void setStartNumber(Integer integer);

        void setTeamName(String teamName);

        void setWeight(Integer weight);

        void setWideCategory(boolean b);
    }

    final private static Logger logger = (Logger) LoggerFactory.getLogger(LiftingOrder.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    JsonArray cattempts;
    JsonArray sattempts;
    private Group curGroup;
    private boolean darkMode;

    private Dialog dialog;
    private boolean initializationNeeded;
    private int liftsDone;
    private Location location;
    private UI locationUI;
    private List<Athlete> order;
    private EventBus uiEventBus;

    /**
     * Instantiates a new results board.
     */
    public LiftingOrder() {
        OwlcmsFactory.waitDBInitialized();
        this.getElement().getStyle().set("width", "100%");
        setDarkMode(true);
    }

    @Override
    public void addDialogContent(Component target, VerticalLayout vl) {
        DisplayOptions.addLightingEntries(vl, target, this);
    }

    @Override
    public void doBreak(UIEvent e) {
        OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            LiftingOrderModel model = getModel();
            order = fop.getLiftingOrder();
            model.setHidden(false);
            doUpdate(fop.getCurAthlete(), null);
        }));
    }

    @Override
    public void doCeremony(UIEvent.CeremonyStarted e) {
        doBreak(e);
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
        return getTranslation("Scoreboard.LiftingOrder") + OwlcmsSession.getFopNameIfMultiple();
    }

    @Override
    public boolean isDarkMode() {
        return this.darkMode;
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
        return this.initializationNeeded;
    }

    @Override
    public boolean isSilenced() {
        return true;
    }

    /**
     * Reset.
     */
    public void reset() {
        order = ImmutableList.of();
    }

    @Override
    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
    }

    @Override
    public void setDialog(Dialog dialog) {
        this.dialog = dialog;
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

    /**
     * @see app.owlcms.apputils.queryparameters.DisplayParameters#setSilenced(boolean)
     */
    @Override
    public void setSilenced(boolean silent) {
        // no-op, silenced by definition
    }

    @Subscribe
    public void slaveBreakDone(UIEvent.BreakDone e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            Athlete a = e.getAthlete();
            getModel().setHidden(false);
            liftsDone = AthleteSorter.countLiftsDone(order);
            doUpdate(a, e);
        });
    }

    @Subscribe
    public void slaveDecision(UIEvent.Decision e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            getModel().setHidden(false);
            doUpdateBottomPart(e);
        });
    }

    @Subscribe
    public void slaveDecisionReset(UIEvent.DecisionReset e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            getModel().setHidden(false);
            this.getElement().callJsFunction("reset");
        });
    }

    @Subscribe
    public void slaveGroupDone(UIEvent.GroupDone e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            getModel().setHidden(false);
            doDone(e.getGroup());
        });
    }

    @Subscribe
    public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            Athlete a = e.getAthlete();
            order = e.getLiftingOrder();
            liftsDone = AthleteSorter.countLiftsDone(order);
            doUpdate(a, e);
        });
    }

    @Subscribe
    public void slaveStartBreak(UIEvent.BreakStarted e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            getModel().setHidden(false);
            doBreak(e);
        });
    }

    @Subscribe
    public void slaveStartLifting(UIEvent.StartLifting e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            getModel().setHidden(false);
            this.getElement().callJsFunction("reset");
        });
    }

    @Subscribe
    public void slaveStopBreak(UIEvent.BreakDone e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            getModel().setHidden(false);
            Athlete a = e.getAthlete();
            this.getElement().callJsFunction("reset");
            doUpdate(a, e);
        });
    }

    @Subscribe
    public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            syncWithFOP(e);
        });
    }

    public void syncWithFOP(UIEvent.SwitchGroup e) {
        OwlcmsSession.withFop(fop -> {
            switch (fop.getState()) {
            case INACTIVE:
                doEmpty();
                break;
            case BREAK:
                doBreak(e);
                break;
            default:
                doUpdate(fop.getCurAthlete(), e);
            }
        });
    }

    public void uiLog(UIEvent e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
    }

    protected void doEmpty() {
        logger.trace("doEmpty");
        this.getModel().setHidden(true);
    }

    protected void doUpdate(Athlete a, UIEvent e) {
        logger.debug("doUpdate {} {}", a, a != null ? a.getAttemptsDone() : null);
        LiftingOrderModel model = getModel();

//            model.setHidden(a == null);
        if (a != null) {
            model.setFullName(getTranslation("Scoreboard.LiftingOrder"));
            updateBottom(model, computeLiftType(a));
        }
        if (a == null || a.getAttemptsDone() >= 6) {
            OwlcmsSession.withFop((fop) -> doDone(fop.getGroup()));
            return;
        }
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
            order = fop.getLiftingOrder();
            liftsDone = AthleteSorter.countLiftsDone(order);
            syncWithFOP(null);
            // we listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        });
        switchLightingMode(this, isDarkMode(), true);
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

    private String computeLiftType(Athlete a) {
        if (a == null) {
            return "";
        }
        String liftType = a.getAttemptsDone() >= 3 ? Translator.translate("Clean_and_Jerk")
                : Translator.translate("Snatch");
        return liftType;
    }

    private void doDone(Group g) {
        if (g == null) {
            return;
        } else {
            getModel().setFullName(getTranslation("Group_number_done", g.toString()));
            this.getElement().callJsFunction("groupDone");
        }
    }

    private void doUpdateBottomPart(Decision e) {
        LiftingOrderModel model = getModel();
        Athlete a = e.getAthlete();
        updateBottom(model, computeLiftType(a));
    }

    private JsonValue getAthletesJson(List<Athlete> list2) {
        JsonArray jath = Json.createArray();
        int athx = 0;
        List<Athlete> list3 = Collections.unmodifiableList(list2);
        for (Athlete a : list3) {
            JsonObject ja = Json.createObject();
            Category curCat = a.getCategory();
            String category;
            if (Competition.getCurrent().isMasters()) {
                category = a.getBWCategory();
            } else {
                category = curCat != null ? curCat.getName() : "";
            }
            ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
            ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
            ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
            Integer startNumber = a.getStartNumber();
            ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
            String mastersAgeGroup = a.getMastersAgeGroup();
            ja.put("mastersAgeGroup", mastersAgeGroup != null ? mastersAgeGroup : "");
            ja.put("category", category != null ? category : "");
            ja.put("nextAttemptNo", AthleteGridContent.formatAttemptNumber(a));
            Integer nextAttemptRequestedWeight = a.getNextAttemptRequestedWeight();
            ja.put("requestedWeight", nextAttemptRequestedWeight == 0 ? "-" : nextAttemptRequestedWeight.toString());
            Integer liftOrderRank = a.getLiftOrderRank();
            boolean notDone = a.getAttemptsDone() < 6;
            String blink = (notDone ? " blink" : "");
            if (notDone) {
                ja.put("classname", (liftOrderRank == 1 ? "current" + blink : (liftOrderRank == 2) ? "next" : ""));
            }
            jath.set(athx, ja);
            athx++;
        }
        return jath;
    }

    private Object getOrigin() {
        return this;
    }

    private void init() {
        OwlcmsSession.withFop(fop -> {
            logger.trace("{}Starting result board", fop.getLoggingName());
            setId("scoreboard-" + fop.getName());
            curGroup = fop.getGroup();
            getModel().setWideCategory(true);
        });
        setTranslationMap();
        order = ImmutableList.of();
    }

    private void updateBottom(LiftingOrderModel model, String liftType) {
        OwlcmsSession.withFop((fop) -> {
            curGroup = fop.getGroup();
            model.setGroupName(
                    curGroup != null ? Translator.translate("Scoreboard.GroupLiftType", curGroup.getName(), liftType)
                            : "");
        });
        model.setLiftsDone(Translator.translate("Scoreboard.AttemptsDone", liftsDone));
        this.getElement().setPropertyJson("athletes", getAthletesJson(order));
    }
}
