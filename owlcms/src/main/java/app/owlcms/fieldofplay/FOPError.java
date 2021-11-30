/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import app.owlcms.fieldofplay.FOPEvent.TimeStarted;
import ch.qos.logback.classic.Logger;
import app.owlcms.i18n.Translator;

public class FOPError {

    static Table<String, String, String> errorMessages = HashBasedTable.create();
    private final static Logger logger = (Logger) LoggerFactory.getLogger(FOPError.class);

    static {
        safePut(FOPState.DECISION_VISIBLE, TimeStarted.class, "ERROR_WAIT_FOR_RESET");
    }

    public static String translateMessage(FOPState fopState, FOPEvent event) {
        String fopStateString = fopState.toString();
        String stateTr = Translator.translate("STATE_" + fopStateString);
        String fopEventString = event.getClass().getSimpleName();
        String eventTr = Translator.translate("EVENT_" + fopEventString);
        String message = Translator.translate(getMessageTemplate(fopStateString, fopEventString), eventTr, stateTr);
        return message;
    }

    public static String translateMessage(String fopStateString, String fopEventString) {
        String stateTr = Translator.translate("STATE_" + fopStateString);
        String eventTr = Translator.translate("EVENT_" + fopEventString);
        String message = Translator.translate(getMessageTemplate(fopStateString, fopEventString), eventTr, stateTr);
        return message;
    }

    @SuppressWarnings("unused")
    private static String getMessageTemplate(FOPState fopState, Class<? extends FOPEvent> fopEventClass) {
        String msg = errorMessages.get(fopState.toString(), fopEventClass.getSimpleName());
        if (msg == null) {
            msg = "Unexpected_event_state";
        }
        return msg;
    }

    private static String getMessageTemplate(String fopStateString, String fopEventString) {
        logger.debug("getting message for {} {}", fopStateString, fopEventString);
        String msg = errorMessages.get(fopStateString, fopEventString);
        if (msg == null) {
            msg = "Unexpected_event_state";
        }
        return msg;
    }

    private static void safePut(FOPState state, Class<? extends FOPEvent> fopEventClass, String messagePattern) {
        String fopStateString = state.toString();
        String fopEventString = fopEventClass.getSimpleName();
        logger.debug("adding {} for {} {}", messagePattern, fopStateString, fopEventString);
        errorMessages.put(fopStateString, fopEventString, messagePattern);
    }

}
