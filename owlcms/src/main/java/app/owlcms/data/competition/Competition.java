/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.competition;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.AthleteSorter.Ranking;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.LocaleAttributeConverter;
import app.owlcms.ui.results.Resource;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/**
 * Class Competition.
 */
@Cacheable
@Entity
public class Competition {

    public static final int SHORT_TEAM_LENGTH = 6;
    private static Competition competition;

    final static private Logger logger = (Logger) LoggerFactory.getLogger(Competition.class);

    /**
     * Gets the current.
     *
     * @return the current
     */
    public static Competition getCurrent() {
        if (competition == null) {
            competition = CompetitionRepository.findAll().get(0);
        }
        return competition;
    }

    public static void setCurrent(Competition c) {
        competition = c;
    }

    public static void splitByGender(List<Athlete> sortedAthletes, List<Athlete> sortedMen, List<Athlete> sortedWomen) {
        for (Athlete l : sortedAthletes) {
            Gender gender = l.getGender();
            if (Gender.M == gender) {
                sortedMen.add(l);
            } else if (Gender.F == gender) {
                sortedWomen.add(l);
            } else {
                throw new RuntimeException("gender is " + gender);
            }
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    private String ageGroupsFileName;
    private String competitionCity;
    private LocalDate competitionDate = null;
    private String competitionName;
    private String competitionOrganizer;
    private String competitionSite;

    /**
     * enable overriding total for kids categories with bonus points
     */
    @Column(columnDefinition = "boolean default false")
    private boolean customScore;

    /**
     * announcer sees decisions as they are made by referee.
     */
    @Column(columnDefinition = "boolean default true")
    private boolean announcerLiveDecisions;

    @Convert(converter = LocaleAttributeConverter.class)
    private Locale defaultLocale = null;

    private boolean enforce20kgRule;

    private String federation;
    private String federationAddress;

    private String federationEMail;
    private String federationWebSite;

    @Lob
    private byte[] finalPackageTemplate;

    private String finalPackageTemplateFileName;

    /**
     * In a mixed group, call all female lifters then all male lifters
     */
    @Column(columnDefinition = "boolean default false")
    private boolean genderOrder;
    private boolean masters;

    /**
     * Add W75 and W80+ masters categories
     */
    @Column(columnDefinition = "boolean default false")
    private boolean mastersGenderEquality = false;
    private Integer mensTeamSize;

    private String protocolFileName;

    @Lob
    private byte[] protocolTemplate;

    @Transient
    private HashMap<String, Object> reportingBeans = new HashMap<>();

    /**
     * Do not require month and day for birth.
     */
    @Column(columnDefinition = "boolean default true")
    private boolean useBirthYear = true;

    /**
     * Idiosyncratic rule in Québec federation computes best lifter using Sinclair at bodyweight boundary.
     */
    @Column(columnDefinition = "boolean default false")
    private boolean useCategorySinclair = false;

    /**
     * For traditional competitions that have lower body weight comes out first. Tie breaker for identical Sinclair.
     */
    @Column(columnDefinition = "boolean default false")
    private boolean useOldBodyWeightTieBreak = false;

    /**
     * Obsolete. We no longer infer categories.
     */
    @Column(columnDefinition = "boolean default false")
    private boolean useRegistrationCategory = false;

    private Integer womensTeamSize;

    synchronized public void computeGlobalRankings(boolean full) {
        List<Athlete> athletes = AthleteRepository.findAllByGroupAndWeighIn(null, true);
        if (athletes.isEmpty()) {
            // prevent outputting silliness.
            logger./**/warn("no athletes");
            reportingBeans.clear();
            return;
        }
        sortGroupResults(athletes);
        if (full) {
            sortTeamResults(athletes);
        }

    }

    public String getAgeGroupsFileName() {
        return ageGroupsFileName;
    }

    /**
     * Gets the competition city.
     *
     * @return the competition city
     */
    public String getCompetitionCity() {
        return competitionCity;
    }

    /**
     * Gets the competition date.
     *
     * @return the competition date
     */
    public LocalDate getCompetitionDate() {
        return competitionDate;
    }

    /**
     * Gets the competition name.
     *
     * @return the competition name
     */
    public String getCompetitionName() {
        return competitionName;
    }

    /**
     * Gets the competition organizer.
     *
     * @return the competition organizer
     */
    public String getCompetitionOrganizer() {
        return competitionOrganizer;
    }

    /**
     * Gets the competition site.
     *
     * @return the competition site
     */
    public String getCompetitionSite() {
        return competitionSite;
    }

    /**
     * Gets the default locale.
     *
     * @return the default locale
     */
    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    /**
     * Gets the federation.
     *
     * @return the federation
     */
    public String getFederation() {
        return federation;
    }

    /**
     * Gets the federation address.
     *
     * @return the federation address
     */
    public String getFederationAddress() {
        return federationAddress;
    }

    /**
     * Gets the federation E mail.
     *
     * @return the federation E mail
     */
    public String getFederationEMail() {
        return federationEMail;
    }

    /**
     * Gets the federation web site.
     *
     * @return the federation web site
     */
    public String getFederationWebSite() {
        return federationWebSite;
    }

    public byte[] getFinalPackageTemplate() {
        return finalPackageTemplate;
    }

    /**
     * Gets the result template file name.
     *
     * @return the result template file name
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public String getFinalPackageTemplateFileName() throws IOException {
        if (finalPackageTemplateFileName == null) {
            List<Resource> resourceList = new ResourceWalker().getResourceList("/templates/competitionBook",
                    ResourceWalker::relativeName, null);
            for (Resource r : resourceList) {
                if (this.isMasters() && r.getFileName().startsWith("Masters_")) {
                    return r.getFileName();
                } else if (r.getFileName().startsWith("Total_")) {
                    return r.getFileName();
                }
            }
            return null;
        } else {
            return finalPackageTemplateFileName;
        }
    }

    synchronized public List<Athlete> getGlobalCategoryRankingsForGroup(Group group) {
        if (group == null || group.getName() == null) {
            logger.debug("null group");
            return null;
        }
        return getListOrElseRecompute(group.getName());
    }

    synchronized public List<Athlete> getGlobalSinclairRanking(Gender gender) {
        return getListOrElseRecompute(gender == Gender.F ? "wSinclair" : "mSinclair");
    }

    synchronized public List<Athlete> getGlobalSnatchRanking(Gender gender) {
        return getListOrElseRecompute(gender == Gender.F ? "wSn" : "mSn");
    }

    public Collection<Athlete> getGlobalTeamsRanking(Gender gender) {
        List<Athlete> athletes = getAthletes(gender);
        if (athletes == null) {
            // not cached yet (we are likely the first on a reset/restart).
            computeGlobalRankings(true);
            athletes = getAthletes(gender);
            if (athletes == null) {
                String error = MessageFormat.format("team list not found for gender {0}", gender);
                logger./**/warn(error);
                athletes = Collections.emptyList();
            }
            logger.debug("team rankings recomputed {} size {}", gender, athletes != null ? athletes.size() : null);
        } else {
            logger.debug("found team rankings {} size {}", gender, athletes != null ? athletes.size() : null);
        }
        return athletes;
    }

    synchronized public List<Athlete> getGlobalTotalRanking(Gender gender) {
        return getListOrElseRecompute(gender == Gender.F ? "wTot" : "mTot");
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the invited if born before.
     *
     * @return the invited if born before
     */
    public Integer getInvitedIfBornBefore() {
        return 0;
    }

    @SuppressWarnings("unchecked")
    synchronized public List<Athlete> getListOrElseRecompute(String listName) {
        List<Athlete> athletes = (List<Athlete>) reportingBeans.get(listName);
        if (athletes == null) {
            // not cached yet (we are likely the first on a reset/restart).
            computeGlobalRankings(false);
            athletes = (List<Athlete>) reportingBeans.get(listName);
            if (athletes == null) {
                String error = MessageFormat.format("list {0} not found", listName);
                logger./**/warn(error);
                athletes = Collections.emptyList();
            }
            logger.debug("recomputed {} size {}", listName, athletes != null ? athletes.size() : null);
        } else {
            logger.debug("found {} size {}", listName, athletes != null ? athletes.size() : null);
        }
        return athletes;
    }

    /**
     * Gets the locale.
     *
     * @return the locale
     */
    public Locale getLocale() {
        return getDefaultLocale();
    }

    /**
     * Gets the masters.
     *
     * @return the masters
     */
    public boolean getMasters() {
        return isMasters();
    }

    public Integer getMensTeamSize() {
        return mensTeamSize;
    }

    /**
     * Gets the protocol file name.
     *
     * @return the protocol file name
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public String getProtocolFileName() throws IOException {
        if (protocolFileName == null) {
            List<Resource> resourceList = new ResourceWalker().getResourceList("/templates/protocol",
                    ResourceWalker::relativeName, null);
            for (Resource r : resourceList) {
                if (this.isMasters() && r.getFileName().startsWith("Masters_")) {
                    return r.getFileName();
                } else if (r.getFileName().startsWith("Protocol_")) {
                    return r.getFileName();
                }
            }
            return null;
        } else {
            return protocolFileName;
        }
    }

    public byte[] getProtocolTemplate() {
        return protocolTemplate;
    }

    public HashMap<String, Object> getReportingBeans() {
        return reportingBeans;
    }

    public Integer getWomensTeamSize() {
        return womensTeamSize;
    }

    public boolean isAnnouncerLiveDecisions() {
        return announcerLiveDecisions;
    }

    public boolean isCustomScore() {
        return customScore;
    }

    /**
     * Checks if is enforce 20 kg rule.
     *
     * @return true, if is enforce 20 kg rule
     */
    public boolean isEnforce20kgRule() {
        return enforce20kgRule;
    }

    public boolean isGenderOrder() {
        if (StartupUtils.getBooleanParam("genderOrder")) {
            setGenderOrder(true);
        }
        return genderOrder;
    }

    /**
     * Checks if is masters.
     *
     * @return true, if is masters
     */
    public boolean isMasters() {
        return masters;
    }

    public boolean isMastersGenderEquality() {
        return mastersGenderEquality;
    }

    /**
     * Checks if is use birth year.
     *
     * @return the useBirthYear
     */
    public boolean isUseBirthYear() {
        return useBirthYear;
    }

    /**
     * Checks if is use category sinclair.
     *
     * @return true, if is use category sinclair
     */
    public boolean isUseCategorySinclair() {
        return useCategorySinclair;
    }

    /**
     * Checks if is use old body weight tie break.
     *
     * @return true, if is use old body weight tie break
     */
    public boolean isUseOldBodyWeightTieBreak() {
        return useOldBodyWeightTieBreak;
    }

    /**
     * Checks if is use registration category.
     *
     * @return true, if is use registration category
     */
    @Deprecated
    public boolean isUseRegistrationCategory() {
        return false;
    }

    public void setAgeGroupsFileName(String localizedName) {
        this.ageGroupsFileName = localizedName;
    }

    public void setAnnouncerLiveDecisions(boolean announcerLiveDecisions) {
        this.announcerLiveDecisions = announcerLiveDecisions;
    }

    /**
     * Sets the competition city.
     *
     * @param competitionCity the new competition city
     */
    public void setCompetitionCity(String competitionCity) {
        this.competitionCity = competitionCity;
    }

    /**
     * Sets the competition date.
     *
     * @param localDate the new competition date
     */
    public void setCompetitionDate(LocalDate localDate) {
        this.competitionDate = localDate;
    }

    /**
     * Sets the competition name.
     *
     * @param competitionName the new competition name
     */
    public void setCompetitionName(String competitionName) {
        this.competitionName = competitionName;
    }

    /**
     * Sets the competition organizer.
     *
     * @param competitionOrganizer the new competition organizer
     */
    public void setCompetitionOrganizer(String competitionOrganizer) {
        this.competitionOrganizer = competitionOrganizer;
    }

    /**
     * Sets the competition site.
     *
     * @param competitionSite the new competition site
     */
    public void setCompetitionSite(String competitionSite) {
        this.competitionSite = competitionSite;
    }

    public void setCustomScore(boolean customScore) {
        this.customScore = customScore;
    }

    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public void setEnforce20kgRule(boolean enforce20kgRule) {
        this.enforce20kgRule = enforce20kgRule;
    }

    /**
     * Sets the federation.
     *
     * @param federation the new federation
     */
    public void setFederation(String federation) {
        this.federation = federation;
    }

    /**
     * Sets the federation address.
     *
     * @param federationAddress the new federation address
     */
    public void setFederationAddress(String federationAddress) {
        this.federationAddress = federationAddress;
    }

    /**
     * Sets the federation E mail.
     *
     * @param federationEMail the new federation E mail
     */
    public void setFederationEMail(String federationEMail) {
        this.federationEMail = federationEMail;
    }

    /**
     * Sets the federation web site.
     *
     * @param federationWebSite the new federation web site
     */
    public void setFederationWebSite(String federationWebSite) {
        this.federationWebSite = federationWebSite;
    }

    public void setFinalPackageTemplate(byte[] finalPackageTemplate) {
        this.finalPackageTemplate = finalPackageTemplate;
    }

    /**
     * Sets the result template file name.
     *
     * @param finalPackageTemplateFileName the new result template file name
     */
    public void setFinalPackageTemplateFileName(String finalPackageTemplateFileName) {
        this.finalPackageTemplateFileName = finalPackageTemplateFileName;
    }

    public void setGenderOrder(boolean genderOrder) {
        this.genderOrder = genderOrder;
    }

    /**
     * Sets the invited if born before.
     *
     * @param invitedIfBornBefore the new invited if born before
     */
    public void setInvitedIfBornBefore(Integer invitedIfBornBefore) {
    }

    public void setMasters(boolean masters) {
        this.masters = masters;
    }

    public void setMastersGenderEquality(boolean mastersGenderEquality) {
        this.mastersGenderEquality = mastersGenderEquality;
    }

    public void setMensTeamSize(Integer mensTeamSize) {
        this.mensTeamSize = mensTeamSize;
    }

    /**
     * Sets the protocol file name.
     *
     * @param protocolFileName the new protocol file name
     */
    public void setProtocolFileName(String protocolFileName) {
        this.protocolFileName = protocolFileName;
    }

    public void setProtocolTemplate(byte[] protocolTemplate) {
        this.protocolTemplate = protocolTemplate;
    }

    /**
     * Sets the use birth year.
     *
     * @param b the new use birth year
     */
    public void setUseBirthYear(boolean b) {
        this.useBirthYear = b;
    }

    /**
     * Sets the use registration category. No longer used. We always use the category. Only kept for backward
     * compatibility.
     *
     * @param useRegistrationCategory the useRegistrationCategory to set
     */
    @Deprecated
    public void setUseRegistrationCategory(boolean useRegistrationCategory) {
        this.useRegistrationCategory = false;
    }

    public void setWomensTeamSize(Integer womensTeamSize) {
        this.womensTeamSize = womensTeamSize;
    }

    @Override
    public String toString() {
        return "Competition [id=" + id + ", competitionName=" + competitionName + ", competitionDate=" + competitionDate
                + ", competitionOrganizer=" + competitionOrganizer + ", competitionSite=" + competitionSite
                + ", competitionCity=" + competitionCity + ", federation=" + federation + ", federationAddress="
                + federationAddress + ", federationEMail=" + federationEMail + ", federationWebSite="
                + federationWebSite + ", defaultLocale=" + defaultLocale + ", protocolFileName=" + protocolFileName
                + ", protocolTemplate=" + Arrays.toString(protocolTemplate) + ", finalPackageTemplateFileName="
                + finalPackageTemplateFileName + ", ageGroupsFileName=" + ageGroupsFileName + ", finalPackageTemplate="
                + Arrays.toString(finalPackageTemplate) + ", enforce20kgRule=" + enforce20kgRule + ", masters="
                + masters + ", mensTeamSize=" + mensTeamSize + ", womensTeamSize=" + womensTeamSize + ", customScore="
                + customScore + ", mastersGenderEquality=" + mastersGenderEquality + ", useBirthYear=" + useBirthYear
                + ", useCategorySinclair=" + useCategorySinclair + ", useOldBodyWeightTieBreak="
                + useOldBodyWeightTieBreak + ", useRegistrationCategory=" + useRegistrationCategory
                + ", reportingBeans=" + reportingBeans + "]";
    }

    @SuppressWarnings("unchecked")
    private List<Athlete> getAthletes(Gender gender) {
        List<Athlete> athletes = null;
        List<Athlete> mTeam = (List<Athlete>) reportingBeans.get("mTeam");
        List<Athlete> wTeam = (List<Athlete>) reportingBeans.get("wTeam");
        switch (gender) {
        case M:
            athletes = mTeam;
            break;
        case F:
            athletes = wTeam;
            break;
        case MIXED:
            athletes = new ArrayList<>();
            if (mTeam != null) {
                athletes.addAll(mTeam);
            }
            if (wTeam != null) {
                athletes.addAll(wTeam);
            }
            break;
        }
        return athletes;
    }

    @SuppressWarnings("unchecked")
    private void sortGroupResults(List<Athlete> athletes) {
        List<Athlete> sortedAthletes;
        List<Athlete> sortedMen = null;
        List<Athlete> sortedWomen = null;

        reportingBeans.clear();

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.SNATCH);
        AthleteSorter.assignCategoryRanks(sortedAthletes, Ranking.SNATCH);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mSn", sortedMen);
        reportingBeans.put("wSn", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.CLEANJERK);
        AthleteSorter.assignCategoryRanks(sortedAthletes, Ranking.CLEANJERK);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mCJ", sortedMen);
        reportingBeans.put("wCJ", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.TOTAL);
        AthleteSorter.assignCategoryRanks(sortedAthletes, Ranking.TOTAL);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mTot", sortedMen);
        reportingBeans.put("wTot", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.BW_SINCLAIR);
        AthleteSorter.assignSinclairRanksAndPoints(sortedAthletes, Ranking.BW_SINCLAIR);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mSinclair", sortedMen);
        reportingBeans.put("wSinclair", sortedWomen);
        logger.debug("mSinclair {}", sortedMen);
        logger.debug("wSinclair {}", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.CAT_SINCLAIR);
        AthleteSorter.assignSinclairRanksAndPoints(sortedAthletes, Ranking.CAT_SINCLAIR);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mCatSinclair", sortedMen);
        reportingBeans.put("wCatSinclair", sortedWomen);
        logger.debug("mCatSinclair {}", sortedMen);
        logger.debug("wCatSinclair {}", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.SMM);
        AthleteSorter.assignSinclairRanksAndPoints(sortedAthletes, Ranking.SMM);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mSmm", sortedMen);
        reportingBeans.put("wSmm", sortedWomen);
        logger.debug("mSmm {}", sortedMen);
        logger.debug("wSmm {}", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.ROBI);
        reportingBeans.put("athletes", sortedAthletes);
        AthleteSorter.assignSinclairRanksAndPoints(sortedAthletes, Ranking.ROBI);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mRobi", sortedMen);
        reportingBeans.put("wRobi", sortedWomen);

        // create one list per competition group
        for (Group g : GroupRepository.findAll()) {
            String name = g.getName();
            if (name != null) {
                reportingBeans.remove(name);
                reportingBeans.put(name, new ArrayList<Athlete>());
            }
        }

        AthleteSorter.displayOrder(athletes);
        for (Athlete a : athletes) {
            Group group = a.getGroup();
            if (group != null && group.getName() != null) {
                List<Athlete> list = (List<Athlete>) reportingBeans.get(group.getName());
                list.add(a);
            }
        }
        logger.debug("updated reports");
    }

    private void sortTeamResults(List<Athlete> athletes) {
        List<Athlete> sortedAthletes;
        List<Athlete> sortedMen = null;
        List<Athlete> sortedWomen = null;

        // extract club lists
        TreeSet<String> teams = new TreeSet<>();
        for (Athlete curAthlete : athletes) {
            if (curAthlete.getTeam() != null) {
                teams.add(curAthlete.getTeam());
            }
        }
        reportingBeans.put("clubs", teams);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.CUSTOM);
        AthleteSorter.assignCategoryRanks(sortedAthletes, Ranking.CUSTOM);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mCus", sortedMen);
        reportingBeans.put("wCus", sortedWomen);

        // only needed once
        reportingBeans.put("nbMen", sortedMen.size());
        reportingBeans.put("nbWomen", sortedWomen.size());
        reportingBeans.put("nbAthletes", sortedAthletes.size());
        reportingBeans.put("nbClubs", teams.size());
        if (sortedMen.size() > 0) {
            reportingBeans.put("mClubs", teams);
        } else {
            reportingBeans.put("mClubs", new ArrayList<String>());
        }
        if (sortedWomen.size() > 0) {
            reportingBeans.put("wClubs", teams);
        } else {
            reportingBeans.put("wClubs", new ArrayList<String>());
        }

        // team-oriented rankings. These rankings put all the athletes from the same team
        // together, sorted according to their points, so the top n can be kept if needed.
        // substitutes are not included -- they should be marked as !isEligibleForTeamRanking
        sortedAthletes = AthleteSorter.teamPointsOrderCopy(athletes, Ranking.CUSTOM);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mCustom", sortedMen);
        reportingBeans.put("wCustom", sortedWomen);

        sortedAthletes = AthleteSorter.teamPointsOrderCopy(athletes, Ranking.COMBINED);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mCombined", sortedMen);
        reportingBeans.put("wCombined", sortedWomen);
        reportingBeans.put("mwCombined", sortedAthletes);

        AthleteSorter.teamPointsOrder(sortedAthletes, Ranking.TOTAL);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mTeam", sortedMen);
        reportingBeans.put("wTeam", sortedWomen);
        reportingBeans.put("mwTeam", sortedAthletes);
    }

}
