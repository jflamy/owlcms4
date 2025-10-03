/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
// com.vaadin.flow.server.InputStreamFactory; not used directly here

import app.owlcms.apputils.queryparameters.BaseContent;
// LazyDownloadButton and atomic refs were used in older implementation; removed after refactor
import app.owlcms.components.fields.LocalDateTimeField;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.RegistrationOrderComparator;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSessionThreadLocal;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.data.config.Config;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.servlet.StopProcessingException;
import app.owlcms.spreadsheet.JXLSCardsDocs;
import app.owlcms.spreadsheet.JXLSCategoriesListDocs;
import app.owlcms.spreadsheet.JXLSJurySheet;
import app.owlcms.spreadsheet.JXLSResultSheet;
import app.owlcms.spreadsheet.JXLSStartingListDocs;
import app.owlcms.spreadsheet.JXLSWeighInSheet;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.Resource;
import app.owlcms.utils.ZipUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class GroupContent.
 *
 * Defines the toolbar and the table for editing data on sessions.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/documents", layout = OwlcmsLayout.class)
public class DocumentsContent extends BaseContent implements CrudListener<Group>, OwlcmsContent {

	// Use top-level KitElement record (moved to KitElement.java)

	final static Logger logger = (Logger) LoggerFactory.getLogger(DocumentsContent.class);

	static {
		logger.setLevel(Level.INFO);
	}
	private final DocumentsPrecheckService precheckService = new DocumentsPrecheckService();
	boolean documentPage;
	private DocumentsGrid crud;
	private OwlcmsCrudFormFactory<Group> editingFormFactory;
	private OwlcmsLayout routerLayout;
	private FlexLayout topBar;

	// Default error processor used by kit-definition helpers. Uses notifyError with current UI.
	private final BiConsumer<Throwable, String> defaultErrorProcessor = (e, m) -> notifyError(e, UI.getCurrent(), m);

