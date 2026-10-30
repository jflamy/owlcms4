package app.owlcms.access;

import org.slf4j.LoggerFactory;

import app.owlcms.audit.AuthAudit;
import ch.qos.logback.classic.Logger;

/**
 * The cp challenge: before an action that needs locality, the operator pastes the control panel token. Retrieving the
 * token requires access to the OWLCMS server (see the threat model in 130). Same rule as {@link SameMachineProof}: the
 * token must match, and an OWLCMS started without one refuses.
 */
public final class ControlPanelChallenge {

	/**
	 * @param blockedSeconds when refused because of earlier failures, else 0
	 * @param noToken        refused because OWLCMS was started without a control panel token
	 */
	public record Result(boolean accepted, long blockedSeconds, boolean noToken) {
	}

	/** Throttling key: challenge attempts are counted apart from logins. */
	static final String THROTTLE_USER = "control-panel-challenge";

	private static final Logger logger = (Logger) LoggerFactory.getLogger(ControlPanelChallenge.class);

	private ControlPanelChallenge() {
	}

	/**
	 * @param presented  the token pasted by the operator
	 * @param remoteAddr the client address, for throttling, the log and the audit trail
	 * @param what       the protected action, for the log and the audit trail
	 */
	public static Result check(String presented, String remoteAddr, String what) {
		return check(presented, remoteAddr, what, SameMachineProof.configuredToken(), LoginThrottle.shared());
	}

	static Result check(String presented, String remoteAddr, String what, String expected, LoginThrottle throttle) {
		long blocked = throttle.blockedSeconds(THROTTLE_USER, remoteAddr);
		if (blocked > 0) {
			logger./**/warn("control panel challenge throttled for {} ip={} seconds={}", what, remoteAddr, blocked);
			return new Result(false, blocked, false);
		}
		String refusal = SameMachineProof.refusal(expected, presented);
		if (refusal == null) {
			throttle.success(THROTTLE_USER, remoteAddr);
			logger.info("control panel challenge passed for {} ip={}", what, remoteAddr);
			return new Result(true, 0, false);
		}
		logger./**/warn("control panel challenge refused for {} ip={} reason={}", what, remoteAddr, refusal);
		AuthAudit.tokenRefused(remoteAddr, what, refusal, null, null);
		if (SameMachineProof.NO_TOKEN.equals(refusal)) {
			// nothing to guess: no token can be accepted
			return new Result(false, 0, true);
		}
		throttle.fail(THROTTLE_USER, remoteAddr);
		return new Result(false, 0, false);
	}
}
