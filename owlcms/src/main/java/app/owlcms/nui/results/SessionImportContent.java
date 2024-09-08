/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.results;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.components.fields.LocalDateTimeField;
import app.owlcms.data.export.AthleteSessionDataReader;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.servlet.StopProcessingException;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class GroupContent.
 *
 * Defines the toolbar and the table for editing data on sessions.
 */
@SuppressWarnings("serial")
@Route(value = "results/sessionImport", layout = OwlcmsLayout.class)
public class SessionImportContent extends BaseContent implements CrudListener<Group>, OwlcmsContent {

	final static Logger logger = (Logger) LoggerFactory.getLogger(SessionImportContent.class);

	static {
		logger.setLevel(Level.INFO);
	}
	private SessionSelectionGrid crud;
	private OwlcmsLayout routerLayout;
	private FlexLayout topBar;

	/**
	 * Instantiates the Group crudGrid.
	 */
	public SessionImportContent() {
		OwlcmsFactory.waitDBInitialized();
		GridCrud<Group> crud = createGrid(null);
		fillHW(crud, this);
	}

	@Override
	public Group add(Group domainObjectToAdd) {
		return null;
	}

	public void closeDialog() {
	}

	@Override
	public FlexLayout createMenuArea() {
		this.topBar = new FlexLayout();
		this.topBar.getStyle().set("flex", "100 1");
		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);
		
		Button uploadCustom = new Button(Translator.translate("ImportSessions.UploadDatabaseExport"),
		        new Icon(VaadinIcon.UPLOAD_ALT),
		        buttonClickEvent -> {
		        	SessionResultsUploadDialog sessionUploadDialog = new SessionResultsUploadDialog();
		        	sessionUploadDialog.setCallback((mb) -> {
						try {
							AthleteSessionDataReader.importAthletes(mb.getInputStream(),getSortedSelection());
						} catch (IOException e) {
							LoggerUtils.logError(logger, e);
						}
					});
		        	sessionUploadDialog.open();
		        });
		topBar.add(uploadCustom);

		return this.topBar;
	}

	@Override
	public void delete(Group domainObjectToDelete) {
	}


	@Override
	public Collection<Group> findAll() {
		return GroupRepository.findAll().stream().sorted(Group::compareToWeighIn).collect(Collectors.toList());
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
		return Translator.translate("ImportSessions.PageTitle");
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
	public Group update(Group domainObjectToUpdate) {
		return null;
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		this.crud.getAddButton().getStyle().set("display", "none");
	}

	@SuppressWarnings("unused")
	private void checkNoSelection(List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		if (selectedItems == null || selectedItems.size() == 0) {
			Exception e = new Exception("NoAthletes");
			errorProcessor.accept(e, e.getMessage());
			throw new StopProcessingException(e.getMessage(), e);
		}
	}

	/**
	 * The columns of the crudGrid
	 *
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	private GridCrud<Group> createGrid(OwlcmsCrudFormFactory<Group> crudFormFactory) {
		Grid<Group> grid = new Grid<>(Group.class, false);
		this.crud = new SessionSelectionGrid(Group.class, new OwlcmsGridLayout(Group.class), crudFormFactory, grid);
		grid.getThemeNames().add("row-stripes");
		grid.addColumn(Group::getName).setHeader(Translator.translate("Name")).setComparator(Group::compareTo).setAutoWidth(true);
		grid.addColumn(Group::getDescription).setHeader(Translator.translate("Group.Description")).setAutoWidth(true);
		grid.addColumn(Group::size).setHeader(Translator.translate("GroupSize")).setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn(LocalDateTimeField.getRenderer(Group::getWeighInTime, this.getLocale()))
		        .setHeader(Translator.translate("WeighInTime")).setComparator(Group::compareToWeighIn);
		grid.addColumn(LocalDateTimeField.getRenderer(Group::getCompetitionTime, this.getLocale()))
		        .setHeader(Translator.translate("StartTime"));
		grid.addColumn(Group::getPlatform).setHeader(Translator.translate("Platform")).setTextAlign(ColumnTextAlign.CENTER);
		
		for (Column<Group> c : grid.getColumns()) {
			c.setResizable(true);
		}

		this.crud.setCrudListener(this);
		this.crud.setClickRowToUpdate(true);
		grid.setSelectionMode(SelectionMode.MULTI);
		return this.crud;
	}

	private List<Group> getSortedSelection() {
		return this.crud.getSelectedItems().stream().sorted(Group.groupWeighinTimeComparator).toList();
	}
}
