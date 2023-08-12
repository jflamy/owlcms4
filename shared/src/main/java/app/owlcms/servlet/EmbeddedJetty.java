package app.owlcms.servlet;

import java.util.concurrent.CountDownLatch;

import ch.qos.logback.classic.Logger;

public class EmbeddedJetty extends VaadinBoot {
	
    private static Logger startLogger;
	private Runnable initConfig;
	private Runnable initData;
	private int serverPort;
    private CountDownLatch latch;

	public EmbeddedJetty(CountDownLatch countDownLatch) {
        this.setLatch(countDownLatch);
	}

	public EmbeddedJetty setInitConfig(Runnable initConfig) {
        this.initConfig = initConfig;
        return this;
    }

    public EmbeddedJetty setInitData(Runnable initData) {
        this.initData = initData;
        return this;
    }

    public EmbeddedJetty setStartLogger(Logger startLogger) {
        EmbeddedJetty.startLogger = startLogger;
        return this;
    }

	public void run(Integer serverPort, String string) throws Exception {
        initConfig.run();
        initData.run();
        this.setPort(serverPort);
		this.run();
	}
	
	@Override
	protected void onStarted() {
        getLatch().countDown();
		startLogger.info("started on port {}", serverPort);
	}

	public CountDownLatch getLatch() {
		return latch;
	}

	public void setLatch(CountDownLatch latch) {
		this.latch = latch;
	}
	

}
