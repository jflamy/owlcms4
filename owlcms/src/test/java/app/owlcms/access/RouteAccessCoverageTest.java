package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Test;

import com.vaadin.flow.router.Route;

import app.owlcms.access.Principal.AuthSource;
import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.nui.lifting.AnnouncerContent;
import app.owlcms.nui.lifting.CompetitionDirectorContent;
import app.owlcms.nui.lifting.PassiveAnnouncerContent;

/**
 * Every {@code @Route} class must declare exactly one access rule. The classes are loaded without initialization, so no
 * Vaadin object is created.
 */
public class RouteAccessCoverageTest {

	@Test
	public void everyRouteDeclaresExactlyOneAccessRule() throws Exception {
		List<String> problems = new ArrayList<>();
		List<String> matrix = new ArrayList<>();
		int routes = 0;
		for (Class<?> pageClass : routeClasses()) {
			routes++;
			int declared = (pageClass.isAnnotationPresent(PublicPage.class) ? 1 : 0)
			        + (pageClass.isAnnotationPresent(AuthenticatedPage.class) ? 1 : 0)
			        + (pageClass.isAnnotationPresent(RequiresRole.class) ? 1 : 0);
			if (declared != 1) {
				problems.add(pageClass.getName() + " declares " + declared + " access annotations");
			}
			PageRule rule = PageRule.of(pageClass);
			matrix.add(String.format("%-70s %-13s %-5s %s", pageClass.getSimpleName(), rule.kind(),
			        rule.platformBound() ? "bound" : "", rule.roles()));
		}
		matrix.sort(null);
		System.out.println("ROUTE ACCESS MATRIX (" + routes + " routes)");
		matrix.forEach(System.out::println);
		assertTrue("at least the known routes must be found, found " + routes, routes >= 70);
		assertEquals(problems.toString(), 0, problems.size());
	}

	@Test
	public void pinModeDisplayPrincipalOpensOnlyDisplayPagesAndPublicOnes() throws Exception {
		Principal displays = Principal.displays();
		Principal officials = Principal.admin(AuthSource.OFFICIALS_PIN);
		List<String> problems = new ArrayList<>();
		for (Class<?> pageClass : routeClasses()) {
			PageRule rule = PageRule.of(pageClass);
			if (rule.kind() == PageRule.Kind.NONE) {
				continue;
			}
			if (rule.kind() == PageRule.Kind.ROLES && !AccessPolicy.canOpen(officials, rule, "A")) {
				problems.add("officials cannot open " + pageClass.getSimpleName());
			}
			boolean expected = rule.kind() != PageRule.Kind.ROLES || rule.isDisplayPage();
			boolean actual = AccessPolicy.canOpen(displays, rule, "A");
			if (rule.kind() != PageRule.Kind.AUTHENTICATED && expected != actual) {
				problems.add("display principal: " + pageClass.getSimpleName() + " expected " + expected);
			}
		}
		assertEquals(problems.toString(), 0, problems.size());
	}

	@Test
	public void directorAndSpeakerRoutesKeepSeparateContracts() {
		assertEquals(AnnouncerContent.class, CompetitionDirectorContent.class.getSuperclass());
		assertEquals(AnnouncerContent.class, PassiveAnnouncerContent.class.getSuperclass());
		assertEquals(FeatureSwitch.COMPETITION_DIRECTOR_PAGE, PageRule.of(CompetitionDirectorContent.class).feature());
		assertNull(PageRule.of(AnnouncerContent.class).feature());
		assertNull(PageRule.of(PassiveAnnouncerContent.class).feature());
		Principal speakerA = new Principal(AuthSource.ACCOUNT, 1L, "speaker",
		        List.of(new RoleGrant(Role.ANNOUNCER, "A")), "A");
		for (Class<?> page : List.of(AnnouncerContent.class, PassiveAnnouncerContent.class)) {
			PageRule rule = PageRule.of(page);
			assertEquals(Set.of(Role.ANNOUNCER), rule.roles());
			assertTrue(rule.platformBound());
			assertTrue(AccessPolicy.canOpen(speakerA, rule, "A"));
			assertFalse(AccessPolicy.canOpen(speakerA, rule, "B"));
			assertFalse(AccessPolicy.canOpen(Principal.displays(), rule, "A"));
		}
		PageRule directorRule = PageRule.of(CompetitionDirectorContent.class);
		assertEquals(Set.of(Role.COMPETITION_DIRECTOR), directorRule.roles());
		assertTrue(directorRule.platformBound());
		assertFalse(AccessPolicy.canOpen(speakerA, directorRule, "A"));
		Principal director = new Principal(AuthSource.ACCOUNT, 2L, "director",
		        List.of(new RoleGrant(Role.COMPETITION_DIRECTOR, null)), null);
		assertTrue(AccessPolicy.canOpen(director, directorRule, "B"));
		assertTrue(AccessPolicy.canOpen(Principal.admin(AuthSource.ACCOUNT), directorRule, "A"));
	}

	private static List<Class<?>> routeClasses() throws IOException, URISyntaxException {
		Path classes = Path.of(AccessPolicy.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		Path root = classes.resolve("app/owlcms");
		List<Class<?>> result = new ArrayList<>();
		try (Stream<Path> files = Files.walk(root)) {
			for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".class"))::iterator) {
				String name = classes.relativize(file).toString().replace('/', '.').replace('\\', '.');
				name = name.substring(0, name.length() - ".class".length());
				try {
					Class<?> candidate = Class.forName(name, false, RouteAccessCoverageTest.class.getClassLoader());
					if (candidate.isAnnotationPresent(Route.class)) {
						result.add(candidate);
					}
				} catch (LinkageError | ClassNotFoundException e) {
					// classes that cannot be linked in a test JVM cannot be routes we need to check
				}
			}
		}
		return result;
	}
}
