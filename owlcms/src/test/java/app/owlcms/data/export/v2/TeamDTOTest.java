/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class TeamDTOTest {

	@Test
	public void namesWithTheSameHashAreReported() {
		assertEquals("Aa".hashCode(), "BB".hashCode());
		Map<Integer, List<String>> collisions = TeamDTO.idCollisions(List.of("BB", "CAN", "Aa", "USA"));
		assertEquals(Map.of("Aa".hashCode(), List.of("Aa", "BB")), collisions);
	}

	@Test
	public void distinctIdsAreNotReported() {
		assertTrue(TeamDTO.idCollisions(List.of("CAN", "USA", "FRA", "Club de Montréal")).isEmpty());
	}
}
