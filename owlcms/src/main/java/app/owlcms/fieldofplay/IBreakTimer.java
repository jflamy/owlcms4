package app.owlcms.fieldofplay;

import java.time.LocalDateTime;

public interface IBreakTimer extends IProxyTimer {

    void setEnd(LocalDateTime targetTime);

    void setIndefinite();

    Integer getBreakDuration();

    void setBreakDuration(Integer breakDuration);

    void setTimeRemaining(int intValue);

    boolean isIndefinite();

    void setOrigin(Object origin);

}