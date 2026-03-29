/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dialog.Dialog;

import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.uievents.BreakType;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class BreakDialog extends Dialog {
	private BreakManagement content;
	final private Logger logger = (Logger) LoggerFactory.getLogger(BreakDialog.class);
	{
		this.logger.setLevel(Level.INFO);
	}

	/**
	 * @param brt
	 * @param cdt
	 * @param origin
	 */
	public BreakDialog(FieldOfPlay fop, BreakType brt, CountdownType cdt, Integer secondsRemaining, Object origin) {
		// logger.debug("BreakDialog brt = {}", brt);
		this.content = new BreakManagement(fop, brt, cdt, secondsRemaining, this, origin);
		this.add(this.content);

		this.addDialogCloseActionListener((e) -> {

			// defensive, should have been unregistered already
			try {
				FieldOfPlay cfop = fop;
				if (cfop != null) {
					cfop.getUiEventBus().unregister(this.content);
				}
				// logger.debug("+unregistered {}", breakTimer.id);
			} catch (Exception e1) {
			}
			try {
				FieldOfPlay cfop = fop;
				if (cfop != null) {
					cfop.getFopEventBus().unregister(this.content);
				}
			} catch (Exception e1) {
			}
			this.content = null;
			this.removeAll();
			this.close();
		});
	}

	/**
	 * Used by the announcer -- tries to guess what type of break is pertinent based on field of play.
	 *
	 * @param origin the origin
	 */
	@SuppressWarnings("deprecation")
	public BreakDialog(FieldOfPlay fop, Object origin) {
		this.content = new BreakManagement(fop, this, origin);
		this.add(this.content);

		this.addDialogCloseActionListener((e) -> {
			this.removeAll();
			this.close();

			// defensive, should have been unregistered already
			try {
				FieldOfPlay cfop = fop;
				if (cfop != null) {
					cfop.getUiEventBus().unregister(this.content);
				}
			} catch (Exception e1) {
			}

			try {
				FieldOfPlay cfop = fop;
				if (cfop != null) {
					cfop.getFopEventBus().unregister(this.content);
				}
			} catch (Exception e1) {
			}
			this.content = null;

		});
	}

}
