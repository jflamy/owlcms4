package app.owlcms.data.athlete;

/** Inputs of the registration form; Card results and decision evidence never belong to this copy. */
public final class AthleteRegistrationValues {
	private AthleteRegistrationValues() {
	}

	public static void copyTo(Athlete source, Athlete target) {
		boolean validation = target.isValidation();
		target.setValidation(false);
		try {
			AthleteMetadata.from(source).copyTo(target);
			target.setMetadataUpdatedAt(source.getMetadataUpdatedAt());
			target.setIsoBirthDate(source.getIsoBirthDate());
			target.setGender(source.getGender());
			target.setMembership(source.getMembership());
			target.setScaleWeight(source.getScaleWeight());
			target.setBodyWeight(source.getBodyWeight());
			target.setPresumedBodyWeight(source.getPresumedBodyWeight());
			target.setQualifyingTotal(source.getQualifyingTotal());
			target.computeCategory(source.getCategory());
			target.setEligibleCategories(source.getEligibleCategories());
			target.setAgeGroupTeams(source.getAgeGroupTeams());
			target.setMixedAgeGroupTeams(source.getMixedAgeGroupTeams());
			target.setSubCategory(source.getSubCategory());
			target.setGroup(source.getGroup());
			target.setCoach(source.getCoach());
			target.setCustom1(source.getCustom1());
			target.setCustom2(source.getCustom2());
			target.setPersonalBestSnatch(source.getPersonalBestSnatch());
			target.setPersonalBestCleanJerk(source.getPersonalBestCleanJerk());
			target.setPersonalBestTotal(source.getPersonalBestTotal());
			target.setLotNumber(source.getLotNumber());
			target.setStartNumber(source.getStartNumber());
		} finally {
			target.setValidation(validation);
		}
	}
}
