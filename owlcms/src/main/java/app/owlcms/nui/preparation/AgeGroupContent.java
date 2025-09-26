/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.NotificationUtils;
import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.AssignedAthletesException;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.RequireLogin;
import app.owlcms.spreadsheet.XLSXAgeGroupsExport;
import app.owlcms.utils.Resource;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AgeGroupContent.
 *
 * Defines the toolbar and the table for editing data on categories.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/agegroup", layout = OwlcmsLayout.class)
public class AgeGroupContent extends BaseContent implements CrudListener<AgeGroup>, OwlcmsContent, RequireLogin {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	private Checkbox activeFilter = new Checkbox();
	private ComboBox<Championship> championshipFilter = new ComboBox<>();
	private ComboBox<Resource> ageGroupDefinitionSelect;
	private OwlcmsCrudFormFactory<AgeGroup> ageGroupEditingFormFactory;
	private GridCrud<AgeGroup> crud;
	// private ComboBox<AgeGroup> ageGroupFilter = new ComboBox<>();
	private TextField nameFilter = new TextField();
	private Button resetCats;
	private OwlcmsLayout routerLayout;
	private FlexLayout topBar;
	private Button clearEligibles;

	/**
	 * Instantiates the ageGroup crudGrid.
	 */
	public AgeGroupContent() {
		OwlcmsFactory.waitDBInitialized();
		OwlcmsCrudFormFactory<AgeGroup> editingFormFactory = new AgeGroupEditingFormFactory(AgeGroup.class, this);
		setAgeGroupEditingFormFactory(editingFormFactory);
		this.setCrud(createGrid(getAgeGroupEditingFormFactory()));
		defineFilters(this.getCrud());
		fillHW(this.getCrud(), this);
	}

	@Override
	public AgeGroup add(AgeGroup ageGroup) {
		return getAgeGroupEditingFormFactory().add(ageGroup);
	}

