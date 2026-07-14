/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.simulation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.utils.NaturalOrderComparator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 *
 * Simulate a meet by triggering events and reacting to response.
 *
 * @author Jean-François Lamy
 *
 */
public class CompetitionSimulator {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(CompetitionSimulator.class);
	private static List<FOPSimulator> registeredSimulators = new ArrayList<>();
	private static volatile boolean running;

	public static boolean isRunning() {
		return running;
	}

	public static void setRunning(boolean b) {
		running = b;
	}

	public static synchronized void stopSimulation() {
		setRunning(false);
		Competition.getCurrent().setSimulation(false);
		for (FOPSimulator s : new ArrayList<>(registeredSimulators)) {
			s.stop();
		}
		registeredSimulators.clear();
	}

	public static synchronized void simulatorCompleted(FOPSimulator simulator) {
		registeredSimulators.remove(simulator);
		if (registeredSimulators.isEmpty()) {
			setRunning(false);
			Competition.getCurrent().setSimulation(false);
		}
	}

	private Random r = new Random(0);
	private final boolean skipDone;
	private final String skipBefore;
	private final Set<String> platformFilter;
	private final boolean randomDeclarationJumps;

	public CompetitionSimulator() {
		this(false);
	}

	public CompetitionSimulator(boolean skipDone) {
		this(skipDone, null);
	}

	public CompetitionSimulator(boolean skipDone, String skipBefore) {
		this(skipDone, skipBefore, null);
	}

	public CompetitionSimulator(boolean skipDone, String skipBefore, String platforms) {
		this(skipDone, skipBefore, platforms, false);
	}

	public CompetitionSimulator(boolean skipDone, String skipBefore, String platforms, boolean randomDeclarationJumps) {
		this.skipDone = skipDone;
		this.skipBefore = normalizeSkipBefore(skipBefore);
		this.platformFilter = parsePlatformFilter(platforms);
		this.randomDeclarationJumps = randomDeclarationJumps;
	}

