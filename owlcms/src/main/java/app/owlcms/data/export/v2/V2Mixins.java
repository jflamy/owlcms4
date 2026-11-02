/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export.v2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.ParticipationId;
import app.owlcms.data.group.Group;
import app.owlcms.data.platform.Platform;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * The differences between V1 (Jackson on the entities) and V2. Everything else is the entity as Jackson sees it: a
 * property with both a getter and a setter is exported and restored, so a new one needs no code here.
 * <ul>
 * <li>database ids are not exported; references are written as names or codes</li>
 * <li>nested objects do not refer back to their parent</li>
 * <li>a few computed values are exported because external readers of the V2 format use them</li>
 * <li>attempt weights are numbers, teams are numeric ids into the {@code teams} table (external readers)</li>
 * </ul>
 */
final class V2Mixins {

	private V2Mixins() {
	}

	/** Attempt weights are text in the entity and numbers in V2. */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@JacksonAnnotationsInside
	@JsonSerialize(using = V2Json.WeightSerializer.class)
	@JsonDeserialize(using = V2Json.WeightDeserializer.class)
	@interface Weight {
	}

	@JsonIdentityInfo(generator = ObjectIdGenerators.None.class)
	abstract static class SessionMixin {
		@JsonIgnore
		abstract Long getId();

		@JsonProperty("platformName")
		@JsonSerialize(using = V2Json.PlatformNameSerializer.class)
		abstract Platform getPlatform();

		@JsonProperty("platformName")
		@JsonDeserialize(using = V2Json.PlatformByNameDeserializer.class)
		abstract void setPlatform(Platform platform);
	}

	@JsonIdentityInfo(generator = ObjectIdGenerators.None.class)
	@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
	abstract static class CategoryMixin {
		@JsonIgnore
		abstract Long getId();

		/** Categories are nested under their age group. */
		@JsonIgnore
		abstract AgeGroup getAgeGroup();

		/** The entity hides its stored code from V1; V2 refers to categories by code. */
		@JsonProperty
		abstract String getCode();

		@JsonProperty
		abstract void setCode(String code);

		@JsonProperty(access = JsonProperty.Access.READ_ONLY)
		abstract String getCategoryName();
	}

	abstract static class ParticipationMixin {
		/** Participations are nested under their athlete. */
		@JsonIgnore
		abstract Athlete getAthlete();

		@JsonIgnore
		abstract ParticipationId getId();

		@JsonProperty("categoryCode")
		@JsonSerialize(using = V2Json.CategoryCodeSerializer.class)
		abstract Category getCategory();

		@JsonProperty("categoryCode")
		@JsonDeserialize(using = V2Json.CategoryByCodeDeserializer.class)
		abstract void setCategory(Category category);

		@JsonIgnore(false)
		@JsonProperty
		abstract int getBestAthleteRank();

		@JsonIgnore(false)
		@JsonProperty
		abstract int getCategoryScoreRank();

		@JsonIgnore(false)
		@JsonProperty(access = JsonProperty.Access.READ_ONLY)
		abstract Double getCategoryScore();

		@JsonProperty(access = JsonProperty.Access.READ_ONLY)
		abstract ChampionshipType getChampionshipType();
	}

	@JsonIdentityInfo(generator = ObjectIdGenerators.None.class)
	abstract static class AthleteMixin {
		@JsonIgnore
		abstract Long getId();

		@JsonProperty("sessionName")
		@JsonSerialize(using = V2Json.SessionNameSerializer.class)
		abstract Group getGroup();

		@JsonProperty("sessionName")
		@JsonDeserialize(using = V2Json.SessionByNameDeserializer.class)
		abstract void setGroup(Group group);

		@JsonProperty("categoryCode")
		@JsonSerialize(using = V2Json.CategoryCodeSerializer.class)
		abstract Category getCategory();

		@JsonProperty("categoryCode")
		@JsonDeserialize(using = V2Json.CategoryByCodeDeserializer.class)
		abstract void setCategory(Category category);

		@JsonSerialize(using = V2Json.TeamIdSerializer.class)
		abstract String getTeam();

		@JsonDeserialize(using = V2Json.TeamByIdDeserializer.class)
		abstract void setTeam(String team);

		/** Keeps year-only precision; the entity hides this setter from V1. */
		@JsonIgnore(false)
		@JsonProperty("isoBirthDate")
		abstract void setIsoBirthDate(String isoBirthDate);

		@JsonIgnore(false)
		@JsonProperty
		abstract Double getPresumedBodyWeight();

