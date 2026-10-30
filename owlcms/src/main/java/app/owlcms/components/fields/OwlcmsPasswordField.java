package app.owlcms.components.fields;

import com.vaadin.flow.component.textfield.PasswordField;

/**
 * A password field whose eye icon is skipped by the Tab key, so Tab goes to the next field. The icon still works with
 * the mouse.
 */
@SuppressWarnings("serial")
public class OwlcmsPasswordField extends PasswordField {

	public OwlcmsPasswordField() {
		// the web component sets the icon's tabindex during its first update, so this runs after the update
		addAttachListener(event -> getElement().executeJs(
		        "this.updateComplete.then(() => {"
		                + " const reveal = this.querySelector('[slot=\"reveal\"]');"
		                + " if (reveal) { reveal.setAttribute('tabindex', '-1'); } })"));
	}

	public OwlcmsPasswordField(String label) {
		this();
		setLabel(label);
	}
}
