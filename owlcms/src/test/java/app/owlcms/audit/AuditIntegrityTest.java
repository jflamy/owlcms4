package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Base64;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

public class AuditIntegrityTest {
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void withoutAKeyTheBuiltinKeySealsAndSwitchesAreIndependent() throws Exception {
		Path home = temporary.getRoot().toPath();
		AuditIntegrity.State tcrr = AuditIntegrity.load(false, true, null, null, home);
		assertFalse(tcrr.iwfCompliance());
		assertTrue(tcrr.tcrrCompliance());
		assertTrue(tcrr.signingKey().isBuiltin());
		AuditIntegrity.State iwf = AuditIntegrity.load(true, false, null, null, home);
		assertTrue(iwf.iwfCompliance());
		assertFalse(iwf.tcrrCompliance());
		assertTrue(iwf.signingKey().isBuiltin());
	}

	@Test
	public void aKeyFileSealsWhateverTheSwitches() throws Exception {
		Path home = temporary.getRoot().toPath();
		Path active = AuditKeyFiles.resolvePath(null, home);
		Files.createDirectories(active.getParent());
		AuditSigningKey key = AuditSigningKey.generate();
		Files.writeString(active, key.toPem());
		assertEquals(key.fingerprint(), AuditIntegrity.load(false, false, null, null, home).signingKey().fingerprint());
		assertEquals(key.fingerprint(), AuditIntegrity.load(true, false, null, null, home).signingKey().fingerprint());
	}

	@Test
	public void pendingKeyPromotesAndPreservesOldKeyUntilStartup() throws Exception {
		Path home = temporary.getRoot().toPath();
		Path active = AuditKeyFiles.resolvePath(null, home);
		Files.createDirectories(active.getParent());
		AuditSigningKey old = AuditSigningKey.generate();
		Files.writeString(active, old.toPem());
		AuditSigningKey pending = AuditKeyFiles.prepare(active);
		assertEquals(old.fingerprint(), AuditSigningKey.parsePem(Files.readString(active)).fingerprint());
		AuditIntegrity.State state = AuditIntegrity.load(false, false, null, null, home);
		assertEquals(pending.fingerprint(), state.signingKey().fingerprint());
		assertFalse(Files.exists(AuditKeyFiles.pendingPath(active)));
		assertEquals(pending.fingerprint(), AuditSigningKey.parsePem(Files.readString(active)).fingerprint());
	}

	@Test
	public void pendingKeyCompletesPromotionWhenActiveFileIsAbsent() throws Exception {
		Path home = temporary.getRoot().toPath();
		Path active = AuditKeyFiles.resolvePath(null, home);
		AuditSigningKey pending = AuditKeyFiles.prepare(active);
		assertFalse(Files.exists(active));
		assertEquals(pending.fingerprint(), AuditIntegrity.load(false, false, null, null, home)
				.signingKey().fingerprint());
	}

	@Test
	public void invalidPendingKeyDoesNotDeleteExistingPem() throws Exception {
		Path home = temporary.getRoot().toPath();
		Path active = AuditKeyFiles.resolvePath(null, home);
		Files.createDirectories(active.getParent());
		String original = AuditSigningKey.generate().toPem();
		Files.writeString(active, original);
		Files.writeString(AuditKeyFiles.pendingPath(active), "incomplete");
		assertThrows(IOException.class, () -> AuditIntegrity.load(false, false, null, null, home));
		assertEquals(original, Files.readString(active));
	}

	@Test
	public void environmentTakesPrecedenceAndLeavesBothFilesUntouched() throws Exception {
		Path home = temporary.getRoot().toPath();
		Path active = AuditKeyFiles.resolvePath(null, home);
		Files.createDirectories(active.getParent());
		Files.writeString(active, "invalid active");
		Files.writeString(AuditKeyFiles.pendingPath(active), "invalid pending");
		AuditSigningKey envKey = AuditSigningKey.generate();
		String flattened = Base64.getEncoder().encodeToString(envKey.toPem().getBytes(StandardCharsets.UTF_8));
		AuditIntegrity.State state = AuditIntegrity.load(false, false, flattened, "invalid relative override", home);
		assertEquals(envKey.fingerprint(), state.signingKey().fingerprint());
		assertTrue(state.environmentKey());
		assertEquals("invalid active", Files.readString(active));
		assertEquals("invalid pending", Files.readString(AuditKeyFiles.pendingPath(active)));
		assertThrows(IOException.class, () -> AuditIntegrity.load(false, false, "", null, home));
	}

	@Test
	public void missingExplicitPathFailsInsteadOfFallingBackToTheBuiltinKey() {
		Path missing = temporary.getRoot().toPath().resolve("missing.pem");
		assertThrows(IOException.class, () -> AuditIntegrity.load(false, false, null,
				missing.toString(), temporary.getRoot().toPath()));
	}

