/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;

public class RCompetition {

    static Map<String, Category> activeCategories = new HashMap<>();

    public static Map<String, Category> getActiveCategories() {
        return activeCategories;
    }

    public static void resetActiveCategories() {
        activeCategories.clear();
        CategoryRepository.findActive().forEach(c -> {
            activeCategories.put(c.getName(), c);
        });
    }

    Competition c = new Competition();

    public Competition getCompetition() {
        return c;
    }

    /**
     * @param competitionCity
     * @see app.owlcms.data.competition.Competition#setCompetitionCity(java.lang.String)
     */
    public void setCompetitionCity(String competitionCity) {
        c.setCompetitionCity(competitionCity);
    }

    public void setCompetitionDate(Date date) {
        LocalDate cd = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        c.setCompetitionDate(cd);
    }

    /**
     * @param competitionName
     * @see app.owlcms.data.competition.Competition#setCompetitionName(java.lang.String)
     */
    public void setCompetitionName(String competitionName) {
        c.setCompetitionName(competitionName);
    }

    /**
     * @param competitionOrganizer
     * @see app.owlcms.data.competition.Competition#setCompetitionOrganizer(java.lang.String)
     */
    public void setCompetitionOrganizer(String competitionOrganizer) {
        c.setCompetitionOrganizer(competitionOrganizer);
    }

    /**
     * @param competitionSite
     * @see app.owlcms.data.competition.Competition#setCompetitionSite(java.lang.String)
     */
    public void setCompetitionSite(String competitionSite) {
        c.setCompetitionSite(competitionSite);
    }

    /**
     * @param federation
     * @see app.owlcms.data.competition.Competition#setFederation(java.lang.String)
     */
    public void setFederation(String federation) {
        c.setFederation(federation);
    }

    /**
     * @param federationAddress
     * @see app.owlcms.data.competition.Competition#setFederationAddress(java.lang.String)
     */
    public void setFederationAddress(String federationAddress) {
        c.setFederationAddress(federationAddress);
    }

    /**
     * @param federationEMail
     * @see app.owlcms.data.competition.Competition#setFederationEMail(java.lang.String)
     */
    public void setFederationEMail(String federationEMail) {
        c.setFederationEMail(federationEMail);
    }

    /**
     * @param federationWebSite
     * @see app.owlcms.data.competition.Competition#setFederationWebSite(java.lang.String)
     */
    public void setFederationWebSite(String federationWebSite) {
        c.setFederationWebSite(federationWebSite);
    }

}
