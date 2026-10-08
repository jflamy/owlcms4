package app.owlcms.audit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		AuditContextTest.class,
		AuditFormatTest.class,
		AuditSigningKeyTest.class,
		AuditMarkerTest.class,
		AuditBlockChainTest.class,
		SealedAuditStreamTest.class,
		AuditFileStoreTest.class,
		AuditIntegrityCheckerTest.class,
		SealedAuditBackendTest.class,
		AuditLogPersistenceTest.class,
		AuditIntegrityTest.class,
		ExportAuditTest.class,
		AthleteDiffTest.class,
		RecordChallengeTrackerTest.class,
		StationResolverTest.class
})
public class AuditTests {
}