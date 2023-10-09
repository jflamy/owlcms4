/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.apputils;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.internal.AllowInert;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.Element;

import app.owlcms.components.elements.SoundEnabler;
import ch.qos.logback.classic.Logger;

public class SoundUtils {

	static Logger logger = (Logger) LoggerFactory.getLogger(SoundUtils.class);

//	public static void doEnableAudioContext(Element element) {
//		PendingJavaScriptResult result = element.executeJs(
//		        "console.warn('setting audio status'); return (window.isIOS ? window.audioCtx.state : 'running')");
//		result.then(String.class, r -> {
//			logger.debug("audio state {}", r);
//			if (!r.equals("running")) {
//				element.executeJs("console.warn('setting audio status'); window.audioCtx.resume()");
//			} else {
//				// Notification.show("Audio enabled");
//			}
//		});
//	}

	@AllowInert
	public static void enableAudioContextNotification(Element element) {
		logger.debug("enableAudioContextNotification");
		PendingJavaScriptResult result = element.executeJs(
		        "console.warn('checking audio status'); return window.audioCtx.state");
		audioStatusCallback(element, result);
	}

//	@AllowInert
//	public static void enableAudioContextNotification(Element element, boolean useState) {
//		PendingJavaScriptResult result = element
//		        .executeJs("return (window.isIOS ||" + useState + " ? window.audioCtx.state : 'running')");
//		audioStatusCallback(element, result);
//	}

	@AllowInert
	private static void audioStatusCallback(Element element, PendingJavaScriptResult result) {
		result.then(String.class, r -> {
			logger.warn("audio state {}", r);
			if (!r.equals("running")) {
				Notification n = new Notification();
				n.setDuration(0);
				n.setPosition(Position.TOP_STRETCH);
				n.addThemeVariants(NotificationVariant.LUMO_ERROR);
				SoundEnabler content = new SoundEnabler(() -> n.close());
				n.add(content);
				n.open();
			} else {
				// Notification.show("Audio enabled");
			}
		});
	}

}
