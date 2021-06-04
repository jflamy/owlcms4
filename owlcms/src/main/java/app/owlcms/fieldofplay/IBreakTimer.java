package app.owlcms.fieldofplay;

import java.time.LocalDateTime;

public interface IBreakTimer extends IProxyTimer {

    Integer getBreakDuration();

    boolean isIndefinite();

    void setBreakDuration(Integer breakDuration);

    void setEnd(LocalDateTime targetTime);

    void setIndefinite();

    void setOrigin(Object origin);

    @Override
    void setTimeRemaining(int intValue);

}