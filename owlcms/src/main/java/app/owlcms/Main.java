/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.jpa.DemoData;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.ProdData;
import app.owlcms.init.AbstractMain;
import app.owlcms.init.EmbeddedJetty;
import app.owlcms.init.OwlcmsFactory;
import ch.qos.logback.classic.Logger;

/**
 * Main.
 */
public class Main extends AbstractMain {
	
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
	protected static int init() throws IOException, ParseException {
		// read server.port parameter from -D"server.port"=9999 on java command line
		// this is required for running on Heroku which assigns us the port at run time.
		// default is 8080
		Integer serverPort = Integer.getInteger("server.port", 8080);
		logStart(serverPort);

		// reads system property (-D on command line)
		boolean demoMode = Boolean.getBoolean("demoMode");
		boolean devMode = Boolean.getBoolean("devMode");
		
		boolean inMemory = demoMode;
		JPAService.init(inMemory);
		
		if (demoMode) {
			DemoData.insertInitialData(20, true);
		} else {
			List<Competition> allCompetitions = CompetitionRepository.findAll();
			if (allCompetitions.isEmpty()) {
				if (devMode) {
					DemoData.insertInitialData(20, true);
				} else {
					ProdData.insertInitialData(0, false);
				}
			} else {
				logger.info("database not empty {}",allCompetitions.get(0).getCompetitionName());
			}
		}

		// initializes the owlcms singleton
		OwlcmsFactory.getDefaultFOP();

		return serverPort;
	}

}