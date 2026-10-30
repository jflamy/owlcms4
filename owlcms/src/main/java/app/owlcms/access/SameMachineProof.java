package app.owlcms.access;

import java.util.List;
import java.util.function.Function;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.utils.StartupUtils;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Proof that a maintenance request ({@code /controlpanel/stop}) comes from this
 * machine. The client address alone is not one: a local reverse proxy such as Caddy makes remote requests appear to
 * come from localhost. The backdoor list never replaces the proof.
 * <ul>
 * <li>The control panel gives OWLCMS a token at launch ({@code OWLCMS_CONTROLPANELTOKEN}), offers to copy it and
 * sends it on stop. Whenever OWLCMS has a token, the request must present it, from any address.</li>
 * <li>Only when OWLCMS was started without a token, a weaker check applies: a loopback connection that carries no
 * forwarding header. A proxy adds such a header (Caddy does by default), so a request it forwards is not taken as
 * local. In IWF compliance mode OWLCMS refuses to start without a token.</li>
 * </ul>
 * The token is also the emergency admin password: see {@link AccountPasswords}.
 */
public final class SameMachineProof {

	/** OWLCMS parameter, {@code OWLCMS_CONTROLPANELTOKEN} in the environment. */
	public static final String CONTROL_PANEL_TOKEN = "controlPanelToken";

	/** Headers a reverse proxy adds to the requests it forwards. */
	static final List<String> FORWARDING_HEADERS = List.of("X-Forwarded-For", "Forwarded", "X-Real-IP");

	private SameMachineProof() {
	}

	/** @return null if the request is allowed, else the refusal reason for the audit */
	public static String refusal(HttpServletRequest request) {
		String presented = TokenAuthenticator.presentedToken(request.getParameter("token"),
		        request.getHeader("Authorization"));
		return refusal(configuredToken(), presented, request.getRemoteAddr(),
		        request::getHeader, FeatureSwitch.IWF_COMPLIANCE.isEffective());
	}

	/** @return the control panel token OWLCMS was started with, or null */
	public static String configuredToken() {
		String token = StartupUtils.getStringParam(CONTROL_PANEL_TOKEN);
		return token == null || token.isBlank() ? null : token;
	}

	/**
	 * @param expected      the control panel token given at launch, or null
	 * @param presented     the token in the request, or null
	 * @param remoteAddr    the socket peer ({@code getRemoteAddr()}), not a forwarded address
	 * @param header        looks up a request header, null if absent
	 * @param iwfCompliance no fallback to a local connection
	 * @return null if allowed, else "missing", "invalid" or "not-local"
	 */
	static String refusal(String expected, String presented, String remoteAddr, Function<String, String> header,
	        boolean iwfCompliance) {
		boolean tokenPresented = presented != null && !presented.isBlank();
		if (expected != null && !expected.isBlank()) {
			if (!tokenPresented) {
				return "missing";
			}
			return matches(presented, expected) ? null : "invalid";
		}
		if (tokenPresented) {
			// OWLCMS has no token to compare with
			return "invalid";
		}
		if (iwfCompliance) {
			return "missing";
		}
		return isDirectLoopback(remoteAddr, header) ? null : "not-local";
	}

	/** Compares in constant time; false if either value is missing. */
	static boolean matches(String presented, String expected) {
		if (presented == null || presented.isBlank() || expected == null || expected.isBlank()) {
			return false;
		}
		return AccessUtils.constantTimeEquals(expected.strip(), presented.strip());
	}

	/** @return true if the connection is from loopback and no proxy forwarded it */
	static boolean isDirectLoopback(String remoteAddr, Function<String, String> header) {
		if (!AccessUtils.isLocalhost(remoteAddr)) {
			return false;
		}
		for (String name : FORWARDING_HEADERS) {
			String value = header.apply(name);
			if (value != null && !value.isBlank()) {
				return false;
			}
		}
		return true;
	}
}
