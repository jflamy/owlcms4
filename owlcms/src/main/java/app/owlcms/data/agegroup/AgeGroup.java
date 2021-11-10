/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
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
import app.owlcms.data.category.AgeDivision;
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
@JsonIgnoreProperties({ "hibernateLazyInitializer", "logger" })
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
            ageGroupEquals = Objects.equals(firstAgeGroup.getCode(), otherAgeGroup.getCode())
                    && Objects.equals(firstAgeGroup.getGender(), otherAgeGroup.getGender())
                    && Objects.equals(firstAgeGroup.getMinAge(), otherAgeGroup.getMinAge())
                    && Objects.equals(firstAgeGroup.getMaxAge(), otherAgeGroup.getMaxAge());
        }
        return ageGroupEquals;
    }

    @Transient
    Logger logger = (Logger) LoggerFactory.getLogger(AgeGroup.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private Long id;

    boolean active;

    String code;

    String key;

    public String getKey() {
        return ageDivision.name() + "_" + gender.name() + "_" + minAge + "_" + maxAge;
    }

    public void setKey(String key) {
        // do nothing, this is to fool Json deserialization.
    }

    Integer minAge;

    Integer maxAge;

    @Enumerated(EnumType.STRING)
    private AgeDivision ageDivision;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Transient
    @JsonIgnore
    public String categoriesAsString;

    @OneToMany(mappedBy = "ageGroup", cascade = { CascadeType.ALL }, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Category> categories = new ArrayList<>();

    private Integer qualificationTotal;

    public AgeGroup() {
    }

    public AgeGroup(String code, boolean active, Integer minAge, Integer maxAge, Gender gender,
            AgeDivision ageDivision, Integer qualificationTotal) {
        super();
        this.active = active;
        this.code = code;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.ageDivision = ageDivision;
        this.gender = gender;
        this.setQualificationTotal(qualificationTotal);
    }

    public void addCategory(Category category) {
        if (category != null) {
            categories.add(category);
            category.setAgeGroup(this);
        }
    }

    @Override
    public int compareTo(AgeGroup o) {
        if (o == null) {
            return 1; // we are bigger.
        }
        int compare = 0;

        compare = ObjectUtils.compare(gender, o.getGender());
        if (compare != 0) {
            return compare;
        }
        compare = ObjectUtils.compare(minAge, o.getMinAge());
        if (compare != 0) {
            return compare;
        }
        compare = ObjectUtils.compare(maxAge, o.getMaxAge());

        return compare;
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
        // logger./**/warn("categories {} .equals(other.categories {}) = {}", categories, other.categories,
        // Objects.equals(categories, other.categories));
        return active == other.active && ageDivision == other.ageDivision
                && Objects.equals(categories, other.categories) && Objects.equals(code, other.code)
                && gender == other.gender && Objects.equals(id, other.id) && Objects.equals(maxAge, other.maxAge)
                && Objects.equals(minAge, other.minAge);
    }

    public AgeDivision getAgeDivision() {
        return ageDivision;
    }

    @JsonIgnore
    public List<Category> getAllCategories() {
        return categories;
    }

    /**
     * @return the categories for which we are the AgeGroup
     */
    public List<Category> getCategories() {
        return categories.stream().filter(c -> {
            return !(c.getAgeGroup() == null);
        }).sorted().collect(Collectors.toList());
    }

    public String getCategoriesAsString() {
        if (categories == null || categories.size() == 0) {
            return "";
        }
        return getCategories().stream().map(c -> c.getLimitString()).collect(Collectors.joining(", "));
    }

    public String getCode() {
        return code;
    }

    public Gender getGender() {
        return gender;
    }

    public Long getId() {
        return id;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public Integer getMinAge() {
        return minAge;
    }

    @JsonIgnore
    public String getName() {
        String code2 = this.getCode();
        if (code2 == null) {
            return "";
        }

        String translatedCode = getTranslatedCode(code2);
        if (ageDivision == AgeDivision.MASTERS) {
            return translatedCode;
        } else if (ageDivision == AgeDivision.DEFAULT) {
            return getGender().toString();
        } else {
            return translatedCode + " " + getGender();
        }
    }

    /**
     * @return the qualificationTotal
     */
    public Integer getQualificationTotal() {
        return qualificationTotal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, ageDivision, categories, code, gender, id, maxAge, minAge);
    }

    public boolean isActive() {
        return active;
    }

    public void removeCategory(Category category) {
        if (category != null) {
            category.setAgeGroup(null);
            categories.remove(category);
        }
    }

    public void setActive(boolean active) {
        this.active = active;
    }

//    /**
//     * Set the categories.
//     *
//     * We preserve existing category Ids so as not to pollute the database. Categories with no age group will be removed
//     * when saving. New categories will be persisted by the save.
//     *
//     * @param nCats
//     * @see AgeGroupRepository#save(AgeGroup)
//     */
//    public void setCategories(List<Category> nCats) {
//        logger.debug("ageGroup {} setting categories {}", System.identityHashCode(this), nCats);
//        Map<Long, Category> curCatMap = new HashMap<>();
//        categories.forEach(c -> curCatMap.put(c.getId(), c));
//        for (Category nc : nCats) {
//            Category curCat = curCatMap.get(nc.getId());
//            if (curCat != null) {
//                curCat.setActive(nc.getActive());
//                curCat.setMaximumWeight(nc.getMaximumWeight());
//                curCat.setMinimumWeight(nc.getMinimumWeight());
//                curCat.setAgeGroup(nc.getAgeGroup());
//                curCat.setGender(this.getGender()); // category gets gender of age group
//            } else {
//                categories.add(new Category(nc));
//            }
//        }
//        categories.sort((c1, c2) -> ObjectUtils.compare(c1.getMaximumWeight(), c2.getMaximumWeight()));
//    }

    public void setAgeDivision(AgeDivision ageDivision) {
        this.ageDivision = ageDivision;
    }

    public void setCategories(List<Category> value) {
        this.categories = value;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
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
        return getName();
    }

    private String getTranslatedCode(String code2) {
        String translatedCode = Translator.translateOrElseEn(
                "AgeGroup." + code2,
                OwlcmsSession.getLocale());
        return translatedCode != null ? translatedCode : code2;
    }

}
