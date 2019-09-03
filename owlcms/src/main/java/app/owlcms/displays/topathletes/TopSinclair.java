/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.displays.topathletes;

import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.LoggerFactory;

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
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.competition.Competition;
import app.owlcms.displays.attemptboard.BreakDisplay;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.QueryParameterReader;
import app.owlcms.ui.shared.RequireLogin;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class TopSinclair
 *
 * Show athlete lifting order
 *
 */
@SuppressWarnings("serial")
@Tag("topsinclair-template")
@HtmlImport("frontend://components/TopSinclair.html")
@Route("displays/topsinclair")
@Theme(value = Material.class, variant = Material.DARK)
@Push
public class TopSinclair extends PolymerTemplate<TopSinclair.LiftingOrderModel> implements QueryParameterReader,
        SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle, RequireLogin {

    /**
     * LiftingOrderModel
     *
     * Vaadin Flow propagates these variables to the corresponding Polymer template
     * JavaScript properties. When the JS properties are changed, a
     * "propname-changed" event is triggered.
     * {@link Element.#addPropertyChangeListener(String, String,
     * com.vaadin.flow.dom.PropertyChangeListener)}
     *
     */
    public interface LiftingOrderModel extends TemplateModel {

        String getFullName();

        Boolean isHidden();

        Boolean isMasters();

        void setFullName(String lastName);

        void setHidden(boolean b);

        void setMasters(boolean b);
    }

    final private static Logger logger = (Logger) LoggerFactory.getLogger(TopSinclair.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    private EventBus uiEventBus;

    JsonArray sattempts;
    JsonArray cattempts;
    private double topManSinclair;
    private double topWomanSinclair;
    private List<Athlete> sortedMen;
    private List<Athlete> sortedWomen;

    /**
     * Instantiates a new results board.
     */
    public TopSinclair() {
    }

    @Override
    public void doBreak() {
        OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            // just update the display
            doUpdate(fop.getCurAthlete(), null);
        }));
    }

    @Override
    public String getPageTitle() {
        return getTranslation("Scoreboard.TopSinclair");
    }

    @Override
    public boolean isIgnoreGroupFromURL() {
        return true;
    }

    /**
     * Reset.
     */
    public void reset() {
    }

    @Subscribe
    public void slaveGlobalRankingUpdated(UIEvent.GlobalRankingUpdated e) {
        uiLog(e);
        Competition competition = Competition.getCurrent();

        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            this.getElement().callJsFunction("reset");

            sortedMen = competition.getGlobalSinclairRanking(Gender.M);
            sortedWomen = competition.getGlobalSinclairRanking(Gender.F);

            int minMen = java.lang.Math.min(5, sortedMen.size());
            sortedMen = sortedMen.subList(0, minMen);
            ListIterator<Athlete> iterMen = sortedMen.listIterator();
            while (iterMen.hasNext()) {
                if (iterMen.next().getSinclairForDelta() <= 0)
                    iterMen.remove();
            }
            Athlete topMan = (sortedMen.size() > 0 ? sortedMen.get(0) : null);
            topManSinclair = (topMan != null ? topMan.getSinclairForDelta() : 999.0D);

            int minWomen = java.lang.Math.min(5, sortedWomen.size());
            sortedWomen = sortedWomen.subList(0, minWomen);
            ListIterator<Athlete> iterWomen = sortedWomen.listIterator();
            while (iterWomen.hasNext()) {
                Athlete curAthlete = iterWomen.next();
                if (curAthlete.getSinclairForDelta() <= 0) {
                    iterWomen.remove();
                }
            }
            Athlete topWoman = (sortedWomen.size() > 0 ? sortedWomen.get(0) : null);
            topWomanSinclair = (topWoman != null ? topWoman.getSinclairForDelta() : 999.0D);

            updateBottom(getModel());
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
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            LiftingOrderModel model = getModel();
            if (a != null) {
                model.setFullName(getTranslation("Scoreboard.TopSinclair"));
                updateBottom(model);
            }
        });
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
     * AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        setTranslationMap();
        for (FieldOfPlay fop : OwlcmsFactory.getFOPs()) {
            // we listen on all the uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        }
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

    private JsonValue getAthletesJson(List<Athlete> list2) {
        JsonArray jath = Json.createArray();
        int athx = 0;
        for (Athlete a : list2) {
            JsonObject ja = Json.createObject();
            Gender curGender = a.getGender();

            int needed;
            if (curGender == Gender.F) {
                needed = (int) Math
                        .round(Math.ceil((topWomanSinclair - a.getSinclairForDelta()) / a.getSinclairFactor()));
            } else {
                needed = (int) Math
                        .round(Math.ceil((topManSinclair - a.getSinclairForDelta()) / a.getSinclairFactor()));
            }
            getAthleteJson(a, ja, curGender, needed);
            jath.set(athx, ja);
            athx++;
        }
        return jath;
    }

    public void getAthleteJson(Athlete a, JsonObject ja, Gender g, int needed) {
        String category;
        if (Competition.getCurrent().isMasters()) {
            category = a.getShortCategory();
        } else {
            category = a.getCategory() != null ? a.getCategory().getName() : "";
        }
        ja.put("fullName", a.getFullName());
        ja.put("teamName", a.getTeam());
        ja.put("yearOfBirth", a.getYearOfBirth());
        Integer startNumber = a.getStartNumber();
        ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
        ja.put("mastersAgeGroup", a.getMastersAgeGroup());
        ja.put("category", category);
        getAttemptsJson(a);
        ja.put("sattempts", sattempts);
        ja.put("cattempts", cattempts);
        ja.put("total", formatInt(a.getTotal()));
        ja.put("sinclair", a.getSinclair());
        ja.put("needed", needed);
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

//    private String formatAttempt(Integer attemptNo) {
//        return Translator.translate("AttemptBoard_attempt_number", (attemptNo % 3) + 1);
//    }

    private String formatInt(Integer total) {
        if (total == -1)
            return "inv.";// invited lifter, not eligible.
        return (total == null || total == 0) ? "-" : (total < 0 ? "(" + Math.abs(total) + ")" : total.toString());
    }

    private String formatKg(String total) {
        return (total == null || total.trim().isEmpty()) ? "-"
                : (total.startsWith("-") ? "(" + total.substring(1) + ")" : total);
    }

    private Object getOrigin() {
        return this;
    }

    private void updateBottom(LiftingOrderModel model) {
        this.getElement().setPropertyJson("sortedMen", getAthletesJson(sortedMen));
        this.getElement().setPropertyJson("sortedWomen", getAthletesJson(sortedWomen));
    }
}
