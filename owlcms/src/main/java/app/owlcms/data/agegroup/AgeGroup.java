/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;
import ch.qos.logback.classic.Logger;

/**
 * An AgeGroup designates an age range and the associated bodyweight categories, for a given gender.
 *
 * @author Jean-François Lamy
 */

// must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
@Cacheable
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "key", scope = AgeGroup.class)
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
public class AgeGroup implements Comparable<AgeGroup>, Serializable {

	private static final long serialVersionUID = 8154757158144876816L;
	@Transient
	static Logger logger = (Logger) LoggerFactory.getLogger(AgeGroup.class);
	public static Comparator<AgeGroup> registrationComparator = (a, b) -> {
		if (a == null || b == null) {
			return ObjectUtils.compare(a, b, true);
		}

		int compare = ObjectUtils.compare(a.getGender(), b.getGender());
		if (compare != 0) {
			// logger.debug("agegroup gender {} {} {} ", a.getGender(), compare > 0 ? ">" : "<", b.getGender());
			return compare;
		}

		compare = ObjectUtils.compare(a.getMaxAge(), b.getMaxAge());
		if (compare != 0) {
			// logger.debug("maxage {} {} {} ", a.getMaxAge(), compare > 0 ? ">" : "<", b.getMaxAge());
			return compare;
		}

		compare = ObjectUtils.compare(a.getMinAge(), b.getMinAge());
		if (compare != 0) {
			// logger.debug("agegroup minage {} {} {} ", a.getMinAge(), compare > 0 ? ">" : "<", b.getMinAge());
			return compare;
		}
		
		if (!Competition.getCurrent().getDisplayByAgeGroup() && Config.getCurrent().featureSwitch("bwClassThenAgeGroup")) {
			return ObjectUtils.compare(a.getChampionshipName(), b.getChampionshipName());
		}

		return compare;
	};

	/**
	 * don't deep compare the categories inside age group to avoid circularities. This method is used when comparing categories (rely on code and other
	 * top-level properties only)
	 *
	 * @param firstAgeGroup
	 * @param otherAgeGroup
	 * @return
	 */
	public static boolean looseEquals(AgeGroup firstAgeGroup, AgeGroup otherAgeGroup) {
		boolean ageGroupEquals;

		if (firstAgeGroup == null && otherAgeGroup == null) {
			ageGroupEquals = true;
		} else if (firstAgeGroup == null) {
			ageGroupEquals = false;
		} else if (otherAgeGroup == null) {
			ageGroupEquals = false;
		} else {
			ageGroupEquals = Objects.equals(firstAgeGroup.computeChampionshipName(), otherAgeGroup.computeChampionshipName())
			        && Objects.equals(firstAgeGroup.getCode(), otherAgeGroup.getCode())
			        && Objects.equals(firstAgeGroup.getGender(), otherAgeGroup.getGender())
			        && Objects.equals(firstAgeGroup.getMinAge(), otherAgeGroup.getMinAge())
			        && Objects.equals(firstAgeGroup.getMaxAge(), otherAgeGroup.getMaxAge());
		}
		return ageGroupEquals;
	}

