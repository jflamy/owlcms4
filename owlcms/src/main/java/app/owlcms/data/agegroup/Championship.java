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
import java.util.Optional;
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
	 * Persists to database and updates the in-memory cache.
	 */
	public static Championship addChampionship(String nameString, ChampionshipType u2) {
		if (allChampionshipsMap == null) {
			findAll();
		}
		String canonicalName = canonicalizeChampionshipName(nameString);
		ChampionshipType canonicalType = canonicalizeChampionshipType(canonicalName, u2);
		Championship championship = allChampionshipsMap.get(canonicalName);
		if (championship == null) {
			Championship newChampionship = new Championship(canonicalName, canonicalType);
			newChampionship.populateScoringDefaults();
			newChampionship = ChampionshipRepository.save(newChampionship);
			allChampionshipsMap.put(canonicalName, newChampionship);
			logger.debug("Added to map and DB: key='{}', name='{}', type='{}'", canonicalName, newChampionship.getName(), newChampionship.getType());
			return newChampionship;
		}
		return championship;
	}

	/**
	 * Returns the canonical championship type for known names (e.g., 'Masters').
	 */
	private static ChampionshipType canonicalizeChampionshipType(String name, ChampionshipType type) {
		if (name != null && name.equals("Masters")) {
			return ChampionshipType.MASTERS;
		}
		return type != null ? type : ChampionshipType.U;
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

			List<Championship> stored = ChampionshipRepository.findAll();
			if ((stored == null || stored.isEmpty()) && !AgeGroupRepository.findAll().isEmpty()) {
				ChampionshipRepository.bootstrapFromAgeGroups();
				stored = ChampionshipRepository.findAll();
			}
			if (stored != null) {
				for (Championship c : stored) {
					allChampionshipsMap.put(c.getName(), c);
				}
			}
		}
		ArrayList<Championship> allChampionshipsList = new ArrayList<>(allChampionshipsMap.values());
		allChampionshipsList.sort(Championship::compareTo);
		return allChampionshipsList;
	}

	public static List<Championship> findAllUsed(boolean activeOnly) {
		var results = new ArrayList<Championship>();
		findAll();
		List<String> names = AgeGroupRepository.allActiveChampionshipsNames(activeOnly).stream()
		        .map(n -> canonicalizeChampionshipName(n))
		        .sorted().distinct().toList();
		for (String n : names) {
			Championship of = Championship.of(n);
			results.add(of);
		}
		results.sort(ct.reversed());
		return results;
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

	public static Championship of(String championshipName) {
		if (allChampionshipsMap == null) {
			findAll();
		}
		if (championshipName == null) {
			return new Championship("", ChampionshipType.U);
		}
		String canonicalName = canonicalizeChampionshipName(championshipName);
		Championship cached = allChampionshipsMap.get(canonicalName);
		if (cached != null) {
			return cached;
		}
		// Fallback: check DB in case it was just created by another path
		Championship fromDb = ChampionshipRepository.findByName(canonicalName);
		if (fromDb != null) {
			allChampionshipsMap.put(fromDb.getName(), fromDb);
		}
		return fromDb;
	}

	public static Championship ofType(ChampionshipType t) {
		if (allChampionshipsMap == null) {
			findAll();
		}
		// return first championship of the type
		// we use reverse order to get Open and Senior and U20 first.
		Optional<Championship> found = allChampionshipsMap.values().stream().sorted(Comparator.reverseOrder()).filter(v -> v.getType() == t).findFirst();
		return found.isPresent() ? found.get() : null;
	}

	public static void remove(Championship c) {
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

	@Column(columnDefinition = "boolean default false")
	private boolean mixedTeamEnabled = false;

	@Enumerated(EnumType.STRING)
	private Ranking teamScoringSystem;

	@Enumerated(EnumType.STRING)
	private Ranking mixedTeamScoringSystem;

	public Championship() {
	}

	public Championship(ChampionshipType type) {
		this.name = type.name();
		this.setType(type);
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
		return this.scoringSystem;
	}

	public Ranking getBestAthleteScoringSystem() {
		return this.bestAthleteScoringSystem;
	}

	public Ranking getBestSnatchScoringSystem() {
		return this.bestSnatchScoringSystem;
	}

	public Ranking getBestCJScoringSystem() {
		return this.bestCJScoringSystem;
	}

	public boolean isSnatchCJTotalMedals() {
		return this.snatchCJTotalMedals;
	}

	public Integer getTeamPoints1st() {
		return this.teamPoints1st;
	}

	public Integer getTeamPoints2nd() {
		return this.teamPoints2nd;
	}

	public Integer getTeamPoints3rd() {
		return this.teamPoints3rd;
	}

	public Integer getMensBestN() {
		return this.mensBestN;
	}

	public Integer getWomensBestN() {
		return this.womensBestN;
	}

	public Integer getMixedMensBestN() {
		return this.mixedMensBestN;
	}

	public Integer getMixedWomensBestN() {
		return this.mixedWomensBestN;
	}

	public Integer getMixedBestN() {
		return this.mixedBestN;
	}

	public Integer getExplicitTeamSize() {
		return this.explicitTeamSize != null ? this.explicitTeamSize : getMaxTeamSize();
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
		return this.type;
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
		if (allChampionshipsMap != null) {
			allChampionshipsMap.remove(this.name);
		}
		this.name = name;
		if (allChampionshipsMap != null) {
			allChampionshipsMap.put(this.name, this);
		}
		if (this.id != null) {
			ChampionshipRepository.save(this);
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
		return this.maxTeamSize != null ? this.maxTeamSize : 8;
	}

	public void setMaxTeamSize(Integer maxTeamSize) {
		this.maxTeamSize = maxTeamSize;
	}

	private int getGenderedTeamSize(String ageGroupPrefix, Integer configuredTopN, Gender gender) {
		Integer topN = positiveCap(configuredTopN);
		if (topN != null) {
			return topN;
		}
		return getRosterSizeLimit(ageGroupPrefix, gender);
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
		return this.maxPerCategory != null && this.maxPerCategory > 0 ? this.maxPerCategory : 2;
	}

	public void setMaxPerCategory(Integer maxPerCategory) {
		this.maxPerCategory = maxPerCategory;
	}

	public void setExplicitMixedTeamMembers(boolean explicitMixedTeamMembers) {
		this.explicitMixedTeamMembers = explicitMixedTeamMembers;
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
		this.type = type;
	}

	public void populateScoringDefaults() {
		Competition comp = Competition.getCurrent();
		if (comp == null) {
			return;
		}
		this.scoringSystem = comp.getScoringSystem();
		this.bestAthleteScoringSystem = comp.getScoringSystem();
		this.snatchCJTotalMedals = comp.isSnatchCJTotalMedals();
		this.teamPoints1st = comp.getTeamPoints1st();
		this.teamPoints2nd = comp.getTeamPoints2nd();
		this.teamPoints3rd = comp.getTeamPoints3rd();
		// bestN fields: 0 means "count all" (no scoring cap).
		// maxTeamSize is the roster cap (how many athletes on a team).
		// These are separate concepts; don't conflate them.
		this.mensBestN = 0;
		this.womensBestN = 0;
		this.mixedMensBestN = 0;
		this.mixedWomensBestN = 0;
		this.mixedBestN = 0;
		this.explicitTeamSize = comp.getMaxTeamSize();
		this.maxTeamSize = comp.getMaxTeamSize();
		this.maxPerCategory = comp.getMaxPerCategory();
		this.explicitMixedTeamMembers = false;
		this.teamScoringSystem = null;
		this.mixedTeamScoringSystem = null;

		List<AgeGroup> ageGroups = AgeGroupRepository.findFiltered(null, null, this, null, true, -1, -1);
		for (AgeGroup ageGroup : ageGroups) {
			if (ageGroup.getScoringSystem() != null) {
				this.scoringSystem = ageGroup.getScoringSystem();
			}
			if (ageGroup.getBestAthleteScoringSystem() != null) {
				this.bestAthleteScoringSystem = ageGroup.getBestAthleteScoringSystem();
			}
		}

		this.bestSnatchScoringSystem = this.bestAthleteScoringSystem;
		this.bestCJScoringSystem = this.bestAthleteScoringSystem;
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
