/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import app.owlcms.apputils.NotificationUtils;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.agegroup.AgeGapCheck;
import app.owlcms.data.agegroup.AgeGapCheck.Gap;
import app.owlcms.data.agegroup.AgeGapCheck.Overlap;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupChange;
import app.owlcms.data.agegroup.AgeGroupDefinitionService;
import app.owlcms.data.agegroup.AgeGroupDefinitionService.Decision;
import app.owlcms.data.agegroup.AgeGroupDefinitionService.SaveOutcome;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.SessionsInProgressException;
import app.owlcms.data.agegroup.reassign.ReassignmentPreview;
import app.owlcms.data.category.Category;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.StaleSingletonException;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

/**
 * UI orchestration for the age-group page actions (compliance item 200, spec 3.4). All database work runs on a
 * background worker; only dialog building, notifications and the caller's completion run in a short
 * {@code ui.access}. Every translated string is built on the UI thread. A save is reported to the caller only after
 * commit; a committed save with a refresh failure still completes, with a separate warning. Cancel and failures
 * write nothing and leave the editor's scratchpad available.
 */
@SuppressWarnings("serial")
public class AgeGroupSaveFlow {

	private static final ExecutorService WORKER = Executors.newCachedThreadPool(r -> {
		Thread t = new Thread(r, "agegroup-save-flow");
		t.setDaemon(true);
		return t;
	});
	private static final Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupSaveFlow.class);

	private AgeGroupSaveFlow() {
	}

	/** The editor's UPDATE button: classify, then the matching dialog flow; {@code committed} runs after commit. */
	public static void saveFromEditor(UI ui, AgeGroup scratch, AuditOrigin origin, Runnable committed,
	        Runnable laterHighlight) {
		saveFromEditor(ui, scratch, origin, committed, laterHighlight, () -> {
		});
	}

	/** Same, with {@code onAborted} invoked on the UI thread when the flow ends without a write. */
	public static void saveFromEditor(UI ui, AgeGroup scratch, AuditOrigin origin, Runnable committed,
	        Runnable laterHighlight, Runnable onAborted) {
		Long id = scratch.getId();
		Long draftStamp = scratch.getLastSavedAt();
		WORKER.submit(() -> {
			try {
				AgeGroup stored = AgeGroupRepository.loadForEdit(id);
				AgeGroupRepository.assertCurrent(id, draftStamp);
				Double duplicate = duplicateUpperLimit(scratch);
				if (duplicate != null) {
					ui.access(() -> {
						NotificationUtils.errorNotification(
						        Translator.translate("AgeGroup.DuplicateUpperLimit", formatWeight(duplicate)));
						onAborted.run();
					});
					return;
				}
				AgeGroupChange.Kind kind = AgeGroupChange.classify(stored, scratch);
				switch (kind) {
					case NONE, DIRECT -> doSave(ui, scratch, origin, null, null, committed, laterHighlight);
					case CHAMPIONSHIP_MIXED -> ui.access(() -> {
						NotificationUtils.errorNotification(
						        Translator.translate("AgeGroup.ChampionshipChange.MixedRefusal"));
						onAborted.run();
					});
					case CHAMPIONSHIP_ONLY -> championshipOnlyFlow(ui, stored, scratch, origin, committed,
					        laterHighlight, onAborted);
					case REASSIGN -> reassignFlow(ui, stored, scratch, origin, committed, laterHighlight, onAborted);
				}
			} catch (Throwable t) {
				reportFailure(ui, t, onAborted);
			}
		});
	}

	/** The editor's ADD button: one transaction, no dialog; {@code committed} runs after commit. */
	public static void createFromEditor(UI ui, AgeGroup scratch, AuditOrigin origin, Runnable committed) {
		WORKER.submit(() -> {
			SaveOutcome outcome;
			try {
				Double duplicate = duplicateUpperLimit(scratch);
				if (duplicate != null) {
					ui.access(() -> NotificationUtils.errorNotification(
					        Translator.translate("AgeGroup.DuplicateUpperLimit", formatWeight(duplicate))));
					return;
				}
				outcome = new AgeGroupDefinitionService().create(scratch, origin);
			} catch (Throwable t) {
				reportFailure(ui, t, null);
				return;
			}
			reportCommitted(ui, outcome, null, committed, null);
		});
	}

	/**
	 * Delete from the page, after the caller's confirmation: one dialog combining the age-gap warning (when the
	 * delete opens a hole) and the dry run (athletes added or removed); deleted directly when there is nothing to
	 * report.
	 */
	public static void deleteAgeGroup(UI ui, AgeGroup row, AuditOrigin origin, Runnable committed) {
		WORKER.submit(() -> {
			try {
				List<Gap> newGaps = gapsCreatedByDeleting(row);
				ReassignmentPreview preview = new AgeGroupDefinitionService().previewDelete(row.getId(),
				        row.getLastSavedAt());
				if (newGaps.isEmpty() && preview.entries().isEmpty()) {
					doDelete(ui, row, origin, null, committed);
					return;
				}
				String championshipName = row.getChampionshipName();
				ui.access(() -> showDeleteDialog(ui, row, origin, championshipName, newGaps, preview.entries(),
				        committed));
			} catch (Throwable t) {
				reportFailure(ui, t, null);
			}
		});
	}

	/** Worker: the guarded delete, after the warnings have been accepted. */
	private static void doDelete(UI ui, AgeGroup row, AuditOrigin origin, Dialog dialog, Runnable committed) {
		SaveOutcome outcome;
		try {
			outcome = new AgeGroupDefinitionService().delete(row.getLastSavedAt(), origin, row.getId());
		} catch (Throwable t) {
			reportFailure(ui, t, dialog != null ? dialog::close : null);
			return;
		}
		reportCommitted(ui, outcome, dialog, committed, null);
	}

	private static void showDeleteDialog(UI ui, AgeGroup row, AuditOrigin origin, String championshipName,
	        List<Gap> gaps, List<ReassignmentPreview.Entry> entries, Runnable committed) {
		VerticalLayout body = new VerticalLayout();
		addDeleteGapLines(body, row.getName(), championshipName, gaps);
		if (!entries.isEmpty()) {
			body.add(new Paragraph(Translator.translate("AgeGroup.Delete.Reassign", row.getName(), championshipName)));
			body.add(changeTable(entries, false));
		}
		Dialog dialog = new Dialog();
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);
		dialog.setWidth("min(60em, 95vw)");
		Button delete = new Button(Translator.translate("Delete"));
		delete.setId("agegroup-delete-apply");
		delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		Button cancel = new Button(Translator.translate("Cancel"), e -> dialog.close());
		cancel.setId("agegroup-delete-cancel");
		delete.addClickListener(e -> {
			delete.setEnabled(false);
			cancel.setEnabled(false);
			WORKER.submit(() -> doDelete(ui, row, origin, dialog, committed));
		});
		String title = gaps.isEmpty()
		        ? Translator.translate("AgeGroup.Reassign.Title", championshipName)
		        : Translator.translate("AgeGroup.AgeGap.Title");
		dialog.add(new VerticalLayout(new H3(title), body, new HorizontalLayout(delete, cancel)));
		// opened from the confirmation dialog's callback: attach to the UI, not to the dialog that is closing
		ui.add(dialog);
		dialog.addOpenedChangeListener(e -> {
			if (!e.isOpened()) {
				dialog.getElement().removeFromParent();
			}
		});
		dialog.open();
	}

	/** The Active checkbox in the grid: an edit of the active flag, same flow as the editor. */
	public static void toggleActive(UI ui, AgeGroup gridRow, boolean newValue, AuditOrigin origin,
	        Runnable committed, Runnable laterHighlight, Runnable revert) {
		WORKER.submit(() -> {
			try {
				AgeGroup stored = AgeGroupRepository.loadForEdit(gridRow.getId());
				if (stored == null
				        || !Objects.equals(stored.getLastSavedAt(), gridRow.getLastSavedAt())) {
					throw new StaleSingletonException(StaleSingletonException.Kind.AGE_GROUP,
					        stored != null ? stored.getLastSavedBy() : null);
				}
				AgeGroup scratch = new AgeGroup(stored);
				scratch.setActive(newValue);
				AgeGroupRepository.assertCurrent(scratch.getId(), scratch.getLastSavedAt());
				// an active-flag change always classifies as REASSIGN
				reassignFlow(ui, stored, scratch, origin, committed, laterHighlight, revert);
			} catch (Throwable t) {
				reportFailure(ui, t, revert);
			}
		});
	}

	/** The two global actions: dry run, list, Apply/Cancel. {@code committed} runs after commit. */
	public static void globalAction(UI ui, AuditOrigin origin, boolean recomputeAll, Runnable committed) {
		WORKER.submit(() -> {
			try {
				AgeGroupDefinitionService service = new AgeGroupDefinitionService();
				ReassignmentPreview preview = recomputeAll
				        ? service.previewRecomputeAll()
				        : service.previewReassignWithin();
				ui.access(() -> showGlobalPreviewDialog(ui, origin, recomputeAll, preview, committed));
			} catch (Throwable t) {
				reportFailure(ui, t, null);
			}
		});
	}

	/** Load a detached scratchpad for the editor, off the UI thread; {@code showForm} runs in ui.access. */
	public static void loadScratchpad(UI ui, Long id, Consumer<AgeGroup> showForm) {
		WORKER.submit(() -> {
			try {
				AgeGroup scratch = AgeGroupRepository.loadForEdit(id);
				if (scratch == null) {
					throw new StaleSingletonException(StaleSingletonException.Kind.AGE_GROUP, null);
				}
				ui.access(() -> showForm.accept(scratch));
			} catch (Throwable t) {
				reportFailure(ui, t, null);
			}
		});
	}

	// ---------------------------------------------------------------------------------------------
	// flows
	// ---------------------------------------------------------------------------------------------

	private static void championshipOnlyFlow(UI ui, AgeGroup stored, AgeGroup scratch, AuditOrigin origin,
	        Runnable committed, Runnable laterHighlight, Runnable onAborted) {
		String from = stored.getChampionshipName();
		String to = scratch.getChampionshipName();
		try {
			AgeGroupDefinitionService.assertNoSessionConflict(List.of(from, to));
			long moving = countAthletesInAgeGroup(stored.getId());
			List<Gap> gaps = new ArrayList<>();
			gaps.addAll(gapsForChampionship(from, scratch, true));
			gaps.addAll(gapsForChampionship(to, scratch, false));
			List<Overlap> fromOverlaps = overlapsForChampionship(from, scratch, true);
			List<Overlap> toOverlaps = overlapsForChampionship(to, scratch, false);
			ui.access(() -> {
				VerticalLayout body = new VerticalLayout();
				body.add(new Paragraph(Translator.translate("AgeGroup.ChampionshipChange.Warning",
				        stored.getCode(), from, to, moving)));
				addGapLines(body, from, gapsForChampionshipSilent(from, scratch, true));
				addGapLines(body, to, gapsForChampionshipSilent(to, scratch, false));
				addOverlapLines(body, from, fromOverlaps);
				addOverlapLines(body, to, toOverlaps);
				Dialog dialog = new Dialog();
				dialog.setCloseOnEsc(false);
				dialog.setCloseOnOutsideClick(false);
				dialog.setWidth("min(42em, 95vw)");
				Button confirm = new Button(Translator.translate("AgeGroup.ChampionshipChange.Confirm"));
				confirm.setId("agegroup-move-confirm");
				confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
				Button cancel = new Button(Translator.translate("Cancel"), e -> {
					dialog.close();
					onAborted.run();
				});
				cancel.setId("agegroup-move-cancel");
				confirm.addClickListener(e -> {
					confirm.setEnabled(false);
					cancel.setEnabled(false);
					WORKER.submit(() -> doSave(ui, scratch, origin, null, dialog, committed, laterHighlight));
				});
				dialog.add(new VerticalLayout(new H3(Translator.translate("AgeGroup.ChampionshipChange.Title")),
				        body, new HorizontalLayout(confirm, cancel)));
				dialog.open();
			});
		} catch (Throwable t) {
			reportFailure(ui, t, onAborted);
		}
	}

	private static void reassignFlow(UI ui, AgeGroup stored, AgeGroup scratch, AuditOrigin origin,
	        Runnable committed, Runnable laterHighlight, Runnable onAborted) {
		String championshipName = stored.getChampionshipName();
		try {
			AgeGroupDefinitionService.assertNoSessionConflict(List.of(championshipName));
			List<Gap> gaps = gapsForChampionship(championshipName, scratch, false);
			List<Overlap> overlaps = overlapsForChampionship(championshipName, scratch, false);
			if (!gaps.isEmpty() || !overlaps.isEmpty()) {
				ui.access(() -> showAgeCheckDialog(ui, stored, scratch, origin, championshipName, gaps, overlaps,
				        committed, laterHighlight, onAborted));
			} else {
				previewAndConfirm(ui, stored, scratch, origin, championshipName, committed, laterHighlight, onAborted);
			}
		} catch (Throwable t) {
			reportFailure(ui, t, onAborted);
		}
	}

	private static void showAgeCheckDialog(UI ui, AgeGroup stored, AgeGroup scratch, AuditOrigin origin,
	        String championshipName, List<Gap> gaps, List<Overlap> overlaps, Runnable committed,
	        Runnable laterHighlight, Runnable onAborted) {
		VerticalLayout body = new VerticalLayout();
		addGapLines(body, championshipName, gaps);
		addOverlapLines(body, championshipName, overlaps);
		Dialog dialog = new Dialog();
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);
		dialog.setWidth("min(42em, 95vw)");
		Button cont = new Button(Translator.translate("AgeGroup.AgeGap.Continue"));
		cont.setId("agegroup-gap-continue");
		Button cancel = new Button(Translator.translate("Cancel"), e -> {
			dialog.close();
			onAborted.run();
		});
		cancel.setId("agegroup-gap-cancel");
		cont.addClickListener(e -> {
			cont.setEnabled(false);
			cancel.setEnabled(false);
			WORKER.submit(() -> {
				ui.access(dialog::close);
				previewAndConfirm(ui, stored, scratch, origin, championshipName, committed, laterHighlight,
				        onAborted);
			});
		});
		dialog.add(new VerticalLayout(new H3(Translator.translate(
		        gaps.isEmpty() ? "AgeGroup.AgeOverlap.Title" : "AgeGroup.AgeGap.Title")), body,
		        new HorizontalLayout(cont, cancel)));
		dialog.open();
	}

	/** Worker: dry run, then either save directly (no participants) or show the reassignment dialog. */
	private static void previewAndConfirm(UI ui, AgeGroup stored, AgeGroup scratch, AuditOrigin origin,
	        String championshipName, Runnable committed, Runnable laterHighlight, Runnable onAborted) {
		try {
			ReassignmentPreview preview = new AgeGroupDefinitionService().preview(scratch,
			        scratch.getLastSavedAt());
			long participants = countParticipants(championshipName);
			boolean categoriesRemoved = categoriesRemoved(stored, scratch);
			if (participants == 0) {
				doSave(ui, scratch, origin, Decision.APPLY, null, committed, laterHighlight);
				return;
			}
			List<ReassignmentPreview.Entry> leavers = preview.leavers(championshipName);
			ui.access(() -> showReassignDialog(ui, scratch, origin, championshipName, leavers, categoriesRemoved,
			        committed, laterHighlight, onAborted));
		} catch (Throwable t) {
			reportFailure(ui, t, onAborted);
		}
	}

	private static void showReassignDialog(UI ui, AgeGroup scratch, AuditOrigin origin, String championshipName,
	        List<ReassignmentPreview.Entry> leavers, boolean categoriesRemoved, Runnable committed,
	        Runnable laterHighlight, Runnable onAborted) {
		VerticalLayout body = new VerticalLayout();
		body.add(new Paragraph(Translator.translate("AgeGroup.Reassign.Warning")));
		if (leavers.isEmpty()) {
			body.add(new Paragraph(Translator.translate("AgeGroup.Reassign.NobodyLeaves")));
		} else {
			body.add(changeTable(leavers, false));
		}
		if (categoriesRemoved) {
			Paragraph note = new Paragraph(Translator.translate("AgeGroup.Reassign.DeletionNote"));
			note.getStyle().set("font-weight", "bold");
			body.add(note);
		}
		Dialog dialog = new Dialog();
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);
		dialog.setWidth("min(60em, 95vw)");
		Button apply = new Button(Translator.translate("Apply"));
		apply.setId("agegroup-reassign-apply");
		apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		Button later = new Button(Translator.translate("AgeGroup.Reassign.Later"));
		later.setId("agegroup-reassign-later");
		later.setEnabled(!categoriesRemoved);
		Button cancel = new Button(Translator.translate("Cancel"), e -> {
			dialog.close();
			onAborted.run();
		});
		cancel.setId("agegroup-reassign-cancel");
		apply.addClickListener(e -> {
			apply.setEnabled(false);
			later.setEnabled(false);
			cancel.setEnabled(false);
			WORKER.submit(() -> doSave(ui, scratch, origin, Decision.APPLY, dialog, committed, laterHighlight));
		});
		later.addClickListener(e -> {
			apply.setEnabled(false);
			later.setEnabled(false);
			cancel.setEnabled(false);
			WORKER.submit(() -> doSave(ui, scratch, origin, Decision.LATER, dialog, committed, laterHighlight));
		});
		dialog.add(new VerticalLayout(
		        new H3(Translator.translate("AgeGroup.Reassign.Title", championshipName)), body,
		        new HorizontalLayout(apply, later, cancel)));
		dialog.open();
	}

	/** One row per athlete: what happens (added, removed, moved, with the reason), categories before and after. */
	private static Grid<ReassignmentPreview.Entry> changeTable(List<ReassignmentPreview.Entry> entries,
	        boolean withChampionship) {
		Grid<ReassignmentPreview.Entry> table = new Grid<>(ReassignmentPreview.Entry.class, false);
		table.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);
		table.addColumn(ReassignmentPreview.Entry::athleteName).setHeader(Translator.translate("Athlete"))
		        .setAutoWidth(true).setFlexGrow(0);
		if (withChampionship) {
			table.addColumn(ReassignmentPreview.Entry::championshipName)
			        .setHeader(Translator.translate("Championship")).setAutoWidth(true).setFlexGrow(0);
		}
		table.addColumn(AgeGroupSaveFlow::changeLabel).setHeader(Translator.translate("Change"))
		        .setAutoWidth(true).setFlexGrow(0);
		table.addColumn(e -> String.join(", ", e.currentCategories())).setHeader(Translator.translate("Current"))
		        .setFlexGrow(1);
		table.addColumn(e -> String.join(", ", e.afterCategories())).setHeader(Translator.translate("After"))
		        .setFlexGrow(1);
		table.setItems(entries);
		if (entries.size() <= 12) {
			table.setAllRowsVisible(true);
		} else {
			table.setHeight("min(24em, 50vh)");
		}
		table.setWidthFull();
		return table;
	}

	private static String changeLabel(ReassignmentPreview.Entry entry) {
		String label = Translator.translate(switch (entry.change()) {
			case JOINS -> "AgeGroup.DryRun.Joins";
			case LEAVES -> "AgeGroup.DryRun.Leaves";
			case MOVES -> "AgeGroup.DryRun.Moves";
		});
		return entry.reason() == null ? label
		        : label + " (" + Translator.translate("AgeGroup.LeaveReason." + entry.reason().name()) + ")";
	}

	private static void showGlobalPreviewDialog(UI ui, AuditOrigin origin, boolean recomputeAll,
	        ReassignmentPreview preview, Runnable committed) {
		String titleKey = recomputeAll ? "AgeGroup.RecomputeAll.Title" : "AgeGroup.ReassignWithin.Title";
		String warningKey = recomputeAll ? "AgeGroup.RecomputeAll.Warning" : "AgeGroup.ReassignWithin.Warning";
		VerticalLayout body = new VerticalLayout();
		body.add(new Paragraph(Translator.translate(warningKey)));
		if (preview.entries().isEmpty()) {
			body.add(new Paragraph(Translator.translate("AgeGroup.Reassign.NobodyLeaves")));
		} else {
			body.add(changeTable(preview.entries(), true));
		}
		Dialog dialog = new Dialog();
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);
		dialog.setWidth("min(70em, 95vw)");
		Button apply = new Button(Translator.translate("Apply"));
		apply.setId("agegroup-global-apply");
		apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		Button cancel = new Button(Translator.translate("Cancel"), e -> dialog.close());
		cancel.setId("agegroup-global-cancel");
		apply.addClickListener(e -> {
			apply.setEnabled(false);
			cancel.setEnabled(false);
			WORKER.submit(() -> {
				SaveOutcome outcome;
				try {
					AgeGroupDefinitionService service = new AgeGroupDefinitionService();
					outcome = recomputeAll
					        ? service.recomputeAllEligibilities(origin)
					        : service.reassignWithin(origin);
				} catch (Throwable t) {
					reportFailure(ui, t, dialog::close);
					return;
				}
				reportCommitted(ui, outcome, dialog, committed, null);
			});
		});
		dialog.add(new VerticalLayout(new H3(Translator.translate(titleKey)), body,
		        new HorizontalLayout(apply, cancel)));
		dialog.open();
	}

	// ---------------------------------------------------------------------------------------------
	// save and completion
	// ---------------------------------------------------------------------------------------------

	/** Worker: the authoritative save; the dialog (when any) closes on success and on failure. */
	private static void doSave(UI ui, AgeGroup scratch, AuditOrigin origin, Decision decision, Dialog dialog,
	        Runnable committed, Runnable laterHighlight) {
		SaveOutcome outcome;
		try {
			outcome = new AgeGroupDefinitionService().save(scratch.getLastSavedAt(), origin, scratch, decision);
		} catch (Throwable t) {
			reportFailure(ui, t, dialog != null ? dialog::close : null);
			return;
		}
		reportCommitted(ui, outcome, dialog, committed, decision == Decision.LATER ? laterHighlight : null);
	}

	/** ui.access: a committed action is reported to the caller exactly once; refresh failures warn separately. */
	private static void reportCommitted(UI ui, SaveOutcome outcome, Dialog dialog, Runnable committed,
	        Runnable laterHighlight) {
		try {
			ui.access(() -> {
				if (dialog != null) {
					dialog.close();
				}
				if (outcome != null && !outcome.refreshFailures().isEmpty()) {
					NotificationUtils.errorNotification(Translator.translate("AgeGroup.SaveSucceededRefreshFailed",
					        outcome.refreshFailures().get(0).toString()));
				}
				if (laterHighlight != null) {
					laterHighlight.run();
				}
				committed.run();
			});
		} catch (UIDetachedException e) {
			logger./**/warn("age group action committed, but the page was closed before it could be updated");
		}
	}

	/** ui.access: pre-commit failures close the confirmation dialog; the editor keeps its scratchpad. */
	private static void reportFailure(UI ui, Throwable t, Runnable also) {
		logger.warn("age group action failed: {}", t.toString());
		try {
			ui.access(() -> {
				if (also != null) {
					also.run();
				}
				StaleSingletonException stale = StaleSingletonException.find(t);
				if (stale != null) {
					NotificationUtils.staleSettings(stale.getSavedBy());
					return;
				}
				if (t instanceof SessionsInProgressException sipe) {
					String list = sipe.getConflicts().stream().map(Object::toString)
					        .collect(Collectors.joining(", "));
					NotificationUtils.errorNotification(Translator.translate("AgeGroup.SessionInProgress", list));
					return;
				}
				NotificationUtils.errorNotification(t.getMessage() != null ? t.getMessage() : t.toString());
			});
		} catch (UIDetachedException e) {
			logger./**/warn("age group action failure could not be shown: the page was closed");
		}
	}

	// ---------------------------------------------------------------------------------------------
	// read helpers (worker thread only)
	// ---------------------------------------------------------------------------------------------

	private static List<AgeGroup> proposedActive(String championshipName, AgeGroup scratch, boolean removeScratch) {
		List<AgeGroup> active = activeAgeGroupsOf(championshipName);
		List<AgeGroup> proposed = new ArrayList<>();
		for (AgeGroup ag : active) {
			if (!Objects.equals(ag.getId(), scratch.getId())) {
				proposed.add(ag);
			}
		}
		if (!removeScratch && scratch.isActive()) {
			proposed.add(scratch);
		}
		return proposed;
	}

	private static List<Gap> gapsForChampionship(String championshipName, AgeGroup scratch, boolean removeScratch) {
		return AgeGapCheck.gaps(proposedActive(championshipName, scratch, removeScratch));
	}

	/** Every overlap in the championship as it would be after the change, not only those of the changed age group. */
	private static List<Overlap> overlapsForChampionship(String championshipName, AgeGroup scratch,
	        boolean removeScratch) {
		return AgeGapCheck.overlaps(proposedActive(championshipName, scratch, removeScratch));
	}

	private static List<Gap> gapsForChampionshipSilent(String championshipName, AgeGroup scratch,
	        boolean removeScratch) {
		try {
			return gapsForChampionship(championshipName, scratch, removeScratch);
		} catch (Throwable t) {
			return List.of();
		}
	}

	/** Holes that deleting the age group opens; a hole that already existed is not reported again. */
	private static List<Gap> gapsCreatedByDeleting(AgeGroup row) {
		return AgeGapCheck.gapsOpenedByRemoving(activeAgeGroupsOf(row.getChampionshipName()), row);
	}

	private static void addGapLines(VerticalLayout body, String championshipName, List<Gap> gaps) {
		for (Gap gap : gaps) {
			body.add(new Paragraph(Translator.translate("AgeGroup.AgeGap.Warning", championshipName,
			        gap.gender().asGenderName(), gapAges(gap))));
		}
	}

	private static void addOverlapLines(VerticalLayout body, String championshipName, List<Overlap> overlaps) {
		for (Overlap overlap : overlaps) {
			body.add(new Paragraph(Translator.translate("AgeGroup.AgeOverlap.Warning", championshipName,
			        overlap.firstCode(), overlap.secondCode(), overlap.gender().asGenderName(),
			        overlapAges(overlap))));
		}
	}

	private static String overlapAges(Overlap overlap) {
		return overlap.fromAge() == overlap.toAge() ? String.valueOf(overlap.fromAge())
		        : overlap.fromAge() + "-" + overlap.toAge();
	}

	/** Same holes, worded as the consequence of deleting the named age group. */
	private static void addDeleteGapLines(VerticalLayout body, String ageGroupName, String championshipName,
	        List<Gap> gaps) {
		for (Gap gap : gaps) {
			body.add(new Paragraph(Translator.translate("AgeGroup.AgeGap.DeleteWarning", ageGroupName,
			        championshipName, gap.gender().asGenderName(), gapAges(gap))));
		}
	}

	private static String gapAges(Gap gap) {
		return gap.fromAge() == gap.toAge() ? String.valueOf(gap.fromAge()) : gap.fromAge() + "-" + gap.toAge();
	}

	private static List<AgeGroup> activeAgeGroupsOf(String championshipName) {
		if (championshipName == null || championshipName.isBlank()) {
			return List.of();
		}
		return JPAService.runInTransaction(em -> em
		        .createQuery("select ag from AgeGroup ag where lower(ag.championshipName) = :name and ag.active = true",
		                AgeGroup.class)
		        .setParameter("name", championshipName.trim().toLowerCase())
		        .getResultList());
	}

	private static long countParticipants(String championshipName) {
		if (championshipName == null || championshipName.isBlank()) {
			return 0;
		}
		return JPAService.runInTransaction(em -> em.createQuery(
		        "select count(distinct a) from Athlete a join a.participations p join p.category c"
		                + " join c.ageGroup ag where lower(ag.championshipName) = :name",
		        Long.class)
		        .setParameter("name", championshipName.trim().toLowerCase())
		        .getSingleResult());
	}

	private static long countAthletesInAgeGroup(Long ageGroupId) {
		return JPAService.runInTransaction(em -> em.createQuery(
		        "select count(distinct a) from Athlete a join a.participations p join p.category c"
		                + " where c.ageGroup.id = :id",
		        Long.class)
		        .setParameter("id", ageGroupId)
		        .getSingleResult());
	}

	private static boolean categoriesRemoved(AgeGroup stored, AgeGroup scratch) {
		Set<Double> scratchLimits = scratch.getCategories().stream().map(Category::getMaximumWeight)
		        .collect(Collectors.toCollection(LinkedHashSet::new));
		return stored.getCategories().stream().map(Category::getMaximumWeight)
		        .anyMatch(limit -> !scratchLimits.contains(limit));
	}

	private static Double duplicateUpperLimit(AgeGroup scratch) {
		Set<Double> seen = new LinkedHashSet<>();
		for (Category c : scratch.getCategories()) {
			Double max = c.getMaximumWeight();
			if (max != null && !seen.add(max)) {
				return max;
			}
		}
		return null;
	}

	private static String formatWeight(double weight) {
		return weight == Math.floor(weight) ? String.valueOf((int) weight) : String.valueOf(weight);
	}
}
