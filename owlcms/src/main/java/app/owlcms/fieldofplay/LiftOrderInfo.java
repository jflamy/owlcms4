package app.owlcms.fieldofplay;

import java.util.Objects;

import app.owlcms.data.athlete.Athlete;

public class LiftOrderInfo implements Comparable<LiftOrderInfo> {
    public void setLotNumber(int lotNumber) {
        this.lotNumber = lotNumber;
    }

    public void setStartNumber(int startNumber) {
        this.startNumber = startNumber;
    }

    public void setProgression(int progression) {
        this.progression = progression;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public void setAttemptNo(int attemptNo) {
        this.attemptNo = attemptNo-1;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    public int getLotNumber() {
        return lotNumber;
    }

    public int getStartNumber() {
        return startNumber;
    }

    public int getProgression() {
        return progression;
    }

    public int getWeight() {
        return weight;
    }

    public int getAttemptNo() {
        return attemptNo+1;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    int lotNumber;
    int startNumber;
    int progression;
    int weight;
    int attemptNo;
    Athlete athlete;

    @Override
    public int compareTo(LiftOrderInfo actualLiftInfo) {
        int compare = 0;

        compare = Integer.compare(this.weight, actualLiftInfo.weight);
        if (compare != 0) {
            return compare;
        }

        compare = Integer.compare(this.attemptNo, actualLiftInfo.attemptNo);
        if (compare != 0) {
            return compare;
        }

        compare = Integer.compare(this.progression, actualLiftInfo.progression);
        if (compare != 0) {
            return compare;
        }

        compare = Integer.compare(this.startNumber, actualLiftInfo.startNumber);
        if (compare != 0) {
            return compare;
        }

        compare = Integer.compare(this.lotNumber, actualLiftInfo.lotNumber);
        if (compare != 0) {
            return compare;
        }

        return compare;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hash(attemptNo, lotNumber, progression, startNumber, weight);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LiftOrderInfo other = (LiftOrderInfo) obj;
        return attemptNo == other.attemptNo
                && lotNumber == other.lotNumber && progression == other.progression
                && startNumber == other.startNumber && weight == other.weight;
    }

    @Override
    public String toString() {
        return "ActualLiftInfo [athlete=" + athlete.getLastName() + ", weight=" + weight + ", attemptNo=" + attemptNo
                + ", progression=" + progression + ", startNumber=" + startNumber + ", lotNumber=" + lotNumber
                + "]";
    }

}