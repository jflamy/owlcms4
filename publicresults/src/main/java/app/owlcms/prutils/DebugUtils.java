/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.prutils;

import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;

import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

public class DebugUtils {

    static Logger logger = (Logger) LoggerFactory.getLogger("garbageCollection");

    final static String LINESEPARATOR = System.getProperty("line.separator");

    public static void gc() {
        final String where = LoggerUtils.whereFrom();
        new Thread(() -> {
            try {
                logger.debug("clearing memory {}", where);
                System.gc();
                Thread.sleep(500);
                System.gc();
            } catch (InterruptedException e) {
            }

        }).start();
    }
    

    public static String getOwlcmsParentName(Component e) {
        Class<? extends Component> class1 = e.getClass();
        String className = class1.getName();
        if (className.contains("vaadin") || (!className.endsWith("Board") && !className.endsWith("Content")
                && !className.endsWith("Display") && !className.endsWith("Layout"))) {
            Optional<Component> parent = e.getParent();
            if (parent.isPresent()) {
                return getOwlcmsParentName(parent.get());
            } else {
                return class1.getSimpleName();
            }
        } else {
            return class1.getSimpleName();
        }
    }
}
