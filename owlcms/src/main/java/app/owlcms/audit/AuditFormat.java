package app.owlcms.audit;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

import app.owlcms.data.athlete.Athlete;
import tools.jackson.databind.json.JsonMapper;

public final class AuditFormat {
	private static final int SCHEMA_VERSION = 2;
	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	private AuditFormat() {
	}

	public static String format(long sequence, AuditEntry entry) {
		return format(sequence, entry, OffsetDateTime.now(), true);
	}

	/** Plain lines are readable only; full lines append one self-contained JSON object. */
	public static String format(long sequence, AuditEntry entry, OffsetDateTime now, boolean full) {
		AuditRecord record = capture(sequence, entry, now);
		return full ? full(record) : readable(record);
	}

	static AuditRecord capture(long sequence, AuditEntry entry, OffsetDateTime now) {
		AuditActor actor = entry.actor() != null ? entry.actor() : AuditActor.system();
		Athlete athlete = entry.athlete();
		AuditRecord.Athlete subject = athlete == null ? null
				: new AuditRecord.Athlete(athlete.getId() == null ? null : athlete.getId().toString(),
						athlete.getFullName());
		AuditRecord.Change change = entry.field() == null && entry.oldValue() == null && entry.newValue() == null
				? null : new AuditRecord.Change(entry.field(), freeze(entry.oldValue()), freeze(entry.newValue()),
						entry.priorRequested(), entry.previousAttemptRequested());
		return new AuditRecord(SCHEMA_VERSION, sequence, TIMESTAMP.format(now), entry.platform(), actor,
				entry.action(), subject, entry.session(), entry.attempt(), change, entry.cause(),
				freezeMap(entry.details()));
	}

	/**
	 * Schema version 2 adds the session column before the attempt; version 1 records are rendered as they were
	 * written, so the readable part of an older file can still be regenerated from its JSON.
	 */
	public static String readable(AuditRecord record) {
		String athlete = record.athlete() == null ? "-" : value(record.athlete().name());
		List<String> columns = new ArrayList<>(List.of(
				String.format(Locale.ROOT, "%5d", record.sequence()),
				TIME.format(OffsetDateTime.parse(record.timestamp())),
				String.format(Locale.ROOT, "%-13s", value(record.actor().displayStation())),
				String.format(Locale.ROOT, "%-19s", value(record.action())),
				String.format(Locale.ROOT, "%-40s", athlete)));
		if (record.schemaVersion() >= 2) {
			columns.add(String.format(Locale.ROOT, "%-8s", value(record.session())));
		}
		columns.add(String.format(Locale.ROOT, "%-3s", value(record.attempt())));
		columns.add(value(what(record)));
		return String.join(" | ", columns);
	}

	static String full(AuditRecord record) {
		return full(readable(record), record);
	}

	/** The full line is the readable line, exactly as it is, followed by the JSON. */
	static String full(String readable, AuditRecord record) {
		return readable + " | json=" + json(record);
	}

	static String json(AuditRecord record) {
		return MAPPER.writeValueAsString(record);
	}

	private static String what(AuditRecord record) {
		String change = change(record);
		String detail = details(record.action(), record.details());
		return detail.isEmpty() ? change : change.isEmpty() ? detail : change + ", " + detail;
	}

