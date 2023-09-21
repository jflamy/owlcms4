/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dialog.Dialog;

import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.uievents.BreakType;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class BreakDialog extends Dialog {
	private BreakManagement content;
	final private Logger logger = (Logger) LoggerFactory.getLogger(BreakDialog.class);
	{
		logger.setLevel(Level.INFO);
	}

	/**
	 * Used by the announcer -- tries to guess what type of break is pertinent based
	 * on field of play.
	 *
	 * @param origin the origin
	 */
	public BreakDialog(Object origin) {
		content = new BreakManagement(OwlcmsSession.getFop(), this, origin);
		this.add(content);

		this.addDialogCloseActionListener((e) -> {
			this.removeAll();
			this.close();

			// defensive, should have been unregistered already
			try {
				OwlcmsSession.getFop().getUiEventBus().unregister(content);
			} catch (Exception e1) {
			}

			try {
				OwlcmsSession.getFop().getFopEventBus().unregister(content);
			} catch (Exception e1) {
			}
			content = null;

		});
	}

	/**
	 * @param brt
	 * @param cdt
	 * @param origin
	 */
	public BreakDialog(BreakType brt, CountdownType cdt, Integer secondsRemaining, Object origin) {
		// logger.debug("BreakDialog brt = {}", brt);
		content = new BreakManagement(OwlcmsSession.getFop(), brt, cdt, secondsRemaining, this, origin);
		this.add(content);

		this.addDialogCloseActionListener((e) -> {

			// defensive, should have been unregistered already
			try {
				OwlcmsSession.getFop().getUiEventBus().unregister(content);
				// logger.debug("++++++ unregistered {}", breakTimer.id);
			} catch (Exception e1) {
			}
			try {
				OwlcmsSession.getFop().getFopEventBus().unregister(content);
			} catch (Exception e1) {
			}
			content = null;
			this.removeAll();
			this.close();
		});
	}

}
