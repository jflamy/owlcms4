/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

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
import com.vaadin.flow.component.html.Hr;
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

import app.owlcms.apputils.SoundUtils;
import app.owlcms.apputils.queryparameters.DisplayParameters;
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
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.RequireLogin;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.Decision;
import app.owlcms.uievents.UIEvent.LiftingOrderUpdated;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import app.owlcms.i18n.Translator;

/**
 * Class Scoreboard
 *
 * Show athlete 6-attempt results and leaders for the athlete's category
 *
 */
@SuppressWarnings("serial")
@Tag("scoreleader-template")
@JsModule("./components/ScoreWithLeaders.js")
@JsModule("./components/AudioContext.js")
@Route("displays/scoreleader")
@Theme(value = Lumo.class, variant = Lumo.DARK)
@Push
public class ScoreWithLeaders extends PolymerTemplate<ScoreWithLeaders.ScoreboardModel>
        implements DisplayParameters, SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle,
        RequireLogin {

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

        Boolean isHidden();

        Boolean isWideTeamNames();

        void setAttempt(String formattedAttempt);

        void setCategoryName(String categoryName);

        void setCompetitionName(String competitionName);

        void setFullName(String lastName);

        void setGroupName(String name);

        void setHidden(boolean b);

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
    private boolean darkMode = true;
    private Location location;
    private UI locationUI;
    private boolean groupDone;
    private Dialog dialog;
    private boolean silenced = true;
    private boolean initializationNeeded;

    /**
     * Instantiates a new results board.
     */
    public ScoreWithLeaders() {
        OwlcmsFactory.waitDBInitialized();
        timer.setOrigin(this);
        setDarkMode(true);
    }

    /**
     * @see app.owlcms.apputils.queryparameters.DisplayParameters#addDialogContent(com.vaadin.flow.component.Component,
     *      com.vaadin.flow.component.orderedlayout.VerticalLayout)
     */
    @Override
    public void addDialogContent(Component target, VerticalLayout vl) {
        DisplayOptions.addLightingEntries(vl, target, this);
        vl.add(new Hr());
        DisplayOptions.addSoundEntries(vl, target, this);
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
            uiEventLogger.debug("$$$ scoreWithLeaders calling doBreak()");
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
        if (dialog == null) {
            dialog = new Dialog();
            return dialog;
        } else {
            return null;
        }
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
        return getTranslation("ScoreboardWLeadersTitle");
    }

    @Override
    public boolean isDarkMode() {
        return darkMode;
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
        return silenced;
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
        this.timer.setSilenced(silenced);
        this.breakTimer.setSilenced(silenced);
        this.silenced = silenced;
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
            if (isDone()) {
                doDone(e.getAthlete().getGroup());
            } else {
                this.getElement().callJsFunction("reset");
            }
        });
    }

    @Subscribe
    public void slaveDownSignal(UIEvent.DownSignal e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            getModel().setHidden(false);
            this.getElement().callJsFunction("down");
        });
    }

    @Subscribe
    public void slaveGlobalRankingUpdated(UIEvent.GlobalRankingUpdated e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            computeLeaders();
        });
    }

    @Subscribe
    public void slaveGroupDone(UIEvent.GroupDone e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            getModel().setHidden(false);
//          Group g = e.getGroup();
            setDone(true);
        });
    }

    @Subscribe
    public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            Athlete a = e.getAthlete();
            order = e.getDisplayOrder();
            liftsDone = AthleteSorter.countLiftsDone(order);
            doUpdate(a, e);
        });
    }

    @Subscribe
    public void slaveStartBreak(UIEvent.BreakStarted e) {
        uiLog(e);
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
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            getModel().setHidden(false);
            Athlete a = e.getAthlete();
            this.getElement().callJsFunction("reset");
            doUpdate(a, e);
        });
    }

    @Subscribe
    public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            syncWithFOP(e);
        });
    }

    private void uiLog(UIEvent e) {
        uiEventLogger.debug("### {} {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin(), LoggerUtils.whereFrom());
    }

    protected void doEmpty() {
        this.getModel().setHidden(true);
    }

    protected void doUpdate(Athlete a, UIEvent e) {
        logger.debug("doUpdate {} {} {}", e != null ? e.getClass().getSimpleName() : "no event", a,
                a != null ? a.getAttemptsDone() : null);
        ScoreboardModel model = getModel();
        boolean leaveTopAlone = false;
        if (e instanceof UIEvent.LiftingOrderUpdated) {
            LiftingOrderUpdated e2 = (UIEvent.LiftingOrderUpdated) e;
            if (e2.isInBreak()) {
                leaveTopAlone = !e2.isDisplayToggle();
            } else {
                leaveTopAlone = !e2.isCurrentDisplayAffected();
            }
        }

        FieldOfPlay fop = OwlcmsSession.getFop();
        if (!leaveTopAlone) {
            if (a != null) {
                Group group = fop.getGroup();
                if (!group.isDone()) {
                    logger.debug("updating top {} {} {}", a.getFullName(), group, System.identityHashCode(group));
                    model.setFullName(a.getFullName());
                    model.setTeamName(a.getTeam());
                    model.setStartNumber(a.getStartNumber());
                    String formattedAttempt = formatAttempt(a.getAttemptsDone());
                    model.setAttempt(formattedAttempt);
                    model.setWeight(a.getNextAttemptRequestedWeight());
                } else {
                    logger.debug("group done {} {}", group, System.identityHashCode(group));
                    doBreak();
                }
            }
            this.getElement().callJsFunction("reset");
        }
        logger.debug("updating bottom");
        updateBottom(model, computeLiftType(a));
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // crude workaround -- randomly getting light or dark due to multiple themes detected in app.
        getElement().executeJs("document.querySelector('html').setAttribute('theme', 'dark');");

        // fop obtained via FOPParameters interface default methods.
        OwlcmsSession.withFop(fop -> {
            init();

            // get the global category rankings for the group
            order = fop.getDisplayOrder();

            liftsDone = AthleteSorter.countLiftsDone(order);
            syncWithFOP(new UIEvent.SwitchGroup(fop.getGroup(), fop.getState(), fop.getCurAthlete(), this));
            // we listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        });
        switchLightingMode(this, isDarkMode(), true);
        SoundUtils.enableAudioContextNotification(this.getElement());
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

    private void computeLeaders() {
        OwlcmsSession.withFop(fop -> {
            Athlete curAthlete = fop.getCurAthlete();
            if (curAthlete != null && curAthlete.getGender() != null) {
                getModel().setCategoryName(curAthlete.getCategory().getName());

                order = fop.getLeaders();
                if (order != null && order.size() > 0) {
                    // null as second argument because we do not highlight current athletes in the leaderboard
                    this.getElement().setPropertyJson("leaders", getAthletesJson(order, null));
                } else {
                    // nothing to show
                    this.getElement().setPropertyJson("leaders", Json.createNull());
                }
            }
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

    private void doDone(Group g) {
        logger.debug("doDone {}", g == null ? null : g.getName());
        if (g == null) {
            doEmpty();
        } else {
            OwlcmsSession.withFop(fop -> {
                updateBottom(getModel(), null);
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

    private String formatKg(String total) {
        return (total == null || total.trim().isEmpty()) ? "-"
                : (total.startsWith("-") ? "(" + total.substring(1) + ")" : total);
    }

    private void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank) {
        String category;
        category = curCat != null ? curCat.getName() : "";
        ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
        ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
        ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
        Integer startNumber = a.getStartNumber();
        ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
        ja.put("category", category != null ? category : "");
        getAttemptsJson(a, liftOrderRank);
        ja.put("sattempts", sattempts);
        ja.put("cattempts", cattempts);
        ja.put("total", formatInt(a.getTotal()));
        ja.put("snatchRank", formatInt(a.getMainRankings().getSnatchRank()));
        ja.put("cleanJerkRank", formatInt(a.getMainRankings().getCleanJerkRank()));
        ja.put("totalRank", formatInt(a.getMainRankings().getTotalRank()));
        ja.put("group", a.getGroup() != null ? a.getGroup().getName() : "");
        boolean notDone = a.getAttemptsDone() < 6;
        String blink = (notDone ? " blink" : "");
        if (notDone) {
            ja.put("classname", (liftOrderRank == 1 ? "current" + blink : (liftOrderRank == 2) ? "next" : ""));
        }
    }

    /**
     * @param groupAthletes, List<Athlete> liftOrder
     * @return
     */
    private JsonValue getAthletesJson(List<Athlete> displayOrder, List<Athlete> liftOrder) {
        JsonArray jath = Json.createArray();
        int athx = 0;
        Category prevCat = null;
        long currentId = (liftOrder != null && liftOrder.size() > 0) ? liftOrder.get(0).getId() : -1L;
        long nextId = (liftOrder != null && liftOrder.size() > 1) ? liftOrder.get(1).getId() : -1L;
        List<Athlete> athletes = displayOrder != null ? Collections.unmodifiableList(displayOrder)
                : Collections.emptyList();
        for (Athlete a : athletes) {
            JsonObject ja = Json.createObject();
            Category curCat = a.getCategory();
            if (curCat != null && !curCat.sameAs(prevCat)) {
                // changing categories, put marker before athlete
                ja.put("isSpacer", true);
                jath.set(athx, ja);
                ja = Json.createObject();
                prevCat = curCat;
                athx++;
            }
            // compute the blinking rank
            getAthleteJson(a, ja, curCat, (a.getId() == currentId)
                    ? 1
                    : ((a.getId() == nextId)
                            ? 2
                            : 0));
            String team = a.getTeam();
            if (team != null && team.trim().length() > Competition.SHORT_TEAM_LENGTH) {
                logger.trace("long team {}", team);
                getModel().setWideTeamNames(true);
            }
            jath.set(athx, ja);
            athx++;
        }
        return jath;
    }

    /**
     * Compute Json string ready to be used by web component template
     *
     * CSS classes are pre-computed and passed along with the values; weights are formatted.
     *
     * @param a
     * @param liftOrderRank2
     * @return json string with nested attempts values
     */
    private void getAttemptsJson(Athlete a, int liftOrderRank) {
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

    private Object getOrigin() {
        return this;
    }

    private void init() {
        OwlcmsSession.withFop(fop -> {
            logger.trace("{}Starting result board on FOP {}", fop.getLoggingName());
            setId("scoreboard-" + fop.getName());
            curGroup = fop.getGroup();
            getModel().setWideTeamNames(false);
            getModel().setCompetitionName(Competition.getCurrent().getCompetitionName());
        });
        setTranslationMap();
        order = ImmutableList.of();
    }

    private boolean isDone() {
        return this.groupDone;
    }

    private void setDone(boolean b) {
        this.groupDone = b;
    }

    private void syncWithFOP(UIEvent.SwitchGroup e) {
        switch (e.getState()) {
        case INACTIVE:
            doEmpty();
            break;
        case BREAK:
            if (e.getGroup() == null) {
                doEmpty();
            } else {
                doUpdate(e.getAthlete(), e);
                doBreak();
            }
            break;
        default:
            doUpdate(e.getAthlete(), e);
        }
    }

    private void updateBottom(ScoreboardModel model, String liftType) {
        OwlcmsSession.withFop((fop) -> {
            curGroup = fop.getGroup();
            order = fop.getDisplayOrder();
            if (liftType != null) {
                model.setGroupName(
                        curGroup != null
                                ? Translator.translate("Scoreboard.GroupLiftType", curGroup.getName(), liftType)
                                : "");
                liftsDone = AthleteSorter.countLiftsDone(order);
                model.setLiftsDone(Translator.translate("Scoreboard.AttemptsDone", liftsDone));
            } else {
                model.setGroupName("X");
                model.setLiftsDone("Y");
                this.getElement().callJsFunction("groupDone");
            }
            this.getElement().setPropertyJson("athletes",
                    getAthletesJson(fop.getDisplayOrder(), fop.getLiftingOrder()));
            computeLeaders();
        });
    }
}