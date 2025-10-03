/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.utils;

import java.io.IOException;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * Provide a local resource as a byte array, for use in jxls image directives
 * Do not expose all the methods of ResourceWalker.
 */
public class LocalResource {

	private static Logger logger = (Logger) LoggerFactory.getLogger(LocalResource.class);
	
	static public byte[] getBytes(String resourceName) throws IOException {
		try {
			logger.debug("getBytes {}", resourceName);
			return ResourceWalker.getBytes(resourceName);
		} catch (Exception e) {
			logger.error("Error getting local resource " + resourceName);
			return null;
		}
	}

}
