package app.owlcms.nui.shared;

import com.vaadin.flow.dom.Element;

import app.owlcms.fieldofplay.FOPState;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;

public interface HasBoardMode {

	public enum BoardMode {
		WAIT,
		INTRO_COUNTDOWN,
		CEREMONY,
		LIFT_COUNTDOWN,
		CURRENT_ATHLETE,
		INTERRUPTION,
		SESSION_DONE
	}

	public default void setBoardMode(FOPState fopState, BreakType breakType, CeremonyType ceremonyType,
	        Element element) {
		element.setProperty("mode", computeBoardModeName(fopState, breakType, ceremonyType));
	}

	public default String computeBoardModeName(FOPState fopState, BreakType breakType, CeremonyType ceremonyType) {
		BoardMode bm = computeBoardMode(fopState, breakType, ceremonyType);
		return bm.name();
	}

	public default BoardMode computeBoardMode(FOPState fopState, BreakType breakType, CeremonyType ceremonyType) {
		BoardMode bm = BoardMode.WAIT;
		if (fopState == FOPState.BREAK && ceremonyType != null) {
			bm = BoardMode.CEREMONY;
		} else if (fopState == FOPState.BREAK && breakType == BreakType.BEFORE_INTRODUCTION) {
			bm = BoardMode.INTRO_COUNTDOWN;
		} else if (fopState == FOPState.BREAK
		        && (breakType == BreakType.FIRST_CJ || breakType == BreakType.FIRST_SNATCH)) {
			bm = BoardMode.LIFT_COUNTDOWN;
		} else if (fopState == FOPState.BREAK && (breakType == BreakType.GROUP_DONE)) {
			bm = BoardMode.SESSION_DONE;
		} else if (fopState == FOPState.BREAK &&
		        (breakType == BreakType.JURY
		                || breakType == BreakType.CHALLENGE
		                || breakType == BreakType.MARSHAL
		                || breakType == BreakType.TECHNICAL)) {
			bm = BoardMode.INTERRUPTION;
		} else if (fopState != FOPState.INACTIVE && fopState != FOPState.BREAK) {
			bm = BoardMode.CURRENT_ATHLETE;
		} else if (fopState == FOPState.INACTIVE) {
			bm = BoardMode.WAIT;
		}
		return bm;
	}

}
