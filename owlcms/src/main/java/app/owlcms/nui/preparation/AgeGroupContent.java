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
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import app.owlcms.access.AccessUi;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.access.Access;
import app.owlcms.access.RequiresAccess;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.NotificationUtils;
import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupDefinitionChecks;
import app.owlcms.data.agegroup.AgeGroupRepository;
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
@RequiresAccess(Access.PREPARATION)
@Route(value = "preparation/agegroup", layout = OwlcmsLayout.class)
public class AgeGroupContent extends BaseContent implements CrudListener<AgeGroup>, OwlcmsContent {

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
	private Button reassignWithin;
	private OwlcmsLayout routerLayout;
	private FlexLayout topBar;
	private boolean applyingUrlFilter;

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
		this.reassignWithin = new Button(Translator.translate("AgeGroup.ReassignWithin.Label"), (e) -> {
			AgeGroupSaveFlow.globalAction(UI.getCurrent(), AuditOrigin.fromUi("reassign within championships"), false,
			        () -> {
				        this.getCrud().refreshGrid();
				        unHighlightReassignWithinButton();
			        });
		});
		this.reassignWithin.setId("agegroup-reassign-within");
		this.reassignWithin.getElement().setAttribute("title",
		        Translator.translate("AgeGroup.ReassignWithin.MouseOver"));
		HorizontalLayout reassignWithinButton = new HorizontalLayout(this.reassignWithin);
		reassignWithinButton.setMargin(false);

