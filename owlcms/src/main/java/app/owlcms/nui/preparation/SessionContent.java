/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.components.JXLSDownloader;
import app.owlcms.components.fields.LocalDateTimeField;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.RegistrationOrderComparator;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.servlet.StopProcessingException;
import app.owlcms.spreadsheet.JXLSCardsDocs;
import app.owlcms.spreadsheet.JXLSCardsWeighIn;
import app.owlcms.spreadsheet.JXLSCategoriesListDocs;
import app.owlcms.spreadsheet.JXLSJurySheet;
import app.owlcms.spreadsheet.JXLSResultSheet;
import app.owlcms.spreadsheet.JXLSStartingListDocs;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.URLUtils;
import app.owlcms.utils.ZipUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class GroupContent.
 *
 * Defines the toolbar and the table for editing data on sessions.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/sessions", layout = OwlcmsLayout.class)
@RouteAlias(value = "preparation/documents", layout = OwlcmsLayout.class)
public class SessionContent extends BaseContent implements CrudListener<Group>, OwlcmsContent {

	private record KitElement(String id, String name, String extension, Path isp, int count, Supplier<JXLSWorkbookStreamSource> writerFactory) {
	}

	final static Logger logger = (Logger) LoggerFactory.getLogger(SessionContent.class);

	static {
		logger.setLevel(Level.INFO);
	}
	boolean documentPage;
	private GroupGrid crud;
	private OwlcmsCrudFormFactory<Group> editingFormFactory;
	private OwlcmsLayout routerLayout;
	private FlexLayout topBar;

	/**
	 * Instantiates the Group crudGrid.
	 */
	public SessionContent() {
		this.editingFormFactory = new SessionEditingFormFactory(Group.class, this);
		GridCrud<Group> crud = createGrid(this.editingFormFactory);
		// defineFilters(crudGrid);
		fillHW(crud, this);
	}

	@Override
	public Group add(Group domainObjectToAdd) {
		return this.editingFormFactory.add(domainObjectToAdd);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		String path = event.getLocation().getPath();
		this.documentPage = path.contains("documents");
	}

	public void closeDialog() {
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		if (documentPage) {
			crud.getAddButton().removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
		}
	}
	
	@Override
	public FlexLayout createMenuArea() {
		this.topBar = new FlexLayout();

		Button bwButton = createBWButton();
		Button categoriesListButton = createCategoriesListButton();
		Button teamsListButton = createTeamsListButton();
		Button registrationTemplateSelection = new Button(
		        Translator.translate("Documents.SelectTemplates"), VaadinIcon.COG.create(), event -> registrationTemplateSelection());
		registrationTemplateSelection.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
		
		Button startListButton = createFullStartListButton();
		Button scheduleButton = createFullScheduleButton();
		Button officialSchedule = createOfficalsButton();
		Button checkInButton = createCheckInButton();
		Button competitionTemplateSelection = new Button(
		        Translator.translate("Documents.SelectTemplates"), VaadinIcon.COG.create(), event -> competitionTemplateSelection());
		competitionTemplateSelection.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

		Div cardsButton = createCardsButton();
		Div weighInSummaryButton = createWeighInSummaryButton();
		Div preWeighInKitButton = createPreWeighInButton();
		Button preWeighInTemplateSelection = new Button(
		        Translator.translate("Documents.SelectTemplates"), VaadinIcon.COG.create(), event -> preWeighInTemplateSelection());
		preWeighInTemplateSelection.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

		Div introductionButton = createIntroductionButton();
		Div emptyProtocolButton = createEmptyProtocolButton();
		Div juryButton = createJuryButton();
		Div postWeighInKitButton = createPostWeighInButton();
		Button postWeighInTemplateSelection = new Button(
		        Translator.translate("Documents.SelectTemplates"), VaadinIcon.COG.create(), event -> postWeighInTemplateSelection());
		postWeighInTemplateSelection.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

		this.topBar.getStyle().set("flex", "100 1");
		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);

		if (this.documentPage) {
			FlexLayout buttons = new FlexLayout(
			        new NativeLabel(Translator.translate("Documents.Registration")),
			        categoriesListButton, bwButton, teamsListButton, spacer(), registrationTemplateSelection,
			        createRule(),
			        new NativeLabel(Translator.translate("Documents.StartBook")),
			        startListButton, scheduleButton, officialSchedule, checkInButton, spacer(), competitionTemplateSelection,
			        createRule(),
			        new NativeLabel(Translator.translate("Documents.PreWeighIn")),
			        cardsButton, weighInSummaryButton, spacer(), preWeighInKitButton, spacer(), preWeighInTemplateSelection,
			        createRule(),
			        new NativeLabel(Translator.translate("Documents.PostWeighIn")),
			        introductionButton, emptyProtocolButton, juryButton, spacer(), postWeighInKitButton, spacer(), postWeighInTemplateSelection);
			buttons.getStyle().set("flex-wrap", "wrap");
			buttons.getStyle().set("gap", "1ex");
			buttons.getStyle().set("margin-left", "5em");
			buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
			this.topBar.add(buttons);
		}

