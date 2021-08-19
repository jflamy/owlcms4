/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
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
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.ui.shared.RequireLogin;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.utils.queryparameters.FOPParameters;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Attempt board.
 */
@SuppressWarnings("serial")
@Tag("athlete-card-template")
@JsModule("./components/AthleteCard.js")
@CssImport(value = "./styles/shared-styles.css")
@Route("weighin/AthleteCard")
@Theme(value = Lumo.class, variant = Lumo.LIGHT)
@Push
public class AthleteCard extends PolymerTemplate<AthleteCard.AthleteCardModel>
        implements FOPParameters, SafeEventBusRegistration, HasDynamicTitle, RequireLogin {

    /**
     * AthleteCardModel
     *
     * Vaadin Flow propagates these variables to the corresponding Polymer template JavaScript properties. When the JS
     * properties are changed, a "propname-changed" event is triggered.
     *
     * {@link Element.#addPropertyChangeListener(String, String, com.vaadin.flow.dom.PropertyChangeListener)}
     */
    public interface AthleteCardModel extends TemplateModel {

        String getAgeDivision();

        Integer getAgeGroup();

        String getBirth();

        String getBodyWeight();

        String getCategory();

        String getCleanJerk1Declaration();

        String getEntryTotal();

        String getFullName();

        String getGroup();

        String getLotNumber();

        String getSnatch1Declaration();

        String getStartNumber();

        String getTeam();

        void setAgeDivision(String division);

        void setAgeGroup(String string);

        void setBirth(String birth);

        void setBodyWeight(String format);

        void setCategory(String name);

        void setCleanJerk1Declaration(String cleanJerk1Declaration);

        void setEntryTotal(String entryTotal);

        void setFullName(String fullName);

        void setGroup(String group);

        void setLotNumber(String string);

        void setSnatch1Declaration(String snatch1Declaration);

        void setStartNumber(String string);

        void setTeam(String team);

    }

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
     * @see app.owlcms.utils.queryparameters.FOPParameters#setParameter(com.vaadin.flow.router.BeforeEvent,
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

        AthleteCardModel model = getModel();
        model.setFullName(athlete.getFullName());
        model.setTeam(athlete.getTeam());
        model.setBodyWeight(String.format("%.2f", athlete.getBodyWeight()));
        AgeGroup ageGroup = athlete.getAgeGroup();
        model.setAgeGroup(ageGroup != null ? ageGroup.getName() : "");
        model.setAgeDivision(ageGroup != null ? getTranslation("Division." + ageGroup.getAgeDivision().name()) : "");
        Integer yearOfBirth = athlete.getYearOfBirth();
        if (yearOfBirth != null && yearOfBirth > 1900) {
            model.setBirth(yearOfBirth.toString());
        } else {
            model.setBirth("");
        }
        Integer lotNumber = athlete.getLotNumber();
        if (lotNumber != null && lotNumber > 0) {
            model.setLotNumber(lotNumber.toString());
        } else {
            model.setLotNumber("");
        }
        Integer startNumber = athlete.getStartNumber();
        if (startNumber != null && startNumber > 0) {
            model.setStartNumber(startNumber.toString());
        } else {
            model.setStartNumber("");
        }
        Group group = athlete.getGroup();
        if (group != null && group != null) {
            model.setGroup(group.getName());
        } else {
            model.setGroup("");
        }
        Category category = athlete.getCategory();
        if (category != null) {
            model.setCategory(category.getName());
        } else {
            model.setCategory("");
        }
        String snatch1Declaration = athlete.getSnatch1Declaration();
        if (snatch1Declaration != null && zeroIfInvalid(snatch1Declaration) > 0) {
            model.setSnatch1Declaration(snatch1Declaration);
        } else {
            model.setSnatch1Declaration("");
        }
        String cleanJerk1Declaration = athlete.getCleanJerk1Declaration();
        if (cleanJerk1Declaration != null && zeroIfInvalid(cleanJerk1Declaration) > 0) {
            model.setCleanJerk1Declaration(cleanJerk1Declaration);
        } else {
            model.setCleanJerk1Declaration("");
        }
        Integer entryTotal = athlete.getEntryTotal();
        if (entryTotal != null && entryTotal > 0) {
            model.setEntryTotal(entryTotal.toString());
        } else {
            model.setEntryTotal("");
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
