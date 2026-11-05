package app.owlcms.audit;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import app.owlcms.Main;
import app.owlcms.access.Principal;
import app.owlcms.access.Principal.AuthSource;
import app.owlcms.audit.ExportSignature.KeyMatch;
import app.owlcms.audit.ExportSignature.Result;
import app.owlcms.audit.ExportSignature.Status;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.FormatDetector;
import app.owlcms.data.export.v2.CompetitionDataV2;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.tests.TestData;

/**
 * Separate JVM: in-memory database with sealed audit logging and a configured key, so the real V2 export path picks
 * up the audit signing key. Exports, checks the signature, then imports an edited copy. Writes the key fingerprint
 * to {@code fingerprint.txt} for the calling test, which reads the audit trail.
 */
public final class ExportSigningFixture {
	private ExportSigningFixture() {
	}

	public static void main(String[] args) {
		try {
			run();
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}

	private static void run() throws Exception {
		Path home = Path.of("home").toAbsolutePath();
		System.setProperty("user.home", home.toString());
		AuditKeyFiles.prepare(AuditKeyFiles.resolvePath(null, home));
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		AuditIntegrity.initialize(Config.getCurrent());
		TestData.insertInitialData(1, true);
		AuditSigningKey key = AuditIntegrity.current().signingKey();
		require(!key.isBuiltin(), "the fixture key was not configured");
		Files.writeString(Path.of("fingerprint.txt"), key.fingerprint());

		byte[] export;
		try (var in = new CompetitionDataV2().exportData(AuditActor.system(), ExportAudit.CHANNEL_DOWNLOAD)) {
			export = in.readAllBytes();
		}
		Result result = ExportSignature.verify(export);
		require(result.status() == Status.VALID, "the export is " + result.status());
		require(ExportSignature.match(result, key) == KeyMatch.OWN, "the export is not signed with the server key");

		// still valid JSON, but no longer the signed bytes
		byte[] edited = export.clone();
		int indent = new String(edited, StandardCharsets.ISO_8859_1).indexOf("\n ") + 1;
		edited[indent] = '\t';
		require(ExportSignature.verify(edited).status() == Status.INVALID, "the edited copy still verifies");
		FormatDetector.importData(edited, "edited.json", Principal.admin(AuthSource.ACCOUNT),
				AuditOrigin.system("fixture"));
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}
}