	/**
	 * Instantiates the Group crudGrid.
	 */
	public DocumentsContent() {
		this.editingFormFactory = null; // new SessionEditingFormFactory(Group.class, this);
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
	public FlexLayout createMenuArea() {
		this.topBar = new FlexLayout();

		Div bwButton = createBodyweightButton();
		Div categoriesListButton = createCategoriesButton();
		Div teamsListButton = createTeamsButton();

		Div startListButton = createStartListButton();
		Div scheduleButton = createFullScheduleButton();
		Div officialSchedule = createOfficialsButton();
		Div checkInButton = createCheckinButton();

		Div cardsButton = createCardsButton();
		Div weighInButton = createWeighInButton();
		Div preWeighInKitButton = createPreWeighInButton();
		Div credentialsButton = createCredentialsButton();

		Div introductionButton = createIntroductionButton();
		Div emptyProtocolButton = createEmptyProtocolButton();
		Div juryButton = createJuryButton();
		Div postWeighInKitButton = createPostWeighInButton();

		this.topBar.getStyle().set("flex", "100 1");
		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);

		if (this.documentPage) {
			FlexLayout buttons = new FlexLayout(
			        new NativeLabel(Translator.translate("Documents.Registration")),
			        categoriesListButton, bwButton, teamsListButton,
			        createRule(),
			        new NativeLabel(Translator.translate("Documents.StartBook")),
			        startListButton, scheduleButton, officialSchedule, checkInButton, credentialsButton,
					createRule(),
					new NativeLabel(Translator.translate("Documents.PreWeighIn")),
					cardsButton, weighInButton, spacer(), preWeighInKitButton,
			        createRule(),
			        new NativeLabel(Translator.translate("Documents.PostWeighIn")),
			        introductionButton, emptyProtocolButton, juryButton, spacer(), postWeighInKitButton
			);
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
	private List<Athlete> athletesFindAll(boolean sessionOrder) {
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

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		if (this.documentPage) {
			this.crud.getAddButton().getStyle().set("display", "none");
		}
	}

	protected List<Athlete> participationFindAll() {
		List<Athlete> athletes = AgeGroupRepository.allPAthletesForAgeGroupAgeDivision(null, null);
		List<Athlete> found = filterAthletes(athletes);
		return found;
	}

	private KitElement defineKit(String id, PreCompetitionTemplate templateEnum,
	        BiFunction<List<Athlete>, Group, Optional<Exception>> explicitPreCheck,
	        BiFunction<List<Athlete>, Group, JXLSWorkbookStreamSource> writerFactory) {
		try {
			// logger removed
			String resourceFolder = templateEnum.folder + "/";
			resourceFolder = resourceFolder.endsWith("/") ? resourceFolder : (resourceFolder + "/");
			String template = templateEnum.templateFileNameSupplier.get();
			String templateName = template == null ? null : (resourceFolder + template);
			// Do not check for template existence here; defer existence checks to the
			// dialog / precheck service so this helper only composes the KitElement.
			Path isp = null;
			String ext = FilenameUtils.getExtension(template == null ? "" : template);

			// The precheck should be provided explicitly by callers. The default
			// behavior is obtained via defaultScopePrecheckFor(templateEnum)
			BiFunction<List<Athlete>, Group, Optional<Exception>> pre = explicitPreCheck;

			// Determine processing message based on template type
			Supplier<String> processingMessageSupplier = () -> {
				if (templateEnum == PreCompetitionTemplate.CARDS || templateEnum == PreCompetitionTemplate.START_LIST) {
					return "LongProcessing";
				} else {
					return "Processing";
				}
			};

			Supplier<List<Resource>> availableTemplatesSupplier = () -> computeResourceList(templateEnum.folder, (f) -> matchExtension(templateEnum, f));
			Supplier<String> selectedTemplateSupplier = () -> templateEnum.templateFileNameSupplier.get();

	    KitElement kitElement = new KitElement(id, templateEnum, templateName, ext, isp, 1, writerFactory, pre, processingMessageSupplier,
		    availableTemplatesSupplier, selectedTemplateSupplier);
			return kitElement;
		} catch (Exception e2) {
			logger.error("Unexpected exception: {}", e2.toString());
			defaultErrorProcessor.accept(e2, e2.getMessage());
			throw new StopProcessingException(templateEnum.name(), e2);
		}
	}

	// Provide the lightweight default preCheck used historically when callers passed null.
	private BiFunction<List<Athlete>, Group, Optional<Exception>> defaultScopePrecheckFor(PreCompetitionTemplate templateEnum) {
		return (a, g) -> runDefaultScopePrecheck(templateEnum, a, g, false);
	}

	// Cards scope precheck isolated as a class-level variable so it can be reused by credential variants
	private final BiFunction<List<Athlete>, Group, Optional<Exception>> cardsScopePrecheck = (a, g) -> {
		if (g != null) {
			if (a == null || a.isEmpty()) {
				return Optional.of(new NoAthletesException());
			}
			return Optional.empty();
		} else {
			int total = athletesFindAll(true).size();
			if (total == 0) {
				return Optional.of(new NoAthletesException());
			}
			if (total >= 100) {
				return Optional.of(new TooManyAthletesException());
			}
			// Check reasonable selection: if no session selected and too many athletes overall
			if (total > 150) {
				return Optional.of(new TooManyAthletesException());
			}
			return Optional.empty();
		}
	};

	// Variant of default scope precheck that allows no session to be selected. Used for
	// templates that make sense for the whole competition (categories, bodyweight,
	// teams) where a global report may be generated without selecting a specific
	// session/group.
	private BiFunction<List<Athlete>, Group, Optional<Exception>> defaultScopePrecheckAllowNoSelectionFor(PreCompetitionTemplate templateEnum) {
		return (a, g) -> runDefaultScopePrecheck(templateEnum, a, g, true);
	}

	private List<Resource> computeResourceList(String resourceDirectoryLocation, Predicate<String> nameFilter) {
		List<Resource> resourceList = new ResourceWalker().getResourceList(
		        resourceDirectoryLocation,
		        ResourceWalker::relativeName,
		        nameFilter,
		        OwlcmsSession.getLocale(),
		        Config.getCurrent().isLocalTemplatesOnly());
		List<Resource> prioritizedList = xlsxPriority(resourceList);
		return prioritizedList;
	}

	public boolean matchExtension(PreCompetitionTemplate template, String f) {
		if (template.extension.equals(".xlsx")) {
			return (f.endsWith(".xlsx") || f.endsWith(".xlsm"));
		} else {
			return f.endsWith(template.extension);
		}
	}

	private List<Resource> xlsxPriority(List<Resource> resourceList) {
		resourceList.sort(Comparator.comparing(Resource::getFileName).reversed());

		ArrayList<Resource> proritizedList = new ArrayList<>();
		String prevName = "";
		for (Resource r : resourceList) {
			String curName = r.getFileName();
			if (curName.endsWith(".xlsx") || curName.endsWith(".xlsm") || (curName.endsWith(".xls") && !prevName.contentEquals(curName + "x"))) {
				proritizedList.add(r);
			}
			prevName = curName;
		}
		proritizedList.sort(Comparator.comparing(Resource::getFileName));
		return proritizedList;
	}

	/**
	 * Shared logic for default scope prechecks. If allowNoSelection is false, a missing group (g==null) results in a NoSession exception; otherwise group may be
	 * null.
	 */
	private Optional<Exception> runDefaultScopePrecheck(PreCompetitionTemplate templateEnum, List<Athlete> a, Group g, boolean allowNoSelection) {
		try {
			if (!allowNoSelection && g == null) {
				return Optional.of(new NoSessionException());
			}

			int incomingCount = a == null ? 0 : a.size();
			String sampleIds = "";
			if (a != null && !a.isEmpty()) {
				sampleIds = a.stream().limit(10).map(ath -> String.valueOf(ath.getId())).collect(Collectors.joining(","));
			}
			String groupInfo = (g == null) ? "<no-group>" : (g.getId() + ":" + g.getName());
			Optional<Exception> outcome = Optional.empty();

			if (g != null) {
				if (incomingCount == 0) {
					outcome = Optional.of(new StopProcessingException("NoAthletes", new RuntimeException(Translator.translate("NoAthletes"))));
				}
			}

			String resultText = outcome.isEmpty() ? "OK" : (outcome.get().getMessage() == null ? outcome.get().toString() : outcome.get().getMessage());
			logger.debug("scopePrecheck %s for template=%s received: incomingCount=%d, sampleIds=[%s], group=%s, resolvedCount=%d, outcome=%s",
			        allowNoSelection ? "allow-no-selection" : "default",
			        templateEnum.name(), incomingCount, sampleIds, groupInfo, incomingCount, resultText);
			return outcome;
		} catch (Throwable t) {
			LoggerUtils.logError(logger, t, true);
			logger.debug("scopePrecheck %s for template=%s threw exception: %s", allowNoSelection ? "allow-no-selection" : "default", templateEnum.name(),
			        t.toString());
			return Optional.of(new Exception(t));
		}
	}

	private Div createBodyweightButton() {
		return createDocumentDownloadButton(
		        PreCompetitionTemplate.BY_BODYWEIGHT,
		        null,
		        () -> prepareBodyweight(PreCompetitionTemplate.BY_BODYWEIGHT, getSortedSelection()),
		        false);
	}

	private Div createCardsButton() {
		return createDocumentDownloadButton(
		        PreCompetitionTemplate.CARDS,
		        null,
		        () -> prepareCards(PreCompetitionTemplate.CARDS, getSortedSelection()),
		        true);
	}

	private Div createCategoriesButton() {
		return createDocumentDownloadButton(
		        PreCompetitionTemplate.BY_CATEGORY,
		        null,
		        () -> prepareCategories(PreCompetitionTemplate.BY_CATEGORY, getSortedSelection()),
		        false);
	}

	private Div createCheckinButton() {
		return createDocumentDownloadButton(
		        PreCompetitionTemplate.CHECKIN,
		        null,
		        () -> prepareCheckin(PreCompetitionTemplate.CHECKIN, getSortedSelection()),
		        false);
	}

	/**
	 * Delegates to DocumentDownloadDialog.createDoItButtonForKits to centralize wiring. The caller must provide the currently-sorted selection so the method
	 * does not call getSortedSelection() itself (avoids duplicate preparation/work).
	 */
	private Component createDoItButtonForKits(List<KitElement> kit, DocumentDownloadDialog dialog, Supplier<List<Group>> selectedSessionsSupplier,
	        Supplier<List<Athlete>> computeAthletesSupplier) {
		return createDoItButtonForKits(kit, dialog, selectedSessionsSupplier, computeAthletesSupplier, null);
	}

	/**
	 * Delegates to DocumentDownloadDialog.createDoItButtonForKits to centralize wiring. The caller must provide the currently-sorted selection so the method
	 * does not call getSortedSelection() itself (avoids duplicate preparation/work).
	 */
	private Component createDoItButtonForKits(List<KitElement> kit, DocumentDownloadDialog dialog, Supplier<List<Group>> selectedSessionsSupplier,
	        Supplier<List<Athlete>> computeAthletesSupplier, Supplier<String> zipBaseOverride) {
		// Build suppliers used by the dialog helper
		Supplier<String> baseFile = () -> {
			if (kit == null || kit.isEmpty())
				return "undefined";
			if (kit.size() == 1) {
				// Single-element kit: base name derived from the selected template (if any), fallback to element name
				String selected = kit.get(0).selectedTemplateSupplier() == null ? null : kit.get(0).selectedTemplateSupplier().get();
				String raw = selected == null || selected.isBlank() ? kit.get(0).name() : selected;
				String justName = org.apache.commons.io.FilenameUtils.getName(raw == null ? "" : raw);
				return stripSuffix(justName);
			} else {
				// Composite kit: use the kit id (short and stable) as the base name
				String id = kit.get(0).id();
				if (id == null || id.isBlank())
					return "document-set";
				return id.replaceAll("[^A-Za-z0-9]", "");
			}
		};
		// stream/ui precheck are provided to the dialog via helper method references below

		Supplier<String> extSupplier = () -> {
			String ext = kit.get(0).extension();
			return ext == null || ext.isBlank() ? ".xlsx" : (ext.startsWith(".") ? ext : ("." + ext));
		};

		// Use the dialog helper to create and wire the control; pass method references for domain helpers
		Supplier<String> zipBase = () -> {
			if (zipBaseOverride != null)
				return zipBaseOverride.get();
			if (kit == null || kit.isEmpty())
				return baseFile.get();
			String id = kit.get(0).id();
			return (id == null || id.isBlank()) ? baseFile.get() : id.replaceAll("[^A-Za-z0-9]", "");
		};

		return dialog.createDoItButtonForKitsWithHelpers(
		        baseFile,
		        kit,
		        selectedSessionsSupplier,
		        computeAthletesSupplier,
		        (selSessions, k) -> zipKitToInputStream(selSessions, k, (e, m) -> notifyError(e, UI.getCurrent(), m), t -> {
			        /* no-op */}, UI.getCurrent()),
		        (selSessions, k) -> {
			        try {
				        return excelKitElement(selSessions, k, UI.getCurrent(), t -> {
				        });
			        } catch (IOException ioe) {
				        throw new RuntimeException(ioe);
			        }
		        },
		        (elements, g, athletes, d) -> precheckService.runSetScopePrecheckOrThrow(elements, g, athletes, d),
		        (elements, g, athletes, d) -> precheckService.filterElementsByScopePrecheckOrThrow(elements, g, athletes, d),
		        zipBase,
		        extSupplier,
		        VaadinIcon.DOWNLOAD_ALT.create());
	}

	private Div createDocumentDownloadButton(PreCompetitionTemplate templateDefinition,
	        Runnable preAction,
	        Supplier<List<KitElement>> elementSupplier,
	        boolean primary) {
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        if (preAction != null) {
				        preAction.run();
			        }
			        // logger removed
			        List<KitElement> kit = elementSupplier.get();
			        // Create dialog that receives the precomputed kit and builds the do-it control
			        Supplier<List<Group>> selectedSessionsSupplier = this::getSortedSelection;
			        Supplier<List<Athlete>> computeAthletesSupplier = () -> {
				        List<Group> ss = getSortedSelection();
				        Group g = (ss != null && ss.size() > 0) ? ss.get(0) : null;
				        return (g != null) ? groupAthletes(g, true) : athletesFindAll(true);
			        };
			        DocumentDownloadDialog dialog = new DocumentDownloadDialog(kit, selectedSessionsSupplier, computeAthletesSupplier,
			                (d, kits) -> createDoItButtonForKits(kits, d, selectedSessionsSupplier, computeAthletesSupplier, () -> {
				                if (kits == null || kits.isEmpty())
					                return "undefined";
				                if (kits.size() == 1) {
					                String selected = kits.get(0).selectedTemplateSupplier() == null ? null : kits.get(0).selectedTemplateSupplier().get();
					                String raw = selected == null || selected.isBlank() ? kits.get(0).name() : selected;
					                String justName = org.apache.commons.io.FilenameUtils.getName(raw == null ? "" : raw);
					                return stripSuffix(justName);
				                } else {
					                String id = kits.get(0).id();
					                if (id == null || id.isBlank())
						                return "document-set";
					                return id.replaceAll("[^A-Za-z0-9]", "");
				                }
			                }));
			        dialog.open();
		        });
		if (primary) {
			openDialog.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		}
		return new Div(openDialog);
	}

