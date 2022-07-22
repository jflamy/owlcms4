/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.prutils;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.Element;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

public class SoundUtils {

    static Logger logger = (Logger) LoggerFactory.getLogger(SoundUtils.class);

    public static void doEnableAudioContext(Element element) {
        // this.getElement().executeJs("window.audioCtx.suspend()");
        PendingJavaScriptResult result = element.executeJs("return (window.isIOS ? window.audioCtx.state : 'running')");
        result.then(String.class, r -> {
            logger.debug("audio state {}", r);
            if (!r.equals("running")) {
                element.executeJs("window.audioCtx.resume()");
            } else {
                // Notification.show("Audio enabled");
            }
        });
    }

    public static void enableAudioContextNotification(Element element) {
        // this.getElement().executeJs("window.audioCtx.suspend()");
        PendingJavaScriptResult result = element.executeJs("return (window.isIOS ? window.audioCtx.state : 'running')");
        // PendingJavaScriptResult result = element.executeJs("return window.audioCtx.state");
        audioStatusCallback(element, result);
    }

    public static void enableAudioContextNotification(Element element, boolean useState) {
        PendingJavaScriptResult result = element
                .executeJs("return (window.isIOS ||" + useState + " ? window.audioCtx.state : 'running')");
        // PendingJavaScriptResult result = element.executeJs("return window.audioCtx.state");
        audioStatusCallback(element, result);
    }

    private static void audioStatusCallback(Element element, PendingJavaScriptResult result) {
        result.then(String.class, r -> {
            // logger.debug("audio state {}", r);
            if (!r.equals("running")) {
                Notification n = new Notification();
                n.setDuration(0);
                n.setPosition(Position.TOP_STRETCH);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                Button content = new Button();
                content.setText(Translator.translate("ClickOrTapToEnableSound"));
                content.addClickListener(c -> {
                    element.executeJs("window.audioCtx.resume()");
                    Component component = element.getComponent().get();
                    if (component instanceof DisplayParameters) {
                        ((DisplayParameters) component).setSilenced(false);
                    }
                    n.close();
                });
                n.add(content);
                n.open();
            } else {
                // Notification.show("Audio enabled");
            }
        });
    }

}
