/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.init;

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

import org.slf4j.LoggerFactory;

import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Logger;

/**
 * Main.
 */
@WebListener
public class AbstractMain implements ServletContextListener {
	private final static Logger logger = (Logger) LoggerFactory.getLogger(AbstractMain.class);

	protected static void logStart(Integer serverPort) throws IOException, ParseException {
		InputStream in = AbstractMain.class.getResourceAsStream("/build.properties"); //$NON-NLS-1$
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