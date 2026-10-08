package app.owlcms.audit;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.vaadin.flow.component.UI;

import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.sift.MDCBasedDiscriminator;
import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.sift.AppenderFactory;

public final class AuditLog {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(AuditLog.class);
	private static final String APPENDER_NAME = "OWL_AUDIT_SIFT";
	private static final String AUDIT_LOGGER_NAME = "owlcms.audit";
	private static final String RUN_ID = UUID.randomUUID().toString();
	private static final Map<String, State> STATES = new HashMap<>();
	private static volatile SealedAuditBackend sealed;

	private AuditLog() {
	}

	public static void bulk(String platform, String action, int count) {
		AuditActor actor = AuditContext.actor() != null ? AuditContext.actor() : AuditActor.fromCurrentUi();
		if (actor != null) {
			write(AuditEntry.builder(platform, "bulk." + action).actor(actor).detail(AuditFormat.kv("count", count)).build());
		}
	}

	public static void write(AuditEntry entry) {
		writeBatch(List.of(entry), false);
	}

	public static synchronized void writeBatch(List<AuditEntry> entries, boolean closeBlock) {
		if (AuditContext.isSuppressed()) {
			return;
		}
		if (entries.isEmpty()) {
			return;
		}
		SealedAuditBackend backend = sealed;
		if (backend != null) {
			String platform = entries.getFirst().platform();
			if (entries.stream().anyMatch(entry -> !Objects.equals(platform, entry.platform()))) {
				throw new IllegalArgumentException("An audit batch must belong to one platform stream");
			}
			OffsetDateTime now = OffsetDateTime.now();
			List<AuditRecordSnapshot> captured = entries.stream()
					.map(entry -> AuditRecordSnapshot.capture(entry, now)).toList();
			try {
				backend.write(platform, captured, closeBlock, UI.getCurrent() == null);
			} catch (IOException e) {
				logger.error("Unable to persist sealed audit records platform={}", platform, e);
				throw new IllegalStateException("Audit persistence failed", e);
			}
			return;
		}
		for (AuditEntry entry : entries) {
			writePlain(entry);
		}
	}

	public static void closeBlock(String platform) {
		SealedAuditBackend backend = sealed;
		if (backend != null && !AuditContext.isSuppressed()) {
			try {
				backend.closeBlock(platform, UI.getCurrent() == null);
			} catch (IOException e) {
				logger.error("Unable to seal audit block platform={}", platform, e);
				throw new IllegalStateException("Audit sealing failed", e);
			}
		}
	}

	public static synchronized void initializeIntegrity(AuditSigningKey key) throws IOException {
		if (sealed != null) {
			throw new IllegalStateException("Sealed audit logging is already initialized");
		}
		String version = StartupUtils.getVersion();
		sealed = new SealedAuditBackend(DIRECTORY, key, AuditSealLimits.DEFAULT,
				Clock.systemDefaultZone(), System::nanoTime, RUN_ID,
				version == null || version.isBlank() ? "unknown" : version,
				AuditLog::emitReadable, true);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				ApplicationAudit.stopping("audit shutdown");
			} finally {
				try {
					sealed.close();
				} catch (IOException e) {
					logger.error("Audit shutdown could not finalize all streams", e);
				}
			}
		}, "AuditShutdown"));
	}

	public static boolean integrityHealthy() {
		SealedAuditBackend backend = sealed;
		return backend != null && backend.healthy();
	}

	public static final Path DIRECTORY = Path.of("logs", "audit");

	/**
	 * Forces every sealed stream into a new file so that all previous files are finalized. Returns the files
	 * now being written; empty when sealing is off, since plain logs have nothing to finalize.
	 */
	public static Set<Path> rollover() throws IOException {
		SealedAuditBackend backend = sealed;
		return backend == null ? Set.of() : backend.rollover();
	}

	private static void emitReadable(String platform, String line) {
		try {
			MDC.put("auditPlatform", sanitize(platform));
			ensureAppender().info(line);
		} finally {
			MDC.remove("auditPlatform");
		}
	}

	private static void writePlain(AuditEntry entry) {
		String platform = sanitize(entry.platform());
		try {
			State state = state(platform);
			Logger auditLogger = ensureAppender();
			if (!state.opened) {
				emit(auditLogger, platform, ++state.sequence,
						AuditEntry.builder(entry.platform(), "audit.open").actor(AuditActor.system())
								.detail(openDetail()).build());
				state.opened = true;
			}

			emit(auditLogger, platform, ++state.sequence, entry);
		} catch (RuntimeException e) {
			logger./**/error("Unable to write audit entry action={} platform={}", entry.action(), entry.platform(), e);
		} finally {
			MDC.remove("auditPlatform");
		}
	}

	private static String openDetail() {
		return AuditFormat.kvs("version", StartupUtils.getVersion(), "runId", RUN_ID);
	}

	private static void emit(Logger auditLogger, String platform, long sequence, AuditEntry entry) {
		OffsetDateTime now = OffsetDateTime.now();
		MDC.put("auditPlatform", platform);
		auditLogger.info(AuditFormat.format(sequence, entry, now, false));
		MDC.put("auditPlatform", platform + "_full");
		auditLogger.info(AuditFormat.format(sequence, entry, now, true));
	}

	private static State state(String platform) {
		return STATES.computeIfAbsent(platform, key -> new State());
	}

	private static Logger ensureAppender() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger auditLogger = context.getLogger(AUDIT_LOGGER_NAME);
		Appender<ILoggingEvent> appender = auditLogger.getAppender(APPENDER_NAME);
		if (appender == null || !appender.isStarted()) {
			auditLogger.detachAndStopAllAppenders();
			auditLogger.addAppender(createAppender(context));
		}
		auditLogger.setAdditive(false);
		auditLogger.setLevel(Level.INFO);
		return auditLogger;
	}

	private static SiftingAppender createAppender(LoggerContext context) {
		MDCBasedDiscriminator discriminator = new MDCBasedDiscriminator();
		discriminator.setContext(context);
		discriminator.setKey("auditPlatform");
		discriminator.setDefaultValue("competition");
		discriminator.start();

		SiftingAppender appender = new SiftingAppender();
		appender.setName(APPENDER_NAME);
		appender.setContext(context);
		appender.setDiscriminator(discriminator);
		appender.setAppenderFactory(new AppenderFactory<>() {
			@Override
			public Appender<ILoggingEvent> buildAppender(Context appenderContext, String discriminatingValue) {
				return rollingAppender(context, sanitize(discriminatingValue));
			}
		});
		appender.start();
		return appender;
	}

	private static RollingFileAppender<ILoggingEvent> rollingAppender(LoggerContext context, String platform) {
		RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
		appender.setName("OWL_AUDIT_" + platform);
		appender.setContext(context);
		appender.setAppend(true);
		appender.setFile("logs/audit/" + platform + ".log");

		TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
		policy.setContext(context);
		policy.setParent(appender);
		policy.setFileNamePattern("logs/audit/" + platform + "_%d{yyyy-MM-dd}.log");
		policy.start();

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(context);
		encoder.setPattern("%msg%n");
		encoder.start();

		appender.setRollingPolicy(policy);
		appender.setEncoder(encoder);
		appender.start();
		return appender;
	}

	private static String sanitize(String platform) {
		String name = platform == null || platform.isBlank() ? "competition" : platform;
		return name.replaceAll("[^A-Za-z0-9_-]", "_");
	}

	private static class State {
		private long sequence;
		private boolean opened;
	}
}