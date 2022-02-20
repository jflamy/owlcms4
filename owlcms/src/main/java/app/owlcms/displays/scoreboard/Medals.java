/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.LoggerFactory;

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

import app.owlcms.apputils.SoundUtils;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
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
import app.owlcms.uievents.UIEvent.LiftingOrderUpdated;
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
 * Show athlete 6-attempt results and leaders for the athlete's category
 *
 */
@SuppressWarnings("serial")
@Tag("medals-template")
@JsModule("./components/Medals.js")
@Route("displays/medals")
@Theme(value = Lumo.class, variant = Lumo.DARK)
@Push
public class Medals extends PolymerTemplate<Medals.MedalsTemplate>
        implements DisplayParameters, SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle,
        RequireLogin {

    /**
     * MedalsTemplate
     *
     * Vaadin Flow propagates these variables to the corresponding Polymer template JavaScript properties. When the JS
     * properties are changed, a "propname-changed" event is triggered.
     * {@link Element.#addPropertyChangeListener(String, String, com.vaadin.flow.dom.PropertyChangeListener)}
     *
     */
    public interface MedalsTemplate extends TemplateModel {
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

    final private Logger logger = (Logger) LoggerFactory.getLogger(Medals.class);

    final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    private EventBus uiEventBus;

    JsonArray sattempts;

    JsonArray cattempts;
    private boolean darkMode = true;
    private Location location;
    private UI locationUI;
    private Dialog dialog;
    private boolean silenced = true;
    private boolean initializationNeeded;

    private TreeMap<Category, TreeSet<Athlete>> medals;

    /**
     * Instantiates a new results board.
     */
    public Medals() {
        uiEventLogger.setLevel(Level.DEBUG);
        OwlcmsFactory.waitDBInitialized();
        setDarkMode(true);
    }

    /**
     * @see app.owlcms.apputils.queryparameters.DisplayParameters#addDialogContent(com.vaadin.flow.component.Component,
     *      com.vaadin.flow.component.orderedlayout.VerticalLayout)
     */
    @Override
    public void addDialogContent(Component target, VerticalLayout vl) {
        DisplayOptions.addLightingEntries(vl, target, this);
        // vl.add(new Hr());
        // DisplayOptions.addSoundEntries(vl, target, this);
    }

    @Override
    public void doBreak() {
        OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            MedalsTemplate model = getModel();
            BreakType breakType = fop.getBreakType();
            model.setFullName(inferGroupName() + " &ndash; " + inferMessage(breakType));
            model.setTeamName("");
            model.setAttempt("");
            setHidden(false);

            updateBottom(model, computeLiftType(fop.getCurAthlete()), fop);
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
    public Location getLocation() {
        return this.location;
    }

    @Override
    public UI getLocationUI() {
        return this.locationUI;
    }

    @Override
    public String getPageTitle() {
        return getTranslation("BreakType.MEDALS") + OwlcmsSession.getFopNameIfMultiple();
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

    @Override
    public void setSilenced(boolean silent) {
    }

    @Subscribe
    public void slaveBreakDone(UIEvent.BreakDone e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
            setHidden(false);
            doUpdate(e);
        }));
    }

    @Subscribe
    public void slaveDecision(UIEvent.Decision e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            setHidden(false);
            doUpdateBottomPart(e);
        });
    }

    @Subscribe
    public void slaveGlobalRankingUpdated(UIEvent.GlobalRankingUpdated e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            computeMedalJson();
        });
    }

    @Subscribe
    public void slaveGroupDone(UIEvent.GroupDone e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            setHidden(false);
//          Group g = e.getGroup();
        });
    }

    @Subscribe
    public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            setHidden(false);
            doUpdate(e);
        });
    }

    @Subscribe
    public void slaveStartBreak(UIEvent.BreakStarted e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            setHidden(false);
            doBreak();
        });
    }

    @Subscribe
    public void slaveStartLifting(UIEvent.StartLifting e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            setHidden(false);
            this.getElement().callJsFunction("reset");
        });
    }

    @Subscribe
    public void slaveStopBreak(UIEvent.BreakDone e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            setHidden(false);
            this.getElement().callJsFunction("reset");
            doUpdate(e);
        });
    }

    @Subscribe
    public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            syncWithFOP(e);
        });
    }

    protected void doEmpty() {
        this.setHidden(true);
    }

    protected void doUpdate(UIEvent e) {
        logger.debug("doUpdate {} {} {}", e != null ? e.getClass().getSimpleName() : "no event");
        MedalsTemplate model = getModel();
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
            this.getElement().callJsFunction("reset");
        }
        logger.debug("updating bottom");
        updateBottom(model, null, fop);
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
            medals = fop.getMedals();
            setHidden(false);
            computeMedalJson();
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

    private void computeMedalJson() {
        OwlcmsSession.withFop(fop -> {
            medals = fop.getMedals();
            JsonArray jsonMCArray = Json.createArray();
            int mcX = 0;
            for (Entry<Category, TreeSet<Athlete>> medalCat : medals.entrySet()) {
                JsonObject jMC = Json.createObject();
                TreeSet<Athlete> medalists = medalCat.getValue();
                if (medalists != null && !medalists.isEmpty()) {
                    jMC.put("categoryName", medalCat.getKey().getName());
                    jMC.put("leaders", getAthletesJson(new ArrayList<>(medalists), fop));
                    logger.warn("medalCategory: {}", jMC.toJson());
                    jsonMCArray.set(mcX, jMC);
                    mcX++;
                }
            }
            logger.warn("medalCategories {}", jsonMCArray.toJson());
            this.getElement().setPropertyJson("medalCategories", jsonMCArray);
            if (mcX == 0) {
                this.getElement().setProperty("noCategories", true);
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

    private void doUpdateBottomPart(UIEvent e) {
        MedalsTemplate model = getModel();
        updateBottom(model, null, OwlcmsSession.getFop());
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

    private String formatRank(Integer total) {
        if (total == null || total == 0) {
            return "";
        } else if (total == -1) {
            return "inv.";// invited lifter, not eligible.
        } else {
            return total.toString();
        }
    }

    private void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank, FieldOfPlay fop) {
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
        ja.put("cattempts", cattempts);
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

        boolean notDone = a.getAttemptsDone() < 6;
        String blink = (notDone ? " blink" : "");
        String highlight = "";
        if (fop.getState() != FOPState.DECISION_VISIBLE && notDone) {
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
        logger.debug("{} {} {}", a.getShortName(), fop.getState(), highlight);
        ja.put("classname", highlight);
    }

    /**
     * @param groupAthletes, List<Athlete> liftOrder
     * @return
     */
    private JsonValue getAthletesJson(List<Athlete> displayOrder, FieldOfPlay fop) {
        JsonArray jath = Json.createArray();
        int athx = 0;
        Category prevCat = null;
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
            // no blinking = 0
            getAthleteJson(a, ja, curCat, 0, fop);
            String team = a.getTeam();
            if (team != null && team.trim().length() > Competition.SHORT_TEAM_LENGTH) {
                logger.trace("long team {}", team);
                setWideTeamNames(true);
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
     * @param fop
     * @return json string with nested attempts values
     */
    private void getAttemptsJson(Athlete a, int liftOrderRank, FieldOfPlay fop) {
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
                        // logger.debug("{} {} {}", fop.getState(), x.getShortName(), curLift);

                        String highlight = "";
                        // don't blink while decision is visible. wait until lifting displayOrder has been
                        // recomputed and we get DECISION_RESET
                        int liftBeingDisplayed = i.getLiftNo();
                        if (liftBeingDisplayed == curLift && (fop.getState() != FOPState.DECISION_VISIBLE)) {
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

    private Object getOrigin() {
        return this;
    }

    private void init() {
        OwlcmsSession.withFop(fop -> {
            logger.trace("{}Starting result board on FOP {}", fop.getLoggingName());
            setId("scoreboard-" + fop.getName());
            setWideTeamNames(false);
            getModel().setCompetitionName(Competition.getCurrent().getCompetitionName());
        });
        setTranslationMap();
    }

    private void setHidden(boolean hidden) {
        logger.warn("setHidden {}", LoggerUtils.whereFrom());
        this.getElement().setProperty("hiddenStyle", (hidden ? "display:none" : "display:block"));
        this.getElement().setProperty("inactiveStyle", (hidden ? "display:block" : "display:none"));
        this.getElement().setProperty("inactiveClass", (hidden ? "bigTitle" : ""));
    }

    private void setWideTeamNames(boolean wide) {
        this.getElement().setProperty("teamWidthClass", (wide ? "wideTeams" : "narrowTeams"));
    }

    private void syncWithFOP(UIEvent.SwitchGroup e) {
        logger.warn("sync {}", e.getState());
        switch (e.getState()) {
        case INACTIVE:
            doEmpty();
            break;
        case BREAK:
            if (e.getGroup() == null) {
                doEmpty();
            } else {
                doUpdate(e);
                doBreak();
            }
            break;
        default:
            setHidden(false);
            doUpdate(e);
        }
    }

    private void uiLog(UIEvent e) {
        uiEventLogger.debug("### {} {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin(), LoggerUtils.whereFrom());
    }

    private void updateBottom(MedalsTemplate model, String liftType, FieldOfPlay fop) {
        logger.warn("updateBottom");
        model.setGroupName("");
        model.setLiftsDone("Y");
        // this.getElement().callJsFunction("groupDone");

        computeMedalJson();
    }
}