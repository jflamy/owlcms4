/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import java.util.Objects;

import app.owlcms.data.athlete.Athlete;

public class LiftOrderInfo implements Comparable<LiftOrderInfo> {
    private int lotNumber;

    private int startNumber;

    private int progression;

    private int weight;

    private int attemptNo;

    private Athlete athlete;

    @Override
    public int compareTo(LiftOrderInfo actualLiftInfo) {
        int compare = 0;

        // all snatches come before all clean and jerks
        if (this.getAttemptNo() <= 3 && actualLiftInfo.getAttemptNo() > 3) {
            return -1;
        }
        if (this.getAttemptNo() > 3 && actualLiftInfo.getAttemptNo() <= 3) {
            return 1;
        }

        // same kind of lift.

        compare = Integer.compare(this.getWeight(), actualLiftInfo.getWeight());
        if (compare != 0) {
            return compare;
        }

        compare = Integer.compare(this.getAttemptNo(), actualLiftInfo.getAttemptNo());
        if (compare != 0) {
            return compare;
        }

        compare = Integer.compare(this.getProgression(), actualLiftInfo.getProgression());
        if (compare != 0) {
            return compare;
        }

        compare = Integer.compare(this.getStartNumber(), actualLiftInfo.getStartNumber());
        if (compare != 0) {
            return compare;
        }

        compare = Integer.compare(this.getLotNumber(), actualLiftInfo.getLotNumber());
        if (compare != 0) {
            return compare;
        }

        return compare;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        LiftOrderInfo other = (LiftOrderInfo) obj;
        return getAttemptNo() == other.getAttemptNo()
                && getLotNumber() == other.getLotNumber() && getProgression() == other.getProgression()
                && getStartNumber() == other.getStartNumber() && getWeight() == other.getWeight();
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public int getAttemptNo() {
        return attemptNo;
    }

    public int getLotNumber() {
        return lotNumber;
    }

    public int getProgression() {
        return progression;
    }

    public int getStartNumber() {
        return startNumber;
    }

    public int getWeight() {
        return weight;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + Objects.hash(getAttemptNo(), getLotNumber(), getProgression(), getStartNumber(), getWeight());
        return result;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    public void setAttemptNo(int attemptNo) {
        // 1-based !!!
        this.attemptNo = attemptNo;
    }

    public void setLotNumber(int lotNumber) {
        this.lotNumber = lotNumber;
    }

    public void setProgression(int progression) {
        this.progression = progression;
    }

    public void setStartNumber(int startNumber) {
        this.startNumber = startNumber;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "ActualLiftInfo [athlete=" + getAthlete().getLastName() + ", weight=" + getWeight() + ", attemptNo="
                + getAttemptNo()
                + ", progression=" + getProgression() + ", startNumber=" + getStartNumber() + ", lotNumber="
                + getLotNumber()
                + "]";
    }

}