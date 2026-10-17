package app.owlcms.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import app.owlcms.displays.DecisionDisplayRules;

public class DecisionDisplayRulesTest {

	@Test
	public void publicVideoAndWarmupToggleControlDecisionSection() {
		assertTrue(DecisionDisplayRules.showDecisionSection(true, false, false));
		assertTrue(DecisionDisplayRules.showDecisionSection(false, true, false));
		assertTrue(DecisionDisplayRules.showDecisionSection(false, false, true));
		assertFalse(DecisionDisplayRules.showDecisionSection(false, false, false));
	}

	@Test
	public void jurySizeControlsScoreboardJuryCircles() {
		assertFalse(DecisionDisplayRules.showJuryDecisionsOnScoreboard(0));
		assertTrue(DecisionDisplayRules.showJuryDecisionsOnScoreboard(3));
		assertTrue(DecisionDisplayRules.showJuryDecisionsOnScoreboard(5));
	}

	@Test
	public void attemptBoardJuryDecisionsRequireJuryAndFeature() {
		assertFalse(DecisionDisplayRules.showJuryDecisionsOnAttemptBoard(0, false));
		assertFalse(DecisionDisplayRules.showJuryDecisionsOnAttemptBoard(0, true));
		assertFalse(DecisionDisplayRules.showJuryDecisionsOnAttemptBoard(3, false));
		assertTrue(DecisionDisplayRules.showJuryDecisionsOnAttemptBoard(3, true));
	}
}
