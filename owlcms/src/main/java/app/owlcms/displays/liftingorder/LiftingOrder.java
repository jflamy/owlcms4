/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.displays.liftingorder;

import java.util.Enumeration;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.material.Material;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.displays.attemptboard.BreakDisplay;
import app.owlcms.fieldofplay.BreakType;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.fieldofplay.UIEvent.BreakStarted;
import app.owlcms.fieldofplay.UIEvent.Decision;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.ui.shared.QueryParameterReader;
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
 * Class LiftingOrder
 *
 * Show athlete 6-attempt results
 *
 */
@SuppressWarnings("serial")
@Tag("liftingorder-template")
@HtmlImport("frontend://components/LiftingOrder.html")
@Route("displays/liftingorder")
@Theme(value = Material.class, variant = Material.DARK)
@Push
public class LiftingOrder extends PolymerTemplate<LiftingOrder.LiftingOrderModel>
implements QueryParameterReader, SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle, RequireLogin {

    /**
     * LiftingOrderModel
     *
     * Vaadin Flow propagates these variables to the corresponding Polymer template JavaScript
     * properties. When the JS properties are changed, a "propname-changed" event is triggered.
     * {@link Element.#addPropertyChangeListener(String, String,
     * com.vaadin.flow.dom.PropertyChangeListener)}
     *
     */
    public interface LiftingOrderModel extends TemplateModel {
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
    final private static Logger logger = (Logger) LoggerFactory.getLogger(LiftingOrder.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());

    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.DEBUG);
    }


    private EventBus uiEventBus;
    private List<Athlete> liftingOrder;
    private Group curGroup;
    private int liftsDone;

    JsonArray sattempts;
    JsonArray cattempts;

    /**
     * Instantiates a new results board.
     */
    public LiftingOrder() {
//        timer.setOrigin(this);
    }

    @Subscribe
    public void breakDone(UIEvent.BreakDone e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            Athlete a = e.getAthlete();
            liftsDone = AthleteSorter.countLiftsDone(liftingOrder);
            doUpdate(a, e);
        });
    }

    @Override
    public void doBreak(BreakStarted e) {
        uiEventLogger.debug("$$$ {} [{}]", e.getClass().getSimpleName(), LoggerUtils.whereFrom());
        OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            BreakType breakType = fop.getBreakType();
            getModel().setFullName(inferGroupName()+" "+inferMessage(breakType));
            getModel().setTeamName("");
            getModel().setAttempt("");

            uiEventLogger.debug("$$$ attemptBoard calling doBreak()");
            this.getElement().callFunction("doBreak");
        }));
    }

    @Override
    public String getPageTitle() {
        return "LiftingOrder";
    }

    @Override
    public boolean isIgnoreGroupFromURL() {
        return true;
    }

    /**
     * Reset.
     */
    public void reset() {
        liftingOrder = ImmutableList.of();
    }

    @Subscribe
    public void slaveAthleteAnnounced(UIEvent.AthleteAnnounced e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            Athlete a = e.getAthlete();
            doUpdate(a, e);
        });
    }

    @Subscribe
    public void slaveDecision(UIEvent.Decision e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            doUpdateBottomPart(e);
//            this.getElement().callFunction("refereeDecision");
        });
    }


    @Subscribe
    public void slaveDecisionReset(UIEvent.DecisionReset e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            this.getElement().callFunction("reset");
        });
    }


    @Subscribe
    public void slaveGroupDone(UIEvent.GroupDone e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> doDone(e.getGroup()));
    }

    @Subscribe
    public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            this.getElement().callFunction("reset");
            Athlete a = e.getAthlete();
            liftingOrder = e.getLiftingOrder();
            liftsDone = AthleteSorter.countLiftsDone(liftingOrder);
            doUpdate(a, e);
        });
    }

    @Subscribe
    public void slaveStartBreak(UIEvent.BreakStarted e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        UIEventProcessor.uiAccess(this, uiEventBus, e, () ->  doBreak(e));
    }

    @Subscribe
    public void slaveStopBreak(UIEvent.BreakDone e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        Athlete a = e.getAthlete();
        this.getElement().callFunction("reset");
        doUpdate(a, e);
    }

    public void uiLog(UIEvent e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(), this.getOrigin(), e.getOrigin());
    }


    protected void doHide() {
        this.getModel().setHidden(true);
    }

    protected void doUpdate(Athlete a, UIEvent e) {
        logger.debug("doUpdate {} {}",a, a != null ? a.getAttemptsDone() : null);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            LiftingOrderModel model = getModel();
            model.setHidden(a == null);
//            boolean leaveTopAlone = e instanceof UIEvent.LiftingOrderUpdated && !((UIEvent.LiftingOrderUpdated)e).isStopAthleteTimer();
            if (a != null) {
                model.setFullName(getTranslation("Scoreboard.LiftingOrder"));
//                if (!leaveTopAlone) {
//                    this.getElement().callFunction("reset");
//                    model.setFullName(a.getFullName());
//                    model.setTeamName(a.getTeam());
//                    model.setStartNumber(a.getStartNumber());
//                    String formattedAttempt = formatAttempt(a.getAttemptsDone());
//                    model.setAttempt(formattedAttempt);
//                    model.setWeight(a.getNextAttemptRequestedWeight());
//                }
                updateBottom(model,computeLiftType(a));
            }
        });
        if (a == null || a.getAttemptsDone() >= 6) {
            OwlcmsSession.withFop((fop) -> doDone(fop.getGroup()));
            return;
        }
    }

