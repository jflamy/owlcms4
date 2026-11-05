package app.owlcms.audit;

import java.util.function.Function;

import app.owlcms.audit.ExportSignature.KeyMatch;
import app.owlcms.audit.ExportSignature.Result;
import app.owlcms.audit.ExportSignature.Status;

/**
 * Text of an export signature check, kept apart from the page so it can be tested. Labels come from {@code label}, a
 * translation lookup.
 */
public final class ExportCheckText {

	private ExportCheckText() {
	}

	/** @return the translation key of the outcome: unsigned, invalid, or signed with which key */
	public static String verdictKey(Result result, KeyMatch match) {
		return switch (result.status()) {
			case ABSENT -> "ExportCheck.Unsigned";
			case INVALID -> "ExportCheck.Invalid";
			case VALID -> "ExportCheck.Signed." + match.name();
		};
	}

	/** @return true when the fingerprint identifies the signer and should be compared with the competition's */
	public static boolean compareFingerprint(Result result, KeyMatch match) {
		return result.status() == Status.VALID && (match == KeyMatch.OWN || match == KeyMatch.FOREIGN);
	}

	/** File name, size, locally computed SHA-256 and, when the file carries one, the key fingerprint. */
	public static String details(String fileName, Result result, Function<String, String> label) {
		StringBuilder text = new StringBuilder();
		line(text, label.apply("ExportCheck.File"), fileName == null || fileName.isBlank() ? "-" : fileName);
		line(text, label.apply("ExportCheck.Size"), Long.toString(result.bytes()));
		line(text, "SHA-256", result.sha256());
		if (result.fingerprint() != null) {
			line(text, label.apply("AuditCheck.Fingerprint"), AuditSigningKey.displayFingerprint(result.fingerprint()));
		}
		return text.toString().strip();
	}

	private static void line(StringBuilder text, String name, String value) {
		text.append(name).append(": ").append(value).append('\n');
	}
}
