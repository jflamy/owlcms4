package app.owlcms.data.access;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

import app.owlcms.access.RememberedBrowser.Kind;

/**
 * A browser allowed to start a new session without presenting the PIN or password again.
 */
@Entity
public class RememberedLogin {

	@Id
	private String selector;
	@Column(nullable = false)
	private String validatorHash;
	@Column(nullable = false)
	private String credentialCheck;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Kind kind;
	private Long accountId;
	private String loginPlatform;
	@Column(nullable = false)
	private Instant issuedAt;
	@Column(nullable = false)
	private Instant expiresAt;
	@Column(nullable = false)
	private Instant lastUsedAt;
	private String clientIp;
	@Column(length = 512)
	private String userAgent;

	public RememberedLogin() {
	}

	public String getSelector() {
		return this.selector;
	}

	public void setSelector(String selector) {
		this.selector = selector;
	}

	public String getValidatorHash() {
		return this.validatorHash;
	}

	public void setValidatorHash(String validatorHash) {
		this.validatorHash = validatorHash;
	}

	public String getCredentialCheck() {
		return this.credentialCheck;
	}

	public void setCredentialCheck(String credentialCheck) {
		this.credentialCheck = credentialCheck;
	}

	public Kind getKind() {
		return this.kind;
	}

	public void setKind(Kind kind) {
		this.kind = kind;
	}

	public Long getAccountId() {
		return this.accountId;
	}

	public void setAccountId(Long accountId) {
		this.accountId = accountId;
	}

	public String getLoginPlatform() {
		return this.loginPlatform;
	}

	public void setLoginPlatform(String loginPlatform) {
		this.loginPlatform = loginPlatform;
	}

	public Instant getIssuedAt() {
		return this.issuedAt;
	}

	public void setIssuedAt(Instant issuedAt) {
		this.issuedAt = issuedAt;
	}

	public Instant getExpiresAt() {
		return this.expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Instant getLastUsedAt() {
		return this.lastUsedAt;
	}

	public void setLastUsedAt(Instant lastUsedAt) {
		this.lastUsedAt = lastUsedAt;
	}

	public String getClientIp() {
		return this.clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public String getUserAgent() {
		return this.userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
}
