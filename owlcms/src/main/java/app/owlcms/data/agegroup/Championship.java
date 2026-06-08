/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import app.owlcms.data.competition.Competition;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

/**
 * Championship entity.
 *
 * Represents a named championship grouping of age groups used for award computation.
 * Persisted to the database; the static methods provide a thin cache over the DB.
 */
@Entity
@Cacheable
public class Championship implements Comparable<Championship>, Serializable {

	private static final long serialVersionUID = 1L;
	public static final String COMPETITION_TEMPLATE_NAME = "COMPETITION_TEMPLATE";
	private static final int DEFAULT_TEAM_SIZE = 8;
	private static final int UNBOUNDED_TEAM_SIZE = 999;
	private static final int LEGACY_UNBOUNDED_TEAM_SIZE = 50;

	final private static Logger logger = (Logger) LoggerFactory.getLogger(Championship.class);
	private static Map<String, Championship> allChampionshipsMap;
	static Comparator<Championship> ct = (a, b) -> {
		int compare = 0;
		if (a == null || b == null) {
			return ObjectUtils.compare(a, b, true);
		}
		compare = ObjectUtils.compare(a.getType(), b.getType(), true);
		if (compare != 0) {
			return compare;
		}
		var aLength = a.getName() != null ? a.getName().length() : 0;
		var bLength = b.getName() != null ? b.getName().length() : 0;
		compare = ObjectUtils.compare(aLength, bLength);
		if (compare != 0) {
			return compare;
		}
		compare = ObjectUtils.compare(a.getName(), b.getName(), true);
		return compare;
	};

	/**
	 * Adds a championship, normalizing 'Masters' variants to canonical form.
	 * Delegates to the single canonical creation primitive in
	 * {@link ChampionshipRepository#createChampionship(String, ChampionshipType)}
	 * so the row inherits the competition template the same way an auto-materialized
	 * one does.
	 */
	public static Championship addChampionship(String nameString, ChampionshipType u2) {
		if (allChampionshipsMap == null) {
			findAll();
		}
		String canonicalName = canonicalizeChampionshipName(nameString);
		ChampionshipType canonicalType = canonicalizeChampionshipType(canonicalName, u2);
		Championship existing = allChampionshipsMap.get(canonicalName);
		if (existing != null) {
			return existing;
		}
		Championship created = ChampionshipRepository.createChampionship(canonicalName, canonicalType);
		// createChampionship resets the cache; refresh and return the canonical cached instance.
		Championship cached = allChampionshipsMap != null ? allChampionshipsMap.get(canonicalName) : null;
		if (cached == null) {
			cached = findStored(canonicalName);
		}
		if (cached != null) {
			logger.debug("Added to map and DB: key='{}', name='{}', type='{}'", canonicalName, cached.getName(),
			        cached.getType());
			return cached;
		}
		return created;
	}

	/**
	 * Returns the canonical championship type for known names (e.g., 'Masters').
	 */
	private static ChampionshipType canonicalizeChampionshipType(String name, ChampionshipType type) {
		if (name != null && name.equals("Masters")) {
			return ChampionshipType.MASTERS;
		}
		return ChampionshipType.normalizeOrDefault(type);
	}

	   /**
		* Returns the canonical championship name for known types (e.g., 'Masters').
		*/
	   public static String canonicalizeChampionshipName(String name) {
		   if (name != null && name.trim().equalsIgnoreCase("masters")) {
			   return "Masters";
		   }
		   return name;
	   }

	/**
	 * Find all championships from the stored Championship table.
	 *
	 * @return the sorted list
	 */
	public static List<Championship> findAll() {
		if (allChampionshipsMap == null || allChampionshipsMap.isEmpty()) {
			allChampionshipsMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

			List<Championship> stored = ChampionshipRepository.findAllIncludingTemplate();
			if ((stored == null || stored.isEmpty()) && !AgeGroupRepository.findAll().isEmpty()) {
				ChampionshipRepository.bootstrapFromAgeGroups();
				stored = ChampionshipRepository.findAllIncludingTemplate();
			}
			if (stored != null) {
				for (Championship c : stored) {
					allChampionshipsMap.put(c.getName(), c);
				}
			}
		}
		ArrayList<Championship> allChampionshipsList = new ArrayList<>(allChampionshipsMap.values().stream()
		        .filter(c -> !c.isCompetitionTemplate()).toList());
		allChampionshipsList.sort(Championship::compareTo);
		return allChampionshipsList;
	}