//    /**
//     * Compute Json string ready to be used by web component template
//     *
//     * CSS classes are pre-computed and passed along with the values; weights are formatted.
//     *
//     * @param a
//     * @return json string with nested attempts values
//     */
//    protected void getAttemptsJson(Athlete a) {
//        sattempts = Json.createArray(); // snatch
//        cattempts = Json.createArray(); // clean-and-jerk
//        XAthlete x = new XAthlete(a);
//        Integer liftOrderRank = x.getLiftOrderRank();
//        Integer curLift = x.getAttemptsDone();
//        int ix = 0;
//        for (LiftInfo i : x.getRequestInfoArray()) {
//            JsonObject jri = Json.createObject();
//            String stringValue = i.getStringValue();
//            boolean notDone = x.getAttemptsDone() < 6;
//            String blink = (notDone ? " blink" : "");
//
//            jri.put("goodBadClassName", "narrow empty");
//            jri.put("stringValue", "");
//            if (i.getChangeNo() >= 0) {
//                String trim = stringValue != null ? stringValue.trim() : "";
//                switch (Changes.values()[i.getChangeNo()]) {
//                case ACTUAL:
//                    if (!trim.isEmpty()) {
//                        if (trim.contentEquals("-") || trim.contentEquals("0")) {
//                            jri.put("goodBadClassName", "narrow fail");
//                            jri.put("stringValue", "-");
//                        } else {
//                            boolean failed = stringValue.startsWith("-");
//                            jri.put("goodBadClassName", failed ? "narrow fail" : "narrow good");
//                            jri.put("stringValue", formatKg(stringValue));
//                        }
//                    }
//                    break;
//                default:
//                    if (stringValue != null && !trim.isEmpty()) {
//                        String highlight = i.getLiftNo() == curLift && liftOrderRank == 1 ? (" current" + blink)
//                                : (i.getLiftNo() == curLift && liftOrderRank == 2) ? " next" : "";
//                        jri.put("goodBadClassName","narrow request");
//                        if (notDone) {
//                            jri.put("className", highlight);
//                        }
//                        jri.put("stringValue", stringValue);
//                    }
//                    break;
//                }
//            }
//
//            if (ix < 3) {
//                sattempts.set(ix, jri);
//            } else {
//                cattempts.set(ix % 3, jri);
//            }
//            ix++;
//        }
//    }

    /* @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent) */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // fop obtained via QueryParameterReader interface default methods.
        OwlcmsSession.withFop(fop -> {
            init();
            // sync with current status of FOP
            liftingOrder = fop.getLiftingOrder();
            liftsDone = AthleteSorter.countLiftsDone(liftingOrder);
            doUpdate(fop.getCurAthlete(), null);
            // we listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        });
    }

    protected void setTranslationMap() {
        JsonObject translations = Json.createObject();
        Enumeration<String> keys = Translator.getKeys();
        while (keys.hasMoreElements()) {
            String curKey = keys.nextElement();
            if (curKey.startsWith("Scoreboard.")) {
                translations.put(curKey.replace("Scoreboard.", ""),Translator.translate(curKey));
            }
        }
        this.getElement().setPropertyJson("t", translations);
    }

    private String computeLiftType(Athlete a) {
        if (a == null) return "";
        String liftType = a.getAttemptsDone() >= 3 ? Translator.translate("Clean_and_Jerk") : Translator.translate("Snatch");
        return liftType;
    }

    private void doDone(Group g) {
        if (g == null) return;
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            getModel().setFullName(getTranslation("Group_number_done", g.toString()));
            this.getElement().callFunction("groupDone");
        });
    }

    private void doUpdateBottomPart(Decision e) {
        LiftingOrderModel model = getModel();
        Athlete a = e.getAthlete();
        updateBottom(model,computeLiftType(a));
    }

