package app.owlcms.audit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		AuditContextTest.class,
		AuditFormatTest.class,
		ExportAuditTest.class,
		AthleteDiffTest.class,
		RecordChallengeTrackerTest.class,
		StationResolverTest.class
})
public class AuditTests {
}