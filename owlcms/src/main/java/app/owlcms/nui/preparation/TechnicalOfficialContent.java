/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Collection;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.data.technicalofficial.TechnicalOfficial;
import app.owlcms.data.technicalofficial.TechnicalOfficialRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class TechnicalOfficialContent.
 *
 * Defines the toolbar and the table for editing data on technical officials.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/officials", layout = OwlcmsLayout.class)
public class TechnicalOfficialContent extends BaseContent implements CrudListener<TechnicalOfficial>, OwlcmsContent {

	final static Logger logger = (Logger) LoggerFactory.getLogger(TechnicalOfficialContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	private OwlcmsCrudFormFactory<TechnicalOfficial> editingFormFactory;
	private OwlcmsLayout routerLayout;

	/**
	 * Instantiates the TechnicalOfficial crudGrid.
	 */
	public TechnicalOfficialContent() {
		OwlcmsCrudFormFactory<TechnicalOfficial> crudFormFactory = createFormFactory();
		GridCrud<TechnicalOfficial> crud = createGrid(crudFormFactory);
		// defineFilters(crudGrid);
		fillHW(crud, this);
	}

	@Override
	public TechnicalOfficial add(TechnicalOfficial domainObjectToAdd) {
		return this.editingFormFactory.add(domainObjectToAdd);
	}

	@Override
	public FlexLayout createMenuArea() {
		return new FlexLayout();
	}

	@Override
	public void delete(TechnicalOfficial domainObjectToDelete) {
		this.editingFormFactory.delete(domainObjectToDelete);
	}

	/**
	 * The refresh button on the toolbar
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<TechnicalOfficial> findAll() {
		return TechnicalOfficialRepository.findAll();
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("TechnicalOfficials");
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return Translator.translate("TechnicalOfficials");
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return this.routerLayout;
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}

	@Override
	public TechnicalOfficial update(TechnicalOfficial domainObjectToUpdate) {
		return this.editingFormFactory.update(domainObjectToUpdate);
	}

	/**
	 * The content and ordering of the editing form.
	 *
	 * @param crudFormFactory the factory that will create the form using this information
	 */
	protected void createFormLayout(OwlcmsCrudFormFactory<TechnicalOfficial> crudFormFactory) {
		((TechnicalOfficialEditingFormFactory)crudFormFactory).technicalOfficialLayout();
	}

	/**
	 * The columns of the crudGrid
	 *
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	protected GridCrud<TechnicalOfficial> createGrid(OwlcmsCrudFormFactory<TechnicalOfficial> crudFormFactory) {
		Grid<TechnicalOfficial> grid = new Grid<>(TechnicalOfficial.class, false);
		grid.getThemeNames().add("row-stripes");
		grid.addColumn(TechnicalOfficial::getFullName).setHeader(Translator.translate("Name"));
		grid.addColumn(TechnicalOfficial::getLevel).setHeader(Translator.translate("TechnicalOfficial.Level"));
		grid.addColumn(TechnicalOfficial::getFederationId).setHeader(Translator.translate("TechnicalOfficial.FederationId"));
		grid.addColumn(TechnicalOfficial::getFederation).setHeader(Translator.translate("TechnicalOfficial.Federation"));
		grid.addColumn(TechnicalOfficial::getIwfId).setHeader(Translator.translate("TechnicalOfficial.IWFId"));

		GridCrud<TechnicalOfficial> crud = new OwlcmsCrudGrid<>(TechnicalOfficial.class, new OwlcmsGridLayout(TechnicalOfficial.class),
		        crudFormFactory, grid);
		crud.setCrudListener(this);
		crud.setClickRowToUpdate(true);
		return crud;
	}

	/**
	 * Define the form used to edit a given TechnicalOfficial.
	 *
	 * @return the form factory that will create the actual form on demand
	 */
	private OwlcmsCrudFormFactory<TechnicalOfficial> createFormFactory() {
		this.editingFormFactory =  new TechnicalOfficialEditingFormFactory(TechnicalOfficial.class);
		return this.editingFormFactory;
	}

	// private <T extends Component & HasUrlParameter<String>> String getWindowOpenerFromClass(Class<T> targetClass,
	//         String parameter) {
	// 	return "window.open('" + URLUtils.getUrlFromTargetClass(targetClass) + "?fop=" + parameter
	// 	        + "','" + targetClass.getSimpleName() + "')";
	// }

	// private <T extends Component & HasUrlParameter<String>> Button openInNewTab(Class<T> targetClass,
	//         String label, String parameter) {
	// 	Button button = new Button(label);
	// 	button.getElement().setAttribute("onClick", getWindowOpenerFromClass(targetClass, parameter));
	// 	return button;
	// }
}
