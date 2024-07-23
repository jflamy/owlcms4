package app.owlcms.prutils;

import java.util.Timer;
import java.util.TimerTask;

public class CountdownTimer {
    private Timer timer;
    private long startTime;
    private long duration;
    private TimerTask task;
    private boolean running;

    public CountdownTimer(long duration) {
        this.duration = duration;
        this.timer = new Timer();
        this.running = false;
        this.task = new TimerTask() {
            @Override
            public void run() {
                // Task to be scheduled
            }
        };
    }

    public void start() {
        this.running = true;
        this.startTime = System.currentTimeMillis();
        this.timer.schedule(task, this.duration);
    }
    
    public void set(long duration) {
        stop();
        this.duration = duration;
    }

    public void stop() {
        this.running = false;
        this.timer.cancel();
        this.timer.purge();
    }

    public void restart() {
        this.timer = new Timer();
        this.task = new TimerTask() {
            @Override
            public void run() {
                // Task to be scheduled
            }
        };
        start();
    }

    public void restartAtValue(long duration) {
        stop();
        this.duration = duration;
        restart();
    }

    public long getTimeRemaining() {
        if (!running) {
            return this.duration;
        }
        long elapsedTime = System.currentTimeMillis() - this.startTime;
        return this.duration - elapsedTime;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isIndefinite() {
        return duration < 0;
    }
}
