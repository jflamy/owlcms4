/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.poi.ss.formula.functions.T;
import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.components.fields.LocalDateTimeField;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class CategoryContent.
 *
 * Defines the toolbar and the table for editing data on groups.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/groups", layout = OwlcmsLayout.class)
public class GroupContent extends BaseContent implements CrudListener<Group>, OwlcmsContent {

	final static Logger logger = (Logger) LoggerFactory.getLogger(GroupContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	private OwlcmsCrudFormFactory<Group> editingFormFactory;
	private OwlcmsLayout routerLayout;
	private FlexLayout topBar;
	private GroupGrid crud;

	/**
	 * Instantiates the Group crudGrid.
	 */
	public GroupContent() {
		this.editingFormFactory = new GroupEditingFormFactory(Group.class, this);
		GridCrud<Group> crud = createGrid(this.editingFormFactory);
		// defineFilters(crudGrid);
		fillHW(crud, this);
	}

	@Override
	public Group add(Group domainObjectToAdd) {
		return this.editingFormFactory.add(domainObjectToAdd);
	}

	public void closeDialog() {
	}

	@Override
	public FlexLayout createMenuArea() {
		this.topBar = new FlexLayout();

		Button cardsButton = createCardsButton();
		Button weighInSummaryButton = createWeighInSummaryButton();
		Button sessionsButton = createSessionsButton();
		Button officialSchedule = createOfficalsButton();
		Button checkInButton = createCheckInButton();

		Hr hr = new Hr();
		hr.setWidthFull();
		hr.getStyle().set("margin", "0");
		hr.getStyle().set("padding", "0");
		FlexLayout buttons = new FlexLayout(
		        new NativeLabel(Translator.translate("Preparation_Groups")),
		        sessionsButton, cardsButton, weighInSummaryButton, checkInButton, officialSchedule);
		buttons.getStyle().set("flex-wrap", "wrap");
		buttons.getStyle().set("gap", "1ex");
		buttons.getStyle().set("margin-left", "5em");
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		this.topBar.getStyle().set("flex", "100 1");
		this.topBar.add(buttons);
		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);

		return this.topBar;
	}

	private Button createCheckInButton() {
		return new Button("CheckIn");
	}

	private Button createOfficalsButton() {
		return new Button("Officials");
	}

	private Button createSessionsButton() {
		return new Button("StartList", (e) -> {
			logger.warn("selected: {}",crud.getSelectedItems());
		});
	}

	private Button createWeighInSummaryButton() {
		return new Button("WeighIn");
	}

	private Button createCardsButton() {
		return new Button("Cards");
	}

	@Override
	public void delete(Group domainObjectToDelete) {
		this.editingFormFactory.delete(domainObjectToDelete);
	}

	/**
	 * The refresh button on the toolbar
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
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
		return Translator.translate("Preparation_Groups");
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
		return this.editingFormFactory.update(domainObjectToUpdate);
	}

	/**
	 * The columns of the crudGrid
	 *
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	protected GridCrud<Group> createGrid(OwlcmsCrudFormFactory<Group> crudFormFactory) {
		Grid<Group> grid = new Grid<>(Group.class, false);
		crud = new GroupGrid(Group.class, new OwlcmsGridLayout(Group.class), crudFormFactory, grid);
		grid.getThemeNames().add("row-stripes");
		grid.addColumn(Group::getName).setHeader(Translator.translate("Name")).setComparator(Group::compareTo);
		grid.addColumn(Group::getDescription).setHeader(Translator.translate("Group.Description"));
		grid.addColumn(Group::size).setHeader(Translator.translate("GroupSize")).setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn(LocalDateTimeField.getRenderer(Group::getWeighInTime, this.getLocale()))
		        .setHeader(Translator.translate("WeighInTime")).setComparator(Group::compareToWeighIn);
		grid.addColumn(LocalDateTimeField.getRenderer(Group::getCompetitionTime, this.getLocale()))
		        .setHeader(Translator.translate("StartTime"));
		grid.addColumn(Group::getPlatform).setHeader(Translator.translate("Platform"));
		String translation = Translator.translate("EditAthletes");
		int tSize = translation.length();
		grid.addColumn(new ComponentRenderer<>(p -> {
			Button technical = openInNewTab(RegistrationContent.class, translation, p != null ? p.getName() : "?");
			// prevent grid row selection from triggering
			technical.getElement().addEventListener("click", ignore -> {
			}).addEventData("event.stopPropagation()");
			technical.addThemeVariants(ButtonVariant.LUMO_SMALL);
			return technical;
		})).setHeader("").setWidth(tSize + "ch");

		crud.setCrudListener(this);
		crud.setClickRowToUpdate(true);
		grid.setSelectionMode(SelectionMode.MULTI);
		return crud;
	}

	private <T extends Component> String getWindowOpenerFromClass(Class<T> targetClass,
	        String parameter) {
		return "window.open('" + URLUtils.getUrlFromTargetClass(targetClass) + "?group="
		        + URLEncoder.encode(parameter, StandardCharsets.UTF_8)
		        + "','" + targetClass.getSimpleName() + "')";
	}

	private <T extends Component> Button openInNewTab(Class<T> targetClass,
	        String label, String parameter) {
		Button button = new Button(label);
		button.getElement().setAttribute("onClick", getWindowOpenerFromClass(targetClass, parameter != null ? parameter : "-"));
		return button;
	}
	
	protected void saveCallBack(OwlcmsCrudGrid<T> owlcmsCrudGrid, String successMessage, CrudOperation operation, T domainObject) {
		try {
			//logger.debug("postOperation {}", domainObject);
			owlcmsCrudGrid.getOwlcmsGridLayout().hideForm();
			crud.refreshGrid();
			Notification.show(successMessage);
			logger.trace("operation performed");
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
		}
	}

}