	private Div createEmptyProtocolButton() {
		PreCompetitionTemplate templateDefinition = PreCompetitionTemplate.EMPTY_PROTOCOL;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        // logger removed
			        List<KitElement> kit = prepareEmptyProtocol(templateDefinition, getSortedSelection());
			        Supplier<List<Group>> selectedSessionsSupplier = this::getSortedSelection;
			        Supplier<List<Athlete>> computeAthletesSupplier = () -> {
				        List<Group> ss = getSortedSelection();
				        Group g = (ss != null && ss.size() > 0) ? ss.get(0) : null;
				        return (g != null) ? groupAthletes(g, true) : athletesFindAll(true);
			        };
			        DocumentDownloadDialog dialog = new DocumentDownloadDialog(kit, selectedSessionsSupplier, computeAthletesSupplier,
			                (d, kits) -> createDoItButtonForKits(kits, d, selectedSessionsSupplier, computeAthletesSupplier, () -> {
				                if (kits == null || kits.isEmpty())
					                return "undefined";
				                if (kits.size() == 1) {
					                String selected = kits.get(0).selectedTemplateSupplier() == null ? null : kits.get(0).selectedTemplateSupplier().get();
					                String raw = selected == null || selected.isBlank() ? kits.get(0).name() : selected;
					                String justName = org.apache.commons.io.FilenameUtils.getName(raw == null ? "" : raw);
					                return stripSuffix(justName);
				                } else {
					                String id = kits.get(0).id();
					                if (id == null || id.isBlank())
						                return "document-set";
					                return id.replaceAll("[^A-Za-z0-9]", "");
				                }
			                }));
			        dialog.open();
		        });
		// add debug id and log creation
		try {
			openDialog.setId("doc-empty-protocol-btn");
			// logger removed
		} catch (Throwable ignore) {
		}
		return new Div(openDialog);
	}

	private Div createFullScheduleButton() {
		PreCompetitionTemplate templateDefinition = PreCompetitionTemplate.SCHEDULE;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        List<KitElement> kit = prepareSchedule(templateDefinition, getSortedSelection());
			        // Create the dialog with a factory that receives the precomputed kit list
			        // so the dialog can add a do-it control that does not re-run preparation.
			        Supplier<List<Group>> selectedSessionsSupplier = this::getSortedSelection;
			        Supplier<List<Athlete>> computeAthletesSupplier = () -> {
				        List<Group> ss = getSortedSelection();
				        Group g = (ss != null && ss.size() > 0) ? ss.get(0) : null;
				        return (g != null) ? groupAthletes(g, true) : athletesFindAll(true);
			        };
			        DocumentDownloadDialog dialog = new DocumentDownloadDialog(kit, selectedSessionsSupplier, computeAthletesSupplier,
			                (d, kits) -> createDoItButtonForKits(kits, d, selectedSessionsSupplier, computeAthletesSupplier, () -> {
				                if (kits == null || kits.isEmpty())
					                return "undefined";
				                if (kits.size() == 1) {
					                String selected = kits.get(0).selectedTemplateSupplier() == null ? null : kits.get(0).selectedTemplateSupplier().get();
					                String raw = selected == null || selected.isBlank() ? kits.get(0).name() : selected;
					                String justName = org.apache.commons.io.FilenameUtils.getName(raw == null ? "" : raw);
					                return stripSuffix(justName);
				                } else {
					                String id = kits.get(0).id();
					                if (id == null || id.isBlank())
						                return "document-set";
					                return id.replaceAll("[^A-Za-z0-9]", "");
				                }
			                }));
			        dialog.open();
		        });
		return new Div(openDialog);
	}

	/**
	 * The columns of the crudGrid
	 *
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	private GridCrud<Group> createGrid(OwlcmsCrudFormFactory<Group> crudFormFactory) {
		Grid<Group> grid = new Grid<>(Group.class, false);
		this.crud = new DocumentsGrid(Group.class, new OwlcmsGridLayout(Group.class), crudFormFactory, grid);
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

	private Div createIntroductionButton() {
		PreCompetitionTemplate templateDefinition = PreCompetitionTemplate.INTRODUCTION;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        // logger removed
			        List<KitElement> kit = prepareIntroduction(templateDefinition, getSortedSelection());
			        Supplier<List<Group>> selectedSessionsSupplier = this::getSortedSelection;
			        Supplier<List<Athlete>> computeAthletesSupplier = () -> {
				        List<Group> ss = getSortedSelection();
				        Group g = (ss != null && ss.size() > 0) ? ss.get(0) : null;
				        return (g != null) ? groupAthletes(g, true) : athletesFindAll(true);
			        };
			        DocumentDownloadDialog dialog = new DocumentDownloadDialog(kit, selectedSessionsSupplier, computeAthletesSupplier,
			                (d, kits) -> createDoItButtonForKits(kits, d, selectedSessionsSupplier, computeAthletesSupplier, () -> {
				                if (kits == null || kits.isEmpty())
					                return "undefined";
				                if (kits.size() == 1) {
					                String selected = kits.get(0).selectedTemplateSupplier() == null ? null : kits.get(0).selectedTemplateSupplier().get();
					                String raw = selected == null || selected.isBlank() ? kits.get(0).name() : selected;
					                String justName = org.apache.commons.io.FilenameUtils.getName(raw == null ? "" : raw);
					                return stripSuffix(justName);
				                } else {
					                String id = kits.get(0).id();
					                if (id == null || id.isBlank())
						                return "document-set";
					                return id.replaceAll("[^A-Za-z0-9]", "");
				                }
			                }));
			        dialog.open();
		        });
		// add debug id and log creation
		try {
			openDialog.setId("doc-introduction-btn");
			// logger removed
		} catch (Throwable ignore) {
		}
		return new Div(openDialog);
	}

	private Div createJuryButton() {
		PreCompetitionTemplate templateDefinition = PreCompetitionTemplate.JURY;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        // logger removed
			        List<KitElement> kit = prepareJury(templateDefinition, getSortedSelection());
			        Supplier<List<Group>> selectedSessionsSupplier = this::getSortedSelection;
			        Supplier<List<Athlete>> computeAthletesSupplier = () -> {
				        List<Group> ss = getSortedSelection();
				        Group g = (ss != null && ss.size() > 0) ? ss.get(0) : null;
				        return (g != null) ? groupAthletes(g, true) : athletesFindAll(true);
			        };
			        DocumentDownloadDialog dialog = new DocumentDownloadDialog(kit, selectedSessionsSupplier, computeAthletesSupplier,
			                (d, kits) -> createDoItButtonForKits(kits, d, selectedSessionsSupplier, computeAthletesSupplier, () -> {
				                if (kits == null || kits.isEmpty())
					                return "undefined";
				                if (kits.size() == 1) {
					                String selected = kits.get(0).selectedTemplateSupplier() == null ? null : kits.get(0).selectedTemplateSupplier().get();
					                String raw = selected == null || selected.isBlank() ? kits.get(0).name() : selected;
					                String justName = org.apache.commons.io.FilenameUtils.getName(raw == null ? "" : raw);
					                return stripSuffix(justName);
				                } else {
					                String id = kits.get(0).id();
					                if (id == null || id.isBlank())
						                return "document-set";
					                return id.replaceAll("[^A-Za-z0-9]", "");
				                }
			                }));
			        dialog.open();
		        });
		// add debug id and log creation
		try {
			openDialog.setId("doc-jury-btn");
			// logger removed
		} catch (Throwable ignore) {
		}
		return new Div(openDialog);
	}

	private Div createOfficialsButton() {
		PreCompetitionTemplate templateDefinition = PreCompetitionTemplate.OFFICIALS;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        List<KitElement> kit = prepareOfficials(getSortedSelection());
			        Supplier<List<Group>> selectedSessionsSupplier = this::getSortedSelection;
			        Supplier<List<Athlete>> computeAthletesSupplier = () -> {
				        List<Group> ss = getSortedSelection();
				        Group g = (ss != null && ss.size() > 0) ? ss.get(0) : null;
				        return (g != null) ? groupAthletes(g, true) : athletesFindAll(true);
			        };
			        DocumentDownloadDialog dialog = new DocumentDownloadDialog(kit, selectedSessionsSupplier, computeAthletesSupplier,
			                (d, kits) -> createDoItButtonForKits(kits, d, selectedSessionsSupplier, computeAthletesSupplier, () -> {
				                if (kits == null || kits.isEmpty())
					                return "undefined";
				                if (kits.size() == 1) {
					                String selected = kits.get(0).selectedTemplateSupplier() == null ? null : kits.get(0).selectedTemplateSupplier().get();
					                String raw = selected == null || selected.isBlank() ? kits.get(0).name() : selected;
					                String justName = org.apache.commons.io.FilenameUtils.getName(raw == null ? "" : raw);
					                return stripSuffix(justName);
				                } else {
					                String id = kits.get(0).id();
					                if (id == null || id.isBlank())
						                return "document-set";
					                return id.replaceAll("[^A-Za-z0-9]", "");
				                }
			                }));
			        dialog.open();
		        });
		return new Div(openDialog);
	}

	private Div createPostWeighInButton() {
		Button openDialog = new Button(
		        Translator.translate("Documents.Kits"),
		        VaadinIcon.ARCHIVE.create(),
		        (e) -> {
			        List<KitElement> kit = preparePostWeighInKit(getSortedSelection());
			        Supplier<List<Group>> selectedSessionsSupplier = this::getSortedSelection;
			        Supplier<List<Athlete>> computeAthletesSupplier = () -> {
				        List<Group> ss = getSortedSelection();
				        Group g = (ss != null && ss.size() > 0) ? ss.get(0) : null;
				        return (g != null) ? groupAthletes(g, true) : athletesFindAll(true);
			        };
			        DocumentDownloadDialog dialog = new DocumentDownloadDialog(kit, selectedSessionsSupplier, computeAthletesSupplier,
			                (d, kits) -> createDoItButtonForKits(kits, d, selectedSessionsSupplier, computeAthletesSupplier, () -> "PostWeighIn"));
			        dialog.open();
		        });
		return new Div(openDialog);
	}

	private Div createPreWeighInButton() {
		Button openDialog = new Button(
		        Translator.translate("Documents.Kits"),
		        VaadinIcon.ARCHIVE.create(),
		        (e) -> {
			        List<KitElement> kit = preparePreWeighInKit(getSortedSelection());
			        Supplier<List<Group>> selectedSessionsSupplier = this::getSortedSelection;
			        Supplier<List<Athlete>> computeAthletesSupplier = () -> {
				        List<Group> ss = getSortedSelection();
				        Group g = (ss != null && ss.size() > 0) ? ss.get(0) : null;
				        return (g != null) ? groupAthletes(g, true) : athletesFindAll(true);
			        };
			        DocumentDownloadDialog dialog = new DocumentDownloadDialog(kit, selectedSessionsSupplier, computeAthletesSupplier,
			                (d, kits) -> createDoItButtonForKits(kits, d, selectedSessionsSupplier, computeAthletesSupplier, () -> "PreWeighIn"));
			        dialog.open();
		        });
		return new Div(openDialog);
	}

	private List<KitElement> prepareCredentials(List<Group> selectedItems) {
		List<KitElement> elements = new ArrayList<>();
		// Use same PreCompetitionTemplate.CARDS for athlete credentials, and reuse a generic template for TO/Coach
		elements.add(doElementAthleteCredentials(PreCompetitionTemplate.ATHLETE_CREDENTIALS));
		elements.add(doElementTOCredentials(PreCompetitionTemplate.TO_CREDENTIALS));
		elements.add(doElementCoachCredentials(PreCompetitionTemplate.COACH_CREDENTIALS));
		return elements;
	}

	private Div createCredentialsButton() {
		Button openDialog = new Button(
				Translator.translate("Credentials"),
				VaadinIcon.ARCHIVE.create(),
				(e) -> {
					List<KitElement> kit = prepareCredentials(getSortedSelection());
					logger.debug("credentials kit {}", kit);
					Supplier<List<Group>> selectedSessionsSupplier = this::getSortedSelection;
					Supplier<List<Athlete>> computeAthletesSupplier = () -> {
						List<Group> ss = getSortedSelection();
						Group g = (ss != null && ss.size() > 0) ? ss.get(0) : null;
						return (g != null) ? groupAthletes(g, true) : athletesFindAll(true);
					};
					DocumentDownloadDialog dialog = new DocumentDownloadDialog(
						kit,
						selectedSessionsSupplier, computeAthletesSupplier,
						(d, kits) -> createDoItButtonForKits(
							kits, d, selectedSessionsSupplier, computeAthletesSupplier, () -> "Credentials"));
					dialog.open();
				});
		return new Div(openDialog);
	}

	private Hr createRule() {
		Hr hr = new Hr();
		hr.setWidthFull();
		hr.getStyle().set("margin", "0");
		hr.getStyle().set("padding", "0");
		return hr;
	}

	private Div createStartListButton() {
		PreCompetitionTemplate templateDefinition = PreCompetitionTemplate.START_LIST;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        List<KitElement> kit = prepareStartList(templateDefinition, getSortedSelection());
			        Supplier<List<Group>> selectedSessionsSupplier = this::getSortedSelection;
			        Supplier<List<Athlete>> computeAthletesSupplier = () -> {
				        List<Group> ss = getSortedSelection();
				        Group g = (ss != null && ss.size() > 0) ? ss.get(0) : null;
				        return (g != null) ? groupAthletes(g, true) : athletesFindAll(true);
			        };
			        DocumentDownloadDialog dialog = new DocumentDownloadDialog(kit, selectedSessionsSupplier, computeAthletesSupplier,
			                (d, kits) -> createDoItButtonForKits(kits, d, selectedSessionsSupplier, computeAthletesSupplier));
			        dialog.open();
		        });
		openDialog.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		return new Div(openDialog);
	}

	private Div createTeamsButton() {
		PreCompetitionTemplate templateDefinition = PreCompetitionTemplate.BY_TEAM;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        List<KitElement> kit = prepareTeam(templateDefinition, getSortedSelection());
			        Supplier<List<Group>> selectedSessionsSupplier = this::getSortedSelection;
			        Supplier<List<Athlete>> computeAthletesSupplier = () -> {
				        List<Group> ss = getSortedSelection();
				        Group g = (ss != null && ss.size() > 0) ? ss.get(0) : null;
				        return (g != null) ? groupAthletes(g, true) : athletesFindAll(true);
			        };
			        DocumentDownloadDialog dialog = new DocumentDownloadDialog(kit, selectedSessionsSupplier, computeAthletesSupplier,
			                (d, kits) -> createDoItButtonForKits(kits, d, selectedSessionsSupplier, computeAthletesSupplier));
			        dialog.open();
		        });
		// openDialog.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		return new Div(openDialog);
	}

	private Div createWeighInButton() {
		PreCompetitionTemplate templateDefinition = PreCompetitionTemplate.WEIGHIN;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        List<KitElement> kit = prepareWeighIn(templateDefinition, getSortedSelection());
			        Supplier<List<Group>> selectedSessionsSupplier = this::getSortedSelection;
			        Supplier<List<Athlete>> computeAthletesSupplier = () -> {
				        List<Group> ss = getSortedSelection();
				        Group g = (ss != null && ss.size() > 0) ? ss.get(0) : null;
				        return (g != null) ? groupAthletes(g, true) : athletesFindAll(true);
			        };
			        DocumentDownloadDialog dialog = new DocumentDownloadDialog(kit, selectedSessionsSupplier, computeAthletesSupplier,
			                (d, kits) -> createDoItButtonForKits(kits, d, selectedSessionsSupplier, computeAthletesSupplier));
			        dialog.open();
		        });
		// openDialog.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		return new Div(openDialog);
	}

	private KitElement doElementBodyweight(PreCompetitionTemplate templateDefinition) {
		// Use the enum name so the dialog can detect the element and add a template selector
		// (DocumentDownloadDialog maps normalized id -> PreCompetitionTemplates enum).
		return defineKit(PreCompetitionTemplate.BY_BODYWEIGHT.name(),
		        templateDefinition,
		        defaultScopePrecheckAllowNoSelectionFor(templateDefinition),
		        (a, ignored) -> {
			        JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			        startingXlsWriter.setGroup(null);
			        // get current version of athletes.
			        startingXlsWriter.setSortedAthletes(AthleteSorter.registrationBWCopy(athletesFindAll(false)));
			        startingXlsWriter.createAgeGroupColumns(10, 7);
			        return startingXlsWriter;
		        });
	}

	private KitElement doElementCards(PreCompetitionTemplate templateDefinition) {
		// CARDS: when a single session is selected require some athletes; when no session selected
		// use total athletes and require >0 and less than 100, and check for reasonable selection (not too many athletes)
		return defineKit("cards",
				templateDefinition,
				cardsScopePrecheck,
				(a, g) -> {
					JXLSCardsDocs xlsWriter = new JXLSCardsDocs();
					xlsWriter.setGroup(g);
					return xlsWriter;
				});
	}

	/**
	 * Athlete credentials use the same scope precheck as cards (require some athletes for a selected session,
	 * and reasonable total counts when no session is selected).
	 */
	private KitElement doElementAthleteCredentials(PreCompetitionTemplate templateDefinition) {
		return defineKit("athleteCredentials",
				templateDefinition,
				cardsScopePrecheck,
				(a, g) -> {
					JXLSCardsDocs xlsWriter = new JXLSCardsDocs();
					xlsWriter.setGroup(g);
					return xlsWriter;
				});
	}

	/**
	 * Technical Official (TO) credentials allow no selection (global report), reuse the allow-no-selection precheck.
	 */
	private KitElement doElementTOCredentials(PreCompetitionTemplate templateDefinition) {
		return defineKit("toCredentials",
				templateDefinition,
				defaultScopePrecheckAllowNoSelectionFor(templateDefinition),
				(a, g) -> {
					JXLSCardsDocs xlsWriter = new JXLSCardsDocs();
					xlsWriter.setGroup(g);
					return xlsWriter;
				});
	}

	/**
	 * Coach credentials allow no selection (global report), reuse the allow-no-selection precheck.
	 */
	private KitElement doElementCoachCredentials(PreCompetitionTemplate templateDefinition) {
		return defineKit("coachCredentials",
				templateDefinition,
				defaultScopePrecheckAllowNoSelectionFor(templateDefinition),
				(a, g) -> {
					JXLSCardsDocs xlsWriter = new JXLSCardsDocs();
					// we set no athletes.  setReportingInfo will add the the coaches to
					// the reporting beans.
			        xlsWriter.setGroup(null);
			        xlsWriter.setSortedAthletes(List.of());
			        xlsWriter.setEmptyOk(true);
					return xlsWriter;
				});
	}

	private KitElement doElementCategories(PreCompetitionTemplate template) {
		return defineKit(PreCompetitionTemplate.BY_CATEGORY.name(),
		        template,
		        defaultScopePrecheckAllowNoSelectionFor(template),
		        (a, ignored) -> {
			        JXLSCategoriesListDocs xlsWriter = new JXLSCategoriesListDocs();
			        xlsWriter.setGroup(null);
			        // use the rules from JXLSCategoriesListDocs
			        // var athletes = participationFindAll();
			        // athletes.sort(RegistrationOrderComparator.athleteReportOrderComparator);
			        // xlsWriter.setSortedAthletes(athletes);
			        return xlsWriter;
		        });
	}

	private KitElement doElementCheckin(PreCompetitionTemplate template) {
		return defineKit("checkin",
		        template,
		        defaultScopePrecheckAllowNoSelectionFor(template),
		        (a, ignored) -> {
			        JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			        startingXlsWriter.setGroup(null);
			        startingXlsWriter.setPostProcessor(null);
			        List<Athlete> athletesFindAll = athletesFindAll(true);
			        // logger removed
			        startingXlsWriter.setSortedAthletes(athletesFindAll);
			        return startingXlsWriter;
		        });
	}

	private KitElement doElementEmptyProtocol(PreCompetitionTemplate template) {
		return defineKit("emptyProtocol",
		        template,
		        (a, g) -> {
			        // Require session selection
			        if (g == null) {
				        return Optional.of(new NoSessionException());
			        }
			        return Optional.empty();
		        },
		        (a, g) -> {
			        AthleteRepository.assignStartNumbers(a);
			        JXLSResultSheet rs = new JXLSResultSheet(false);
			        rs.setGroup(g);
			        rs.setSortedAthletes(a);
			        return rs;
		        });
	}

	private KitElement doElementIntroduction(PreCompetitionTemplate template) {
		return defineKit("introduction",
		        template,
		        (a, g) -> {
			        // Require session selection
			        if (g == null) {
				        return Optional.of(new NoSessionException());
			        }
			        return Optional.empty();
		        },
		        (a, g) -> {
			        AthleteRepository.assignStartNumbers(a);
			        JXLSCategoriesListDocs xlsWriter = new JXLSCategoriesListDocs();
			        xlsWriter.setGroup(g);

			        // sort to the desired order
			        a.sort((x, y) -> ObjectUtils.compare(x.getCategoryCode(), y.getCategoryCode()));
			        xlsWriter.setSortedAthletes(a);
			        return xlsWriter;
		        });
	}

	private KitElement doElementJury(PreCompetitionTemplate template) {
		return defineKit("jury",
		        template,
		        defaultScopePrecheckFor(template),
		        (a, g) -> {
			        AthleteRepository.assignStartNumbers(a);
			        JXLSJurySheet rs = new JXLSJurySheet();
			        rs.setGroup(g);
			        rs.setSortedAthletes(a);
			        return rs;
		        });
	}

	private KitElement doElementOfficials() {
		return defineKit("officials",
		        PreCompetitionTemplate.OFFICIALS,
		        defaultScopePrecheckAllowNoSelectionFor(PreCompetitionTemplate.OFFICIALS),
		        (a, ignored) -> {
			        JXLSStartingListDocs xlsWriter = new JXLSStartingListDocs();
			        xlsWriter.setGroup(null);
			        xlsWriter.setSortedAthletes(List.of());
			        xlsWriter.setEmptyOk(true);
			        return xlsWriter;
		        });
	}

	private KitElement doElementSchedule() {
		return defineKit("schedule",
		        PreCompetitionTemplate.SCHEDULE,
		        defaultScopePrecheckAllowNoSelectionFor(PreCompetitionTemplate.SCHEDULE),
		        (a, ignored) -> {
			        // schedule is currently a variation on starting list
			        JXLSStartingListDocs xlsWriter = new JXLSStartingListDocs();
			        xlsWriter.setPostProcessor((w) -> {
				        if (xlsWriter.getFirstMergeLine() != null) {
					        logger.debug("merging {} {}", xlsWriter.getFirstMergeLine(), xlsWriter.getMergeColumnList());
					        // merged columns
					        fixMerges(w, xlsWriter.getFirstMergeLine(), xlsWriter.getMergeColumnList());
					        fixLastLine(w);
				        } else {
					        // simple schedule with no nested merged columns
					        xlsWriter.setPostProcessor(null);
					        xlsWriter.setSortedAthletes(a);
				        }
			        });

			        return xlsWriter;
		        });
	}

	private KitElement doElementStartList(PreCompetitionTemplate templateDefinition) {
		return defineKit("startList",
		        templateDefinition,
		        defaultScopePrecheckAllowNoSelectionFor(templateDefinition),
		        (a, ignored) -> {
			        System.err.println("*** doElementStartList for " + templateDefinition.name());
			        try {
				        JXLSStartingListDocs xlsWriter = new JXLSStartingListDocs();
				        System.err.println("*** doElementStartList created xlsWriter for " + templateDefinition.name() + ": " + xlsWriter);
				        xlsWriter.setGroup(null);
				        // get current version of athletes.
				        List<Athlete> athletesFindAll = athletesFindAll(true);
				        xlsWriter.setSortedAthletes(athletesFindAll);
				        xlsWriter.setPostProcessor(null);
				        return xlsWriter;
			        } catch (Throwable e) {
				        e.printStackTrace();
			        }
			        return null;
		        });
	}

	private KitElement doElementTeam(PreCompetitionTemplate template) {
		return defineKit(PreCompetitionTemplate.BY_TEAM.name(),
		        template,
		        defaultScopePrecheckAllowNoSelectionFor(template),
		        (a, ignored) -> {
			        JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			        startingXlsWriter.setGroup(null);
			        // get current version of athletes.
			        startingXlsWriter.setSortedAthletes(AthleteSorter.registrationOrderCopy(athletesFindAll(false)));
			        startingXlsWriter.createTeamColumns(9, 6);
			        return startingXlsWriter;
		        });
	}

	private KitElement doElementWeighIn(PreCompetitionTemplate template) {
		// Use the enum name so the dialog can add a template selector. Weigh-in
		// requires a selected session, so use the default precheck that enforces a session.
		return defineKit(PreCompetitionTemplate.WEIGHIN.name(),
		        template,
		        defaultScopePrecheckFor(template),
		        (a, g) -> {
			        JXLSWeighInSheet rs = new JXLSWeighInSheet(); // Create a new weigh-in sheet
			        System.err.println("============ group g " + g + LoggerUtils.stackTrace());
			        rs.setGroup(g);
			        return rs;
		        });
	}

	private void doKitElement(KitElement elem, String seq, ZipOutputStream zipOut, Group g, List<Athlete> athletes) throws IOException {
		JXLSWorkbookStreamSource xlsWriter = elem.writerFactory().apply(athletes, g);

		// apply default if the factory did not set
		if (xlsWriter.getGroup() == null) {
			xlsWriter.setGroup(g);
		}
		if (xlsWriter.getSortedAthletes() == null) {
			xlsWriter.setSortedAthletes(athletes);
		}

		// logger removed
		InputStream is = null;
		try {
			if (elem.isp() != null) {
				is = Files.newInputStream(elem.isp());
			} else {
				java.nio.file.Path resolved = ResourceWalker.getFileOrResourcePath(elem.name());
				// logger removed
				is = Files.newInputStream(resolved);
			}
		} catch (java.io.FileNotFoundException fnf) {
			// logger removed
			throw fnf;
		}
		xlsWriter.setInputStream(is);
		xlsWriter.setTemplateFileName(elem.name());
		InputStream in = xlsWriter.createInputStream();
		// Handle null group for documents that don't require a session (e.g., coach credentials)
		String groupName = (g != null) ? g.getName() : "All";
		String name = seq + "_" + elem.id() + "_" + groupName + "." + elem.extension();
		ZipUtils.zipStream(in, name, false, zipOut);
	}

	private void doNotification(String text) {
		this.getUI().get().access(() -> {
			// logger removed
			Notification notif = new Notification();
			notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
			notif.setPosition(Position.TOP_STRETCH);
			notif.setDuration(5000);
			Div div = new Div(text);
			div.getStyle().set("font-size", "140%");
			notif.add(div);
			notif.open();
		});
	}

	private void doPrintScript(ZipOutputStream zipOut) {
		try {
			ZipUtils.zipStream(ResourceWalker.getFileOrResource("/templates/scripts/print.bat"), "print.bat", false, zipOut);
			ZipUtils.zipStream(ResourceWalker.getFileOrResource("/templates/scripts/print.ps1"), "print.ps1", false, zipOut);
		} catch (IOException e) {
			LoggerUtils.logError(logger, e, true);
		}
	}

	private InputStream excelKitElement(List<Group> selectedSessions, List<KitElement> elements, UI ui, Consumer<Throwable> doneCallback)
	        throws IOException {
		// always called with a single template
		// for items that are one per session, selected sessions will be non-empty.
		System.err.println("*** excelKitElement for " + (elements == null ? "null" : elements.size()) + " elements and "
		        + (selectedSessions == null ? "null" : selectedSessions.size()) + " sessions");

		Group g = (selectedSessions != null && selectedSessions.size() > 0) ? selectedSessions.get(0) : null;
		KitElement elem = elements.get(0);

		List<Athlete> athletes = null;
		if (g != null) {
			athletes = groupAthletes(g, true);
		}

		System.err.println("g = " + g + " athletes = " + (athletes == null ? "null" : athletes.size()) + " for element " + elem.id());
		// logger removed

		// writerFactory can apply custom sorting order to the athletes
		JXLSWorkbookStreamSource xlsWriter = elem.writerFactory().apply(athletes, g);
		System.err.println("*** excelKitElement created " + xlsWriter);
		xlsWriter.setUi(ui);
		if (xlsWriter.getSortedAthletes() == null) {
			// writerFactory did not set them explicitly, set default
			xlsWriter.setSortedAthletes(athletes);
		}
		if (xlsWriter.getGroup() == null) {
			// writerFactory did not set them explicitly, set default.
			xlsWriter.setGroup(g);
		}

		InputStream is = null;
		try {
			if (elem.isp() != null) {
				is = Files.newInputStream(elem.isp());
			} else {
				// attempt to resolve missing path
				java.nio.file.Path resolved = ResourceWalker.getFileOrResourcePath(elem.name());
				// logger removed
				is = Files.newInputStream(resolved);
			}
		} catch (java.io.FileNotFoundException fnf) {
			// logger removed
			throw fnf;
		}
		xlsWriter.setInputStream(is);
		xlsWriter.setTemplateFileName(elem.name());

		if (doneCallback == null) {
			Notification n = new Notification(Translator.translate("Documents.ProcessingExcel"));
			xlsWriter.setDoneCallback((t) -> ui.access(() -> {
				if (t == null) {
					// success: close processing notification
					n.close();
				} else {
					// show error message from Throwable
					String msg = t.getMessage() == null ? Translator.translate("Download.failed") : t.getMessage();
					n.setText(msg);
					n.addThemeVariants(NotificationVariant.LUMO_ERROR);
					n.setPosition(Position.TOP_STRETCH);
					n.setDuration(0); // keep open until user dismisses
					n.open();
				}
			}));
			n.setPosition(Position.TOP_END);
			ui.access(() -> {
				n.open();
			});
		} else {
			xlsWriter.setDoneCallback(doneCallback);
		}
		InputStream in;
		try {
			xlsWriter.setUi(ui);
			// logger removed
			// if an exception happens here, it is caught in the caller, it needs to close the dialog.
			in = xlsWriter.createInputStream();
			return in;
		} catch (Exception e) {
			// logger removed
			LoggerUtils.logError(logger, e, true);
			// ensure the dialog (or processing indicator) is closed via the provided callback
			try {
				if (doneCallback != null) {
					logger.info("Invoking doneCallback to close dialog from DocumentsContent.excelKitElement catch: {}", LoggerUtils.stackTrace());
					// convert exception to Throwable and pass it
					try {
						doneCallback.accept(e);
					} catch (Throwable cb) {
						LoggerUtils.logError(logger, cb, true);
					}
				}
			} catch (Throwable cb) {
				LoggerUtils.logError(logger, cb, true);
			}
			throw e;
		}

	}

	private InputStream excelToInputStream(List<Group> selectedSessions,
	        List<KitElement> elements, BiConsumer<Throwable, String> errorProcessor, Consumer<Throwable> doneCallback, UI ui) {
		String context = LoggerUtils.stackTrace();
		try {
			return excelKitElement(selectedSessions, elements, ui, doneCallback);
		} catch (Exception e) {
			System.err.println("%%%%%%%%%% Exception context %%%%%%%%%%%%%\n" + context);
			// propagate as StopProcessingException so caller can handle and notify once
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

	private void fixLastLine(Workbook w) {
		Sheet sheet = w.getSheetAt(0);
		// Define the border style properties
		Map<String, Object> properties = new HashMap<>();
		properties.put(CellUtil.BORDER_BOTTOM, BorderStyle.THIN);
		properties.put(CellUtil.BOTTOM_BORDER_COLOR, IndexedColors.BLACK.getIndex());

		// Retrieve the last row
		int lastRowNum = sheet.getLastRowNum();
		Row lastRow = sheet.getRow(lastRowNum);

		// Apply the border style to the cells in the last row
		for (int i = 0; i < lastRow.getLastCellNum(); i++) {
			Cell cell = lastRow.getCell(i);
			if (cell == null) {
				cell = lastRow.createCell(i);
			}
			CellUtil.setCellStyleProperties(cell, properties);
		}
	}

	private void fixMerges(Workbook workbook, Integer startRowNum, List<Integer> columns) {
		try {
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
							int regionSize = (row.getRowNum() - 1) - firstRow;
							logger.debug("     region size = {}", regionSize);
							if (regionSize > 0) {
								CellRangeAddress region = new CellRangeAddress(firstRow, row.getRowNum() - 1, col, col);
								sheet.addMergedRegion(region);
								// Apply the captured style to the first cell of the merged region
								Cell cell2 = sheet.getRow(firstRow).getCell(col);
								if (style != null) {
									style.setBorderBottom(BorderStyle.HAIR);
									cell2.setCellStyle(style);
								}
								isMerging = false;
							}

							// start a new merge
							logger.debug("**** {}{}: starting merge 1", (char) ('A' + col), row.getRowNum() + 1, isMerging);
							firstRow = row.getRowNum();
							style = cell.getCellStyle(); // capture the style
							isMerging = true;
						} else {
							logger.debug("**** {}{}: starting merge 2", (char) ('A' + col), row.getRowNum() + 1, isMerging);
							firstRow = row.getRowNum();
							style = cell.getCellStyle(); // capture the style
							isMerging = true;
						}
					}
				}
				// Merge the bottom region if needed
				if (isMerging) {
					logger.debug("**** {}{}: merging bottom from {}{}", (char) ('A' + col), sheet.getLastRowNum() + 1,
					        (char) ('A' + col), firstRow + 1);
					CellRangeAddress region = new CellRangeAddress(firstRow, sheet.getLastRowNum(), col, col);
					sheet.addMergedRegion(region);
					Cell cell22 = sheet.getRow(firstRow).getCell(col);
					if (style != null) {
						style.setBorderBottom(BorderStyle.HAIR);
						cell22.setCellStyle(style);
					}
				}
			}
		} catch (Exception e) {
			// logger removed
		}
	}

	private Platform getPlatform() {
		return null;
	}

	private List<Group> getSortedSelection() {
		return this.crud.getSelectedItems().stream().sorted(Group.groupWeighinTimeComparator).toList();
	}

	private static List<Athlete> groupAthletes(Group g, boolean sessionOrder) {
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
		if (m != null && m.equals("NoAthletes")) {
			String text = Translator.translate("Documents.NoSession");
			doNotification(text);
		} else if (m != null && m.equals("TooManyAthletes")) {
			String text = Translator.translate("Documents.TooManyAthletes");
			doNotification(text);
		} else {
			String text = Translator.translate("Documents.NoTemplate");
			LoggerUtils.logError(logger, e, false);
			doNotification(text);
		}
	}

	private List<KitElement> prepareBodyweight(PreCompetitionTemplate templateDefinition, List<Group> selectedItems) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementBodyweight(templateDefinition));
		return elements;
	}

	private List<KitElement> prepareCards(PreCompetitionTemplate templateDefinition, List<Group> selectedItems) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementCards(templateDefinition));
		return elements;
	}

	private List<KitElement> prepareCategories(PreCompetitionTemplate template, List<Group> selectedItems) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementCategories(template));
		return elements;
	}

	private List<KitElement> prepareCheckin(PreCompetitionTemplate template, List<Group> selectedItems) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementCheckin(template));
		return elements;
	}

	private List<KitElement> prepareEmptyProtocol(PreCompetitionTemplate template, List<Group> selectedItems) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementEmptyProtocol(template));
		return elements;
	}

	private List<KitElement> prepareIntroduction(PreCompetitionTemplate template, List<Group> selectedItems) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementIntroduction(template));
		return elements;
	}

	private List<KitElement> prepareJury(PreCompetitionTemplate template, List<Group> selectedItems) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementJury(template));
		return elements;
	}

	private List<KitElement> prepareOfficials(List<Group> selectedItems) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementOfficials());
		return elements;
	}

	private List<KitElement> preparePostWeighInKit(List<Group> selectedItems) {
		List<KitElement> elements = new ArrayList<>();
		KitElement kit = doElementIntroduction(PreCompetitionTemplate.INTRODUCTION);
		if (kit != null) {
			elements.add(kit);
		}

		KitElement kit2 = doElementEmptyProtocol(PreCompetitionTemplate.EMPTY_PROTOCOL);
		if (kit2 != null) {
			elements.add(kit2);
		}

		KitElement kit3 = doElementJury(PreCompetitionTemplate.JURY);
		if (kit3 != null) {
			elements.add(kit3);
		}
		return elements;
	}

	private List<KitElement> preparePreWeighInKit(List<Group> selectedItems) {
		List<KitElement> elements = new ArrayList<>();
		KitElement kit = doElementWeighIn(PreCompetitionTemplate.WEIGHIN);
		if (kit != null) {
			elements.add(kit);
		}

		KitElement kit2 = doElementCards(PreCompetitionTemplate.CARDS);
		if (kit2 != null) {
			elements.add(kit2);
		}
		return elements;
	}

	private List<KitElement> prepareSchedule(PreCompetitionTemplate templateDefinition, List<Group> selectedItems) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementSchedule());
		return elements;
	}

	private List<KitElement> prepareStartList(PreCompetitionTemplate templateDefinition, List<Group> selectedItems) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementStartList(templateDefinition));
		return elements;
	}

	private List<KitElement> prepareTeam(PreCompetitionTemplate template, List<Group> selectedItems) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementTeam(template));
		return elements;
	}

	private List<KitElement> prepareWeighIn(PreCompetitionTemplate template, List<Group> selectedItems) {
		// Do NOT require a selected session here; allow weigh-in to be generated for the
		// whole competition when no session is selected. The element's precheck will
		// enforce session/no-session semantics via defaultScopePrecheckAllowNoSelectionFor.
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementWeighIn(template));
		return elements;
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
		templateName = templateName.replace(".xlsm", "");
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

			boolean anyProcessed = false;
			
			// Handle case where no sessions are selected (for documents that don't require sessions)
			if (selectedItems == null || selectedItems.isEmpty()) {
				// Process elements that can work without a session (e.g., coach credentials, categories)
				for (KitElement elem : elements) {
					// Skip elements without a template selected (user chose to skip this document)
					if (elem.selectedTemplateSupplier() != null) {
						String selected = elem.selectedTemplateSupplier().get();
						if (selected == null || selected.isBlank()) {
							continue; // skip this element during processing
						}
					}
					String seq = String.format("%02d", i);
					doKitElement(elem, seq, zipOut, null, null);
					anyProcessed = true;
					i++;
				}
			} else {
				// Process elements for each selected session
				for (Group g : selectedItems) {
					// get current version of athletes.
					List<Athlete> athletes = groupAthletes(g, true);
					if (athletes == null || athletes.isEmpty()) {
						// skip empty session
						continue;
					}

					for (KitElement elem : elements) {
						// Skip elements without a template selected (user chose to skip this document)
						if (elem.selectedTemplateSupplier() != null) {
							String selected = elem.selectedTemplateSupplier().get();
							if (selected == null || selected.isBlank()) {
								continue; // skip this element during processing
							}
						}
						String seq = String.format("%02d", i);
						doKitElement(elem, seq, zipOut, g, athletes);
						anyProcessed = true;
						i++;
					}
				}
			}

			// Only throw NoSession if nothing was processed AND we have elements with templates.
			// Elements may legitimately process with no sessions (emptyOk flag on their writers).
			if (!anyProcessed) {
				// Check if any elements actually have templates selected
				boolean anyTemplateSelected = false;
				for (KitElement elem : elements) {
					if (elem.selectedTemplateSupplier() != null) {
						String selected = elem.selectedTemplateSupplier().get();
						if (selected != null && !selected.isBlank()) {
							anyTemplateSelected = true;
							break;
						}
					}
				}
				// Only throw if we had templates but couldn't process anything
				if (anyTemplateSelected) {
					Exception e = new Exception("NoSession");
					throw new StopProcessingException(e.getMessage(), e);
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

	private InputStream zipKitToInputStream(List<Group> selectedItems, List<KitElement> elements,
	        BiConsumer<Throwable, String> errorProcessor, Consumer<Throwable> doneCallback, UI ui) {
		PipedOutputStream out;
		PipedInputStream in;
		try {
			out = new PipedOutputStream();
			in = new PipedInputStream(out);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (doneCallback == null) {
			Notification n = new Notification(Translator.translate("Documents.ProcessingZip"));
			n.setPosition(Position.TOP_END);
			ui.access(() -> {
				n.open();
			});
			doneCallback = s -> ui.access(() -> {
				n.close();
			});
		}
		final var dc = doneCallback;
		Thread writer = new Thread(() -> {
			try {
				zipKitToOutputStream(selectedItems, elements, errorProcessor, out);
				try {
					if (dc != null)
						dc.accept(null);
				} catch (Throwable ignore) {
				}
			} catch (Throwable t) {
				// Ensure unexpected errors are logged; zipKitToOutputStream reports via errorProcessor
				LoggerUtils.logError(logger, t, true);
				try {
					if (dc != null)
						dc.accept(t);
				} catch (Throwable ignore) {
				}
				throw t;
			} finally {
				// Defensive cleanup of thread-local state copied into this thread via InheritableThreadLocal
				try {
					OwlcmsSessionThreadLocal.remove();
				} catch (Throwable ignore) {
				}
			}
		}, "Documents-zip-writer");
		writer.setDaemon(true);
		writer.setUncaughtExceptionHandler((th, ex) -> LoggerUtils.logError(logger, ex, true));
		writer.start();
		return in;
	}

	private void zipKitToOutputStream(List<Group> selectedItems, List<KitElement> elements, BiConsumer<Throwable, String> errorProcessor,
	        PipedOutputStream out) {
		try {
			zipKit(selectedItems, elements, out);
			out.flush();
			out.close();
		} catch (Throwable e) {
			defaultErrorProcessor.accept(e, e.getMessage());
		}
	}

	@SuppressWarnings("unused")
	private InputStream zipOrExcelInputStream(UI ui, List<KitElement> elements, Consumer<Throwable> doneCallback) {
		System.err.println("*** zipOrExcelInputStream called " + ui + " with elements " + elements);
		InputStream z;
		// logger removed
		if (getSortedSelection().size() > 1 || elements.size() > 1) {
			z = zipKitToInputStream(getSortedSelection(), elements, defaultErrorProcessor, doneCallback, ui);
		} else {
			z = excelToInputStream(getSortedSelection(), elements, defaultErrorProcessor, doneCallback, ui);
		}
		return z;
	}

	// Precheck orchestration was moved to DocumentsPrecheckService to keep UI wiring
	// (DocumentsContent) separate from domain precheck logic. Use precheckService
	// where prechecks are needed.

}
