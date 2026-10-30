package app.owlcms.access;

import java.util.function.Function;

import org.slf4j.LoggerFactory;

import app.owlcms.audit.AuthAudit;
import app.owlcms.data.config.FeatureSwitch;
import ch.qos.logback.classic.Logger;

/**
 * The cp challenge: before an action that needs locality, the operator pastes the control panel token. Retrieving the
 * token requires access to the OWLCMS server (see the threat model in 130). Same rule as {@link SameMachineProof}:
 * when OWLCMS has a token it must match; only an OWLCMS started without one accepts a direct local connection.
 */
public final class ControlPanelChallenge {

	/** @param blockedSeconds when refused because of earlier failures, else 0 */
	public record Result(boolean accepted, long blockedSeconds) {
	}

	/** Throttling key: challenge attempts are counted apart from logins. */
	static final String THROTTLE_USER = "control-panel-challenge";

	private static final Logger logger = (Logger) LoggerFactory.getLogger(ControlPanelChallenge.class);

	private ControlPanelChallenge() {
	}

	/**
	 * @param presented  the token pasted by the operator
	 * @param remoteAddr the socket peer of the request, not a forwarded address
	 * @param header     looks up a request header, null if absent
	 * @param what       the protected action, for the log and the audit trail
	 */
	public static Result check(String presented, String remoteAddr, Function<String, String> header, String what) {
		return check(presented, remoteAddr, header, what, SameMachineProof.configuredToken(),
		        FeatureSwitch.IWF_COMPLIANCE.isEffective(), LoginThrottle.shared());
	}

	static Result check(String presented, String remoteAddr, Function<String, String> header, String what,
	        String expected, boolean iwfCompliance, LoginThrottle throttle) {
		long blocked = throttle.blockedSeconds(THROTTLE_USER, remoteAddr);
		if (blocked > 0) {
			logger./**/warn("control panel challenge throttled for {} ip={} seconds={}", what, remoteAddr, blocked);
			return new Result(false, blocked);
		}
		String refusal = SameMachineProof.refusal(expected, presented, remoteAddr, header, iwfCompliance);
		if (refusal == null) {
			throttle.success(THROTTLE_USER, remoteAddr);
			logger.info("control panel challenge passed for {} ip={}", what, remoteAddr);
			return new Result(true, 0);
		}
		throttle.fail(THROTTLE_USER, remoteAddr);
		logger./**/warn("control panel challenge refused for {} ip={} reason={}", what, remoteAddr, refusal);
		AuthAudit.tokenRefused(remoteAddr, what, refusal, null, null);
		return new Result(false, 0);
	}
}
