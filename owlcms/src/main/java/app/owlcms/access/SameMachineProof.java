package app.owlcms.access;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.utils.StartupUtils;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Proof that a request comes from someone in control of the OWLCMS server: the control panel token. The client address
 * is never one: a local reverse proxy such as Caddy makes remote requests appear to come from localhost, and the
 * backdoor list never replaces the proof.
 * <p>
 * The control panel gives OWLCMS a token at launch ({@code OWLCMS_CONTROLPANELTOKEN}), offers to copy it and sends it on
 * stop; in the cloud it is a secret. The request must present it, from any address. An OWLCMS started without a token
 * refuses every such request.
 * <p>
 * The token is also the emergency admin password: see {@link AccountPasswords}.
 */
public final class SameMachineProof {

	/** OWLCMS parameter, {@code OWLCMS_CONTROLPANELTOKEN} in the environment. */
	public static final String CONTROL_PANEL_TOKEN = "controlPanelToken";

	/** Refusal reason when OWLCMS was started without a control panel token. */
	public static final String NO_TOKEN = "no-token";

	private SameMachineProof() {
	}

	/** @return null if the request is allowed, else the refusal reason for the audit */
	public static String refusal(HttpServletRequest request) {
		String presented = TokenAuthenticator.presentedToken(request.getParameter("token"),
		        request.getHeader("Authorization"));
		return refusal(configuredToken(), presented);
	}

	/** @return the control panel token OWLCMS was started with, or null */
	public static String configuredToken() {
		String token = StartupUtils.getStringParam(CONTROL_PANEL_TOKEN);
		return token == null || token.isBlank() ? null : token;
	}

	/**
	 * @param expected  the control panel token given at launch, or null
	 * @param presented the token in the request, or null
	 * @return null if allowed, else {@link #NO_TOKEN}, "missing" or "invalid"
	 */
	static String refusal(String expected, String presented) {
		if (expected == null || expected.isBlank()) {
			return NO_TOKEN;
		}
		if (presented == null || presented.isBlank()) {
			return "missing";
		}
		return matches(presented, expected) ? null : "invalid";
	}

	/** Compares in constant time; false if either value is missing. */
	public static boolean matches(String presented, String expected) {
		if (presented == null || presented.isBlank() || expected == null || expected.isBlank()) {
			return false;
		}
		return AccessUtils.constantTimeEquals(expected.strip(), presented.strip());
	}
}
