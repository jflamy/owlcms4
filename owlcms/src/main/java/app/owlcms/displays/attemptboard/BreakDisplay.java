/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.displays.attemptboard;

import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.BreakType;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.UIEvent.BreakStarted;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;

public interface BreakDisplay {
	
	public void doBreak(BreakStarted e);
	
	public default BreakType inferBreakType(FieldOfPlay fop) {
		BreakType bt;
		switch (fop.getState()) {
		case BREAK:
			bt = fop.countLiftsDone() > 0 ? BreakType.FIRST_SNATCH : BreakType.FIRST_CJ;
			break;
		case INACTIVE:
			bt = BreakType.INTRODUCTION;
			break;
		default:
			bt = BreakType.TECHNICAL;
			break;
		}
		return bt;
	}

	public default String inferGroupName() {
		FieldOfPlay fop = OwlcmsSession.getFop();
		Group group = fop.getGroup();
		String groupName = group != null ? group.getName() : "";
		return Translator.translate("Group_number", groupName);
	}

	public default String inferMessage(BreakType bt) {
		switch (bt) {
		case FIRST_CJ:
			return Translator.translate("TimeBeforeNextLift");
		case FIRST_SNATCH:
			return Translator.translate("TimeBeforeFirstLift");
		case INTRODUCTION:
			return Translator.translate("TimeBeforeIntroduction");
		case TECHNICAL:
			return Translator.translate("CompetitionPaused");
		default:
			return "";
		}
	}

}