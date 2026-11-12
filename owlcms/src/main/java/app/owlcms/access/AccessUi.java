package app.owlcms.access;

import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.UI;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;

/** Vaadin-side adapter for applying the pure access policy to FOP selectors. */
public final class AccessUi {

	private AccessUi() {
	}

	/**
	 * UI thread only. The identity of the current tab: its token identity if the tab was opened with a token, else the
	 * session's login. A token tab and a password tab in the same browser each see their own identity.
	 */
	public static Principal currentPrincipal() {
		TokenTab tab = TokenTab.of(UI.getCurrent());
		return tab != null ? tab.principal() : OwlcmsSession.getPrincipal();
	}

	public static List<FieldOfPlay> selectableFops(Set<Access> pageAccess) {
		Principal principal = currentPrincipal();
		List<FieldOfPlay> allFops = List.copyOf(OwlcmsFactory.getFOPs());
		List<String> names = allFops.stream().map(FieldOfPlay::getName).toList();
		Set<String> allowed = Set.copyOf(AccessPolicy.selectablePlatforms(principal, pageAccess, names));
		return allFops.stream().filter(fop -> allowed.contains(fop.getName())).toList();
	}

	public static boolean fopSelectorReadOnly(Set<Access> pageAccess, List<FieldOfPlay> fops) {
		Principal principal = currentPrincipal();
		return fops.size() <= 1 || AccessPolicy.isLockedFor(principal, pageAccess);
	}

	public static boolean canOpen(Class<?> target, FieldOfPlay fop) {
		return canOpenOnPlatform(target, fop != null ? fop.getName() : null);
	}

	/** UI thread only: whether the current tab has the in-page function. */
	public static boolean hasPermission(Permission permission, FieldOfPlay fop) {
		return AccessPolicy.hasPermission(currentPrincipal(), permission, fop != null ? fop.getName() : null);
	}

	/** UI thread only: whether the current tab may open {@code target} for the named platform. */
	public static boolean canOpenOnPlatform(Class<?> target, String platformName) {
		return AccessPolicy.canOpen(currentPrincipal(), PageRule.of(target), platformName);
	}
}