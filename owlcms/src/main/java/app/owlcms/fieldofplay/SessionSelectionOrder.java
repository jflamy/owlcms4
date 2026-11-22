/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.group.Group;
import app.owlcms.data.platform.Platform;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.utils.NaturalOrderComparator;

/**
 * Shared ordering, section grouping, and label formatting for session selectors.
 * <p>
 * Sections are ordered as:
 * <ol>
 * <li>Unstarted and unfinished sessions on the current platform, in starting order.</li>
 * <li>Unstarted and unfinished sessions on other platforms, grouped by platform (natural platform order, unassigned
 * last), each in starting order.</li>
 * <li>Finished sessions, all together in starting order.</li>
 * </ol>
 */
public final class SessionSelectionOrder {

	private static final NaturalOrderComparator<String> NATURAL_ORDER = new NaturalOrderComparator<>();

	public static final Comparator<Group> STARTING_ORDER_COMPARATOR = (g1, g2) -> {
		if (g1 == g2) {
			return 0;
		}
		if (g1 == null) {
			return 1;
		}
		if (g2 == null) {
			return -1;
		}
		int compare = ObjectUtils.compare(startingTime(g1), startingTime(g2), true);
		if (compare != 0) {
			return compare;
		}
		compare = ObjectUtils.compare(g1.getWeighInTime(), g2.getWeighInTime(), true);
		if (compare != 0) {
			return compare;
		}
		compare = comparePlatformNames(platformName(g1), platformName(g2));
		if (compare != 0) {
			return compare;
		}
		return g1.compareTo(g2);
	};

	private SessionSelectionOrder() {
	}

	/**
	 * Partitions {@code groups} into non-empty display sections separated by dividers in UI pickers.
	 */
	public static List<List<Group>> groupSections(Collection<Group> groups, String currentPlatformName,
	        Predicate<Group> isFinished) {
		if (groups == null || groups.isEmpty()) {
			return List.of();
		}
		Predicate<Group> finishedCheck = isFinished != null ? isFinished : g -> false;
		List<Group> currentPlatform = new ArrayList<>();
		Map<String, List<Group>> otherPlatforms = new TreeMap<>(NATURAL_ORDER);
		List<Group> unassigned = new ArrayList<>();
		List<Group> finished = new ArrayList<>();

		for (Group group : groups) {
			if (group == null) {
				continue;
			}
			if (finishedCheck.test(group)) {
				finished.add(group);
			} else if (isOnPlatform(group, currentPlatformName)) {
				currentPlatform.add(group);
			} else {
				String pName = platformName(group);
				if (pName == null) {
					unassigned.add(group);
				} else {
					otherPlatforms.computeIfAbsent(pName, k -> new ArrayList<>()).add(group);
				}
			}
		}

		List<List<Group>> sections = new ArrayList<>();
		addSortedSection(sections, currentPlatform);
		for (List<Group> platformGroups : otherPlatforms.values()) {
			addSortedSection(sections, platformGroups);
		}
		addSortedSection(sections, unassigned);
		addSortedSection(sections, finished);
		return List.copyOf(sections);
	}

	public static List<Group> order(Collection<Group> groups, String currentPlatformName,
	        Predicate<Group> isFinished) {
		return groupSections(groups, currentPlatformName, isFinished).stream()
		        .flatMap(List::stream)
		        .toList();
	}

	public static Comparator<Group> candidateComparator(String currentPlatformName) {
		return (g1, g2) -> {
			int rank1 = unfinishedPlatformRank(g1, currentPlatformName);
			int rank2 = unfinishedPlatformRank(g2, currentPlatformName);
			if (rank1 != rank2) {
				return Integer.compare(rank1, rank2);
			}
			if (rank1 == 1) {
				int platCompare = comparePlatformNames(platformName(g1), platformName(g2));
				if (platCompare != 0) {
					return platCompare;
				}
			}
			return STARTING_ORDER_COMPARATOR.compare(g1, g2);
		};
	}

	public static boolean hasMultiplePlatforms(Collection<Group> groups) {
		int configuredPlatforms = OwlcmsFactory.getFopByName() != null
		        ? OwlcmsFactory.getFopByName().size()
		        : 0;
		return hasMultiplePlatforms(groups, configuredPlatforms);
	}

	public static boolean hasMultiplePlatforms(Collection<Group> groups, int configuredPlatformCount) {
		if (configuredPlatformCount > 1) {
			return true;
		}
		if (groups == null || groups.isEmpty()) {
			return false;
		}
		return groups.stream()
		        .map(SessionSelectionOrder::platformName)
		        .filter(Objects::nonNull)
		        .distinct()
		        .limit(2)
		        .count() > 1;
	}

	/**
	 * Formats a session label as {@code Platform - Session[ - Description]} when multiple platforms exist, or
	 * {@code Session[ - Description]} when there is only one platform (or the session has no assigned platform).
	 */
	public static String formatLabel(Group group, boolean multiplePlatforms) {
		if (group == null) {
			return "";
		}
		StringBuilder label = new StringBuilder();
		String pName = platformName(group);
		if (multiplePlatforms && pName != null) {
			label.append(pName).append(" - ");
		}
		String name = group.getName();
		if (name != null) {
			label.append(name);
		}
		String description = group.getDescription();
		if (description != null && !description.isBlank()) {
			label.append(" - ").append(description.trim());
		}
		return label.toString();
	}

	public static boolean isOnPlatform(Group group, String platformName) {
		if (platformName == null || platformName.isBlank()) {
			return false;
		}
		String groupPlatform = platformName(group);
		return groupPlatform != null && platformName.trim().equals(groupPlatform);
	}

	public static String platformName(Group group) {
		if (group == null) {
			return null;
		}
		Platform platform = group.getPlatform();
		if (platform == null || platform.getName() == null || platform.getName().isBlank()) {
			return null;
		}
		return platform.getName().trim();
	}

	private static void addSortedSection(List<List<Group>> sections, List<Group> items) {
		if (items.isEmpty()) {
			return;
		}
		items.sort(STARTING_ORDER_COMPARATOR);
		sections.add(List.copyOf(items));
	}

	private static int unfinishedPlatformRank(Group group, String currentPlatformName) {
		if (isOnPlatform(group, currentPlatformName)) {
			return 0;
		}
		return platformName(group) != null ? 1 : 2;
	}

	private static LocalDateTime startingTime(Group group) {
		LocalDateTime competitionTime = group.getCompetitionTime();
		return competitionTime != null ? competitionTime : group.getWeighInTime();
	}

	private static int comparePlatformNames(String p1, String p2) {
		if (Objects.equals(p1, p2)) {
			return 0;
		}
		if (p1 == null) {
			return 1;
		}
		if (p2 == null) {
			return -1;
		}
		return NATURAL_ORDER.compare(p1, p2);
	}
}
