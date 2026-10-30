package app.owlcms.nui.admin;

import com.vaadin.flow.server.VaadinServletRequest;

import app.owlcms.access.ControlPanelChallenge;
import app.owlcms.components.fields.OwlcmsPasswordField;
import app.owlcms.i18n.Translator;
import jakarta.servlet.http.HttpServletRequest;

/** The cp challenge field: the operator pastes the control panel token before an action that needs locality. */
@SuppressWarnings("serial")
class ControlPanelTokenField extends OwlcmsPasswordField {

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
		ControlPanelChallenge.Result result = ControlPanelChallenge.check(getValue(), request.getRemoteAddr(), what);
		clear();
		if (result.accepted()) {
			setInvalid(false);
			return true;
		}
		if (result.noToken()) {
			setErrorMessage(Translator.translate("Access.ControlPanelTokenNotSet"));
		} else {
			setErrorMessage(result.blockedSeconds() > 0
			        ? Translator.translate("Access.TooManyAttempts") + " (" + result.blockedSeconds() + " s)"
			        : Translator.translate("Access.ControlPanelTokenWrong"));
		}
		setInvalid(true);
		return false;
	}
}
