/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.team;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.competition.Competition;

/**
 * A non-persistent class to assist in creating reports and team results
 *
 * @author Jean-François Lamy
 */
public class Team {

	public static Comparator<Team> pointsComparator = ((a,
	        b) -> -ObjectUtils.compare(a.getPoints(), b.getPoints(), true));
	public static Comparator<Team> scoreComparator = ((a,
	        b) -> -ObjectUtils.compare(a.getScore(), b.getScore(), true));
	private int counted;
	private Gender gender;
	private String name;
	private int points = 0;
	private double sinclairScore = 0.0D;
	private double catSinclairScore = 0.0D;
	private long size;
	private double smfScore = 0.0D;
	private double robi = 0.0D;
	private double gamx;
	private double qPoints = 0.0D;

	private Ranking scoringSystem;
	
	public Team(String curTeamName, Gender gender) {
		name = curTeamName;
		this.gender = gender;
		this.scoringSystem = Competition.getCurrent().getScoringSystem();
	}

	public Double getScore() {
		switch (scoringSystem) {
			case BW_SINCLAIR:
				return getSinclairScore();
			case CAT_SINCLAIR:
				return getCatSinclairScore();
			case QPOINTS:
				return getQPoints();
			case ROBI:
				return getRobi();
			case SMM:
				return getSmfScore();
			case GAMX:
				return getGamx();
			default:
				return 0D;
		}
	}

	public int getCounted() {
		return counted;
	}

	public Gender getGender() {
		return gender;
	}

	public String getName() {
		return name;
	}

	public int getPoints() {
		return points;
	}

	public double getRobi() {
		return robi;
	}

	public double getSinclairScore() {
		return sinclairScore;
	}

	public long getSize() {
		return size;
	}

	/**
	 * @return the smfScore
	 */
	public double getSmfScore() {
		return smfScore;
	}

	public void setCounted(int counted) {
		this.counted = counted;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public void setRobi(double robi) {
		this.robi = robi;
	}

	public void setSinclairScore(double d) {
		this.sinclairScore = d;
	}

	public void setSize(long size) {
		this.size = size;
	}

	/**
	 * @param smfScore the smfScore to set
	 */
	public void setSmfScore(double smfScore) {
		this.smfScore = smfScore;
	}

	public double getGamx() {
		return gamx;
	}

	public void setGamx(double gamx) {
		this.gamx = gamx;
	}

	public double getCatSinclairScore() {
		return catSinclairScore;
	}

	public void setCatSinclairScore(double catSinclairScore) {
		this.catSinclairScore = catSinclairScore;
	}

	public double getQPoints() {
		return qPoints;
	}

	public void setQPoints(double qPoints) {
		this.qPoints = qPoints;
	}

}
