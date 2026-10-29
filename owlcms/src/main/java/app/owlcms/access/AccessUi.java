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

	public static List<FieldOfPlay> selectableFops(Set<Role> pageRoles) {
		Principal principal = currentPrincipal();
		List<FieldOfPlay> allFops = List.copyOf(OwlcmsFactory.getFOPs());
		List<String> names = allFops.stream().map(FieldOfPlay::getName).toList();
		Set<String> allowed = Set.copyOf(AccessPolicy.selectablePlatforms(principal, pageRoles, names));
		return allFops.stream().filter(fop -> allowed.contains(fop.getName())).toList();
	}

	public static boolean fopSelectorReadOnly(Set<Role> pageRoles, List<FieldOfPlay> fops) {
		Principal principal = currentPrincipal();
		boolean lockablePage = pageRoles.stream().anyMatch(Role::isLockable);
		return fops.size() <= 1 || (AccessPolicy.isLocked(principal) && lockablePage);
	}

	/** UI thread only: uses the current tab's identity. */
	public static boolean displayInputAllowed(FieldOfPlay fop) {
		return AccessPolicy.displayInputAllowed(currentPrincipal(), fop != null ? fop.getName() : null);
	}

	public static boolean canOpen(Class<?> target, FieldOfPlay fop) {
		return AccessPolicy.canOpen(currentPrincipal(), PageRule.of(target),
		        fop != null ? fop.getName() : null);
	}
}