package app.owlcms.data.account;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import app.owlcms.access.Role;

/** A role held by an account, for one platform (by name) or for all platforms when the name is null. */
@Embeddable
public class RoleGrant {

	@Enumerated(EnumType.STRING)
	@Column(name = "roleName")
	private Role role;

	private String platformName;

	public RoleGrant() {
	}

	public RoleGrant(Role role, String platformName) {
		this.role = role;
		this.platformName = platformName;
	}

	public Role getRole() {
		return this.role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public String getPlatformName() {
		return this.platformName;
	}

	public void setPlatformName(String platformName) {
		this.platformName = platformName;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof RoleGrant other)) {
			return false;
		}
		return this.role == other.role && Objects.equals(this.platformName, other.platformName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.role, this.platformName);
	}

	@Override
	public String toString() {
		return this.role + (this.platformName != null ? " (" + this.platformName + ")" : "");
	}
}
