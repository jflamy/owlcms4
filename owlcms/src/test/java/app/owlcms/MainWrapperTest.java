package app.owlcms;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class MainWrapperTest {

	@Test
	public void restartWithoutStartupImportDropsOnlyTheLoadJsonProperty() {
		List<String> vmArgs = List.of("-Xmx1g", "-DloadJson=/data/checkpoints", "-DloadJsonOther=x", "-DloadJson",
		        "-Dlogback.configurationFile=logback.xml");
		assertEquals(List.of("-Xmx1g", "-DloadJsonOther=x", "-Dlogback.configurationFile=logback.xml"),
		        MainWrapper.withoutStartupImport(vmArgs));
	}

	@Test
	public void theRestartCodeIsRestartableAndDistinctFromTheLoadFailure() {
		int code = Main.EXIT_RESTART_WITHOUT_STARTUP_IMPORT;
		// the wrapper and the control panel restart on 1-127; 78 means the startup load itself failed
		assertEquals(true, code > 1 && code < 128);
		assertEquals(false, code == Main.EXIT_STARTUP_IMPORT_FAILED);
	}
}
