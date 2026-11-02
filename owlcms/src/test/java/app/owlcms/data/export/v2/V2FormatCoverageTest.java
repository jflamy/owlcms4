/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export.v2;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.export.StoredChampionshipMixin;
import app.owlcms.data.group.Group;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetable;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.introspect.ClassIntrospector;
import tools.jackson.databind.json.JsonMapper;

/**
 * V2 is the entity as Jackson sees it, with the differences in {@link V2Mixins}. Every property that V1 writes and
 * restores must still be in V2, under its own name or under the name listed here. A mixin that hides a property, or
 * renames one without saying so here, fails this test.
 */
public class V2FormatCoverageTest {

	private static final String DROPPED = null;

	/** As the V1 export ({@code CompetitionData}) maps the entities. */
	private static final JsonMapper V1_MAPPER = JsonMapper.builder()
	        .addMixIn(Championship.class, StoredChampionshipMixin.class)
	        .build();

	private record Mapping(Class<?> entity, Map<String, String> renamed) {
	}

	/** V1 property name to V2 name; {@link #DROPPED} for database ids and references to the parent. */
	private static final List<Mapping> MAPPINGS = List.of(
	        new Mapping(Athlete.class, renames("id", DROPPED, "group", "sessionName", "category", "categoryCode")),
	        new Mapping(Group.class, renames("id", DROPPED, "platform", "platformName")),
	        new Mapping(AgeGroup.class, renames()),
	        new Mapping(Category.class, renames("id", DROPPED, "ageGroup", DROPPED)),
	        new Mapping(Participation.class, renames("athlete", DROPPED, "category", "categoryCode")),
	        new Mapping(Championship.class, renames("id", DROPPED)),
	        new Mapping(TechnicalOfficialsTimetable.class, renames("id", DROPPED, "group", "sessionName")));

	@Test
	public void everyPropertyRoundTrippedByV1IsInV2() {
		List<String> problems = new ArrayList<>();
		for (Mapping mapping : MAPPINGS) {
			String entity = mapping.entity().getSimpleName();
			Set<String> v1 = roundTripProperties(V1_MAPPER, mapping.entity());
			Set<String> v2 = roundTripProperties(V2Json.MAPPER, mapping.entity());
			for (String property : v1) {
				if (mapping.renamed().containsKey(property)) {
					String v2Name = mapping.renamed().get(property);
					if (v2Name != null && !v2.contains(v2Name)) {
						problems.add(entity + "." + property + " should be in V2 as " + v2Name);
					}
				} else if (!v2.contains(property)) {
					problems.add(entity + "." + property + " is round-tripped by V1 but missing from V2");
				}
			}
			for (String renamed : mapping.renamed().keySet()) {
				if (!v1.contains(renamed)) {
					problems.add(entity + "." + renamed + " is listed here but V1 no longer has it: remove it");
				}
			}
		}
		assertTrue(String.join("\n", problems), problems.isEmpty());
	}

	/** The properties the mapper both writes and restores. */
	private static Set<String> roundTripProperties(JsonMapper mapper, Class<?> type) {
		SerializationConfig config = mapper.serializationConfig();
		JavaType javaType = config.constructType(type);
		ClassIntrospector introspector = config.classIntrospectorInstance().forOperation(config);
		AnnotatedClass annotated = introspector.introspectClassAnnotations(javaType);
		BeanDescription description = introspector.introspectForSerialization(javaType, annotated);
		Set<String> ignored = config.getDefaultPropertyIgnorals(type, annotated).findIgnoredForSerialization();
		Set<String> result = new TreeSet<>();
		for (BeanPropertyDefinition property : description.findProperties()) {
			if (!ignored.contains(property.getName()) && property.couldSerialize() && property.couldDeserialize()) {
				result.add(property.getName());
			}
		}
		return result;
	}

	private static Map<String, String> renames(String... pairs) {
		Map<String, String> result = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			result.put(pairs[i], pairs[i + 1]);
		}
		return result;
	}
}
