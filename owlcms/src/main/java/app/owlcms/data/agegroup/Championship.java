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

	public static Championship addChampionship(String nameString, ChampionshipType u2) {
		Championship championship = allChampionshipsMap.get(nameString.toLowerCase());
		if (championship == null) {
			Championship newChampionship = new Championship(nameString, u2);
			allChampionshipsMap.put(nameString.toLowerCase(),
			        newChampionship);
			return newChampionship;
		}
		return championship;
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
			allChampionshipsMap.put(name.toLowerCase(), new Championship(name, ChampionshipType.DEFAULT));
			name = Translator.translate("Division." + ChampionshipType.MASTERS.name());
			allChampionshipsMap.put(name.toLowerCase(), new Championship(name, ChampionshipType.MASTERS));

			// allChampionshipsMap.put(ADAPTIVE, new Championship(ChampionshipType.ADAPTIVE));

			// additional championships.
			List<String> allChampionships = AgeGroupRepository.allChampionshipsForAllAgeGroups();

			for (String s : allChampionships) {
				String typeString = null;
				String nameString = null;
				if (s.contains("¤")) {
					String[] arr = s.split("¤");
					typeString = arr[1];
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
			allChampionshipsList.sort(Championship::compareTo);
		} else {
			allChampionshipsList = new ArrayList<>(allChampionshipsMap.values());
			allChampionshipsList.sort(Championship::compareTo);
		}
		return allChampionshipsList;
	}

	public static List<Championship> findAllUsed(boolean activeOnly) {
		var results = new ArrayList<Championship>();
		findAll();
		List<String> names = AgeGroupRepository.allActiveChampionshipsNames(activeOnly);
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
		return allChampionshipsMap.get(championshipName.toLowerCase());
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

}
