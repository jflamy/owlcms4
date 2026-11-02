package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Simple Access mode: the guarded servlets take the officials PIN when one is set, else the local network. */
public class ServletTokenGuardTest {

	private static final AuditOrigin ORIGIN = AuditOrigin.system("test");
	private static final String PIN = "4821";
	private static final String LOCAL = "192.168.1.20";
	private static final String REMOTE = "203.0.113.7";

	@BeforeClass
	public static void setUpDatabase() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void closeDatabase() {
		setPin(null);
		JPAService.close();
	}

	@Before
	public void pinMode() {
		Config config = Config.getCurrent();
		config.setIwfCompliance(false);
		config.setAccessMode(AccessMode.PIN);
		Config.setCurrentTesting(config, ORIGIN);
	}

	private static void setPin(String pin) {
		Config config = Config.getCurrent();
		config.setPin(pin == null ? null : PinHashes.hash(pin));
		Config.setCurrentTesting(config, ORIGIN);
	}

	private static final class Response {
		int status;
		boolean authorized;
		final Map<String, String> headers = new HashMap<>();
		final HttpServletResponse proxy = (HttpServletResponse) Proxy.newProxyInstance(
		        HttpServletResponse.class.getClassLoader(), new Class<?>[] { HttpServletResponse.class },
		        (p, method, args) -> {
			        switch (method.getName()) {
				        case "setStatus" -> this.status = (Integer) args[0];
				        case "setHeader" -> this.headers.put((String) args[0], (String) args[1]);
				        default -> {
				        }
			        }
			        return null;
		        });
	}

	private static HttpServletRequest request(String tokenParameter, String authorization) {
		return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
		        new Class<?>[] { HttpServletRequest.class }, (p, method, args) -> switch (method.getName()) {
			        case "getServletPath" -> "/competition/checkpoint";
			        case "getParameter" -> "token".equals(args[0]) ? tokenParameter : null;
			        case "getHeader" -> "Authorization".equals(args[0]) ? authorization : null;
			        default -> null;
		        });
	}

	private static Response authorize(HttpServletRequest request, String host) {
		Response response = new Response();
		response.authorized = ServletTokenGuard.authorize(request, response.proxy, host,
		        Capability.CHECKPOINT, "checkpoint") != null;
		return response;
	}

	@Test
	public void withAnOfficialsPinTheLocalNetworkIsNotEnough() {
		setPin(PIN);
		Response response = authorize(request(null, null), LOCAL);
		assertFalse(response.authorized);
		assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status);
		assertEquals("Bearer", response.headers.get("WWW-Authenticate"));
	}

	@Test
	public void theOfficialsPinIsAcceptedAsParameterOrBearerFromAnyAddress() {
		setPin(PIN);
		Response parameter = authorize(request(PIN, null), REMOTE);
		assertTrue(parameter.authorized);
		assertEquals(0, parameter.status);
		Response bearer = authorize(request(null, "Bearer " + PIN), LOCAL);
		assertTrue(bearer.authorized);
		assertEquals(0, bearer.status);
	}

	@Test
	public void aWrongPinIsRefused() {
		setPin(PIN);
		Response response = authorize(request("1111", null), LOCAL);
		assertFalse(response.authorized);
		assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status);
		// a correct PIN clears the shared officials PIN failure count for the other tests
		assertTrue(authorize(request(PIN, null), LOCAL).authorized);
	}

	@Test
	public void withoutAnOfficialsPinOnlyTheLocalNetworkIsAllowed() {
		setPin(null);
		assertTrue(authorize(request(null, null), LOCAL).authorized);
		Response remote = authorize(request(null, null), REMOTE);
		assertFalse(remote.authorized);
		assertEquals(HttpServletResponse.SC_FORBIDDEN, remote.status);
	}
}
