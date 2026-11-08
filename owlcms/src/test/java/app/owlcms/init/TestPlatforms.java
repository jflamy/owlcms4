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
 * Test-only access to the platforms registered in {@link OwlcmsFactory}, which keeps its setter package-private.
 */
public final class TestPlatforms {

	private TestPlatforms() {
	}

	/** @return the registered platforms before the replacement, to be restored by the test */
	public static Map<String, FieldOfPlay> replace(Map<String, FieldOfPlay> platforms) {
		Map<String, FieldOfPlay> previous = OwlcmsFactory.getFopByName();
		OwlcmsFactory.setFopByName(platforms);
		return previous;
	}
}
