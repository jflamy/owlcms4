package app.owlcms.data.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import app.owlcms.data.export.StartupImportSource.Candidate;

public class StartupImportSourceTest {

	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	private Path file(String name, long epochSecond) throws IOException {
		File created = this.temporary.newFile(name);
		Files.setLastModifiedTime(created.toPath(), FileTime.from(Instant.ofEpochSecond(epochSecond)));
		return created.toPath();
	}

	@Test
	public void unsetOrBlankMeansNoImport() {
		assertFalse(StartupImportSource.resolve(null).isPresent());
		assertFalse(StartupImportSource.resolve("").isPresent());
		assertFalse(StartupImportSource.resolve("   ").isPresent());
	}

	@Test
	public void fileIsUsedAsIs() throws IOException {
		Path export = file("competition.json", 1000);
		assertEquals(export, StartupImportSource.resolve(export.toString()).orElseThrow());
	}

	@Test
	public void quotesAddedByAFileManagerAreRemoved() throws IOException {
		Path export = file("competition.json", 1000);
		assertEquals(export, StartupImportSource.resolve("\"" + export + "\"").orElseThrow());
	}

	@Test
	public void folderUsesTheMostRecentJsonOrZip() throws IOException {
		file("old.json", 1000);
		Path newest = file("checkpoint_2.ZIP", 3000);
		file("middle.json", 2000);
		assertEquals(newest, StartupImportSource.resolve(this.temporary.getRoot().toString()).orElseThrow());
	}

	@Test
	public void temporaryAndOtherFilesAreNeverCandidates() throws IOException {
		Path published = file("checkpoint.zip", 1000);
		file("checkpoint.zip.partial", 5000);
		file("notes.txt", 6000);
		this.temporary.newFolder("subfolder.json");
		assertEquals(List.of(published),
		        StartupImportSource.candidates(this.temporary.getRoot().toPath()).stream().map(Candidate::path)
		                .toList());
	}

	@Test
	public void candidatesAreNewestFirstWithNameBreakingTies() throws IOException {
		file("a.json", 1000);
		file("b.json", 2000);
		file("c.json", 2000);
		List<String> names = StartupImportSource.candidates(this.temporary.getRoot().toPath()).stream()
		        .map(candidate -> candidate.path().getFileName().toString()).toList();
		assertEquals(List.of("c.json", "b.json", "a.json"), names);
	}

	@Test
	public void emptyFolderIsAnError() {
		try {
			StartupImportSource.resolve(this.temporary.getRoot().toString());
			fail("expected an error");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("No .json or .zip"));
		}
	}

	@Test
	public void missingPathIsAnError() {
		try {
			StartupImportSource.resolve(new File(this.temporary.getRoot(), "absent.json").toString());
			fail("expected an error");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("does not name an existing file or folder"));
		}
	}
}
