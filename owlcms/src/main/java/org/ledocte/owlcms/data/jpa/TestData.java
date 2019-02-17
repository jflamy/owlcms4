/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.data.jpa;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javax.persistence.EntityManager;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.athlete.AthleteRepository;
import org.ledocte.owlcms.data.athleteSort.AthleteSorter;
import org.ledocte.owlcms.data.category.Category;
import org.ledocte.owlcms.data.category.CategoryRepository;
import org.ledocte.owlcms.data.competition.Competition;
import org.ledocte.owlcms.data.group.Group;
import org.ledocte.owlcms.data.platform.Platform;
import org.ledocte.owlcms.i18n.Messages;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * The Class TestData.
 */
public class TestData {

	private static Locale getLocale() {
		return Locale.ENGLISH;
	}

	private static Logger logger = (Logger) LoggerFactory.getLogger(TestData.class);

	/**
	 * Insert initial data if the database is empty.
	 *
	 * @param nbAthletes 	how many athletes
	 * @param testMode      true if creating dummy data
	 */
	public static void insertInitialData(int nbAthletes, boolean testMode) {
		JPAService.runInTransaction(em -> {
			Competition competition = createDefaultCompetition();

			if (testMode) {
				setupTestData(em, competition, nbAthletes);
			} else {
				setupEmptyCompetition(em, competition);
			}
			em.persist(competition);
			return null;
		});
	}

	protected static Competition createDefaultCompetition() {
		Competition competition = new Competition();

		competition.setCompetitionName(Messages.getString("Competition.competitionName", getLocale()) + " ?");
		competition.setCompetitionCity(Messages.getString("Competition.competitionCity", getLocale()) + " ?");
		competition.setCompetitionDate(LocalDate.now());
		competition
			.setCompetitionOrganizer(Messages.getString("Competition.competitionOrganizer", getLocale()) + " ?");
		competition.setCompetitionSite(Messages.getString("Competition.competitionSite", getLocale()) + " ?");

		String federationLabel = Messages.getString("Competition.federation", getLocale()) + " ?";
		String defaultFederationKey = "Competition.defaultFederation";
		String defaultFederation = Messages.getString(defaultFederationKey, getLocale());
		// if string is not translated, we get its key back.
		competition
			.setFederation(defaultFederation.equals(defaultFederationKey) ? federationLabel : defaultFederation);

		String federationAddressLabel = Messages.getString("Competition.federationAddress", getLocale()) + " ?";
		String defaultFederationAddressKey = "Competition.defaultFederationAddress";
		String defaultFederationAddress = Messages.getString(defaultFederationAddressKey, getLocale());
		// if string is not translated, we get its key back.
		competition.setFederationAddress(
			defaultFederationAddress.equals(defaultFederationAddressKey) ? federationAddressLabel
					: defaultFederationAddress);

		String federationEMailLabel = Messages.getString("Competition.federationEMail", getLocale()) + " ?";
		String defaultFederationEMailKey = "Competition.defaultFederationEMail";
		String defaultFederationEMail = Messages.getString(defaultFederationEMailKey, getLocale());
		// if string is not translated, we get its key back.
		competition
			.setFederationEMail(defaultFederationEMail.equals(defaultFederationEMailKey) ? federationEMailLabel
					: defaultFederationEMail);

		String federationWebSiteLabel = Messages.getString("Competition.federationWebSite", getLocale()) + " ?";
		String defaultFederationWebSiteKey = "Competition.defaultFederationWebSite";
		String defaultFederationWebSite = Messages.getString(defaultFederationWebSiteKey, getLocale());
		// if string is not translated, we get its key back.
		competition.setFederationWebSite(
			defaultFederationWebSite.equals(defaultFederationWebSiteKey) ? federationWebSiteLabel
					: defaultFederationWebSite);
		return competition;
	}

	/**
	 * Create an empty competition. Set-up the defaults for using the timekeeping
	 * and refereeing features.
	 * 
	 * @param em
	 *
	 * @param competition the new up empty competition
	 */
	protected static void setupEmptyCompetition(EntityManager em, Competition competition) {
		Platform platform1 = new Platform("Platform"); //$NON-NLS-1$
		defaultPlates(platform1);

//		setupCompetitionDocuments(competition, platform1);

		em.persist(new Group("M1", null, null)); //$NON-NLS-1$
		em.persist(new Group("M2", null, null)); //$NON-NLS-1$
		em.persist(new Group("M3", null, null)); //$NON-NLS-1$
		em.persist(new Group("M4", null, null)); //$NON-NLS-1$
		em.persist(new Group("F1", null, null)); //$NON-NLS-1$
		em.persist(new Group("F2", null, null)); //$NON-NLS-1$
		em.persist(new Group("F3", null, null)); //$NON-NLS-1$

	}

