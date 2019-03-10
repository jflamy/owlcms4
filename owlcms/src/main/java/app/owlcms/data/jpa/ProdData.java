/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.data.jpa;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Locale;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;

import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.platform.Platform;
import app.owlcms.i18n.Messages;
import ch.qos.logback.classic.Logger;

/**
 * Production data.
 */
public class ProdData {

	private static Locale getLocale() {
		return Locale.ENGLISH;
	}

	private static Logger logger = (Logger) LoggerFactory.getLogger(ProdData.class);

	/**
	 * Insert initial data if the database is empty.
	 *
	 * @param nbAthletes 	how many athletes
	 * @param testMode      true if creating dummy data
	 */
	public static void insertInitialData(int nbAthletes, boolean testMode) {
		JPAService.runInTransaction(em -> {
			setupEmptyCompetition(em);
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
	 */
	protected static void setupEmptyCompetition(EntityManager em) {
		Competition competition = createDefaultCompetition(); 
		em.persist(competition);
		Platform platform1 = new Platform("A"); //$NON-NLS-1$
		CategoryRepository.insertStandardCategories(em);
		defaultPlates(platform1);
		setupCompetitionDocuments(competition, platform1);

		em.persist(new Group("M1", null, null)); //$NON-NLS-1$
		em.persist(new Group("M2", null, null)); //$NON-NLS-1$
		em.persist(new Group("M3", null, null)); //$NON-NLS-1$
		em.persist(new Group("M4", null, null)); //$NON-NLS-1$
		em.persist(new Group("F1", null, null)); //$NON-NLS-1$
		em.persist(new Group("F2", null, null)); //$NON-NLS-1$
		em.persist(new Group("F3", null, null)); //$NON-NLS-1$
		em.persist(new Group("F4", null, null)); //$NON-NLS-1$
		
		em.persist(platform1);

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

}
