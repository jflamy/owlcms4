package app.owlcms.nui.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.nui.admin.BirthDateRepairService.BirthDateRepairPreview;
import app.owlcms.nui.admin.BirthDateRepairService.BirthDateRepairResult;
import app.owlcms.nui.admin.BirthDateRepairService.BirthDateRepairRow;

@SuppressWarnings("serial")
final class BirthDateRepairDialog extends Dialog {

	BirthDateRepairDialog(Button repairButton) {
		BirthDateRepairPreview preview = BirthDateRepairService.preview();
		Set<Long> skippedAthleteIds = new HashSet<>();
		Span skippedSummary = new Span("Skipped athletes: 0");

		setHeaderTitle("Repair Birth Dates");
		setWidth("72em");
		setMaxWidth("calc(100vw - 2rem)");
		setCloseOnEsc(true);
		setCloseOnOutsideClick(false);

		VerticalLayout content = new VerticalLayout();
		content.setPadding(false);
		content.setSpacing(true);
		content.setAlignItems(FlexComponent.Alignment.STRETCH);
		content.getStyle().set("min-height", "0");

		content.add(warningText());
		content.add(summary(preview, skippedSummary));
		addReviewRows(content, preview, skippedAthleteIds, skippedSummary);

		Button cancel = new Button("Cancel", event -> close());
		Button apply = new Button("Apply Repair", event -> {
			ConfirmationDialog confirmationDialog = new ConfirmationDialog(
			        "Apply Birth Date Repair",
			        confirmationQuestion(preview, skippedAthleteIds.size()),
			        "Apply Repair",
			        null,
			        null,
			        () -> {
				        BirthDateRepairResult result = BirthDateRepairService.apply(Set.copyOf(skippedAthleteIds),
				                AccessUtils.getClientIp());
				        repairButton.setEnabled(false);
				        removeAll();
				        setHeaderTitle("Birth Date Repair Applied");
				        add(successContent(result));
			        });
			confirmationDialog.open();
		});
		apply.setEnabled(preview.getTotalCount() > 0);

		HorizontalLayout buttons = new HorizontalLayout(cancel, apply);
		buttons.setWidthFull();
		buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

		content.add(buttons);
		add(content);
	}

	private VerticalLayout warningText() {
		VerticalLayout warning = new VerticalLayout();
		warning.setPadding(false);
		warning.setSpacing(false);

		warning.add(new Paragraph("This emergency action adds one day to every athlete birth date in the database."));
		warning.add(new Paragraph(
		        "Use it only for databases where athlete birth dates were mass-imported from a registration sheet before the birth-date storage fix and all imported DOB values are known to be one day too early."));
		warning.add(new Paragraph(
		        "This action cannot distinguish corrupted dates from correct dates. Review Jan 1 and Dec 31 athletes carefully before applying. Some Jan 1 or Dec 31 values may have been manually corrected already and may need separate handling."));
		warning.add(new Paragraph("Export or back up the database before continuing."));

		warning.getStyle().set("color", "var(--lumo-error-text-color)");
		return warning;
	}

	private VerticalLayout summary(BirthDateRepairPreview preview, Span skippedSummary) {
		VerticalLayout summary = new VerticalLayout();
		summary.setPadding(false);
		summary.setSpacing(false);
		summary.add(new Span("Athletes with birth dates: " + preview.getTotalCount()));
		summary.add(new Span("Jan 1 athletes: " + preview.getJan1Count()));
		summary.add(new Span("Dec 31 athletes: " + preview.getDec31Count()));
		summary.add(skippedSummary);
		return summary;
	}

	private void addReviewRows(VerticalLayout content, BirthDateRepairPreview preview, Set<Long> skippedAthleteIds,
	        Span skippedSummary) {
		List<BirthDateRepairRow> rowsToReview = preview.getRows().stream()
		        .filter(row -> row.isJan1() || row.isDec31())
		        .toList();

		if (rowsToReview.isEmpty()) {
			return;
		}

		content.add(new H3("Jan 1 / Dec 31 Rows To Review"));
		Grid<BirthDateRepairRow> reviewGrid = baseGrid(rowsToReview, skippedAthleteIds, skippedSummary);
		content.add(reviewGrid);
		content.setFlexGrow(1, reviewGrid);

		setHeight("min(52rem, calc(100vh - 2rem))");
		content.setHeightFull();
	}

	private Grid<BirthDateRepairRow> baseGrid(List<BirthDateRepairRow> rows, Set<Long> skippedAthleteIds,
	        Span skippedSummary) {
		Grid<BirthDateRepairRow> grid = new Grid<>(BirthDateRepairRow.class, false);
		grid.addColumn(BirthDateRepairRow::getId).setHeader("ID").setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(BirthDateRepairRow::getLotNumber).setHeader("Lot").setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(BirthDateRepairRow::getLastName).setHeader("Last Name").setAutoWidth(true);
		grid.addColumn(BirthDateRepairRow::getFirstName).setHeader("First Name").setAutoWidth(true);
		grid.addColumn(BirthDateRepairRow::getCurrentBirthDate).setHeader("Current DOB").setAutoWidth(true);
		grid.addColumn(BirthDateRepairRow::getRepairedBirthDate).setHeader("After Repair").setAutoWidth(true);
		grid.addColumn(new ComponentRenderer<>(row -> skipCheckbox(row, skippedAthleteIds, skippedSummary)))
		        .setHeader("Skip")
		        .setAutoWidth(true)
		        .setFlexGrow(0);
		grid.setWidthFull();
		grid.setHeightFull();
		grid.setMinHeight("10rem");
		grid.setItems(rows);
		grid.getThemeNames().add("row-stripes");
		return grid;
	}

	private Checkbox skipCheckbox(BirthDateRepairRow row, Set<Long> skippedAthleteIds, Span skippedSummary) {
		Checkbox checkbox = new Checkbox();
		checkbox.setValue(skippedAthleteIds.contains(row.getId()));
		checkbox.addValueChangeListener(event -> {
			if (Boolean.TRUE.equals(event.getValue())) {
				skippedAthleteIds.add(row.getId());
			} else {
				skippedAthleteIds.remove(row.getId());
			}
			skippedSummary.setText("Skipped athletes: " + skippedAthleteIds.size());
		});
		return checkbox;
	}

	private String confirmationQuestion(BirthDateRepairPreview preview, int skippedCount) {
		return "This will add one day to " + (preview.getTotalCount() - skippedCount)
		        + " athlete birth dates.<br><br>Skipped athletes: " + skippedCount
		        + "<br><br>Export or back up the database before continuing.";
	}

	private VerticalLayout successContent(BirthDateRepairResult result) {
		VerticalLayout content = new VerticalLayout();
		content.setPadding(false);
		content.setSpacing(true);
		content.add(new Paragraph("Updated athlete birth dates: " + result.getUpdatedCount()));
		content.add(new Paragraph("Skipped athletes: " + result.getSkippedCount()));
		content.add(new Paragraph("Jan 1 before repair: " + result.getJan1Count()));
		content.add(new Paragraph("Dec 31 before repair: " + result.getDec31Count()));
		content.add(new Button("Close", event -> close()));
		return content;
	}
}