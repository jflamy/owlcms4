package app.owlcms.audit;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.LoggerFactory;

import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import ch.qos.logback.classic.Logger;

/** Startup-frozen integrity configuration and signing identity. */
public final class AuditIntegrity {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(AuditIntegrity.class);
	private static volatile State state;

	private AuditIntegrity() {
	}

	public record State(boolean enabled, boolean tcrrCompliance, boolean forcedByKey, boolean environmentKey,
			Path keyPath, AuditSigningKey signingKey) {
	}

	public static State load(boolean configuredIntegrity, boolean configuredTcrr, String flattenedPem,
			String pathOverride, Path home) throws IOException {
		if (flattenedPem != null) {
			return new State(true, configuredTcrr, true, true, null,
					AuditSigningKey.fromFlattenedPem(flattenedPem));
		}
		Path path = AuditKeyFiles.resolvePath(pathOverride, home);
		boolean forced = pathOverride != null || AuditKeyFiles.exists(AuditKeyFiles.pendingPath(path))
				|| AuditKeyFiles.exists(path);
		if (forced) {
			return new State(true, configuredTcrr, true, false, path, AuditKeyFiles.loadAndPromote(path));
		}
		return new State(configuredIntegrity, configuredTcrr, false, false, path,
				configuredIntegrity ? AuditSigningKey.builtin() : null);
	}

	public static synchronized void initialize(Config config) {
		if (state != null) {
			throw new IllegalStateException("Audit integrity is already initialized; key changes require a restart");
		}
		try {
			State loaded = load(config.requestedFeatureSwitch(FeatureSwitch.IWF_COMPLIANCE),
					config.requestedFeatureSwitch(FeatureSwitch.TCRR_COMPLIANCE),
					System.getenv(AuditKeyFiles.PEM_ENV), System.getenv(AuditKeyFiles.PATH_ENV),
					Path.of(System.getProperty("user.home")));
			if (loaded.enabled()) {
				AuditLog.initializeIntegrity(loaded.signingKey());
				logger.info("Sealed audit logging enabled keySource={} fingerprint={} forcedByKey={}",
						loaded.signingKey().isBuiltin() ? "builtin" : "configured",
						loaded.signingKey().fingerprint(), loaded.forcedByKey());
			} else {
				logger.info("Audit integrity mode is off");
			}
			state = loaded;
		} catch (IOException | RuntimeException e) {
			logger.error("Audit integrity startup failed", e);
			throw new IllegalStateException("Audit integrity startup failed: " + e.getMessage(), e);
		}
	}

	public static boolean isInitialized() {
		return state != null;
	}

	public static State current() {
		State current = state;
		if (current == null) {
			throw new IllegalStateException("Audit integrity startup configuration is not initialized");
		}
		return current;
	}
}
