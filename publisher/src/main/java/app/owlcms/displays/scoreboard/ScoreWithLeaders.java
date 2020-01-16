/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.displays.scoreboard;

import org.slf4j.LoggerFactory;

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
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.ui.parameters.DarkModeParameters;
import app.owlcms.ui.parameters.QueryParameterReader;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import publish.ResultsListenerServlet;
import publish.UpdateEvent;

/**
 * Class Scoreboard
 *
 * Show athlete 6-attempt results
 *
 */
@Tag("scoreleader-template")
@JsModule("./components/ScoreWithLeaders.js")
@Route("displays/scoreleader")
@Theme(value = Lumo.class, variant = Lumo.DARK)
@Push
public class ScoreWithLeaders extends PolymerTemplate<ScoreWithLeaders.ScoreboardModel>
        implements QueryParameterReader, DarkModeParameters, HasDynamicTitle {

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

        String getFullName();

        Integer getStartNumber();

        String getTeamName();

        Integer getWeight();

        Boolean isHidden();

        Boolean isWideTeamNames();

        void setAttempt(String formattedAttempt);

        void setCategoryName(String categoryName);

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

//    @Id("timer")
//    private AthleteTimerElement timer; // Flow creates it
//
//    @Id("breakTimer")
//    private BreakTimerElement breakTimer; // Flow creates it
//
//    @Id("decisions")
//    private DecisionElement decisions; // Flow creates it

    private boolean darkMode;
    private ContextMenu contextMenu;
    private Location location;
    private UI locationUI;

    /**
     * Instantiates a new results board.
     */
    public ScoreWithLeaders() {
        ResultsListenerServlet.getEventBus().register(this);
        setDarkMode(true);
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
    public void slaveGlobalRankingUpdated(UpdateEvent e) {
        UI.getCurrent().access(() -> {
            this.getElement().setProperty("leaders", e.getLeaders());
            this.getElement().setProperty("athletes", e.getAthletes());
            this.getElement().setProperty("t", e.getTranslationMap());
            getModel().setCategoryName(e.getCategoryName());
            getModel().setWideTeamNames(e.getWideTeamNames());
            getModel().setLiftsDone(e.getLiftsDone());
        });
    }

    protected void doEmpty() {
        this.getModel().setHidden(true);
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
    }


}