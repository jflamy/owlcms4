package app.owlcms.audit;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import app.owlcms.access.HasPrincipal;
import app.owlcms.access.Principal;
import app.owlcms.access.Principal.AuthSource;

public class AuditActorTest {

	@Test
	public void captureAttributesToThePrincipalOfTheOriginPage() {
		Principal tokenUser = new Principal(AuthSource.TOKEN, 7L, "speaker", List.of(), null, "t1");

		AuditActor actor = AuditActor.capture(new Wrapper(new Page(tokenUser)));

		assertEquals("speaker", actor.user());
		assertEquals("ACCOUNTS", actor.mode());
		assertEquals("token id=t1", actor.device());
	}

	@Test
	public void injectedPrincipalDoesNotNeedAPageOrigin() {
		Principal user = new Principal(AuthSource.TOKEN, 7L, "announcer", List.of(), null, "t1");

		for (Object origin : Arrays.asList(null, new Object())) {
			AuditActor actor = AuditActor.capture(user, origin);
			assertEquals("announcer", actor.user());
			assertEquals("ACCOUNTS", actor.mode());
			assertEquals("token id=t1", actor.device());
		}
	}

	@Test
	public void injectedActorTakesPrecedenceOverTheNotificationOrigin() {
		Principal announcer = new Principal(AuthSource.ACCOUNT, 7L, "announcer", List.of(), null);
		Principal notificationUser = new Principal(AuthSource.TOKEN, 8L, "jury", List.of(), null, "jury-token");

		AuditActor actor = AuditActor.capture(announcer, new Wrapper(new Page(notificationUser)));

		assertEquals("announcer", actor.user());
		assertEquals("ACCOUNTS", actor.mode());
		assertEquals("-", actor.device());
	}

	public static class Page implements HasPrincipal {
		private final Principal principal;

		Page(Principal principal) {
			this.principal = principal;
		}

		@Override
		public Principal getPrincipal() {
			return this.principal;
		}
	}

	/** Stands for a timer or dialog that records the page it was triggered from. */
	public static class Wrapper {
		private final Object origin;

		Wrapper(Object origin) {
			this.origin = origin;
		}

		public Object getOrigin() {
			return this.origin;
		}
	}
}
