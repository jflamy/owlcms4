package app.owlcms.utils;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class DelayTimerTest {

	@Test
	public void failingTaskIsLoggedAndTheTimerKeepsRunning() throws Exception {
		Logger logger = (Logger) LoggerFactory.getLogger(DelayTimer.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		try {
			DelayTimer timer = new DelayTimer();
			CountDownLatch failed = new CountDownLatch(1);
			timer.schedule(() -> {
				failed.countDown();
				throw new IllegalArgumentException("A live lift requires a nonzero result");
			}, 0);
			assertTrue(failed.await(5, TimeUnit.SECONDS));

			CountDownLatch next = new CountDownLatch(1);
			timer.schedule(next::countDown, 0);
			assertTrue("the timer thread survived the failure", next.await(5, TimeUnit.SECONDS));
			assertTrue(appender.list.stream()
			        .anyMatch(e -> e.getFormattedMessage().contains("A live lift requires a nonzero result")));
		} finally {
			logger.detachAppender(appender);
		}
	}
}
