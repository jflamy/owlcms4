package app.owlcms.fieldofplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import app.owlcms.data.competition.Competition;
import app.owlcms.uievents.UIEvent;

public class ProxyAthleteTimerTest {

    private FieldOfPlay fop;
    private ProxyAthleteTimer timer;
    private int twoMinutes;
    private int oneMinute;
    private int initialWarning;
    private int finalWarning;
    private final CountDownLatch expired = new CountDownLatch(1);
    private final CountDownLatch stopped = new CountDownLatch(1);
    private final CountDownLatch warned = new CountDownLatch(1);
    private final CountDownLatch callbackEntered = new CountDownLatch(1);
    private final CountDownLatch releaseCallback = new CountDownLatch(1);
    private final AtomicReference<Thread> callbackThread = new AtomicReference<>();
    private final AtomicReference<Throwable> callbackFailure = new AtomicReference<>();
    private final AtomicInteger expiryCount = new AtomicInteger();
    private final AtomicInteger initialWarningCount = new AtomicInteger();
    private final AtomicInteger finalWarningCount = new AtomicInteger();
    private boolean blockWarning;
    private boolean blockExpiry;

    @Before
    public void setUp() {
        twoMinutes = Competition.athleteTimerTwoMinutes;
        oneMinute = Competition.athleteTimerOneMinute;
        initialWarning = Competition.athleteTimerInitialWarning;
        finalWarning = Competition.athleteTimerFinalWarning;
        Competition.athleteTimerTwoMinutes = twoMinutes / 100;
        Competition.athleteTimerOneMinute = oneMinute / 100;
        Competition.athleteTimerInitialWarning = initialWarning / 100;
        Competition.athleteTimerFinalWarning = finalWarning / 100;

        fop = new FieldOfPlay() {
            @Override
            public boolean isEmitSoundsOnServer() {
                return false;
            }

            @Override
            void emitInitialWarning() {
                initialWarningCount.incrementAndGet();
            }

            @Override
            void emitFinalWarning() {
                finalWarningCount.incrementAndGet();
                warned.countDown();
            }

            @Override
            void emitTimeOver() {
                expiryCount.incrementAndGet();
                expired.countDown();
            }

            @Override
            public void fopEventPost(FOPEvent event) {
                if (blockExpiry && event instanceof FOPEvent.TimeOver) {
                    callbackThread.set(Thread.currentThread());
                    callbackEntered.countDown();
                }
            }

            @Override
            public void pushOutUIEvent(UIEvent event) {
                if (event instanceof UIEvent.StopTime) {
                    stopped.countDown();
                }
            }
        };
        timer = new ProxyAthleteTimer(fop) {
            @Override
            public void finalWarning(Object origin) {
                if (blockWarning) {
                    blockCallback();
                }
                super.finalWarning(origin);
            }
        };
    }

    @After
    public void tearDown() throws InterruptedException {
        releaseCallback.countDown();
        try {
            if (timer != null) {
                timer.stop();
            }
            Thread worker = callbackThread.get();
            if (worker != null) {
                worker.join(3000);
                assertFalse("old timer thread did not terminate", worker.isAlive());
            }
        } finally {
            Competition.athleteTimerTwoMinutes = twoMinutes;
            Competition.athleteTimerOneMinute = oneMinute;
            Competition.athleteTimerInitialWarning = initialWarning;
            Competition.athleteTimerFinalWarning = finalWarning;
        }
    }

    @Test
    public void oneMinuteCountdownWarnsExpiresAndStops() throws InterruptedException {
        assertCountdown(Competition.athleteTimerOneMinute, 0);
    }

    @Test
    public void twoMinuteCountdownWarnsExpiresAndStops() throws InterruptedException {
        assertCountdown(Competition.athleteTimerTwoMinutes, 1);
    }

    @Test
    public void stopCancelsExpiryAndResumeStillExpires() throws InterruptedException {
        timer.start(Competition.athleteTimerOneMinute);
        timer.stop();
        int remaining = timer.getTimeRemainingAtLastStop();
        assertFalse("stopped countdown expired", expired.await(Competition.athleteTimerTwoMinutes, TimeUnit.MILLISECONDS));
        assertEquals(remaining, timer.liveTimeRemaining());
        timer.start();
        assertTrue("resumed countdown did not expire", expired.await(3, TimeUnit.SECONDS));
        assertEquals(1, expiryCount.get());
    }

