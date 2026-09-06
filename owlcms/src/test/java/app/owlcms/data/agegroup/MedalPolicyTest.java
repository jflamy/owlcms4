package app.owlcms.data.agegroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class MedalPolicyTest {

	@Test
	public void legacyMedalsDefaultFromBoolean() {
		assertEquals(MedalPolicy.ALL_THREE, MedalPolicy.effective(null, true));
		assertEquals(MedalPolicy.TOTAL_ONLY, MedalPolicy.effective(null, false));
		assertEquals(MedalPolicy.LIFTS_ONLY, MedalPolicy.effective(MedalPolicy.LIFTS_ONLY, false));
	}

	@Test
	public void teamPointsAreRestrictedToMedalledEvents() {
		for (TeamPointsPolicy points : TeamPointsPolicy.values()) {
			assertEquals(points, TeamPointsPolicy.effective(points, MedalPolicy.ALL_THREE));
			assertEquals(TeamPointsPolicy.TOTAL_ONLY, TeamPointsPolicy.effective(points, MedalPolicy.TOTAL_ONLY));
		}
		assertEquals(TeamPointsPolicy.ALL_THREE,
		        TeamPointsPolicy.effective(TeamPointsPolicy.ALL_THREE, MedalPolicy.LIFTS_ONLY));
		assertEquals(TeamPointsPolicy.LIFTS_ONLY,
		        TeamPointsPolicy.effective(TeamPointsPolicy.LIFTS_ONLY, MedalPolicy.LIFTS_ONLY));
		assertEquals(TeamPointsPolicy.TOTAL_ONLY,
		        TeamPointsPolicy.effective(TeamPointsPolicy.TOTAL_ONLY, MedalPolicy.LIFTS_ONLY));
		assertEquals(TeamPointsPolicy.ALL_THREE, TeamPointsPolicy.effective(null, MedalPolicy.ALL_THREE));
		assertEquals(TeamPointsPolicy.TOTAL_ONLY, TeamPointsPolicy.effective(null, MedalPolicy.TOTAL_ONLY));
		assertEquals(TeamPointsPolicy.LIFTS_ONLY, TeamPointsPolicy.effective(null, MedalPolicy.LIFTS_ONLY));
	}

	@Test
	public void liftMedalsAllowAnyTeamPointsPolicy() {
		assertEquals(List.of(TeamPointsPolicy.values()),
		        TeamPointsPolicy.allowedFor(MedalPolicy.LIFTS_ONLY));
		assertEquals(List.of(TeamPointsPolicy.TOTAL_ONLY),
		        TeamPointsPolicy.allowedFor(MedalPolicy.TOTAL_ONLY));
		assertEquals(List.of(TeamPointsPolicy.values()),
		        TeamPointsPolicy.allowedFor(MedalPolicy.ALL_THREE));
	}

	@Test
	public void onlyRanksForAwardedMedalsQualify() {
		assertFalse(MedalPolicy.LIFTS_ONLY.isMedalist(4, 4, 1));
		assertTrue(MedalPolicy.ALL_THREE.isMedalist(4, 4, 1));
		assertTrue(MedalPolicy.LIFTS_ONLY.isMedalist(1, 4, 0));
		assertTrue(MedalPolicy.LIFTS_ONLY.isMedalist(0, 3, 0));
		assertFalse(MedalPolicy.TOTAL_ONLY.isMedalist(1, 1, 4));
		assertFalse(MedalPolicy.ALL_THREE.isMedalist(0, -1, 0));
	}

	@Test
	public void liftsOnlyExcludesTotalMedals() {
		assertTrue(MedalPolicy.LIFTS_ONLY.includesSnatchAndCleanJerk());
		assertFalse(MedalPolicy.LIFTS_ONLY.includesTotal());
		assertFalse(MedalPolicy.TOTAL_ONLY.includesSnatchAndCleanJerk());
		assertTrue(MedalPolicy.TOTAL_ONLY.includesTotal());
		assertTrue(MedalPolicy.ALL_THREE.includesSnatchAndCleanJerk());
		assertTrue(MedalPolicy.ALL_THREE.includesTotal());
	}
}