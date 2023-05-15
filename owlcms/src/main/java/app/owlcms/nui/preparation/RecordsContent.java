/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.ArrayList;
import java.util.Collection;

import javax.naming.OperationNotSupportedException;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.layout.CrudLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.data.records.RecordConfig;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class PreparationNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/records", layout = OwlcmsLayout.class)
public class RecordsContent extends Composite<VerticalLayout>
        implements CrudLayout, OwlcmsContent, CrudListener<RecordConfig> {

	Logger logger = (Logger) LoggerFactory.getLogger(RecordsContent.class);
	private OwlcmsCrudFormFactory<RecordConfig> factory;
	private OwlcmsLayout routerLayout;

	/**
	 * Instantiates a new preparation navigation content.
	 */
	public RecordsContent() {
		initLoggers();
		factory = createFormFactory();
		RecordConfig current = RecordConfig.getCurrent();
		current.addMissing(RecordRepository.findAllRecordNames());
		Component form = factory.buildNewForm(CrudOperation.UPDATE, current, false, null, event -> {
		});
		fillH(form, getContent());
	}

	@Override
	public RecordConfig add(RecordConfig domainObjectToAdd) {
		// implemented by factory
		throw new RuntimeException(new OperationNotSupportedException());
	}

	@Override
	public void addFilterComponent(Component component) {
	}

	@Override
	public void addToolbarComponent(Component component) {
	}

	@Override
	public FlexLayout createMenuArea() {
		return new FlexLayout();
	}

	@Override
	public void delete(RecordConfig domainObjectToDelete) {
		// not used
		factory.delete(domainObjectToDelete);
	}

	@Override
	public Collection<RecordConfig> findAll() {
		ArrayList<RecordConfig> arrayList = new ArrayList<>();
		arrayList.add(RecordConfig.getCurrent());
		return arrayList;
	}

	@Override
	public String getMenuTitle() {
		return getPageTitle();
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation("Records.RecordsManagementTitle");
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return routerLayout;
	}

	@Override
	public void hideForm() {
	}

	public void initLoggers() {
		logger.setLevel(Level.INFO);
	}

	@Override
	public void setHeaderContent() {
		routerLayout.setMenuTitle(getPageTitle());
		routerLayout.showLocaleDropdown(true);
		routerLayout.setDrawerOpened(false);
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
	public void setPadding(boolean b) {
		// not needed
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
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

	@Override
	public RecordConfig update(RecordConfig domainObjectToUpdate) {
		// implemented by factory
		throw new RuntimeException(new OperationNotSupportedException());
	}

	/**
	 * Define the form used to edit a given athlete.
	 *
	 * @return the form factory that will create the actual form on demand
	 */
	protected OwlcmsCrudFormFactory<RecordConfig> createFormFactory() {
		OwlcmsCrudFormFactory<RecordConfig> factory = new RecordConfigEditingFormFactory(
		        RecordConfig.class);
		return factory;
	}

}
