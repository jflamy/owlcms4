package app.owlcms.fieldofplay;

import app.owlcms.ui.shared.BreakManagement.CountdownType;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.JuryNotification;

public interface JuryEvents {
    
    public default void postJurySummonNotification(FieldOfPlay fop, Object origin) {
        if (fop.getState() != FOPState.BREAK) {
            fop.fopEventPost(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, this));
        }
        JuryNotification event = new UIEvent.JuryNotification(null, origin, JuryDeliberationEventType.CALL_REFEREES,
                null, null);
        fop.getUiEventBus().post(event);
    }

}
