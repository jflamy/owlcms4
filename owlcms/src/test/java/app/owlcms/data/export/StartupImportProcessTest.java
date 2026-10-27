/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import app.owlcms.Main;
import app.owlcms.StartupImportProcessFixture;

public class StartupImportProcessTest {

	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void missingSourceIsTerminalAndNeverSignalsReady() throws Exception {
		assertTerminalFailure(this.temporary.getRoot().toPath().resolve("missing.json"));
	}

	@Test
	public void malformedJsonIsTerminalAndNeverSignalsReady() throws Exception {
		Path malformed = write("malformed.json", "{\"formatVersion\":");
		assertTerminalFailure(malformed);
	}

	@Test
	public void finalProcessingFailureIsTerminalAndNeverSignalsReady() throws Exception {
		Path noConfig = write("no-config.json", "{\"formatVersion\":\"2.0\"}");
		assertTerminalFailure(noConfig);
	}

	private Path write(String name, String content) throws Exception {
		Path path = this.temporary.getRoot().toPath().resolve(name);
		Files.writeString(path, content, StandardCharsets.UTF_8);
		return path;
	}

	private void assertTerminalFailure(Path source) throws Exception {
		Path root = this.temporary.newFolder(source.getFileName().toString() + "-run").toPath();
		Path output = root.resolve("output.txt");
		String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
		Path java = Path.of(System.getProperty("java.home"), "bin", "java");
		ProcessBuilder builder = new ProcessBuilder(List.of(java.toString(), "-cp", classpath,
		        StartupImportProcessFixture.class.getName(), source.toString()))
		        .directory(root.toFile())
		        .redirectErrorStream(true)
		        .redirectOutput(output.toFile());
		builder.environment().remove("OWLCMS_LOADJSON");
		Process process = builder.start();
		if (!process.waitFor(60, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			throw new IllegalStateException("Startup import fixture timed out: " + Files.readString(output));
		}
		String log = Files.readString(output);
		assertEquals(log, Main.EXIT_STARTUP_IMPORT_FAILED, process.exitValue());
		assertFalse(log, log.contains("OWLCMS Ready."));
		assertFalse(log, log.contains("Database ready"));
	}
}
