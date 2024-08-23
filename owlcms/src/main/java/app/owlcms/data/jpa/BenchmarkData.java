/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.jpa;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * TestData.
 */
public class BenchmarkData {

	private static final double LENGTH_OF_WEIGHIN = 2.0;
	private static final long LENGTH_OF_SESSION = 2;
	private static Logger logger = (Logger) LoggerFactory.getLogger(BenchmarkData.class);

	// private static Logger startLogger = (Logger) LoggerFactory.getLogger(Main.class);
	static {
		logger.setLevel(Level.INFO);
	}
	final static String[] lnames = { "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson",
	        "Moore", "Taylor", "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson", "Garcia",
	        "Martinez", "Robinson", "Clark", "Rodriguez", "Lewis", "Lee", "Walker", "Hall", "Allen", "Young",
	        "Hernandez", "King", "Wright", "Lopez", "Hill", "Scott", "Green", "Adams", "Baker", "Gonzalez",
	        "Nelson", "Carter", "Mitchell", "Perez", "Roberts", "Turner", "Phillips", "Campbell", "Parker", "Evans",
	        "Edwards", "Collins", };
	final static String[] mNames = { "James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph",
	        "Thomas", "Charles", "Christopher", "Daniel", "Matthew", "Anthony", "Donald", "Mark", "Paul", "Steven",
	        "Andrew", "Kenneth", "George", "Joshua", "Kevin", "Brian", "Edward", "Ronald", "Timothy", "Jason",
	        "Jeffrey", "Ryan", "Jacob", "Gary", "Nicholas", "Eric", "Stephen", "Jonathan", "Larry", "Justin",
	        "Scott", "Brandon", "Frank", "Benjamin", "Gregory", "Raymond", "Samuel", "Patrick", "Alexander", "Jack",
	        "Dennis", "Jerry", };
	final static String[] fNames = { "Emily", "Abigail", "Alexis", "Alyssa", "Angela", "Ashley", "Brianna", "Cynthia",
	        "Deborah", "Donna", "Elizabeth", "Elizabeth", "Emma", "Grace", "Hannah", "Jennifer", "Jessica", "Julie",
	        "Karen", "Kayla", "Kimberly", "Laura", "Lauren", "Linda", "Lisa", "Lori", "Madison", "Mary", "Megan",
	        "Michelle", "Olivia", "Pamela", "Patricia", "Samantha", "Sandra", "Sarah", "Susan", "Tammy", "Taylor",
	        "Victoria", };
	static String[][] bwcats = {
	        { "45", "49", "55", "59", "64", "71", "76", "81", "87", "+87" },
	        { "55", "61", "67", "73", "81", "89", "96", "102", "109", "+109" } };
	static String[][] ageGroups = {
	        { "YTH", "JR", "SR", "W35", "W40", "W45", "W50", "W55", "W60", "W65", "W70", "W75", "W80", "W85" },
	        { "YTH", "JR", "SR", "M35", "M40", "W45", "W50", "M55", "M60", "M65", "M70", "M75", "M80", "M85" } };
	private static int nbAthletesPerGender;

	/**
	 * Insert initial data if the database is empty.
	 *
	 * @param championshipTypes
	 */
	public static void insertInitialData(EnumSet<ChampionshipType> championshipTypes) {
		JPAService.runInTransaction(em -> {
			Competition competition = createDefaultCompetition(championshipTypes);
			CompetitionRepository.save(competition);
			AgeGroupRepository.insertAgeGroups(em, championshipTypes);
			return null;
		});

		JPAService.runInTransaction(em -> {
			setupBenchmarkData(em, 4, 5);
			return null;
		});

		AthleteRepository.resetParticipations();

		JPAService.runInTransaction(em -> {
			AthleteRepository.doFindAll(em).stream()
			        .forEach(a -> a.getParticipations().forEach(part -> part.setTeamMember(true)));
			return null;
		});

		JPAService.runInTransaction(em -> {
			startNumbers(em);
			return null;
		});
	}

	private static void assignStartNumbers(EntityManager em, Group groupA) {
		List<Athlete> athletes = AthleteRepository.doFindAllByGroupAndWeighIn(em, groupA, true, (Gender) null);
		AthleteSorter.registrationOrder(athletes);
		AthleteSorter.doAssignStartNumbers(athletes);
	}

