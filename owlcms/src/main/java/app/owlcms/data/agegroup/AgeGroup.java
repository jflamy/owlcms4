/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.agegroup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
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

    @OneToMany(mappedBy = "ageGroup", cascade = { CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.REFRESH },
//        orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<Category> categories = new ArrayList<>();

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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AgeGroup other = (AgeGroup) obj;
        return Objects.equals(id, other.id);
    }

    public AgeDivision getAgeDivision() {
        return ageDivision;
    }

    public List<Category> getAllCategories() {
        return categories;
    }

    /**
     * @return the categories for which we are the AgeGroup
     */
    public List<Category> getCategories() {
//        // simpler to use a query; it is sufficient to call Category.setAgeGroup()
//        // to manage the relationship.
//        return JPAService
//                .runInTransaction(em -> em
//                        .createQuery("select c " + "from Category c "
//                                + "where c.ageGroup.id = :agId order by c.maximumWeight", Category.class)
//                        .setParameter("agId", this.getId()).getResultList());
        return categories.stream().filter(c -> {
            return !(c.getAgeGroup() == null);
        }).sorted().collect(Collectors.toList());
//        return categories;
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

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean isActive() {
        return active;
    }

    public void removeCategory(Category category) {
        if (category != null) {
            // logger.warn("ageGroup={} removing {} {}", this.getId(), category.getCode(), category.getId());
            category.setAgeGroup(null);
            categories.remove(category);
            // logger.warn("ageGroup={} removed {} {}", this.getId(), category.getCode(), category.getId());
        }
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setAgeDivision(AgeDivision ageDivision) {
        this.ageDivision = ageDivision;
    }

    /**
     * Set the categories.
     *
     * We preserve existing category Ids so as not to pollute the database. Categories with no age group will be removed
     * when saving. New categories will be persisted by the save.
     *
     * @param nCats
     * @see AgeGroupRepository#save(AgeGroup)
     */
    public void setCategories(List<Category> nCats) {
        logger.debug("ageGroup {} setting categories {}", System.identityHashCode(this), nCats);
        Map<Long, Category> curCatMap = new HashMap<>();
        categories.forEach(c -> curCatMap.put(c.getId(), c));
        for (Category nc : nCats) {
            Category curCat = curCatMap.get(nc.getId());
            if (curCat != null) {
                curCat.setActive(nc.getActive());
                curCat.setMaximumWeight(nc.getMaximumWeight());
                curCat.setMinimumWeight(nc.getMinimumWeight());
                curCat.setAgeGroup(nc.getAgeGroup());
            } else {
                categories.add(new Category(nc));
            }
        }
        categories.sort((c1, c2) -> ObjectUtils.compare(c1.getMaximumWeight(), c2.getMaximumWeight()));
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
