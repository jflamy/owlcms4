/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.displays.athletecard;

import java.util.Enumeration;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.competition.Competition;
import app.owlcms.i18n.Translator;
import app.owlcms.ui.shared.QueryParameterReader;
import app.owlcms.ui.shared.RequireLogin;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Attempt board.
 */
@SuppressWarnings("serial")
@Tag("athlete-card-template")
@HtmlImport("frontend://components/AthleteCard.html")
@HtmlImport("frontend://styles/shared-styles.html")
@Route("weighin/AthleteCard")
@Theme(value = Lumo.class)
@Push
public class AthleteCard extends PolymerTemplate<AthleteCard.AthleteCardModel>
implements QueryParameterReader, SafeEventBusRegistration, HasDynamicTitle, RequireLogin {

    /**
     * AthleteCardModel
     *
     * Vaadin Flow propagates these variables to the corresponding Polymer template
     * JavaScript properties. When the JS properties are changed, a
     * "propname-changed" event is triggered.
     *
     * {@link Element.#addPropertyChangeListener(String, String,
     * com.vaadin.flow.dom.PropertyChangeListener)}
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

        void setAgeGroup(Integer ageGroup);

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

    /**
     * Instantiates a new attempt board.
     */
    public AthleteCard() {
    }

    @Override
    public String getPageTitle() {
        return getTranslation("AthleteCard");
    }

    /**
     * @see app.owlcms.ui.shared.QueryParameterReader#setParameter(com.vaadin.flow.router.BeforeEvent,
     *      java.lang.String)
     */
    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        this.athlete = AthleteRepository.findById(Long.parseLong(parameter));
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
     * AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        init();

        AthleteCardModel model = getModel();
        model.setFullName(athlete.getFullName());
        model.setTeam(athlete.getTeam());
        model.setBodyWeight(String.format("%.2f",athlete.getBodyWeight()));
        AgeDivision ageDivision = athlete.getAgeDivision();
        if (ageDivision != null && ageDivision != AgeDivision.DEFAULT) {
            model.setAgeDivision(getTranslation("AgeDivision."+ageDivision.name()));
        } else {
            model.setAgeDivision(null);
        }
        if (Competition.getCurrent().isMasters()) {
            model.setAgeGroup(athlete.getAgeGroup());
        } else {
            model.setAgeGroup(null);
        }
        if (athlete.getYearOfBirth() > 1900) {
            model.setBirth(athlete.getYearOfBirth().toString());
        } else {
            model.setBirth("");
        }
        if (athlete.getLotNumber() > 0) {
            model.setLotNumber(athlete.getLotNumber().toString());
        } else {
            model.setLotNumber("");
        }
        if (athlete.getStartNumber() > 0) {
            model.setStartNumber(athlete.getStartNumber().toString());
        } else {
            model.setStartNumber("");
        }
        if (athlete.getGroup() != null) {
            model.setGroup(athlete.getGroup().getName());
        } else {
            model.setGroup("");
        }
        if (athlete.getCategory() != null) {
            model.setCategory(athlete.getCategory().getName());
        } else {
            model.setCategory("");
        }
        if (athlete.getSnatch1Declaration() != null) {
            model.setSnatch1Declaration(athlete.getSnatch1Declaration());
        } else {
            model.setSnatch1Declaration("");
        }
        if (athlete.getCleanJerk1Declaration() != null) {
            model.setCleanJerk1Declaration(athlete.getCleanJerk1Declaration());
        } else {
            model.setCleanJerk1Declaration("");
        }
        if (athlete.getQualifyingTotal() != null) {
            model.setEntryTotal(athlete.getQualifyingTotal().toString());
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
