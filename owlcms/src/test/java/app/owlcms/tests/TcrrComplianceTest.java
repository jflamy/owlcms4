/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumMap;
import java.util.Map;

import org.junit.Test;

import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.config.TcrrCompliance;

public class TcrrComplianceTest {

	@Test
	public void enabledModeVetoesAlternativeCompetitionRules() {
		assertEquals(4, TcrrCompliance.jurySize(true, 3));
		assertTrue(TcrrCompliance.enforce20kgRule(true, false));
		assertFalse(TcrrCompliance.imwaRules(true, true));
		assertFalse(TcrrCompliance.alternativeLiftingOrder(true, true));
		assertEquals(Ranking.GAMX, TcrrCompliance.bestAthleteScoring(true, Ranking.BW_SINCLAIR));
		assertEquals(28, TcrrCompliance.teamPointsFirst(true, 99));
		assertEquals(25, TcrrCompliance.teamPointsSecond(true, 98));
		assertEquals(23, TcrrCompliance.teamPointsThird(true, 97));
	}

	@Test
	public void disabledModePreservesConfiguredRules() {
		assertEquals(3, TcrrCompliance.jurySize(false, 3));
		assertEquals(0, TcrrCompliance.jurySize(false, null));
		assertFalse(TcrrCompliance.enforce20kgRule(false, false));
		assertTrue(TcrrCompliance.imwaRules(false, true));
		assertTrue(TcrrCompliance.alternativeLiftingOrder(false, true));
		assertEquals(Ranking.BW_SINCLAIR,
		        TcrrCompliance.bestAthleteScoring(false, Ranking.BW_SINCLAIR));
		assertEquals(99, TcrrCompliance.teamPointsFirst(false, 99));
		assertEquals(98, TcrrCompliance.teamPointsSecond(false, 98));
		assertEquals(97, TcrrCompliance.teamPointsThird(false, 97));
	}

	@Test
	public void enumConstantCanVetoItsConfiguredValue() {
		Map<FeatureSwitch, Boolean> values = new EnumMap<>(FeatureSwitch.class);
		Config config = new Config() {
			@Override
			public boolean featureSwitch(FeatureSwitch featureSwitch) {
				return values.getOrDefault(featureSwitch, false);
			}
		};
		values.put(FeatureSwitch.GENDER_INCLUSIVE, true);

		assertTrue(FeatureSwitch.GENDER_INCLUSIVE.isEffective(config));
		values.put(FeatureSwitch.TCRR_COMPLIANCE, true);
		assertFalse(FeatureSwitch.GENDER_INCLUSIVE.isEffective(config));
		assertTrue(config.featureSwitch(FeatureSwitch.GENDER_INCLUSIVE));
	}
}