    @Test
    public void resetWithoutRestartCancelsPendingExpiry() throws InterruptedException {
        timer.start(Competition.athleteTimerOneMinute);
        timer.setTimeRemaining(Competition.athleteTimerTwoMinutes, false);
        assertFalse("reset countdown expired without a restart", expired.await(Competition.athleteTimerTwoMinutes, TimeUnit.MILLISECONDS));
        assertFalse(timer.isRunning());
        assertEquals(Competition.athleteTimerTwoMinutes, timer.liveTimeRemaining());
    }

    @Test
    public void resetAndRestartCancelsOriginalDeadline() throws InterruptedException {
        timer.start(Competition.athleteTimerOneMinute);
        timer.setTimeRemaining(Competition.athleteTimerTwoMinutes, false);
        timer.start();
        assertFalse("original deadline expired the replacement countdown",
                expired.await(Competition.athleteTimerOneMinute + Competition.athleteTimerFinalWarning / 2,
                    TimeUnit.MILLISECONDS));
        assertTrue(timer.isRunning());
        assertTrue("replacement countdown never expired", expired.await(3, TimeUnit.SECONDS));
        assertEquals(1, expiryCount.get());
    }

    @Test
    public void resetDuringWarningDoesNotCrashOldTimerThread() throws InterruptedException {
        blockFinalWarning();
        timer.start(Competition.athleteTimerFinalWarning);
        awaitBlockedCallback();
        timer.setTimeRemaining(Competition.athleteTimerTwoMinutes, false);
        releaseCallback.countDown();
        callbackThread.get().join(3000);
        assertFalse("cancelled timer thread did not terminate", callbackThread.get().isAlive());
        assertNull("in-flight warning failed after resetting the timer", callbackFailure.get());
        assertEquals(0, expiryCount.get());
    }

    @Test
    public void resetDuringWarningDoesNotScheduleExpiryOnReplacement() throws InterruptedException {
        blockFinalWarning();
        timer.start(Competition.athleteTimerFinalWarning);
        awaitBlockedCallback();
        timer.setTimeRemaining(Competition.athleteTimerTwoMinutes, false);
        timer.start();
        releaseCallback.countDown();
        assertFalse("old warning scheduled an early expiry on the replacement countdown",
                expired.await(Competition.athleteTimerOneMinute, TimeUnit.MILLISECONDS));
        assertTrue("replacement countdown never expired", expired.await(3, TimeUnit.SECONDS));
        assertEquals(1, expiryCount.get());
        assertNull(callbackFailure.get());
    }

    @Test
    public void oldExpiryDoesNotStopReplacementCountdown() throws InterruptedException {
        blockExpiry = true;
        timer.start(Competition.athleteTimerFinalWarning / 2);
        awaitBlockedCallback();
        timer.setTimeRemaining(Competition.athleteTimerTwoMinutes * 10, false);
        timer.start();
        releaseCallback.countDown();
        assertFalse("old expiry stopped the replacement countdown after its grace delay",
                stopped.await(1800, TimeUnit.MILLISECONDS));
        assertTrue(timer.isRunning());
        assertTrue(timer.liveTimeRemaining() > 0);
    }

    private void assertCountdown(int duration, int expectedInitialWarnings) throws InterruptedException {
        timer.start(duration);
        assertTrue("final warning was not emitted", warned.await(3, TimeUnit.SECONDS));
        assertTrue("countdown did not expire", expired.await(3, TimeUnit.SECONDS));
        assertTrue("expired countdown did not stop", stopped.await(3, TimeUnit.SECONDS));
        assertEquals(expectedInitialWarnings, initialWarningCount.get());
        assertEquals(1, finalWarningCount.get());
        assertEquals(1, expiryCount.get());
        assertTrue(timer.getTimeRemainingAtLastStop() <= 0);
    }

    private void blockFinalWarning() {
        blockWarning = true;
    }

    private void blockCallback() {
        Thread worker = Thread.currentThread();
        if (callbackThread.compareAndSet(null, worker)) {
            worker.setUncaughtExceptionHandler((thread, failure) -> callbackFailure.set(failure));
            callbackEntered.countDown();
            try {
                assertTrue("test did not release the timer callback", releaseCallback.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("timer callback interrupted", exception);
            }
        }
    }

    private void awaitBlockedCallback() throws InterruptedException {
        assertTrue("timer callback did not start", callbackEntered.await(3, TimeUnit.SECONDS));
    }
}