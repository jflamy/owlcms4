/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.displays.attemptboard;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.material.Material;

import app.owlcms.i18n.TranslationProvider;

@SuppressWarnings("serial")
@Tag("attempt-board-template")
@HtmlImport("frontend://components/AttemptBoard.html")
@Route("displays/athleteFacingAttempt")
@Theme(value = Material.class, variant = Material.DARK)
public class AthleteFacingAttemptBoard extends AttemptBoard {
	
	public AthleteFacingAttemptBoard() {
		super();
		setPublicFacing(false);
	}
	
	public void setPublicFacing(boolean publicFacing) {
		getModel().setPublicFacing(publicFacing);
	}
	
	public boolean isPublicFacing() {
		return Boolean.TRUE.equals(getModel().isPublicFacing());
	}

	/* (non-Javadoc)
	 * @see app.owlcms.displays.attemptboard.AttemptBoard#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		decisions.setPublicFacing(false);
	}
	
	@Override
	public String getPageTitle() {
		return TranslationProvider.getTranslation("AthleteFacingAttemptBoard.0"); //$NON-NLS-1$
	}
}
