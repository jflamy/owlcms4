package app.owlcms.data.group;

import app.owlcms.data.agegroup.AgeGroup;

public class AgeGroupInfo {
	AgeGroup ageGroup;
	Double smallestWeightClass;
	Double largestWeightClass;
	String weightClassRange;
	int nbAthletes;
	private String largestWeightClassLimitString;

	public AgeGroup getAgeGroup() {
		return this.ageGroup;
	}

	public Double getLargestWeightClass() {
		return this.largestWeightClass;
	}

	public String getLargestWeightClassLimitString() {
		return this.largestWeightClassLimitString;
	}

	public Double getSmallestWeightClass() {
		return this.smallestWeightClass;
	}

	public int getNbAthletes() {
		return this.nbAthletes;
	}

	public String getWeightClassRange() {
		return this.weightClassRange;
	}

	public void setAgeGroup(AgeGroup ageGroup) {
		this.ageGroup = ageGroup;
	}

	public void setLargestWeightClass(Double double1) {
		this.largestWeightClass = double1;
	}

	public void setLargestWeightClassLimitString(String string) {
		this.largestWeightClassLimitString = string;
	}

	public void setSmallestWeightClass(Double double1) {
		this.smallestWeightClass = double1;
	}

	public void setNbAthletes(int nbAthletes) {
		this.nbAthletes = nbAthletes;
	}

	public void setWeightClassRange(String weightClassRange) {
		this.weightClassRange = weightClassRange;
	}
}
