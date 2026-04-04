/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;

import app.owlcms.i18n.Translator;
import app.owlcms.nui.shared.OwlcmsLayout;

/**
 * Public read-only records page for recordsOnly mode.
 *
 * Inherits all grid columns, filters, and data logic from {@link RecordContent}.
 * Only the menu area (action buttons) and authorization differ.
 */
@SuppressWarnings("serial")
@Route(value = "publicRecords", layout = OwlcmsLayout.class)
public class PublicRecordsContent extends RecordContent {

	public PublicRecordsContent() {
		super(true);
	}

	/**
	 * Skip authorization — this page is publicly accessible.
	 */
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		// Public page — no login required
	}

	@Override
	public FlexLayout createMenuArea() {
		this.topBar = new FlexLayout();
		this.topBar.getStyle().set("flex", "100 1");
		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);
		applyRecordsOnlyToolbarOffset();

		Button exportRecordsButton = createExportRecordsButton();

		Button loginButton = new Button(Translator.translate("Edit"),
		        e -> UI.getCurrent().navigate("preparation/records", getLocation().getQueryParameters()));
		loginButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
		loginButton.getElement().getStyle().set("margin-right", "1em");

		Button importButton = createImportButton();

		this.topBar.add(exportRecordsButton, loginButton, importButton);
		return this.topBar;
	}
}
