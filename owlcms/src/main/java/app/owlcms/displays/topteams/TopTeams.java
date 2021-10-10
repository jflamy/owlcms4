/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.topteams;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.PageConfigurator;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.team.Team;
import app.owlcms.data.team.TeamTreeData;
import app.owlcms.data.team.TeamTreeItem;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.RequireLogin;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.queryparameters.DisplayParameters;
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
public class TopTeams extends PolymerTemplate<TopTeams.TopTeamsModel> implements DisplayParameters,
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
    private boolean darkMode;
    private Location location;
    private UI locationUI;
    private List<TeamTreeItem> mensTeams;
    private List<TeamTreeItem> womensTeams;
    private DecimalFormat floatFormat;
    private Dialog dialog;
    private boolean initializationNeeded;

    /**
     * Instantiates a new results board.
     */
    public TopTeams() {
        OwlcmsFactory.waitDBInitialized();
    }

    /**
     * @see app.owlcms.utils.queryparameters.DisplayParameters#addDialogContent(com.vaadin.flow.component.Component,
     *      com.vaadin.flow.component.orderedlayout.VerticalLayout)
     */
    @Override
    public void addDialogContent(Component target, VerticalLayout vl) {
        DisplayOptions.addLightingEntries(vl, target, this);
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

        TeamTreeData teamTreeData = new TeamTreeData("SR", AgeDivision.IWF, (Gender)null);
        Map<Gender, List<TeamTreeItem>> teamsByGender = teamTreeData.getTeamItemsByGender();

        mensTeams = teamsByGender.get(Gender.M);
        if (mensTeams != null) {
            mensTeams.sort(TeamTreeItem.pointComparator);
        }
        mensTeams = topN(mensTeams);

        womensTeams = teamsByGender.get(Gender.F);
        if (womensTeams != null) {
            womensTeams.sort(TeamTreeItem.pointComparator);
        }
        womensTeams = topN(womensTeams);

        updateBottom(getModel());
    }

    /**
     * return dialog, but only on first call.
     *
     * @see app.owlcms.utils.queryparameters.DisplayParameters#getDialog()
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

    /**
     * @see app.owlcms.utils.queryparameters.DisplayParameters#isShowInitialDialog()
     */
    @Override
    public boolean isShowInitialDialog() {
        return this.initializationNeeded;
    }

    @Override
    public boolean isSilenced() {
        return true;
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
     * @see app.owlcms.utils.queryparameters.DisplayParameters#setShowInitialDialog(boolean)
     */
    @Override
    public void setShowInitialDialog(boolean b) {
        this.initializationNeeded = true;
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

    /**
     * @see app.owlcms.utils.queryparameters.DisplayParameters#setSilenced(boolean)
     */
    @Override
    public void setSilenced(boolean silent) {
        // no-op, silenced by definition
    }

    @Subscribe
    public void slaveGroupDone(UIEvent.GroupDone e) {
        uiLog(e);
        Competition competition = Competition.getCurrent();

        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            doUpdate(competition);
        });
    }

    @Subscribe
    public void slaveStartLifting(UIEvent.StartLifting e) {
        uiLog(e);
        Competition competition = Competition.getCurrent();
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            doUpdate(competition);
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
        switchLightingMode(this, isDarkMode(), true);
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

    private String formatDouble(double d) {
        if (floatFormat == null) {
            floatFormat = new DecimalFormat();
            floatFormat.setMinimumIntegerDigits(1);
            floatFormat.setMaximumFractionDigits(0);
            floatFormat.setGroupingUsed(false);
        }
        return floatFormat.format(d);
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

    @SuppressWarnings("unused")
    private Object getOrigin() {
        return this;
    }

    private void getTeamJson(Team t, JsonObject ja, Gender g) {
        ja.put("team", t.getName());
        ja.put("counted", formatInt(t.getCounted()));
        ja.put("size", formatInt((int) t.getSize()));
        ja.put("score", formatDouble(t.getScore()));
        ja.put("points", formatInt(t.getPoints()));
    }

    private JsonValue getTeamsJson(List<TeamTreeItem> teamItems, boolean overrideTeamWidth) {
        JsonArray jath = Json.createArray();
        int athx = 0;
        List<Team> list3 = teamItems != null
                ? teamItems.stream().map(TeamTreeItem::getTeam).collect(Collectors.toList())
                : Collections.emptyList();
        if (overrideTeamWidth) {
            // when we are called for the second time, and there was a wide team in the top section.
            // we use the wide team setting for the remaining sections.
            setWide(false);
        }

        for (Team t : list3) {
            JsonObject ja = Json.createObject();
            Gender curGender = t.getGender();

            getTeamJson(t, ja, curGender);
            String teamName = t.getName();
            if (teamName != null && teamName.length() > Competition.SHORT_TEAM_LENGTH) {
                setWide(true);
            }
            jath.set(athx, ja);
            athx++;
        }
        return jath;
    }

    private void setWide(boolean b) {
        getModel().setWideTeamNames(b);
    }

    private List<TeamTreeItem> topN(List<TeamTreeItem> list) {
        if (list == null) return new ArrayList<TeamTreeItem>();
        int size = list.size();
        if (size > 0) {
            int min = Math.min(size, TOP_N);
            list = list.subList(0, min);
        }
        return list;
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
