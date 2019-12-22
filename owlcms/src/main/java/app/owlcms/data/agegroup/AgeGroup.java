/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.agegroup;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@Entity
@Cacheable
public class AgeGroup implements Comparable<AgeGroup>, Serializable {

    private static final long serialVersionUID = 8154757158144876816L;
    Logger logger = (Logger) LoggerFactory.getLogger(AgeGroup.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    boolean active;

    String code;

    Integer minAge;

    Integer maxAge;

    @Enumerated(EnumType.STRING)
    private AgeDivision ageDivision;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Transient
    public String categoriesAsString;

    public AgeGroup() {
    }

    public AgeGroup(String code, boolean active, Integer minAge, Integer maxAge, Gender gender,
            AgeDivision ageDivision) {
        super();
        this.active = active;
        this.code = code;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.ageDivision = ageDivision;
        this.gender = gender;
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
        compare = ObjectUtils.compare(ageDivision, o.getAgeDivision());
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

    public AgeDivision getAgeDivision() {
        return ageDivision;
    }

    /**
     * @return the categories for which we are the AgeGroup
     */
    public List<Category> getCategories() {
        // simpler to use a query; it is sufficient to call Category.setAgeGroup()
        // to manage the relationship. 
        return JPAService
                .runInTransaction(em -> em
                        .createQuery("select c " + "from Category c "
                                + "where c.ageGroup.id = :agId order by c.maximumWeight", Category.class)
                        .setParameter("agId", this.getId()).getResultList());

    }

    public String getCategoriesAsString() {
        List<Category> cats = getCategories();
        int previousMax = 0;
        StringBuilder buf = new StringBuilder();
        for (Category cat : cats) {
            Double maximumWeight = cat.getMaximumWeight();
            if (maximumWeight.compareTo(998.9D) > 0) {
                buf.append(" >");
                buf.append(previousMax);
                break;
            } else {
                int curMax = (int) Math.round(maximumWeight);
                buf.append(curMax);
                buf.append(", ");
                previousMax = curMax;
            }
        }
        return buf.toString();
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

//    public void addCategory(Category category) {
//        if (category != null) category.setAgeGroup(this);
//    }
//
//    public void removeCategory(Category category) {
//        if (category != null) category.setAgeGroup(null);
//    }

    public String getName() {
        String code2 = this.getCode();
        String translatedCode = getTranslatedCode();
        if (ageDivision == AgeDivision.MASTERS) {
            return translatedCode != null ? translatedCode : code2;
        } else if (ageDivision == AgeDivision.DEFAULT) {
            return getGender().toString();
        } else {
            translatedCode = translatedCode != null ? translatedCode : code2;
            return translatedCode + " " + getGender();
        }
    }

    private String getTranslatedCode() {
        return Translator.translateOrElseEn(
                "AgeGroup." + getCode(),
                OwlcmsSession.getLocale());
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setAgeDivision(AgeDivision ageDivision) {
        this.ageDivision = ageDivision;
    }

    public void setCategories(Set<Category> categories) {
        for (Category category : categories) {
            category.setAgeGroup(this);
        }
    }

//    public void setCategoriesAsString(String unused) {
//    }

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

    @Override
    public String toString() {
        return getName();
    }

}
