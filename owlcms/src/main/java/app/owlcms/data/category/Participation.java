/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.category;

import java.util.Objects;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

import app.owlcms.data.athlete.Athlete;

/**
 * Association class between Athlete and Category. Holds rankings and points of athlete and category.
 *
 * An athlete participates in one or more category (when eligible according to age, gender and qualifying total). A
 * category contains zero or more athletes.
 *
 * @author Jean-François Lamy
 */
@Entity(name = "Participation")
@Table(name = "participation")
public class Participation {

    @EmbeddedId
    private ParticipationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("athleteId")
    private Athlete athlete;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    private Category category;

    private int snatchRank = 0;
    private int cleanJerkRank = 0;
    private int totalRank = 0;

    public int getCleanJerkRank() {
        return cleanJerkRank;
    }

    public void setCleanJerkRank(int cleanJerkRank) {
        this.cleanJerkRank = cleanJerkRank;
    }

    public int getTotalRank() {
        return totalRank;
    }

    public void setTotalRank(int totalRank) {
        this.totalRank = totalRank;
    }

    private int teamRank = 0;

    private int teamPoints = 0;
    
    private Participation() {}

    public Participation(Athlete athlete, Category category) {
        this();
        this.athlete = athlete;
        this.category = category;
        this.id = new ParticipationId(athlete.getId(), category.getId());
    }

    @Override
    public String toString() {
        return "Participation [athlete=" + athlete + ", category=" + category + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Participation that = (Participation) o;
        return Objects.equals(athlete, that.athlete) &&
                Objects.equals(category, that.category);
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public Category getCategory() {
        return category;
    }

    /**
     * @return the snatchRank
     */
    public int getSnatchRank() {
        return snatchRank;
    }

    public ParticipationId getId() {
        return id;
    }

    // Getters and setters omitted for brevity

    /**
     * @return the teamPoints
     */
    public int getTeamPoints() {
        return teamPoints;
    }

    /**
     * @return the teamRank
     */
    public int getTeamRank() {
        return teamRank;
    }

    @Override
    public int hashCode() {
        return Objects.hash(athlete, category);
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * @param snatchRank the snatchRank to set
     */
    public void setSnatchRank(int categoryRank) {
        this.snatchRank = categoryRank;
    }

    /**
     * @param teamPoints the teamPoints to set
     */
    public void setTeamPoints(int teamPoints) {
        this.teamPoints = teamPoints;
    }

    /**
     * @param teamRank the teamRank to set
     */
    public void setTeamRank(int teamRank) {
        this.teamRank = teamRank;
    }
}