		Button recomputeAll = new Button(Translator.translate("AgeGroup.RecomputeAll.Label"), (e) -> {
			AgeGroupSaveFlow.globalAction(UI.getCurrent(), AuditOrigin.fromUi("recompute all eligibilities"), true,
			        () -> this.getCrud().refreshGrid());
		});
		recomputeAll.setId("agegroup-recompute-all");
		recomputeAll.getElement().setAttribute("title",
		        Translator.translate("AgeGroup.RecomputeAll.MouseOver"));
		HorizontalLayout recomputeAllButton = new HorizontalLayout(recomputeAll);
		recomputeAllButton.setMargin(false);

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
				AuditOrigin loadOrigin = AuditOrigin.fromUi("age group definitions load");
				Locale loadLocale = OwlcmsSession.getLocale();
				new ConfirmationDialog(Translator.translate("ResetCategories.ResetCategories"),
				        Translator.translate("ResetCategories.Warning_ResetCategories"),
				        Translator.translate("ResetCategories.CategoriesReset"), () -> {
					        AgeGroupRepository.reloadDefinitions(definitions.getFileName(), loadOrigin);
					        resetCategories();
					        showDefinitionWarnings(AgeGroupDefinitionChecks.warnings(loadLocale));
				        }).open();
			}
		});
		Button uploadCustom = new Button(Translator.translate("AgeGroups.ImportDefinitions"),
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
			        UI.getCurrent().navigate(ChampionshipsContent.class);
		        });
		editChampionships.setVisible(AccessUi.canOpen(ChampionshipsContent.class, null));

		FlexLayout buttons = new FlexLayout(
		        new NativeLabel(Translator.translate("AgeGroups.Predefined")),
		        reloadDefinition,
		        hr(),
		        new NativeLabel(Translator.translate("AgeGroups.Custom")),
		        exportAgeGroups, uploadCustom, editChampionships,
		        hr(),
		        new NativeLabel(Translator.translate("AgeGroups.Reassign")),
		        reassignWithinButton, recomputeAllButton);
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
		AgeGroupSaveFlow.deleteAgeGroup(UI.getCurrent(), domainObjectToDelete,
		        AuditOrigin.fromUi("age group delete"), () -> this.getCrud().refreshGrid());
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
				return compare; // configured championship order
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

	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	public void highlightReassignWithinButton() {
		this.reassignWithin.setThemeName("primary error");
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
		// unreachable: the form factory routes updates through AgeGroupSaveFlow
		throw new IllegalStateException("age group updates go through AgeGroupSaveFlow");
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
			activeBox.getElement().setAttribute("data-agegroup", String.valueOf(ag.getCode()) + "_" + ag.getGender());
			activeBox.addClassName("agegroup-active");
			activeBox.getElement().getThemeList().set("secondary", true);
			// the checkbox shows the stored value until the change is confirmed (spec 3.5)
			activeBox.setValue(ag.isActive());
			activeBox.addValueChangeListener(click -> {
				if (!click.isFromClient()) {
					return;
				}
				activeBox.setValue(ag.isActive());
				AgeGroupSaveFlow.toggleActive(UI.getCurrent(), ag, click.getValue(),
				        AuditOrigin.fromUi("age group active toggle"),
				        () -> this.getCrud().refreshGrid(),
				        () -> highlightReassignWithinButton(),
				        () -> activeBox.setValue(ag.isActive()));
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
			        if (championship != null && !championship.isCompetitionTemplate()) {
				        return championship.translate();
			        }
			        String championshipName = item.getChampionshipName();
			        if (championshipName == null || championshipName.isBlank()
			                || championshipName.trim().equalsIgnoreCase(Championship.COMPETITION_TEMPLATE_NAME)) {
				        championshipName = item.getCode();
			        }
			        return championshipName != null && !championshipName.isBlank() ? championshipName : "?";
		        }))
		        .setHeader(Translator.translate("Championship"));
		grid.addColumn(new TextRenderer<>(
		        item -> {
			        return medalsText(item);
		        }))
		        .setHeader(Translator.translate("CeremonyType.MEDALS"));
		grid.addColumn(new TextRenderer<>(
		        item -> {
			        return rankingText(item.computedBestAthleteScoringSystem());
		        }))
		        .setHeader(Translator.translate("Championship.bestAthleteScoringSystem"));
		grid.addColumn(new TextRenderer<>(
		        item -> {
			        Gender gender = item.getGender();
					return gender != null ? gender.asGenderName() : "";
		        }))
		        .setHeader(Translator.translate("Gender"));
		grid.addColumn(AgeGroup::getMinAge).setHeader(Translator.translate("MinimumAge"));
		grid.addColumn(AgeGroup::getMaxAge).setHeader(Translator.translate("MaximumAge"));
		grid.addColumn(AgeGroup::getCategoriesAsString).setAutoWidth(true)
		        .setHeader(Translator.translate("BodyWeightCategories"));

		this.setCrud(new OwlcmsCrudGrid<AgeGroup>(AgeGroup.class, new OwlcmsGridLayout(AgeGroup.class),
		        crudFormFactory, grid) {
			// edit opens a detached scratchpad fetched in a worker; the grid item is never mutated (spec 3.4)
			@Override
			protected void showForm(CrudOperation operation, AgeGroup item, boolean readOnly, String successMessage,
			        ComponentEventListener<ClickEvent<Button>> unused) {
				if (operation == CrudOperation.UPDATE && item != null && item.getId() != null) {
					AgeGroupSaveFlow.loadScratchpad(UI.getCurrent(), item.getId(),
					        scratch -> super.showForm(operation, scratch, readOnly, successMessage, unused));
					return;
				}
				super.showForm(operation, item, readOnly, successMessage, unused);
			}
		});
		this.getCrud().setCrudListener(this);
		this.getCrud().setClickRowToUpdate(true);
		return this.getCrud();
	}

	private static String rankingText(Ranking ranking) {
		return ranking != null ? Translator.translate("Ranking." + ranking) : "";
	}

	private static String medalsText(AgeGroup ageGroup) {
		Championship championship = ageGroup.getChampionship();
		if (championship != null && championship.isSnatchCJTotalMedals()) {
			String lifts = String.join(" ",
			        Translator.translate("Results.Snatch_abbrev"),
			        Translator.translate("Results.CJ_abbrev"));
			return championship.getMedalPolicy().includesTotal()
			        ? lifts + " " + Translator.translate("Results.Total") : lifts;
		}
		return rankingText(ageGroup.getComputedScoringSystem());
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
			if (!this.applyingUrlFilter) {
				crud.refreshGrid();
			}
		});
		crud.getCrudLayout().addFilterComponent(this.championshipFilter);
		crud.getCrudLayout().addToolbarComponent(new NativeLabel(""));

		this.activeFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		this.activeFilter.setLabel(Translator.translate("EditChampionships.HideInactive"));
		this.activeFilter.setAriaLabel(Translator.translate("ActiveCategoriesOnly"));
		crud.getCrudLayout().addFilterComponent(this.activeFilter);

		Button clearFilters = new Button((String) null, VaadinIcon.CLOSE.create());
		clearFilters.addClickListener(event -> {
			this.nameFilter.clear();
			this.activeFilter.clear();
			this.championshipFilter.clear();
		});
		crud.getCrudLayout().addFilterComponent(clearFilters);
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		super.setParameter(event, parameter);
		Map<String, List<String>> parameters = getUrlParameterMap();
		List<String> championshipNames = parameters != null ? parameters.get("championship") : null;
		if (championshipNames != null && !championshipNames.isEmpty()) {
			Championship championship = Championship.findStored(championshipNames.get(0));
			if (championship != null) {
				this.applyingUrlFilter = true;
				try {
					this.championshipFilter.setValue(championship);
				} finally {
					this.applyingUrlFilter = false;
				}
			}
		}
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
		unHighlightReassignWithinButton();
	}

	/** Gaps and overlaps in the definitions just loaded: a warning only, the definitions are already in place. */
	private void showDefinitionWarnings(AgeGroupDefinitionChecks.Warnings warnings) {
		if (warnings.lines().isEmpty()) {
			return;
		}
		UI ui = UI.getCurrent();
		Dialog dialog = new Dialog();
		VerticalLayout body = new VerticalLayout();
		warnings.lines().forEach(line -> body.add(new Paragraph(line)));
		Button close = new Button(Translator.translate("Close"), e -> dialog.close());
		dialog.add(new VerticalLayout(new H3(Translator.translate(
		        warnings.hasGaps() ? "AgeGroup.AgeGap.Title" : "AgeGroup.AgeOverlap.Title")), body, close));
		// opened from the confirmation dialog's callback: attach to the UI, not to the dialog that is closing
		ui.add(dialog);
		dialog.addOpenedChangeListener(e -> {
			if (!e.isOpened()) {
				dialog.getElement().removeFromParent();
			}
		});
		dialog.open();
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
			String fileName = e.getValue().getFileName();
			if (!Objects.equals(Competition.getCurrent().getAgeGroupsFileName(), fileName)) {
				CompetitionRepository.apply(c -> c.setAgeGroupsFileName(fileName), AuditOrigin.fromUi("age group file"));
			}
		});
		this.ageGroupDefinitionSelect.setValue(found);
	}

	private void unHighlightReassignWithinButton() {
		this.reassignWithin.setThemeName("");
	}

}
