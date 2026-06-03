/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;

import app.owlcms.data.records.RecordDefinitionReader;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordImportImpact;
import app.owlcms.data.records.RecordImportImpactRow;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

/**
 * Two-step import dialog for record spreadsheets.
 *
 * Step 1: the user picks an .xls/.xlsx file; it is parsed in memory and a
 *         preview of the impact (records to add, replace, remove, skip) is shown.
 * Step 2: the user clicks "Confirm Import" to persist the parsed records.
 *
 * The locale is captured in the constructor (UI thread) so upload callbacks are safe.
 */
@SuppressWarnings("serial")
public class RecordImportDialog extends Dialog {

	public static final Logger logger = (Logger) LoggerFactory.getLogger(RecordImportDialog.class);

	private final Runnable afterImport;
	private final Locale capturedLocale;

	private List<RecordEvent> pendingRecords;
	private String pendingFileName;
	private String pendingBaseName;

	private Button confirmButton;
	private VerticalLayout previewArea;

	public RecordImportDialog(Runnable afterImport) {
		this.afterImport = afterImport;
		// Capture locale on UI thread BEFORE any upload callback executes.
		this.capturedLocale = OwlcmsSession.getLocale();

		setWidth("70em");
		setHeaderTitle(translate("Records.ImportDialog.Title"));

		VerticalLayout content = new VerticalLayout();
		content.setPadding(false);
		content.setSpacing(true);

		Paragraph instructions = new Paragraph(translate("Records.ImportDialog.Instructions"));

		UploadHandler uploadHandler = UploadHandler.inMemory((metadata, bytes) -> {
			handleUpload(metadata.fileName(), bytes);
		}).whenStart(() -> {
			clearPendingImport();
			clearPreview();
			this.confirmButton.setEnabled(false);
		});

		Upload upload = new Upload(uploadHandler);
		upload.setAcceptedFileTypes(".xls", ".xlsx");
		upload.setWidth("40em");

		this.previewArea = new VerticalLayout();
		this.previewArea.setPadding(false);
		this.previewArea.setSpacing(true);
		this.previewArea.setVisible(false);

		content.add(instructions, upload, this.previewArea);
		add(content);

		// Footer
		this.confirmButton = new Button(translate("Records.ImportDialog.ConfirmButton"), e -> doImport());
		this.confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		this.confirmButton.setEnabled(false);

		Button cancelButton = new Button(translate("Cancel"), e -> close());
		getFooter().add(cancelButton, this.confirmButton);
	}

	private void handleUpload(String fileName, byte[] bytes) {
		List<String> errors = new ArrayList<>();
		try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		        Workbook workbook = WorkbookFactory.create(bais)) {

			String baseName = FilenameUtils.removeExtension(fileName);
			RecordDefinitionReader reader = new RecordDefinitionReader(this.capturedLocale);
			List<RecordEvent> parsed = reader.parseRecordsFromWorkbook(workbook, fileName, baseName, errors);

			this.pendingRecords = parsed;
			this.pendingFileName = fileName;
			this.pendingBaseName = baseName;

			RecordImportImpact impact = reader.previewImport(parsed);
			showPreview(impact, errors);
			this.confirmButton.setEnabled(!parsed.isEmpty());

		} catch (Exception e) {
			logger.error("Error reading records file {}", fileName, e);
			clearPendingImport();
			clearPreview();
			this.previewArea.add(new Span(translate("Records.couldNotProcess", fileName)));
			this.previewArea.setVisible(true);
			this.confirmButton.setEnabled(false);
		}
	}

	private void showPreview(RecordImportImpact impact, List<String> errors) {
		clearPreview();

		this.previewArea.add(new H3(translate("Records.ImportDialog.PreviewTitle")));
		this.previewArea.add(summaryLine("Records.ImportDialog.TotalRecords", impact.getTotalImported()));
		this.previewArea.add(summaryLine("Records.ImportDialog.OfficialImported", impact.getOfficialImported()));
		this.previewArea.add(summaryLine("Records.ImportDialog.ProvisionalImported", impact.getProvisionalImported()));
		this.previewArea.add(summaryLine("Records.ImportDialog.OfficialToReplace", impact.getOfficialToReplace()));
		this.previewArea.add(summaryLine("Records.ImportDialog.ProvisionalToRemove", impact.getProvisionalToRemove()));
		this.previewArea.add(summaryLine("Records.ImportDialog.DuplicateSkipped", impact.getDuplicateProvisionalToSkip()));

		if (!impact.getRows().isEmpty()) {
			Grid<RecordImportImpactRow> grid = new Grid<>(RecordImportImpactRow.class, false);
			grid.addColumn(RecordImportImpactRow::getRecordFederation)
			        .setHeader(translate("Records.ImportDialog.Col.Federation")).setAutoWidth(true);
			grid.addColumn(RecordImportImpactRow::getRecordName)
			        .setHeader(translate("Records.ImportDialog.Col.RecordName")).setAutoWidth(true);
			grid.addColumn(RecordImportImpactRow::getAgeGrp)
			        .setHeader(translate("Records.ImportDialog.Col.AgeGroup")).setAutoWidth(true);
			grid.addColumn(RecordImportImpactRow::getImportedCount)
			        .setHeader(translate("Records.ImportDialog.Col.Imported")).setAutoWidth(true);
			grid.addColumn(RecordImportImpactRow::getOfficialToReplace)
			        .setHeader(translate("Records.ImportDialog.Col.OfficialToReplace")).setAutoWidth(true);
			grid.addColumn(RecordImportImpactRow::getProvisionalToRemove)
			        .setHeader(translate("Records.ImportDialog.Col.ProvisionalToRemove")).setAutoWidth(true);
			grid.addColumn(RecordImportImpactRow::getDuplicateProvisionalToSkip)
			        .setHeader(translate("Records.ImportDialog.Col.DuplicateSkipped")).setAutoWidth(true);
			grid.setItems(impact.getRows());
			grid.setAllRowsVisible(true);
			this.previewArea.add(grid);
		}

		if (!errors.isEmpty()) {
			Paragraph errorPara = new Paragraph(String.join("\n", errors));
			errorPara.getStyle().set("color", "var(--lumo-error-color)");
			this.previewArea.add(errorPara);
		}

		this.previewArea.setVisible(true);
	}

	private Span summaryLine(String key, int value) {
		return new Span(translate(key) + ": " + value);
	}

	private String translate(String key, Object... params) {
		return new Translator().getTranslationExplicitLocale(key, this.capturedLocale, params);
	}

	private void clearPreview() {
		this.previewArea.removeAll();
		this.previewArea.setVisible(false);
	}

	private void clearPendingImport() {
		this.pendingRecords = null;
		this.pendingFileName = null;
		this.pendingBaseName = null;
	}

	private void doImport() {
		if (this.pendingRecords == null || this.pendingRecords.isEmpty()) {
			return;
		}
		try {
			new RecordDefinitionReader(this.capturedLocale)
			        .importParsedRecords(this.pendingRecords, this.pendingFileName, this.pendingBaseName);
		} catch (Exception e) {
			logger.error("Error persisting records from {}", this.pendingFileName, e);
		}
		this.afterImport.run();
		close();
	}
}
