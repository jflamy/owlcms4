package app.owlcms.data.athlete;

import java.util.Set;

import app.owlcms.access.AccessPolicy;
import app.owlcms.access.Capability;
import app.owlcms.access.PageRule;
import app.owlcms.access.Permission;
import app.owlcms.access.Principal;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;

/** Action authorization with an explicitly captured identity; no UI/session lookup. */
public final class AthleteEditAccess {
	private AthleteEditAccess() {
	}

	public static boolean metadata(Principal principal) {
		return AccessPolicy.hasCapability(principal, Capability.METADATA_EDITING, null);
	}

	public static boolean registration(Principal principal, boolean weighInPage, boolean resultsEntry) {
		if (resultsEntry) {
			return AccessPolicy.canOpen(principal, PageRule.permissions(Set.of(Permission.PREPARATION), false), null);
		}
		return AccessPolicy.canOpen(principal,
		        PageRule.permissions(Set.of(weighInPage ? Permission.WEIGHIN : Permission.REGISTRATION), false), null);
	}

	public static boolean card(Principal principal, Athlete stored) {
		FieldOfPlay fop = OwlcmsFactory.getFOPWithGroup(stored.getGroup());
		String platform = fop != null ? fop.getName()
		        : stored.getGroup() != null && stored.getGroup().getPlatform() != null
		                ? stored.getGroup().getPlatform().getName() : null;
		if (platform == null) {
			// no platform is identifiable: only global roles (preparation, supervisor, admin) may edit
			return principal != null && principal.grants().stream().anyMatch(
			        g -> !g.getRole().isPlatformScopable()
			                && g.getRole().permissions().contains(Permission.ATHLETE_CARD_EDITING));
		}
		return AccessPolicy.hasCapability(principal, Capability.ATHLETE_CARD_EDITING, platform);
	}

	public static void requireMetadata(Principal principal) {
		if (!metadata(principal)) {
			throw new SecurityException("Athlete metadata editing is not permitted");
		}
	}

	public static void requireCard(Principal principal, Athlete stored) {
		if (!card(principal, stored)) {
			throw new SecurityException("Athlete card editing is not permitted");
		}
	}
}
