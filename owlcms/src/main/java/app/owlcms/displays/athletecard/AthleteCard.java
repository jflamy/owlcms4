/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.athletecard;

import java.util.Enumeration;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;

import app.owlcms.apputils.queryparameters.FOPParameters;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.shared.RequireLogin;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Attempt board.
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("athlete-card-template")
@JsModule("./components/AthleteCard.js")
@CssImport(value = "./styles/shared-styles.css")
@Route("weighin/AthleteCard")

public class AthleteCard extends PolymerTemplate<TemplateModel>
        implements FOPParameters, SafeEventBusRegistration, HasDynamicTitle, RequireLogin {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteCard.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    private Athlete athlete;
    private Location location;
    private UI locationUI;

    /**
     * Instantiates a new attempt board.
     */
    public AthleteCard() {
        OwlcmsFactory.waitDBInitialized();
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
        return getTranslation("AthleteCard");
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
     * @see app.owlcms.apputils.queryparameters.FOPParameters#setParameter(com.vaadin.flow.router.BeforeEvent,
     *      java.lang.String)
     */
    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        long id = Long.parseLong(parameter);
        this.athlete = AthleteRepository.findById(id);
    }

    public int zeroIfInvalid(String v) {
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        init();

        getElement().setProperty("fullName",athlete.getFullName());
        getElement().setProperty("team",athlete.getTeam());
        getElement().setProperty("bodyWeight",String.format("%.2f", athlete.getBodyWeight()));
        AgeGroup ageGroup = athlete.getAgeGroup();
        getElement().setProperty("ageGroup",ageGroup != null ? ageGroup.getName() : "");
        getElement().setProperty("ageDivision",ageGroup != null ? getTranslation("Division." + ageGroup.getAgeDivision().name()) : "");
        Integer yearOfBirth = athlete.getYearOfBirth();
        if (yearOfBirth != null && yearOfBirth > 1900) {
            getElement().setProperty("birth",yearOfBirth.toString());
        } else {
            getElement().setProperty("birth","");
        }
        Integer lotNumber = athlete.getLotNumber();
        if (lotNumber != null && lotNumber > 0) {
            getElement().setProperty("lotNumber",lotNumber.toString());
        } else {
            getElement().setProperty("lotNumber","");
        }
        Integer startNumber = athlete.getStartNumber();
        if (startNumber != null && startNumber > 0) {
            getElement().setProperty("startNumber",startNumber.toString());
        } else {
            getElement().setProperty("startNumber","");
        }
        Group group = athlete.getGroup();
        if (group != null && group != null) {
            getElement().setProperty("group",group.getName());
        } else {
            getElement().setProperty("group","");
        }
        Category category = athlete.getCategory();
        if (category != null) {
            getElement().setProperty("category",category.getName());
        } else {
            getElement().setProperty("category","");
        }
        String snatch1Declaration = athlete.getSnatch1Declaration();
        if (snatch1Declaration != null && zeroIfInvalid(snatch1Declaration) > 0) {
            getElement().setProperty("snatch1Declaration",snatch1Declaration);
        } else {
            getElement().setProperty("snatch1Declaration","");
        }
        String cleanJerk1Declaration = athlete.getCleanJerk1Declaration();
        if (cleanJerk1Declaration != null && zeroIfInvalid(cleanJerk1Declaration) > 0) {
            getElement().setProperty("cleanJerk1Declaration",cleanJerk1Declaration);
        } else {
            getElement().setProperty("cleanJerk1Declaration","");
        }
        Integer entryTotal = athlete.getEntryTotal();
        if (entryTotal != null && entryTotal > 0) {
            getElement().setProperty("entryTotal",entryTotal.toString());
        } else {
            getElement().setProperty("entryTotal","");
        }
    }

    protected void setTranslationMap() {
        JsonObject translations = Json.createObject();
        Enumeration<String> keys = Translator.getKeys();
        while (keys.hasMoreElements()) {
            String curKey = keys.nextElement();
            if (curKey.startsWith("Card.")) {
                translations.put(curKey.replace("Card.", ""), Translator.translate(curKey));
            }
        }
        this.getElement().setPropertyJson("t", translations);
    }

    private void init() {
        getElement().executeJs("document.querySelector('html').setAttribute('theme', 'light');");
        setTranslationMap();

        Button button = new Button(getTranslation("Print"));
        button.setThemeName("primary success");
        button.getElement().setAttribute("onClick", "window.print()");
        HorizontalLayout banner = new HorizontalLayout(button);
        banner.setJustifyContentMode(JustifyContentMode.END);
        banner.setPadding(true);
        banner.setClassName("printing");
        getElement().getParent().appendChild(banner.getElement());
    }

}
