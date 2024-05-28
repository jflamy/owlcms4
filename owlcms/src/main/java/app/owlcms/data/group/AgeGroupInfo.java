package app.owlcms.data.group;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter;

public class AgeGroupInfo {
	AgeGroup ageGroup;
	Double smallestWeightClass;
	Double largestWeightClass;
	String weightClassRange;
	int nbAthletes;
	private String largestWeightClassLimitString;
	private List<Athlete> athletes = new ArrayList<>();

	public AgeGroup getAgeGroup() {
		return this.ageGroup;
	}

	public List<Athlete> getAthletes() {
		return AthleteSorter.registrationOrderCopy(this.athletes);
	}

	public List<Athlete> getAthletesByEntryTotal() {
		var nAthletes = new ArrayList<>(this.athletes);
		nAthletes.sort((a, b) -> ObjectUtils.compare(a.getEntryTotal(), a.getEntryTotal(), false));
		return nAthletes;
	}

	public List<Athlete> getAthletesByStartNumber() {
		return AthleteSorter.registrationOrderCopy(this.athletes);
	}

	public Double getLargestWeightClass() {
		return this.largestWeightClass;
	}

	public String getLargestWeightClassLimitString() {
		return this.largestWeightClassLimitString;
	}

	public int getNbAthletes() {
		return this.nbAthletes;
	}

	public Double getSmallestWeightClass() {
		return this.smallestWeightClass;
	}

	public String getWeightClassRange() {
		return this.weightClassRange;
	}

	public void setAgeGroup(AgeGroup ageGroup) {
		this.ageGroup = ageGroup;
	}

	public void setAthletes(List<Athlete> athletes) {
		this.athletes = athletes;
	}

	public void setLargestWeightClass(Double double1) {
		this.largestWeightClass = double1;
	}

	public void setLargestWeightClassLimitString(String string) {
		this.largestWeightClassLimitString = string;
	}

	public void setNbAthletes(int nbAthletes) {
		this.nbAthletes = nbAthletes;
	}

	public void setSmallestWeightClass(Double double1) {
		this.smallestWeightClass = double1;
	}

	public void setWeightClassRange(String weightClassRange) {
		this.weightClassRange = weightClassRange;
	}
	
	public static List<AgeGroupInfo> getAgeGroupInfo(Group group){
		List<Athlete> athletes = group.getAthletes();
		TreeMap<AgeGroup, AgeGroupInfo> ageGroupMap = new TreeMap<>();
		for (Athlete a : athletes) {
			AgeGroup ageGroup = a.getAgeGroup();
			if (ageGroup == null) {
				continue;
			}
			AgeGroupInfo agi = ageGroupMap.get(ageGroup);
			if (agi == null) {
				agi = new AgeGroupInfo();
				agi.setNbAthletes(1);
				agi.setAgeGroup(ageGroup);
				agi.setSmallestWeightClass(a.getCategory().getMaximumWeight());
				agi.setLargestWeightClass(a.getCategory().getMaximumWeight());
				agi.setWeightClassRange(a.getCategory().getLimitString());
				ageGroupMap.put(ageGroup, agi);
			} else {
				agi.setNbAthletes(agi.getNbAthletes()+1);
				if (a.getCategory().getMinimumWeight() < agi.getSmallestWeightClass()) {
					agi.setSmallestWeightClass(a.getCategory().getMaximumWeight());
				}
				if (a.getCategory().getMaximumWeight() > agi.getLargestWeightClass()) {
					agi.setLargestWeightClass(a.getCategory().getMaximumWeight());
					agi.setLargestWeightClassLimitString(a.getCategory().getLimitString());
				}
				if (Math.abs(agi.getLargestWeightClass() - agi.getSmallestWeightClass()) < 0.1) {
					// same
					agi.setWeightClassRange(a.getCategory().getLimitString());
				} else {
					agi.setWeightClassRange((int)Math.round(agi.getSmallestWeightClass())+"-"+agi.getLargestWeightClassLimitString());
				}
			}
			agi.getAthletes().add(a);
		}
		return ageGroupMap.values().stream().toList();
	}
}
