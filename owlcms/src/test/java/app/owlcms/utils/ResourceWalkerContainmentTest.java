/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Names given to the resource lookup (including URLs under /local/*) must stay inside the override directory or the
 * classpath.
 */
public class ResourceWalkerContainmentTest {

	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void resolveInsideKeepsOrdinaryNames() throws Exception {
		Path base = this.temporary.newFolder("local").toPath();
		assertEquals(base.toAbsolutePath().normalize().resolve("css/a.css"),
		        ResourceWalker.resolveInside(base, "css/a.css"));
		assertEquals(base.toAbsolutePath().normalize().resolve("b.css"),
		        ResourceWalker.resolveInside(base, "css/../b.css"));
	}

	@Test
	public void resolveInsideRefusesNamesOutsideTheBase() throws Exception {
		Path base = this.temporary.newFolder("local").toPath();
		assertNull(ResourceWalker.resolveInside(base, "../x"));
		assertNull(ResourceWalker.resolveInside(base, "css/../../x"));
		assertNull(ResourceWalker.resolveInside(base, base.getParent().resolve("x").toAbsolutePath().toString()));
		assertNull("the base itself is not a file inside it", ResourceWalker.resolveInside(base, ""));
		assertNull("invalid name", ResourceWalker.resolveInside(base, "a\u0000b"));
	}

	@Test
	public void resolveInsideRefusesWindowsForms() throws Exception {
		if (File.separatorChar != '\\') {
			return; // a backslash is an ordinary character elsewhere
		}
		Path base = this.temporary.newFolder("local").toPath();
		assertNull(ResourceWalker.resolveInside(base, "..\\x"));
		assertNull(ResourceWalker.resolveInside(base, "C:\\Windows\\win.ini"));
		assertNull(ResourceWalker.resolveInside(base, "C:x"));
	}

	@Test
	public void lookupRefusesTraversal() {
		for (String name : new String[] { "../pom.xml", "/timing/../../pom.xml" }) {
			FileNotFoundException refused = assertThrows(FileNotFoundException.class,
			        () -> ResourceWalker.getFileOrResourcePath(name));
			assertTrue(refused.getMessage(), refused.getMessage().contains("outside the resource directories"));
		}
		FileNotFoundException refused = assertThrows(FileNotFoundException.class,
		        () -> ResourceWalker.getFileOrResource("../pom.xml"));
		assertTrue(refused.getMessage(), refused.getMessage().contains("outside the resource directories"));
	}

	@Test
	public void lookupStillFindsResources() throws Exception {
		assertNotNull(ResourceWalker.getFileOrResourcePath("/timing/timing.properties"));
		try (InputStream is = ResourceWalker.getFileOrResource("/timing/timing.properties")) {
			assertNotNull(is);
		}
	}
}
