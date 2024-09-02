/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.data.category;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.athlete.Gender;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.IdUtils;
import ch.qos.logback.classic.Logger;

/**
 * Contains information regarding each competition category.
 *
 * A category is the combination of an age range (AgeGroup), a gender, and a bodyweight range.
 *
 * Category currently include record information for the computation of Robi points. Category links to its associated
 * records.
 *
 * Robi = * A x (total)^b where b = log(10)/log(2)
 *
 * A = 1000 / [ (WR)^b ] WR = World RecordEvent
 *
 * @author owlcms
 *
 */
@SuppressWarnings("serial")

// must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
@Cacheable
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
public class Category implements Serializable, Comparable<Category>, Cloneable {

	public final static Double ROBI_B = 3.321928095;
	@Transient
	final private static Logger logger = (Logger) LoggerFactory.getLogger(Category.class);
	/** The maximum weight. */
	Double maximumWeight; // exclusive
	/** The minimum weight. */
	Double minimumWeight; // inclusive
	@Column(columnDefinition = "boolean default false")
	private boolean active;
	@ManyToOne(fetch = FetchType.EAGER) // ok in this case
	@JoinColumn(name = "agegroup_id")
	@JsonIdentityReference(alwaysAsId = true)
	private AgeGroup ageGroup;

	// combines age group and bw category (which includes gender).
	private String code;
	@Enumerated(EnumType.STRING)
	private Gender gender;
	/** The id. */
	@Id
	// @GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String name;
	@OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<Participation> participations = new ArrayList<>();
	/** minimum weight to be considered eligible */
	@Column(columnDefinition = "integer default 0")
	private int qualifyingTotal = 0;
	private Integer wrJr;
	private Integer wrSr;
	private Integer wrYth;

	/**
	 * Instantiates a new category.
	 */
	public Category() {
		// manually generate the Id to avoid issues when creating many-to-many
		// Participations
		setId(IdUtils.getTimeBasedId(this));
		// logger.debug("category constructor {}, {}",System.identityHashCode(this),
		// this.getId());
	}

	public Category(Category c) {
		this(c.minimumWeight, c.maximumWeight, c.getGender(), c.active, c.getWrYth(), c.getWrJr(), c.getWrSr(),
		        c.ageGroup,
		        c.qualifyingTotal);
	}

	public Category(Double minimumWeight, Double maximumWeight, Gender gender, boolean active, Integer wrYth,
	        Integer wrJr, Integer wrSr, AgeGroup ageGroup, Integer qualifyingTotal) {
		this();
		this.setMinimumWeight(minimumWeight);
		this.setMaximumWeight(maximumWeight);
		this.setGender(gender);
		this.setActive(active);
		this.setAgeGroup(ageGroup);
		this.setWrYth(wrYth);
		this.setWrJr(wrJr);
		this.setWrSr(wrSr);
		this.setQualifyingTotal(qualifyingTotal);
		this.setCode(getComputedCode());
		this.setName(getDisplayName());
		// logger.debug("{} Category({},{},{}) [{}]", getComputedCode(), gender,
		// minimumWeight, maximumWeight,
		// LoggerUtils.whereFrom(1));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Category o) {
		if (o == null) {
			return -1; // we are smaller than null -- null goes to the end;
		}
		
		int compare;
		
		compare = ObjectUtils.compare(this.getCode(), o.getCode());
		if (compare == 0) {
			// shortcut.  identical codes are identical
			return compare;
		}
		
		compare = ObjectUtils.compare(this.getGender(), o.getGender());
		if (compare != 0) {
			return compare;
		}

		compare = ObjectUtils.compare(this.getAgeGroup(), o.getAgeGroup(), true);
		if (compare != 0) {
			return compare;
		}

		// same division, same gender, rank according to maximumWeight.
		Double value1 = this.getMaximumWeight();
		Double value2 = o.getMaximumWeight();
		compare = ObjectUtils.compare(value1, value2);
		return compare;
	}

	
	public static Comparator<Category> specificityComparator = (a,b) -> {
		if (a == null || b == null) return ObjectUtils.compare(a,b,true);
		var aAgeGroup = a.getAgeGroup();
		var bAgeGroup = b.getAgeGroup();
		if (aAgeGroup == null || bAgeGroup == null) return ObjectUtils.compare(aAgeGroup,bAgeGroup,true);
		int compare = ObjectUtils.compare(aAgeGroup.getGender(), bAgeGroup.getGender());
		if (compare != 0) return compare;
		int aDelta = aAgeGroup.getMaxAge() - aAgeGroup.getMinAge();
		int bDelta = bAgeGroup.getMaxAge() - bAgeGroup.getMinAge();
		return Integer.compare(aDelta, bDelta);
	};

	public String dump() {
		return "Category [code=" + this.code + ", name=" + getSafeName() + ", minimumWeight=" + this.minimumWeight
		        + ", maximumWeight="
		        + this.maximumWeight + ", ageGroup=" + this.ageGroup + ", gender=" + getGender() + ", active="
		        + this.active + ", wrSr="
		        + getWrSr() + ", wrJr=" + getWrJr() + ", wrYth=" + getWrYth() + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Category cat = (Category) o;

		// String name2 = getCode();
		// String name3 = cat.getCode();
		// boolean equal1 = StringUtils.equals(name2,name3);

		Long id1 = getId();
		Long id2 = cat.getId();
		boolean equal2 = id1 == id2;

		return equal2;
	}

