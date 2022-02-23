/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import java.time.LocalDateTime;

import app.owlcms.uievents.BreakType;

public interface IBreakTimer extends IProxyTimer {

    Integer getBreakDuration();

    boolean isIndefinite();

    void setBreakDuration(Integer breakDuration);

    void setEnd(LocalDateTime targetTime);

    void setIndefinite();

    void setOrigin(Object origin);

    @Override
    void setTimeRemaining(int intValue);

    BreakType getBreakType();

}