	private static void createAthlete(EntityManager em, Group session, Random r, Gender gender, int genderIndex,
	        String ageGroup, String bwcat) {
		Athlete p = new Athlete();
		p.setValidation(false);
		Level prevLoggerLevel = p.getLogger().getLevel();
		try {
			p.setLoggerLevel(Level.WARN);
			Group mg = (em.contains(session) ? session : em.merge(session));
			p.setGroup(mg);
			if (gender == Gender.F) {
				p.setFirstName(fNames[r.nextInt(fNames.length)]);
			} else {
				p.setFirstName(mNames[r.nextInt(mNames.length)]);
			}
			p.setLastName(lnames[r.nextInt(lnames.length)]);
			p.setGender(gender);
			double nextDouble = r.nextDouble();
			int minAge = 13;
			int maxAge = 99;
			if (ageGroup.startsWith("M") || ageGroup.startsWith("W")) {
				// ageDivision = Championship.of(Championship.MASTERS));
				String s = ageGroup.substring(1);
				minAge = Integer.parseInt(s);
				maxAge = minAge + 5; // (exclusive)
			} else {
				// ageDivision = Championship.of(Championship.IWF));
				if (ageGroup.contentEquals("YTH")) {
					minAge = 13;
					maxAge = 17;
				} else {
					minAge = 15;
					maxAge = (ageGroup.contentEquals("JR")) ? 20 : 999;
				}
			}

			int cat;
			if (bwcat.startsWith(">") || bwcat.startsWith("+")) {
				bwcat = bwcat.substring(1);
				cat = Integer.parseInt(bwcat);
				cat = cat + 10;
			} else {
				cat = Integer.parseInt(bwcat);
			}

			int referenceYear = LocalDate.now().getYear();
			LocalDate baseDate = LocalDate.of(referenceYear, 12, 31);

			double catLimit = cat;
			double bodyWeight = catLimit - (nextDouble * 2.0);
			p.setBodyWeight(bodyWeight);
			double sd = catLimit * (1 + (r.nextGaussian() / 10));
			long isd = Math.round(sd);
			p.setSnatch1Declaration(Long.toString(isd));
			long icjd = Math.round(sd * 1.20D);
			p.setCleanJerk1Declaration(Long.toString(icjd));
			nextDouble = r.nextDouble();

			// 10 athletes per team max (roughly); so normally there are nbAthletes/10 teams at a minimum
			// create more to be under 10.
			int nbTeams = (int) ((nbAthletesPerGender / 10) * 1.4);
			String team = "T" + (int) (nextDouble * nbTeams);
			p.setTeam(team);
			logger.info("creating athlete {} {} {} {} {} {}", p.getLastName(), p.getFirstName(), team, gender, ageGroup,
			        bwcat);
			// compute a random number of weeks inside the age bracket
			long weeksToSubtract = (long) ((minAge * 52) + Math.floor(r.nextDouble() * (maxAge - minAge) * 52));
			LocalDate fullBirthDate = baseDate.minusWeeks(weeksToSubtract);
			p.setFullBirthDate(fullBirthDate);

			// categories not assigned here, will be computed automatically according to birth
			// date etc.

			// respect 20kg rule
			p.setQualifyingTotal((int) (isd + icjd - 15));
			em.persist(p);
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
		} finally {
			p.setLoggerLevel(prevLoggerLevel);
		}

	}

	private static Competition createDefaultCompetition(EnumSet<ChampionshipType> championshipTypes) {
		// RecordConfig rc = new RecordConfig(Arrays.asList());
		// JPAService.runInTransaction(em -> {
		// em.persist(rc);
		// return null;
		// });

		Competition competition = new Competition();

		competition.setCompetitionName("Spring Equinox Open");
		competition.setCompetitionCity("Sometown, Lower Cascadia");
		competition.setCompetitionDate(LocalDate.of(2019, 03, 23));
		competition.setCompetitionOrganizer("Giant Weightlifting Club");
		competition.setCompetitionSite("West-End Gym");
		competition.setFederation("National Weightlifting Federation");
		competition.setFederationAddress("22 River Street, Othertown, Upper Cascadia,  J0H 1J8");
		competition.setFederationEMail("results@national-weightlifting.org");
		competition.setFederationWebSite("http://national-weightlifting.org");

		competition.setEnforce20kgRule(true);
		competition.setMasters(championshipTypes != null && championshipTypes.contains(ChampionshipType.MASTERS));
		competition.setUseBirthYear(false);
		competition.setAnnouncerLiveDecisions(true);
		competition.setMensBestN(null);
		competition.setWomensBestN(null);

		return competition;
	}

