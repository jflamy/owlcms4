package app.owlcms.data.config;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import app.owlcms.utils.InstallationSecret;

public class ForwardingConnection {

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

	@JsonIgnore
	public String getUpdateKey() {
		try {
			return InstallationSecret.decrypt(this.updateKey);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to decrypt forwarding update key", e);
		}
	}

	@JsonProperty("updateKey")
	public String getUpdateKeyForJson() {
		try {
			return InstallationSecret.encrypt(this.updateKey);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to encrypt forwarding update key", e);
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
		try {
			this.updateKey = InstallationSecret.encrypt(normalizeKey(updateKey));
		} catch (IOException e) {
			throw new IllegalStateException("Unable to encrypt forwarding update key", e);
		}
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