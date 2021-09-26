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

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import ch.qos.logback.classic.Logger;

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
public class Participation extends CategoryRankingHolder {

    final static Logger logger = (Logger) LoggerFactory.getLogger(Participation.class);

    @EmbeddedId
    private ParticipationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("athleteId")
    private Athlete athlete;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    private Category category;

    public Participation(Athlete athlete, Category category) {
        this();
        this.athlete = athlete;
        this.category = category;
        this.id = new ParticipationId(athlete.getId(), category.getId());
    }

    private Participation() {
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

    public int getCleanJerkPoints() {
        return AthleteSorter.pointsFormula(cleanJerkRank);
    }

    public int getCustomPoints() {
        return AthleteSorter.pointsFormula(customRank);
    }

    public ParticipationId getId() {
        return id;
    }

    public int getSnatchPoints() {
        return AthleteSorter.pointsFormula(snatchRank);
    }

    public int getTotalPoints() {
        return AthleteSorter.pointsFormula(totalRank);
    }

    @Override
    public int hashCode() {
        return Objects.hash(athlete, category);
    }

    public String long_dump() {
        return "Participation " + System.identityHashCode(this)
                + " [id=" + id
                + ", athlete=" + athlete + "(" + System.identityHashCode(athlete) + ")"
                + ", category=" + category + "(" + System.identityHashCode(category) + ")"
                + ", snatchRank=" + getSnatchRank()
                + ", cleanJerkRank=" + getCleanJerkRank()
                + ", totalRank=" + getTotalRank() + "]";
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    @Override
    public void setCleanJerkRank(int cleanJerkRank) {
        super.setCleanJerkRank(cleanJerkRank);
        logger.trace("cleanJerkRank {}", long_dump());
    }

    /**
     * @param snatchRank the snatchRank to set
     */
    @Override
    public void setSnatchRank(int categoryRank) {
        super.setSnatchRank(categoryRank);
        logger.trace("snatchRank {}", long_dump());
    }


    @Override
    public void setTotalRank(int totalRank) {
        super.setTotalRank(totalRank);
        logger.trace("totalRank {}", long_dump());
    }

    @Override
    public String toString() {
        return "Participation [athlete=" + athlete + ", category=" + category + "]";
    }
}