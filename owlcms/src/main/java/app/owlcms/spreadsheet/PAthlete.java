/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Transient;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
 * Used to produce results and team rankings for a given eligibility category.
 * Use athlete as a basis, and a copy of the participation to the eligible
 * category to recover ranks and points.
 *
 * @author Jean-François Lamy
 *
 */
@SuppressWarnings("deprecation")
public class PAthlete extends Athlete implements IRankHolder {

	Logger logger = (Logger) LoggerFactory.getLogger(PAthlete.class);
	private Athlete a;
	private Category c;
	@JsonIgnore
	@Transient
	private Participation originalParticipation;
	private Participation p;

	public PAthlete(Participation p) {
		this.a = p.getAthlete();
		this.c = p.getCategory();
		this.p = new Participation(p, this.a, this.c);
		this.originalParticipation = p;
	}

	/**
	 * used only for debugging
	 *
	 * @return
	 */
	@JsonIgnore
	@Transient
	public Athlete _getAthlete() {
		return this.a;
	}

	/**
	 * used only for debugging
	 *
	 * @return
	 */
	@JsonIgnore
	@Transient
	public Participation _getOriginalParticipation() {
		return this.originalParticipation;
	}

	/**
	 * used only for debugging
	 *
	 * @return
	 */
	@JsonIgnore
	@Transient
	public Participation _getParticipation() {
		return this.p;
	}

	@Override
	public void computeMainAndEligibleCategories() {
		this.a.getCategory();
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
		return Objects.equals(this.a, other.a) && Objects.equals(this.c, other.c) && Objects.equals(this.p, other.p);
	}

	@Override
	public int getActuallyAttemptedLifts() {
		return this.a.getActuallyAttemptedLifts();
	}

	// the remaining methods come from athlete
	@Override
	public Integer getAge() {
		return this.a.getAge();
	}

	@Override
	public AgeGroup getAgeGroup() {
		return this.p.getCategory().getAgeGroup();
	}

	@Override
	public Integer getAttemptNumber() {
		return this.a.getAttemptNumber();
	}

	@Override
	public Integer getAttemptsDone() {
		return this.a.getAttemptsDone();
	}

	@Override
	public Integer getBestCleanJerk() {
		return this.a.getBestCleanJerk();
	}

	@Override
	public int getBestCleanJerkAttemptNumber() {
		return this.a.getBestCleanJerkAttemptNumber();
	}

	@Override
	public int getBestResultAttemptNumber() {
		return this.a.getBestResultAttemptNumber();
	}

	@Override
	public Integer getBestSnatch() {
		return this.a.getBestSnatch();
	}

	@Override
	public int getBestSnatchAttemptNumber() {
		return this.a.getBestSnatchAttemptNumber();
	}

	@Override
	public Integer getBirthDate() {
		return this.a.getBirthDate();
	}

	@Override
	public Double getBodyWeight() {
		return this.a.getBodyWeight();
	}

	@Override
	public String getBWCategory() {
		return this.a.getBWCategory();
	}

	@Override
	public Category getCategory() {
		return this.p.getCategory();
	}

	@Override
	public String getCategoryCode() {
		return this.a.getCategoryCode();
	}

	@Override
	public Double getCategorySinclair() {
		return this.a.getCategorySinclair();
	}

	@Override
	public int getCatSinclairRank() {
		return this.a.getCatSinclairRank();
	}

	@Override
	public String getCleanJerk1ActualLift() {
		return this.a.getCleanJerk1ActualLift();
	}

	@Override
	public Integer getCleanJerk1AsInteger() {
		return this.a.getCleanJerk1AsInteger();
	}

	@Override
	public String getCleanJerk1AutomaticProgression() {
		return this.a.getCleanJerk1AutomaticProgression();
	}

	@Override
	public String getCleanJerk1Change1() {
		return this.a.getCleanJerk1Change1();
	}

	@Override
	public String getCleanJerk1Change2() {
		return this.a.getCleanJerk1Change2();
	}

	@Override
	public String getCleanJerk1Declaration() {
		return this.a.getCleanJerk1Declaration();
	}

	@Override
	public LocalDateTime getCleanJerk1LiftTime() {
		return this.a.getCleanJerk1LiftTime();
	}

