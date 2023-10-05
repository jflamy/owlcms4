/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.internal.AllowInert;
import com.vaadin.flow.component.littemplate.LitTemplate;

import app.owlcms.i18n.Translator;
import app.owlcms.nui.shared.SafeEventBusRegistration;

/**
 * Button that triggers an initSounds document event that causes components
 * to initialize their sounds.
 * 
 * iOS in particular is stringent about requiring that sounds be played in response
 * to a user interaction.  Once a sound has been played once, it can be played
 * again without user intervention
 * 
 * @author jflamy
 * 
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("soundenabler-element")
@JsModule("./components/SoundEnabler.js")
public class SoundEnabler extends LitTemplate
        implements SafeEventBusRegistration {
	
	private Runnable afterSoundEnabled;

	public SoundEnabler(Runnable afterSoundEnabled) {
		this.afterSoundEnabled = afterSoundEnabled;
		this.getStyle().set("background-color", "var(--lumo-error-text-color");
		this.getStyle().set("color", "white");
		this.getStyle().set("padding", "1ex");
		this.getElement().setProperty("caption", Translator.translate("ClickOrTapToEnableSound"));
	}
	
	@AllowInert
	@ClientCallable
	public void soundEnabled() {
		afterSoundEnabled.run();
	}

}
