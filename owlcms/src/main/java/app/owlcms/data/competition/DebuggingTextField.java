/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.competition;

import com.vaadin.flow.component.textfield.TextField;

@SuppressWarnings("serial")
public class DebuggingTextField extends TextField {
	@Override
	public void setInvalid(boolean invalid) {
		super.setInvalid(invalid);
	}
}
