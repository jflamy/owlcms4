/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.tests;

import static app.owlcms.tests.AllTests.assertEqualsToReferenceFile;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.utils.DebugUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class TwoMinutesRuleTest {
	private static Level LoggerLevel = Level.INFO;
	private static Group gA;
	private static Group gB;
	private static Group gC;
	final Logger logger = (Logger) LoggerFactory.getLogger(TwoMinutesRuleTest.class);
	private List<Athlete> athletes;

	@BeforeClass
	public static void setupTests() {
		JPAService.init(true, true);
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	@Before
	public void setupTest() {
		logger.setLevel(LoggerLevel);
		
		TestData.insertInitialData(5, true);
		JPAService.runInTransaction((em) -> {
			gA = GroupRepository.doFindByName("A",em);
			gB = GroupRepository.doFindByName("B", em);
			gC = GroupRepository.doFindByName("C", em);
			TestData.deleteAllLifters(em);
			TestData.insertSampleLifters(em, 5, gA, gB, gC);
			return null;
		});
		athletes = AthleteRepository.findAll();
	}

	@Test
	public void initialCheck() {
		final String resName = "/initialCheck.txt";
		AthleteSorter.assignLotNumbers(athletes);
		AthleteSorter.assignStartNumbers(athletes);

		Collections.shuffle(athletes);

		List<Athlete> sorted = AthleteSorter.liftingOrderCopy(athletes);
		final String actual = DebugUtils.shortDump(sorted);
		assertEqualsToReferenceFile(resName, actual);
	}

	@Test
	public void liftSequence3() throws InterruptedException {
		AthleteSorter.assignLotNumbers(athletes);

		final Athlete schneiderF = athletes.get(0);
		final Athlete simpsonR = athletes.get(1);

		// simulate initial declaration at weigh-in
		schneiderF.setSnatch1Declaration(Integer.toString(60));
		simpsonR.setSnatch1Declaration(Integer.toString(60));
		schneiderF.setCleanJerk1Declaration(Integer.toString(80));
		simpsonR.setCleanJerk1Declaration(Integer.toString(82));

		// hide non-athletes
		AthleteSorter.liftingOrder(athletes);
		final int size = athletes.size();
		for (int i = 2; i < size; i++)
			athletes.remove(2);

		FieldOfPlay fopState = new FieldOfPlay(athletes, new MockCountdownTimer(), new MockCountdownTimer(), true);
		fopState.getLogger().setLevel(LoggerLevel);
		EventBus fopBus = fopState.getFopEventBus();

		// competition start
		assertEquals(60000, fopState.getTimeAllowed());
		logger.debug("\n{}", DebugUtils.shortDump(fopState.getLiftingOrder()));

		// schneiderF is called with initial weight
		Athlete curLifter = fopState.getCurAthlete();
		Athlete previousLifter = fopState.getPreviousAthlete();
		assertEquals(schneiderF, curLifter);
		assertEquals(null, previousLifter);
		successfulLift(fopBus, curLifter);

		logger.debug("\n{}", DebugUtils.shortDump(fopState.getLiftingOrder()));
		// first is now simpsonR ; he has declared 60kg
		curLifter = fopState.getCurAthlete();
		previousLifter = fopState.getPreviousAthlete();
		assertEquals(simpsonR, curLifter);
		assertEquals(schneiderF, previousLifter);
		assertEquals(60000, fopState.getTimeAllowed());

		// ... but simpsonR changes to 62 before being called by announcer (time not
		// restarted)
		declaration(curLifter, "62", fopBus);
		logger.info("declaration by {}: {}", curLifter, "62");
		logger.debug("\n{}", DebugUtils.shortDump(fopState.getLiftingOrder()));

		// so now schneider should be back on top at 61, with two minutes because
		// there was no time started.
		curLifter = fopState.getCurAthlete();
		previousLifter = fopState.getPreviousAthlete();
		assertEquals(schneiderF, curLifter);
		assertEquals(schneiderF, previousLifter);
		assertEquals(120000, fopState.getTimeAllowed());
		successfulLift(fopBus, curLifter);

		// schneider has lifted 62, is now simpson's turn, he should NOT have 2
		// minutes
		curLifter = fopState.getCurAthlete();
		assertEquals(simpsonR, curLifter);
		assertEquals(60000, fopState.getTimeAllowed());
		failedLift(fopBus, curLifter);

		// still simpson because 2nd try and schneider is at 3rd.
		curLifter = fopState.getCurAthlete();
		assertEquals(simpsonR, curLifter);
		assertEquals(120000, fopState.getTimeAllowed());
		// simpson is called again with two minutes
		logger.info("calling lifter: {}", curLifter);
		fopBus.post(new FOPEvent.TimeStarted(null)); // this starts logical time
		assertEquals(FOPState.TIME_RUNNING, fopState.getState());
		
		// but simpson now asks for more; weight change should stop clock.
		declaration(curLifter, "67", fopBus);
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, fopState.getState());
		logger.info("declaration by {}: {}", curLifter, "67");

		// schneider does not get 2 minutes.
		curLifter = fopState.getCurAthlete();
		assertEquals(schneiderF, curLifter);
		assertEquals(60000, fopState.getTimeAllowed());
		// schneider is called
		logger.info("calling lifter: {}", curLifter);
		fopBus.post(new FOPEvent.TimeStarted(null)); // this starts logical time
		assertEquals(FOPState.TIME_RUNNING, fopState.getState());
		// but asks for more weight -- the following stops time.
		declaration(curLifter, "65", fopBus);
		assertEquals(FOPState.TIME_STOPPED, fopState.getState());
		int remainingTime = fopState.getAthleteTimer()
			.getTimeRemaining();

		// at this point, if schneider is called again, he should get the remaining
		// time.
		curLifter = fopState.getCurAthlete();
		assertEquals(schneiderF, curLifter);
		assertEquals(remainingTime, fopState.getTimeAllowed());
	}

	@Test
	public void liftSequence4() throws InterruptedException {
		AthleteSorter.assignLotNumbers(athletes);

		final Athlete schneiderF = athletes.get(0);
		final Athlete simpsonR = athletes.get(1);

		// simulate initial declaration at weigh-in
		schneiderF.setSnatch1Declaration(Integer.toString(60));
		simpsonR.setSnatch1Declaration(Integer.toString(65));
		schneiderF.setCleanJerk1Declaration(Integer.toString(80));
		simpsonR.setCleanJerk1Declaration(Integer.toString(85));

		// hide non-athletes
		AthleteSorter.liftingOrder(athletes);
		final int size = athletes.size();
		for (int i = 2; i < size; i++)
			athletes.remove(2);
		FieldOfPlay fopState = new FieldOfPlay(athletes, new MockCountdownTimer(), new MockCountdownTimer(), true);
		EventBus fopBus = fopState.getFopEventBus();

		// competition start
		assertEquals(60000, fopState.getTimeAllowed());
		assertEquals(60000, fopState.getTimeAllowed());

		// schneiderF snatch1
		Athlete curLifter = fopState.getCurAthlete();
		assertEquals(schneiderF, curLifter);
		successfulLift(fopBus, curLifter);

		// schneiderF snatch2
		curLifter = fopState.getCurAthlete();
		assertEquals(schneiderF, curLifter);
		assertEquals(120000, fopState.getTimeAllowed());
		successfulLift(fopBus, curLifter);

		// schneiderF snatch3
		curLifter = fopState.getCurAthlete();
		assertEquals(schneiderF, curLifter);
		assertEquals(120000, fopState.getTimeAllowed());
		successfulLift(fopBus, curLifter);

		// simpsonR snatch1
		curLifter = fopState.getCurAthlete();
		assertEquals(simpsonR, curLifter);
		assertEquals(60000, fopState.getTimeAllowed());
		successfulLift(fopBus, curLifter);

		// simpsonR snatch2
		curLifter = fopState.getCurAthlete();
		assertEquals(simpsonR, curLifter);
		assertEquals(120000, fopState.getTimeAllowed());
		successfulLift(fopBus, curLifter);

		// simpsonR snatch3
		curLifter = fopState.getCurAthlete();
		assertEquals(simpsonR, curLifter);
		assertEquals(120000, fopState.getTimeAllowed());
		successfulLift(fopBus, curLifter);

		// schneiderF cj1
		curLifter = fopState.getCurAthlete();
		assertEquals(schneiderF, curLifter);
		assertEquals(60000, fopState.getTimeAllowed());
		successfulLift(fopBus, curLifter);

		// schneiderF cj2
		curLifter = fopState.getCurAthlete();
		assertEquals(schneiderF, curLifter);
		assertEquals(schneiderF, fopState.getPreviousAthlete());
		assertEquals(120000, fopState.getTimeAllowed());
		successfulLift(fopBus, curLifter);

		// schneiderF cj3
		curLifter = fopState.getCurAthlete();
		assertEquals(schneiderF, curLifter);
		assertEquals(120000, fopState.getTimeAllowed());
		successfulLift(fopBus, curLifter);

		// simpsonR cj1
		curLifter = fopState.getCurAthlete();
		assertEquals(simpsonR, curLifter);
		assertEquals(60000, fopState.getTimeAllowed());
		successfulLift(fopBus, curLifter);

		// simpsonR cj2
		curLifter = fopState.getCurAthlete();
		assertEquals(simpsonR, curLifter);
		assertEquals(120000, fopState.getTimeAllowed());
		successfulLift(fopBus, curLifter);

		// simpsonR cj3
		curLifter = fopState.getCurAthlete();
		assertEquals(simpsonR, curLifter);
		assertEquals(120000, fopState.getTimeAllowed());
		successfulLift(fopBus, curLifter);
	}

	/**
	 * @param lifter
	 * @param weight
	 * @param eventBus
	 */
	private void declaration(final Athlete lifter, final String weight, EventBus eventBus) {
		switch (lifter.getAttemptsDone() + 1) {
		case 1:
			lifter.setSnatch1Declaration(weight);
			break;
		case 2:
			lifter.setSnatch2Declaration(weight);
			break;
		case 3:
			lifter.setSnatch3Declaration(weight);
			break;
		case 4:
			lifter.setCleanJerk1Declaration(weight);
			break;
		case 5:
			lifter.setCleanJerk2Declaration(weight);
			break;
		case 6:
			lifter.setCleanJerk3Declaration(weight);
			break;
		}
		eventBus.post(new FOPEvent.WeightChange(this, lifter));
	}


	private void failedLift(EventBus fopBus, Athlete curLifter) {
		logger.debug("calling lifter: {}", curLifter);
		fopBus.post(new FOPEvent.TimeStarted(null));
		fopBus.post(new FOPEvent.DownSignal(null));
		fopBus.post(new FOPEvent.DecisionFullUpdate(this, curLifter, false, false, false, 0, 0, 0));
		logger.debug("failed lift for {}", curLifter);
		fopBus.post(new FOPEvent.DecisionReset(null));
	}

	private void successfulLift(EventBus fopBus, Athlete curLifter) {
		logger.debug("calling lifter: {}", curLifter);
		fopBus.post(new FOPEvent.TimeStarted(null));
		fopBus.post(new FOPEvent.DownSignal(null));
		fopBus.post(new FOPEvent.DecisionFullUpdate(this, curLifter, true, true, true, 0, 0, 0));
		logger.debug("successful lift for {}", curLifter);
		fopBus.post(new FOPEvent.DecisionReset(null));
	}

}