	protected static void setupCompetitionDocuments(Competition competition, Platform platform1) {
		// competition template
		File templateFile;
		String defaultLanguage = getDefaultLanguage();
		String templateName;
		if (!defaultLanguage.equals("fr")) {
			templateName = "/templates/protocolSheet/ProtocolSheetTemplate_" + defaultLanguage + ".xls";
		} else {
			// historical kludge for Québec
			templateName = "/templates/protocolSheet/Quebec_" + defaultLanguage + ".xls";
		}
		URL templateUrl = platform1.getClass()
			.getResource(templateName);
		try {
			templateFile = new File(templateUrl.toURI());
			competition.setProtocolFileName(templateFile.getCanonicalPath());
		} catch (URISyntaxException e) {
			templateFile = new File(templateUrl.getPath());
		} catch (IOException e) {
		} catch (Exception e) {
			logger.debug("templateName = {}", templateName);
		}

		// competition book template
		templateUrl = platform1.getClass()
			.getResource(
				"/templates/competitionBook/CompetitionBook_Total_" + defaultLanguage + ".xls");
		try {
			templateFile = new File(templateUrl.toURI());
			competition.setResultTemplateFileName(templateFile.getCanonicalPath());
		} catch (URISyntaxException e) {
			templateFile = new File(templateUrl.getPath());
		} catch (IOException e) {
		} catch (Exception e) {
			logger.debug("templateUrl = {}", templateUrl);
		}
	}

	protected static void defaultPlates(Platform platform1) {
		// setDefaultMixerName(platform1);
		platform1.setShowDecisionLights(true);
		platform1.setShowTimer(true);
		// collar
		platform1.setNbC_2_5(1);
		// small plates
		platform1.setNbS_0_5(1);
		platform1.setNbS_1(1);
		platform1.setNbS_1_5(1);
		platform1.setNbS_2(1);
		platform1.setNbS_2_5(1);
		platform1.setNbS_5(1);
		// large plates, regulation set-up
		platform1.setNbL_2_5(0);
		platform1.setNbL_5(0);
		platform1.setNbL_10(1);
		platform1.setNbL_15(1);
		platform1.setNbL_20(1);
		platform1.setNbL_25(3);
	}

	private static String getDefaultLanguage() {
		// default language as defined in properties file (not the JVM).
		// this will typically be en.
		return getLocale().getLanguage();
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
	protected static void setupTestData(EntityManager em, Competition competition, int liftersToLoad) {
		
		LocalDateTime w = LocalDateTime.now();
		LocalDateTime c = w.plusHours((long) 2.0);
		
		Platform platform1 = new Platform("Gym 1"); //$NON-NLS-1$
		defaultPlates(platform1);
		Platform platform2 = new Platform("Gym 2"); //$NON-NLS-1$
		defaultPlates(platform2);

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
	}

//	/**
//	 * @param platform1
//	 */
//	protected static void setDefaultMixerName(Platform platform1) {
//		String mixerName = null;
//		try {
//			mixerName = Speakers.getOutputNames()
//				.get(0);
//			platform1.setMixerName(mixerName);
//		} catch (Exception e) {
//			// leave mixerName null
//		}
//	}

	private static void insertSampleLifters(EntityManager em, int liftersToLoad, Group groupA,
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

		drawLots(em);

		assignStartNumbers(em, groupA);
		assignStartNumbers(em, groupB);
	}

	protected static void createGroup(EntityManager em, Group group, final String[] fnames, final String[] lnames,
			Random r,
			int cat1, int cat2, int liftersToLoad) {
		for (int i = 0; i < liftersToLoad; i++) {
			Athlete p = new Athlete();
			p.setCompetitionSession(group);
			p.setFirstName(fnames[r.nextInt(fnames.length)]);
			p.setLastName(lnames[r.nextInt(lnames.length)]);
			double nextDouble = r.nextDouble();
			if (nextDouble > 0.5F) {
				createAthlete(em, r, p, nextDouble, cat1);
			} else {
				createAthlete(em, r, p, nextDouble, cat2);
			}
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

		double sd = catLimit * (1 + (r.nextGaussian() / 10));
		p.setSnatch1Declaration(Long.toString(Math.round(sd)));
		p.setCleanJerk1Declaration(Long.toString(Math.round(sd * 1.20D)));
		nextDouble = r.nextDouble();
		String team;
		if (nextDouble < 0.333)
			team = "EAST";
		else if (nextDouble < 0.666)
			team = "WEST";
		else
			team = "NORTH";
		p.setTeam(team);
	}

}
