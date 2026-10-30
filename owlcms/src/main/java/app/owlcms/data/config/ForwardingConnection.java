package app.owlcms.data.config;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import app.owlcms.audit.AuditIntegrity;
import app.owlcms.audit.AuditSigningKey;
import ch.qos.logback.classic.Logger;

public class ForwardingConnection {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(ForwardingConnection.class);
	private static final Set<String> reportedUnusableKeys = ConcurrentHashMap.newKeySet();

	private boolean active = true;
	private boolean controlPanelManaged;
	private String updateKey;
	private String url;

	public ForwardingConnection() {
	}

	public ForwardingConnection(String url, String updateKey) {
		this(url, updateKey, true, false);
	}

	public ForwardingConnection(String url, String updateKey, boolean active, boolean controlPanelManaged) {
		this.url = url;
		setUpdateKey(updateKey);
		this.active = active;
		this.controlPanelManaged = controlPanelManaged;
	}

	public ForwardingConnection(ForwardingConnection connection) {
		this.url = connection.url;
		this.updateKey = connection.updateKey;
		this.active = connection.active;
		this.controlPanelManaged = connection.controlPanelManaged;
	}

	static ForwardingConnection fromStored(String url, String updateKey, boolean active, boolean controlPanelManaged) {
		ForwardingConnection connection = new ForwardingConnection();
		connection.url = url;
		connection.updateKey = normalizeKey(updateKey);
		connection.active = active;
		connection.controlPanelManaged = controlPanelManaged;
		return connection;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isControlPanelManaged() {
		return controlPanelManaged;
	}

	/**
	 * @return the plain key; null if none, or if the stored key cannot be decrypted with this installation's audit key
	 *         (for example an export from accounts mode with another PEM key), in which case it must be entered again
	 */
	@JsonIgnore
	public String getUpdateKey() {
		try {
			return ForwardingKeyCipher.reveal(this.updateKey);
		} catch (IOException e) {
			reportUnusable(e);
			return null;
		}
	}

	/** Plain text in PIN mode, encrypted in accounts mode; see {@link ForwardingKeyCipher}. */
	@JsonProperty("updateKey")
	public String getUpdateKeyForJson() {
		try {
			return ForwardingKeyCipher.forStorage(this.updateKey);
		} catch (IOException e) {
			reportUnusable(e);
			return null;
		}
	}

	private void reportUnusable(IOException e) {
		// before the audit key is loaded at startup, a key encrypted with a PEM key cannot be read yet
		if (AuditIntegrity.isInitialized() && reportedUnusableKeys.add(this.updateKey)) {
			logger./**/warn("forwarding key for {} treated as unset, enter it again in the settings: {}", this.url,
			        e.getMessage());
		}
	}

	/** The value as stored: plain text or {@code enc:v1:...}. */
	String storedUpdateKey() {
		return this.updateKey;
	}

	/**
	 * Re-stores the key for a change of protection; see {@link ForwardingKeyCipher#reencrypt}. A key encrypted with
	 * an unknown audit key is left as is: it is unusable until entered again.
	 *
	 * @return true if the stored value changed
	 */
	boolean reencryptUpdateKey(AuditSigningKey oldKey, AuditSigningKey newKey) {
		try {
			String next = ForwardingKeyCipher.reencrypt(this.updateKey, oldKey, newKey);
			if (Objects.equals(next, this.updateKey)) {
				return false;
			}
			this.updateKey = next;
			return true;
		} catch (IOException e) {
			reportUnusable(e);
			return false;
		}
	}

	public String getUrl() {
		return url;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void setControlPanelManaged(boolean controlPanelManaged) {
		this.controlPanelManaged = controlPanelManaged;
	}

	public void setUpdateKey(String updateKey) {
		this.updateKey = normalizeKey(updateKey);
	}

	@JsonProperty("updateKey")
	public void setUpdateKeyFromJson(String updateKey) {
		this.updateKey = normalizeKey(updateKey);
	}

	public void setUrl(String url) {
		this.url = url;
	}

	private static String normalizeKey(String updateKey) {
		return updateKey == null || updateKey.isBlank() ? null : updateKey;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ForwardingConnection)) {
			return false;
		}
		ForwardingConnection other = (ForwardingConnection) obj;
		return active == other.active && controlPanelManaged == other.controlPanelManaged
		        && Objects.equals(updateKey, other.updateKey) && Objects.equals(url, other.url);
	}

	@Override
	public int hashCode() {
		return Objects.hash(active, controlPanelManaged, updateKey, url);
	}
}