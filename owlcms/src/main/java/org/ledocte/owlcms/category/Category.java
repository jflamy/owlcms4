/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.ledocte.owlcms.category;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.ledocte.owlcms.data.AgeDivision;
import org.ledocte.owlcms.data.Gender;

/**
 * Contains information regarding each competition category.
 *
 * A category is the combination of an age division and a weight class. Categories also define information for the computation of Robi
 * points Robi = A x (total)^b where b = log(10)/log(2) (any logrithm base) A = 1000 / [ (WR)^b ] WR = World Record
 *
 * @author owlcms
 *
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Category implements Serializable, Comparable<Category> {

    /**
     *
     */
    private static final long serialVersionUID = -6364919176226698835L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    String name;
    
    // @Enumerated(EnumType.STRING)
    private String gender;

    Double minimumWeight; // inclusive

    Double maximumWeight; // exclusive

    Boolean active;

    String ageDivision;

    private Integer wr;

    private Double robiA = 0.0D;

    private final static Double ROBI_B = 3.321928095;

    private AgeDivision enumAgeDivision;

    private Gender enumGender;

    public Category() {
    }

    public Category(Double minimumWeight, Double maximumWeight, Gender enumGender, Boolean active, AgeDivision enumAgeDivision, Integer wr) {
        this.setMinimumWeight(minimumWeight);
        this.setMaximumWeight(maximumWeight);
        this.setEnumGender(enumGender);
        this.setEnumAgeDivision(enumAgeDivision);
        this.setActive(active);
        this.setWr(wr);
        if (wr >= 0) {
            this.setRobiA(1000.0D/Math.pow((double)wr,ROBI_B));
        }

        this.setGender(enumGender.toString());
        this.setAgeDivision(enumAgeDivision.getCode());
        setCategoryName(minimumWeight, maximumWeight, enumGender, enumAgeDivision);
    }

    public Category(Double minimumWeight, Double maximumWeight, String genderCode, Boolean active, String ageDivisionCode, Integer wr) {
        this.setMinimumWeight(minimumWeight);
        this.setMaximumWeight(maximumWeight);
        this.setGender(genderCode);
        this.setActive(active);
        this.setAgeDivision(ageDivisionCode);
        this.setWr(wr);
        if (wr >= 0) {
            this.setRobiA(1000.0D/Math.pow(wr,ROBI_B));
        }

        setCategoryName(minimumWeight, maximumWeight, this.getEnumGender(), this.getEnumAgeDivision());
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Category o) {
        if (o == null)
            return 1; // we are greater than null;

        int compare = this.enumAgeDivision.compareTo(o.getEnumAgeDivision());
        if (compare != 0)
            return compare;

        compare = this.enumGender.compareTo(o.getEnumGender());
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
        if (enumAgeDivision != other.enumAgeDivision)
            return false;
        if (enumGender != other.enumGender)
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

    public Boolean getActive() {
        return active;
    }

    public String GetAgeGroup() {
        return ageDivision;
    }

    public AgeDivision getEnumAgeDivision() {
        return enumAgeDivision;
    }

    public Gender getEnumGender() {
        if (enumGender == null) {
            this.enumGender = Gender.UNKOWN;
            this.gender = "?";
        }
        return enumGender;
    }

    /**
     * @return the gender
     */
    public String getGender() {
        return gender;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the maximumWeight
     */
    public Double getMaximumWeight() {
        return maximumWeight;
    }

    /**
     * @return the minimumWeight
     */
    public Double getMinimumWeight() {
        return minimumWeight;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public Double getRobiA() {
        return robiA;
    }

    /**
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
        result = prime * result + ((enumAgeDivision == null) ? 0 : enumAgeDivision.hashCode());
        result = prime * result + ((enumGender == null) ? 0 : enumGender.hashCode());
        result = prime * result + ((gender == null) ? 0 : gender.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((maximumWeight == null) ? 0 : maximumWeight.hashCode());
        result = prime * result + ((minimumWeight == null) ? 0 : minimumWeight.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    public Boolean isActive() {
        return active;
    }

    // private static Locale setLocale() {
    // Locale locale = WebApplicationConfiguration.getLocale();
    // String testString = Messages.getString("Category.f48", locale);
    // if (testString.startsWith("Category.")) {
    // locale = Locale.getDefault();
    // testString = Messages.getString("Category.f48", locale);
    // if (testString.startsWith("Category.")) {
    // locale = Locale.ENGLISH;
    // }
    // }
    // return locale;
    // }

    public void setActive(Boolean active) {
        this.active = active;
    }

    /**
     * @return the ageDivision
     */
    public String getAgeDivision() {
        if (ageDivision == null) {
            setAgeDivision(null); // will set enum correctly;
        }
        return ageDivision;
    }

    public void setAgeDivision(String ageDivision) {
        this.ageDivision = ageDivision;
        if (ageDivision == null || ageDivision.equals("")) {
            this.ageDivision = "";
            setEnumAgeDivision(AgeDivision.DEFAULT);
        } else if (enumAgeDivision == null || !ageDivision.equals(enumAgeDivision.getCode())) {
            setEnumAgeDivision(AgeDivision.getAgeDivisionFromCode(ageDivision));
        } else {
            setEnumAgeDivision(AgeDivision.DEFAULT);
        }
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

    public void setEnumAgeDivision(AgeDivision enumAgeGroup2) {
        if (enumAgeGroup2 == null){
            this.enumAgeDivision = AgeDivision.DEFAULT;
        } else {
            this.enumAgeDivision = enumAgeGroup2;
        }

    }

    public void setEnumGender(Gender enumGender) {
        this.enumGender = enumGender;
    }

    /**
     * @param string
     *            the gender to set
     */
    public void setGender(String string) {
        this.gender = string;
        if (enumGender == null || !string.equals(enumGender.toString())) {
            try {
                enumGender = Gender.valueOf(string.toUpperCase());
            } catch (Exception e) {
                throw new RuntimeException("Must be m or f");
            }
        }
    }

    /**
     * @param maximumWeight
     *            the maximumWeight to set
     */
    public void setMaximumWeight(Double maximumWeight) {
        this.maximumWeight = maximumWeight;
    }

    /**
     * @param minimumWeight
     *            the minimumWeight to set
     */
    public void setMinimumWeight(Double minimumWeight) {
        this.minimumWeight = minimumWeight;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public void setRobiA(Double robiA) {
        this.robiA = robiA;
    }

    /**
     * @param wr
     *            the wr to set
     */
    public void setWr(Integer wr) {
        this.wr = wr;
    }

    @Override
    public String toString() {
        return name + "_" + active;
    }

    public Double getRobiB() {
        return ROBI_B;
    }
}
