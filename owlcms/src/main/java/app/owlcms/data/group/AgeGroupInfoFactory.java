package app.owlcms.data.group;

import java.util.List;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

public class AgeGroupInfoFactory {
	Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupInfoFactory.class);

	public List<AgeGroupInfo> getAgeGroupInfos(Group group) {
		List<Athlete> athletes = group.getAthletes();
		if (athletes == null || athletes.isEmpty()) {
			return List.of();
		}
		TreeMap<AgeGroup, AgeGroupInfo> ageGroupMap = new TreeMap<>();
		for (Athlete a : athletes) {
			AgeGroup ageGroup = a.getAgeGroup();
			if (ageGroup == null) {
				continue;
			}
			AgeGroupInfo agi = ageGroupMap.get(ageGroup);
			String subCategory = a.getSubCategory();
			if (subCategory.isBlank()) {
				subCategory = null;
			}
			if (agi == null) {
				agi = new AgeGroupInfo();
				agi.setNbAthletes(1);
				agi.setUnanimous(true);
				agi.setAgeGroup(ageGroup);
				agi.setSmallestWeightClass(a.getCategory().getMaximumWeight());
				agi.setLargestWeightClass(a.getCategory().getMaximumWeight());
				agi.setLargestWeightClassLimitString(a.getCategory().getLimitString());
				agi.setWeightClassRange(a.getCategory().getLimitString());
				agi.setBestSubCategory(subCategory);

				BWCatInfo bwi = new BWCatInfo(a.getCategory().getMaximumWeight().intValue(), a.getCategory().getLimitString(), a.getSubCategory());
				agi.addToList(bwi.getKey(), bwi);


				ageGroupMap.put(ageGroup, agi);
				//logger.debug("created ageGroup {} {}", ageGroup, agi.isUnanimous());
			} else {
				//logger.debug("found ageGroup {} {} {}", ageGroup, agi.getNbAthletes(), agi.isUnanimous());
				agi.setNbAthletes(agi.getNbAthletes() + 1);
				if (agi.getSmallestWeightClass() == null
				        || a.getCategory().getMaximumWeight() < agi.getSmallestWeightClass()) {
					agi.setSmallestWeightClass(a.getCategory().getMaximumWeight());
				}
				if (agi.getLargestWeightClass() == null
				        || a.getCategory().getMaximumWeight() > agi.getLargestWeightClass()) {
					agi.setLargestWeightClass(a.getCategory().getMaximumWeight());
					agi.setLargestWeightClassLimitString(a.getCategory().getLimitString());
				}

				if (subCategory != null) {
					if (agi.getBestSubCategory() != null) {
						int compare = subCategory.compareToIgnoreCase(agi.getBestSubCategory());
						//logger.debug("compare {} {} {} {}", subCategory, agi.getBestSubCategory(), compare, agi.isUnanimous());
						if (compare < 0) {
							// A is better than B
							agi.setBestSubCategory(subCategory);
						}
						agi.setUnanimous(agi.isUnanimous() && (compare == 0));
					} else {
						// largest was null, if we are "A", still unanimous
						int compare = "A".compareToIgnoreCase(subCategory);
						//logger.debug("current best = {}, athlete = {}", agi.getBestSubCategory(), subCategory);
						agi.setBestSubCategory(subCategory);
						agi.setUnanimous(agi.isUnanimous() && (compare == 0));
					}
				} else {
					if (agi.getBestSubCategory() != null) {
						// a null subcategory is considered to be the same as "A".
						int compare = "A".compareToIgnoreCase(agi.getBestSubCategory());
						agi.setUnanimous(agi.isUnanimous() && (compare == 0));
					} else {
						// all null subCategories so far.
						agi.setUnanimous(true);
					}
				}

				BWCatInfo bwi = new BWCatInfo(a.getCategory().getMaximumWeight().intValue(), a.getCategory().getLimitString(), a.getSubCategory());
				agi.addToList(bwi.getKey(), bwi);

				if (Math.abs(agi.getLargestWeightClass() - agi.getSmallestWeightClass()) < 0.1) {
					// same
					agi.setWeightClassRange(a.getCategory().getLimitString());
				} else {
					String weightClassRange = Translator.translate("Range.LowerUpper",
							(int) Math.round(agi.getSmallestWeightClass()),agi.getLargestWeightClassLimitString());
					agi.setWeightClassRange(weightClassRange);
				}
			}

			//logger.debug("athlete {} largest sub {} unanimous {} list {}", a, a.getSubCategory(), agi.isUnanimous(), agi.getList());
			agi.addAthlete(a);
		}
//		for (AgeGroupInfo agi: ageGroupMap.values()) {
//			//logger.debug("***** {} {} {} {} {}", agi.getAgeGroup(), agi.getWeightClassRange(), agi.getBestSubCategory(), agi.isUnanimous(), agi.getList());
//		}
		return ageGroupMap.values().stream().toList();
	}

	public static List<AgeGroupInfoFactory> getNbForSessionBlock(int i) {
		return null;
	}
}
