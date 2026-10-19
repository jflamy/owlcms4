package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import app.owlcms.fieldofplay.AdjudicationPanel;

public class AdjudicationPanelTest {

    private static final Boolean W = Boolean.TRUE;
    private static final Boolean R = Boolean.FALSE;

    private static Boolean[] votes(Boolean... v) {
        return v;
    }

    @Test
    public void emptyPanelHasNoResult() {
        AdjudicationPanel panel = new AdjudicationPanel(null, null);
        assertEquals(0, panel.votesReceived());
        assertFalse(panel.isComplete());
        assertFalse(panel.isJuryComplete());
        assertNull(panel.majority());
        assertFalse(panel.differsFrom(Boolean.TRUE));
    }

    @Test
    public void incompletePanelHasNoEarlyResult() {
        // 3 referees white, 3 of 4 jurors red: the fourth juror decides, nothing may be revealed yet
        AdjudicationPanel panel = new AdjudicationPanel(votes(W, W, W), votes(R, R, R, null));
        assertEquals(6, panel.votesReceived());
        assertEquals(3, panel.whites());
        assertEquals(3, panel.reds());
        assertFalse(panel.isComplete());
        assertFalse(panel.isJuryComplete());
        assertNull(panel.majority());
        assertFalse("no early review", panel.differsFrom(Boolean.TRUE));
    }

    @Test
    public void refereesIncompleteJuryCompleteIsNotComplete() {
        AdjudicationPanel panel = new AdjudicationPanel(votes(W, W, null), votes(R, R, R, R));
        assertTrue(panel.isJuryComplete());
        assertFalse(panel.isComplete());
        assertNull(panel.majority());
    }

    @Test
    public void splitRefereesGoodThreeJuryBadOverturns() {
        AdjudicationPanel panel = new AdjudicationPanel(votes(W, W, R), votes(R, R, R, W));
        assertTrue(panel.isComplete());
        assertEquals(3, panel.whites());
        assertEquals(4, panel.reds());
        assertEquals(Boolean.FALSE, panel.majority());
        assertTrue(panel.differsFrom(AdjudicationPanel.refereeMajority(votes(W, W, R))));
    }

    @Test
    public void unanimousRefereesGoodFourJuryBadOverturns() {
        AdjudicationPanel panel = new AdjudicationPanel(votes(W, W, W), votes(R, R, R, R));
        assertEquals(Boolean.FALSE, panel.majority());
        assertTrue(panel.differsFrom(Boolean.TRUE));
    }

    @Test
    public void unanimousRefereesGoodThreeJuryBadConfirms() {
        AdjudicationPanel panel = new AdjudicationPanel(votes(W, W, W), votes(R, R, R, W));
        assertEquals(4, panel.whites());
        assertEquals(Boolean.TRUE, panel.majority());
        assertFalse(panel.differsFrom(Boolean.TRUE));
    }

    @Test
    public void splitJuryKeepsRefereeMajority() {
        for (Boolean[] referees : new Boolean[][] { votes(W, W, R), votes(R, R, W), votes(W, W, W), votes(R, R, R) }) {
            Boolean refereeMajority = AdjudicationPanel.refereeMajority(referees);
            AdjudicationPanel panel = new AdjudicationPanel(referees, votes(W, R, W, R));
            assertEquals(refereeMajority, panel.majority());
            assertFalse(panel.differsFrom(refereeMajority));
        }
    }

    @Test
    public void everyoneAgrees() {
        AdjudicationPanel white = new AdjudicationPanel(votes(W, W, W), votes(W, W, W, W));
        assertEquals(7, white.whites());
        assertEquals(Boolean.TRUE, white.majority());
        assertFalse(white.differsFrom(Boolean.TRUE));

        AdjudicationPanel red = new AdjudicationPanel(votes(R, R, R), votes(R, R, R, R));
        assertEquals(7, red.reds());
        assertEquals(Boolean.FALSE, red.majority());
        assertFalse(red.differsFrom(Boolean.FALSE));
    }

    @Test
    public void refereeMajorityNeedsTwoMatchingVotes() {
        assertNull(AdjudicationPanel.refereeMajority(null));
        assertNull(AdjudicationPanel.refereeMajority(votes(W, null, null)));
        assertNull(AdjudicationPanel.refereeMajority(votes(W, R, null)));
        assertEquals(Boolean.TRUE, AdjudicationPanel.refereeMajority(votes(W, null, W)));
        assertEquals(Boolean.FALSE, AdjudicationPanel.refereeMajority(votes(null, R, R)));
    }

    @Test
    public void juryArrayLargerThanFourIsTruncated() {
        // the FOP keeps five slots; a fifth vote must never count
        AdjudicationPanel panel = new AdjudicationPanel(votes(W, W, W), votes(W, W, W, W, R));
        assertEquals(7, panel.votesReceived());
        assertEquals(0, panel.reds());
        assertEquals(Boolean.TRUE, panel.majority());
    }
}