	@Override
	public String getCleanJerk2ActualLift() {
		return this.a.getCleanJerk2ActualLift();
	}

	@Override
	public Integer getCleanJerk2AsInteger() {
		return this.a.getCleanJerk2AsInteger();
	}

	@Override
	public String getCleanJerk2AutomaticProgression() {
		return this.a.getCleanJerk2AutomaticProgression();
	}

	@Override
	public String getCleanJerk2Change1() {
		return this.a.getCleanJerk2Change1();
	}

	@Override
	public String getCleanJerk2Change2() {
		return this.a.getCleanJerk2Change2();
	}

	@Override
	public String getCleanJerk2Declaration() {
		return this.a.getCleanJerk2Declaration();
	}

	@Override
	public LocalDateTime getCleanJerk2LiftTime() {
		return this.a.getCleanJerk2LiftTime();
	}

	@Override
	public String getCleanJerk3ActualLift() {
		return this.a.getCleanJerk3ActualLift();
	}

	@Override
	public Integer getCleanJerk3AsInteger() {
		return this.a.getCleanJerk3AsInteger();
	}

	@Override
	public String getCleanJerk3AutomaticProgression() {
		return this.a.getCleanJerk3AutomaticProgression();
	}

	@Override
	public String getCleanJerk3Change1() {
		return this.a.getCleanJerk3Change1();
	}

	@Override
	public String getCleanJerk3Change2() {
		return this.a.getCleanJerk3Change2();
	}

	@Override
	public String getCleanJerk3Declaration() {
		return this.a.getCleanJerk3Declaration();
	}

	@Override
	public LocalDateTime getCleanJerk3LiftTime() {
		return this.a.getCleanJerk3LiftTime();
	}

	@Override
	public Integer getCleanJerkAttemptsDone() {
		return this.a.getCleanJerkAttemptsDone();
	}

	@Override
	public int getCleanJerkPoints() {
		return this.p.getCleanJerkPoints();
	}

	@Override
	public int getCleanJerkRank() {
		return this.p.getCleanJerkRank();
	}

	@Override
	public int getCleanJerkTotal() {
		return this.a.getCleanJerkTotal();
	}

	@Override
	public String getClub() {
		return this.a.getClub();
	}

	@Override
	public String getCoach() {
		return this.a.getCoach();
	}

	@Override
	public Integer getCombinedPoints() {
		return this.p.getCombinedPoints();
	}

	@Override
	public int getCombinedRank() {
		return this.p.getCombinedRank();
	}

	@Override
	public String getCurrentAutomatic() {
		return this.a.getCurrentAutomatic();
	}

	@Override
	public String getCurrentChange1() {
		return this.a.getCurrentChange1();
	}

	@Override
	public String getCurrentDeclaration() {
		return this.a.getCurrentDeclaration();
	}

	@Override
	public String getCustom1() {
		return this.a.getCustom1();
	}

	@Override
	public String getCustom2() {
		return this.a.getCustom2();
	}

	@Override
	public int getCustomPoints() {
		return this.p.getCustomPoints();
	}

	@Override
	public int getCustomRank() {
		return this.p.getCustomRank();
	}

	@Override
	public Double getCustomScore() {
		return this.a.getCustomScore();
	}

	@Override
	public Double getCustomScoreComputed() {
		return this.a.getCustomScoreComputed();
	}

	@Override
	public String getDisplayCategory() {
		return this.p.getCategory().getTranslatedName();
	}

	@Override
	public Set<Category> getEligibleCategories() {
		return this.a.getEligibleCategories();
	}

	@Override
	public Integer getEntryTotal() {
		return this.a.getEntryTotal();
	}

	@Override
	public LocalDateTime getFirstAttemptedLiftTime() {
		return this.a.getFirstAttemptedLiftTime();
	}

	@Override
	public String getFirstName() {
		return this.a.getFirstName();
	}

	@Override
	public String getFormattedBirth() {
		return this.a.getFormattedBirth();
	}

	@Override
	public LocalDate getFullBirthDate() {
		return this.a.getFullBirthDate();
	}

	@Override
	public String getFullId() {
		return this.a.getFullId();
	}

