/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
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
import app.owlcms.data.category.Category;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

/**
 * An AgeGroup designates an age range and the associated bodyweight categories, for a given gender.
 *
 * @author Jean-François Lamy
 *
 */

// must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
@Cacheable
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "key", scope = AgeGroup.class)
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
public class AgeGroup implements Comparable<AgeGroup>, Serializable {

	private static final long serialVersionUID = 8154757158144876816L;

	/**
	 * don't deep compare the categories inside age group to avoid circularities. This method is used when comparing
	 * categories (rely on code and other top-level properties only)
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
			ageGroupEquals = 
					Objects.equals(firstAgeGroup.computeChampionshipName(), otherAgeGroup.computeChampionshipName())
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
	@Transient
	static Logger logger = (Logger) LoggerFactory.getLogger(AgeGroup.class);
	Integer maxAge;
	Integer minAge;

	/*
	 * ageDivision is the legacy name for a championship type. MASTERS, U, DEFAULT. The only value that has special
	 * meaning is MASTERS.
	 * 
	 * A championship is a set of age groups. Most championships group only one age group (a U15 championship at the
	 * same time as a U17 and a U20). Masters are the exception where multiple age groups are combined to create a
	 * single team competition. But there might be other situations -- like a SCHOOL championship where there are two
	 * age groups, but still only one school team wins by combining the two, or any combination.
	 * 
	 */
	private String ageDivision;
	private String championshipName; // foreign key: PanAm, SouthAm, etc. Same the type if not specified explicitly.
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
		
//		int length1 = championshipName1 != null ? championshipName1.length() : 0;
//		int length2 = championshipName2 != null ? championshipName2.length() : 0;	
//		compare = ObjectUtils.compare(length1, length2);
//		if (compare != 0) {
//			//logger.trace("(agegroup championshipName length) {} {} {}", -compare, championshipName1, championshipName2);
//			return compare; // shorter first
//		}
		compare = ObjectUtils.compare(championshipName1, championshipName2);
		if (compare != 0) {
			//logger.trace("(agegroup championshipName) {} {} {}", compare, championshipName1, championshipName2);
			return compare;
		}

		compare = ObjectUtils.compare(this.gender, o.getGender());
		if (compare != 0) {
			//logger.trace("(agegroup gender) {} {} {}", compare, this.gender, o.getGender());
			return compare;
		}
		
		compare = ObjectUtils.compare(this.minAge, o.getMinAge());
		if (compare != 0) {
			//logger.trace("(agegroup minage) {} {} {}", compare, this.minAge, o.getMinAge());
			return compare;
		}
		compare = ObjectUtils.compare(this.maxAge, o.getMaxAge());
		if (compare != 0) {
			//logger.trace("(agegroup maxage) {} {} {}", compare, this.maxAge, o.getMaxAge());
			return compare;
		}
		return compare;
	}
	
	public static Comparator<AgeGroup> registrationComparator = (a,b) -> {
		//logger.debug("comparing agegroups");
		if (b == null) {
			return -1; // we are smaller, we come first in the list
		}
		int compare = 0;

		compare = ObjectUtils.compare(a.getGender(), b.getGender());
		if (compare != 0) {
			//logger.debug("agegroup gender {} {} {} ", a.getGender(), compare > 0 ? ">" : "<",  b.getGender());
			return compare;
		}
		
		compare = ObjectUtils.compare(a.getMaxAge(), b.getMaxAge());
		if (compare != 0) {
			//logger.debug("maxage {} {} {} ", a.getMaxAge(), compare > 0 ? ">" : "<",  b.getMaxAge());
			return compare;
		}
		
		compare = ObjectUtils.compare(a.getMinAge(), b.getMinAge());
		if (compare != 0) {
			//logger.debug("agegroup minage {} {} {} ", a.getMinAge(), compare > 0 ? ">" : "<",  b.getMinAge());
			return compare;
		}
		
		return compare;
	};

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
		return this.active == other.active 
				&& this.ageDivision.contentEquals(other.ageDivision)
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

	@JsonIgnore
	@Transient
	public ChampionshipType getChampionshipType() {
		Championship of = Championship.of(this.computeChampionshipName());
		return of != null ? of.getType() : ChampionshipType.DEFAULT;
	}

	@JsonIgnore
	@Transient
	public String computeChampionshipName() {
		return (this.getChampionshipName() != null && !this.getChampionshipName().isBlank()) ? this.getChampionshipName()
		        : this.ageDivision;
	}

	public String getCode() {
		return this.code;
	}

	@JsonIgnore
	public String getDisplayName() {
		String code2 = this.getCode();
		if (code2 == null) {
			return "";
		}

		String value = null;
		String translatedCode = getTranslatedCode(code2);
		if (this.isAlreadyGendered() || this.getAgeDivision().contentEquals(Championship.MASTERS)) {
			value = translatedCode;
		} else {
			value = translatedCode + " " + getTranslatedGender();
		}
		return value;
	}

	public Gender getGender() {
		return this.gender;
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
		if (this.ageDivision.contentEquals(Championship.of(Championship.MASTERS).getName()) || this.isAlreadyGendered()) {
			value = translatedCode;
		} else if (this.ageDivision.contentEquals(Championship.DEFAULT)) {
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

	public void removeCategory(Category category) {
		if (category != null) {
			category.setAgeGroup(null);
			this.categories.remove(category);
		}
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void setAgeDivision(String ageDivision) {
		this.ageDivision = ageDivision;
	}

	public void setCategories(List<Category> value) {
		this.categories = value;
	}

	public void setChampionship(Championship championship) {
		this.ageDivision = championship.getType().name();
	}

	public void setChampionshipName(String championshipName) {
		this.championshipName = championshipName;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
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

	@Override
	public String toString() {
		return getDisplayName();
	}

	private String getTranslatedCode(String code2) {
		String translatedCode = Translator.translateOrElseNull(
		        "AgeGroup." + code2,
		        OwlcmsSession.getLocale());
		return translatedCode != null ? translatedCode : code2;
	}

	public void setAlreadyGendered(boolean b) {
		this.alreadyGendered = b;
	}

	public boolean isAlreadyGendered() {
		return alreadyGendered == null ? false : alreadyGendered;
	}

	public String getChampionshipName() {
		return championshipName;
	}

}
