/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
//import com.vaadin.componentfactory.EnhancedDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import app.owlcms.nui.preparation.DocumentDownloadDialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;

import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.Resource;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class JXLSDownloader {

	private String buttonLabel;
	private String dialogTitle;
	private Function<Competition, String> templateNameGetter;
	private BiConsumer<Competition, String> templateNameSetter;
	private Logger logger = (Logger) LoggerFactory.getLogger(JXLSDownloader.class);
	private String resourceDirectoryLocation;
	private Supplier<JXLSWorkbookStreamSource> streamSourceSupplier;
	private JXLSWorkbookStreamSource xlsWriter;
	private Anchor downloadAnchor;
	private Dialog dialog;
	private ComboBox<Resource> templateSelect;
	private String processingMessage;
	private Predicate<String> nameFilter;
	// Optional pre-check invoked before activating the download/dialog. Return Optional.empty() on OK
	// or Optional.of(Exception) to indicate a validation error that should be shown to the user.
	private Supplier<Optional<Exception>> preCheckSupplier;

	/**
	 * @param streamSourceSupplier lambda that creates a JXLSWorkbookStreamSource and sets its filters
	 * @param templateNameGetter   get last file name stored in Competition
	 * @param templateNameSetter   set last file name in Competition
	 * @param dialogTitle
	 * @param buttonLabel          label used dialog button
	 * @param resourceDirectory    Location where to look for templates
	 * @return
	 */
	public JXLSDownloader(
	        Supplier<JXLSWorkbookStreamSource> streamSourceSupplier,
	        String resourceDirectoryLocation,
	        Function<Competition, String> templateNameGetter,
	        BiConsumer<Competition, String> templateNameSetter,
	        String dialogTitle,
	        String buttonLabel) {
		this.logger.setLevel(Level.DEBUG);
		this.streamSourceSupplier = streamSourceSupplier;
		this.resourceDirectoryLocation = resourceDirectoryLocation;
		this.templateNameGetter = templateNameGetter;
		this.templateNameSetter = templateNameSetter;
		this.buttonLabel = buttonLabel;
		this.dialogTitle = dialogTitle;
	}

	/**
	 * @param streamSourceSupplier lambda that creates a JXLSWorkbookStreamSource and sets its filters
	 * @param templateName
	 * @param dialogTitle
	 * @param buttonLabel          label used dialog button
	 * @param resourceDirectory    Location where to look for templates
	 * @return
	 */
	public JXLSDownloader(
	        Supplier<JXLSWorkbookStreamSource> streamSourceSupplier,
	        String resourceDirectoryLocation,
	        String templateName,
	        String buttonLabel) {
		this.logger.setLevel(Level.DEBUG);
		this.streamSourceSupplier = streamSourceSupplier;
		this.resourceDirectoryLocation = resourceDirectoryLocation;
		this.templateNameGetter = c -> {
			return templateName;
		};
		this.templateNameSetter = (c, s) -> {
		};
		this.buttonLabel = Translator.translate("Download");
		this.dialogTitle = buttonLabel;
	}

	/**
	 * This constructor is used when downloading a known file. The given template name is used.
	 *
	 * @param streamSourceSupplier
	 * @param resourceDirectoryLocation
	 * @param templateName
	 * @param buttonLabel
	 * @param nameFilter
	 */
	public JXLSDownloader(
	        Supplier<JXLSWorkbookStreamSource> streamSourceSupplier,
	        String resourceDirectoryLocation,
	        String templateName,
	        String buttonLabel,
	        Predicate<String> nameFilter) {
		this.logger.setLevel(Level.DEBUG);
		this.streamSourceSupplier = streamSourceSupplier;
		this.resourceDirectoryLocation = resourceDirectoryLocation;
		this.templateNameGetter = c -> {
			return templateName;
		};
		this.templateNameSetter = (c, s) -> {
		};
		this.buttonLabel = buttonLabel;
		this.dialogTitle = buttonLabel; // no dialog
		this.nameFilter = nameFilter;
	}

	/**
	 * @return
	 */
	public Button createDownloadButton() {
		Button dialogOpen = new Button(this.dialogTitle, new Icon(VaadinIcon.DOWNLOAD_ALT),
				e -> {
						Dialog dialog = createDialog();
						dialog.open();

						// After opening the dialog, run the optional pre-check and show any
						// validation errors inside the dialog so the user sees them in-context.
						try {
							if (this.preCheckSupplier != null) {
								Optional<Exception> pre = this.preCheckSupplier.get();
								if (pre != null && pre.isPresent()) {
									Exception ex = pre.get();
									// If the dialog is our DocumentDownloadDialog, let it handle rendering
									if (dialog instanceof DocumentDownloadDialog) {
										DocumentDownloadDialog d = (DocumentDownloadDialog) dialog;
										List<Exception> errors = new ArrayList<>();
										errors.add(ex);
										d.reportPrecheckErrors(errors);
									} else {
										UI ui = UI.getCurrent();
										ui.access(() -> {
											// fallback: create an error paragraph
											String msg = ex.getMessage() == null ? Translator.translate("Download.failed") : ex.getMessage();
											Paragraph err = new Paragraph(msg);
											err.setId("documents-processing");
											err.getStyle().set("color", "var(--lumo-error-text-color)");
											err.getStyle().set("font-weight", "bold");
											err.getStyle().set("text-align", "center");
											err.getStyle().set("font-size", "large");
											dialog.add(err);
										});
									}
								}
							}
						} catch (Throwable t) {
							LoggerUtils.logError(logger, t);
						}
				});
		return dialogOpen;
	}

	/**
	 * Set an optional pre-check supplier invoked on the UI thread before the download dialog
	 * is opened. The supplier should return Optional.empty() when validation passes or
	 * Optional.of(Exception) to signal an error message to show to the user.
	 */
	public void setPreCheckSupplier(Supplier<Optional<Exception>> preCheckSupplier) {
		this.preCheckSupplier = preCheckSupplier;
	}

	public void setProcessingMessage(String processingMessage) {
		this.processingMessage = processingMessage;
	}

	private Dialog createDialog() {
		// Button innerButton = new Button(buttonLabel, new Icon(VaadinIcon.DOWNLOAD_ALT));
	this.dialog = new DocumentDownloadDialog();
		this.dialog.setCloseOnEsc(true);
		this.dialog.setHeaderTitle(this.dialogTitle);
		this.templateSelect = new ComboBox<>();

		HorizontalLayout templateSelection = new HorizontalLayout();
		templateSelection.setSpacing(false);

		this.templateSelect.setPlaceholder(Translator.translate("AvailableTemplates"));
		this.templateSelect.setHelperText(Translator.translate("SelectTemplate"));
		List<Resource> resourceList = new ResourceWalker().getResourceList(
		        this.resourceDirectoryLocation,
		        ResourceWalker::relativeName,
		        this.nameFilter,
		        OwlcmsSession.getLocale(),
		        Config.getCurrent().isLocalTemplatesOnly());
		List<Resource> prioritizedList = xlsxPriority(resourceList);
		this.templateSelect.setItems(prioritizedList);
		this.templateSelect.setValue(null);
		this.templateSelect.setWidth("15em");
		// templateSelect.getStyle().set("margin-left", "1em");
		this.templateSelect.getStyle().set("margin-right", "0.8em");


		try {
			// Competition.getTemplateFileName()
			// the getter should return a default if not set.
			String curTemplateName = this.templateNameGetter.apply(Competition.getCurrent());
			this.logger.debug("(1) curTemplateName {}", curTemplateName);
			// searchMatch should always return something unless the directory is empty.
			Resource found = searchMatch(prioritizedList, curTemplateName);
			this.logger.debug("(1) template found {}", found != null ? found.getFilePath() : null);

			templateSelection.add(this.templateSelect);
			this.dialog.add(templateSelection);
			this.templateSelect.setValue(found);
			processTemplateSelection(templateSelection, prioritizedList, found != null ? found.getFileName() : null);

			this.templateSelect.addValueChangeListener(e -> {
				updateTemplateSelection(templateSelection, prioritizedList, e);
			});

		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}

		return this.dialog;
	}

	private void updateTemplateSelection(HorizontalLayout templateSelection, List<Resource> prioritizedList,
	        ComponentValueChangeEvent<ComboBox<Resource>, Resource> e) {
		String newTemplateName = e.getValue().getFileName();
		processTemplateSelection(templateSelection, prioritizedList, newTemplateName);
	}

	private void processTemplateSelection(HorizontalLayout templateSelection, List<Resource> prioritizedList, String newTemplateName) {
		try {
			try {
				this.downloadAnchor.setEnabled(false);
				this.downloadAnchor.getElement().getChild(0).setEnabled(false);
			} catch (Exception e) {
			}
			UI ui = UI.getCurrent();
			ui.push();
			Competition current = Competition.getCurrent();

			// supplier is a lambda that sets the template and the filter values in the xls
			// source
		       Resource res = searchMatch(prioritizedList, newTemplateName);
		       if (res == null) {
			       this.logger.debug("(2) template NOT found {} {} - waiting for user to select a template", newTemplateName, prioritizedList);
			       return;
		       }
		       this.logger.debug("(2) template found {}", res.getFilePath());
		       this.templateNameSetter.accept(current, newTemplateName);
		       this.logger.debug("(2) template as set {}", this.templateNameGetter.apply(current));

		       this.xlsWriter = this.streamSourceSupplier.get();
		       this.logger.debug("(2) xlsWriter dialog {} {}", this.xlsWriter, this.dialog);
		       if (this.xlsWriter == null) {
			       ui.access(() -> this.dialog.close());
			       return;
		       }
		       this.logger.debug("(2) xlsWriter {} {}", this.xlsWriter.getClass().getSimpleName(),
			       newTemplateName);

		       CompetitionRepository.save(current);
		       current = Competition.getCurrent();
		       this.logger.debug("(2) template as stored {}", this.templateNameGetter.apply(current));

			   // Do not run prechecks here. The precheck callback/validation is handled by the
			   // centralized pre-check flow elsewhere. If no template is selected we return early
			   // (caller will show appropriate UI); otherwise obtain the template stream and set it.
			   InputStream is = res.getStream();
			   this.xlsWriter.setInputStream(is);
		       this.logger.debug("(2) filter present = {} {} {}", this.xlsWriter.getGroup(),
			       this.xlsWriter.getCategory(),
			       this.xlsWriter.getChampionship());

		       String targetFileName = getTargetFileName();
		       this.logger.debug("(2) targetFileName final = {}", targetFileName);

		       Supplier<String> supplier = () -> getTargetFileName();

			Anchor nDownloadAnchor = doCreateActualDownloadButton(this.xlsWriter, supplier.get());
		       // if downloadAnchor is null, same as add nDownloadAnchor
		       templateSelection.replace(this.downloadAnchor, nDownloadAnchor);
		       this.downloadAnchor = nDownloadAnchor;

		       this.xlsWriter.setDoneCallback((t) -> ui.access(() -> {
		       	   if (t == null) {
		       		   // success: close dialog
		       		   this.dialog.close();
		       	   } else {
		       		   // close dialog and show an error notification with the throwable message
		       		   this.dialog.close();
		       		   String msg = t.getMessage() == null ? Translator.translate("Download.failed") : t.getMessage();
		       		   Notification err = new Notification(msg);
		       		   err.addThemeVariants(NotificationVariant.LUMO_ERROR);
		       		   err.setPosition(Position.TOP_END);
		       		   err.setDuration(0); // keep open until dismissed
		       		   err.open();
		       	   }
		       }));
		} catch (Throwable e1) {
			this.logger.error("{}", LoggerUtils.stackTrace(e1));
		}

		// After processing template selection, clear any processing messages and run the optional preCheckSupplier
		try {
			if (this.dialog instanceof DocumentDownloadDialog) {
				DocumentDownloadDialog d = (DocumentDownloadDialog) this.dialog;
				// Clear any previous processing/message
				d.clearProcessing();
				// Run optional pre-check and show any errors inside the dialog
				if (this.preCheckSupplier != null) {
					Optional<Exception> pre = this.preCheckSupplier.get();
					if (pre != null && pre.isPresent()) {
						List<Exception> errors = new ArrayList<>();
						errors.add(pre.get());
						d.reportPrecheckErrors(errors);
					} else {
						d.clearProcessing();
					}
				}
			}
		} catch (Throwable ignore) {}
	}

       private Anchor doCreateActualDownloadButton(JXLSWorkbookStreamSource writer, String fileName) {
	       DownloadHandler downloadHandler = event -> {
		       event.setFileName(fileName);
		       try (InputStream is = writer.createInputStream()) {
			       is.transferTo(event.getOutputStream());
		       } catch (Exception ex) {
			       this.logger.error("Download error: {}", LoggerUtils.stackTrace(ex));
		       }
	       };
	       Anchor link = new Anchor(downloadHandler, "");
	       link.getElement().setAttribute("download", true);
	       Button innerButton = new Button(this.buttonLabel, new Icon(VaadinIcon.DOWNLOAD_ALT));
	       link.add(innerButton);
	       innerButton.setDisableOnClick(true);
			   innerButton.addClickListener((c) -> {
			   this.templateSelect.setEnabled(false);
			   if (this.dialog instanceof DocumentDownloadDialog) {
				   ((DocumentDownloadDialog) this.dialog).showProcessing(getProcessingMessage());
			   } else {
				   this.dialog.add(new Paragraph(getProcessingMessage()));
			   }
		   });
	       innerButton.focus();
	       // highlight because Vaadin does not show a focus ring for some unknown reason
	       innerButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_PRIMARY);
	       return link;
       }

	private String getProcessingMessage() {
		return this.processingMessage == null ? Translator.translate("Processing") : this.processingMessage;
	}

	private String getTargetFileName() {
		StringBuilder suffix = new StringBuilder();
		if (this.xlsWriter.getCategory() != null) {
			suffix.append("_");
			suffix.append(this.xlsWriter.getCategory().getCode());
		} else if (this.xlsWriter.getAgeGroupPrefix() != null) {
			suffix.append("_");
			suffix.append(this.xlsWriter.getAgeGroupPrefix().toString());
		} else if (this.xlsWriter.getChampionship() != null) {
			suffix.append("_");
			suffix.append(this.xlsWriter.getChampionship().getName());
		}

		if (this.xlsWriter.getGroup() != null) {
			suffix.append("_");
			suffix.append(this.xlsWriter.getGroup().toString());
		}
		LocalDateTime now = LocalDateTime.now().withNano(0);
		suffix.append("_");
		suffix.append(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm';'ss")));

		String fileName = "";
		String templateName = this.templateNameGetter.apply(Competition.getCurrent());

	String extension = FilenameUtils.getExtension(templateName);
		if ((templateName.matches(".*[_-](A4|LETTER|LEGAL).*"))) {
			fileName = templateName.replaceAll("[_-](A4|LETTER|LEGAL)(." + extension + ")", "") + suffix + "."
			        + extension;
		} else {
			fileName = templateName.replaceAll("[.]" + extension, "") + suffix + "." + extension;
		}

		// if (outputFileName != null) {
		// fileName = outputFileName + suffix + "." + extension;
		// } else {
		// fileName = "output" + suffix + "." + extension;
		// }
		fileName = sanitizeFilename(fileName);
		this.logger.trace(fileName);
		return fileName;
	}

	private String sanitizeFilename(String name) {
		return name.replaceAll("[:\\\\/*?|<>]", "_");
	}

	private Resource searchMatch(List<Resource> resourceList, String curTemplateName) {
		Resource found = null;
		for (Resource curResource : resourceList) {
			String fileName = curResource.getFileName();
			this.logger.trace("comparing {} {}", fileName, curTemplateName);
			if (fileName.equals(curTemplateName)) {
				found = curResource;
				break;
			}
		}
		return found;
	}

	/**
	 * give precedence to .xlsx file if both .xls and .xlsx
	 *
	 * @param resourceList
	 * @return
	 */
	private List<Resource> xlsxPriority(List<Resource> resourceList) {
		// xlsx will come before xls
		resourceList.sort(Comparator.comparing(Resource::getFileName).reversed());

		ArrayList<Resource> proritizedList = new ArrayList<>();
		String prevName = "";
		for (Resource r : resourceList) {
			String curName = r.getFileName();
			// give precedence to .xlsx file if both .xls and .xlsx
			if (curName.endsWith(".xlsm") || curName.endsWith(".xlsx") || (curName.endsWith(".xls") && !prevName.contentEquals(curName + "x"))) {
				proritizedList.add(r);
			}
			prevName = curName;
		}
		proritizedList.sort(Comparator.comparing(Resource::getFileName));
		return proritizedList;
	}

}
