package app.owlcms.nui.shared;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.dom.Element;

import app.owlcms.fieldofplay.FOPState;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import ch.qos.logback.classic.Logger;

public interface HasBoardMode {

	Logger logger = (Logger) LoggerFactory.getLogger(HasBoardMode.class);

	public enum BoardMode {
		WAIT,
		INTRO_COUNTDOWN,
		CEREMONY,
		LIFT_COUNTDOWN,
		CURRENT_ATHLETE,
		INTERRUPTION,
		SESSION_DONE,
		LIFT_COUNTDOWN_CEREMONY
	}

	public default void setBoardMode(FOPState fopState, BreakType breakType, CeremonyType ceremonyType,
	        Element element) {
		element.setProperty("mode", computeBoardModeName(fopState, breakType, ceremonyType));
		element.setProperty("breakType", fopState == FOPState.BREAK ? breakType.name() : null);
	}

	public default String computeBoardModeName(FOPState fopState, BreakType breakType, CeremonyType ceremonyType) {
		BoardMode bm = computeBoardMode(fopState, breakType, ceremonyType);
		return bm.name();
	}

	public default BoardMode computeBoardMode(FOPState fopState, BreakType breakType, CeremonyType ceremonyType) {
		BoardMode bm = BoardMode.WAIT;
		if (fopState == FOPState.BREAK && breakType == BreakType.BEFORE_INTRODUCTION) {
			bm = BoardMode.INTRO_COUNTDOWN;
		} else if (fopState == FOPState.BREAK
		        && breakType == BreakType.FIRST_SNATCH) {
			if (ceremonyType != null) {
				bm = (ceremonyType != CeremonyType.INTRODUCTION) ? BoardMode.LIFT_COUNTDOWN_CEREMONY : BoardMode.CEREMONY;
			} else {
				bm = BoardMode.LIFT_COUNTDOWN;
			}
		} else if (fopState == FOPState.BREAK
		        && breakType == BreakType.FIRST_CJ) {
			bm = ceremonyType != null ? BoardMode.LIFT_COUNTDOWN_CEREMONY : BoardMode.LIFT_COUNTDOWN;
		} else if (fopState == FOPState.BREAK && ceremonyType != null) {
			bm = BoardMode.CEREMONY;
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
		//logger.debug("computeBoardMode {} {} {} = {}", fopState, breakType, ceremonyType, bm);
		return bm;
	}

}
