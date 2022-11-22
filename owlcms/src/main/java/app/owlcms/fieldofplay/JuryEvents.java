package app.owlcms.fieldofplay;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.JuryNotification;

public interface JuryEvents {

//    public default void postJurySummonNotification(FieldOfPlay fop, Object origin, int refIndex) {
//        //Logger logger = (Logger) LoggerFactory.getLogger("JuryEvents");
//        //logger.debug("fop.getState() = {}", fop.getState());
//        if (fop.getState() != FOPState.BREAK) {
//            fop.fopEventPost(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, this));
//            // do not notify multiple times if calling all referees.
//                JuryNotification event = new UIEvent.JuryNotification(null, origin, JuryDeliberationEventType.CALL_REFEREES,
//                        null, null);
//                fop.getUiEventBus().post(event);
//        }
//    }

//    public default void postJuryTechnicalPause(FieldOfPlay fop, Object origin) {
//        // technical pause from Jury
//        fop.fopEventPost(
//                new FOPEvent.BreakStarted(BreakType.TECHNICAL, CountdownType.INDEFINITE, 0, null, true, this));
//        JuryNotification event = new UIEvent.JuryNotification(null, origin,
//                JuryDeliberationEventType.TECHNICAL_PAUSE, null, null);
//        fop.getUiEventBus().post(event);
//    }
//    
//    public default void postJuryDeliberation(FieldOfPlay fop, Object origin, Athlete athleteUnderReview) {
//        // stop competition
//        fop.fopEventPost(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, this));
//        JuryNotification event = new UIEvent.JuryNotification(athleteUnderReview, origin,
//                JuryDeliberationEventType.START_DELIBERATION, null, null);
//        fop.getUiEventBus().post(event);
//    }
    
//    public default void postJuryResumeCompetition(FieldOfPlay fop, Object origin, Athlete athleteUnderReview) {
//        JuryDeliberationEventType endEvent = null;
//        BreakType deliberation = fop.getBreakType();
//        if (deliberation == null) {
//            endEvent = JuryDeliberationEventType.END_JURY_BREAK;
//        } else {
//            // TODO : using the break type.  not clear that the jury types are useful.
//            switch (deliberation) {
//            case TECHNICAL:
//                endEvent = JuryDeliberationEventType.END_TECHNICAL_PAUSE;
//                break;
//            default:
//                endEvent = JuryDeliberationEventType.END_JURY_BREAK;
//                break;
//            }
//        }
//        JuryNotification event = new UIEvent.JuryNotification(athleteUnderReview, origin, endEvent, null, null);
//        OwlcmsSession.getFop().getUiEventBus().post(event);
//        fop.fopEventPost(new FOPEvent.StartLifting(this));
//    }
    
//    public default void postJuryCallController(FieldOfPlay fop, Object origin) {
//        JuryNotification event = new UIEvent.JuryNotification(null, origin,
//                JuryDeliberationEventType.CALL_TECHNICAL_CONTROLLER, null, null);
//        OwlcmsSession.getFop().getUiEventBus().post(event);
//    }

}
