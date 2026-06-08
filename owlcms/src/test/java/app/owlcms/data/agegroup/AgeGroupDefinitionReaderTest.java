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
        assertEquals(Ranking.QAGE, AgeGroupDefinitionReader.getRankingFromExportValue(" qmasters "));
        assertEquals(Ranking.SMM, AgeGroupDefinitionReader.getRankingFromExportValue("SMHF"));
    }
}
