package app.owlcms.data.athlete;

/** Copies only editable Card inputs, not registration fields or adjudication evidence. */
public final class AthleteCardValues {
	private AthleteCardValues() {
	}

	public static void copyTo(Athlete source, Athlete target) {
		boolean validation = target.isValidation();
		target.setValidation(false);
		try {
			target.setSnatch1Declaration(source.getSnatch1Declaration());
			target.setSnatch1Change1(source.getSnatch1Change1());
			target.setSnatch1Change2(source.getSnatch1Change2());
			target.setSnatch1ActualLift(source.getSnatch1ActualLift());
			target.setSnatch2Declaration(source.getSnatch2Declaration());
			target.setSnatch2Change1(source.getSnatch2Change1());
			target.setSnatch2Change2(source.getSnatch2Change2());
			target.setSnatch2ActualLift(source.getSnatch2ActualLift());
			target.setSnatch3Declaration(source.getSnatch3Declaration());
			target.setSnatch3Change1(source.getSnatch3Change1());
			target.setSnatch3Change2(source.getSnatch3Change2());
			target.setSnatch3ActualLift(source.getSnatch3ActualLift());
			target.setCleanJerk1Declaration(source.getCleanJerk1Declaration());
			target.setCleanJerk1Change1(source.getCleanJerk1Change1());
			target.setCleanJerk1Change2(source.getCleanJerk1Change2());
			target.setCleanJerk1ActualLift(source.getCleanJerk1ActualLift());
			target.setCleanJerk2Declaration(source.getCleanJerk2Declaration());
			target.setCleanJerk2Change1(source.getCleanJerk2Change1());
			target.setCleanJerk2Change2(source.getCleanJerk2Change2());
			target.setCleanJerk2ActualLift(source.getCleanJerk2ActualLift());
			target.setCleanJerk3Declaration(source.getCleanJerk3Declaration());
			target.setCleanJerk3Change1(source.getCleanJerk3Change1());
			target.setCleanJerk3Change2(source.getCleanJerk3Change2());
			target.setCleanJerk3ActualLift(source.getCleanJerk3ActualLift());
			target.setCustomScore(source.getCustomScore());
			target.setForcedAsCurrent(source.isForcedAsCurrent());
		} finally {
			target.setValidation(validation);
		}
	}
}
