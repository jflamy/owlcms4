package org.ledocte.owlcms.data.athlete;

public class AthleteDisplayData {
	/**
	 * @return the lotNumber
	 */
	public Integer getLotNumber() {
		return lotNumber;
	}

	/**
	 * @return the startNumber
	 */
	public Integer getStartNumber() {
		return startNumber;
	}

	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * @return the team
	 */
	public String getTeamName() {
		return teamName;
	}

	/**
	 * @return the gender
	 */
	public String getGender() {
		return gender;
	}

	/**
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * @return the snatch1Declaration
	 */
	public String getSnatch1Declaration() {
		return snatch1Declaration;
	}

	/**
	 * @return the snatch1ActualLift
	 */
	public String getSnatch1ActualLift() {
		return snatch1ActualLift;
	}

	/**
	 * @return the snatch2Declaration
	 */
	public String getSnatch2Declaration() {
		return snatch2Declaration;
	}

	/**
	 * @return the snatch2ActualLift
	 */
	public String getSnatch2ActualLift() {
		return snatch2ActualLift;
	}

	/**
	 * @return the snatch3Declaration
	 */
	public String getSnatch3Declaration() {
		return snatch3Declaration;
	}

	/**
	 * @return the snatch3ActualLift
	 */
	public String getSnatch3ActualLift() {
		return snatch3ActualLift;
	}

	/**
	 * @return the cleanJerk1Declaration
	 */
	public String getCleanJerk1Declaration() {
		return cleanJerk1Declaration;
	}

	/**
	 * @return the cleanJerk1ActualLift
	 */
	public String getCleanJerk1ActualLift() {
		return cleanJerk1ActualLift;
	}

	/**
	 * @return the cleanJerk2Declaration
	 */
	public String getCleanJerk2Declaration() {
		return cleanJerk2Declaration;
	}

	/**
	 * @return the cleanJerk2ActualLift
	 */
	public String getCleanJerk2ActualLift() {
		return cleanJerk2ActualLift;
	}

	/**
	 * @return the cleanJerk3Declaration
	 */
	public String getCleanJerk3Declaration() {
		return cleanJerk3Declaration;
	}

	/**
	 * @return the cleanJerk3ActualLift
	 */
	public String getCleanJerk3ActualLift() {
		return cleanJerk3ActualLift;
	}

	/**
	 * @return the snatchRank
	 */
	public Integer getSnatchRank() {
		return snatchRank;
	}

	/**
	 * @return the cleanJerkRank
	 */
	public Integer getCleanJerkRank() {
		return cleanJerkRank;
	}

	/**
	 * @return the totalRank
	 */
	public Integer getTotalRank() {
		return totalRank;
	}

	/**
	 * @return the sinclairRank
	 */
	public Integer getSinclairRank() {
		return sinclairRank;
	}

	/**
	 * @return the robiRank
	 */
	public Integer getRobiRank() {
		return robiRank;
	}

	/**
	 * @return the customRank
	 */
	public Integer getCustomRank() {
		return customRank;
	}

	public Integer lotNumber;
	public Integer startNumber;
	public String firstName;
	public String lastName;
	public String teamName;
	public String gender;
	public String category;
	public String snatch1Declaration;
	public String snatch1ActualLift;
	public String snatch2Declaration;
	public String snatch2ActualLift;
	public String snatch3Declaration;
	public String snatch3ActualLift;
	public String cleanJerk1Declaration;
	public String cleanJerk1ActualLift;
	public String cleanJerk2Declaration;
	public String cleanJerk2ActualLift;
	public String cleanJerk3Declaration;
	public String cleanJerk3ActualLift;
	public Integer snatchRank;
	public Integer cleanJerkRank;
	public Integer totalRank;
	public Integer sinclairRank;
	public Integer robiRank;
	public Integer customRank;

	public AthleteDisplayData(Athlete a) {
		this.lotNumber = a.getLotNumber();
		this.startNumber = a.getStartNumber();
		this.firstName = a.getFirstName();
		this.lastName = a.getLastName();
		this.teamName = a.getTeam();
		this.gender = a.getGender();
		this.category = a.getDisplayCategory();
		this.snatch1Declaration = a.getSnatch1Declaration();
		this.snatch1ActualLift = a.getSnatch1ActualLift();
		this.snatch2Declaration = a.getSnatch2Declaration();
		this.snatch2ActualLift = a.getSnatch2ActualLift();
		this.snatch3Declaration = a.getSnatch3Declaration();
		this.snatch3ActualLift = a.getSnatch3ActualLift();
		this.cleanJerk1Declaration = a.getCleanJerk1Declaration();
		this.cleanJerk1ActualLift = a.getCleanJerk1ActualLift();
		this.cleanJerk2Declaration = a.getCleanJerk2Declaration();
		this.cleanJerk2ActualLift = a.getCleanJerk2ActualLift();
		this.cleanJerk3Declaration = a.getCleanJerk3Declaration();
		this.cleanJerk3ActualLift = a.getCleanJerk3ActualLift();
		this.snatchRank = a.getSnatchRank();
		this.cleanJerkRank = a.getCleanJerkRank();
		this.totalRank = a.getTotalRank();
		this.sinclairRank = a.getSinclairRank();
		this.robiRank = a.getRobiRank();
	}
}