	@Test
	public void homeShortcutIsPortableAndRejectsTraversal() throws Exception {
		Path home = temporary.getRoot().toPath();
		Path expected = home.resolve(".owlcms").resolve("auditkey.pem");
		assertEquals(expected, AuditKeyFiles.resolvePath("~/.owlcms/auditkey.pem", home));
		assertEquals(expected, AuditKeyFiles.resolvePath("~\\.owlcms\\auditkey.pem", home));
		assertThrows(IOException.class, () -> AuditKeyFiles.resolvePath("~/../other/auditkey.pem", home));
		assertThrows(IOException.class, () -> AuditKeyFiles.resolvePath("relative/auditkey.pem", home));
		assertThrows(IOException.class, () -> AuditKeyFiles.resolvePath(" ", home));
		try (FileSystem windows = Jimfs.newFileSystem(Configuration.windows())) {
			Path winHome = windows.getPath("C:\\Users\\Director");
			assertEquals(winHome.resolve(".owlcms").resolve("auditkey.pem"),
					AuditKeyFiles.resolvePath("~/.owlcms/auditkey.pem", winHome));
			assertEquals(windows.getPath("E:\\owlcms\\auditkey.pem"),
					AuditKeyFiles.resolvePath("E:/owlcms/auditkey.pem", winHome));
			assertThrows(IOException.class, () -> AuditKeyFiles.resolvePath("~\\..\\other\\auditkey.pem", winHome));
		}
	}

	@Test
	public void preparationDoesNotOverwriteAnExistingPendingKeyAndUsesOwnerOnlyPermissions() throws Exception {
		Path active = AuditKeyFiles.resolvePath(null, temporary.getRoot().toPath());
		AuditSigningKey pending = AuditKeyFiles.prepare(active);
		assertThrows(IOException.class, () -> AuditKeyFiles.prepare(active));
		assertEquals(pending.fingerprint(),
				AuditSigningKey.parsePem(Files.readString(AuditKeyFiles.pendingPath(active))).fingerprint());
		if (active.getFileSystem().supportedFileAttributeViews().contains("posix")) {
			assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
					Files.getPosixFilePermissions(AuditKeyFiles.pendingPath(active)));
		}
	}

	@Test
	public void replacingAPendingKeyLeavesTheActiveKeyAndNoStagingFile() throws Exception {
		Path active = AuditKeyFiles.resolvePath(null, temporary.getRoot().toPath());
		Files.createDirectories(active.getParent());
		AuditSigningKey current = AuditSigningKey.generate();
		Files.writeString(active, current.toPem());
		AuditSigningKey first = AuditKeyFiles.prepare(active);
		AuditSigningKey second = AuditKeyFiles.prepare(active, true);
		assertFalse(first.fingerprint().equals(second.fingerprint()));
		assertEquals(second.fingerprint(),
				AuditSigningKey.parsePem(Files.readString(AuditKeyFiles.pendingPath(active))).fingerprint());
		assertEquals(current.toPem(), Files.readString(active));
		assertFalse(Files.exists(AuditKeyFiles.stagingPath(active)));
		if (active.getFileSystem().supportedFileAttributeViews().contains("posix")) {
			assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
					Files.getPosixFilePermissions(AuditKeyFiles.pendingPath(active)));
		}
		AuditIntegrity.State state = AuditIntegrity.load(false, false, null, null, temporary.getRoot().toPath());
		assertEquals(second.fingerprint(), state.signingKey().fingerprint());
	}

	@Test
	public void pendingKeyIsReportedAsAbsentValidOrInvalid() throws Exception {
		Path active = AuditKeyFiles.resolvePath(null, temporary.getRoot().toPath());
		assertTrue(AuditKeyFiles.readPending(active).isEmpty());
		AuditSigningKey prepared = AuditKeyFiles.prepare(active);
		AuditKeyFiles.PendingKey valid = AuditKeyFiles.readPending(active).orElseThrow();
		assertTrue(valid.valid());
		assertEquals(prepared.fingerprint(), valid.key().fingerprint());
		Files.writeString(AuditKeyFiles.pendingPath(active), "incomplete");
		AuditKeyFiles.PendingKey invalid = AuditKeyFiles.readPending(active).orElseThrow();
		assertFalse(invalid.valid());
		assertTrue(invalid.error() != null && !invalid.error().isBlank());
		AuditSigningKey replacement = AuditKeyFiles.prepare(active, true);
		assertEquals(replacement.fingerprint(), AuditKeyFiles.readPending(active).orElseThrow().key().fingerprint());
	}
}
