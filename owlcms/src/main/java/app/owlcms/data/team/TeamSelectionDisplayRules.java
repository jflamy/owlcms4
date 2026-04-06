/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.team;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Gender;

public final class TeamSelectionDisplayRules {

	private TeamSelectionDisplayRules() {
	}

	public static boolean shouldShowMembershipColumn(Gender selectedGender) {
		return selectedGender != Gender.MF;
	}

	public static boolean shouldShowMembershipColumn(Championship championship, Gender selectedGender, TeamTreeItem item) {
		if (!shouldShowMembershipColumn(selectedGender)) {
			return false;
		}
		if (item == null) {
			return true;
		}
		return !shouldShowMixedMembershipColumn(championship, selectedGender, item);
	}

	public static boolean shouldShowMixedMembershipColumn(Championship championship, Gender selectedGender) {
		return championship != null
		        && (selectedGender == Gender.MF || (selectedGender == null && championship.isMixedTeamEnabled()));
	}

	public static boolean shouldShowMixedMembershipColumn(Championship championship, Gender selectedGender,
	        TeamTreeItem item) {
		if (!shouldShowMixedMembershipColumn(championship, selectedGender)) {
			return false;
		}
		if (selectedGender == Gender.MF) {
			return true;
		}
		return isMixedTeamContext(item);
	}

	public static boolean isMixedTeamContext(TeamTreeItem item) {
		if (item == null) {
			return false;
		}

		TeamTreeItem teamItem = item.getAthlete() == null ? item : item.getParent();
		return teamItem != null && teamItem.getAthlete() == null && teamItem.getGender() == Gender.MF;
	}
}