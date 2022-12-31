/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.apputils;

import java.util.List;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

public class DebugUtils {

    final static String LINESEPARATOR = System.getProperty("line.separator");

    static Logger logger = (Logger) LoggerFactory.getLogger("garbageCollection");

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

    /**
     * @param lifterList
     * @return ordered printout of lifters, one per line.
     */
    public static String longDump(List<Athlete> lifterList) {
        StringBuffer sb = new StringBuffer();
        for (Athlete lifter : lifterList) {
            sb.append(lifter.longDump());
            sb.append(LINESEPARATOR);
        }
        return sb.toString();
    }

    /**
     * @param lifterList
     * @return ordered printout of lifters, one per line.
     */
    public static String shortDump(List<? extends Athlete> lifterList) {
        StringBuffer sb = new StringBuffer();
        for (Athlete lifter : lifterList) {
            sb.append(lifter.getLastName() + " " + lifter.getFirstName() + " " + lifter.getNextAttemptRequestedWeight()
                    + " " + (lifter.getAttemptsDone() + 1) + " " + lifter.getLotNumber());
            sb.append(LINESEPARATOR);
        }
        return sb.toString();
    }

    /**
     * @param lifterList
     * @return ordered printout of lifters, one per line.
     */
    static String longDump(List<Athlete> lifterList, boolean includeTimeStamp) {
        StringBuffer sb = new StringBuffer();
        for (Athlete lifter : lifterList) {
            sb.append(lifter.longDump());
            sb.append(LINESEPARATOR);
        }
        return sb.toString();
    }
}
