package app.owlcms.data.technicalofficial;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import app.owlcms.data.group.Group;
import app.owlcms.utils.IdUtils;

/**
 * Transient in-memory data holder used for building export reports
 * (e.g. JXLSExportTechnicalOfficials).  Not a JPA entity — not
 * registered in the persistence unit and has no database table.
 */
public class SessionAssignment implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private TechnicalOfficial official;
    private Group group;
    private Set<OfficialRole> roles = new HashSet<>();

    public SessionAssignment() {
        setId(IdUtils.getTimeBasedId());
    }

    public SessionAssignment(TechnicalOfficial official, Group group) {
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

    public Group getSession() {
        return group;
    }

    public void setSession(Group session) {
        this.group = session;
    }

    public Set<OfficialRole> getRoles() {
        return roles;
    }

    public String getRoleAsString() {
        return roles.stream().findFirst().map(Object::toString).orElse("");
    }

    public void setRoleAsString(String unused) {
        // no-op
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
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        SessionAssignment other = (SessionAssignment) obj;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
