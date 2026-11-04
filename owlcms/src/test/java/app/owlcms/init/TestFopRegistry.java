/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import java.util.Map;

import app.owlcms.fieldofplay.FieldOfPlay;

/**
 * Test-only bridge to swap the platform registry: {@code OwlcmsFactory.setFopByName} is package-private, and calling
 * {@code initFOPByName} in a test creates real FieldOfPlay instances whose MQTT monitors retry a broker connection
 * forever. Never call the init path from a test.
 */
public final class TestFopRegistry {

	private TestFopRegistry() {
	}

	/** Install a replacement registry, returning the previous one for restoration in a finally block. */
	public static Map<String, FieldOfPlay> replace(Map<String, FieldOfPlay> replacement) {
		Map<String, FieldOfPlay> previous = OwlcmsFactory.getFopByName();
		OwlcmsFactory.setFopByName(replacement);
		return previous;
	}
}
