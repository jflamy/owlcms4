/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.apputils.queryparameters;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.HasStyle;

import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import ch.qos.logback.classic.Logger;

public interface FOPParameters extends HasStyle {

	final String FOP = "fop";
	final String GROUP = "group";
	final Logger logger = (Logger) LoggerFactory.getLogger(FOPParameters.class);

	public FieldOfPlay getFop();

	public Group getGroup();

	public void setFop(FieldOfPlay fop);

	public void setGroup(Group group);

}