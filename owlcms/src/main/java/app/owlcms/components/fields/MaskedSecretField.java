package app.owlcms.components.fields;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.Autocapitalize;
import com.vaadin.flow.component.textfield.TextField;

/**
 * A masked text field for non-login secrets (configuration PINs, MQTT passwords, forwarding keys, control-panel
 * tokens, admin-set passwords). Uses {@code <input type="text">} masked with CSS instead of
 * {@code <input type="password">} because password managers (including LastPass) ignore opt-out attributes on
 * {@code type="password"} inputs and autofill site credentials into them.
 */
@SuppressWarnings("serial")
public class MaskedSecretField extends TextField {

	private static final String SECRET_CLASS = "owlcms-secret-field";
	private static final String VISIBLE_CLASS = "owlcms-secret-visible";

	private final Button revealButton = new Button();
	private boolean secretVisible = false;

	public MaskedSecretField() {
		addClassName(SECRET_CLASS);
		setAutocorrect(false);
		setAutocapitalize(Autocapitalize.NONE);
		OwlcmsPasswordField.disablePasswordManager(this);

		this.revealButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
		this.revealButton.setTabIndex(-1);
		this.revealButton.getStyle()
		        .set("color", "var(--vaadin-input-field-button-text-color, var(--lumo-contrast-60pct))")
		        .set("padding", "0")
		        .set("min-width", "auto")
		        .set("cursor", "pointer");
		this.revealButton.addClickListener(event -> setSecretVisible(!this.secretVisible));
		this.revealButton.addAttachListener(event -> this.revealButton.getElement().executeJs(
		        "if (!this.__owlcmsRevealMousedown) {"
		                + " this.__owlcmsRevealMousedown = true;"
		                + " this.addEventListener('mousedown', (e) => {"
		                + " e.preventDefault();"
		                + " const field = this.closest('vaadin-text-field');"
		                + " const input = field && (field.inputElement || field.querySelector('input'));"
		                + " if (input && !field.disabled) { input.focus(); }"
		                + " });"
		                + "}"));
		updateRevealButton();
		setSuffixComponent(this.revealButton);
		addBlurListener(event -> setSecretVisible(false));
	}

	public MaskedSecretField(String label) {
		this();
		setLabel(label);
	}

	public boolean isSecretVisible() {
		return this.secretVisible;
	}

	public void setSecretVisible(boolean visible) {
		if (this.secretVisible == visible) {
			return;
		}
		this.secretVisible = visible;
		if (visible) {
			addClassName(VISIBLE_CLASS);
		} else {
			removeClassName(VISIBLE_CLASS);
		}
		updateRevealButton();
	}

	public void setRevealButtonVisible(boolean visible) {
		this.revealButton.setVisible(visible);
	}

	private void updateRevealButton() {
		this.revealButton.setIcon(this.secretVisible ? VaadinIcon.EYE_SLASH.create() : VaadinIcon.EYE.create());
		this.revealButton.getElement().setAttribute("aria-pressed", Boolean.toString(this.secretVisible));
	}
}
