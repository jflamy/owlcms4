package app.owlcms.spreadsheet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

    private Participation p;
    private Athlete a;
    private Category c;

    public PAthlete(Participation p) {
        this.a = p.getAthlete();
        this.c = p.getCategory();
        this.p = new Participation(p, a, c);
    }

// the methods used for ranking come from the participation to an eligible category

    @Override
    public Participation getMainRankings() {
        return p;
    }

    @Override
    public List<Participation> getParticipations() {
        List<Participation> lp = new ArrayList<>(1);
        lp.add(p);
        return lp;
    }

    @Override
    public AgeGroup getAgeGroup() {
        return p.getCategory().getAgeGroup();
    }

    @Override
    public Category getCategory() {
        return p.getCategory();
    }

    @Override
    public Category getRegistrationCategory() {
        return a.getCategory();
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
    public Integer getCombinedPoints() {
        return p.getCombinedPoints();
    }

    @Override
    public int getCombinedRank() {
        return p.getCombinedRank();
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
    public int getSnatchPoints() {
        return p.getSnatchPoints();
    }

    @Override
    public int getSnatchRank() {
        return p.getSnatchRank();
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
    public String toStringRanks() {
        // super is used because we want the methods from PAthlete to be called
        // and we don't want to copy the code.
        return super.toStringRanks();
    }
    
    @Override
    public boolean isTeamMember() {
        return p.isTeamMember();
    }

    public void computeMainCategory() {
        a.getCategory();
    }

    public String getDisplayCategory() {
        return p.getCategory().getName();
    }

    // the remaining methods come from athlete
    public Integer getAge() {
        return a.getAge();
    }

    public int getAttemptedLifts() {
        return a.getAttemptedLifts();
    }

    public Integer getAttemptNumber() {
        return a.getAttemptNumber();
    }

    public Integer getAttemptsDone() {
        return a.getAttemptsDone();
    }

    public Integer getBestCleanJerk() {
        return a.getBestCleanJerk();
    }

    public int getBestCleanJerkAttemptNumber() {
        return a.getBestCleanJerkAttemptNumber();
    }

    public int getBestResultAttemptNumber() {
        return a.getBestResultAttemptNumber();
    }

    public Integer getBestSnatch() {
        return a.getBestSnatch();
    }

    public int getBestSnatchAttemptNumber() {
        return a.getBestSnatchAttemptNumber();
    }

    public Integer getBirthDate() {
        return a.getBirthDate();
    }

    public Double getBodyWeight() {
        return a.getBodyWeight();
    }

    public String getBWCategory() {
        return a.getBWCategory();
    }

    public Double getCategorySinclair() {
        return a.getCategorySinclair();
    }

    public int getCatSinclairRank() {
        return a.getCatSinclairRank();
    }

    public String getCleanJerk1ActualLift() {
        return a.getCleanJerk1ActualLift();
    }

    public Integer getCleanJerk1AsInteger() {
        return a.getCleanJerk1AsInteger();
    }

    public String getCleanJerk1AutomaticProgression() {
        return a.getCleanJerk1AutomaticProgression();
    }

    public String getCleanJerk1Change1() {
        return a.getCleanJerk1Change1();
    }

    public String getCleanJerk1Change2() {
        return a.getCleanJerk1Change2();
    }

    public String getCleanJerk1Declaration() {
        return a.getCleanJerk1Declaration();
    }

    public LocalDateTime getCleanJerk1LiftTime() {
        return a.getCleanJerk1LiftTime();
    }

    public String getCleanJerk2ActualLift() {
        return a.getCleanJerk2ActualLift();
    }

    public Integer getCleanJerk2AsInteger() {
        return a.getCleanJerk2AsInteger();
    }

    public String getCleanJerk2AutomaticProgression() {
        return a.getCleanJerk2AutomaticProgression();
    }

    public String getCleanJerk2Change1() {
        return a.getCleanJerk2Change1();
    }

    public String getCleanJerk2Change2() {
        return a.getCleanJerk2Change2();
    }

    public String getCleanJerk2Declaration() {
        return a.getCleanJerk2Declaration();
    }

    public LocalDateTime getCleanJerk2LiftTime() {
        return a.getCleanJerk2LiftTime();
    }

    public String getCleanJerk3ActualLift() {
        return a.getCleanJerk3ActualLift();
    }

    public Integer getCleanJerk3AsInteger() {
        return a.getCleanJerk3AsInteger();
    }

    public String getCleanJerk3AutomaticProgression() {
        return a.getCleanJerk3AutomaticProgression();
    }

    public String getCleanJerk3Change1() {
        return a.getCleanJerk3Change1();
    }

    public String getCleanJerk3Change2() {
        return a.getCleanJerk3Change2();
    }

    public String getCleanJerk3Declaration() {
        return a.getCleanJerk3Declaration();
    }

    public LocalDateTime getCleanJerk3LiftTime() {
        return a.getCleanJerk3LiftTime();
    }

    public Integer getCleanJerkAttemptsDone() {
        return a.getCleanJerkAttemptsDone();
    }

    public int getCleanJerkTotal() {
        return a.getCleanJerkTotal();
    }

    public String getClub() {
        return a.getClub();
    }

    public String getCurrentAutomatic() {
        return a.getCurrentAutomatic();
    }

    public String getCurrentChange1() {
        return a.getCurrentChange1();
    }

    public String getCurrentDeclaration() {
        return a.getCurrentDeclaration();
    }

    public Double getCustomScore() {
        return a.getCustomScore();
    }

    public Double getCustomScoreComputed() {
        return a.getCustomScoreComputed();
    }

    public Set<Category> getEligibleCategories() {
        return a.getEligibleCategories();
    }

    public Integer getEntryTotal() {
        return a.getEntryTotal();
    }

    public LocalDateTime getFirstAttemptedLiftTime() {
        return a.getFirstAttemptedLiftTime();
    }

    public String getFirstName() {
        return a.getFirstName();
    }

    public String getFormattedBirth() {
        return a.getFormattedBirth();
    }

    public LocalDate getFullBirthDate() {
        return a.getFullBirthDate();
    }

    public String getFullId() {
        return a.getFullId();
    }

    public String getFullName() {
        return a.getFullName();
    }

    public Gender getGender() {
        return a.getGender();
    }

    public Group getGroup() {
        return a.getGroup();
    }

    public Long getId() {
        return a.getId();
    }

    public LocalDateTime getLastAttemptedLiftTime() {
        return a.getLastAttemptedLiftTime();
    }

    public String getLastName() {
        return a.getLastName();
    }

    public LocalDateTime getLastSuccessfulLiftTime() {
        return a.getLastSuccessfulLiftTime();
    }

    public Integer getLiftOrderRank() {
        return a.getLiftOrderRank();
    }

    public Logger getLogger() {
        return a.getLogger();
    }

    public String getLongCategory() {
        return a.getLongCategory();
    }

    public Integer getLotNumber() {
        return a.getLotNumber();
    }

    public String getMastersAgeGroup() {
        return a.getMastersAgeGroup();
    }

    public String getMastersAgeGroupInterval() {
        return a.getMastersAgeGroupInterval();
    }

    public String getMastersGenderAgeGroupInterval() {
        return a.getMastersGenderAgeGroupInterval();
    }

    public String getMastersLongCategory() {
        return a.getMastersLongCategory();
    }

    public Integer getMedalRank() {
        return a.getMedalRank();
    }

    public String getMembership() {
        return a.getMembership();
    }

    public Integer getNextAttemptRequestedWeight() {
        return a.getNextAttemptRequestedWeight();
    }

    public Double getPresumedBodyWeight() {
        return a.getPresumedBodyWeight();
    }

    public LocalDateTime getPreviousLiftTime() {
        return a.getPreviousLiftTime();
    }

    public Integer getQualifyingTotal() {
        return a.getQualifyingTotal();
    }

    public Integer getRank() {
        return a.getRank();
    }

    public Integer getRequestedWeightForAttempt(int attempt) {
        return a.getRequestedWeightForAttempt(attempt);
    }

    public Double getRobi() {
        return a.getRobi();
    }

    public Integer getRobiRank() {
        return a.getRobiRank();
    }

    public String getRoundedBodyWeight() {
        return a.getRoundedBodyWeight();
    }

    public String getShortCategory() {
        return a.getShortCategory();
    }

    public String getShortName() {
        return a.getShortName();
    }

    public Double getSinclair() {
        return a.getSinclair();
    }

    public Double getSinclair(Double bodyWeight1) {
        return a.getSinclair(bodyWeight1);
    }

    public Double getSinclairFactor() {
        return a.getSinclairFactor();
    }

    public Double getSinclairForDelta() {
        return a.getSinclairForDelta();
    }

    public Integer getSinclairRank() {
        return a.getSinclairRank();
    }

    public Double getSmm() {
        return a.getSmm();
    }

    public int getSmmRank() {
        return a.getSmmRank();
    }

    public String getSnatch1ActualLift() {
        return a.getSnatch1ActualLift();
    }

    public Integer getSnatch1AsInteger() {
        return a.getSnatch1AsInteger();
    }

    public String getSnatch1AutomaticProgression() {
        return a.getSnatch1AutomaticProgression();
    }

    public String getSnatch1Change1() {
        return a.getSnatch1Change1();
    }

    public String getSnatch1Change2() {
        return a.getSnatch1Change2();
    }

    public String getSnatch1Declaration() {
        return a.getSnatch1Declaration();
    }

    public LocalDateTime getSnatch1LiftTime() {
        return a.getSnatch1LiftTime();
    }

    public String getSnatch2ActualLift() {
        return a.getSnatch2ActualLift();
    }

    public Integer getSnatch2AsInteger() {
        return a.getSnatch2AsInteger();
    }

    public String getSnatch2AutomaticProgression() {
        return a.getSnatch2AutomaticProgression();
    }

    public String getSnatch2Change1() {
        return a.getSnatch2Change1();
    }

    public String getSnatch2Change2() {
        return a.getSnatch2Change2();
    }

    public String getSnatch2Declaration() {
        return a.getSnatch2Declaration();
    }

    public LocalDateTime getSnatch2LiftTime() {
        return a.getSnatch2LiftTime();
    }

    public String getSnatch3ActualLift() {
        return a.getSnatch3ActualLift();
    }

    public Integer getSnatch3AsInteger() {
        return a.getSnatch3AsInteger();
    }

    public String getSnatch3AutomaticProgression() {
        return a.getSnatch3AutomaticProgression();
    }

    public String getSnatch3Change1() {
        return a.getSnatch3Change1();
    }

    public String getSnatch3Change2() {
        return a.getSnatch3Change2();
    }

    public String getSnatch3Declaration() {
        return a.getSnatch3Declaration();
    }

    public LocalDateTime getSnatch3LiftTime() {
        return a.getSnatch3LiftTime();
    }

    public Integer getSnatchAttemptsDone() {
        return a.getSnatchAttemptsDone();
    }

    public int getSnatchTotal() {
        return a.getSnatchTotal();
    }

    public Integer getStartNumber() {
        return a.getStartNumber();
    }

    public String getTeam() {
        return a.getTeam();
    }

    public Integer getTeamCleanJerkRank() {
        return a.getTeamCleanJerkRank();
    }

    public Integer getTeamCombinedRank() {
        return a.getTeamCombinedRank();
    }

    public Boolean getTeamMember() {
        return a.getTeamMember();
    }

    public Integer getTeamRobiRank() {
        return a.getTeamRobiRank();
    }

    public Integer getTeamSinclairRank() {
        return a.getTeamSinclairRank();
    }

    public Integer getTeamSnatchRank() {
        return a.getTeamSnatchRank();
    }

    public Integer getTeamTotalRank() {
        return a.getTeamTotalRank();
    }

    public Integer getTotal() {
        return a.getTotal();
    }

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
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        PAthlete other = (PAthlete) obj;
        return Objects.equals(a, other.a) && Objects.equals(c, other.c) && Objects.equals(p, other.p);
    }

    public boolean isATeamMember() {
        return a.isATeamMember();
    }

    public int isDeclaring() {
        return a.isDeclaring();
    }

    public boolean isEligibleForIndividualRanking() {
        return a.isEligibleForIndividualRanking();
    }

    public boolean isEligibleForTeamRanking() {
        return a.isEligibleForTeamRanking();
    }

    public boolean isForcedAsCurrent() {
        return a.isForcedAsCurrent();
    }

    public boolean isInvited() {
        return a.isInvited();
    }

    public boolean isValidation() {
        return a.isValidation();
    }

    public String longDump() {
        return a.longDump();
    }


}
