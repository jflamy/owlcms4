package app.owlcms.data.account;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import app.owlcms.audit.AuditFormat;
import app.owlcms.utils.IdUtils;

// must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@SuppressWarnings("serial")
@Entity
@Cacheable
// exports from earlier versions carry a displayName that is no longer used
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAccount implements Serializable {

	public static final String BUILT_IN_ADMIN = "admin";

	@Id
	private Long id;

	@Column(unique = true, nullable = false)
	private String username;

	private String passwordHash;

	private boolean enabled = true;

	@Column(columnDefinition = "boolean default false")
	private boolean mustChangePassword;

	/** Created by OWLCMS for a role (and platform) rather than for a person; the built-in admin is always generic. */
	@Column(columnDefinition = "boolean default false")
	private boolean generic;

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
		copy.passwordHash = this.passwordHash;
		copy.enabled = this.enabled;
		copy.mustChangePassword = this.mustChangePassword;
		copy.generic = this.generic;
		for (RoleGrant grant : this.grants) {
			copy.grants.add(new RoleGrant(grant.getRole(), grant.getPlatformName()));
		}
		return copy;
	}

	public static String normalizeUsername(String username) {
		return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
	}

	/**
	 * Audit detail for a change to an account: changed field names, grants given and taken away, and the previous
	 * username after a rename. Never contains password values or hashes.
	 *
	 * @param before the account before the change, null when it was created
	 * @param after  the account after the change, null when it was deleted
	 * @return the detail, or null when nothing changed
	 */
	public static String auditDetail(UserAccount before, UserAccount after) {
		UserAccount current = after != null ? after : before;
		List<String> changes = new ArrayList<>();
		if (before != null && after != null) {
			if (!Objects.equals(before.getUsername(), after.getUsername())) {
				changes.add("username");
			}
			if (before.isEnabled() != after.isEnabled()) {
				changes.add(after.isEnabled() ? "enabled" : "disabled");
			}
		}
		boolean hadPassword = before != null && before.hasPassword();
		boolean hasPassword = after != null && after.hasPassword();
		if (after != null && hasPassword
		        && (before == null || !Objects.equals(before.getPasswordHash(), after.getPasswordHash()))) {
			changes.add("password");
		} else if (before != null && after != null && hadPassword && !hasPassword) {
			changes.add("password-removed");
		}
		boolean mustBefore = before != null && before.isMustChangePassword();
		boolean mustAfter = after != null && after.isMustChangePassword();
		if (after != null && mustAfter && !mustBefore) {
			changes.add("must-change-set");
		} else if (before != null && after != null && mustBefore && !mustAfter) {
			changes.add("must-change-cleared");
		}
		Set<RoleGrant> grantsBefore = grantSet(before);
		Set<RoleGrant> grantsAfter = grantSet(after);
		List<String> granted = grantNames(grantsAfter, grantsBefore);
		List<String> revoked = grantNames(grantsBefore, grantsAfter);
		if (before != null && after != null && changes.isEmpty() && granted.isEmpty() && revoked.isEmpty()) {
			return null;
		}
		if (before != null && after != null && (!granted.isEmpty() || !revoked.isEmpty())) {
			changes.add("grants");
		}
		List<String> detail = new ArrayList<>();
		detail.add(AuditFormat.kv("account", current.getUsername()));
		if (before != null && after != null && changes.contains("username")) {
			detail.add(AuditFormat.kv("previous", before.getUsername()));
		}
		if (!changes.isEmpty()) {
			detail.add(AuditFormat.kv("changes", String.join(",", changes)));
		}
		if (!granted.isEmpty()) {
			detail.add(AuditFormat.kv("granted", String.join(",", granted)));
		}
		if (!revoked.isEmpty()) {
			detail.add(AuditFormat.kv("revoked", String.join(",", revoked)));
		}
		return String.join(",", detail);
	}

	private static Set<RoleGrant> grantSet(UserAccount account) {
		return account == null || account.getGrants() == null ? Set.of() : new HashSet<>(account.getGrants());
	}

	/** @return ROLE@platform, or ROLE for a grant on all platforms, for grants in {@code from} missing in {@code minus} */
	private static List<String> grantNames(Set<RoleGrant> from, Set<RoleGrant> minus) {
		return from.stream().filter(g -> !minus.contains(g))
		        .map(g -> g.getRole() + (g.getPlatformName() == null ? "" : "@" + g.getPlatformName()))
		        .sorted().toList();
	}

	public boolean isBuiltInAdmin() {
		return BUILT_IN_ADMIN.equals(normalizeUsername(this.username));
	}

	public boolean hasPassword() {
		return this.passwordHash != null && !this.passwordHash.isBlank();
	}

	@JsonIgnore
	public boolean isPasswordChangeRequired() {
		return !hasPassword();
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
		this.username = username == null ? null : username.trim();
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

	/** The user must replace the current password before opening any other page. */
	public boolean isMustChangePassword() {
		return this.mustChangePassword;
	}

	public void setMustChangePassword(boolean mustChangePassword) {
		this.mustChangePassword = mustChangePassword;
	}

	public boolean isGeneric() {
		return this.generic || isBuiltInAdmin();
	}

	public void setGeneric(boolean generic) {
		this.generic = generic;
	}

	public List<RoleGrant> getGrants() {
		return this.grants;
	}

	public void setGrants(List<RoleGrant> grants) {
		this.grants = grants != null ? grants : new ArrayList<>();
	}

	@Override
	public String toString() {
		return this.username;
	}
}
