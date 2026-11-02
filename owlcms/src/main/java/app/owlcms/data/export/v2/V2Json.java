/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.category.Participation;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetable;
import ch.qos.logback.classic.Logger;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Converts entities to and from their V2 JSON form. Jackson writes every property that has both a getter and a
 * setter, as V1 does; the V2 differences are declared in {@link V2Mixins}.
 * <p>
 * References are written as names or codes. On import they are looked up with the import's entity manager, which
 * sees the platforms, sessions and categories the import has written but not yet committed.
 */
public final class V2Json {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(V2Json.class);
	private static final String ENTITY_MANAGER = "v2.entityManager";
	private static final String TEAM_NAMES = "v2.teamNames";

	static final JsonMapper MAPPER = JsonMapper.builder()
	        .enable(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS)
	        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
	        .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
	        .addMixIn(Group.class, V2Mixins.SessionMixin.class)
	        .addMixIn(Category.class, V2Mixins.CategoryMixin.class)
	        .addMixIn(Participation.class, V2Mixins.ParticipationMixin.class)
	        .addMixIn(Athlete.class, V2Mixins.AthleteMixin.class)
	        .addMixIn(Championship.class, V2Mixins.ChampionshipMixin.class)
	        .addMixIn(TechnicalOfficialsTimetable.class, V2Mixins.TimetableEntryMixin.class)
	        .build();

	private V2Json() {
	}

	/** @return the V2 form of a session, athlete, age group, championship or timetable entry */
	public static JsonNode toJson(Object entity) {
		return MAPPER.valueToTree(entity);
	}

	/**
	 * @param em the import's entity manager, or null to look up committed platforms
	 */
	public static Group toSession(JsonNode session, EntityManager em) {
		return reader(Group.class, em, null).readValue(session);
	}

	/** Categories are attached to their age group, and their stored code (used for lookups) is computed. */
	public static AgeGroup toAgeGroup(JsonNode ageGroup) {
		AgeGroup result = MAPPER.treeToValue(ageGroup, AgeGroup.class);
		for (Category category : result.getAllCategories()) {
			category.setAgeGroup(result);
			category.setCode(category.getComputedCode());
		}
		return result;
	}

	/**
	 * @param em        the import's entity manager, or null to look up committed sessions and categories
	 * @param teamNames the {@code teams} table of the export, by id; null when there is none
	 */
	public static Athlete toAthlete(JsonNode athlete, EntityManager em, Map<Integer, String> teamNames) {
		Athlete result = reader(Athlete.class, em, teamNames).readValue(athlete);
		// participations whose category is unknown were reported by the category lookup and are dropped
		List<Participation> participations = new ArrayList<>();
		for (Participation participation : result.getParticipations()) {
			if (participation.getCategory() != null) {
				participation.attachImported(result);
				participations.add(participation);
			}
		}
		result.setParticipations(participations);
		// year-only precision wins over the derived full date, whatever their order in the file
		JsonNode isoBirthDate = athlete.get("isoBirthDate");
		if (isoBirthDate != null && !isoBirthDate.isNull()) {
			result.setIsoBirthDate(isoBirthDate.asString());
		}
		return result;
	}

	public static Championship toChampionship(JsonNode championship) {
		return MAPPER.treeToValue(championship, Championship.class);
	}

	/** Copies every exported field of {@code from} onto {@code into}, which keeps its database id. */
	public static void copyChampionship(Championship from, Championship into) {
		MAPPER.readerForUpdating(into).readValue(toJson(from));
	}

	/**
	 * @param em the import's entity manager, or null to look up committed sessions
	 * @return the entry; its session is null when the session name is unknown
	 */
	public static TechnicalOfficialsTimetable toTimetableEntry(JsonNode entry, EntityManager em) {
		return reader(TechnicalOfficialsTimetable.class, em, null).readValue(entry);
	}

	private static ObjectReader reader(Class<?> type, EntityManager em, Map<Integer, String> teamNames) {
		return MAPPER.readerFor(type).withAttribute(ENTITY_MANAGER, em).withAttribute(TEAM_NAMES, teamNames);
	}

	private static EntityManager entityManager(DeserializationContext context) {
		return (EntityManager) context.getAttribute(ENTITY_MANAGER);
	}

