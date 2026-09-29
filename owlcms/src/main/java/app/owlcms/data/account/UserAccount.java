package app.owlcms.data.account;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

import app.owlcms.utils.IdUtils;

// must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@SuppressWarnings("serial")
@Entity
@Cacheable
public class UserAccount implements Serializable {

	public static final String BUILT_IN_ADMIN = "admin";

	@Id
	private Long id;

	@Column(unique = true, nullable = false)
	private String username;

	private String displayName;

	private String passwordHash;

	private boolean enabled = true;

	@ElementCollection(fetch = FetchType.EAGER)
	private List<RoleGrant> grants = new ArrayList<>();

	public UserAccount() {
		this.id = IdUtils.getTimeBasedId();
	}

	/** Detached copy, so that editing a grid row does not alter the cached account. */
	public UserAccount copy() {
		UserAccount copy = new UserAccount();
		copy.id = this.id;
		copy.username = this.username;
		copy.displayName = this.displayName;
		copy.passwordHash = this.passwordHash;
		copy.enabled = this.enabled;
		for (RoleGrant grant : this.grants) {
			copy.grants.add(new RoleGrant(grant.getRole(), grant.getPlatformName()));
		}
		return copy;
	}

	public static String normalizeUsername(String username) {
		return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
	}

	public boolean isBuiltInAdmin() {
		return BUILT_IN_ADMIN.equals(this.username);
	}

	public boolean hasPassword() {
		return this.passwordHash != null && !this.passwordHash.isBlank();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.id != null && this.id.equals(((UserAccount) obj).id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = normalizeUsername(username);
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getPasswordHash() {
		return this.passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<RoleGrant> getGrants() {
		return this.grants;
	}

	public void setGrants(List<RoleGrant> grants) {
		this.grants = grants != null ? grants : new ArrayList<>();
	}

	/** Name shown to people; falls back to the login name. */
	public String getShownName() {
		return this.displayName != null && !this.displayName.isBlank() ? this.displayName : this.username;
	}

	@Override
	public String toString() {
		return this.username;
	}
}
