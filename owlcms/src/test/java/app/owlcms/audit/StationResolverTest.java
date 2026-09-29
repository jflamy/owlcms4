package app.owlcms.audit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StationResolverTest {
	@Test
	public void adminViewResolvesToAdminStation() {
		assertEquals("ADMIN", StationResolver.resolve(new AdminView()));
	}

	private static class AdminView {
	}
}