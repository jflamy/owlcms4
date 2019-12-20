/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.category;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;

/**
 * Contains information regarding each competition category.
 *
 * A category is the combination of an age group and a weight class.
 *
 * Categories also define information for the computation of Robi points
 *
 * Robi = * A x (total)^b where b = log(10)/log(2)
 *
 * A = 1000 / [ (WR)^b ] WR = World Record
 *
 * @author owlcms
 *
 */
@SuppressWarnings("serial")
@Entity
@Cacheable
public class Category implements Serializable, Comparable<Category> {

    private final static Double ROBI_B = 3.321928095;
    private Double robiA = 0.0D;

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    /** The name. */
    private String name;

    /** The minimum weight. */
    Double minimumWeight; // inclusive

    /** The maximum weight. */
    Double maximumWeight; // exclusive

    @ManyToOne(fetch = FetchType.EAGER) // ok in this case
    @JoinColumn(name = "agegroup_id")
    private AgeGroup ageGroup;

    @ManyToMany(mappedBy = "eligibleCategories")
    private List<Athlete> athletes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(columnDefinition = "boolean default false")
    private boolean active;

    private Integer wrSr;
    private Integer wrJr;
    private Integer wrYth;

    private String code;

    /**
     * Instantiates a new category.
     */
    public Category() {
    }

    /**
     * Instantiates a new category.
     *
     * @param minimumWeight the minimum weight
     * @param maximumWeight the maximum weight
     * @param enumGender    the enum gender
     * @param active        the active
     * @param wr            the world record, for IWF Seniors
     * @param ageGroup      the age group for the athlete
     */
    public Category(Double minimumWeight, Double maximumWeight, Gender enumGender, Boolean active, Integer wr,
            AgeGroup ageGroup) {
        this.setMinimumWeight(minimumWeight);
        this.setMaximumWeight(maximumWeight);
        this.setGender(enumGender);
        this.setActive(active);
        this.setAgeGroup(ageGroup);
        this.setWr(wr);
        if (wr >= 0) {
            this.setRobiA(1000.0D / Math.pow(wr, ROBI_B));
        }
        setCategoryName(minimumWeight, maximumWeight, enumGender, ageGroup);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Category o) {
        if (o == null) {
            return 1; // we are greater than null;
        }

        int compare = this.ageGroup.compareTo(o.getAgeGroup());
        if (compare != 0) {
            return compare;
        }

        compare = this.gender.compareTo(o.getGender());
        if (compare != 0) {
            return compare;
        }

        // same division, same gender, rank according to maximumWeight.
        Double value1 = this.getMaximumWeight();
        Double value2 = o.getMaximumWeight();
        compare = value1.compareTo(value2);
        return compare;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Category)) {
            return false;
        }
        Category other = (Category) obj;
        return Objects.equals(active, other.active) && Objects.equals(ageGroup, other.ageGroup)
                && gender == other.gender && Objects.equals(id, other.id)
                && Objects.equals(maximumWeight, other.maximumWeight)
                && Objects.equals(minimumWeight, other.minimumWeight) && Objects.equals(name, other.name)
                && Objects.equals(robiA, other.robiA) && Objects.equals(wrSr, other.wrSr);
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

    public List<Athlete> getAthletes() {
        return athletes;
    }

    public String getCode() {
        return code;
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
        String catName = ageGroup.getName() + " ";
        if (maximumWeight > 110) {
            catName = catName + ">" + (int) (Math.round(minimumWeight));
        } else {
            catName = catName + (int) (Math.round(maximumWeight));
        }
        return catName;
    }

    /**
     * Gets the robi A.
     *
     * @return the robi A
     */
    public Double getRobiA() {
        return robiA;
    }

    /**
     * Gets the robi B.
     *
     * @return the robi B
     */
    public Double getRobiB() {
        return ROBI_B;
    }

    /**
     * Gets the wr.
     *
     * @return the wr
     */
    public Integer getWr() {
        if (ageGroup == null) {
            return 0;
        }
        if (ageGroup.getAgeDivision() != AgeDivision.IWF) {
            return 0;
        }
        if (ageGroup.getMaxAge() == 999) {
            return wrSr;
        } else if (ageGroup.getMaxAge() == 20) {
            return wrJr;
        } else if (ageGroup.getMaxAge() == 17) {
            return wrYth;
        } else {
            return 0;
        }
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

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(active, ageGroup, gender, id, maximumWeight, minimumWeight, name, robiA, wrSr);
    }

    /**
     * Checks if is active.
     *
     * @return the boolean
     */
    public Boolean isActive() {
        return active;
    }

    public String longDump() {
        return "Category [name=" + getName() + ", active=" + active + ", id=" + id
                + ", minimumWeight=" + minimumWeight
                + ", maximumWeight=" + maximumWeight + ", ageGroup=" + ageGroup.getName() + ", athletes=" + athletes
                + ", gender="
                + gender + ", wr=" + wrSr + ", code=" + code + ", robiA=" + robiA + "]";
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

    public void setAthletes(List<Athlete> athletes) {
        for (Athlete a : athletes) {
            // manage both sides of the relationship
            a.addEligibleCategory(this);
        }
    }

    private void setCategoryName(Double minimumWeight, Double maximumWeight, Gender enumGender, AgeGroup ageGroup) {
        String catName = ageGroup.getName() + " ";
        if (maximumWeight > 110) {
            catName = catName + ">" + (int) (Math.round(minimumWeight));
        } else {
            catName = catName + (int) (Math.round(maximumWeight));
        }
        // this.setName(catName);
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

    /**
     * Sets the robi A.
     *
     * @param robiA the new robi A
     */
    public void setRobiA(Double robiA) {
        this.robiA = robiA;
    }

//    /**
//     * Sets the name.
//     *
//     * @param name the name to set
//     */
//    public void setName(String name) {
//        this.name = name;
//    }

    /**
     * Sets the wr.
     *
     * @param wr the wr to set
     */
    public void setWr(Integer wr) {
        this.wrSr = wr;
    }

    public void setWrJr(Integer wrJr) {
        this.wrJr = wrJr;
    }

    public void setWrSr(Integer wrSr) {
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
        return name + "_" + active + "_" + gender + "_" + ageGroup;
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
}
