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
import java.util.HashMap;
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
	 * Find all championships. Loads from database; falls back to age-group derivation
	 * if the Championship table is empty (pre-migration or first startup).
	 *
	 * @return the sorted list
	 */
	public static List<Championship> findAll() {
		if (allChampionshipsMap == null || allChampionshipsMap.isEmpty()) {
			allChampionshipsMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

			// Try loading from the persisted Championship table first.
			List<Championship> stored = ChampionshipRepository.findAll();
			if (stored != null && !stored.isEmpty()) {
				for (Championship c : stored) {
					allChampionshipsMap.put(c.getName(), c);
				}
			} else {
				// Fallback: derive from age groups (pre-migration or empty DB).
				// Seed default entries.
				String name = null;
				name = Translator.translate("Division." + ChampionshipType.DEFAULT.name());
				Championship defaultChamp = new Championship(name, ChampionshipType.DEFAULT);
				allChampionshipsMap.put(name, defaultChamp);
				name = Translator.translate("Division." + ChampionshipType.MASTERS.name());
				Championship mastersChamp = new Championship(name, ChampionshipType.MASTERS);
				allChampionshipsMap.put(name, mastersChamp);

				// Derive additional championships from persisted age groups.
				List<String> allChampionships = AgeGroupRepository.allChampionshipsForAllAgeGroups();
				for (String s : allChampionships) {
					String typeString = null;
					String nameString = null;
					if (s.contains("¤")) {
						String[] arr = s.split("¤");
						if (arr.length > 1) {
							typeString = arr[1];
						} else {
							typeString = "U";
						}
						nameString = arr[0];
					} else {
						typeString = s;
						nameString = s;
					}
					ChampionshipType cType = ChampionshipType.U;
					try {
						cType = ChampionshipType.valueOf(typeString);
					} catch (Exception e) {
					}
					// Add to cache only (not to DB) during fallback.
					String canonicalName = canonicalizeChampionshipName(nameString);
					ChampionshipType canonicalType = canonicalizeChampionshipType(canonicalName, cType);
					if (!allChampionshipsMap.containsKey(canonicalName)) {
						allChampionshipsMap.put(canonicalName, new Championship(canonicalName, canonicalType));
					}
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

	public ChampionshipType getType() {
		return this.type;
	}

	/**
	 * Checks if is default.
	 *
	 * @return true, if is default
	 */
	public boolean isDefault() {
		return this.getType() == ChampionshipType.DEFAULT;
	}

	public boolean isMixed() {
		return this.getType() != null && this.getType().isMixed();
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

	public void setType(ChampionshipType type) {
		this.type = type;
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
	
	/**
	 * Gets the best athlete scoring system from the age groups in this championship.
	 * Uses majority vote - returns the scoring system used by most age groups.
	 * 
	 * @param ageGroupPrefix the age group prefix to filter by (optional, can be null)
	 * @return the best athlete scoring system, or null if none found
	 */
	public Ranking getBestAthleteScoringSystem(String ageGroupPrefix) {
		List<AgeGroup> ageGroups = AgeGroupRepository.findFiltered(null, null, this, null, true, -1, -1);
		
		// Filter by age group prefix if provided
		if (ageGroupPrefix != null && !ageGroupPrefix.isBlank()) {
			ageGroups = ageGroups.stream()
				.filter(ag -> ageGroupPrefix.equals(ag.getCode()))
				.toList();
		}
		
		// Count occurrences of each scoring system
		Map<Ranking, Integer> scoringSystemCounts = new HashMap<>();
		for (AgeGroup ag : ageGroups) {
			Ranking system = ag.getBestAthleteScoringSystem();
			if (system != null) {
				scoringSystemCounts.put(system, scoringSystemCounts.getOrDefault(system, 0) + 1);
			}
		}
		
		// Return the most common scoring system (majority vote)
		Ranking mostCommon = null;
		int maxCount = 0;
		for (Map.Entry<Ranking, Integer> entry : scoringSystemCounts.entrySet()) {
			if (entry.getValue() > maxCount) {
				maxCount = entry.getValue();
				mostCommon = entry.getKey();
			}
		}
		
		return mostCommon;
	}

}
