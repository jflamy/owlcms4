package app.owlcms.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.component.UI;

import app.owlcms.access.Principal;
import app.owlcms.apputils.AccessUtils;
import app.owlcms.init.OwlcmsSession;

public record AuditActor(String mode, String user, String station, Integer index, boolean inferred, String client,
		String device) {
	public static AuditActor system() {
		return new AuditActor("PIN", "-", "SYSTEM", null, false, "-", "-");
	}

	public static AuditActor capture(Object origin) {
		Principal principal = currentPrincipal();
		return new AuditActor(modeOf(principal), userOf(principal), StationResolver.resolve(origin), null, true,
				captureClient(), "-");
	}

	/** The person logging in or out, as opposed to a station acting on the competition. */
	public static AuditActor login(String mode, String user) {
		return new AuditActor(mode, user, "LOGIN", null, false, captureClient(), "-");
	}

	public static AuditActor fromCurrentUi() {
		UI ui = UI.getCurrent();
		if (ui == null) {
			return null;
		}
		Object target = ui.getInternals().getActiveRouterTargetsChain().stream().findFirst().orElse(null);
		return capture(target);
	}

	public static AuditActor device(String station, Integer index, String device) {
		return new AuditActor("PIN", "-", station, index, false, "-", device);
	}

	public String displayStation() {
		return this.index == null ? this.station : this.station + "#" + this.index;
	}

	public AuditActor atStation(String station, Integer index, boolean inferred) {
		return new AuditActor(this.mode, this.user, station, index, inferred, this.client, this.device);
	}

	private static Principal currentPrincipal() {
		try {
			return OwlcmsSession.getPrincipal();
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static String modeOf(Principal principal) {
		return principal != null && principal.source() == Principal.AuthSource.ACCOUNT ? "ACCOUNTS" : "PIN";
	}

	private static String userOf(Principal principal) {
		return principal != null && principal.username() != null ? principal.username() : "-";
	}

	private static String captureClient() {
		try {
			VaadinSession session = VaadinSession.getCurrent();
			if (session == null) {
				return "-";
			}
			String id = session.getSession().getId();
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(id.getBytes(StandardCharsets.UTF_8));
			return AccessUtils.getClientIp() + "#" + String.format("%02x%02x", digest[0], digest[1]);
		} catch (NoSuchAlgorithmException | RuntimeException e) {
			return "-";
		}
	}
}