		@JsonIgnore(false)
		@JsonProperty
		abstract boolean isForcedAsCurrent();

		@JsonIgnore(false)
		@JsonProperty(access = JsonProperty.Access.READ_ONLY)
		abstract Integer getKey();

		@JsonIgnore(false)
		@JsonProperty(access = JsonProperty.Access.READ_ONLY)
		abstract Integer getBestSnatch();

		@JsonIgnore(false)
		@JsonProperty(access = JsonProperty.Access.READ_ONLY)
		abstract Integer getBestCleanJerk();

		@Weight abstract String getSnatch1Declaration();
		@Weight abstract void setSnatch1Declaration(String w);
		@Weight abstract String getSnatch1Change1();
		@Weight abstract void setSnatch1Change1(String w);
		@Weight abstract String getSnatch1Change2();
		@Weight abstract void setSnatch1Change2(String w);
		@Weight abstract String getSnatch1ActualLift();
		@Weight abstract void setSnatch1ActualLift(String w);

		@Weight abstract String getSnatch2Declaration();
		@Weight abstract void setSnatch2Declaration(String w);
		@Weight abstract String getSnatch2Change1();
		@Weight abstract void setSnatch2Change1(String w);
		@Weight abstract String getSnatch2Change2();
		@Weight abstract void setSnatch2Change2(String w);
		@Weight abstract String getSnatch2ActualLift();
		@Weight abstract void setSnatch2ActualLift(String w);

		@Weight abstract String getSnatch3Declaration();
		@Weight abstract void setSnatch3Declaration(String w);
		@Weight abstract String getSnatch3Change1();
		@Weight abstract void setSnatch3Change1(String w);
		@Weight abstract String getSnatch3Change2();
		@Weight abstract void setSnatch3Change2(String w);
		@Weight abstract String getSnatch3ActualLift();
		@Weight abstract void setSnatch3ActualLift(String w);

		@Weight abstract String getCleanJerk1Declaration();
		@Weight abstract void setCleanJerk1Declaration(String w);
		@Weight abstract String getCleanJerk1Change1();
		@Weight abstract void setCleanJerk1Change1(String w);
		@Weight abstract String getCleanJerk1Change2();
		@Weight abstract void setCleanJerk1Change2(String w);
		@Weight abstract String getCleanJerk1ActualLift();
		@Weight abstract void setCleanJerk1ActualLift(String w);

		@Weight abstract String getCleanJerk2Declaration();
		@Weight abstract void setCleanJerk2Declaration(String w);
		@Weight abstract String getCleanJerk2Change1();
		@Weight abstract void setCleanJerk2Change1(String w);
		@Weight abstract String getCleanJerk2Change2();
		@Weight abstract void setCleanJerk2Change2(String w);
		@Weight abstract String getCleanJerk2ActualLift();
		@Weight abstract void setCleanJerk2ActualLift(String w);

		@Weight abstract String getCleanJerk3Declaration();
		@Weight abstract void setCleanJerk3Declaration(String w);
		@Weight abstract String getCleanJerk3Change1();
		@Weight abstract void setCleanJerk3Change1(String w);
		@Weight abstract String getCleanJerk3Change2();
		@Weight abstract void setCleanJerk3Change2(String w);
		@Weight abstract String getCleanJerk3ActualLift();
		@Weight abstract void setCleanJerk3ActualLift(String w);
	}

	/** Stored fields, as in V1 ({@code StoredChampionshipMixin}), without the database id. */
	@JsonAutoDetect(
	        fieldVisibility = JsonAutoDetect.Visibility.ANY,
	        getterVisibility = JsonAutoDetect.Visibility.NONE,
	        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	        setterVisibility = JsonAutoDetect.Visibility.NONE)
	abstract static class ChampionshipMixin {
		@JsonIgnore
		Long id;
	}

	abstract static class TimetableEntryMixin {
		@JsonIgnore
		abstract Long getId();

		/** The entity refers to its session by id, through annotations on this field. */
		@JsonIdentityInfo(generator = ObjectIdGenerators.None.class)
		@JsonIdentityReference(alwaysAsId = false)
		Group group;

		@JsonProperty("sessionName")
		@JsonSerialize(using = V2Json.SessionNameSerializer.class)
		abstract Group getGroup();

		@JsonProperty("sessionName")
		@JsonDeserialize(using = V2Json.SessionByNameDeserializer.class)
		abstract void setGroup(Group group);
	}
}
