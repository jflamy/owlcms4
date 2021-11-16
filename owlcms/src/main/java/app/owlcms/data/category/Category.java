/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.data.category;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

//must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
@Cacheable
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
public class Category implements Serializable, Comparable<Category>, Cloneable {

    @Transient
    final private static Logger logger = (Logger) LoggerFactory.getLogger(Category.class);

    public final static Double ROBI_B = 3.321928095;

    /** The id. */
    @Id
    // @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /** The minimum weight. */
    Double minimumWeight; // inclusive

    /** The maximum weight. */
    Double maximumWeight; // exclusive

    /** minimum weight to be considered eligible */
    @Column(columnDefinition = "integer default 0")
    private int qualifyingTotal = 0;

    @ManyToOne(fetch = FetchType.LAZY) // ok in this case
    @JoinColumn(name = "agegroup_id")
    @JsonIdentityReference(alwaysAsId = true)
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(columnDefinition = "boolean default false")
    private boolean active;

    private Integer wrSr;

    private Integer wrJr;

    private Integer wrYth;

    // combines age group and bw category (which includes gender).
    private String code;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Participation> participations = new ArrayList<>();

    private String name;

    /**
     * Instantiates a new category.
     */
    public Category() {
        // manually generate the Id to avoid issues when creating many-to-many Participations
        setId(IdUtils.getTimeBasedId(this));
        //logger.debug("category constructor {}, {}",System.identityHashCode(this), this.getId());
    }