		return this.topBar;
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
		if (this.documentPage) {
			return Translator.translate("Documents.Title");
		}
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

	@SuppressWarnings("unchecked")
	protected List<Athlete> athletesFindAll(boolean sessionOrder) {
		List<Athlete> found = participationFindAll();
		// for cards and starting lists we only want the actual athlete, without duplicates
		Set<Athlete> regCatAthletes = found.stream().map(pa -> ((PAthlete) pa)._getAthlete())
		        .collect(Collectors.toSet());

		// we also need athletes with no participations (implies no category)
		List<Athlete> noCat = AthleteRepository.findAthletesNoCategory();
		List<Athlete> found2 = filterAthletes(noCat);
		regCatAthletes.addAll(found2);

		// sort
		List<Athlete> regCatAthletesList = new ArrayList<>(regCatAthletes);
		if (sessionOrder) {
			Collections.sort(regCatAthletesList, RegistrationOrderComparator.athleteSessionRegistrationOrderComparator);
		} else {
			AthleteSorter.registrationOrder(regCatAthletesList);
		}

		updateURLLocations();
		return regCatAthletesList;
	}

	protected List<Athlete> participationFindAll() {
		List<Athlete> athletes = AgeGroupRepository.allPAthletesForAgeGroupAgeDivision(null, null);
		List<Athlete> found = filterAthletes(athletes);
		return found;
	}

	private KitElement checkKit(String id, PreCompetitionTemplates templateEnum, BiConsumer<Throwable, String> errorProcessor,
	        Supplier<JXLSWorkbookStreamSource> writerFactory) {
		try {
			String resourceFolder = templateEnum.folder;
			resourceFolder = resourceFolder.endsWith("/") ? resourceFolder : (resourceFolder + "/");
			String template = templateEnum.templateFileNameSupplier.get();
			String templateName = resourceFolder + template;
			Path isp = ResourceWalker.getFileOrResourcePath(templateName);
			String ext = FileNameUtils.getExtension(isp);
			return new KitElement(id, templateName, ext, isp, 1, writerFactory);
		} catch (FileNotFoundException e1) {
			if (errorProcessor != null) {
				errorProcessor.accept(e1, templateEnum.name());
				throw new StopProcessingException(templateEnum.name(), e1);
			}
			return null;
		} catch (Exception e2) {
			errorProcessor.accept(e2, e2.getMessage());
			throw new StopProcessingException(templateEnum.name(), e2);
		}
	}

	private void checkNoSelection(List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		if (selectedItems == null || selectedItems.size() == 0) {
			Exception e = new Exception("NoAthletes");
			errorProcessor.accept(e, e.getMessage());
			throw new StopProcessingException(e.getMessage(), e);
		}
	}

	private void competitionTemplateSelection() {
		Dialog dialog = new Dialog();
		dialog.add(new TemplateSelectionFormFactory().competitionTemplateSelectionForm());
		dialog.open();
	}
	
	private void registrationTemplateSelection() {
		Dialog dialog = new Dialog();
		dialog.add(new TemplateSelectionFormFactory().registrationTemplateSelectionForm());
		dialog.open();
	}

	private Div createCardsButton() {
		Div localDirZipDiv = null;
		UI ui = UI.getCurrent();
		localDirZipDiv = DownloadButtonFactory.createDynamicDownloadButton(
		        () -> stripSuffix(PreCompetitionTemplates.CARDS.templateFileNameSupplier.get()),
		        Translator.translate("AthleteCards"),
		        () -> {
			        List<KitElement> elements = prepareCardsKit(getSortedSelection(), (e, m) -> notifyError(e, ui, m));
			        return zipOrExcelInputStream(ui, elements);
		        },
		        () -> {
			        return (getSortedSelection().size() > 1 ? ".zip" : PreCompetitionTemplates.CARDS.extension);
		        });
		Button b = (Button) localDirZipDiv.getChildren().findFirst().get();
		b.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		return localDirZipDiv;
	}