	public static Championship findCompetitionTemplate() {
		if (allChampionshipsMap == null) {
			findAll();
		}
		return allChampionshipsMap.values().stream()
		        .filter(Championship::isCompetitionTemplate)
		        .findFirst()
		        .orElse(null);
	}

	public static List<Championship> findAllIncludingTemplate() {
		if (allChampionshipsMap == null) {
			findAll();
		}
		ArrayList<Championship> allChampionshipsList = new ArrayList<>(allChampionshipsMap.values());
		allChampionshipsList.sort(Championship::compareTo);
		return allChampionshipsList;
	}

	public static List<Championship> findAllUsed(boolean activeOnly) {
		var results = new TreeMap<String, Championship>(String.CASE_INSENSITIVE_ORDER);
		findAll();
		for (AgeGroup ageGroup : AgeGroupRepository.findAll()) {
			if (activeOnly && !ageGroup.isActive()) {
				continue;
			}
			String name = effectiveChampionshipName(ageGroup);
			if (name == null || name.isBlank()) {
				continue;
			}
			Championship championship = findStored(name);
			if (championship == null) {
				championship = new Championship(name,
				        canonicalizeChampionshipType(name, ageGroup.getConfiguredChampionshipType()));
			}
			if (!championship.isCompetitionTemplate()) {
				results.put(championship.getName(), championship);
			}
		}
		var sortedResults = new ArrayList<>(results.values());
		sortedResults.sort(ct.reversed());
		return sortedResults;
	}

	private static String effectiveChampionshipName(AgeGroup ageGroup) {
		String name = ageGroup.computeChampionshipName();
		if (name == null || name.isBlank() || name.trim().equalsIgnoreCase(COMPETITION_TEMPLATE_NAME)) {
			name = ageGroup.getCode();
		}
		return canonicalizeChampionshipName(name != null ? name.trim() : null);
	}

	/**
	 * Gets the age division from name.
	 *
	 * @param name the name
	 * @return the age division from name
	 */
	static public Championship getChampionshipFromName(String name) {
		if (name == null) {
			return null;
		}
		Championship value = of(name);
		return value;
	}

	public static Map<String, Championship> getMap() {
		if (allChampionshipsMap == null) {
			findAll();
		}
		return allChampionshipsMap;
	}

	public static Championship findStored(String championshipName) {
		if (championshipName == null || championshipName.isBlank()) {
			return null;
		}
		if (allChampionshipsMap == null) {
			findAll();
		}
		String canonicalName = canonicalizeChampionshipName(championshipName);
		Championship cached = allChampionshipsMap.get(canonicalName);
		if (cached != null) {
			return cached;
		}
		Championship fromDb = ChampionshipRepository.findByName(canonicalName);
		if (fromDb != null) {
			allChampionshipsMap.put(fromDb.getName(), fromDb);
		}
		return fromDb;
	}

	public static Championship ensureStored(String championshipName, ChampionshipType type) {
		Championship existing = findStored(championshipName);
		if (existing != null) {
			return existing;
		}
		if (championshipName == null || championshipName.isBlank()) {
			return null;
		}
		return addChampionship(championshipName, type);
	}

	public static Championship of(String championshipName) {
		Championship stored = findStored(championshipName);
		if (stored != null) {
			return stored;
		}
		Championship template = findCompetitionTemplate();
		return template != null ? template : DefaultChampionship.getInstance();
	}

	public static boolean anyMultiMedal(Set<Championship> championships) {
		return championships != null && championships.stream().filter(c -> c != null).anyMatch(Championship::isSnatchCJTotalMedals);
	}

	public static boolean anyScoreMedalChampionship(Set<Championship> championships) {
		return championships != null && championships.stream().filter(c -> c != null).anyMatch(Championship::isScoreMedalChampionship);
	}

