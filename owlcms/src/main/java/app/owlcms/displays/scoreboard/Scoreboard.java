/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.displays.scoreboard;

import java.util.Enumeration;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
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

import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.elements.DecisionElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.displays.DarkModeParameters;
import app.owlcms.displays.attemptboard.BreakDisplay;
import app.owlcms.fieldofplay.BreakType;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.fieldofplay.UIEvent.Decision;
import app.owlcms.fieldofplay.UIEvent.LiftingOrderUpdated;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.RequireLogin;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class Scoreboard
 *
 * Show athlete 6-attempt results
 *
 */
@SuppressWarnings("serial")
@Tag("scoreboard-template")
@JsModule("./components/Scoreboard.js")
@Route("displays/scoreboard")
@Theme(value = Lumo.class, variant = Lumo.DARK)
@Push
public class Scoreboard extends PolymerTemplate<Scoreboard.ScoreboardModel> implements DarkModeParameters,
        SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle, RequireLogin {

    /**
     * ScoreboardModel
     *
     * Vaadin Flow propagates these variables to the corresponding Polymer template
     * JavaScript properties. When the JS properties are changed, a
     * "propname-changed" event is triggered.
     * {@link Element.#addPropertyChangeListener(String, String,
     * com.vaadin.flow.dom.PropertyChangeListener)}
     *
     */
    public interface ScoreboardModel extends TemplateModel {
        String getAttempt();

        String getFullName();

        Integer getStartNumber();

        String getTeamName();

        Integer getWeight();

        Boolean isHidden();

        Boolean isMasters();

        void setAttempt(String formattedAttempt);

        void setFullName(String lastName);

        void setGroupName(String name);

        void setHidden(boolean b);

        void setLiftsDone(String formattedDone);

        void setMasters(boolean b);

        void setStartNumber(Integer integer);

        void setTeamName(String teamName);

        void setWeight(Integer weight);
    }

    final private static Logger logger = (Logger) LoggerFactory.getLogger(Scoreboard.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    @Id("timer")
    private AthleteTimerElement timer; // Flow creates it

    @Id("breakTimer")
    private BreakTimerElement breakTimer; // Flow creates it

    @Id("decisions")
    private DecisionElement decisions; // Flow creates it

    private EventBus uiEventBus;
    private List<Athlete> order;
    private Group curGroup;
    private int liftsDone;

    JsonArray sattempts;
    JsonArray cattempts;
    private boolean darkMode;
    private ContextMenu contextMenu;
    private Location location;
    private UI locationUI;

    /**
     * Instantiates a new results board.
     */
    public Scoreboard() {
        timer.setOrigin(this);
        setDarkMode(true);
    }

    @Override
    public void doBreak() {
        OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            ScoreboardModel model = getModel();
            BreakType breakType = fop.getBreakType();
            model.setFullName(inferGroupName() + " &ndash; " + inferMessage(breakType));
            model.setTeamName("");
            model.setAttempt("");
            model.setHidden(false);

            updateBottom(model, computeLiftType(fop.getCurAthlete()));
            uiEventLogger.debug("$$$ attemptBoard calling doBreak()");
            this.getElement().callJsFunction("doBreak");
        }));
    }

    public void getAthleteJson(Athlete a, JsonObject ja, Category curCat) {
        String category;
        if (Competition.getCurrent().isMasters()) {
            category = a.getShortCategory();
        } else {
            category = curCat != null ? curCat.getName() : "";
        }
        ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
        ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
        ja.put("yearOfBirth", a.getYearOfBirth());
        Integer startNumber = a.getStartNumber();
        ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
        String mastersAgeGroup = a.getMastersAgeGroup();
        ja.put("mastersAgeGroup", mastersAgeGroup != null ? mastersAgeGroup : "");
        ja.put("category", category != null ? category : "");
        getAttemptsJson(a);
        ja.put("sattempts", sattempts);
        ja.put("cattempts", cattempts);
        ja.put("total", formatInt(a.getTotal()));
        ja.put("snatchRank", formatInt(a.getSnatchRank()));
        ja.put("cleanJerkRank", formatInt(a.getCleanJerkRank()));
        ja.put("totalRank", formatInt(a.getTotalRank()));
        Integer liftOrderRank = a.getLiftOrderRank();
        boolean notDone = a.getAttemptsDone() < 6;
        String blink = (notDone ? " blink" : "");
        if (notDone) {
            ja.put("classname", (liftOrderRank == 1 ? "current" + blink : (liftOrderRank == 2) ? "next" : ""));
        }
    }

    @Override
    public String getPageTitle() {
        return getTranslation("Scoreboard");
    }

    @Override
    public boolean isIgnoreGroupFromURL() {
        return true;
    }

    /**
     * Reset.
     */
    public void reset() {
        order = ImmutableList.of();
    }

    @Subscribe
    public void slaveBreakDone(UIEvent.BreakDone e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
            Athlete a = e.getAthlete();
            getModel().setHidden(false);
            if (a == null) {
                order = fop.getLiftingOrder();
                a = order.size() > 0 ? order.get(0) : null;
                liftsDone = AthleteSorter.countLiftsDone(order);
                doUpdate(a, e);
            } else {
                liftsDone = AthleteSorter.countLiftsDone(order);
                doUpdate(a, e);
            }
        }));
    }

    @Subscribe
    public void slaveDecision(UIEvent.Decision e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            getModel().setHidden(false);
            doUpdateBottomPart(e);
            this.getElement().callJsFunction("refereeDecision");
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
    public void slaveDownSignal(UIEvent.DownSignal e) {
        uiLog(e);
        // ignore if the down signal was initiated by this result board.
        // (the timer element on the result board will actually process the keyboard
        // codes if devices are attached)
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
            getModel().setHidden(false);
            this.getElement().callJsFunction("down");
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
        // uiLog(e);
        uiEventLogger.debug("### {} isDisplayToggle={}", this.getClass().getSimpleName(), e.isDisplayToggle());
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            Athlete a = e.getAthlete();
            order = e.getDisplayOrder();
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
            doBreak();
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
                doUpdate(fop.getCurAthlete(), e);
                doBreak();
                break;
            default:
                doUpdate(fop.getCurAthlete(), e);
            }
        });
    }

    public void uiLog(UIEvent e) {
        uiEventLogger.debug("### {} {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin(), LoggerUtils.whereFrom());
    }

    protected void doEmpty() {
        this.getModel().setHidden(true);
    }

    protected void doUpdate(Athlete a, UIEvent e) {
        logger.debug("doUpdate {} {}", a, a != null ? a.getAttemptsDone() : null);
        ScoreboardModel model = getModel();
        boolean leaveTopAlone = false;
        if (e instanceof UIEvent.LiftingOrderUpdated) {
            LiftingOrderUpdated e2 = (UIEvent.LiftingOrderUpdated) e;
            if (e2.isInBreak()) {
                leaveTopAlone = !e2.isDisplayToggle();
            } else {
                leaveTopAlone = !e2.isStopAthleteTimer();
            }
        }

        logger.debug("doUpdate a={} leaveTopAlone={}", a, leaveTopAlone);
        if (a != null && a.getAttemptsDone() < 6) {
            if (!leaveTopAlone) {
                logger.debug("updating top {}", a.getFullName());
                model.setFullName(a.getFullName());
                model.setTeamName(a.getTeam());
                model.setStartNumber(a.getStartNumber());
                String formattedAttempt = formatAttempt(a.getAttemptsDone());
                model.setAttempt(formattedAttempt);
                model.setWeight(a.getNextAttemptRequestedWeight());
                this.getElement().callJsFunction("reset");
                logger.debug("updated top {}", a.getFullName());
            }
            logger.debug("updating bottom");
            updateBottom(model, computeLiftType(a));
        } else {
            if (!leaveTopAlone) {
                logger.debug("doUpdate doDone");
                OwlcmsSession.withFop((fop) -> doDone(fop.getGroup()));
            }
            return;
        }
    }

    /**
     * Compute Json string ready to be used by web component template
     *
     * CSS classes are pre-computed and passed along with the values; weights are
     * formatted.
     *
     * @param a
     * @return json string with nested attempts values
     */
    protected void getAttemptsJson(Athlete a) {
        sattempts = Json.createArray();
        cattempts = Json.createArray();
        XAthlete x = new XAthlete(a);
        Integer liftOrderRank = x.getLiftOrderRank();
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
                            boolean failed = stringValue.startsWith("-");
                            jri.put("goodBadClassName", failed ? "narrow fail" : "narrow good");
                            jri.put("stringValue", formatKg(stringValue));
                        }
                    }
                    break;
                default:
                    if (stringValue != null && !trim.isEmpty()) {
                        String highlight = i.getLiftNo() == curLift && liftOrderRank == 1 ? (" current" + blink)
                                : (i.getLiftNo() == curLift && liftOrderRank == 2) ? " next" : "";
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

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
     * AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // fop obtained via QueryParameterReader interface default methods.
        OwlcmsSession.withFop(fop -> {
            init();
            // sync with current status of FOP
            order = fop.getDisplayOrder();
            liftsDone = AthleteSorter.countLiftsDone(order);
            syncWithFOP(null);
            // we listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        });
        buildContextMenu(this);
        setDarkMode(this, isDarkMode(), false);
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
        if (a == null)
            return "";
        String liftType = a.getAttemptsDone() >= 3 ? Translator.translate("Clean_and_Jerk")
                : Translator.translate("Snatch");
        return liftType;
    }

    private void doDone(Group g) {
        logger.debug("doDone {}", g == null ? null : g.getName());
        if (g == null) {
            doEmpty();
        } else {
            OwlcmsSession.withFop(fop -> {
                updateBottom(getModel(), computeLiftType(fop.getCurAthlete()));
                getModel().setFullName(getTranslation("Group_number_results", g.toString()));
                this.getElement().callJsFunction("groupDone");
            });
        }
    }

    private void doUpdateBottomPart(Decision e) {
        ScoreboardModel model = getModel();
        Athlete a = e.getAthlete();
        updateBottom(model, computeLiftType(a));
    }

    private String formatAttempt(Integer attemptNo) {
        String translate = Translator.translate("AttemptBoard_attempt_number", (attemptNo % 3) + 1);
        return translate;
    }

    private String formatInt(Integer total) {
        if (total == null || total == 0) return "-";
        else if (total == -1) return "inv.";// invited lifter, not eligible.
        else if (total < 0) return  "(" + Math.abs(total) + ")";
        else return total.toString();
    }

    private String formatKg(String total) {
        return (total == null || total.trim().isEmpty()) ? "-"
                : (total.startsWith("-") ? "(" + total.substring(1) + ")" : total);
    }

    /**
     * @param list2
     * @return
     */
    private JsonValue getAthletesJson(List<Athlete> list2) {
        JsonArray jath = Json.createArray();
        int athx = 0;
        Category prevCat = null;
        for (Athlete a : list2) {
            JsonObject ja = Json.createObject();
            Category curCat = a.getCategory();
            if (curCat != null && !curCat.equals(prevCat)) {
                // changing categories, put marker before athlete
                ja.put("isSpacer", true);
                jath.set(athx, ja);
                ja = Json.createObject();
                prevCat = curCat;
                athx++;
            }
            getAthleteJson(a, ja, curCat);
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
            logger.trace("Starting result board on FOP {}", fop.getName());
            setId("scoreboard-" + fop.getName());
            curGroup = fop.getGroup();
            getModel().setMasters(Competition.getCurrent().isMasters());
        });
        setTranslationMap();
        order = ImmutableList.of();
    }

    private void updateBottom(ScoreboardModel model, String liftType) {
        OwlcmsSession.withFop((fop) -> {
            curGroup = fop.getGroup();
            model.setGroupName(
                    curGroup != null ? Translator.translate("Scoreboard.GroupLiftType", curGroup.getName(), liftType)
                            : "");
            order = fop.getDisplayOrder();
            model.setLiftsDone(Translator.translate("Scoreboard.AttemptsDone", liftsDone));
            this.getElement().setPropertyJson("athletes", getAthletesJson(order));
        });

    }

    @Override
    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
    }

    @Override
    public boolean isDarkMode() {
        return darkMode;
    }
    
    @Override
    public ContextMenu getContextMenu() {
        return contextMenu;
    }
    
    @Override
    public void setContextMenu(ContextMenu contextMenu) {
        this.contextMenu = contextMenu;
    }
    
    @Override
    public Location getLocation() {
        return this.location;
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public UI getLocationUI() {
        return this.locationUI;
    }

    @Override
    public void setLocationUI(UI locationUI) {
        this.locationUI = locationUI;
    }

}