package app.owlcms.spreadsheet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.IRankHolder;
import app.owlcms.data.category.Participation;
import app.owlcms.data.group.Group;
import ch.qos.logback.classic.Logger;

/**
 * Fake athlete that belongs to a single category.
 *
 * Used to produce results and team rankings for a given eligibility category. Use athlete as a basis, and a copy of the
 * participation to the eligible category to recover ranks and points.
 *
 * @author Jean-François Lamy
 *
 */
@SuppressWarnings("deprecation")
public class PAthlete extends Athlete implements IRankHolder {
    
    Logger logger = (Logger) LoggerFactory.getLogger(PAthlete.class);

    private Participation p;

    private Athlete a;

    private Category c;

    public PAthlete(Participation p) {
        this.a = p.getAthlete();
        this.c = p.getCategory();
        this.p = new Participation(p, a, c);
    }

    public Athlete _getAthlete() {
        return a;
    }

    public Participation _getParticipation() {
        return p;
    }

    @Override
    public void computeMainCategory() {
        a.getCategory();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj) || (getClass() != obj.getClass())) {
            return false;
        }
        PAthlete other = (PAthlete) obj;
        return Objects.equals(a, other.a) && Objects.equals(c, other.c) && Objects.equals(p, other.p);
    }

    // the remaining methods come from athlete
    @Override
    public Integer getAge() {
        return a.getAge();
    }

    @Override
    public AgeGroup getAgeGroup() {
        return p.getCategory().getAgeGroup();
    }

    @Override
    public int getAttemptedLifts() {
        return a.getAttemptedLifts();
    }

    @Override
    public Integer getAttemptNumber() {
        return a.getAttemptNumber();
    }

    @Override
    public Integer getAttemptsDone() {
        return a.getAttemptsDone();
    }

    @Override
    public Integer getBestCleanJerk() {
        return a.getBestCleanJerk();
    }

    @Override
    public int getBestCleanJerkAttemptNumber() {
        return a.getBestCleanJerkAttemptNumber();
    }

    @Override
    public int getBestResultAttemptNumber() {
        return a.getBestResultAttemptNumber();
    }

    @Override
    public Integer getBestSnatch() {
        return a.getBestSnatch();
    }

    @Override
    public int getBestSnatchAttemptNumber() {
        return a.getBestSnatchAttemptNumber();
    }

    @Override
    public Integer getBirthDate() {
        return a.getBirthDate();
    }

    @Override
    public Double getBodyWeight() {
        return a.getBodyWeight();
    }

    @Override
    public String getBWCategory() {
        return a.getBWCategory();
    }

    @Override
    public Category getCategory() {
        return p.getCategory();
    }

    @Override
    public Double getCategorySinclair() {
        return a.getCategorySinclair();
    }

    @Override
    public int getCatSinclairRank() {
        return a.getCatSinclairRank();
    }

    @Override
    public String getCleanJerk1ActualLift() {
        return a.getCleanJerk1ActualLift();
    }

    @Override
    public Integer getCleanJerk1AsInteger() {
        return a.getCleanJerk1AsInteger();
    }

    @Override
    public String getCleanJerk1AutomaticProgression() {
        return a.getCleanJerk1AutomaticProgression();
    }

    @Override
    public String getCleanJerk1Change1() {
        return a.getCleanJerk1Change1();
    }

    @Override
    public String getCleanJerk1Change2() {
        return a.getCleanJerk1Change2();
    }

    @Override
    public String getCleanJerk1Declaration() {
        return a.getCleanJerk1Declaration();
    }

    @Override
    public LocalDateTime getCleanJerk1LiftTime() {
        return a.getCleanJerk1LiftTime();
    }

    @Override
    public String getCleanJerk2ActualLift() {
        return a.getCleanJerk2ActualLift();
    }

    @Override
    public Integer getCleanJerk2AsInteger() {
        return a.getCleanJerk2AsInteger();
    }

    @Override
    public String getCleanJerk2AutomaticProgression() {
        return a.getCleanJerk2AutomaticProgression();
    }

    @Override
    public String getCleanJerk2Change1() {
        return a.getCleanJerk2Change1();
    }

    @Override
    public String getCleanJerk2Change2() {
        return a.getCleanJerk2Change2();
    }

    @Override
    public String getCleanJerk2Declaration() {
        return a.getCleanJerk2Declaration();
    }

    @Override
    public LocalDateTime getCleanJerk2LiftTime() {
        return a.getCleanJerk2LiftTime();
    }

    @Override
    public String getCleanJerk3ActualLift() {
        return a.getCleanJerk3ActualLift();
    }

    @Override
    public Integer getCleanJerk3AsInteger() {
        return a.getCleanJerk3AsInteger();
    }

    @Override
    public String getCleanJerk3AutomaticProgression() {
        return a.getCleanJerk3AutomaticProgression();
    }

    @Override
    public String getCleanJerk3Change1() {
        return a.getCleanJerk3Change1();
    }

    @Override
    public String getCleanJerk3Change2() {
        return a.getCleanJerk3Change2();
    }

    @Override
    public String getCleanJerk3Declaration() {
        return a.getCleanJerk3Declaration();
    }

    @Override
    public LocalDateTime getCleanJerk3LiftTime() {
        return a.getCleanJerk3LiftTime();
    }

    @Override
    public Integer getCleanJerkAttemptsDone() {
        return a.getCleanJerkAttemptsDone();
    }

    @Override
    public int getCleanJerkPoints() {
        return p.getCleanJerkPoints();
    }

    @Override
    public int getCleanJerkRank() {
        return p.getCleanJerkRank();
    }

    @Override
    public int getCleanJerkTotal() {
        return a.getCleanJerkTotal();
    }

    @Override
    public String getClub() {
        return a.getClub();
    }

    @Override
    public Integer getCombinedPoints() {
        return p.getCombinedPoints();
    }

    @Override
    public int getCombinedRank() {
        return p.getCombinedRank();
    }

    @Override
    public String getCurrentAutomatic() {
        return a.getCurrentAutomatic();
    }

    @Override
    public String getCurrentChange1() {
        return a.getCurrentChange1();
    }

    @Override
    public String getCurrentDeclaration() {
        return a.getCurrentDeclaration();
    }

    @Override
    public int getCustomPoints() {
        return p.getCustomPoints();
    }

    @Override
    public int getCustomRank() {
        return p.getCustomRank();
    }

    @Override
    public Double getCustomScore() {
        return a.getCustomScore();
    }

    @Override
    public Double getCustomScoreComputed() {
        return a.getCustomScoreComputed();
    }

    @Override
    public String getDisplayCategory() {
        return p.getCategory().getName();
    }

    @Override
    public Set<Category> getEligibleCategories() {
        return a.getEligibleCategories();
    }

    @Override
    public Integer getEntryTotal() {
        return a.getEntryTotal();
    }

    @Override
    public LocalDateTime getFirstAttemptedLiftTime() {
        return a.getFirstAttemptedLiftTime();
    }

    @Override
    public String getFirstName() {
        return a.getFirstName();
    }

    @Override
    public String getFormattedBirth() {
        return a.getFormattedBirth();
    }

    @Override
    public LocalDate getFullBirthDate() {
        return a.getFullBirthDate();
    }

    @Override
    public String getFullId() {
        return a.getFullId();
    }

    @Override
    public String getFullName() {
        return a.getFullName();
    }

    @Override
    public Gender getGender() {
        return a.getGender();
    }

    @Override
    public Group getGroup() {
        return a.getGroup();
    }

    @Override
    public Long getId() {
        return a.getId();
    }

    @Override
    public LocalDateTime getLastAttemptedLiftTime() {
        return a.getLastAttemptedLiftTime();
    }

    @Override
    public String getLastName() {
        return a.getLastName();
    }

    @Override
    public LocalDateTime getLastSuccessfulLiftTime() {
        return a.getLastSuccessfulLiftTime();
    }

    @Override
    public Integer getLiftOrderRank() {
        return a.getLiftOrderRank();
    }

    @Override
    public Logger getLogger() {
        return a.getLogger();
    }

    @Override
    public String getLongCategory() {
        return a.getLongCategory();
    }

    @Override
    public Integer getLotNumber() {
        return a.getLotNumber();
    }

    @Override
    public Participation getMainRankings() {
        return p;
    }

    @Override
    public String getMastersAgeGroup() {
        return a.getMastersAgeGroup();
    }

    @Override
    public String getMastersAgeGroupInterval() {
        return a.getMastersAgeGroupInterval();
    }

    @Override
    public String getMastersGenderAgeGroupInterval() {
        return a.getMastersGenderAgeGroupInterval();
    }

    @Override
    public String getMastersLongCategory() {
        return a.getMastersLongCategory();
    }

    @Override
    public Integer getMedalRank() {
        return a.getMedalRank();
    }

    @Override
    public String getMembership() {
        return a.getMembership();
    }

    @Override
    public Integer getNextAttemptRequestedWeight() {
        return a.getNextAttemptRequestedWeight();
    }

    @Override
    public List<Participation> getParticipations() {
        List<Participation> lp = new ArrayList<>(1);
        lp.add(p);
        return lp;
    }

    @Override
    public Double getPresumedBodyWeight() {
        return a.getPresumedBodyWeight();
    }

    @Override
    public LocalDateTime getPreviousLiftTime() {
        return a.getPreviousLiftTime();
    }

    @Override
    public Integer getQualifyingTotal() {
        return a.getQualifyingTotal();
    }

    @Override
    public Integer getRank() {
        return a.getRank();
    }

    @Override
    public Category getRegistrationCategory() {
        return a.getCategory();
    }

    @Override
    public Integer getRequestedWeightForAttempt(int attempt) {
        return a.getRequestedWeightForAttempt(attempt);
    }

    @Override
    public Double getRobi() {
        // we want the getMainRanking from this class which uses
        // the participation, not the real athlete's category
        Double robi = super.getRobi();
        //logger.trace("getRobi {} {} {} {}", _getAthlete().getShortName(), _getAthlete().getCategory(), getCategory(), robi);
        return robi;
    }

    @Override
    public Integer getRobiRank() {
        return a.getRobiRank();
    }

    @Override
    public String getRoundedBodyWeight() {
        return a.getRoundedBodyWeight();
    }

    @Override
    public String getShortCategory() {
        return a.getShortCategory();
    }

    @Override
    public String getShortName() {
        return a.getShortName();
    }

    @Override
    public Double getSinclair() {
        return a.getSinclair();
    }

    @Override
    public Double getSinclair(Double bodyWeight1) {
        return a.getSinclair(bodyWeight1);
    }

    @Override
    public Double getSinclairFactor() {
        return a.getSinclairFactor();
    }

    @Override
    public Double getSinclairForDelta() {
        return a.getSinclairForDelta();
    }

    @Override
    public Integer getSinclairRank() {
        return a.getSinclairRank();
    }

    @Override
    public Double getSmm() {
        return a.getSmm();
    }

    @Override
    public int getSmmRank() {
        return a.getSmmRank();
    }

    @Override
    public String getSnatch1ActualLift() {
        String snatch1ActualLift = a.getSnatch1ActualLift();
        return snatch1ActualLift;
    }

    @Override
    public Integer getSnatch1AsInteger() {
        return a.getSnatch1AsInteger();
    }

    @Override
    public String getSnatch1AutomaticProgression() {
        return a.getSnatch1AutomaticProgression();
    }

    @Override
    public String getSnatch1Change1() {
        return a.getSnatch1Change1();
    }

    @Override
    public String getSnatch1Change2() {
        return a.getSnatch1Change2();
    }

    @Override
    public String getSnatch1Declaration() {
        return a.getSnatch1Declaration();
    }

    @Override
    public LocalDateTime getSnatch1LiftTime() {
        return a.getSnatch1LiftTime();
    }

    @Override
    public String getSnatch2ActualLift() {
        return a.getSnatch2ActualLift();
    }

    @Override
    public Integer getSnatch2AsInteger() {
        return a.getSnatch2AsInteger();
    }

    @Override
    public String getSnatch2AutomaticProgression() {
        return a.getSnatch2AutomaticProgression();
    }

    @Override
    public String getSnatch2Change1() {
        return a.getSnatch2Change1();
    }

    @Override
    public String getSnatch2Change2() {
        return a.getSnatch2Change2();
    }

    @Override
    public String getSnatch2Declaration() {
        return a.getSnatch2Declaration();
    }

    @Override
    public LocalDateTime getSnatch2LiftTime() {
        return a.getSnatch2LiftTime();
    }

    @Override
    public String getSnatch3ActualLift() {
        return a.getSnatch3ActualLift();
    }

    @Override
    public Integer getSnatch3AsInteger() {
        return a.getSnatch3AsInteger();
    }

    @Override
    public String getSnatch3AutomaticProgression() {
        return a.getSnatch3AutomaticProgression();
    }

    @Override
    public String getSnatch3Change1() {
        return a.getSnatch3Change1();
    }

    @Override
    public String getSnatch3Change2() {
        return a.getSnatch3Change2();
    }

    @Override
    public String getSnatch3Declaration() {
        return a.getSnatch3Declaration();
    }

    @Override
    public LocalDateTime getSnatch3LiftTime() {
        return a.getSnatch3LiftTime();
    }

    @Override
    public Integer getSnatchAttemptsDone() {
        return a.getSnatchAttemptsDone();
    }

    @Override
    public int getSnatchPoints() {
        return p.getSnatchPoints();
    }

    @Override
    public int getSnatchRank() {
        return p.getSnatchRank();
    }

    @Override
    public int getSnatchTotal() {
        return a.getSnatchTotal();
    }

    @Override
    public Integer getStartNumber() {
        return a.getStartNumber();
    }

    @Override
    public String getTeam() {
        return a.getTeam();
    }

    @Override
    public Integer getTeamCleanJerkRank() {
        return a.getTeamCleanJerkRank();
    }

    @Override
    public Integer getTeamCombinedRank() {
        return a.getTeamCombinedRank();
    }

    @Override
    public Boolean getTeamMember() {
        return a.getTeamMember();
    }

    @Override
    public Integer getTeamRobiRank() {
        return a.getTeamRobiRank();
    }

    @Override
    public Integer getTeamSinclairRank() {
        return a.getTeamSinclairRank();
    }

    @Override
    public Integer getTeamSnatchRank() {
        return a.getTeamSnatchRank();
    }

    @Override
    public Integer getTeamTotalRank() {
        return a.getTeamTotalRank();
    }

    @Override
    public Integer getTotal() {
        return a.getTotal();
    }

    @Override
    public int getTotalPoints() {
        return p.getTotalPoints();
    }

    @Override
    public int getTotalRank() {
        return p.getTotalRank();
    }

    @Override
    public Integer getYearOfBirth() {
        return a.getYearOfBirth();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(a, c, p);
        return result;
    }

    @Override
    public boolean isATeamMember() {
        return a.isATeamMember();
    }

    @Override
    public int isDeclaring() {
        return a.isDeclaring();
    }

    @Override
    public boolean isEligibleForIndividualRanking() {
        return a.isEligibleForIndividualRanking();
    }

    @Override
    public boolean isEligibleForTeamRanking() {
        return a.isEligibleForTeamRanking();
    }

    @Override
    public boolean isForcedAsCurrent() {
        return a.isForcedAsCurrent();
    }

    @Override
    public boolean isInvited() {
        return a.isInvited();
    }

    @Override
    public boolean isTeamMember() {
        return p.getTeamMember();
    }

    @Override
    public boolean isValidation() {
        return a.isValidation();
    }

    @Override
    public String longDump() {
        return a.longDump();
    }

    @Override
    public void setCatSinclairRank(int i) {
        a.setCatSinclairRank(i);
    }

    @Override
    public void setRobiRank(Integer robiRank) {
        a.setRobiRank(robiRank);
    }

    @Override
    public void setSinclairRank(Integer sinclairRank) {
        a.setSinclairRank(sinclairRank);
    }

    @Override
    public void setSmmRank(int i) {
        a.setSmmRank(i);
    }

    @Override
    public void setTeamCleanJerkRank(Integer teamCJRank) {
        p.setTeamCleanJerkRank(teamCJRank);
    }

    @Override
    public void setTeamCombinedRank(Integer teamCombinedRank) {
        p.setTeamCombinedRank(teamCombinedRank);
    }

    @Override
    public void setTeamMember(boolean member) {
        p.setTeamMember(member);
    }

    @Override
    public void setTeamRobiRank(Integer teamRobiRank) {
        p.setTeamRobiRank(teamRobiRank);
    }

    @Override
    public void setTeamSinclairRank(Integer teamSinclairRank) {
        p.setTeamSinclairRank(teamSinclairRank);
    }

    @Override
    public void setTeamSnatchRank(Integer teamSnatchRank) {
        p.setTeamSnatchRank(teamSnatchRank);
    }

    @Override
    public void setTeamTotalRank(Integer teamTotalRank) {
        p.setTeamTotalRank(teamTotalRank);
    }

    @Override
    public String toStringRanks() {
        // super is used because we want the methods from PAthlete to be called
        // and we don't want to copy the code.
        return super.toStringRanks();
    }

}
