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
	private static volatile Key key;

	private AuditIntegrity() {
	}

	/**
	 * @param signingKey    the configured key when present, else the built-in key
	 * @param replacedKey   the key that a pending key replaced at this startup, else null; see
	 *                      {@link AuditKeyFiles.Promotion}
	 */
	public record State(boolean tcrrCompliance, boolean environmentKey, Path keyPath,
			AuditSigningKey signingKey, AuditSigningKey replacedKey) {
	}

	/** The signing key as loaded at startup: its source, and the key a pending key replaced, if any. */
	private record Key(boolean environmentKey, Path keyPath, AuditSigningKey signingKey,
			AuditSigningKey replacedKey) {
	}

	/**
	 * Audit logs are always sealed: with the configured key when present (a pending one is promoted first), else with
	 * the built-in key, whose fingerprint is predictable and gives no protection.
	 */
	public static State load(boolean configuredTcrr, String flattenedPem, String pathOverride,
			Path home) throws IOException {
		Key loaded = loadKey(flattenedPem, pathOverride, home);
		return new State(configuredTcrr, loaded.environmentKey(), loaded.keyPath(), loaded.signingKey(),
				loaded.replacedKey());
	}

	private static Key loadKey(String flattenedPem, String pathOverride, Path home) throws IOException {
		if (flattenedPem != null) {
			return new Key(true, null, AuditSigningKey.fromFlattenedPem(flattenedPem), null);
		}
		Path path = AuditKeyFiles.resolvePath(pathOverride, home);
		boolean keyConfigured = pathOverride != null || AuditKeyFiles.exists(AuditKeyFiles.pendingPath(path))
				|| AuditKeyFiles.exists(path);
		if (!keyConfigured) {
			return new Key(false, path, AuditSigningKey.builtin(), null);
		}
		AuditKeyFiles.Promotion promotion = AuditKeyFiles.loadAndPromote(path);
		return new Key(false, path, promotion.key(), promotion.replaced());
	}

	/**
	 * Starts sealing before anything can be audited, first thing at startup: the key comes from the environment or the
	 * home directory, not from the database.
	 */
	public static synchronized void start() {
		if (key != null) {
			throw new IllegalStateException("Audit sealing is already started; key changes require a restart");
		}
		try {
			Key loaded = loadKey(System.getenv(AuditKeyFiles.PEM_ENV), System.getenv(AuditKeyFiles.PATH_ENV),
					Path.of(System.getProperty("user.home")));
			AuditLog.initializeIntegrity(loaded.signingKey());
			logger.info("Sealed audit logging enabled keySource={} fingerprint={}",
					loaded.signingKey().isBuiltin() ? "builtin" : "configured", loaded.signingKey().fingerprint());
			// the built-in key's fingerprint is public, so only a configured key provides a usable fingerprint
			StartupUtils.getStartupLogger().info("Audit Integrity: active, fingerprinting {}",
					loaded.signingKey().isBuiltin() ? "off" : "on");
			key = loaded;
		} catch (IOException | RuntimeException e) {
			logger.error("Audit integrity startup failed", e);
			throw new IllegalStateException("Audit integrity startup failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Freezes the startup configuration once the database is read: the TCRR setting, with the key that {@link #start()}
	 * loaded.
	 */
	public static synchronized void initialize(Config config) {
		if (state != null) {
			throw new IllegalStateException("Audit integrity is already initialized; key changes require a restart");
		}
		Key loaded = key;
		if (loaded == null) {
			throw new IllegalStateException("Audit sealing must be started before the configuration is read");
		}
		state = new State(config.requestedFeatureSwitch(FeatureSwitch.TCRR_COMPLIANCE), loaded.environmentKey(),
				loaded.keyPath(), loaded.signingKey(), loaded.replacedKey());
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