	/** @return the weight as a number, null for no weight; V2 writes attempt weights this way */
	public static Integer parseWeight(String weight) {
		if (weight == null || weight.isBlank()) {
			return null;
		}
		try {
			return Integer.parseInt(weight.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	static final class WeightSerializer extends ValueSerializer<String> {
		@Override
		public void serialize(String weight, JsonGenerator gen, SerializationContext context) {
			Integer value = parseWeight(weight);
			if (value == null) {
				gen.writeNull();
			} else {
				gen.writeNumber(value);
			}
		}
	}

	static final class WeightDeserializer extends ValueDeserializer<String> {
		@Override
		public String deserialize(JsonParser parser, DeserializationContext context) {
			if (parser.currentToken().isNumeric()) {
				return Integer.toString(parser.getValueAsInt());
			}
			String text = parser.getValueAsString();
			return text == null ? "" : text.trim();
		}

		@Override
		public String getNullValue(DeserializationContext context) {
			return "";
		}
	}

	/** Team ids are the hash of the team name, see {@link TeamDTO}. */
	static final class TeamIdSerializer extends ValueSerializer<String> {
		@Override
		public void serialize(String team, JsonGenerator gen, SerializationContext context) {
			Integer id = TeamDTO.idOf(team);
			if (id == null) {
				gen.writeNull();
			} else {
				gen.writeNumber(id);
			}
		}
	}

	static final class TeamByIdDeserializer extends ValueDeserializer<String> {
		@Override
		public String deserialize(JsonParser parser, DeserializationContext context) {
			if (parser.currentToken() == JsonToken.VALUE_STRING) {
				return parser.getString();
			}
			int id = parser.getValueAsInt();
			@SuppressWarnings("unchecked")
			Map<Integer, String> names = (Map<Integer, String>) context.getAttribute(TEAM_NAMES);
			if (names == null) {
				return Integer.toString(id);
			}
			String name = names.get(id);
			if (name == null) {
				logger./**/warn("V2 import: team id {} is not in the teams table", id);
				return "";
			}
			return name;
		}

		@Override
		public String getNullValue(DeserializationContext context) {
			return "";
		}
	}

	static final class SessionNameSerializer extends ValueSerializer<Group> {
		@Override
		public void serialize(Group session, JsonGenerator gen, SerializationContext context) {
			gen.writeString(session.getName());
		}
	}

	static final class SessionByNameDeserializer extends ValueDeserializer<Group> {
		@Override
		public Group deserialize(JsonParser parser, DeserializationContext context) {
			String name = parser.getValueAsString();
			if (name == null || name.isBlank()) {
				return null;
			}
			EntityManager em = entityManager(context);
			Group session = em != null ? GroupRepository.doFindByName(name, em) : GroupRepository.findByName(name);
			if (session == null) {
				logger./**/warn("V2 import: unknown session {}", name);
			}
			return session;
		}
	}

	static final class CategoryCodeSerializer extends ValueSerializer<Category> {
		@Override
		public void serialize(Category category, JsonGenerator gen, SerializationContext context) {
			gen.writeString(category.getCode());
		}
	}

	static final class CategoryByCodeDeserializer extends ValueDeserializer<Category> {
		@Override
		public Category deserialize(JsonParser parser, DeserializationContext context) {
			String code = parser.getValueAsString();
			if (code == null || code.isBlank()) {
				return null;
			}
			EntityManager em = entityManager(context);
			Category category = em != null ? CategoryRepository.doFindByCode(code, em)
			        : CategoryRepository.findByCode(code);
			if (category == null) {
				logger./**/warn("V2 import: unknown category {}", code);
			}
			return category;
		}
	}

	static final class PlatformNameSerializer extends ValueSerializer<Platform> {
		@Override
		public void serialize(Platform platform, JsonGenerator gen, SerializationContext context) {
			gen.writeString(platform.getName());
		}
	}

	static final class PlatformByNameDeserializer extends ValueDeserializer<Platform> {
		@Override
		public Platform deserialize(JsonParser parser, DeserializationContext context) {
			String name = parser.getValueAsString();
			if (name == null || name.isBlank()) {
				return null;
			}
			EntityManager em = entityManager(context);
			Platform platform = em != null ? PlatformRepository.doFindByName(name, em)
			        : PlatformRepository.findByName(name);
			if (platform == null) {
				logger./**/warn("V2 import: unknown platform {}; the session has no platform", name);
			}
			return platform;
		}
	}
}
