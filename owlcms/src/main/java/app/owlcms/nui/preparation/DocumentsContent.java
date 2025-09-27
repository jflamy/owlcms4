/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;

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
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.init.OwlcmsSessionThreadLocal;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.DownloadButtonFactory;
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

	private static final String DIALOG_WIDTH = "50em";

	private record KitElement(String id, String name, String extension, Path isp, int count,
		BiFunction<List<Athlete>, Group, JXLSWorkbookStreamSource> writerFactory,
		BiFunction<List<Athlete>, Group, java.util.Optional<Exception>> preCheck) {
	}

	final static Logger logger = (Logger) LoggerFactory.getLogger(DocumentsContent.class);

	static {
		logger.setLevel(Level.INFO);
	}
	boolean documentPage;
	private DocumentsGrid crud;
	private OwlcmsCrudFormFactory<Group> editingFormFactory;
	private OwlcmsLayout routerLayout;
	private FlexLayout topBar;

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
			        categoriesListButton, bwButton, teamsListButton, // spacer(), registrationTemplateSelection,
			        createRule(),
			        new NativeLabel(Translator.translate("Documents.StartBook")),
			        startListButton, scheduleButton, officialSchedule, checkInButton, // spacer(), competitionTemplateSelection,
			        createRule(),
			        new NativeLabel(Translator.translate("Documents.PreWeighIn")),
			        cardsButton, weighInButton, spacer(), preWeighInKitButton, // spacer(), preWeighInTemplateSelection,
			        createRule(),
			        new NativeLabel(Translator.translate("Documents.PostWeighIn")),
			        introductionButton, emptyProtocolButton, juryButton, spacer(), postWeighInKitButton // , spacer(), postWeighInTemplateSelection
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

	private KitElement checkKit(String id, PreCompetitionTemplates templateEnum, BiConsumer<Throwable, String> errorProcessor,
			BiFunction<List<Athlete>, Group, java.util.Optional<Exception>> explicitPreCheck,
			BiFunction<List<Athlete>, Group, JXLSWorkbookStreamSource> writerFactory) {
		try {
			System.err.println("*** checkKit for " + templateEnum.name());
			String resourceFolder = templateEnum.folder;
			resourceFolder = resourceFolder.endsWith("/") ? resourceFolder : (resourceFolder + "/");
			String template = templateEnum.templateFileNameSupplier.get();
			String templateName = resourceFolder + template;
			Path isp = ResourceWalker.getFileOrResourcePath(templateName);
			String ext = FilenameUtils.getExtension(isp.getFileName().toString());
			// default lightweight preCheck that instantiates the writer and inspects athlete counts
			BiFunction<List<Athlete>, Group, java.util.Optional<Exception>> defaultPre = (a, g) -> {
				try {
					// Log input sample for debugging
					int incomingCount = a == null ? 0 : a.size();
					String sampleIds = "";
					if (a != null && !a.isEmpty()) {
						sampleIds = a.stream().limit(10).map(ath -> String.valueOf(ath.getId())).collect(Collectors.joining(","));
					}
					String groupInfo = (g == null) ? "<no-group>" : (g.getId() + ":" + g.getName());

					JXLSWorkbookStreamSource w = writerFactory.apply(a, g);
					// writerFactory should set sorted athletes/group when appropriate
					List<Athlete> list = w.getSortedAthletes();
					int size = list != null ? list.size() : 0;

					// Determine outcome
					java.util.Optional<Exception> outcome;
					if (!(size == 0 ? w.isEmptyOk() : w.getSizeLimit() > size)) {
						if (size == 0) {
							outcome = java.util.Optional.of(new StopProcessingException("NoAthletes", new RuntimeException(Translator.translate("NoAthletes"))));
						} else {
							outcome = java.util.Optional.of(new StopProcessingException("TooManyAthletes", new RuntimeException(Translator.translate("TooManyAthletes", Integer.toString(w.getSizeLimit())))));
						}
					} else {
						outcome = java.util.Optional.empty();
					}

					// Log what we received and the resulting outcome
					String resultText = outcome.isEmpty() ? "OK" : (outcome.get().getMessage() == null ? outcome.get().toString() : outcome.get().getMessage());
					logger.warn("preCheck default for template={} received: incomingCount={}, sampleIds=[{}], group={}, resolvedCount={}, outcome={}", templateEnum.name(), incomingCount, sampleIds, groupInfo, size, resultText);

					return outcome;
				} catch (Throwable t) {
					LoggerUtils.logError(logger, t, true);
					logger.warn("preCheck default for template={} threw exception: {}", templateEnum.name(), t.toString());
					return java.util.Optional.of(new Exception(t));
				}
			};

			BiFunction<List<Athlete>, Group, java.util.Optional<Exception>> pre = explicitPreCheck != null ? explicitPreCheck : defaultPre;

			KitElement kitElement = new KitElement(id, templateName, ext, isp, 1, writerFactory, pre);
			return kitElement;
		} catch (FileNotFoundException e1) {
			if (errorProcessor != null) {
				errorProcessor.accept(e1, templateEnum.name());
				throw new StopProcessingException(templateEnum.name(), e1);
			}
			return null;
		} catch (Exception e2) {
			logger.error("Unexpected exception: {}", e2.toString());
			errorProcessor.accept(e2, e2.getMessage());
			throw new StopProcessingException(templateEnum.name(), e2);
		}
	}

	private void checkNoSelection(List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		if (selectedItems == null || selectedItems.size() == 0) {
			Exception e = new Exception("NoSession");
			errorProcessor.accept(e, e.getMessage());
			throw new StopProcessingException(e.getMessage(), e);
		}
	}

	private void checkReasonableSelection(List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		if (selectedItems == null || selectedItems.size() == 0) {
			int nbAthletes = athletesFindAll(true).size();
			if (nbAthletes > 150) {
				Exception e = new Exception(Translator.translate("Documents.TooManyAthletes"));
				errorProcessor.accept(e, "TooManyAthletes");
				throw new StopProcessingException(e.getMessage(), e);
			}
		}
	}

	private Div createBodyweightButton() {
		return createTemplateDownloadButton(
		        PreCompetitionTemplates.BY_BODYWEIGHT,
		        null,
		        () -> prepareBodyweight(PreCompetitionTemplates.BY_BODYWEIGHT, getSortedSelection(), (ex, m) -> notifyError(ex, UI.getCurrent(), m)),
		        false);
	}

	private Div createCardsButton() {
		return createTemplateDownloadButton(
		        PreCompetitionTemplates.CARDS,
		        () -> checkReasonableSelection(getSortedSelection(), (ex, m) -> notifyError(ex, UI.getCurrent(), m)),
		        () -> prepareCards(PreCompetitionTemplates.CARDS, getSortedSelection(), (ex, m) -> notifyError(ex, UI.getCurrent(), m)),
		        true);
	}

	private Div createCategoriesButton() {
		return createTemplateDownloadButton(
		        PreCompetitionTemplates.BY_CATEGORY,
		        null,
		        () -> prepareCategories(PreCompetitionTemplates.BY_CATEGORY, getSortedSelection(), (ex, m) -> notifyError(ex, UI.getCurrent(), m)),
		        false);
	}

	private Div createCheckinButton() {
		return createTemplateDownloadButton(
		        PreCompetitionTemplates.CHECKIN,
		        null,
		        () -> prepareCheckin(PreCompetitionTemplates.CHECKIN, getSortedSelection(), (ex, m) -> notifyError(ex, UI.getCurrent(), m)),
		        false);
	}

	private Div createDoItButton(PreCompetitionTemplates template, Supplier<List<KitElement>> elementSupplier, Consumer<Throwable> doneCallback, Dialog dialog) {
		Div localDirZipDiv = null;
		UI ui = UI.getCurrent();
		ui.setLocale(OwlcmsSession.getLocale());
		localDirZipDiv = DownloadButtonFactory.createDynamicDownloadButton(
		        () -> stripSuffix(template.templateFileNameSupplier.get()),
		        Translator.translate(template.name()),
		        () -> {
			        System.err.println("*** createDoItButton running for " + template.name() + " elementSupplier=" + elementSupplier);
			        List<KitElement> elements = null;
			        try {
				        elements = elementSupplier.get();
			        } catch (StopProcessingException spe) {
				        System.err.println("*** createDoItButton got StopProcessingException for " + template.name() + ": " + spe.getMessage());
				        throw spe;
			        } catch (Throwable e) {
				        System.err.println("*** createDoItButton got Exception for " + template.name() + ": " + e.toString());
				        throw e;
			        }
				    System.err.println("*** createDoItButton got elements for " + template.name() + ": " + (elements == null ? "null" : elements.size()));
					// declare wrappedDone first so pre-checks can call it to report errors immediately
					java.util.function.Consumer<Throwable> wrappedDone = t -> {
						if (t == null) {
							ui.access(() -> dialog.close());
							return;
						}
						String s = t.getMessage() == null ? Translator.translate("Download.failed") : t.getMessage();
						ui.access(() -> {
							// Remove any existing processing/error paragraph with the canonical id, then add a single error paragraph
							java.util.Optional<Paragraph> existing = dialog.getChildren()
								.filter(c -> c instanceof Paragraph)
								.map(c -> (Paragraph) c)
								.filter(p -> "documents-processing".equals(p.getId().orElse(null)))
								.findFirst();
							if (existing.isPresent()) {
								Paragraph p = existing.get();
								p.setText(s);
								p.getStyle().set("color", "var(--lumo-error-text-color)");
								p.getStyle().set("font-weight", "bold");
								p.getStyle().set("text-align", "center");
								p.getStyle().set("font-size", "large");
							} else {
								Paragraph err = new Paragraph(s);
								err.setId("documents-processing");
								err.getStyle().set("color", "var(--lumo-error-text-color)");
								err.getStyle().set("font-weight", "bold");
								err.getStyle().set("text-align", "center");
								err.getStyle().set("font-size", "large");
								dialog.add(err);
							}
						});
					};

					// Run lightweight preChecks for each element to catch "no athletes" or "too many" before building streams
					if (elements != null) {
						List<Group> selectedSessions = getSortedSelection();
						Group g = (selectedSessions != null && selectedSessions.size() > 0) ? selectedSessions.get(0) : null;
						// If no session is selected, use all athletes for the pre-check (and enforce overall limits such as <100)
						List<Athlete> athletes = (g != null) ? groupAthletes(g, true) : athletesFindAll(true);
						for (KitElement ke : elements) {
							try {
								System.err.println("*** running preCheck for element=" + ke.id() + " session=" + (g == null ? "(all)" : g.toString()) + " athletes=" + (athletes == null ? 0 : athletes.size()));
								java.util.Optional<Exception> pre = ke.preCheck().apply(athletes, g);
								System.err.println("*** preCheck result for element=" + ke.id() + " -> " + (pre == null ? "null" : (pre.isPresent() ? "FAIL" : "OK")));
								if (pre != null && pre.isPresent()) {
									Exception e = pre.get();
									// log and show the validation error inside the dialog processing paragraph
									logger.warn("Pre-check failed for {}: {}", template.name(), e.getMessage());
									ui.access(() -> {
										try {
											dialog.getChildren()
												.filter(c -> c.getId().isPresent() && "documents-processing".equals(c.getId().get()))
												.findFirst()
												.ifPresent(c -> {
													if (c instanceof Paragraph) {
														((Paragraph) c).setText(Translator.translateExplicitLocale(e.getMessage(), ui.getLocale()));
													}
												});
										} catch (Exception ee) {
											LoggerUtils.logError(logger, ee, false);
										}
									});

									// delegate to wrappedDone so UI shows error consistently
									if (wrappedDone != null) {
										try { wrappedDone.accept(e); } catch (Throwable cb) { LoggerUtils.logError(logger, cb, true); }
									}
									return null;
								}
							} catch (Throwable t) {
								LoggerUtils.logError(logger, t, true);
								if (wrappedDone != null) {
									try { wrappedDone.accept(new Exception(t)); } catch (Throwable cb) { LoggerUtils.logError(logger, cb, true); }
								}
								return null;
							}
						}
					}
					feedback(dialog, ui);

					// Quick UI-thread sanity checks only: ensure we actually have template elements to
					// process. Heavy template resolution and writer-specific validation is performed
					// by the LazyDownloadButton.runPreCheck() (which calls JXLS/XLSX preCheck()) when
					// the user initiates the download; avoid duplicating that work here.
					System.err.println("*** createDoItButton running quick sanity checks for " + template.name());
					try {
						if (elements == null || elements.isEmpty()) {
							Exception e = new Exception("NoTemplates");
							if (wrappedDone != null) {
								try { wrappedDone.accept(e); } catch (Throwable cb) { LoggerUtils.logError(logger, cb, true); }
							}
							throw new StopProcessingException(e.getMessage(), e);
						}
					} catch (StopProcessingException spe) {
						throw spe;
					} catch (Exception e) {
						// unexpected quick-check failure
						LoggerUtils.logError(logger, e, true);
						if (wrappedDone != null) {
							try { wrappedDone.accept(e); } catch (Throwable cb) { LoggerUtils.logError(logger, cb, true); }
						}
						throw new StopProcessingException(e.getMessage(), e);
					}
					System.err.println("*** createDoItButton calling zipOrExcelInputStream for " + template.name());
					try {
						return zipOrExcelInputStream(ui, elements, wrappedDone);
					} catch (Exception e) {
						// Unexpected error during stream creation: log, notify the UI, and rethrow so the
						// download handler returns a 500 and the error is visible in server logs.
						LoggerUtils.logError(logger, e, true);
						try {
							if (wrappedDone != null) {
								wrappedDone.accept(e);
							}
						} catch (Throwable cb) {
							LoggerUtils.logError(logger, cb, true);
						}
						throw e;
					}
		        },
		        () -> {
			        String extension = FilenameUtils.getExtension(template.templateFileNameSupplier.get());
			        return (getSortedSelection().size() > 1 ? ".zip" : "." + extension);
		        });
		Button b = (Button) localDirZipDiv.getChildren().findFirst().get();
		b.focus();
		b.addClickListener(e -> b.setEnabled(false));
		b.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		return localDirZipDiv;
	}

	private Div createTemplateDownloadButton(PreCompetitionTemplates templateDefinition,
	        Runnable preAction,
	        Supplier<List<KitElement>> elementSupplier,
	        boolean primary) {
		UI ui = UI.getCurrent();
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        if (preAction != null) {
				        preAction.run();
			        }
			        Dialog dialog = new Dialog();
			        dialog.setWidth(DIALOG_WIDTH);
			        dialog.add(new TemplateSelectionFormFactory().singleTemplateSelection(templateDefinition));
			        dialog.getFooter().add(createDoItButton(
			                templateDefinition,
			                elementSupplier,
			                ev -> ui.access(() -> dialog.close()), dialog));
			        dialog.open();
		        });
		if (primary) {
			openDialog.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		}
		return new Div(openDialog);
	}

	private Div createEmptyProtocolButton() {
		UI ui = UI.getCurrent();
		PreCompetitionTemplates templateDefinition = PreCompetitionTemplates.EMPTY_PROTOCOL;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        checkNoSelection(getSortedSelection(), (ex, m) -> notifyError(ex, ui, m));
			        Dialog dialog = new Dialog();
			        dialog.setWidth(DIALOG_WIDTH);
			        dialog.add(new TemplateSelectionFormFactory().singleTemplateSelection(templateDefinition));
			        dialog.getFooter().add(createDoItButton(
			                templateDefinition,
			                () -> prepareEmptyProtocol(templateDefinition, getSortedSelection(), (ex, m) -> notifyError(ex, ui, m)),
			                ev -> ui.access(() -> dialog.close()), dialog));
			        dialog.open();
		        });
		// openDialog.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		return new Div(openDialog);
	}

	private Div createFullScheduleButton() {
		UI ui = UI.getCurrent();
		PreCompetitionTemplates templateDefinition = PreCompetitionTemplates.SCHEDULE;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        Dialog dialog = new Dialog();
			        dialog.setWidth(DIALOG_WIDTH);
			        dialog.add(new TemplateSelectionFormFactory().singleTemplateSelection(templateDefinition));
			        dialog.getFooter().add(createDoItButton(
			                templateDefinition,
			                () -> prepareSchedule(getSortedSelection(), (ex, m) -> notifyError(ex, ui, m)),
			                ev -> ui.access(() -> dialog.close()), dialog));
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
		// String translation = Translator.translate("EditAthletes");
		// int tSize = translation.length();
		// grid.addColumn(new ComponentRenderer<>(p -> {
		// Button editDetails = new Button(Translator.translate("Sessions.EditDetails"));
		// editDetails.addThemeVariants(ButtonVariant.LUMO_SMALL);
		// Button technical = openInNewTab(RegistrationContent.class, translation, p != null ? p.getName() : "?");
		// // prevent grid row selection from triggering
		// technical.getElement().addEventListener("click", ignore -> {
		// }).addEventData("event.stopPropagation()");
		// technical.addThemeVariants(ButtonVariant.LUMO_SMALL);
		// return new HorizontalLayout(editDetails, technical);
		// })).setHeader("").setAutoWidth(true);

		for (Column<Group> c : grid.getColumns()) {
			c.setResizable(true);
		}

		this.crud.setCrudListener(this);
		this.crud.setClickRowToUpdate(true);
		grid.setSelectionMode(SelectionMode.MULTI);
		return this.crud;
	}

	private Div createIntroductionButton() {
		UI ui = UI.getCurrent();
		PreCompetitionTemplates templateDefinition = PreCompetitionTemplates.INTRODUCTION;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        checkNoSelection(getSortedSelection(), (ex, m) -> notifyError(ex, ui, m));
			        Dialog dialog = new Dialog();
			        dialog.setWidth(DIALOG_WIDTH);
			        dialog.add(new TemplateSelectionFormFactory().singleTemplateSelection(templateDefinition));
			        dialog.getFooter().add(createDoItButton(
			                templateDefinition,
			                () -> prepareIntroduction(templateDefinition, getSortedSelection(), (ex, m) -> notifyError(ex, ui, m)),
			                ev -> ui.access(() -> dialog.close()), dialog));
			        dialog.open();
		        });
		// openDialog.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		return new Div(openDialog);
	}

	private Div createJuryButton() {
		UI ui = UI.getCurrent();
		PreCompetitionTemplates templateDefinition = PreCompetitionTemplates.JURY;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        checkNoSelection(getSortedSelection(), (ex, m) -> notifyError(ex, ui, m));
			        Dialog dialog = new Dialog();
			        dialog.setWidth(DIALOG_WIDTH);
			        dialog.add(new TemplateSelectionFormFactory().singleTemplateSelection(templateDefinition));
			        dialog.getFooter().add(createDoItButton(
			                templateDefinition,
			                () -> prepareJury(templateDefinition, getSortedSelection(), (ex, m) -> notifyError(ex, ui, m)),
			                ev -> ui.access(() -> dialog.close()), dialog));
			        dialog.open();
		        });
		// openDialog.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		return new Div(openDialog);
	}

	private Div createOfficialsButton() {
		UI ui = UI.getCurrent();
		PreCompetitionTemplates templateDefinition = PreCompetitionTemplates.OFFICIALS;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        checkNoSelection(getSortedSelection(), (ex, m) -> notifyError(ex, ui, m));
			        Dialog dialog = new Dialog();
			        dialog.setWidth(DIALOG_WIDTH);
			        dialog.add(new TemplateSelectionFormFactory().singleTemplateSelection(templateDefinition));
			        dialog.getFooter().add(createDoItButton(
			                templateDefinition,
			                () -> prepareOfficials(getSortedSelection(), (ex, m) -> notifyError(ex, ui, m)),
			                ev -> ui.access(() -> dialog.close()), dialog));
			        dialog.open();
		        });
		return new Div(openDialog);
	}

	private Div createPostWeighInButton() {
		UI ui = UI.getCurrent();
		Button openDialog = new Button(
		        Translator.translate("Documents.Kits"),
		        VaadinIcon.ARCHIVE.create(),
		        (e) -> {
			        checkNoSelection(getSortedSelection(), (ex, m) -> notifyError(ex, ui, m));
			        Dialog dialog = new Dialog();
			        dialog.setWidth(DIALOG_WIDTH);
			        dialog.add(new TemplateSelectionFormFactory().postWeighInTemplateSelectionForm(dialog));
			        dialog.getFooter().add(createPostWeighInButtonDoIt(dialog, ev -> ui.access(() -> dialog.close())));
			        dialog.open();
		        });
		return new Div(openDialog);
	}

	private Div createPostWeighInButtonDoIt(Dialog dialog, Consumer<Throwable> doneCallback) {
		Div localDirZipDiv = null;
		UI ui = UI.getCurrent();
		localDirZipDiv = DownloadButtonFactory.createDynamicZipDownloadButton(
		        "postWeighIn",
		        Translator.translate(PreCompetitionTemplates.POST_WEIGHIN.name()),
		        () -> {
			        List<KitElement> elements = preparePostWeighInKit(getSortedSelection(), (e, m) -> notifyError(e, ui, m));
			        feedback(dialog, ui);
			        return zipKitToInputStream(getSortedSelection(), elements, (e, m) -> notifyError(e, ui, m), doneCallback, ui);
		        },
		        VaadinIcon.ARCHIVE.create());
		return localDirZipDiv;
	}

	private Div createPreWeighInButton() {
		UI ui = UI.getCurrent();
		Button openDialog = new Button(
		        Translator.translate("Documents.Kits"),
		        VaadinIcon.ARCHIVE.create(),
		        (e) -> {
			        checkNoSelection(getSortedSelection(), (ex, m) -> notifyError(ex, ui, m));
			        Dialog dialog = new Dialog();
			        dialog.setWidth(DIALOG_WIDTH);
			        dialog.add(new TemplateSelectionFormFactory().preWeighInTemplateSelectionForm(dialog));
			        dialog.getFooter().add(createPreWeighInButtonDoIt(dialog, ev -> ui.access(() -> dialog.close())));
			        dialog.open();
		        });
		return new Div(openDialog);
	}

	private Div createPreWeighInButtonDoIt(Dialog dialog, Consumer<Throwable> doneCallback) {
		Div localDirZipDiv = null;
		UI ui = UI.getCurrent();
		localDirZipDiv = DownloadButtonFactory.createDynamicZipDownloadButton(
		        "preWeighIn",
		        Translator.translate(PreCompetitionTemplates.PRE_WEIGHIN.name()),
		        () -> {
			        List<KitElement> elements = preparePreWeighInKit(getSortedSelection(), (e, m) -> notifyError(e, ui, m));
			        feedback(dialog, ui);
			        return zipKitToInputStream(getSortedSelection(), elements, (e, m) -> notifyError(e, ui, m), doneCallback, ui);
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

	private Div createStartListButton() {
		UI ui = UI.getCurrent();
		PreCompetitionTemplates templateDefinition = PreCompetitionTemplates.START_LIST;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        Dialog dialog = new Dialog();
			        dialog.setWidth(DIALOG_WIDTH);
			        dialog.add(new TemplateSelectionFormFactory().singleTemplateSelection(templateDefinition));
			        dialog.getFooter().add(createDoItButton(
			                templateDefinition,
			                () -> prepareStartList(templateDefinition, getSortedSelection(), (ex, m) -> notifyError(ex, ui, m)),
			                ev -> ui.access(() -> dialog.close()), dialog));
			        dialog.open();
		        });
		openDialog.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		return new Div(openDialog);
	}

	private Div createTeamsButton() {
		UI ui = UI.getCurrent();
		PreCompetitionTemplates templateDefinition = PreCompetitionTemplates.BY_TEAM;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        Dialog dialog = new Dialog();
			        dialog.setWidth(DIALOG_WIDTH);
			        dialog.add(new TemplateSelectionFormFactory().singleTemplateSelection(templateDefinition));
			        dialog.getFooter().add(createDoItButton(
			                templateDefinition,
			                () -> prepareTeam(templateDefinition, getSortedSelection(), (ex, m) -> notifyError(ex, ui, m)),
			                ev -> ui.access(() -> dialog.close()), dialog));
			        dialog.open();
		        });
		// openDialog.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		return new Div(openDialog);
	}

	private Div createWeighInButton() {
		UI ui = UI.getCurrent();
		PreCompetitionTemplates templateDefinition = PreCompetitionTemplates.WEIGHIN;
		Button openDialog = new Button(
		        Translator.translate(templateDefinition.name()),
		        VaadinIcon.DOWNLOAD_ALT.create(),
		        (e) -> {
			        checkNoSelection(getSortedSelection(), (ex, m) -> notifyError(ex, ui, m));
			        Dialog dialog = new Dialog();
			        dialog.setWidth(DIALOG_WIDTH);
			        dialog.add(new TemplateSelectionFormFactory().singleTemplateSelection(templateDefinition));
			        dialog.getFooter().add(createDoItButton(
			                templateDefinition,
			                () -> prepareWeighIn(templateDefinition, getSortedSelection(), (ex, m) -> notifyError(ex, ui, m)),
			                ev -> ui.access(() -> dialog.close()), dialog));
			        dialog.open();
		        });
		// openDialog.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		return new Div(openDialog);
	}

    private KitElement doElementBodyweight(PreCompetitionTemplates templateDefinition, BiConsumer<Throwable, String> errorProcessor) {
	return checkKit("bodyweight",
		templateDefinition,
		errorProcessor,
		null,
		(a, ignored) -> {
			JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			startingXlsWriter.setGroup(null);
			// get current version of athletes.
			startingXlsWriter.setSortedAthletes(AthleteSorter.registrationBWCopy(athletesFindAll(false)));
			startingXlsWriter.createAgeGroupColumns(10, 7);
			return startingXlsWriter;
		});
	}

	private KitElement doElementCards(PreCompetitionTemplates templateDefinition, BiConsumer<Throwable, String> errorProcessor) {
		// CARDS: when a single session is selected require some athletes; when no session selected
		// use total athletes and require >0 and less than 100
		BiFunction<List<Athlete>, Group, java.util.Optional<Exception>> cardsPre = (a, g) -> {
			if (g != null) {
				if (a == null || a.isEmpty()) {
					return java.util.Optional.of(new Exception("NoAthletes"));
				}
				return java.util.Optional.empty();
			} else {
				int total = athletesFindAll(true).size();
				if (total == 0) {
					return java.util.Optional.of(new Exception("NoAthletes"));
				}
				if (total >= 100) {
					return java.util.Optional.of(new Exception("TooManyAthletes"));
				}
				return java.util.Optional.empty();
			}
		};

		return checkKit("cards",
				templateDefinition,
				errorProcessor,
				cardsPre,
				(a, g) -> {
						JXLSCardsDocs xlsWriter = new JXLSCardsDocs();
						List<Athlete> athletes;
						if (g == null) {
								athletes = athletesFindAll(true);
								athletes.sort(RegistrationOrderComparator.athleteSessionRegistrationOrderComparator);
						} else {
								athletes = a;
						}
						xlsWriter.setSortedAthletes(athletes);
						return xlsWriter;
				});
	}

    private KitElement doElementCategories(PreCompetitionTemplates template, BiConsumer<Throwable, String> errorProcessor) {
	return checkKit("categories",
		template,
		errorProcessor,
		null,
		(a, ignored) -> {
			JXLSCategoriesListDocs xlsWriter = new JXLSCategoriesListDocs();
			xlsWriter.setGroup(null);
			var athletes = participationFindAll();
			athletes.sort(RegistrationOrderComparator.athleteReportOrderComparator);
			xlsWriter.setSortedAthletes(athletes);
			return xlsWriter;
		});
	}

    private KitElement doElementCheckin(PreCompetitionTemplates template, BiConsumer<Throwable, String> errorProcessor) {
	return checkKit("checkin",
		template,
		errorProcessor,
		null,
		(a, ignored) -> {
			JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			startingXlsWriter.setGroup(null);
			startingXlsWriter.setPostProcessor(null);
			List<Athlete> athletesFindAll = athletesFindAll(true);
			logger.warn("Checkin athletes: {}", athletesFindAll.size());
			startingXlsWriter.setSortedAthletes(athletesFindAll);
			return startingXlsWriter;
		});
	}

    private KitElement doElementEmptyProtocol(PreCompetitionTemplates template, BiConsumer<Throwable, String> errorProcessor) {
	return checkKit("emptyProtocol",
		template,
		errorProcessor,
		null,
		(a, g) -> {
			AthleteRepository.assignStartNumbers(a);
			JXLSResultSheet rs = new JXLSResultSheet(false);
			rs.setGroup(g);
			rs.setSortedAthletes(a);
			return rs;
		});
	}

    private KitElement doElementIntroduction(PreCompetitionTemplates template, BiConsumer<Throwable, String> errorProcessor) {
	return checkKit("introduction",
		template,
		errorProcessor,
		null,
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

    private KitElement doElementJury(PreCompetitionTemplates template, BiConsumer<Throwable, String> errorProcessor) {
	return checkKit("jury",
		template,
		errorProcessor,
		null,
		(a, g) -> {
			AthleteRepository.assignStartNumbers(a);
			JXLSJurySheet rs = new JXLSJurySheet();
			rs.setGroup(g);
			rs.setSortedAthletes(a);
			return rs;
		});
	}

    private KitElement doElementOfficials(BiConsumer<Throwable, String> errorProcessor) {
	return checkKit("officials",
		PreCompetitionTemplates.OFFICIALS,
		errorProcessor,
		null,
		(a, ignored) -> {
			JXLSStartingListDocs xlsWriter = new JXLSStartingListDocs();
			xlsWriter.setGroup(null);
			xlsWriter.setSortedAthletes(List.of());
			xlsWriter.setEmptyOk(true);
			return xlsWriter;
		});
	}

    private KitElement doElementSchedule(BiConsumer<Throwable, String> errorProcessor) {
	return checkKit("schedule",
		PreCompetitionTemplates.SCHEDULE,
		errorProcessor,
		null,
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

    private KitElement doElementStartList(PreCompetitionTemplates templateDefinition, BiConsumer<Throwable, String> errorProcessor) {
	return checkKit("startList",
		templateDefinition,
		errorProcessor,
		null,
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

    private KitElement doElementTeam(PreCompetitionTemplates template, BiConsumer<Throwable, String> errorProcessor) {
	return checkKit("team",
		template,
		errorProcessor,
		null,
		(a, ignored) -> {
			JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			startingXlsWriter.setGroup(null);
			// get current version of athletes.
			startingXlsWriter.setSortedAthletes(AthleteSorter.registrationOrderCopy(athletesFindAll(false)));
			startingXlsWriter.createTeamColumns(9, 6);
			return startingXlsWriter;
		});
	}

    private KitElement doElementWeighIn(PreCompetitionTemplates template, BiConsumer<Throwable, String> errorProcessor) {
	return checkKit("weighin",
		template,
		errorProcessor,
		null,
		(a, g) -> {
			JXLSWeighInSheet rs = new JXLSWeighInSheet();
			rs.setGroup(g);
			return rs;
		});
	}

	private void doKitElement(KitElement elem, String seq, ZipOutputStream zipOut, Group g, List<Athlete> athletes) throws IOException {
		JXLSWorkbookStreamSource xlsWriter = elem.writerFactory.apply(athletes, g);

		// apply default if the factory did not set
		if (xlsWriter.getGroup() == null) {
			xlsWriter.setGroup(g);
		}
		if (xlsWriter.getSortedAthletes() == null) {
			xlsWriter.setSortedAthletes(athletes);
		}

		InputStream is = Files.newInputStream(elem.isp);
		xlsWriter.setInputStream(is);
		xlsWriter.setTemplateFileName(elem.name);
		InputStream in = xlsWriter.createInputStream();
		String name = seq + "_" + elem.id + "_" + g.getName() + "." + elem.extension;
		ZipUtils.zipStream(in, name, false, zipOut);
	}

	private void doNotification(String text) {
		this.getUI().get().access(() -> {
			logger.warn("DocumentsContent: showing error notification -> {}\n{}", text, LoggerUtils.stackTrace());
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

	private InputStream excelKitElement(List<Group> selectedSessions, List<KitElement> elements, UI ui, java.util.function.Consumer<Throwable> doneCallback) throws IOException {
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

		// writerFactory can apply custom sorting order to the athletes
		JXLSWorkbookStreamSource xlsWriter = elem.writerFactory.apply(athletes, g);
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

		InputStream is = Files.newInputStream(elem.isp);
		xlsWriter.setInputStream(is);
		xlsWriter.setTemplateFileName(elem.name);

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
			// Heavy writer-specific validation (template resolution, athlete validation) is
			// performed by the LazyDownloadButton.runPreCheck() on the UI thread when the
			// user clicks the download button. Here we only set UI and create the stream.
			xlsWriter.setUi(ui);
			logger.warn("======= Creating download for template: {}", elem.name);
			// if an exception happens here, it is caught in the caller, it needs to close the dialog.
			in = xlsWriter.createInputStream();
			return in;
		} catch (Exception e) {
			logger.warn("======= Exception creating download for template: {}", elem.name);
			LoggerUtils.logError(logger, e, true);
			// ensure the dialog (or processing indicator) is closed via the provided callback
			try {
				if (doneCallback != null) {
					logger.info("Invoking doneCallback to close dialog from DocumentsContent.excelKitElement catch: {}", LoggerUtils.stackTrace());
					// convert exception to Throwable and pass it
					try { doneCallback.accept(e); } catch (Throwable cb) { LoggerUtils.logError(logger, cb, true); }
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

	private void feedback(Dialog dialog, UI ui) {
		System.err.println("*** feedback called " + ui);
		ui.access(() -> {
			boolean zipping = getSortedSelection().size() > 1;
			Paragraph processing = new Paragraph(Translator.translateExplicitLocale(zipping ? "LongProcessing" : "Processing", ui.getLocale()));
			processing.setId("documents-processing");
			processing.getStyle().set("text-align", "center");
			processing.getStyle().set("font-size", "large");
			processing.getStyle().set("font-weight", "bold");
			dialog.add(processing);
		});
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
			logger./**/warn("jxls merging correction failed");
		}
	}

	private Platform getPlatform() {
		return null;
	}

	private List<Group> getSortedSelection() {
		return this.crud.getSelectedItems().stream().sorted(Group.groupWeighinTimeComparator).toList();
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

	private List<KitElement> prepareBodyweight(PreCompetitionTemplates templateDefinition, List<Group> selectedItems,
	        BiConsumer<Throwable, String> errorProcessor) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementBodyweight(templateDefinition, errorProcessor));
		return elements;
	}

	private List<KitElement> prepareCards(PreCompetitionTemplates templateDefinition, List<Group> selectedItems,
	        BiConsumer<Throwable, String> errorProcessor) {
		checkReasonableSelection(selectedItems, errorProcessor);
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementCards(templateDefinition, errorProcessor));
		return elements;
	}

	private List<KitElement> prepareCategories(PreCompetitionTemplates template, List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementCategories(template, errorProcessor));
		return elements;
	}

	private List<KitElement> prepareCheckin(PreCompetitionTemplates template, List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementCheckin(template, errorProcessor));
		return elements;
	}

	private List<KitElement> prepareEmptyProtocol(PreCompetitionTemplates template, List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		checkNoSelection(selectedItems, errorProcessor);
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementEmptyProtocol(template, errorProcessor));
		return elements;
	}

	private List<KitElement> prepareIntroduction(PreCompetitionTemplates template, List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		checkNoSelection(selectedItems, errorProcessor);
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementIntroduction(template, errorProcessor));
		return elements;
	}

	private List<KitElement> prepareJury(PreCompetitionTemplates template, List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		checkNoSelection(selectedItems, errorProcessor);
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementJury(template, errorProcessor));
		return elements;
	}

	private List<KitElement> prepareOfficials(List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementOfficials(errorProcessor));
		return elements;
	}

	private List<KitElement> preparePostWeighInKit(List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		checkNoSelection(selectedItems, errorProcessor);
		List<KitElement> elements = new ArrayList<>();
		KitElement kit = doElementIntroduction(PreCompetitionTemplates.INTRODUCTION, null);
		if (kit != null) {
			elements.add(kit);
		}

		KitElement kit2 = doElementEmptyProtocol(PreCompetitionTemplates.EMPTY_PROTOCOL, null);
		if (kit2 != null) {
			elements.add(kit2);
		}

		KitElement kit3 = doElementJury(PreCompetitionTemplates.JURY, null);
		if (kit3 != null) {
			elements.add(kit3);
		}
		return elements;
	}

	private List<KitElement> preparePreWeighInKit(List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		checkNoSelection(selectedItems, errorProcessor);
		List<KitElement> elements = new ArrayList<>();
		KitElement kit = doElementWeighIn(PreCompetitionTemplates.WEIGHIN, null);
		if (kit != null) {
			elements.add(kit);
		}

		KitElement kit2 = doElementCards(PreCompetitionTemplates.CARDS, null);
		if (kit2 != null) {
			elements.add(kit2);
		}
		return elements;
	}

	private List<KitElement> prepareSchedule(List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementSchedule(errorProcessor));
		return elements;
	}

	private List<KitElement> prepareStartList(PreCompetitionTemplates templateDefinition, List<Group> selectedItems,
	        BiConsumer<Throwable, String> errorProcessor) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementStartList(templateDefinition, errorProcessor));
		return elements;
	}

	private List<KitElement> prepareTeam(PreCompetitionTemplates template, List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementTeam(template, errorProcessor));
		return elements;
	}

	private List<KitElement> prepareWeighIn(PreCompetitionTemplates template, List<Group> selectedItems, BiConsumer<Throwable, String> errorProcessor) {
		checkNoSelection(selectedItems, errorProcessor);
		List<KitElement> elements = new ArrayList<>();
		elements.add(doElementWeighIn(template, errorProcessor));
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

			boolean anyNonEmpty = false;
			for (Group g : selectedItems) {
				// get current version of athletes.
				List<Athlete> athletes = groupAthletes(g, true);
				if (athletes == null || athletes.isEmpty()) {
					// skip empty session
					continue;
				}
				anyNonEmpty = true;

				for (KitElement elem : elements) {
					String seq = String.format("%02d", i);
					doKitElement(elem, seq, zipOut, g, athletes);
					i++;
				}
			}

			if (!anyNonEmpty) {
				Exception e = new Exception("NoSession");
				throw new StopProcessingException(e.getMessage(), e);
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
				try { if (dc != null) dc.accept(null); } catch (Throwable ignore) {}
			} catch (Throwable t) {
				// Ensure unexpected errors are logged; zipKitToOutputStream reports via errorProcessor
				LoggerUtils.logError(logger, t, true);
				try { if (dc != null) dc.accept(t); } catch (Throwable ignore) {}
				throw t;
			} finally {
				// Defensive cleanup of thread-local state copied into this thread via InheritableThreadLocal
				try { OwlcmsSessionThreadLocal.remove(); } catch (Throwable ignore) {}
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
			errorProcessor.accept(e, e.getMessage());
		}
	}

	private InputStream zipOrExcelInputStream(UI ui, List<KitElement> elements, Consumer<Throwable> doneCallback) {
		System.err.println("*** zipOrExcelInputStream called " + ui + " with elements " + elements);
		InputStream z;
		logger.warn("Generating {} for {} sessions", elements.size() > 1 ? "zip" : "excel", getSortedSelection().size());
		if (getSortedSelection().size() > 1 || elements.size() > 1) {
			z = zipKitToInputStream(getSortedSelection(), elements, (e, m) -> notifyError(e, ui, m), doneCallback, ui);
		} else {
			z = excelToInputStream(getSortedSelection(), elements, (e, m) -> notifyError(e, ui, m), doneCallback, ui);
		}
		return z;

	}

}
