/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static app.owlcms.tests.AllTests.assertEqualsToReferenceFile;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import app.owlcms.Main;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.LiftOrderReconstruction;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class LiftingOrderReconstructionTest {

    @BeforeClass
    public static void setupTests() {
        Main.injectSuppliers();
        JPAService.init(true, true);
        Config.initConfig();
    }

    @AfterClass
    public static void tearDownTests() {
        JPAService.close();
    }

    // use same data as TwoMinutesRuleTest
    TwoMinutesRuleTest liftSequence = new TwoMinutesRuleTest();

    final Logger logger = (Logger) LoggerFactory.getLogger(LiftingOrderReconstructionTest.class);

    @Test
    public void liftSequence3() throws InterruptedException {
        FieldOfPlay fopState = OwlcmsSession.getFop();
        EventBus fopBus = fopState.getFopEventBus();

        liftSequence.doSequence3(fopState, fopBus, logger);

        LiftOrderReconstruction liftOrderReconstruction = new LiftOrderReconstruction(fopState);
        final String actual = liftOrderReconstruction.shortDump();
        assertEqualsToReferenceFile("/reconstructedSequence3.txt", actual);
    }

    @Test
    public void liftSequence4() throws InterruptedException {
        FieldOfPlay fopState = OwlcmsSession.getFop();
        EventBus fopBus = fopState.getFopEventBus();

        liftSequence.doLiftSequence4(fopState, fopBus, logger);

        LiftOrderReconstruction liftOrderReconstruction = new LiftOrderReconstruction(fopState);
        final String actual = liftOrderReconstruction.shortDump();
        assertEqualsToReferenceFile("/reconstructedSequence4.txt", actual);
    }

    @Before
    public void setupTest() {
        // sets up the OwlcmsSession()
        liftSequence.setupTest();
        logger.setLevel(Level.ERROR);
    }

}
