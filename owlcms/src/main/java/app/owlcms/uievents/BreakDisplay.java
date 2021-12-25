/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.uievents;

import org.slf4j.LoggerFactory;

import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;
import app.owlcms.i18n.Translator;

public interface BreakDisplay {

    Logger logger = (Logger) LoggerFactory.getLogger(BreakDisplay.class);

    public void doBreak();

    public default String inferGroupName() {
        FieldOfPlay fop = OwlcmsSession.getFop();
        if (fop == null) {
            return "";
        }
        Group group = fop.getGroup();
        String groupName = group != null ? group.getName() : "";
        return Translator.translate("Group_number", groupName);
    }

    public default String inferMessage(BreakType bt) {
        if (bt == null) {
            return Translator.translate("PublicMsg.CompetitionPaused");
        }
        switch (bt) {
        case FIRST_CJ:
            return Translator.translate("BreakType.FIRST_CJ");
        case FIRST_SNATCH:
            return Translator.translate("BreakType.FIRST_SNATCH");
        case BEFORE_INTRODUCTION:
            return Translator.translate("BreakType.BEFORE_INTRODUCTION");
        case DURING_INTRODUCTION:
            return Translator.translate("PublicMsg.DuringIntroduction");
        case TECHNICAL:
            return Translator.translate("PublicMsg.CompetitionPaused");
        case JURY:
            return Translator.translate("PublicMsg.JuryDeliberation");
        case GROUP_DONE:
            return Translator.translate("PublicMsg.GroupDone");
        case MEDALS:
            return Translator.translate("PublicMsg.Medals");
        }
        // can't happen
        return "";
    }

}