	private static String change(AuditRecord record) {
		AuditRecord.Change change = record.change();
		if (change == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		String field = change.field();
		if (field != null && record.attempt() != null && field.contains(".")) {
			field = field.substring(field.indexOf('.') + 1);
		}
		append(sb, field);
		Object after = record.attempt() == null ? change.after()
				: annotate(change.field(), change.after(), change.priorRequested(), change.previousAttemptRequested());
		if (change.before() != null || (record.action() != null
				&& (record.action().endsWith(".change") || record.action().startsWith("athlete.")))) {
			append(sb, changeValue(record, change.before()) + " -> " + changeValue(record, after));
		} else if (after != null) {
			append(sb, changeValue(record, after));
		}
		return sb.toString();
	}

	static Object annotate(String field, Object value, Integer priorRequested, Integer previousAttemptRequested) {
		if (value == null || field == null) {
			return value;
		}
		Integer requested = AthleteDiff.weight(value);
		if (field.endsWith(".automatic")) {
			if (requested == null || previousAttemptRequested == null || previousAttemptRequested <= 0) {
				return value;
			}
			if (requested == previousAttemptRequested + 1) {
				return value + " (+1)";
			}
			return requested.equals(previousAttemptRequested) ? value + " (SAME)" : value;
		}
		if (requested == null || priorRequested == null) {
			return value;
		}
		long delta = (long) requested - priorRequested;
		return value + (delta == 0 ? " (SAME)" : String.format(Locale.ROOT, " (%+d)", delta));
	}

	private static String changeValue(AuditRecord record, Object value) {
		if (value != null && record.action() != null && record.action().startsWith("clock.set")
				&& record.change() != null && "clock".equals(record.change().field())) {
			Integer millis = millis(value);
			return FopAudit.formatClock(millis) + " (" + millis + "ms)";
		}
		return text(value);
	}

	/** Ordered, typed detail fields. */
	public static Map<String, Object> fields(Object... keysAndValues) {
		if (keysAndValues.length % 2 != 0) {
			throw new IllegalArgumentException("Audit detail fields require key/value pairs");
		}
		Map<String, Object> result = new LinkedHashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			if (!(keysAndValues[i] instanceof String key) || key.isBlank() || result.containsKey(key)) {
				throw new IllegalArgumentException("Audit detail field name is missing or duplicated");
			}
			result.put(key, keysAndValues[i + 1]);
		}
		return result;
	}

	public static String details(String action, Map<String, Object> values) {
		StringJoiner result = new StringJoiner(",");
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if ("clockMillis".equals(key)) {
				result.add(value == null ? kv("clock", null) : FopAudit.timeDetail("clock", millis(value)));
			} else if ("remainingMillis".equals(key)) {
				result.add(FopAudit.timeDetail("remaining", millis(value)));
			} else if ("automatic".equals(key) && "break.start".equals(action) && Boolean.TRUE.equals(value)) {
				result.add("automatic");
			} else if (decisionField(action, key)) {
				result.add(kv(key, Boolean.TRUE.equals(value) ? "GOOD LIFT" : "NO LIFT"));
			} else if (value instanceof Collection<?> collection) {
				StringJoiner joined = new StringJoiner(",");
				collection.forEach(item -> joined.add(String.valueOf(item)));
				result.add(kv(key, joined.toString()));
			} else {
				result.add(kv(key, value));
			}
		}
		return result.toString();
	}

	private static boolean decisionField(String action, String key) {
		return action != null && (("decision".equals(key)
				&& (action.startsWith("referee.") || action.startsWith("jury.")))
				|| ("result".equals(key) && action.startsWith("referee.decision")));
	}

	private static Integer millis(Object value) {
		if (value == null) {
			return null;
		}
		if (!(value instanceof Number number) || number.longValue() < Integer.MIN_VALUE
				|| number.longValue() > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Audit clock milliseconds must be an integer");
		}
		return number.intValue();
	}

	private static Map<String, Object> freezeMap(Map<String, ?> values) {
		Map<String, Object> result = new LinkedHashMap<>();
		values.forEach((key, value) -> result.put(key, freeze(value)));
		return Collections.unmodifiableMap(result);
	}

	private static Object freeze(Object value) {
		if (value == null || value instanceof String || value instanceof Boolean || value instanceof Number) {
			return value;
		}
		if (value instanceof CharSequence || value instanceof Character || value instanceof Enum<?>
				|| value instanceof TemporalAccessor) {
			return value.toString();
		}
		if (value instanceof Collection<?> collection) {
			List<Object> copy = new ArrayList<>();
			collection.forEach(item -> copy.add(freeze(item)));
			return Collections.unmodifiableList(copy);
		}
		throw new IllegalArgumentException("Unsupported audit value type: " + value.getClass().getName());
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
		StringJoiner result = new StringJoiner(",");
		fields(keysAndValues).forEach((key, value) -> result.add(kv(key, value)));
		return result.toString();
	}
}