//    private String formatAttempt(Integer attemptNo) {
//        return MessageFormat.format("{0}<sup>{0,choice,1#st|2#nd|3#rd}</sup> att.",(attemptNo%3)+1);
//    }

//    private String formatInt(Integer total) {
//        if (total == -1) return "inv.";//invited lifter, not eligible.
//        return (total == null || total == 0) ? "-" : (total < 0 ? "("+Math.abs(total)+")" : total.toString());
//    }
    
//    private String formatKg(String total) {
//        return (total == null || total.trim().isEmpty()) ? "-" : (total.startsWith("-") ? "("+total.substring(1)+")" : total);
//    }

    private JsonValue getAthletesJson(List<Athlete> list2) {
        JsonArray jath = Json.createArray();
        int athx = 0;
//        Category prevCat = null;
        for (Athlete a: list2) {
            JsonObject ja = Json.createObject();
            Category curCat = a.getCategory();
//            if (curCat != null && !curCat.equals(prevCat)) {
//                // changing categories, put marker before athlete
//                ja.put("isSpacer", true);
//                jath.set(athx, ja);
//                ja = Json.createObject();
//                prevCat = curCat;
//                athx++;
//            }
            String category;
            if (Competition.getCurrent().isMasters()) {
                category = a.getShortCategory();
            } else {
                category = curCat != null ? curCat.getName() : "";
            }
            ja.put("fullName", a.getFullName());
            ja.put("teamName", a.getTeam());
            ja.put("yearOfBirth", a.getYearOfBirth());
            Integer startNumber = a.getStartNumber();
            ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
            ja.put("mastersAgeGroup", a.getMastersAgeGroup());
            ja.put("category", category);
//            getAttemptsJson(a);
//            ja.put("sattempts", sattempts);
//            ja.put("cattempts", cattempts);
//            ja.put("total", formatInt(a.getTotal()));
//            ja.put("snatchRank", formatInt(a.getSnatchRank()));
//            ja.put("cleanJerkRank", formatInt(a.getCleanJerkRank()));
//            ja.put("totalRank", formatInt(a.getTotalRank()));
            ja.put("nextAttemptNo", AthleteGridContent.formatAttemptNumber(a));
            Integer nextAttemptRequestedWeight = a.getNextAttemptRequestedWeight();
            ja.put("requestedWeight", nextAttemptRequestedWeight == 0 ? "-" : nextAttemptRequestedWeight.toString());
            Integer liftOrderRank = a.getLiftOrderRank();
            boolean notDone = a.getAttemptsDone() < 6;
            String blink =  (notDone ? " blink" : "");
            if (notDone) {
                ja.put("classname", (liftOrderRank == 1 ? "current"+blink : (liftOrderRank == 2) ? "next" : ""));
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
            logger.trace("Starting result board on FOP {}", fop.getName());
            setId("scoreboard-"+fop.getName());
            curGroup = fop.getGroup();
            getModel().setMasters(Competition.getCurrent().isMasters());
        });
        setTranslationMap();
        liftingOrder = ImmutableList.of();
    }

    private void updateBottom(LiftingOrderModel model, String liftType) {
        OwlcmsSession.withFop((fop) -> {
            curGroup = fop.getGroup();
            model.setGroupName(curGroup != null ? Translator.translate("Scoreboard.GroupLiftType", curGroup.getName(), liftType) : "");
        });
        model.setLiftsDone(Translator.translate("Scoreboard.AttemptsDone",liftsDone)); //$NON-NLS-1$
        this.getElement().setPropertyJson("athletes", getAthletesJson(liftingOrder)); //$NON-NLS-1$
    }
}
