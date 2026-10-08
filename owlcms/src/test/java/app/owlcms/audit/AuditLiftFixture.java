package app.owlcms.audit;

import java.nio.file.Path;

import app.owlcms.Main;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.tests.TwoMinutesRuleTest;
import ch.qos.logback.classic.Logger;

/**
 * Separate JVM: in-memory database and a mock field of play (synchronous timers, no Vaadin UI) with sealed
 * audit logging enabled. Runs the existing TwoMinutesRuleTest sequence 3 so the lift-save boundary can be checked in the written file.
 */
public final class AuditLiftFixture {
	private AuditLiftFixture() {
	}

	public static void main(String[] args) {
		try {
			runLift();
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}

	private static void runLift() throws Exception {
		System.setProperty("user.home", Path.of("home").toAbsolutePath().toString());
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		Config.getCurrent().setFeatureSwitchValue(FeatureSwitch.IWF_COMPLIANCE, true);
		AuditIntegrity.initialize(Config.getCurrent());

		TwoMinutesRuleTest scenario = new TwoMinutesRuleTest();
		scenario.setupTest();
		FieldOfPlay fop = OwlcmsSession.getFop();
		scenario.doSequence3(fop, fop.getFopEventBus(), (Logger) fop.getLogger());
	}
}
