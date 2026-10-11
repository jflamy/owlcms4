/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import app.owlcms.components.ConfirmationDialog;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.i18n.Translator;
import app.owlcms.monitors.WebSocketEventForwarder;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * Selects the age groups that belong to a championship.
 *
 * Age groups already in the championship are shown selected and cannot be removed (an age group always
 * belongs to exactly one championship). Selecting an age group from another championship moves it here,
 * after a confirmation whose default is Cancel.
 */
@SuppressWarnings("serial")
public class SelectAgeGroupsDialog extends Dialog {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(SelectAgeGroupsDialog.class);

	private final Championship target;
	private final Set<Long> currentIds;
	private final Runnable onSaved;
	private final Grid<AgeGroup> grid = new Grid<>(AgeGroup.class, false);

	public SelectAgeGroupsDialog(Championship target, List<AgeGroup> currentAgeGroups, Runnable onSaved) {
		this.target = target;
		this.onSaved = onSaved;
		this.currentIds = currentAgeGroups.stream().map(AgeGroup::getId).filter(Objects::nonNull)
		        .collect(Collectors.toSet());

		setHeaderTitle(Translator.translate("Championship.SelectAgeGroups") + " — " + target.translate());
		setWidth("80em");
		setMaxWidth("calc(100vw - 4rem)");
		setCloseOnOutsideClick(false);

		configureGrid();
		add(this.grid);

		Button updateButton = new Button(Translator.translate("Update"), e -> confirmAndApply());
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		Button cancelButton = new Button(Translator.translate("Cancel"), e -> close());
		HorizontalLayout buttons = new HorizontalLayout(updateButton, cancelButton);
		buttons.setWidthFull();
		buttons.setJustifyContentMode(JustifyContentMode.END);
		getFooter().add(buttons);
	}

	private void configureGrid() {
		this.grid.setSelectionMode(SelectionMode.MULTI);
		this.grid.getThemeNames().add("row-stripes");
		// lets the narrow age-limit headers wrap instead of being truncated
		this.grid.addThemeVariants(GridVariant.WRAP_CELL_CONTENT);
		this.grid.setAllRowsVisible(true);
		this.grid.addColumn(AgeGroup::getDisplayName).setHeader(Translator.translate("Name"))
		        .setAutoWidth(true).setFlexGrow(0);
		this.grid.addColumn(SelectAgeGroupsDialog::championshipLabel)
		        .setHeader(Translator.translate("Championship")).setAutoWidth(true).setFlexGrow(0);
		this.grid.addColumn(ag -> ag.getGender() != null ? ag.getGender().asGenderName() : "")
		        .setHeader(Translator.translate("Gender")).setAutoWidth(true).setFlexGrow(0);
		this.grid.addColumn(AgeGroup::getMinAge).setHeader(Translator.translate("MinimumAge"))
		        .setWidth("7em").setFlexGrow(0);
		this.grid.addColumn(AgeGroup::getMaxAge).setHeader(Translator.translate("MaximumAge"))
		        .setWidth("7em").setFlexGrow(0);
		this.grid.addColumn(AgeGroup::getCategoriesAsString)
		        .setHeader(Translator.translate("BodyWeightCategories")).setFlexGrow(1);

		List<AgeGroup> all = AgeGroupRepository.findAll();
		all.sort(Comparator
		        .comparing((AgeGroup ag) -> !this.currentIds.contains(ag.getId()))
		        .thenComparing((a, b) -> ObjectUtils.compare(a.getChampionship(), b.getChampionship()))
		        .thenComparing((a, b) -> ObjectUtils.compare(a.getGender(), b.getGender()))
		        .thenComparing((a, b) -> ObjectUtils.compare(a.getMinAge(), b.getMinAge()))
		        .thenComparing((a, b) -> ObjectUtils.compare(a.getMaxAge(), b.getMaxAge())));
		this.grid.setItems(all);

		// members cannot be removed: an age group must always belong to a championship
		this.grid.setItemSelectableProvider(ag -> !this.currentIds.contains(ag.getId()));
		all.stream().filter(ag -> this.currentIds.contains(ag.getId())).forEach(this.grid::select);
	}

	private void confirmAndApply() {
		List<AgeGroup> toMove = this.grid.getSelectedItems().stream()
		        .filter(ag -> !this.currentIds.contains(ag.getId()))
		        .sorted(Comparator.comparing(AgeGroup::getDisplayName))
		        .toList();
		if (toMove.isEmpty()) {
			close();
			return;
		}

		String targetName = this.target.translate();
		String warning = toMove.stream()
		        .map(ag -> Translator.translate("Championship.MoveAgeGroupWarning",
		                ag.getDisplayName(),
		                championshipLabel(ag),
		                targetName))
		        .collect(Collectors.joining("<br>"));

		new ConfirmationDialog(
		        Translator.translate("Championship.SelectAgeGroups"),
		        warning,
		        null,
		        () -> {
			        try {
				        AgeGroupRepository.moveToChampionship(toMove.stream().map(AgeGroup::getId).toList(),
				                this.target.getName());
			        } catch (RuntimeException e) {
				        // nothing was saved; keep this dialog open so the user can retry or cancel
				        LoggerUtils.logError(logger, e);
				        Notification notification = Notification.show(Translator.translate("Error"), 5000,
				                Position.MIDDLE);
				        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
				        return;
			        }
			        WebSocketEventForwarder.sendDatabaseToAll();
			        close();
			        if (this.onSaved != null) {
				        this.onSaved.run();
			        }
		        }).open();
	}

	private static String championshipLabel(AgeGroup ageGroup) {
		String name = ageGroup.getChampionshipName();
		if (name == null || name.isBlank()) {
			return "";
		}
		Championship championship = Championship.of(name);
		if (championship != null && !championship.isCompetitionTemplate()) {
			return championship.translate();
		}
		return name;
	}
}
