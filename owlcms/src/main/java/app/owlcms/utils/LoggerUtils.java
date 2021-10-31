/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.MDC;

import app.owlcms.init.OwlcmsSession;

/**
 * The Class LoggerUtils.
 */
public class LoggerUtils {

    /**
     * @param e1
     * @return
     */
    public static String exceptionMessage(Throwable e1) {
        String message = null;
        if (e1.getCause() != null) {
            message = e1.getCause().getMessage();
        }
        if (message == null) {
            message = e1.getMessage();
        }
        if (message == null) {
            message = e1.getClass().getSimpleName();
        }
        return message;
    }

    public static void setWhere(String where) {
        MDC.put("page", where);
        OwlcmsSession.withFop(fop -> MDC.put("currentGroup", fop.getGroup() != null ? fop.getGroup() : "-"));
    }

    /**
     * Where from.
     *
     * @return the string
     */
    public static String stackTrace() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        int i = 0;
        for (StackTraceElement ste : trace) {
            String string = ste.toString();
            if (string.startsWith("com.vaadin.flow.server.communication") || string.startsWith("com.vaadin.flow.internal")) {
                break;
            }
            if (i > 1) {
                pw.println("\t" + string);
            }

            i++;
        }
        return sw.toString();
    }

    /**
     * @param t
     * @return
     */
    public static String stackTrace(Throwable t) {
        // IDEA: skip from "at javax.servlet.http.HttpServlet.service" to line starting
        // with "Caused by"
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Where from.
     *
     * @return the string
     */
    public static String whereFrom() {
        return whereFrom(1);
    }

    /**
     * Where from, additional depth
     *
     * @return the string
     */
    public static String whereFrom(int depth) {
        String where = Thread.currentThread().getStackTrace()[3 + depth].toString();
        return where.replaceFirst(".*\\(", "(");
    }
}
