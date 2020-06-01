/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms;

import java.util.concurrent.CountDownLatch;

import org.slf4j.LoggerFactory;

import app.owlcms.init.EmbeddedJetty;
import app.owlcms.simulation.FOPSimulator;
import ch.qos.logback.classic.Logger;

/**
 * Modified Main class to run a simulated competition with all the groups and athletes present in the database.
 * 
 * To use:
 * - run owlcms {@link Main} normally, open all the browser windows you wish
 * - stop owlcms
 * - start this class
 * - the browsers will notice that the first one got killed and reconnect to the new instance
 * - this class will run through all the groups, making referee decisions at random (70% of the lifts should be good)
 * 
 * @author Jean-François Lamy
 *
 */
public class Simulation extends Main {

    public final static Logger logger = (Logger) LoggerFactory.getLogger(Simulation.class);


    /**
     * The main method.
     *
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(String... args) throws Exception {

        try {
            init();
            
            // in order to wait for server to have started
            CountDownLatch latch = new CountDownLatch(1); 
            Thread server = new Thread( () -> {
                try {
                    EmbeddedJetty embeddedJetty = new EmbeddedJetty(latch);
                    embeddedJetty.run(serverPort, "/");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            server.start();
            
            // wait for server to be ready enough
            latch.await();
            FOPSimulator.runSimulation();
            
            // wait for server to exit
            server.join();
        } finally {
            tearDown();
        }
    }

}