package app.owlcms.monitors;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

record ResourceSample(LocalDateTime timestamp, double processCpu, double systemCpu,
		long rss, long peakRss, long heapUsed, long heapCommitted, long heapMax,
		long nonHeapUsed, boolean sinceStart, List<GcDelta> collectors) {

	private static final long MIB = 1024 * 1024;
	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	private static final List<String> COLUMNS = List.of("Timestamp", "CPU own %", "CPU sys %",
			"RSS MB", "RSS peak MB", "Heap used MB", "Heap comm MB", "Heap max MB",
			"Nonheap MB", "GC baseline");

	ResourceSample {
		collectors = List.copyOf(collectors);
	}

	record GcDelta(String name, long count, long millis) {
	}

	static long delta(long current, long previous) {
		return current < 0 || previous < 0 || current < previous ? -1 : current - previous;
	}

	static String collectorName(String name) {
		return switch (name) {
			case "G1 Young Generation" -> "young";
			case "G1 Concurrent GC" -> "conc";
			case "G1 Old Generation" -> "old";
			default -> name.replaceAll("\\s+", "");
		};
	}

	static String tableHeader(List<String> collectorNames) {
		return tableLine(columnNames(collectorNames), collectorNames);
	}

	String tableRow() {
		List<String> names = this.collectors.stream().map(GcDelta::name).toList();
		List<String> values = new ArrayList<>(List.of(TIMESTAMP.format(this.timestamp),
				cpu(this.processCpu), cpu(this.systemCpu), memory(this.rss), memory(this.peakRss),
				memory(this.heapUsed), memory(this.heapCommitted), memory(this.heapMax),
				memory(this.nonHeapUsed), this.sinceStart ? "since-start" : "interval"));
		// Undefined heap max is distinct from a metric that could not be sampled.
		if (this.heapMax < 0) {
			values.set(7, "?");
		}
		for (GcDelta gc : this.collectors) {
			values.add(number(gc.count()));
			values.add(number(gc.millis()));
		}
		return tableLine(values, names);
	}

	String standardMessage() {
		StringBuilder message = new StringBuilder(String.format(Locale.ROOT, "cpu own %s sys %s",
				percent(this.processCpu), percent(this.systemCpu)));
		if (this.rss >= 0) {
			message.append(" | rss ").append(memory(this.rss)).append("MB (peak ")
					.append(memory(this.peakRss)).append(')');
		}
		message.append(" | heap ").append(memory(this.heapUsed)).append('/')
				.append(memory(this.heapCommitted)).append('/')
				.append(this.heapMax < 0 ? "?" : memory(this.heapMax)).append("MB")
				.append(" | nonheap ").append(memory(this.nonHeapUsed)).append("MB | gc");
		if (this.sinceStart) {
			message.append(" since start");
		}
		for (GcDelta gc : this.collectors) {
			message.append(' ').append(collectorName(gc.name())).append(' ')
					.append(gc.count() < 0 ? "n/a" : "+" + gc.count()).append(" (")
					.append(gc.millis() < 0 ? "n/a" : gc.millis() + "ms").append(')');
		}
		return message.toString();
	}

	private static List<String> columnNames(List<String> collectorNames) {
		List<String> columns = new ArrayList<>(COLUMNS);
		for (String name : collectorNames) {
			columns.add(collectorName(name) + " count");
			columns.add(collectorName(name) + " ms");
		}
		return columns;
	}

	private static String tableLine(List<String> values, List<String> collectorNames) {
		List<String> headings = columnNames(collectorNames);
		List<String> cells = new ArrayList<>();
		for (int i = 0; i < values.size(); i++) {
			int width = i == 0 ? 23 : Math.max(12, headings.get(i).length());
			cells.add(String.format(Locale.ROOT, "%-" + width + "s", values.get(i)));
		}
		return String.join(" | ", cells);
	}

	private static String memory(long bytes) {
		return bytes < 0 ? "n/a" : Long.toString(bytes / MIB);
	}

	private static String number(long value) {
		return value < 0 ? "n/a" : Long.toString(value);
	}

	private static String cpu(double load) {
		return load < 0 || !Double.isFinite(load) ? "n/a" : String.format(Locale.ROOT, "%.1f", load * 100);
	}

	private static String percent(double load) {
		String value = cpu(load);
		return value.equals("n/a") ? value : value + "%";
	}
}
