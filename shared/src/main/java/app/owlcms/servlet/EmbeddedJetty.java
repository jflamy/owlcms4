package app.owlcms.servlet;

import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.slf4j.LoggerFactory;

import com.vaadin.open.Open;

import ch.qos.logback.classic.Logger;

public class EmbeddedJetty extends com.github.mvysny.vaadinboot.VaadinBoot {

	private static Logger startLogger = (Logger) LoggerFactory.getLogger(EmbeddedJetty.class);
	private Runnable initConfig;
	private Runnable initData;
	private CountDownLatch latch;

	public EmbeddedJetty(CountDownLatch countDownLatch, String appName) {
		this.setLatch(countDownLatch);
		//this.setAppName(appName);
	}

	public CountDownLatch getLatch() {
		return latch;
	}

	public void run(Integer serverPort, String string) throws Exception {
		initConfig.run();
		initData.run();
		this.setPort(serverPort);
		this.run();
	}

	public EmbeddedJetty setInitConfig(Runnable initConfig) {
		this.initConfig = initConfig;
		return this;
	}

	public EmbeddedJetty setInitData(Runnable initData) {
		this.initData = initData;
		return this;
	}

	public void setLatch(CountDownLatch latch) {
		this.latch = latch;
	}

	public EmbeddedJetty setStartLogger(Logger startLogger) {
		EmbeddedJetty.startLogger = startLogger;
		return this;
	}

	@Override
	public void onStarted(WebAppContext c) {
		getLatch().countDown();
		startLogger.info("started on port {}", this.getPort());
	}

    @Override
	public void run() throws Exception {
        start();

        // this gets called both when CTRL+C is pressed, and when main() terminates.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop("Shutdown hook called, shutting down")));
        startLogger.info("Press CTRL+C to shutdown");

        Open.open(getServerURL());

    }

}
