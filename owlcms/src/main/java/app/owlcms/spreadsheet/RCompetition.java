/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;

import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.DateTimeUtils;
import ch.qos.logback.classic.Logger;

public class RCompetition {

	static Map<String, Category> activeCategories = new HashMap<>();
	static Map<String, Group> activeGroups = new HashMap<>();
	static Map<Long, Set<Category>> athleteToEligibles = new HashMap<>();
	static Map<Long, Set<Category>> athleteToTeams = new HashMap<>();

	public static Map<String, Category> getActiveCategories() {
		return activeCategories;
	}

	public static Map<String, Group> getActiveGroups() {
		return activeGroups;
	}

	public static Map<Long, Set<Category>> getAthleteToEligibles() {
		return athleteToEligibles;
	}

	public static Map<Long, Set<Category>> getAthleteToTeams() {
		return athleteToTeams;
	}

	public static void resetActiveCategories() {
		activeCategories.clear();
		CategoryRepository.findActive().forEach(c -> {
			activeCategories.put(c.getName(), c);
		});
	}

	public static void resetActiveGroups() {
		activeGroups.clear();
		GroupRepository.findAll().forEach(g -> {
			activeGroups.put(g.getName(), g);
		});
	}

	public static void resetAthleteToEligibles() {
		athleteToEligibles = new HashMap<>();
	}

	public static void resetAthleteToTeams() {
		athleteToTeams = new HashMap<>();
	}

	Competition c = new Competition();

	Logger logger = (Logger) LoggerFactory.getLogger(RCompetition.class);

	public Competition getCompetition() {
		return c;
	}

	/**
	 * @param competitionCity
	 * @see app.owlcms.data.competition.Competition#setCompetitionCity(java.lang.String)
	 */
	public void setCompetitionCity(String competitionCity) {
		if (competitionCity == null || competitionCity.isBlank()) {
			return;
		}
		c.setCompetitionCity(competitionCity);
	}

	public void setCompetitionDate(String date) throws Exception {
		if (date == null || date.isBlank()) {
			return;
		}
		c.setCompetitionDate(DateTimeUtils.parseExcelDate(date, OwlcmsSession.getLocale()));
	}

	/**
	 * @param competitionName
	 * @see app.owlcms.data.competition.Competition#setCompetitionName(java.lang.String)
	 */
	public void setCompetitionName(String competitionName) {
		if (competitionName == null || competitionName.isBlank()) {
			return;
		}
		c.setCompetitionName(competitionName);
	}

	/**
	 * @param competitionOrganizer
	 * @see app.owlcms.data.competition.Competition#setCompetitionOrganizer(java.lang.String)
	 */
	public void setCompetitionOrganizer(String competitionOrganizer) {
		if (competitionOrganizer == null || competitionOrganizer.isBlank()) {
			return;
		}
		c.setCompetitionOrganizer(competitionOrganizer);
	}

	/**
	 * @param competitionSite
	 * @see app.owlcms.data.competition.Competition#setCompetitionSite(java.lang.String)
	 */
	public void setCompetitionSite(String competitionSite) {
		if (competitionSite == null || competitionSite.isBlank()) {
			return;
		}
		c.setCompetitionSite(competitionSite);
	}

	/**
	 * @param federation
	 * @see app.owlcms.data.competition.Competition#setFederation(java.lang.String)
	 */
	public void setFederation(String federation) {
		if (federation == null || federation.isBlank()) {
			return;
		}
		c.setFederation(federation);
	}

	/**
	 * @param federationAddress
	 * @see app.owlcms.data.competition.Competition#setFederationAddress(java.lang.String)
	 */
	public void setFederationAddress(String federationAddress) {
		if (federationAddress == null || federationAddress.isBlank()) {
			return;
		}
		c.setFederationAddress(federationAddress);
	}

	/**
	 * @param federationEMail
	 * @see app.owlcms.data.competition.Competition#setFederationEMail(java.lang.String)
	 */
	public void setFederationEMail(String federationEMail) {
		if (federationEMail == null || federationEMail.isBlank()) {
			return;
		}
		c.setFederationEMail(federationEMail);
	}

	/**
	 * @param federationWebSite
	 * @see app.owlcms.data.competition.Competition#setFederationWebSite(java.lang.String)
	 */
	public void setFederationWebSite(String federationWebSite) {
		if (federationWebSite == null || federationWebSite.isBlank()) {
			return;
		}
		c.setFederationWebSite(federationWebSite);
	}

}
