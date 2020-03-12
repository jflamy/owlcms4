/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.displays.topteams;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.PageConfigurator;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.team.Team;
import app.owlcms.data.team.TeamTreeItem;
import app.owlcms.displays.attemptboard.BreakDisplay;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.parameters.DarkModeParameters;
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
 * Class TopTeams
 *
 * Show athlete lifting order
 *
 */
@SuppressWarnings("serial")
@Tag("topteams-template")
@JsModule("./components/TopTeams.js")
@Route("displays/topteams")
@Theme(value = Lumo.class, variant = Lumo.DARK)
@Push
public class TopTeams extends PolymerTemplate<TopTeams.TopTeamsModel> implements DarkModeParameters,
        SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle, RequireLogin, PageConfigurator {

    /**
     * LiftingOrderModel
     *
     * Vaadin Flow propagates these variables to the corresponding Polymer template JavaScript properties. When the JS
     * properties are changed, a "propname-changed" event is triggered.
     * {@link Element.#addPropertyChangeListener(String, String, com.vaadin.flow.dom.PropertyChangeListener)}
     *
     */
    public interface TopTeamsModel extends TemplateModel {

        String getFullName();

        Boolean isHidden();

        Boolean isWideTeamNames();

        void setFullName(String lastName); // misnomer, is actually the title

        void setHidden(boolean b);

        void setWideTeamNames(boolean b);
    }

    final private static Logger logger = (Logger) LoggerFactory.getLogger(TopTeams.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
    private static final int TOP_N = 5;

    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    private EventBus uiEventBus;

    JsonArray sattempts;
    JsonArray cattempts;
    private List<Athlete> sortedMen;
    private List<Athlete> sortedWomen;
    private boolean darkMode;
    private ContextMenu contextMenu;
    private Location location;
    private UI locationUI;
    private List<TeamTreeItem> mensTeams;
    private List<TeamTreeItem> womensTeams;

    /**
     * Instantiates a new results board.
     */
    public TopTeams() {
    }

    @Override
    public void configurePage(InitialPageSettings settings) {
        settings.addMetaTag("mobile-web-app-capable", "yes");
        settings.addMetaTag("apple-mobile-web-app-capable", "yes");
        settings.addLink("shortcut icon", "frontend/images/owlcms.ico");
        settings.addFavIcon("icon", "frontend/images/logo.png", "96x96");
        settings.setViewport("width=device-width, minimum-scale=1, initial-scale=1, user-scalable=yes");
    }

    @Override
    public void doBreak() {
        OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            // just update the display
            doUpdate(fop.getCurAthlete(), null);
        }));
    }

    public void doUpdate(Competition competition) {
        this.getElement().callJsFunction("reset");

        // create copies because we want to change the list
        setSortedMen(competition.getGlobalTeamsRanking(Gender.M).stream().collect(Collectors.toList()));
        setSortedWomen(competition.getGlobalTeamsRanking(Gender.F).stream().collect(Collectors.toList()));

        Map<Gender, List<TeamTreeItem>> teamsByGender = new EnumMap<>(Gender.class);

        TeamTreeItem.buildTeamItemTree(competition, teamsByGender);

        mensTeams = teamsByGender.get(Gender.M);
        if (mensTeams != null) {
            mensTeams.sort(Team.menComparator);
        }
        mensTeams = mensTeams.subList(0, TOP_N);

        womensTeams = teamsByGender.get(Gender.F);
        if (womensTeams != null) {
            womensTeams.sort(Team.womenComparator);
        }
        womensTeams = womensTeams.subList(0, TOP_N);

        updateBottom(getModel());
    }

    private void getTeamJson(Team t, JsonObject ja, Gender g) {
        ja.put("team", t.name);
        ja.put("counted", formatInt(t.counted));
        ja.put("size", formatInt((int) t.size));
        ja.put("points", formatInt(g == Gender.F ? t.womenScore : t.menScore));
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
        return getTranslation("Scoreboard.TopTeams");
    }

    @Override
    public boolean isDarkMode() {
        return this.darkMode;
    }

    @Override
    public boolean isIgnoreFopFromURL() {
        return true;
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

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public void setLocationUI(UI locationUI) {
        this.locationUI = locationUI;
    }

    @Subscribe
    public void slaveGroupDone(UIEvent.GroupDone e) {
        uiLog(e);
        Competition competition = Competition.getCurrent();

        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            doUpdate(competition);
        });
    }