	@Override
	public String getFullName() {
		return this.a.getFullName();
	}

	@Override
	public Gender getGender() {
		return this.a.getGender();
	}

	@Override
	public Group getGroup() {
		return this.a.getGroup();
	}

	@Override
	public Long getId() {
		return this.a.getId();
	}

	@Override
	public LocalDateTime getLastAttemptedLiftTime() {
		return this.a.getLastAttemptedLiftTime();
	}

	@Override
	public String getLastName() {
		return this.a.getLastName();
	}

	@Override
	public LocalDateTime getLastSuccessfulLiftTime() {
		return this.a.getLastSuccessfulLiftTime();
	}

	@Override
	public Integer getLiftOrderRank() {
		return this.a.getLiftOrderRank();
	}

	@Override
	public Logger getLogger() {
		return this.a.getLogger();
	}

	@Override
	public String getLongCategory() {
		return this.a.getLongCategory();
	}

	@Override
	public Integer getLotNumber() {
		return this.a.getLotNumber();
	}

	@Override
	public Participation getMainRankings() {
		return this.p;
	}

	@Override
	public String getMastersAgeGroup() {
		return this.a.getMastersAgeGroup();
	}

	@Override
	public String getMastersAgeGroupInterval() {
		return this.a.getMastersAgeGroupInterval();
	}

	@Override
	public String getMastersGenderAgeGroupInterval() {
		return this.a.getMastersGenderAgeGroupInterval();
	}

	@Override
	public String getMastersLongCategory() {
		return this.a.getMastersLongCategory();
	}

	@Override
	public Integer getMedalRank() {
		return this.a.getMedalRank();
	}

	@Override
	public String getMembership() {
		return this.a.getMembership();
	}

	@Override
	public Integer getNextAttemptRequestedWeight() {
		return this.a.getNextAttemptRequestedWeight();
	}

	@Override
	public List<Participation> getParticipations() {
		List<Participation> lp = new ArrayList<>(1);
		lp.add(this.p);
		return lp;
	}

	@Override
	public Integer getPersonalBestCleanJerk() {
		return this.a.getPersonalBestCleanJerk();
	}

	@Override
	public Integer getPersonalBestSnatch() {
		return this.a.getPersonalBestSnatch();
	}

	@Override
	public Integer getPersonalBestTotal() {
		return this.a.getPersonalBestTotal();
	}

	@Override
	public Double getPresumedBodyWeight() {
		return this.a.getPresumedBodyWeight();
	}

	@Override
	public String getPresumedBodyWeightString() {
		return this.a.getPresumedBodyWeightString();
	}

	@Override
	public String getPresumedOpenCategoryString() {
		return this.a.getPresumedOpenCategoryString();
	}

	@Override
	public LocalDateTime getPreviousLiftTime() {
		return this.a.getPreviousLiftTime();
	}

	@Override
	public Integer getQualifyingTotal() {
		return this.a.getQualifyingTotal();
	}

	@Override
	public Integer getRank() {
		return this.a.getRank();
	}

	@Override
	public Category getRegistrationCategory() {
		return this.a.getCategory();
	}

	@Override
	public Integer getRequestedWeightForAttempt(int attempt) {
		return this.a.getRequestedWeightForAttempt(attempt);
	}

	@Override
	public Double getRobi() {
		// we want the getMainRanking from this class which uses
		// the participation, not the real athlete's category
		Double robi = super.getRobi();
		// logger.trace("getRobi {} {} {} {}", _getAthlete().getShortName(),
		// _getAthlete().getCategory(), getCategory(),
		// robi);
		return robi;
	}

	@Override
	public Integer getRobiRank() {
		return this.a.getRobiRank();
	}

	@Override
	public String getRoundedBodyWeight() {
		return this.a.getRoundedBodyWeight();
	}

	@Override
	public String getSessionPattern() {
		return this.a.getSessionPattern();
	}

	@Override
	public String getShortCategory() {
		return this.a.getShortCategory();
	}

	@Override
	public String getShortName() {
		return this.a.getShortName();
	}

	@Override
	public Double getSinclair() {
		return this.a.getSinclair();
	}

