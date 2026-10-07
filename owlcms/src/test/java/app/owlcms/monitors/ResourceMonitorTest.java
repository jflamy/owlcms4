package app.owlcms.monitors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import app.owlcms.data.config.FeatureSwitch;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.status.Status;
import jdk.jfr.FlightRecorder;

public class ResourceMonitorTest {
	private static final long MIB = 1024 * 1024;
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 14, 20, 30, 123_000_000);

	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void standardSampleUsesSpecifiedUnitsAndCollectorNames() {
		ResourceSample sample = sample(false);
		assertEquals("cpu own 3.8% sys 13.5% | rss 982MB (peak 1000) | heap 398/1108/6144MB"
				+ " | nonheap 112MB | gc young +2 (50ms) conc +2 (3ms) old +0 (0ms)",
				sample.standardMessage());
	}

	@Test
	public void timestampIsFirstTableColumnAndSchemaMatchesRows() {
		ResourceSample sample = sample(true);
		List<String> names = sample.collectors().stream().map(ResourceSample.GcDelta::name).toList();
		String[] headings = ResourceSample.tableHeader(names).split(" \\| ");
		String[] values = sample.tableRow().split(" \\| ");
		assertEquals(16, headings.length);
		assertEquals(headings.length, values.length);
		assertEquals("Timestamp", headings[0].trim());
		assertEquals("2026-10-07 14:20:30.123", values[0].trim());
		assertEquals("3.8", values[1].trim());
		assertEquals("982", values[3].trim());
		assertEquals("since-start", values[9].trim());
		assertEquals("young count", headings[10].trim());
		assertEquals("2", values[10].trim());
		assertEquals("50", values[11].trim());
		assertEquals("interval", sample(false).tableRow().split(" \\| ")[9].trim());
		assertTrue(sample.standardMessage().contains("gc since start young"));
		for (int i = 0; i < headings.length; i++) {
			assertEquals(headings[i].length(), values[i].length());
		}
	}

	@Test
	public void unavailableMetricsKeepTableColumns() {
		ResourceSample sample = new ResourceSample(NOW, -1, Double.NaN, -1, -1,
				398 * MIB, 1108 * MIB, -1, 112 * MIB, false,
				List.of(new ResourceSample.GcDelta("Other Collector", -1, -1)));
		String[] values = sample.tableRow().split(" \\| ");
		assertEquals(12, values.length);
		assertEquals("n/a", values[1].trim());
		assertEquals("n/a", values[2].trim());
		assertEquals("n/a", values[3].trim());
		assertEquals("n/a", values[4].trim());
		assertEquals("?", values[7].trim());
		assertEquals("n/a", values[10].trim());
		assertEquals("n/a", values[11].trim());
		assertFalse(sample.standardMessage().contains(" | rss "));
		assertTrue(sample.standardMessage().contains("cpu own n/a sys n/a"));
		assertTrue(sample.standardMessage().contains("heap 398/1108/?MB"));
		assertTrue(sample.standardMessage().contains("OtherCollector n/a (n/a)"));
	}

	@Test
	public void gcDeltasHandleFirstSampleNoCollectionsAndUnavailableCounters() {
		assertEquals(12, ResourceSample.delta(12, 0));
		assertEquals(2, ResourceSample.delta(12, 10));
		assertEquals(0, ResourceSample.delta(12, 12));
		assertEquals(-1, ResourceSample.delta(-1, 12));
		assertEquals(-1, ResourceSample.delta(12, -1));
		assertEquals(-1, ResourceSample.delta(3, 12));
	}

	@Test
	public void collectorNamesAreNotRestrictedToG1() {
		assertEquals("young", ResourceSample.collectorName("G1 Young Generation"));
		assertEquals("conc", ResourceSample.collectorName("G1 Concurrent GC"));
		assertEquals("old", ResourceSample.collectorName("G1 Old Generation"));
		assertEquals("ZGCMinorCycles", ResourceSample.collectorName("ZGC Minor Cycles"));
		assertTrue(ResourceSample.tableHeader(List.of("ZGC Minor Cycles")).contains("ZGCMinorCycles count"));
	}

	@Test
	public void formattingDoesNotDependOnUiLocale() {
		Locale previous = Locale.getDefault();
		try {
			Locale.setDefault(Locale.FRANCE);
			assertTrue(sample(false).standardMessage().startsWith("cpu own 3.8% sys 13.5%"));
			assertEquals("3.8", sample(false).tableRow().split(" \\| ")[1].trim());
		} finally {
			Locale.setDefault(previous);
		}
	}

	@Test
	public void resourceFeatureSwitchIsOffByDefaultAndDiscoverable() {
		assertFalse(FeatureSwitch.RESOURCE_TRACES.isEnabledByDefault());
		assertEquals(FeatureSwitch.RESOURCE_TRACES, FeatureSwitch.fromId("resourceTraces").orElseThrow());
		assertEquals("FeatureSwitch.resourceTraces", FeatureSwitch.RESOURCE_TRACES.getTranslationKey());
	}

	@Test
	public void tableLoggerRemainsEnabledWhenStandardLoggerIsOff() {
		LoggerContext context = new LoggerContext();
		try {
			ListAppender<ILoggingEvent> standard = new ListAppender<>();
			standard.setContext(context);
			standard.start();
			ListAppender<ILoggingEvent> table = new ListAppender<>();
			table.setContext(context);
			table.start();
			Logger normal = context.getLogger("app.owlcms.monitors.ResourceMonitor");
			normal.setLevel(Level.INFO);
			normal.addAppender(standard);
			Logger tabular = context.getLogger(normal.getName() + ".table");
			tabular.setLevel(Level.INFO);
			tabular.setAdditive(false);
			tabular.addAppender(table);

			normal.info(sample(false).standardMessage());
			tabular.info(sample(false).tableRow());
			assertEquals(1, standard.list.size());
			assertEquals(1, table.list.size());

			normal.setLevel(Level.OFF);
			normal.info(sample(false).standardMessage());
			tabular.info(sample(false).tableRow());
			assertEquals(1, standard.list.size());
			assertEquals(2, table.list.size());

			normal.setLevel(Level.INFO);
			normal.info(sample(false).standardMessage());
			assertEquals(2, standard.list.size());
		} finally {
			context.stop();
		}
	}

	@Test
	public void monitorLifecycleSamplesBothOutputsAndClosesJfrOnDisable() {
		LoggerContext context = new LoggerContext();
		AtomicBoolean tracing = new AtomicBoolean();
		ListAppender<ILoggingEvent> standard = new ListAppender<>();
		standard.setContext(context);
		standard.start();
		ListAppender<ILoggingEvent> rows = new ListAppender<>();
		rows.setContext(context);
		rows.start();
		Logger normal = context.getLogger("resources");
		normal.setLevel(Level.INFO);
		normal.addAppender(standard);
		Logger table = context.getLogger("resources.table");
		table.setLevel(Level.INFO);
		table.setAdditive(false);
		table.addAppender(rows);
		ResourceMonitor monitor = new ResourceMonitor(tracing::get, normal, table);
		int recordingsBefore = recordingCount();
		boolean rssAvailable = FlightRecorder.isAvailable()
				&& FlightRecorder.getFlightRecorder().getEventTypes().stream()
						.anyMatch(type -> type.getName().equals("jdk.ResidentSetSize"));
		try {
			monitor.startScheduler();
			monitor.startScheduler();
			monitor.tick();
			assertEquals(0, standard.list.size());
			assertEquals(0, rows.list.size());
			assertEquals(recordingsBefore, recordingCount());

			tracing.set(true);
			monitor.tick();
			assertEquals(recordingsBefore + (rssAvailable ? 1 : 0), recordingCount());
			assertEquals("resource monitor enabled", standard.list.getFirst().getFormattedMessage());
			assertEquals(1, rows.list.size());
			assertEquals("since-start", rows.list.getFirst().getFormattedMessage().split(" \\| ")[9].trim());
			assertTrue(standard.list.getLast().getFormattedMessage().contains("gc since start"));

			monitor.tick();
			assertEquals(2, rows.list.size());
			assertEquals("interval", rows.list.getLast().getFormattedMessage().split(" \\| ")[9].trim());
			int standardEvents = standard.list.size();
			normal.setLevel(Level.OFF);
			monitor.tick();
			assertEquals(standardEvents, standard.list.size());
			assertEquals(3, rows.list.size());

			tracing.set(false);
			monitor.tick();
			assertEquals(recordingsBefore, recordingCount());
			assertEquals(3, rows.list.size());

			normal.setLevel(Level.INFO);
			tracing.set(true);
			monitor.tick();
			assertEquals(4, rows.list.size());
			assertEquals("since-start", rows.list.getLast().getFormattedMessage().split(" \\| ")[9].trim());
			monitor.stopScheduler();
			assertEquals(recordingsBefore, recordingCount());
			assertEquals("resource monitor disabled", standard.list.getLast().getFormattedMessage());
			standardEvents = standard.list.size();
			monitor.stopScheduler();
			assertEquals(standardEvents, standard.list.size());

			tracing.set(false);
			monitor.startScheduler();
			monitor.tick();
			assertEquals(standardEvents, standard.list.size());
			assertEquals(4, rows.list.size());
		} finally {
			monitor.stopScheduler();
			context.stop();
		}
	}

	@Test
	public void bothLoggingConfigurationsSupportIndependentTableOutput() throws Exception {
		for (String configuration : List.of("logback.xml", "logback-console.xml")) {
			File directory = this.temporary.newFolder(configuration);
			String contents;
			try (InputStream source = getClass().getClassLoader().getResourceAsStream(configuration)) {
				assertTrue("Missing " + configuration, source != null);
				contents = new String(source.readAllBytes(), StandardCharsets.UTF_8);
			}
			String path = directory.getAbsolutePath().replace('\\', '/')
					.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
			contents = contents.replace("logs/", path + "/");
			LoggerContext context = new LoggerContext();
			try {
				context.setMDCAdapter(new LogbackMDCAdapter());
				JoranConfigurator configurator = new JoranConfigurator();
				configurator.setContext(context);
				configurator.doConfigure(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)));
				assertFalse(configuration + " has configuration errors",
						context.getStatusManager().getCopyOfStatusList().stream()
								.anyMatch(status -> status.getLevel() == Status.ERROR));
				Logger normal = context.getLogger("app.owlcms.monitors.ResourceMonitor");
				Logger table = context.getLogger(normal.getName() + ".table");
				assertEquals(Level.INFO, normal.getLevel());
				assertEquals(Level.INFO, table.getLevel());
				assertFalse(table.isAdditive());
				assertTrue(table.getAppender("RESOURCE_TABLE").isStarted());
				context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender("CONSOLE");

				normal.info(sample(false).standardMessage());
				normal.setLevel(Level.OFF);
				normal.info("suppressed resource sample");
				table.info(sample(false).tableRow());
				context.stop();

				String tableFile = Files.readString(new File(directory, "resources.log").toPath());
				assertTrue(tableFile.startsWith("Timestamp"));
				assertTrue(tableFile.contains(sample(false).tableRow()));
				assertFalse(tableFile.contains(sample(false).standardMessage()));
				if (configuration.equals("logback.xml")) {
					String standardFile = Files.readString(new File(directory, "owlcms.log").toPath());
					assertTrue(standardFile.contains(sample(false).standardMessage()));
					assertFalse(standardFile.contains(sample(false).tableRow()));
					assertFalse(standardFile.contains("suppressed resource sample"));
				}
			} finally {
				context.stop();
			}
		}
	}

	@Test
	public void encoderWritesHeaderOnFileOpeningAndRollover() throws Exception {
		File directory = this.temporary.newFolder("resources");
		File active = new File(directory, "resources.log");
		LoggerContext context = new LoggerContext();
		RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
		try {
			context.setMDCAdapter(new LogbackMDCAdapter());
			appender.setContext(context);
			appender.setName("RESOURCE_TABLE_TEST");
			appender.setFile(active.getAbsolutePath());
			ResourceTableEncoder encoder = new ResourceTableEncoder();
			encoder.setContext(context);
			encoder.start();
			appender.setEncoder(encoder);

			FixedWindowRollingPolicy policy = new FixedWindowRollingPolicy();
			policy.setContext(context);
			policy.setParent(appender);
			policy.setFileNamePattern(new File(directory, "resources.%i.log").getAbsolutePath());
			policy.setMinIndex(1);
			policy.setMaxIndex(1);
			policy.start();
			appender.setRollingPolicy(policy);
			SizeBasedTriggeringPolicy<ILoggingEvent> trigger = new SizeBasedTriggeringPolicy<>();
			trigger.setContext(context);
			trigger.setMaxFileSize(new FileSize(1024 * 1024));
			trigger.start();
			appender.setTriggeringPolicy(trigger);
			appender.start();
			assertTrue(appender.isStarted());

			Logger table = context.getLogger("resources");
			table.setLevel(Level.INFO);
			table.setAdditive(false);
			table.addAppender(appender);
			String row = "2026-10-07 14:20:30.123 | sample";
			table.info(row);
			appender.rollover();
			table.info(row);
			appender.stop();

			String header = new String(encoder.headerBytes(), StandardCharsets.UTF_8);
			String expected = header + row + System.lineSeparator();
			assertEquals(expected, Files.readString(active.toPath()));
			assertEquals(expected, Files.readString(new File(directory, "resources.1.log").toPath()));
			assertFalse(header.contains("%msg"));
		} finally {
			appender.stop();
			context.stop();
		}
	}

	private static ResourceSample sample(boolean sinceStart) {
		return new ResourceSample(NOW, 0.038, 0.135, 982 * MIB, 1000 * MIB,
				398 * MIB, 1108 * MIB, 6144 * MIB, 112 * MIB, sinceStart,
				List.of(new ResourceSample.GcDelta("G1 Young Generation", 2, 50),
						new ResourceSample.GcDelta("G1 Concurrent GC", 2, 3),
						new ResourceSample.GcDelta("G1 Old Generation", 0, 0)));
	}

	private static int recordingCount() {
		return FlightRecorder.isAvailable() ? FlightRecorder.getFlightRecorder().getRecordings().size() : 0;
	}
}
