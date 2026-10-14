package app.owlcms.audit;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import app.owlcms.audit.AuditIntegrityChecker.CoveredRange;
import app.owlcms.audit.AuditIntegrityChecker.FingerprintCoverage;
import app.owlcms.audit.AuditIntegrityChecker.RecordRange;
import app.owlcms.audit.AuditIntegrityChecker.Report;
import app.owlcms.audit.AuditIntegrityChecker.SegmentResult;

/**
 * Text of an integrity report, kept apart from the page so it can be tested. Labels come from {@code label}, a
 * translation lookup; failure and warning texts are the checker's own diagnostics.
 */
public final class AuditIntegrityReportText {
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/** One distinct fingerprint and the ranges it covers, one line per range. */
	public record FingerprintSummary(String fingerprint, boolean builtinKey, List<String> ranges) {
	}

	private AuditIntegrityReportText() {
	}

	public static String ranges(List<RecordRange> ranges) {
		return ranges.stream()
				.map(range -> range.firstRecord() == range.lastRecord() ? Long.toString(range.firstRecord())
						: range.firstRecord() + "–" + range.lastRecord())
				.collect(Collectors.joining(", "));
	}

	/** Per-file results, then failures and warnings; empty sections are omitted. */
	public static String details(Report report, Function<String, String> label) {
		StringBuilder text = new StringBuilder();
		if (!report.segments().isEmpty()) {
			text.append(label.apply("AuditCheck.Files")).append('\n');
			for (SegmentResult segment : report.segments()) {
				text.append('\n').append(segment.fileName()).append('\n');
				text.append("  ").append(label.apply("AuditCheck.Stream")).append(' ').append(value(segment.stream()))
						.append("   ").append(label.apply("AuditCheck.Run")).append(' ').append(value(segment.runId()))
						.append("   ").append(label.apply(segment.beingWritten() ? "AuditCheck.BeingWritten"
								: segment.finalized() ? "AuditCheck.Finalized" : "AuditCheck.NotFinalized"))
						.append('\n');
				text.append("  ").append(label.apply("AuditCheck.Fingerprint")).append(' ')
						.append(AuditSigningKey.displayFingerprint(segment.fingerprint()));
				if (segment.builtinKey()) {
					text.append("  (").append(label.apply("AuditIntegrity.Builtin")).append(')');
				}
				text.append('\n');
				if (!segment.validatedRanges().isEmpty()) {
					text.append("  ").append(label.apply("AuditCheck.Validated")).append(' ')
							.append(ranges(segment.validatedRanges())).append('\n');
				}
				if (!segment.unvalidatedRanges().isEmpty()) {
					text.append("  ").append(label.apply("AuditCheck.NotValidated")).append(' ')
							.append(ranges(segment.unvalidatedRanges()));
					if (segment.unvalidatedReason() != null) {
						text.append(" — ").append(label.apply("AuditCheck.Reason." + segment.unvalidatedReason().name()));
					}
					text.append('\n');
				}
			}
		}
		list(text, label.apply("AuditCheck.Failures"), report.failures());
		list(text, label.apply("AuditCheck.Warnings"), report.warnings());
		return text.toString().strip();
	}

	/** The closing summary: each distinct fingerprint in full, with its covered records and seal times. */
	public static List<FingerprintSummary> fingerprints(Report report, Function<String, String> label, ZoneId zone) {
		List<FingerprintSummary> result = new ArrayList<>();
		for (FingerprintCoverage coverage : report.fingerprints()) {
			List<String> lines = new ArrayList<>();
			for (CoveredRange range : coverage.coveredRanges()) {
				lines.add(range.fileName() + "   " + label.apply("AuditCheck.Run") + ' ' + value(range.runId())
						+ "   " + label.apply("AuditCheck.Records") + ' ' + ranges(range.ranges())
						+ "   " + time(range.firstSealedAt(), zone) + " → " + time(range.lastSealedAt(), zone));
			}
			result.add(new FingerprintSummary(AuditSigningKey.displayFingerprint(coverage.fingerprint()),
					coverage.builtinKey(), lines));
		}
		return result;
	}

	private static void list(StringBuilder text, String title, List<String> items) {
		if (items.isEmpty()) {
			return;
		}
		text.append("\n\n").append(title).append('\n');
		items.forEach(item -> text.append("- ").append(item).append('\n'));
	}

	private static String time(Instant instant, ZoneId zone) {
		return instant == null ? "-" : TIME.format(instant.atZone(zone));
	}

	private static String value(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}
}
