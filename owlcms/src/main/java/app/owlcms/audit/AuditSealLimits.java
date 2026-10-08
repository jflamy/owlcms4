package app.owlcms.audit;

import java.time.Duration;
import java.util.Objects;

public record AuditSealLimits(int blockBytes, int blockRecords, Duration blockAge,
		Duration idleTime, int queueBytes, long fileBytes) {
	public static final AuditSealLimits DEFAULT = new AuditSealLimits(1_048_576, 500,
			Duration.ofMinutes(5), Duration.ofMinutes(5), 8_388_608, 10_485_760L);

	public AuditSealLimits {
		Objects.requireNonNull(blockAge, "blockAge");
		Objects.requireNonNull(idleTime, "idleTime");
		if (blockBytes <= 0 || blockRecords <= 0 || queueBytes < blockBytes || fileBytes <= 0
				|| blockAge.isNegative() || blockAge.isZero() || idleTime.isNegative() || idleTime.isZero()) {
			throw new IllegalArgumentException("Audit sealing limits must be positive and fit the signing queue");
		}
	}
}