	public String fullDump() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.longDump());
		for (Participation p : getParticipations()) {
			sb.append("    ");
			sb.append(p.long_dump());
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}

	/**
	 * Gets the active.
	 *
	 * @return the active
	 */
	public Boolean getActive() {
		return this.active;
	}

	public AgeGroup getAgeGroup() {
		return this.ageGroup;
	}

	public String getCode() {
		return this.code != null ? this.code : "";
	}

	@Transient
	@JsonIgnore
	public String getCodeLimitString() {
		if (this.id == null || this.maximumWeight == null
		        || this.maximumWeight - Math.round(this.maximumWeight) > 0.1) {
			String val = "temp_" + this.minimumWeight + "_" + this.maximumWeight;
			// logger.debug("{} \n{}", val, LoggerUtils.stackTrace());
			return val;
		}
		if (this.maximumWeight > 130) {
			return "999";
		} else {
			return String.valueOf((int) (Math.round(this.maximumWeight)));
		}
	}

	@JsonIgnore
	@Transient
	public String getComputedCode() {
		String agName = (this.ageGroup != null ? this.ageGroup.getName() : "");

		if (agName == null || agName.isEmpty()) {
			String catName = getGender() + getCodeLimitString();
			return catName;
		} else {
			return this.ageGroup.getCode() + "_" + getGender() + getCodeLimitString();
		}
	}

	@JsonIgnore
	@Transient
	public String getDisplayName() {
		String agName = (this.ageGroup != null ? this.ageGroup.getName() : "");
		String catName = getLimitString();
		String result;
		if (isAlreadyGendered()) {
			// this takes priority over DEFAULT championship
			result = agName + " " + catName;
		} else if (getChampionshipType() == ChampionshipType.DEFAULT) {
			// legacy case - just the gender and the category.
			result = getTranslatedGender() + " " + catName;
		} else {
			result = agName + " " + catName;
		}
		return result.trim();
	}

	@JsonIgnore
	@Transient
	ChampionshipType getChampionshipType() {
		ChampionshipType championshipType = this.ageGroup != null ? ageGroup.getChampionshipType() : null;
		return championshipType;
	}

	@JsonIgnore
	@Transient
	private boolean isAlreadyGendered() {
		boolean alreadyGendered = this.ageGroup != null ? ageGroup.isAlreadyGendered() : false;
		return alreadyGendered;
	}

	/**
	 * Gets the gender.
	 *
	 * @return the gender
	 */
	public Gender getGender() {
		return this.gender;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public Long getId() {
		return this.id;
	}

	@Transient
	@JsonIgnore
	public String getLimitString() {
		// logger.debug("category {} {} {} {}", this.getId(), this.getCode(),
		// this.getMinimumWeight(),
		// this.getMinimumWeight());
		if (this.id == null || this.maximumWeight == null
		        || this.maximumWeight - Math.round(this.maximumWeight) > 0.1) {
			String val = "temp_" + this.minimumWeight + "_" + this.maximumWeight;
			// logger.debug("{} \n{}", val, LoggerUtils.stackTrace());
			return val;
		}
		if (this.maximumWeight > 998.1 && this.minimumWeight < 0.1) {
			// all body weights
			return "";
		} else if (this.maximumWeight > 130) {
			return Translator.translate("catAboveFormat",
			        this.minimumWeight != null ? String.valueOf((int) (Math.round(this.minimumWeight))) : "");
		} else {
			return String.valueOf((int) (Math.round(this.maximumWeight)));
		}
	}

	/**
	 * Gets the maximum weight.
	 *
	 * @return the maximumWeight
	 */
	public Double getMaximumWeight() {
		return this.maximumWeight;
	}

	/**
	 * Gets the minimum weight.
	 *
	 * @return the minimumWeight
	 */
	public Double getMinimumWeight() {
		return this.minimumWeight;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	@JsonIgnore
	@Transient
	public String getSafeName() {
		if (this.name == null || this.name.isBlank()) {
			return getDisplayName();
		}
		return this.name;
	}

	@JsonIgnore
	public List<Participation> getParticipations() {
		return this.participations;
	}

	/**
	 * @return the qualifyingTotal
	 */
	public int getQualifyingTotal() {
		return this.qualifyingTotal;
	}

	public String getTranslatedGender() {
		if (gender == null) {
			return "";
		}
		switch (getGender()) {
			case F:
			case I:
			case M:
				return getGender().asPublicGenderCode();
			default:
				throw new IllegalStateException();
		}
	}

	@JsonIgnore
	@Transient
	public String getNameWithAgeGroup() {
		String agName = (this.ageGroup != null ? this.ageGroup.getName() : "");
		String catName = getLimitString();
		String result;
		if (agName == null || agName.isEmpty()) {
			result = getTranslatedGender() + " " + catName;
		} else {
			result = agName + " " + catName;
		}
		return result.trim();
	}

	public String getUpperBound() {
		if (getMaximumWeight() > 998) {
			return ">" + Math.round(getMinimumWeight());
		} else {
			return "" + Math.round(getMaximumWeight());
		}
	}

	/**
	 * Gets the wr.
	 *
	 * @return the wr
	 */
	@Transient
	@JsonIgnore
	public Integer getWr() {
		if (this.ageGroup == null) {
			return 0;
		}
		int wr = 0;
		if (this.ageGroup.getChampionship() != Championship.of(Championship.IWF)) {
			wr = 0;
		} else if (this.ageGroup.getMaxAge() == 999) {
			wr = getWrSr();
		} else if (this.ageGroup.getMaxAge() == 20) {
			wr = getWrJr();
		} else if (this.ageGroup.getMaxAge() == 17) {
			wr = getWrYth();
		} else {
			wr = 0;
		}
		// logger./**/warn("wr({} {} {} {}) = {}",ageGroup, ageGroup.getAgeDivision(),
		// ageGroup.getMaxAge(),
		// getCode(),wr);
		return wr;
	}

	/**
	 * Gets the wr.
	 *
	 * @return the wr
	 */
	public Integer getWr(int age) {
		int wr;
		// logger./**/warn("{} {} {} {} {}", this.getCode(), age, getWrYth(), getWrJr(),
		// getWrSr());
		if (age <= 17) {
			wr = getWrYth();
		} else if (age <= 20) {
			wr = getWrJr();
		} else {
			wr = getWrSr();
		}
		return wr;
	}

	public Integer getWrJr() {
		return this.wrJr;
	}

	public Integer getWrSr() {
		return this.wrSr;
	}

	public Integer getWrYth() {
		return this.wrYth;
	}

	// @Override
	// public int hashCode() {
	// return Objects.hash(active, ageGroup, code, gender, id, maximumWeight, minimumWeight, name, getWrJr(),
	// getWrSr(),
	// getWrYth());
	// }

	@Override
	public int hashCode() {
		return Objects.hash(getSafeName());
	}

	/**
	 * Checks if is active.
	 *
	 * @return the boolean
	 */
	public Boolean isActive() {
		return this.active;
	}

	public String longDump() {
		return "Category " + System.identityHashCode(this)
		        + " [name=" + getSafeName()
		        + ", active=" + this.active
		        + ", id=" + getId()
		        + ", minimumWeight=" + this.minimumWeight
		        + ", maximumWeight=" + this.maximumWeight + ", ageGroup="
		        + (this.ageGroup != null ? this.ageGroup.getName() : null)
		        + ", gender=" + getGender()
		        + ", qualifying=" + this.qualifyingTotal
		        + ", wr=" + getWrSr()
		        + ", code=" + this.code + "]";
	}

	public boolean sameAs(Category prevCat) {
		return this.compareTo(prevCat) == 0;
	}

	public boolean sameAsAny(Set<Category> set) {
		return set.stream().anyMatch(c -> this.getCode().contentEquals(c.getCode()));
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * Sets the active.
	 *
	 * @param active the new active
	 */
	public void setActive(Boolean active) {
		this.active = active;
	}

	public void setAgeGroup(AgeGroup ageGroup) {
		this.ageGroup = ageGroup;
	}

	public void setCode(String cellValue) {
		this.code = cellValue;
	}

	/**
	 * Sets the gender.
	 *
	 * @param enumGender the new gender
	 */
	public void setGender(Gender enumGender) {
		this.gender = enumGender;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Sets the maximum weight.
	 *
	 * @param maximumWeight the maximumWeight to set
	 */
	public void setMaximumWeight(Double maximumWeight) {
		this.maximumWeight = maximumWeight;
	}

	/**
	 * Sets the minimum weight.
	 *
	 * @param minimumWeight the minimumWeight to set
	 */
	public void setMinimumWeight(Double minimumWeight) {
		this.minimumWeight = minimumWeight;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setParticipations(List<Participation> participations) {
		this.participations = participations;
	}

	/**
	 * @param qualifyingTotal the qualifyingTotal to set
	 */
	public void setQualifyingTotal(int qualifyingTotal) {
		this.qualifyingTotal = qualifyingTotal;
	}

	public void setWrJr(Integer wrJr) {
		this.wrJr = wrJr;
	}

	public void setWrSr(Integer wrSr) {
		// logger./**/warn("wrSr={}",wrSr);
		this.wrSr = wrSr;
	}

	public void setWrYth(Integer wrYth) {
		this.wrYth = wrYth;
	}

	/**
	 * Short dump.
	 *
	 * @return the string
	 */
	public String shortDump() {
		return getSafeName() + "_" + System.identityHashCode(this) + "_" + this.active + "_" + getGender() + "_"
		        + this.ageGroup;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getDisplayName();
	}

	public String getName() {
		return name;
	}

	public static String codeFromName(String catName) {
		Category cat = CategoryRepository.codeFromName(catName);
		return cat != null ? cat.getCode() : null;
	}

}
