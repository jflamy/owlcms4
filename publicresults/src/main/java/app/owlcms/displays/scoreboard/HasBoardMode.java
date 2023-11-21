package app.owlcms.displays.scoreboard;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.dom.Element;

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

	public default void setBoardMode(String fopState, BreakType breakType, CeremonyType ceremonyType, Element element) {
        BoardMode bm = BoardMode.WAIT;
        if (fopState == "BREAK" && breakType == BreakType.BEFORE_INTRODUCTION) {
            bm = BoardMode.INTRO_COUNTDOWN;
        } else if (fopState == "BREAK"
                && breakType == BreakType.FIRST_SNATCH) {
            if (ceremonyType != null) {
                bm = (ceremonyType != CeremonyType.INTRODUCTION) ? BoardMode.LIFT_COUNTDOWN_CEREMONY : BoardMode.CEREMONY;
            } else {
                bm = BoardMode.LIFT_COUNTDOWN;
            }
        } else if (fopState == "BREAK"
                && breakType == BreakType.FIRST_CJ) {
            bm = ceremonyType != null ? BoardMode.LIFT_COUNTDOWN_CEREMONY : BoardMode.LIFT_COUNTDOWN;
        } else if (fopState == "BREAK" && ceremonyType != null) {
            bm = BoardMode.CEREMONY;
        } else if (fopState == "BREAK" && (breakType == BreakType.GROUP_DONE)) {
            bm = BoardMode.SESSION_DONE;
        } else if (fopState == "BREAK" &&
                (breakType == BreakType.JURY
                        || breakType == BreakType.CHALLENGE
                        || breakType == BreakType.MARSHAL
                        || breakType == BreakType.TECHNICAL)) {
            bm = BoardMode.INTERRUPTION;
        } else if (fopState != "INACTIVE" && fopState != "BREAK") {
            bm = BoardMode.CURRENT_ATHLETE;
        } else if (fopState == "INACTIVE") {
            bm = BoardMode.WAIT;
        }
		element.setProperty("mode", bm.name());
    }

}