	private static void drawLots(EntityManager em) {
		List<Athlete> athletes = AthleteRepository.doFindAll(em);
		AthleteSorter.drawLots(athletes);
	}

	/**
	 * Setup benchmark data.
	 *
	 * 4 groups A-D, 14 athletes per session, all IWF and IMWA age groups. 81 sessions required to produce 1134 athletes
	 * (target size of benchmark).
	 *
	 * @param sessionsPerDay
	 *
	 */
	private static void setupBenchmarkData(EntityManager em, int nbPlatforms, int sessionsPerDay) {
		char lastGroup = 'D';
		LocalDateTime c = LocalDateTime.now();
		nbAthletesPerGender = (lastGroup - 'A' + 1) * ageGroups[0].length * bwcats[0].length;

		LocalDateTime startOfCompetition = LocalDateTime.of(c.getYear(), c.getMonth(), c.getDayOfMonth(), 9, 00, 0);
		c = startOfCompetition;

		Platform[] platforms = new Platform[4];
		for (int i = 0; i < nbPlatforms; i++) {
			platforms[i] = new Platform("P" + (i + 1));
			em.persist(platforms[i]);
		}
		int sessionCount = 0;
		Random r = new Random(0);

		for (char groupName = lastGroup; groupName >= 'A'; groupName--) {
			for (int bwCatIndex = 0; bwCatIndex < 10; bwCatIndex++) {
				for (int genderIndex = 0; genderIndex < 2; genderIndex++) {
					Gender g = Gender.values()[genderIndex];

					int platformIndex = sessionCount % nbPlatforms;
					String sessionName = "P" + (platformIndex + 1) + "-" + g.name()
					        + bwcats[genderIndex][bwCatIndex]
					        + groupName;
					sessionCount++;

					if (sessionCount > 0 && (sessionCount % (nbPlatforms * sessionsPerDay)) == 0) {
						// all sessions for one day are done
						c = startOfCompetition.plusDays(sessionCount / (nbPlatforms * sessionsPerDay));
					} else if (sessionCount > 0 && (sessionCount % nbPlatforms) == 0) {
						// all platforms done with their respective session
						c = c.plusHours(LENGTH_OF_SESSION);

					}
					Group session = new Group(sessionName, c.plusHours((long) -LENGTH_OF_WEIGHIN), c);
					session.setPlatform(platforms[platformIndex]);
					em.persist(session);

					// probability of being in A B C D group varies with age group, we ignore this.
					// we could do something more sophisticated like.
					// group D and C = 3 YTH + one each masters
					// group B is 50% JR and 50% SR
					// group A is 75% SR and 25% JR
					logger.info(sessionName);
					for (int ageGroupIndex = 0; ageGroupIndex < ageGroups[genderIndex].length; ageGroupIndex++) {
						// add an athlete to the session
						createAthlete(em, session, r, g, genderIndex, ageGroups[genderIndex][ageGroupIndex],
						        bwcats[genderIndex][bwCatIndex]);
						em.flush();
					}

				}
			}
		}
		int nbSess = sessionCount + 1;
		logger.info("sessions: " + nbSess + " athletes: " + nbSess * ageGroups[0].length);

		for (Platform p : platforms) {
			em.persist(p);
		}
		em.flush();

		logger.info("assigning start numbers");
		startNumbers(em);

	}

	private static void startNumbers(EntityManager em) {
		drawLots(em);
		List<Group> sessions = GroupRepository.doFindAll(em);
		for (Group s : sessions) {
			assignStartNumbers(em, s);
		}
	}
}
