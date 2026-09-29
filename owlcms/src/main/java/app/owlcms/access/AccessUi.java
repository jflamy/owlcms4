package app.owlcms.access;

import java.util.List;
import java.util.Set;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;

/** Vaadin-side adapter for applying the pure access policy to FOP selectors. */
public final class AccessUi {

	private AccessUi() {
	}

	public static List<FieldOfPlay> selectableFops(Set<Role> pageRoles) {
		Principal principal = OwlcmsSession.getPrincipal();
		List<FieldOfPlay> allFops = List.copyOf(OwlcmsFactory.getFOPs());
		List<String> names = allFops.stream().map(FieldOfPlay::getName).toList();
		Set<String> allowed = Set.copyOf(AccessPolicy.selectablePlatforms(principal, pageRoles, names));
		return allFops.stream().filter(fop -> allowed.contains(fop.getName())).toList();
	}

	public static boolean fopSelectorReadOnly(Set<Role> pageRoles, List<FieldOfPlay> fops) {
		Principal principal = OwlcmsSession.getPrincipal();
		boolean lockablePage = pageRoles.stream().anyMatch(Role::isLockable);
		return fops.size() <= 1 || (AccessPolicy.isLocked(principal) && lockablePage);
	}

	public static boolean canOpen(Class<?> target, FieldOfPlay fop) {
		return AccessPolicy.canOpen(OwlcmsSession.getPrincipal(), PageRule.of(target),
		        fop != null ? fop.getName() : null);
	}
}