package app.owlcms.nui.admin;

import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.server.VaadinServletRequest;

import app.owlcms.access.ControlPanelChallenge;
import app.owlcms.i18n.Translator;
import jakarta.servlet.http.HttpServletRequest;

/** The cp challenge field: the operator pastes the control panel token before an action that needs locality. */
@SuppressWarnings("serial")
class ControlPanelTokenField extends PasswordField {

	ControlPanelTokenField() {
		super(Translator.translate("Access.ControlPanelToken"));
		setHelperText(Translator.translate("Access.ControlPanelTokenHint"));
		setRevealButtonVisible(true);
		setWidthFull();
	}

	/**
	 * Runs on the request thread: the challenge needs the connection of the current request. Clears the field, and
	 * shows the reason when refused.
	 *
	 * @param what the protected action, for the log and the audit trail
	 */
	boolean passes(String what) {
		HttpServletRequest request = VaadinServletRequest.getCurrent().getHttpServletRequest();
		ControlPanelChallenge.Result result = ControlPanelChallenge.check(getValue(), request.getRemoteAddr(),
		        request::getHeader, what);
		clear();
		if (result.accepted()) {
			setInvalid(false);
			return true;
		}
		setErrorMessage(result.blockedSeconds() > 0
		        ? Translator.translate("Access.TooManyAttempts") + " (" + result.blockedSeconds() + " s)"
		        : Translator.translate("Access.ControlPanelTokenWrong"));
		setInvalid(true);
		return false;
	}
}
