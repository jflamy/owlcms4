package app.owlcms.audit;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import app.owlcms.data.athlete.Athlete;

public final class AuditFormat {
	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

	private AuditFormat() {
	}

	public static String format(long sequence, AuditEntry entry) {
		return format(sequence, entry, OffsetDateTime.now(), true);
	}

	/** Short form for the readable log; full form adds cause, ids, timestamp and client identification. */
	public static String format(long sequence, AuditEntry entry, OffsetDateTime now, boolean full) {
		AuditActor actor = entry.actor() != null ? entry.actor() : AuditActor.system();
		Athlete athlete = entry.athlete();
		String line = String.join(" | ",
				String.format("%5d", sequence),
				TIME.format(now),
				String.format("%-12s", value(actor.displayStation())),
				String.format("%-18s", value(entry.action())),
				String.format("%-40s", athlete == null ? "-" : value(athlete.getFullName())),
				String.format("%-3s", value(entry.attempt())),
				value(what(entry)));
		if (!full) {
			return line;
		}
		return String.join(" | ", line,
				"cause=" + value(entry.cause()),
				"athleteId=" + (athlete == null ? "-" : value(athlete.getId())),
				"platform=" + value(entry.platform()),
				"timestamp=" + TIMESTAMP.format(now),
				"mode=" + value(actor.mode()),
				"user=" + value(actor.user()),
				"client=" + value(actor.client()),
				"device=" + value(actor.device()));
	}

	/** Change and detail together, e.g. "change1 - -> 47" or "decision=GOOD". */
	static String what(AuditEntry entry) {
		String change = change(entry);
		String detail = entry.detail();
		if (detail == null || detail.isBlank()) {
			return change;
		}
		return change.isEmpty() ? detail : change + ", " + detail;
	}

	/** Field and value, e.g. "change1 - -> 47". */
	static String change(AuditEntry entry) {
		StringBuilder sb = new StringBuilder();
		String field = entry.field();
		if (field != null && entry.attempt() != null && field.contains(".")) {
			field = field.substring(field.indexOf('.') + 1);
		}
		append(sb, field);
		if (entry.oldValue() != null || (entry.action() != null && entry.action().endsWith(".change"))) {
			append(sb, text(entry.oldValue()) + " -> " + text(entry.newValue()));
		} else if (entry.newValue() != null) {
			append(sb, entry.newValue().toString());
		}
		return sb.toString();
	}

	private static void append(StringBuilder sb, String part) {
		if (part == null || part.isBlank()) {
			return;
		}
		if (!sb.isEmpty()) {
			sb.append(' ');
		}
		sb.append(part);
	}

	private static String text(Object value) {
		return value == null || value.toString().isBlank() ? "-" : value.toString();
	}

	static String value(Object value) {
		if (value == null || value.toString().isBlank()) {
			return "-";
		}
		return value.toString().replace("\\", "\\\\").replace("|", "\\|").replace("\r", "\\n")
				.replace("\n", "\\n");
	}

	/** {@code key=value}, quoting the value when it contains blanks, commas, '=' or quotes. */
	public static String kv(String key, Object value) {
		String s = value == null ? "" : value.toString();
		if (s.isBlank()) {
			return key + "=-";
		}
		if (s.matches(".*[\\s,=\"].*")) {
			s = "\"" + s.replace("\"", "\"\"") + "\"";
		}
		return key + "=" + s;
	}

	/** Comma-separated {@link #kv} pairs from alternating keys and values. */
	public static String kvs(Object... keysAndValues) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
			if (!sb.isEmpty()) {
				sb.append(',');
			}
			sb.append(kv((String) keysAndValues[i], keysAndValues[i + 1]));
		}
		return sb.toString();
	}
}