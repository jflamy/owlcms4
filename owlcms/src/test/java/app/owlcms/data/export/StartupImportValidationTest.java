/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export;

import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import app.owlcms.data.export.v2.CompetitionDataV2;

public class StartupImportValidationTest {

	@Test
	public void v1RejectsEmptyMalformedTruncatedAndTrailingInput() {
		assertThrows(Exception.class, () -> importV1(""));
		assertThrows(Exception.class, () -> importV1("not json"));
		assertThrows(Exception.class, () -> importV1("{\"competition\":"));
		assertThrows(Exception.class, () -> importV1("{} trailing"));
	}

	@Test
	public void v2RejectsEmptyMalformedTruncatedAndTrailingInput() {
		assertThrows(IllegalStateException.class, () -> importV2(""));
		assertThrows(IllegalStateException.class, () -> importV2("not json"));
		assertThrows(IllegalStateException.class, () -> importV2("{\"formatVersion\":\"2.0\""));
		assertThrows(IllegalStateException.class, () -> importV2("{\"formatVersion\":\"2.0\"} trailing"));
	}

	private static void importV1(String json) throws Exception {
		new CompetitionData().importDataFromString(json);
	}

	private static void importV2(String json) {
		new CompetitionDataV2().importData(
		        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
	}
}