	public static Championship ofType(ChampionshipType t) {
		if (allChampionshipsMap == null) {
			findAll();
		}
		// return first championship of the type
		// we use reverse order to get Open and Senior and U20 first.
		Optional<Championship> found = allChampionshipsMap.values().stream().filter(v -> !v.isCompetitionTemplate()).sorted(Comparator.reverseOrder()).filter(v -> v.getType() == t).findFirst();
		return found.isPresent() ? found.get() : null;
	}

	public static void remove(Championship c) {
		if (c.isCompetitionTemplate()) {
			return;
		}
		allChampionshipsMap.remove(c.getName());
		if (c.getId() != null) {
			ChampionshipRepository.delete(c);
		}
	}

	public static void reset() {
		allChampionshipsMap = null;
		findAll();
	}

	public static void update(Championship c) {
		ChampionshipRepository.save(c);
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(unique = true, nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	private ChampionshipType type;

	@Enumerated(EnumType.STRING)
	private Ranking scoringSystem;

	@Enumerated(EnumType.STRING)
	private Ranking bestAthleteScoringSystem;

	@Enumerated(EnumType.STRING)
	private Ranking bestSnatchScoringSystem;

	@Enumerated(EnumType.STRING)
	private Ranking bestCJScoringSystem;

	@Column(columnDefinition = "boolean default false")
	private boolean snatchCJTotalMedals = false;

	private Integer teamPoints1st;
	private Integer teamPoints2nd;
	private Integer teamPoints3rd;

	private Integer mensBestN;
	private Integer womensBestN;
	private Integer mixedMensBestN;
	private Integer mixedWomensBestN;
	private Integer mixedBestN;
	private Integer explicitTeamSize;

	private Integer maxTeamSize;
	private Integer maxPerCategory;

	@Column(columnDefinition = "boolean default false")
	private boolean explicitMixedTeamMembers = false;

	@Column(columnDefinition = "boolean default true")
	private boolean genderedTeamsEnabled = true;

	@Column(columnDefinition = "boolean default false")
	private boolean mixedTeamEnabled = false;

	@Column(columnDefinition = "boolean default false")
	private boolean competitionTemplate = false;

	@Enumerated(EnumType.STRING)
	private Ranking teamScoringSystem;

	@Enumerated(EnumType.STRING)
	private Ranking mixedTeamScoringSystem;

	public Championship() {
	}

	public Championship(ChampionshipType type) {
		ChampionshipType normalized = ChampionshipType.normalizeOrDefault(type);
		this.name = normalized.name();
		this.setType(normalized);
	}

	public Championship(String name, ChampionshipType type) {
		this.name = name;
		this.setType(type);
	}

	@Override
	public int compareTo(Championship o) {
		return ct.compare(this, o);
	}

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public Ranking getScoringSystem() {
		if (computeUsesCompetitionDefaults()) {
			return getCompetitionDefaults().getScoringSystem();
		}
		return this.scoringSystem;
	}

	public Ranking getBestAthleteScoringSystem() {
		if (computeUsesCompetitionDefaults()) {
			return getCompetitionDefaults().getBestAthleteScoringSystem();
		}
		return this.bestAthleteScoringSystem;
	}

	public Ranking getBestSnatchScoringSystem() {
		if (computeUsesCompetitionDefaults()) {
			return getCompetitionDefaults().getBestSnatchScoringSystem();
		}
		return this.bestSnatchScoringSystem;
	}

	public Ranking getBestCJScoringSystem() {
		if (computeUsesCompetitionDefaults()) {
			return getCompetitionDefaults().getBestCJScoringSystem();
		}
		return this.bestCJScoringSystem;
	}

	public boolean isSnatchCJTotalMedals() {
		if (computeUsesCompetitionDefaults()) {
			return getCompetitionDefaults().isSnatchCJTotalMedals();
		}
		return this.snatchCJTotalMedals;
	}

	public Integer getTeamPoints1st() {
		if (computeUsesCompetitionDefaults()) {
			return getCompetitionDefaults().getTeamPoints1st();
		}
		return this.teamPoints1st;
	}

	public Integer getTeamPoints2nd() {
		if (computeUsesCompetitionDefaults()) {
			return getCompetitionDefaults().getTeamPoints2nd();
		}
		return this.teamPoints2nd;
	}

	public Integer getTeamPoints3rd() {
		if (computeUsesCompetitionDefaults()) {
			return getCompetitionDefaults().getTeamPoints3rd();
		}
		return this.teamPoints3rd;
	}

	public Integer getMensBestN() {
		if (computeUsesCompetitionDefaults()) {
			return getCompetitionDefaults().getMensBestN();
		}
		return this.mensBestN;
	}

	public Integer getWomensBestN() {
		if (computeUsesCompetitionDefaults()) {
			return getCompetitionDefaults().getWomensBestN();
		}
		return this.womensBestN;
	}

	public Integer getMixedMensBestN() {
		return this.mixedMensBestN;
	}

	public Integer getMixedWomensBestN() {
		return this.mixedWomensBestN;
	}

	public Integer getMixedBestN() {
		if (computeUsesCompetitionDefaults()) {
			return getCompetitionDefaults().getMixedBestN();
		}
		return this.mixedBestN;
	}

	public Integer getExplicitTeamSize() {
		return this.explicitTeamSize != null ? this.explicitTeamSize : getDefaultExplicitMixedTeamSize();
	}

	@JsonIgnore
	public int getConfiguredTeamSize(String ageGroupPrefix, Gender gender) {
		if (gender == null) {
			return getRosterSizeLimit(ageGroupPrefix, Gender.MF);
		}

		switch (gender) {
			case M:
				return getGenderedTeamSize(ageGroupPrefix, this.mensBestN, Gender.M);
			case F:
				return getGenderedTeamSize(ageGroupPrefix, this.womensBestN, Gender.F);
			case MF:
				return getMixedTeamSize(ageGroupPrefix);
			case I:
			default:
				return 0;
		}
	}

	@JsonIgnore
	public int getGenderedTeamSize(String ageGroupPrefix, Gender gender) {
		return getConfiguredTeamSize(ageGroupPrefix, gender);
	}

	@JsonIgnore
	public int getMixedTeamSize(String ageGroupPrefix) {
		Integer topNMixed = positiveCap(this.mixedBestN);
		if (topNMixed != null) {
			return topNMixed;
		}

		Integer topNMen = positiveCap(this.mixedMensBestN);
		Integer topNWomen = positiveCap(this.mixedWomensBestN);
		if (topNMen != null || topNWomen != null) {
			return defaultZero(topNMen) + defaultZero(topNWomen);
		}

		return getRosterSizeLimit(ageGroupPrefix, Gender.MF);
	}

	public Ranking getTeamScoringSystem() {
		return this.teamScoringSystem;
	}

	public boolean isGenderedTeamsEnabled() {
		return this.genderedTeamsEnabled;
	}

	public boolean computePointsBased() {
		return this.teamScoringSystem == null;
	}

	public Ranking getMixedTeamScoringSystem() {
		return this.mixedTeamScoringSystem;
	}

	public boolean computeMixedPointsBased() {
		return this.mixedTeamScoringSystem == null;
	}

	public boolean isExplicitMixedTeamMembers() {
		return this.explicitMixedTeamMembers;
	}

	public ChampionshipType getType() {
		return ChampionshipType.normalizeOrDefault(this.type);
	}

	public boolean isCompetitionTemplate() {
		return this.competitionTemplate;
	}

	public void setCompetitionTemplate(boolean competitionTemplate) {
		this.competitionTemplate = competitionTemplate;
	}

	/**
	 * Checks if is default.
	 *
	 * @return true, if is default
	 */
	@JsonIgnore
	public boolean isDefault() {
		return this.getType() == ChampionshipType.DEFAULT;
	}

	@JsonIgnore
	public boolean isMixed() {
		return this.explicitMixedTeamMembers;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Rename a championship: update the in-memory cache and persist.
	 * Must only be called on fully-constructed, managed entities.
	 */
	public void rename(String newName) {
		if (newName == null) {
			return;
		}
		String canonicalNewName = canonicalizeChampionshipName(newName.trim());
		if (canonicalNewName == null || canonicalNewName.isBlank() || canonicalNewName.equals(this.name)) {
			return;
		}

		String oldName = this.name;
		Championship renamed = this.id != null ? ChampionshipRepository.rename(this, canonicalNewName) : null;
		this.name = renamed != null ? renamed.getName() : canonicalNewName;

		if (allChampionshipsMap != null) {
			if (oldName != null) {
				allChampionshipsMap.remove(oldName);
			}
			allChampionshipsMap.put(this.name, this);
		}
	}

	public void setScoringSystem(Ranking scoringSystem) {
		this.scoringSystem = scoringSystem;
	}

	public void setBestAthleteScoringSystem(Ranking bestAthleteScoringSystem) {
		this.bestAthleteScoringSystem = bestAthleteScoringSystem;
	}

	public void setBestSnatchScoringSystem(Ranking bestSnatchScoringSystem) {
		this.bestSnatchScoringSystem = bestSnatchScoringSystem;
	}

	public void setBestCJScoringSystem(Ranking bestCJScoringSystem) {
		this.bestCJScoringSystem = bestCJScoringSystem;
	}

	public void setSnatchCJTotalMedals(boolean snatchCJTotalMedals) {
		this.snatchCJTotalMedals = snatchCJTotalMedals;
	}

	public void setTeamPoints1st(Integer teamPoints1st) {
		this.teamPoints1st = teamPoints1st;
	}

	public void setTeamPoints2nd(Integer teamPoints2nd) {
		this.teamPoints2nd = teamPoints2nd;
	}

	public void setTeamPoints3rd(Integer teamPoints3rd) {
		this.teamPoints3rd = teamPoints3rd;
	}

	public Integer getMaxTeamSize() {
		if (computeUsesCompetitionDefaults()) {
			Integer competitionMaxTeamSize = getCompetitionDefaults().getMaxTeamSize();
			return competitionMaxTeamSize != null ? competitionMaxTeamSize : 8;
		}
		return this.maxTeamSize != null ? this.maxTeamSize : 8;
	}

	public void setMaxTeamSize(Integer maxTeamSize) {
		this.maxTeamSize = maxTeamSize;
	}

	private Integer creationMaxTeamSize(Integer maxTeamSize) {
		if (maxTeamSize == null || maxTeamSize <= 0 || maxTeamSize == LEGACY_UNBOUNDED_TEAM_SIZE) {
			return UNBOUNDED_TEAM_SIZE;
		}
		return maxTeamSize;
	}

	private Integer creationBestN(Integer bestN, Integer maxTeamSize) {
		if (bestN == null || bestN <= 0) {
			return bestN;
		}
		Integer normalizedMaxTeamSize = creationMaxTeamSize(maxTeamSize);
		if (normalizedMaxTeamSize != null && normalizedMaxTeamSize != UNBOUNDED_TEAM_SIZE
		        && bestN > normalizedMaxTeamSize) {
			return normalizedMaxTeamSize;
		}
		return bestN;
	}

	private int getGenderedTeamSize(String ageGroupPrefix, Integer configuredTopN, Gender gender) {
		Integer topN = positiveCap(configuredTopN);
		if (topN != null) {
			return topN;
		}
		return getRosterSizeLimit(ageGroupPrefix, gender);
	}

	private int getDefaultExplicitMixedTeamSize() {
		return AgeGroupRepository.findFiltered(null, null, this, null, true, -1, -1).stream()
		        .map(AgeGroup::getCategories)
		        .mapToInt(List::size)
		        .filter(categoryCount -> categoryCount > 0)
		        .min()
		        .orElse(getMaxTeamSize());
	}

	private int getRosterSizeLimit(String ageGroupPrefix, Gender gender) {
		if (gender == Gender.MF && this.explicitMixedTeamMembers) {
			return getExplicitTeamSize();
		}
		return getMaxTeamSize();
	}

	private Integer positiveCap(Integer value) {
		return value != null && value > 0 ? value : null;
	}

	private int defaultZero(Integer value) {
		return value != null ? value.intValue() : 0;
	}

	public Integer getMaxPerCategory() {
		if (computeUsesCompetitionDefaults()) {
			return getCompetitionDefaults().getMaxPerCategory();
		}
		return this.maxPerCategory != null && this.maxPerCategory > 0 ? this.maxPerCategory : 2;
	}

	@JsonIgnore
	public boolean usesCompetitionDefaults() {
		return computeUsesCompetitionDefaults();
	}

	public void setUseCompetitionDefaults(boolean useCompetitionDefaults) {
		if (useCompetitionDefaults && !this.competitionTemplate) {
			copyCompetitionDefaults();
		}
	}

	@JsonIgnore
	public boolean computeUsesCompetitionDefaults() {
		return !this.competitionTemplate && !computeDifferentFromCompetitionDefaults();
	}

	@JsonIgnore
	public boolean computeDifferentFromCompetitionDefaults() {
		return computeDifferentFromCompetitionDefaults(getCompetitionDefaults(), false);
	}

	public boolean computeDifferentFromCompetitionDefaults(Championship competitionDefaults) {
		return computeDifferentFromCompetitionDefaults(competitionDefaults, false);
	}

	public boolean computeDifferentFromCompetitionDefaults(Championship competitionDefaults, boolean traceWhenDifferent) {
		return !computeCompetitionDefaultDifferences(competitionDefaults, traceWhenDifferent).isEmpty();
	}

	public List<String> computeCompetitionDefaultDifferences(Championship competitionDefaults) {
		return computeCompetitionDefaultDifferences(competitionDefaults, false);
	}

	public List<String> computeCompetitionDefaultDifferences(Championship competitionDefaults, boolean traceWhenDifferent) {
		List<String> differences = new ArrayList<>();
		if (competitionDefaults == null) {
			differences.add("competitionDefaults=<null>");
			traceCompetitionDefaultDifferences(differences, traceWhenDifferent);
			return differences;
		}
		addDifference(differences, "scoringSystem", this.scoringSystem, competitionDefaults.scoringSystem);
		addDifference(differences, "bestAthleteScoringSystem", this.bestAthleteScoringSystem,
		        competitionDefaults.bestAthleteScoringSystem);
		addDifference(differences, "bestSnatchScoringSystem", this.bestSnatchScoringSystem,
		        competitionDefaults.bestSnatchScoringSystem);
		addDifference(differences, "bestCJScoringSystem", this.bestCJScoringSystem,
		        competitionDefaults.bestCJScoringSystem);
		addDifference(differences, "snatchCJTotalMedals", this.snatchCJTotalMedals,
		        competitionDefaults.snatchCJTotalMedals);
		addDifference(differences, "teamPoints1st", this.teamPoints1st, competitionDefaults.teamPoints1st);
		addDifference(differences, "teamPoints2nd", this.teamPoints2nd, competitionDefaults.teamPoints2nd);
		addDifference(differences, "teamPoints3rd", this.teamPoints3rd, competitionDefaults.teamPoints3rd);
		addDifference(differences, "mensBestN", this.mensBestN, competitionDefaults.mensBestN);
		addDifference(differences, "womensBestN", this.womensBestN, competitionDefaults.womensBestN);
		addDifference(differences, "mixedMensBestN", this.mixedMensBestN, competitionDefaults.mixedMensBestN);
		addDifference(differences, "mixedWomensBestN", this.mixedWomensBestN, competitionDefaults.mixedWomensBestN);
		addDifference(differences, "mixedBestN", this.mixedBestN, competitionDefaults.mixedBestN);
		addDifference(differences, "explicitTeamSize", this.explicitTeamSize, competitionDefaults.explicitTeamSize);
		addDifference(differences, "maxTeamSize", this.maxTeamSize, competitionDefaults.maxTeamSize);
		addDifference(differences, "maxPerCategory", this.maxPerCategory, competitionDefaults.maxPerCategory);
		addDifference(differences, "explicitMixedTeamMembers", this.explicitMixedTeamMembers,
		        competitionDefaults.explicitMixedTeamMembers);
		addDifference(differences, "genderedTeamsEnabled", this.genderedTeamsEnabled,
		        competitionDefaults.genderedTeamsEnabled);
		addDifference(differences, "mixedTeamEnabled", this.mixedTeamEnabled, competitionDefaults.mixedTeamEnabled);
		addDifference(differences, "teamScoringSystem", this.teamScoringSystem, competitionDefaults.teamScoringSystem);
		addDifference(differences, "mixedTeamScoringSystem", this.mixedTeamScoringSystem,
		        competitionDefaults.mixedTeamScoringSystem);
		traceCompetitionDefaultDifferences(differences, traceWhenDifferent);
		return differences;
	}

	private void addDifference(List<String> differences, String fieldName, Object value, Object defaultValue) {
		if (!Objects.equals(value, defaultValue)) {
			differences.add(fieldName + "=" + value + " (<> " + defaultValue + ")");
		}
	}

	private void traceCompetitionDefaultDifferences(List<String> differences, boolean traceWhenDifferent) {
		if (traceWhenDifferent && !differences.isEmpty()) {
			logger.debug("CHAMPIONSHIP_DEFAULT_TRACE '{}' differs from competition defaults: {}", this.name,
			        differences);
		}
	}

	public void copyCompetitionSettingsFrom(Championship template) {
		if (template == null) {
			return;
		}
		this.scoringSystem = template.scoringSystem;
		this.bestAthleteScoringSystem = template.bestAthleteScoringSystem;
		this.bestSnatchScoringSystem = template.bestSnatchScoringSystem;
		this.bestCJScoringSystem = template.bestCJScoringSystem;
		this.snatchCJTotalMedals = template.snatchCJTotalMedals;
		this.teamPoints1st = template.teamPoints1st;
		this.teamPoints2nd = template.teamPoints2nd;
		this.teamPoints3rd = template.teamPoints3rd;
		this.mensBestN = template.mensBestN;
		this.womensBestN = template.womensBestN;
		this.mixedMensBestN = template.mixedMensBestN;
		this.mixedWomensBestN = template.mixedWomensBestN;
		this.mixedBestN = template.mixedBestN;
		this.explicitTeamSize = template.explicitTeamSize;
		this.maxTeamSize = template.maxTeamSize;
		this.maxPerCategory = template.maxPerCategory;
		this.explicitMixedTeamMembers = template.explicitMixedTeamMembers;
		this.genderedTeamsEnabled = template.genderedTeamsEnabled;
		this.mixedTeamEnabled = template.mixedTeamEnabled;
		this.teamScoringSystem = template.teamScoringSystem;
		this.mixedTeamScoringSystem = template.mixedTeamScoringSystem;
	}

	@JsonIgnore
	public boolean isScoreMedalChampionship() {
		Ranking scoringSystem = getScoringSystem();
		return scoringSystem != null && scoringSystem.isMedalScore();
	}

	@Deprecated
	@JsonIgnore
	public boolean isSinclair() {
		return isScoreMedalChampionship();
	}

	public void copyCompetitionDefaults() {
		Championship comp = getCompetitionDefaults();
		if (comp == null) {
			return;
		}
		copyCompetitionSettingsFrom(comp);
	}

	protected Championship getCompetitionDefaults() {
		Championship template = Championship.findCompetitionTemplate();
		return template != null && template != this ? template : DefaultChampionship.getInstance();
	}

	public void setMaxPerCategory(Integer maxPerCategory) {
		this.maxPerCategory = maxPerCategory;
	}

	public void setExplicitMixedTeamMembers(boolean explicitMixedTeamMembers) {
		this.explicitMixedTeamMembers = explicitMixedTeamMembers;
	}

	public void setGenderedTeamsEnabled(boolean genderedTeamsEnabled) {
		this.genderedTeamsEnabled = genderedTeamsEnabled;
	}

	public boolean isMixedTeamEnabled() {
		return this.mixedTeamEnabled;
	}

	public void setMixedTeamEnabled(boolean mixedTeamEnabled) {
		this.mixedTeamEnabled = mixedTeamEnabled;
	}

	public void setMensBestN(Integer mensBestN) {
		this.mensBestN = mensBestN;
	}

	public void setWomensBestN(Integer womensBestN) {
		this.womensBestN = womensBestN;
	}

	public void setMixedMensBestN(Integer mixedMensBestN) {
		this.mixedMensBestN = mixedMensBestN;
	}

	public void setMixedWomensBestN(Integer mixedWomensBestN) {
		this.mixedWomensBestN = mixedWomensBestN;
	}

	public void setMixedBestN(Integer mixedBestN) {
		this.mixedBestN = mixedBestN;
	}

	public void setExplicitTeamSize(Integer explicitTeamSize) {
		this.explicitTeamSize = explicitTeamSize;
	}

	public void setTeamScoringSystem(Ranking teamScoringSystem) {
		this.teamScoringSystem = teamScoringSystem;
	}

	public void setMixedTeamScoringSystem(Ranking mixedTeamScoringSystem) {
		this.mixedTeamScoringSystem = mixedTeamScoringSystem;
	}

	public void setType(ChampionshipType type) {
		this.type = ChampionshipType.normalizeOrDefault(type);
	}

	public void populateScoringDefaults() {
		Championship comp = getCompetitionDefaults();
		if (comp == null) {
			return;
		}
		// Inherit every settable field from the competition template so a freshly
		// materialized championship starts out aligned with the defaults. Per-age-group
		// scoring overrides (if any) are then layered on top.
		copyCompetitionSettingsFrom(comp);

		List<AgeGroup> ageGroups = AgeGroupRepository.findFiltered(null, null, this, null, true, -1, -1);
		for (AgeGroup ageGroup : ageGroups) {
			if (ageGroup.getScoringSystem() != null) {
				this.scoringSystem = ageGroup.getScoringSystem();
			}
			if (ageGroup.getBestAthleteScoringSystem() != null) {
				this.bestAthleteScoringSystem = ageGroup.getBestAthleteScoringSystem();
			}
		}

		this.bestSnatchScoringSystem = null;
		this.bestCJScoringSystem = null;
	}

	public void populateCompetitionTemplateDefaults(Competition comp) {
		this.name = COMPETITION_TEMPLATE_NAME;
		this.type = ChampionshipType.U;
		setCompetitionTemplate(true);
		this.scoringSystem = comp != null ? comp.getScoringSystem() : Ranking.BW_SINCLAIR;
		this.bestAthleteScoringSystem = this.scoringSystem;
		this.bestSnatchScoringSystem = null;
		this.bestCJScoringSystem = null;
		this.snatchCJTotalMedals = comp != null && comp.isSnatchCJTotalMedals();
		this.teamPoints1st = comp != null ? comp.getTeamPoints1st() : 28;
		this.teamPoints2nd = comp != null ? comp.getTeamPoints2nd() : 25;
		this.teamPoints3rd = comp != null ? comp.getTeamPoints3rd() : 23;
		this.maxTeamSize = comp != null ? creationMaxTeamSize(comp.getMaxTeamSize()) : DEFAULT_TEAM_SIZE;
		this.mensBestN = comp != null ? creationBestN(comp.getMensBestN(), comp.getMaxTeamSize()) : null;
		this.womensBestN = comp != null ? creationBestN(comp.getWomensBestN(), comp.getMaxTeamSize()) : null;
		this.mixedMensBestN = null;
		this.mixedWomensBestN = null;
		this.mixedBestN = comp != null ? comp.getMixedBestN() : null;
		this.explicitTeamSize = DEFAULT_TEAM_SIZE;
		this.maxPerCategory = comp != null ? comp.getMaxPerCategory() : 2;
		this.explicitMixedTeamMembers = false;
		this.genderedTeamsEnabled = true;
		this.mixedTeamEnabled = false;
		this.teamScoringSystem = null;
		this.mixedTeamScoringSystem = null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Championship other = (Championship) obj;
		if (this.name == null || other.name == null) return false;
		return this.name.equals(other.name);
	}

	@Override
	public int hashCode() {
		return this.name != null ? this.name.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "Championship [name=" + this.name + ", type=" + this.type + "]";
	}

	public String translate() {
		String tr = Translator.translateOrElseNull("Championship." + getName(), OwlcmsSession.getLocale());
		return tr != null ? tr : getName();
	}

}
