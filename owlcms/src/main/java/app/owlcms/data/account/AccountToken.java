package app.owlcms.data.account;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Embeddable;

/**
 * An access token owned by an account. Only the SHA-256 of the token is stored and exported; the token itself is
 * shown once when generated. The identifier is not secret and names the token in the audit trail and the UI.
 */
@SuppressWarnings("serial")
@Embeddable
public class AccountToken implements Serializable {

	private String tokenId;
	private String tokenHash;
	private LocalDateTime createdAt;

	public AccountToken() {
	}

	public AccountToken(String tokenId, String tokenHash, LocalDateTime createdAt) {
		this.tokenId = tokenId;
		this.tokenHash = tokenHash;
		this.createdAt = createdAt;
	}

	public AccountToken copy() {
		return new AccountToken(this.tokenId, this.tokenHash, this.createdAt);
	}

	public String getTokenId() {
		return this.tokenId;
	}

	public void setTokenId(String tokenId) {
		this.tokenId = tokenId;
	}

	public String getTokenHash() {
		return this.tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof AccountToken other)) {
			return false;
		}
		return Objects.equals(this.tokenId, other.tokenId);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.tokenId);
	}

	@Override
	public String toString() {
		return this.tokenId;
	}
}
