/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.eventbus.Subscribe;

import app.owlcms.Main;
import app.owlcms.components.elements.DecisionElementState;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.MockFieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.monitors.WebSocketEventForwarder;
import app.owlcms.monitors.websocket.ForwarderDecisionState;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;

/**
 * WebSocket representation of the attempt board and adjudication facts. Uses the FOP domain state machine with mock
 * timers; no Vaadin objects.
 */
public class WebSocketAdjudicationForwardingTest {

	private static final boolean W = true;
	private static final boolean R = false;

	private static Group groupA;

	private FieldOfPlay fop;
	private SevenPersonAdjudicationTest.Recorder recorder;

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

	@Before
	public void setupSession() {
		TestData.insertInitialData(5, true);
		JPAService.runInTransaction(em -> {
			groupA = GroupRepository.doFindByName("A", em);
			Group groupB = GroupRepository.doFindByName("B", em);
			Group groupC = GroupRepository.doFindByName("C", em);
			TestData.deleteAllLifters(em);
			TestData.insertSampleLifters(em, 5, groupA, groupB, groupC);
			return null;
		});
		AthleteRepository.resetParticipations(false, true);
		setSwitch(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD, false);
		setSwitch(FeatureSwitch.SHOW_DECISIONS_IMMEDIATELY, false);
		Competition.getCurrent().setVideoPlaybackTechnology(true);

		this.fop = MockFieldOfPlay.create(AthleteRepository.findAll(), new MockCountdownTimer(),
		        new MockCountdownTimer());
		OwlcmsSession.setFop(this.fop);
		this.fop.setPlatform(new Platform());
		this.fop.setJurySize(4);
		startSessionWithOneAthlete();
		this.recorder = new SevenPersonAdjudicationTest.Recorder();
		this.fop.getUiEventBus().register(this.recorder);
	}

	@After
	public void restoreSettings() {
		setSwitch(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD, false);
		setSwitch(FeatureSwitch.AUTOMATIC_BREAK_NO_VPT, false);
		Competition.getCurrent().setVideoPlaybackTechnology(false);
	}