	public String runSimulation() throws InterruptedException {
		if (isRunning()) {
			return "simulation already running.";
		}
		Competition.getCurrent().setSimulation(true);
		setRunning(true);
		logger.setLevel(Level.DEBUG);

		Map<Platform, List<Group>> groupsByPlatform = new TreeMap<>();
		List<Platform> ps = PlatformRepository.findAll().stream().collect(Collectors.toList());
		List<Platform> simulationPlatforms = ps;
		if (!this.platformFilter.isEmpty()) {
			simulationPlatforms = ps.stream().filter(p -> platformMatchesFilter(p.getName(), this.platformFilter))
			        .collect(Collectors.toList());
		}
		List<Group> gs = GroupRepository.findAll().stream().sorted(new NaturalOrderComparator<>())
		        .sorted((a, b) -> {
			        LocalDateTime ta = a.getCompetitionTime();
			        LocalDateTime tb = b.getCompetitionTime();
			        return ObjectUtils.compare(ta, tb, true);
		        }).collect(Collectors.toList());
		if (this.skipBefore != null) {
			int boundaryIndex = findSkipBoundaryIndex(gs.stream().map(Group::getName).collect(Collectors.toList()),
			        this.skipBefore);
			if (boundaryIndex < 0) {
				logger.warn("simulation skip boundary '{}' is not in the computed group order; no groups will be simulated",
				        this.skipBefore);
				gs = List.of();
			} else {
				gs = new ArrayList<>(gs.subList(boundaryIndex, gs.size()));
			}
		}
		if (!this.platformFilter.isEmpty()) {
			gs = gs.stream().filter(g -> platformMatchesFilter(g.getPlatform(), this.platformFilter))
			        .collect(Collectors.toList());
		}

		if (this.skipDone) {
			gs = gs.stream().filter(g -> !g.isDone()).collect(Collectors.toList());
		} else {
			clearLifts(gs);
		}

		int i = 0;
		for (Group g : gs) {
			if (!isRunning()) {
				return "simulation stopped.";
			}

			List<Athlete> as;
			if (this.skipDone) {
				as = AthleteRepository.findAllByGroupAndWeighIn(g, null);
			} else {
				as = AthleteRepository.findAllByGroupAndWeighIn(g, true);
				if (as.isEmpty()) {
					as = weighIn(g);
				}
				as = AthleteRepository.findAllByGroupAndWeighIn(g, true);
			}
			if (as.size() == 0) {
				logger.info("skipping group {} size {}", g.getName(), as.size());
				continue;
			}
			logger.info("group {} size {} platform {}", g.getName(), as.size(), g.getPlatform());

			int index;

			Platform curP;
			curP = g.getPlatform();
			if (curP == null) {
				index = i % ps.size();
				curP = ps.get(index);
				i++;
			}

			List<Group> curGroupList = groupsByPlatform.get(curP);
			if (curGroupList == null) {
				curGroupList = new ArrayList<>();
			}

			logger.info("platform {}", curP.getName());
			if (as.size() > 0) {
				curGroupList.add(g);
				groupsByPlatform.put(curP, curGroupList);
				logger.info("platform {} groups {}", curP.getName(), groupsByPlatform.get(curP));
			}
		}

		for (FOPSimulator s : registeredSimulators) {
			s.stop();
		}
		registeredSimulators.clear();
		resetFopsForSimulation(simulationPlatforms);

		for (Platform p : simulationPlatforms) {
			if (!isRunning()) {
				return "simulation stopped.";
			}
			List<Group> platformGroups = groupsByPlatform.get(p);
			if (platformGroups == null || platformGroups.isEmpty()) {
				logger.info("skipping platform {} with no simulation groups", p.getName());
				continue;
			}
			FieldOfPlay f = OwlcmsFactory.getFOPByName(p.getName());
			FOPSimulator fopSimulator = new FOPSimulator(f, platformGroups, this.skipDone, this.randomDeclarationJumps);
			fopSimulator.setCompetitionSimulator(this);
			registeredSimulators.add(fopSimulator);
			fopSimulator.go();
		}

		if (registeredSimulators.isEmpty()) {
			setRunning(false);
			Competition.getCurrent().setSimulation(false);
		}
		return "simulation done.";
	}

	private void resetFopsForSimulation(List<Platform> platforms) {
		for (Platform platform : platforms) {
			FieldOfPlay fieldOfPlay = OwlcmsFactory.getFOPByName(platform.getName());
			if (fieldOfPlay == null) {
				continue;
			}
			logger.info("resetting FOP {} before simulation", platform.getName());
			fieldOfPlay.getAthleteTimer().stop();
			fieldOfPlay.getBreakTimer().stop();
			fieldOfPlay.loadGroup(null, this, true);
			fieldOfPlay.setBreakType(null);
			fieldOfPlay.setState(FOPState.INACTIVE);
		}
	}

	private void clearLifts(List<Group> groups) {
		JPAService.runInTransaction(em -> {
			for (Group g : groups) {
				List<Athlete> athletes = AthleteRepository.doFindAllByGroupAndWeighIn(em, g, null, null);
				for (Athlete a : athletes) {
					a.clearLifts();
					em.merge(a);
				}
			}
			em.flush();
			return null;
		});
	}

	static int findSkipBoundaryIndex(List<String> groupNames, String skipBefore) {
		String boundary = normalizeSkipBefore(skipBefore);
		if (groupNames == null || boundary == null) {
			return -1;
		}

		for (int index = 0; index < groupNames.size(); index++) {
			String groupName = groupNames.get(index);
			if (matchesSkipBoundary(groupName, boundary)) {
				return index;
			}
		}
		return -1;
	}

	private static boolean matchesSkipBoundary(String groupName, String boundary) {
		if (groupName == null || !groupName.regionMatches(true, 0, boundary, 0, boundary.length())) {
			return false;
		}
		if (groupName.length() == boundary.length()) {
			return true;
		}
		if (!boundary.chars().allMatch(Character::isDigit)) {
			return false;
		}
		return !Character.isDigit(groupName.charAt(boundary.length()));
	}