//    @Subscribe
//    public void slaveGlobalRankingUpdated(UIEvent.GlobalRankingUpdated e) {
//        uiLog(e);
//        Competition competition = Competition.getCurrent();
//
//        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
//            doUpdate(competition);
//        });
//    }

    @Subscribe
    public void slaveStartLifting(UIEvent.StartLifting e) {
        uiLog(e);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            getModel().setHidden(false);
            this.getElement().callJsFunction("reset");
        });
    }

    public void uiLog(UIEvent e) {
        if (e == null) {
            uiEventLogger.debug("### {} {}", this.getClass().getSimpleName(), LoggerUtils.whereFrom());
        } else {
            uiEventLogger.debug("### {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                    LoggerUtils.whereFrom());
        }
    }

    protected void doEmpty() {
        logger.trace("doEmpty");
        this.getModel().setHidden(true);
    }

    protected void doUpdate(Athlete a, UIEvent e) {
        logger.debug("doUpdate {} {}", a, a != null ? a.getAttemptsDone() : null);
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            TopTeamsModel model = getModel();
            if (a != null) {
                model.setFullName(getTranslation("Scoreboard.TopTeams"));
                updateBottom(model);
            }
        });
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        setDarkMode(this, isDarkMode(), false);
        setWide(false);
        setTranslationMap();
        for (FieldOfPlay fop : OwlcmsFactory.getFOPs()) {
            // we listen on all the uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        }
        Competition competition = Competition.getCurrent();
        doUpdate(competition);
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

    private JsonValue getTeamsJson(List<TeamTreeItem> teamItems, boolean overrideTeamWidth) {
        JsonArray jath = Json.createArray();
        int athx = 0;
        List<Team> list3 = teamItems != null ? Collections.unmodifiableList(teamItems) : Collections.emptyList();
        if (overrideTeamWidth) {
            // when we are called for the second time, and there was a wide team in the top section.
            // we use the wide team setting for the remaining sections.
            setWide(false);
        }

        for (Team t : list3) {
            JsonObject ja = Json.createObject();
            Gender curGender = t.gender;

            getTeamJson(t, ja, curGender);
            String teamName = t.name;
            if (teamName != null && teamName.length() > Competition.SHORT_TEAM_LENGTH) {
                setWide(true);
            }
            jath.set(athx, ja);
            athx++;
        }
        return jath;
    }

    @SuppressWarnings("unused")
    private Object getOrigin() {
        return this;
    }

    private List<Athlete> getSortedMen() {
        return this.sortedMen;
    }

    private List<Athlete> getSortedWomen() {
        return this.sortedWomen;
    }

    private void setSortedMen(List<Athlete> sortedMen) {
        this.sortedMen = sortedMen;
        logger.debug("sortedMen = {} -- {}", getSortedMen(), LoggerUtils.whereFrom());
    }

    private void setSortedWomen(List<Athlete> sortedWomen) {
        this.sortedWomen = sortedWomen;
        logger.debug("sortedWomen = {} -- {}", getSortedWomen(), LoggerUtils.whereFrom());
    }

    private void setWide(boolean b) {
        getModel().setWideTeamNames(b);
    }

    private void updateBottom(TopTeamsModel model) {
        getModel().setFullName(getTranslation("Scoreboard.TopTeams"));
        this.getElement().setProperty("topTeamsMen",
                mensTeams != null && mensTeams.size() > 0 ? getTranslation("Scoreboard.TopTeamsMen") : "");
        this.getElement().setPropertyJson("mensTeams", getTeamsJson(mensTeams, true));

        this.getElement().setProperty("topTeamsWomen",
                womensTeams != null && womensTeams.size() > 0 ? getTranslation("Scoreboard.TopTeamsWomen") : "");
        this.getElement().setPropertyJson("womensTeams", getTeamsJson(womensTeams, false));
    }

}
