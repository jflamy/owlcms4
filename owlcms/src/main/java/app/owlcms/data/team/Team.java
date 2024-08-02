/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.team;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.competition.Competition;
import app.owlcms.utils.URLUtils;

/**
 * A non-persistent class to assist in creating reports and team results
 *
 * @author Jean-François Lamy
 */
public class Team {

	public static String[] flagExtensions = {
		".svg",
		".png",
		".jpg",
		".jpeg",
		".webp"
	};
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

	public static String[] getFlagExtensions() {
		return flagExtensions;
	}

	public static String getImgTag(String teamName, String style) {
		String teamFileName = URLUtils.sanitizeFilename(teamName);

		return Arrays.stream(getFlagExtensions())
			.map(ext -> URLUtils.getImgTag("flags/", teamFileName, ext, style))
			.filter(img -> img != null)
			.findFirst()
			.orElse(null);
	}

	public Team(String curTeamName, Gender gender) {
		this.name = curTeamName;
		this.gender = gender;
		this.scoringSystem = Competition.getCurrent().getScoringSystem();
	}

	public double getCatSinclairScore() {
		return this.catSinclairScore;
	}

	public int getCounted() {
		return this.counted;
	}

	public double getGamx() {
		return this.gamx;
	}

	public Gender getGender() {
		return this.gender;
	}

	public String getName() {
		return this.name;
	}

	public int getPoints() {
		return this.points;
	}

	public double getQPoints() {
		return this.qPoints;
	}

	public double getRobi() {
		return this.robi;
	}

	public Double getScore() {
		switch (this.scoringSystem) {
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

	public double getSinclairScore() {
		return this.sinclairScore;
	}

	public long getSize() {
		return this.size;
	}

	/**
	 * @return the smfScore
	 */
	public double getSmfScore() {
		return this.smfScore;
	}

	public void setCatSinclairScore(double catSinclairScore) {
		this.catSinclairScore = catSinclairScore;
	}

	public void setCounted(int counted) {
		this.counted = counted;
	}

	public void setGamx(double gamx) {
		this.gamx = gamx;
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

	public void setQPoints(double qPoints) {
		this.qPoints = qPoints;
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

}