	private Button createCheckInButton() {
		String resourceDirectoryLocation = "/templates/checkin";
		String title = Translator.translate(PreCompetitionTemplates.CHECKIN.name());
		JXLSDownloader startingListFactory = new JXLSDownloader(
		        () -> {
			        JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			        // group may have been edited since the page was loaded
			        startingXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        startingXlsWriter.setPostProcessor(null);
			        List<Athlete> athletesFindAll = athletesFindAll(true);
			        startingXlsWriter.setSortedAthletes(athletesFindAll);
			        return startingXlsWriter;
		        },
		        resourceDirectoryLocation,
		        Competition::getCheckInTemplateFileName,
		        Competition::setCheckInTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return startingListFactory.createDownloadButton();
	}

	private Div createEmptyProtocolButton() {
		Div localDirZipDiv = null;
		UI ui = UI.getCurrent();
		localDirZipDiv = DownloadButtonFactory.createDynamicDownloadButton(
		        () -> stripSuffix(Competition.getCurrent().getEmptyProtocolTemplateFileName()),
		        Translator.translate(PreCompetitionTemplates.EMPTY_PROTOCOL.name()),
		        () -> {
			        List<KitElement> elements = prepareEmptyProtocolKit(getSortedSelection(), (e, m) -> notifyError(e, ui, m));
			        return zipOrExcelInputStream(ui, elements);
		        },
		        () -> {
			        return (getSortedSelection().size() > 1 ? ".zip" : PreCompetitionTemplates.EMPTY_PROTOCOL.extension);
		        });
		return localDirZipDiv;
	}

	private Button createFullScheduleButton() {
		String resourceDirectoryLocation = "/templates/schedule";
		String title = Translator.translate(PreCompetitionTemplates.SCHEDULE.name());

		JXLSDownloader startingListFactory = new JXLSDownloader(
		        () -> {
			        JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			        // group may have been edited since the page was loaded
			        startingXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        List<Athlete> athletesFindAll = athletesFindAll(true);
			        startingXlsWriter.setSortedAthletes(athletesFindAll);

			        String tn = Competition.getCurrent().getComputedStartListTemplateFileName();
			        if (/* Config.getCurrent().featureSwitch("usaw") && */tn.equals("Schedule.xlsx")) {
				        startingXlsWriter.setPostProcessor((w) -> fixMerges(w, 4, List.of(1, 2)));
			        } else {
				        startingXlsWriter.setPostProcessor(null);
			        }

			        return startingXlsWriter;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedStartListTemplateFileName,
		        Competition::setStartListTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return startingListFactory.createDownloadButton();
	}

	private Button createFullStartListButton() {
		String resourceDirectoryLocation = "/templates/start";
		String title = Translator.translate("StartingList");

		JXLSDownloader startingListFactory = new JXLSDownloader(
		        () -> {
			        JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			        // group may have been edited since the page was loaded
			        startingXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        List<Athlete> athletesFindAll = athletesFindAll(true);
			        startingXlsWriter.setSortedAthletes(athletesFindAll);

			        String tn = Competition.getCurrent().getComputedStartListTemplateFileName();
			        if (Config.getCurrent().featureSwitch("usaw") && tn.equals("Schedule.xlsx")) {
				        startingXlsWriter.setPostProcessor((w) -> fixMerges(w, 4, List.of(1, 2)));
			        } else {
				        startingXlsWriter.setPostProcessor(null);
			        }

			        return startingXlsWriter;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedStartListTemplateFileName,
		        Competition::setStartListTemplateFileName,
		        title,
		        Translator.translate("Download"));
		Button b = startingListFactory.createDownloadButton();
		b.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		return b;
	}

	/**
	 * The columns of the crudGrid
	 *
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	private GridCrud<Group> createGrid(OwlcmsCrudFormFactory<Group> crudFormFactory) {
		Grid<Group> grid = new Grid<>(Group.class, false);
		this.crud = new GroupGrid(Group.class, new OwlcmsGridLayout(Group.class), crudFormFactory, grid);
		grid.getThemeNames().add("row-stripes");
		grid.addColumn(Group::getName).setHeader(Translator.translate("Name")).setComparator(Group::compareTo).setAutoWidth(true);
		grid.addColumn(Group::getDescription).setHeader(Translator.translate("Group.Description")).setAutoWidth(true);
		grid.addColumn(Group::size).setHeader(Translator.translate("GroupSize")).setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn(LocalDateTimeField.getRenderer(Group::getWeighInTime, this.getLocale()))
		        .setHeader(Translator.translate("WeighInTime")).setComparator(Group::compareToWeighIn);
		grid.addColumn(LocalDateTimeField.getRenderer(Group::getCompetitionTime, this.getLocale()))
		        .setHeader(Translator.translate("StartTime"));
		grid.addColumn(Group::getPlatform).setHeader(Translator.translate("Platform")).setTextAlign(ColumnTextAlign.CENTER);
		String translation = Translator.translate("EditAthletes");
		//int tSize = translation.length();
		grid.addColumn(new ComponentRenderer<>(p -> {
			Button editDetails = new Button(Translator.translate("Sessions.EditDetails"));
			editDetails.addThemeVariants(ButtonVariant.LUMO_SMALL);
			Button technical = openInNewTab(RegistrationContent.class, translation, p != null ? p.getName() : "?");
			// prevent grid row selection from triggering
			technical.getElement().addEventListener("click", ignore -> {
			}).addEventData("event.stopPropagation()");
			technical.addThemeVariants(ButtonVariant.LUMO_SMALL);
			return new HorizontalLayout(editDetails,technical);
		})).setHeader("").setAutoWidth(true);
		
		for (Column<Group> c : grid.getColumns()) {
			c.setResizable(true);
		}

		this.crud.setCrudListener(this);
		this.crud.setClickRowToUpdate(true);
		grid.setSelectionMode(SelectionMode.MULTI);
		return this.crud;
	}
	
	
	protected Button createBWButton() {
		String resourceDirectoryLocation = "/templates/bwStart";
		String title = Translator.translate("BodyWeightCategories");

		JXLSDownloader startingListFactory = new JXLSDownloader(
		        () -> {
			        JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			        // group may have been edited since the page was loaded
			        startingXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        startingXlsWriter.setSortedAthletes(AthleteSorter.registrationBWCopy(athletesFindAll(false)));
			        startingXlsWriter.createAgeGroupColumns(10, 7);
			        return startingXlsWriter;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedStartListTemplateFileName,
		        Competition::setStartListTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return startingListFactory.createDownloadButton();
	}
	
	protected Button createTeamsListButton() {
		String resourceDirectoryLocation = "/templates/teams";
		String title = Translator.translate("StartingList.Teams");

		JXLSDownloader startingListFactory = new JXLSDownloader(
		        () -> {
			        JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			        // group may have been edited since the page was loaded
			        startingXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        // findAll();
			        // List<Athlete> sortedAthletes = startingXlsWriter.getSortedAthletes();
			        startingXlsWriter.setSortedAthletes(AthleteSorter.registrationOrderCopy(participationFindAll()));
			        startingXlsWriter.createTeamColumns(9, 6);
			        return startingXlsWriter;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedTeamsListTemplateFileName,
		        Competition::setTeamsListTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return startingListFactory.createDownloadButton();
	}

	protected Button createCategoriesListButton() {
		String resourceDirectoryLocation = "/templates/categories";
		String title = Translator.translate("StartingList.Categories");

		JXLSDownloader startingListFactory = new JXLSDownloader(
		        () -> {
			        JXLSCategoriesListDocs categoriesXlsWriter = new JXLSCategoriesListDocs();
			        // group may have been edited since the page was loaded
			        categoriesXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        var athletes = participationFindAll();
			        AthleteSorter.registrationOrder(athletes);
			        categoriesXlsWriter.setSortedAthletes(athletes);
			        return categoriesXlsWriter;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedCategoriesListTemplateFileName,
		        Competition::setCategoriesListTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return startingListFactory.createDownloadButton();
	}
	

	private Div createIntroductionButton() {
		Div localDirZipDiv = null;
		UI ui = UI.getCurrent();
		localDirZipDiv = DownloadButtonFactory.createDynamicDownloadButton(
		        () -> stripSuffix(Competition.getCurrent().getIntroductionTemplateFileName()),
		        Translator.translate(PreCompetitionTemplates.INTRODUCTION.name()),
		        () -> {
			        List<KitElement> elements = prepareIntroductionKit(getSortedSelection(), (e, m) -> notifyError(e, ui, m));
			        return zipOrExcelInputStream(ui, elements);
		        },
		        () -> {
			        return (getSortedSelection().size() > 1 ? ".zip" : PreCompetitionTemplates.INTRODUCTION.extension);
		        });
		return localDirZipDiv;
	}

	private Div createJuryButton() {
		Div localDirZipDiv = null;
		UI ui = UI.getCurrent();
		localDirZipDiv = DownloadButtonFactory.createDynamicDownloadButton(
		        () -> stripSuffix(Competition.getCurrent().getJuryTemplateFileName()),
		        Translator.translate(PreCompetitionTemplates.JURY.name()),
		        () -> {
			        List<KitElement> elements = prepareJuryKit(getSortedSelection(), (e, m) -> notifyError(e, ui, m));
			        return zipOrExcelInputStream(ui, elements);
		        },
		        () -> {
			        return (getSortedSelection().size() > 1 ? ".zip" : PreCompetitionTemplates.JURY.extension);
		        });
		return localDirZipDiv;
	}

	private Button createOfficalsButton() {
		String resourceDirectoryLocation = "/templates/officials";
		String title = Translator.translate(PreCompetitionTemplates.OFFICIALS.name());

		JXLSDownloader startingListFactory = new JXLSDownloader(
		        () -> {
			        JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			        startingXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        startingXlsWriter.setSortedAthletes(List.of());
			        startingXlsWriter.setEmptyOk(true);
			        return startingXlsWriter;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedOfficialsListTemplateFileName,
		        Competition::setOfficialsListTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return startingListFactory.createDownloadButton();
	}

	private Div createPreWeighInButton() {
		Div localDirZipDiv = null;
		UI ui = UI.getCurrent();
		localDirZipDiv = DownloadButtonFactory.createDynamicZipDownloadButton("preWeighIn",
		        Translator.translate("Documents.Kits"),
		        () -> {
			        List<KitElement> elements = preparePreWeighInKit(getSortedSelection(), (e, m) -> notifyError(e, ui, m));
			        return zipKitToInputStream(getSortedSelection(), elements, (e, m) -> notifyError(e, ui, m), ui);
		        },
		        VaadinIcon.ARCHIVE.create());
		return localDirZipDiv;
	}

	private Div createPostWeighInButton() {
		Div localDirZipDiv = null;
		UI ui = UI.getCurrent();
		localDirZipDiv = DownloadButtonFactory.createDynamicZipDownloadButton("postWeighIn",
		        Translator.translate("Documents.Kits"),
		        () -> {
			        List<KitElement> elements = preparePostWeighInKit(getSortedSelection(), (e, m) -> notifyError(e, ui, m));
			        return zipKitToInputStream(getSortedSelection(), elements, (e, m) -> notifyError(e, ui, m), ui);
		        },
		        VaadinIcon.ARCHIVE.create());
		return localDirZipDiv;
	}

	private Hr createRule() {
		Hr hr = new Hr();
		hr.setWidthFull();
		hr.getStyle().set("margin", "0");
		hr.getStyle().set("padding", "0");
		return hr;
	}

	private Div createWeighInSummaryButton() {
		Div localDirZipDiv = null;
		UI ui = UI.getCurrent();
		localDirZipDiv = DownloadButtonFactory.createDynamicDownloadButton(
		        () -> stripSuffix(Competition.getCurrent().getWeighInFormTemplateFileName()),
		        Translator.translate("WeighinForm"),
		        () -> {
			        List<KitElement> elements = prepareWeighInKit(getSortedSelection(), (e, m) -> notifyError(e, ui, m));
			        return zipOrExcelInputStream(ui, elements);
		        },
		        () -> {
			        return (getSortedSelection().size() > 1 ? ".zip" : PreCompetitionTemplates.WEIGHIN.extension);
		        });
		return localDirZipDiv;
	}

	private void doKitElement(KitElement elem, String seq, ZipOutputStream zipOut, Group g, List<Athlete> athletes) throws IOException {
		JXLSWorkbookStreamSource cardsXlsWriter = elem.writerFactory.get();
		InputStream is = Files.newInputStream(elem.isp);
		cardsXlsWriter.setInputStream(is);
		cardsXlsWriter.setGroup(g);
		cardsXlsWriter.setSortedAthletes(athletes);
		cardsXlsWriter.setTemplateFileName(elem.name);
		InputStream in = cardsXlsWriter.createInputStream();
		String name = seq + "_" + elem.id + "_" + g.getName() + "." + elem.extension;
		ZipUtils.zipStream(in, name, false, zipOut);
	}

	private void doPrintScript(ZipOutputStream zipOut) {
		try {
			ZipUtils.zipStream(ResourceWalker.getFileOrResource("/templates/cards/print.ps1"), "print.ps1", false, zipOut);
		} catch (IOException e) {
			LoggerUtils.logError(logger, e, true);
		}
	}

	private InputStream excelKitElement(List<Group> selectedItems, List<KitElement> elements, UI ui) throws IOException {
		// always called with a single template and a single session.
		Group g = selectedItems.get(0);
		KitElement elem = elements.get(0);

		// get current version of athletes.
		List<Athlete> athletes = groupAthletes(g, true);
		Notification n = new Notification(Translator.translate("Documents.ProcessingExcel"));
		JXLSWorkbookStreamSource cardsXlsWriter = elem.writerFactory.get();
		InputStream is = Files.newInputStream(elem.isp);
		cardsXlsWriter.setInputStream(is);
		cardsXlsWriter.setGroup(g);
		cardsXlsWriter.setSortedAthletes(athletes);
		cardsXlsWriter.setTemplateFileName(elem.name);
		cardsXlsWriter.setDoneCallback((s) -> ui.access(() -> {
			n.close();
		}));
		n.setPosition(Position.TOP_END);
		ui.access(() -> {
			n.open();
		});
		InputStream in = cardsXlsWriter.createInputStream();
		return in;
	}

	private InputStream excelToInputStream(List<Group> selectedItems,
	        List<KitElement> elements, BiConsumer<Throwable, String> errorProcessor, UI ui) {
		try {
			return excelKitElement(selectedItems, elements, ui);
		} catch (Throwable e) {
			errorProcessor.accept(e, e.getMessage());
			throw new StopProcessingException(e.getMessage(), e);
		}
	}

	private List<Athlete> filterAthletes(List<Athlete> athletes) {
		Stream<Athlete> stream = athletes.stream()
		        .filter(a -> {
			        Platform platformFilterValue = getPlatform();
			        if (platformFilterValue == null) {
				        return true;
			        }
			        Platform athletePlaform = a.getGroup() != null
			                ? (a.getGroup().getPlatform() != null ? a.getGroup().getPlatform() : null)
			                : null;
			        return platformFilterValue.equals(athletePlaform);
		        })
		        .map(a -> {
			        if (a.getTeam() == null) {
				        a.setTeam("");
			        }
			        return a;
		        });

		List<Athlete> found = stream.sorted(
		        groupCategoryComparator())
		        .collect(Collectors.toList());
		return found;
	}

	private void fixMerges(Workbook workbook, Integer startRowNum, List<Integer> columns) {
		Sheet sheet = workbook.getSheetAt(0);
		int firstRow = 0;
		boolean isMerging = false;
		CellStyle style = null;

		for (int colA : columns) {
			isMerging = false;
			firstRow = 0;
			style = null;

			int col = colA - 1;
			for (Row row : sheet) {
				Cell cell = row.getCell(col);
				// logger.debug("cell {}{} {}", (char)('A'+col), row.getRowNum()+1, firstRow);
				if (row.getRowNum() + 1 < startRowNum) {
					// logger.debug("cellB {}{}",(char)('A'+col), row.getRowNum()+1);
					continue;
				}

				if (cell != null && cell.getCellType() != CellType.BLANK) {
					if (isMerging) {
						logger.debug("**** {}{}: merging from {}{}", (char) ('A' + col), row.getRowNum() + 1,
						        (char) ('A' + col), firstRow + 1);
						CellRangeAddress region = new CellRangeAddress(firstRow, row.getRowNum() - 1, col, col);
						sheet.addMergedRegion(region);
						// Apply the captured style to the first cell of the merged region
						Cell cell2 = sheet.getRow(firstRow).getCell(col);
						style.setBorderBottom(BorderStyle.HAIR);
						cell2.setCellStyle(style);
						isMerging = false;

						// start a new merge
						logger.debug("**** {}{}: capturing style", (char) ('A' + col), row.getRowNum() + 1, isMerging);
						firstRow = row.getRowNum();
						style = cell.getCellStyle(); // capture the style
						isMerging = true;
					} else {
						logger.debug("**** {}{}: capturing style", (char) ('A' + col), row.getRowNum() + 1, isMerging);
						firstRow = row.getRowNum();
						style = cell.getCellStyle(); // capture the style
						isMerging = true;
					}
				}
			}
			// Merge the last region if the last cell(s) is/are non-empty
			if (isMerging) {
				logger.debug("**** {}{}: merging bottom from {}{}", (char) ('A' + col), sheet.getLastRowNum() + 1,
				        (char) ('A' + col), firstRow + 1);
				CellRangeAddress region = new CellRangeAddress(firstRow, sheet.getLastRowNum(), col, col);
				sheet.addMergedRegion(region);
				Cell cell22 = sheet.getRow(firstRow).getCell(col);
				style.setBorderBottom(BorderStyle.HAIR);
				cell22.setCellStyle(style);
			}
		}
	}

	private Platform getPlatform() {
		return null;
	}

	private List<Group> getSortedSelection() {
		return this.crud.getSelectedItems().stream().sorted(Group.groupWeighinTimeComparator).toList();
	}

	private <C extends Component> String getWindowOpenerFromClass(Class<C> targetClass,
	        String parameter) {
		return "window.open('" + URLUtils.getUrlFromTargetClass(targetClass) + "?group="
		        + URLEncoder.encode(parameter, StandardCharsets.UTF_8)
		        + "','" + targetClass.getSimpleName() + "')";
	}

	private List<Athlete> groupAthletes(Group g, boolean sessionOrder) {
		List<Athlete> regCatAthletesList = new ArrayList<>(g.getAthletes());
		if (sessionOrder) {
			Collections.sort(regCatAthletesList, RegistrationOrderComparator.athleteSessionRegistrationOrderComparator);
		} else {
			AthleteSorter.registrationOrder(regCatAthletesList);
		}
		return regCatAthletesList;
	}

	private Comparator<? super Athlete> groupCategoryComparator() {
		Comparator<? super Athlete> groupCategoryComparator = (a1, a2) -> {
			int compare;
			compare = ObjectUtils.compare(a1.getGroup(), a2.getGroup(), true);
			if (compare != 0) {
				logComparison(compare, a1, a2, "group");
				return compare;
			}

			// deal with athletes not fully registered or not eligible to any category.
			Participation mainRankings1 = a1.getMainRankings() != null ? a1.getMainRankings() : null;
			Participation mainRankings2 = a2.getMainRankings() != null ? a2.getMainRankings() : null;
			Category category1 = mainRankings1 != null ? mainRankings1.getCategory() : null;
			Category category2 = mainRankings2 != null ? mainRankings2.getCategory() : null;
			compare = ObjectUtils.compare(category1, category2, true);
			if (compare != 0) {
				logComparison(compare, a1, a2, "mainCategory");
				return compare;
			}

			compare = ObjectUtils.compare(a1.getEntryTotal(), a2.getEntryTotal());
			logComparison(compare, a1, a2, "entryTotal");
			return -compare;
		};
		return groupCategoryComparator;
	}

	private void logComparison(int compare, Athlete a1, Athlete a2, String string) {
		if (compare == 0) {
			// logger.trace("({}) {} = {}", string, athleteLog(a1), athleteLog(a2));
		} else if (compare < 0) {
			// logger.trace("({}) {} < {}", string, athleteLog(a1), athleteLog(a2));
		} else if (compare > 0) {
			// logger.trace("({}) {} > {}", string, athleteLog(a1), athleteLog(a2));
		}
	}

	private void notifyError(Throwable e, UI ui, final String m) {
		if (m.equals("NoAthletes")) {
			this.getUI().get().access(() -> {
				Notification notif = new Notification();
				notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
				notif.setPosition(Position.TOP_STRETCH);
				notif.setDuration(5000);
				Div div = new Div(Translator.translate("Documents.NoSession"));
				div.getStyle().set("font-size","140%");
				notif.add(div);
				notif.open();
			});
		} else {
			this.getUI().get().access(() -> {
				Notification notif = new Notification();
				notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
				notif.setPosition(Position.TOP_STRETCH);
				notif.setDuration(5000);
				Div div = new Div(Translator.translate("Documents.NoTemplate"));
				div.getStyle().set("font-size","140%");
				notif.add(div);
				notif.open();
				
				Dialog dialog = new Dialog();
				PreCompetitionTemplates templateKind = PreCompetitionTemplates.valueOf(m);
				dialog.add(new TemplateSelectionFormFactory().templateSelectionForm(m, templateKind));
				dialog.open();
			});
		}
	}

	private <C extends Component> Button openInNewTab(Class<C> targetClass,
	        String label, String parameter) {
		Button button = new Button(label);
		button.getElement().setAttribute("onClick", getWindowOpenerFromClass(targetClass, parameter != null ? parameter : "-"));
		return button;
	}

	private void postWeighInTemplateSelection() {
		Dialog dialog = new Dialog();
		dialog.add(new TemplateSelectionFormFactory().postWeighInTemplateSelectionForm());
		dialog.open();
	}

	private List<KitElement> prepareCardsKit(List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		checkNoSelection(selectedItems, errorProcessor);
		List<KitElement> elements = new ArrayList<>();
		elements.add(
		        checkKit("cards",
		                PreCompetitionTemplates.CARDS,
		                errorProcessor,
		                () -> new JXLSCardsWeighIn()));
		return elements;
	}

	private List<KitElement> prepareEmptyProtocolKit(List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		checkNoSelection(selectedItems, errorProcessor);
		List<KitElement> elements = new ArrayList<>();
		elements.add(
		        checkKit("emptyProtocol",
		                PreCompetitionTemplates.EMPTY_PROTOCOL,
		                errorProcessor,
		                () -> new JXLSResultSheet()));
		return elements;
	}

	private List<KitElement> prepareIntroductionKit(List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		checkNoSelection(selectedItems, errorProcessor);
		List<KitElement> elements = new ArrayList<>();
		elements.add(
		        checkKit("introduction",
		                PreCompetitionTemplates.INTRODUCTION,
		                errorProcessor,
		                () -> new JXLSCardsWeighIn()));
		return elements;
	}

	private List<KitElement> prepareJuryKit(List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		checkNoSelection(selectedItems, errorProcessor);
		List<KitElement> elements = new ArrayList<>();
		elements.add(
		        checkKit("jury",
		                PreCompetitionTemplates.JURY,
		                errorProcessor,
		                () -> new JXLSJurySheet()));
		return elements;
	}

	private List<KitElement> preparePreWeighInKit(List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		checkNoSelection(selectedItems, errorProcessor);
		List<KitElement> elements = new ArrayList<>();
		KitElement kit = checkKit("weighin",
		        PreCompetitionTemplates.WEIGHIN,
		        null, // no error processor - ignore this item if no template
		        () -> new JXLSCardsDocs());
		if (kit != null) {
			elements.add(kit);
		}

		KitElement kit2 = checkKit("cards",
		        PreCompetitionTemplates.CARDS,
		        null, // no error processor - ignore this item if no template
		        () -> new JXLSCardsWeighIn());
		if (kit2 != null) {
			elements.add(kit2);
		}
		return elements;
	}

	private List<KitElement> preparePostWeighInKit(List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		checkNoSelection(selectedItems, errorProcessor);
		List<KitElement> elements = new ArrayList<>();
		KitElement kit = checkKit("introduction",
		        PreCompetitionTemplates.INTRODUCTION,
		        null, // no error processor - ignore this item if no template
		        () -> new JXLSCardsWeighIn());
		if (kit != null) {
			elements.add(kit);
		}

		KitElement kit2 = checkKit("emptyProtocol",
                PreCompetitionTemplates.EMPTY_PROTOCOL,
                null, // no error processor - ignore this item if no template
                () -> new JXLSResultSheet());
		if (kit2 != null) {
			elements.add(kit2);
		}
		
		KitElement kit3 = checkKit("jury",
                PreCompetitionTemplates.JURY,
                null, // no error processor - ignore this item if no template
                () -> new JXLSJurySheet());
		if (kit3 != null) {
			elements.add(kit3);
		}
		return elements;
	}

	private List<KitElement> prepareWeighInKit(List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		checkNoSelection(selectedItems, errorProcessor);
		List<KitElement> elements = new ArrayList<>();
		elements.add(
		        checkKit("weighin",
		                PreCompetitionTemplates.WEIGHIN,
		                errorProcessor,
		                () -> new JXLSCardsDocs()));
		return elements;
	}

	private void preWeighInTemplateSelection() {
		Dialog dialog = new Dialog();
		dialog.add(new TemplateSelectionFormFactory().preWeighInTemplateSelectionForm());
		dialog.open();
	}

	private Html spacer() {
		return new Html("<span>&nbsp;&nbsp;<span>");
	}

	private String stripSuffix(String templateName) {
		if (templateName == null) {
			// defensive, will not be used due to prior error check.
			return "undefined";
		}
		templateName = templateName.replaceFirst("[\\-_]LETTER", "");
		templateName = templateName.replaceFirst("[\\-_]LLEGAL", "");
		templateName = templateName.replaceFirst("[\\-_]A4", "");
		// remove longer first
		templateName = templateName.replace(".xlsx", "");
		templateName = templateName.replace(".xls", "");
		return templateName;
	}

	private void updateURLLocations() {
	}

	private ZipOutputStream zipKit(List<Group> selectedItems, List<KitElement> elements, PipedOutputStream os) throws IOException {
		int i = 1;
		ZipOutputStream zipOut = null;
		try {
			zipOut = new ZipOutputStream(os);
			doPrintScript(zipOut);

			for (Group g : selectedItems) {
				// get current version of athletes.
				List<Athlete> athletes = groupAthletes(g, true);

				for (KitElement elem : elements) {
					String seq = String.format("%02d", i);
					doKitElement(elem, seq, zipOut, g, athletes);
					i++;
				}
			}
			return zipOut;
		} finally {
			if (zipOut != null) {
				zipOut.finish();
				zipOut.close();
			}
		}
	}

	private InputStream zipKitToInputStream(List<Group> selectedItems, List<KitElement> elements, BiConsumer<Throwable, String> errorProcessor, UI ui) {
		PipedOutputStream out;
		PipedInputStream in;
		try {
			out = new PipedOutputStream();
			in = new PipedInputStream(out);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		Notification n = new Notification(Translator.translate("Documents.ProcessingZip"));
		n.setPosition(Position.TOP_END);
		ui.access(() -> {
			n.open();
		});
		new Thread(() -> {
			try {
				zipKitToOutputStream(selectedItems, elements, errorProcessor, out);
			} finally {
				ui.access(() -> {
					n.close();
				});
			}
		}).start();
		return in;
	}

	private void zipKitToOutputStream(List<Group> selectedItems, List<KitElement> elements, BiConsumer<Throwable, String> errorProcessor,
	        PipedOutputStream out) {
		try {
			zipKit(selectedItems, elements, out);
			out.flush();
			out.close();
		} catch (Throwable e) {
			errorProcessor.accept(e, e.getMessage());
		}
	}

	private InputStream zipOrExcelInputStream(UI ui, List<KitElement> elements) {
		InputStream z;
		if (getSortedSelection().size() > 1) {
			z = zipKitToInputStream(getSortedSelection(), elements, (e, m) -> notifyError(e, ui, m), ui);
		} else {
			z = excelToInputStream(getSortedSelection(), elements, (e, m) -> notifyError(e, ui, m), ui);
		}
		return z;

	}

}
