/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.jpa;

import java.time.LocalDate;
import java.util.Locale;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.platform.Platform;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

/**
 * Production data.
 */
public class ProdData {

    @SuppressWarnings("unused")
    private static Logger logger = (Logger) LoggerFactory.getLogger(ProdData.class);

    /**
     * Insert initial data if the database is empty.
     *
     * @param nbAthletes how many athletes
     */
    public static void insertInitialData(int nbAthletes) {
        JPAService.runInTransaction(em -> {
            Competition competition = createDefaultCompetition();
            CompetitionRepository.save(competition);

            AgeGroupRepository.insertAgeGroups(em, null);
            return null;
        });
        JPAService.runInTransaction(em -> {
            setupEmptyCompetition(em);
            return null;
        });
    }

    protected static Competition createDefaultCompetition() {
        Competition competition = new Competition();

        competition.setCompetitionName(Translator.translate("Competition", getLocale()) + " ?");
        competition.setCompetitionCity(Translator.translate("Competition.competitionCity", getLocale()) + " ?");
        competition.setCompetitionDate(LocalDate.now());
        competition
                .setCompetitionOrganizer(Translator.translate("Competition.competitionOrganizer", getLocale()) + " ?");
        competition.setCompetitionSite(Translator.translate("Competition.competitionSite", getLocale()) + " ?");

        String federationLabel = Translator.translate("Competition.federation", getLocale()) + " ?";
        String defaultFederationKey = "Competition.defaultFederation";
        String defaultFederation = Translator.translate(defaultFederationKey, getLocale());
        // if string is not translated, we get its key back.
        competition.setFederation(defaultFederation.equals(defaultFederationKey) ? federationLabel : defaultFederation);

        String federationAddressLabel = Translator.translate("Competition.federationAddress", getLocale()) + " ?";
        String defaultFederationAddressKey = "Competition.defaultFederationAddress";
        String defaultFederationAddress = Translator.translate(defaultFederationAddressKey, getLocale());
        // if string is not translated, we get its key back.
        competition.setFederationAddress(
                defaultFederationAddress.equals(defaultFederationAddressKey) ? federationAddressLabel
                        : defaultFederationAddress);

        String federationEMailLabel = Translator.translate("Competition.federationEMail", getLocale()) + " ?";
        String defaultFederationEMailKey = "Competition.defaultFederationEMail";
        String defaultFederationEMail = Translator.translate(defaultFederationEMailKey, getLocale());
        // if string is not translated, we get its key back.
        competition.setFederationEMail(defaultFederationEMail.equals(defaultFederationEMailKey) ? federationEMailLabel
                : defaultFederationEMail);

        String federationWebSiteLabel = Translator.translate("Competition.federationWebSite", getLocale()) + " ?";
        String defaultFederationWebSiteKey = "Competition.defaultFederationWebSite";
        String defaultFederationWebSite = Translator.translate(defaultFederationWebSiteKey, getLocale());
        // if string is not translated, we get its key back.
        competition.setFederationWebSite(
                defaultFederationWebSite.equals(defaultFederationWebSiteKey) ? federationWebSiteLabel
                        : defaultFederationWebSite);

        competition.setUseBirthYear(true);
        competition.setEnforce20kgRule(true);
        competition.setAnnouncerLiveDecisions(true);

        return competition;
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

    /**
     * Create an empty competition. Set-up the defaults for using the timekeeping and refereeing features.
     *
     * @param em
     */
    protected static void setupEmptyCompetition(EntityManager em) {
        Platform platform1 = new Platform("A");
        defaultPlates(platform1);

        em.persist(new Group("M1", null, null));
        em.persist(new Group("M2", null, null));
        em.persist(new Group("M3", null, null));
        em.persist(new Group("M4", null, null));
        em.persist(new Group("F1", null, null));
        em.persist(new Group("F2", null, null));
        em.persist(new Group("F3", null, null));
        em.persist(new Group("F4", null, null));

        em.persist(platform1);

    }

    private static Locale getLocale() {
        return Locale.ENGLISH;
    }

}
