package app.owlcms.data.technicalofficial;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Cacheable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import app.owlcms.data.group.Group;  // Fixed import
import app.owlcms.utils.IdUtils;

@Entity
@Cacheable
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer" })
public class GroupAssignment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    private TechnicalOfficial official;

    @ManyToOne(fetch = FetchType.EAGER)
    private Group group;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<OfficialRole> roles = new HashSet<>();

    public GroupAssignment() {
        setId(IdUtils.getTimeBasedId());
    }

    public GroupAssignment(TechnicalOfficial official, Group group) {
        this();
        this.official = official;
        this.group = group;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TechnicalOfficial getOfficial() {
        return official;
    }

    public void setOfficial(TechnicalOfficial official) {
        this.official = official;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Set<OfficialRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<OfficialRole> roles) {
        this.roles = roles;
    }

    public void addRole(OfficialRole role) {
        this.roles.add(role);
    }

    public void removeRole(OfficialRole role) {
        this.roles.remove(role);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GroupAssignment other = (GroupAssignment) obj;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
