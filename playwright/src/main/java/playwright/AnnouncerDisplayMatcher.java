package playwright;

/**
 * Verifies that the announcer page reflects the expected display.
 * Requires both the top-bar header AND the grid first-row confirmation
 * (same logic as the original {@code MonitoredPlatform}).
 */
class AnnouncerDisplayMatcher implements UpdateCheck.DisplayMatcher {

    @Override
    public boolean expectedDisplayVisible(UpdateCheck.ExpectedDisplay expected,
            UpdateCheck.ExpectationState state, UpdateCheck.CleanLog log, MonitoredPage mp) {
        UpdateCheck.SnapshotRead snapshotRead = mp.readSnapshot();
        state.recordSnapshotRead(snapshotRead);
        UpdateCheck.Snapshot snapshot = snapshotRead.snapshot();
        if (snapshot == null) {
            return false;
        }
        boolean athleteOk = UpdateCheck.matchesExpected(snapshot, expected);
        boolean gridOk = UpdateCheck.gridConfirmed(snapshot, expected);
        if (athleteOk && !state.athleteConfirmed()) {
            state.confirmAthlete();
            log.confirmedAthlete(mp.fop(), mp.role(), state.elapsedMillis(), expected.sequence(), snapshot);
        }
        if (gridOk && !state.gridConfirmed()) {
            state.confirmGrid();
            log.confirmedGrid(mp.fop(), mp.role(), state.elapsedMillis(), expected.sequence());
        }
        return athleteOk && gridOk;
    }

    @Override
    public void logExpectedMiss(UpdateCheck.ExpectedDisplay expected, UpdateCheck.ExpectationState state,
            UpdateCheck.CleanLog log, MonitoredPage mp) {
        String lastPollSummary = state.lastSnapshotReadSummary();
        int inWindowPolls = state.snapshotReads();
        UpdateCheck.SnapshotRead finalRead = mp.readSnapshot();
        state.recordSnapshotRead(finalRead);
        UpdateCheck.Snapshot current = finalRead.snapshot();
        String fop = mp.fop();
        UpdateCheck.BoardRole role = mp.role();
        if (current == null) {
            log.status(fop, role,
                    "playwright timed out after " + state.elapsedMillis() + "ms / "
                            + inWindowPolls + " polls; expected " + expected.display()
                            + " but got no snapshot: " + finalRead.summary()
                            + "; last poll: " + lastPollSummary,
                    false);
            return;
        }
        if (!UpdateCheck.athleteNameMatches(current.athleteName(), expected.displayName())) {
            log.status(fop, role, "playwright expected athlete " + expected.displayName()
                    + " but saw " + visible(current.athleteName())
                    + " after " + state.elapsedMillis() + "ms / " + state.snapshotReads() + " polls", false);
        } else if (!UpdateCheck.digitsOnly(current.attempt()).equals(UpdateCheck.digitsOnly(expected.attempt()))) {
            log.status(fop, role, "playwright expected attempt " + expected.attempt()
                    + " but saw " + visible(current.attempt())
                    + " after " + state.elapsedMillis() + "ms / " + state.snapshotReads() + " polls", false);
        } else if (!UpdateCheck.digitsOnly(current.weight()).equals(UpdateCheck.digitsOnly(expected.weight()))) {
            log.status(fop, role, "playwright expected weight " + expected.weight()
                    + " but header shows " + visible(current.weight())
                    + " after " + state.elapsedMillis() + "ms / " + state.snapshotReads() + " polls", false);
        } else if (!UpdateCheck.gridConfirmed(current, expected)) {
            log.status(fop, role, "playwright grid not confirmed: header weight=" + current.weight()
                    + " expected weight=" + expected.weight()
                    + " gridCell='" + current.gridFirstCell()
                    + "' after " + state.elapsedMillis() + "ms / " + state.snapshotReads() + " polls", false);
        } else {
            log.status(fop, role, "playwright expected " + expected.display()
                    + " but confirmation failed after "
                    + state.elapsedMillis() + "ms / " + state.snapshotReads() + " polls", false);
        }
    }

    private static String visible(String value) {
        return value == null || value.isBlank() ? "<empty>" : value;
    }
}
