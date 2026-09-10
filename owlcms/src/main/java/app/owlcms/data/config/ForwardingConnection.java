package app.owlcms.data.config;

import java.util.Objects;

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
		this.updateKey = updateKey;
		this.active = active;
		this.controlPanelManaged = controlPanelManaged;
	}

	public ForwardingConnection(ForwardingConnection connection) {
		this(connection.getUrl(), connection.getUpdateKey(), connection.isActive(), connection.isControlPanelManaged());
	}

	public boolean isActive() {
		return active;
	}

	public boolean isControlPanelManaged() {
		return controlPanelManaged;
	}

	public String getUpdateKey() {
		return updateKey;
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
		this.updateKey = updateKey;
	}

	public void setUrl(String url) {
		this.url = url;
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