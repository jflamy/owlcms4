/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms;

import java.io.IOException;
import java.text.ParseException;

import org.ledocte.owlcms.data.jpa.DemoData;
import org.ledocte.owlcms.data.jpa.JPAService;
import org.ledocte.owlcms.data.jpa.ProdData;
import org.ledocte.owlcms.init.AbstractMain;
import org.ledocte.owlcms.init.OwlcmsFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * Main.
 */
public class Main extends AbstractMain {
	@SuppressWarnings("unused")
	private final static Logger logger = (Logger) LoggerFactory.getLogger(Main.class);

	/**
	 * Prepare owlcms
	 * 
	 * Reads configuration options, injects data, initializes singletons and configurations. The
	 * embedded web server can then be started.
	 * 
	 * Sample command line to run on port 80 and in demo mode (automatically generated fake data,
	 * in-memory database)
	 * 
	 * <code><pre>java -D"server.port"=80 -DdemoMode=true -jar owlcms-4.0.1-SNAPSHOT.jar org.ledocte.owlcms.Main</pre></code>
	 * 
	 * @return the server port on which we want to run
	 * @throws IOException
	 * @throws ParseException
	 */
	protected static int setup() throws IOException, ParseException {
		// read server.port parameter from -D"server.port"=9999 on java command line
		// this is required for running on Heroku which assigns us the port at run time.
		// default is 8080
		Integer serverPort = Integer.getInteger("server.port", 8080);
		logStart(serverPort);

		// reads system property (-D on command line)
		boolean demoMode = Boolean.getBoolean("demoMode");
		boolean inMemory = demoMode;
		
		JPAService.init(inMemory);
		if (demoMode) {
			// H2 in-memory mode
			DemoData.insertInitialData(20, true);
		} else {
			// H2 embedded mode
			// TODO: use Postgres on Heroku
			ProdData.insertInitialData(0, false);
		}

		// initializes the owlcms singleton
		OwlcmsFactory.getDefaultFOP();

		return serverPort;
	}

}