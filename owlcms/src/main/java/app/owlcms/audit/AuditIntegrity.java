package app.owlcms.audit;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.LoggerFactory;

import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/** Startup-frozen audit signing identity. */
public final class AuditIntegrity {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(AuditIntegrity.class);
	private static volatile State state;

	private AuditIntegrity() {
	}

	/**
	 * @param signingKey  the configured key when present, else the built-in key
	 * @param replacedKey the key that a pending key replaced at this startup, else null; see
	 *                    {@link AuditKeyFiles.Promotion}
	 */
	public record State(boolean environmentKey, Path keyPath, AuditSigningKey signingKey,
			AuditSigningKey replacedKey) {
	}

	/**
	 * Audit logs are always sealed: with the configured key when present (a pending one is promoted first), else with
	 * the built-in key, whose fingerprint is predictable and gives no protection.
	 */
	public static State load(String flattenedPem, String pathOverride, Path home) throws IOException {
		if (flattenedPem != null) {
			return new State(true, null, AuditSigningKey.fromFlattenedPem(flattenedPem), null);
		}
		Path path = AuditKeyFiles.resolvePath(pathOverride, home);
		boolean keyConfigured = pathOverride != null || AuditKeyFiles.exists(AuditKeyFiles.pendingPath(path))
				|| AuditKeyFiles.exists(path);
		if (!keyConfigured) {
			return new State(false, path, AuditSigningKey.builtin(), null);
		}
		AuditKeyFiles.Promotion promotion = AuditKeyFiles.loadAndPromote(path);
		return new State(false, path, promotion.key(), promotion.replaced());
	}

	/**
	 * Starts sealing before anything can be audited, first thing at startup: the key comes from the environment or the
	 * home directory, not from the database.
	 */
	public static synchronized void start() {
		if (state != null) {
			throw new IllegalStateException("Audit sealing is already started; key changes require a restart");
		}
		try {
			State loaded = load(System.getenv(AuditKeyFiles.PEM_ENV), System.getenv(AuditKeyFiles.PATH_ENV),
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
