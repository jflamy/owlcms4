/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.uievents;

import org.slf4j.LoggerFactory;

import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

public interface BreakDisplay {

    Logger logger = (Logger) LoggerFactory.getLogger(BreakDisplay.class);

    public void doBreak(UIEvent e);

    public void doCeremony(UIEvent.CeremonyStarted e);

    public default String inferGroupName() {
        return inferGroupName(null);
    }

    public default String inferGroupName(CeremonyType ceremonyType) {
        FieldOfPlay fop = OwlcmsSession.getFop();
        if (fop == null || ceremonyType == CeremonyType.MEDALS) {
            return "";
        }
        Group group = fop.getGroup();
        String groupName = group != null ? group.getName() : "";
        if (!groupName.isBlank()) {
            return Translator.translate("Group_number", groupName);
        } else {
            return "";
        }
    }

    public default String inferMessage(BreakType breakType, CeremonyType ceremonyType, boolean publicDisplay) {
        if (breakType == null && ceremonyType == null) {
            return Translator.translate("PublicMsg.CompetitionPaused");
        }
        if (ceremonyType != null && publicDisplay) {
            switch (ceremonyType) {
            case INTRODUCTION:
                return Translator.translate("BreakMgmt.IntroductionOfAthletes");
            case MEDALS:
                return Translator.translate("PublicMsg.Medals");
            case OFFICIALS_INTRODUCTION:
                return Translator.translate("BreakMgmt.IntroductionOfOfficials");
            }
        }
        if (breakType == null) {
            return "";
        }
        switch (breakType) {
        case FIRST_CJ:
            return Translator.translate("BreakType.FIRST_CJ");
        case FIRST_SNATCH:
            return Translator.translate("BreakType.FIRST_SNATCH");
        case BEFORE_INTRODUCTION:
            return Translator.translate("BreakType.BEFORE_INTRODUCTION");
        case TECHNICAL:
            return Translator.translate("PublicMsg.CompetitionPaused");
        case JURY:
            return Translator.translate("PublicMsg.JuryDeliberation");
        case GROUP_DONE:
            return Translator.translate("PublicMsg.GroupDone");
        case MARSHAL:
            return Translator.translate("PublicMsg.CompetitionPaused");
        default:
            break;
        }
        // can't happen
        return "";
    }

}