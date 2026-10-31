/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Extraction of uploaded override zips must stay inside the target directory ("zip slip").
 */
public class ZipUtilsTest {

	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	private static byte[] zipOf(String... names) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
			for (String name : names) {
				zip.putNextEntry(new ZipEntry(name));
				zip.write(name.getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		}
		return bytes.toByteArray();
	}

	@Test
	public void ordinaryEntriesAreExtractedWithoutTheLocalPrefix() throws IOException {
		Path target = this.temporary.newFolder("target").toPath();
		ZipUtils.extractZip(new ByteArrayInputStream(zipOf("local/css/a.css", "templates/b.xlsx")), target);
		assertEquals("local/css/a.css", Files.readString(target.resolve("css/a.css")));
		assertTrue(Files.exists(target.resolve("templates/b.xlsx")));
	}

	@Test
	public void entryClimbingOutOfTheTargetIsRefused() throws IOException {
		Path target = this.temporary.newFolder("parent", "target").toPath();
		byte[] zip = zipOf("local/../../escaped.txt");
		assertThrows(IOException.class, () -> ZipUtils.extractZip(new ByteArrayInputStream(zip), target));
		assertFalse("nothing written beside the target", Files.exists(target.getParent().resolve("escaped.txt")));
		assertFalse(Files.exists(target.getParent().getParent().resolve("escaped.txt")));
	}

	@Test
	public void absoluteEntryIsRefused() throws IOException {
		Path target = this.temporary.newFolder("target").toPath();
		Path outside = this.temporary.getRoot().toPath().resolve("absolute.txt").toAbsolutePath();
		byte[] zip = zipOf(outside.toString());
		assertThrows(IOException.class, () -> ZipUtils.extractZip(new ByteArrayInputStream(zip), target));
		assertFalse(Files.exists(outside));
	}
}
