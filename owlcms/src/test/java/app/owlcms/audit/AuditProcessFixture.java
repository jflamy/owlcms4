package app.owlcms.audit;

import java.nio.file.Path;
import java.util.List;

import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;

/** Isolated audit-only JVM: no server, Vaadin components, competition database or devices are started. */
public final class AuditProcessFixture {
	private AuditProcessFixture() {
	}

	public static void main(String[] args) throws Exception {
		System.setProperty("user.home", Path.of("home").toAbsolutePath().toString());
		Config config = new Config();
		if ("forced".equals(args[0])) {
			AuditKeyFiles.prepare(AuditKeyFiles.resolvePath(null, Path.of(System.getProperty("user.home"))));
		} else if (!"plain".equals(args[0])) {
			config.setFeatureSwitchValue(FeatureSwitch.IWF_COMPLIANCE, true);
		}
		AuditIntegrity.initialize(config);
		AuditLog.writeBatch(List.of(
				AuditEntry.builder("A", "athlete.change").actor(AuditActor.device("WEIGHIN", null, "-"))
						.field("bodyWeight").newValue(81.35).build(),
				AuditEntry.builder("A", "athlete.change").actor(AuditActor.device("WEIGHIN", null, "-"))
						.attempt("SN1").field("declaration").newValue(120).build()), true);
		AuditLog.write(AuditEntry.builder("A", "clock.start").actor(AuditActor.system()).build());
		if ("crash".equals(args[0])) {
			Runtime.getRuntime().halt(0);
		}
	}
}
