/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import app.owlcms.apputils.NotificationUtils;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.config.Config;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.spreadsheet.XLSXAgeGroupsExport;
import app.owlcms.utils.Resource;
import app.owlcms.utils.ResourceWalker;

/**
 * Shared top-bar of actions that mutate age groups / categories.
 *
 * Both {@link AgeGroupContent} and {@link ChampionshipsContent} expose this
 * menu so that the user does not have to navigate between the two pages to
 * load/upload/export age group definitions or reset participations.
 */
final class AgeGroupActionsMenu {

	private AgeGroupActionsMenu() {
	}

	/**
	 * Build the actions FlexLayout.
	 *
	 * @param onChange callback invoked after any operation that modifies age
	 *                 groups or athlete participations; pages should use this to
	 *                 refresh their grids. May be {@code null}.
	 * @return the populated layout
	 */
	static FlexLayout build(Runnable onChange) {
		Runnable refresh = onChange != null ? onChange : () -> {
		};

		ComboBox<Resource> ageGroupDefinitionSelect = new ComboBox<>();
		ageGroupDefinitionSelect.setPlaceholder(Translator.translate("ResetCategories.AvailableDefinitions"));
		Locale locale = OwlcmsSession.getLocale();
		List<Resource> resourceList = new ResourceWalker().getResourceList("/agegroups",
		        ResourceWalker::relativeName, null, locale, Config.getCurrent().isLocalTemplatesOnly());
		resourceList.sort(Resource::compareTo);
		ageGroupDefinitionSelect.setItems(resourceList);
		ageGroupDefinitionSelect.setValue(null);
		ageGroupDefinitionSelect.setWidth("15em");
		ageGroupDefinitionSelect.getStyle().set("margin-left", "1em");

		Button loadPredefined = new Button(Translator.translate("AgeGroups.LoadPredefined"), e -> {
			Resource definitions = ageGroupDefinitionSelect.getValue();
			if (definitions == null) {
				NotificationUtils.errorNotification(
				        Translator.translate("ResetCategories.PleaseSelectDefinitionFile"));
			} else {
				new ConfirmationDialog(Translator.translate("ResetCategories.ResetCategories"),
				        Translator.translate("ResetCategories.Warning_ResetCategories"),
				        Translator.translate("ResetCategories.CategoriesReset"), () -> {
					        AgeGroupRepository.reloadDefinitions(definitions.getFileName());
					        AthleteRepository.resetParticipations(false, true);
					        refresh.run();
				        }).open();
			}
		});

		Button uploadCustom = new Button(Translator.translate("AgeGroups.UploadCustom"),
		        new Icon(VaadinIcon.UPLOAD_ALT),
		        buttonClickEvent -> {
			        AgeGroupsFileUploadDialog ageGroupsFileUploadDialog = new AgeGroupsFileUploadDialog();
			        ageGroupsFileUploadDialog.setCallback(() -> {
				        AthleteRepository.resetParticipations(false, true);
				        refresh.run();
			        });
			        ageGroupsFileUploadDialog.open();
		        });

		HorizontalLayout reloadDefinition = new HorizontalLayout(ageGroupDefinitionSelect, loadPredefined);
		reloadDefinition.setAlignItems(FlexComponent.Alignment.BASELINE);
		reloadDefinition.setMargin(false);
		reloadDefinition.setPadding(false);
		reloadDefinition.setSpacing(false);

		Div exportAgeGroups = DownloadButtonFactory.createDynamicXLSXDownloadButton("AgeGroups",
		        Translator.translate("AgeGroups.ExportDefinitions"), new XLSXAgeGroupsExport(UI.getCurrent()));
		exportAgeGroups.getStyle().set("margin-left", "1em");

		Button editAgeGroups = new Button(
		        Translator.translate("DefineAgeGroups"),
		        VaadinIcon.PENCIL.create(),
		        e -> UI.getCurrent().navigate(AgeGroupContent.class));

		FlexLayout buttons = new FlexLayout(
		        new NativeLabel(Translator.translate("AgeGroups.Predefined")),
		        reloadDefinition,
		        hr(),
		        new NativeLabel(Translator.translate("AgeGroups.Custom")),
		        exportAgeGroups, uploadCustom, editAgeGroups);
		buttons.getStyle().set("flex-wrap", "wrap");
		buttons.getStyle().set("gap", "1ex");
		buttons.getStyle().set("margin-left", "5em");
		buttons.getStyle().set("flex", "100 1");
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		return buttons;
	}

	private static Hr hr() {
		Hr hr = new Hr();
		hr.setWidthFull();
		hr.getStyle().set("margin", "0");
		hr.getStyle().set("padding", "0");
		return hr;
	}
}
