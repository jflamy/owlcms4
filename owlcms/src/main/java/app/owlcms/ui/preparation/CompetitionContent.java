/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import java.util.Locale;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.CrudLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;

import app.owlcms.data.competition.Competition;
import app.owlcms.i18n.Translator;
import app.owlcms.ui.crudui.OwlcmsComboBoxProvider;
import app.owlcms.ui.shared.OwlcmsContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class PreparationNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/competition", layout = CompetitionLayout.class)
public class CompetitionContent extends Composite<VerticalLayout> implements CrudLayout, OwlcmsContent {

    Logger logger = (Logger) LoggerFactory.getLogger(CompetitionContent.class);
    private OwlcmsRouterLayout routerLayout;

    /**
     * Instantiates a new preparation navigation content.
     */
    public CompetitionContent() {
        initLoggers();
        CompetitionEditingFormFactory factory = createFormFactory();
        Component form = factory.buildNewForm(CrudOperation.UPDATE, Competition.getCurrent(), false, null, event -> {
        });
        fillH(form, getContent());
    }

    @Override
    public void addFilterComponent(Component component) {
    }

    @Override
    public void addToolbarComponent(Component component) {
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("Preparation_Competition");
    }

    @Override
    public OwlcmsRouterLayout getRouterLayout() {
        return routerLayout;
    }

    @Override
    public void hideForm() {
    }

    public void initLoggers() {
        logger.setLevel(Level.INFO);
    }

    /**
     * @see org.vaadin.crudui.layout.CrudLayout#setMainComponent(com.vaadin.flow.component.Component)
     */
    @Override
    public void setMainComponent(Component component) {
        getContent().removeAll();
        getContent().add(component);
    }

    @Override
    public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
        this.routerLayout = routerLayout;
    }

    @Override
    public void showDialog(String caption, Component form) {
    }

    @Override
    public void showForm(CrudOperation operation, Component form, String caption) {
        getContent().removeAll();
        getContent().add(form);
    }

    /**
     * Define the form used to edit a given athlete.
     *
     * @return the form factory that will create the actual form on demand
     */
    protected CompetitionEditingFormFactory createFormFactory() {
        CompetitionEditingFormFactory competitionEditingFormFactory = new CompetitionEditingFormFactory(
                Competition.class);
        createFormLayout(competitionEditingFormFactory);
        return competitionEditingFormFactory;
    }

    /**
     * The content and ordering of the editing form
     *
     * @param crudFormFactory the factory that will create the form using this information
     */
    private void createFormLayout(DefaultCrudFormFactory<Competition> crudFormFactory) {
        crudFormFactory.setVisibleProperties("competitionName", "competitionDate", "competitionOrganizer",
                "competitionSite", "competitionCity", "federation", "federationAddress", "federationEMail",
                "federationWebSite", "defaultLocale", "enforce20kgRule", "masters", "useBirthYear");
        crudFormFactory.setFieldCaptions(Translator.translate("Competition.competitionName"),
                Translator.translate("Competition.competitionDate"),
                Translator.translate("Competition.competitionOrganizer"),
                Translator.translate("Competition.competitionSite"),
                Translator.translate("Competition.competitionCity"), Translator.translate("Competition.federation"),
                Translator.translate("Competition.federationAddress"),
                Translator.translate("Competition.federationEMail"),
                Translator.translate("Competition.federationWebSite"),
                Translator.translate("Competition.defaultLocale"),
                Translator.translate("Competition.enforce20kgRule"), Translator.translate("Competition.masters"),
                Translator.translate("Competition.useBirthYear"));
        ItemLabelGenerator<Locale> nameGenerator = (locale) -> locale.getDisplayName(locale);
        crudFormFactory.setFieldProvider("defaultLocale", new OwlcmsComboBoxProvider<>(getTranslation("Locale"),
                Translator.getAllAvailableLocales(), new TextRenderer<>(nameGenerator), nameGenerator));
        crudFormFactory.setFieldType("competitionDate", DatePicker.class);
    }
}
