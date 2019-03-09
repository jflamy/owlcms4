package org.ledocte.owlcms.data.athlete;

import java.util.Arrays;
import java.util.List;

public class AthleteDisplayData {
	public Integer lotNumber;

	public Integer startNumber;

	public String firstName;

	public String lastName;

	public String teamName;

	public Gender gender;

	public String category;

	public Integer snatchRank;

	public Integer cleanJerkRank;

	public Integer totalRank;

	public Integer sinclairRank;

	public Integer robiRank;

	public Integer customRank;

	public List<LiftInfo> attempts;

	public AthleteDisplayData(Athlete a) {
		this.lotNumber = a.getLotNumber();
		this.startNumber = a.getStartNumber();
		this.firstName = a.getFirstName();
		this.lastName = a.getLastName();
		this.teamName = a.getTeam();
		this.gender = a.getGender();
		this.category = a.getDisplayCategory();
		this.snatchRank = a.getSnatchRank();
		this.cleanJerkRank = a.getCleanJerkRank();
		this.totalRank = a.getTotalRank();
		this.sinclairRank = a.getSinclairRank();
		this.robiRank = a.getRobiRank();
		XAthlete x = new XAthlete(a);
		this.attempts = Arrays.asList(x.getRequestInfoArray());
	}

	/**
	 * @return the attempts
	 */
	public List<LiftInfo> getAttempts() {
		return attempts;
	}

	/**
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * @return the cleanJerkRank
	 */
	public Integer getCleanJerkRank() {
		return cleanJerkRank;
	}

	/**
	 * @return the customRank
	 */
	public Integer getCustomRank() {
		return customRank;
	}

	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @return the gender
	 */
	public Gender getGender() {
		return gender;
	}

	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * @return the lotNumber
	 */
	public Integer getLotNumber() {
		return lotNumber;
	}

	/**
	 * @return the robiRank
	 */
	public Integer getRobiRank() {
		return robiRank;
	}

	/**
	 * @return the sinclairRank
	 */
	public Integer getSinclairRank() {
		return sinclairRank;
	}

	/**
	 * @return the snatchRank
	 */
	public Integer getSnatchRank() {
		return snatchRank;
	}

	/**
	 * @return the startNumber
	 */
	public Integer getStartNumber() {
		return startNumber;
	}

	/**
	 * @return the team
	 */
	public String getTeamName() {
		return teamName;
	}
	/**
	 * @return the totalRank
	 */
	public Integer getTotalRank() {
		return totalRank;
	}
	/**
	 * @param attempts the attempts to set
	 */
	public void setAttempts(List<LiftInfo> attempts) {
		this.attempts = attempts;
	}
	/**
	 * @param category the category to set
	 */
	public void setCategory(String category) {
		this.category = category;
	}
	/**
	 * @param cleanJerkRank the cleanJerkRank to set
	 */
	public void setCleanJerkRank(Integer cleanJerkRank) {
		this.cleanJerkRank = cleanJerkRank;
	}
	/**
	 * @param customRank the customRank to set
	 */
	public void setCustomRank(Integer customRank) {
		this.customRank = customRank;
	}
	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	/**
	 * @param gender the gender to set
	 */
	public void setGender(Gender gender) {
		this.gender = gender;
	}
	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	/**
	 * @param lotNumber the lotNumber to set
	 */
	public void setLotNumber(Integer lotNumber) {
		this.lotNumber = lotNumber;
	}
	/**
	 * @param robiRank the robiRank to set
	 */
	public void setRobiRank(Integer robiRank) {
		this.robiRank = robiRank;
	}
	/**
	 * @param sinclairRank the sinclairRank to set
	 */
	public void setSinclairRank(Integer sinclairRank) {
		this.sinclairRank = sinclairRank;
	}
	/**
	 * @param snatchRank the snatchRank to set
	 */
	public void setSnatchRank(Integer snatchRank) {
		this.snatchRank = snatchRank;
	}
	/**
	 * @param startNumber the startNumber to set
	 */
	public void setStartNumber(Integer startNumber) {
		this.startNumber = startNumber;
	}

	/**
	 * @param teamName the teamName to set
	 */
	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	/**
	 * @param totalRank the totalRank to set
	 */
	public void setTotalRank(Integer totalRank) {
		this.totalRank = totalRank;
	}
}