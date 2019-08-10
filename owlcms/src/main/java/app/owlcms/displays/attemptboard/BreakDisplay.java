package app.owlcms.displays.attemptboard;

import java.text.MessageFormat;

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
		return MessageFormat.format(Translator.translate("Group_number"), groupName);
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