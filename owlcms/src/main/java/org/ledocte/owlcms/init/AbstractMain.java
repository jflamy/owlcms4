/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.init;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.ledocte.owlcms.data.jpa.DemoData;
import org.ledocte.owlcms.data.jpa.JPAService;
import org.ledocte.owlcms.data.jpa.ProdData;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import ch.qos.logback.classic.Logger;

/**
 * Main.
 */
@WebListener
public class AbstractMain implements ServletContextListener {
	private final static Logger logger = (Logger) LoggerFactory.getLogger(AbstractMain.class);

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
			logger.info("waiting");
		} finally {
			tearDown();
		}
    }

	protected static void logStart(Integer serverPort) throws IOException, ParseException {
		InputStream in = AbstractMain.class.getResourceAsStream("/build.properties"); //$NON-NLS-1$
		Properties buildProps = new Properties();
		buildProps.load(in);
		Properties props = buildProps;
    	String version = props.getProperty("version"); //$NON-NLS-1$
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

	protected static int init() throws IOException, ParseException {
    	// read server.port parameter from -D"server.port"=9999 on java command line
		// this is required for running on Heroku which assigns us the port at run time.
		// default is 8080
    	Integer serverPort = Integer.getInteger("server.port",8080);
    	logStart(serverPort);
    	
		// reads system property (-D on command line)
		boolean demoMode = Boolean.getBoolean("demoMode");
		boolean inMemory = demoMode;
		JPAService.init(inMemory);
		if (demoMode) {
			DemoData.insertInitialData(20, true);
		} else {
			ProdData.insertInitialData(0, false);
		}
		
		// initializes the owlcms singleton
		OwlcmsFactory.getDefaultFOP();
		
		return serverPort;
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