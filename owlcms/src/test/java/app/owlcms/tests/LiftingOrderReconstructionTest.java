/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static app.owlcms.tests.AllTests.assertEqualsToReferenceFile;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.LiftOrderReconstruction;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class LiftingOrderReconstructionTest {
    private static Level LoggerLevel = Level.INFO;

    @BeforeClass
    public static void setupTests() {
        JPAService.init(true, true);
    }

    @AfterClass
    public static void tearDownTests() {
        JPAService.close();
    }

    final Logger logger = (Logger) LoggerFactory.getLogger(LiftingOrderReconstructionTest.class);

    TwoMinutesRuleTest liftSequence = new TwoMinutesRuleTest();

    private List<Athlete> athletes;

    @Test
    public void liftSequence3() throws InterruptedException {
        FieldOfPlay fopState = new FieldOfPlay(athletes, new MockCountdownTimer(), new MockCountdownTimer(), true);
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LoggerLevel);
        EventBus fopBus = fopState.getFopEventBus();
        
        logger.setLevel(Level.ERROR);
        fopState.getLogger().setLevel(Level.ERROR);
        liftSequence.doSequence3(fopState, fopBus, logger);
        
        LiftOrderReconstruction liftOrderReconstruction = new LiftOrderReconstruction(fopState);
        final String actual = liftOrderReconstruction.shortDump();
        assertEqualsToReferenceFile("/reconstructedSequence3.txt", actual);
    }

    @Test
    public void liftSequence4() throws InterruptedException {
        FieldOfPlay fopState = new FieldOfPlay(athletes, new MockCountdownTimer(), new MockCountdownTimer(), true);
        OwlcmsSession.setFop(fopState);
        EventBus fopBus = fopState.getFopEventBus();
        
        logger.setLevel(Level.ERROR);
        fopState.getLogger().setLevel(Level.ERROR);
        liftSequence.doLiftSequence4(fopState, fopBus, logger);
        
        LiftOrderReconstruction liftOrderReconstruction = new LiftOrderReconstruction(fopState);
        final String actual = liftOrderReconstruction.shortDump();
        assertEqualsToReferenceFile("/reconstructedSequence4.txt", actual);
    }

    @Before
    public void setupTest() {
        liftSequence.setupTest();
        athletes = liftSequence.getAthletes();
    }

}
