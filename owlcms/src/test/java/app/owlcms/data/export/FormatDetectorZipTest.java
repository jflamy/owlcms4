package app.owlcms.data.export;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Test;

public class FormatDetectorZipTest {

	private static final byte[] JSON = "{\"formatVersion\":\"2.0\"}".getBytes(StandardCharsets.UTF_8);

	private static byte[] zip(String... namesAndContents) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
			for (int i = 0; i < namesAndContents.length; i += 2) {
				zip.putNextEntry(new ZipEntry(namesAndContents[i]));
				zip.write(namesAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		}
		return bytes.toByteArray();
	}

	private static byte[] read(byte[] content) throws IOException {
		return FormatDetector.readJsonBytes(new ByteArrayInputStream(content));
	}

	@Test
	public void plainJsonIsReturnedUnchanged() throws IOException {
		assertArrayEquals(JSON, read(JSON));
	}

	@Test
	public void zipYieldsItsJsonEntry() throws IOException {
		byte[] archive = zip("competition.json", new String(JSON, StandardCharsets.UTF_8));
		assertArrayEquals(JSON, read(archive));
	}

	@Test
	public void firstJsonEntryIsUsedAndOtherEntriesAreSkipped() throws IOException {
		byte[] archive = zip("readme.txt", "ignore me", "logs/", "", "database_v2_x.json",
		        new String(JSON, StandardCharsets.UTF_8), "second.json", "{}");
		assertArrayEquals(JSON, read(archive));
	}

	@Test
	public void zipWithoutJsonIsAnError() throws IOException {
		try {
			read(zip("readme.txt", "nothing here"));
			fail("expected an error");
		} catch (IOException e) {
			// expected
		}
	}

	@Test
	public void truncatedZipIsAnError() throws IOException {
		byte[] archive = zip("competition.json", new String(JSON, StandardCharsets.UTF_8));
		try {
			byte[] result = read(Arrays.copyOf(archive, 20));
			fail("expected an error, got " + new String(result, StandardCharsets.UTF_8));
		} catch (IOException e) {
			// expected
		}
	}
}
