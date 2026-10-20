/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.nui.home.ReleaseNotice;

public class ReleaseNoticeTest {

	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	@Test
	public void kindFollowsVersionSuffix() {
		assertEquals("snapshot", ReleaseNotice.kind("69.0.0-SNAPSHOT"));
		assertEquals("alpha", ReleaseNotice.kind("69.0.0-alpha00"));
		assertEquals("beta", ReleaseNotice.kind("69.0.0-beta01"));
		assertEquals("rc", ReleaseNotice.kind("69.0.0-rc02"));
		assertEquals("release", ReleaseNotice.kind("69.0.0"));
		assertEquals("release", ReleaseNotice.kind(null));
	}

	@Test
	public void translationKeyFollowsKind() {
		assertEquals("ReleaseNotice.alpha", ReleaseNotice.translationKey("69.0.0-alpha00"));
		assertEquals("ReleaseNotice.snapshot", ReleaseNotice.translationKey("69.0.0-SNAPSHOT"));
	}

	@Test
	public void releasesHaveNoNotice() {
		assertNull(ReleaseNotice.read("69.0.0", Locale.ENGLISH));
		assertNull(ReleaseNotice.read("69.0.0", Locale.forLanguageTag("es-ES")));
	}
}
