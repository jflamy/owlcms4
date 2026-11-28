/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import app.owlcms.access.AccessUi;
import app.owlcms.access.Principal;
import app.owlcms.apputils.NotificationUtils;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.ConfigRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.preparation.ConfigContent;
import app.owlcms.utils.ResourceWalker;

/**
 * Compact style-directory dropdown with an immediate Apply button for the display selection tabs.
 */
final class DisplayStyleSelector {

	private DisplayStyleSelector() {
	}

	static VerticalLayout create(Principal principal, String labelText, Function<Config, String> reader,
	        BiConsumer<Config, String> writer, String auditCause) {
		VerticalLayout container = new VerticalLayout();
		container.setSpacing(false);
		container.setPadding(true);
		container.setWidthFull();
		if (!AccessUi.canOpen(principal, ConfigContent.class, null)) {
			container.setVisible(false);
			return container;
		}

		NativeLabel heading = new NativeLabel(Translator.translate("Config.stylesTitle"));
		heading.getStyle().set("font-weight", "bold");

		NativeLabel description = new NativeLabel(labelText);
		description.getStyle().set("margin-bottom", "0.6ex");

		List<String> styles = new ArrayList<>(ResourceWalker.getAvailableStyleDirectories());
		String current = reader.apply(Config.getCurrent());
		if (current != null && !current.isBlank() && !styles.contains(current)) {
			styles.add(current);
		}

		ComboBox<String> styleSelect = new ComboBox<>();
		styleSelect.setItems(styles);
		styleSelect.setAllowCustomValue(true);
		styleSelect.addCustomValueSetListener(e -> {
			String custom = e.getDetail() != null ? e.getDetail().trim() : "";
			if (!custom.isEmpty() && !styles.contains(custom)) {
				styles.add(custom);
				styleSelect.setItems(styles);
			}
			styleSelect.setValue(custom);
		});
		if (current != null && !current.isBlank()) {
			styleSelect.setValue(current);
		}
		styleSelect.setWidth("24em");
		styleSelect.setMaxWidth("100%");

		Button applyButton = new Button(Translator.translate("Apply"), e -> {
			String raw = styleSelect.getValue();
			String trimmed = raw != null ? raw.trim() : "";
			Config saved = ConfigRepository.apply(c -> writer.accept(c, trimmed), AuditOrigin.fromUi(auditCause));
			String normalized = reader.apply(saved);
			List<String> refreshed = new ArrayList<>(ResourceWalker.getAvailableStyleDirectories());
			if (normalized != null && !normalized.isBlank() && !refreshed.contains(normalized)) {
				refreshed.add(normalized);
			}
			styles.clear();
			styles.addAll(refreshed);
			styleSelect.setItems(styles);
			styleSelect.setValue(normalized != null ? normalized : "");
			NotificationUtils.saved();
		});
		applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		HorizontalLayout row = new HorizontalLayout(styleSelect, applyButton);
		row.setAlignItems(Alignment.CENTER);
		row.setPadding(false);
		row.setWidthFull();

		VerticalLayout content = new VerticalLayout(description, row);
		content.setSpacing(false);
		content.setPadding(false);
		content.getStyle().set("padding-top", "0.4em");

		Details details = new Details(heading, content);
		details.setOpened(false);
		details.setWidthFull();

		container.add(details);
		return container;
	}
}
