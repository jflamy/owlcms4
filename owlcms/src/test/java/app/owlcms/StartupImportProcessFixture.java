/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms;

/** Child-JVM fixture because a failed startup import deliberately terminates the process. */
public class StartupImportProcessFixture {

	public static void main(String[] args) {
		if (args.length != 1) {
			throw new IllegalArgumentException("expected the startup import path");
		}
		System.setProperty("loadJson", args[0]);
		Main.memoryMode = true;
		Main.initStartup();
		throw new IllegalStateException("failed startup import returned instead of terminating");
	}
}
