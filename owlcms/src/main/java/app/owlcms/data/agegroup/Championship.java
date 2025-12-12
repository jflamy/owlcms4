/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

/**
 * The Enum Championship.
 *
 * Divisions are listed in registration preference order.
 */
public class Championship implements Comparable<Championship> {

	// public static final String MASTERS = ChampionshipType.MASTERS.name();
	// public static final String U = ChampionshipType.U.name();
	// public static final String IWF = ChampionshipType.IWF.name();
	// public static final String DEFAULT = ChampionshipType.DEFAULT.name();

	@SuppressWarnings("unused")
	final private static Logger logger = (Logger) LoggerFactory.getLogger(Championship.class);
	private static Map<String, Championship> allChampionshipsMap;
	// private static List<Championship> allChampionshipsList;
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
	 */
	public static Championship addChampionship(String nameString, ChampionshipType u2) {
		String canonicalName = canonicalizeChampionshipName(nameString);
		ChampionshipType canonicalType = canonicalizeChampionshipType(canonicalName, u2);
		Championship championship = allChampionshipsMap.get(canonicalName.toLowerCase());
		if (championship == null) {
			Championship newChampionship = new Championship(canonicalName, canonicalType);
			String key = canonicalName.toLowerCase();
			allChampionshipsMap.put(key, newChampionship);
			logger.debug("Added to map: key='{}', name='{}', type='{}'", key, newChampionship.getName(), newChampionship.getType());
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
	 * Find all.
	 *
	 * @return the collection
	 */
	public static List<Championship> findAll() {
		ArrayList<Championship> allChampionshipsList = new ArrayList<>();
		if (allChampionshipsMap == null || allChampionshipsMap.isEmpty()) {
			allChampionshipsMap = new HashMap<>();

			// default championships, always present.
			// allChampionshipsMap.put(U, new Championship(ChampionshipType.U));
			// allChampionshipsMap.put(MASTERS, new Championship(ChampionshipType.MASTERS));
			// allChampionshipsMap.put(OLY, new Championship(ChampionshipType.OLY));
			// allChampionshipsMap.put(IWF, new Championship(ChampionshipType.IWF));
			String name = null;
			name = Translator.translate("Division." + ChampionshipType.DEFAULT.name());
			Championship defaultChamp = new Championship(name, ChampionshipType.DEFAULT);
			allChampionshipsMap.put(name.toLowerCase(), defaultChamp);
			logger.debug("Added to map: key='{}', name='{}', type='{}'", name.toLowerCase(), defaultChamp.getName(), defaultChamp.getType());
			name = Translator.translate("Division." + ChampionshipType.MASTERS.name());
			Championship mastersChamp = new Championship(name, ChampionshipType.MASTERS);
			allChampionshipsMap.put(name.toLowerCase(), mastersChamp);
			logger.debug("Added to map: key='{}', name='{}', type='{}'", name.toLowerCase(), mastersChamp.getName(), mastersChamp.getType());

			// allChampionshipsMap.put(ADAPTIVE, new Championship(ChampionshipType.ADAPTIVE));

			// additional championships.
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
				addChampionship(nameString, cType);
			}
			allChampionshipsList = new ArrayList<>(allChampionshipsMap.values());
			if (!allChampionshipsList.isEmpty()) {
				allChampionshipsList.sort(Championship::compareTo);
			}
		} else {
			allChampionshipsList = new ArrayList<>(allChampionshipsMap.values());
			if (!allChampionshipsList.isEmpty()) {
				allChampionshipsList.sort(Championship::compareTo);
			}
		}
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
		return allChampionshipsMap.get(canonicalName.toLowerCase());
	}

	public static Championship ofType(ChampionshipType t) {
		// return first championship of the type
		// we use reverse order to get Open and Senior and U20 first.
		Optional<Championship> found = allChampionshipsMap.values().stream().sorted(Comparator.reverseOrder()).filter(v -> v.getType() == t).findFirst();
		return found.isPresent() ? found.get() : null;
	}

	public static void remove(Championship c) {
		allChampionshipsMap.remove(c.name.toLowerCase());
	}

	public static void reset() {
		allChampionshipsMap = null;
		findAll();
	}

	public static void update(Championship c) {
	}

	private String name;
	private ChampionshipType type;

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

	public void setName(String name) {
		allChampionshipsMap.remove(this.name.toLowerCase());
		this.name = name;
		allChampionshipsMap.put(this.name.toLowerCase(), this);
	}

	public void setType(ChampionshipType type) {
		this.type = type;
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
