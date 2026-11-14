package app.owlcms.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.access.HasPrincipal;
import app.owlcms.access.Principal;
import app.owlcms.apputils.AccessUtils;

public record AuditActor(String mode, String user, String station, Integer index, boolean inferred, String client,
		String device) {
	public static AuditActor system() {
		return new AuditActor("PIN", "-", "SYSTEM", null, false, "-", "-");
	}

	/**
	 * The user is the principal captured by the page the action comes from (see {@link HasPrincipal}), never the
	 * session's: events are often built on event-bus threads. A tab opened with a token is attributed to the token's
	 * owner, not to the browser's password login.
	 */
	public static AuditActor capture(Object origin) {
		return capture(principalOf(origin), origin);
	}

	/** Capture at the UI edge: the injected principal supplies who; the audit origin supplies the station. */
	public static AuditActor capture(Principal principal, Object origin) {
		String device = principal != null && principal.tokenId() != null ? "token id=" + principal.tokenId() : "-";
		return new AuditActor(modeOf(principal), userOf(principal), StationResolver.resolve(origin), null, true,
				captureClient(), device);
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

	/** An unauthenticated servlet request from the local network or backdoor list. */
	public static AuditActor http(String clientIp) {
		return new AuditActor("-", "-", "HTTP", null, false, clientIp, "-");
	}

	/** A servlet request authenticated by an account token; the token identifier is not secret. */
	public static AuditActor token(String username, String tokenId, String clientIp) {
		return new AuditActor("ACCOUNTS", username, "HTTP", null, false, clientIp, "token id=" + tokenId);
	}

	public String displayStation() {
		return this.index == null ? this.station : this.station + "#" + this.index;
	}

	public AuditActor atStation(String station, Integer index, boolean inferred) {
		return new AuditActor(this.mode, this.user, station, index, inferred, this.client, this.device);
	}

	/**
	 * The first {@link HasPrincipal} along the origin chain; failing that, the page displayed in the UI of a component
	 * origin, such as a dialog.
	 */
	static Principal principalOf(Object origin) {
		Object current = origin;
		for (int depth = 0; depth < 5 && current != null; depth++) {
			if (current instanceof HasPrincipal page) {
				return page.getPrincipal();
			}
			Object next = StationResolver.next(current);
			if (next == null) {
				break;
			}
			current = next;
		}
		if (current instanceof Component component) {
			return component.getUI()
			        .flatMap(ui -> ui.getInternals().getActiveRouterTargetsChain().stream()
			                .filter(HasPrincipal.class::isInstance)
			                .map(HasPrincipal.class::cast)
			                .findFirst())
			        .map(HasPrincipal::getPrincipal)
			        .orElse(null);
		}
		return null;
	}

	private static String modeOf(Principal principal) {
		return principal != null && (principal.source() == Principal.AuthSource.ACCOUNT
		        || principal.source() == Principal.AuthSource.TOKEN) ? "ACCOUNTS" : "PIN";
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