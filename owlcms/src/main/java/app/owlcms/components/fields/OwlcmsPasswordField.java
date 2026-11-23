package app.owlcms.components.fields;

import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextFieldBase;

/**
 * A password field whose eye icon is skipped by the Tab key, so Tab goes to the next field. The icon still works with
 * the mouse.
 */
@SuppressWarnings("serial")
public class OwlcmsPasswordField extends PasswordField {

	private static final String IGNORE_PASSWORD_MANAGER_JS = "const apply = () => {"
	        + " const input = this.inputElement || this.querySelector('input');"
	        + " if (input) {"
	        + " input.setAttribute('data-lpignore', 'true');"
	        + " input.setAttribute('data-1p-ignore', 'true');"
	        + " input.setAttribute('data-bwignore', 'true');"
	        + " input.setAttribute('data-protonpass-ignore', 'true');"
	        + " input.setAttribute('data-form-type', 'other');"
	        + " input.setAttribute('spellcheck', 'false');"
	        + " }"
	        + "};"
	        + "apply();"
	        + "if (this.updateComplete) { this.updateComplete.then(apply); }";

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

	/**
	 * Marks a non-login secret field so browsers and password managers (LastPass, 1Password, Bitwarden, etc.) do not
	 * autofill a saved site password into it.
	 */
	public OwlcmsPasswordField disablePasswordManager() {
		disablePasswordManager(this);
		return this;
	}

	/**
	 * Marks a non-login field so browsers and password managers do not autofill saved login credentials into it.
	 */
	public static void disablePasswordManager(TextFieldBase<?, ?> field) {
		// browsers ignore autocomplete="off" on password inputs, so use new-password for password fields
		field.setAutocomplete(field instanceof PasswordField ? Autocomplete.NEW_PASSWORD : Autocomplete.OFF);
		field.getElement().setAttribute("data-lpignore", "true");
		field.getElement().setAttribute("data-1p-ignore", "true");
		field.getElement().setAttribute("data-bwignore", "true");
		field.getElement().setAttribute("data-protonpass-ignore", "true");
		field.getElement().setAttribute("data-form-type", "other");
		field.addAttachListener(event -> field.getElement().executeJs(IGNORE_PASSWORD_MANAGER_JS));
		if (field.isAttached()) {
			field.getElement().executeJs(IGNORE_PASSWORD_MANAGER_JS);
		}
	}
}