	@Override
	public Double getSinclair(Double bodyWeight1) {
		return this.a.getSinclair(bodyWeight1);
	}

	@Override
	public Double getSinclairFactor() {
		return this.a.getSinclairFactor();
	}

	@Override
	public Double getSinclairForDelta() {
		return this.a.getSinclairForDelta();
	}

	@Override
	public Integer getSinclairRank() {
		return this.a.getSinclairRank();
	}

	@Override
	public Double getSmfForDelta() {
		return this.a.getSmfForDelta();
	}

	@Override
	public Double getSmm() {
		return this.a.getSmm();
	}

	@Override
	public int getSmmRank() {
		return this.a.getSmmRank();
	}

	@Override
	public String getSnatch1ActualLift() {
		String snatch1ActualLift = this.a.getSnatch1ActualLift();
		return snatch1ActualLift;
	}

	@Override
	public Integer getSnatch1AsInteger() {
		return this.a.getSnatch1AsInteger();
	}

	@Override
	public String getSnatch1AutomaticProgression() {
		return this.a.getSnatch1AutomaticProgression();
	}

	@Override
	public String getSnatch1Change1() {
		return this.a.getSnatch1Change1();
	}

	@Override
	public String getSnatch1Change2() {
		return this.a.getSnatch1Change2();
	}

	@Override
	public String getSnatch1Declaration() {
		return this.a.getSnatch1Declaration();
	}

	@Override
	public LocalDateTime getSnatch1LiftTime() {
		return this.a.getSnatch1LiftTime();
	}

	@Override
	public String getSnatch2ActualLift() {
		return this.a.getSnatch2ActualLift();
	}

	@Override
	public Integer getSnatch2AsInteger() {
		return this.a.getSnatch2AsInteger();
	}

	@Override
	public String getSnatch2AutomaticProgression() {
		return this.a.getSnatch2AutomaticProgression();
	}

	@Override
	public String getSnatch2Change1() {
		return this.a.getSnatch2Change1();
	}

	@Override
	public String getSnatch2Change2() {
		return this.a.getSnatch2Change2();
	}

	@Override
	public String getSnatch2Declaration() {
		return this.a.getSnatch2Declaration();
	}

	@Override
	public LocalDateTime getSnatch2LiftTime() {
		return this.a.getSnatch2LiftTime();
	}

	@Override
	public String getSnatch3ActualLift() {
		return this.a.getSnatch3ActualLift();
	}

	@Override
	public Integer getSnatch3AsInteger() {
		return this.a.getSnatch3AsInteger();
	}

	@Override
	public String getSnatch3AutomaticProgression() {
		return this.a.getSnatch3AutomaticProgression();
	}

	@Override
	public String getSnatch3Change1() {
		return this.a.getSnatch3Change1();
	}

	@Override
	public String getSnatch3Change2() {
		return this.a.getSnatch3Change2();
	}

	@Override
	public String getSnatch3Declaration() {
		return this.a.getSnatch3Declaration();
	}

	@Override
	public LocalDateTime getSnatch3LiftTime() {
		return this.a.getSnatch3LiftTime();
	}

	@Override
	public Integer getSnatchAttemptsDone() {
		return this.a.getSnatchAttemptsDone();
	}

	@Override
	public int getSnatchPoints() {
		return this.p.getSnatchPoints();
	}

	@Override
	public int getSnatchRank() {
		return this.p.getSnatchRank();
	}

	@Override
	public int getSnatchTotal() {
		return this.a.getSnatchTotal();
	}

	@Override
	public Integer getStartNumber() {
		return this.a.getStartNumber();
	}

	@Override
	public String getSubCategory() {
		return this.a.getSubCategory();
	}

	@Override
	public String getTeam() {
		return this.a.getTeam();
	}

	@Override
	public Integer getTeamCleanJerkRank() {
		return this.a.getTeamCleanJerkRank();
	}

	@Override
	public Integer getTeamCombinedRank() {
		return this.a.getTeamCombinedRank();
	}

	@Override
	public Integer getTeamCustomRank() {
		return this.a.getTeamCustomRank();
	}

	@Override
	public Boolean getTeamMember() {
		return this.a.getTeamMember();
	}

