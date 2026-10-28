package app.owlcms.nui.shared;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import app.owlcms.access.AccountTokens;
import app.owlcms.data.account.AccountToken;
import app.owlcms.data.account.UserAccount;
import app.owlcms.i18n.Translator;

/**
 * Generate, replace or revoke an account's token. The change is pending until the enclosing form saves it; a new
 * token is shown only while pending, since only its hash is kept.
 */
@SuppressWarnings("serial")
public class AccountTokenEditor extends VerticalLayout {

	private enum Pending {
		NONE, NEW, REVOKE
	}

	private final DateTimeFormatter dateFormat;
	private final NativeLabel status = new NativeLabel();
	private final Button generate = new Button();
	private final Button revoke = new Button(Translator.translate("Access.Token.Revoke"));
	private final TextField value = new TextField(Translator.translate("Access.Token.Value"));
	private final Paragraph notice = new Paragraph();
	private AccountToken current;
	private Pending pending = Pending.NONE;
	private AccountTokens.Generated generated;

	/** @param locale captured on the UI thread by the caller, for the creation date */
	public AccountTokenEditor(AccountToken current, Locale locale) {
		this.dateFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale);
		setPadding(false);
		setSpacing(false);
		NativeLabel title = new NativeLabel(Translator.translate("Access.Token.Title"));
		title.getStyle().set("font-weight", "bold");
		this.generate.addClickListener(e -> stageNew());
		this.revoke.addClickListener(e -> stageRevoke());
		this.revoke.addThemeVariants(ButtonVariant.LUMO_ERROR);
		this.value.setReadOnly(true);
		this.value.setWidthFull();
		this.value.getStyle().set("font-family", "monospace");
		add(title, this.status, new HorizontalLayout(this.generate, this.revoke), this.value, this.notice);
		reset(current);
	}

	/** Shows {@code token} as the saved token and discards any pending change. */
	public void reset(AccountToken token) {
		this.current = token;
		this.pending = Pending.NONE;
		this.generated = null;
		this.status.setText(token == null ? Translator.translate("Access.Token.None")
		        : Translator.translate("Access.Token.Current", token.getTokenId(),
		                token.getCreatedAt() == null ? "-" : this.dateFormat.format(token.getCreatedAt())));
		this.generate.setText(Translator.translate(token == null ? "Access.Token.Generate" : "Access.Token.Replace"));
		this.revoke.setEnabled(token != null);
		this.value.clear();
		this.value.setVisible(false);
		this.notice.setVisible(false);
	}

	public boolean hasChange() {
		return this.pending != Pending.NONE;
	}

	/** @return the token record to save, or null to remove the token; only meaningful when {@link #hasChange()} */
	public AccountToken pendingToken() {
		return this.pending == Pending.NEW ? this.generated.record() : null;
	}

	/** Applies the pending change, if any, to the account about to be saved. */
	public void applyTo(UserAccount account) {
		if (hasChange()) {
			account.replaceToken(pendingToken());
		}
	}

	private void stageNew() {
		this.generated = AccountTokens.generate();
		this.pending = Pending.NEW;
		this.value.setValue(this.generated.value());
		this.value.setVisible(true);
		this.notice.setText(Translator.translate("Access.Token.ShownOnce"));
		this.notice.setVisible(true);
		this.revoke.setEnabled(true);
	}

	private void stageRevoke() {
		this.generated = null;
		this.value.clear();
		this.value.setVisible(false);
		if (this.current == null) {
			// discarding a token that was never saved
			this.pending = Pending.NONE;
			this.notice.setVisible(false);
			this.revoke.setEnabled(false);
			return;
		}
		this.pending = Pending.REVOKE;
		this.notice.setText(Translator.translate("Access.Token.RevokePending"));
		this.notice.setVisible(true);
	}
}
