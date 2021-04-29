package app.owlcms.fieldofplay;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import app.owlcms.data.athlete.Athlete;

public class LiftOrderReconstruction {

    public class ActualLiftInfo implements Comparable<ActualLiftInfo> {
        int lotNumber;
        int startNumber;
        int progression;
        int weight;
        int attemptNo;
        Athlete athlete;

        @Override
        public int compareTo(ActualLiftInfo actualLiftInfo) {
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
            ActualLiftInfo other = (ActualLiftInfo) obj;
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

    final static String LINESEPARATOR = System.getProperty("line.separator");

    private TreeSet<ActualLiftInfo> pastOrder;

    public LiftOrderReconstruction(FieldOfPlay fop) {
        computePastOrder(fop);
    }

    public TreeSet<ActualLiftInfo> getPastOrder() {
        return pastOrder;
    }

    /**
     * @param liftList
     * @return ordered printout of lifters, one per line.
     */
    public String shortDump() {
        StringBuffer sb = new StringBuffer();
        for (ActualLiftInfo ali : this.pastOrder) {
            sb.append(ali.toString());
            sb.append(LINESEPARATOR);
        }
        return sb.toString();
    }

    /**
     * Reconstructed lifting order according to rules
     *
     * @param fop
     */
    private Collection<ActualLiftInfo> computePastOrder(FieldOfPlay fop) {
        this.pastOrder = new TreeSet<>();
        List<Athlete> athletes = fop.getLiftingOrder();

        for (Athlete a : athletes) {
            int prevweight = 0;
            for (int liftNo = 0; liftNo < 6; liftNo++) {
                ActualLiftInfo ali = new ActualLiftInfo();
                switch (liftNo) {
                case 0:
                    prevweight = 0;
                    ali.weight = Math.abs(Athlete.zeroIfInvalid(a.getSnatch1ActualLift()));
                    break;
                case 1:
                    ali.weight = Math.abs(Athlete.zeroIfInvalid(a.getSnatch2ActualLift()));
                    break;
                case 2:
                    ali.weight = Math.abs(Athlete.zeroIfInvalid(a.getSnatch3ActualLift()));
                    break;
                case 3:
                    prevweight = 0;
                    ali.weight = Math.abs(Athlete.zeroIfInvalid(a.getCleanJerk1ActualLift()));
                    break;
                case 4:
                    ali.weight = Math.abs(Athlete.zeroIfInvalid(a.getCleanJerk2ActualLift()));
                    break;
                case 5:
                    ali.weight = Math.abs(Athlete.zeroIfInvalid(a.getCleanJerk3ActualLift()));
                    break;
                }
                ali.athlete = a;
                ali.attemptNo = liftNo;
                ali.progression = ali.weight - prevweight;
                prevweight = ali.weight;
                ali.startNumber = a.getStartNumber();
                ali.lotNumber = a.getLotNumber();
                
                if (ali.weight > 0) {
                    this.pastOrder.add(ali);
                }

            }
        }
        return this.pastOrder;
    }
    
    public ActualLiftInfo getLastLift() {
        if (this.pastOrder == null || this.pastOrder.isEmpty()) {
            return null;
        }
        return this.pastOrder.last();
        
    }

}
