package app.owlcms.access;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import app.owlcms.data.config.FeatureSwitch;

/**
 * The access rule declared on a page class by one of the access annotations.
 *
 * @param tokenAccessible the page is a display output page that a token may open; the roles still decide
 */
public record PageRule(Kind kind, Set<Role> roles, boolean platformBound, FeatureSwitch feature,
        boolean tokenAccessible) {

	public enum Kind {
		/** No annotation: deny by default. */
		NONE, PUBLIC, AUTHENTICATED, ROLES
	}

	private static final Map<Class<?>, PageRule> CACHE = new ConcurrentHashMap<>();

	public PageRule(Kind kind, Set<Role> roles, boolean platformBound, FeatureSwitch feature) {
		this(kind, roles, platformBound, feature, false);
	}

	public static PageRule of(Class<?> pageClass) {
		return CACHE.computeIfAbsent(pageClass, PageRule::parse);
	}

	public static PageRule roles(Set<Role> roles, boolean platformBound) {
		return new PageRule(Kind.ROLES, roles, platformBound, null, false);
	}

	/** A role rule for a token-accessible page; for tests. */
	public static PageRule tokenRoles(Set<Role> roles, boolean platformBound) {
		return new PageRule(Kind.ROLES, roles, platformBound, null, true);
	}

	/** Pages that show scoreboards accept display credentials. */
	public boolean isDisplayPage() {
		return this.kind == Kind.ROLES && this.roles.contains(Role.DISPLAYS);
	}

	private static PageRule parse(Class<?> pageClass) {
		FeatureSwitch feature = pageClass.isAnnotationPresent(RequiresFeature.class)
		        ? pageClass.getAnnotation(RequiresFeature.class).value()
		        : null;
		if (pageClass.isAnnotationPresent(PublicPage.class)) {
			return new PageRule(Kind.PUBLIC, Set.of(), false, feature);
		}
		if (pageClass.isAnnotationPresent(AuthenticatedPage.class)) {
			return new PageRule(Kind.AUTHENTICATED, Set.of(), false, feature);
		}
		RequiresRole required = pageClass.getAnnotation(RequiresRole.class);
		if (required != null) {
			Set<Role> roles = EnumSet.noneOf(Role.class);
			roles.addAll(Arrays.asList(required.value()));
			return new PageRule(Kind.ROLES, roles, required.platformBound(), feature,
			        pageClass.isAnnotationPresent(TokenAccessible.class));
		}
		return new PageRule(Kind.NONE, Set.of(), false, feature);
	}
}
