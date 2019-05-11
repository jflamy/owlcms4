/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.jpa.DemoData;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.ProdData;
import app.owlcms.init.EmbeddedJetty;
import app.owlcms.init.OwlcmsFactory;
import ch.qos.logback.classic.Logger;

/**
 * Main.
 */
public class Main implements ServletContextListener {
	
	private final static Logger logger = (Logger) LoggerFactory.getLogger(Main.class);
	
    /**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws Exception the exception
	 */
	public static void main(String... args) throws Exception {
		// Redirect java.util.logging logs to SLF4J
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		
		int serverPort = init();
        try {
			new EmbeddedJetty().run(serverPort, "/"); //$NON-NLS-1$
		} finally {
			tearDown();
		}
    }
	
	/**
	 * Prepare owlcms
	 * 
	 * Reads configuration options, injects data, initializes singletons and configurations. The
	 * embedded web server can then be started.
	 * 
	 * Sample command line to run on port 80 and in demo mode (automatically generated fake data,
	 * in-memory database)
	 * 
	 * <code><pre>java -D"server.port"=80 -DdemoMode=true -jar owlcms-4.0.1-SNAPSHOT.jar app.owlcms.Main</pre></code>
	 * 
	 * @return the server port on which we want to run
	 * @throws IOException
	 * @throws ParseException
	 */
	@SuppressWarnings("unused")
	protected static int init() throws IOException, ParseException {
		System.setProperty("java.net.preferIPv4Stack", "true"); 
		
		// read server.port parameter from -D"server.port"=9999 on java command line
		// this is required for running on Heroku which assigns us the port at run time.
		// default is 8080
		Integer serverPort = Integer.getInteger("port", 8080);
		logStart(serverPort);

		// reads system property (-D on command line)
		boolean demoMode = Boolean.getBoolean("demoMode"); // run in memory with large dummy data, in memory, reset first
		boolean memoryMode = Boolean.getBoolean("memoryMode"); // run in memory
		boolean resetMode = Boolean.getBoolean("resetMode"); // drop the schema first
		boolean devMode = Boolean.getBoolean("devMode"); // load large dummy data if empty, do not reset unless resetMode, persistent unless memoryMode also
		boolean testMode = Boolean.getBoolean("testMode"); // load small dummy data if empty, do not reset unless resetMode, persistent unless memoryMode
		boolean masters = Boolean.getBoolean("masters");
		
		initializeLibraries();
		
		JPAService.init(demoMode || memoryMode);
		injectData(demoMode, devMode, testMode, masters);

		// initializes the owlcms singleton
		OwlcmsFactory.getDefaultFOP();

		return serverPort;
	}

	private static void initializeLibraries() {
		// misc initializations
		ConvertUtils.register(new DateConverter(null), java.util.Date.class);
		ConvertUtils.register(new DateConverter(null), java.sql.Date.class);
	}

	private static void injectData(boolean demoMode, boolean devMode, boolean testMode, boolean masters) {
		if (testMode) {
			DemoData.insertInitialData(2, masters);
		} else if (demoMode) {
			DemoData.insertInitialData(20, masters);
		} else {
			List<Competition> allCompetitions = CompetitionRepository.findAll();
			if (allCompetitions.isEmpty()) {
				if (devMode) {
					DemoData.insertInitialData(20, masters);
				} else {
					ProdData.insertInitialData(0);
				}
			} else {
				logger.info("database not empty: {}",allCompetitions.get(0).getCompetitionName());
			}
		}
	}
	
	protected static void logStart(Integer serverPort) throws IOException, ParseException {
		InputStream in = Main.class.getResourceAsStream("/build.properties"); //$NON-NLS-1$
		Properties props = new Properties();
		props.load(in);
    	String version = props.getProperty("version"); //$NON-NLS-1$
    	OwlcmsFactory.setVersion(version);
		String buildTimestamp = props.getProperty("buildTimestamp"); //$NON-NLS-1$
		//String buildZone = props.getProperty("buildZone");
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm"); //$NON-NLS-1$
		format.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
		Date date = format.parse(buildTimestamp);
		//format.setTimeZone(TimeZone.getTimeZone(buildZone));
		SimpleDateFormat homeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String homeTimestamp = homeFormat.format(date);
		logger.info("owlcms {} (built {})",version,homeTimestamp);
	}
	
	protected static void tearDown() {
		JPAService.close();
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		tearDown();
		logger.info("owlcms end.");
	}

}