/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.results;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.icon.Icon;

import app.owlcms.components.GroupSelectionMenu;
import app.owlcms.components.GroupSelectionMode;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;

@SuppressWarnings("serial")
public class SessionResultsGroupSelectionMenu extends GroupSelectionMenu {

	public SessionResultsGroupSelectionMenu(List<Group> groups, Group currentGroup, Collection<FieldOfPlay> fops,
	        Consumer<Group> whenChecked, Consumer<Group> whenUnselected, Icon unselectedIcon, String unselectedLabel) {
		super(sortedGroups(groups, fops), currentGroup, null, whenChecked, whenUnselected, unselectedIcon, unselectedLabel,
		        GroupSelectionMode.DONE_FIRST);
	}

	static List<Group> sortedGroups(List<Group> groups, Collection<FieldOfPlay> fops) {
		return SessionResultsGroupOrder.sortedGroups(groups, fops);
	}
}