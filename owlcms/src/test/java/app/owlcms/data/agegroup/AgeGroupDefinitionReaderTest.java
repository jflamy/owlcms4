/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import app.owlcms.data.athleteSort.Ranking;

public class AgeGroupDefinitionReaderTest {

    @Test
    public void rankingParserAcceptsEnumNamesAndReportingNames() {
        assertEquals(Ranking.QAGE, AgeGroupDefinitionReader.getRankingFromExportValue("QAGE"));
        assertEquals(Ranking.QAGE, AgeGroupDefinitionReader.getRankingFromExportValue("QMasters"));
        assertEquals(Ranking.SMM, AgeGroupDefinitionReader.getRankingFromExportValue("SMHF"));
    }

    @Test
    public void missingTeamPointsPolicyUsesMedalConfiguration() {
        TeamPointsPolicy missing = AgeGroupDefinitionReader.getTeamPointsPolicyFromExportValue("");

        assertEquals(TeamPointsPolicy.ALL_THREE, TeamPointsPolicy.effective(missing, true));
        assertEquals(TeamPointsPolicy.TOTAL_ONLY, TeamPointsPolicy.effective(missing, false));
    }

    @Test
    public void teamPointsPolicyParserAcceptsExportedEnumNames() {
        assertEquals(TeamPointsPolicy.ALL_THREE,
                AgeGroupDefinitionReader.getTeamPointsPolicyFromExportValue("ALL_THREE"));
        assertEquals(TeamPointsPolicy.TOTAL_ONLY,
            AgeGroupDefinitionReader.getTeamPointsPolicyFromExportValue("TOTAL_ONLY"));
        assertEquals(TeamPointsPolicy.LIFTS_ONLY,
            AgeGroupDefinitionReader.getTeamPointsPolicyFromExportValue("LIFTS_ONLY"));
    }
}
