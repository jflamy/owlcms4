package app.owlcms.nui.admin;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.nui.admin.BirthYearRepairService.BirthYearRepairPreview;
import app.owlcms.nui.admin.BirthYearRepairService.BirthYearRepairResult;
import app.owlcms.nui.admin.BirthYearRepairService.BirthYearRepairRow;

@SuppressWarnings("serial")
final class BirthYearRepairDialog extends Dialog {

	BirthYearRepairDialog(Button repairButton) {
		BirthYearRepairPreview preview = BirthYearRepairService.preview();
		Set<Long> selectedAthleteIds = new HashSet<>();
		preview.getRows().stream().map(BirthYearRepairRow::getId).forEach(selectedAthleteIds::add);
		Span selectedSummary = new Span();

		setHeaderTitle("Repair Birth Years");
		setWidth("72em");
		setMaxWidth("calc(100vw - 2rem)");
		setCloseOnEsc(true);
		setCloseOnOutsideClick(false);

		VerticalLayout content = new VerticalLayout();
		content.setPadding(false);
		content.setSpacing(true);
		content.setAlignItems(FlexComponent.Alignment.STRETCH);

		content.add(warningText());
		content.add(summary(preview, selectedSummary));

		Grid<BirthYearRepairRow> reviewGrid = baseGrid(preview);
		reviewGrid.asMultiSelect().addSelectionListener(event -> {
			selectedAthleteIds.clear();
			event.getAllSelectedItems().stream()
			        .map(BirthYearRepairRow::getId)
			        .forEach(selectedAthleteIds::add);
			updateSelectedSummary(selectedSummary, selectedAthleteIds.size(), preview.getTotalCount());
		});
		selectAll(reviewGrid, preview);
		content.add(reviewGrid);

		Button selectAll = new Button("Select All", event -> selectAll(reviewGrid, preview));
		Button cancel = new Button("Cancel", event -> close());
		Button apply = new Button("Apply Repair", event -> {
			ConfirmationDialog confirmationDialog = new ConfirmationDialog(
			        "Apply Birth Year Repair",
			        confirmationQuestion(preview, selectedAthleteIds.size()),
			        "Apply Repair",
			        null,
			        null,
			        () -> {
				        BirthYearRepairResult result = BirthYearRepairService.apply(Set.copyOf(selectedAthleteIds),
				                AccessUtils.getClientIp());
				        repairButton.setEnabled(false);
				        removeAll();
				        setHeaderTitle("Birth Year Repair Applied");
				        add(successContent(result));
			        });
			confirmationDialog.open();
		});
		apply.setEnabled(!selectedAthleteIds.isEmpty());
		reviewGrid.asMultiSelect().addSelectionListener(event -> apply.setEnabled(!event.getAllSelectedItems().isEmpty()));

		HorizontalLayout buttons = new HorizontalLayout(selectAll, cancel, apply);
		buttons.setWidthFull();
		buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

		content.add(buttons);
		add(content);
	}

	private VerticalLayout warningText() {
		VerticalLayout warning = new VerticalLayout();
		warning.setPadding(false);
		warning.setSpacing(false);

		warning.add(new Paragraph(
		        "This emergency action adds one year to every selected athlete and stores January 1 of the following year."));
		warning.add(new Paragraph(
		        "Existing month and day information will be permanently discarded. This action cannot distinguish incorrect dates from correct dates."));
		warning.add(new Paragraph(
		        "Use it only once when all selected birth years are known to be one year too low."));
		warning.add(new Paragraph("Export or back up the database before continuing."));

		warning.getStyle().set("color", "var(--lumo-error-text-color)");
		return warning;
	}

	private VerticalLayout summary(BirthYearRepairPreview preview, Span selectedSummary) {
		VerticalLayout summary = new VerticalLayout();
		summary.setPadding(false);
		summary.setSpacing(false);
		summary.add(new Span("Athletes with birth dates: " + preview.getTotalCount()));
		updateSelectedSummary(selectedSummary, preview.getTotalCount(), preview.getTotalCount());
		summary.add(selectedSummary);
		return summary;
	}

	private void updateSelectedSummary(Span selectedSummary, int selectedCount, int totalCount) {
		selectedSummary.setText("Selected athletes: " + selectedCount + " / " + totalCount);
	}

	private void selectAll(Grid<BirthYearRepairRow> grid, BirthYearRepairPreview preview) {
		grid.asMultiSelect().select(preview.getRows().toArray(BirthYearRepairRow[]::new));
	}

	private Grid<BirthYearRepairRow> baseGrid(BirthYearRepairPreview preview) {
		Grid<BirthYearRepairRow> grid = new Grid<>(BirthYearRepairRow.class, false);
		grid.setSelectionMode(Grid.SelectionMode.MULTI);
		grid.addColumn(BirthYearRepairRow::getId).setHeader("ID").setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(BirthYearRepairRow::getLotNumber).setHeader("Lot").setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(BirthYearRepairRow::getLastName).setHeader("Last Name").setAutoWidth(true);
		grid.addColumn(BirthYearRepairRow::getFirstName).setHeader("First Name").setAutoWidth(true);
		grid.addColumn(BirthYearRepairRow::getCurrentBirthDate).setHeader("Current DOB").setAutoWidth(true);
		grid.addColumn(BirthYearRepairRow::getRepairedBirthDate).setHeader("After Repair").setAutoWidth(true);
		grid.setItems(preview.getRows());
		grid.setHeight("32em");
		grid.getThemeNames().add("row-stripes");
		return grid;
	}

	private String confirmationQuestion(BirthYearRepairPreview preview, int selectedCount) {
		return "This will move " + selectedCount
		        + " athlete birth dates to January 1 of the following year.<br><br>Unselected athletes: "
		        + (preview.getTotalCount() - selectedCount)
		        + "<br><br>Export or back up the database before continuing.";
	}

	private VerticalLayout successContent(BirthYearRepairResult result) {
		VerticalLayout content = new VerticalLayout();
		content.setPadding(false);
		content.setSpacing(true);
		content.add(new Paragraph("Updated athletes: " + result.getUpdatedCount()));
		content.add(new Paragraph("Unselected athletes: " + result.getUnselectedCount()));
		content.add(new Button("Close", event -> close()));
		return content;
	}
}