/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.competition;

import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.AthleteSorter.Ranking;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.utils.DateTimeUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/**
 * Class Competition.
 */
@Cacheable

//must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
public class Competition {

    public static final int SHORT_TEAM_LENGTH = 6;
    private static Competition competition;

    @Transient
    final static private Logger logger = (Logger) LoggerFactory.getLogger(Competition.class);

    public static void debugRanks(String label, Athlete a) {
        logger./**/warn("{} {} {} {} {} {}", label, System.identityHashCode(a), a.getId(), a.getShortName(),
                a.getTotalRank(), a.getCategory(), a.getParticipations().size());
    }

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

    public static void splitByGender(List<Athlete> athletes, List<Athlete> sortedMen, List<Athlete> sortedWomen) {
        for (Athlete l : athletes) {
            Gender gender = l.getGender();
            if (Gender.M == gender) {
                sortedMen.add(l);
            } else if (Gender.F == gender) {
                sortedWomen.add(l);
            } else {
                // throw new RuntimeException("gender is " + gender);
            }
        }
    }

    public static void splitPByGender(List<PAthlete> athletes, List<Athlete> men, List<Athlete> women) {
        for (PAthlete l : athletes) {
            Gender gender = l.getGender();
            if (Gender.M == gender) {
                men.add(l);
            } else if (Gender.F == gender) {
                women.add(l);
            } else {
                // throw new RuntimeException("gender is " + gender);
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

    private boolean enforce20kgRule;
    private String federation;

    private String federationAddress;
    private String federationEMail = "";
    private String federationWebSite;

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

    @Transient
    private HashMap<String, Object> reportingBeans = new HashMap<>();

    /**
     * Do not require month and day for birth.
     */
    @Column(columnDefinition = "boolean default false")
    private boolean useBirthYear = false;

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
    @JsonIgnore
    private boolean rankingsInvalid = true;

    private String protocolTemplateFileName;
    private String cardsTemplateFileName;
    private String startListTemplateFileName;
    private String juryTemplateFileName;
    private String startingWeightsSheetTemplateFileName;
    private String finalPackageTemplateFileName;

    @Column(name = "refdelay", columnDefinition = "integer default 1500")
    private int refereeWakeUpDelay = 1500;

    public void setRefereeWakeUpDelay(int refereeWakeUpDelay) {
        this.refereeWakeUpDelay = refereeWakeUpDelay;
    }

    synchronized public HashMap<String, Object> computeReportingInfo() {
        List<PAthlete> athletes = AgeGroupRepository.allPAthletesForAgeGroupAgeDivision(null, null);
        doComputeReportingInfo(true, athletes, (String) null, null);
        return reportingBeans;
    }

    synchronized public HashMap<String, Object> computeReportingInfo(String ageGroupPrefix, AgeDivision ad) {
        List<PAthlete> athletes = AgeGroupRepository.allPAthletesForAgeGroupAgeDivision(ageGroupPrefix, ad);
        doComputeReportingInfo(true, athletes, ageGroupPrefix, ad);
        return reportingBeans;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        Competition other = (Competition) obj;
        return id != null && id.equals(other.getId());
    }

    public String getAgeGroupsFileName() {
        return ageGroupsFileName;
    }

    /**
     * @return the cardsTemplateFileName
     */
    public String getCardsTemplateFileName() {
        return cardsTemplateFileName;
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

    @Transient
    @JsonIgnore
    public Date getCompetitionDateAsDate() {
        return DateTimeUtils.dateFromLocalDate(competitionDate);
    }

//    @Transient
//    @JsonIgnore
    public String getLocalizedCompetitionDate() {
        try {
            String pattern = ((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, OwlcmsSession.getLocale())).toPattern();
            // if 2-digit year, force 4 digits.
            pattern = pattern.replaceFirst("\\byy\\b","yyyy");
            System.err.println("pattern="+pattern);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            String str = competitionDate.format(formatter);
            return str;
        } catch (Exception a) {
            a.printStackTrace();
        }
        return "error";
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

    @Transient
    @JsonIgnore
    public String getComputedCardsTemplateFileName() {
        if (cardsTemplateFileName == null) {
            return "CardTemplate.xls";
        }
        return cardsTemplateFileName;
    }

    /**
     * Gets the result template file name.
     *
     * @return the result template file name
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public String getComputedFinalPackageTemplateFileName() {
        if (finalPackageTemplateFileName == null) {
            return "Total.xls";
        } else {
            return finalPackageTemplateFileName;
        }
    }

    @Transient
    @JsonIgnore
    public String getComputedJuryTemplateFileName() {
        if (juryTemplateFileName == null) {
            return "JurySheetTemplate.xls";
        }
        return juryTemplateFileName;
    }

    /**
     * Gets the protocol file name.
     *
     * @return the protocol file name
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Transient
    @JsonIgnore
    public String getComputedProtocolTemplateFileName() {
        if (getProtocolTemplateFileName() == null) {
            return "Protocol.xls";
        } else {
            return getProtocolTemplateFileName();
        }
    }

//    synchronized public List<Athlete> getGlobalTotalRanking(Gender gender) {
//        return getListOrElseRecompute(gender == Gender.F ? "wTot" : "mTot");
//    }

    @Transient
    @JsonIgnore
    public String getComputedStartingWeightsSheetTemplateFileName() {
        if (startingWeightsSheetTemplateFileName == null) {
            return "WeighInSheetTemplate.xls";
        }
        return startingWeightsSheetTemplateFileName;
    }

    @Transient
    @JsonIgnore
    public String getComputedStartListTemplateFileName() {
        if (startListTemplateFileName == null) {
            return "StartSheetTemplate.xls";
        }
        return startListTemplateFileName;
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
     * @return the finalPackageTemplateFileName
     */
    public String getFinalPackageTemplateFileName() {
        return finalPackageTemplateFileName;
    }

    @Transient
    @JsonIgnore
    synchronized public List<Athlete> getGlobalSinclairRanking(Gender gender) {
        return getListOrElseRecompute(gender == Gender.F ? "wSinclair" : "mSinclair");
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

    /**
     * @return the juryTemplateFileName
     */
    public String getJuryTemplateFileName() {
        return juryTemplateFileName;
    }

    @SuppressWarnings("unchecked")
    @Transient
    @JsonIgnore
    synchronized public List<Athlete> getListOrElseRecompute(String listName) {
        List<Athlete> athletes = (List<Athlete>) reportingBeans.get(listName);
        if (isRankingsInvalid() || athletes == null) {
            setRankingsInvalid(true);
            while (isRankingsInvalid()) { // could be made invalid again while we compute
                setRankingsInvalid(false);
                // recompute because an athlete has been saved (new weight requested, good/bad lift, etc.)
                computeReportingInfo();
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
     * Gets the masters.
     *
     * @return the masters
     */
    public boolean getMasters() {
        return isMasters();
    }

    @Transient
    @JsonIgnore
    public Integer getMenPerTeamElseDefault() {
        return mensTeamSize != null ? mensTeamSize : 10;
    }

    public Integer getMensTeamSize() {
        return mensTeamSize;
    }

    /**
     * @return the protocolTemplateFileName
     */
    public String getProtocolTemplateFileName() {
        return protocolTemplateFileName;
    }

    public int getRefereeWakeUpDelay() {
        return refereeWakeUpDelay;
    }

    @Transient
    @JsonIgnore
    public HashMap<String, Object> getReportingBeans() {
        return reportingBeans;
    }

    /**
     * @return the startingWeightsSheetTemplateFileName
     */
    public String getStartingWeightsSheetTemplateFileName() {
        return startingWeightsSheetTemplateFileName;
    }

    /**
     * @return the startListTemplateFileName
     */
    public String getStartListTemplateFileName() {
        return startListTemplateFileName;
    }

    @Transient
    @JsonIgnore
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
    @Transient
    @JsonIgnore
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
     * @param cardsTemplateFileName the cardsTemplateFileName to set
     */
    public void setCardsTemplateFileName(String cardsFileName) {
        this.cardsTemplateFileName = cardsFileName;
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

    public void setCompetitionDateAsDate(Date ignored) {
    }

    public void setLocalizedCompetitionDate(String ignored) {
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

    public void setJuryTemplateFileName(String juryTemplateFileName) {
        this.juryTemplateFileName = juryTemplateFileName;
    }

    public void setMasters(boolean masters) {
        this.masters = masters;
    }

//    private String doFindFinalPackageTemplateFileName(String absoluteRoot) {
//        List<Resource> resourceList = new ResourceWalker().getResourceList(absoluteRoot,
//                ResourceWalker::relativeName, null, OwlcmsSession.getLocale());
//        for (Resource r : resourceList) {
//            logger.trace("checking {}", r.getFilePath());
//            if (this.isMasters() && r.getFileName().startsWith("Masters")) {
//                return r.getFileName();
//            } else if (r.getFileName().startsWith("Total")) {
//                return r.getFileName();
//            }
//        }
//        throw new RuntimeException("final package templates not found under " + absoluteRoot);
//    }

//    private String doFindProtocolFileName(String absoluteRoot) {
//        List<Resource> resourceList = new ResourceWalker().getResourceList(absoluteRoot,
//                ResourceWalker::relativeName, null, OwlcmsSession.getLocale());
//        for (Resource r : resourceList) {
//            logger.trace("checking {}", r.getFilePath());
//            if (this.isMasters() && r.getFileName().startsWith("Masters")) {
//                return r.getFileName();
//            } else if (r.getFileName().startsWith("Protocol")) {
//                return r.getFileName();
//            }
//        }
//        throw new RuntimeException("result templates not found under " + absoluteRoot);
//    }

    public void setMastersGenderEquality(boolean mastersGenderEquality) {
        this.mastersGenderEquality = mastersGenderEquality;
    }

    public void setMensTeamSize(Integer mensTeamSize) {
        this.mensTeamSize = mensTeamSize;
    }

    /**
     * Sets the protocol file name.
     *
     * @param protocolTemplateFileName the new protocol file name
     */
    public void setProtocolTemplateFileName(String protocolFileName) {
        this.protocolTemplateFileName = protocolFileName;
    }

    synchronized public void setRankingsInvalid(boolean invalid) {
        this.rankingsInvalid = invalid;
    }

    public void setRoundRobinOrder(boolean roundRobinOrder) {
        this.roundRobinOrder = roundRobinOrder;
    }

    public void setStartingWeightsSheetTemplateFileName(String startingWeightsSheetTemplateFileName) {
        this.startingWeightsSheetTemplateFileName = startingWeightsSheetTemplateFileName;
    }

    public void setStartListTemplateFileName(String startingListFileName) {
        this.startListTemplateFileName = startingListFileName;
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
    @Transient
    @JsonIgnore
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
                + federationWebSite + ", protocolTemplateFileName=" + getProtocolTemplateFileName()
                + ", finalPackageTemplateFileName=" + finalPackageTemplateFileName
                + ", ageGroupsFileName=" + ageGroupsFileName + ", enforce20kgRule="
                + enforce20kgRule + ", masters=" + masters + ", mensTeamSize=" + mensTeamSize + ", womensTeamSize="
                + womensTeamSize + ", customScore=" + customScore + ", mastersGenderEquality=" + mastersGenderEquality
                + ", useBirthYear=" + useBirthYear + ", useCategorySinclair=" + useCategorySinclair
                + ", useOldBodyWeightTieBreak=" + useOldBodyWeightTieBreak + ", useRegistrationCategory="
                + useRegistrationCategory + ", reportingBeans=" + reportingBeans + "]";
    }

    private void addToReportingBean(String string, List<Athlete> sorted) {
        List<Athlete> athletes = getOrCreateBean(string);
        athletes.addAll(sorted);
    }

    private void categoryRankings(List<PAthlete> athletes) {
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

        // sort only, use ranks stored in database
        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.SNATCH, false);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mSn", sortedMen);
        reportingBeans.put("wSn", sortedWomen);

        // sort only, use ranks stored in database
        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.CLEANJERK, false);
        AthleteSorter.assignCategoryRanks(sortedAthletes, Ranking.CLEANJERK);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mCJ", sortedMen);
        reportingBeans.put("wCJ", sortedWomen);

        // sort only, use ranks stored in database
        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.TOTAL, false);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mTot", sortedMen);
        reportingBeans.put("wTot", sortedWomen);
        reportingBeans.put("mwTot", sortedAthletes);
        logger.debug("mTot {}", sortedMen);
        logger.debug("wTot {}", sortedWomen);
//        for (Athlete a : sortedMen) {
//            debugRanks("mTot", a);
//        }

        // sort only, use ranks stored in database
        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.CUSTOM);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mCus", sortedMen);
        reportingBeans.put("wCus", sortedWomen);
        logger.debug("mCus {}", sortedMen);
        logger.debug("wCus {}", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.CAT_SINCLAIR);
        AthleteSorter.assignOverallRanksAndPoints(sortedAthletes, Ranking.CAT_SINCLAIR);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mCatSinclair", sortedMen);
        reportingBeans.put("wCatSinclair", sortedWomen);
        logger.debug("mCatSinclair {}", sortedMen);
        logger.debug("wCatSinclair {}", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.ROBI);
        AthleteSorter.assignOverallRanksAndPoints(sortedAthletes, Ranking.ROBI);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mRobi", sortedMen);
        reportingBeans.put("wRobi", sortedWomen);
        reportingBeans.put("mwRobi", sortedAthletes);
    }

    private void clearTeamReportingBeans(String suffix) {
        getOrCreateBean("mCombined" + suffix).clear();
        getOrCreateBean("wCombined" + suffix).clear();
        getOrCreateBean("mwCombined" + suffix).clear();
        getOrCreateBean("mTeam" + suffix).clear();
        getOrCreateBean("wTeam" + suffix).clear();
        getOrCreateBean("mwTeam" + suffix).clear();
        getOrCreateBean("mCustom" + suffix).clear();
        getOrCreateBean("wCustom" + suffix).clear();
        getOrCreateBean("mwCustom" + suffix).clear();
    }

    private void doComputeReportingInfo(boolean full, List<PAthlete> athletes, String ageGroupPrefix,
            AgeDivision ad) {
        // reporting does many database queries. fork a low-priority thread.
        runInThread(() -> {
            if (athletes.isEmpty()) {
                // prevent outputting silliness.
                logger./**/warn("no athletes");
                reportingBeans.clear();
                return;
            }

            // the ranks within a category are stored in the database and
            // not recomputed
            categoryRankings(athletes);

            // splitResultsByGroups(athletes);
            if (full) {
                reportingBeans.put("athletes", athletes);
                // logger.debug("ad={} ageGroupPrefix={}", ad, ageGroupPrefix);
                if (ad != null && (ageGroupPrefix == null || ageGroupPrefix.isBlank())) {
                    // iterate over all age groups present in age division ad
                    teamRankingsForAgeDivision(ad);
                } else {
                    teamRankings(athletes, ageGroupPrefix);
                }
            }

            globalRankings();
        }, Thread.MIN_PRIORITY);
    }

    /**
     * Compute a team-ranking for the specified PAthletes.
     *
     * PAthletes have a single participation, which is the one that will be used for ranking. Caller is responsible for
     * putting several age groups together (e.g. for Masters), or using a single age group (e.g. SR)
     *
     * Reporting beans are modified. Caller must clear them beforehand if needed.
     *
     * @param athletes
     * @param singleAgeGroup true if not called in a loop, can compute team stats.
     * @param ageGroupPrefix
     */
    private void doTeamRankings(List<PAthlete> athletes, String suffix, boolean singleAgeGroup) {
        // team-oriented rankings. These rankings put all the athletes from the same team
        // together, sorted according to their points, so the top n can be kept if needed.
        // substitutes are not included -- they should be marked as !isEligibleForTeamRanking

        List<Athlete> sortedAthletes;
        List<Athlete> sortedMen = new ArrayList<>();
        List<Athlete> sortedWomen = new ArrayList<>();
        splitPByGender(athletes, sortedMen, sortedWomen);

        sortedAthletes = AthleteSorter.teamPointsOrderCopy(athletes, Ranking.TOTAL);
        sortedMen = AthleteSorter.teamPointsOrderCopy(sortedMen, Ranking.TOTAL);
        sortedWomen = AthleteSorter.teamPointsOrderCopy(sortedWomen, Ranking.TOTAL);
        addToReportingBean("mTeam" + suffix, sortedMen);
        addToReportingBean("wTeam" + suffix, sortedWomen);
        addToReportingBean("mwTeam" + suffix, sortedAthletes);
        if (singleAgeGroup) {
            reportTeams(sortedAthletes, sortedMen, sortedWomen);
        }

        sortedAthletes = AthleteSorter.teamPointsOrderCopy(athletes, Ranking.SNATCH_CJ_TOTAL);
        sortedMen = AthleteSorter.teamPointsOrderCopy(sortedMen, Ranking.SNATCH_CJ_TOTAL);
        sortedWomen = AthleteSorter.teamPointsOrderCopy(sortedWomen, Ranking.SNATCH_CJ_TOTAL);
        addToReportingBean("mCombined" + suffix, sortedMen);
        addToReportingBean("wCombined" + suffix, sortedWomen);
        addToReportingBean("mwCombined" + suffix, sortedAthletes);
        if (singleAgeGroup) {
            reportCombined(sortedAthletes, sortedMen, sortedWomen);
        }

        sortedAthletes = AthleteSorter.teamPointsOrderCopy(athletes, Ranking.CUSTOM);
        sortedMen = AthleteSorter.teamPointsOrderCopy(sortedMen, Ranking.CUSTOM);
        sortedWomen = AthleteSorter.teamPointsOrderCopy(sortedWomen, Ranking.CUSTOM);
        addToReportingBean("mCustom" + suffix, sortedMen);
        addToReportingBean("wCustom" + suffix, sortedWomen);
        addToReportingBean("mwCustom" + suffix, sortedAthletes);
        if (singleAgeGroup) {
            reportCustom(sortedAthletes, sortedMen, sortedWomen);
        }

        sortedMen = getOrCreateBean("mTeamSinclair" + suffix);
        sortedWomen = getOrCreateBean("wTeamSinclair" + suffix);
        AthleteSorter.teamPointsOrder(sortedMen, Ranking.BW_SINCLAIR);
        AthleteSorter.teamPointsOrder(sortedWomen, Ranking.BW_SINCLAIR);
    }

    @SuppressWarnings("unchecked")
    private List<Athlete> getOrCreateBean(String string) {
        List<Athlete> list = (List<Athlete>) reportingBeans.get(string);
        if (list == null) {
            list = new ArrayList<>();
            reportingBeans.put(string, list);
        }
        return list;
    }

    private void globalRankings() {
        List<Athlete> athletes = AthleteRepository.findAllByGroupAndWeighIn(null, true);
        List<Athlete> sortedAthletes;
        List<Athlete> sortedMen;
        List<Athlete> sortedWomen;

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.BW_SINCLAIR);
        AthleteSorter.assignOverallRanksAndPoints(sortedAthletes, Ranking.BW_SINCLAIR);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mSinclair", sortedMen);
        reportingBeans.put("wSinclair", sortedWomen);
        logger.debug("mSinclair {}", sortedMen);
        logger.debug("wSinclair {}", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.SMM);
        AthleteSorter.assignOverallRanksAndPoints(sortedAthletes, Ranking.SMM);
        sortedMen = new ArrayList<>(sortedAthletes.size());
        sortedWomen = new ArrayList<>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mSmm", sortedMen);
        reportingBeans.put("wSmm", sortedWomen);
        logger.debug("mSmm {}", sortedMen);
        logger.debug("wSmm {}", sortedWomen);
    }

    private void reportCombined(List<Athlete> sortedAthletes, List<Athlete> sortedMen, List<Athlete> sortedWomen) {
        getOrCreateBean("mCombined");
        reportingBeans.put("mCombined", sortedMen);
        getOrCreateBean("wCombined");
        reportingBeans.put("wCombined", sortedWomen);
        getOrCreateBean("mwCombined");
        reportingBeans.put("mwCombined", sortedAthletes);
    }

    private void reportCustom(List<Athlete> sortedAthletes, List<Athlete> sortedMen, List<Athlete> sortedWomen) {
        getOrCreateBean("mCustom");
        reportingBeans.put("mCustom", sortedMen);
        getOrCreateBean("wCustom");
        reportingBeans.put("wCustom", sortedWomen);
        getOrCreateBean("mwCustom");
        reportingBeans.put("mwCustom", sortedAthletes);
    }

    private void reportSinclair(List<Athlete> sortedMen, List<Athlete> sortedWomen) {
        getOrCreateBean("mSinclair");
        reportingBeans.put("mSinclair", sortedMen);
        getOrCreateBean("wSinclair");
        reportingBeans.put("wSinclair", sortedWomen);
    }

    private void reportSMF(List<Athlete> sortedMen, List<Athlete> sortedWomen) {
        getOrCreateBean("mSMF");
        reportingBeans.put("mSMF", sortedMen);
        getOrCreateBean("wSMF");
        reportingBeans.put("wSMF", sortedWomen);
    }

    private void reportTeams(List<Athlete> sortedAthletes, List<Athlete> sortedMen,
            List<Athlete> sortedWomen) {
        // only needed once
        reportingBeans.put("nbMen", sortedMen.size());
        reportingBeans.put("nbWomen", sortedWomen.size());
        reportingBeans.put("nbAthletes", sortedMen.size() + sortedWomen.size());
        // extract club lists
        TreeSet<String> teams = new TreeSet<>();
        for (Athlete curAthlete : sortedAthletes) {
            if (curAthlete.getTeam() != null) {
                teams.add(curAthlete.getTeam());
            }
        }

        getOrCreateBean("mTeam");
        reportingBeans.put("mTeam", sortedMen);
        getOrCreateBean("wTeam");
        reportingBeans.put("wTeam", sortedWomen);
        getOrCreateBean("mwTeam");
        reportingBeans.put("mwTeam", sortedAthletes);

        reportingBeans.put("clubs", teams);
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
    }

    private void runInThread(Runnable runnable, int priority) {
        Thread t = new Thread(runnable);
        AtomicReference<Throwable> errorReference = new AtomicReference<>();
        t.setUncaughtExceptionHandler((th, ex) -> {
            errorReference.set(ex);
        });
        t.setPriority(priority);
        try {
            t.start();
            t.join();
            Throwable throwable = errorReference.get();
            if (throwable != null) {
                throw new RuntimeException(throwable);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private void splitResultsByGroups(List<PAthlete> athletes) {
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
                // logger.trace("adding {} to {}", a.getShortName(), group.getName());
                list.add(a);
            }
        }
        logger.debug("updated reporting data");
    }

    private void teamRankings(List<PAthlete> athletes, String ageGroupPrefix) {
        clearTeamReportingBeans(ageGroupPrefix);
        doTeamRankings(athletes, ageGroupPrefix, true);
    }

    /**
     * Iterate over all age prefixes in the age division and accumulate results.
     *
     * @param athletes
     * @param ageGroupPrefix
     */
    private void teamRankingsForAgeDivision(AgeDivision ad) {
        if (ad == null) {
            return;
        }
        List<String> agePrefixes = AgeGroupRepository.findActiveAndUsed(ad);

        for (String curAGPrefix : agePrefixes) {
            List<PAthlete> athletes = AgeGroupRepository.allPAthletesForAgeGroup(curAGPrefix);
            doTeamRankings(athletes, ad.name(), false);
        }

        List<Athlete> sortedAthletes;
        List<Athlete> sortedMen;
        List<Athlete> sortedWomen;

        sortedMen = getOrCreateBean("mTeam" + ad.name());
        sortedWomen = getOrCreateBean("wTeam" + ad.name());
        sortedAthletes = getOrCreateBean("mwTeam" + ad.name());
        AthleteSorter.teamPointsOrder(sortedMen, Ranking.TOTAL);
        AthleteSorter.teamPointsOrder(sortedWomen, Ranking.TOTAL);
        AthleteSorter.teamPointsOrder(sortedAthletes, Ranking.TOTAL);

        reportTeams(sortedAthletes, sortedMen, sortedWomen);

        sortedMen = getOrCreateBean("mCombined" + ad.name());
        sortedWomen = getOrCreateBean("wCombined" + ad.name());
        sortedAthletes = getOrCreateBean("mwCombined" + ad.name());
        AthleteSorter.teamPointsOrder(sortedMen, Ranking.SNATCH_CJ_TOTAL);
        AthleteSorter.teamPointsOrder(sortedWomen, Ranking.SNATCH_CJ_TOTAL);
        AthleteSorter.teamPointsOrder(sortedAthletes, Ranking.SNATCH_CJ_TOTAL);

        reportCombined(sortedAthletes, sortedMen, sortedWomen);

        sortedMen = getOrCreateBean("mCustom" + ad.name());
        sortedWomen = getOrCreateBean("wCustom" + ad.name());
        sortedAthletes = getOrCreateBean("mwCustom" + ad.name());
        AthleteSorter.teamPointsOrder(sortedMen, Ranking.CUSTOM);
        AthleteSorter.teamPointsOrder(sortedWomen, Ranking.CUSTOM);
        AthleteSorter.teamPointsOrder(sortedAthletes, Ranking.CUSTOM);

        reportCustom(sortedAthletes, sortedMen, sortedWomen);

        sortedMen = getOrCreateBean("mTeamSinclair" + ad.name());
        sortedWomen = getOrCreateBean("wTeamSinclair" + ad.name());
        AthleteSorter.teamPointsOrder(sortedMen, Ranking.BW_SINCLAIR);
        AthleteSorter.teamPointsOrder(sortedWomen, Ranking.BW_SINCLAIR);

        reportSinclair(sortedMen, sortedWomen);

        sortedMen = getOrCreateBean("mTeamSMF" + ad.name());
        sortedWomen = getOrCreateBean("wTeamSMF" + ad.name());
        AthleteSorter.teamPointsOrder(sortedMen, Ranking.SMM);
        AthleteSorter.teamPointsOrder(sortedWomen, Ranking.SMM);

        reportSMF(sortedMen, sortedWomen);
    }
}
