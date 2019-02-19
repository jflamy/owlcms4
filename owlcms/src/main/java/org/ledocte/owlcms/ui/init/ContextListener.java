/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.ui.init;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.ledocte.owlcms.data.jpa.DemoData;
import org.ledocte.owlcms.data.jpa.JPAService;
import org.ledocte.owlcms.data.jpa.ProdData;
import org.ledocte.owlcms.data.jpa.TestData;

/**
 * The listener interface for receiving context events. The methods will be
 * called when the servlet container is started and closed.
 */
@WebListener
public class ContextListener implements ServletContextListener {

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.
	 * ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		JPAService.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.
	 * ServletContextEvent)
	 */
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		// reads system property (-D on command line)
		boolean testMode = Boolean.getBoolean("testMode");
		boolean demoMode = Boolean.getBoolean("demoMode");
		boolean inMemory = testMode || demoMode;
		JPAService.init(inMemory);
		if (testMode) {
			TestData.insertInitialData(5, true);
		} else if (demoMode) {
			DemoData.insertInitialData(12, true);
		} else {
			ProdData.insertInitialData(0, false);
		}
	}
}