	/**
	 * @see #showRouterLayoutContent(HasElement) for how to content to layout and vice-versa
	 */
	@Override
	public FlexLayout createMenuArea() {
		this.topBar = new FlexLayout();
		this.resetCats = new Button(Translator.translate("ResetCategories.ResetAthletes"), (e) -> {
			new ConfirmationDialog(
			        Translator.translate("ResetCategories.ResetCategories"),
			        Translator.translate("ResetCategories.Warning_ResetCategories"),
			        Translator.translate("ResetCategories.CategoriesReset"), () -> {
				        resetCategories();
			        }).open();
		});
		this.resetCats.getElement().setAttribute("title", Translator.translate("ResetCategories.ResetCategoriesMouseOver"));
		HorizontalLayout resetButton = new HorizontalLayout(this.resetCats);
		resetButton.setMargin(false);
		
		this.clearEligibles = new Button(Translator.translate("ResetCategories.ClearEligibles"), (e) -> {
			new ConfirmationDialog(
			        Translator.translate("ResetCategories.ClearEligibles"),
			        Translator.translate("ResetCategories.Warning_ClearEligibles"),
			        Translator.translate("ResetCategories.ClearEligibles"), () -> {
				        clearEligibleCategories();
			        }).open();
		});
		this.resetCats.getElement().setAttribute("title", Translator.translate("ResetCategories.ClearEligiblesMouseOver"));
		HorizontalLayout clearEligiblesButton = new HorizontalLayout(this.clearEligibles);
		clearEligiblesButton.setMargin(false);

		this.ageGroupDefinitionSelect = new ComboBox<>();
		this.ageGroupDefinitionSelect.setPlaceholder(Translator.translate("ResetCategories.AvailableDefinitions"));

		Locale locale = OwlcmsSession.getLocale();
		// locale = new Locale("fr","FR");

		List<Resource> resourceList = new ResourceWalker().getResourceList("/agegroups", ResourceWalker::relativeName,
		        null, locale, Config.getCurrent().isLocalTemplatesOnly());
		resourceList.sort((a, b) -> a.compareTo(b));
		this.ageGroupDefinitionSelect.setItems(resourceList);
		this.ageGroupDefinitionSelect.setValue(null);
		this.ageGroupDefinitionSelect.setWidth("15em");
		this.ageGroupDefinitionSelect.getStyle().set("margin-left", "1em");
		setAgeGroupsSelectionListener(resourceList);

		Button loadPredefined = new Button(Translator.translate("AgeGroups.LoadPredefined"), (e) -> {
			Resource definitions = this.ageGroupDefinitionSelect.getValue();
			if (definitions == null) {
				String labelText = Translator.translate("ResetCategories.PleaseSelectDefinitionFile");
				NotificationUtils.errorNotification(labelText);
			} else {
				new ConfirmationDialog(Translator.translate("ResetCategories.ResetCategories"),
				        Translator.translate("ResetCategories.Warning_ResetCategories"),
				        Translator.translate("ResetCategories.CategoriesReset"), () -> {
					        AgeGroupRepository.reloadDefinitions(definitions.getFileName());
					        resetCategories();
				        }).open();
			}
		});
		Button uploadCustom = new Button(Translator.translate("AgeGroups.UploadCustom"),
		        new Icon(VaadinIcon.UPLOAD_ALT),
		        buttonClickEvent -> {
			        AgeGroupsFileUploadDialog ageGroupsFileUploadDialog = new AgeGroupsFileUploadDialog();
			        ageGroupsFileUploadDialog.setCallback(() -> resetCategories());
			        ageGroupsFileUploadDialog.open();
		        });

		HorizontalLayout reloadDefinition = new HorizontalLayout(this.ageGroupDefinitionSelect, loadPredefined);
		reloadDefinition.setAlignItems(FlexComponent.Alignment.BASELINE);
		reloadDefinition.setMargin(false);
		reloadDefinition.setPadding(false);
		reloadDefinition.setSpacing(false);

		Div exportAgeGroups = DownloadButtonFactory.createDynamicXLSXDownloadButton("AgeGroups",
		        Translator.translate("AgeGroups.ExportDefinitions"), new XLSXAgeGroupsExport(UI.getCurrent()));
		exportAgeGroups.getStyle().set("margin-left", "1em");

		Button editChampionships = new Button(
		        Translator.translate("EditChampionships.Title"),
		        VaadinIcon.PENCIL.create(),
		        e -> {
			        new EditChampionshipsDialog(this).open();
		        });

		FlexLayout buttons = new FlexLayout(
		        new NativeLabel(Translator.translate("AgeGroups.Predefined")),
		        reloadDefinition,
		        hr(),
		        new NativeLabel(Translator.translate("AgeGroups.Custom")),
		        exportAgeGroups, uploadCustom, editChampionships,
		        hr(),
		        new NativeLabel(Translator.translate("AgeGroups.Reassign")),
		        resetButton, clearEligiblesButton);
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

	@Override
	public void delete(AgeGroup domainObjectToDelete) {
		getAgeGroupEditingFormFactory().delete(domainObjectToDelete);
	}

	/**
	 * The refresh button on the toolbar
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<AgeGroup> findAll() {
		List<AgeGroup> all = AgeGroupRepository.findFiltered(this.nameFilter.getValue(), (Gender) null,
		        this.championshipFilter.getValue(),
		        (Integer) null, this.activeFilter.getValue(), -1, -1);
		all.sort((ag1, ag2) -> {
			int compare = 0;
			compare = ObjectUtils.compare(ag1.getChampionship(), ag2.getChampionship());
			if (compare != 0) {
				return -compare; // reverse order for DEFAULT first.
			}
			compare = ObjectUtils.compare(ag1.getGender(), ag2.getGender());
			if (compare != 0) {
				return compare;
			}
			compare = ObjectUtils.compare(ag1.getMinAge(), ag2.getMinAge());
			if (compare != 0) {
				return compare;
			}
			compare = ObjectUtils.compare(ag1.getMaxAge(), ag2.getMaxAge());
			if (compare != 0) {
				return compare;
			}
			return 0;
		});
		CategoryRepository.resetCodeMap();
		return all;
	}

	public GridCrud<AgeGroup> getCrud() {
		return this.crud;
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("EditAgeGroups");
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return Translator.translate("Preparation_AgeGroups");
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return this.routerLayout;
	}

	public void highlightResetButton() {
		this.resetCats.setThemeName("primary error");
	}

	public void setCrud(GridCrud<AgeGroup> crud) {
		this.crud = crud;
	}

	@Override
	public void setHeaderContent() {
		getRouterLayout().setMenuTitle(getMenuTitle());
		getRouterLayout().setMenuArea(createMenuArea());
		getRouterLayout().showLocaleDropdown(false);
		getRouterLayout().setDrawerOpened(false);
		getRouterLayout().updateHeader(true);
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}

	@Override
	public AgeGroup update(AgeGroup domainObjectToUpdate) {
		AgeGroup ageGroup = getAgeGroupEditingFormFactory().update(domainObjectToUpdate);
		return ageGroup;
	}

	/**
	 * The columns of the crudGrid
	 *
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	protected GridCrud<AgeGroup> createGrid(OwlcmsCrudFormFactory<AgeGroup> crudFormFactory) {
		Grid<AgeGroup> grid = new Grid<>(AgeGroup.class, false);
		grid.getThemeNames().add("row-stripes");
		grid.addColumn(new ComponentRenderer<>(ag -> {
			Checkbox activeBox = new Checkbox("Name");
			activeBox.setLabel(null);
			activeBox.getElement().getThemeList().set("secondary", true);
			activeBox.setValue(ag.isActive());
			activeBox.addValueChangeListener(click -> {
				activeBox.setValue(click.getValue());
				ag.setActive(click.getValue());
				try {
					AgeGroupRepository.save(ag);
				} catch (AssignedAthletesException e) {
					// ignore -- saving the active flag cannot trigger this exception
				}
				grid.getDataProvider().refreshItem(ag);
			});
			// prevent getting the row selection involved.
			activeBox.getElement().addEventListener("click", ignore -> {
			}).addEventData("event.stopPropagation()");
			return activeBox;
		})).setHeader(Translator.translate("Active")).setWidth("0");
		grid.addColumn(AgeGroup::getDisplayName).setHeader(Translator.translate("Name"));
		grid.addColumn(new TextRenderer<>(
		        item -> {
			        Championship championship = item.getChampionship();
			        logger.trace("createGrid age division {}", championship);
			        String tr = (championship != null ? championship.translate() : "?");
			        return tr;
		        }))
		        .setHeader(Translator.translate("Championship"));
		grid.addColumn(new TextRenderer<>(
		        item -> {
			        Ranking ss = item.getScoringSystem();
			        String tr = (ss != null ? Translator.translate("Ranking." + ss) : "");
			        return tr;
		        }))
		        .setHeader(Translator.translate("CeremonyType.MEDALS"));
		grid.addColumn(new TextRenderer<>(
		        item -> {
			        return item.getGender().asGenderName();
		        }))
		        .setHeader(Translator.translate("Gender"));
		grid.addColumn(AgeGroup::getMinAge).setHeader(Translator.translate("MinimumAge"));
		grid.addColumn(AgeGroup::getMaxAge).setHeader(Translator.translate("MaximumAge"));
		grid.addColumn(AgeGroup::getCategoriesAsString).setAutoWidth(true)
		        .setHeader(Translator.translate("BodyWeightCategories"));

		this.setCrud(new OwlcmsCrudGrid<>(AgeGroup.class, new OwlcmsGridLayout(AgeGroup.class),
		        crudFormFactory, grid));
		this.getCrud().setCrudListener(this);
		this.getCrud().setClickRowToUpdate(true);
		return this.getCrud();
	}

	/**
	 * The filters at the top of the crudGrid
	 *
	 * @param crudGrid the crudGrid that will be filtered.
	 */
	protected void defineFilters(GridCrud<AgeGroup> crud) {
		this.nameFilter.setPlaceholder(Translator.translate("Name"));
		this.nameFilter.setClearButtonVisible(true);
		this.nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
		this.nameFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		crud.getCrudLayout().addFilterComponent(this.nameFilter);

		this.championshipFilter.setPlaceholder(Translator.translate("Championship"));
		this.championshipFilter.setItems(Championship.findAllUsed(false));
		this.championshipFilter.setItemLabelGenerator((ad) -> ad.translate());
		this.championshipFilter.setClearButtonVisible(true);
		this.championshipFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		crud.getCrudLayout().addFilterComponent(this.championshipFilter);
		crud.getCrudLayout().addToolbarComponent(new NativeLabel(""));

		this.activeFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		this.activeFilter.setLabel(Translator.translate("Active"));
		this.activeFilter.setAriaLabel(Translator.translate("ActiveCategoriesOnly"));
		crud.getCrudLayout().addFilterComponent(this.activeFilter);

		Button clearFilters = new Button(null, VaadinIcon.CLOSE.create());
		clearFilters.addClickListener(event -> {
			this.nameFilter.clear();
			this.activeFilter.clear();
			this.championshipFilter.clear();
		});
		crud.getCrudLayout().addFilterComponent(clearFilters);
	}

	/**
	 * We do not connect to the event bus, and we do not track a field of play (non-Javadoc)
	 *
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
	}

	void closeDialog() {
		this.getCrud().getCrudLayout().hideForm();
		this.getCrud().getGrid().asSingleSelect().clear();
	}

	private OwlcmsCrudFormFactory<AgeGroup> getAgeGroupEditingFormFactory() {
		return this.ageGroupEditingFormFactory;
	}

	private Hr hr() {
		Hr hr = new Hr();
		hr.setWidthFull();
		hr.getStyle().set("margin", "0");
		hr.getStyle().set("padding", "0");
		return hr;
	}

	private void resetCategories() {
		AthleteRepository.resetParticipations(false, true);
		this.getCrud().refreshGrid();
		unHighlightResetButton();
	}
	
	private void clearEligibleCategories() {
		AthleteRepository.resetParticipations(true, false);
		this.getCrud().refreshGrid();
		unHighlightResetButton();
	}


	private Resource searchMatch(List<Resource> resourceList, String curTemplateName) {
		Resource found = null;
		for (Resource curResource : resourceList) {
			String fileName = curResource.getFileName();
			if (fileName.equals(curTemplateName)) {
				found = curResource;
				break;
			}
		}
		return found;
	}

	private void setAgeGroupEditingFormFactory(OwlcmsCrudFormFactory<AgeGroup> ageGroupEditingFormFactory) {
		this.ageGroupEditingFormFactory = ageGroupEditingFormFactory;
	}

	private void setAgeGroupsSelectionListener(List<Resource> resourceList) {
		String curTemplateName = Competition.getCurrent().getAgeGroupsFileName();
		Resource found = searchMatch(resourceList, curTemplateName);
		this.ageGroupDefinitionSelect.addValueChangeListener((e) -> {
			logger.debug("setTemplateSelectionListener {}", found);
			Competition.getCurrent().setAgeGroupsFileName(e.getValue().getFileName());
			CompetitionRepository.save(Competition.getCurrent());
		});
		this.ageGroupDefinitionSelect.setValue(found);
	}

	private void unHighlightResetButton() {
		this.resetCats.setThemeName("");
	}

}