	@Transient
	@JsonIgnore
	public String categoriesAsString;
	boolean active;
	String code;
	@Column(name = "agkey")
	String key;
	Integer maxAge = 999;
	Integer minAge = 0;
	/*
	 * ageDivision is the legacy name for a championship type. MASTERS, U, DEFAULT. The only value that has special meaning is MASTERS.
	 *
	 * A championship is a set of age groups. Most championships group only one age group (a U15 championship at the same time as a U17 and a U20). Masters are
	 * the exception where multiple age groups are combined to create a single team competition. But there might be other situations -- like a SCHOOL
	 * championship where there are two age groups, but still only one school team wins by combining the two, or any combination.
	 *
	 */
	private String ageDivision;
	private String championshipName; // foreign key; also shown to users.
	@OneToMany(mappedBy = "ageGroup", cascade = { CascadeType.ALL }, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<Category> categories = new ArrayList<>();
	@Enumerated(EnumType.STRING)
	private Gender gender;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
	private Long id;
	private Integer qualificationTotal;
	@Column(columnDefinition = "boolean default false")
	private Boolean alreadyGendered = false;
	private Ranking scoringSystem;
	@Transient
	@JsonIgnore
	private Boolean forceSave = null;
	private ChampionshipType championshipType;
	private Ranking bestAthleteScoringSystem;
	@Column(columnDefinition = "boolean default true")
	private Boolean medals = true;
	
	public AgeGroup() {
	}

	public AgeGroup(String code, boolean active, Integer minAge, Integer maxAge, Gender gender,
	        String ageDivisionName, Integer qualificationTotal) {
		this.active = active;
		this.code = code;
		this.minAge = minAge;
		this.maxAge = maxAge;
		this.ageDivision = ageDivisionName;
		this.gender = gender;
		this.setQualificationTotal(qualificationTotal);
		this.alreadyGendered = false;
	}

	public void addCategory(Category category) {
		if (category != null) {
			this.categories.add(category);
			category.setAgeGroup(this);
		}
	}

	@Override
	public int compareTo(AgeGroup o) {
		if (o == null) {
			return 1; // we are bigger.
		}
		int compare = 0;

		String championshipName1 = this.computeChampionshipName();
		String championshipName2 = o.computeChampionshipName();

		// int length1 = championshipName1 != null ? championshipName1.length() : 0;
		// int length2 = championshipName2 != null ? championshipName2.length() : 0;
		// compare = ObjectUtils.compare(length1, length2);
		// if (compare != 0) {
		// //logger.trace("(agegroup championshipName length) {} {} {}", -compare, championshipName1, championshipName2);
		// return compare; // shorter first
		// }
		compare = ObjectUtils.compare(championshipName1, championshipName2);
		if (compare != 0) {
			// logger.trace("(agegroup championshipName) {} {} {}", compare, championshipName1, championshipName2);
			return compare;
		}

		compare = ObjectUtils.compare(this.gender, o.getGender());
		if (compare != 0) {
			// logger.trace("(agegroup gender) {} {} {}", compare, this.gender, o.getGender());
			return compare;
		}

		compare = ObjectUtils.compare(this.maxAge, o.getMaxAge());
		if (compare != 0) {
			// logger.trace("(agegroup maxage) {} {} {}", compare, this.maxAge, o.getMaxAge());
			return compare;
		}

		compare = ObjectUtils.compare(this.minAge, o.getMinAge());
		if (compare != 0) {
			// logger.trace("(agegroup minage) {} {} {}", compare, this.minAge, o.getMinAge());
			return compare;
		}

		return compare;
	}

	@JsonIgnore
	@Transient
	public String computeChampionshipName() {
		return (this.getChampionshipName() != null && !this.getChampionshipName().isBlank()) ? this.getChampionshipName()
		        : this.ageDivision;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		AgeGroup other = (AgeGroup) obj;
		if (other.getId() == this.getId()) {
			return true;
		}
		var cs1 = this.ageDivision;
		var cs2 = other.ageDivision;
		return this.active == other.active
				&& ((cs1 == null) ? (cs2 == null) : cs1.contentEquals(cs2))
		        && Objects.equals(this.getChampionshipName(), other.getChampionshipName())
		        && Objects.equals(this.categories, other.categories)
		        && Objects.equals(this.code, other.code)
		        && this.gender == other.gender && Objects.equals(this.id, other.id)
		        && Objects.equals(this.maxAge, other.maxAge)
		        && Objects.equals(this.minAge, other.minAge);
	}

	public String getAgeDivision() {
		return this.ageDivision;
	}

	@Transient
	@JsonIgnore
	public String getAgeFirstSortCode() {
		String core = scoreCodeCore();
		String result = core + "_" + this.gender.ordinal();
		return result;
	}

	@JsonIgnore
	public List<Category> getAllCategories() {
		return this.categories;
	}

	/**
	 * @return the categories for which we are the AgeGroup
	 */
	public List<Category> getCategories() {
		return this.categories.stream().filter(c -> {
			return !(c.getAgeGroup() == null);
		}).sorted().collect(Collectors.toList());
	}

	public String getCategoriesAsString() {
		if (this.categories == null || this.categories.size() == 0) {
			return "";
		}
		return getCategories().stream().map(c -> c.getLimitString()).collect(Collectors.joining(", "));
	}

	@JsonIgnore
	@Transient
	public Championship getChampionship() {
		return Championship.of(this.computeChampionshipName());
	}

	public String getChampionshipName() {
		return this.championshipName;
	}

	public ChampionshipType getChampionshipType() {
		if (this.championshipType == null) {
			Championship of = Championship.of(this.computeChampionshipName());
			return of != null ? of.getType() : ChampionshipType.U;
		} else {
			return this.championshipType;
		}
	}
	
	public void setChampionshipType(ChampionshipType c) {
		this.championshipType = c;
	}

	public String getCode() {
		return this.code;
	}

	@Transient
	@JsonIgnore
	public Ranking getComputedScoringSystem() {
		if (this.scoringSystem == null) {
			return Ranking.TOTAL;
		}
		return this.scoringSystem;
	}

	@JsonIgnore
	public String getDisplayName() {
		String code2 = this.getCode();
		if (code2 == null) {
			return "";
		}

		String value = null;
		String translatedCode = getTranslatedCode(code2);
		if (this.isAlreadyGendered() || this.getChampionship().getType() == ChampionshipType.MASTERS) {
			value = translatedCode;
		} else {
			value = translatedCode + " " + getTranslatedGender();
		}
		return value;
	}

	public Boolean getForceSave() {
		return this.forceSave;
	}

	public Gender getGender() {
		return this.gender;
	}

	@Transient
	@JsonIgnore
	public String getGenderFirstSortCode() {
		// all women before all men
		String core = scoreCodeCore();
		String result = this.gender.ordinal() + "_" + core;
		return result;
	}

	public Long getId() {
		return this.id;
	}

	public String getKey() {
		return getCode() + "_" + this.ageDivision + "_" + this.gender.name() + "_" + this.minAge + "_"
		        + this.maxAge;
	}

	public Integer getMaxAge() {
		return this.maxAge;
	}

	@Transient
	@JsonIgnore
	public Ranking getMedalScoringSystem() {
		return this.scoringSystem != null && this.scoringSystem.isMedalScore() ? this.scoringSystem : null;
	}

	public Integer getMinAge() {
		return this.minAge;
	}

	@JsonIgnore
	public String getName() {
		String code2 = this.getCode();
		if (code2 == null) {
			return "";
		}

		String value = null;
		String translatedCode = getTranslatedCode(code2);
		if (this.getChampionshipType() == ChampionshipType.MASTERS || this.isAlreadyGendered()) {
			value = translatedCode;
		} else if (this.getChampionshipType() == ChampionshipType.DEFAULT) {
			value = getTranslatedGender();
		} else {
			value = translatedCode + " " + getTranslatedGender();
		}
		return value;
	}

	/**
	 * @return the qualificationTotal
	 */
	public Integer getQualificationTotal() {
		return this.qualificationTotal;
	}

	public Ranking getScoringSystem() {
		return this.scoringSystem;
	}

	@Transient
	@JsonIgnore
	public String getScoringTitle() {
		var scoringSystem = getComputedScoringSystem();
		return Ranking.getScoringTitle(scoringSystem);
	}

	public String getTranslatedGender() {
		switch (getGender()) {
			case F:
			case I:
			case M:
				return getGender().asPublicGenderCode();
			default:
				throw new IllegalStateException();
		}
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	public boolean isActive() {
		return this.active;
	}

	public boolean isAlreadyGendered() {
		return this.alreadyGendered == null ? false : this.alreadyGendered;
	}

	/**
	 * @return a code that changes if the categories were edited in a way that requires reassigning athletes
	 */
	public int reassignmentHashCode() {
		List<Category> a = getCategories();
		if (a == null) {
			return 0;
		}
		int result = 1;
		for (Category element : a) {
			result = 31 * result + (element == null ? 0 : element.reassignmentHashCode());
		}
		return result;
	}

	public void removeCategory(Category category) {
		if (category != null) {
			category.setAgeGroup(null);
			this.categories.remove(category);
		}
	}

	public String scoreCodeCore() {
		// younger group firsts (max age)
		// 15-20 should come before 17-20 (min age)
		// but 85-999 must come before 0-999
		// then with categories before no categories (score medals last within an age range)
		Integer maxAge2 = this.getMaxAge();
		Integer minAge2 = this.getMinAge();
		String core = String.format("%03d", maxAge2)
		        + "_" + String.format("%03d", maxAge2 >= 900 ? maxAge2 : minAge2)
		        + "_" + String.format("%03d", 999 - this.getCategories().size());
		return core;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void setAgeDivision(String ageDivision) {
		this.ageDivision = ageDivision;
	}

	@Transient
	@JsonIgnore
	public void setAgeFirstSortCode(String ignored) {
	}

	public void setAlreadyGendered(boolean b) {
		this.alreadyGendered = b;
	}

	public void setCategories(List<Category> value) {
		this.categories = value;
	}

	public void setChampionship(Championship championship) {
		logger.debug("setting {} championship to {}", this, championship.getName());
		this.setChampionshipName(championship.getName());
	}

	public void setChampionshipName(String championshipName) {
		this.championshipName = championshipName;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Transient
	@JsonIgnore
	public void setForceSave(boolean b) {
		this.forceSave = true;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	@Transient
	@JsonIgnore
	public void setGenderFirstSortCode(String ignored) {
	}

	public void setKey(String key) {
		// do nothing, this is to fool Json deserialization.
	}

	public void setMaxAge(Integer maxAge) {
		this.maxAge = maxAge;
	}

	public void setMinAge(Integer minAge) {
		this.minAge = minAge;
	}

	/**
	 * @param qualificationTotal the qualificationTotal to set
	 */
	public void setQualificationTotal(Integer qualificationTotal) {
		this.qualificationTotal = qualificationTotal;
	}

	public void setScoringSystem(Ranking setScoringSystem) {
		this.scoringSystem = setScoringSystem;
	}

	@Override
	public String toString() {
		return getDisplayName();
	}

	private String getTranslatedCode(String code2) {
		// String translatedCode = Translator.translateOrElseNull(
		// "AgeGroup." + code2,
		// OwlcmsSession.getLocale());
		// return translatedCode != null ? translatedCode : code2;
		return code2;
	}

	public void setBestAthleteScoringSystem(Ranking rv) {
		this.bestAthleteScoringSystem = rv;
	}

	public Ranking getBestAthleteScoringSystem() {
		return bestAthleteScoringSystem;
	}
	
	public String getBestAthleteScoringSystemTitle() {
		var explicitBLR = JXLSWorkbookStreamSource.getBestLifterRankingThreadLocal();
		if (explicitBLR != null) {
			return Translator.translate("Ranking."+explicitBLR);
		} else if (bestAthleteScoringSystem != null) {
			return Translator.translate("Ranking."+bestAthleteScoringSystem);
		} else {
			return Translator.translate("Ranking."+Competition.getCurrent().getScoringSystem());
		}
	}

	public Boolean getMedals() {
		return medals;
	}

	public void setMedals(Boolean medals) {
		this.medals = medals;
	}
}
