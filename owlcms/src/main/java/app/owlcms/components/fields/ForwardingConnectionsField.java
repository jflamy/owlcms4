package app.owlcms.components.fields;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import app.owlcms.data.config.ForwardingConnection;
import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")
public class ForwardingConnectionsField extends CustomField<List<ForwardingConnection>> {

	private final VerticalLayout connectionRows = new VerticalLayout();
	private List<ForwardingConnection> connections = new ArrayList<>();

	/** Blank URLs are dropped on save; only non-blank URLs must carry a supported protocol. */
	public static boolean isValidUrl(String url) {
		return url == null || url.isBlank() || url.trim().matches("^(https?://|wss?://).+");
	}

	public ForwardingConnectionsField() {
		super(new ArrayList<>());
		setWidthFull();

		connectionRows.setPadding(false);
		connectionRows.setSpacing(false);
		connectionRows.setWidthFull();

		Button addConnection = new Button(Translator.translate("Config.AddEventForwardingConnection"),
		        new Icon(VaadinIcon.PLUS));
		addConnection.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		addConnection.addClickListener(event -> {
			connections.add(new ForwardingConnection());
			rebuildRows();
			updateValue();
		});

		add(connectionRows, addConnection);
	}

	@Override
	protected List<ForwardingConnection> generateModelValue() {
		return connections.stream().map(ForwardingConnection::new).toList();
	}

	@Override
	protected void setPresentationValue(List<ForwardingConnection> newConnections) {
		connections = newConnections == null
		        ? new ArrayList<>()
		        : newConnections.stream().map(ForwardingConnection::new).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
		rebuildRows();
	}

	private void rebuildRows() {
		connectionRows.removeAll();
		for (ForwardingConnection connection : connections) {
			connectionRows.add(createConnectionRow(connection));
		}
	}

	private FlexLayout createConnectionRow(ForwardingConnection connection) {
		TextField url = new TextField(Translator.translate("Config.EventForwardingURL"));
		url.setHelperText(Translator.translate("Config.EventForwardingURLHelp"));
		url.setManualValidation(true);
		url.setErrorMessage(Translator.translate("URL.missingProtocol"));
		url.setValue(connection.getUrl() != null ? connection.getUrl() : "");
		url.setInvalid(!isValidUrl(url.getValue()));
		url.setValueChangeMode(ValueChangeMode.LAZY);
		url.setValueChangeTimeout(600);
		url.setMinWidth("18em");
		url.getStyle().set("flex", "1 1 24em");
		url.addValueChangeListener(event -> {
			connection.setUrl(event.getValue());
			url.setInvalid(!isValidUrl(event.getValue()));
			updateValue();
		});

		PasswordField updateKey = new PasswordField(Translator.translate("Config.UpdateKey"));
		updateKey.setValue(connection.getUpdateKey() != null ? connection.getUpdateKey() : "");
		updateKey.setMinWidth("14em");
		updateKey.getStyle().set("flex", "1 1 18em");
		updateKey.addValueChangeListener(event -> {
			connection.setUpdateKey(event.getValue());
			updateValue();
		});

		Checkbox active = new Checkbox(Translator.translate("Active"), connection.isActive());
		active.getStyle().set("align-self", "center");
		active.addValueChangeListener(event -> {
			connection.setActive(event.getValue());
			updateValue();
		});

		Button remove = new Button(Translator.translate("Delete"), new Icon(VaadinIcon.TRASH));
		remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		remove.setAriaLabel(Translator.translate("Config.RemoveEventForwardingConnection"));
		remove.getStyle().set("align-self", "center");
		remove.addClickListener(event -> {
			connections.remove(connection);
			rebuildRows();
			updateValue();
		});

		FlexLayout row = new FlexLayout(url, updateKey, active);
		if (connection.isControlPanelManaged()) {
			Span managed = new Span(Translator.translate("Config.ControlPanelManaged"));
			managed.getElement().setAttribute("theme", "badge primary");
			managed.getStyle().set("align-self", "center");
			row.add(managed);
		} else {
			row.add(remove);
		}
		row.setWidthFull();
		row.getStyle().set("flex-wrap", "wrap");
		row.getStyle().set("gap", "var(--lumo-space-m)");
		row.getStyle().set("padding-bottom", "var(--lumo-space-s)");
		return row;
	}
}