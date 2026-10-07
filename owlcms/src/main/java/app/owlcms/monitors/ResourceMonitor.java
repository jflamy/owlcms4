package app.owlcms.monitors;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.management.OperatingSystemMXBean;

import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import jdk.jfr.FlightRecorder;
import jdk.jfr.consumer.RecordingStream;

public final class ResourceMonitor {
	private static final ResourceMonitor INSTANCE = new ResourceMonitor();
	private static final long INTERVAL_SECONDS = 30;
	private static final String RSS_EVENT = "jdk.ResidentSetSize";

	private final Logger logger;
	private final Logger tableLogger;
	private final BooleanSupplier tracingEnabled;
	private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
	private final List<GarbageCollectorMXBean> collectors = List.copyOf(ManagementFactory.getGarbageCollectorMXBeans());
	private final OperatingSystemMXBean operatingSystem =
			ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean bean ? bean : null;
	private ScheduledExecutorService executor;
	private RecordingStream stream;
	private boolean enabled;
	private Map<String, GcCounters> previous;
	private volatile Rss rss = new Rss(-1, -1);

	private record Rss(long current, long peak) {
	}

	private record GcCounters(long count, long millis) {
	}

	private ResourceMonitor() {
		this(() -> Config.getCurrent().featureSwitch(FeatureSwitch.RESOURCE_TRACES),
				LoggerFactory.getLogger(ResourceMonitor.class),
				LoggerFactory.getLogger(ResourceMonitor.class.getName() + ".table"));
	}

	ResourceMonitor(BooleanSupplier tracingEnabled, Logger logger, Logger tableLogger) {
		this.tracingEnabled = tracingEnabled;
		this.logger = logger;
		this.tableLogger = tableLogger;
	}

	public static void start() {
		INSTANCE.startScheduler();
	}

	public static void stop() {
		INSTANCE.stopScheduler();
	}

	synchronized void startScheduler() {
		if (this.executor != null) {
			return;
		}
		this.executor = Executors.newSingleThreadScheduledExecutor(task -> {
			Thread thread = new Thread(task, "owlcms-resource-monitor");
			thread.setDaemon(true);
			return thread;
		});
		this.executor.scheduleAtFixedRate(this::tick, INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
	}

	synchronized void stopScheduler() {
		if (this.executor != null) {
			this.executor.shutdownNow();
			this.executor = null;
		}
		disable();
	}

	synchronized void tick() {
		try {
			if (this.executor == null) {
				return;
			}
			if (!this.tracingEnabled.getAsBoolean()) {
				disable();
				return;
			}
			if (!this.enabled) {
				this.enabled = true;
				logger.info("resource monitor enabled");
				openStream();
			}
			sample();
		} catch (Throwable failure) {
			// Scheduled executors cancel all subsequent ticks if an exception escapes.
			logger./**/warn("Unable to sample resource usage", failure);
		}
	}

	private void disable() {
		if (this.stream != null) {
			this.stream.close();
			this.stream = null;
		}
		this.rss = new Rss(-1, -1);
		this.previous = null;
		if (this.enabled) {
			this.enabled = false;
			logger.info("resource monitor disabled");
		}
	}

	private void openStream() {
		try {
			if (!FlightRecorder.isAvailable()
					|| FlightRecorder.getFlightRecorder().getEventTypes().stream()
							.noneMatch(type -> RSS_EVENT.equals(type.getName()))) {
				logger.info("Resource RSS unavailable: JFR or {} is not available", RSS_EVENT);
				return;
			}
			RecordingStream recording = new RecordingStream();
			try {
				recording.enable(RSS_EVENT).withPeriod(Duration.ofSeconds(INTERVAL_SECONDS));
				recording.onEvent(RSS_EVENT, event -> this.rss = new Rss(event.getLong("size"), event.getLong("peak")));
				recording.onError(failure -> logger./**/warn("Resource RSS recording failed", failure));
				recording.onClose(() -> this.rss = new Rss(-1, -1));
				recording.startAsync();
				this.stream = recording;
			} catch (RuntimeException failure) {
				recording.close();
				throw failure;
			}
		} catch (IllegalStateException | SecurityException failure) {
			logger.info("Resource RSS unavailable: JFR recording could not start", failure);
		}
	}

	private void sample() {
		LocalDateTime timestamp = LocalDateTime.now();
		MemoryUsage heap = this.memory.getHeapMemoryUsage();
		Rss resident = this.rss;
		List<ResourceSample.GcDelta> deltas = new ArrayList<>();
		Map<String, GcCounters> current = new HashMap<>();
		for (GarbageCollectorMXBean collector : this.collectors) {
			String name = collector.getName();
			GcCounters counters = new GcCounters(collector.getCollectionCount(), collector.getCollectionTime());
			current.put(name, counters);
			GcCounters baseline = this.previous == null ? new GcCounters(0, 0) : this.previous.get(name);
			deltas.add(new ResourceSample.GcDelta(name,
					ResourceSample.delta(counters.count(), baseline.count()),
					ResourceSample.delta(counters.millis(), baseline.millis())));
		}
		ResourceSample snapshot = new ResourceSample(timestamp,
				this.operatingSystem == null ? -1 : this.operatingSystem.getProcessCpuLoad(),
				this.operatingSystem == null ? -1 : this.operatingSystem.getCpuLoad(),
				resident.current(), resident.peak(), heap.getUsed(), heap.getCommitted(), heap.getMax(),
				this.memory.getNonHeapMemoryUsage().getUsed(), this.previous == null, deltas);
		if (logger.isInfoEnabled()) {
			logger.info(snapshot.standardMessage());
		}
		if (tableLogger.isInfoEnabled()) {
			tableLogger.info(snapshot.tableRow());
		}
		this.previous = current;
	}
}
