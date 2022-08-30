package app.owlcms.utils;

import java.util.Timer;
import java.util.TimerTask;

public class DelayTimer {
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
                    r.run();
                }
            };
            t.schedule(task, delay);
            return task;
        }
    }
}