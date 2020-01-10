/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */

package app.owlcms.data.category;

import java.io.Serializable;
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
import javax.persistence.ManyToOne;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Gender;
import app.owlcms.i18n.Translator;

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
public class Category implements Serializable, Comparable<Category>, Cloneable {

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

    public Category(Category c) {
        this(c.id, c.minimumWeight, c.maximumWeight, c.gender, c.active, c.wrYth, c.wrJr, c.wrSr, c.ageGroup);
    }

    public Category(Long id, Double minimumWeight, Double maximumWeight, Gender gender, boolean active, Integer wrYth,
            Integer wrJr, Integer wrSr, AgeGroup ageGroup) {
        this.setId(id);
        this.setMinimumWeight(minimumWeight);
        this.setMaximumWeight(maximumWeight);
        this.setGender(gender);
        this.setActive(active);
        this.setAgeGroup(ageGroup);
        this.setWrYth(wrYth);
        this.setWrJr(wrJr);
        this.setWrSr(wrSr);
        if (wrSr >= 0) {
            this.setRobiA(1000.0D / Math.pow(wrSr, ROBI_B));
        }
        setCategoryName(minimumWeight, maximumWeight, gender, ageGroup);
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

        int compare = ObjectUtils.compare(this.ageGroup, o.getAgeGroup(), true);
        if (compare != 0) {
            return compare;
        }

        compare = ObjectUtils.compare(this.gender, o.getGender());
        if (compare != 0) {
            return compare;
        }

        // same division, same gender, rank according to maximumWeight.
        Double value1 = this.getMaximumWeight();
        Double value2 = o.getMaximumWeight();
        compare = ObjectUtils.compare(value1, value2);
        return compare;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Category other = (Category) obj;
        return active == other.active && Objects.equals(ageGroup, other.ageGroup) && Objects.equals(code, other.code)
                && gender == other.gender && Objects.equals(id, other.id)
                && Objects.equals(maximumWeight, other.maximumWeight)
                && Objects.equals(minimumWeight, other.minimumWeight) && Objects.equals(name, other.name)
                && Objects.equals(robiA, other.robiA) && Objects.equals(wrJr, other.wrJr)
                && Objects.equals(wrSr, other.wrSr) && Objects.equals(wrYth, other.wrYth);
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

    public String getLimitString() {
        if (maximumWeight > 110) {
            return Translator.translate("catAboveFormat", String.valueOf((int) (Math.round(minimumWeight))));
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
        if (ageGroup == null) {
            return null;
        }
        if (maximumWeight == null) {
            return null;
        }
        String agName = ageGroup.getName();
        String catName = getLimitString();
        if (agName == null || agName.isEmpty()) {
            return catName;
        } else {
            return agName + " " + catName;
        }
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

    @Override
    public int hashCode() {
        return Objects.hash(active, ageGroup, code, gender, id, maximumWeight, minimumWeight, name, robiA, wrJr, wrSr,
                wrYth);
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
                + ", maximumWeight=" + maximumWeight + ", ageGroup=" + ageGroup.getName()
                + ", gender="
                + gender + ", wr=" + wrSr + ", code=" + code + ", robiA=" + robiA + "]";
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

    public void setCategoryName(Double minimumWeight, Double maximumWeight, Gender enumGender, AgeGroup ageGroup) {
        // does nothing
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

//    /**
//     * Sets the name.
//     *
//     * @param name the name to set
//     */
//    public void setName(String name) {
//        this.name = name;
//    }

    /**
     * Sets the robi A.
     *
     * @param robiA the new robi A
     */
    public void setRobiA(Double robiA) {
        this.robiA = robiA;
    }

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
