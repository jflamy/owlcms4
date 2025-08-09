/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.utils;

import java.io.IOException;

/**
 * Provide a local resource as a byte array, for use in jxls image directives
 * Do not expose all the methods of ResourceWalker.
 */
public class LocalResource {
	
	static public byte[] getBytes(String resourceName) throws IOException {
		return ResourceWalker.getBytes(resourceName);
	}

}
