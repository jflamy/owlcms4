/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
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

	public static final String MASTERS = ChampionshipType.MASTERS.name();
	public static final String U = ChampionshipType.U.name();
	public static final String IWF = ChampionshipType.IWF.name();
	public static final String OLY = ChampionshipType.OLY.name();
	public static final String DEFAULT = ChampionshipType.DEFAULT.name();
	public static final String ADAPTIVE = ChampionshipType.ADAPTIVE.name();
	@SuppressWarnings("unused")
	final private static Logger logger = (Logger) LoggerFactory.getLogger(Championship.class);
	private static Map<String, Championship> allChampionshipsMap;
	private static List<Championship> allChampionshipsList;
	static Comparator<Championship> ct = Comparator.comparing(Championship::getType)
			.thenComparing(Championship::getNameLength)
	        .thenComparing(Championship::getName);
	
	/**
	 * Find all.
	 *
	 * @return the collection
	 */
	public static List<Championship> findAll() {
		if (allChampionshipsMap == null || allChampionshipsMap.isEmpty()) {
			allChampionshipsMap = new HashMap<>();

			// default championships, always present.
			allChampionshipsMap.put(U, new Championship(ChampionshipType.U));
			allChampionshipsMap.put(MASTERS, new Championship(ChampionshipType.MASTERS));
			allChampionshipsMap.put(OLY, new Championship(ChampionshipType.OLY));
			allChampionshipsMap.put(IWF, new Championship(ChampionshipType.IWF));
			allChampionshipsMap.put(DEFAULT, new Championship(ChampionshipType.DEFAULT));
			allChampionshipsMap.put(ADAPTIVE, new Championship(ChampionshipType.ADAPTIVE));

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
				if (allChampionshipsMap.get(nameString) == null) {
					allChampionshipsMap.put(nameString,
					        new Championship(nameString, ChampionshipType.valueOf(typeString)));
				}
			}
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
		return value != null ? value : of(Championship.DEFAULT);
	}

	public static Championship of(String championshipName) {
		if (allChampionshipsMap == null) {
			findAll();
		}
		return allChampionshipsMap.get(championshipName);
	}

	public static void reset() {
		allChampionshipsMap = null;
		allChampionshipsList = null;
		findAll();
	}

	public static Championship[] values() {
		return findAll().toArray(new Championship[0]);
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
	
	public int getNameLength() {
		return this.name.length();
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

	public void setType(ChampionshipType type) {
		this.type = type;
	}

	public String translate() {
		String tr = Translator.translateOrElseNull("Championship."+getName(), OwlcmsSession.getLocale());
		return tr != null ? tr : getName();
	}

	@Override
	public String toString() {
		return "Championship [name=" + name + ", type=" + type + "]";
	}

}