	@Test
	public void forwarderHandlesEveryEventOfTheAttemptBoardStateMachine() {
		for (Method method : DecisionElementState.class.getDeclaredMethods()) {
			if (!method.isAnnotationPresent(Subscribe.class)) {
				continue;
			}
			Class<?> eventType = method.getParameterTypes()[0];
			assertTrue("WebSocketEventForwarder subscribes to " + eventType.getSimpleName(),
			        Arrays.stream(WebSocketEventForwarder.class.getDeclaredMethods())
			                .anyMatch(m -> m.isAnnotationPresent(Subscribe.class) && m.getParameterCount() == 1
			                        && m.getParameterTypes()[0] == eventType));
			assertTrue("ForwarderDecisionState handles " + eventType.getSimpleName(),
			        Arrays.stream(ForwarderDecisionState.class.getMethods())
			                .anyMatch(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0] == eventType));
		}
	}

	@Test
	public void forwarderReportsThePanelReviewAndItsDecision() {
		ForwarderDecisionState tracker = forwarderDecisionState();

		reviewAfterLift(W, R, W);
		assertTrue("the votes that led to the review", awaitState(tracker,
		        s -> "deliberation".equals(s.get("review"))
		                && List.of("white", "red", "white").equals(s.get("refs"))
		                && List.of("red", "red", "red", "red").equals(s.get("jury"))));
		Map<String, Object> state = tracker.toMap();
		assertEquals(Boolean.TRUE, state.get("panelReview"));
		assertEquals(Arrays.asList(true, false, true), state.get("originalRefereeDecisions"));
		assertNotNull(state.get("reviewStartMillis"));

		callForVotes();
		assertTrue("cells cleared, vote open", awaitState(tracker,
		        s -> List.of("empty", "empty", "empty").equals(s.get("refs"))
		                && List.of("empty", "empty", "empty", "empty").equals(s.get("jury"))
		                && Boolean.TRUE.equals(s.get("reviewVoteOpen"))));
		reviewVotes(R, R, R);
		juryVotes(R, R, R);
		assertTrue("who has voted", awaitState(tracker,
		        s -> List.of("voted", "voted", "voted").equals(s.get("refs"))
		                && List.of("voted", "voted", "voted", "empty").equals(s.get("jury"))));
		juryVote(3, W);
		assertTrue("all revealed at the last vote", awaitState(tracker,
		        s -> List.of("red", "red", "red").equals(s.get("refs"))
		                && List.of("red", "red", "red", "white").equals(s.get("jury"))));

		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 17));
		Map<String, Object> decision = ForwarderDecisionState.adjudicationDecision(awaitVerdict(), this.fop);
		assertEquals(Boolean.FALSE, decision.get("decision"));
		assertEquals(Boolean.TRUE, decision.get("reversal"));
		assertEquals(17, decision.get("ruleCode"));
		assertEquals("Snatch", decision.get("liftTypeKey"));
		assertEquals(1, decision.get("attemptNumber"));
		assertEquals(Arrays.asList(true, false, true), decision.get("originalRefereeDecisions"));
		assertEquals(Arrays.asList(false, false, false), decision.get("refereeVotes"));
		assertEquals(Arrays.asList(false, false, false, true), decision.get("juryVotes"));
		assertEquals(Boolean.FALSE, decision.get("chairEntered"));
		assertEquals(Boolean.FALSE, decision.get("speakerEntered"));
		assertTrue("StartLifting clears the forwarded decision before the next clock", awaitState(tracker,
		        s -> Boolean.FALSE.equals(s.get("visible"))
		                && List.of("empty", "empty", "empty").equals(s.get("refs"))
		                && List.of("empty", "empty", "empty", "empty").equals(s.get("jury"))));
	}

	@Test
	public void forwarderKeepsTheRefereesDecisionDuringAJuryVoteWithoutVideo() {
		ForwarderDecisionState tracker = forwarderDecisionState();

		fourMemberDeliberationWithoutVideo(W, W, W);
		juryVotes(R, R, R, R);
		assertTrue("the referees' decision stays, the jury votes are revealed", awaitState(tracker,
		        s -> List.of("white", "white", "white").equals(s.get("refs"))
		                && List.of("red", "red", "red", "red").equals(s.get("jury"))));

		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 5));
		Map<String, Object> decision = ForwarderDecisionState.adjudicationDecision(awaitVerdict(), this.fop);
		assertEquals("the referees do not vote again", List.of(), decision.get("refereeVotes"));
		assertEquals(Arrays.asList(false, false, false, false), decision.get("juryVotes"));
		assertEquals(Arrays.asList(true, true, true), decision.get("originalRefereeDecisions"));
		assertEquals(Boolean.TRUE, decision.get("reversal"));
	}

	@Test
	public void forwarderReportsNoVotesAfterAChairOverride() {
		ForwarderDecisionState tracker = forwarderDecisionState();

		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(R, R, R);
		juryVotes(W, W);
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 2, true));
		Map<String, Object> decision = ForwarderDecisionState.adjudicationDecision(awaitVerdict(), this.fop);
		assertEquals(Boolean.TRUE, decision.get("chairEntered"));
		assertEquals("the Chair's decision clears the votes", Arrays.asList(null, null, null, null),
		        decision.get("juryVotes"));
		assertTrue("the cells are cleared", awaitState(tracker,
		        s -> Boolean.FALSE.equals(s.get("visible"))
		                && List.of("empty", "empty", "empty").equals(s.get("refs"))));
	}

	/** Four-member jury without VPT: a lift followed by a manual jury deliberation. */
	private void fourMemberDeliberationWithoutVideo(boolean... refereeVotes) {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		assertEquals(4, this.fop.getJurySize());
		lift(refereeVotes);
		post(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, new Object()));
		assertEquals(FOPState.BREAK, this.fop.getState());
	}

	/** The referees' initial decision on the current athlete's attempt. */
	private void lift(boolean... refereeVotes) {
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals(FOPState.TIME_RUNNING, this.fop.getState());
		post(new FOPEvent.TimeStopped(new Object()));
		for (int i = 0; i < refereeVotes.length; i++) {
			post(new FOPEvent.DecisionUpdate(new Object(), i, refereeVotes[i]));
		}
		assertNotNull("the lift was recorded", this.fop.getAthleteUnderReview());
	}

	private void reviewAfterLift(boolean... refereeVotes) {
		lift(refereeVotes);
		juryVotes(R, R, R, R);
		assertEquals(FOPState.BREAK, this.fop.getState());
		assertEquals(BreakType.JURY, this.fop.getBreakType());
		assertTrue(this.fop.isPanelReview());
	}

	private void juryVotes(boolean... votes) {
		for (int i = 0; i < votes.length; i++) {
			juryVote(i, votes[i]);
		}
	}

	private void juryVote(int juror, boolean vote) {
		post(new FOPEvent.JuryMemberDecisionUpdate(new Object(), juror, vote));
	}

	private void reviewVotes(boolean... votes) {
		for (int i = 0; i < votes.length; i++) {
			post(new FOPEvent.DecisionUpdate(new Object(), i, votes[i]));
		}
	}

	private void callForVotes() {
		post(new FOPEvent.PanelVoteCalled(new Object()));
	}

	private void post(FOPEvent e) {
		this.fop.fopEventPost(e);
	}

	private UIEvent.JuryNotification awaitVerdict() {
		assertTrue("jury decision notification",
		        this.recorder.await(this.recorder.notifications,
		                n -> n.getDeliberationEventType() == JuryDeliberationEventType.GOOD_LIFT
		                        || n.getDeliberationEventType() == JuryDeliberationEventType.BAD_LIFT));
		return this.recorder.notifications.stream()
		        .filter(n -> n.getDeliberationEventType() == JuryDeliberationEventType.GOOD_LIFT
		                || n.getDeliberationEventType() == JuryDeliberationEventType.BAD_LIFT)
		        .findFirst().orElseThrow();
	}

	private void startSessionWithOneAthlete() {
		this.fop.testBefore();
		this.fop.loadGroup(groupA, this, true);
		this.fop.testStartLifting(groupA, this.fop);
		List<Athlete> athletes = this.fop.getDisplayOrder();
		Athlete only = athletes.get(0);
		JPAService.runInTransaction(em -> {
			AthleteSorter.testAssignStartNumbers(athletes);
			only.setSnatch1Declaration(Integer.toString(60));
			only.setCleanJerk1Declaration(Integer.toString(80));
			em.merge(only);
			for (int i = 1; i < athletes.size(); i++) {
				Athlete other = athletes.get(i);
				other.setGroup(null);
				em.merge(other);
			}
			em.flush();
			return null;
		});
		this.fop.loadGroup(groupA, this, true);
		this.fop.fopEventPost(new FOPEvent.SwitchGroup(groupA, this));
		this.fop.fopEventPost(new FOPEvent.StartLifting(this));
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertEquals(0, this.fop.getCurAthlete().getAttemptsDone().intValue());
	}

	private static void setSwitch(FeatureSwitch featureSwitch, boolean enabled) {
		Config config = Config.getCurrent();
		config.setFeatureSwitchValue(featureSwitch, enabled);
		Config.setCurrent(config);
	}

	private ForwarderDecisionState forwarderDecisionState() {
		ForwarderDecisionState tracker = new ForwarderDecisionState();
		tracker.setFop(this.fop);
		this.fop.getUiEventBus().register(new ForwarderAdapter(tracker));
		return tracker;
	}

	private boolean awaitState(ForwarderDecisionState tracker, Predicate<Map<String, Object>> condition) {
		long deadline = System.currentTimeMillis() + 2_000;
		while (System.currentTimeMillis() < deadline) {
			if (condition.test(tracker.toMap())) {
				return true;
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return false;
	}

	/** Passes UI events to the forwarder's decision state, as WebSocketEventForwarder does. */
	public static final class ForwarderAdapter {
		private final ForwarderDecisionState tracker;

		ForwarderAdapter(ForwarderDecisionState tracker) {
			this.tracker = tracker;
		}

		@Subscribe
		public void decision(UIEvent.Decision e) {
			this.tracker.onDecision(e);
		}

		@Subscribe
		public void initialDecision(UIEvent.InitialDecision e) {
			this.tracker.onInitialDecision(e);
		}

		@Subscribe
		public void downSignal(UIEvent.DownSignal e) {
			this.tracker.onDownSignal(e);
		}

		@Subscribe
		public void decisionReset(UIEvent.DecisionReset e) {
			this.tracker.onDecisionReset(e);
		}

		@Subscribe
		public void refereeUpdate(UIEvent.RefereeUpdate e) {
			this.tracker.onRefereeUpdate(e);
		}

		@Subscribe
		public void resetOnNewClock(UIEvent.ResetOnNewClock e) {
			this.tracker.onResetOnNewClock(e);
		}

		@Subscribe
		public void breakStarted(UIEvent.BreakStarted e) {
			this.tracker.onBreakStarted(e);
		}

		@Subscribe
		public void startTime(UIEvent.StartTime e) {
			this.tracker.onStartTime(e);
		}

		@Subscribe
		public void stopTime(UIEvent.StopTime e) {
			this.tracker.onStopTime(e);
		}

		@Subscribe
		public void juryNotification(UIEvent.JuryNotification e) {
			this.tracker.onJuryNotification(e);
		}

		@Subscribe
		public void juryUpdate(UIEvent.JuryUpdate e) {
			this.tracker.onJuryUpdate(e);
		}

		@Subscribe
		public void panelVoteCalled(UIEvent.PanelVoteCalled e) {
			this.tracker.onPanelVoteCalled(e);
		}

		@Subscribe
		public void panelVoteUpdate(UIEvent.PanelVoteUpdate e) {
			this.tracker.onPanelVoteUpdate(e);
		}

		@Subscribe
		public void juryDisplayReset(UIEvent.JuryDisplayReset e) {
			this.tracker.onJuryDisplayReset(e);
		}

		@Subscribe
		public void startLifting(UIEvent.StartLifting e) {
			this.tracker.onStartLifting();
		}
	}
}
