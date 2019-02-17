/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Affero GNU License amended with the
 * Commons Clause.
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.data.category;

import java.io.Serializable;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.ledocte.owlcms.data.athlete.Gender;

/**
 * Contains information regarding each competition category.
 *
 * A category is the combination of an age division and a weight class. Categories also define information for the computation of Robi
 * points Robi = A x (total)^b where b = log(10)/log(2) (any logrithm base) A = 1000 / [ (WR)^b ] WR = World Record
 *
 * @author owlcms
 *
 */
@SuppressWarnings("serial")
@Entity
@Cacheable
public class Category implements Serializable, Comparable<Category> {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    /** The name. */
    String name;

    /** The minimum weight. */
    Double minimumWeight; // inclusive

    /** The maximum weight. */
    Double maximumWeight; // exclusive
    
    @Enumerated(EnumType.STRING)
    private AgeDivision ageDivision;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    /** The active. */
    Boolean active;

    private Integer wr;

    private Double robiA = 0.0D;

    private final static Double ROBI_B = 3.321928095;


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
     * @param enumGender the enum gender
     * @param active the active
     * @param enumAgeDivision the enum age division
     * @param wr the wr
     */
    public Category(Double minimumWeight, Double maximumWeight, Gender enumGender, Boolean active, AgeDivision enumAgeDivision, Integer wr) {
        this.setMinimumWeight(minimumWeight);
        this.setMaximumWeight(maximumWeight);
        this.setGender(enumGender);
        this.setAgeDivision(enumAgeDivision);
        this.setActive(active);
        this.setWr(wr);
        if (wr >= 0) {
            this.setRobiA(1000.0D/Math.pow(wr,ROBI_B));
        }

//        this.setGender(gender.toString());
//        this.setAgeDivision(ageDivision.getCode());
        setCategoryName(minimumWeight, maximumWeight, enumGender, enumAgeDivision);
    }

//    public Category(Double minimumWeight, Double maximumWeight, String genderCode, Boolean active, String ageDivisionCode, Integer wr) {
//        this.setMinimumWeight(minimumWeight);
//        this.setMaximumWeight(maximumWeight);
//        this.setGender(genderCode);
//        this.setActive(active);
//        this.setAgeDivision(ageDivisionCode);
//        this.setWr(wr);
//        if (wr >= 0) {
//            this.setRobiA(1000.0D/Math.pow(wr,ROBI_B));
//        }
//
//        setCategoryName(minimumWeight, maximumWeight, this.getEnumGender(), this.getEnumAgeDivision());
//    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Category o) {
        if (o == null)
            return 1; // we are greater than null;

        int compare = this.ageDivision.compareTo(o.getAgeDivision());
        if (compare != 0)
            return compare;

        compare = this.gender.compareTo(o.getGender());
        if (compare != 0)
            return compare;

        // same division, same gender, rank according to maximumWeight.
        Double value1 = this.getMaximumWeight();
        Double value2 = o.getMaximumWeight();
        compare = value1.compareTo(value2);
        return compare;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Category other = (Category) obj;
        if (active == null) {
            if (other.active != null)
                return false;
        } else if (!active.equals(other.active))
            return false;
        if (ageDivision == null) {
            if (other.ageDivision != null)
                return false;
        } else if (!ageDivision.equals(other.ageDivision))
            return false;
        if (ageDivision != other.ageDivision)
            return false;
        if (gender != other.gender)
            return false;
        if (gender == null) {
            if (other.gender != null)
                return false;
        } else if (!gender.equals(other.gender))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (maximumWeight == null) {
            if (other.maximumWeight != null)
                return false;
        } else if (!maximumWeight.equals(other.maximumWeight))
            return false;
        if (minimumWeight == null) {
            if (other.minimumWeight != null)
                return false;
        } else if (!minimumWeight.equals(other.minimumWeight))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    /**
     * Gets the active.
     *
     * @return the active
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * Gets the age division.
     *
     * @return the age division
     */
    public AgeDivision getAgeDivision() {
        return ageDivision;
    }

    /**
     * Gets the gender.
     *
     * @return the gender
     */
    public Gender getGender() {
        if (gender == null) {
            this.gender = Gender.UNKOWN;
        }
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
        return name;
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
     * Gets the wr.
     *
     * @return the wr
     */
    public Integer getWr() {
        if (wr == null) return 0;
        return wr;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((active == null) ? 0 : active.hashCode());
        result = prime * result + ((ageDivision == null) ? 0 : ageDivision.hashCode());
        result = prime * result + ((ageDivision == null) ? 0 : ageDivision.hashCode());
        result = prime * result + ((gender == null) ? 0 : gender.hashCode());
        result = prime * result + ((gender == null) ? 0 : gender.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((maximumWeight == null) ? 0 : maximumWeight.hashCode());
        result = prime * result + ((minimumWeight == null) ? 0 : minimumWeight.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /**
     * Checks if is active.
     *
     * @return the boolean
     */
    public Boolean isActive() {
        return active;
    }

    /**
     * Sets the active.
     *
     * @param active the new active
     */
    public void setActive(Boolean active) {
        this.active = active;
    }

    private void setCategoryName(Double minimumWeight, Double maximumWeight, Gender enumGender, AgeDivision enumAgeGroup) {
        String catName = enumAgeGroup.getCode() + enumGender.toString();
        if (maximumWeight > 110) {
            catName = catName + ">" + (int) (Math.round(minimumWeight));
        } else {
            catName = catName + (int) (Math.round(maximumWeight));
        }
        this.setName(catName);
    }

    /**
     * Sets the age division.
     *
     * @param enumAgeGroup2 the new age division
     */
    public void setAgeDivision(AgeDivision enumAgeGroup2) {
        if (enumAgeGroup2 == null){
            this.ageDivision = AgeDivision.DEFAULT;
        } else {
            this.ageDivision = enumAgeGroup2;
        }
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
     * @param maximumWeight            the maximumWeight to set
     */
    public void setMaximumWeight(Double maximumWeight) {
        this.maximumWeight = maximumWeight;
    }

    /**
     * Sets the minimum weight.
     *
     * @param minimumWeight            the minimumWeight to set
     */
    public void setMinimumWeight(Double minimumWeight) {
        this.minimumWeight = minimumWeight;
    }

    /**
     * Sets the name.
     *
     * @param name            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

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
     * @param wr            the wr to set
     */
    public void setWr(Integer wr) {
        this.wr = wr;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name + "_" + active + "_" + gender + "_" + ageDivision;
    }
    
    /**
     * Short dump.
     *
     * @return the string
     */
    public String shortDump() {
        return name + "_" + active;
    }

    /**
     * Gets the robi B.
     *
     * @return the robi B
     */
    public Double getRobiB() {
        return ROBI_B;
    }
}
