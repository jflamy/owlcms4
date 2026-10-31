/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.access;

/** An import was refused because imports are turned off on the Administration page. */
@SuppressWarnings("serial")
public class ImportsNotAllowedException extends RuntimeException {

	private final ImportGate.Kind kind;

	public ImportsNotAllowedException(ImportGate.Kind kind) {
		super(kind + " import refused: imports are not allowed");
		this.kind = kind;
	}

	public ImportGate.Kind getKind() {
		return this.kind;
	}

	/** The refusal in a cause chain, or null. */
	public static ImportsNotAllowedException find(Throwable t) {
		for (Throwable c = t; c != null; c = c.getCause()) {
			if (c instanceof ImportsNotAllowedException refused) {
				return refused;
			}
			if (c.getCause() == c) {
				break;
			}
		}
		return null;
	}
}
