package org.ledocte.owlcms.state;

import org.ledocte.owlcms.data.group.Group;
import org.ledocte.owlcms.data.platform.Platform;

import com.google.common.eventbus.EventBus;

/**
 * This class describes one field of play at runtime It encapsulates the
 * in-memory data structures used to compute the state of the competition and
 * links them to the database descriptions of the group and platform.
 * 
 * @author owlcms
 */
public class FieldOfPlayState {

	public EventBus eventBus = new EventBus();

	private Group group = null;

	private Platform platform = null;

	private String name;

	private LiftingOrderState groupState;

	public FieldOfPlayState(Group group, Platform platform) {
		super();
		this.group = group;
		this.platform = platform;
		this.name = platform.getName();
		this.setLiftingOrderState(new LiftingOrderState(this));
	}

	public FieldOfPlayState() {
		this.setLiftingOrderState(new LiftingOrderState(this));
	}

	/**
	 * @return the eventBus
	 */
	public EventBus getEventBus() {
		return eventBus;
	}

	/**
	 * @return the group
	 */
	public Group getGroup() {
		return group;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the platform
	 */
	public Platform getPlatform() {
		return platform;
	}

	/**
	 * @param eventBus the eventBus to set
	 */
	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	/**
	 * @param group the group to set
	 */
	public void setGroup(Group group) {
		this.group = group;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param platform the platform to set
	 */
	public void setPlatform(Platform platform) {
		this.platform = platform;
	}

	public LiftingOrderState getGroupState() {
		return groupState;
	}

	public void setLiftingOrderState(LiftingOrderState groupState) {
		this.groupState = groupState;
	}

}