	@Override
	public Integer getTeamRobiRank() {
		return this.a.getTeamRobiRank();
	}

	@Override
	public Integer getTeamSinclairRank() {
		return this.a.getTeamSinclairRank();
	}

	@Override
	public Integer getTeamSnatchRank() {
		return this.a.getTeamSnatchRank();
	}

	@Override
	public Integer getTeamTotalRank() {
		return this.a.getTeamTotalRank();
	}

	@Override
	public Integer getTotal() {
		return this.a.getTotal();
	}

	@Override
	public int getTotalPoints() {
		return this.p.getTotalPoints();
	}

	@Override
	public int getTotalRank() {
		return this.p.getTotalRank();
	}

	@Override
	public Integer getYearOfBirth() {
		return this.a.getYearOfBirth();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(this.a, this.c, this.p);
		return result;
	}

	@Override
	public boolean isATeamMember() {
		return this.a.isATeamMember();
	}

	@Override
	public int isDeclaring() {
		return this.a.isDeclaring();
	}

	@Override
	public boolean isEligibleForIndividualRanking() {
		return this.a.isEligibleForIndividualRanking();
	}

	@Override
	public boolean isEligibleForTeamRanking() {
		return this.a.isEligibleForTeamRanking();
	}

	@Override
	public boolean isForcedAsCurrent() {
		return this.a.isForcedAsCurrent();
	}

	@Override
	public boolean isInvited() {
		return this.a.isInvited();
	}

	@Override
	public boolean isTeamMember() {
		return this.p.getTeamMember();
	}

	@Override
	public boolean isValidation() {
		return this.a.isValidation();
	}

	@Override
	public String longDump() {
		return this.a.longDump();
	}

	@Override
	public void setCatSinclairRank(int i) {
		this.a.setCatSinclairRank(i);
	}

	@Override
	public void setCoach(String coach) {
		this.a.setCoach(coach);
	}

	@Override
	public void setCustom1(String v) {
		this.a.setCustom1(v);
	}

	@Override
	public void setCustom2(String v) {
		this.a.setCustom2(v);
	}

	@Override
	public void setPersonalBestCleanJerk(Integer personalBestCleanJerk) {
		this.a.setPersonalBestCleanJerk(personalBestCleanJerk);
	}

	@Override
	public void setPersonalBestSnatch(Integer personalBestSnatch) {
		this.a.setPersonalBestSnatch(personalBestSnatch);
	}

	@Override
	public void setPersonalBestTotal(Integer personalBestTotal) {
		this.a.setPersonalBestTotal(personalBestTotal);
	}

	@Override
	public void setRobiRank(Integer robiRank) {
		this.a.setRobiRank(robiRank);
	}

	@Override
	public void setSessionPattern(String ignored) {
		this.a.setSessionPattern(ignored);
	}

	@Override
	public void setSinclairRank(Integer sinclairRank) {
		this.a.setSinclairRank(sinclairRank);
	}

	@Override
	public void setSmmRank(int i) {
		this.a.setSmmRank(i);
	}

	@Override
	public void setTeamCleanJerkRank(Integer teamCJRank) {
		this.p.setTeamCleanJerkRank(teamCJRank);
	}

	@Override
	public void setTeamCombinedRank(Integer teamCombinedRank) {
		this.p.setTeamCombinedRank(teamCombinedRank);
	}

	@Override
	public void setTeamMember(boolean member) {
		this.p.setTeamMember(member);
	}

	@Override
	public void setTeamRobiRank(Integer teamRobiRank) {
		this.p.setTeamRobiRank(teamRobiRank);
	}

	@Override
	public void setTeamSinclairRank(Integer teamSinclairRank) {
		this.p.setTeamSinclairRank(teamSinclairRank);
	}

	@Override
	public void setTeamSnatchRank(Integer teamSnatchRank) {
		this.p.setTeamSnatchRank(teamSnatchRank);
	}

	@Override
	public void setTeamTotalRank(Integer teamTotalRank) {
		this.p.setTeamTotalRank(teamTotalRank);
	}

	@Override
	public String toStringRanks() {
		// super is used because we want the methods from PAthlete to be called
		// and we don't want to copy the code.
		return super.toStringRanks();
	}
}
