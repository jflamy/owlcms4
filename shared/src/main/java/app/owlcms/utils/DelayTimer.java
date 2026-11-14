/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

public class DelayTimer {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(DelayTimer.class);
    private boolean testingMode;

    public DelayTimer()  {
        this(false);
    }
    
    public DelayTimer(boolean testingMode) {
        this.testingMode = testingMode;
    }

    private final Timer t = new Timer();

    public TimerTask schedule(final Runnable r, long delay) {
        if (testingMode) {
            r.run();
            return null;
        } else {
            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    // an exception escaping a TimerTask is never logged and kills the timer thread
                    try {
                        r.run();
                    } catch (RuntimeException e) {
                        LoggerUtils.logError(logger, e);
                    } catch (Error e) {
                        LoggerUtils.logError(logger, e);
                        throw e;
                    }
                }
            };
            t.schedule(task, delay);
            return task;
        }
    }
}