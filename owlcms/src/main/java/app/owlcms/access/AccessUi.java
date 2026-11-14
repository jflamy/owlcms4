package app.owlcms.access;

import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.UI;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;

/**
 * Vaadin-side adapter for applying the pure access policy. Every check takes the principal explicitly: pages capture it
 * once with {@link #currentPrincipal()} when they are created, and pass it down (see {@link HasPrincipal}).
 */
public final class AccessUi {

	private AccessUi() {
	}

	/**
	 * Capture point, for page creation during navigation only (UI request thread). The identity of the current tab: its
	 * token identity if the tab was opened with a token, else the session's login. A token tab and a password tab in the
	 * same browser each see their own identity. Do not call from event handlers or background work, where the session
	 * lookup does not designate the page's user; use the page's captured principal.
	 */
	public static Principal currentPrincipal() {
		TokenTab tab = TokenTab.of(UI.getCurrent());
		return tab != null ? tab.principal() : OwlcmsSession.getPrincipal();
	}

	public static List<FieldOfPlay> selectableFops(Principal principal, Set<Access> pageAccess) {
		List<FieldOfPlay> allFops = List.copyOf(OwlcmsFactory.getFOPs());
		List<String> names = allFops.stream().map(FieldOfPlay::getName).toList();
		Set<String> allowed = Set.copyOf(AccessPolicy.selectablePlatforms(principal, pageAccess, names));
		return allFops.stream().filter(fop -> allowed.contains(fop.getName())).toList();
	}

	public static boolean fopSelectorReadOnly(Principal principal, Set<Access> pageAccess, List<FieldOfPlay> fops) {
		return fops.size() <= 1 || AccessPolicy.isLockedFor(principal, pageAccess);
	}

	/** Whether {@code principal} has the in-page function. */
	public static boolean hasPermission(Principal principal, Permission permission, FieldOfPlay fop) {
		return AccessPolicy.hasPermission(principal, permission, fop != null ? fop.getName() : null);
	}

	/** Whether {@code principal} may open {@code target} for {@code fop}. */
	public static boolean canOpen(Principal principal, Class<?> target, FieldOfPlay fop) {
		return canOpenOnPlatform(principal, target, fop != null ? fop.getName() : null);
	}

	/** Whether {@code principal} may open {@code target} for the named platform. */
	public static boolean canOpenOnPlatform(Principal principal, Class<?> target, String platformName) {
		return AccessPolicy.canOpen(principal, PageRule.of(target), platformName);
	}
}