    public Category(Category c) {
        this(c.minimumWeight, c.maximumWeight, c.gender, c.active, c.getWrYth(), c.getWrJr(), c.getWrSr(), c.ageGroup,
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
        //logger.debug("{} Category({},{},{}) [{}]", getComputedCode(), gender, minimumWeight, maximumWeight, LoggerUtils.whereFrom(1));
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

        int compare = ObjectUtils.compare(this.gender, o.getGender());
        if (compare != 0) {
            return compare;
        }

        compare = ObjectUtils.compare(this.ageGroup, o.getAgeGroup(), true);
        if (compare != 0) {
            return compare;
        }

        // same division, same gender, rank according to maximumWeight.
        Double value1 = this.getMaximumWeight();
        Double value2 = o.getMaximumWeight();
        compare = ObjectUtils.compare(value1, value2);
        return compare;
    }

    public String dump() {
        return "Category [code=" + code + ", name=" + getName() + ", minimumWeight=" + minimumWeight
                + ", maximumWeight="
                + maximumWeight + ", ageGroup=" + ageGroup + ", gender=" + gender + ", active=" + active + ", wrSr="
                + getWrSr() + ", wrJr=" + getWrJr() + ", wrYth=" + getWrYth() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Category cat = (Category) o;

//        String name2 = getCode();
//        String name3 = cat.getCode();
//        boolean equal1 = StringUtils.equals(name2,name3);

        Long id1 = getId();
        Long id2 = cat.getId();
        boolean equal2 = id1 == id2;

        return equal2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj) {
//            return true;
//        }
//        if ((obj == null) || (getClass() != obj.getClass())) {
//            return false;
//        }
//        Category other = (Category) obj;
//
//        // other is not null, and neither are we
//        // don't compare the categories inside age group, this gets circular.
//        boolean ageGroupEquals = AgeGroup.looseEquals(this.ageGroup, other.ageGroup);
//
//        return active == other.active && ageGroupEquals && Objects.equals(code, other.code)
//                && gender == other.gender && Objects.equals(id, other.id)
//                && Objects.equals(maximumWeight, other.maximumWeight)
//                && Objects.equals(minimumWeight, other.minimumWeight) && Objects.equals(name, other.name)
//                && Objects.equals(getWrJr(), other.getWrJr())
//                && Objects.equals(getWrSr(), other.getWrSr()) && Objects.equals(getWrYth(), other.getWrYth());
//    }

    @JsonIgnore
    public List<Participation> getParticipations() {
        return participations;
    }

    public void setParticipations(List<Participation> participations) {
        this.participations = participations;
    }

    /**
     * Gets the active.
     *
     * @return the active
     */
    public Boolean getActive() {
        return active;
    }

    public AgeGroup getAgeGroup() {
        return ageGroup;
    }

    public String getCode() {
        return code != null ? code : "";
    }

    /**
     * Gets the gender.
     *
     * @return the gender
     */
    public Gender getGender() {
        return gender;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    @Transient
    @JsonIgnore
    public String getLimitString() {
        //logger.debug("category {} {} {} {}", this.getId(), this.getCode(), this.getMinimumWeight(), this.getMinimumWeight());
        if (id == null || maximumWeight == null || maximumWeight - Math.round(maximumWeight) > 0.1) {
            String val = "temp_"+minimumWeight+"_"+maximumWeight;
            //logger.debug("{} \n{}", val, LoggerUtils.stackTrace());
            return val;
        }
        if (maximumWeight > 130) {
            return Translator.translate("catAboveFormat",
                    minimumWeight != null ? String.valueOf((int) (Math.round(minimumWeight))) : "");
        } else {
            return String.valueOf((int) (Math.round(maximumWeight)));
        }
    }
    
    @Transient
    @JsonIgnore
    public String getCodeLimitString() {
        //logger.debug("category {} {} {} {}", this.getId(), this.getCode(), this.getMinimumWeight(), this.getMinimumWeight());
        if (id == null || maximumWeight == null || maximumWeight - Math.round(maximumWeight) > 0.1) {
            String val = "temp_"+minimumWeight+"_"+maximumWeight;
            //logger.debug("{} \n{}", val, LoggerUtils.stackTrace());
            return val;
        }
        if (maximumWeight > 130) {
            return "999";
        } else {
            return String.valueOf((int) (Math.round(maximumWeight)));
        }
    }

    /**
     * Gets the maximum weight.
     *
     * @return the maximumWeight
     */
    public Double getMaximumWeight() {
        return maximumWeight;
    }

    /**
     * Gets the minimum weight.
     *
     * @return the minimumWeight
     */
    public Double getMinimumWeight() {
        return minimumWeight;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        if (name == null || name.isBlank()) {
            return getComputedName();
        }
        return name;
    }

    @JsonIgnore
    @Transient
    public String getComputedCode() {
        String agName = (ageGroup != null ? ageGroup.getName() : "");
 
        if (agName == null || agName.isEmpty()) {
            String catName = gender+getCodeLimitString();
            return catName;
        } else {
            return ageGroup.getCode() + "_" + gender+getCodeLimitString();
        }
    }
    
    @JsonIgnore
    @Transient
    public String getComputedName() {
        String agName = (ageGroup != null ? ageGroup.getName() : "");
        String catName = getLimitString();
        if (agName == null || agName.isEmpty()) {
            return gender + catName;
        } else {
            return agName + " " + catName;
        }
    }

    /**
     * @return the qualifyingTotal
     */
    public int getQualifyingTotal() {
        return qualifyingTotal;
    }

    /**
     * Gets the wr.
     *
     * @return the wr
     */
    @Transient
    @JsonIgnore
    public Integer getWr() {
        if (ageGroup == null) {
            return 0;
        }
        int wr = 0;
        if (ageGroup.getAgeDivision() != AgeDivision.IWF) {
            wr = 0;
        } else if (ageGroup.getMaxAge() == 999) {
            wr = getWrSr();
        } else if (ageGroup.getMaxAge() == 20) {
            wr = getWrJr();
        } else if (ageGroup.getMaxAge() == 17) {
            wr = getWrYth();
        } else {
            wr = 0;
        }
        // logger./**/warn("wr({} {} {} {}) = {}",ageGroup, ageGroup.getAgeDivision(), ageGroup.getMaxAge(),
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
        // logger./**/warn("{} {} {} {} {}", this.getCode(), age, getWrYth(), getWrJr(), getWrSr());
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
        return wrJr;
    }

    public Integer getWrSr() {
        return wrSr;
    }

    public Integer getWrYth() {
        return wrYth;
    }

//    @Override
//    public int hashCode() {
//        return Objects.hash(active, ageGroup, code, gender, id, maximumWeight, minimumWeight, name, getWrJr(),
//                getWrSr(),
//                getWrYth());
//    }

    /**
     * Checks if is active.
     *
     * @return the boolean
     */
    public Boolean isActive() {
        return active;
    }

    public String longDump() {
        return "Category " + System.identityHashCode(this)
                + " [name=" + getName()
                + ", active=" + active
                + ", id=" + getId()
                + ", minimumWeight=" + minimumWeight
                + ", maximumWeight=" + maximumWeight + ", ageGroup=" + (ageGroup != null ? ageGroup.getName() : null)
                + ", gender=" + gender
                + ", qualifying=" + qualifyingTotal
                + ", wr=" + getWrSr()
                + ", code=" + code + "]";
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
        return getName() + "_" + System.identityHashCode(this) + "_" + active + "_" + gender + "_" + ageGroup;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getName();
    }

    public boolean sameAs(Category prevCat) {
        return this.compareTo(prevCat) == 0;
    }

}
