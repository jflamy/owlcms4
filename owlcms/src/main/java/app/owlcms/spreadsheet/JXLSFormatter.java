package app.owlcms.spreadsheet;

import java.text.MessageFormat;

import app.owlcms.i18n.Translator;

public class JXLSFormatter {
	
	public String format(String pattern, int arg) {
		String translated = Translator.translate(pattern);
		return MessageFormat.format(translated, arg);
	}

}
