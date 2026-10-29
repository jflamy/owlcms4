package app.owlcms.audit;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.LoggerFactory;

import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/** Startup-frozen integrity configuration and signing identity. */
public final class AuditIntegrity {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(AuditIntegrity.class);
	private static volatile State state;

	private AuditIntegrity() {
	}

	/**
	 * @param iwfCompliance the switch as read at startup; it forces accounts mode
	 * @param signingKey    the configured key when present, else the built-in key
	 */
	public record State(boolean iwfCompliance, boolean tcrrCompliance, boolean environmentKey, Path keyPath,
			AuditSigningKey signingKey) {
	}

	/**
	 * Audit logs are always sealed: with the configured key when present (a pending one is promoted first), else with
	 * the built-in key, whose fingerprint is predictable and gives no protection.
	 */
	public static State load(boolean iwfCompliance, boolean configuredTcrr, String flattenedPem, String pathOverride,
			Path home) throws IOException {
		if (flattenedPem != null) {
			return new State(iwfCompliance, configuredTcrr, true, null, AuditSigningKey.fromFlattenedPem(flattenedPem));
		}
		Path path = AuditKeyFiles.resolvePath(pathOverride, home);
		boolean keyConfigured = pathOverride != null || AuditKeyFiles.exists(AuditKeyFiles.pendingPath(path))
				|| AuditKeyFiles.exists(path);
		return new State(iwfCompliance, configuredTcrr, false, path,
				keyConfigured ? AuditKeyFiles.loadAndPromote(path) : AuditSigningKey.builtin());
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
			AuditLog.initializeIntegrity(loaded.signingKey());
			logger.info("Sealed audit logging enabled keySource={} fingerprint={}",
					loaded.signingKey().isBuiltin() ? "builtin" : "configured", loaded.signingKey().fingerprint());
			// the built-in key's fingerprint is public, so only a configured key provides a usable fingerprint
			StartupUtils.getStartupLogger().info("Audit Integrity: active, fingerprinting {}",
					loaded.signingKey().isBuiltin() ? "off" : "on");
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