	private static String normalizeSkipBefore(String skipBefore) {
		if (skipBefore == null || skipBefore.isBlank()) {
			return null;
		}
		return skipBefore.trim();
	}

	static Set<String> parsePlatformFilter(String platforms) {
		Set<String> parsed = new HashSet<>();
		if (platforms == null || platforms.isBlank()) {
			return parsed;
		}
		for (String platform : platforms.split(",")) {
			String normalized = platform.trim().toLowerCase(Locale.ROOT);
			if (!normalized.isEmpty()) {
				parsed.add(normalized);
			}
		}
		return parsed;
	}

	private static boolean platformMatchesFilter(Platform platform, Set<String> platformFilter) {
		return platform != null && platformMatchesFilter(platform.getName(), platformFilter);
	}

	static boolean platformMatchesFilter(String platformName, Set<String> platformFilter) {
		if (platformFilter == null || platformFilter.isEmpty()) {
			return true;
		}
		if (platformName == null) {
			return false;
		}
		return platformFilter.contains(platformName.trim().toLowerCase(Locale.ROOT));
	}

	List<Athlete> weighIn(Group g) {
		List<Athlete> as = AthleteRepository.findAllByGroupAndWeighIn(g, null);
		weighInAthletes(as);
		return as;
	}

	/**
	 * Prepare a not-done session for re-simulation in skip-done mode.
	 *
	 * A not-done session may contain a mix of weighed-in and not-weighed-in athletes (left over from a
	 * previous simulation, or added afterwards). To get a clean, consistent run we clear any leftover
	 * lifts and then re-weigh the whole session, re-randomizing body weight and openers for every
	 * athlete.
	 */
	void prepareSkipDoneGroup(Group g) {
		List<Athlete> as = AthleteRepository.findAllByGroupAndWeighIn(g, null);
		for (Athlete a : as) {
			a.clearLifts();
			AthleteRepository.save(a);
		}
		weighInAthletes(as);
	}

	private void weighInAthletes(List<Athlete> as) {
		Random r = new Random();
		for (Athlete a : as) {
			Category c = a.getCategory();
			if (c == null) {
				a.setGroup(null);
				AthleteRepository.save(a);
				continue;
			}
			Double catUpper = c.getMaximumWeight();
			Double catLower = c.getMinimumWeight();
			if (catUpper > 998 && catLower <= 1.01) {
				// logger.trace("open {} {} {} {}", a.getLastName(), a.getCategoryCode(), catLower, catUpper);
				// open category
				double nextGaussian = r.nextGaussian(85, 15);
				a.setBodyWeight(nextGaussian);
				catUpper = (double) Math.round(2.0 + nextGaussian + 2.0);
			} else {
				// logger.trace("!!! not open {} {} {} {}", a.getLastName(), a.getCategoryCode(), catLower, catUpper);
				if (catUpper > 998) {
					catUpper = catLower * 1.1;
				}
				double bodyWeight = catUpper - (this.r.nextDouble() * 2.0);
				a.setBodyWeight(bodyWeight);
			}

			Integer entryTotal = a.getEntryTotal();
			if (entryTotal != null && entryTotal > 0) {
				long isd = Math.round(entryTotal * 0.44D); // qualification snatch
				long icjd = Math.round(entryTotal * 0.56D); // qualification CJ
				a.setSnatch1Declaration(Long.toString(isd));
				a.setCleanJerk1Declaration(Long.toString(icjd));
				AthleteRepository.save(a);
			} else {
				double sd = catUpper * (1 + (this.r.nextGaussian() / 10));
				long isd = Math.round(sd);
				a.setSnatch1Declaration(Long.toString(isd));
				long icjd = Math.round(sd * 1.20D);
				a.setCleanJerk1Declaration(Long.toString(icjd));
				AthleteRepository.save(a);
			}
		}
	}

}
