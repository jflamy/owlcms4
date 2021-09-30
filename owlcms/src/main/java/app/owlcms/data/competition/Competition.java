/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.competition;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.AthleteSorter.Ranking;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Participation;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.LocaleAttributeConverter;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.results.Resource;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/**
 * Class Competition.
 */
@Cacheable

//must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
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

    private String federationEMail = "";
    private String federationWebSite;

//    @Lob
//    private byte[] finalPackageTemplate;

    private String finalPackageTemplateFileName;

    /**
     * In a mixed group, call all female lifters then all male lifters
     */
    @Column(columnDefinition = "boolean default false")
    private boolean genderOrder;

    /**
     * All first lifts, then all second lifts, then all third lifts, etc. Can be combined with genderOrder as well.
     */
    @Column(columnDefinition = "boolean default false")
    private boolean roundRobinOrder;

    private boolean masters;

    /**
     * Add W75 and W80+ masters categories
     */
    @Column(columnDefinition = "boolean default false")
    private boolean mastersGenderEquality = false;

    @Column(columnDefinition = "integer default 10")
    private Integer mensTeamSize = 10;
    private String protocolFileName;

//    @Lob
//    private byte[] protocolTemplate;

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

    @Column(columnDefinition = "integer default 10")
    private Integer womensTeamSize = 10;

    @Transient
    private boolean rankingsInvalid = true;
    private String timeZoneId;

    synchronized public void computeReportingInfo(boolean full) {
        Thread t = new Thread(() -> {
            List<Athlete> athletes = AthleteRepository.findAllByGroupAndWeighIn(null, true);
            logger.warn("computeReportingInfo {}", LoggerUtils.stackTrace());
            if (athletes.isEmpty()) {
                // prevent outputting silliness.
                logger./**/warn("no athletes");
                reportingBeans.clear();
                return;
            }
            sortGroupResults(athletes);
            logger.warn("computeReportingInfo full = {}", full);
            if (full) {
                sortTeamResults(athletes);
            }
        });
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Competition other = (Competition) obj;
        return id != null && id.equals(other.getId());
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

    /**
     * Gets the result template file name.
     *
     * @return the result template file name
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public String getFinalPackageTemplateFileName() throws IOException {
        String absoluteRoot = "/templates/competitionBook";
        if (finalPackageTemplateFileName == null) {
            return doFindFinalPackageTemplateFileName(absoluteRoot);
        } else if (this.getClass().getResource(absoluteRoot + "/" + finalPackageTemplateFileName) != null) {
            return finalPackageTemplateFileName;
        } else {
            return doFindFinalPackageTemplateFileName(absoluteRoot);
        }
    }

    @Deprecated
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
        if (isRankingsInvalid() || athletes == null) {
            setRankingsInvalid(true);
            while (isRankingsInvalid()) { // could be made invalid again while we compute
                setRankingsInvalid(false);
                // recompute because an athlete has been saved (new weight requested, good/bad lift, etc.)
                computeReportingInfo(true);
                athletes = getAthletes(gender);
                if (athletes == null) {
                    String error = MessageFormat.format("team list not found for gender {0}", gender);
                    logger./**/warn(error);
                    athletes = Collections.emptyList();
                }
            }
            logger.warn("team rankings recomputed {} size {}", gender, athletes != null ? athletes.size() : null);
        } else {
            logger.warn("found team rankings {} size {}", gender, athletes != null ? athletes.size() : null);
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
        if (isRankingsInvalid() || athletes == null) {
            setRankingsInvalid(true);
            while (isRankingsInvalid()) { // could be made invalid again while we compute
                setRankingsInvalid(false);
                // recompute because an athlete has been saved (new weight requested, good/bad lift, etc.)
                computeReportingInfo(false);
                athletes = (List<Athlete>) reportingBeans.get(listName);
                if (athletes == null) {
                    String error = MessageFormat.format("list {0} not found", listName);
                    logger./**/warn(error);
                    athletes = Collections.emptyList();
                }
            }
            logger.debug("recomputed {} size {} from {}", listName, athletes != null ? athletes.size() : null);
        } else {
            logger.debug("found {} size {} from {}", listName, athletes != null ? athletes.size() : null);
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

    public Integer getMenPerTeamElseDefault() {
        return mensTeamSize != null ? mensTeamSize : 10;
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
    public String getProtocolFileName() {
        String absoluteRoot = "/templates/protocol";
        if (protocolFileName == null) {
            return doFindProtocolFileName(absoluteRoot);
        } else if (this.getClass().getResource(absoluteRoot + "/" + protocolFileName) != null) {
            return protocolFileName;
        } else {
            return protocolFileName;
        }
    }

    public HashMap<String, Object> getReportingBeans() {
        return reportingBeans;
    }

    public TimeZone getTimeZone() {
        if (timeZoneId == null) {
            return null;
        } else {
            return TimeZone.getTimeZone(timeZoneId);
        }
    }

    public Integer getWomenPerTeamElseDefault() {
        return womensTeamSize != null ? womensTeamSize : 10;
    }

    public Integer getWomensTeamSize() {
        return womensTeamSize;
    }

    @Override
    public int hashCode() {
        return 31;
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

    synchronized public boolean isRankingsInvalid() {
        return rankingsInvalid;
    }

    public boolean isRoundRobinOrder() {
        return roundRobinOrder;
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

    synchronized public void setRankingsInvalid(boolean invalid) {
        this.rankingsInvalid = invalid;
    }

    public void setRoundRobinOrder(boolean roundRobinOrder) {
        this.roundRobinOrder = roundRobinOrder;
    }

    public void setTimeZone(TimeZone timeZone) {
        if (timeZone == null) {
            this.timeZoneId = null;
            return;
        } else {
            this.timeZoneId = timeZone.getID();
        }
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
                + ", finalPackageTemplateFileName=" + finalPackageTemplateFileName
                + ", ageGroupsFileName=" + ageGroupsFileName + ", enforce20kgRule="
                + enforce20kgRule + ", masters=" + masters + ", mensTeamSize=" + mensTeamSize + ", womensTeamSize="
                + womensTeamSize + ", customScore=" + customScore + ", mastersGenderEquality=" + mastersGenderEquality
                + ", useBirthYear=" + useBirthYear + ", useCategorySinclair=" + useCategorySinclair
                + ", useOldBodyWeightTieBreak=" + useOldBodyWeightTieBreak + ", useRegistrationCategory="
                + useRegistrationCategory + ", reportingBeans=" + reportingBeans + "]";
    }

    private String doFindFinalPackageTemplateFileName(String absoluteRoot) {
        List<Resource> resourceList = new ResourceWalker().getResourceList(absoluteRoot,
                ResourceWalker::relativeName, null, OwlcmsSession.getLocale());
        for (Resource r : resourceList) {
            logger.trace("checking {}", r.getFilePath());
            if (this.isMasters() && r.getFileName().startsWith("Masters")) {
                return r.getFileName();
            } else if (r.getFileName().startsWith("Total")) {
                return r.getFileName();
            }
        }
        throw new RuntimeException("final package templates not found under " + absoluteRoot);
    }

    private String doFindProtocolFileName(String absoluteRoot) {
        List<Resource> resourceList = new ResourceWalker().getResourceList(absoluteRoot,
                ResourceWalker::relativeName, null, OwlcmsSession.getLocale());
        for (Resource r : resourceList) {
            logger.trace("checking {}", r.getFilePath());
            if (this.isMasters() && r.getFileName().startsWith("Masters")) {
                return r.getFileName();
            } else if (r.getFileName().startsWith("Protocol")) {
                return r.getFileName();
            }
        }
        throw new RuntimeException("result templates not found under " + absoluteRoot);
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

        reportingBeans.put("competition", Competition.getCurrent());
        reportingBeans.put("groups", GroupRepository.findAll().stream().sorted((a, b) -> {
            int compare = ObjectUtils.compare(a.getWeighInTime(), b.getWeighInTime(), true);
            if (compare != 0) {
                return compare;
            }
            return compare = ObjectUtils.compare(a.getPlatform(), b.getPlatform(), true);
        }).collect(Collectors.toList()));
        reportingBeans.put("t", Translator.getMap());

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

        if (Competition.getCurrent().isCustomScore()) {
            sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.CUSTOM);
            AthleteSorter.assignCategoryRanks(sortedAthletes, Ranking.CUSTOM);
            sortedMen = new ArrayList<>(sortedAthletes.size());
            sortedWomen = new ArrayList<>(sortedAthletes.size());
            splitByGender(sortedAthletes, sortedMen, sortedWomen);
            reportingBeans.put("mCus", sortedMen);
            reportingBeans.put("wCus", sortedWomen);
            logger.debug("mCus {}", sortedMen);
            logger.debug("wCus {}", sortedWomen);
        }

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.BW_SINCLAIR);
        AthleteSorter.assignOverallRanksAndPoints(sortedAthletes, Ranking.BW_SINCLAIR);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mSinclair", sortedMen);
        reportingBeans.put("wSinclair", sortedWomen);
        logger.debug("mSinclair {}", sortedMen);
        logger.debug("wSinclair {}", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.CAT_SINCLAIR);
        AthleteSorter.assignOverallRanksAndPoints(sortedAthletes, Ranking.CAT_SINCLAIR);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mCatSinclair", sortedMen);
        reportingBeans.put("wCatSinclair", sortedWomen);
        logger.debug("mCatSinclair {}", sortedMen);
        logger.debug("wCatSinclair {}", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.SMM);
        AthleteSorter.assignOverallRanksAndPoints(sortedAthletes, Ranking.SMM);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mSmm", sortedMen);
        reportingBeans.put("wSmm", sortedWomen);
        logger.debug("mSmm {}", sortedMen);
        logger.debug("wSmm {}", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.ROBI);
        reportingBeans.put("athletes", sortedAthletes);
        AthleteSorter.assignOverallRanksAndPoints(sortedAthletes, Ranking.ROBI);
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
                logger.warn("adding {} to {}", a.getShortName(), group.getName());
                list.add(a);
            }
        }
        logger.debug("updated reporting data");
    }

    private void sortTeamResults(List<Athlete> athletes) {

        if (competition.isMasters()) {
            // extract club lists
            TreeSet<String> teams = new TreeSet<>();
            for (Athlete curAthlete : athletes) {
                if (curAthlete.getTeam() != null) {
                    teams.add(curAthlete.getTeam());
                }
            }

            reportingBeans.put("clubs", teams);
            List<Athlete> sortedAthletes = null;
            List<Athlete> sortedMen = null;
            List<Athlete> sortedWomen = null;

            // the age groups don't matter

            // team-oriented rankings. These rankings put all the athletes from the same team
            // together, sorted according to their points, so the top n can be kept if needed.
            // substitutes are not included -- they should be marked as !isEligibleForTeamRanking

            sortedAthletes = AthleteSorter.teamPointsOrderCopy(athletes, Ranking.SNATCH_CJ_TOTAL);
            sortedMen = new ArrayList<>(sortedAthletes.size());
            sortedWomen = new ArrayList<>(sortedAthletes.size());
            splitByGender(sortedAthletes, sortedMen, sortedWomen);

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

        // one team point tally per age group

        // team-oriented rankings. These rankings put all the athletes from the same team
        // together, sorted according to their points, so the top n can be kept if needed.
        // substitutes are not included -- they should be marked as !isEligibleForTeamRanking

        List<AgeGroup> nonMastersAgeGroups = AgeGroupRepository.findActive();
        Set<String> nonMastersAgePrefixes = nonMastersAgeGroups.stream()
                .filter(g -> !(g.getAgeDivision() == AgeDivision.MASTERS))
                .map(ag -> ag.getCode())
                .collect(Collectors.toSet());

        logger.warn("nonMastersAgePrefixes {}", nonMastersAgePrefixes);
        for (String ageGroupPrefix : nonMastersAgePrefixes) {
            logger.warn("");
            logger.warn("{}", ageGroupPrefix);

            List<Participation> wAgeGroupParticipations = AgeGroupRepository
                    .allParticipationsForAgeGroup(ageGroupPrefix, Gender.F);
            List<Participation> mAgeGroupParticipations = AgeGroupRepository
                    .allParticipationsForAgeGroup(ageGroupPrefix, Gender.M);
            List<Participation> mwAgeGroupParticipations = new ArrayList<>();
            mwAgeGroupParticipations.addAll(wAgeGroupParticipations);
            mwAgeGroupParticipations.addAll(mAgeGroupParticipations);
            for (Participation p : mwAgeGroupParticipations) {
                logger.warn("participation {} {}", p.getCategory(), p.long_dump());
            }

            List<Athlete> sortedMen;
            List<Athlete> sortedWomen;
            List<Athlete> sortedAthletes;

            sortedAthletes = AthleteSorter.teamPointsOrderParticipations(mwAgeGroupParticipations,
                    Ranking.SNATCH_CJ_TOTAL);
            sortedMen = AthleteSorter.teamPointsOrderParticipations(mAgeGroupParticipations, Ranking.SNATCH_CJ_TOTAL);
            sortedWomen = AthleteSorter.teamPointsOrderParticipations(wAgeGroupParticipations, Ranking.SNATCH_CJ_TOTAL);
            reportingBeans.put("mCombined" + ageGroupPrefix, sortedMen);
            reportingBeans.put("wCombined" + ageGroupPrefix, sortedWomen);
            reportingBeans.put("mwCombined" + ageGroupPrefix, sortedAthletes);

            sortedAthletes = AthleteSorter.teamPointsOrderParticipations(mwAgeGroupParticipations, Ranking.TOTAL);
            sortedMen = AthleteSorter.teamPointsOrderParticipations(mAgeGroupParticipations, Ranking.TOTAL);
            sortedWomen = AthleteSorter.teamPointsOrderParticipations(wAgeGroupParticipations, Ranking.TOTAL);
            reportingBeans.put("mTeam" + ageGroupPrefix, sortedMen);
            logger.warn("{} {}", "mTeam" + ageGroupPrefix, sortedMen);
            reportingBeans.put("wTeam" + ageGroupPrefix, sortedWomen);
            logger.warn("{} {}", "wTeam" + ageGroupPrefix, sortedWomen);
            reportingBeans.put("mwTeam" + ageGroupPrefix, sortedAthletes);

            if (Competition.getCurrent().isCustomScore()) {
                sortedAthletes = AthleteSorter.teamPointsOrderParticipations(mwAgeGroupParticipations, Ranking.CUSTOM);
                sortedMen = AthleteSorter.teamPointsOrderParticipations(mAgeGroupParticipations, Ranking.CUSTOM);
                sortedWomen = AthleteSorter.teamPointsOrderParticipations(wAgeGroupParticipations, Ranking.CUSTOM);
                reportingBeans.put("mCustom" + ageGroupPrefix, sortedMen);
                reportingBeans.put("wCustom" + ageGroupPrefix, sortedWomen);
                reportingBeans.put("mwCustom" + ageGroupPrefix, sortedAthletes);
            }

            // only needed once
            reportingBeans.put("nbMen", mAgeGroupParticipations.size());
            reportingBeans.put("nbWomen", wAgeGroupParticipations.size());
            reportingBeans.put("nbAthletes", mwAgeGroupParticipations.size());
            // extract club lists
            TreeSet<String> teams = new TreeSet<>();
            for (Athlete curAthlete : athletes) {
                if (curAthlete.getTeam() != null) {
                    teams.add(curAthlete.getTeam());
                }
            }

            // intentional : show all clubs on both genders-- non participating will see 0 athletes listed
            reportingBeans.put("clubs", teams);
            reportingBeans.put("nbClubs", teams.size());
            if (mAgeGroupParticipations.size() > 0) {
                reportingBeans.put("mClubs", teams);
            } else {
                reportingBeans.put("mClubs", new ArrayList<String>());
            }
            if (wAgeGroupParticipations.size() > 0) {
                reportingBeans.put("wClubs", teams);
            } else {
                reportingBeans.put("wClubs", new ArrayList<String>());
            }

        }
    }

}
