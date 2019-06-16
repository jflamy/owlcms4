/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import java.util.Arrays;
import java.util.Locale;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.impl.field.provider.ComboBoxProvider;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.CrudLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;

import app.owlcms.components.fields.LocalDateField;
import app.owlcms.data.competition.Competition;
import app.owlcms.i18n.TranslationProvider;
import app.owlcms.ui.shared.OwlcmsContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class PreparationNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/competition", layout = CompetitionLayout.class)
public class CompetitionContent extends Composite<VerticalLayout>
		implements CrudLayout, OwlcmsContent {

	Logger logger = (Logger) LoggerFactory.getLogger(CompetitionContent.class);
	private OwlcmsRouterLayout routerLayout;
	public void initLoggers() {
		logger.setLevel(Level.INFO);
	}

	/**
	 * Instantiates a new preparation navigation content.
	 */
	public CompetitionContent() {
		initLoggers();
		CompetitionEditingFormFactory factory = createFormFactory();
		Component form = factory.buildNewForm(CrudOperation.UPDATE, Competition.getCurrent(), false, null, event -> {});	
		fillH(form, getContent());	
	}


	
	/**
	 * Define the form used to edit a given athlete.
	 * 
	 * @return the form factory that will create the actual form on demand
	 */
	protected CompetitionEditingFormFactory  createFormFactory() {
		CompetitionEditingFormFactory competitionEditingFormFactory = new CompetitionEditingFormFactory(Competition.class);
		createFormLayout(competitionEditingFormFactory);
		return competitionEditingFormFactory;
	}

	/**
	 * The content and ordering of the editing form
	 * 
	 * @param crudFormFactory the factory that will create the form using this information
	 */
	private void createFormLayout(DefaultCrudFormFactory<Competition> crudFormFactory) {
		crudFormFactory.setVisibleProperties(
			"competitionName", //$NON-NLS-1$
			"competitionDate", //$NON-NLS-1$
			"competitionOrganizer", //$NON-NLS-1$
			"competitionSite", //$NON-NLS-1$
			"competitionCity", //$NON-NLS-1$
			"federation", //$NON-NLS-1$
			"federationAddress", //$NON-NLS-1$
			"federationEMail", //$NON-NLS-1$
			"federationWebSite", //$NON-NLS-1$
			"defaultLocale", //$NON-NLS-1$
//			"protocolFileName",
//			"finalPackageTemplateFileName",
			"enforce20kgRule", //$NON-NLS-1$
			"masters", //$NON-NLS-1$
			"useBirthYear" //$NON-NLS-1$
			);
		ItemLabelGenerator<Locale> nameGenerator = (locale) -> locale.getDisplayName(Locale.US);
		crudFormFactory.setFieldProvider("defaultLocale", //$NON-NLS-1$
            new ComboBoxProvider<Locale>(TranslationProvider.getString("CompetitionContent.1"), Arrays.asList(Locale.ENGLISH,Locale.FRENCH), new TextRenderer<>(nameGenerator),  //$NON-NLS-1$
            		nameGenerator));
		crudFormFactory.setFieldType("competitionDate", LocalDateField.class); //$NON-NLS-1$
	}
	
	@Override
	public void setMainComponent(Component component) {
		getContent().removeAll();
		getContent().add(component);
	}

	@Override
	public void addFilterComponent(Component component) {
	}

	@Override
	public void addToolbarComponent(Component component) {
	}

	@Override
	public void showForm(CrudOperation operation, Component form, String caption) {
	    getContent().removeAll();
	    getContent().add(form);
	}

	@Override
	public void hideForm() {
	}

	@Override
	public void showDialog(String caption, Component form) {
	}

	@Override
	public OwlcmsRouterLayout getRouterLayout() {
		return routerLayout;
	}

	@Override
	public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
		this.routerLayout = routerLayout;
	}
	
	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return TranslationProvider.getString("CompetitionContent.0"); //$NON-NLS-1$
	}
}
