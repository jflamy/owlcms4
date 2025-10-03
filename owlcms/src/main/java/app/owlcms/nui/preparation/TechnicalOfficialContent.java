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
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.components.JXLSDownloader;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.technicalofficial.TechnicalOfficial;
import app.owlcms.data.technicalofficial.TechnicalOfficialRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.spreadsheet.JXLSExportTechnicalOfficials;
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

	/**
	 * Custom CrudGrid to handle focus management after save operations
	 */
	private final class TechnicalOfficialCrudGrid extends OwlcmsCrudGrid<TechnicalOfficial> {
		
		private TechnicalOfficialCrudGrid(Class<TechnicalOfficial> domainType, OwlcmsGridLayout crudLayout,
		        OwlcmsCrudFormFactory<TechnicalOfficial> owlcmsCrudFormFactory, Grid<TechnicalOfficial> grid) {
			super(domainType, crudLayout, owlcmsCrudFormFactory, grid);
		}

		@Override
		protected void saveCallBack(OwlcmsCrudGrid<TechnicalOfficial> owlcmsCrudGrid, String successMessage,
		        CrudOperation operation, TechnicalOfficial official) {
			try {
				owlcmsCrudGrid.getGrid().asSingleSelect().clear();
				owlcmsCrudGrid.getOwlcmsGridLayout().hideForm();
				refreshGrid();
				Notification.show(successMessage);
				focusOutsideThenBackToTriggeringItem();
			} catch (Exception e) {
				logger.error("Error in save callback", e);
			}
		}
	}

	final static Logger logger = (Logger) LoggerFactory.getLogger(TechnicalOfficialContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	private OwlcmsCrudFormFactory<TechnicalOfficial> editingFormFactory;
	private OwlcmsLayout routerLayout;
	private FlexLayout topBar;
	private GridCrud<TechnicalOfficial> crud;
	private Grid<TechnicalOfficial> grid;
	private TextField lastNameFilter = new TextField();
	private String lastNameValue;

	/**
	 * Instantiates the TechnicalOfficial crudGrid.
	 */
	public TechnicalOfficialContent() {
		OwlcmsCrudFormFactory<TechnicalOfficial> crudFormFactory = createFormFactory();
		crud = createGrid(crudFormFactory);
		defineFilters(crud);
		fillHW(crud, this);
	}

	@Override
	public TechnicalOfficial add(TechnicalOfficial domainObjectToAdd) {
		return this.editingFormFactory.add(domainObjectToAdd);
	}

	@Override
	// public FlexLayout createMenuArea() {
	// return new FlexLayout();
	// }

	public FlexLayout createMenuArea() {
		this.topBar = new FlexLayout();

		// Export current officials button using the age groups pattern
        Div exportOfficials = DownloadButtonFactory.createDynamicXLSXDownloadButton(
            "TechnicalOfficials",
            Translator.translate("TechnicalOfficials.Export"), 
            new XLSXTechnicalOfficialsExport(UI.getCurrent()));
        exportOfficials.getStyle().set("margin-left", "1em");

		Button uploadCustom = new Button(Translator.translate("TechnicalOfficials.Upload"),
		        new Icon(VaadinIcon.UPLOAD_ALT),
		        buttonClickEvent -> {
			        TechnicalOfficialsUploadDialog dialog = new TechnicalOfficialsUploadDialog();
			        dialog.setCallback(() -> refreshGrid());
			        dialog.open();
		        });

		var toAssignmentsWriter = new JXLSExportTechnicalOfficials(UI.getCurrent());
		JXLSDownloader dd1 = new JXLSDownloader(
		        () -> {
			        return toAssignmentsWriter;
		        },
		        "/templates/toAssignments",
		        Competition::getComputedTechnicalOfficialsTemplateFileName,
		        Competition::setTechnicalOfficialsTemplateFileName,
		        Translator.translate("TechnicalOfficials.ExportAssignmentReports"),
		        Translator.translate("Download"));
		Div allRecords1 = new Div();
		Button downloadButton = dd1.createDownloadButton();
		downloadButton.setWidthFull();
		allRecords1.add(downloadButton);

		FlexLayout buttons = new FlexLayout(
		        new NativeLabel(Translator.translate("TechnicalOfficials.ImportExport")),
		        exportOfficials,
		        uploadCustom,
				hr(),
				new NativeLabel(Translator.translate("TechnicalOfficials.AssignmentReports")),
				allRecords1);
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

	private Hr hr() {
		Hr hr = new Hr();
		hr.setWidthFull();
		hr.getStyle().set("margin", "0");
		hr.getStyle().set("padding", "0");
		return hr;
	}

	private Object refreshGrid() {
		crud.refreshGrid();
        return null;
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
		Collection<TechnicalOfficial> officials = TechnicalOfficialRepository.findAll();
		
		// Apply last name filter if present
		if (lastNameValue != null && !lastNameValue.trim().isEmpty()) {
			String filterLower = lastNameValue.toLowerCase().trim();
			officials = officials.stream()
				.filter(official -> {
					String lastName = official.getLastName();
					return lastName != null && lastName.toLowerCase().contains(filterLower);
				})
				.collect(java.util.stream.Collectors.toList());
		}
		
		return officials;
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
		((TechnicalOfficialEditingFormFactory) crudFormFactory).technicalOfficialLayout();
	}

	/**
	 * The columns of the crudGrid
	 *
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	protected GridCrud<TechnicalOfficial> createGrid(OwlcmsCrudFormFactory<TechnicalOfficial> crudFormFactory) {
		this.grid = new Grid<>(TechnicalOfficial.class, false);
		this.grid.getThemeNames().add("row-stripes");
		
		// Active checkbox column - immediate update without opening form
		this.grid.addColumn(new ComponentRenderer<>(official -> {
			Checkbox activeBox = new Checkbox();
			activeBox.setLabel(null);
			activeBox.getElement().getThemeList().set("secondary", true);
			activeBox.setValue(official.isActive());
			activeBox.addValueChangeListener(click -> {
				activeBox.setValue(click.getValue());
				official.setActive(click.getValue());
				TechnicalOfficialRepository.save(official);
				this.grid.getDataProvider().refreshItem(official);
			});
			// prevent getting the row selection involved.
			activeBox.getElement().addEventListener("click", ignore -> {
			}).addEventData("event.stopPropagation()");
			return activeBox;
		})).setHeader(Translator.translate("TechnicalOfficial.Active")).setWidth("0");
		
		this.grid.addColumn(TechnicalOfficial::getFullName).setHeader(Translator.translate("Name"));
		
		// Role column with translated role name
		this.grid.addColumn(official -> {
			TechnicalOfficial.Role role = official.getRole();
			if (role == null) {
				return "";
			}
			return Translator.translate("TO.Role." + role.name());
		}).setHeader(Translator.translate("TO.Role"));
		
		this.grid.addColumn(TechnicalOfficial::getLevel).setHeader(Translator.translate("TechnicalOfficial.Level"));
		this.grid.addColumn(TechnicalOfficial::getFederationId).setHeader(Translator.translate("TechnicalOfficial.FederationId"));
		this.grid.addColumn(TechnicalOfficial::getFederation).setHeader(Translator.translate("TechnicalOfficial.Federation"));
		this.grid.addColumn(TechnicalOfficial::getAffiliation).setHeader(Translator.translate("TechnicalOfficial.Affiliation"));
		this.grid.addColumn(TechnicalOfficial::getIwfId).setHeader(Translator.translate("TechnicalOfficial.IWFId"));

		TechnicalOfficialCrudGrid crud = new TechnicalOfficialCrudGrid(TechnicalOfficial.class, 
				new OwlcmsGridLayout(TechnicalOfficial.class),
		        crudFormFactory, this.grid);
		crud.setCrudListener(this);
		crud.setClickRowToUpdate(true);
		return crud;
	}

	/**
	 * The filters at the top of the crudGrid
	 *
	 * @param crud the crudGrid that will be filtered.
	 */
	protected void defineFilters(GridCrud<TechnicalOfficial> crud) {
		this.lastNameFilter.setPlaceholder(Translator.translate("LastName"));
		this.lastNameFilter.setClearButtonVisible(true);
		this.lastNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
		this.lastNameFilter.addValueChangeListener(e -> {
			this.lastNameValue = e.getValue();
			crud.refreshGrid();
		});
		this.lastNameFilter.setWidth("15em");
		crud.getCrudLayout().addFilterComponent(this.lastNameFilter);
	}

	/**
	 * Define the form used to edit a given TechnicalOfficial.
	 *
	 * @return the form factory that will create the actual form on demand
	 */
	private OwlcmsCrudFormFactory<TechnicalOfficial> createFormFactory() {
		this.editingFormFactory = new TechnicalOfficialEditingFormFactory(TechnicalOfficial.class);
		return this.editingFormFactory;
	}

	// private <T extends Component & HasUrlParameter<String>> String getWindowOpenerFromClass(Class<T> targetClass,
	// String parameter) {
	// return "window.open('" + URLUtils.getUrlFromTargetClass(targetClass) + "?fop=" + parameter
	// + "','" + targetClass.getSimpleName() + "')";
	// }

	// private <T extends Component & HasUrlParameter<String>> Button openInNewTab(Class<T> targetClass,
	// String label, String parameter) {
	// Button button = new Button(label);
	// button.getElement().setAttribute("onClick", getWindowOpenerFromClass(targetClass, parameter));
	// return button;
	// }
}
