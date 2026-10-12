/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.jpa;

import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class H2UrlPolicyTest {

	private static final String JDBC_DATABASE_URL = "JDBC_DATABASE_URL";
	private String originalJdbcUrl;

	@Before
	public void captureJdbcUrl() {
		Assume.assumeTrue(System.getenv(JDBC_DATABASE_URL) == null);
		this.originalJdbcUrl = System.getProperty(JDBC_DATABASE_URL);
	}

	@After
	public void restoreJdbcUrl() {
		if (this.originalJdbcUrl == null) {
			System.clearProperty(JDBC_DATABASE_URL);
		} else {
			System.setProperty(JDBC_DATABASE_URL, this.originalJdbcUrl);
		}
	}

	@Test
	public void acceptsEmbeddedFileAndNamedMemoryUrls() {
		H2UrlPolicy.validate("jdbc:h2:file:./database/owlcms-h2v2");
		H2UrlPolicy.validate("jdbc:h2:file:C:\\owlcms\\database\\owlcms-h2v2;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=4");
		H2UrlPolicy.validate("jdbc:h2:mem:owlcms;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=4");
		H2UrlPolicy.validate("jdbc:h2:mem:test;db_close_delay=-1;TRACE_LEVEL_SYSTEM_OUT=0;");
	}

	@Test
	public void rejectsServerAndNonEmbeddedModes() {
		assertRejected("jdbc:h2:tcp://localhost:9092/./database/owlcms-h2v2");
		assertRejected("jdbc:h2:ssl://localhost:9092/./database/owlcms-h2v2");
		assertRejected("jdbc:h2:./database/owlcms-h2v2");
		assertRejected("jdbc:h2:zip:./database/owlcms.zip!/owlcms");
		assertRejected("JDBC:H2:FILE:./database/owlcms-h2v2");
		assertRejected("jdbc:h2:mem:");
		assertRejected("jdbc:h2:mem:;DB_CLOSE_DELAY=-1");
		assertRejected("jdbc:h2:file:");
	}

	@Test
	public void rejectsUnsafeAndUnsupportedOptions() {
		assertRejected("jdbc:h2:file:./db;AUTO_SERVER=TRUE");
		assertRejected("jdbc:h2:file:./db;auto_server=true");
		assertRejected("jdbc:h2:file:./db;FILE_LOCK=NO");
		assertRejected("jdbc:h2:file:./db;INIT=RUNSCRIPT FROM 'evil.sql'");
		assertRejected("jdbc:h2:mem:x;ACCESS_MODE_DATA=r");
		assertRejected("jdbc:h2:file:./db;DB_CLOSE_DELAY=-1;DB_CLOSE_DELAY=-1");
		assertRejected("jdbc:h2:file:./db;DB_CLOSE_DELAY=-1;db_close_delay=0");
		assertRejected("jdbc:h2:file:./db;DB_CLOSE_DELAY=-1\\;INIT=x");
		assertRejected("jdbc:h2:file:./db;DB_CLOSE_DELAY");
		assertRejected("jdbc:h2:file:./db;DB_CLOSE_DELAY=forever");
		assertRejected("jdbc:h2:file:./db;=1");
	}

	@Test
	public void defaultOptionsAreAddedOnlyWhenMissing() {
		assertEquals("jdbc:h2:file:./db;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=4",
		        H2UrlPolicy.withDefaultOptions("jdbc:h2:file:./db", "DB_CLOSE_DELAY=-1", "TRACE_LEVEL_FILE=4"));
		assertEquals("jdbc:h2:file:./db;TRACE_LEVEL_FILE=0;DB_CLOSE_DELAY=-1",
		        H2UrlPolicy.withDefaultOptions("jdbc:h2:file:./db;TRACE_LEVEL_FILE=0", "DB_CLOSE_DELAY=-1",
		                "TRACE_LEVEL_FILE=4"));
	}

	@Test
	public void processSettingsRejectsRemoteH2() {
		System.setProperty(JDBC_DATABASE_URL, "jdbc:h2:tcp://localhost:9092/./database/owlcms-h2v2");
		assertThrows(IllegalArgumentException.class, () -> JPAService.processSettings(false, false));
	}

	@Test
	public void processSettingsKeepsEmbeddedFileUrlWithoutDuplicateOptions() {
		System.setProperty(JDBC_DATABASE_URL, "jdbc:h2:file:./target/h2policy/owlcms.mv.db;DB_CLOSE_DELAY=-1");
		Properties properties = JPAService.processSettings(false, false);
		assertEquals("jdbc:h2:file:./target/h2policy/owlcms;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=4",
		        properties.getProperty(JPA_JDBC_URL));
	}

	@Test
	public void processSettingsAcceptsNamedMemoryUrl() {
		String url = "jdbc:h2:mem:h2PolicyTest;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=4";
		System.setProperty(JDBC_DATABASE_URL, url);
		Properties properties = JPAService.processSettings(false, false);
		assertEquals(url, properties.getProperty(JPA_JDBC_URL));
		assertTrue(JPAService.isLocalDbConfigured(false));
	}

	private static void assertRejected(String url) {
		assertThrows(url, IllegalArgumentException.class, () -> H2UrlPolicy.validate(url));
	}
}
