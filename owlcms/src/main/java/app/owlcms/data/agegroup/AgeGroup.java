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

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.jpa.JPAService;

@Entity
@Cacheable
public class AgeGroup implements Comparable<AgeGroup>, Serializable {

    private static final long serialVersionUID = 8154757158144876816L;

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
        if (ageDivision == AgeDivision.MASTERS) {
            return getCode();
        } else if (ageDivision == AgeDivision.DEFAULT) {
            return getGender().toString();
        } else {
            return getCode() + " " + getGender();
        }
    }

//    public void addCategory(Category category) {
//        if (category != null) category.setAgeGroup(this);
//    }
//
//    public void removeCategory(Category category) {
//        if (category != null) category.setAgeGroup(null);
//    }

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

}
