/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;

import app.owlcms.components.ConfirmationDialog;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * Dialog that lists the loaded records as a Federation &gt; Record Name &gt; Age Group tree and
 * lets the user select branches (or whole sub-trees) to delete.
 */
@SuppressWarnings("serial")
public class RecordManagementDialog extends Dialog {

	enum NodeLevel {
		FEDERATION, RECORD_NAME, AGE_GROUP
	}

	/**
	 * A node in the records tree. A node carries the scope it represents so that deletion can be
	 * driven directly from the selected node, regardless of its level.
	 */
	static class RecordNode {
		final NodeLevel level;
		final String federation;
		final String recordName;
		final String ageGroup;
		final Gender gender;
		final String label;
		boolean active = true;

		RecordNode(NodeLevel level, String federation, String recordName, String ageGroup, Gender gender,
		        String label) {
			this.level = level;
			this.federation = federation;
			this.recordName = recordName;
			this.ageGroup = ageGroup;
			this.gender = gender;
			this.label = label;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof RecordNode)) {
				return false;
			}
			RecordNode other = (RecordNode) o;
			return this.level == other.level
			        && Objects.equals(this.federation, other.federation)
			        && Objects.equals(this.recordName, other.recordName)
			        && Objects.equals(this.ageGroup, other.ageGroup)
			        && this.gender == other.gender;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.level, this.federation, this.recordName, this.ageGroup, this.gender);
		}
	}

	final static Logger logger = (Logger) LoggerFactory.getLogger(RecordManagementDialog.class);

	private final Runnable onDeleted;
	private TreeGrid<RecordNode> treeGrid;
	private Checkbox showInactiveCheckbox;
	private VerticalLayout treeContainer;
	private H3 emptyMessage;
	// federation -> recordName -> ageGroup -> gender -> active (true if any record for that combination is active)
	private final TreeMap<String, TreeMap<String, TreeMap<String, TreeMap<Gender, Boolean>>>> hierarchy = new TreeMap<>();
	private boolean showInactive = false;
	private boolean hasLoadedRecords;

	public RecordManagementDialog(Runnable onDeleted) {
		this.onDeleted = onDeleted;
		setHeaderTitle(Translator.translate("Records.ManageTitle"));
		getHeader().add(createHeaderCloseButton());
		setCloseOnEsc(true);
		setCloseOnOutsideClick(true);
		setWidth("50em");
		setHeight("40em");
		setResizable(true);
		setDraggable(true);

		buildHierarchy();

		VerticalLayout content = new VerticalLayout();
		content.setPadding(false);
		content.setSpacing(true);
		content.setSizeFull();

		Paragraph instructions = new Paragraph(Translator.translate("Records.ManageExplanation"));
		instructions.getStyle().set("margin-top", "0");

		this.showInactiveCheckbox = new Checkbox(Translator.translate("Records.ShowInactive"));
		this.showInactiveCheckbox.setValue(this.showInactive);
		this.showInactiveCheckbox.addValueChangeListener(e -> {
			this.showInactive = Boolean.TRUE.equals(e.getValue());
			refreshTree();
		});

		HorizontalLayout header = new HorizontalLayout(instructions, this.showInactiveCheckbox);
		header.setWidthFull();
		header.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
		header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
		content.add(header);

		this.treeContainer = new VerticalLayout();
		this.treeContainer.setPadding(false);
		this.treeContainer.setSpacing(false);
		this.treeContainer.setSizeFull();
		content.add(this.treeContainer);
		renderTree();

		add(content);

		Button cancelButton = new Button(Translator.translate("Cancel"), e -> close());
		Button markActiveButton = new Button(Translator.translate("RecordEvent.MarkSelectedActive"),
		        e -> setActiveForSelection(true));
		Button markInactiveButton = new Button(Translator.translate("RecordEvent.MarkSelectedInactive"),
		        e -> setActiveForSelection(false));
		Button deleteButton = new Button(Translator.translate("RecordEvent.DeleteSelected"),
		        e -> confirmAndDelete());
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		markActiveButton.setEnabled(!this.hierarchy.isEmpty());
		markInactiveButton.setEnabled(!this.hierarchy.isEmpty());
		deleteButton.setEnabled(!this.hierarchy.isEmpty());

		getFooter().add(cancelButton, markActiveButton, markInactiveButton, deleteButton);
	}

	private Button createHeaderCloseButton() {
		Button closeButton = new Button(VaadinIcon.CLOSE.create(), e -> close());
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		closeButton.getElement().setAttribute("aria-label", Translator.translate("Close"));
		closeButton.getElement().setAttribute("title", Translator.translate("Close"));
		return closeButton;
	}

	private boolean cascadingSelection;

	private TreeGrid<RecordNode> createTreeGrid() {
		this.treeGrid = new TreeGrid<>();
		this.treeGrid.setSelectionMode(SelectionMode.MULTI);
		this.treeGrid.setSizeFull();

		this.treeGrid.addHierarchyColumn(node -> node.label)
		        .setHeader(Translator.translate("Records.ManageColumnHeader"))
		        .setAutoWidth(true);

		if (this.showInactive) {
			this.treeGrid.addComponentColumn((RecordNode node) -> {
				if (node.level != NodeLevel.AGE_GROUP) {
					return new com.vaadin.flow.component.html.Span();
				}
				Checkbox cb = new Checkbox(node.active);
				cb.setReadOnly(true);
				return cb;
			})
			        .setHeader(Translator.translate("Active"))
			        .setAutoWidth(true)
			        .setFlexGrow(0)
			        .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);
		}

		this.treeGrid.setItems(buildRootNodes(), this::childNodes);
		this.treeGrid.asMultiSelect().addSelectionListener(event -> {
			if (this.cascadingSelection) {
				return;
			}
			this.cascadingSelection = true;
			try {
				for (RecordNode added : event.getAddedSelection()) {
					selectDescendants(added, true);
				}
				for (RecordNode removed : event.getRemovedSelection()) {
					selectDescendants(removed, false);
					deselectAncestors(removed);
				}
			} finally {
				this.cascadingSelection = false;
			}
		});
		expandAll();
		return this.treeGrid;
	}

	private void selectDescendants(RecordNode parent, boolean select) {
		for (RecordNode child : childNodes(parent)) {
			if (select) {
				this.treeGrid.select(child);
			} else {
				this.treeGrid.deselect(child);
			}
			selectDescendants(child, select);
		}
	}

	private void deselectAncestors(RecordNode node) {
		if (node.level == NodeLevel.FEDERATION) {
			return;
		}
		RecordNode recName = new RecordNode(NodeLevel.RECORD_NAME, node.federation, node.recordName, null, null,
		        node.recordName);
		RecordNode fed = new RecordNode(NodeLevel.FEDERATION, node.federation, null, null, null, node.federation);
		if (node.level == NodeLevel.AGE_GROUP) {
			this.treeGrid.deselect(recName);
		}
		this.treeGrid.deselect(fed);
	}

	private void expandAll() {
		List<RecordNode> roots = buildRootNodes();
		this.treeGrid.expand(roots);
		for (RecordNode root : roots) {
			Collection<RecordNode> recordNames = childNodes(root);
			this.treeGrid.expand(recordNames);
			for (RecordNode recordName : recordNames) {
				this.treeGrid.expand(childNodes(recordName));
			}
		}
	}

	private void buildHierarchy() {
		this.hierarchy.clear();
		List<RecordEvent> loaded = RecordRepository.findAllLoadedRecords();
		this.hasLoadedRecords = !loaded.isEmpty();
		for (RecordEvent rec : loaded) {
			boolean recActive = !Boolean.FALSE.equals(rec.getActive());
			if (!this.showInactive && !recActive) {
				continue;
			}
			String fed = blankToPlaceholder(rec.getRecordFederation());
			String recName = blankToPlaceholder(rec.getRecordName());
			TreeMap<String, TreeMap<String, TreeMap<Gender, Boolean>>> recNames = this.hierarchy.computeIfAbsent(fed,
			        k -> new TreeMap<>());
			TreeMap<String, TreeMap<Gender, Boolean>> ageGroups = recNames.computeIfAbsent(recName,
			        k -> new TreeMap<>());
			String ageGrp = rec.getAgeGrp();
			if (ageGrp != null && !ageGrp.isBlank()) {
				TreeMap<Gender, Boolean> genders = ageGroups.computeIfAbsent(ageGrp.trim(),
				        k -> new TreeMap<>());
				if (rec.getGender() != null) {
					// active if any underlying record is active
					genders.merge(rec.getGender(), recActive, (a, b) -> a || b);
				}
			}
		}
	}

	private void refreshTree() {
		buildHierarchy();
		renderTree();
	}

	private void renderTree() {
		this.treeContainer.removeAll();
		this.treeGrid = null;
		if (this.emptyMessage != null) {
			this.emptyMessage = null;
		}
		if (this.hierarchy.isEmpty()) {
			String emptyKey = this.hasLoadedRecords ? "Records.NoDisplayOrderRecords" : "Records.NoRecordsLoaded";
			this.emptyMessage = new H3(Translator.translate(emptyKey));
			this.treeContainer.add(this.emptyMessage);
		} else {
			this.treeContainer.add(createTreeGrid());
		}
	}

	private List<RecordNode> buildRootNodes() {
		List<RecordNode> roots = new ArrayList<>();
		for (String fed : this.hierarchy.keySet()) {
			roots.add(new RecordNode(NodeLevel.FEDERATION, fed, null, null, null, fed));
		}
		return roots;
	}

	private Collection<RecordNode> childNodes(RecordNode parent) {
		List<RecordNode> children = new ArrayList<>();
		if (parent.level == NodeLevel.FEDERATION) {
			TreeMap<String, TreeMap<String, TreeMap<Gender, Boolean>>> recNames = this.hierarchy.get(parent.federation);
			if (recNames != null) {
				for (String recName : recNames.keySet()) {
					children.add(new RecordNode(NodeLevel.RECORD_NAME, parent.federation, recName, null, null,
					        recName));
				}
			}
		} else if (parent.level == NodeLevel.RECORD_NAME) {
			TreeMap<String, TreeMap<String, TreeMap<Gender, Boolean>>> names = this.hierarchy.get(parent.federation);
			if (names != null) {
				TreeMap<String, TreeMap<Gender, Boolean>> ageGroups = names.get(parent.recordName);
				if (ageGroups != null) {
					for (var ageEntry : ageGroups.entrySet()) {
						String ageGrp = ageEntry.getKey();
						for (var genderEntry : ageEntry.getValue().entrySet()) {
							Gender gender = genderEntry.getKey();
							String label = ageGrp + " " + gender.name();
							RecordNode leaf = new RecordNode(NodeLevel.AGE_GROUP, parent.federation,
							        parent.recordName, ageGrp, gender, label);
							leaf.active = Boolean.TRUE.equals(genderEntry.getValue());
							children.add(leaf);
						}
					}
				}
			}
		}
		return children;
	}

	private void confirmAndDelete() {
		Set<RecordNode> effective = effectiveSelection();
		if (effective.isEmpty()) {
			return;
		}

		ConfirmationDialog confirmDialog = new ConfirmationDialog(
		        Translator.translate("RecordEvent.DeleteSelected"),
		        Translator.translate("RecordEvent.DeleteSelectedExplanation"),
		        null,
		        () -> {
			        deleteNodes(effective);
			        if (this.onDeleted != null) {
				        this.onDeleted.run();
			        }
			        close();
		        });
		confirmDialog.open();
	}

	private void setActiveForSelection(boolean active) {
		Set<RecordNode> effective = effectiveSelection();
		if (effective.isEmpty()) {
			return;
		}
		for (RecordNode node : effective) {
			String fed = placeholderToNull(node.federation);
			String recName = node.level == NodeLevel.FEDERATION ? null : placeholderToNull(node.recordName);
			String ageGrp = node.level == NodeLevel.AGE_GROUP ? node.ageGroup : null;
			Gender gender = node.level == NodeLevel.AGE_GROUP ? node.gender : null;
			RecordRepository.setActiveWithFilters(fed, recName, ageGrp, gender, active);
		}
		if (this.onDeleted != null) {
			this.onDeleted.run();
		}
		refreshTree();
	}

	/**
	 * Collapse the current selection to its top-most nodes; a selected ancestor already covers its
	 * descendants.
	 */
	private Set<RecordNode> effectiveSelection() {
		Set<RecordNode> selected = this.treeGrid == null ? Set.of() : this.treeGrid.getSelectedItems();
		Set<RecordNode> effective = new LinkedHashSet<>();
		for (RecordNode node : selected) {
			if (!isAncestorSelected(node, selected)) {
				effective.add(node);
			}
		}
		return effective;
	}

	private boolean isAncestorSelected(RecordNode node, Set<RecordNode> selected) {
		if (node.level == NodeLevel.AGE_GROUP) {
			RecordNode recName = new RecordNode(NodeLevel.RECORD_NAME, node.federation, node.recordName, null, null,
			        null);
			RecordNode fed = new RecordNode(NodeLevel.FEDERATION, node.federation, null, null, null, null);
			return selected.contains(recName) || selected.contains(fed);
		}
		if (node.level == NodeLevel.RECORD_NAME) {
			RecordNode fed = new RecordNode(NodeLevel.FEDERATION, node.federation, null, null, null, null);
			return selected.contains(fed);
		}
		return false;
	}

	private void deleteNodes(Set<RecordNode> nodes) {
		for (RecordNode node : nodes) {
			String fed = placeholderToNull(node.federation);
			String recName = node.level == NodeLevel.FEDERATION ? null : placeholderToNull(node.recordName);
			String ageGrp = node.level == NodeLevel.AGE_GROUP ? node.ageGroup : null;
			Gender gender = node.level == NodeLevel.AGE_GROUP ? node.gender : null;
			try {
				RecordRepository.deleteRecordsWithFilters(fed, recName, ageGrp, gender, null, "ALL", null);
			} catch (IOException e) {
				LoggerUtils.logError(logger, e);
			}
		}
	}

	private static final String NO_VALUE = "—";

	private static String blankToPlaceholder(String value) {
		return (value == null || value.isBlank()) ? NO_VALUE : value.trim();
	}

	private static String placeholderToNull(String value) {
		return NO_VALUE.equals(value) ? null : value;
	}
}
