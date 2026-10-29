package app.owlcms.access;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;

import app.owlcms.i18n.Translator;

/**
 * Shown when a navigation is refused because of its token. Deliberately bland: only the status text, no layout, no
 * buttons, no account details. It needs no authenticated principal.
 */
@SuppressWarnings("serial")
public class TokenRefusedView extends Div implements HasErrorParameter<TokenRefusedException> {

	@Override
	public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<TokenRefusedException> parameter) {
		// a dialog left open by the previous page in this tab would cover the refusal
		event.getUI().getChildren().filter(Dialog.class::isInstance).map(Dialog.class::cast)
		        .forEach(Dialog::close);
		int status = parameter.getCaughtException() instanceof TokenRefusedException refused ? refused.status()
		        : TokenNavigation.FORBIDDEN;
		setText(Translator.translate(switch (status) {
			case TokenNavigation.UNAUTHORIZED -> "Access.Refused.Unauthenticated";
			case TokenNavigation.TOO_MANY_REQUESTS -> "Access.Refused.TooManyRequests";
			default -> "Access.Refused.Forbidden";
		}));
		return status;
	}
}
