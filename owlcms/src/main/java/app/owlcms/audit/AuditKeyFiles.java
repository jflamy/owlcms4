package app.owlcms.audit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
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

	public static AuditSigningKey loadAndPromote(Path active) throws IOException {
		Path pending = pendingPath(active);
		if (exists(pending)) {
			AuditSigningKey.parsePem(Files.readString(pending, StandardCharsets.UTF_8));
			Files.deleteIfExists(active);
			Files.move(pending, active);
		}
		return AuditSigningKey.parsePem(Files.readString(active, StandardCharsets.UTF_8));
	}

	public static AuditSigningKey prepare(Path active) throws IOException {
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
		try (FileChannel file = FileChannel.open(pending,
				Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), fileAttributes)) {
			ByteBuffer bytes = StandardCharsets.UTF_8.encode(generated.toPem());
			while (bytes.hasRemaining()) {
				file.write(bytes);
			}
			file.force(true);
		}
		AuditSigningKey reread = AuditSigningKey.parsePem(Files.readString(pending, StandardCharsets.UTF_8));
		if (!generated.fingerprint().equals(reread.fingerprint())) {
			throw new IOException("Prepared audit key fingerprint changed during writing");
		}
		return reread;
	}
}
