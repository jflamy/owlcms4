package app.owlcms.data.group;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import ch.qos.logback.classic.Logger;

public class AgeGroupInfo {
	AgeGroup ageGroup;
	Double smallestWeightClass;
	Double largestWeightClass;
	String weightClassRange;
	int nbAthletes;
	private String largestWeightClassLimitString;
	private List<Athlete> athletes = new ArrayList<>();
	private boolean unanimous;
	private String bestSubCategory;
	static Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupInfo.class);

	public void addAthlete(Athlete athlete) {
		this.athletes.add(athlete);
	}

	public AgeGroup getAgeGroup() {
		return this.ageGroup;
	}

	public List<Athlete> getAthletes() {
		return AthleteSorter.registrationOrderCopy(this.athletes);
	}

	public List<Athlete> getAthletesByEntryTotal() {
		var nAthletes = new ArrayList<>(this.athletes);
		// reverse order required to show highest entry total first
		nAthletes.sort((a, b) -> -ObjectUtils.compare(a.getEntryTotal(), b.getEntryTotal(), false));
		return nAthletes;
	}

	public List<Athlete> getAthletesByStartNumber() {
		return AthleteSorter.registrationOrderCopy(this.athletes);
	}

	public int getHighestEntryTotal() {
		return getAthletesByEntryTotal().get(0).getEntryTotal();
	}

	public Double getLargestWeightClass() {
		return this.largestWeightClass;
	}

	public String getLargestWeightClassLimitString() {
		return this.largestWeightClassLimitString;
	}

	public int getLowestEntryTotal() {
		return getAthletesByEntryTotal().get(this.athletes.size() - 1).getEntryTotal();
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

	public boolean isUnanimous() {
		logger.debug("unanimous {}", unanimous);
		return unanimous;
	}

	public String getBestSubCategory() {
		return bestSubCategory;
	}

	public void setBestSubCategory(String largestSubCategory) {
		this.bestSubCategory = largestSubCategory;
	}

	public void setUnanimous(boolean unanimous) {
		this.unanimous = unanimous;
	}

	TreeMap<String, BWCatInfo> subCats = new TreeMap<>();

	public void addToList(String key, BWCatInfo info) {
		subCats.put(key, info);
	}

	public Collection<String> getList() {
		return subCats.values().stream().map(v -> v.getFormattedString()).toList();
	}

	public String getFormattedRange() {
		if (unanimous) {
			if (getBestSubCategory() == null) {
				return getWeightClassRange();
			} else {
				return getWeightClassRange() +" "+getBestSubCategory();
			}
		} else {
			return subCats.values().stream().map(v -> v.getFormattedString()).collect(Collectors.joining(", "));
		}
	}

	public void setFormattedRange(String unused) {
		// for dumb introspection
	}
}