package app.owlcms.audit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;
import java.util.Set;

public final class AuditKeyFiles {
	public static final String PEM_ENV = "OWLCMS_AUDIT_SIGNING_PEM_BASE64";
	public static final String PATH_ENV = "OWLCMS_AUDIT_KEY_FILE";

	private AuditKeyFiles() {
	}

	public static Path resolvePath(String override, Path home) throws IOException {
		Path normalizedHome = home.toAbsolutePath().normalize();
		if (override == null) {
			return normalizedHome.resolve(".owlcms").resolve("auditkey.pem");
		}
		String value = override.strip();
		if (value.isEmpty()) {
			throw new IOException("Audit key path is empty");
		}
		if ("~".equals(value)) {
			return normalizedHome;
		}
		if (value.startsWith("~/") || value.startsWith("~\\")) {
			Path resolved = normalizedHome.resolve(value.substring(2).replace('\\', '/')).normalize();
			if (!resolved.startsWith(normalizedHome)) {
				throw new IOException("Audit key path escapes the user home directory");
			}
			return resolved;
		}
		Path resolved = home.getFileSystem().getPath(value).normalize();
		if (!resolved.isAbsolute()) {
			throw new IOException("Audit key path must be absolute or begin with ~/");
		}
		return resolved;
	}

	public static Path pendingPath(Path active) {
		return active.resolveSibling(active.getFileName() + ".new");
	}

	public static boolean exists(Path path) throws IOException {
		try {
			Files.readAttributes(path, BasicFileAttributes.class);
			return true;
		} catch (NoSuchFileException e) {
			return false;
		}
	}

	/**
	 * @param key      the key in force after promotion
	 * @param replaced the key a promoted pending key replaced, kept in memory for this run only so that values
	 *                 protected by it can be re-encrypted; null when nothing was promoted
	 */
	public record Promotion(AuditSigningKey key, AuditSigningKey replaced) {
	}

	public static Promotion loadAndPromote(Path active) throws IOException {
		Path pending = pendingPath(active);
		AuditSigningKey replaced = null;
		if (exists(pending)) {
			AuditSigningKey.parsePem(Files.readString(pending, StandardCharsets.UTF_8));
			if (exists(active)) {
				replaced = AuditSigningKey.parsePem(Files.readString(active, StandardCharsets.UTF_8));
			}
			Files.deleteIfExists(active);
			Files.move(pending, active);
		}
		return new Promotion(AuditSigningKey.parsePem(Files.readString(active, StandardCharsets.UTF_8)), replaced);
	}

	public static AuditSigningKey prepare(Path active) throws IOException {
		return prepare(active, false);
	}

	/**
	 * Writes a new pending key beside the active one. The key is written to a staging file, reread and validated, and
	 * only then moved to {@code .new}, so startup never finds a partially written pending key. The active key is never
	 * modified.
	 *
	 * @param replacePending replace an existing pending key instead of failing
	 */
	public static AuditSigningKey prepare(Path active, boolean replacePending) throws IOException {
		AuditSigningKey generated = AuditSigningKey.generate();
		Path parent = active.toAbsolutePath().getParent();
		boolean posix = active.getFileSystem().supportedFileAttributeViews().contains("posix");
		FileAttribute<?>[] directoryAttributes = posix
				? new FileAttribute<?>[] { PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")) }
				: new FileAttribute<?>[0];
		FileAttribute<?>[] fileAttributes = posix
				? new FileAttribute<?>[] { PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")) }
				: new FileAttribute<?>[0];
		Files.createDirectories(parent, directoryAttributes);
		Path pending = pendingPath(active);
		if (!replacePending && exists(pending)) {
			throw new FileAlreadyExistsException(pending.toString(), null, "A pending audit key already exists");
		}
		Path staging = stagingPath(active);
		Files.deleteIfExists(staging);
		try {
			try (FileChannel file = FileChannel.open(staging,
					Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), fileAttributes)) {
				ByteBuffer bytes = StandardCharsets.UTF_8.encode(generated.toPem());
				while (bytes.hasRemaining()) {
					file.write(bytes);
				}
				file.force(true);
			}
			requireFingerprint(staging, generated);
			if (replacePending) {
				try {
					Files.move(staging, pending, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				} catch (AtomicMoveNotSupportedException e) {
					Files.move(staging, pending, StandardCopyOption.REPLACE_EXISTING);
				}
			} else {
				Files.move(staging, pending);
			}
		} finally {
			Files.deleteIfExists(staging);
		}
		return requireFingerprint(pending, generated);
	}

	/** A pending key waiting for the next startup: either a valid key or the reason it cannot be used. */
	public record PendingKey(AuditSigningKey key, String error) {
		public boolean valid() {
			return this.key != null;
		}
	}

	/** @return empty when no pending key is waiting */
	public static Optional<PendingKey> readPending(Path active) throws IOException {
		Path pending = pendingPath(active);
		if (!exists(pending)) {
			return Optional.empty();
		}
		try {
			return Optional.of(new PendingKey(AuditSigningKey.parsePem(Files.readString(pending, StandardCharsets.UTF_8)),
					null));
		} catch (IOException | RuntimeException e) {
			return Optional.of(new PendingKey(null, e.getMessage()));
		}
	}

	/** Withdraws a pending key: the active key stays in force after the next restart. */
	public static void deletePending(Path active) throws IOException {
		Files.deleteIfExists(pendingPath(active));
	}

	static Path stagingPath(Path active) {
		return active.resolveSibling(active.getFileName() + ".new.tmp");
	}

	private static AuditSigningKey requireFingerprint(Path file, AuditSigningKey expected) throws IOException {
		AuditSigningKey reread = AuditSigningKey.parsePem(Files.readString(file, StandardCharsets.UTF_8));
		if (!expected.fingerprint().equals(reread.fingerprint())) {
			throw new IOException("Prepared audit key fingerprint changed during writing");
		}
		return reread;
	}
}
