/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.technicalofficial.SessionAssignmentGenerator;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetableRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.spreadsheet.XLSXTimetableExport;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@Route(value = "preparation/team-schedule", layout = OwlcmsLayout.class)
public class TeamScheduleContent extends BaseContent implements OwlcmsContent {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(TeamScheduleContent.class);
	private final TeamScheduleElement scheduleEditor;
	private Button clearTimetableButton;
	private Button generateAssignmentsButton;
	private OwlcmsLayout routerLayout;

	public TeamScheduleContent() {
		setPadding(false);
		setSpacing(false);
		setSizeFull();
		this.scheduleEditor = new TeamScheduleElement();
		fillHW(this.scheduleEditor, this);
	}

	@Override
	public FlexLayout createMenuArea() {
		Div exportTimetable = DownloadButtonFactory.createDynamicXLSXDownloadButton("timetable",
		        Translator.translate("Timetable.ExportTimetable"), new XLSXTimetableExport(UI.getCurrent()));
		Button importTimetableButton = new Button(Translator.translate("Timetable.ImportTimetable"),
		        new Icon(VaadinIcon.UPLOAD_ALT), event -> {
			        TimetableUploadDialog dialog = new TimetableUploadDialog();
			        dialog.setCallback(() -> {
				        this.scheduleEditor.refreshSchedule();
				        updateTimetableButtons();
			        });
			        dialog.open();
		        });
		this.clearTimetableButton = new Button(Translator.translate("Timetable.ClearTimetable"),
		        new Icon(VaadinIcon.TRASH), event -> new ConfirmationDialog(
		                Translator.translate("Timetable.ClearTimetable"),
		                Translator.translate("Timetable.ClearTimetableWarning"),
		                Translator.translate("Delete"),
		                null,
		                this::clearTimetable).open());
		this.generateAssignmentsButton = new Button(Translator.translate("Timetable.GenerateSessionAssignments"),
		        new Icon(VaadinIcon.COGS), event -> new ConfirmationDialog(
		                Translator.translate("Timetable.GenerateSessionAssignments"),
		                Translator.translate("Timetable.GenerateAssignmentsWarning"),
		                null,
		                this::generateAssignments).open());
		Button clearAssignmentsButton = new Button(Translator.translate("Timetable.ClearSessionAssignments"),
		        new Icon(VaadinIcon.ERASER), event -> clearSessionAssignments());

		FlexLayout toolbar = new FlexLayout(exportTimetable, importTimetableButton, this.clearTimetableButton,
		        this.generateAssignmentsButton, clearAssignmentsButton);
		toolbar.getStyle().set("flex", "100 1");
		toolbar.getStyle().set("gap", "1ex");
		toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
		updateTimetableButtons();
		return toolbar;
	}

	@Override
	public String getMenuTitle() {
		return getPageTitle();
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("Timetable.TeamAssignments");
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return this.routerLayout;
	}

	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}

	private void clearTimetable() {
		try {
			int count = JPAService.runInTransaction(em -> {
				int entryCount = TechnicalOfficialsTimetableRepository.findAll(em).size();
				TechnicalOfficialsTimetableRepository.deleteAll(em);
				return entryCount;
			});
			this.scheduleEditor.refreshSchedule();
			updateTimetableButtons();
			Notification.show(Translator.translate("Timetable.TimetableCleared", count));
		} catch (Exception e) {
			logger.error("Error clearing timetable", e);
			Notification.show(Translator.translate("Timetable.TimetableClearFailed") + ": " + e.getMessage());
		}
	}

	private void generateAssignments() {
		try {
			int count = SessionAssignmentGenerator.generateSessionAssignments();
			Notification.show(Translator.translate("Timetable.AssignmentsGenerated", count));
		} catch (Exception e) {
			logger.error("Error generating session assignments", e);
			Notification.show(Translator.translate("Timetable.AssignmentGenerationFailed") + ": " + e.getMessage());
		}
	}

	private void clearSessionAssignments() {
		try {
			int count = JPAService.runInTransaction(em -> {
				List<Group> groups = GroupRepository.findAll();
				for (Group group : groups) {
					group.clearAllAssignments();
					em.merge(group);
				}
				return groups.size();
			});
			OwlcmsFactory.refreshActiveFOPGroups();
			Notification.show(Translator.translate("Timetable.AssignmentsCleared", count));
		} catch (Exception e) {
			logger.error("Error clearing session assignments", e);
			Notification.show(Translator.translate("Timetable.AssignmentClearFailed") + ": " + e.getMessage());
		}
	}

	private void updateTimetableButtons() {
		boolean hasEntries = JPAService.runInTransaction(em -> !TechnicalOfficialsTimetableRepository.findAll(em).isEmpty());
		this.clearTimetableButton.setEnabled(hasEntries);
		this.generateAssignmentsButton.setEnabled(hasEntries);
	}
}