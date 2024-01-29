/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
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

import org.apache.maven.shared.utils.io.FileUtils;
import org.slf4j.LoggerFactory;

//import com.vaadin.componentfactory.EnhancedDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceWriter;

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
	private StreamResource resource;

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
		logger.setLevel(Level.DEBUG);
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
		logger.setLevel(Level.DEBUG);
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
		logger.setLevel(Level.DEBUG);
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
		Button dialogOpen = new Button(dialogTitle, new Icon(VaadinIcon.DOWNLOAD_ALT),
		        e -> {
			        Dialog dialog = createDialog();
			        dialog.open();
		        });
		return dialogOpen;
	}

	public Anchor createImmediateDownloadButton() {
		xlsWriter = streamSourceSupplier.get();
		Supplier<String> supplier = () -> getTargetFileName();
		resource = new StreamResource(supplier.get(), (StreamResourceWriter) xlsWriter);
		Anchor link = new Anchor(resource, "");
		link.getElement().setAttribute("download", true);
		Button innerButton = new Button(buttonLabel, new Icon(VaadinIcon.DOWNLOAD_ALT));
		innerButton.setWidth("100%");
		link.add(innerButton);
		return link;
	}

	public void setProcessingMessage(String processingMessage) {
		this.processingMessage = processingMessage;
	}

	private Dialog createDialog() {
		// Button innerButton = new Button(buttonLabel, new Icon(VaadinIcon.DOWNLOAD_ALT));
		dialog = new Dialog();
		dialog.setCloseOnEsc(true);
		dialog.setHeaderTitle(dialogTitle);
		templateSelect = new ComboBox<Resource>();

		HorizontalLayout templateSelection = new HorizontalLayout();
		templateSelection.setSpacing(false);

		templateSelect.setPlaceholder(Translator.translate("AvailableTemplates"));
		templateSelect.setHelperText(Translator.translate("SelectTemplate"));
		List<Resource> resourceList = new ResourceWalker().getResourceList(
		        resourceDirectoryLocation,
		        ResourceWalker::relativeName,
		        nameFilter,
		        OwlcmsSession.getLocale(),
		        Config.getCurrent().isLocalTemplatesOnly());
		List<Resource> prioritizedList = xlsxPriority(resourceList);
		templateSelect.setItems(prioritizedList);
		templateSelect.setValue(null);
		templateSelect.setWidth("15em");
		// templateSelect.getStyle().set("margin-left", "1em");
		templateSelect.getStyle().set("margin-right", "0.8em");

		resource = null;

		try {
			// Competition.getTemplateFileName()
			// the getter should return a default if not set.
			String curTemplateName = templateNameGetter.apply(Competition.getCurrent());
			logger.debug("(1) curTemplateName {}", curTemplateName);
			// searchMatch should always return something unless the directory is empty.
			Resource found = searchMatch(prioritizedList, curTemplateName);
			logger.debug("(1) template found {}", found != null ? found.getFilePath() : null);

			templateSelect.addValueChangeListener(e -> {
				try {
					String newTemplateName = e.getValue().getFileName();

					Competition current = Competition.getCurrent();

					xlsWriter = streamSourceSupplier.get();
					logger.trace("(2) xlsWriter {} {}", xlsWriter, newTemplateName);

					// supplier is a lambda that sets the template and the filter values in the xls
					// source
					Resource res = searchMatch(prioritizedList, newTemplateName);
					if (res == null) {
						logger.debug("(2) template NOT found {} {}", newTemplateName, prioritizedList);
						throw new Exception("template not found " + newTemplateName);
					}
					logger.debug("(2) template found {}", res != null ? res.getFilePath() : null);
					templateNameSetter.accept(current, newTemplateName);
					logger.debug("(2) template as set {}", templateNameGetter.apply(current));

					CompetitionRepository.save(current);
					current = Competition.getCurrent();
					logger.debug("(2) template as stored {}", templateNameGetter.apply(current));

					InputStream is = res.getStream();
					xlsWriter.setInputStream(is);
					logger.debug("(2) filter present = {} {} {}", xlsWriter.getGroup(), xlsWriter.getCategory(),
					        xlsWriter.getAgeDivision());

					String targetFileName = getTargetFileName();
					logger.debug("(2) targetFileName final = {}", targetFileName);

					Supplier<String> supplier = () -> getTargetFileName();

					Anchor nDownloadAnchor = doCreateActualDownloadButton(resource, xlsWriter, supplier.get());
					// if downloadAnchor is null, same as add nDownloadAnchor
					templateSelection.replace(downloadAnchor, nDownloadAnchor);
					downloadAnchor = nDownloadAnchor;

					xlsWriter.setDoneCallback((message) -> dialog.close());

					// downloadButton.setFileNameCallback(supplier);
					// downloadButton.setInputStreamCallback(() -> xlsWriter.createInputStream());
					// downloadButton.addDownloadStartsListener(ds -> dialog.close());
				} catch (Throwable e1) {
					logger.error("{}", LoggerUtils.stackTrace(e1));
				}
			});
			templateSelection.add(templateSelect);
			dialog.add(templateSelection);
			templateSelect.setValue(found);
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}

		return dialog;
	}

	/**
	 * give precedence to .xlsx file if both .xls and .xlsx
	 * @param resourceList
	 * @return
	 */
	private List<Resource> xlsxPriority(List<Resource> resourceList) {
		// xlsx will come before xls
		resourceList.sort(Comparator.comparing(Resource::getFileName).reversed());

		ArrayList<Resource> proritizedList = new ArrayList<Resource>();
		String prevName = "";
		for (Resource r : resourceList) {
			String curName = r.getFileName();
			// give precedence to .xlsx file if both .xls and .xlsx
			if (curName.endsWith(".xlsx") || (curName.endsWith(".xls") && !prevName.contentEquals(curName + "x"))) {
				proritizedList.add(r);
			}
			prevName = curName;
		}
		proritizedList.sort(Comparator.comparing(Resource::getFileName));
		return proritizedList;
	}

	private Anchor doCreateActualDownloadButton(StreamResource resource, StreamResourceWriter writer, String fileName) {
		resource = new StreamResource(fileName, writer);
		Anchor link = new Anchor(resource, "");
		link.getElement().setAttribute("download", true);
		Button innerButton = new Button(buttonLabel, new Icon(VaadinIcon.DOWNLOAD_ALT));
		link.add(innerButton);
		innerButton.setDisableOnClick(true);
		innerButton.addClickListener((c) -> {
			templateSelect.setEnabled(false);
			dialog.add(new Paragraph(getProcessingMessage()));
		});
		return link;
	}

	private String getProcessingMessage() {
		return processingMessage == null ? Translator.translate("Processing") : processingMessage;
	}

	private String getTargetFileName() {
		StringBuilder suffix = new StringBuilder();
		if (xlsWriter.getCategory() != null) {
			suffix.append("_");
			suffix.append(xlsWriter.getCategory().getCode());
		} else if (xlsWriter.getAgeGroupPrefix() != null) {
			suffix.append("_");
			suffix.append(xlsWriter.getAgeGroupPrefix().toString());
		} else if (xlsWriter.getAgeDivision() != null) {
			suffix.append("_");
			suffix.append(xlsWriter.getAgeDivision().toString());
		}

		if (xlsWriter.getGroup() != null) {
			suffix.append("_");
			suffix.append(xlsWriter.getGroup().toString());
		}
		LocalDateTime now = LocalDateTime.now().withNano(0);
		suffix.append("_");
		suffix.append(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm';'ss")));

		String fileName = "";
		String templateName = templateNameGetter.apply(Competition.getCurrent());
		String extension = FileUtils.getExtension(templateName);
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
		logger.trace(fileName);
		return fileName;
	}

	private String sanitizeFilename(String name) {
		return name.replaceAll("[:\\\\/*?|<>]", "_");
	}

	private Resource searchMatch(List<Resource> resourceList, String curTemplateName) {
		Resource found = null;
		for (Resource curResource : resourceList) {
			String fileName = curResource.getFileName();
			logger.trace("comparing {} {}", fileName, curTemplateName);
			if (fileName.equals(curTemplateName)) {
				found = curResource;
				break;
			}
		}
		return found;
	}

}
