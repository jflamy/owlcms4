/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.tests;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class TestData.
 */
public class TestData {

	private static Logger logger = (Logger) LoggerFactory.getLogger(TestData.class);
	static { logger.setLevel(Level.INFO); }

	/**
	 * Insert initial data if the database is empty.
	 *
	 * @param nbAthletes how many athletes
	 * @param testMode   true if creating dummy data
	 */
	public static void insertInitialData(int nbAthletes, boolean testMode) {
		JPAService.runInTransaction(em -> {
			setupTestData(em, nbAthletes);
			return null;
		});
	}

	/**
	 * Setup test data.
	 * 
	 * @param em
	 *
	 * @param competition   the competition
	 * @param liftersToLoad the lifters to load
	 * @param w             the w
	 * @param c             the c
	 */
	protected static void setupTestData(EntityManager em, int liftersToLoad) {
		logger.info("inserting test data.");
		CategoryRepository.insertStandardCategories(em);

		LocalDateTime w = LocalDateTime.now();
		LocalDateTime c = w.plusHours((long) 2.0);

		Platform platform1 = new Platform("Gym 1"); //$NON-NLS-1$
		Platform platform2 = new Platform("Gym 2"); //$NON-NLS-1$

		Group groupA = new Group("A", w, c); //$NON-NLS-1$
		groupA.setPlatform(platform1);

		Group groupB = new Group("B", w, c); //$NON-NLS-1$
		groupB.setPlatform(platform2);

		Group groupC = new Group("C", w, c); //$NON-NLS-1$
		groupC.setPlatform(platform1);

		insertSampleLifters(em, liftersToLoad, groupA, groupB, groupC);

		em.persist(groupA);
		em.persist(groupB);
		em.persist(groupC);
		
		// needed because some classes such as Athlete refer to the current competition
		Competition.setCurrent(new Competition());
	}

	static void insertSampleLifters(EntityManager em, int liftersToLoad, Group groupA,
			Group groupB,
			Group groupC) {
		final String[] fnames = { "Peter", "Albert", "Joshua", "Mike", "Oliver", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				"Paul", "Alex", "Richard", "Dan", "Umberto", "Henrik", "Rene", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
				"Fred", "Donald" }; //$NON-NLS-1$ //$NON-NLS-2$
		final String[] lnames = { "Smith", "Gordon", "Simpson", "Brown", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"Clavel", "Simons", "Verne", "Scott", "Allison", "Gates", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
				"Rowling", "Barks", "Ross", "Schneider", "Tate" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

		Random r = new Random(0);

		createGroup(em, groupA, fnames, lnames, r, 81, 73, liftersToLoad);
		createGroup(em, groupB, fnames, lnames, r, 73, 67, liftersToLoad);

	}

	protected static void createGroup(EntityManager em, Group group, final String[] fnames, final String[] lnames,
			Random r,
			int cat1, int cat2, int liftersToLoad) {
		for (int i = 0; i < liftersToLoad; i++) {
			Athlete p = new Athlete();
			p.setGroup(group);
			p.setFirstName(fnames[r.nextInt(fnames.length)]);
			p.setLastName(lnames[r.nextInt(lnames.length)]);
			createAthlete(em, r, p, 0.0D, cat1);
			em.persist(p);
		}
	}

	protected static void drawLots(EntityManager em) {
		List<Athlete> athletes = AthleteRepository.doFindAll(em);
		AthleteSorter.drawLots(athletes);
	}

	protected static void assignStartNumbers(EntityManager em, Group groupA) {
		List<Athlete> athletes = AthleteRepository.doFindAllByGroupAndWeighIn(em, groupA, true);
		AthleteSorter.registrationOrder(athletes);
		AthleteSorter.assignStartNumbers(athletes);
	}

	protected static void createAthlete(EntityManager em, Random r, Athlete p, double nextDouble, int catLimit) {
		p.setBodyWeight(81 - nextDouble);
		Category categ = CategoryRepository.doFindByName("m" + catLimit, em);
		p.setCategory(categ);
	}
	
	protected static void deleteAllLifters(EntityManager em) {
		List<Athlete> athletes = AthleteRepository.doFindAll(em);
		for (Athlete a: athletes) {
			em.remove(a);
		}
	}

}
