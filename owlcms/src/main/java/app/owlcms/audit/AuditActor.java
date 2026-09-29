package app.owlcms.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.component.UI;

import app.owlcms.apputils.AccessUtils;

public record AuditActor(String mode, String user, String station, Integer index, boolean inferred, String client,
		String device) {
	public static AuditActor system() {
		return new AuditActor("PIN", "-", "SYSTEM", null, false, "-", "-");
	}

	public static AuditActor capture(Object origin) {
		return new AuditActor("PIN", "-", StationResolver.resolve(origin), null, true, captureClient(), "-");
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