/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Jackson mixin used by exports to serialize {@code Championship} via its
 * stored fields rather than its smart getters.
 *
 * <p>The smart getters on {@code Championship} (e.g. {@code getScoringSystem},
 * {@code getMaxTeamSize}, {@code getTeamPoints1st}…) resolve to the competition
 * template when {@code useCompetitionDefaults=true}. Exports must be "dumb"
 * and reflect the database, so this mixin disables getter discovery and
 * enables field discovery for any visibility.
 *
 * <p>Apply with {@code mapper.addMixIn(Championship.class, StoredChampionshipMixin.class)}.
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class StoredChampionshipMixin {

	// Suppress the method-level @JsonProperty("useCompetitionDefaults") on
	// Championship.usesCompetitionDefaults() so it does not collide with the
	// field of the same name now exposed by field discovery.
	@JsonIgnore
	abstract boolean usesCompetitionDefaults();
}
